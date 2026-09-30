package com.aura.what2eat.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.AppContext
import com.aura.what2eat.model.CookedHistoryEntry
import com.aura.what2eat.model.CuisineType
import com.aura.what2eat.model.DayType
import com.aura.what2eat.model.Dish
import com.aura.what2eat.model.MealType
import com.aura.what2eat.service.ContextService
import com.aura.what2eat.service.FirebaseService
import com.aura.what2eat.service.GeminiAIService
import com.aura.what2eat.ui.theme.*
import com.aura.what2eat.viewmodel.MealDeciderViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDeciderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToDishDetail: (Dish) -> Unit,
    viewModel: MealDeciderViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager.getInstance(context) }

    var appContext by remember { mutableStateOf(AppContext()) }
    var selectedMealType by viewModel::selectedMealType
    var selectedDishCount by viewModel::selectedDishCount
    var selectedDayType by viewModel::selectedDayType
    var includeSweets by viewModel::includeSweets
    var sweetDishCount by viewModel::sweetDishCount

    // AI Suggestions State
    var isLoading by viewModel::isLoading
    var suggestionsList by viewModel::suggestionsList
    var currentDishIndex by viewModel::currentDishIndex
    var previouslySuggestedNames by viewModel::previouslySuggestedNames
    var swappingDishIndex by viewModel::swappingDishIndex

    // Quotas & Modals
    var skipsRemaining by viewModel::skipsRemaining
    var showSkipLimitSheet by viewModel::showSkipLimitSheet
    var confirmedDish by viewModel::confirmedDish
    var confirmCount by viewModel::confirmCount

    LaunchedEffect(Unit) {
        appContext = ContextService.buildAppContext(context)
        viewModel.refreshSkips()
        com.aura.what2eat.service.AnalyticsService.logMealDeciderUsed(
            cuisine = "All",
            mealType = selectedMealType.name,
            dishCount = selectedDishCount
        )
    }

    // Determine visible meal types based on Ramadan status (Evening Snacks removed)
    val availableMealTypes = remember(appContext.isRamadan) {
        if (appContext.isRamadan) {
            listOf(
                MealType.SEHRI,
                MealType.BREAKFAST,
                MealType.LUNCH,
                MealType.DINNER,
                MealType.IFTARI
            )
        } else {
            listOf(
                MealType.BREAKFAST,
                MealType.LUNCH,
                MealType.DINNER
            )
        }
    }

    BackHandler(enabled = suggestionsList.isNotEmpty()) {
        viewModel.clearSuggestions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Meal Decider",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (suggestionsList.isNotEmpty()) {
                            viewModel.clearSuggestions()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundOffWhite)
            )
        },
        containerColor = BackgroundOffWhite
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Debug AI Status Banner (Only in Debug Builds)
            if (com.aura.what2eat.BuildConfig.DEBUG) {
                val aiStatus by com.aura.what2eat.service.AiStatusService.currentStatus.collectAsState()
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (aiStatus.isWorking) SecondaryEmerald.copy(alpha = 0.12f) else RedCancel.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (aiStatus.isWorking) SecondaryEmerald.copy(alpha = 0.4f) else RedCancel.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = if (aiStatus.isWorking) "🟢" else "⚠️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Debug AI Status: ${aiStatus.message}",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (aiStatus.isWorking) SecondaryEmerald else RedCancel
                        )
                    }
                }
            }

            if (isLoading) {
                // Loading State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = PrimaryOrange,
                            modifier = Modifier.size(52.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "What2eat is thinking...",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Analyzing 30-day cooking history, season & regional cuisine",
                            fontFamily = NunitoFontFamily,
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (suggestionsList.isNotEmpty()) {
                // Multi-Dish / Single-Dish Meal Results View
                MealSuggestionsResultView(
                    dishes = suggestionsList,
                    skipsRemaining = skipsRemaining,
                    swappingDishIndex = swappingDishIndex,
                    onCookingDish = { dish ->
                        confirmedDish = dish
                        confirmCount++
                        scope.launch {
                            val entry = CookedHistoryEntry(
                                dishName = dish.name,
                                dishId = dish.id,
                                mealType = dish.mealType.name,
                                cookedDate = System.currentTimeMillis()
                            )
                            prefsManager.saveCookedHistoryLocally(entry)
                            FirebaseService.saveCookedDish(dish, dish.mealType)
                        }
                    },
                    onCookingAll = {
                        confirmedDish = suggestionsList.firstOrNull()
                        confirmCount++
                        scope.launch {
                            suggestionsList.forEach { d ->
                                val entry = CookedHistoryEntry(
                                    dishName = d.name,
                                    dishId = d.id,
                                    mealType = d.mealType.name,
                                    cookedDate = System.currentTimeMillis()
                                )
                                prefsManager.saveCookedHistoryLocally(entry)
                                FirebaseService.saveCookedDish(d, d.mealType)
                            }
                        }
                    },
                    onRefreshAll = {
                        if (skipsRemaining <= 0 && com.aura.what2eat.BuildConfig.DEBUG) {
                            prefsManager.grantExtraSkips(3)
                            skipsRemaining = prefsManager.getSkipsRemaining()
                            Toast.makeText(context, "Debug Mode: +3 Free suggestions granted! 🎉", Toast.LENGTH_SHORT).show()
                        }

                        if (skipsRemaining > 0) {
                            skipsRemaining = prefsManager.useSkip()
                            isLoading = true
                            scope.launch {
                                val userPrefs = prefsManager.loadUserPreferences()
                                val excluded30Days = FirebaseService.getDishesCooked30Days()
                                val currentNames = suggestionsList.map { it.name }
                                val allExcluded = (excluded30Days + previouslySuggestedNames + currentNames).distinct()

                                val result = GeminiAIService.suggestDishes(
                                    country = appContext.country,
                                    city = appContext.city,
                                    mealType = selectedMealType,
                                    dayType = selectedDayType,
                                    dishCount = selectedDishCount,
                                    sweetDishCount = if (includeSweets) sweetDishCount else 0,
                                    excludedDishes = allExcluded,
                                    wantToCook = userPrefs.wantToCook,
                                    dontWantToCook = userPrefs.dontWantToCook,
                                    season = appContext.season,
                                    isRamadan = appContext.isRamadan,
                                    occasion = appContext.islamicOccasion
                                )

                                isLoading = false
                                if (result.isNotEmpty()) {
                                    com.aura.what2eat.service.LocalAiStorageService.saveGeneratedDishesLocally(context, result)
                                    previouslySuggestedNames = (previouslySuggestedNames + currentNames).distinct().takeLast(50)
                                    suggestionsList = result
                                    currentDishIndex = 0
                                } else {
                                    Toast.makeText(context, "No more unique dishes found. Try adjusting preferences!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            showSkipLimitSheet = true
                        }
                    },
                    onSwapDish = { index ->
                        if (skipsRemaining <= 0 && com.aura.what2eat.BuildConfig.DEBUG) {
                            prefsManager.grantExtraSkips(3)
                            skipsRemaining = prefsManager.getSkipsRemaining()
                            Toast.makeText(context, "Debug Mode: +3 Free suggestions granted! 🎉", Toast.LENGTH_SHORT).show()
                        }

                        if (skipsRemaining > 0) {
                            skipsRemaining = prefsManager.useSkip()
                            swappingDishIndex = index
                            scope.launch {
                                try {
                                    val userPrefs = prefsManager.loadUserPreferences()
                                    val excluded30Days = FirebaseService.getDishesCooked30Days()
                                    val newSingleDish = GeminiAIService.suggestDishes(
                                        country = appContext.country,
                                        city = appContext.city,
                                        mealType = selectedMealType,
                                        dayType = selectedDayType,
                                        dishCount = 1,
                                        sweetDishCount = if (suggestionsList[index].cuisine == CuisineType.DESSERT) 1 else 0,
                                        excludedDishes = (excluded30Days + suggestionsList.map { it.name } + previouslySuggestedNames).distinct(),
                                        wantToCook = userPrefs.wantToCook,
                                        dontWantToCook = userPrefs.dontWantToCook,
                                        season = appContext.season,
                                        isRamadan = appContext.isRamadan,
                                        occasion = appContext.islamicOccasion
                                    ).firstOrNull()

                                    if (newSingleDish != null) {
                                        com.aura.what2eat.service.LocalAiStorageService.saveGeneratedDishesLocally(context, listOf(newSingleDish))
                                        previouslySuggestedNames = (previouslySuggestedNames + suggestionsList[index].name).distinct()
                                        val updated = suggestionsList.toMutableList()
                                        updated[index] = newSingleDish
                                        suggestionsList = updated
                                    }
                                } finally {
                                    swappingDishIndex = null
                                }
                            }
                        } else {
                            showSkipLimitSheet = true
                        }
                    },
                    onNavigateToDishDetail = onNavigateToDishDetail,
                    onReset = {
                        suggestionsList = emptyList()
                        currentDishIndex = 0
                        selectedDishCount = 1
                        selectedMealType = MealType.DINNER
                        swappingDishIndex = null
                    }
                )
            } else {
                // Step-by-Step Configuration Wizard

                // Step 1: Meal Time Tab Row
                Text(
                    text = "1. Select Meal Time",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkText
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableMealTypes) { mealType ->
                        val isSelected = (selectedMealType == mealType)
                        val isRamadanSpecial = (mealType == MealType.SEHRI || mealType == MealType.IFTARI)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PrimaryOrange else SurfaceWhite,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) PrimaryOrange else CardBorder
                            ),
                            modifier = Modifier.clickable { selectedMealType = mealType }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isRamadanSpecial) "🌙 ${mealType.displayName}" else mealType.displayName,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isSelected) SurfaceWhite else DarkText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Step 2: Dish Count Chips [1][2][3][4][5]
                Text(
                    text = "2. Number of Main Dishes",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..5).forEach { count ->
                        val isSelected = (selectedDishCount == count)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SecondaryEmerald else SurfaceWhite,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) SecondaryEmerald else CardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDishCount = count }
                        ) {
                            Text(
                                text = "$count",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isSelected) SurfaceWhite else DarkText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Step 3: Day Type Selection (Normal vs Special)
                Text(
                    text = "3. Cooking Occasion",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Normal Day Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedDayType = DayType.NORMAL },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedDayType == DayType.NORMAL) PrimaryOrange.copy(alpha = 0.08f) else SurfaceWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selectedDayType == DayType.NORMAL) 2.dp else 1.dp,
                            color = if (selectedDayType == DayType.NORMAL) PrimaryOrange else CardBorder
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🏠", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Normal Day",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DarkText
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Everyday homestyle cooking & comfort meals",
                                fontFamily = NunitoFontFamily,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Special Occasion Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedDayType = DayType.SPECIAL },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedDayType == DayType.SPECIAL) GoldPro.copy(alpha = 0.08f) else SurfaceWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selectedDayType == DayType.SPECIAL) 2.dp else 1.dp,
                            color = if (selectedDayType == DayType.SPECIAL) GoldPro else CardBorder
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "✨", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Special Day",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DarkText
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Feasts, dining treats & celebratory favorites",
                                fontFamily = NunitoFontFamily,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Step 4: Sweet Dishes Prompt
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🍨 Would you like sweet dishes too?",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DarkText
                                )
                                Text(
                                    text = "Include delicious desserts & sweet treats",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }

                            Switch(
                                checked = includeSweets,
                                onCheckedChange = { includeSweets = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryOrange, checkedTrackColor = OrangeGradientEnd)
                            )
                        }

                        if (includeSweets) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Dessert Count:",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = DarkText
                                )
                                (1..3).forEach { count ->
                                    val isSelected = (sweetDishCount == count)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) PrimaryOrange else BackgroundOffWhite,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable { sweetDishCount = count }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$count",
                                                fontFamily = NunitoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) SurfaceWhite else DarkText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Primary CTA Button
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            val userPrefs = prefsManager.loadUserPreferences()
                            val excluded30Days = FirebaseService.getDishesCooked30Days()

                            val result = GeminiAIService.suggestDishes(
                                country = appContext.country,
                                city = appContext.city,
                                mealType = selectedMealType,
                                dayType = selectedDayType,
                                dishCount = selectedDishCount,
                                sweetDishCount = if (includeSweets) sweetDishCount else 0,
                                excludedDishes = (excluded30Days + previouslySuggestedNames).distinct(),
                                wantToCook = userPrefs.wantToCook,
                                dontWantToCook = userPrefs.dontWantToCook,
                                season = appContext.season,
                                isRamadan = appContext.isRamadan,
                                occasion = appContext.islamicOccasion
                            )

                            isLoading = false
                            if (result.isNotEmpty()) {
                                com.aura.what2eat.service.LocalAiStorageService.saveGeneratedDishesLocally(context, result)
                                previouslySuggestedNames = (previouslySuggestedNames + result.map { it.name }).distinct().takeLast(30)
                                suggestionsList = result
                                currentDishIndex = 0
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = SurfaceWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Decide Meal with AI",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SurfaceWhite
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Skip Limit BottomSheet
    if (showSkipLimitSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSkipLimitSheet = false },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "⏭️", fontSize = 44.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (com.aura.what2eat.BuildConfig.DEBUG) "Debug Mode: Free Suggestions" else "Daily Skips Used Up!",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (com.aura.what2eat.BuildConfig.DEBUG)
                        "Debug mode active: Refill 3 more suggestions anytime for testing!"
                    else
                        "You have used all 3 free suggestions for today. Upgrade to What2Eat PRO for unlimited meal planning!",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (com.aura.what2eat.BuildConfig.DEBUG) {
                    Button(
                        onClick = {
                            prefsManager.grantExtraSkips(3)
                            skipsRemaining = prefsManager.getSkipsRemaining()
                            showSkipLimitSheet = false
                            Toast.makeText(context, "Debug Mode: +3 Suggestions granted! 🎉", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald)
                    ) {
                        Text(
                            text = "🛠️ Debug: Refill +3 Skips",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = SurfaceWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Upgrade Button
                Button(
                    onClick = {
                        showSkipLimitSheet = false
                        onNavigateToSubscription()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text(
                        text = "👑 Upgrade to PRO — Unlimited Skips",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceWhite
                    )
                }
            }
        }
    }

    // Recipe Dialog BottomSheet (After confirming dish)
    confirmedDish?.let { dish ->
        val viewsUsed = prefsManager.getMonthlyRecipeViews()
        val viewsRemaining = (PreferencesManager.FREE_RECIPE_VIEW_LIMIT - viewsUsed).coerceAtLeast(0)

        ModalBottomSheet(
            onDismissRequest = { confirmedDish = null },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "👨", fontSize = 44.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Do you know how to cook this?",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dish.name,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = PrimaryOrange
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recipe views remaining: $viewsRemaining of ${PreferencesManager.FREE_RECIPE_VIEW_LIMIT} this month",
                    fontFamily = NunitoFontFamily,
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // View Full Recipe Button
                Button(
                    onClick = {
                        val canView = prefsManager.useRecipeView()
                        if (canView) {
                            confirmedDish = null
                            onNavigateToDishDetail(dish)
                        } else {
                            Toast.makeText(context, "Monthly recipe limit reached! Upgrade to PRO for 10 recipes.", Toast.LENGTH_LONG).show()
                            onNavigateToSubscription()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = SurfaceWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View Full Recipe",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceWhite
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // I Know How to Cook Button
                OutlinedButton(
                    onClick = {
                        confirmedDish = null
                        Toast.makeText(context, "Great! Added to your cooking history. Happy cooking! 🍳", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryEmerald)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SecondaryEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I Know How to Cook!",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryEmerald
                    )
                }
            }
        }
    }
}

@Composable
fun MealSuggestionsResultView(
    dishes: List<Dish>,
    skipsRemaining: Int,
    swappingDishIndex: Int? = null,
    onCookingDish: (Dish) -> Unit,
    onCookingAll: () -> Unit,
    onRefreshAll: () -> Unit,
    onSwapDish: (Int) -> Unit,
    onNavigateToDishDetail: (Dish) -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Results Header
        Text(
            text = if (dishes.size > 1) "Your Suggested Meal (${dishes.size} Dishes)" else "Your Meal Suggestion",
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = DarkText
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tap any dish for the full recipe, or swap with 🔄",
                fontFamily = NunitoFontFamily,
                fontSize = 12.sp,
                color = TextMuted
            )

            Text(
                text = "Skips: $skipsRemaining/3",
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (skipsRemaining > 0) SecondaryEmerald else PrimaryOrange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dish Cards List
        dishes.forEachIndexed { index, dish ->
            val isDessert = dish.cuisine == CuisineType.DESSERT
            val isSpecial = dish.isSpecial || dish.dayType == DayType.SPECIAL || com.aura.what2eat.service.LocalFallbackService.isSpecialDish(dish.name)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onNavigateToDishDetail(dish) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge / Emoji
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    if (isDessert) listOf(EmeraldGradientStart, EmeraldGradientEnd)
                                    else if (isSpecial) listOf(OrangeGradientStart, GoldPro)
                                    else listOf(OrangeGradientStart, OrangeGradientEnd)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isDessert) "🍨" else "${index + 1}",
                            fontSize = if (isDessert) 22.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SurfaceWhite
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = dish.name,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DarkText,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Category & Occasion Badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSpecial) GoldPro.copy(alpha = 0.15f) else SecondaryEmerald.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isSpecial) "✨ Special" else "🏠 Normal",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSpecial) Color(0xFFC78500) else SecondaryEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (dish.tags.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BackgroundOffWhite
                                ) {
                                    Text(
                                        text = dish.tags.first(),
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱️ ${dish.prepTimeMinutes}m",
                                fontFamily = NunitoFontFamily,
                                fontSize = 12.sp,
                                color = TextMuted
                            )

                            Text(
                                text = "• ${dish.cuisine.displayName}",
                                fontFamily = NunitoFontFamily,
                                fontSize = 12.sp,
                                color = PrimaryOrange,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = "• ${dish.difficulty}",
                                fontFamily = NunitoFontFamily,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Swap / Re-roll single dish button
                    val isSwappingThis = (swappingDishIndex == index)
                    IconButton(
                        onClick = { onSwapDish(index) },
                        enabled = (swappingDishIndex == null),
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isSwappingThis) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryOrange
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Swap Dish",
                                tint = PrimaryOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Cooking Button
        Button(
            onClick = onCookingAll,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald)
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SurfaceWhite)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (dishes.size > 1) "Cooking This Meal Today!" else "Cooking This Today!",
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = SurfaceWhite
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Reset / Decide Again Button
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Change Settings & Decide Again",
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
