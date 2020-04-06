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
import androidx.documentfile.provider.DocumentFile
import com.maxmpz.poweramp.player.PowerampAPI
import com.maxmpz.poweramp.player.RemoteTrackTime

class LrcService: Service(), RemoteTrackTime.TrackTimeListener {
    private var mWindow: View? = null
    private var timerOn = false
    private var mCurrentPosition = -1
    private var remoteTrackTime: RemoteTrackTime? = null
    private var mKeyMap = mutableMapOf<String, String>()
    private var mPathMap = mutableMapOf<String, String>()
    private var nowPlayingPath = ""

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
        if (intent!!.getBooleanExtra("foreground", false)) {
            val notificationIntent = Intent().apply {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("android.provider.extra.APP_PACKAGE", packageName)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_PATH,
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
            val extras = intent.extras
            val path = extras!!.getString(PowerampAPI.Track.PATH)
            if (!path!!.startsWith("/") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                extras.putBoolean("saf", true)
                val key = path.substringBefore('/')
                if (!mPathMap.containsKey(path)) {
                    if (!mKeyMap.containsKey(key)) {
                        val keyPref = getSharedPreferences("paths", Context.MODE_PRIVATE)
                        if (keyPref.contains(key)) {
                            val pathValue = keyPref.getString(key, key)!!
                            if (checkSAFUsability(pathValue)) {
                                val finalPath = findFile(path, pathValue)
                                if (finalPath != null) {
                                    extras.putString(PowerampAPI.Track.PATH, finalPath)
                                    extras.putBoolean("safFound", true)
                                    mPathMap[path] = finalPath
                                } else
                                    extras.putBoolean("safFound", false)
                                mKeyMap[key] = pathValue
                            } else {
                                startPermissionRequest(key)
                            }
                        } else {
                            startPermissionRequest(key)
                        }
                    } else {
                        if (checkSAFUsability(mKeyMap.getValue(key))) {
                            val finalPath = findFile(path, mKeyMap.getValue(key))
                            if (finalPath != null) {
                                extras.putString(PowerampAPI.Track.PATH, finalPath)
                                extras.putBoolean("safFound", true)
                                mPathMap[path] = finalPath
                            } else
                                extras.putBoolean("safFound", false)
                        } else {
                            startPermissionRequest(key)
                        }
                    }
                } else {
                    extras.putString(PowerampAPI.Track.PATH, mPathMap.getValue(path))
                    extras.putBoolean("safFound", true)
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
            contentResolver.takePersistableUriPermission(Uri.parse(path), takeFlags)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    private fun findFile (path: String, pathValue: String): String?{
        val treeFile = DocumentFile.fromTreeUri(this, Uri.parse(pathValue))
        var subTreeFile: DocumentFile? = null
        val folders = path.split("/")
        folders.forEach {
            if (treeFile?.findFile(it) != null) {
                subTreeFile = treeFile.findFile(it)
            } else {
                subTreeFile = subTreeFile?.findFile(it)
                if (subTreeFile != null) {
                    if (subTreeFile!!.isFile) {
                        return@findFile subTreeFile!!.uri.toString()
                    }
                }
            }
        }
        return null
    }
}