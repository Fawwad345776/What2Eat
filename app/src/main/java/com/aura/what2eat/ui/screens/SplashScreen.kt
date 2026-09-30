package com.aura.what2eat.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.what2eat.R
import com.aura.what2eat.ui.theme.BackgroundOffWhite
import com.aura.what2eat.ui.theme.DarkText
import com.aura.what2eat.ui.theme.NunitoFontFamily
import com.aura.what2eat.ui.theme.OrangeGradientEnd
import com.aura.what2eat.ui.theme.OrangeGradientStart
import com.aura.what2eat.ui.theme.PrimaryOrange
import com.aura.what2eat.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.aura.what2eat.data.local.PreferencesManager

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLocationSetup: () -> Unit
) {
    val context = LocalContext.current
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Smoothly fade in text
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            )
        )

        // Clean, responsive transition duration
        delay(1200)
        val isSetupDone = PreferencesManager.getInstance(context).isFirstLaunchCompleted()
        if (isSetupDone) {
            onNavigateToHome()
        } else {
            onNavigateToLocationSetup()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Official App Icon (Rock-solid, crisp, no jump)
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "What2Eat App Icon",
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .shadow(12.dp, RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(24.dp))

                // App Title
                Text(
                    text = "What2Eat",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                    color = PrimaryOrange,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.alpha(textAlpha.value)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tagline / Subtitle
                Text(
                    text = "Smart AI Meal Decider & Kitchen Planner",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = DarkText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(textAlpha.value)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Personalized recipes, weekly plans & dining spots",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(textAlpha.value)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer Branding
            Text(
                text = "Your Daily Culinary Companion ✨",
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .padding(bottom = 16.dp)
            )
        }
    }
}
