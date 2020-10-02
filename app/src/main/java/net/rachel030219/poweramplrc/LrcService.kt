package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
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
                            if (!LrcWindow.initialized) {
                                LrcWindow.initialize(this, mWindow!!)
                                mWindow!!.findViewById<Button>(R.id.close).setOnClickListener {
                                    LrcWindow.destroy(mWindow!!)
                                    LrcWindow.sendNotification(this, extras, false)
                                }
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