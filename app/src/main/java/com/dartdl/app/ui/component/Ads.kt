package com.dartdl.app.ui.component

import android.util.DisplayMetrics
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dartdl.app.util.AdManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import android.os.Bundle
import com.google.ads.mediation.admob.AdMobAdapter

@Composable
fun AdmobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-1760578259657250/2034875086", // Example banner ID, should be replaced with user's
    isCollapsible: Boolean = true
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = {
            AdView(it).apply {
                setAdUnitId(adUnitId)
                setAdSize(getAdSize(it))
                
                val adRequestBuilder = AdRequest.Builder()
                if (isCollapsible) {
                    val extras = Bundle()
                    extras.putString("collapsible", "bottom")
                    adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                }
                loadAd(adRequestBuilder.build())
            }
        },
        update = { adView ->
            // Update logic if needed
        },
        onRelease = { adView ->
            adView.destroy()
        }
    )
}

private fun getAdSize(context: android.content.Context): AdSize {
    val displayMetrics = context.resources.displayMetrics
    val adWidthPixels = displayMetrics.widthPixels.toFloat()
    val density = displayMetrics.density
    val adWidth = (adWidthPixels / density).toInt()
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
}
