package com.aura.what2eat.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.CookedHistoryEntry
import com.aura.what2eat.model.DayType
import com.aura.what2eat.model.Dish
import com.aura.what2eat.model.MealType
import com.aura.what2eat.service.ContextService
import com.aura.what2eat.service.FirebaseService
import com.aura.what2eat.service.GeminiAIService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MealDeciderViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager.getInstance(application)

    // Configuration & Suggestions State (retained across detail view navigation)
    var selectedMealType by mutableStateOf(MealType.DINNER)
    var selectedDishCount by mutableIntStateOf(1)
    var selectedDayType by mutableStateOf(DayType.NORMAL)
    var includeSweets by mutableStateOf(false)
    var sweetDishCount by mutableIntStateOf(1)

    var suggestionsList by mutableStateOf<List<Dish>>(emptyList())
    var isLoading by mutableStateOf(false)
    var currentDishIndex by mutableIntStateOf(0)
    var previouslySuggestedNames by mutableStateOf<List<String>>(emptyList())
    var swappingDishIndex by mutableStateOf<Int?>(null)

    var skipsRemaining by mutableIntStateOf(prefsManager.getSkipsRemaining())
    var showSkipLimitSheet by mutableStateOf(false)
    var confirmedDish by mutableStateOf<Dish?>(null)
    var confirmCount by mutableIntStateOf(0)

    private val _dishQueue = mutableListOf<Dish>()
    private val _currentDish = MutableStateFlow<Dish?>(null)
    val currentDish: StateFlow<Dish?> = _currentDish.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<Dish>>(UiState.Idle)
    val uiState: StateFlow<UiState<Dish>> = _uiState.asStateFlow()

    fun refreshSkips() {
        skipsRemaining = prefsManager.getSkipsRemaining()
    }

    fun clearSuggestions() {
        suggestionsList = emptyList()
        currentDishIndex = 0
        swappingDishIndex = null
    }

    /**
     * Confirms the dish for cooking and logs it to 30-day anti-repetition memory.
     */
    fun confirmDish(dish: Dish, mealType: MealType = MealType.DINNER) {
        logCookedToHistory(dish, mealType)
    }

    /**
     * Logs the dish locally and to Firestore to prevent repetition for 30 days.
     */
    fun logCookedToHistory(dish: Dish, mealType: MealType = MealType.DINNER) {
        viewModelScope.launch {
            val entry = CookedHistoryEntry(
                dishName = dish.name,
                dishId = dish.id.ifBlank { "d_${System.currentTimeMillis()}" },
                mealType = mealType.name,
                cookedDate = System.currentTimeMillis()
            )

            // Local cache
            prefsManager.saveCookedHistoryLocally(entry)

            // Cloud Firestore
            FirebaseService.saveCookedDish(dish, mealType)
        }
    }

    /**
     * Called when the user watches a rewarded video ad → grants 3 extra skips.
     */
    fun onWatchAdComplete() {
        prefsManager.grantExtraSkips(3)
        skipsRemaining = prefsManager.getSkipsRemaining()
        showSkipLimitSheet = false
    }

    fun dismissSkipLimitDialog() {
        showSkipLimitSheet = false
    }
}
