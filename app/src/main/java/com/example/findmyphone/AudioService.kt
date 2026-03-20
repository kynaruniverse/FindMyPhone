package com.example.findmyphone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.room.Room
import com.example.findmyphone.database.AppDatabase
import kotlinx.coroutines.*

class AudioService : Service() {
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "audio_service_channel"
    private var isListening = true
    private lateinit var db: AppDatabase
    private var mediaPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dtwMatcher = DTWMatcher()
    private val TAG = "AudioService"

    private var THRESHOLD = 150.0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "findmyphone.db").build()

        serviceScope.launch {
            try {
                val allPhrases = db.phraseDao().getAll()
                showToast("Total phrases: ${allPhrases.size}")

                if (allPhrases.isNotEmpty()) {
                    showToast("Active: ${allPhrases.count { it.isActive }}")
                }

                val templates = loadTemplates()
                if (templates.isNotEmpty()) {
                    val (id, template) = templates[0]
                    showToast("Loaded template with ${template.size} samples")
                    Log.d(TAG, "Loaded template with ${template.size} samples")
                    startListening(template)
                } else {
                    showToast("No active template found")
                    sendDebug("No template saved")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error: ${e.message}")
            }
        }
    }

    private suspend fun loadTemplates(): List<Pair<Int, FloatArray>> {
        val phrases = db.phraseDao().getAllActive()
        return phrases.map { it.id to it.template }
    }

    private fun startListening(template: FloatArray) {
        try {
            showToast("Starting audio recorder...")
            val audioRecorder = AudioRecorder()
            val buffer = mutableListOf<Float>()
            val templateSize = template.size

            showToast("Listening...")
            sendDebug("Listening...")

            audioRecorder.startListening { chunk ->
                if (!isListening) return@startListening

                buffer.addAll(chunk.toList())

                while (buffer.size >= templateSize) {
                    val segment = buffer.take(templateSize).toFloatArray()
                    val distance = dtwMatcher.similarity(template, segment)
                    // Show distance occasionally to avoid spam
                    if (System.currentTimeMillis() % 1000 < 100) {
                        showToast("DTW: $distance")
                    }
                    sendDebug("DTW distance = $distance")

                    if (distance < THRESHOLD) {
                        showToast("MATCH!")
                        triggerAlarm()
                        isListening = false
                        audioRecorder.stopListening()
                        return@startListening
                    }

                    val shift = (templateSize / 3).coerceAtLeast(1)
                    repeat(shift) { if (buffer.isNotEmpty()) buffer.removeAt(0) }
                }
            }

            while (isListening) {
                Thread.sleep(1000)
            }
            audioRecorder.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Listening error: ${e.message}")
        }
    }

    private fun triggerAlarm() {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer.create(this, alarmUri)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

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

    private fun showToast(message: String) {
        // Must run on main thread
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendDebug(message: String) {
        val intent = Intent("AudioServiceDebug").apply {
            putExtra("message", message)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
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
