package com.aura.what2eat.service

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth

/**
 * Centralized Firebase Analytics Service for What2Eat.
 * Tracks screen transitions, feature engagement frequency, and user properties.
 */
object AnalyticsService {

    private const val TAG = "AnalyticsService"

    private var analytics: FirebaseAnalytics? = null

    /**
     * Initializes Firebase Analytics instance.
     */
    fun init(context: Context) {
        try {
            analytics = FirebaseAnalytics.getInstance(context.applicationContext)
            Log.d(TAG, "Firebase Analytics initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Analytics", e)
        }
    }

    /**
     * Sets user properties in Firebase Analytics for audience segmentation.
     */
    fun setUserProperties(userId: String?, isPro: Boolean) {
        try {
            val uid = userId ?: FirebaseAuth.getInstance().currentUser?.uid
            analytics?.setUserId(uid)
            analytics?.setUserProperty("user_type", if (isPro) "pro_user" else "free_user")
            analytics?.setUserProperty("is_pro", isPro.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Error setting user properties", e)
        }
    }

    /**
     * Logs screen view event in Firebase Analytics (Reports -> Pages & screens).
     */
    fun logScreenView(screenName: String, screenClass: String = screenName) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
            analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            Log.d(TAG, "Analytics Screen View: $screenName")
        } catch (e: Exception) {
            Log.w(TAG, "Error logging screen view: $screenName", e)
        }
    }

    /**
     * Master feature usage event logger.
     * Logs `feature_used` with the feature name and any supplementary parameters.
     * Check Firebase Console -> Analytics -> Events -> `feature_used` to see which feature is used most.
     */
    fun logFeatureUsed(featureName: String, extraParams: Map<String, Any> = emptyMap()) {
        try {
            val bundle = Bundle().apply {
                putString("feature_name", featureName)
                putString("user_id", FirebaseAuth.getInstance().currentUser?.uid ?: "guest")
                extraParams.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putBoolean(key, value)
                    }
                }
            }
            analytics?.logEvent("feature_used", bundle)

            // Also log as a direct distinct event name (e.g. "meal_decider", "weekly_planner", "dine_out")
            // so it shows up as an individual row in Google Analytics top events table
            val directEventName = featureName
                .trim()
                .lowercase()
                .replace(Regex("[^a-z0-9_]"), "_")
                .take(40)
            if (directEventName.isNotBlank() && directEventName != "feature_used" && directEventName.first().isLetter()) {
                analytics?.logEvent(directEventName, bundle)
            }

            Log.d(TAG, "Analytics Feature Used: $featureName, params: $extraParams")
        } catch (e: Exception) {
            Log.w(TAG, "Error logging feature used: $featureName", e)
        }
    }

    // ── Pre-configured Feature Event Helpers ──

    fun logMealDeciderUsed(cuisine: String = "All", mealType: String = "Dinner", dishCount: Int = 1) {
        logFeatureUsed(
            featureName = "meal_decider",
            extraParams = mapOf(
                "cuisine" to cuisine,
                "meal_type" to mealType,
                "dish_count" to dishCount
            )
        )
    }

    fun logWeeklyPlannerViewed() {
        logFeatureUsed("weekly_planner")
    }

    fun logWeeklyPlannerRegenerated(mode: String) {
        logFeatureUsed("weekly_planner_regenerated", mapOf("mode" to mode))
    }

    fun logDineOutViewed(city: String, filter: String) {
        logFeatureUsed(
            featureName = "dine_out",
            extraParams = mapOf("city" to city, "filter" to filter)
        )
    }

    fun logPantryMatchUsed(ingredientsCount: Int) {
        logFeatureUsed(
            featureName = "pantry_matcher",
            extraParams = mapOf("ingredients_count" to ingredientsCount)
        )
    }

    fun logCommunityViewed() {
        logFeatureUsed("community_dishes")
    }

    fun logCommunityDishLiked(dishId: String, dishName: String) {
        logFeatureUsed(
            featureName = "community_dish_liked",
            extraParams = mapOf("dish_id" to dishId, "dish_name" to dishName)
        )
    }

    fun logRecipeViewed(recipeName: String) {
        logFeatureUsed(
            featureName = "recipe_detail",
            extraParams = mapOf("recipe_name" to recipeName)
        )
    }

    fun logGuestPlannerUsed(guestCount: Int, eventType: String) {
        logFeatureUsed(
            featureName = "guest_planner",
            extraParams = mapOf("guest_count" to guestCount, "event_type" to eventType)
        )
    }

    fun logSpecialOccasionUsed(occasionType: String) {
        logFeatureUsed(
            featureName = "special_occasion",
            extraParams = mapOf("occasion" to occasionType)
        )
    }

    fun logSubscriptionScreenViewed() {
        logFeatureUsed("subscription_screen")
    }

    fun logSubscriptionScreenView() {
        logSubscriptionScreenViewed()
    }
}
