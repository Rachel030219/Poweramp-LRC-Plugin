package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.maxmpz.poweramp.player.PowerampAPI
import com.maxmpz.poweramp.player.RemoteTrackTime

class LrcService: Service(), RemoteTrackTime.TrackTimeListener {
    private var mWindow: View? = null
    private var timerOn = false
    private var mCurrentPosition = -1
    private var remoteTrackTime: RemoteTrackTime? = null
    private var mKeyMap = mutableMapOf<String, String>()
    private var mPathMap = mutableMapOf<String, String>()

    companion object {
        val REQUEST_PATH = 10
    }

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
            val path = extras?.getString(PowerampAPI.Track.PATH)
            val key = path!!.substringBefore('/')
            // Path process
            if (!path.startsWith("/")) {
                extras.putBoolean("saf", true)
                // Attempt to read path from cache
                if (!mPathMap.containsKey(path)) {
                    // Attempt to read corresponding path for key from cache
                    if (!mKeyMap.containsKey(key)) {
                        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("legacy", false)){
                            extras.putBoolean("legacy", true)
                            val finalPath = path.replace(key, Environment.getExternalStorageDirectory().toString())
                            extras.putString(PowerampAPI.Track.PATH, finalPath)
                            mPathMap[path] = finalPath
                        } else {
                            val keyPref = getSharedPreferences("paths", Context.MODE_PRIVATE)
                            // 检测是否已经存入了 key 对应的 content URI
                            if (keyPref.contains(key)) {
                                // 若已存入则直接读取存储值
                                val pathValue = keyPref.getString(key, key)!!
                                // 检测可用性
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
            // End of path process
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
        var treeFile = DocumentFile.fromTreeUri(this, Uri.parse(pathValue))
        var file : DocumentFile? = null
        val folders = extractAndReplaceExt(path).split("/")
        folders.forEach {
            val subTreeFile = treeFile?.findFile(it)
            // 如果能找到名字对应的文件
            if (subTreeFile != null) {
                // 如果是文件夹，下次循环时步进
                if (subTreeFile.isDirectory) {
                    treeFile = subTreeFile
                }
                // 如果是文件，直接对应为该文件
                else if (subTreeFile.isFile) {
                    file = subTreeFile
                }
            }
        }
        if (file != null) {
            return file!!.uri.toString()
        }
        return null
    }

    private fun extractAndReplaceExt (oldString: String): String {
        return StringBuilder(oldString).substring(0, oldString.lastIndexOf('.')) + ".lrc"
    }
}