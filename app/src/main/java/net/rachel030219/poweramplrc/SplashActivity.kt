package net.rachel030219.poweramplrc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager

class SplashActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("permission", false)) {
            startActivity(Intent(this, ConfigurationActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
        }
    }
}