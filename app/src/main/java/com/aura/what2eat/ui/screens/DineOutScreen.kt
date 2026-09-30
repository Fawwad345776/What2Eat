package com.aura.what2eat.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.what2eat.service.AdManager
import com.aura.what2eat.service.LocationService
import com.aura.what2eat.ui.LocalSubscriptionManager
import com.aura.what2eat.ui.theme.*
import com.aura.what2eat.viewmodel.DineOutViewModel
import com.aura.what2eat.viewmodel.RestaurantResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DineOutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit = {},
    viewModel: DineOutViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val billingManager = LocalSubscriptionManager.current
    val isProUser by billingManager.isPro.collectAsStateWithLifecycle()
    val isPro = isProUser || com.aura.what2eat.BuildConfig.DEBUG

    val nearbyResults by viewModel.nearbyRestaurants.collectAsStateWithLifecycle()
    val displayedRestaurants by viewModel.displayedRestaurants.collectAsStateWithLifecycle()
    val dailyDish by viewModel.dailyDishRecommendation.collectAsStateWithLifecycle()
    val skipsRemaining by viewModel.skipsRemaining.collectAsStateWithLifecycle()
    val likedRestaurantIds by viewModel.likedRestaurantIds.collectAsStateWithLifecycle()
    val hiddenRestaurantIds by viewModel.hiddenRestaurantIds.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    var selectedFilter by remember { mutableStateOf("⭐ Top Rated") }

    LaunchedEffect(isPro) {
        viewModel.refreshSkipsRemaining(isPro)
    }

    LaunchedEffect(Unit) {
        com.aura.what2eat.service.AnalyticsService.logDineOutViewed(city = "user_location", filter = selectedFilter)
    }

    var showPaywallDialog by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }
    var showHiddenManager by remember { mutableStateOf(false) }

    // Location-wise currency helper
    fun currencySymbol(forCountry: String): String {
        return when (forCountry.lowercase().trim()) {
            "pakistan", "pk" -> "Rs."
            "india", "in" -> "₹"
            "bangladesh", "bd" -> "৳"
            "saudi arabia", "sa" -> "SAR"
            "united arab emirates", "uae", "ae" -> "AED"
            "united kingdom", "uk", "gb" -> "£"
            "european union", "europe", "germany", "france", "spain", "italy", "de", "fr", "es", "it" -> "€"
            "canada", "ca" -> "CA$"
            "australia", "au" -> "AU$"
            else -> "$"
        }
    }

    val initialLoc = remember { LocationService.getSavedLocation(context) }
    var city by remember { mutableStateOf(initialLoc.first) }
    var country by remember { mutableStateOf(initialLoc.second) }
    var area by remember { mutableStateOf(LocationService.getSavedArea(context)) }
    var userLat by remember { mutableStateOf(LocationService.getSavedCoordinates(context).first) }
    var userLng by remember { mutableStateOf(LocationService.getSavedCoordinates(context).second) }
    var isLocationPickerOpen by remember { mutableStateOf(false) }

    // Helper to reload restaurants with latest coordinates & parameters
    fun triggerReload(filter: String = selectedFilter) {
        viewModel.loadRestaurants(
            city = city,
            country = country,
            area = area,
            filter = filter,
            searchQuery = dailyDish,
            priceRange = "",
            userLat = userLat,
            userLng = userLng
        )
    }

    // Function to acquire live GPS location
    fun fetchLiveGpsAndFilterNearest() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            // Permission not granted, will be triggered by launcher
            return
        }

        if (!LocationService.isGpsEnabled(context)) {
            showGpsDialog = true
            return
        }

        scope.launch {
            isLocating = true
            val (newCity, newCountry) = LocationService.getCurrentLocation(context, forceGps = true)
            val coords = LocationService.getSavedCoordinates(context)
            userLat = coords.first
            userLng = coords.second
            city = newCity
            country = newCountry
            area = LocationService.getSavedArea(context)
            isLocating = false

            selectedFilter = "📍 Nearest"
            viewModel.loadRestaurants(
                city = city,
                country = country,
                area = area,
                filter = "📍 Nearest",
                searchQuery = dailyDish,
                priceRange = "",
                userLat = userLat,
                userLng = userLng
            )
            snackbarHostState.showSnackbar("📍 Found top dining spots within 20 km of you")
        }
    }

    // Permission Request Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLiveGpsAndFilterNearest()
        } else {
            // Permission denied -> open manual location picker
            isLocationPickerOpen = true
        }
    }

    // Initial Location Check on Open
    LaunchedEffect(Unit) {
        AdManager.loadInterstitialAd(context)

        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasSavedLoc = context.getSharedPreferences("what2eat_location_prefs", Context.MODE_PRIVATE).contains("saved_city")

        // Immediately trigger reload with saved city/coordinates
        triggerReload()

        if (!hasFine && !hasCoarse && !hasSavedLoc) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (hasFine || hasCoarse) {
            scope.launch {
                val (freshCity, freshCountry) = LocationService.getCurrentLocation(context, forceGps = false)
                val coords = LocationService.getSavedCoordinates(context)
                userLat = coords.first
                userLng = coords.second
                city = freshCity
                country = freshCountry
                area = LocationService.getSavedArea(context)
                triggerReload()
            }
        }

        LocationService.locationChangedFlow.collect { (newCity, newCountry) ->
            city = newCity
            country = newCountry
            area = LocationService.getSavedArea(context)
            val coords = LocationService.getSavedCoordinates(context)
            userLat = coords.first
            userLng = coords.second
            triggerReload()
        }
    }

    // Handle exit
    val handleExit = {
        onNavigateBack()
    }

    BackHandler {
        handleExit()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dine Out & Takeaway",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleExit) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundOffWhite)
            )
        },
        containerColor = BackgroundOffWhite
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Location Header Chip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (area.isNotBlank()) "Spots in $area" else "Restaurants in $city",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = DarkText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (selectedFilter == "📍 Nearest") "Showing spots within 20 km of you" else "Top dining spots in $city",
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .wrapContentWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isLocationPickerOpen = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "📍 ${if (area.isNotBlank()) area else city}",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PrimaryOrange,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.EditLocation,
                                contentDescription = "Change Location",
                                tint = PrimaryOrange,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // 1. Persistent Daily AI Recommendation Card with 3 Skips & PRO Paywall
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                listOf(OrangeGradientStart, OrangeGradientEnd)
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "✨", fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Daily Dining Suggestion",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PrimaryOrange
                                    )
                                    Text(
                                        text = if (isPro) "PRO Active • Unlimited Suggestions ✨" else "$skipsRemaining of 3 suggestions left today",
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Next Suggestion / Refresh Button (Icon only without text)
                            IconButton(
                                onClick = {
                                    if (isPro || skipsRemaining > 0) {
                                        val ok = viewModel.skipDailyRecommendation(isPro, city, country, area, selectedFilter, userLat, userLng)
                                        if (!ok) {
                                            showPaywallDialog = true
                                        }
                                    } else {
                                        showPaywallDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BackgroundOffWhite, RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Next Suggestion",
                                    modifier = Modifier.size(18.dp),
                                    tint = PrimaryOrange
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryOrange.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🍲 $dailyDish",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = DarkText,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Segmented Tab Bar: "⭐ Top Rated" vs "📍 Nearest"
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        // Top Rated Tab
                        val isTopRated = (selectedFilter == "⭐ Top Rated")
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isTopRated) PrimaryOrange else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedFilter = "⭐ Top Rated"
                                    triggerReload("⭐ Top Rated")
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "⭐ Top Rated",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = if (isTopRated) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isTopRated) SurfaceWhite else DarkText
                                )
                            }
                        }

                        // Nearest Tab (Live Location + 20km Check)
                        val isNearest = (selectedFilter == "📍 Nearest")
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isNearest) PrimaryOrange else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                                    if (!hasFine && !hasCoarse) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    } else if (!LocationService.isGpsEnabled(context)) {
                                        showGpsDialog = true
                                    } else {
                                        fetchLiveGpsAndFilterNearest()
                                    }
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isLocating) {
                                        CircularProgressIndicator(
                                            color = if (isNearest) SurfaceWhite else PrimaryOrange,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = if (isLocating) "Locating..." else "📍 Nearest",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = if (isNearest) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (isNearest) SurfaceWhite else DarkText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Empty State
            if (nearbyResults.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🍽️", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedFilter == "📍 Nearest") "No dining spots found within 20 km" else "No spots found in $city",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedFilter == "📍 Nearest") "No dining spots found within 20 km. Try adjusting your location or checking Top Rated spots!" else "No spots found in $city. Try exploring Nearest dining spots or changing your city!",
                                fontFamily = NunitoFontFamily,
                                fontSize = 13.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 5. Restaurant List Items with Like & Hide actions (paginated)
            items(displayedRestaurants, key = { it.id }) { restaurant ->
                val isLiked = restaurant.id in likedRestaurantIds
                RestaurantCard(
                    restaurant = restaurant,
                    isLiked = isLiked,
                    onToggleLike = {
                        viewModel.toggleLikeRestaurant(restaurant.id)
                    },
                    onHide = {
                        viewModel.hideRestaurant(restaurant.id)
                        scope.launch {
                            snackbarHostState.showSnackbar("${restaurant.name} hidden. You won't see it again.")
                        }
                    },
                    onGetDirections = {
                        val destinationQuery = "${restaurant.name}, ${restaurant.area}"
                        val mapsIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destinationQuery)}")
                        ).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        try {
                            context.startActivity(mapsIntent)
                        } catch (e: Exception) {
                            try {
                                val fallbackIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destinationQuery)}")
                                )
                                context.startActivity(fallbackIntent)
                            } catch (e2: Exception) {
                                val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(destinationQuery)}"))
                                context.startActivity(geoIntent)
                            }
                        }
                    }
                )
            }



            // 6. Hidden Restaurants Banner — lets user quickly manage hidden spots
            if (hiddenRestaurantIds.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHiddenManager = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${hiddenRestaurantIds.size} restaurant${if (hiddenRestaurantIds.size > 1) "s" else ""} hidden",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = "Manage →",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PrimaryOrange
                            )
                        }
                    }
                }
            }
        }
    }

    // Hidden Restaurants Manager Dialog
    if (showHiddenManager) {
        AlertDialog(
            onDismissRequest = { showHiddenManager = false },
            title = {
                Text(
                    text = "🚫 Hidden Restaurants",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (hiddenRestaurantIds.isEmpty()) {
                        Text(
                            text = "No hidden restaurants. Restaurants you hide will appear here and can be restored at any time.",
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    } else {
                        Text(
                            text = "Tap \"Unhide\" on a restaurant to bring it back to your list.",
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(hiddenRestaurantIds.toList()) { id ->
                                val restaurant = viewModel.getRestaurantById(id)
                                val displayName = restaurant?.name ?: "Restaurant ($id)"
                                val displayArea = restaurant?.area ?: ""
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BackgroundOffWhite,
                                    border = BorderStroke(1.dp, CardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = displayName,
                                                fontFamily = NunitoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = DarkText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (displayArea.isNotBlank()) {
                                                Text(
                                                    text = displayArea,
                                                    fontFamily = NunitoFontFamily,
                                                    fontSize = 11.sp,
                                                    color = TextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(
                                            onClick = {
                                                viewModel.unhideRestaurant(id)
                                                triggerReload()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Unhide",
                                                fontFamily = NunitoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = PrimaryOrange
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (hiddenRestaurantIds.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.clearAllHiddenRestaurants()
                            triggerReload()
                            showHiddenManager = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Unhide All", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showHiddenManager = false }) {
                    Text("Close", fontFamily = NunitoFontFamily, color = TextMuted)
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Location Picker Sheet
    if (isLocationPickerOpen) {
        LocationPickerSheet(
            currentCity = city,
            currentCountry = country,
            onDismiss = { isLocationPickerOpen = false },
            onSelectLocation = { newCity, newCountry ->
                city = newCity
                country = newCountry
                area = ""
                LocationService.saveLocation(context, newCity, newCountry)
                viewModel.loadRestaurants(newCity, newCountry, "", selectedFilter, dailyDish, "", userLat, userLng)
                isLocationPickerOpen = false
            }
        )
    }

    // PRO Paywall Alert Dialog when skips are exhausted
    if (showPaywallDialog) {
        AlertDialog(
            onDismissRequest = { showPaywallDialog = false },
            icon = {
                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = GoldPro, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(
                    text = "Daily Limit Reached",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "You have used all 3 free dining suggestions for today. Upgrade to What2Eat PRO for unlimited daily suggestions, personalized AI recommendations, and full meal planner access!",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    color = DarkText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPaywallDialog = false
                        onNavigateToSubscription()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Upgrade to PRO", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaywallDialog = false }) {
                    Text("Maybe Later", fontFamily = NunitoFontFamily, color = TextMuted)
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // GPS Disabled Alert Dialog
    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = { showGpsDialog = false },
            icon = {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(
                    text = "Turn On Location (GPS)",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "To find the best restaurants near you within 20 km, please turn on your device's GPS / Location service.",
                    fontFamily = NunitoFontFamily,
                    fontSize = 14.sp,
                    color = DarkText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGpsDialog = false
                        try {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (e: Exception) {
                            // ignore
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Turn On GPS", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsDialog = false }) {
                    Text("Cancel", fontFamily = NunitoFontFamily, color = TextMuted)
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun RestaurantCard(
    restaurant: RestaurantResult,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onHide: () -> Unit,
    onGetDirections: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Restaurant Title & Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = restaurant.name,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkText,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = GoldPro, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${restaurant.rating} (${restaurant.reviewsCount})",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkText
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Specialty
            Text(
                text = "🍴 ${restaurant.specialty}",
                fontFamily = NunitoFontFamily,
                fontSize = 13.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Area, Distance, Price Level & Open Now
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buildString {
                            if (restaurant.distanceKm != null && restaurant.distanceKm < 100.0) {
                                append("${restaurant.distanceKm} km away • ")
                            }
                            append(restaurant.area)
                            append(" • ~Rs. ${restaurant.approxPrice}")
                        },
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (restaurant.isOpenNow) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SecondaryEmerald.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "🟢 Open",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = SecondaryEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: Like (Heart), Hide (Block), Directions (Google Maps)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (isLiked) Color(0xFFFFEBEE) else BackgroundOffWhite, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) Color(0xFFE53935) else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Hide / Not Interested Button
                OutlinedButton(
                    onClick = onHide,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                    border = BorderStroke(1.dp, CardBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Hide restaurant",
                        tint = TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hide",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                // Directions Button
                Button(
                    onClick = onGetDirections,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        tint = SurfaceWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Directions",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SurfaceWhite
                    )
                }
            }
        }
    }
}
