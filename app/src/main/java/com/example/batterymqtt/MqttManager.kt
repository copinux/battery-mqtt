package com.example.batterymqtt

import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttManager(
    private val serverUri: String,
    private val clientId: String,
    private val username: String?,
    private val password: String?
) {
    private var client: MqttAsyncClient? = null

    @Synchronized
    fun connect() {
        if (client?.isConnected == true) return
        val c = MqttAsyncClient(serverUri, clientId, MemoryPersistence())
        val options = MqttConnectOptions().apply {
            isCleanSession = true
            isAutomaticReconnect = true
            connectionTimeout = 15
            keepAliveInterval = 60
            if (!username.isNullOrEmpty()) userName = username
            if (!password.isNullOrEmpty()) password = password.toCharArray()
        }
        c.connect(options).waitForCompletion(20_000)
        client = c
    }

    @Synchronized
    fun publish(topic: String, payload: String) {
        val c = client ?: throw IllegalStateException("Client MQTT non initialisé")
        if (!c.isConnected) throw IllegalStateException("Connexion MQTT perdue")
        val message = MqttMessage(payload.toByteArray()).apply {
            qos = 1
            isRetained = false
        }
        c.publish(topic, message).waitForCompletion(10_000)
    }

    @Synchronized
    fun disconnect() {
        runCatching { client?.disconnect()?.waitForCompletion(5_000) }
        runCatching { client?.close() }
        client = null
    }
}
