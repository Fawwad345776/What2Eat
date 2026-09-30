package com.aura.what2eat.model

data class DayPlan(
    val dayName: String = "",
    val date: Long = 0L,
    val lunch: Dish? = null,
    val dinner: Dish? = null,
    val isGuestDay: Boolean = false,
    val isSpecialDay: Boolean = false,
    val isRoutineDay: Boolean = true,
    val tag: String = "Routine"
)

data class RoutineRule(
    val id: String = "",
    val dayOfWeek: String = "Monday", // Monday, Tuesday, etc.
    val mealSlot: String = "Dinner",   // Lunch, Dinner, Both
    val ruleDescription: String = "Daal / Lentils"
)

data class WeeklyPlan(
    val weekStartDate: Long = 0L,
    val days: List<DayPlan> = emptyList(),
    val routineRules: List<RoutineRule> = emptyList()
)
