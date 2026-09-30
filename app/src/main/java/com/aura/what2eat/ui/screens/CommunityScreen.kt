package com.aura.what2eat.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.what2eat.auth.GoogleAuthManager
import com.aura.what2eat.model.CommunityDish
import com.aura.what2eat.model.Dish
import com.aura.what2eat.model.toDish
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.service.FirebaseService
import com.aura.what2eat.service.LocationService
import com.aura.what2eat.ui.components.ReportBottomSheet
import com.aura.what2eat.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddSuggestion: () -> Unit,
    onNavigateToDishDetail: (dish: Dish) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager.getInstance(context) }
    val savedLocation = remember { LocationService.getSavedLocation(context) }
    val currentCountry = savedLocation.second

    val dishesFlow = remember { FirebaseService.observeSuggestions() }
    val dishes by dishesFlow.collectAsState(initial = emptyList())

    var reportingDish by remember { mutableStateOf<CommunityDish?>(null) }
    val likedDishes = remember { mutableStateMapOf<String, Boolean>() }
    val localLikesCount = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(Unit) {
        prefsManager.getLikedDishIds().forEach { id ->
            likedDishes[id] = true
        }
    }

    // Directly displays added suggestions in real-time, with most liked recipes always visible at top
    val displayDishes: List<CommunityDish> = remember(dishes, currentCountry) {
        val defaultDishes = FirebaseService.getDefaultCommunityDishes(currentCountry)
        val combined = mutableListOf<CommunityDish>()
        // 1. All real community dishes submitted by users
        combined.addAll(dishes)
        // 2. Default dishes if not already added by user
        for (defaultDish in defaultDishes) {
            if (combined.none { it.dishName.trim().equals(defaultDish.dishName.trim(), ignoreCase = true) }) {
                combined.add(defaultDish)
            }
        }
        // Most liked dishes always visible at the top
        combined.distinctBy { it.id.ifBlank { it.dishName.trim().lowercase() } }
            .sortedByDescending { it.likesCount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Community Dishes",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddSuggestion,
                containerColor = PrimaryOrange,
                contentColor = SurfaceWhite,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Dish", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = BackgroundOffWhite
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🏆 Top Ranked Community Recipes",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DarkText
                        )
                        Text(
                            text = "Voted and loved by passionate home chefs in $currentCountry. Tap any recipe for full ingredients and steps!",
                            fontFamily = NunitoFontFamily,
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            itemsIndexed(displayDishes) { index, dish ->
                RankedDishItem(
                    rank = index + 1,
                    dish = dish,
                    isLiked = likedDishes[dish.id] ?: false,
                    likesCount = localLikesCount[dish.id] ?: dish.likesCount,
                    onItemClick = {
                        val dishModel = dish.toDish()
                        onNavigateToDishDetail(dishModel)
                    },
                    onLikeClick = {
                        val currentUser = GoogleAuthManager.getCurrentUser()
                        if (currentUser == null || currentUser.isAnonymous) {
                            Toast.makeText(context, "Please sign in to like recipes", Toast.LENGTH_SHORT).show()
                            return@RankedDishItem
                        }

                        val isLikedNow = prefsManager.toggleLikedDish(dish.id)
                        likedDishes[dish.id] = isLikedNow
                        val delta = if (isLikedNow) 1 else -1
                        val updated = ((localLikesCount[dish.id] ?: dish.likesCount) + delta).coerceAtLeast(0)
                        localLikesCount[dish.id] = updated

                        scope.launch {
                            if (isLikedNow) {
                                FirebaseService.likeSuggestion(dish.id, currentUser.uid)
                            } else {
                                FirebaseService.unlikeSuggestion(dish.id, currentUser.uid)
                            }
                        }
                    },
                    onReportClick = {
                        reportingDish = dish
                    }
                )
            }
        }
    }

    // Report Bottom Sheet
    reportingDish?.let { dish ->
        ReportBottomSheet(
            dishId = dish.id,
            dishName = dish.dishName,
            onDismiss = { reportingDish = null },
            onSubmitReport = { reason ->
                scope.launch {
                    val uid = com.aura.what2eat.auth.GoogleAuthManager.getCurrentUser()?.uid
                        ?: "anon_${android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: System.currentTimeMillis()}"
                    FirebaseService.reportSuggestion(dish.id, uid, reason)
                    Toast.makeText(context, "Report submitted. Thank you for keeping What2Eat safe!", Toast.LENGTH_SHORT).show()
                }
                reportingDish = null
            }
        )
    }
}

@Composable
fun RankedDishItem(
    rank: Int,
    dish: CommunityDish,
    isLiked: Boolean,
    likesCount: Int = dish.likesCount,
    onItemClick: () -> Unit,
    onLikeClick: () -> Unit,
    onReportClick: () -> Unit
) {
    val rankBadgeColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> CardBorder      // Grey
    }

    val rankTextColor = when (rank) {
        1, 2, 3 -> DarkText
        else -> TextMuted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(rankBadgeColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = rankTextColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Dish Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = dish.dishName,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DarkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${dish.submittedBy.ifBlank { "HomeChef" }}",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = PrimaryOrange
                    )

                    Text(
                        text = " • ${dish.cuisine}",
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            // Actions: Like + Report
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Like button
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) PrimaryOrange else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "$likesCount",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DarkText
                )

                IconButton(
                    onClick = onReportClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Report",
                        tint = TextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
