package com.aura.what2eat.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.what2eat.R
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.CommunityDish
import com.aura.what2eat.model.Dish
import com.aura.what2eat.service.FirebaseService
import com.aura.what2eat.service.LocationService
import com.aura.what2eat.ui.theme.BackgroundOffWhite
import com.aura.what2eat.ui.theme.CardBorder
import com.aura.what2eat.ui.theme.DarkText
import com.aura.what2eat.ui.theme.EmeraldGradientEnd
import com.aura.what2eat.ui.theme.EmeraldGradientStart
import com.aura.what2eat.ui.theme.GoldPro
import com.aura.what2eat.ui.theme.GoldProContainer
import com.aura.what2eat.ui.theme.NunitoFontFamily
import com.aura.what2eat.ui.theme.OrangeGradientEnd
import com.aura.what2eat.ui.theme.OrangeGradientStart
import com.aura.what2eat.ui.theme.PrimaryOrange
import com.aura.what2eat.ui.theme.SecondaryEmerald
import com.aura.what2eat.ui.theme.SurfaceWhite
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.what2eat.ui.LocalSubscriptionManager
import com.aura.what2eat.viewmodel.HomeViewModel
import com.aura.what2eat.ui.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMealDecider: () -> Unit,
    onNavigateToKitchenPreferences: () -> Unit,
    onNavigateToGuestPlanner: () -> Unit,
    onNavigateToSpecialOccasion: () -> Unit,
    onNavigateToWeeklyPlanner: () -> Unit,
    onNavigateToDineOut: () -> Unit,
    onNavigateToPantryMatcher: () -> Unit,
    onNavigateToCommunity: () -> Unit,
    onNavigateToAddSuggestion: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToCookedHistory: () -> Unit,
    onNavigateToDishDetail: (Dish) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val prefsManager = remember { PreferencesManager.getInstance(context) }
    val billingManager = LocalSubscriptionManager.current
    val isProUser by billingManager.isPro.collectAsStateWithLifecycle()
    val isWeeklyPlannerLocked = !com.aura.what2eat.BuildConfig.DEBUG && !isProUser

    val topSuggestions by viewModel.topSuggestions.collectAsStateWithLifecycle()
    val appContext by viewModel.appContext.collectAsStateWithLifecycle()

    var currentCity by remember { mutableStateOf("Karachi") }
    var currentCountry by remember { mutableStateOf("Pakistan") }
    var isLocationPickerOpen by remember { mutableStateOf(false) }

    var debugLocationInfo by remember {
        mutableStateOf(LocationService.getDebugLocationInfo(context))
    }

    val likedDishState = remember { mutableStateMapOf<String, Boolean>() }
    val likesCountOverrides = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(Unit) {
        prefsManager.getLikedDishIds().forEach { id ->
            likedDishState[id] = true
        }
    }

    LaunchedEffect(currentCity, currentCountry) {
        debugLocationInfo = LocationService.getDebugLocationInfo(context)
    }

    LaunchedEffect(Unit) {
        LocationService.locationChangedFlow.collect { (city, country) ->
            currentCity = city
            currentCountry = country
            debugLocationInfo = LocationService.getDebugLocationInfo(context)
        }
    }

    LaunchedEffect(appContext) {
        if (appContext.city.isNotBlank()) currentCity = appContext.city
        if (appContext.country.isNotBlank()) currentCountry = appContext.country
        debugLocationInfo = LocationService.getDebugLocationInfo(context)
    }

    // Dynamic country-aware community dishes (synchronized with CommunityScreen)
    val communityDishes: List<CommunityDish> = remember(topSuggestions, currentCountry) {
        val countryDishes = topSuggestions.filter {
            it.country.equals(currentCountry, ignoreCase = true) || it.country.isBlank()
        }
        val defaultDishes = FirebaseService.getDefaultCommunityDishes(currentCountry)
        val combined = mutableListOf<CommunityDish>()
        combined.addAll(countryDishes)
        combined.addAll(defaultDishes)
        combined.distinctBy { it.dishName.trim().lowercase() }
            .sortedByDescending { it.likesCount }
            .take(6)
    }

    var showExitDialog by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (isLocationPickerOpen) {
            isLocationPickerOpen = false
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000L) {
                showExitDialog = true
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            com.aura.what2eat.ui.components.AppDrawer(
                currentRoute = com.aura.what2eat.navigation.Screen.Home.route,
                onNavigate = { route ->
                    when (route) {
                        com.aura.what2eat.navigation.Screen.Home.route -> { /* already home */ }
                        com.aura.what2eat.navigation.Screen.MealDecider.route -> onNavigateToMealDecider()
                        com.aura.what2eat.navigation.Screen.GuestPlanner.route -> onNavigateToGuestPlanner()
                        com.aura.what2eat.navigation.Screen.SpecialOccasion.route -> onNavigateToSpecialOccasion()
                        com.aura.what2eat.navigation.Screen.WeeklyPlanner.route -> onNavigateToWeeklyPlanner()
                        com.aura.what2eat.navigation.Screen.DineOut.route -> onNavigateToDineOut()
                        com.aura.what2eat.navigation.Screen.PantryMatcher.route -> onNavigateToPantryMatcher()
                        com.aura.what2eat.navigation.Screen.Community.route -> onNavigateToCommunity()
                        com.aura.what2eat.navigation.Screen.KitchenPreferences.route -> onNavigateToKitchenPreferences()
                        com.aura.what2eat.navigation.Screen.CookedHistory.route -> onNavigateToCookedHistory()
                        com.aura.what2eat.navigation.Screen.Subscription.route -> onNavigateToSubscription()
                    }
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundOffWhite)
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = DarkText
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Location Chip
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.clickable { isLocationPickerOpen = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📍 $currentCity, $currentCountry ▾",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = DarkText
                                )
                            }
                        }
                    }

                    // PRO Gold Chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = GoldProContainer,
                        modifier = if (!isProUser) {
                            Modifier.clickable { onNavigateToSubscription() }
                        } else {
                            Modifier.clickable {
                                Toast.makeText(context, "VIP PRO Member Active 👑", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isProUser) "👑 PRO" else "👑 Upgrade",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = GoldPro
                            )
                        }
                    }
                }
            },
            containerColor = BackgroundOffWhite
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // DEBUG ONLY: Complete Location Details (Area, City, Country, Coordinates, Mode, GPS)
                if (com.aura.what2eat.BuildConfig.DEBUG) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🛠️", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DEBUG: Complete Location",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFE65100)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PrimaryOrange.copy(alpha = 0.12f),
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            viewModel.refreshLocation(context)
                                            debugLocationInfo = LocationService.getDebugLocationInfo(context)
                                            Toast.makeText(context, "Location refreshed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "🔄 Refresh GPS",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = PrimaryOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = debugLocationInfo.summaryText,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color.Black,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // 1. Kitchen Preferences Full-Width Card Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToKitchenPreferences() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(OrangeGradientStart, OrangeGradientEnd)
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🍽️", fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Kitchen Preferences",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = SurfaceWhite
                                    )
                                    Text(
                                        text = "Want to cook • Don't want to cook • Daily habits",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp,
                                        color = SurfaceWhite.copy(alpha = 0.9f)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open",
                                tint = SurfaceWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Hero Banner Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "What should we cook today?",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = DarkText
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val memoryDays = com.aura.what2eat.data.local.PreferencesManager.getInstance(context).getAntiRepetitionDays()
                            val memoryLabel = if (memoryDays > 0) "$memoryDays-day anti-repetition memory active" else "Repetition allowed (No memory restriction)"
                            Text(text = "✓", color = SecondaryEmerald, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = memoryLabel,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = SecondaryEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onNavigateToMealDecider,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                        ) {
                            Text(
                                text = "Decide Now →",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SurfaceWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Feature Grid (2 Columns, 3 Rows)
                Text(
                    text = "Kitchen Features",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DarkText
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🍲",
                        title = "What to Cook Today",
                        subtitle = "Smart AI decider",
                        onClick = onNavigateToMealDecider
                    )

                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        emoji = "👥",
                        title = "Guest Feast Planner",
                        subtitle = "1-7 Mains + Sweets",
                        onClick = onNavigateToGuestPlanner
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        emoji = "✨",
                        title = "Special Occasion",
                        subtitle = "Ramadan, Eid & Dawat",
                        onClick = onNavigateToSpecialOccasion
                    )

                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        emoji = "📅",
                        title = "Weekly Planner",
                        subtitle = "7-Day custom plan",
                        isPro = isWeeklyPlannerLocked,
                        onClick = onNavigateToWeeklyPlanner
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Row 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🛵",
                        title = "Dine Out",
                        subtitle = "Nearby restaurants",
                        onClick = onNavigateToDineOut
                    )

                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🥦",
                        title = "Pantry Matcher",
                        subtitle = "Match ingredients",
                        onClick = onNavigateToPantryMatcher
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Community Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Community Suggestions",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkText
                    )

                    Text(
                        text = "View All →",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PrimaryOrange,
                        modifier = Modifier.clickable { onNavigateToCommunity() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Add Suggestion Outlined Button
                OutlinedButton(
                    onClick = onNavigateToAddSuggestion,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Your Suggestion",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // LazyRow of Top Community Dish Cards
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(communityDishes) { dish ->
                        val isLiked = likedDishState[dish.id] ?: prefsManager.isDishLiked(dish.id)
                        val currentLikes = likesCountOverrides[dish.id] ?: dish.likesCount

                        CommunityDishCard(
                            dish = dish,
                            isLiked = isLiked,
                            likesCount = currentLikes,
                            onClick = {
                                onNavigateToDishDetail(dish.toDish())
                            },
                            onLikeToggle = {
                                val currentUser = com.aura.what2eat.auth.GoogleAuthManager.getCurrentUser()
                                if (currentUser == null || currentUser.isAnonymous) {
                                    Toast.makeText(context, "Please sign in to like dishes", Toast.LENGTH_SHORT).show()
                                    return@CommunityDishCard
                                }

                                val newLiked = prefsManager.toggleLikedDish(dish.id)
                                likedDishState[dish.id] = newLiked
                                val delta = if (newLiked) 1 else -1
                                val updatedCount = ((likesCountOverrides[dish.id] ?: dish.likesCount) + delta).coerceAtLeast(0)
                                likesCountOverrides[dish.id] = updatedCount

                                scope.launch {
                                    if (newLiked) {
                                        FirebaseService.likeSuggestion(dish.id, currentUser.uid)
                                    } else {
                                        FirebaseService.unlikeSuggestion(dish.id, currentUser.uid)
                                    }
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Location Picker Sheet Modal
    if (isLocationPickerOpen) {
        LocationPickerSheet(
            currentCity = currentCity,
            currentCountry = currentCountry,
            onDismiss = { isLocationPickerOpen = false },
            onSelectLocation = { city, country ->
                currentCity = city
                currentCountry = country
                LocationService.saveLocation(context, city, country)
                viewModel.refreshLocation(context)
                isLocationPickerOpen = false
            }
        )
    }

    // Two-times back press Exit Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                showExitDialog = false
                lastBackPressTime = 0L
            },
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "What2Eat Logo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            },
            title = {
                Text(
                    text = "Exit What2Eat?",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = DarkText,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to close the app?",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        (context as? Activity)?.finishAffinity()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Yes, Exit",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showExitDialog = false
                        lastBackPressTime = 0L
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No, Stay",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkText
                    )
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun FeatureCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    subtitle: String,
    isPro: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(125.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = emoji, fontSize = 28.sp)

                Column {
                    Text(
                        text = title,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DarkText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontFamily = NunitoFontFamily,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isPro) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    color = GoldProContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "👑 PRO",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        color = GoldPro,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CommunityDishCard(
    dish: CommunityDish,
    isLiked: Boolean,
    likesCount: Int,
    onClick: () -> Unit,
    onLikeToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = dish.dishName,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DarkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "by ${dish.submittedBy.ifBlank { "Home Chef" }} • ${dish.cuisine}",
                    fontFamily = NunitoFontFamily,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isLiked) "❤️" else "🤍",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$likesCount",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = DarkText
                    )
                }

                IconButton(
                    onClick = onLikeToggle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) PrimaryOrange else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
