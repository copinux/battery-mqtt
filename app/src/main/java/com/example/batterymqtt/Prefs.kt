package com.example.batterymqtt

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    const val NAME = "battery_mqtt"
    const val KEY_BROKER = "broker"
    const val KEY_PORT = "port"
    const val KEY_TOPIC = "topic"
    const val KEY_USER = "user"
    const val KEY_PASSWORD = "password"
    const val KEY_AUTO_START = "auto_start"

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}
