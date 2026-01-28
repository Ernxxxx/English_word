package com.example.englishword.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.englishword.ads.AdManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "BannerAdView"

/**
 * Composable wrapper for AdMob banner ads.
 * Automatically hides if user has premium subscription.
 *
 * @param adManager The AdManager instance for checking premium status
 * @param modifier Modifier for the banner container
 * @param adUnitId Custom ad unit ID (defaults to test ID)
 */
@Composable
fun BannerAdView(
    adManager: AdManager,
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.BANNER_AD_UNIT_ID
) {
    val isPremium by adManager.isPremium.collectAsState()
    val isInitialized by adManager.isInitialized.collectAsState()

    // Don't show ads for premium users
    if (isPremium) {
        return
    }

    // Wait for SDK initialization
    if (!isInitialized) {
        // Show placeholder while loading
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.Transparent)
        )
        return
    }

    BannerAdContent(
        adUnitId = adUnitId,
        modifier = modifier
    )
}

/**
 * Internal composable that renders the actual AdMob banner.
 */
@Composable
private fun BannerAdContent(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }

    // Create AdView and manage its lifecycle
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId

            adListener = object : AdListener() {
                override fun onAdClicked() {
                    Log.d(TAG, "Banner ad clicked")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "Banner ad closed")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Banner ad failed to load: ${adError.message}")
                    isAdLoaded = false
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Banner ad impression recorded")
                }

                override fun onAdLoaded() {
                    Log.d(TAG, "Banner ad loaded")
                    isAdLoaded = true
                }

                override fun onAdOpened() {
                    Log.d(TAG, "Banner ad opened")
                }
            }
        }
    }

    // Load the ad
    DisposableEffect(adView) {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        onDispose {
            adView.destroy()
        }
    }

    // Render the AdView
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Banner ad with adaptive sizing.
 * Uses the full width of the container for better fill rate.
 *
 * @param adManager The AdManager instance for checking premium status
 * @param modifier Modifier for the banner container
 * @param adUnitId Custom ad unit ID (defaults to test ID)
 */
@Composable
fun AdaptiveBannerAdView(
    adManager: AdManager,
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.BANNER_AD_UNIT_ID
) {
    val isPremium by adManager.isPremium.collectAsState()
    val isInitialized by adManager.isInitialized.collectAsState()

    // Don't show ads for premium users
    if (isPremium) {
        return
    }

    // Wait for SDK initialization
    if (!isInitialized) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.Transparent)
        )
        return
    }

    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }

    // Create AdView with adaptive banner
    val adView = remember {
        AdView(context).apply {
            // Get adaptive banner size based on screen width
            val displayMetrics = context.resources.displayMetrics
            val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
            val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)

            setAdSize(adaptiveSize)
            this.adUnitId = adUnitId

            adListener = object : AdListener() {
                override fun onAdClicked() {
                    Log.d(TAG, "Adaptive banner ad clicked")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "Adaptive banner ad closed")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Adaptive banner ad failed to load: ${adError.message}")
                    isAdLoaded = false
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Adaptive banner ad impression recorded")
                }

                override fun onAdLoaded() {
                    Log.d(TAG, "Adaptive banner ad loaded")
                    isAdLoaded = true
                }

                override fun onAdOpened() {
                    Log.d(TAG, "Adaptive banner ad opened")
                }
            }
        }
    }

    // Load the ad
    DisposableEffect(adView) {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        onDispose {
            adView.destroy()
        }
    }

    // Render the AdView
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Simplified banner ad composable that checks premium status internally.
 * Use this when AdManager is injected via ViewModel.
 *
 * @param isPremium Whether the user has premium subscription
 * @param modifier Modifier for the banner container
 * @param adUnitId Custom ad unit ID (defaults to test ID)
 */
@Composable
fun SimpleBannerAdView(
    isPremium: Boolean,
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.BANNER_AD_UNIT_ID
) {
    // Don't show ads for premium users
    if (isPremium) {
        return
    }

    BannerAdContent(
        adUnitId = adUnitId,
        modifier = modifier
    )
}
