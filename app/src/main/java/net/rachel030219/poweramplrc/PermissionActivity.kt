package net.rachel030219.poweramplrc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.setupwizardlib.view.NavigationBar
import kotlinx.android.synthetic.main.activity_permissions.*

@RequiresApi(Build.VERSION_CODES.M)
class PermissionActivity: AppCompatActivity() {
    private var storage = false
    private var floating = false
    private var floating_asked = false
    private val REQUEST_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        // Initialize SetupWizardLayout
        permission_setup.setIllustration(getDrawable(R.drawable.suw_layout_background))
        permission_setup.setIllustrationAspectRatio(4f)
        permission_setup.navigationBar.setNavigationBarListener(
            object : NavigationBar.NavigationBarListener {
                override fun onNavigateBack() {
                    onBackPressed()
                }

                override fun onNavigateNext() {
                    if (storage && floating) {
                        startActivity(Intent(this@PermissionActivity, DoneActivity::class.java))
                        finish()
                    }
                }
            })
        // Request permissions
        permission_storage_check.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION)
            }
        }
        permission_floating_check.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                floating_asked = true
                val permissionIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(permissionIntent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (floating_asked) {
            floating_asked = false
            if (Settings.canDrawOverlays(this)) {
                permission_floating_text.setText(R.string.permission_floating_granted)
                permission_floating_check.visibility = View.GONE
                floating = true
            } else {
                permission_floating_text.setText(R.string.permission_floating_denied)
                permission_floating_text.setTextColor(getColor(R.color.text_failure))
                floating = false
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permission_storage_text.setText(R.string.permission_storage_granted)
                permission_storage_check.visibility = View.GONE
                storage = true
            } else {
                permission_storage_text.setText(R.string.permission_storage_denied)
                permission_storage_text.setTextColor(getColor(R.color.text_failure))
                storage = false
            }
        }
    }
}