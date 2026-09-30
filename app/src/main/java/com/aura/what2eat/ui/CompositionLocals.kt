package com.aura.what2eat.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.aura.what2eat.billing.BillingManager
import com.aura.what2eat.data.local.PreferencesManager
import com.aura.what2eat.model.AppContext

val LocalAppContext = compositionLocalOf { AppContext() }
val LocalSubscriptionManager = staticCompositionLocalOf<BillingManager> { error("No BillingManager provided") }
val LocalPreferencesManager = staticCompositionLocalOf<PreferencesManager> { error("No PreferencesManager provided") }
