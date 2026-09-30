package com.aura.what2eat.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.CookedHistoryEntry
import com.aura.what2eat.model.Dish
import com.aura.what2eat.model.MealType
import com.aura.what2eat.service.FirebaseService
import com.aura.what2eat.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookedHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDishDetail: (Dish) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager.getInstance(context) }

    var historyEntries by remember { mutableStateOf<List<CookedHistoryEntry>>(emptyList()) }
    var showClearDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Load combined cloud and local history
        val cloudHistory = FirebaseService.getCookedHistory()
        if (cloudHistory.isNotEmpty()) {
            historyEntries = cloudHistory
        } else {
            // Local fallback history if offline
            historyEntries = prefsManager.getLocalCookedHistory()
        }
        isLoading = false
    }

    // Group history entries by formatted date string
    val groupedHistory = remember(historyEntries) {
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val todayStr = dateFormat.format(Date())
        val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
        val yesterdayStr = dateFormat.format(yesterdayCalendar.time)

        historyEntries.sortedByDescending { it.cookedDate }
            .groupBy { entry: CookedHistoryEntry ->
                val dateStr = dateFormat.format(Date(entry.cookedDate))
                when (dateStr) {
                    todayStr -> "Today"
                    yesterdayStr -> "Yesterday"
                    else -> dateStr
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "30-Day Cooking History",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (historyEntries.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear History",
                                tint = PrimaryOrange
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundOffWhite)
            )
        },
        containerColor = BackgroundOffWhite
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // 30-Day Anti-Repetition Info Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🛡️", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "30-Day Anti-Repetition Memory",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = DarkText
                                )
                                Text(
                                    text = "Dishes logged here are excluded from new AI suggestions for 30 days.",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryOrange)
                    }
                }
            } else if (historyEntries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(BackgroundOffWhite, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📜", fontSize = 36.sp)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No dishes cooked yet!",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "When you cook a meal from the decider or planner, tap 'Cooking This Today' to log it here.",
                                fontFamily = NunitoFontFamily,
                                fontSize = 13.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                // Grouped by Date
                groupedHistory.forEach { (dateHeader, entries) ->
                    item {
                        Text(
                            text = dateHeader,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = PrimaryOrange,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    items(entries) { entry ->
                        HistoryEntryCard(
                            entry = entry,
                            onClick = {
                                onNavigateToDishDetail(
                                    Dish(
                                        id = entry.dishId,
                                        name = entry.dishName,
                                        mealType = MealType.fromString(entry.mealType)
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Clear History Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Clear Cooking History?",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will clear your 30-day cooking history. What2eat will be able to suggest these dishes again immediately.",
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    color = DarkText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefsManager.clearLocalCookedHistory()
                        historyEntries = emptyList()
                        showClearDialog = false
                        Toast.makeText(context, "Cooking history cleared! 🧹", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Clear All", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", fontFamily = NunitoFontFamily)
                }
            }
        )
    }
}

@Composable
fun HistoryEntryCard(
    entry: CookedHistoryEntry,
    onClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = timeFormat.format(Date(entry.cookedDate))
    val mealTypeEnum = MealType.fromString(entry.mealType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(SecondaryEmerald.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (mealTypeEnum) {
                        MealType.BREAKFAST, MealType.SEHRI -> "🍳"
                        MealType.LUNCH -> "☀️"
                        MealType.EVENING_SNACKS -> "☕"
                        MealType.DINNER -> "🌙"
                        MealType.IFTARI -> "🌙"
                    },
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.dishName,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BackgroundOffWhite
                    ) {
                        Text(
                            text = mealTypeEnum.displayName,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = PrimaryOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "• Logged at $timeStr",
                        fontFamily = NunitoFontFamily,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
