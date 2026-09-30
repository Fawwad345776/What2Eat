package com.aura.what2eat.service

import android.content.Context
import android.util.Log
import com.aura.what2eat.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Centralized Firebase Crashlytics Service for What2Eat.
 * Handles crash reporting, non-fatal exception tracking, custom user keys, and breadcrumb logs.
 */
object CrashlyticsService {

    private const val TAG = "CrashlyticsService"

    /**
     * Initializes Crashlytics configuration.
     */
    fun init(context: Context) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()

            // Enable Crashlytics collection (enabled for all builds so issues are tracked)
            crashlytics.setCrashlyticsCollectionEnabled(true)

            // Set default environment keys
            crashlytics.setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
            crashlytics.setCustomKey("version_name", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)

            // Associate current user ID if logged in
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            if (!currentUid.isNullOrBlank()) {
                crashlytics.setUserId(currentUid)
            }

            Log.d(TAG, "Firebase Crashlytics initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Crashlytics", e)
        }
    }

    /**
     * Sets the user identifier for crash reports.
     */
    fun setUserId(userId: String?) {
        try {
            val uid = userId ?: FirebaseAuth.getInstance().currentUser?.uid ?: "guest_user"
            FirebaseCrashlytics.getInstance().setUserId(uid)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting Crashlytics user ID", e)
        }
    }

    /**
     * Sets custom key-value pairs for additional context during crashes.
     */
    fun setCustomKey(key: String, value: String) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting custom key: $key", e)
        }
    }

    fun setCustomKey(key: String, value: Boolean) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting custom key: $key", e)
        }
    }

    fun setCustomKey(key: String, value: Int) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting custom key: $key", e)
        }
    }

    /**
     * Updates user PRO status and country context in Crashlytics.
     */
    fun updateUserContext(isPro: Boolean, country: String? = null) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("is_pro", isPro)
            if (!country.isNullOrBlank()) {
                crashlytics.setCustomKey("country", country)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating user context in Crashlytics", e)
        }
    }

    /**
     * Adds breadcrumb log message in Crashlytics report.
     */
    fun log(message: String) {
        try {
            FirebaseCrashlytics.getInstance().log(message)
            Log.d(TAG, "Crashlytics Log: $message")
        } catch (e: Exception) {
            Log.w(TAG, "Error logging to Crashlytics", e)
        }
    }

    /**
     * Records non-fatal exceptions to Firebase Crashlytics dashboard.
     */
    fun recordException(throwable: Throwable, contextMessage: String? = null) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            if (!contextMessage.isNullOrBlank()) {
                crashlytics.log("Exception context: $contextMessage")
            }
            crashlytics.recordException(throwable)
            Log.e(TAG, "Non-fatal exception recorded to Crashlytics: ${throwable.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Error recording exception to Crashlytics", e)
        }
    }
}
