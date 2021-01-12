package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.wcy.lrcview.LrcView
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.CannotReadVideoException
import org.jaudiotagger.tag.FieldKey
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
    var lastX: Float = 0f
    var lastY: Float = 0f
    var lastXForClick: Float = 0F
    var lastYForClick: Float = 0F // used to determine being clicked
    var nowPlayingFile = ""
    // components, initialized and refreshed separately
    var lrcView: LrcView? = null
    var closeButton: Button? = null
    var lockButton: Button? = null
    // used to determine whether a file was found in sub dir mode
    var finalUri: Uri = Uri.EMPTY
    // the global offset
    var globalOffset: Long? = 0

    private val readScope = CoroutineScope(Dispatchers.IO)
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

            // set width to screen width (in portrait) to avoid padding to whole screen in landscape
            val screen = Point()
            window!!.defaultDisplay.getSize(screen)
            width = if (screen.x < screen.y) screen.x else screen.y
        }
        closeButton = layout.findViewById(R.id.close)
        lockButton = layout.findViewById(R.id.lock)
        lrcView = layout.findViewById(R.id.lrcview)
        layout.setOnTouchListener { _, event ->
            if (displaying) {
                when (event!!.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = event.rawX
                        lastY = event.rawY
                        lastXForClick = event.rawX
                        lastYForClick = event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val rawX = event.rawX
                        val rawY = event.rawY
                        params!!.y += (rawY - lastY).toInt()
                        params!!.x += (rawX - lastX).toInt()
                        lastX = rawX
                        lastY = rawY
                        window!!.updateViewLayout(layout, params)
                    }
                    MotionEvent.ACTION_UP -> {
                        if (abs(event.rawX - lastXForClick) <= 5 && abs(event.rawY - lastYForClick) <= 5) {
                            layout.callOnClick()
                        }
                        lastX = event.rawX
                        lastY = event.rawY
                        lastXForClick = event.rawX
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
        // refresh settings
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val embedded = preferences.getBoolean("embedded", false)
        lrcView?.apply {
            setNormalTextSize(MiscUtil.spToPx(preferences.getString("textSize", "18")!!.toFloat(), context))
            setCurrentTextSize(MiscUtil.spToPx(preferences.getString("textSize", "18")!!.toFloat(), context))
            setCurrentColor(preferences.getInt("textColor", ResourcesCompat.getColor(resources, R.color.lrc_current_red, context.theme)))
            setCurrentTextStrokeColor(preferences.getInt("strokeColor", ResourcesCompat.getColor(resources, R.color.lrc_stroke_dark, context.theme)))
            setCurrentTextStrokeWidth(preferences.getString("strokeWidth", "5")!!.toFloat())
            setAnimationDuration(preferences.getString("duration", "250")!!.toLong())
            layoutParams = layoutParams.apply {
                height = MiscUtil.dpToPx(preferences.getString("height", "64")!!.toFloat(), context).toInt()
            }
        }
        if (nowPlayingFile != extras.getString(PowerampAPI.Track.PATH)) {
            layout.findViewById<LrcView>(R.id.lrcview).setLabel(context.resources.getString(R.string.lrc_loading))
        }
        updateLyrics(layout.findViewById(R.id.lrcview), extras.getString(PowerampAPI.Track.PATH).toString(), embedded, context)
        refreshTime(extras.getInt(PowerampAPI.Track.POSITION), layout, 0)
        if (popup && !displaying) {
            layout.visibility = View.VISIBLE
            displaying = true
        }
        if (initialized) {
            window!!.updateViewLayout(layout, params)
        }
    }

    fun refreshTime(time: Int, layout: View, offset: Long) {
        if (time != -1) {
            val timeInMillis = TimeUnit.MILLISECONDS.convert(time.toLong(), TimeUnit.SECONDS) + offset
            if (globalOffset != null)
                layout.findViewById<LrcView>(R.id.lrcview).updateTime(timeInMillis + globalOffset!!)
            else
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
            layout.callOnClick()
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

    private fun updateLyrics(lrcView: LrcView, path: String, embedded: Boolean, context: Context? = null) {
        // initialize variables persistent between iterations
        var tempContext: Context? = null
        if (context != null)
            tempContext = context
        
        if (tempContext != null) {
            // find and read files
            val subDir = (PreferenceManager.getDefaultSharedPreferences(tempContext).getBoolean("subDir", false))
            val fileName = if (embedded) path.substringAfterLast("/") else MiscUtil.extractAndReplaceExt(path.substringAfterLast("/"))
            // databases to store folders and paths to the files
            val foldersDatabaseHelper = FoldersDatabaseHelper(tempContext)
            val pathsDatabaseHelper = PathsDatabaseHelper(tempContext)
            val folders = foldersDatabaseHelper.fetchFolders()
            readScope.launch {
                if (nowPlayingFile != path) {
                    // path from intent passed by Poweramp
                    // check first if song is instrumental
                    if (!pathsDatabaseHelper.isInstrumental(path)) {
                        var lyricPath = Uri.EMPTY
                        pathsDatabaseHelper.queryPath(path, embedded).forEach {
                            val fileByMap = DocumentFile.fromSingleUri(tempContext, Uri.parse(it))
                            if (fileByMap != null && fileByMap.isFile && fileByMap.canRead()) {
                                lyricPath = fileByMap.uri
                            } else {
                                pathsDatabaseHelper.removePath(it)
                            }
                        }
                        if (lyricPath == Uri.EMPTY) {
                            folders.forEach { folder ->
                                if (checkSAFDirUsability(folder.path, tempContext)) {
                                    findFile(path, folder, tempContext, subDir, embedded)?.also {
                                        lyricPath = it
                                        pathsDatabaseHelper.addPath(PathsDatabaseHelper.Companion.Path(path, it.toString(), embedded))
                                    }
                                } else {
                                    foldersDatabaseHelper.removeFolder(folder)
                                }
                            }
                        }
                        // if the file is found, read it and return the content; else return "not found"
                        if(lyricPath != Uri.EMPTY) {
                            var lyricText = tempContext.resources.getString(R.string.no_lrc_hint)
                            var found = false
                            val ins: BufferedInputStream? = tempContext.contentResolver.openInputStream(lyricPath)?.buffered()
                            if (embedded) {
                                val audioCacheFile = File(tempContext.cacheDir, fileName)
                                FileOutputStream(audioCacheFile).buffered().use {
                                    ins?.copyTo(it)
                                    it.flush()
                                }
                                if (audioCacheFile.exists()) {
                                    try {
                                        val audioFile = AudioFileIO.read(audioCacheFile)
                                        if (audioFile.tag != null && audioFile.tag.hasField(FieldKey.LYRICS) && audioFile.tag.getFirst(FieldKey.LYRICS).isNotBlank()) {
                                            lyricText = audioFile.tag.getFirst(FieldKey.LYRICS)
                                            found = true
                                        } else {
                                            found = false
                                        }
                                        audioCacheFile.delete()
                                    } catch (e: CannotReadException) {
                                        found = false
                                    } catch (e: CannotReadVideoException) {
                                        found = false
                                    }
                                }
                            } else {
                                try {
                                    ins?.bufferedReader(charset = findCharset(ins, tempContext))?.use {
                                        lyricText = it.readText()
                                        found = true
                                    }
                                } catch (e: UnsupportedCharsetException) {
                                    lyricText = tempContext.resources.getString(R.string.no_charset_hint)
                                    found = false
                                }
                            }
                            updateLrcView(lrcView, path, embedded, Lyrics(lyricText, found))
                        } else
                            updateLrcView(lrcView, path, embedded, Lyrics(tempContext.resources.getString(R.string.no_lrc_hint), false))
                    } else {
                        updateLrcView(lrcView, path, embedded, Lyrics(tempContext.resources.getString(R.string.instrumental_hint), false))
                    }
                }
            }
        }
    }

    private suspend fun updateLrcView (lrcView: LrcView, path: String, embedded: Boolean, lyrics: Lyrics) = withContext(Dispatchers.Main) {
        if (lyrics.found) {
            lrcView.apply {
                loadLrc(lyrics.text)
                setLabel(context.resources.getString(R.string.no_lrc_hint))
            }
            nowPlayingFile = path
        } else {
            if (embedded) {
                updateLyrics(lrcView, path, false)
            } else {
                lrcView.apply {
                    setLabel(lyrics.text)
                    loadLrc("")
                    // TODO: send notifications here

                }
                nowPlayingFile = path
            }
        }
    }

    private fun findCharset(inputStream: BufferedInputStream?, context: Context): Charset {
        var charset = Charsets.UTF_8
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("charset", false) && inputStream != null) {
            inputStream.mark(Int.MAX_VALUE)
            val buf = ByteArray(8*1024)
            val detector = UniversalDetector(null)
            var nread: Int
            while (inputStream.read(buf).also { nread = it } > 0 && !detector.isDone) {
                detector.handleData(buf, 0, nread)
            }
            detector.dataEnd()
            val charsetName = detector.detectedCharset
            detector.reset()
            inputStream.reset()
            if (charsetName != null)
                charset = Charset.forName(charsetName)
            else
                throw UnsupportedCharsetException("null")
        }
        return charset
    }

    private fun checkSAFDirUsability(path: String, context: Context): Boolean {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        return try {
            context.contentResolver.takePersistableUriPermission(Uri.parse(path), takeFlags)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    private fun findFile(filePath: String, folder: FoldersDatabaseHelper.Companion.Folder, context: Context, subDir: Boolean, embedded: Boolean): Uri?{
        finalUri = Uri.EMPTY
        val treeUri = Uri.parse(folder.path)
        var file: DocumentFile? = null
        val elements = if (embedded) filePath.split("/") else MiscUtil.extractAndReplaceExt(filePath).split("/")
        if (subDir) {
            file = findInSubfolder(treeUri, elements[elements.count() - 1], context)
        } else {
            DocumentFile.fromTreeUri(context, Uri.parse(folder.path))?.let {
                file = it.findFile(elements[elements.count() - 1])
            }
        }
        if (file != null && file!!.exists()) {
            return file!!.uri
        }
        return null
    }

    private fun findInSubfolder(treeUri: Uri, fileName: String, context: Context): DocumentFile? {
        val treeContractUri: Uri = try {
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DocumentsContract.getDocumentId(treeUri))
        } catch (e: Exception) {
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        }
        val dirList = mutableListOf<Uri>()
        context.contentResolver.query(treeContractUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val type = cursor.getString(1)
                val name = cursor.getString(2)
                if (type == DocumentsContract.Document.MIME_TYPE_DIR) {
                    dirList.add(DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, id))
                } else if (name == fileName) {
                    finalUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                }
            }
        }
        dirList.forEach {
            findInSubfolder(it, fileName, context)
        }
        return if (finalUri != Uri.EMPTY) DocumentFile.fromTreeUri(context, finalUri) else null
    }

    class Lyrics(val text: String, val found: Boolean)
}