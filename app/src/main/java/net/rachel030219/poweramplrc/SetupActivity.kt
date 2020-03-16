package net.rachel030219.poweramplrc

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener
import kotlinx.android.synthetic.main.activity_setup.*


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
        main_setup.setIllustrationAspectRatio(4f)
        main_setup.navigationBar.setNavigationBarListener(
            object : NavigationBarListener {
                override fun onNavigateBack() {
                    onBackPressed()
                }
                override fun onNavigateNext() {
                    // Create notification channel on Oreo and above
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
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        startActivity(Intent(this@SetupActivity, DoneActivity::class.java))
                        finish()
                    }
                    else {
                        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && Settings.canDrawOverlays(applicationContext)) {
                            startActivity(Intent(this@SetupActivity, DoneActivity::class.java))
                            finish()
                        }
                        else
                            startActivity(Intent(this@SetupActivity, PermissionActivity::class.java))
                    }
                }
            })
    }
}