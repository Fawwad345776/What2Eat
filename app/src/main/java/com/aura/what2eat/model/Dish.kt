package com.aura.what2eat.model

data class Dish(
    val id: String = "",
    val name: String = "",
    val cuisine: CuisineType = CuisineType.OTHER,
    val country: String = "",
    val mealType: MealType = MealType.DINNER,
    val dayType: DayType = DayType.NORMAL,
    val season: String = "",
    val isSpecial: Boolean = false,
    val prepTimeMinutes: Int = 0,
    val difficulty: String = "Easy",
    val servings: Int = 1,
    val ingredients: List<String> = emptyList(),
    val recipeSteps: List<String> = emptyList(),
    val chefTip: String = "",
    val tags: List<String> = emptyList(),
    val isAIGenerated: Boolean = false,
    val submittedBy: String? = null,
    val likesCount: Int = 0
)
