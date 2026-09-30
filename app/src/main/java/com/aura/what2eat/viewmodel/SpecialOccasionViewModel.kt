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

class SpecialOccasionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager.getInstance(application)

    private val _selectedOccasion = MutableStateFlow("🌙 Eid ul Fitr")
    val selectedOccasion: StateFlow<String> = _selectedOccasion.asStateFlow()

    private val _dishCount = MutableStateFlow(1)
    val dishCount: StateFlow<Int> = _dishCount.asStateFlow()

    private val _occasionDishes = MutableStateFlow<List<Dish>>(emptyList())
    val occasionDishes: StateFlow<List<Dish>> = _occasionDishes.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _skipsRemaining = MutableStateFlow(prefsManager.getOccasionSkipsRemaining())
    val skipsRemaining: StateFlow<Int> = _skipsRemaining.asStateFlow()

    private val _showSkipLimitDialog = MutableStateFlow(false)
    val showSkipLimitDialog: StateFlow<Boolean> = _showSkipLimitDialog.asStateFlow()

    fun setSelectedOccasion(occasion: String) {
        _selectedOccasion.value = occasion
    }

    fun setDishCount(count: Int) {
        _dishCount.value = count
    }

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

    fun generateSpecialMenu(appContext: AppContext) {
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
            _isGenerating.value = true
            try {
                val userPrefs = prefsManager.loadUserPreferences()
                val excluded = FirebaseService.getDishesCooked30Days()

                val result = GeminiAIService.suggestDishes(
                    country = appContext.country,
                    city = appContext.city,
                    mealType = MealType.DINNER,
                    dayType = DayType.SPECIAL,
                    dishCount = _dishCount.value,
                    sweetDishCount = 1,
                    excludedDishes = excluded,
                    wantToCook = userPrefs.wantToCook,
                    dontWantToCook = userPrefs.dontWantToCook,
                    season = appContext.season,
                    isRamadan = appContext.isRamadan,
                    occasion = _selectedOccasion.value
                )

                _occasionDishes.value = result
            } catch (e: Exception) {
                // Ignore or log error
            } finally {
                _isGenerating.value = false
            }
        }
    }
}
