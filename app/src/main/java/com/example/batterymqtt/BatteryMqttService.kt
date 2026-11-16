package com.example.batterymqtt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BatteryMqttService : Service() {

    companion object {
        const val CHANNEL_ID = "battery_mqtt_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STATUS = "com.example.batterymqtt.STATUS"
        const val EXTRA_STATUS = "status"
        const val INTERVAL_MS = 60_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null
    private var mqtt: MqttManager? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.status_starting)))
        loopJob?.cancel()
        loopJob = scope.launch { runLoop() }
        return START_STICKY
    }

    override fun onDestroy() {
        loopJob?.cancel()
        scope.cancel()
        mqtt?.disconnect()
        mqtt = null
        super.onDestroy()
    }

    private suspend fun runLoop() {
        val prefs = Prefs.get(this)
        val broker = prefs.getString(Prefs.KEY_BROKER, "")?.trim().orEmpty()
        val port = prefs.getString(Prefs.KEY_PORT, "1883")?.trim().orEmpty().ifEmpty { "1883" }
        val topic = prefs.getString(Prefs.KEY_TOPIC, "battery/level")?.trim().orEmpty()
            .ifEmpty { "battery/level" }
        val user = prefs.getString(Prefs.KEY_USER, null)
        val password = prefs.getString(Prefs.KEY_PASSWORD, null)
        val serverUri = if (broker.startsWith("tcp://") || broker.startsWith("ssl://")) {
            broker
        } else {
            "tcp://$broker:$port"
        }

        try {
            sendStatus(getString(R.string.status_connecting, serverUri))
            mqtt = MqttManager(serverUri, "battery-mqtt-${System.currentTimeMillis()}", user, password)
                .also { it.connect() }
            sendStatus(getString(R.string.status_connected, serverUri))

            while (true) {
                val level = readBatteryLevel()
                if (level in 0..100) {
                    mqtt?.publish(topic, level.toString())
                    val time = timeFormat.format(Date())
                    val text = getString(R.string.status_published, level, time)
                    updateNotification(text)
                    sendStatus(text)
                } else {
                    sendStatus(getString(R.string.status_read_error))
                }
                delay(INTERVAL_MS)
            }
        } catch (t: Throwable) {
            sendStatus(getString(R.string.status_error, t.message ?: t.javaClass.simpleName))
            stopSelf()
        }
    }

    private fun readBatteryLevel(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val capacity = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (capacity in 0..100) return capacity

        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) level * 100 / scale else -1
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_battery_foreground)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun sendStatus(text: String) {
        val intent = Intent(ACTION_STATUS).apply { setPackage(packageName); putExtra(EXTRA_STATUS, text) }
        sendBroadcast(intent)
    }
}
