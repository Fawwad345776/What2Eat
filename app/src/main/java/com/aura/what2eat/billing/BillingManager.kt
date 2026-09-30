package com.aura.what2eat.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.aura.what2eat.model.UserSubscription
import com.aura.what2eat.service.FirebaseService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isProPlay = MutableStateFlow(false)
    private val _isProFirebase = MutableStateFlow(false)

    val isPro: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(_isProPlay, _isProFirebase) { play, firebase ->
        play || firebase
    }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, false)

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_MONTHLY = "what2eat.monthly"
        const val PRODUCT_YEARLY = "what2eat.yearly"

        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        startConnection()
        syncFirebaseProStatus()
    }

    /**
     * Connects to Google Play Billing Service.
     */
    fun startConnection(onConnected: (() -> Unit)? = null) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client setup successful")
                    scope.launch {
                        queryProducts()
                        restorePurchases()
                    }
                    onConnected?.invoke()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected. Will retry on next query.")
            }
        })
    }

    /**
     * Queries subscription products (Monthly & Yearly) from Google Play.
     */
    suspend fun queryProducts(): List<ProductDetails> = withContext(Dispatchers.IO) {
        if (!billingClient.isReady) {
            connectClientSuspending()
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val productDetailsResult = billingClient.queryProductDetails(params)
        val detailsList = productDetailsResult.productDetailsList ?: emptyList()
        _products.value = detailsList
        Log.d(TAG, "Fetched ${detailsList.size} subscription products from Google Play")
        return@withContext detailsList
    }

    /**
     * Launches the Google Play billing flow for a selected subscription product.
     */
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails, offerToken: String): BillingResult {
        if (!billingClient.isReady) {
            Log.e(TAG, "BillingClient is not ready to launch flow")
            return BillingResult.newBuilder().setResponseCode(BillingResponseCode.SERVICE_UNAVAILABLE).build()
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Callback from BillingClient when a purchase is updated or completed.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                scope.launch {
                    handlePurchase(purchase)
                }
            }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User cancelled the purchase flow")
        } else {
            Log.e(TAG, "Purchase error: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
        }
    }

    /**
     * Handles and acknowledges an active purchase.
     */
    suspend fun handlePurchase(purchase: Purchase) = withContext(Dispatchers.IO) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                val ackResult = billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                if (ackResult.responseCode == BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged successfully: ${purchase.orderId}")
                }
            }

            _isProPlay.value = true
            com.aura.what2eat.service.AnalyticsService.setUserProperties(FirebaseService.currentUserId, true)

            // Determine plan type
            val planType = when {
                purchase.products.contains(PRODUCT_YEARLY) -> "yearly"
                purchase.products.contains(PRODUCT_MONTHLY) -> "monthly"
                else -> "pro"
            }

            // Sync subscription to Firebase cloud
            FirebaseService.saveUserSubscription(
                UserSubscription(
                    isPro = true,
                    planType = planType,
                    expiryDate = purchase.purchaseTime + (30L * 24 * 60 * 60 * 1000) // approx
                )
            )
        }
    }

    /**
     * Restores previous active subscriptions from Google Play.
     */
    suspend fun restorePurchases(): Boolean = withContext(Dispatchers.IO) {
        if (!billingClient.isReady) {
            connectClientSuspending()
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingResponseCode.OK) {
            val activePurchases = result.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            if (activePurchases.isNotEmpty()) {
                for (purchase in activePurchases) {
                    handlePurchase(purchase)
                }
                _isProPlay.value = true
                com.aura.what2eat.service.AnalyticsService.setUserProperties(FirebaseService.currentUserId, true)
                Log.d(TAG, "Restored active subscriptions: ${activePurchases.size}")
                return@withContext true
            }
        }
        _isProPlay.value = false
        // Also re-verify Firebase remote PRO status
        val remotePro = FirebaseService.checkRemoteProStatus()
        _isProFirebase.value = remotePro
        com.aura.what2eat.service.AnalyticsService.setUserProperties(FirebaseService.currentUserId, isPro.value)
        com.aura.what2eat.service.CrashlyticsService.updateUserContext(isPro.value)
        return@withContext remotePro
    }

    /**
     * Checks and starts real-time listening to Firebase Remote PRO status.
     * When the developer toggles a user to PRO in Firebase Console, the app unlocks PRO instantly.
     */
    fun syncFirebaseProStatus() {
        scope.launch {
            try {
                val remotePro = FirebaseService.checkRemoteProStatus()
                _isProFirebase.value = remotePro
                com.aura.what2eat.service.AnalyticsService.setUserProperties(FirebaseService.currentUserId, isPro.value)
                com.aura.what2eat.service.CrashlyticsService.updateUserContext(isPro.value)
                com.aura.what2eat.service.CrashlyticsService.setUserId(FirebaseService.currentUserId)

                FirebaseService.listenToRemoteProStatus { isRemotePro ->
                    _isProFirebase.value = isRemotePro
                    com.aura.what2eat.service.AnalyticsService.setUserProperties(FirebaseService.currentUserId, isPro.value)
                    com.aura.what2eat.service.CrashlyticsService.updateUserContext(isPro.value)
                    Log.d(TAG, "Remote PRO status updated in real-time: $isRemotePro")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing Firebase PRO status", e)
            }
        }
    }

    fun isProActive(): Boolean {
        return isPro.value
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    private suspend fun connectClientSuspending(): Boolean = suspendCancellableCoroutine { continuation ->
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }

            override fun onBillingServiceDisconnected() {
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        })
    }
}
