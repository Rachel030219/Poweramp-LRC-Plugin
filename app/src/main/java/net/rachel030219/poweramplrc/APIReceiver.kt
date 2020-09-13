package net.rachel030219.poweramplrc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import com.maxmpz.poweramp.player.PowerampAPI

class APIReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null) {
            when (intent.action) {
                PowerampAPI.ACTION_STATUS_CHANGED_EXPLICIT -> {
                    context.registerReceiver(null, IntentFilter(PowerampAPI.ACTION_TRACK_CHANGED))?.let { trackIntent ->
                        trackIntent.getBundleExtra(PowerampAPI.TRACK)?.let { bundle ->
                            bundle.putInt(PowerampAPI.Track.POSITION, intent.getIntExtra(PowerampAPI.Track.POSITION, -1))
                            bundle.putBoolean(PowerampAPI.PAUSED, intent.getBooleanExtra(PowerampAPI.PAUSED, true))
                            if (LrcWindow.displaying) {
                                LrcWindow.sendNotification(context, bundle, true)
                            } else {
                                LrcWindow.sendNotification(context, bundle, false)
                            }
                            refreshWindow(context, bundle)
                        }
                    }
                }

                PowerampAPI.ACTION_TRACK_CHANGED_EXPLICIT -> {
                    context.registerReceiver(null, IntentFilter(PowerampAPI.ACTION_STATUS_CHANGED))?.let { statusIntent ->
                        intent.getBundleExtra(PowerampAPI.TRACK)?.let { bundle ->
                            bundle.putInt(PowerampAPI.Track.POSITION, 0)
                            bundle.putBoolean(PowerampAPI.PAUSED, statusIntent.getBooleanExtra(PowerampAPI.PAUSED, true))
                            if (LrcWindow.displaying) {
                                LrcWindow.sendNotification(context, bundle, true)
                            } else {
                                LrcWindow.sendNotification(context, bundle, false)
                            }
                            refreshWindow(context, bundle)
                        }
                    }
                }
            }
        }
    }

    private fun refreshWindow (context: Context?, bundle: Bundle?) {
        val intents = Intent(context, LrcService::class.java).putExtra("request", LrcWindow.REQUEST_UPDATE).putExtras(bundle!!)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context!!.startForegroundService(intents.apply { putExtra("foreground", true) })
        else
            context!!.startService(intents)
    }
}