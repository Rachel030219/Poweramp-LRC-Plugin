package net.rachel030219.poweramplrc

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi

class MainActivity : Activity() {
    private val REQUEST_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val builder = AlertDialog.Builder(this).setTitle(R.string.main_title).setPositiveButton(R.string.take_off) { _, _ -> init() }.setOnCancelListener {
            finish()
        }
        builder.setMessage(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) (resources.getString(R.string.q_message) + resources.getString(R.string.main_message)) else resources.getString(R.string.main_message))
        builder.create().show()
    }
    private fun init () {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = resources.getString(R.string.notification_channel_name)
            val descriptionText = resources.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel("ENTRANCE", name, importance)
            mChannel.description = descriptionText
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && !Settings.canDrawOverlays(this)) {
                Toast.makeText(applicationContext, R.string.main_hint, Toast.LENGTH_LONG).show()
                Handler().postDelayed({
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQUEST_PERMISSION
                    )
                }, 1000)
            } else {
                hide()
            }
        } else {
            hide()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse(
                    "package:$packageName"
                )
            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(permissionIntent)
            //hide()
    }

    private fun hide(){
        val p = packageManager
        val componentName = ComponentName(this, MainActivity::class.java)
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        finish()
    }
}
