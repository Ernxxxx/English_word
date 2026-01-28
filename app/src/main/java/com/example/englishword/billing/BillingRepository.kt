package com.example.englishword.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.example.englishword.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing Google Play Billing operations.
 * Provides a clean API for the rest of the app to interact with billing.
 */
@Singleton
class BillingRepository @Inject constructor(
    private val billingClientWrapper: BillingClientWrapper,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    companion object {
        private const val TAG = "BillingRepository"
    }

    // ==================== State Flows ====================

    /**
     * Current connection state to the billing service.
     */
    val connectionState: StateFlow<BillingClientWrapper.ConnectionState> =
        billingClientWrapper.connectionState

    /**
     * Current list of purchases.
     */
    val purchases: StateFlow<List<Purchase>> = billingClientWrapper.purchases

    /**
     * Available product details.
     */
    val productDetails: StateFlow<Map<String, ProductDetails>> =
        billingClientWrapper.productDetails

    /**
     * Purchase state for UI.
     */
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    /**
     * Premium subscription status.
     */
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    /**
     * Error messages for UI.
     */
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    init {
        // Start connection when repository is created
        startConnection()

        // Collect purchase events
        applicationScope.launch {
            billingClientWrapper.purchaseEvents.collect { event ->
                handlePurchaseEvent(event)
            }
        }

        // Monitor connection state
        applicationScope.launch {
            connectionState.collect { state ->
                if (state == BillingClientWrapper.ConnectionState.Connected) {
                    // Refresh products and purchases when connected
                    refreshProductsAndPurchases()
                }
            }
        }
    }

    // ==================== Purchase States ====================

    /**
     * Purchase operation states.
     */
    sealed class PurchaseState {
        object Idle : PurchaseState()
        object Loading : PurchaseState()
        data class Success(val purchase: Purchase) : PurchaseState()
        object Pending : PurchaseState()
        object Cancelled : PurchaseState()
        data class Error(val message: String, val code: Int) : PurchaseState()
    }

    // ==================== Connection Management ====================

    /**
     * Start connection to billing service.
     */
    fun startConnection() {
        billingClientWrapper.startConnection()
    }

    /**
     * End connection to billing service.
     */
    fun endConnection() {
        billingClientWrapper.endConnection()
    }

    // ==================== Product Operations ====================

    /**
     * Query available products (subscriptions).
     */
    suspend fun queryProducts(): Result<List<ProductDetails>> {
        Log.d(TAG, "Querying products...")
        _purchaseState.value = PurchaseState.Loading

        val result = billingClientWrapper.querySubscriptionProductDetails()

        result.fold(
            onSuccess = { products ->
                Log.d(TAG, "Products loaded: ${products.size}")
                _purchaseState.value = PurchaseState.Idle
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to load products", error)
                _purchaseState.value = PurchaseState.Error(
                    error.message ?: "Unknown error",
                    (error as? BillingException)?.responseCode ?: BillingClient.BillingResponseCode.ERROR
                )
                _errorMessage.emit(error.message ?: "Failed to load products")
            }
        )

        return result
    }

    /**
     * Get premium product details.
     */
    fun getPremiumProductDetails(): ProductDetails? {
        return billingClientWrapper.getProductDetails(BillingClientWrapper.PRODUCT_ID_PREMIUM_MONTHLY)
    }

    /**
     * Get formatted price for premium subscription.
     */
    fun getPremiumPrice(): String {
        return billingClientWrapper.getPremiumPrice() ?: "N/A"
    }

    // ==================== Purchase Operations ====================

    /**
     * Launch the purchase flow for premium subscription.
     */
    suspend fun launchPurchaseFlow(activity: Activity): Result<Unit> {
        Log.d(TAG, "Launching purchase flow...")
        _purchaseState.value = PurchaseState.Loading

        val result = billingClientWrapper.launchPurchaseFlow(
            activity = activity,
            productId = BillingClientWrapper.PRODUCT_ID_PREMIUM_MONTHLY
        )

        return result.fold(
            onSuccess = { billingResult ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        Log.d(TAG, "Billing flow launched successfully")
                        Result.success(Unit)
                    }
                    else -> {
                        val message = "Failed to launch purchase: ${billingResult.debugMessage}"
                        Log.e(TAG, message)
                        _purchaseState.value = PurchaseState.Error(
                            message,
                            billingResult.responseCode
                        )
                        _errorMessage.emit(message)
                        Result.failure(BillingException(message, billingResult.responseCode))
                    }
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to launch purchase flow", error)
                _purchaseState.value = PurchaseState.Error(
                    error.message ?: "Unknown error",
                    (error as? BillingException)?.responseCode ?: BillingClient.BillingResponseCode.ERROR
                )
                _errorMessage.emit(error.message ?: "Failed to launch purchase")
                Result.failure(error)
            }
        )
    }

    /**
     * Query existing purchases (for restore functionality).
     */
    suspend fun queryPurchases(): Result<List<Purchase>> {
        Log.d(TAG, "Querying purchases...")
        _purchaseState.value = PurchaseState.Loading

        val result = billingClientWrapper.querySubscriptionPurchases()

        result.fold(
            onSuccess = { purchaseList ->
                Log.d(TAG, "Purchases loaded: ${purchaseList.size}")
                _purchaseState.value = PurchaseState.Idle

                // Update premium status
                updatePremiumStatus(purchaseList)

                // Acknowledge any unacknowledged purchases
                purchaseList.forEach { purchase ->
                    if (!purchase.isAcknowledged &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        acknowledgePurchase(purchase)
                    }
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to load purchases", error)
                _purchaseState.value = PurchaseState.Error(
                    error.message ?: "Unknown error",
                    (error as? BillingException)?.responseCode ?: BillingClient.BillingResponseCode.ERROR
                )
                _errorMessage.emit(error.message ?: "Failed to load purchases")
            }
        )

        return result
    }

    /**
     * Acknowledge a purchase.
     */
    suspend fun acknowledgePurchase(purchase: Purchase): Result<Unit> {
        Log.d(TAG, "Acknowledging purchase: ${purchase.orderId}")

        val result = billingClientWrapper.acknowledgePurchase(purchase)

        result.fold(
            onSuccess = {
                Log.d(TAG, "Purchase acknowledged successfully")
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to acknowledge purchase", error)
                _errorMessage.emit("Failed to acknowledge purchase: ${error.message}")
            }
        )

        return result
    }

    // ==================== Premium Status ====================

    /**
     * Check if user has active premium subscription.
     */
    suspend fun checkPremiumStatus(): Boolean {
        val hasActive = billingClientWrapper.hasActivePremiumSubscription()
        _isPremium.value = hasActive
        return hasActive
    }

    /**
     * Get active premium purchase.
     */
    suspend fun getActivePremiumPurchase(): Purchase? {
        return billingClientWrapper.getActivePremiumPurchase()
    }

    // ==================== Private Helpers ====================

    private fun handlePurchaseEvent(event: BillingClientWrapper.PurchaseEvent) {
        when (event) {
            is BillingClientWrapper.PurchaseEvent.PurchaseCompleted -> {
                Log.d(TAG, "Purchase completed: ${event.purchase.orderId}")
                _purchaseState.value = PurchaseState.Success(event.purchase)
                _isPremium.value = true

                // Acknowledge the purchase
                applicationScope.launch {
                    acknowledgePurchase(event.purchase)
                }
            }
            is BillingClientWrapper.PurchaseEvent.PurchasePending -> {
                Log.d(TAG, "Purchase pending: ${event.purchase.orderId}")
                _purchaseState.value = PurchaseState.Pending
            }
            is BillingClientWrapper.PurchaseEvent.PurchaseCancelled -> {
                Log.d(TAG, "Purchase cancelled")
                _purchaseState.value = PurchaseState.Cancelled
            }
            is BillingClientWrapper.PurchaseEvent.PurchaseError -> {
                Log.e(TAG, "Purchase error: ${event.message}")
                _purchaseState.value = PurchaseState.Error(event.message, event.responseCode)
                applicationScope.launch {
                    _errorMessage.emit(event.message)
                }
            }
        }
    }

    private fun updatePremiumStatus(purchases: List<Purchase>) {
        val hasPremium = purchases.any { purchase ->
            purchase.products.contains(BillingClientWrapper.PRODUCT_ID_PREMIUM_MONTHLY) &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        _isPremium.value = hasPremium
    }

    private suspend fun refreshProductsAndPurchases() {
        Log.d(TAG, "Refreshing products and purchases...")
        queryProducts()
        queryPurchases()
    }

    /**
     * Reset purchase state to idle.
     */
    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }

    /**
     * Check if subscriptions are supported on this device.
     */
    fun isSubscriptionsSupported(): Boolean {
        return billingClientWrapper.isSubscriptionsSupported()
    }
}
