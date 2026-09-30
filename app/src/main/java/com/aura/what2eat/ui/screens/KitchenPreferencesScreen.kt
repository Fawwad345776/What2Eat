package com.aura.what2eat.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.UserPreferences
import com.aura.what2eat.service.FirebaseService
import com.aura.what2eat.service.GeminiAIService
import com.aura.what2eat.service.LocationService
import com.aura.what2eat.ui.theme.*
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.what2eat.viewmodel.PreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KitchenPreferencesScreen(
    onNavigateBack: () -> Unit,
    viewModel: PreferencesViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager.getInstance(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val userPreferences by viewModel.preferences.collectAsStateWithLifecycle()
    val (aiWantSuggestions, aiDontWantSuggestions) = viewModel.aiSuggestions.collectAsStateWithLifecycle().value

    var country by remember { mutableStateOf("Pakistan") }
    var city by remember { mutableStateOf("Karachi") }

    // Section 1: Want to cook
    val selectedWant = remember { mutableStateListOf<String>() }
    val suggestedWant = remember { mutableStateListOf<String>() }
    var customWantInput by remember { mutableStateOf("") }

    // Section 2: Don't want to cook
    val selectedDontWant = remember { mutableStateListOf<String>() }
    val suggestedDontWant = remember { mutableStateListOf<String>() }
    var customDontWantInput by remember { mutableStateOf("") }

    // Section 3: Routine Rules
    var dailyDaal by remember { mutableStateOf(false) }
    var dailySabzi by remember { mutableStateOf(false) }
    var dailyMeat by remember { mutableStateOf(false) }
    var seasonalAI by remember { mutableStateOf(true) }
    var antiRepetitionDays by remember { mutableIntStateOf(30) }

    var isSaving by remember { mutableStateOf(false) }

    // Load saved preferences & AI suggestions on start
    LaunchedEffect(Unit) {
        val (savedCity, savedCountry) = LocationService.getSavedLocation(context)
        city = savedCity
        country = savedCountry

        val loaded = prefsManager.loadUserPreferences()
        selectedWant.clear()
        selectedWant.addAll(loaded.wantToCook)

        selectedDontWant.clear()
        selectedDontWant.addAll(loaded.dontWantToCook)

        dailyDaal = loaded.dailyDaal
        dailySabzi = loaded.dailySabzi
        dailyMeat = loaded.dailyMeat
        seasonalAI = loaded.seasonalAI
        antiRepetitionDays = if (loaded.antiRepetitionDays != 0 || prefsManager.getAntiRepetitionDays() != 0) {
            loaded.antiRepetitionDays.takeIf { it != 0 } ?: prefsManager.getAntiRepetitionDays()
        } else 0

        // Load country-specific AI suggestions
        val (aiWant, aiDontWant) = GeminiAIService.getKitchenPreferenceSuggestions(country)
        suggestedWant.clear()
        suggestedWant.addAll(aiWant)

        suggestedDontWant.clear()
        suggestedDontWant.addAll(aiDontWant)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kitchen Preferences",
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
            // Header Info Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🍽️", fontSize = 26.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Kitchen Preferences",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = DarkText
                                )
                                Text(
                                    text = "Applied to ALL meal suggestions across the app",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 12.sp,
                                    color = PrimaryOrange,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "These preferences are sent with every AI request to tailor meals to your household.",
                                fontFamily = NunitoFontFamily,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // Section 1: "I WANT TO COOK"
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💚", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "I WANT TO COOK (Priorities)",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DarkText
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dishes, ingredients, or styles you love. What2eat will prioritize these.",
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // AI Suggested Chips
                        if (suggestedWant.isNotEmpty()) {
                            Text(
                                text = "Popular choices (tap to toggle):",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = DarkText
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                suggestedWant.forEach { item ->
                                    val isSelected = selectedWant.contains(item)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) SecondaryEmerald else BackgroundOffWhite,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) SecondaryEmerald else CardBorder
                                        ),
                                        modifier = Modifier.clickable {
                                            if (isSelected) selectedWant.remove(item) else selectedWant.add(item)
                                        }
                                    ) {
                                        Text(
                                            text = if (isSelected) "✓ $item" else "+ $item",
                                            fontFamily = NunitoFontFamily,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (isSelected) SurfaceWhite else DarkText,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Add Custom Text Field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customWantInput,
                                onValueChange = { customWantInput = it },
                                placeholder = { Text("+ Add Custom (e.g. Mutton Pulao)", fontSize = 13.sp, color = TextMuted) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SecondaryEmerald,
                                    unfocusedBorderColor = CardBorder
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (customWantInput.isNotBlank() && !selectedWant.contains(customWantInput.trim())) {
                                        selectedWant.add(customWantInput.trim())
                                        customWantInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(SecondaryEmerald, shape = RoundedCornerShape(12.dp))
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = SurfaceWhite)
                            }
                        }

                        // Selected Items list
                        if (selectedWant.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Active Want List (${selectedWant.size}):",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SecondaryEmerald
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                selectedWant.forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SecondaryEmerald.copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryEmerald.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item,
                                                fontFamily = NunitoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = SecondaryEmerald
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = SecondaryEmerald,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { selectedWant.remove(item) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: "I DON'T WANT TO COOK"
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🚫", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "I DON'T WANT TO COOK (Hard Exclusions)",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DarkText
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ingredients or meals you dislike. What2eat will NEVER suggest these.",
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Suggested Exclusions
                        if (suggestedDontWant.isNotEmpty()) {
                            Text(
                                text = "Common exclusions (tap to add):",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = DarkText
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                suggestedDontWant.forEach { item ->
                                    val isSelected = selectedDontWant.contains(item)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) PrimaryOrange else BackgroundOffWhite,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) PrimaryOrange else CardBorder
                                        ),
                                        modifier = Modifier.clickable {
                                            if (isSelected) selectedDontWant.remove(item) else selectedDontWant.add(item)
                                        }
                                    ) {
                                        Text(
                                            text = if (isSelected) "✕ $item" else "+ $item",
                                            fontFamily = NunitoFontFamily,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (isSelected) SurfaceWhite else DarkText,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Add Custom Don't Want Text Field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customDontWantInput,
                                onValueChange = { customDontWantInput = it },
                                placeholder = { Text("+ Add Custom (e.g. Karela / Beef)", fontSize = 13.sp, color = TextMuted) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryOrange,
                                    unfocusedBorderColor = CardBorder
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (customDontWantInput.isNotBlank() && !selectedDontWant.contains(customDontWantInput.trim())) {
                                        selectedDontWant.add(customDontWantInput.trim())
                                        customDontWantInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(PrimaryOrange, shape = RoundedCornerShape(12.dp))
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = SurfaceWhite)
                            }
                        }

                        // Active Exclusions List
                        if (selectedDontWant.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Active Excluded List (${selectedDontWant.size}):",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PrimaryOrange
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                selectedDontWant.forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PrimaryOrange.copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item,
                                                fontFamily = NunitoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = PrimaryOrange
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = PrimaryOrange,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { selectedDontWant.remove(item) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: "ANTI-REPETITION MEMORY"
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔄", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ANTI-REPETITION MEMORY",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DarkText
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "How long should What2Eat remember cooked dishes before suggesting them again?",
                            fontFamily = NunitoFontFamily,
                            fontSize = 13.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val options = listOf(
                            Pair(30, "30 Days (Recommended)"),
                            Pair(15, "15 Days"),
                            Pair(7, "7 Days"),
                            Pair(0, "Allow Repetition (No Restriction)")
                        )

                        options.forEach { (days, label) ->
                            val isSelected = (antiRepetitionDays == days)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryOrange.copy(alpha = 0.1f) else BackgroundOffWhite,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) PrimaryOrange else CardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { antiRepetitionDays = days }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (isSelected) PrimaryOrange else DarkText
                                    )
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { antiRepetitionDays = days },
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryOrange)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Save Preferences Button
            item {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            val updatedPrefs = UserPreferences(
                                wantToCook = selectedWant.toList(),
                                dontWantToCook = selectedDontWant.toList(),
                                dailyDaal = dailyDaal,
                                dailySabzi = dailySabzi,
                                dailyMeat = dailyMeat,
                                seasonalAI = seasonalAI,
                                antiRepetitionDays = antiRepetitionDays,
                                country = country,
                                city = city
                            )

                            // 1. Save to SharedPreferences locally
                            prefsManager.saveUserPreferences(updatedPrefs)
                            prefsManager.setAntiRepetitionDays(antiRepetitionDays)

                            // 2. Sync to Firebase if logged in
                            FirebaseService.savePreferences(updatedPrefs)

                            isSaving = false
                            snackbarHostState.showSnackbar("Preferences saved! All suggestions updated ✓")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = SurfaceWhite, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = SurfaceWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Preferences",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SurfaceWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
