package com.example.englishword.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.Purchase
import com.example.englishword.BuildConfig
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

        // Re-verification window: if the app cannot reach Google Play,
        // premium access expires after this duration to prevent abuse.
        // queryPurchasesAsync only returns active subscriptions, so each
        // successful check renews this window.
        private const val PREMIUM_CACHE_DURATION_MS = 48L * 60 * 60 * 1000 // 48 hours
    }

    // ==================== Premium State ====================

    /**
     * Combined premium status from billing, local settings, and trial.
     * This is the single source of truth for premium status.
     */
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    /**
     * Trial state information.
     */
    private val _trialDaysRemaining = MutableStateFlow(0)
    val trialDaysRemaining: StateFlow<Int> = _trialDaysRemaining.asStateFlow()

    private val _isTrialActive = MutableStateFlow(false)
    val isTrialActive: StateFlow<Boolean> = _isTrialActive.asStateFlow()

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
        // Initialize premium status from local settings and trial
        applicationScope.launch {
            settingsRepository.isPremium().collect { localPremium ->
                // Only update if we're not currently loading from Google Play
                if (!_isLoading.value) {
                    updatePremiumStatus(localPremium)
                }
            }
        }

        // Check and update trial status on startup
        applicationScope.launch {
            refreshTrialStatus()
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
        if (BuildConfig.DEBUG) Log.d(TAG, "Checking premium status with Google Play...")
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
                        // Google Play confirmed no active subscription -- clear immediately
                        if (BuildConfig.DEBUG) Log.d(TAG, "No active subscription found, clearing premium")
                        settingsRepository.clearPremium()
                    }

                    hasActivePremium
                },
                onFailure = { error ->
                    if (BuildConfig.DEBUG) Log.e(TAG, "Failed to check premium status", error)
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
        if (BuildConfig.DEBUG) Log.d(TAG, "Starting premium purchase...")
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
        if (BuildConfig.DEBUG) Log.d(TAG, "Restoring purchases...")
        _isLoading.value = true

        try {
            return checkPremiumStatus()
        } finally {
            _isLoading.value = false
        }
    }

    // ==================== Private Helpers ====================

    private suspend fun onPurchaseSuccess(purchase: Purchase) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Processing successful purchase: ${purchase.orderId}")
        savePremiumPurchase(purchase)
        _isPremium.value = true
    }

    private suspend fun savePremiumPurchase(purchase: Purchase) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Saving premium purchase to settings...")

        // queryPurchasesAsync only returns active subscriptions, so if we reach
        // here the subscription is confirmed active. Set a cache window so premium
        // stays valid offline until the next re-verification with Google Play.
        val expiresAt = System.currentTimeMillis() + PREMIUM_CACHE_DURATION_MS

        val success = settingsRepository.savePremiumPurchase(
            purchaseToken = purchase.purchaseToken,
            sku = purchase.products.firstOrNull() ?: BillingClientWrapper.PRODUCT_ID_PREMIUM_MONTHLY,
            expiresAt = expiresAt
        )

        if (success) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Premium purchase saved successfully")
        } else {
            if (BuildConfig.DEBUG) Log.e(TAG, "Failed to save premium purchase")
            _error.emit("Failed to save purchase information")
        }
    }

    private suspend fun clearPremiumIfExpired() {
        // Check if premium has expired based on local settings
        if (settingsRepository.isPremiumExpired()) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Premium subscription expired, clearing status")
            settingsRepository.clearPremium()
            _isPremium.value = false
        } else if (!settingsRepository.isPremiumSync()) {
            // Already not premium, no action needed
            _isPremium.value = false
        }
    }

    /**
     * Update combined premium status (paid + trial).
     */
    private suspend fun updatePremiumStatus(paidPremium: Boolean) {
        val trialActive = settingsRepository.isTrialActive()
        _isPremium.value = paidPremium || trialActive
    }

    // ==================== Trial Management ====================

    /**
     * Start a free trial for new users.
     * @return true if trial was started successfully
     */
    suspend fun startTrial(): Boolean {
        val result = settingsRepository.startTrial()
        if (result) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Free trial started")
            refreshTrialStatus()
        }
        return result
    }

    /**
     * Refresh trial status and update state flows.
     */
    suspend fun refreshTrialStatus() {
        _isTrialActive.value = settingsRepository.isTrialActive()
        _trialDaysRemaining.value = settingsRepository.getTrialDaysRemaining()

        // Update combined premium status
        val paidPremium = settingsRepository.isPremiumSync()
        updatePremiumStatus(paidPremium)

        if (_isTrialActive.value) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Trial active: ${_trialDaysRemaining.value} days remaining")
        }
    }

    /**
     * Check if trial has expired (for showing upgrade prompts).
     */
    suspend fun isTrialExpired(): Boolean {
        return settingsRepository.isTrialExpired()
    }

    /**
     * Check if user has premium access (paid or trial).
     */
    suspend fun hasPremiumAccess(): Boolean {
        return settingsRepository.hasPremiumAccess()
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
