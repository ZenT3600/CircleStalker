package it.matteoleggio.osustalker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import it.matteoleggio.osustalker.Logic.Helpers.*

class StartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Intent(context, ScoreService::class.java).also {
                it.action = Actions.START.name
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(it)
                    return
                }
                context.startService(it)
            }
        }
    }
}
