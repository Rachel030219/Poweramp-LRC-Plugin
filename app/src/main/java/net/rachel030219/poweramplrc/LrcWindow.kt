package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.maxmpz.poweramp.player.PowerampAPI
import com.mpatric.mp3agic.Mp3File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.wcy.lrcview.LrcView
import org.mozilla.universalchardet.UniversalDetector
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object LrcWindow {
    var window: WindowManager? = null
    var params: WindowManager.LayoutParams? = null
    var displaying = false
    var initialized = false
    var lastY: Float = 0f
    var lastYForClick: Float = 0F // used to determine being clicked
    var extras: Bundle? = null
    var nowPlayingFile = ""
    // components, initialized and refreshed separately
    var lrcView: LrcView? = null
    var closeButton: Button? = null
    var lockButton: Button? = null
    const val REQUEST_WINDOW = 1
    const val REQUEST_UPDATE = 2
    const val REQUEST_UNLOCK = 3

    @SuppressLint("ClickableViewAccessibility")
    fun initialize(context: Context, layout: View){
        window = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_TOAST
            format = PixelFormat.TRANSLUCENT
        }
        closeButton = layout.findViewById(R.id.close)
        lockButton = layout.findViewById(R.id.lock)
        lrcView = layout.findViewById(R.id.lrcview)
        layout.setOnTouchListener { _, event ->
            if (displaying) {
                when (event!!.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastY = event.rawY
                        lastYForClick = event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val rawY = event.rawY
                        params!!.y += (rawY - lastY).toInt()
                        lastY = rawY
                        window!!.updateViewLayout(layout, params)
                    }
                    MotionEvent.ACTION_UP -> {
                        if (abs(event.rawY - lastYForClick) <= 5) {
                            layout.callOnClick()
                        }
                        lastY = event.rawY
                        lastYForClick = event.rawY
                    }
                }
            }
            true
        }
        window!!.addView(layout, params)
        displaying = true
        initialized = true
    }

    fun refresh(layout: View, extras: Bundle, popup: Boolean, context: Context) {
        this.extras = extras
        // refresh settings
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        lrcView?.apply {
            setNormalTextSize(MiscUtil.spToPx(preferences.getString("textSize", "18")!!.toFloat(), context))
            setCurrentTextSize(MiscUtil.spToPx(preferences.getString("textSize", "18")!!.toFloat(), context))
            setCurrentColor(preferences.getInt("textColor", ResourcesCompat.getColor(resources, R.color.lrc_current_red, context.theme)))
            layoutParams = layoutParams.apply {
                height = MiscUtil.dpToPx(preferences.getString("height", "64")!!.toFloat(), context).toInt()
            }
        }
        val path = extras.getString(PowerampAPI.Track.PATH)!!
        var lyrics: Lyrics
        val databaseHelper = FoldersDatabaseHelper(context)
        val folders = databaseHelper.fetchFolders()
        val readScope = CoroutineScope(Dispatchers.IO)
        readScope.launch {
            if (nowPlayingFile != path) {
                layout.findViewById<LrcView>(R.id.lrcview).setLabel(context.resources.getString(R.string.lrc_loading))
                nowPlayingFile = path

                // path 来自 Poweramp 传入的 Intent
                var lyricPath = ""
                val embedded = (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("embedded", false))
                val fileName = if (embedded) path.substringAfterLast("/") else MiscUtil.extractAndReplaceExt(path.substringAfterLast("/"))
                folders.forEach { folder ->
                    if (checkSAFDirUsability(folder.path, context)) {
                        DocumentFile.fromTreeUri(context, Uri.parse(folder.path))?.let {
                            val file = it.findFile(fileName)
                            if (it.isDirectory && file != null && file.exists()) {
                                lyricPath = file.uri.toString()
                            }
                        }
                    } else {
                        databaseHelper.removeFolder(folder)
                    }
                }
                lyrics = if(lyricPath.isNotBlank())
                    readFile(lyricPath, context, embedded, fileName)
                else
                    Lyrics(context.resources.getString(R.string.no_lrc_hint), false)

                if (lyrics.found)
                    layout.findViewById<LrcView>(R.id.lrcview).apply {
                        loadLrc(lyrics.text)
                        setLabel(context.resources.getString(R.string.no_lrc_hint))
                    }
                else
                    layout.findViewById<LrcView>(R.id.lrcview).apply {
                        setLabel(lyrics.text)
                        loadLrc("")
                    }
            }
        }
        refreshTime(extras.getInt(PowerampAPI.Track.POSITION), layout)
        if (popup && !displaying) {
            layout.visibility = View.VISIBLE
            displaying = true
        }
        if (initialized) {
            window!!.updateViewLayout(layout, params)
        }
    }

    fun refreshTime(time: Int, layout: View) {
        if (time != -1) {
            val timeInMillis = TimeUnit.MILLISECONDS.convert(time.toLong(), TimeUnit.SECONDS)
            layout.findViewById<LrcView>(R.id.lrcview).updateTime(timeInMillis)
        }
    }

    fun destroy(layout: View) {
        layout.visibility = View.GONE
        window!!.updateViewLayout(layout, params)
        displaying = false
    }

    fun unlock(layout: View) {
        if (initialized) {
            params!!.flags =
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            window!!.updateViewLayout(layout, params)
        }
    }

    fun sendNotification(context: Context?, extras: Bundle, ongoing: Boolean){
        val realExtras: Bundle = extras
        val pendingIntent: PendingIntent?
        val builder = NotificationCompat.Builder(context!!, "ENTRANCE").apply {
            setContentTitle(extras.getString(PowerampAPI.Track.TITLE) + " - " + extras.getString(PowerampAPI.Track.ARTIST))
            setSmallIcon(R.drawable.ic_notification)
            setAutoCancel(false)
            priority = NotificationCompat.PRIORITY_MIN
            setOnlyAlertOnce(true)
            if (ongoing) {
                setOngoing(true)
                pendingIntent = PendingIntent.getService(
                    context,
                    REQUEST_WINDOW,
                    Intent(context, LrcService::class.java).putExtra("request", REQUEST_WINDOW).putExtras(realExtras),
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
                setContentText(context.resources.getString(R.string.notification_message_hide))
            } else {
                setOngoing(false)
                pendingIntent = PendingIntent.getService(
                    context,
                    REQUEST_WINDOW,
                    Intent(context, LrcService::class.java).putExtra("request", REQUEST_WINDOW).putExtras(realExtras),
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
                setContentText(context.resources.getString(R.string.notification_message_show))
            }
            setContentIntent(pendingIntent)
        }
        NotificationManagerCompat.from(context).notify(212, builder.build())
    }

    private suspend fun readFile(path: String, context: Context, embedded: Boolean, name: String) = withContext(Dispatchers.IO){
        var lyrics = context.resources.getString(R.string.no_lrc_hint)
        var found = false
        val ins: BufferedInputStream? = context.contentResolver.openInputStream(Uri.parse(path))?.buffered()
        if (embedded) {
            val mp3CacheFile = File(context.cacheDir, name)
            launch(Dispatchers.IO) {
                FileOutputStream(mp3CacheFile).buffered().use {
                    ins?.copyTo(it)
                    it.flush()
                }
            }.join()
            if (mp3CacheFile.exists()) {
                val mp3File = Mp3File(mp3CacheFile)
                if (mp3File.hasId3v2Tag() && mp3File.id3v2Tag.lyrics != null && mp3File.id3v2Tag.lyrics.isNotEmpty()) {
                    lyrics = mp3File.id3v2Tag.lyrics
                    found = true
                } else {
                    found = false
                }
                mp3CacheFile.delete()
            }
        } else {
            try {
                ins?.bufferedReader(charset = findCharset(ins, context))?.use {
                    lyrics = it.readText()
                    found = true
                }
            } catch (e: UnsupportedCharsetException) {
                lyrics = context.resources.getString(R.string.no_charset_hint)
                found = false
            }
        }
        Lyrics(lyrics, found)
    }

    private fun findCharset(inputStream: BufferedInputStream?, context: Context): Charset {
        var charset = Charsets.UTF_8
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("charset", false) && inputStream != null) {
            val charsetName = detectCharset(inputStream)
            if (charsetName != null)
                charset = Charset.forName(charsetName)
            else
                throw UnsupportedCharsetException("null")
        }
        return charset
    }

    private fun detectCharset(inputStream: BufferedInputStream): String? {
        inputStream.mark(Int.MAX_VALUE)
        val buf = ByteArray(8*1024)
        val detector = UniversalDetector(null)
        var nread: Int
        while (inputStream.read(buf).also { nread = it } > 0 && !detector.isDone) {
            detector.handleData(buf, 0, nread)
        }
        detector.dataEnd()
        val encoding = detector.detectedCharset
        detector.reset()
        inputStream.reset()
        return encoding
    }

    private fun checkSAFDirUsability (path: String, context: Context): Boolean {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        return try {
            context.contentResolver.takePersistableUriPermission(Uri.parse(path), takeFlags)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    class Lyrics(val text: String, val found: Boolean)
}