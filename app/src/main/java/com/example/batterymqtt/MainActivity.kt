package com.example.batterymqtt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var brokerInput: EditText
    private lateinit var portInput: EditText
    private lateinit var topicInput: EditText
    private lateinit var userInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var autoStartCheck: android.widget.CheckBox
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            statusText.text = intent.getStringExtra(BatteryMqttService.EXTRA_STATUS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        brokerInput = findViewById(R.id.brokerInput)
        portInput = findViewById(R.id.portInput)
        topicInput = findViewById(R.id.topicInput)
        userInput = findViewById(R.id.userInput)
        passwordInput = findViewById(R.id.passwordInput)
        autoStartCheck = findViewById(R.id.autoStartCheck)
        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        loadPrefs()

        startButton.setOnClickListener { startPublishing() }
        stopButton.setOnClickListener { stopPublishing() }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            statusReceiver,
            IntentFilter(BatteryMqttService.ACTION_STATUS),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(statusReceiver)
    }

    private fun loadPrefs() {
        val prefs = Prefs.get(this)
        brokerInput.setText(prefs.getString(Prefs.KEY_BROKER, ""))
        portInput.setText(prefs.getString(Prefs.KEY_PORT, "1883"))
        topicInput.setText(prefs.getString(Prefs.KEY_TOPIC, "battery/level"))
        userInput.setText(prefs.getString(Prefs.KEY_USER, ""))
        passwordInput.setText(prefs.getString(Prefs.KEY_PASSWORD, ""))
        autoStartCheck.isChecked = prefs.getBoolean(Prefs.KEY_AUTO_START, false)
    }

    private fun startPublishing() {
        val broker = brokerInput.text.toString().trim()
        if (broker.isEmpty()) {
            Toast.makeText(this, R.string.error_broker_required, Toast.LENGTH_SHORT).show()
            return
        }

        Prefs.get(this).edit()
            .putString(Prefs.KEY_BROKER, broker)
            .putString(Prefs.KEY_PORT, portInput.text.toString().trim().ifEmpty { "1883" })
            .putString(Prefs.KEY_TOPIC, topicInput.text.toString().trim().ifEmpty { "battery/level" })
            .putString(Prefs.KEY_USER, userInput.text.toString().trim())
            .putString(Prefs.KEY_PASSWORD, passwordInput.text.toString())
            .putBoolean(Prefs.KEY_AUTO_START, autoStartCheck.isChecked)
            .apply()

        statusText.text = getString(R.string.status_starting)
        val intent = Intent(this, BatteryMqttService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopPublishing() {
        stopService(Intent(this, BatteryMqttService::class.java))
        statusText.text = getString(R.string.status_stopped)
    }
}
