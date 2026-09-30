package com.aura.what2eat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.*
import com.aura.what2eat.service.ContextService
import com.aura.what2eat.service.FirebaseService
import com.aura.what2eat.service.GeminiAIService
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class WeeklyPlannerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager.getInstance(application)

    private val _weeklyPlan = MutableStateFlow<WeeklyPlan?>(null)
    val weeklyPlan: StateFlow<WeeklyPlan?> = _weeklyPlan.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<WeeklyPlan>>(UiState.Idle)
    val uiState: StateFlow<UiState<WeeklyPlan>> = _uiState.asStateFlow()

    private val _dayPreferences = MutableStateFlow<Map<String, String>>(emptyMap())
    val dayPreferences: StateFlow<Map<String, String>> = _dayPreferences.asStateFlow()

    init {
        _dayPreferences.value = prefsManager.getDayPreferences()
        loadWeeklyPlan()
    }

    companion object {
        val ORDERED_DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        val DEFAULT_WEEK_DAYS = listOf(
            DayPlan("Monday", tag = "Routine", lunch = Dish(name = "Tadka Moong Daal with Rice"), dinner = Dish(name = "Afghani Pulao with Mint Raita")),
            DayPlan("Tuesday", tag = "Routine", lunch = Dish(name = "Aloo Baingan with Roti"), dinner = Dish(name = "Chicken Karahi")),
            DayPlan("Wednesday", tag = "Routine", lunch = Dish(name = "Chana Daal Fry"), dinner = Dish(name = "Aloo Gosht Shorba")),
            DayPlan("Thursday", tag = "Routine", lunch = Dish(name = "Mix Vegetable Sabzi"), dinner = Dish(name = "Beef Shami Kabab & Paratha")),
            DayPlan("Friday", tag = "Friday Feast", isSpecialDay = true, lunch = Dish(name = "Karachi Chicken Biryani"), dinner = Dish(name = "Mutton Shinwari Karahi")),
            DayPlan("Saturday", tag = "Guest Day", isGuestDay = true, lunch = Dish(name = "Mutton Yakhni Pulao"), dinner = Dish(name = "Peshawari Chapli Kabab")),
            DayPlan("Sunday", tag = "Special", isSpecialDay = true, lunch = Dish(name = "Nihari with Roghani Naan"), dinner = Dish(name = "Chicken Tikka Boti with Paratha"))
        )

        val LUNCH_SUGGESTION_POOL = listOf(
            "Tadka Moong Daal with Rice",
            "Aloo Baingan with Roti",
            "Chana Daal Fry",
            "Mix Vegetable Sabzi",
            "Karachi Chicken Biryani",
            "Mutton Yakhni Pulao",
            "Nihari with Roghani Naan",
            "Karhi Pakora with Zeera Rice",
            "Daal Chawal with Kachumber",
            "Bhindi Masala with Phulka",
            "Matar Pulao with Raita",
            "Aloo Zeera with Paratha",
            "Lobio Daal (Kidney Beans) with Rice",
            "Chicken Haleem with Lemon"
        )

        val DINNER_SUGGESTION_POOL = listOf(
            "Afghani Pulao with Mint Raita",
            "Chicken Karahi",
            "Aloo Gosht Shorba",
            "Beef Shami Kabab & Paratha",
            "Mutton Shinwari Karahi",
            "Peshawari Chapli Kabab",
            "Chicken Tikka Boti with Paratha",
            "Chicken Handi with Naan",
            "Mutton Korma with Taftan",
            "Beef Seekh Kabab with Mint Chutney",
            "Chicken White Karahi",
            "Daal Gosht with Zeera Rice",
            "Palak Paneer with Butter Naan",
            "Fish Tikka with Puri"
        )
    }

    private fun resolvePreferredDish(preferredFood: String, pool: List<String>, fallbackDefault: String?): Dish {
        val trimmed = preferredFood.trim()
        if (trimmed.isBlank()) return Dish(name = fallbackDefault ?: "Chicken Karahi")
        val lower = trimmed.lowercase()

        // 1. Try finding a direct match in pool
        val directMatch = pool.firstOrNull { it.lowercase().contains(lower) }
        if (directMatch != null) return Dish(name = directMatch)

        // 2. Keyword mappings for common meal preferences:
        if (lower.contains("daal") || lower.contains("dal")) {
            return Dish(name = "Tadka Moong Daal with Rice")
        }
        if (lower.contains("rice") || lower.contains("chawal") || lower.contains("pulao")) {
            return Dish(name = "Matar Pulao with Raita")
        }
        if (lower.contains("biryani")) {
            return Dish(name = "Karachi Chicken Biryani")
        }
        if (lower.contains("karahi")) {
            return Dish(name = "Chicken Karahi")
        }
        if (lower.contains("korma")) {
            return Dish(name = "Mutton Korma with Taftan")
        }
        if (lower.contains("sabzi") || lower.contains("vegetable")) {
            return Dish(name = "Mix Vegetable Sabzi")
        }
        if (lower.contains("kabab") || lower.contains("kebab")) {
            return Dish(name = "Peshawari Chapli Kabab")
        }
        if (lower.contains("bbq") || lower.contains("tikka")) {
            return Dish(name = "Chicken Tikka Boti with Paratha")
        }
        if (lower.contains("nihari")) {
            return Dish(name = "Nihari with Roghani Naan")
        }
        if (lower.contains("haleem")) {
            return Dish(name = "Chicken Haleem with Lemon")
        }

        // 3. Custom dish name
        return Dish(name = trimmed.replaceFirstChar { it.uppercase() })
    }

    fun setDayPreference(day: String, meal: String, preferredFood: String) {
        val current = _dayPreferences.value.toMutableMap()
        val key = "${day.trim()}_${meal.trim().lowercase().replaceFirstChar { it.uppercase() }}"
        if (preferredFood.isBlank()) {
            current.remove(key)
        } else {
            current[key] = preferredFood.trim()
        }
        _dayPreferences.value = current
        prefsManager.saveDayPreferences(current)
        _weeklyPlan.value?.let { plan ->
            val updated = sanitizeAndEnsureFullWeek(plan)
            _weeklyPlan.value = updated
            prefsManager.saveWeeklyPlanLocally(updated)
        }
    }

    fun removeDayPreference(day: String, meal: String) {
        val current = _dayPreferences.value.toMutableMap()
        val key = "${day.trim()}_${meal.trim().lowercase().replaceFirstChar { it.uppercase() }}"
        current.remove(key)
        _dayPreferences.value = current
        prefsManager.saveDayPreferences(current)
    }

    private fun getInitialDefaultPlan(): WeeklyPlan {
        return WeeklyPlan(
            weekStartDate = System.currentTimeMillis(),
            days = DEFAULT_WEEK_DAYS
        )
    }

    /**
     * Ensures all 7 days (Monday through Sunday) exist and that every Lunch and Dinner
     * slot has a valid, delicious dish suggestion. If any meal is null or blank, it fills it automatically,
     * strictly respecting user day meal preferences.
     */
    fun sanitizeAndEnsureFullWeek(plan: WeeklyPlan?): WeeklyPlan {
        val existingDaysMap = plan?.days?.associateBy { it.dayName.trim().lowercase() } ?: emptyMap()
        val sanitizedDays = mutableListOf<DayPlan>()
        val usedLunches = mutableSetOf<String>()
        val usedDinners = mutableSetOf<String>()
        val prefsMap = _dayPreferences.value

        ORDERED_DAYS.forEachIndexed { index, dayName ->
            val defaultDay = DEFAULT_WEEK_DAYS[index]
            val existingDay = existingDaysMap[dayName.lowercase()]
            val preferredLunch = prefsMap["${dayName}_Lunch"]
            val preferredDinner = prefsMap["${dayName}_Dinner"]

            val lunch = if (preferredLunch != null && preferredLunch.isNotBlank()) {
                resolvePreferredDish(preferredLunch, LUNCH_SUGGESTION_POOL, defaultDay.lunch?.name)
            } else if (existingDay?.lunch != null && existingDay.lunch.name.isNotBlank()) {
                existingDay.lunch
            } else {
                val fallbackLunchName = LUNCH_SUGGESTION_POOL.firstOrNull { it !in usedLunches }
                    ?: defaultDay.lunch?.name ?: "Tadka Moong Daal with Rice"
                Dish(name = fallbackLunchName)
            }
            usedLunches.add(lunch.name)

            val dinner = if (preferredDinner != null && preferredDinner.isNotBlank()) {
                resolvePreferredDish(preferredDinner, DINNER_SUGGESTION_POOL, defaultDay.dinner?.name)
            } else if (existingDay?.dinner != null && existingDay.dinner.name.isNotBlank()) {
                existingDay.dinner
            } else {
                val fallbackDinnerName = DINNER_SUGGESTION_POOL.firstOrNull { it !in usedDinners && it != lunch.name }
                    ?: defaultDay.dinner?.name ?: "Chicken Karahi"
                Dish(name = fallbackDinnerName)
            }
            usedDinners.add(dinner.name)

            val tag = existingDay?.tag?.takeIf { it.isNotBlank() } ?: defaultDay.tag
            sanitizedDays.add(
                DayPlan(
                    dayName = dayName,
                    date = if (existingDay != null && existingDay.date > 0) existingDay.date else System.currentTimeMillis() + (index * 86400000L),
                    lunch = lunch,
                    dinner = dinner,
                    isGuestDay = existingDay?.isGuestDay ?: defaultDay.isGuestDay,
                    isSpecialDay = existingDay?.isSpecialDay ?: defaultDay.isSpecialDay,
                    isRoutineDay = existingDay?.isRoutineDay ?: defaultDay.isRoutineDay,
                    tag = tag
                )
            )
        }

        return WeeklyPlan(
            weekStartDate = if (plan != null && plan.weekStartDate > 0) plan.weekStartDate else System.currentTimeMillis(),
            days = sanitizedDays
        )
    }

    /**
     * Loads the active 7-day meal plan from local storage first, then synchronizes with Firestore.
     * Guarantees that every day has both Lunch and Dinner automatically filled.
     */
    fun loadWeeklyPlan() {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = UiState.Loading
            try {
                // 1. Immediately restore local saved plan and sanitize missing slots
                val localPlan = prefsManager.getLocalWeeklyPlan()
                val sanitizedLocal = sanitizeAndEnsureFullWeek(localPlan)
                _weeklyPlan.value = sanitizedLocal
                _uiState.value = UiState.Success(sanitizedLocal)

                // Stop loading spinner immediately once local plan is displayed!
                _isLoading.value = false

                // 2. Fetch from cloud Firestore in background with a quick timeout without holding UI spinner
                try {
                    val cloudPlan = withTimeoutOrNull(3000L) {
                        FirebaseService.fetchWeeklyPlan()
                    }
                    if (cloudPlan != null && cloudPlan.days.isNotEmpty()) {
                        val sanitizedCloud = sanitizeAndEnsureFullWeek(cloudPlan)
                        _weeklyPlan.value = sanitizedCloud
                        _uiState.value = UiState.Success(sanitizedCloud)
                        prefsManager.saveWeeklyPlanLocally(sanitizedCloud)
                    } else {
                        prefsManager.saveWeeklyPlanLocally(sanitizedLocal)
                    }
                } catch (e: Exception) {
                    Log.w("WeeklyPlannerVM", "Firestore sync skipped or timed out: ${e.message}")
                }
            } catch (e: Exception) {
                val fallback = sanitizeAndEnsureFullWeek(prefsManager.getLocalWeeklyPlan())
                _weeklyPlan.value = fallback
                _uiState.value = UiState.Success(fallback)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Refreshes ALL 14 items across all 7 days (both lunch AND dinner) with fresh diverse dishes.
     * Persists the newly generated menu locally and to Firestore.
     */
    fun refreshAllWeek(context: AppContext, prefs: UserPreferences, history: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = UiState.Loading
            try {
                // Fetch 7 diverse lunch dishes and dinner dishes concurrently to halve waiting time
                val lunchDeferred = async {
                    try {
                        withTimeoutOrNull(10000L) {
                            GeminiAIService.suggestDishes(
                                country = context.country,
                                city = context.city,
                                mealType = MealType.LUNCH,
                                dayType = DayType.NORMAL,
                                dishCount = 7,
                                sweetDishCount = 0,
                                excludedDishes = history,
                                wantToCook = prefs.wantToCook,
                                dontWantToCook = prefs.dontWantToCook,
                                season = context.season,
                                isRamadan = context.isRamadan,
                                occasion = "Weekly Meal Plan Lunch"
                            )
                        } ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                val dinnerDeferred = async {
                    try {
                        withTimeoutOrNull(10000L) {
                            GeminiAIService.suggestDishes(
                                country = context.country,
                                city = context.city,
                                mealType = MealType.DINNER,
                                dayType = DayType.NORMAL,
                                dishCount = 7,
                                sweetDishCount = 0,
                                excludedDishes = history,
                                wantToCook = prefs.wantToCook,
                                dontWantToCook = prefs.dontWantToCook,
                                season = context.season,
                                isRamadan = context.isRamadan,
                                occasion = "Weekly Meal Plan Dinner"
                            )
                        } ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                val lunchDishes = lunchDeferred.await()
                val dinnerDishes = dinnerDeferred.await()

                val oldDays = _weeklyPlan.value?.days ?: emptyList()
                val newDays = mutableListOf<DayPlan>()
                val prefsMap = _dayPreferences.value

                for (i in 0 until 7) {
                    val oldDay = oldDays.getOrNull(i)
                    val defaultDay = DEFAULT_WEEK_DAYS.getOrNull(i)
                    val dayName = oldDay?.dayName?.takeIf { it.isNotBlank() } ?: ORDERED_DAYS[i]

                    val preferredLunch = prefsMap["${dayName}_Lunch"]
                    val preferredDinner = prefsMap["${dayName}_Dinner"]

                    val lunch = if (preferredLunch != null && preferredLunch.isNotBlank()) {
                        resolvePreferredDish(preferredLunch, LUNCH_SUGGESTION_POOL, defaultDay?.lunch?.name)
                    } else {
                        lunchDishes.getOrNull(i)?.takeIf { it.name.isNotBlank() }
                            ?: defaultDay?.lunch ?: Dish(name = LUNCH_SUGGESTION_POOL[i % LUNCH_SUGGESTION_POOL.size])
                    }

                    val dinner = if (preferredDinner != null && preferredDinner.isNotBlank()) {
                        resolvePreferredDish(preferredDinner, DINNER_SUGGESTION_POOL, defaultDay?.dinner?.name)
                    } else {
                        dinnerDishes.getOrNull(i)?.takeIf { it.name.isNotBlank() }
                            ?: defaultDay?.dinner ?: Dish(name = DINNER_SUGGESTION_POOL[i % DINNER_SUGGESTION_POOL.size])
                    }

                    val tag = oldDay?.tag?.takeIf { it.isNotBlank() } ?: when (dayName) {
                        "Friday" -> "Friday Feast"
                        "Saturday" -> "Guest Day"
                        "Sunday" -> "Special"
                        else -> "Routine"
                    }

                    newDays.add(
                        DayPlan(
                            dayName = dayName,
                            date = System.currentTimeMillis() + (i * 86400000L),
                            lunch = lunch,
                            dinner = dinner,
                            isGuestDay = oldDay?.isGuestDay ?: (dayName == "Saturday"),
                            isSpecialDay = oldDay?.isSpecialDay ?: (dayName == "Friday" || dayName == "Sunday"),
                            isRoutineDay = oldDay?.isRoutineDay ?: (dayName !in listOf("Friday", "Saturday", "Sunday")),
                            tag = tag
                        )
                    )
                }

                val updatedPlan = WeeklyPlan(
                    weekStartDate = System.currentTimeMillis(),
                    days = newDays
                )

                _weeklyPlan.value = updatedPlan
                _uiState.value = UiState.Success(updatedPlan)
                prefsManager.saveWeeklyPlanLocally(updatedPlan)

                // Stop the refresh button spinner immediately once suggestions are populated!
                _isLoading.value = false

                // Cloud backup runs asynchronously in background
                viewModelScope.launch {
                    try {
                        withTimeoutOrNull(4000L) {
                            FirebaseService.saveWeeklyPlan(updatedPlan)
                        }
                    } catch (e: Exception) {
                        Log.w("WeeklyPlannerVM", "Background cloud save failed", e)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Refresh all failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Intelligently suggests a single dish for an individual slot (e.g. Friday Lunch).
     * Strictly syncs with day meal preferences (e.g. Rice), excludes currently rejected dishes,
     * and guarantees the suggestion respects the preferred category on repeated auto-suggest clicks.
     */
    suspend fun suggestDishForSlot(
        dayIndex: Int,
        slotName: String,
        currentDishName: String,
        recentlySuggested: List<String>
    ): String {
        val appCtx = ContextService.buildAppContext(getApplication())
        val prefs = prefsManager.loadUserPreferences()
        val dayName = ORDERED_DAYS.getOrNull(dayIndex) ?: "Friday"
        val dayPlan = _weeklyPlan.value?.days?.getOrNull(dayIndex)
        val prefKey = "${dayName}_$slotName"
        val preferredFood = _dayPreferences.value[prefKey]?.trim().orEmpty()

        val isSpecial = dayPlan?.isSpecialDay == true || dayName.equals("Friday", ignoreCase = true) || dayName.equals("Sunday", ignoreCase = true)
        val dayType = if (isSpecial || dayPlan?.isGuestDay == true) DayType.SPECIAL else DayType.NORMAL
        val mealType = if (slotName.equals("Lunch", ignoreCase = true)) MealType.LUNCH else MealType.DINNER

        // Build excluded list: current dish + session suggestions + user dislikes
        val allExcluded = (recentlySuggested + listOfNotNull(currentDishName.takeIf { it.isNotBlank() })).distinct()

        val effectiveWantToCook = if (preferredFood.isNotBlank()) {
            listOf(preferredFood) + prefs.wantToCook
        } else {
            prefs.wantToCook
        }

        val occasionPrompt = if (preferredFood.isNotBlank()) {
            "User preference for $dayName $slotName is strictly: '$preferredFood'. Suggest a dish that features or matches '$preferredFood'. Do not suggest any dish that does not contain '$preferredFood'."
        } else {
            "Meal suggestion for $dayName $slotName"
        }

        try {
            val suggestions = GeminiAIService.suggestDishes(
                country = appCtx.country,
                city = appCtx.city,
                mealType = mealType,
                dayType = dayType,
                dishCount = 1,
                sweetDishCount = 0,
                excludedDishes = allExcluded,
                wantToCook = effectiveWantToCook,
                dontWantToCook = prefs.dontWantToCook,
                season = appCtx.season,
                isRamadan = appCtx.isRamadan,
                occasion = occasionPrompt
            )

            val candidate = suggestions.firstOrNull()?.name?.trim().orEmpty()
            if (candidate.isNotBlank() && candidate !in allExcluded) {
                // If a preferred food was set (e.g. "Rice"), verify candidate matches it
                if (preferredFood.isNotBlank()) {
                    val pLower = preferredFood.lowercase()
                    val cLower = candidate.lowercase()
                    if (cLower.contains(pLower) || isDishMatchingPreference(cLower, pLower)) {
                        return candidate
                    }
                    // If AI returned something unrelated (e.g. Aloo Gosht when preference is Rice), fall back to preferred pool
                } else {
                    return candidate
                }
            }
        } catch (e: Exception) {
            Log.w("WeeklyPlannerVM", "AI suggestDishForSlot failed", e)
        }

        // Fallback: Pick a diverse alternative matching preferredFood from pool
        val pool = if (mealType == MealType.LUNCH) LUNCH_SUGGESTION_POOL else DINNER_SUGGESTION_POOL
        if (preferredFood.isNotBlank()) {
            val pLower = preferredFood.lowercase()
            val matchingInPool = pool.filter { dish ->
                val dLower = dish.lowercase()
                (dLower.contains(pLower) || isDishMatchingPreference(dLower, pLower)) && dish !in allExcluded
            }
            if (matchingInPool.isNotEmpty()) {
                return matchingInPool.random()
            }
            return resolvePreferredDish(preferredFood, pool, null).name
        }

        val remaining = pool.filter { it !in allExcluded }
        return remaining.randomOrNull() ?: pool.random()
    }

    private fun isDishMatchingPreference(dishNameLower: String, prefLower: String): Boolean {
        if (prefLower.contains("rice") || prefLower.contains("chawal")) {
            return dishNameLower.contains("rice") || dishNameLower.contains("biryani") ||
                   dishNameLower.contains("pulao") || dishNameLower.contains("chawal") ||
                   dishNameLower.contains("khichdi") || dishNameLower.contains("tahiri")
        }
        if (prefLower.contains("daal") || prefLower.contains("dal")) {
            return dishNameLower.contains("daal") || dishNameLower.contains("dal") || dishNameLower.contains("lentil")
        }
        if (prefLower.contains("pasta")) {
            return dishNameLower.contains("pasta") || dishNameLower.contains("spaghetti") ||
                   dishNameLower.contains("penne") || dishNameLower.contains("macaroni") || dishNameLower.contains("lasagna")
        }
        if (prefLower.contains("salad")) {
            return dishNameLower.contains("salad")
        }
        if (prefLower.contains("chicken")) {
            return dishNameLower.contains("chicken")
        }
        if (prefLower.contains("beef")) {
            return dishNameLower.contains("beef")
        }
        if (prefLower.contains("fish") || prefLower.contains("seafood")) {
            return dishNameLower.contains("fish") || dishNameLower.contains("prawn") || dishNameLower.contains("salmon")
        }
        if (prefLower.contains("sabzi") || prefLower.contains("vegetable") || prefLower.contains("veggie")) {
            return dishNameLower.contains("sabzi") || dishNameLower.contains("vegetable") ||
                   dishNameLower.contains("aloo") || dishNameLower.contains("bhindi") ||
                   dishNameLower.contains("palak") || dishNameLower.contains("gobi")
        }
        return dishNameLower.contains(prefLower)
    }

    /**
     * Backward-compatible alias for refreshing all items in the week.
     */
    fun autoFillWeek(context: AppContext, prefs: UserPreferences, history: List<String>) {
        refreshAllWeek(context, prefs, history)
    }

    /**
     * Refreshes all suggestions for the entire week using user preferences and history.
     */
    fun refreshAllSuggestions() {
        viewModelScope.launch {
            val appCtx = com.aura.what2eat.service.ContextService.buildAppContext(getApplication())
            val prefs = prefsManager.loadUserPreferences()
            val history = prefsManager.getLocalCookedHistory().map { it.dishName }
            refreshAllWeek(appCtx, prefs, history)
        }
    }

    /**
     * Updates a single item/slot (Lunch or Dinner) on a chosen day and saves the plan locally and in cloud.
     */
    fun updateDayMeal(dayIndex: Int, mealType: String, dish: Dish) {
        viewModelScope.launch {
            val plan = _weeklyPlan.value ?: return@launch
            val updatedDays = plan.days.toMutableList()
            if (dayIndex in updatedDays.indices) {
                val day = updatedDays[dayIndex]
                val safeDish = if (dish.name.isBlank()) {
                    if (mealType.equals("Lunch", ignoreCase = true)) {
                        day.lunch?.takeIf { it.name.isNotBlank() } ?: DEFAULT_WEEK_DAYS.getOrNull(dayIndex)?.lunch ?: Dish(name = "Tadka Moong Daal with Rice")
                    } else {
                        day.dinner?.takeIf { it.name.isNotBlank() } ?: DEFAULT_WEEK_DAYS.getOrNull(dayIndex)?.dinner ?: Dish(name = "Chicken Karahi")
                    }
                } else {
                    dish
                }

                updatedDays[dayIndex] = if (mealType.equals("Lunch", ignoreCase = true)) {
                    day.copy(lunch = safeDish)
                } else {
                    day.copy(dinner = safeDish)
                }

                val updatedPlan = plan.copy(days = updatedDays)
                _weeklyPlan.value = updatedPlan
                _uiState.value = UiState.Success(updatedPlan)
                prefsManager.saveWeeklyPlanLocally(updatedPlan)
                FirebaseService.saveWeeklyPlan(updatedPlan)
            }
        }
    }

    /**
     * Persists the entire plan directly.
     */
    fun saveFullPlan(plan: WeeklyPlan) {
        val sanitized = sanitizeAndEnsureFullWeek(plan)
        _weeklyPlan.value = sanitized
        _uiState.value = UiState.Success(sanitized)
        prefsManager.saveWeeklyPlanLocally(sanitized)
        viewModelScope.launch {
            FirebaseService.saveWeeklyPlan(sanitized)
        }
    }

    /**
     * Toggles Guest Day tag.
     */
    fun toggleGuestDay(dayIndex: Int) {
        val plan = _weeklyPlan.value ?: return
        val updatedDays = plan.days.toMutableList()
        if (dayIndex in updatedDays.indices) {
            val current = updatedDays[dayIndex]
            val isGuest = !current.isGuestDay
            updatedDays[dayIndex] = current.copy(
                isGuestDay = isGuest,
                tag = if (isGuest) "Guest Day" else "Routine"
            )
            val updatedPlan = plan.copy(days = updatedDays)
            _weeklyPlan.value = updatedPlan
            prefsManager.saveWeeklyPlanLocally(updatedPlan)
            viewModelScope.launch { FirebaseService.saveWeeklyPlan(updatedPlan) }
        }
    }

    /**
     * Toggles Special Day tag.
     */
    fun toggleSpecialDay(dayIndex: Int) {
        val plan = _weeklyPlan.value ?: return
        val updatedDays = plan.days.toMutableList()
        if (dayIndex in updatedDays.indices) {
            val current = updatedDays[dayIndex]
            val isSpecial = !current.isSpecialDay
            updatedDays[dayIndex] = current.copy(
                isSpecialDay = isSpecial,
                tag = if (isSpecial) "Special" else "Routine"
            )
            val updatedPlan = plan.copy(days = updatedDays)
            _weeklyPlan.value = updatedPlan
            prefsManager.saveWeeklyPlanLocally(updatedPlan)
            viewModelScope.launch { FirebaseService.saveWeeklyPlan(updatedPlan) }
        }
    }
}
