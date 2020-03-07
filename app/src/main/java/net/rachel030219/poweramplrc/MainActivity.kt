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

class MainActivity : Activity() {
    private val REQUEST_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val builder = AlertDialog.Builder(this).setTitle(R.string.main_title).setMessage(R.string.main_message).setPositiveButton(R.string.take_off) { _, _ -> init() }.setOnCancelListener {
            finish()
        }
        builder.create().show()
    }
    private fun init () {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Toast.makeText(this, R.string.main_hint, Toast.LENGTH_LONG).show()
            Handler().postDelayed({
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQUEST_PERMISSION
                    )
                }
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
            }, 1000)
        } else {
            finish()
            hide()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val permissionIntent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse(
                        "package:$packageName"
                    )
                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(permissionIntent)
                hide()
            }
        }
    }

    private fun hide(){
        val p = packageManager
        val componentName = ComponentName(this, MainActivity::class.java)
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        finish()
    }
}
