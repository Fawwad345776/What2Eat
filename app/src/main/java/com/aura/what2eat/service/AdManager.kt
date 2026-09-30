package com.aura.what2eat.service

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.aura.what2eat.BuildConfig
import com.aura.what2eat.billing.BillingManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {

    private const val TAG = "AdManager"
    // Standard Google AdMob test interstitial ad unit ID
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    /**
     * Preloads an interstitial ad in the background.
     */
    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isLoading) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "Interstitial ad preloaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.w(TAG, "Failed to load interstitial ad: ${error.message}")
                }
            }
        )
    }

    /**
     * Displays an interstitial ad if available and eligible.
     * In DEBUG mode or for PRO users, ads are bypassed gracefully.
     */
    fun showInterstitialAd(activity: Activity, onDismissed: () -> Unit = {}) {
        // 1. Check if user is PRO
        if (BillingManager.getInstance(activity).isProActive()) {
            onDismissed()
            return
        }

        // 2. Except in DEBUG mode (bypasses ad with notice, consistent with app pattern)
        if (BuildConfig.DEBUG) {
            Toast.makeText(activity, "Debug Mode: Ad bypassed 🎉", Toast.LENGTH_SHORT).show()
            onDismissed()
            return
        }

        // 3. Show Ad if loaded, otherwise preload for next time
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onDismissed()
                }
            }
            ad.show(activity)
        } else {
            loadInterstitialAd(activity)
            onDismissed()
        }
    }
}
