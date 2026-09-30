package com.aura.what2eat.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.what2eat.model.*
import com.aura.what2eat.service.ContextService
import com.aura.what2eat.service.GeminiAIService
import com.aura.what2eat.ui.theme.*
import kotlinx.coroutines.launch

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.what2eat.ui.LocalSubscriptionManager
import com.aura.what2eat.viewmodel.WeeklyPlannerViewModel

val DAYS_OF_WEEK = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlannerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToDishDetail: (Dish) -> Unit,
    viewModel: WeeklyPlannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val billingManager = LocalSubscriptionManager.current

    // Access control: In Debug builds, Weekly Planner is accessible for development/testing.
    // In Release / Play Store builds, Weekly Planner is locked until What2Eat Pro is active.
    val isProFromBilling by billingManager.isPro.collectAsStateWithLifecycle()
    val isDebug = com.aura.what2eat.BuildConfig.DEBUG
    val isPlannerUnlocked = isDebug || isProFromBilling

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val savedWeeklyPlan by viewModel.weeklyPlan.collectAsStateWithLifecycle()
    val dayPreferences by viewModel.dayPreferences.collectAsStateWithLifecycle()

    var showPreferencesDialog by remember { mutableStateOf(false) }

    // 7 Days State - guaranteed to start with all 7 days filled with lunch and dinner
    val weekDays = remember {
        mutableStateListOf<DayPlan>().apply {
            addAll(WeeklyPlannerViewModel.DEFAULT_WEEK_DAYS)
        }
    }

    // Synchronize local UI state whenever savedWeeklyPlan updates from ViewModel
    LaunchedEffect(savedWeeklyPlan) {
        savedWeeklyPlan?.let { plan ->
            if (plan.days.isNotEmpty()) {
                weekDays.clear()
                weekDays.addAll(plan.days)
            }
        }
    }

    LaunchedEffect(Unit) {
        com.aura.what2eat.service.AnalyticsService.logWeeklyPlannerViewed()
    }

    // Modal Sheet State for Editing a Slot
    var editingSlot by remember { mutableStateOf<Triple<Int, String, Dish?>?>(null) } // (DayIndex, "Lunch"/"Dinner", currentDish)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Weekly Meal Planner",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundOffWhite)
            )
        },
        containerColor = BackgroundOffWhite
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Main 7-Day Plan List (Blurred/dimmed if not unlocked)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isPlannerUnlocked) 1f else 0.25f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Section Header with ONLY ONE Refresh icon button and Meal Preferences button
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "7-Day Meal Schedule",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = DarkText
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PrimaryOrange.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showPreferencesDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "⚙️ Preferences",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = PrimaryOrange
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Refresh button is strictly ONLY an icon
                            IconButton(
                                onClick = {
                                    if (isPlannerUnlocked) {
                                        viewModel.refreshAllSuggestions()
                                    } else {
                                        onNavigateToSubscription()
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.size(36.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryOrange
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh All Suggestions",
                                        tint = PrimaryOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Day Preferences Chips
                if (dayPreferences.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(dayPreferences.entries.toList()) { (key, food) ->
                                val parts = key.split("_")
                                val day = parts.getOrNull(0) ?: key
                                val meal = parts.getOrNull(1) ?: ""
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceWhite,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "$day $meal: $food",
                                            fontFamily = NunitoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = DarkText
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = TextMuted,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.removeDayPreference(day, meal) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 7 Day Cards
                itemsIndexed(weekDays) { index, dayPlan ->
                    DayPlanCard(
                        dayPlan = dayPlan,
                        onSlotClick = { slot ->
                            if (isPlannerUnlocked) {
                                editingSlot = Triple(index, slot, if (slot == "Lunch") dayPlan.lunch else dayPlan.dinner)
                            } else {
                                onNavigateToSubscription()
                            }
                        },
                        onToggleGuest = {
                            if (isPlannerUnlocked) {
                                viewModel.toggleGuestDay(index)
                            } else {
                                onNavigateToSubscription()
                            }
                        },
                        onToggleSpecial = {
                            if (isPlannerUnlocked) {
                                viewModel.toggleSpecialDay(index)
                            } else {
                                onNavigateToSubscription()
                            }
                        }
                    )
                }
            }

            // PRO Gate Lock Overlay (If not unlocked: strictly in release builds for non-pro users)
            if (!isPlannerUnlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(GoldProContainer, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "👑", fontSize = 32.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Unlock Weekly Meal Planner",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = DarkText,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Plan entire 7-day lunch & dinner menus and prevent cooking fatigue with AI-tailored meal schedules.",
                                fontFamily = NunitoFontFamily,
                                fontSize = 13.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = onNavigateToSubscription,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPro)
                            ) {
                                Text(
                                    text = "👑 Upgrade to PRO — Unlock Planner",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DarkText
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dish Picker / Editor Sheet
    editingSlot?.let { (dayIndex, slotName, currentDish) ->
        var dishInput by remember { mutableStateOf(currentDish?.name ?: "") }
        var isAiSuggesting by remember { mutableStateOf(false) }
        val sessionRejectedSuggestions = remember { mutableStateListOf<String>() }

        ModalBottomSheet(
            onDismissRequest = { editingSlot = null },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Edit ${weekDays.getOrNull(dayIndex)?.dayName ?: "Day"} ($slotName)",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DarkText
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dishInput,
                    onValueChange = { dishInput = it },
                    label = { Text("Dish Name", fontFamily = NunitoFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = CardBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // AI Suggest Dish Button
                OutlinedButton(
                    onClick = {
                        isAiSuggesting = true
                        scope.launch {
                            val candidate = viewModel.suggestDishForSlot(
                                dayIndex = dayIndex,
                                slotName = slotName,
                                currentDishName = dishInput.ifBlank { currentDish?.name.orEmpty() },
                                recentlySuggested = sessionRejectedSuggestions.toList()
                            )
                            isAiSuggesting = false
                            if (candidate.isNotBlank()) {
                                if (dishInput.isNotBlank() && dishInput !in sessionRejectedSuggestions) {
                                    sessionRejectedSuggestions.add(dishInput.trim())
                                }
                                dishInput = candidate
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAiSuggesting) {
                        CircularProgressIndicator(color = PrimaryOrange, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("✨ Auto-Suggest with AI", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val trimmed = dishInput.trim()
                        val newDish = if (trimmed.isNotBlank()) {
                            Dish(name = trimmed)
                        } else {
                            val currentName = currentDish?.name?.trim().orEmpty()
                            if (currentName.isNotBlank()) {
                                Dish(name = currentName)
                            } else {
                                val fallback = if (slotName == "Lunch") {
                                    WeeklyPlannerViewModel.DEFAULT_WEEK_DAYS.getOrNull(dayIndex)?.lunch
                                        ?: Dish(name = "Tadka Moong Daal with Rice")
                                } else {
                                    WeeklyPlannerViewModel.DEFAULT_WEEK_DAYS.getOrNull(dayIndex)?.dinner
                                        ?: Dish(name = "Chicken Karahi")
                                }
                                fallback
                            }
                        }
                        viewModel.updateDayMeal(dayIndex, slotName, newDish)
                        editingSlot = null
                        Toast.makeText(context, "Saved to meal plan", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Save to Meal Plan", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = SurfaceWhite)
                }
            }
        }
    }

    if (showPreferencesDialog) {
        DayPreferencesSheet(
            dayPreferences = dayPreferences,
            onSavePreference = { day, meal, food ->
                viewModel.setDayPreference(day, meal, food)
            },
            onRemovePreference = { day, meal ->
                viewModel.removeDayPreference(day, meal)
            },
            onDismiss = { showPreferencesDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPreferencesSheet(
    dayPreferences: Map<String, String>,
    onSavePreference: (day: String, meal: String, preferredFood: String) -> Unit,
    onRemovePreference: (day: String, meal: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDay by remember { mutableStateOf("Monday") }
    var selectedMeal by remember { mutableStateOf("Dinner") }
    var preferredInput by remember { mutableStateOf("") }

    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "🍽️ Day Meal Preferences",
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tell What2Eat what you prefer on specific days (e.g. Monday Dinner -> Pasta, Friday Lunch -> Rice). When planning meals, What2Eat will automatically follow your preferences!",
                fontFamily = NunitoFontFamily,
                fontSize = 12.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Active Preferences List
            if (dayPreferences.isNotEmpty()) {
                Text(
                    text = "Active Preferences:",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dayPreferences.entries.toList()) { (key, food) ->
                        val parts = key.split("_")
                        val day = parts.getOrNull(0) ?: key
                        val meal = parts.getOrNull(1) ?: ""
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BackgroundOffWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.3f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$day $meal: $food",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PrimaryOrange
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = TextMuted,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onRemovePreference(day, meal) }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Day Selector
            Text(text = "1. Select Day", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(days) { day ->
                    val isSelected = selectedDay == day
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PrimaryOrange else BackgroundOffWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryOrange else CardBorder),
                        modifier = Modifier.clickable { selectedDay = day }
                    ) {
                        Text(
                            text = day.take(3),
                            fontFamily = NunitoFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isSelected) SurfaceWhite else DarkText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meal Selector
            Text(text = "2. Select Meal", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Lunch", "Dinner").forEach { meal ->
                    val isSelected = selectedMeal == meal
                    val label = if (meal == "Lunch") "☀️ Lunch" else "🌙 Dinner / Night"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) (if (meal == "Lunch") PrimaryOrange else SecondaryEmerald) else BackgroundOffWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.clickable { selectedMeal = meal }
                    ) {
                        Text(
                            text = label,
                            fontFamily = NunitoFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isSelected) SurfaceWhite else DarkText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preferred Food Input
            Text(text = "3. Preferred Food", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = preferredInput,
                onValueChange = { preferredInput = it },
                placeholder = { Text("e.g. Pasta, Salad, Rice, Grilled Chicken...", fontFamily = NunitoFontFamily, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = CardBorder
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (preferredInput.isNotBlank()) {
                            onSavePreference(selectedDay, selectedMeal, preferredInput.trim())
                            preferredInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Save Preference", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.6f)
                ) {
                    Text(text = "Done", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = DarkText)
                }
            }
        }
    }
}

@Composable
fun DayPlanCard(
    dayPlan: DayPlan,
    onSlotClick: (slot: String) -> Unit,
    onToggleGuest: (Boolean) -> Unit,
    onToggleSpecial: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Day Header + Tag Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayPlan.dayName,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = DarkText
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (dayPlan.tag) {
                        "Guest Day" -> PrimaryOrange.copy(alpha = 0.12f)
                        "Friday Feast", "Special" -> GoldProContainer
                        else -> SecondaryEmerald.copy(alpha = 0.12f)
                    }
                ) {
                    Text(
                        text = dayPlan.tag,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = when (dayPlan.tag) {
                            "Guest Day" -> PrimaryOrange
                            "Friday Feast", "Special" -> GoldPro
                            else -> SecondaryEmerald
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lunch & Dinner Slots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Lunch Slot
                val lunchName = dayPlan.lunch?.name?.trim()?.ifBlank { "Tadka Moong Daal with Rice" } ?: "Tadka Moong Daal with Rice"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSlotClick("Lunch") },
                    shape = RoundedCornerShape(12.dp),
                    color = BackgroundOffWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "☀️ Lunch",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PrimaryOrange
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lunchName,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = DarkText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Dinner Slot
                val dinnerName = dayPlan.dinner?.name?.trim()?.ifBlank { "Chicken Karahi" } ?: "Chicken Karahi"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSlotClick("Dinner") },
                    shape = RoundedCornerShape(12.dp),
                    color = BackgroundOffWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "🌙 Dinner",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SecondaryEmerald
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dinnerName,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = DarkText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
