package net.rachel030219.poweramplrc

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View

import com.maxmpz.poweramp.player.PowerampAPI
import com.maxmpz.poweramp.player.RemoteTrackTime

class LrcService: Service(), RemoteTrackTime.TrackTimeListener {
    var window: View? = null
    var remoteTrackTime: RemoteTrackTime? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (window == null) {
            window = LayoutInflater.from(this).inflate(R.layout.lrc_window, null)
        }
        if (intent!!.hasExtra("request")) {
            when (intent.getIntExtra("request", 0)) {
                LrcWindow.REQUEST_WINDOW -> {
                    val extras = intent.extras
                    if (LrcWindow.displaying) {
                        LrcWindow.destroy(window!!)
                        LrcWindow.sendNotification(this, extras, false)
                    } else {
                        LrcWindow.sendNotification(this, extras, true)
                        LrcWindow.initialize(this, window!!)
                        LrcWindow.refresh(window!!, extras!!)
                        remoteTrackTime!!.updateTrackPosition(extras.getInt(PowerampAPI.Track.POSITION))
                        if (extras.getBoolean(PowerampAPI.PAUSED)) {
                            remoteTrackTime!!.stopSongProgress()
                        } else {
                            remoteTrackTime!!.startSongProgress()
                        }
                    }
                }
                LrcWindow.REQUEST_UPDATE -> {
                    val extras = intent.extras
                    if (window != null) {
                        LrcWindow.refresh(window!!, extras!!)
                        remoteTrackTime!!.updateTrackPosition(extras.getInt(PowerampAPI.Track.POSITION))
                        remoteTrackTime!!.updateTrackDuration(extras.getInt(PowerampAPI.Track.DURATION))
                        if (extras.getBoolean(PowerampAPI.PAUSED)) {
                            remoteTrackTime!!.stopSongProgress()
                        } else {
                            remoteTrackTime!!.startSongProgress()
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
    }

    override fun onTrackPositionChanged(position: Int) {
        LrcWindow.refreshTime(position, window!!)
    }

    override fun onTrackDurationChanged(duration: Int) {

    }
}