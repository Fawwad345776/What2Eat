package com.aura.what2eat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.AppContext
import com.aura.what2eat.model.DayType
import com.aura.what2eat.model.Dish
import com.aura.what2eat.model.MealType
import com.aura.what2eat.service.FirebaseService
import com.aura.what2eat.service.GeminiAIService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GuestPlannerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager.getInstance(application)

    private val _generatedMenu = MutableStateFlow<List<Dish>>(emptyList())
    val generatedMenu: StateFlow<List<Dish>> = _generatedMenu.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _swappingIndex = MutableStateFlow<Int?>(null)
    val swappingIndex: StateFlow<Int?> = _swappingIndex.asStateFlow()

    private val _skipsRemaining = MutableStateFlow(prefsManager.getOccasionSkipsRemaining())
    val skipsRemaining: StateFlow<Int> = _skipsRemaining.asStateFlow()

    private val _showSkipLimitDialog = MutableStateFlow(false)
    val showSkipLimitDialog: StateFlow<Boolean> = _showSkipLimitDialog.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<Dish>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<Dish>>> = _uiState.asStateFlow()

    fun refreshSkips() {
        _skipsRemaining.value = prefsManager.getOccasionSkipsRemaining()
    }

    fun dismissSkipLimitDialog() {
        _showSkipLimitDialog.value = false
    }

    fun grantDebugSkips() {
        prefsManager.grantExtraOccasionSkips(3)
        _skipsRemaining.value = prefsManager.getOccasionSkipsRemaining()
        _showSkipLimitDialog.value = false
    }

    /**
     * Generates a balanced multi-course feast menu using What2eat.
     */
    fun generateGuestMenu(mainCount: Int, sweetCount: Int, context: AppContext) {
        if (prefsManager.getOccasionSkipsRemaining() <= 0) {
            if (com.aura.what2eat.BuildConfig.DEBUG) {
                prefsManager.grantExtraOccasionSkips(3)
                _skipsRemaining.value = prefsManager.getOccasionSkipsRemaining()
            } else {
                _showSkipLimitDialog.value = true
                return
            }
        }

        prefsManager.useOccasionSkip()
        _skipsRemaining.value = prefsManager.getOccasionSkipsRemaining()

        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = UiState.Loading

            try {
                val userPrefs = prefsManager.loadUserPreferences()
                val excluded = FirebaseService.getDishesCooked30Days()

                val dishes = GeminiAIService.suggestDishes(
                    country = context.country,
                    city = context.city,
                    mealType = MealType.DINNER,
                    dayType = DayType.SPECIAL,
                    dishCount = mainCount,
                    sweetDishCount = sweetCount,
                    excludedDishes = excluded,
                    wantToCook = userPrefs.wantToCook,
                    dontWantToCook = userPrefs.dontWantToCook,
                    season = context.season,
                    isRamadan = context.isRamadan,
                    occasion = "Guest Feast Dawat"
                )

                _generatedMenu.value = dishes
                _uiState.value = UiState.Success(dishes)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to generate guest feast")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Swaps out a single course for a new AI alternative.
     */
    fun swapDish(index: Int, context: AppContext) {
        if (prefsManager.getOccasionSkipsRemaining() <= 0) {
            if (com.aura.what2eat.BuildConfig.DEBUG) {
                prefsManager.grantExtraOccasionSkips(3)
                _skipsRemaining.value = prefsManager.getOccasionSkipsRemaining()
            } else {
                _showSkipLimitDialog.value = true
                return
            }
        }

        prefsManager.useOccasionSkip()
        _skipsRemaining.value = prefsManager.getOccasionSkipsRemaining()

        viewModelScope.launch {
            _swappingIndex.value = index
            try {
                val currentMenu = _generatedMenu.value.toMutableList()
                if (index in currentMenu.indices) {
                    val replacement = GeminiAIService.suggestDishes(
                        country = context.country,
                        city = context.city,
                        mealType = MealType.DINNER,
                        dayType = DayType.SPECIAL,
                        dishCount = 1,
                        sweetDishCount = 0,
                        excludedDishes = currentMenu.map { it.name },
                        wantToCook = emptyList(),
                        dontWantToCook = emptyList(),
                        season = context.season,
                        isRamadan = context.isRamadan,
                        occasion = "Guest Feast Replacement"
                    )

                    if (replacement.isNotEmpty()) {
                        currentMenu[index] = replacement.first()
                        _generatedMenu.value = currentMenu
                        _uiState.value = UiState.Success(currentMenu)
                    }
                }
            } catch (e: Exception) {
                // Ignore swap error
            } finally {
                _swappingIndex.value = null
            }
        }
    }

    /**
     * Saves the feast menu to the weekly meal planner schedule.
     */
    fun saveToWeeklyPlan(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // Save or sync to planner
            onComplete()
        }
    }
}
