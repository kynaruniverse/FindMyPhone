package com.example.findmyphone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.findmyphone.database.AppDatabase
import com.example.findmyphone.database.Phrase
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.android.billingclient.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adView: AdView
    private lateinit var billingClient: BillingClient
    private var isPremium = false
    private val PREMIUM_SKU = "premium_upgrade" // Define your SKU in Google Play Console

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize database
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "findmyphone.db"
        ).build()

        // Initialize Ads (test)
        MobileAds.initialize(this) {}
        adView = findViewById(R.id.adView)
        loadBannerAd()

        // Initialize Billing
        setupBilling()

        // Check premium status
        checkPremiumStatus()

        // Request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }

        // Buttons
        findViewById<Button>(R.id.btnRecord).setOnClickListener { startRecording() }
        findViewById<Button>(R.id.btnStartService).setOnClickListener { startAudioService() }
        findViewById<Button>(R.id.btnStopService).setOnClickListener { stopAudioService() }
        findViewById<Button>(R.id.btnUpgrade).setOnClickListener { startPurchase() }
    }

    private fun loadBannerAd() {
        if (!isPremium) {
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        } else {
            adView.visibility = android.view.View.GONE
        }
    }

    private fun setupBilling() {
        billingClient = BillingClient.newBuilder(this)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        if (purchase.sku == PREMIUM_SKU) {
                            isPremium = true
                            savePremiumStatus(true)
                            adView.visibility = android.view.View.GONE
                            Toast.makeText(this, "Thank you for upgrading!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Query existing purchases
                    val purchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
                    for (purchase in purchases.purchasesList) {
                        if (purchase.sku == PREMIUM_SKU) {
                            isPremium = true
                            savePremiumStatus(true)
                            runOnUiThread { adView.visibility = android.view.View.GONE }
                        }
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to reconnect
            }
        })
    }

    private fun savePremiumStatus(premium: Boolean) {
        getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("premium", premium).apply()
    }

    private fun checkPremiumStatus() {
        isPremium = getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("premium", false)
        if (isPremium) {
            adView.visibility = android.view.View.GONE
        }
    }

    private fun startPurchase() {
        if (isPremium) {
            Toast.makeText(this, "Already premium!", Toast.LENGTH_SHORT).show()
            return
        }

        val skuDetailsParams = SkuDetailsParams.newBuilder()
            .setSkusList(listOf(PREMIUM_SKU))
            .setType(BillingClient.SkuType.INAPP)
            .build()

        billingClient.querySkuDetailsAsync(skuDetailsParams) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                val skuDetails = skuDetailsList[0]
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails)
                    .build()
                billingClient.launchBillingFlow(this@MainActivity, billingFlowParams)
            } else {
                Toast.makeText(this@MainActivity, "Failed to load purchase details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRecording() {
        // Start recording activity/fragment to capture a new phrase
        // For simplicity, we'll just show a toast and trigger a new activity
        // In a real app, you'd implement a proper recording UI
        Toast.makeText(this, "Recording feature coming soon", Toast.LENGTH_SHORT).show()
        // You can add code to record audio, convert to template, and save to database
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
