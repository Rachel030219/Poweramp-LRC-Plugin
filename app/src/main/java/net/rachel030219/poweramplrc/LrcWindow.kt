package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.Button
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs

import me.wcy.lrcview.LrcView
import com.maxmpz.poweramp.player.PowerampAPI

object LrcWindow {
    var window: WindowManager? = null
    var params: WindowManager.LayoutParams? = null
    var displaying = false
    var initialized = false
    var lastY: Float = 0f
    var lastYForClick: Float = 0F
    var extras: Bundle? = null
    var nowPlayingFile = ""
    const val REQUEST_WINDOW = 1
    const val REQUEST_UPDATE = 2

    @SuppressLint("ClickableViewAccessibility")
    fun initialize(context: Context, layout: View){
        if (!displaying) {
            if (!initialized) {
                window = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                params = WindowManager.LayoutParams()
                params!!.width = WindowManager.LayoutParams.MATCH_PARENT
                params!!.height = WindowManager.LayoutParams.WRAP_CONTENT
                params!!.gravity = Gravity.TOP
                params!!.flags =
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    params!!.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    params!!.type = WindowManager.LayoutParams.TYPE_TOAST
                params!!.format = PixelFormat.TRANSLUCENT
                val closeButton = layout.findViewById<Button>(R.id.close)
                closeButton.setOnClickListener {
                    destroy(layout)
                    sendNotification(context, extras, false)
                }
                var showingBg = true
                layout.setOnClickListener {
                    if (showingBg) {
                        layout.background = context.getDrawable(android.R.color.transparent)
                        closeButton.visibility = View.INVISIBLE
                        showingBg = false
                    } else {
                        layout.background = context.getDrawable(R.drawable.window_background)
                        closeButton.visibility = View.VISIBLE
                        showingBg = true
                    }
                }
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
            } else {
                layout.visibility = View.VISIBLE
                window!!.updateViewLayout(layout, params)
                displaying = true
            }
        }
    }

    fun refresh(layout: View, extras: Bundle) {
        //TODO:解决暂停再打开会跳回原时间的问题
        this.extras = extras
        val path = extras.getString(PowerampAPI.Track.PATH)
        val lrcFile: File
        if (nowPlayingFile != path) {
            nowPlayingFile = path!!
            lrcFile = File(extractAndReplaceExt(path))
            layout.findViewById<LrcView>(R.id.lrcview).loadLrc(lrcFile)
        } else {
            refreshTime(extras.getInt(PowerampAPI.Track.POSITION), layout)
        }
        if (!displaying) {
            layout.visibility = View.VISIBLE
            displaying = true
        }
        window!!.updateViewLayout(layout, params)
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

    fun sendNotification(context: Context?, extras: Bundle?, ongoing: Boolean) {
        val realExtras: Bundle = extras!!
        val pendingIntent: PendingIntent?
        val builder = NotificationCompat.Builder(context!!, "ENTRANCE")
        builder.setContentTitle(extras.getString(PowerampAPI.Track.TITLE) + " - " + extras.getString(PowerampAPI.Track.ARTIST))
        builder.setSmallIcon(R.drawable.ic_notification)
        builder.setAutoCancel(false)
        builder.priority = NotificationCompat.PRIORITY_MIN
        if (ongoing) {
            builder.setOngoing(true)
            pendingIntent = PendingIntent.getService(
                context,
                REQUEST_WINDOW,
                Intent(context, LrcService::class.java).putExtra("request", REQUEST_WINDOW).putExtras(realExtras),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setContentText(context.resources.getString(R.string.notification_message_hide))
        } else {
            builder.setOngoing(false)
            pendingIntent = PendingIntent.getService(
                context,
                REQUEST_WINDOW,
                Intent(context, LrcService::class.java).putExtra("request", REQUEST_WINDOW).putExtras(realExtras),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setContentText(context.resources.getString(R.string.notification_message_show))
        }
        builder.setContentIntent(pendingIntent)
        builder.setOnlyAlertOnce(true)
        val manager = NotificationManagerCompat.from(context)
        manager.notify(212, builder.build())
    }

    private fun extractAndReplaceExt (oldString: String): String {
        return Environment.getExternalStorageDirectory().toString() + "/" + StringBuilder(oldString).substring(0, oldString.lastIndexOf('.')).substringAfter('/') + ".lrc"
    }
}