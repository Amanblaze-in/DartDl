package com.dartdl.app.ui.component

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dartdl.app.R
import com.dartdl.app.util.AdManager
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(AdManager.getNativeAd(context)) }

    if (nativeAd == null) {
        LaunchedEffect(Unit) {
            // Poll for ad if not ready
            while (nativeAd == null) {
                val ad = AdManager.getNativeAd(context)
                if (ad != null) {
                    nativeAd = ad
                } else {
                    kotlinx.coroutines.delay(2000) // Wait 2 seconds before retry
                }
            }
        }
    }

    if (nativeAd != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                factory = { ctx ->
                    val adView = LayoutInflater.from(ctx)
                        .inflate(R.layout.ad_unified, null) as NativeAdView
                    
                    // Populate adView with nativeAd data
                    adView.headlineView = adView.findViewById(R.id.ad_headline)
                    adView.bodyView = adView.findViewById(R.id.ad_body)
                    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                    adView.iconView = adView.findViewById(R.id.ad_app_icon)
                    adView.priceView = adView.findViewById(R.id.ad_price)
                    adView.starRatingView = adView.findViewById(R.id.ad_stars)
                    adView.storeView = adView.findViewById(R.id.ad_store)
                    adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
                    adView.mediaView = adView.findViewById(R.id.ad_media)

                    val ad = nativeAd
                    if (ad != null) {
                        (adView.headlineView as TextView).text = ad.headline
                        ad.mediaContent?.let {
                            adView.mediaView?.setMediaContent(it)
                        }

                        if (ad.body == null) {
                            adView.bodyView?.visibility = View.INVISIBLE
                        } else {
                            adView.bodyView?.visibility = View.VISIBLE
                            (adView.bodyView as TextView).text = ad.body
                        }

                        if (ad.callToAction == null) {
                            adView.callToActionView?.visibility = View.INVISIBLE
                        } else {
                            adView.callToActionView?.visibility = View.VISIBLE
                            (adView.callToActionView as Button).text = ad.callToAction
                        }

                        if (ad.icon == null) {
                            adView.iconView?.visibility = View.GONE
                        } else {
                            (adView.iconView as ImageView).setImageDrawable(ad.icon?.drawable)
                            adView.iconView?.visibility = View.VISIBLE
                        }

                        if (ad.price == null) {
                            adView.priceView?.visibility = View.INVISIBLE
                        } else {
                            adView.priceView?.visibility = View.VISIBLE
                            (adView.priceView as TextView).text = ad.price
                        }

                        if (ad.store == null) {
                            adView.storeView?.visibility = View.INVISIBLE
                        } else {
                            adView.storeView?.visibility = View.VISIBLE
                            (adView.storeView as TextView).text = ad.store
                        }

                        if (ad.starRating == null) {
                            adView.starRatingView?.visibility = View.INVISIBLE
                        } else {
                            (adView.starRatingView as RatingBar).rating = ad.starRating!!.toFloat()
                            adView.starRatingView?.visibility = View.VISIBLE
                        }

                        if (ad.advertiser == null) {
                            adView.advertiserView?.visibility = View.INVISIBLE
                        } else {
                            (adView.advertiserView as TextView).text = ad.advertiser
                            adView.advertiserView?.visibility = View.VISIBLE
                        }

                        adView.setNativeAd(ad)
                    }
                    adView
                }
            )
        }
    }
}
