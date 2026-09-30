package com.aura.what2eat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.aura.what2eat.billing.BillingManager
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.AppContext
import com.aura.what2eat.navigation.AppNavigation
import com.aura.what2eat.navigation.Screen
import com.aura.what2eat.service.ContextService
import com.aura.what2eat.ui.LocalAppContext
import com.aura.what2eat.ui.LocalPreferencesManager
import com.aura.what2eat.ui.LocalSubscriptionManager
import com.aura.what2eat.ui.theme.BackgroundOffWhite
import com.aura.what2eat.ui.theme.What2EatTheme
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager
    private lateinit var prefsManager: PreferencesManager

    /** True once all init work is done and Compose has drawn its first frame. */
    private var isReady = false

    /** The start destination resolved while the system splash is still visible. */
    private var startRoute: String = Screen.Home.route

    override fun onCreate(savedInstanceState: Bundle?) {
        // ── 1. Install the AndroidX splash screen BEFORE super.onCreate ──
        val splashScreen = installSplashScreen()

        // Keep the system splash on-screen until we've finished init AND
        // Compose has drawn — this eliminates the black-flash gap entirely.
        splashScreen.setKeepOnScreenCondition { !isReady }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Match the window background to the splash so there's zero flicker
        // during the theme transition (splash theme → app theme).
        window.setBackgroundDrawableResource(R.color.splash_window_background)

        // ── 2. Initialize services synchronously (all fast, no network) ──
        FirebaseApp.initializeApp(this)
        com.aura.what2eat.service.AnalyticsService.init(this)
        com.aura.what2eat.service.CrashlyticsService.init(this)

        prefsManager = PreferencesManager.getInstance(this)
        val initialUid = prefsManager.getOrCreateUserId()
        com.aura.what2eat.service.FirebaseService.setFallbackUserId(initialUid)

        // Ensure every user has a unique ID in Firebase Auth and Firestore even if they don't sign in with Google
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnSuccessListener { result ->
                result.user?.uid?.let { uid ->
                    prefsManager.setPermanentUserId(uid)
                    com.aura.what2eat.service.FirebaseService.setFallbackUserId(uid)
                    CoroutineScope(Dispatchers.IO).launch {
                        com.aura.what2eat.service.FirebaseService.ensureUserDocumentExists(uid)
                    }
                }
            }.addOnFailureListener {
                CoroutineScope(Dispatchers.IO).launch {
                    com.aura.what2eat.service.FirebaseService.ensureUserDocumentExists(initialUid)
                }
            }
        } else {
            val uid = auth.currentUser!!.uid
            prefsManager.setPermanentUserId(uid)
            com.aura.what2eat.service.FirebaseService.setFallbackUserId(uid)
            CoroutineScope(Dispatchers.IO).launch {
                com.aura.what2eat.service.FirebaseService.ensureUserDocumentExists(uid)
            }
        }

        MobileAds.initialize(this) {}
        billingManager = BillingManager.getInstance(this)

        // ── 3. Decide where to navigate BEFORE Compose starts ──
        // This runs while the system splash is still visible, so no second
        // splash screen or intermediate blank screen is ever shown.
        startRoute = if (prefsManager.isFirstLaunchCompleted()) {
            Screen.Home.route
        } else {
            Screen.LocationSetup.route
        }

        // ── 4. Set Compose content ──
        setContent {
            var appContext by remember { mutableStateOf(AppContext()) }

            LaunchedEffect(Unit) {
                appContext = ContextService.buildAppContext(this@MainActivity)
            }

            CompositionLocalProvider(
                LocalAppContext provides appContext,
                LocalSubscriptionManager provides billingManager,
                LocalPreferencesManager provides prefsManager
            ) {
                What2EatTheme {
                    // Fill the entire screen with the matching background
                    // colour so there's never a transparent/black gap.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundOffWhite)
                    ) {
                        // Signal that Compose has drawn; the system splash
                        // can now dismiss. SideEffect fires after every
                        // successful composition of this Box.
                        SideEffect {
                            if (!isReady) {
                                isReady = true
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            val navController = rememberNavController()

                            DisposableEffect(navController) {
                                val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
                                    destination.route?.let { route ->
                                        com.aura.what2eat.service.AnalyticsService.logScreenView(route)
                                        com.aura.what2eat.service.AnalyticsService.logFeatureUsed(route)
                                    }
                                }
                                navController.addOnDestinationChangedListener(listener)
                                onDispose {
                                    navController.removeOnDestinationChangedListener(listener)
                                }
                            }

                            AppNavigation(
                                navController = navController,
                                startDestination = startRoute
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}
