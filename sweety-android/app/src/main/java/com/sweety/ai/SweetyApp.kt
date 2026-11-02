package com.sweety.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SweetyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sweety AI Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Sweety AI ready to respond to your 2-clap or voice wake-up."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "sweety_ai_service_channel"
        lateinit var instance: SweetyApp
            private set
    }
}

