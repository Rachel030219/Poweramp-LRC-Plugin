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
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.maxmpz.poweramp.player.PowerampAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.wcy.lrcview.LrcView
import org.mozilla.universalchardet.UniversalDetector
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
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

    @Suppress("BlockingMethodInNonBlockingContext")
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
        val lrc: StringBuilder = StringBuilder()
        val readScope = CoroutineScope(Dispatchers.Main)
        readScope.launch {
            if (nowPlayingFile != path) {
                nowPlayingFile = path
                if (extras.getBoolean("saf") && !extras.getBoolean("legacy")) {
                    if (extras.getBoolean("safFound") && MiscUtil.checkSAFUsability(context, Uri.parse(path))!!) {
                        // TODO: unable to read anything when UTF-16LE
                        lrc.append(readFile(path, context, true))
                    } else {
                        lrc.append(context.resources.getString(R.string.no_lrc_hint))
                    }
                } else {
                    lrc.append(readFile(path, context, false))
                }
                layout.findViewById<LrcView>(R.id.lrcview).loadLrc(lrc.toString())
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

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun readFile(path: String, context: Context, SAF: Boolean) = withContext(Dispatchers.IO){
        val lyrics = StringBuilder()
        val ins: InputStream?
        val file: File?
        if (SAF) {
            ins = context.contentResolver.openInputStream(Uri.parse(path))
        } else {
            file = File(path)
            ins = if (file.exists())
                file.inputStream()
            else
                null
        }
        var encoding: Charset
        ins?.use {
            try {
                encoding = findCharset(ins, context)
                if (SAF) {
                    lyrics.append(it.bufferedReader(charset = encoding).readText())
                } else {
                    lyrics.append(it.bufferedReader(charset = encoding).readText())
                }
            } catch (e: UnsupportedCharsetException) {
                lyrics.append(context.resources.getString(R.string.no_charset_hint))
            } finally {
                it.close()
            }
        }
        lyrics
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun findCharset (inputStream: InputStream?, context: Context) = withContext(Dispatchers.IO) {
        var charset = Charsets.UTF_8
        inputStream.use {
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("charset", false)) {
                charset = Charset.forName(UniversalDetector.detectCharset(it))
            }
        }
        charset
    }
}