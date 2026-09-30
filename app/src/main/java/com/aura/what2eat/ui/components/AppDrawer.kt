package com.aura.what2eat.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.aura.what2eat.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aura.what2eat.auth.GoogleAuthManager
import com.aura.what2eat.navigation.Screen
import com.aura.what2eat.service.LocationService
import com.aura.what2eat.ui.theme.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

data class DrawerItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String,
    val isProBadge: Boolean = false,
    val emoji: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var currentUser by remember { mutableStateOf(GoogleAuthManager.getCurrentUser()) }
    val billingManager = com.aura.what2eat.ui.LocalSubscriptionManager.current
    val isPro by billingManager.isPro.collectAsState()
    var country by remember { mutableStateOf("Pakistan") }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showHiddenRestaurantsDialog by remember { mutableStateOf(false) }

    val prefsManager = remember { com.aura.what2eat.data.local.PreferencesManager.getInstance(context) }
    var hiddenRestaurantIds by remember { mutableStateOf(prefsManager.getHiddenRestaurantIds()) }

    // Reactively listen to Firebase Auth state changes
    DisposableEffect(Unit) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        scope.launch {
            val (_, savedCountry) = LocationService.getSavedLocation(context)
            country = savedCountry
        }
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Is user a real Google/email user (not anonymous)?
    val isGoogleUser = currentUser != null && !currentUser!!.isAnonymous
    val hasPhoto = isGoogleUser && currentUser?.photoUrl != null && currentUser?.photoUrl.toString().isNotBlank()

    // Streamlined, uncluttered core navigation items
    val coreDrawerItems = listOfNotNull(
        DrawerItem("Home", Icons.Default.Home, Screen.Home.route, emoji = "🏠"),
        DrawerItem("What to Cook Today", Icons.Default.RestaurantMenu, Screen.MealDecider.route, emoji = "🍲"),
        DrawerItem("Kitchen Preferences", Icons.Default.Restaurant, Screen.KitchenPreferences.route, emoji = "🍽️"),
        DrawerItem("30-Day Cooked History", Icons.Default.History, Screen.CookedHistory.route, emoji = "📜"),
        if (!isPro) DrawerItem("Subscription & PRO", Icons.Default.Star, Screen.Subscription.route, isProBadge = true, emoji = "👑") else null
    )

    // Pricing text based on country
    val pricingText = when (country.lowercase()) {
        "pakistan", "pk" -> "Monthly: PKR 599 | Yearly: PKR 4,499"
        "india", "in" -> "Monthly: ₹199 | Yearly: ₹1,499"
        "bangladesh", "bd" -> "Monthly: ৳250 | Yearly: ৳1,999"
        "saudi arabia", "sa", "uae", "ae" -> "Monthly: SAR 12 | Yearly: SAR 89"
        else -> "Monthly: $2.99 | Yearly: $24.99"
    }

    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerContainerColor = SurfaceWhite,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(vertical = 16.dp)
        ) {
            // ── Profile Header Section ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                PrimaryOrange.copy(alpha = 0.12f),
                                PrimaryOrange.copy(alpha = 0.04f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (!isGoogleUser) {
                    // ── Not Signed In (or Anonymous) ─────────────────────────
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(BackgroundOffWhite, shape = CircleShape)
                                    .border(1.dp, CardBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = "What2Eat Logo",
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "Welcome Guest",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = DarkText
                                )
                                Text(
                                    text = "Sign in to sync recipes",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    GoogleAuthManager.signIn(context).collect { result ->
                                        if (result is com.aura.what2eat.auth.AuthResult.Success) {
                                            currentUser = result.user
                                            com.aura.what2eat.service.CrashlyticsService.setUserId(result.user.uid)
                                            com.aura.what2eat.service.AnalyticsService.setUserProperties(result.user.uid, isPro)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                        ) {
                            Text(
                                text = "Sign in with Google",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SurfaceWhite
                            )
                        }
                    }
                } else {
                    // ── Signed In with Google ────────────────────────────────
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Profile photo or initial avatar
                            if (hasPhoto) {
                                AsyncImage(
                                    model = currentUser?.photoUrl,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .border(2.5.dp, PrimaryOrange, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(OrangeGradientStart, OrangeGradientEnd)
                                            ),
                                            shape = CircleShape
                                        )
                                        .border(2.dp, PrimaryOrange.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (currentUser?.displayName?.firstOrNull()?.toString() ?: "U").uppercase(),
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = SurfaceWhite
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.displayName ?: "Google User",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = DarkText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentUser?.email ?: "",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isPro) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = GoldProContainer
                                    ) {
                                        Text(
                                            text = "👑 PRO Active",
                                            fontFamily = NunitoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = GoldPro,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    val uid = currentUser?.uid ?: com.aura.what2eat.service.FirebaseService.currentUserId
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("What2Eat User ID", uid)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(context, "User ID copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryOrange)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Copy ID",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 11.sp,
                                    color = PrimaryOrange
                                )
                            }

                            TextButton(
                                onClick = {
                                    scope.launch {
                                        GoogleAuthManager.signOut(context)
                                        currentUser = null
                                        val persistentUid = com.aura.what2eat.data.local.PreferencesManager.getInstance(context).getOrCreateUserId()
                                        com.aura.what2eat.service.FirebaseService.setFallbackUserId(persistentUid)
                                        com.aura.what2eat.service.CrashlyticsService.setUserId(persistentUid)
                                        com.aura.what2eat.service.AnalyticsService.setUserProperties(persistentUid, isPro)
                                    }
                                }
                            ) {
                                Text(
                                    text = "Sign Out",
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 8.dp))

            // Primary Navigation Items
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                coreDrawerItems.forEach { item ->
                    val isSelected = (currentRoute == item.route)
                    NavigationDrawerItem(
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )

                                if (item.isProBadge) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = GoldProContainer
                                    ) {
                                        Text(
                                            text = "PRO 👑",
                                            fontFamily = NunitoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = GoldPro,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        },
                        icon = {
                            if (item.emoji != null) {
                                Text(text = item.emoji, fontSize = 18.sp)
                            } else {
                                Icon(imageVector = item.icon, contentDescription = null)
                            }
                        },
                        selected = isSelected,
                        onClick = {
                            onCloseDrawer()
                            onNavigate(item.route)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = PrimaryOrange.copy(alpha = 0.12f),
                            selectedTextColor = PrimaryOrange,
                            selectedIconColor = PrimaryOrange,
                            unselectedTextColor = DarkText,
                            unselectedIconColor = TextMuted
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 10.dp))

                // Section: App Info & Support
                Text(
                    text = "INFO & SUPPORT",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                // 1. About Item
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "About What2Eat",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = DarkText
                        )
                    },
                    icon = { Text(text = "ℹ️", fontSize = 18.sp) },
                    selected = false,
                    onClick = {
                        onCloseDrawer()
                        showAboutDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                // 2. Contact Us / Suggestions Item
                NavigationDrawerItem(
                    label = {
                        Column {
                            Text(
                                text = "Contact Us & Suggestions",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = DarkText
                            )
                            Text(
                                text = "fawwadshamim@gmail.com",
                                fontFamily = NunitoFontFamily,
                                fontSize = 11.sp,
                                color = PrimaryOrange
                            )
                        }
                    },
                    icon = { Text(text = "✉️", fontSize = 18.sp) },
                    selected = false,
                    onClick = {
                        onCloseDrawer()
                        showContactDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                // 3. Hidden Restaurants Settings
                if (hiddenRestaurantIds.isNotEmpty()) {
                    NavigationDrawerItem(
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Hidden Restaurants",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = DarkText
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryOrange.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${hiddenRestaurantIds.size} hidden",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = PrimaryOrange,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        icon = { Text(text = "🚫", fontSize = 18.sp) },
                        selected = false,
                        onClick = {
                            hiddenRestaurantIds = prefsManager.getHiddenRestaurantIds()
                            showHiddenRestaurantsDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Upgrade to Pro Bottom Banner (If not PRO)
            if (!isPro) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            onCloseDrawer()
                            onNavigate(Screen.Subscription.route)
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldProContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "👑", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Upgrade to PRO",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = GoldPro
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = pricingText,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DarkText
                        )

                        Text(
                            text = "Unlimited skips • 7-day planner • Ad-free",
                            fontFamily = NunitoFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Version Footer
            Text(
                text = "what2Eat v1.0 • Made with ❤️ for Smart Kitchens",
                fontFamily = NunitoFontFamily,
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Terms of Service",
                    fontFamily = NunitoFontFamily,
                    fontSize = 11.sp,
                    color = PrimaryOrange,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://fawwad345776.github.io/What2Eat/terms.html"))
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    }
                )
                Text(text = " • ", color = TextMuted, fontSize = 11.sp)
                Text(
                    text = "Privacy Policy",
                    fontFamily = NunitoFontFamily,
                    fontSize = 11.sp,
                    color = PrimaryOrange,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://fawwad345776.github.io/What2Eat/privacy.html"))
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    }
                )
            }
        }
    }

    // ABOUT DIALOG
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Got it!",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceWhite
                    )
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🍳", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "About What2Eat",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DarkText
                        )
                        Text(
                            text = "Smart Meal Decider & Kitchen Planner",
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            color = PrimaryOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "What2Eat is your intelligent culinary assistant created to solve the daily dilemma: \"What should I cook today?\"",
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        color = DarkText,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BackgroundOffWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "✨ Key Features:",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "• AI-powered personalized dish recommendations", fontSize = 12.sp, color = TextMuted)
                            Text(text = "• Culturally authentic recipes tailored to your location", fontSize = 12.sp, color = TextMuted)
                            Text(text = "• Anti-repetition memory (30d / 15d / 7d / Allow repetition)", fontSize = 12.sp, color = TextMuted)
                            Text(text = "• Kitchen preferences with hard exclusions & daily routine rules", fontSize = 12.sp, color = TextMuted)
                            Text(text = "• Offline smart fallbacks for 100% reliable meal planning", fontSize = 12.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Version: 1.0.0 (Production Release)\nDesigned with ❤️ for home chefs worldwide.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = SurfaceWhite
        )
    }

    // CONTACT US / SUGGESTIONS & ISSUES DIALOG
    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:fawwadshamim@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "what2Eat — Feedback / Suggestion / Issue")
                        }
                        try {
                            context.startActivity(Intent.createChooser(emailIntent, "Send Email via"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Email: fawwadshamim@gmail.com", Toast.LENGTH_LONG).show()
                        }
                        showContactDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = SurfaceWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Send Email",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceWhite
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("what2Eat Support Email", "fawwadshamim@gmail.com")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Email copied to clipboard! ✓", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkText)
                ) {
                    Text(
                        text = "Copy Email",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "✉️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Contact Us & Suggestions",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DarkText
                        )
                        Text(
                            text = "We'd love to hear from you!",
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            color = PrimaryOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "For any feature suggestions, recipe additions, kitchen routine feedback, or bug reports, feel free to reach out directly to the developer.",
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        color = DarkText,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryOrange.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Support & Developer Email:",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "fawwadshamim@gmail.com",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = PrimaryOrange
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = SurfaceWhite
        )
    }

    // HIDDEN RESTAURANTS DIALOG
    if (showHiddenRestaurantsDialog) {
        AlertDialog(
            onDismissRequest = { showHiddenRestaurantsDialog = false },
            title = {
                Text(
                    text = "🚫 Hidden Restaurants",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DarkText
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
                            text = "No hidden restaurants. Restaurants you hide from Dine Out will appear here and can be restored at any time.",
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    } else {
                        Text(
                            text = "Tap \"Unhide\" to bring a restaurant back to your Dine Out list.",
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(hiddenRestaurantIds.toList()) { id ->
                                // Try to display a friendly name from the id
                                val displayName = id.replace("_", " ")
                                    .split(" ").joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BackgroundOffWhite,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = displayName,
                                            fontFamily = NunitoFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = DarkText,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(
                                            onClick = {
                                                prefsManager.unhideRestaurant(id)
                                                hiddenRestaurantIds = prefsManager.getHiddenRestaurantIds()
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
                            hiddenRestaurantIds.toList().forEach { prefsManager.unhideRestaurant(it) }
                            hiddenRestaurantIds = prefsManager.getHiddenRestaurantIds()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Unhide All", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showHiddenRestaurantsDialog = false }) {
                    Text("Close", fontFamily = NunitoFontFamily, color = TextMuted)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = SurfaceWhite
        )
    }
}

