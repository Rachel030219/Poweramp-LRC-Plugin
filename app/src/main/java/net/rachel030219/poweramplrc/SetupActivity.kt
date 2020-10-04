package net.rachel030219.poweramplrc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener
import kotlinx.android.synthetic.main.activity_setup.*

class SetupActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("permission", false)) {
            showGoToConfig()
        }

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
                        val mEntranceChannel = NotificationChannel("ENTRANCE", resources.getString(R.string.notification_entrance_channel_name), NotificationManager.IMPORTANCE_LOW)
                        val mPlaceholderChannel = NotificationChannel("PLACEHOLDER", resources.getString(R.string.notification_placeholder_channel_name), NotificationManager.IMPORTANCE_LOW)
                        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
                            createNotificationChannel(mEntranceChannel)
                            createNotificationChannel(mPlaceholderChannel)
                        }
                    }
                    if (PreferenceManager.getDefaultSharedPreferences(this@SetupActivity).getBoolean("permission", false)) {
                        startActivity(Intent(this@SetupActivity, ConfigurationActivity::class.java))
                        finish()
                    }
                    else {
                        startActivity(Intent(this@SetupActivity, PermissionActivity::class.java))
                        finish()
                    }
                }
            })
    }
    private fun showGoToConfig() {
        main_configuration.visibility = View.VISIBLE
    }
}