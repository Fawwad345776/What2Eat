package com.aura.what2eat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.AppContext
import com.aura.what2eat.model.DayType
import com.aura.what2eat.model.Dish
import com.aura.what2eat.model.MealType
import com.aura.what2eat.service.GeminiAIService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PantryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager.getInstance(application)

    private val _ingredients = MutableStateFlow<List<String>>(listOf("Tomatoes", "Onions", "Chicken"))
    val ingredients: StateFlow<List<String>> = _ingredients.asStateFlow()

    private val _matchResults = MutableStateFlow<List<Dish>>(emptyList())
    val matchResults: StateFlow<List<Dish>> = _matchResults.asStateFlow()

    private val _searchesRemaining = MutableStateFlow(
        (PreferencesManager.MONTHLY_PANTRY_SEARCH_LIMIT - prefsManager.getMonthlyPantrySearches()).coerceAtLeast(0)
    )
    val searchesRemaining: StateFlow<Int> = _searchesRemaining.asStateFlow()

    private val _showQuotaDialog = MutableStateFlow(false)
    val showQuotaDialog: StateFlow<Boolean> = _showQuotaDialog.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<Dish>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<Dish>>> = _uiState.asStateFlow()

    /**
     * Adds an ingredient tag.
     */
    fun addIngredient(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !_ingredients.value.contains(trimmed)) {
            _ingredients.value = _ingredients.value + trimmed
        }
    }

    /**
     * Removes an ingredient tag.
     */
    fun removeIngredient(name: String) {
        _ingredients.value = _ingredients.value.filter { it != name }
    }

    /**
     * Finds recipes matching selected pantry ingredients. Enforces monthly search quota.
     */
    fun findMatchingRecipes(context: AppContext) {
        if (_ingredients.value.isEmpty()) return

        val canSearch = prefsManager.usePantrySearch()
        _searchesRemaining.value = (PreferencesManager.MONTHLY_PANTRY_SEARCH_LIMIT - prefsManager.getMonthlyPantrySearches()).coerceAtLeast(0)

        if (!canSearch) {
            _showQuotaDialog.value = true
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = UiState.Loading
            try {
                // Ensure loader progress is visible to user
                kotlinx.coroutines.delay(400)

                // Call Groq API Pantry Matcher directly
                val pantryMatch = com.aura.what2eat.service.GroqAIService.matchPantryIngredients(
                    availableIngredients = _ingredients.value,
                    dontWantToCookList = emptyList(),
                    city = context.city,
                    country = context.country
                )

                val suggestions = com.aura.what2eat.service.GroqAIService.suggestDishes(
                    country = context.country,
                    city = context.city,
                    mealType = MealType.DINNER,
                    dayType = DayType.NORMAL,
                    dishCount = 2,
                    sweetDishCount = 0,
                    excludedDishes = emptyList(),
                    wantToCook = _ingredients.value,
                    dontWantToCook = emptyList(),
                    season = context.season,
                    isRamadan = context.isRamadan,
                    occasion = "Pantry Matcher"
                )

                val matchedDish = Dish(
                    id = "pantry_${System.currentTimeMillis()}",
                    name = pantryMatch.dishName,
                    cuisine = com.aura.what2eat.model.CuisineType.PAKISTANI,
                    country = context.country,
                    mealType = MealType.DINNER,
                    dayType = DayType.NORMAL,
                    season = context.season,
                    isSpecial = false,
                    prepTimeMinutes = pantryMatch.prepTimeMinutes,
                    difficulty = "Easy",
                    servings = 4,
                    ingredients = if (pantryMatch.usedIngredients.isNotEmpty()) pantryMatch.usedIngredients else _ingredients.value,
                    recipeSteps = listOf(
                        "Gather ingredients: ${if (pantryMatch.usedIngredients.isNotEmpty()) pantryMatch.usedIngredients.joinToString(", ") else _ingredients.value.joinToString(", ")}.",
                        "Heat oil in a wok or pan over medium flame.",
                        "Sauté and blend seasonings with the ingredients until aromatic and cooked thoroughly.",
                        "Garnish and serve hot."
                    ),
                    chefTip = pantryMatch.description.ifBlank { "Cooked using your fresh pantry staples." },
                    tags = listOf("Pantry Match", "Homemade"),
                    isAIGenerated = true
                )

                val combined = listOf(matchedDish) + suggestions.filter { !it.name.equals(matchedDish.name, ignoreCase = true) }
                _matchResults.value = combined
                _uiState.value = UiState.Success(combined)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to find matching recipes")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Called when rewarded video ad is completed → grants 1 extra search.
     */
    fun onWatchAdComplete() {
        prefsManager.grantExtraPantrySearch()
        _searchesRemaining.value = (PreferencesManager.MONTHLY_PANTRY_SEARCH_LIMIT - prefsManager.getMonthlyPantrySearches()).coerceAtLeast(0)
        _showQuotaDialog.value = false
    }

    fun dismissQuotaDialog() {
        _showQuotaDialog.value = false
    }
}
