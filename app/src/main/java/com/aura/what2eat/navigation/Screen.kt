package com.aura.what2eat.navigation

sealed class Screen(val route: String, val title: String) {
    data object Splash : Screen("splash", "What2Eat")
    data object LocationSetup : Screen("locationSetup", "Location Setup")
    data object Home : Screen("home", "Home")
    data object MealDecider : Screen("mealDecider", "AI Meal Decider")
    data object Community : Screen("community", "Community Dishes")
    data object AddSuggestion : Screen("addSuggestion", "Add Suggestion")
    data object GuestPlanner : Screen("guestPlanner", "Guest Feast Planner")
    data object SpecialOccasion : Screen("specialOccasion", "Special Occasion")
    data object WeeklyPlanner : Screen("weeklyPlanner", "Weekly Meal Planner")
    data object DineOut : Screen("dineOut", "Dine Out Nearby")
    data object PantryMatcher : Screen("pantryMatcher", "Pantry Matcher")
    data object KitchenPreferences : Screen("kitchenPreferences", "Kitchen Preferences")
    data object Subscription : Screen("subscription", "What2Eat PRO")
    data object CookedHistory : Screen("cookedHistory", "Cooking History")
    data object DishDetail : Screen("dishDetail", "Recipe Details")
    data object SideDrawer : Screen("sideDrawer", "Menu")
}
