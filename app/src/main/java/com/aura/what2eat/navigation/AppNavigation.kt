package com.aura.what2eat.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aura.what2eat.model.Dish
import com.aura.what2eat.ui.screens.*

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.LocationSetup.route) {
            LocationSetupScreen(
                onContinueToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.LocationSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            var selectedDish by remember { mutableStateOf<Dish?>(null) }
            if (selectedDish != null) {
                DishDetailScreen(
                    dish = selectedDish!!,
                    onNavigateBack = { selectedDish = null }
                )
            } else {
                HomeScreen(
                    onNavigateToMealDecider = { navController.navigate(Screen.MealDecider.route) },
                    onNavigateToKitchenPreferences = { navController.navigate(Screen.KitchenPreferences.route) },
                    onNavigateToGuestPlanner = { navController.navigate(Screen.GuestPlanner.route) },
                    onNavigateToSpecialOccasion = { navController.navigate(Screen.SpecialOccasion.route) },
                    onNavigateToWeeklyPlanner = { navController.navigate(Screen.WeeklyPlanner.route) },
                    onNavigateToDineOut = { navController.navigate(Screen.DineOut.route) },
                    onNavigateToPantryMatcher = { navController.navigate(Screen.PantryMatcher.route) },
                    onNavigateToCommunity = { navController.navigate(Screen.Community.route) },
                    onNavigateToAddSuggestion = { navController.navigate(Screen.AddSuggestion.route) },
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) },
                    onNavigateToCookedHistory = { navController.navigate(Screen.CookedHistory.route) },
                    onNavigateToDishDetail = { dish -> selectedDish = dish }
                )
            }
        }

        composable(Screen.MealDecider.route) {
            var selectedDish by remember { mutableStateOf<Dish?>(null) }
            if (selectedDish != null) {
                DishDetailScreen(
                    dish = selectedDish!!,
                    onNavigateBack = { selectedDish = null }
                )
            } else {
                MealDeciderScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) },
                    onNavigateToDishDetail = { dish -> selectedDish = dish }
                )
            }
        }

        composable(Screen.Community.route) {
            var selectedDish by remember { mutableStateOf<Dish?>(null) }
            if (selectedDish != null) {
                DishDetailScreen(
                    dish = selectedDish!!,
                    onNavigateBack = { selectedDish = null }
                )
            } else {
                CommunityScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToAddSuggestion = { navController.navigate(Screen.AddSuggestion.route) },
                    onNavigateToDishDetail = { dish -> selectedDish = dish }
                )
            }
        }

        composable(Screen.AddSuggestion.route) {
            AddSuggestionScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) }
            )
        }

        composable(Screen.DishDetail.route) {
            DishDetailScreen(
                dish = Dish(
                    id = "sample_dish",
                    name = "Karachi Chicken Biryani",
                    cuisine = com.aura.what2eat.model.CuisineType.PAKISTANI,
                    country = "Pakistan",
                    ingredients = listOf("Basmati Rice", "Chicken", "Yogurt", "Onions", "Spices"),
                    recipeSteps = listOf("Marinate chicken.", "Parboil rice.", "Layer and dum cook for 25 mins.")
                ),
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.GuestPlanner.route) {
            var selectedDish by remember { mutableStateOf<Dish?>(null) }
            if (selectedDish != null) {
                DishDetailScreen(
                    dish = selectedDish!!,
                    onNavigateBack = { selectedDish = null }
                )
            } else {
                GuestPlannerScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToDishDetail = { dish -> selectedDish = dish },
                    onNavigateToWeeklyPlanner = { navController.navigate(Screen.WeeklyPlanner.route) }
                )
            }
        }

        composable(Screen.SpecialOccasion.route) {
            var selectedDish by remember { mutableStateOf<Dish?>(null) }
            if (selectedDish != null) {
                DishDetailScreen(
                    dish = selectedDish!!,
                    onNavigateBack = { selectedDish = null }
                )
            } else {
                SpecialOccasionScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToDishDetail = { dish -> selectedDish = dish },
                    onNavigateToWeeklyPlanner = { navController.navigate(Screen.WeeklyPlanner.route) }
                )
            }
        }

        composable(Screen.WeeklyPlanner.route) {
            var selectedDish by remember { mutableStateOf<Dish?>(null) }
            if (selectedDish != null) {
                DishDetailScreen(
                    dish = selectedDish!!,
                    onNavigateBack = { selectedDish = null }
                )
            } else {
                WeeklyPlannerScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) },
                    onNavigateToDishDetail = { dish -> selectedDish = dish }
                )
            }
        }

        composable(Screen.DineOut.route) {
            DineOutScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) }
            )
        }

        composable(Screen.PantryMatcher.route) {
            var selectedDish by remember { mutableStateOf<Dish?>(null) }
            if (selectedDish != null) {
                DishDetailScreen(
                    dish = selectedDish!!,
                    onNavigateBack = { selectedDish = null }
                )
            } else {
                PantryMatcherScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) },
                    onNavigateToDishDetail = { dish -> selectedDish = dish }
                )
            }
        }

        composable(Screen.KitchenPreferences.route) {
            KitchenPreferencesScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.CookedHistory.route) {
            var selectedDish by remember { mutableStateOf<Dish?>(null) }
            if (selectedDish != null) {
                DishDetailScreen(
                    dish = selectedDish!!,
                    onNavigateBack = { selectedDish = null }
                )
            } else {
                CookedHistoryScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToDishDetail = { dish -> selectedDish = dish }
                )
            }
        }
    }
}
