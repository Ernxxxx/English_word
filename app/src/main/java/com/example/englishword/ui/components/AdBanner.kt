package com.example.englishword.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Composable for displaying a banner ad.
 * Automatically hides when the user is premium.
 *
 * @param adUnitId The AdMob ad unit ID for the banner
 * @param isPremium Whether the user has premium (hides banner if true)
 * @param modifier Modifier for the banner container
 */
@Composable
fun BannerAdView(
    adUnitId: String,
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    // Don't show banner for premium users
    if (isPremium) {
        return
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // Reload ad if needed
            if (adView.adUnitId != adUnitId) {
                adView.adUnitId = adUnitId
                adView.loadAd(AdRequest.Builder().build())
            }
        }
    )
}
