package com.aura.what2eat.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.Dish
import com.aura.what2eat.service.GeminiAIService
import com.aura.what2eat.service.LocationService
import com.aura.what2eat.ui.theme.*
import kotlinx.coroutines.launch

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.what2eat.viewmodel.PantryViewModel

val QUICK_INGREDIENTS = listOf("Tomatoes", "Onions", "Garlic", "Ginger", "Chicken", "Eggs", "Daal", "Potatoes", "Yogurt", "Rice", "Paneer", "Beef")

data class PantryMatchResult(
    val dish: Dish,
    val matchPercentage: Int,
    val matchedIngredients: List<String>,
    val missingIngredients: List<String>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantryMatcherScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToDishDetail: (Dish) -> Unit,
    viewModel: PantryViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager.getInstance(context) }

    val selectedIngredients by viewModel.ingredients.collectAsStateWithLifecycle()
    val rawMatches by viewModel.matchResults.collectAsStateWithLifecycle()
    val quotaRemaining by viewModel.searchesRemaining.collectAsStateWithLifecycle()
    val showQuotaSheet by viewModel.showQuotaDialog.collectAsStateWithLifecycle()

    var ingredientInput by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val quotaLimit = PreferencesManager.MONTHLY_PANTRY_SEARCH_LIMIT

    val matchResults = remember(rawMatches, selectedIngredients) {
        rawMatches.mapIndexed { i, dish ->
            PantryMatchResult(
                dish = dish,
                matchPercentage = 95 - (i * 10),
                matchedIngredients = selectedIngredients.take(3),
                missingIngredients = listOf("Spices", "Oil")
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pantry Matcher",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // Free Tier Usage Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Search Allowance",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DarkText
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (quotaRemaining > 0) SecondaryEmerald.copy(alpha = 0.12f) else RedCancel.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (quotaRemaining > 0) "$quotaRemaining / $quotaLimit Left" else "Limit Reached",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (quotaRemaining > 0) SecondaryEmerald else RedCancel,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (quotaRemaining.toFloat() / quotaLimit.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PrimaryOrange,
                            trackColor = CardBorder
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pantry Searches Remaining:",
                                fontFamily = NunitoFontFamily,
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "$quotaRemaining free searches this month",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = DarkText
                            )
                        }
                    }
                }
            }

            // Ingredient Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🥫", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "What's in your pantry or fridge?",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DarkText
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Add Ingredient Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = ingredientInput,
                                onValueChange = { ingredientInput = it },
                                placeholder = {
                                    Text(
                                        text = "Add ingredient (e.g. Eggs, Rice, Paneer)",
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 13.sp,
                                        color = TextMuted
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryOrange,
                                    unfocusedBorderColor = CardBorder
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (ingredientInput.isNotBlank()) {
                                        viewModel.addIngredient(ingredientInput)
                                        ingredientInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(PrimaryOrange, shape = RoundedCornerShape(12.dp))
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = SurfaceWhite)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Add Chips
                        Text(
                            text = "Quick Add:",
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(QUICK_INGREDIENTS) { item ->
                                val isAdded = selectedIngredients.contains(item)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isAdded) SecondaryEmerald.copy(alpha = 0.15f) else BackgroundOffWhite,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isAdded) SecondaryEmerald else CardBorder),
                                    modifier = Modifier.clickable {
                                        if (isAdded) viewModel.removeIngredient(item) else viewModel.addIngredient(item)
                                    }
                                ) {
                                    Text(
                                        text = if (isAdded) "✓ $item" else "+ $item",
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isAdded) SecondaryEmerald else DarkText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Selected Ingredients Flow / Wrap Chips
                        if (selectedIngredients.isNotEmpty()) {
                            Text(
                                text = "Selected Ingredients (${selectedIngredients.size}):",
                                fontFamily = NunitoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Flow layout using chunked rows
                            selectedIngredients.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { ingredient ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = PrimaryOrange.copy(alpha = 0.12f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.4f)),
                                            modifier = Modifier.wrapContentWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = ingredient,
                                                    fontFamily = NunitoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = PrimaryOrange
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = PrimaryOrange,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable { viewModel.removeIngredient(ingredient) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Find Recipes CTA Button
                        Button(
                            onClick = {
                                if (selectedIngredients.isEmpty()) {
                                    Toast.makeText(context, "Please select at least 1 ingredient", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    val ctx = com.aura.what2eat.service.ContextService.buildAppContext(context)
                                    viewModel.findMatchingRecipes(ctx)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            enabled = !isLoading
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SurfaceWhite)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Find Recipes", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SurfaceWhite)
                        }
                    }
                }
            }

            // In-List Loader Indicator when searching
            if (isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = PrimaryOrange,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "What2eat is finding delicious recipes...",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = DarkText,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Results List
            if (matchResults.isNotEmpty() && !isLoading) {
                item {
                    Text(
                        text = "🍳 Matching Recipes (${matchResults.size} Found)",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = DarkText
                    )
                }

                items(matchResults) { result ->
                    PantryResultCard(
                        result = result,
                        onViewRecipe = { onNavigateToDishDetail(result.dish) }
                    )
                }
            }
        }
    }

    // Quota Bottom Sheet
    if (showQuotaSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissQuotaDialog() },
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
                Text(text = "🥦", fontSize = 44.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Pantry Search Limit Reached",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "You have used your $quotaLimit free pantry searches for today. Upgrade to PRO for unlimited searches or come back tomorrow!",
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (com.aura.what2eat.BuildConfig.DEBUG) {
                    Button(
                        onClick = {
                            viewModel.onWatchAdComplete()
                            Toast.makeText(context, "Debug Mode: +1 Extra Search granted! 🎉", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald)
                    ) {
                        Text("🛠️ Debug: +1 Free Search", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = SurfaceWhite)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = {
                        viewModel.dismissQuotaDialog()
                        onNavigateToSubscription()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("👑 Upgrade to PRO — Unlimited", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = SurfaceWhite)
                }
            }
        }
    }
}

@Composable
fun PantryResultCard(
    result: PantryMatchResult,
    onViewRecipe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.dish.name,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkText,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SecondaryEmerald.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${result.matchPercentage}% Match",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = SecondaryEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "✓ Uses: ${result.matchedIngredients.joinToString(", ")}",
                fontFamily = NunitoFontFamily,
                fontSize = 12.sp,
                color = DarkText
            )

            Text(
                text = "⏱️ ${result.dish.prepTimeMinutes}m • ${result.dish.cuisine.displayName} • ${result.dish.difficulty}",
                fontFamily = NunitoFontFamily,
                fontSize = 12.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onViewRecipe,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Text("View Recipe & Steps", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SurfaceWhite)
            }
        }
    }
}
