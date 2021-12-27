package it.ministerodellasalute.verificaC19.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.ministerodellasalute.verificaC19.WhiteLabelApplication

class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            (context.applicationContext as WhiteLabelApplication).startAppMaintenanceWorker(true)
        }
    }
}
