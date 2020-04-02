package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.maxmpz.poweramp.player.PowerampAPI
import com.maxmpz.poweramp.player.RemoteTrackTime
import java.net.URLEncoder

class LrcService: Service(), RemoteTrackTime.TrackTimeListener {
    private var mWindow: View? = null
    private var timerOn = false
    private var mCurrentPosition = -1
    private var remoteTrackTime: RemoteTrackTime? = null
    private var mKeyMap = mutableMapOf<String, String>()

    companion object {
        val REQUEST_PATH = 10
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("InflateParams")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mWindow == null) {
            mWindow = LayoutInflater.from(this).inflate(R.layout.lrc_window, null)
        }
        if (intent!!.hasExtra("request")) {
            val extras = intent.extras
            var path = extras!!.getString(PowerampAPI.Track.PATH)
            if (!path!!.startsWith("/") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val key = path.substringBefore('/')
                path = path.replaceFirst("/", ":")
                if (!mKeyMap.containsKey(key)) {
                    val keyPref = getSharedPreferences(key, Context.MODE_PRIVATE)
                    if (keyPref.contains("path")) {
                        //TODO:check usability of saved path
                        val pathValue = keyPref.getString("path", key)!!
                        if (checkSAFUsability(pathValue)) {
                            extras.putString(PowerampAPI.Track.PATH, pathValue + URLEncoder.encode(path, "UTF-8").replace("+", "%20"))
                            extras.putBoolean("saf", true)
                            mKeyMap[key] = pathValue
                        } else {
                            startPermissionRequest(key)
                        }
                    } else {
                        startPermissionRequest(key)
                    }
                } else {
                    if (checkSAFUsability(mKeyMap.getValue(key))) {
                        extras.putString(PowerampAPI.Track.PATH, mKeyMap.getValue(key) + URLEncoder.encode(path, "UTF-8").replace("+", "%20"))
                        extras.putBoolean("saf", true)
                    } else {
                        startPermissionRequest(key)
                    }
                }
            }
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
                        stopForeground(false)
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
                                stopForeground(false)
                                LrcWindow.sendNotification(this, extras, false)
                            }
                        }
                        LrcWindow.refresh(mWindow!!, extras, true, this)
                        val notification = LrcWindow.sendNotification(this, extras, true)
                        startForeground(212, notification)
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

    private fun startPermissionRequest (key: String) {
        val pathIntent = Intent(this, PathActivity::class.java).putExtra("path_request", REQUEST_PATH).putExtra("key", key)
        pathIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_PATH,
            pathIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(this, "PATH").apply {
            setContentTitle(resources.getString(R.string.notification_path_title))
            setContentText(resources.getString(R.string.notification_path_message) + key)
            setSmallIcon(R.drawable.ic_notification)
            setAutoCancel(true)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setContentIntent(pendingIntent)
            setOnlyAlertOnce(true)
        }
        NotificationManagerCompat.from(this).notify(213, builder.build())
    }

    private fun checkSAFUsability (path: String): Boolean {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        return try {
            contentResolver.takePersistableUriPermission(Uri.parse(path.substringBeforeLast("/document/")), takeFlags)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}