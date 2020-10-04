package net.rachel030219.poweramplrc

import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.maxmpz.poweramp.player.PowerampAPI

class ConfigurationActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)

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
}