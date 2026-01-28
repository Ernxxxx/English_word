package com.example.englishword.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.englishword.data.repository.SettingsRepository
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages interstitial ads with frequency capping.
 * Shows interstitial ads on study completion, but not every time.
 */
@Singleton
class InterstitialAdManager @Inject constructor(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "InterstitialAdManager"

        // Test Ad Unit ID - Replace with real ID for production
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

        // Frequency capping: show ad every N study completions
        const val AD_FREQUENCY = 3

        // Minimum time between ads (in milliseconds) - 2 minutes
        const val MIN_AD_INTERVAL_MS = 2 * 60 * 1000L
    }

    private var interstitialAd: InterstitialAd? = null

    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Frequency capping counters
    private var studyCompletionCount = 0
    private var lastAdShownTime = 0L

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    /**
     * Load an interstitial ad.
     * Should be called after AdMob SDK is initialized.
     */
    fun loadAd() {
        coroutineScope.launch {
            // Check if user is premium
            val isPremium = settingsRepository.isPremium().first()
            if (isPremium) {
                Log.d(TAG, "User is premium, skipping ad load")
                return@launch
            }

            if (_isLoading.value || interstitialAd != null) {
                Log.d(TAG, "Ad already loading or loaded")
                return@launch
            }

            _isLoading.value = true

            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                INTERSTITIAL_AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(TAG, "Failed to load interstitial ad: ${adError.message}")
                        interstitialAd = null
                        _isAdLoaded.value = false
                        _isLoading.value = false
                    }

                    override fun onAdLoaded(ad: InterstitialAd) {
                        Log.d(TAG, "Interstitial ad loaded")
                        interstitialAd = ad
                        _isAdLoaded.value = true
                        _isLoading.value = false
                    }
                }
            )
        }
    }

    /**
     * Called when a study session is completed.
     * Determines whether to show an ad based on frequency capping.
     *
     * @param activity The activity to show the ad in
     * @param onComplete Callback when ad is dismissed or not shown
     */
    fun onStudyCompleted(activity: Activity, onComplete: () -> Unit) {
        coroutineScope.launch {
            // Check if user is premium
            val isPremium = settingsRepository.isPremium().first()
            if (isPremium) {
                Log.d(TAG, "User is premium, not showing ad")
                onComplete()
                return@launch
            }

            studyCompletionCount++

            // Check frequency capping
            val shouldShowAd = shouldShowAd()

            if (shouldShowAd) {
                showAd(activity, onComplete)
            } else {
                Log.d(TAG, "Skipping ad due to frequency capping (count: $studyCompletionCount)")
                onComplete()
            }
        }
    }

    /**
     * Determine if an ad should be shown based on frequency capping rules.
     */
    private fun shouldShowAd(): Boolean {
        val currentTime = System.currentTimeMillis()

        // Check minimum time interval
        if (currentTime - lastAdShownTime < MIN_AD_INTERVAL_MS) {
            Log.d(TAG, "Too soon since last ad")
            return false
        }

        // Check frequency (show every N completions)
        return studyCompletionCount % AD_FREQUENCY == 0
    }

    /**
     * Show the interstitial ad.
     *
     * @param activity The activity to show the ad in
     * @param onComplete Callback when ad is dismissed or not shown
     */
    fun showAd(activity: Activity, onComplete: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "No ad available to show")
            onComplete()
            // Try to load for next time
            loadAd()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Log.d(TAG, "Ad clicked")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                lastAdShownTime = System.currentTimeMillis()
                interstitialAd = null
                _isAdLoaded.value = false
                onComplete()
                // Pre-load next ad
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Failed to show ad: ${adError.message}")
                interstitialAd = null
                _isAdLoaded.value = false
                onComplete()
            }

            override fun onAdImpression() {
                Log.d(TAG, "Ad impression recorded")
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed")
            }
        }

        ad.show(activity)
    }

    /**
     * Force show an ad regardless of frequency capping.
     * Use sparingly (e.g., before major features).
     *
     * @param activity The activity to show the ad in
     * @param onComplete Callback when ad is dismissed or not shown
     */
    fun forceShowAd(activity: Activity, onComplete: () -> Unit) {
        coroutineScope.launch {
            // Check if user is premium
            val isPremium = settingsRepository.isPremium().first()
            if (isPremium) {
                Log.d(TAG, "User is premium, not showing ad")
                onComplete()
                return@launch
            }

            showAd(activity, onComplete)
        }
    }

    /**
     * Reset the frequency capping counter.
     * Can be used when user upgrades to premium and then cancels.
     */
    fun resetFrequencyCounter() {
        studyCompletionCount = 0
        lastAdShownTime = 0L
    }

    /**
     * Check if an ad is ready to show.
     */
    fun isAdReady(): Boolean {
        return interstitialAd != null
    }
}
