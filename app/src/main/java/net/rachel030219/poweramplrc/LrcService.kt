package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
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
    private var opacity: Int? = 64
    private var showingBg = true

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("InflateParams")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        opacity = PreferenceManager.getDefaultSharedPreferences(this).getString("opacity", "64")?.toIntOrNull()
        LrcWindow.globalOffset = PreferenceManager.getDefaultSharedPreferences(this).getString("offset", "0")?.toLongOrNull()
        if (mWindow == null) {
            mWindow = LayoutInflater.from(this).inflate(R.layout.lrc_window, null)
        }
        mWindow?.apply {
            if (opacity != null && showingBg)
                background = ColorDrawable(Color.argb(opacity!!, 0, 0, 0))
        }
        if (intent!!.getBooleanExtra("foreground", false)) {
            val notificationIntent = Intent().apply {
                action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, "PLACEHOLDER")
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                PathActivity.REQUEST_FOLDER,
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
                        timerOn = if (extras.getBoolean(PowerampAPI.EXTRA_PAUSED)) {
                            pauseTimer()
                            remoteTrackTime!!.stopSongProgress()
                            false
                        } else {
                            startTimer()
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
                                // read saved position and apply them
                                val positionPreference = getSharedPreferences("position", MODE_PRIVATE)
                                val positionX = positionPreference.getInt("x", -1)
                                val positionY = positionPreference.getInt("y", -1)
                                mWindow!!.setOnClickListener {
                                    if (showingBg) {
                                        mWindow!!.background = ContextCompat.getDrawable(this, android.R.color.transparent)
                                        LrcWindow.closeButton?.visibility = View.INVISIBLE
                                        LrcWindow.lockButton?.visibility = View.INVISIBLE
                                        showingBg = false
                                    } else {
                                        opacity = PreferenceManager.getDefaultSharedPreferences(this).getString("opacity", "64")?.toIntOrNull()
                                        if (opacity != null) {
                                            if (opacity!! > 255)
                                                mWindow!!.background = ColorDrawable(Color.argb(255, 0, 0, 0))
                                            else if (opacity!! < 0)
                                                mWindow!!.background = ColorDrawable(Color.argb(0, 0, 0, 0))
                                            else
                                                mWindow!!.background = ColorDrawable(Color.argb(opacity!!, 0, 0, 0))
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
                                    LrcWindow.params?.apply {
                                        if (positionX > 0) {
                                            x = positionX
                                        }
                                        if (positionY > 0) {
                                            y = positionY
                                        }
                                        this.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                    }
                                    LrcWindow.window?.updateViewLayout(mWindow!!, LrcWindow.params)
                                    val lockNotification = NotificationCompat.Builder(this, "LOCK").apply {
                                        setSmallIcon(R.drawable.ic_lock)
                                        setContentTitle(resources.getString(R.string.notification_lock_title))
                                        setContentText(resources.getString(R.string.notification_lock_message))
                                        setContentIntent(PendingIntent.getService(this@LrcService, LrcWindow.REQUEST_UNLOCK, Intent(this@LrcService, LrcService::class.java).putExtra("request", LrcWindow.REQUEST_UNLOCK), PendingIntent.FLAG_CANCEL_CURRENT))
                                        priority = NotificationCompat.PRIORITY_MIN
                                        setOnlyAlertOnce(true)
                                        setOngoing(true)
                                        setAutoCancel(true)
                                    }.build()
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
                            timerOn = if (extras.getBoolean(PowerampAPI.EXTRA_PAUSED)) {
                                remoteTrackTime!!.stopSongProgress()
                                pauseTimer()
                                false
                            } else {
                                remoteTrackTime!!.startSongProgress()
                                startTimer()
                                true
                            }
                        }
                    }
                    LrcWindow.REQUEST_UNLOCK -> {
                        if (mWindow != null) {
                            LrcWindow.unlock(mWindow!!)
                        }
                    }
                    LrcWindow.REQUEST_INSTRUMENTAL -> {
                        PathsDatabaseHelper(this).setInstrumental(intent.getStringExtra("path"))
                        NotificationManagerCompat.from(this).cancel(210)
                        LrcWindow.reloadLyrics(false, this)
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
        // save window position
        val windowPositionX = LrcWindow.params?.x
        val windowPositionY = LrcWindow.params?.y
        getSharedPreferences("position", MODE_PRIVATE).edit().apply {
            putInt("x", windowPositionX ?: -1)
            putInt("y", windowPositionY ?: -1)
            apply()
        }

        remoteTrackTime!!.setTrackTimeListener(null)
        remoteTrackTime!!.unregister()
        stopForeground(true)
    }

    override fun onTrackPositionChanged(position: Int) {
        mCurrentPosition = position
        if (timerOn)
            restartTimer()
        else
            pauseTimer()
        LrcWindow.refreshTime(position, 0)
    }

    override fun onTrackDurationChanged(duration: Int) {

    }

    // precise timer (with an interval of 250 ms)
    private var offset = 0L
    private var timerHandler = Handler()
    private var timerRunnable: Runnable = object: Runnable {
        override fun run() {
            offset = if (offset < 1000) offset + 250 else 0
            LrcWindow.refreshTime(mCurrentPosition, offset)
            timerHandler.postDelayed(this, 250)
        }
    }

    private fun startTimer() {
        timerHandler.postDelayed(timerRunnable, 250)
    }

    private fun pauseTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun restartTimer() {
        pauseTimer()
        offset = 0L
        startTimer()
    }
}