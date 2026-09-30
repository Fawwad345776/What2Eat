package com.aura.what2eat.model

enum class MealType(val displayName: String) {
    SEHRI("Sehri"),
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    EVENING_SNACKS("Evening Snacks"),
    DINNER("Dinner"),
    IFTARI("Iftari");

    companion object {
        fun fromString(value: String?): MealType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: DINNER
        }
    }
}

enum class DayType(val displayName: String) {
    NORMAL("Normal"),
    SPECIAL("Special");

    companion object {
        fun fromString(value: String?): DayType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: NORMAL
        }
    }
}

enum class CuisineType(val displayName: String) {
    PAKISTANI("Pakistani"),
    INDIAN("Indian"),
    BANGLADESHI("Bangladeshi"),
    CHINESE("Chinese"),
    ITALIAN("Italian"),
    CONTINENTAL("Continental"),
    STREET_FOOD("Street Food"),
    DESSERT("Dessert"),
    OTHER("Other");

    companion object {
        fun fromString(value: String?): CuisineType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: OTHER
        }
    }
}
