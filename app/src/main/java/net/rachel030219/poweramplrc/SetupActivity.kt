package net.rachel030219.poweramplrc

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener
import kotlinx.android.synthetic.main.activity_setup.*
import java.io.File


class SetupActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            main_old.visibility = View.VISIBLE
        }

        // Initialize SetupWizardLayout
        main_setup.setHeaderText(R.string.app_name)
        main_setup.setIllustration(getDrawable(R.drawable.suw_layout_background))
        main_setup.setIllustrationAspectRatio(2.5f)
        main_setup.navigationBar.setNavigationBarListener(
            object : NavigationBarListener {
                override fun onNavigateBack() {
                    onBackPressed()
                }
                override fun onNavigateNext() {
                    // Create notification channel on Oreo and above
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val mEntranceChannel = NotificationChannel("ENTRANCE", resources.getString(R.string.notification_entrance_channel_name), NotificationManager.IMPORTANCE_LOW)
                        val mPathChannel = NotificationChannel("PATH", resources.getString(R.string.notification_path_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
                        val notificationManager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.createNotificationChannel(mEntranceChannel)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            notificationManager.createNotificationChannel(mPathChannel)
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        startActivity(Intent(this@SetupActivity, DoneActivity::class.java))
                        finish()
                    }
                    else {
                        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && Settings.canDrawOverlays(applicationContext)) {
                            startActivity(Intent(this@SetupActivity, DoneActivity::class.java))
                            finish()
                        }
                        else {
                            startActivity(Intent(this@SetupActivity, PermissionActivity::class.java))
                            finish()
                        }
                    }
                }
            })
    }
}