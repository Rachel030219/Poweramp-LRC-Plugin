package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.maxmpz.poweramp.player.PowerampAPI
import com.maxmpz.poweramp.player.RemoteTrackTime

class LrcService: Service(), RemoteTrackTime.TrackTimeListener {
    private var mWindow: View? = null
    private var timerOn = false
    private var mCurrentPosition = -1
    private var remoteTrackTime: RemoteTrackTime? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("InflateParams")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mWindow == null) {
            mWindow = LayoutInflater.from(this).inflate(R.layout.lrc_window, null)
        }
        if (intent!!.getBooleanExtra("foreground", false)) {
            val notificationIntent = Intent().apply {
                action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, "PLACEHOLDER")
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                PathActivity.REQUEST_PATH,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notificationBuilder = NotificationCompat.Builder(this, "PLACEHOLDER").apply {
                setContentTitle(resources.getString(R.string.app_name))
                setContentText(resources.getString(R.string.notification_placeholder_message))
                setSmallIcon(R.drawable.ic_notification)
                priority = NotificationCompat.PRIORITY_LOW
                setContentIntent(pendingIntent)
                setOnlyAlertOnce(true)
            }
            startForeground(211, notificationBuilder.build())
        }
        if (intent.hasExtra("request")) {
            intent.extras?.let { extras ->
                when (intent.getIntExtra("request", 0)) {
                    LrcWindow.REQUEST_WINDOW -> {
                        timerOn = if (extras.getBoolean(PowerampAPI.PAUSED)) {
                            remoteTrackTime!!.stopSongProgress()
                            false
                        } else {
                            remoteTrackTime!!.startSongProgress()
                            true
                        }
                        if (LrcWindow.displaying) {
                            LrcWindow.destroy(mWindow!!)
                            LrcWindow.sendNotification(this, extras, false)
                        } else {
                            if (timerOn) {
                                extras.putInt(PowerampAPI.Track.POSITION, mCurrentPosition)
                            } else {
                                remoteTrackTime!!.updateTrackPosition(extras.getInt(PowerampAPI.Track.POSITION))
                            }
                            if (!LrcWindow.initialized && mWindow != null) {
                                var showingBg = true
                                mWindow!!.setOnClickListener {
                                    if (showingBg) {
                                        mWindow!!.background = ContextCompat.getDrawable(this, android.R.color.transparent)
                                        LrcWindow.closeButton?.visibility = View.INVISIBLE
                                        LrcWindow.lockButton?.visibility = View.INVISIBLE
                                        showingBg = false
                                    } else {
                                        val opacity = PreferenceManager.getDefaultSharedPreferences(this).getString("opacity", "64")?.toIntOrNull()
                                        if (opacity != null) {
                                            if (opacity > 255)
                                                mWindow!!.background = ColorDrawable(Color.argb(255, 0, 0, 0))
                                            else if (opacity < 0)
                                                mWindow!!.background = ColorDrawable(Color.argb(0, 0, 0, 0))
                                            else
                                                mWindow!!.background = ColorDrawable(Color.argb(opacity, 0, 0, 0))
                                        }
                                        else
                                            mWindow!!.background = ColorDrawable(Color.argb(64, 0, 0, 0))
                                        LrcWindow.closeButton?.visibility = View.VISIBLE
                                        LrcWindow.lockButton?.visibility = View.VISIBLE
                                        showingBg = true
                                    }
                                }
                                mWindow!!.findViewById<Button>(R.id.close).setOnClickListener {
                                    LrcWindow.destroy(mWindow!!)
                                    LrcWindow.sendNotification(this, extras, false)
                                }
                                mWindow!!.findViewById<Button>(R.id.lock).setOnClickListener {
                                    mWindow!!.callOnClick()
                                    LrcWindow.params?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                    LrcWindow.window?.updateViewLayout(mWindow!!, LrcWindow.params)
                                    val lockNotification = NotificationCompat.Builder(this, "LOCK").let {
                                        it.setSmallIcon(R.drawable.ic_lock)
                                        it.setContentTitle(this.resources.getString(R.string.notification_lock_title))
                                        it.setContentText(this.resources.getString(R.string.notification_lock_message))
                                        it.setContentIntent(PendingIntent.getService(this, LrcWindow.REQUEST_UNLOCK, Intent(this, LrcService::class.java).putExtra("request", LrcWindow.REQUEST_UNLOCK), PendingIntent.FLAG_CANCEL_CURRENT))
                                        it.priority = NotificationCompat.PRIORITY_MIN
                                        it.setOnlyAlertOnce(true)
                                        it.setAutoCancel(true)
                                        it.build()
                                    }
                                    NotificationManagerCompat.from(this).notify(213, lockNotification)
                                }
                                LrcWindow.initialize(this, mWindow!!)
                            }
                            LrcWindow.refresh(mWindow!!, extras, true, this)
                            LrcWindow.sendNotification(this, extras, true)
                        }
                    }
                    LrcWindow.REQUEST_UPDATE -> {
                        if (mWindow != null) {
                            LrcWindow.refresh(mWindow!!, extras, false, this)
                            remoteTrackTime!!.updateTrackPosition(extras.getInt(PowerampAPI.Track.POSITION))
                            remoteTrackTime!!.updateTrackDuration(extras.getInt(PowerampAPI.Track.DURATION))
                            timerOn = if (extras.getBoolean(PowerampAPI.PAUSED)) {
                                remoteTrackTime!!.stopSongProgress()
                                false
                            } else {
                                remoteTrackTime!!.startSongProgress()
                                true
                            }
                        }
                    }
                    LrcWindow.REQUEST_UNLOCK -> {
                        if (mWindow != null) {
                            LrcWindow.unlock(mWindow!!)
                        }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        remoteTrackTime = RemoteTrackTime(this)
        remoteTrackTime!!.setTrackTimeListener(this)
        remoteTrackTime!!.registerAndLoadStatus()
    }

    override fun onDestroy() {
        remoteTrackTime!!.setTrackTimeListener(null)
        remoteTrackTime!!.unregister()
        stopForeground(true)
    }

    override fun onTrackPositionChanged(position: Int) {
        mCurrentPosition = position
        LrcWindow.refreshTime(position, mWindow!!)
    }

    override fun onTrackDurationChanged(duration: Int) {

    }
}