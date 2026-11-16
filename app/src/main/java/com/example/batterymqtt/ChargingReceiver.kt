package com.example.batterymqtt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class ChargingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = Prefs.get(context)
        if (!prefs.getBoolean(Prefs.KEY_AUTO_START, false)) return
        if (prefs.getString(Prefs.KEY_BROKER, "").isNullOrEmpty()) return

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, BatteryMqttService::class.java)
                )
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                context.stopService(Intent(context, BatteryMqttService::class.java))
            }
        }
    }
}
