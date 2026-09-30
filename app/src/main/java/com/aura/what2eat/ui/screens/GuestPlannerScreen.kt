package com.aura.what2eat.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.*
import com.aura.what2eat.service.ContextService
import com.aura.what2eat.service.FirebaseService
import com.aura.what2eat.service.GeminiAIService
import com.aura.what2eat.ui.theme.*
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.what2eat.viewmodel.GuestPlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestPlannerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDishDetail: (Dish) -> Unit,
    onNavigateToWeeklyPlanner: () -> Unit,
    viewModel: GuestPlannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager.getInstance(context) }

    val feastDishes by viewModel.generatedMenu.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isLoading.collectAsStateWithLifecycle()
    val swappingIndex by viewModel.swappingIndex.collectAsStateWithLifecycle()
    val skipsRemaining by viewModel.skipsRemaining.collectAsStateWithLifecycle()
    val showSkipLimitDialog by viewModel.showSkipLimitDialog.collectAsStateWithLifecycle()

    var mainDishCount by remember { mutableIntStateOf(1) }
    var includeSweets by remember { mutableStateOf(true) }
    var sweetDishCount by remember { mutableIntStateOf(1) }
    var appContext by remember { mutableStateOf(AppContext()) }

    LaunchedEffect(Unit) {
        appContext = ContextService.buildAppContext(context)
        viewModel.refreshSkips()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Guest Feast Planner",
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
            // Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "👥", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "Guest Feast Planner",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = DarkText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Build balanced feast menu for guests",
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

                        // Main Dish Count Chips [1][2][3][4][5][6][7]
                        Text(
                            text = "Main Courses (Gravy, Rice, BBQ, Bread):",
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
                            (1..7).forEach { count ->
                                val isSelected = (mainDishCount == count)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PrimaryOrange else BackgroundOffWhite,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryOrange else CardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { mainDishCount = count }
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sweet dishes prompt [Yes][No]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🍨 Sweet Dishes & Desserts",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DarkText
                                )
                                Text(
                                    text = "Include delicious desserts & sweet treats",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            Switch(
                                checked = includeSweets,
                                onCheckedChange = { includeSweets = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryOrange,
                                    checkedTrackColor = OrangeGradientEnd
                                )
                            )
                        }

                        if (includeSweets) {
                            Spacer(modifier = Modifier.height(10.dp))
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

                        Spacer(modifier = Modifier.height(20.dp))

                        // Generate Feast Menu Button
                        Button(
                            onClick = {
                                viewModel.generateGuestMenu(
                                    mainCount = mainDishCount,
                                    sweetCount = if (includeSweets) sweetDishCount else 0,
                                    context = appContext
                                )
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
                                    text = "Designing Feast Menu...",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = SurfaceWhite
                                )
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = SurfaceWhite)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generate Feast Menu",
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

            // Results: Course-numbered cards
            if (feastDishes.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Curated Feast Menu (${feastDishes.size} Courses)",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap any dish for the full recipe, or tap 🔄 to swap for a different suggestion",
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                itemsIndexed(feastDishes) { index, dish ->
                    FeastCourseCard(
                        courseNumber = index + 1,
                        dish = dish,
                        isReplacing = (swappingIndex == index),
                        onClick = { onNavigateToDishDetail(dish) },
                        onReplace = { viewModel.swapDish(index, appContext) }
                    )
                }

                // Save to Weekly Plan Button
                item {
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.saveToWeeklyPlan()
                            Toast.makeText(context, "Feast menu saved to your Weekly Plan!", Toast.LENGTH_SHORT).show()
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
                    text = "You have used your 3 free guest feast skips for today. Upgrade to PRO for unlimited feast planning, or come back tomorrow!",
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

@Composable
fun FeastCourseCard(
    courseNumber: Int,
    dish: Dish,
    isReplacing: Boolean = false,
    onClick: () -> Unit,
    onReplace: () -> Unit
) {
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
            // Course Number Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(OrangeGradientStart, OrangeGradientEnd)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$courseNumber",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SurfaceWhite
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

            // Replace / Re-roll single dish button
            IconButton(
                onClick = onReplace,
                enabled = !isReplacing,
                modifier = Modifier.size(36.dp)
            ) {
                if (isReplacing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryOrange
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Replace Dish",
                        tint = PrimaryOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
