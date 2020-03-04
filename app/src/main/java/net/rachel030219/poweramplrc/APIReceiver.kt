package net.rachel030219.poweramplrc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
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
                    refreshWindow(context, bundle)
                    if (LrcWindow.displaying) {
                        LrcWindow.sendNotification(context, bundle, true)
                    } else {
                        LrcWindow.sendNotification(context, bundle, false)
                    }
                }

                PowerampAPI.ACTION_TRACK_CHANGED_EXPLICIT -> {
                    val statusIntent = context!!.registerReceiver(null, IntentFilter(PowerampAPI.ACTION_STATUS_CHANGED))
                    val bundle = intent.getBundleExtra(PowerampAPI.TRACK)
                    bundle!!.putInt(PowerampAPI.Track.POSITION, statusIntent!!.getIntExtra(PowerampAPI.Track.POSITION, -1))
                    bundle.putBoolean(PowerampAPI.PAUSED, statusIntent.getBooleanExtra(PowerampAPI.PAUSED, true))
                    if (LrcWindow.displaying) {
                        LrcWindow.sendNotification(context, bundle, true)
                        refreshWindow(context, bundle)
                    } else {
                        LrcWindow.sendNotification(context, bundle, false)
                    }
                    android.util.Log.d("DEBUG-LRC-TRACK", "track bundle: \n" + LrcWindow.dumpBundle(bundle))
                    android.util.Log.d("DEBUG-LRC-TRACK", "original status bundle: \n" + LrcWindow.dumpBundle(statusIntent.extras))
                }
            }
        }
    }

    private fun refreshWindow (context: Context?, bundle: Bundle?) {
        val intents = Intent(context, LrcService::class.java).putExtra("request", LrcWindow.REQUEST_UPDATE).putExtras(bundle!!)
        context!!.startService(intents)
    }
}