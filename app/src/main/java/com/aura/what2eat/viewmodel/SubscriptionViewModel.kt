package com.aura.what2eat.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.aura.what2eat.billing.BillingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SubscriptionUiState {
    data object Idle : SubscriptionUiState()
    data object Loading : SubscriptionUiState()
    data class Success(val message: String) : SubscriptionUiState()
    data class Error(val message: String) : SubscriptionUiState()
}

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val billingManager = BillingManager.getInstance(application)

    val products: StateFlow<List<ProductDetails>> = billingManager.products
    val isPro: StateFlow<Boolean> = billingManager.isPro

    private val _uiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Idle)
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    /**
     * Loads available subscription products from Google Play.
     */
    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            try {
                billingManager.queryProducts()
                _uiState.value = SubscriptionUiState.Idle
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.localizedMessage ?: "Failed to load subscription plans")
            }
        }
    }

    /**
     * Initiates the Google Play subscription flow for the selected product.
     */
    fun subscribe(activity: Activity, product: ProductDetails) {
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken.isNullOrEmpty()) {
            _uiState.value = SubscriptionUiState.Error("Selected plan is currently unavailable on Google Play")
            return
        }

        billingManager.launchBillingFlow(activity, product, offerToken)
    }

    /**
     * Restores active subscription purchases.
     */
    fun restore() {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            val restored = billingManager.restorePurchases()
            if (restored) {
                _uiState.value = SubscriptionUiState.Success("Your Pro subscription was successfully restored! 👑")
            } else {
                _uiState.value = SubscriptionUiState.Error("No active subscriptions found for your Google Play account.")
            }
        }
    }

    val currentUserId: String
        get() = com.aura.what2eat.service.FirebaseService.currentUserId

    val currentUserEmail: String
        get() = com.aura.what2eat.service.FirebaseService.currentUserEmail

    fun clearUiState() {
        _uiState.value = SubscriptionUiState.Idle
    }
}
