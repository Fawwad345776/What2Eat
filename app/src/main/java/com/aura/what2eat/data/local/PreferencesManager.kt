package com.aura.what2eat.data.local

import android.content.Context
import android.content.SharedPreferences
import com.aura.what2eat.model.CookedHistoryEntry
import com.aura.what2eat.model.UserPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "what2eat_prefs"

        // Keys
        private const val KEY_USER_PREFERENCES = "user_preferences_json"
        private const val KEY_SKIPS_REMAINING = "skips_remaining"
        private const val KEY_LAST_SKIP_RESET_DAY = "last_skip_reset_day"
        private const val KEY_RECIPE_VIEWS_USED = "recipe_views_used"
        private const val KEY_LAST_RECIPE_RESET_MONTH = "last_recipe_reset_month"
        private const val KEY_PANTRY_SEARCHES_USED = "pantry_searches_used"
        private const val KEY_LAST_PANTRY_RESET_MONTH = "last_pantry_reset_month"
        private const val KEY_LOCAL_COOKED_HISTORY = "local_cooked_history_json"
        private const val KEY_FIRST_LAUNCH_COMPLETED = "first_launch_completed"
        private const val KEY_ANTI_REPETITION_DAYS = "anti_repetition_days"
        private const val KEY_LIKED_DISH_IDS = "liked_dish_ids_set"
        private const val KEY_LOCAL_WEEKLY_PLAN = "local_weekly_plan_json"
        private const val KEY_DAY_PREFERENCES = "key_day_meal_preferences_json"
        private const val KEY_DINEOUT_ROTATION_INDEX = "key_dineout_rotation_index"
        private const val KEY_DINEOUT_DAILY_DISH = "dineout_daily_dish"
        private const val KEY_DINEOUT_DAILY_DATE = "dineout_daily_date"
        private const val KEY_DINEOUT_DAILY_SKIPS_USED = "dineout_daily_skips_used"
        private const val KEY_DINEOUT_SKIPS_DATE = "dineout_skips_date"
        private const val KEY_HIDDEN_RESTAURANT_IDS = "hidden_restaurant_ids_set"
        private const val KEY_LIKED_RESTAURANT_IDS = "liked_restaurant_ids_set"
        private const val KEY_PERMANENT_USER_ID = "permanent_user_id"

        private const val KEY_OCCASION_SKIPS_REMAINING = "occasion_skips_remaining"
        private const val KEY_LAST_OCCASION_SKIP_RESET_DAY = "last_occasion_skip_reset_day"
        private const val KEY_LAST_SUGGESTION_ADDED_DATE = "last_suggestion_added_date"
        private const val KEY_DAILY_SUGGESTIONS_COUNT = "key_daily_suggestions_count"
        private const val KEY_DAILY_RECIPE_GEN_COUNT = "key_daily_recipe_gen_count"

        // Defaults & Limits (Free vs Pro Tier)
        const val DEFAULT_DAILY_SKIPS = 3
        const val DEFAULT_OCCASION_DAILY_SKIPS = 3
        const val DINEOUT_FREE_SKIPS_LIMIT = 3
        const val FREE_RECIPE_VIEW_LIMIT = 3
        const val PRO_RECIPE_VIEW_LIMIT = 10
        const val FREE_PANTRY_SEARCH_LIMIT = 3
        const val PRO_PANTRY_SEARCH_LIMIT = 9999
        const val FREE_DAILY_SUGGESTIONS_LIMIT = 3
        const val PRO_DAILY_SUGGESTIONS_LIMIT = 10

        // Legacy compatibility aliases
        const val MONTHLY_RECIPE_VIEW_LIMIT = FREE_RECIPE_VIEW_LIMIT
        const val MONTHLY_PANTRY_SEARCH_LIMIT = FREE_PANTRY_SEARCH_LIMIT

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // -------------------------------------------------------------
    // 1. User Preferences
    // -------------------------------------------------------------
    fun saveUserPreferences(userPrefs: UserPreferences) {
        val json = gson.toJson(userPrefs)
        prefs.edit().putString(KEY_USER_PREFERENCES, json).apply()
    }

    fun loadUserPreferences(): UserPreferences {
        val json = prefs.getString(KEY_USER_PREFERENCES, null) ?: return UserPreferences()
        return try {
            gson.fromJson(json, UserPreferences::class.java) ?: UserPreferences()
        } catch (e: Exception) {
            UserPreferences()
        }
    }

    // -------------------------------------------------------------
    // 2. Daily Skips (AI Meal Suggestions - 3 Per Day)
    // -------------------------------------------------------------
    private fun checkDailyReset() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastReset = prefs.getString(KEY_LAST_SKIP_RESET_DAY, "")

        if (lastReset != today) {
            resetDailySkips()
        }
    }

    fun resetDailySkips() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit()
            .putInt(KEY_SKIPS_REMAINING, DEFAULT_DAILY_SKIPS)
            .putString(KEY_LAST_SKIP_RESET_DAY, today)
            .apply()
    }

    fun getSkipsRemaining(): Int {
        checkDailyReset()
        val current = prefs.getInt(KEY_SKIPS_REMAINING, DEFAULT_DAILY_SKIPS)
        return current.coerceAtMost(DEFAULT_DAILY_SKIPS)
    }

    fun useSkip(): Int {
        checkDailyReset()
        val current = getSkipsRemaining()
        val remaining = (current - 1).coerceAtLeast(0)
        prefs.edit().putInt(KEY_SKIPS_REMAINING, remaining).apply()
        return remaining
    }

    fun grantExtraSkips(count: Int) {
        val current = getSkipsRemaining()
        prefs.edit().putInt(KEY_SKIPS_REMAINING, current + count).apply()
    }

    // -------------------------------------------------------------
    // 2b. Occasion Skips (Guest & Special Occasion - 3 Per Day)
    // -------------------------------------------------------------
    private fun checkOccasionDailyReset() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastReset = prefs.getString(KEY_LAST_OCCASION_SKIP_RESET_DAY, "")

        if (lastReset != today) {
            prefs.edit()
                .putInt(KEY_OCCASION_SKIPS_REMAINING, DEFAULT_OCCASION_DAILY_SKIPS)
                .putString(KEY_LAST_OCCASION_SKIP_RESET_DAY, today)
                .apply()
        }
    }

    fun getOccasionSkipsRemaining(): Int {
        checkOccasionDailyReset()
        return prefs.getInt(KEY_OCCASION_SKIPS_REMAINING, DEFAULT_OCCASION_DAILY_SKIPS)
    }

    fun useOccasionSkip(): Int {
        checkOccasionDailyReset()
        val current = getOccasionSkipsRemaining()
        val remaining = (current - 1).coerceAtLeast(0)
        prefs.edit().putInt(KEY_OCCASION_SKIPS_REMAINING, remaining).apply()
        return remaining
    }

    fun grantExtraOccasionSkips(count: Int = 3) {
        val current = getOccasionSkipsRemaining()
        prefs.edit().putInt(KEY_OCCASION_SKIPS_REMAINING, current + count).apply()
    }

    // -------------------------------------------------------------
    // 2c. Add Suggestion & Recipe Generation (3 Free, 10 Pro Per Day Limit)
    // -------------------------------------------------------------
    private fun checkDailySuggestionsReset() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString(KEY_LAST_SUGGESTION_ADDED_DATE, "")
        if (lastDate != today) {
            prefs.edit()
                .putString(KEY_LAST_SUGGESTION_ADDED_DATE, today)
                .putInt(KEY_DAILY_SUGGESTIONS_COUNT, 0)
                .putInt(KEY_DAILY_RECIPE_GEN_COUNT, 0)
                .apply()
        }
    }

    fun getDailySuggestionCount(): Int {
        checkDailySuggestionsReset()
        return prefs.getInt(KEY_DAILY_SUGGESTIONS_COUNT, 0)
    }

    fun getDailyRecipeGenerationCount(): Int {
        checkDailySuggestionsReset()
        return prefs.getInt(KEY_DAILY_RECIPE_GEN_COUNT, 0)
    }

    fun getDailySuggestionLimit(isPro: Boolean = false): Int {
        if (com.aura.what2eat.BuildConfig.DEBUG || isPro) return 999
        return FREE_DAILY_SUGGESTIONS_LIMIT
    }

    fun canAddSuggestionToday(isPro: Boolean = false): Boolean {
        if (com.aura.what2eat.BuildConfig.DEBUG || isPro) return true
        checkDailySuggestionsReset()
        val count = getDailySuggestionCount()
        val limit = getDailySuggestionLimit(isPro)
        return count < limit
    }

    fun getDailyRecipeGenerationsRemaining(isPro: Boolean = false): Int {
        if (com.aura.what2eat.BuildConfig.DEBUG || isPro) return 999
        checkDailySuggestionsReset()
        val count = getDailyRecipeGenerationCount()
        return (FREE_DAILY_SUGGESTIONS_LIMIT - count).coerceAtLeast(0)
    }

    /**
     * User can generate up to 3 recipes per day in free version.
     * In debug mode (BuildConfig.DEBUG) or for PRO subscribers, unlimited generations are permitted.
     */
    fun canGenerateRecipeToday(isPro: Boolean = false): Boolean {
        if (com.aura.what2eat.BuildConfig.DEBUG || isPro) return true
        checkDailySuggestionsReset()
        val genCount = getDailyRecipeGenerationCount()
        return genCount < FREE_DAILY_SUGGESTIONS_LIMIT
    }

    fun recordSuggestionAddedToday() {
        checkDailySuggestionsReset()
        val count = getDailySuggestionCount()
        prefs.edit().putInt(KEY_DAILY_SUGGESTIONS_COUNT, count + 1).apply()
    }

    fun recordRecipeGeneratedToday() {
        checkDailySuggestionsReset()
        val count = getDailyRecipeGenerationCount()
        prefs.edit().putInt(KEY_DAILY_RECIPE_GEN_COUNT, count + 1).apply()
    }

    // -------------------------------------------------------------
    // 3. Daily Recipe Views (3 Free, 10 Pro)
    // -------------------------------------------------------------
    private fun checkDailyRecipeReset() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastReset = prefs.getString(KEY_LAST_RECIPE_RESET_MONTH, "")

        if (lastReset != today) {
            prefs.edit()
                .putInt(KEY_RECIPE_VIEWS_USED, 0)
                .putString(KEY_LAST_RECIPE_RESET_MONTH, today)
                .apply()
        }
    }

    fun getDailyRecipeViews(): Int {
        checkDailyRecipeReset()
        return prefs.getInt(KEY_RECIPE_VIEWS_USED, 0)
    }

    // Legacy alias
    fun getMonthlyRecipeViews(): Int = getDailyRecipeViews()

    fun getRecipeViewLimit(isPro: Boolean = false): Int {
        return if (isPro) PRO_RECIPE_VIEW_LIMIT else FREE_RECIPE_VIEW_LIMIT
    }

    fun useRecipeView(isPro: Boolean = false): Boolean {
        checkDailyRecipeReset()
        val used = getDailyRecipeViews()
        val limit = getRecipeViewLimit(isPro)
        if (used >= limit) {
            return false // Limit reached
        }
        prefs.edit().putInt(KEY_RECIPE_VIEWS_USED, used + 1).apply()
        return true
    }

    // -------------------------------------------------------------
    // 4. Daily Pantry Searches (3 Free, Unlimited Pro)
    // -------------------------------------------------------------
    private fun checkDailyPantryReset() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastReset = prefs.getString(KEY_LAST_PANTRY_RESET_MONTH, "")

        if (lastReset != today) {
            prefs.edit()
                .putInt(KEY_PANTRY_SEARCHES_USED, 0)
                .putString(KEY_LAST_PANTRY_RESET_MONTH, today)
                .apply()
        }
    }

    fun getDailyPantrySearches(): Int {
        checkDailyPantryReset()
        return prefs.getInt(KEY_PANTRY_SEARCHES_USED, 0)
    }

    // Legacy alias
    fun getMonthlyPantrySearches(): Int = getDailyPantrySearches()

    fun getPantrySearchLimit(isPro: Boolean = false): Int {
        return if (isPro) PRO_PANTRY_SEARCH_LIMIT else FREE_PANTRY_SEARCH_LIMIT
    }

    fun usePantrySearch(isPro: Boolean = false): Boolean {
        if (isPro) return true
        checkDailyPantryReset()
        val used = getDailyPantrySearches()
        if (used >= FREE_PANTRY_SEARCH_LIMIT) {
            return false // Free limit reached
        }
        prefs.edit().putInt(KEY_PANTRY_SEARCHES_USED, used + 1).apply()
        return true
    }

    fun grantExtraPantrySearch() {
        val used = getDailyPantrySearches()
        val updated = (used - 1).coerceAtLeast(0)
        prefs.edit().putInt(KEY_PANTRY_SEARCHES_USED, updated).apply()
    }

    // -------------------------------------------------------------
    // 5. First Launch & Setup
    // -------------------------------------------------------------
    fun isFirstLaunchCompleted(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH_COMPLETED, false)
    }

    fun setFirstLaunchCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_COMPLETED, completed).apply()
    }

    // -------------------------------------------------------------
    // 6. Anti-Repetition Days Settings (30, 15, 7, 0)
    // -------------------------------------------------------------
    fun getAntiRepetitionDays(): Int {
        return prefs.getInt(KEY_ANTI_REPETITION_DAYS, 30)
    }

    fun setAntiRepetitionDays(days: Int) {
        prefs.edit().putInt(KEY_ANTI_REPETITION_DAYS, days).apply()
    }

    // -------------------------------------------------------------
    // 7. Local Cooked History (Offline & Instant)
    // -------------------------------------------------------------
    fun saveCookedHistoryLocally(entry: CookedHistoryEntry) {
        val currentList = getLocalCookedHistory().toMutableList()
        currentList.add(0, entry) // Add to top

        // Keep last 100 entries locally
        val trimmed = if (currentList.size > 100) currentList.take(100) else currentList
        val json = gson.toJson(trimmed)
        prefs.edit().putString(KEY_LOCAL_COOKED_HISTORY, json).apply()
    }

    fun getLocalCookedHistory(): List<CookedHistoryEntry> {
        val json = prefs.getString(KEY_LOCAL_COOKED_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CookedHistoryEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getDishesCookedInWindow(): List<String> {
        val days = getAntiRepetitionDays()
        if (days <= 0) return emptyList() // Repetition allowed

        val cutoffMillis = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        return getLocalCookedHistory()
            .filter { it.cookedDate >= cutoffMillis }
            .map { it.dishName }
            .distinct()
    }

    fun clearLocalCookedHistory() {
        prefs.edit().remove(KEY_LOCAL_COOKED_HISTORY).apply()
    }

    // -------------------------------------------------------------
    // 8. Liked Dishes Tracking
    // -------------------------------------------------------------
    fun getLikedDishIds(): Set<String> {
        return prefs.getStringSet(KEY_LIKED_DISH_IDS, emptySet()) ?: emptySet()
    }

    fun isDishLiked(dishId: String): Boolean {
        return getLikedDishIds().contains(dishId)
    }

    fun toggleLikedDish(dishId: String): Boolean {
        val currentSet = getLikedDishIds().toMutableSet()
        val isLikedNow = if (currentSet.contains(dishId)) {
            currentSet.remove(dishId)
            false
        } else {
            currentSet.add(dishId)
            true
        }
        prefs.edit().putStringSet(KEY_LIKED_DISH_IDS, currentSet).apply()
        return isLikedNow
    }

    // -------------------------------------------------------------
    // 9. Local Weekly Meal Plan Persistence
    // -------------------------------------------------------------
    fun saveWeeklyPlanLocally(plan: com.aura.what2eat.model.WeeklyPlan) {
        val json = gson.toJson(plan)
        prefs.edit().putString(KEY_LOCAL_WEEKLY_PLAN, json).apply()
    }

    fun getLocalWeeklyPlan(): com.aura.what2eat.model.WeeklyPlan? {
        val json = prefs.getString(KEY_LOCAL_WEEKLY_PLAN, null) ?: return null
        return try {
            gson.fromJson(json, com.aura.what2eat.model.WeeklyPlan::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // -------------------------------------------------------------
    // 10. Day-by-Day Meal Preferences
    // -------------------------------------------------------------
    fun saveDayPreferences(preferences: Map<String, String>) {
        val json = gson.toJson(preferences)
        prefs.edit().putString(KEY_DAY_PREFERENCES, json).apply()
    }

    fun getDayPreferences(): Map<String, String> {
        val json = prefs.getString(KEY_DAY_PREFERENCES, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // -------------------------------------------------------------
    // 11. Dine Out Rotation Counter
    // -------------------------------------------------------------
    fun getDineOutRotationIndex(): Int {
        return prefs.getInt(KEY_DINEOUT_ROTATION_INDEX, 0)
    }

    fun getAndIncrementDineOutRotationIndex(): Int {
        val current = getDineOutRotationIndex()
        prefs.edit().putInt(KEY_DINEOUT_ROTATION_INDEX, current + 1).apply()
        return current
    }

    // -------------------------------------------------------------
    // 12. Dine Out Daily Recommendation & 3-Skips Limit
    // -------------------------------------------------------------
    fun getDineOutDailyDish(defaultProvider: () -> String): String {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val savedDate = prefs.getString(KEY_DINEOUT_DAILY_DATE, "")
        val savedDish = prefs.getString(KEY_DINEOUT_DAILY_DISH, "")

        return if (savedDate == today && !savedDish.isNullOrBlank()) {
            savedDish
        } else {
            val newDish = defaultProvider()
            prefs.edit()
                .putString(KEY_DINEOUT_DAILY_DATE, today)
                .putString(KEY_DINEOUT_DAILY_DISH, newDish)
                .apply()
            newDish
        }
    }

    fun setDineOutDailyDish(dish: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit()
            .putString(KEY_DINEOUT_DAILY_DATE, today)
            .putString(KEY_DINEOUT_DAILY_DISH, dish)
            .apply()
    }

    fun getDineOutSkipsRemaining(isPro: Boolean = false): Int {
        if (com.aura.what2eat.BuildConfig.DEBUG || isPro) return 999
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString(KEY_DINEOUT_SKIPS_DATE, "")
        if (lastDate != today) {
            prefs.edit()
                .putString(KEY_DINEOUT_SKIPS_DATE, today)
                .putInt(KEY_DINEOUT_DAILY_SKIPS_USED, 0)
                .apply()
            return DINEOUT_FREE_SKIPS_LIMIT
        }
        val used = prefs.getInt(KEY_DINEOUT_DAILY_SKIPS_USED, 0)
        return (DINEOUT_FREE_SKIPS_LIMIT - used).coerceAtLeast(0)
    }

    fun consumeDineOutSkip(isPro: Boolean = false): Boolean {
        if (com.aura.what2eat.BuildConfig.DEBUG || isPro) return true
        val remaining = getDineOutSkipsRemaining(isPro)
        if (remaining <= 0) return false
        val used = prefs.getInt(KEY_DINEOUT_DAILY_SKIPS_USED, 0)
        prefs.edit().putInt(KEY_DINEOUT_DAILY_SKIPS_USED, used + 1).apply()
        return true
    }

    // -------------------------------------------------------------
    // 13. Hidden / Not Interested Restaurants
    // -------------------------------------------------------------
    fun getHiddenRestaurantIds(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN_RESTAURANT_IDS, emptySet()) ?: emptySet()
    }

    fun hideRestaurant(restaurantId: String) {
        val current = getHiddenRestaurantIds().toMutableSet()
        current.add(restaurantId)
        prefs.edit().putStringSet(KEY_HIDDEN_RESTAURANT_IDS, current).apply()
    }

    fun unhideRestaurant(restaurantId: String) {
        val current = getHiddenRestaurantIds().toMutableSet()
        current.remove(restaurantId)
        prefs.edit().putStringSet(KEY_HIDDEN_RESTAURANT_IDS, current).apply()
    }

    // -------------------------------------------------------------
    // 14. Liked / Favorite Restaurants
    // -------------------------------------------------------------
    fun getLikedRestaurantIds(): Set<String> {
        return prefs.getStringSet(KEY_LIKED_RESTAURANT_IDS, emptySet()) ?: emptySet()
    }

    fun toggleLikedRestaurant(restaurantId: String): Boolean {
        val current = getLikedRestaurantIds().toMutableSet()
        val isNowLiked = if (current.contains(restaurantId)) {
            current.remove(restaurantId)
            false
        } else {
            current.add(restaurantId)
            true
        }
        prefs.edit().putStringSet(KEY_LIKED_RESTAURANT_IDS, current).apply()
        return isNowLiked
    }

    fun isRestaurantLiked(restaurantId: String): Boolean {
        return getLikedRestaurantIds().contains(restaurantId)
    }

    // -------------------------------------------------------------
    // Unique Installation / Device User ID
    // -------------------------------------------------------------
    fun getOrCreateUserId(): String {
        val existing = prefs.getString(KEY_PERMANENT_USER_ID, null)
        if (!existing.isNullOrBlank() && existing != "guest_user") {
            return existing
        }
        val newId = "usr_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16)
        prefs.edit().putString(KEY_PERMANENT_USER_ID, newId).apply()
        return newId
    }

    fun setPermanentUserId(id: String) {
        if (id.isNotBlank() && id != "guest_user") {
            prefs.edit().putString(KEY_PERMANENT_USER_ID, id).apply()
        }
    }
}
