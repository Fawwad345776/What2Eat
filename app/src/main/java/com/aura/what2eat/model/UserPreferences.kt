package com.aura.what2eat.model

data class UserPreferences(
    val wantToCook: List<String> = emptyList(),
    val dontWantToCook: List<String> = emptyList(),
    val dailyDaal: Boolean = false,
    val dailySabzi: Boolean = false,
    val dailyMeat: Boolean = false,
    val seasonalAI: Boolean = true,
    val antiRepetitionDays: Int = 30, // 30, 15, 7, or 0 (Allow Repetition)
    val country: String = "",
    val city: String = ""
)
