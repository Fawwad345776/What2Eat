package com.aura.what2eat.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.CookedHistoryEntry
import com.aura.what2eat.model.Dish
import com.aura.what2eat.model.MealType
import com.aura.what2eat.service.FirebaseService
import com.aura.what2eat.service.RECIPE_SUPPORTED_LANGUAGES
import com.aura.what2eat.service.RecipeTranslationService
import com.aura.what2eat.service.SupportedLanguage
import com.aura.what2eat.service.TranslatedRecipe
import com.aura.what2eat.ui.components.ReportBottomSheet
import com.aura.what2eat.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishDetailScreen(
    dish: Dish,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager.getInstance(context) }

    var activeDish by remember(dish) { mutableStateOf(dish) }
    var isFetchingRecipe by remember(dish.id) { mutableStateOf(false) }

    var isLiked by remember(dish.id) { mutableStateOf(prefsManager.isDishLiked(dish.id)) }
    var likesCount by remember(dish.id) { mutableIntStateOf(dish.likesCount) }
    var isReportSheetOpen by remember { mutableStateOf(false) }
    var isCookedRecorded by remember { mutableStateOf(false) }

    // Multi-Language Translation State
    var selectedLanguage by remember { mutableStateOf(RECIPE_SUPPORTED_LANGUAGES.first()) }
    var isTranslating by remember { mutableStateOf(false) }
    var translatedRecipe by remember { mutableStateOf<TranslatedRecipe?>(null) }
    var isLanguageDropdownOpen by remember { mutableStateOf(false) }

    // Dynamic Display Properties (Translated or Original English)
    val displayDishName = translatedRecipe?.dishName?.ifBlank { activeDish.name } ?: activeDish.name
    val displayIngredients = if (translatedRecipe != null && translatedRecipe!!.ingredients.isNotEmpty()) translatedRecipe!!.ingredients else activeDish.ingredients
    val displaySteps = if (translatedRecipe != null && translatedRecipe!!.steps.isNotEmpty()) translatedRecipe!!.steps else activeDish.recipeSteps
    val displayChefTip = translatedRecipe?.chefTip?.ifBlank { activeDish.chefTip } ?: activeDish.chefTip

    // Fetch full recipe via Groq Cloud API & Firestore cache if steps are empty
    LaunchedEffect(dish.name) {
        if (activeDish.recipeSteps.isEmpty()) {
            isFetchingRecipe = true
            try {
                val generated = com.aura.what2eat.service.GroqAIService.generateRecipe(
                    dishName = activeDish.name,
                    cuisine = activeDish.cuisine.displayName,
                    country = activeDish.country
                )
                if (generated != null && generated.recipeSteps.isNotEmpty()) {
                    activeDish = generated.copy(
                        id = if (activeDish.id.isNotBlank()) activeDish.id else generated.id,
                        likesCount = activeDish.likesCount,
                        submittedBy = activeDish.submittedBy
                    )
                    com.aura.what2eat.service.LocalAiStorageService.saveGeneratedRecipeLocally(context, activeDish)
                }
            } catch (e: Exception) {
                // Keep activeDish
            } finally {
                isFetchingRecipe = false
            }
        } else {
            com.aura.what2eat.service.LocalAiStorageService.saveGeneratedRecipeLocally(context, activeDish)
        }
    }

    // Dynamic Translation Trigger
    LaunchedEffect(selectedLanguage, activeDish.name, activeDish.recipeSteps) {
        if (activeDish.recipeSteps.isNotEmpty()) {
            if (selectedLanguage.code == "en") {
                translatedRecipe = null
                isTranslating = false
            } else {
                isTranslating = true
                try {
                    val result = RecipeTranslationService.translateRecipe(activeDish, selectedLanguage)
                    translatedRecipe = result
                } catch (e: Exception) {
                    // Fallback to original
                } finally {
                    isTranslating = false
                }
            }
        }
    }

    // Synchronize latest likes directly from Firestore for community dishes
    LaunchedEffect(dish.id) {
        if (dish.id.isNotBlank()) {
            isLiked = prefsManager.isDishLiked(dish.id)
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("community_dishes")
                    .document(dish.id)
                    .get()
                    .await()
                if (doc.exists()) {
                    val count = doc.getLong("likesCount")?.toInt()
                    if (count != null) {
                        likesCount = count
                    }
                }
            } catch (e: Exception) {
                // Keep dish.likesCount fallback
            }
        }
    }

    // Checked state for ingredients
    val checkedIngredients = remember { mutableStateMapOf<Int, Boolean>() }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recipe Details",
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
                actions = {
                    IconButton(onClick = { isReportSheetOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Report",
                            tint = TextMuted
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
            // Debug AI Status Banner
            if (com.aura.what2eat.BuildConfig.DEBUG) {
                val aiStatus by com.aura.what2eat.service.AiStatusService.currentStatus.collectAsState()
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (aiStatus.isWorking) SecondaryEmerald.copy(alpha = 0.12f) else RedCancel.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (aiStatus.isWorking) SecondaryEmerald.copy(alpha = 0.4f) else RedCancel.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = if (aiStatus.isWorking) "🟢" else "⚠️", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Debug AI Status: ${aiStatus.message}",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = if (aiStatus.isWorking) SecondaryEmerald else RedCancel
                        )
                    }
                }
            }

            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Badge row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (activeDish.isAIGenerated) PrimaryOrange.copy(alpha = 0.1f) else SecondaryEmerald.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = if (activeDish.isAIGenerated) "✨ AI Generated" else "👨 Recipe by ${activeDish.submittedBy ?: "Community Chef"}",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (activeDish.isAIGenerated) PrimaryOrange else SecondaryEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Like Action Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val currentUser = com.aura.what2eat.auth.GoogleAuthManager.getCurrentUser()
                                if (currentUser == null || currentUser.isAnonymous) {
                                    Toast.makeText(context, "Please sign in to like dishes", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }

                                val newLiked = prefsManager.toggleLikedDish(activeDish.id)
                                isLiked = newLiked
                                likesCount = (likesCount + (if (newLiked) 1 else -1)).coerceAtLeast(0)
                                scope.launch {
                                    if (newLiked) {
                                        FirebaseService.likeSuggestion(activeDish.id, currentUser.uid)
                                    } else {
                                        FirebaseService.unlikeSuggestion(activeDish.id, currentUser.uid)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) PrimaryOrange else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$likesCount",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DarkText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dish Title (Translated or Original)
                    Text(
                        text = displayDishName,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = DarkText
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chips: Cuisine, Time, Difficulty, Servings
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BackgroundOffWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(
                                text = "🌍 ${activeDish.cuisine.displayName}",
                                fontFamily = NunitoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BackgroundOffWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(
                                text = "⏱️ ${activeDish.prepTimeMinutes} mins",
                                fontFamily = NunitoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BackgroundOffWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(
                                text = "🔥 ${activeDish.difficulty}",
                                fontFamily = NunitoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Language Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🌐", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Recipe Language",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DarkText
                            )
                            Text(
                                text = if (isTranslating) "Translating recipe..." else "${selectedLanguage.flag} ${selectedLanguage.displayName}",
                                fontFamily = NunitoFontFamily,
                                fontSize = 12.sp,
                                color = if (isTranslating) PrimaryOrange else TextMuted
                            )
                        }
                    }

                    Box {
                        OutlinedButton(
                            onClick = { isLanguageDropdownOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (isTranslating) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = PrimaryOrange)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "${selectedLanguage.flag} Change ▾",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        DropdownMenu(
                            expanded = isLanguageDropdownOpen,
                            onDismissRequest = { isLanguageDropdownOpen = false },
                            modifier = Modifier.background(SurfaceWhite)
                        ) {
                            RECIPE_SUPPORTED_LANGUAGES.forEach { lang ->
                                val isSelected = lang.code == selectedLanguage.code
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${lang.flag} ${lang.displayName}",
                                            fontFamily = NunitoFontFamily,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) PrimaryOrange else DarkText
                                        )
                                    },
                                    onClick = {
                                        selectedLanguage = lang
                                        isLanguageDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ingredients Checklist Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛒 Ingredients Checklist",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DarkText
                        )
                        if (isTranslating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryOrange)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isFetchingRecipe && displayIngredients.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryOrange
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Loading authentic ingredients...",
                                fontFamily = NunitoFontFamily,
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    } else {
                        displayIngredients.forEachIndexed { index, ingredient ->
                            val isChecked = checkedIngredients[index] ?: false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { checkedIngredients[index] = !isChecked }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checkedIngredients[index] = it },
                                    colors = CheckboxDefaults.colors(checkedColor = SecondaryEmerald)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ingredient,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isChecked) TextMuted else DarkText,
                                    style = if (isChecked) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step-by-Step Cooking Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📝 Step-by-Step Instructions",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DarkText
                        )
                        if (isTranslating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryOrange)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isFetchingRecipe && displaySteps.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryOrange
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Preparing complete step-by-step recipe...",
                                fontFamily = NunitoFontFamily,
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    } else {
                        displaySteps.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(PrimaryOrange, shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SurfaceWhite
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = step,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = DarkText,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Chef Tip Card (if present)
            if (displayChefTip.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldProContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "💡", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chef's Pro Tip",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = displayChefTip,
                                fontFamily = NunitoFontFamily,
                                fontSize = 13.sp,
                                color = DarkText,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cooked Confirmation Button (Records to 30-day anti-repetition memory)
            Button(
                onClick = {
                    isCookedRecorded = true
                    scope.launch {
                        val entry = CookedHistoryEntry(
                            dishName = activeDish.name,
                            dishId = activeDish.id,
                            mealType = activeDish.mealType.name,
                            cookedDate = System.currentTimeMillis()
                        )
                        PreferencesManager.getInstance(context).saveCookedHistoryLocally(entry)
                        FirebaseService.saveCookedDish(activeDish, activeDish.mealType)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCookedRecorded) SecondaryEmerald else PrimaryOrange
                )
            ) {
                Text(
                    text = if (isCookedRecorded) "✓ Added to Cooking History (30-Day Memory)" else "✅ Cooking This Meal Today!",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SurfaceWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (isReportSheetOpen) {
        ReportBottomSheet(
            dishId = activeDish.id,
            dishName = activeDish.name,
            onDismiss = { isReportSheetOpen = false },
            onSubmitReport = { reason ->
                scope.launch {
                    val uid = com.aura.what2eat.auth.GoogleAuthManager.getCurrentUser()?.uid
                        ?: "anon_${android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: System.currentTimeMillis()}"
                    FirebaseService.reportSuggestion(activeDish.id, uid, reason)
                }
                isReportSheetOpen = false
            }
        )
    }
}
