package com.example.findmyphone

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.findmyphone.database.AppDatabase
import com.example.findmyphone.database.Phrase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RecordPhraseActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var btnRecord: Button
    private lateinit var etPhraseName: EditText
    private lateinit var tvStatus: TextView
    private var isRecording = false
    private val RECORD_DURATION_MS = 3000L // 3 seconds
    private val sampleRate = 16000
    private var audioRecord: AudioRecord? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_phrase)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "findmyphone.db").build()

        btnRecord = findViewById(R.id.btnRecordPhrase)
        etPhraseName = findViewById(R.id.etPhraseName)
        tvStatus = findViewById(R.id.tvRecordStatus)

        btnRecord.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 200)
        }
    }

    private fun startRecording() {
        if (etPhraseName.text.isBlank()) {
            Toast.makeText(this, "Please enter a name for the phrase", Toast.LENGTH_SHORT).show()
            return
        }

        tvStatus.text = "Recording... (3 seconds)"
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true
        btnRecord.text = "Stop Recording"

        // Record for duration
        val recordedShorts = mutableListOf<Short>()
        val buffer = ShortArray(bufferSize)

        Thread {
            val startTime = System.currentTimeMillis()
            while (isRecording && System.currentTimeMillis() - startTime < RECORD_DURATION_MS) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                for (i in 0 until read) {
                    recordedShorts.add(buffer[i])
                }
            }
            // Convert to float array
            val template = FloatArray(recordedShorts.size) { i ->
                recordedShorts[i].toFloat() / Short.MAX_VALUE
            }

            // Save to database
            lifecycleScope.launch(Dispatchers.IO) {
                val phrase = Phrase(
                    name = etPhraseName.text.toString(),
                    template = template,
                    isActive = true
                )
                db.phraseDao().insert(phrase)

                // Also deactivate any other phrases (simple approach: only one active phrase)
                val all = db.phraseDao().getAll()
                for (p in all) {
                    if (p.id != phrase.id && p.isActive) {
                        val inactive = p.copy(isActive = false)
                        db.phraseDao().update(inactive)
                    }
                }

                withContext(Dispatchers.Main) {
                    tvStatus.text = "Phrase saved!"
                    Toast.makeText(this@RecordPhraseActivity, "Phrase saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            // Clean up
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }.start()
    }

    private fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        btnRecord.text = "Record Phrase"
        tvStatus.text = "Recording stopped."
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
