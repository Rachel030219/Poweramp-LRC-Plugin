package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
    const val REQUEST_WINDOW = 1
    const val REQUEST_UPDATE = 2

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
            setNormalTextSize(
                MiscUtil.spToPx(
                    preferences.getString("textSize", "18")!!.toFloat(),
                    context
                )
            )
            setCurrentTextSize(
                MiscUtil.spToPx(
                    preferences.getString("textSize", "18")!!.toFloat(),
                    context
                )
            )
            setCurrentColor(
                preferences.getInt(
                    "textColor", ResourcesCompat.getColor(
                        resources,
                        R.color.lrc_current_red,
                        context.theme
                    )
                )
            )
            layoutParams = layoutParams.apply {
                height = MiscUtil.dpToPx(preferences.getString("height", "64")!!.toFloat(), context).toInt()
            }
        }
        var showingBg = true
        layout.setOnClickListener {
            if (showingBg) {
                layout.background = ContextCompat.getDrawable(context, android.R.color.transparent)
                closeButton!!.visibility = View.INVISIBLE
                showingBg = false
            } else {
                layout.background = ContextCompat.getDrawable(context, R.drawable.window_background)
                closeButton!!.visibility = View.VISIBLE
                showingBg = true
            }
        }
        val path = extras.getString(PowerampAPI.Track.PATH)!!
        var lyrics: Lyrics
        val readScope = CoroutineScope(Dispatchers.IO)
        readScope.launch {
            if (nowPlayingFile != path) {
                layout.findViewById<LrcView>(R.id.lrcview).setLabel(context.resources.getString(R.string.lrc_loading))
                nowPlayingFile = path

                lyrics = if (extras.getString("embedded") != null)
                    Lyrics(StringBuilder(extras.getString("embedded")!!), true)
                else if (extras.getBoolean("saf") && !extras.getBoolean("legacy")) {
                    if (extras.getBoolean("safFound") && MiscUtil.checkSAFUsability(context, Uri.parse(path))!!)
                        readFile(path, context, true)
                    else
                        Lyrics(StringBuilder(context.resources.getString(R.string.no_lrc_hint)), false)
                } else
                    readFile(path, context, false)

                if (lyrics.foundCharset)
                    layout.findViewById<LrcView>(R.id.lrcview).apply {
                        loadLrc(lyrics.text.toString())
                        setLabel(context.resources.getString(R.string.no_lrc_hint))
                    }
                else
                    layout.findViewById<LrcView>(R.id.lrcview).apply {
                        setLabel(lyrics.text.toString())
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

    fun sendNotification(context: Context?, extras: Bundle?, ongoing: Boolean){
        val realExtras: Bundle = extras!!
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

    private suspend fun readFile(path: String, context: Context, SAF: Boolean) = withContext(Dispatchers.IO){
        val lyrics = StringBuilder()
        val ins: BufferedInputStream?
        val file: File
        var found = false
        if (SAF) {
            ins = context.contentResolver.openInputStream(Uri.parse(path))?.buffered()
        } else {
            // embedded lyrics
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("embedded", false)) {
                ins = null
                found = true
                var embeddedLyrics: StringBuilder? = null
                val songFile = File(extras!!.getString(PowerampAPI.Track.PATH)!!)
                val mp3File = Mp3File(songFile)
                if (mp3File.hasId3v2Tag() && mp3File.id3v2Tag.lyrics != null && mp3File.id3v2Tag.lyrics.isNotEmpty()) {
                    embeddedLyrics = StringBuilder(mp3File.id3v2Tag.lyrics)
                }
                if (embeddedLyrics != null)
                    lyrics.append(embeddedLyrics.toString())
            } else {
                file = File(path)
                ins = if (file.exists())
                    file.inputStream().buffered()
                else
                    null
            }
        }
        ins?.use {
            try {
                lyrics.append(it.bufferedReader(charset = findCharset(it, context)).readText())
                found = true
            } catch (e: UnsupportedCharsetException) {
                lyrics.append(context.resources.getString(R.string.no_charset_hint))
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
        val buf = ByteArray(4096)
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

    class Lyrics(text: StringBuilder, foundCharset: Boolean) {
        var text = StringBuilder()
        var foundCharset = false
        init {
            this.text = text
            this.foundCharset = foundCharset
        }
    }
}