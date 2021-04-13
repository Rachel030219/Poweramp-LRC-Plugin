package net.rachel030219.poweramplrc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.res.ResourcesCompat
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener
import kotlinx.android.synthetic.main.activity_setup.*

class SetupActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        // Initialize SetupWizardLayout
        main_setup.setHeaderText(R.string.app_name)
        main_setup.setIllustration(ResourcesCompat.getDrawable(resources, R.drawable.suw_layout_background, theme))
        main_setup.setIllustrationAspectRatio(2.5f)
        main_setup.navigationBar.setNavigationBarListener(
            object : NavigationBarListener {
                override fun onNavigateBack() {
                    onBackPressed()
                }
                override fun onNavigateNext() {
                    // Create notification channel on Oreo and above
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val mEntranceChannel = NotificationChannel("ENTRANCE", resources.getString(R.string.notification_entrance_channel_name), NotificationManager.IMPORTANCE_HIGH)
                        val mPlaceholderChannel = NotificationChannel("PLACEHOLDER", resources.getString(R.string.notification_placeholder_channel_name), NotificationManager.IMPORTANCE_LOW)
                        val mLockChannel = NotificationChannel("LOCK", resources.getString(R.string.notification_lock_channel_name), NotificationManager.IMPORTANCE_LOW)
                        NotificationManagerCompat.from(this@SetupActivity).apply {
                            createNotificationChannel(mEntranceChannel)
                            createNotificationChannel(mPlaceholderChannel)
                            createNotificationChannel(mLockChannel)
                        }
                    }
                    startActivity(Intent(this@SetupActivity, PermissionActivity::class.java))
                    finish()
                }
            })
    }
}