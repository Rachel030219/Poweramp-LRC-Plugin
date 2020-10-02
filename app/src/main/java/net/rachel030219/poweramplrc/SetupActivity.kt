package net.rachel030219.poweramplrc

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener
import com.maxmpz.poweramp.player.PowerampAPI
import kotlinx.android.synthetic.main.activity_setup.*

class SetupActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            main_old.visibility = View.VISIBLE
            showGoToConfig()
        } else if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("permissionGranted", false)) { // checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && Settings.canDrawOverlays(applicationContext)
            showGoToConfig()

            // send notification
            val statusIntent = registerReceiver(null, IntentFilter(PowerampAPI.ACTION_STATUS_CHANGED))
            val trackIntent = registerReceiver(null, IntentFilter(PowerampAPI.ACTION_TRACK_CHANGED))
            if (statusIntent != null && trackIntent != null) {
                val bundle = trackIntent.getBundleExtra(PowerampAPI.TRACK)?.apply {
                    putBoolean(PowerampAPI.PAUSED, statusIntent.getBooleanExtra(PowerampAPI.PAUSED, true))
                    putInt(PowerampAPI.Track.POSITION, statusIntent.getIntExtra(PowerampAPI.Track.POSITION, -1))
                }
                bundle?.also {
                    if (LrcWindow.displaying)
                        LrcWindow.sendNotification(this, it, true)
                    else
                        LrcWindow.sendNotification(this, it, false)

                    val intents = Intent(this, LrcService::class.java).putExtra("request", LrcWindow.REQUEST_UPDATE).putExtras(it)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        startForegroundService(intents.apply { putExtra("foreground", true) })
                    else
                        startService(intents)
                }
            }
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
                        val mPathChannel = NotificationChannel("PATH", resources.getString(R.string.notification_path_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
                        val mPlaceholderChannel = NotificationChannel("PLACEHOLDER", resources.getString(R.string.notification_placeholder_channel_name), NotificationManager.IMPORTANCE_LOW)
                        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
                            createNotificationChannel(mEntranceChannel)
                            createNotificationChannel(mPlaceholderChannel)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                                createNotificationChannel(mPathChannel)
                        }
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        startActivity(Intent(this@SetupActivity, DoneActivity::class.java))
                        finish()
                    }
                    else {
                        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && Settings.canDrawOverlays(applicationContext)) {
                            startActivity(Intent(this@SetupActivity, ConfigurationActivity::class.java))
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
    private fun showGoToConfig() {
        main_configuration.visibility = View.VISIBLE
    }
}