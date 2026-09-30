package com.aura.what2eat.model

/**
 * Converts a [Dish] instance into a Map suitable for storing in Firebase Firestore.
 */
fun Dish.toFirebaseMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "name" to name,
        "cuisine" to cuisine.name,
        "country" to country,
        "mealType" to mealType.name,
        "dayType" to dayType.name,
        "season" to season,
        "isSpecial" to isSpecial,
        "prepTimeMinutes" to prepTimeMinutes,
        "difficulty" to difficulty,
        "servings" to servings,
        "ingredients" to ingredients,
        "recipeSteps" to recipeSteps,
        "chefTip" to chefTip,
        "tags" to tags,
        "isAIGenerated" to isAIGenerated,
        "submittedBy" to submittedBy,
        "likesCount" to likesCount
    )
}

/**
 * Parses a Map retrieved from Firebase Firestore into a [Dish] instance safely.
 */
fun Map<String, Any?>.toDish(): Dish {
    return Dish(
        id = (this["id"] as? String).orEmpty(),
        name = (this["name"] as? String).orEmpty(),
        cuisine = CuisineType.fromString(this["cuisine"] as? String),
        country = (this["country"] as? String).orEmpty(),
        mealType = MealType.fromString(this["mealType"] as? String),
        dayType = DayType.fromString(this["dayType"] as? String),
        season = (this["season"] as? String).orEmpty(),
        isSpecial = (this["isSpecial"] as? Boolean) ?: false,
        prepTimeMinutes = (this["prepTimeMinutes"] as? Number)?.toInt() ?: 0,
        difficulty = (this["difficulty"] as? String) ?: "Easy",
        servings = (this["servings"] as? Number)?.toInt() ?: 1,
        ingredients = (this["ingredients"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        recipeSteps = (this["recipeSteps"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        chefTip = (this["chefTip"] as? String).orEmpty(),
        tags = (this["tags"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        isAIGenerated = (this["isAIGenerated"] as? Boolean) ?: false,
        submittedBy = this["submittedBy"] as? String,
        likesCount = (this["likesCount"] as? Number)?.toInt() ?: 0
    )
}

/**
 * Converts a [CommunityDish] instance into a Map suitable for storing in Firebase Firestore.
 */
fun CommunityDish.toFirebaseMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "dishName" to dishName,
        "submittedBy" to submittedBy,
        "userEmail" to userEmail,
        "userUID" to userUID,
        "cuisine" to cuisine,
        "country" to country,
        "likesCount" to likesCount,
        "reportCount" to reportCount,
        "ingredients" to ingredients,
        "recipeSteps" to recipeSteps,
        "prepTime" to prepTime,
        "difficulty" to difficulty,
        "chefTip" to chefTip,
        "isAIGenerated" to isAIGenerated,
        "createdAt" to createdAt,
        "isApproved" to isApproved,
        "isBlocked" to isBlocked
    )
}

/**
 * Parses a Map retrieved from Firebase Firestore into a [CommunityDish] instance safely.
 */
fun Map<String, Any?>.toCommunityDish(): CommunityDish {
    return CommunityDish(
        id = (this["id"] as? String).orEmpty(),
        dishName = (this["dishName"] as? String).orEmpty(),
        submittedBy = (this["submittedBy"] as? String).orEmpty(),
        userEmail = (this["userEmail"] as? String).orEmpty(),
        userUID = (this["userUID"] as? String).orEmpty(),
        cuisine = (this["cuisine"] as? String).orEmpty(),
        country = (this["country"] as? String).orEmpty(),
        likesCount = (this["likesCount"] as? Number)?.toInt() ?: 0,
        reportCount = (this["reportCount"] as? Number)?.toInt() ?: 0,
        ingredients = (this["ingredients"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        recipeSteps = (this["recipeSteps"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        prepTime = (this["prepTime"] as? Number)?.toInt() ?: 0,
        difficulty = (this["difficulty"] as? String) ?: "Easy",
        chefTip = (this["chefTip"] as? String).orEmpty(),
        isAIGenerated = (this["isAIGenerated"] as? Boolean) ?: false,
        createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
        isApproved = (this["isApproved"] as? Boolean) ?: false,
        isBlocked = (this["isBlocked"] as? Boolean) ?: false
    )
}

/**
 * Converts a [CommunityDish] into a [Dish] instance for detail view and cooking history.
 */
fun CommunityDish.toDish(): Dish {
    return Dish(
        id = id,
        name = dishName,
        cuisine = CuisineType.fromString(cuisine),
        country = country,
        prepTimeMinutes = prepTime,
        difficulty = difficulty,
        ingredients = ingredients,
        recipeSteps = recipeSteps,
        chefTip = chefTip,
        isAIGenerated = isAIGenerated,
        submittedBy = submittedBy,
        likesCount = likesCount
    )
}
