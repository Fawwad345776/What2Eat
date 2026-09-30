package com.aura.what2eat.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.what2eat.model.AppContext
import com.aura.what2eat.model.CommunityDish
import com.aura.what2eat.service.ContextService
import com.aura.what2eat.service.FirebaseService
import com.aura.what2eat.service.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _topSuggestions = MutableStateFlow<List<CommunityDish>>(emptyList())
    val topSuggestions: StateFlow<List<CommunityDish>> = _topSuggestions.asStateFlow()

    private val _appContext = MutableStateFlow(AppContext())
    val appContext: StateFlow<AppContext> = _appContext.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<CommunityDish>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<CommunityDish>>> = _uiState.asStateFlow()

    init {
        loadData(application)
    }

    /**
     * Loads current kitchen context (location, weather, season, Islamic dates) and top community dishes.
     */
    fun loadData(context: Context) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // 1. Build context
                val ctx = ContextService.buildAppContext(context)
                _appContext.value = ctx

                // 2. Fetch/Observe real-time community suggestions
                FirebaseService.observeSuggestions().collect { suggestions ->
                    _topSuggestions.value = suggestions
                    _uiState.value = UiState.Success(suggestions)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to load home data")
            }
        }
    }

    /**
     * Re-detects GPS location and updates context.
     */
    fun refreshLocation(context: Context) {
        viewModelScope.launch {
            try {
                LocationService.getCurrentLocation(context)
                val updatedContext = ContextService.buildAppContext(context)
                _appContext.value = updatedContext
            } catch (e: Exception) {
                // Keep existing location on error
            }
        }
    }
}
