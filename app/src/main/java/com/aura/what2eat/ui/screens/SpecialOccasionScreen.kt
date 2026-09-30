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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.*
import com.aura.what2eat.service.ContextService
import com.aura.what2eat.ui.theme.*
import com.aura.what2eat.viewmodel.SpecialOccasionViewModel
import kotlinx.coroutines.launch

val OCCASION_PRESETS = listOf(
    "🌙 Eid ul Fitr",
    "🥩 Eid ul Azha",
    "🎂 Birthday Party",
    "💍 Anniversary Dinner",
    "🍲 Festive Dawat",
    "✨ General Celebration"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialOccasionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDishDetail: (Dish) -> Unit,
    onNavigateToWeeklyPlanner: () -> Unit,
    viewModel: SpecialOccasionViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager.getInstance(context) }

    val selectedOccasion by viewModel.selectedOccasion.collectAsStateWithLifecycle()
    val dishCount by viewModel.dishCount.collectAsStateWithLifecycle()
    val occasionDishes by viewModel.occasionDishes.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val skipsRemaining by viewModel.skipsRemaining.collectAsStateWithLifecycle()
    val showSkipLimitDialog by viewModel.showSkipLimitDialog.collectAsStateWithLifecycle()

    var appContext by remember { mutableStateOf(AppContext()) }

    LaunchedEffect(Unit) {
        appContext = ContextService.buildAppContext(context)
        viewModel.refreshSkips()
        // If today is an Islamic occasion and dishes not yet generated, preselect it!
        if (occasionDishes.isEmpty()) {
            if (appContext.isEidUlFitr) viewModel.setSelectedOccasion("🌙 Eid ul Fitr")
            else if (appContext.isEidUlAzha) viewModel.setSelectedOccasion("🥩 Eid ul Azha")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Special Occasion Menu",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Configuration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🎉", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "Celebrate with Recipes",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = DarkText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Traditional & festive celebratory menus",
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (skipsRemaining > 0) SecondaryEmerald.copy(alpha = 0.12f) else PrimaryOrange.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (skipsRemaining > 0) SecondaryEmerald else PrimaryOrange)
                            ) {
                                Text(
                                    text = "Skips: $skipsRemaining/3",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (skipsRemaining > 0) SecondaryEmerald else PrimaryOrange,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Occasion Chips
                        Text(
                            text = "Select Occasion:",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkText
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(OCCASION_PRESETS) { occasion ->
                                val isSelected = (selectedOccasion == occasion)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) GoldPro else BackgroundOffWhite,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GoldPro else CardBorder),
                                    modifier = Modifier.clickable { viewModel.setSelectedOccasion(occasion) }
                                ) {
                                    Text(
                                        text = occasion,
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = DarkText,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dish count [1..6]
                        Text(
                            text = "Number of Dishes:",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkText
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            (1..6).forEach { count ->
                                val isSelected = (dishCount == count)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PrimaryOrange else BackgroundOffWhite,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryOrange else CardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setDishCount(count) }
                                ) {
                                    Text(
                                        text = "$count",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) SurfaceWhite else DarkText,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                viewModel.generateSpecialMenu(appContext)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            enabled = !isGenerating
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(color = SurfaceWhite, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Curating Special Menu...",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = SurfaceWhite
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Celebration, contentDescription = null, tint = SurfaceWhite)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generate Special Menu",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = SurfaceWhite
                                )
                            }
                        }
                    }
                }
            }

            // Results
            if (occasionDishes.isNotEmpty()) {
                item {
                    Text(
                        text = "$selectedOccasion Menu",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkText
                    )
                }

                itemsIndexed(occasionDishes) { index, dish ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDishDetail(dish) },
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
                                    .size(40.dp)
                                    .background(GoldProContainer, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (index) {
                                        0 -> "⭐"
                                        occasionDishes.size - 1 -> "🍨"
                                        else -> "🍲"
                                    },
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dish.name,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = DarkText
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${dish.cuisine.displayName} • ${dish.prepTimeMinutes}m • ${dish.difficulty}",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "$selectedOccasion menu saved to Weekly Plan!", Toast.LENGTH_SHORT).show()
                            onNavigateToWeeklyPlanner()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = SurfaceWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save to Weekly Plan",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SurfaceWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showSkipLimitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSkipLimitDialog() },
            title = {
                Text(
                    text = "Daily Skips Limit Reached",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You have used your 3 free special occasion skips for today. Upgrade to PRO for unlimited menu generations, or come back tomorrow!",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    color = DarkText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (com.aura.what2eat.BuildConfig.DEBUG) {
                            viewModel.grantDebugSkips()
                            Toast.makeText(context, "Debug Mode: +3 Skips granted! 🎉", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.dismissSkipLimitDialog()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text(
                        text = if (com.aura.what2eat.BuildConfig.DEBUG) "Get +3 Debug Skips" else "OK",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSkipLimitDialog() }) {
                    Text(text = "Close", fontFamily = NunitoFontFamily)
                }
            }
        )
    }
}
