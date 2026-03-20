package com.example.findmyphone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.example.findmyphone.database.AppDatabase
import com.example.findmyphone.database.Phrase
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream

class AudioService : Service() {
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "audio_service_channel"
    private var isListening = true
    private lateinit var db: AppDatabase
    private var mediaPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "findmyphone.db").build()

        serviceScope.launch {
            val templates = loadTemplates()
            startListening(templates)
        }
    }

    private suspend fun loadTemplates(): List<Pair<Int, FloatArray>> {
        val phrases = db.phraseDao().getAllActive()
        return phrases.map { it.id to it.template }
    }

    private fun startListening(templates: List<Pair<Int, FloatArray>>) {
        // Simplified: In a real app, you'd implement AudioRecord loop and DTW matching
        // Here we'll just simulate detection for demonstration
        // For actual implementation, use AudioRecord and compare chunks
        // Placeholder: after 10 seconds, trigger alarm
        Thread.sleep(10000)
        if (isListening) {
            triggerAlarm()
        }
    }

    private fun triggerAlarm() {
        // Play loud sound
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        // Vibrate
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(5000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(5000)
        }

        // Stop after 30 seconds (optional)
        serviceScope.launch {
            delay(30000)
            stopAlarm()
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Listening for your trigger phrase"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Find My Phone")
            .setContentText("Listening for your phrase...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isListening = false
        serviceScope.cancel()
        stopAlarm()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
