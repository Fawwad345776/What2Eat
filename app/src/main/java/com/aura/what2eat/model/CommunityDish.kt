package com.aura.what2eat.model

data class CommunityDish(
    val id: String = "",
    val dishName: String = "",
    val submittedBy: String = "",
    val userEmail: String = "",
    val userUID: String = "",
    val cuisine: String = "",
    val country: String = "",
    val likesCount: Int = 0,
    val reportCount: Int = 0,
    val ingredients: List<String> = emptyList(),
    val recipeSteps: List<String> = emptyList(),
    val prepTime: Int = 0,
    val difficulty: String = "Easy",
    val chefTip: String = "",
    val isAIGenerated: Boolean = false,
    val createdAt: Long = 0L,
    val isApproved: Boolean = false,
    val isBlocked: Boolean = false
) {
    fun toDish(): Dish {
        return Dish(
            id = id,
            name = dishName,
            cuisine = CuisineType.fromString(cuisine),
            country = country,
            ingredients = if (ingredients.isNotEmpty()) ingredients else listOf("Main Ingredients", "Onions", "Tomatoes", "Garlic-Ginger Paste", "Spices", "Ghee / Cooking Oil", "Fresh Herbs"),
            recipeSteps = if (recipeSteps.isNotEmpty()) recipeSteps else listOf("Prepare and wash ingredients.", "Sauté aromatics with spices until fragrant.", "Cook on low flame until tender and rich in flavor.", "Garnish with fresh herbs and serve hot."),
            prepTimeMinutes = if (prepTime > 0) prepTime else 30,
            difficulty = if (difficulty.isNotBlank()) difficulty else "Easy",
            chefTip = chefTip.ifBlank { "Serve hot with fresh naan, parathas, or steamed rice." },
            submittedBy = submittedBy,
            tags = listOf(cuisine, country),
            isAIGenerated = isAIGenerated,
            likesCount = likesCount
        )
    }
}
