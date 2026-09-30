package com.aura.what2eat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.what2eat.auth.GoogleAuthManager
import com.aura.what2eat.model.CommunityDish
import com.aura.what2eat.model.Dish
import com.aura.what2eat.service.FirebaseService
import com.aura.what2eat.service.GeminiAIService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommunityViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _suggestions = MutableStateFlow<List<CommunityDish>>(emptyList())
    val suggestions: StateFlow<List<CommunityDish>> = _suggestions.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<CommunityDish>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<CommunityDish>>> = _uiState.asStateFlow()

    init {
        fetchSuggestions()
    }

    /**
     * Observes real-time community suggestions from Firestore.
     */
    fun fetchSuggestions() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                FirebaseService.observeSuggestions().collect { dishes ->
                    _suggestions.value = dishes
                    _uiState.value = UiState.Success(dishes)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to fetch community dishes")
            }
        }
    }

    /**
     * Toggles/increments like for a dish.
     */
    fun likeDish(id: String) {
        viewModelScope.launch {
            val uid = GoogleAuthManager.getCurrentUser()?.uid ?: "anon_${System.currentTimeMillis()}"
            FirebaseService.likeSuggestion(id, uid)
        }
    }

    /**
     * Reports an inappropriate dish suggestion.
     */
    fun reportDish(id: String, reason: String) {
        viewModelScope.launch {
            val uid = GoogleAuthManager.getCurrentUser()?.uid ?: "anon"
            FirebaseService.reportSuggestion(id, uid, reason)
        }
    }

    /**
     * Generates a complete recipe from dish name & cuisine using What2eat.
     */
    suspend fun checkAIForRecipe(dishName: String, cuisine: String, country: String = "Pakistan"): Dish? {
        return try {
            val dish = GeminiAIService.generateRecipe(
                dishName = dishName,
                cuisine = cuisine,
                country = country
            )
            if (dish != null && dish.recipeSteps.isNotEmpty()) dish else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Submits a user's community recipe suggestion.
     */
    fun submitSuggestion(dish: CommunityDish, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val success = FirebaseService.submitSuggestion(dish)
            _isSubmitting.value = false
            onComplete(success)
        }
    }
}
