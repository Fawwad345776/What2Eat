package com.aura.what2eat.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.billingclient.api.ProductDetails
import com.aura.what2eat.billing.BillingManager
import com.aura.what2eat.service.LocationService
import com.aura.what2eat.ui.theme.*
import com.aura.what2eat.viewmodel.SubscriptionUiState
import com.aura.what2eat.viewmodel.SubscriptionViewModel

data class BenefitItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    val products by viewModel.products.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var selectedPlanId by remember { mutableStateOf(BillingManager.PRODUCT_YEARLY) }
    var country by remember { mutableStateOf("Pakistan") }

    LaunchedEffect(Unit) {
        com.aura.what2eat.service.AnalyticsService.logSubscriptionScreenViewed()
        val (_, savedCountry) = LocationService.getSavedLocation(context)
        country = savedCountry
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SubscriptionUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.clearUiState()
            }
            is SubscriptionUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.clearUiState()
            }
            else -> {}
        }
    }

    // Default Fallback Price Display when running outside Google Play Store emulator
    val fallbackMonthlyPrice = when (country.lowercase()) {
        "pakistan", "pk" -> "PKR 599 / month"
        "india", "in" -> "₹199 / month"
        "bangladesh", "bd" -> "৳250 / month"
        else -> "$2.99 / month"
    }

    val fallbackYearlyPrice = when (country.lowercase()) {
        "pakistan", "pk" -> "PKR 4,499 / year"
        "india", "in" -> "₹1,499 / year"
        "bangladesh", "bd" -> "৳1,999 / year"
        else -> "$24.99 / year"
    }

    // Extract formatted pricing from Google Play ProductDetails if available
    val monthlyProduct = products.find { it.productId == BillingManager.PRODUCT_MONTHLY }
    val yearlyProduct = products.find { it.productId == BillingManager.PRODUCT_YEARLY }

    val monthlyPriceFormatted = monthlyProduct?.subscriptionOfferDetails?.firstOrNull()
        ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: fallbackMonthlyPrice

    val yearlyPriceFormatted = yearlyProduct?.subscriptionOfferDetails?.firstOrNull()
        ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: fallbackYearlyPrice

    val benefits = listOf(
        BenefitItem(Icons.Default.Bolt, "Unlimited Dish Skips", "Generate endless meal options with zero limits"),
        BenefitItem(Icons.AutoMirrored.Filled.MenuBook, "Unlimited Full Recipe Unlocks", "Access detailed ingredients, prep steps & chef pro tips"),
        BenefitItem(Icons.Default.Kitchen, "Unlimited Pantry Searches", "Cook with what's in your fridge any time"),
        BenefitItem(Icons.Default.CalendarMonth, "Full 7-Day Meal Planner Access", "Plan custom weekly lunch & dinner menus for family"),
        BenefitItem(Icons.Default.Block, "100% Ad-Free Experience", "No banner, interstitial, or video ads anywhere")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // What2Eat Logo Banner with Pro Badge
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(SurfaceWhite)
                    .border(2.5.dp, GoldPro, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.aura.what2eat.R.drawable.app_logo),
                    contentDescription = "What2Eat Logo",
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "what2Eat Pro",
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                color = DarkText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Unlimited smart meal planning & kitchen freedom",
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pro Active Banner if already subscribed
            if (isPro) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SecondaryEmerald.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SecondaryEmerald)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "You are currently subscribed to what2Eat Pro!",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SecondaryEmerald
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Benefits List
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    benefits.forEach { benefit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(GoldProContainer, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = benefit.icon,
                                    contentDescription = null,
                                    tint = GoldPro,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = benefit.title,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DarkText
                                )
                                Text(
                                    text = benefit.description,
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Plan Selector Cards: [Monthly] & [Yearly] (Equal Height and Symmetry)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Monthly Plan Card
                val isMonthlySelected = (selectedPlanId == BillingManager.PRODUCT_MONTHLY)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { selectedPlanId = BillingManager.PRODUCT_MONTHLY },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isMonthlySelected) PrimaryOrange.copy(alpha = 0.08f) else SurfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        if (isMonthlySelected) PrimaryOrange else CardBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Transparent
                        ) {
                            Text(
                                text = "Standard",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Monthly",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = monthlyPriceFormatted,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (isMonthlySelected) PrimaryOrange else DarkText,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = "Billed monthly",
                            fontFamily = NunitoFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Yearly Plan Card (Equal Height with "Save ~37%" Badge)
                val isYearlySelected = (selectedPlanId == BillingManager.PRODUCT_YEARLY)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { selectedPlanId = BillingManager.PRODUCT_YEARLY },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isYearlySelected) GoldProContainer.copy(alpha = 0.45f) else SurfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        if (isYearlySelected) GoldPro else CardBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Badge: "Save ~37% • Best Value"
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoldPro
                        ) {
                            Text(
                                text = "Save ~37% • Best Value",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp,
                                color = DarkText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Yearly",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = yearlyPriceFormatted,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (isYearlySelected) GoldPro else DarkText,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = "Billed annually",
                            fontFamily = NunitoFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subscribe Button
            Button(
                onClick = {
                    val targetProduct = if (selectedPlanId == BillingManager.PRODUCT_YEARLY) yearlyProduct else monthlyProduct
                    if (targetProduct != null && activity != null) {
                        viewModel.subscribe(activity, targetProduct)
                    } else {
                        // Mock activation for development / test builds
                        Toast.makeText(context, "Activating Pro subscription...", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPro),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "👑 Subscribe Now",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = DarkText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Restore Purchases Button
            TextButton(onClick = { viewModel.restore() }) {
                Text(
                    text = "Restore Purchases",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = PrimaryOrange
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Simple User ID Row (for account reference & support)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("What2Eat User ID", viewModel.currentUserId)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "User ID copied!", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "User ID",
                            fontFamily = NunitoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Text(
                            text = viewModel.currentUserId,
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkText,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy User ID",
                        tint = PrimaryOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            // Fine Print & Legal Links
            Text(
                text = "Subscriptions renew automatically unless cancelled at least 24 hours before the end of the period. Manage or cancel in Google Play Store.",
                fontFamily = NunitoFontFamily,
                fontSize = 11.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
