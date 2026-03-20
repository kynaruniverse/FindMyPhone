package com.example.findmyphone.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

class BillingManager(private val context: Context, private val updateListener: (Boolean) -> Unit) {
    private val billingClient = BillingClient.newBuilder(context)
        .setListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    if (purchase.sku == "premium_upgrade") {
                        updateListener(true)
                    }
                }
            }
        }
        .enablePendingPurchases()
        .build()

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry logic could be added
            }
        })
    }

    private fun queryPurchases() {
        val purchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
        for (purchase in purchases.purchasesList) {
            if (purchase.sku == "premium_upgrade") {
                updateListener(true)
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, sku: String) {
        val skuDetailsParams = SkuDetailsParams.newBuilder()
            .setSkusList(listOf(sku))
            .setType(BillingClient.SkuType.INAPP)
            .build()

        billingClient.querySkuDetailsAsync(skuDetailsParams) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                val skuDetails = skuDetailsList[0]
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails)
                    .build()
                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }

    fun close() {
        billingClient.endConnection()
    }
}
