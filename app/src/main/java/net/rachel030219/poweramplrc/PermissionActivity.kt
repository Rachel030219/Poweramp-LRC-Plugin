package net.rachel030219.poweramplrc

import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.android.setupwizardlib.view.NavigationBar
import com.maxmpz.poweramp.player.PowerampAPI
import kotlinx.android.synthetic.main.activity_permissions.*

@RequiresApi(Build.VERSION_CODES.M)
class PermissionActivity: AppCompatActivity() {
    private var floating = false
    private var floating_asked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        // Initialize SetupWizardLayout
        permission_setup.setIllustration(ResourcesCompat.getDrawable(resources, R.drawable.suw_layout_background, theme))
        permission_setup.setIllustrationAspectRatio(2.5f)
        permission_setup.navigationBar.setNavigationBarListener(
            object : NavigationBar.NavigationBarListener {
                override fun onNavigateBack() {
                    onBackPressed()
                }

                override fun onNavigateNext() {
                    if (floating) {
                        PreferenceManager.getDefaultSharedPreferences(this@PermissionActivity).edit().putBoolean("permissionGranted", true).apply()
                        startActivity(Intent(this@PermissionActivity, DoneActivity::class.java))
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
                                    LrcWindow.sendNotification(this@PermissionActivity, it, true)
                                else
                                    LrcWindow.sendNotification(this@PermissionActivity, it, false)

                                val intents = Intent(this@PermissionActivity, LrcService::class.java).putExtra("request", LrcWindow.REQUEST_UPDATE).putExtras(it)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                    startForegroundService(intents.apply {
                                        putExtra("foreground", true)
                                    })
                                else
                                    startService(intents)
                            }
                        }
                        finish()
                    }
                }
            })
        // Request permissions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            permission_floating.visibility = View.GONE
            floating = true
        }
        permission_floating_check.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                floating_asked = true
                val permissionIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(permissionIntent)
            } else {
                permission_floating_text.setText(R.string.permission_floating_granted)
                permission_floating_text.setTextColor(getColor(R.color.text_normal))
                permission_floating_check.visibility = View.GONE
                floating = true
            }
        }
        permission_folder_check.setOnClickListener {
            startActivity(Intent(this, FoldersActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (floating_asked) {
            floating_asked = false
            if (Settings.canDrawOverlays(this)) {
                permission_floating_text.setText(R.string.permission_floating_granted)
                permission_floating_text.setTextColor(getColor(R.color.text_normal))
                permission_floating_check.visibility = View.GONE
                floating = true
            } else {
                permission_floating_text.setText(R.string.permission_floating_denied)
                permission_floating_text.setTextColor(getColor(R.color.text_failure))
                floating = false
            }
        }
    }
}