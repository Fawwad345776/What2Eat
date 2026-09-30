package com.aura.what2eat

import android.app.Application
import com.aura.what2eat.service.LocalFallbackService
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp

class What2EatApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
        } catch (_: Exception) {}

        // 2. Initialize Mobile Ads SDK
        try {
            MobileAds.initialize(this) {}
        } catch (_: Exception) {}

        // 3. Initialize 1000+ dishes offline database from assets/dishes.json
        LocalFallbackService.init(this)

        // 4. Initialize Local AI persistent storage
        com.aura.what2eat.service.LocalAiStorageService.init(this)
    }
}
