package com.example.englishword.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.englishword.BuildConfig
import com.example.englishword.data.repository.SettingsRepository
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdManager handles all AdMob operations including SDK initialization,
 * banner ads, and interstitial ads.
 *
 * Uses test ad unit IDs for development:
 * - Banner: ca-app-pub-3940256099942544/6300978111
 * - Interstitial: ca-app-pub-3940256099942544/1033173712
 * - App ID: ca-app-pub-3940256099942544~3347511713
 */
@Singleton
class AdManager @Inject constructor(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "AdManager"

        // Test Ad Unit IDs (used in debug builds)
        private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

        // Production Ad Unit IDs (TODO: Replace with real AdMob IDs before release)
        private const val PRODUCTION_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val PRODUCTION_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val PRODUCTION_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

        // Auto-switch based on build type
        val BANNER_AD_UNIT_ID = if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else PRODUCTION_BANNER_AD_UNIT_ID
        val INTERSTITIAL_AD_UNIT_ID = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT_ID else PRODUCTION_INTERSTITIAL_AD_UNIT_ID
        val REWARDED_AD_UNIT_ID = if (BuildConfig.DEBUG) TEST_REWARDED_AD_UNIT_ID else PRODUCTION_REWARDED_AD_UNIT_ID

        // Frequency cap for interstitial ads (show every Nth study completion)
        const val INTERSTITIAL_FREQUENCY_CAP = 3
    }

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private val _isInterstitialLoaded = MutableStateFlow(false)
    val isInterstitialLoaded: StateFlow<Boolean> = _isInterstitialLoaded.asStateFlow()

    private var rewardedAd: RewardedAd? = null
    private val _isRewardedLoaded = MutableStateFlow(false)
    val isRewardedLoaded: StateFlow<Boolean> = _isRewardedLoaded.asStateFlow()

    // Frequency cap counter for interstitial ads
    private var studyCompletionCount = 0

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        // Observe premium status
        coroutineScope.launch {
            settingsRepository.isPremium().collect { premium ->
                _isPremium.value = premium
                if (premium) {
                    // Clear loaded ads when user becomes premium
                    interstitialAd = null
                    _isInterstitialLoaded.value = false
                }
            }
        }
    }

    /**
     * Initialize the Mobile Ads SDK.
     * Should be called once when the app starts.
     */
    fun initialize() {
        if (_isInitialized.value) {
            Log.d(TAG, "AdMob SDK already initialized")
            return
        }

        MobileAds.initialize(context) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for ((adapterClass, status) in statusMap) {
                Log.d(TAG, "Adapter: $adapterClass, Status: ${status.description}")
            }
            _isInitialized.value = true
            Log.d(TAG, "AdMob SDK initialized successfully")

            // Pre-load ads after initialization
            if (!_isPremium.value) {
                loadInterstitialAd()
                loadRewardedAd()
            }
        }
    }

    /**
     * Check if ads should be shown (user is not premium).
     */
    fun shouldShowAds(): Boolean {
        return !_isPremium.value
    }

    /**
     * Create an AdRequest for loading ads.
     */
    fun createAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    /**
     * Load an interstitial ad.
     */
    fun loadInterstitialAd() {
        if (_isPremium.value) {
            Log.d(TAG, "User is premium, skipping interstitial ad load")
            return
        }

        if (!_isInitialized.value) {
            Log.d(TAG, "AdMob SDK not initialized, cannot load interstitial")
            return
        }

        if (interstitialAd != null) {
            Log.d(TAG, "Interstitial ad already loaded")
            return
        }

        val adRequest = createAdRequest()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${adError.message}")
                    interstitialAd = null
                    _isInterstitialLoaded.value = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    interstitialAd = ad
                    _isInterstitialLoaded.value = true
                    setupInterstitialCallbacks()
                }
            }
        )
    }

    /**
     * Setup callbacks for the interstitial ad.
     */
    private fun setupInterstitialCallbacks() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Log.d(TAG, "Interstitial ad clicked")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed")
                interstitialAd = null
                _isInterstitialLoaded.value = false
                // Pre-load next interstitial
                loadInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                interstitialAd = null
                _isInterstitialLoaded.value = false
            }

            override fun onAdImpression() {
                Log.d(TAG, "Interstitial ad impression recorded")
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed")
            }
        }
    }

    /**
     * Show the interstitial ad if available.
     * @param activity The activity to show the ad in
     * @param onAdDismissed Callback when ad is dismissed or not shown
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (_isPremium.value) {
            Log.d(TAG, "User is premium, not showing interstitial")
            onAdDismissed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed")
                    interstitialAd = null
                    _isInterstitialLoaded.value = false
                    onAdDismissed()
                    // Pre-load next interstitial
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                    interstitialAd = null
                    _isInterstitialLoaded.value = false
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad showed")
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "Interstitial ad not available")
            onAdDismissed()
            // Try to load for next time
            loadInterstitialAd()
        }
    }

    /**
     * Check premium status synchronously.
     */
    suspend fun checkPremiumStatus(): Boolean {
        return withContext(Dispatchers.IO) {
            settingsRepository.isPremiumSync()
        }
    }

    // ==================== Frequency Cap Methods ====================

    /**
     * Check if interstitial ad should be shown based on frequency cap.
     * Returns true if we've reached the frequency cap threshold.
     */
    fun shouldShowInterstitialByFrequency(): Boolean {
        return studyCompletionCount >= INTERSTITIAL_FREQUENCY_CAP - 1
    }

    /**
     * Increment the study completion counter.
     * Call this when a study session completes.
     */
    fun incrementCompletionCount() {
        studyCompletionCount++
    }

    /**
     * Reset the completion counter after showing an ad.
     */
    private fun resetCompletionCount() {
        studyCompletionCount = 0
    }

    /**
     * Show interstitial ad with frequency cap check.
     * Only shows ad every Nth completion (defined by INTERSTITIAL_FREQUENCY_CAP).
     *
     * @param activity The activity to show the ad in
     * @param onComplete Callback when ad is dismissed or skipped
     * @return true if ad was shown, false if skipped due to frequency cap
     */
    fun showInterstitialWithFrequencyCap(
        activity: Activity,
        onComplete: () -> Unit = {}
    ): Boolean {
        if (_isPremium.value) {
            Log.d(TAG, "User is premium, skipping interstitial")
            onComplete()
            return false
        }

        // Check frequency cap
        if (!shouldShowInterstitialByFrequency()) {
            Log.d(TAG, "Frequency cap not reached (${studyCompletionCount + 1}/$INTERSTITIAL_FREQUENCY_CAP), skipping ad")
            incrementCompletionCount()
            onComplete()
            return false
        }

        // Show ad
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed (with frequency cap)")
                    interstitialAd = null
                    _isInterstitialLoaded.value = false
                    resetCompletionCount()
                    onComplete()
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                    interstitialAd = null
                    _isInterstitialLoaded.value = false
                    incrementCompletionCount()
                    onComplete()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad showed (frequency cap triggered)")
                }
            }
            ad.show(activity)
            return true
        } else {
            Log.d(TAG, "No interstitial ad available, incrementing counter")
            incrementCompletionCount()
            onComplete()
            loadInterstitialAd()
            return false
        }
    }

    /**
     * Get current completion count (for debugging/testing).
     */
    fun getCompletionCount(): Int = studyCompletionCount

    // ==================== Rewarded Ad Methods ====================

    /**
     * Load a rewarded ad for unit unlock.
     */
    fun loadRewardedAd() {
        if (_isPremium.value) {
            Log.d(TAG, "User is premium, skipping rewarded ad load")
            return
        }

        if (!_isInitialized.value) {
            Log.d(TAG, "AdMob SDK not initialized, cannot load rewarded ad")
            return
        }

        if (rewardedAd != null) {
            Log.d(TAG, "Rewarded ad already loaded")
            return
        }

        val adRequest = createAdRequest()
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${adError.message}")
                    rewardedAd = null
                    _isRewardedLoaded.value = false
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded successfully")
                    rewardedAd = ad
                    _isRewardedLoaded.value = true
                }
            }
        )
    }

    /**
     * Show a rewarded ad for unit unlock.
     * @param activity The activity to show the ad in
     * @param onRewarded Callback when user earns the reward
     * @param onAdDismissed Callback when ad is dismissed without reward
     */
    fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        if (_isPremium.value) {
            Log.d(TAG, "User is premium, not showing rewarded ad")
            onRewarded() // Grant reward for premium users
            return
        }

        val ad = rewardedAd
        if (ad != null) {
            var wasRewarded = false

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    Log.d(TAG, "Rewarded ad clicked")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed, wasRewarded=$wasRewarded")
                    rewardedAd = null
                    _isRewardedLoaded.value = false

                    if (wasRewarded) {
                        onRewarded()
                    } else {
                        onAdDismissed()
                    }

                    // Pre-load next rewarded ad
                    loadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                    rewardedAd = null
                    _isRewardedLoaded.value = false
                    onAdDismissed()
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Rewarded ad impression recorded")
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad showed")
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                wasRewarded = true
            }
        } else {
            Log.d(TAG, "Rewarded ad not available")
            onAdDismissed()
            // Try to load for next time
            loadRewardedAd()
        }
    }
}
