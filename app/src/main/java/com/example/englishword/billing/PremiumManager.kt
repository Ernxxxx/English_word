package com.example.englishword.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.Purchase
import com.example.englishword.data.repository.SettingsRepository
import com.example.englishword.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for premium subscription status.
 * Coordinates between BillingRepository and SettingsRepository to provide
 * a single source of truth for premium status.
 */
@Singleton
class PremiumManager @Inject constructor(
    private val billingRepository: BillingRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    companion object {
        private const val TAG = "PremiumManager"
    }

    // ==================== Premium State ====================

    /**
     * Combined premium status from billing and local settings.
     * This is the single source of truth for premium status.
     */
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    /**
     * Loading state for premium check operations.
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Error state for premium operations.
     */
    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    /**
     * Purchase state from billing repository.
     */
    val purchaseState: StateFlow<BillingRepository.PurchaseState> = billingRepository.purchaseState

    init {
        // Initialize premium status from local settings first (fast)
        applicationScope.launch {
            settingsRepository.isPremium().collect { localPremium ->
                // Only update if we're not currently loading from Google Play
                if (!_isLoading.value) {
                    _isPremium.value = localPremium
                }
            }
        }

        // Sync with Google Play when billing connects
        applicationScope.launch {
            billingRepository.connectionState.collect { state ->
                if (state == BillingClientWrapper.ConnectionState.Connected) {
                    checkPremiumStatus()
                }
            }
        }

        // Handle purchase events
        applicationScope.launch {
            billingRepository.purchaseState.collect { state ->
                when (state) {
                    is BillingRepository.PurchaseState.Success -> {
                        onPurchaseSuccess(state.purchase)
                    }
                    is BillingRepository.PurchaseState.Error -> {
                        _error.emit(state.message)
                    }
                    else -> { /* No action needed */ }
                }
            }
        }

        // Monitor billing repository premium status
        applicationScope.launch {
            billingRepository.isPremium.collect { isPremium ->
                if (isPremium) {
                    _isPremium.value = true
                }
            }
        }
    }

    // ==================== Premium Status Check ====================

    /**
     * Check and verify premium status with Google Play.
     * Updates local settings based on Google Play response.
     */
    suspend fun checkPremiumStatus(): Boolean {
        Log.d(TAG, "Checking premium status with Google Play...")
        _isLoading.value = true

        try {
            // Query purchases from Google Play
            val purchasesResult = billingRepository.queryPurchases()

            val isPremium = purchasesResult.fold(
                onSuccess = { purchases ->
                    val hasActivePremium = purchases.any { purchase ->
                        purchase.products.contains(BillingClientWrapper.PRODUCT_ID_PREMIUM_MONTHLY) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    }

                    // Update local settings
                    if (hasActivePremium) {
                        val purchase = purchases.first {
                            it.products.contains(BillingClientWrapper.PRODUCT_ID_PREMIUM_MONTHLY)
                        }
                        savePremiumPurchase(purchase)
                    } else {
                        // Clear premium if no active subscription found
                        clearPremiumIfExpired()
                    }

                    hasActivePremium
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to check premium status", error)
                    // Fall back to local setting on error
                    settingsRepository.isPremiumSync()
                }
            )

            _isPremium.value = isPremium
            return isPremium
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Force refresh premium status.
     */
    suspend fun refreshPremiumStatus() {
        checkPremiumStatus()
    }

    // ==================== Purchase Operations ====================

    /**
     * Launch premium purchase flow.
     */
    suspend fun purchasePremium(activity: Activity): Result<Unit> {
        Log.d(TAG, "Starting premium purchase...")
        _isLoading.value = true

        try {
            return billingRepository.launchPurchaseFlow(activity)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Restore previous purchases.
     */
    suspend fun restorePurchases(): Boolean {
        Log.d(TAG, "Restoring purchases...")
        _isLoading.value = true

        try {
            return checkPremiumStatus()
        } finally {
            _isLoading.value = false
        }
    }

    // ==================== Private Helpers ====================

    private suspend fun onPurchaseSuccess(purchase: Purchase) {
        Log.d(TAG, "Processing successful purchase: ${purchase.orderId}")
        savePremiumPurchase(purchase)
        _isPremium.value = true
    }

    private suspend fun savePremiumPurchase(purchase: Purchase) {
        Log.d(TAG, "Saving premium purchase to settings...")

        // Calculate expiration (subscriptions auto-renew, so we set a far future date)
        // In production, you would get the actual expiration from Google Play Developer API
        val expiresAt = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000) // 1 year placeholder

        val success = settingsRepository.savePremiumPurchase(
            purchaseToken = purchase.purchaseToken,
            sku = purchase.products.firstOrNull() ?: BillingClientWrapper.PRODUCT_ID_PREMIUM_MONTHLY,
            expiresAt = expiresAt
        )

        if (success) {
            Log.d(TAG, "Premium purchase saved successfully")
        } else {
            Log.e(TAG, "Failed to save premium purchase")
            _error.emit("Failed to save purchase information")
        }
    }

    private suspend fun clearPremiumIfExpired() {
        // Check if premium has expired based on local settings
        if (settingsRepository.isPremiumExpired()) {
            Log.d(TAG, "Premium subscription expired, clearing status")
            settingsRepository.clearPremium()
            _isPremium.value = false
        } else if (!settingsRepository.isPremiumSync()) {
            // Already not premium, no action needed
            _isPremium.value = false
        }
    }

    // ==================== Product Information ====================

    /**
     * Get formatted price for premium subscription.
     */
    fun getPremiumPrice(): String {
        return billingRepository.getPremiumPrice()
    }

    /**
     * Query and cache product details.
     */
    suspend fun queryProducts() {
        billingRepository.queryProducts()
    }

    /**
     * Check if subscriptions are supported on this device.
     */
    fun isSubscriptionsSupported(): Boolean {
        return billingRepository.isSubscriptionsSupported()
    }

    /**
     * Reset purchase state (e.g., after showing error dialog).
     */
    fun resetPurchaseState() {
        billingRepository.resetPurchaseState()
    }

    /**
     * Get current purchase for premium subscription.
     */
    suspend fun getActivePremiumPurchase(): Purchase? {
        return billingRepository.getActivePremiumPurchase()
    }

    // ==================== Connection Management ====================

    /**
     * Start billing connection.
     */
    fun startConnection() {
        billingRepository.startConnection()
    }

    /**
     * End billing connection.
     */
    fun endConnection() {
        billingRepository.endConnection()
    }
}
