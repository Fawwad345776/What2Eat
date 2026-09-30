package com.aura.what2eat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.UserPreferences
import com.aura.what2eat.service.FirebaseService
import com.aura.what2eat.service.GeminiAIService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PreferencesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager.getInstance(application)

    private val _preferences = MutableStateFlow(prefsManager.loadUserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private val _aiSuggestions = MutableStateFlow<Pair<List<String>, List<String>>>(Pair(emptyList(), emptyList()))
    val aiSuggestions: StateFlow<Pair<List<String>, List<String>>> = _aiSuggestions.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<UserPreferences>>(UiState.Idle)
    val uiState: StateFlow<UiState<UserPreferences>> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    /**
     * Loads local user preferences and syncs from Firestore if available.
     */
    fun loadPreferences() {
        val local = prefsManager.loadUserPreferences()
        _preferences.value = local
        loadAISuggestions(local.country.ifBlank { "Pakistan" })

        viewModelScope.launch {
            val cloud = FirebaseService.fetchPreferences()
            if (cloud != null) {
                _preferences.value = cloud
                prefsManager.saveUserPreferences(cloud)
            }
        }
    }

    /**
     * Loads AI recommendations for Want / Don't want lists for the user's country.
     */
    fun loadAISuggestions(country: String) {
        viewModelScope.launch {
            try {
                val suggestions = GeminiAIService.getKitchenPreferenceSuggestions(country)
                _aiSuggestions.value = suggestions
            } catch (e: Exception) {
                // Keep defaults
            }
        }
    }

    fun addWantToCook(item: String) {
        val trimmed = item.trim()
        val current = _preferences.value
        if (trimmed.isNotBlank() && !current.wantToCook.contains(trimmed)) {
            _preferences.value = current.copy(wantToCook = current.wantToCook + trimmed)
        }
    }

    fun removeWantToCook(item: String) {
        val current = _preferences.value
        _preferences.value = current.copy(wantToCook = current.wantToCook.filter { it != item })
    }

    fun addDontWantToCook(item: String) {
        val trimmed = item.trim()
        val current = _preferences.value
        if (trimmed.isNotBlank() && !current.dontWantToCook.contains(trimmed)) {
            _preferences.value = current.copy(dontWantToCook = current.dontWantToCook + trimmed)
        }
    }

    fun removeDontWantToCook(item: String) {
        val current = _preferences.value
        _preferences.value = current.copy(dontWantToCook = current.dontWantToCook.filter { it != item })
    }

    fun updateRoutineRules(dailyDaal: Boolean, dailySabzi: Boolean, dailyMeat: Boolean, seasonalAI: Boolean) {
        val current = _preferences.value
        _preferences.value = current.copy(
            dailyDaal = dailyDaal,
            dailySabzi = dailySabzi,
            dailyMeat = dailyMeat,
            seasonalAI = seasonalAI
        )
    }

    fun updateAntiRepetitionDays(days: Int) {
        val current = _preferences.value
        _preferences.value = current.copy(antiRepetitionDays = days)
    }

    /**
     * Saves user preferences to local SharedPreferences and syncs to Firestore.
     */
    fun savePreferences() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val current = _preferences.value
                prefsManager.saveUserPreferences(current)
                prefsManager.setAntiRepetitionDays(current.antiRepetitionDays)
                FirebaseService.savePreferences(current)
                _uiState.value = UiState.Success(current)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to save preferences")
            }
        }
    }
}
