package com.aura.what2eat.model

data class CookedHistoryEntry(
    val dishName: String = "",
    val dishId: String = "",
    val mealType: String = "",
    val cookedDate: Long = 0L
)
