package net.rachel030219.poweramplrc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import com.maxmpz.poweramp.player.PowerampAPI

class APIReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action != null) {
            when (intent.action) {
                PowerampAPI.ACTION_STATUS_CHANGED_EXPLICIT -> {
                    val trackIntent = context!!.registerReceiver(null, IntentFilter(PowerampAPI.ACTION_TRACK_CHANGED))
                    val bundle = trackIntent!!.getBundleExtra(PowerampAPI.TRACK)
                    bundle!!.putInt(PowerampAPI.Track.POSITION, intent.getIntExtra(PowerampAPI.Track.POSITION, -1))
                    bundle.putBoolean(PowerampAPI.PAUSED, intent.getBooleanExtra(PowerampAPI.PAUSED, true))
                    if (LrcWindow.displaying) {
                        LrcWindow.sendNotification(context, bundle, true)
                    } else {
                        LrcWindow.sendNotification(context, bundle, false)
                    }
                    refreshWindow(context, bundle)
                }

                PowerampAPI.ACTION_TRACK_CHANGED_EXPLICIT -> {
                    val statusIntent = context!!.registerReceiver(null, IntentFilter(PowerampAPI.ACTION_STATUS_CHANGED))
                    val bundle = intent.getBundleExtra(PowerampAPI.TRACK)
                    bundle!!.putInt(PowerampAPI.Track.POSITION, 0)
                    bundle.putBoolean(PowerampAPI.PAUSED, statusIntent!!.getBooleanExtra(PowerampAPI.PAUSED, true))
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

    private fun refreshWindow (context: Context?, bundle: Bundle?) {
        val intents = Intent(context, LrcService::class.java).putExtra("request", LrcWindow.REQUEST_UPDATE).putExtras(bundle!!)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context!!.startForegroundService(intents.apply { putExtra("foreground", true) })
        else
            context!!.startService(intents)
    }
}