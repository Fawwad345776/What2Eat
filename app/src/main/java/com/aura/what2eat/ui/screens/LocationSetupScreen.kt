package com.aura.what2eat.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.aura.what2eat.model.QuickLocation
import com.aura.what2eat.model.WorldwideLocations
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.aura.what2eat.data.local.PreferencesManager
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.aura.what2eat.service.LocationService
import com.aura.what2eat.ui.theme.BackgroundOffWhite
import com.aura.what2eat.ui.theme.CardBorder
import com.aura.what2eat.ui.theme.DarkText
import com.aura.what2eat.ui.theme.NunitoFontFamily
import com.aura.what2eat.ui.theme.OrangeGradientEnd
import com.aura.what2eat.ui.theme.OrangeGradientStart
import com.aura.what2eat.ui.theme.PrimaryOrange
import com.aura.what2eat.ui.theme.SecondaryEmerald
import com.aura.what2eat.ui.theme.SurfaceWhite
import com.aura.what2eat.ui.theme.TextMuted
import kotlinx.coroutines.launch

val PRESET_LOCATIONS = WorldwideLocations.POPULAR_LOCATIONS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSetupScreen(
    onContinueToHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedCity by remember { mutableStateOf("Karachi") }
    var selectedCountry by remember { mutableStateOf("Pakistan") }
    var isAutoDetected by remember { mutableStateOf(true) }
    var isSheetOpen by remember { mutableStateOf(false) }

    // Load initial location
    LaunchedEffect(Unit) {
        val (savedCity, savedCountry) = LocationService.getSavedLocation(context)
        selectedCity = savedCity
        selectedCountry = savedCountry

        // Try getting live location
        val (liveCity, liveCountry) = LocationService.getCurrentLocation(context)
        selectedCity = liveCity
        selectedCountry = liveCountry
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundOffWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(OrangeGradientStart, OrangeGradientEnd)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location Pin",
                        tint = SurfaceWhite,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Your Kitchen Location",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = DarkText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "We customize spice levels, ingredients, and regional recipes based on your city.",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Location Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📍",
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$selectedCity, $selectedCountry",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = DarkText
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isAutoDetected) "(Auto-Detected)" else "(Manually Selected)",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = SecondaryEmerald
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { isSheetOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryOrange
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditLocation,
                            contentDescription = "Change",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Change Location",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // CTA Button Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        PreferencesManager.getInstance(context).setFirstLaunchCompleted(true)
                        LocationService.saveLocation(context, selectedCity, selectedCountry)
                        onContinueToHome()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "Continue to Kitchen →",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SurfaceWhite
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Legal links at bottom
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "By continuing, you agree to ",
                        fontFamily = NunitoFontFamily,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "Terms",
                        fontFamily = NunitoFontFamily,
                        fontSize = 11.sp,
                        color = PrimaryOrange,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://fawwad345776.github.io/What2Eat/terms.html")
                            )
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                    )
                    Text(
                        text = " & ",
                        fontFamily = NunitoFontFamily,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "Privacy Policy",
                        fontFamily = NunitoFontFamily,
                        fontSize = 11.sp,
                        color = PrimaryOrange,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://fawwad345776.github.io/What2Eat/privacy.html")
                            )
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                    )
                }
            }
        }
    }

    // Location Picker Bottom Sheet
    if (isSheetOpen) {
        LocationPickerSheet(
            currentCity = selectedCity,
            currentCountry = selectedCountry,
            onDismiss = { isSheetOpen = false },
            onSelectLocation = { city, country ->
                selectedCity = city
                selectedCountry = country
                isAutoDetected = false
                LocationService.saveLocation(context, city, country)
                isSheetOpen = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(
    currentCity: String,
    currentCountry: String,
    onDismiss: () -> Unit,
    onSelectLocation: (city: String, country: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Step state: null means Step 1 (Select Country), non-null means Step 2 (Select City in that country)
    var selectedCountry by remember { mutableStateOf<String?>(null) }
    var countrySearchQuery by remember { mutableStateOf("") }
    var citySearchQuery by remember { mutableStateOf("") }
    var customCityInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            val country = selectedCountry

            if (country == null) {
                // ── STEP 1: SELECT COUNTRY ──────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryOrange.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "STEP 1 OF 2",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    color = PrimaryOrange,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select Country",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = DarkText
                            )
                        }
                        Text(
                            text = "Current: 📍 $currentCity, $currentCountry",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Country Bar
                OutlinedTextField(
                    value = countrySearchQuery,
                    onValueChange = { countrySearchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Search country (e.g. Pakistan, UAE, USA, UK)...",
                            fontFamily = NunitoFontFamily,
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PrimaryOrange
                        )
                    },
                    trailingIcon = {
                        if (countrySearchQuery.isNotEmpty()) {
                            IconButton(onClick = { countrySearchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextMuted
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = BackgroundOffWhite,
                        unfocusedContainerColor = BackgroundOffWhite
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Filtered countries
                val allCountries = remember { WorldwideLocations.ALL_WORLD_COUNTRIES }
                val filteredCountries = remember(countrySearchQuery) {
                    if (countrySearchQuery.isBlank()) {
                        allCountries
                    } else {
                        allCountries.filter { (_, name) ->
                            name.contains(countrySearchQuery, ignoreCase = true)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (countrySearchQuery.isBlank()) {
                        // Section: Popular Countries
                        item {
                            Text(
                                text = "POPULAR COUNTRIES",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(WorldwideLocations.POPULAR_LOCATIONS) { popLoc ->
                            val isCurrent = popLoc.country.equals(currentCountry, ignoreCase = true)
                            CountryItemRow(
                                flag = popLoc.flag,
                                countryName = popLoc.country,
                                isCurrent = isCurrent,
                                onClick = {
                                    selectedCountry = popLoc.country
                                    citySearchQuery = ""
                                    customCityInput = ""
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "ALL COUNTRIES (A - Z)",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }
                    }

                    items(filteredCountries) { (flag, cName) ->
                        val isCurrent = cName.equals(currentCountry, ignoreCase = true)
                        CountryItemRow(
                            flag = flag,
                            countryName = cName,
                            isCurrent = isCurrent,
                            onClick = {
                                selectedCountry = cName
                                citySearchQuery = ""
                                customCityInput = ""
                            }
                        )
                    }
                }
            } else {
                // ── STEP 2: SELECT CITY IN SELECTED COUNTRY ─────────────────
                val flag = WorldwideLocations.getCountryFlag(country)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = { selectedCountry = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Countries",
                                tint = PrimaryOrange
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryOrange.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "STEP 2 OF 2",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = PrimaryOrange,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$flag $country",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = DarkText,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Select or type your city in $country",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search or Enter City Bar
                OutlinedTextField(
                    value = citySearchQuery,
                    onValueChange = { citySearchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Search or type city in $country...",
                            fontFamily = NunitoFontFamily,
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PrimaryOrange
                        )
                    },
                    trailingIcon = {
                        if (citySearchQuery.isNotEmpty()) {
                            IconButton(onClick = { citySearchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextMuted
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = BackgroundOffWhite,
                        unfocusedContainerColor = BackgroundOffWhite
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick 1-tap card to use typed city
                if (citySearchQuery.isNotBlank()) {
                    val typedCity = citySearchQuery.trim().replaceFirstChar { it.uppercase() }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryOrange.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectLocation(typedCity, country)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📍", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Use \"$typedCity\"",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PrimaryOrange
                                )
                                Text(
                                    text = "Tap to set as your city in $country",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = PrimaryOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val presetCities = remember(country) {
                    WorldwideLocations.getCitiesForCountry(country)
                }

                val filteredCities = remember(presetCities, citySearchQuery) {
                    if (citySearchQuery.isBlank()) {
                        presetCities
                    } else {
                        presetCities.filter { it.contains(citySearchQuery, ignoreCase = true) }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredCities.isNotEmpty()) {
                        item {
                            Text(
                                text = "POPULAR CITIES IN $country",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(filteredCities) { city ->
                            val isSelected = city.equals(currentCity, ignoreCase = true) && country.equals(currentCountry, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryOrange.copy(alpha = 0.08f) else BackgroundOffWhite,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectLocation(city, country) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = city,
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = if (isSelected) PrimaryOrange else DarkText
                                    )

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = PrimaryOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Custom City Entry Box
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Can't find your city?",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DarkText
                                )
                                Text(
                                    text = "Enter any city or town in $country:",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customCityInput,
                                        onValueChange = { customCityInput = it },
                                        placeholder = { Text("City name", fontSize = 13.sp) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (customCityInput.isNotBlank()) {
                                                onSelectLocation(customCityInput.trim().replaceFirstChar { it.uppercase() }, country)
                                            }
                                        },
                                        enabled = customCityInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Set", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryItemRow(
    flag: String,
    countryName: String,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrent) PrimaryOrange.copy(alpha = 0.08f) else BackgroundOffWhite,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = flag, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = countryName,
                    fontFamily = NunitoFontFamily,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (isCurrent) PrimaryOrange else DarkText
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrent) {
                    Text(
                        text = "Current",
                        fontFamily = NunitoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Select",
                    tint = if (isCurrent) PrimaryOrange else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
