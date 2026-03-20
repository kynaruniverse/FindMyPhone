package com.example.findmyphone

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.room.Room
import com.example.findmyphone.database.AppDatabase
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adView: AdView
    private lateinit var tvDebug: TextView

    private val debugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra("message") ?: return
            tvDebug.text = message
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "findmyphone.db"
        ).build()

        MobileAds.initialize(this) {}
        adView = findViewById(R.id.adView)
        loadBannerAd()

        tvDebug = findViewById(R.id.tvDebug)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }

        findViewById<Button>(R.id.btnRecord).setOnClickListener {
            startActivity(Intent(this, RecordPhraseActivity::class.java))
        }
        findViewById<Button>(R.id.btnStartService).setOnClickListener { startAudioService() }
        findViewById<Button>(R.id.btnStopService).setOnClickListener { stopAudioService() }
        findViewById<Button>(R.id.btnUpgrade).visibility = android.view.View.GONE

        // Register local broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(debugReceiver, IntentFilter("AudioServiceDebug"))
    }

    private fun loadBannerAd() {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun startAudioService() {
        val intent = Intent(this, AudioService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Audio service started", Toast.LENGTH_SHORT).show()
    }

    private fun stopAudioService() {
        val intent = Intent(this, AudioService::class.java)
        stopService(intent)
        Toast.makeText(this, "Audio service stopped", Toast.LENGTH_SHORT).show()
        tvDebug.text = "Service stopped"
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(debugReceiver)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }
}
