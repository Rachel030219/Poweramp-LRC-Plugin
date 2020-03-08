package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View

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

    @SuppressLint("InflateParams")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mWindow == null) {
            mWindow = LayoutInflater.from(this).inflate(R.layout.lrc_window, null)
        }
        if (intent!!.hasExtra("request")) {
            when (intent.getIntExtra("request", 0)) {
                LrcWindow.REQUEST_WINDOW -> {
                    val extras = intent.extras
                    timerOn = if (extras!!.getBoolean(PowerampAPI.PAUSED)) {
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
                        LrcWindow.initialize(this, mWindow!!)
                        if (timerOn) {
                            extras.putInt(PowerampAPI.Track.POSITION, mCurrentPosition)
                        } else {
                            remoteTrackTime!!.updateTrackPosition(extras.getInt(PowerampAPI.Track.POSITION))
                        }
                        LrcWindow.refresh(mWindow!!, extras, true)
                        LrcWindow.sendNotification(this, extras, true)
                    }
                }
                LrcWindow.REQUEST_UPDATE -> {
                    val extras = intent.extras
                    if (mWindow != null) {
                        LrcWindow.refresh(mWindow!!, extras!!, false)
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
        mCurrentPosition = position
        LrcWindow.refreshTime(position, mWindow!!)
    }

    override fun onTrackDurationChanged(duration: Int) {

    }
}