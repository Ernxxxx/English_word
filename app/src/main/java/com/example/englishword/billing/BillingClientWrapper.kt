package com.example.englishword.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.example.englishword.BuildConfig
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.delay
import kotlin.coroutines.resume

/**
 * Wrapper class for Google Play BillingClient.
 * Handles initialization, connection, and all billing operations.
 */
class BillingClientWrapper(
    private val context: Context
) : PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private const val TAG = "BillingClientWrapper"

        // Product IDs
        const val PRODUCT_ID_PREMIUM_MONTHLY = "premium_monthly"

        // Reconnection settings
        private const val MAX_RECONNECT_ATTEMPTS = 3
        private const val RECONNECT_DELAY_MS = 1000L

        // Retry settings for transient billing errors
        private const val MAX_QUERY_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val RETRY_BACKOFF_MULTIPLIER = 2.0

        // Transient billing error codes that are worth retrying
        private val TRANSIENT_ERROR_CODES = setOf(
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,   // 2
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,  // -1
            BillingClient.BillingResponseCode.ERROR                  // 6
        )
    }

    // BillingClient instance
    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Product details cache
    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails.asStateFlow()

    // Purchases state
    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases.asStateFlow()

    // Purchase events channel
    private val _purchaseEvents = Channel<PurchaseEvent>(Channel.BUFFERED)
    val purchaseEvents = _purchaseEvents.receiveAsFlow()

    // Reconnection tracking
    private var reconnectAttempts = 0

    /**
     * Connection states for the billing client.
     */
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    /**
     * Events emitted during purchase flow.
     */
    sealed class PurchaseEvent {
        data class PurchaseCompleted(val purchase: Purchase) : PurchaseEvent()
        data class PurchasePending(val purchase: Purchase) : PurchaseEvent()
        object PurchaseCancelled : PurchaseEvent()
        data class PurchaseError(val responseCode: Int, val message: String) : PurchaseEvent()
    }

    // ==================== Connection Management ====================

    /**
     * Start connection to billing service.
     */
    fun startConnection() {
        if (_connectionState.value == ConnectionState.Connected ||
            _connectionState.value == ConnectionState.Connecting) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Already connected or connecting")
            return
        }

        _connectionState.value = ConnectionState.Connecting
        billingClient.startConnection(this)
    }

    /**
     * End connection to billing service.
     */
    fun endConnection() {
        billingClient.endConnection()
        _connectionState.value = ConnectionState.Disconnected
        reconnectAttempts = 0
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "Billing setup finished successfully")
                _connectionState.value = ConnectionState.Connected
                reconnectAttempts = 0
            }
            else -> {
                if (BuildConfig.DEBUG) Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                _connectionState.value = ConnectionState.Error(
                    "Setup failed: ${billingResult.debugMessage}"
                )
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        if (BuildConfig.DEBUG) Log.w(TAG, "Billing service disconnected")
        _connectionState.value = ConnectionState.Disconnected

        // Attempt to reconnect
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            if (BuildConfig.DEBUG) Log.d(TAG, "Attempting to reconnect (attempt $reconnectAttempts)")
            startConnection()
        } else {
            if (BuildConfig.DEBUG) Log.e(TAG, "Max reconnection attempts reached")
            _connectionState.value = ConnectionState.Error("Connection lost")
        }
    }

    /**
     * Ensure billing client is connected, waiting if necessary.
     */
    suspend fun ensureConnected(): Boolean {
        if (_connectionState.value == ConnectionState.Connected) {
            return true
        }

        return suspendCancellableCoroutine { continuation ->
            val callback = object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _connectionState.value = ConnectionState.Connected
                        continuation.resume(true)
                    } else {
                        _connectionState.value = ConnectionState.Error(billingResult.debugMessage)
                        continuation.resume(false)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _connectionState.value = ConnectionState.Disconnected
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }

            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build()

            _connectionState.value = ConnectionState.Connecting
            billingClient.startConnection(callback)
        }
    }

    // ==================== Product Details ====================

    /**
     * Query product details for subscriptions with retry logic for transient errors.
     * Retries up to MAX_QUERY_RETRIES times with exponential backoff for
     * SERVICE_UNAVAILABLE, SERVICE_DISCONNECTED, and generic ERROR responses.
     */
    suspend fun querySubscriptionProductDetails(
        productIds: List<String> = listOf(PRODUCT_ID_PREMIUM_MONTHLY)
    ): Result<List<ProductDetails>> {
        if (!ensureConnected()) {
            return Result.failure(BillingException("Not connected to billing service"))
        }

        val productList = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        var lastException: BillingException? = null

        for (attempt in 0 until MAX_QUERY_RETRIES) {
            if (attempt > 0) {
                val delayMs = (INITIAL_RETRY_DELAY_MS * Math.pow(RETRY_BACKOFF_MULTIPLIER, (attempt - 1).toDouble())).toLong()
                if (BuildConfig.DEBUG) Log.d(TAG, "Retrying product details query (attempt ${attempt + 1}/$MAX_QUERY_RETRIES) after ${delayMs}ms")
                delay(delayMs)

                // Re-establish connection if needed before retry
                if (!ensureConnected()) {
                    return Result.failure(BillingException("Not connected to billing service"))
                }
            }

            val result = suspendCancellableCoroutine<Result<List<ProductDetails>>> { continuation ->
                billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                    when (billingResult.responseCode) {
                        BillingClient.BillingResponseCode.OK -> {
                            if (BuildConfig.DEBUG) Log.d(TAG, "Product details query successful: ${productDetailsList.size} products")

                            // Cache the product details
                            val detailsMap = productDetailsList.associateBy { it.productId }
                            _productDetails.value = _productDetails.value + detailsMap

                            continuation.resume(Result.success(productDetailsList))
                        }
                        else -> {
                            if (BuildConfig.DEBUG) Log.e(TAG, "Product details query failed (attempt ${attempt + 1}): ${billingResult.debugMessage}")
                            continuation.resume(
                                Result.failure(
                                    BillingException(
                                        "Query failed: ${billingResult.debugMessage}",
                                        billingResult.responseCode
                                    )
                                )
                            )
                        }
                    }
                }
            }

            if (result.isSuccess) {
                return result
            }

            // Check if the error is transient and worth retrying
            val exception = result.exceptionOrNull() as? BillingException
            lastException = exception
            if (exception != null && exception.responseCode !in TRANSIENT_ERROR_CODES) {
                // Non-transient error, do not retry
                if (BuildConfig.DEBUG) Log.e(TAG, "Non-transient billing error (code ${exception.responseCode}), not retrying")
                return result
            }
        }

        if (BuildConfig.DEBUG) Log.e(TAG, "All $MAX_QUERY_RETRIES retry attempts exhausted for product details query")
        return Result.failure(
            lastException ?: BillingException("Query failed after $MAX_QUERY_RETRIES attempts")
        )
    }

    /**
     * Get cached product details by ID.
     */
    fun getProductDetails(productId: String): ProductDetails? {
        return _productDetails.value[productId]
    }

    // ==================== Purchase Flow ====================

    /**
     * Launch the billing flow for a subscription purchase.
     */
    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String? = null
    ): BillingResult {
        // Get the offer token - use provided one or get the first available
        val token = offerToken ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
                .setDebugMessage("No offer token available")
                .build()

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(token)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        if (BuildConfig.DEBUG) Log.d(TAG, "Launching billing flow for ${productDetails.productId}")
        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Launch purchase flow using product ID.
     */
    suspend fun launchPurchaseFlow(
        activity: Activity,
        productId: String = PRODUCT_ID_PREMIUM_MONTHLY
    ): Result<BillingResult> {
        // Get product details
        val details = getProductDetails(productId)
            ?: run {
                // Query if not cached
                val queryResult = querySubscriptionProductDetails(listOf(productId))
                if (queryResult.isFailure) {
                    return Result.failure(queryResult.exceptionOrNull()!!)
                }
                queryResult.getOrNull()?.firstOrNull()
            }

        if (details == null) {
            return Result.failure(BillingException("Product not found: $productId"))
        }

        val result = launchBillingFlow(activity, details)
        return Result.success(result)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "Purchases updated: ${purchases?.size ?: 0} purchases")
                purchases?.forEach { purchase ->
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            _purchaseEvents.trySend(PurchaseEvent.PurchaseCompleted(purchase))
                            _purchases.value = _purchases.value + purchase
                        }
                        Purchase.PurchaseState.PENDING -> {
                            _purchaseEvents.trySend(PurchaseEvent.PurchasePending(purchase))
                        }
                        else -> {
                            if (BuildConfig.DEBUG) Log.w(TAG, "Unknown purchase state: ${purchase.purchaseState}")
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "User cancelled purchase")
                _purchaseEvents.trySend(PurchaseEvent.PurchaseCancelled)
            }
            else -> {
                if (BuildConfig.DEBUG) Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                _purchaseEvents.trySend(
                    PurchaseEvent.PurchaseError(
                        billingResult.responseCode,
                        billingResult.debugMessage
                    )
                )
            }
        }
    }

    // ==================== Query Purchases ====================

    /**
     * Query existing subscription purchases.
     */
    suspend fun querySubscriptionPurchases(): Result<List<Purchase>> {
        if (!ensureConnected()) {
            return Result.failure(BillingException("Not connected to billing service"))
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        return suspendCancellableCoroutine { continuation ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Query purchases successful: ${purchasesList.size} purchases")
                        _purchases.value = purchasesList
                        continuation.resume(Result.success(purchasesList))
                    }
                    else -> {
                        if (BuildConfig.DEBUG) Log.e(TAG, "Query purchases failed: ${billingResult.debugMessage}")
                        continuation.resume(
                            Result.failure(
                                BillingException(
                                    "Query failed: ${billingResult.debugMessage}",
                                    billingResult.responseCode
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    // ==================== Acknowledge Purchase ====================

    /**
     * Acknowledge a purchase.
     * This must be called within 3 days of purchase or it will be refunded.
     */
    suspend fun acknowledgePurchase(purchase: Purchase): Result<Unit> {
        if (purchase.isAcknowledged) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Purchase already acknowledged")
            return Result.success(Unit)
        }

        if (!ensureConnected()) {
            return Result.failure(BillingException("Not connected to billing service"))
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        return suspendCancellableCoroutine { continuation ->
            billingClient.acknowledgePurchase(params) { billingResult ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Purchase acknowledged successfully")
                        continuation.resume(Result.success(Unit))
                    }
                    else -> {
                        if (BuildConfig.DEBUG) Log.e(TAG, "Acknowledge failed: ${billingResult.debugMessage}")
                        continuation.resume(
                            Result.failure(
                                BillingException(
                                    "Acknowledge failed: ${billingResult.debugMessage}",
                                    billingResult.responseCode
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    // ==================== Subscription Status ====================

    /**
     * Check if user has an active premium subscription.
     */
    suspend fun hasActivePremiumSubscription(): Boolean {
        val purchasesResult = querySubscriptionPurchases()
        if (purchasesResult.isFailure) {
            return false
        }

        val purchases = purchasesResult.getOrNull() ?: return false
        return purchases.any { purchase ->
            purchase.products.contains(PRODUCT_ID_PREMIUM_MONTHLY) &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
    }

    /**
     * Get the active premium purchase if exists.
     */
    suspend fun getActivePremiumPurchase(): Purchase? {
        val purchasesResult = querySubscriptionPurchases()
        if (purchasesResult.isFailure) {
            return null
        }

        val purchases = purchasesResult.getOrNull() ?: return null
        return purchases.find { purchase ->
            purchase.products.contains(PRODUCT_ID_PREMIUM_MONTHLY) &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Get formatted price string for premium subscription.
     */
    fun getPremiumPrice(): String? {
        val details = getProductDetails(PRODUCT_ID_PREMIUM_MONTHLY)
        return details?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.formattedPrice
    }

    /**
     * Check if billing is available on this device.
     */
    fun isFeatureSupported(feature: String): Boolean {
        return billingClient.isFeatureSupported(feature).responseCode ==
            BillingClient.BillingResponseCode.OK
    }

    /**
     * Check if subscriptions are supported.
     */
    fun isSubscriptionsSupported(): Boolean {
        return isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
    }
}

/**
 * Exception class for billing errors.
 */
class BillingException(
    message: String,
    val responseCode: Int = BillingClient.BillingResponseCode.ERROR
) : Exception(message)
