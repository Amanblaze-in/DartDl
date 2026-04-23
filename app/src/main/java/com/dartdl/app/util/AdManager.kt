package com.dartdl.app.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    private const val TAG = "AdManager"

    // Ad Unit IDs
    const val INTERSTITIAL_ID = "ca-app-pub-1760578259657250/3677467306"
    const val REWARDED_ID = "ca-app-pub-1760578259657250/1674120498"
    const val APP_OPEN_ID = "ca-app-pub-1760578259657250/7891315537"
    const val NATIVE_ID = "ca-app-pub-1760578259657250/5981723095"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var nativeAds = mutableListOf<NativeAd>()
    
    var isRewardedAdLoading = false
        private set

    val isRewardedAdReady: Boolean
        get() = rewardedAd != null

    
    // Frequency cap for back ads (2 minutes)
    private var lastBackAdTime: Long = 0
    private const val BACK_AD_INTERVAL = 10 * 60 * 1000L 

    fun initialize(context: Context) {
        MobileAds.initialize(context) { status ->
            Log.d(TAG, "AdMob Initialized: $status")
            loadInterstitial(context)
            loadRewarded(context)
            loadNativeAds(context)
        }
    }

    fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Interstitial failed to load: ${adError.message}")
                    interstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Interstitial loaded successfully")
                    this@AdManager.interstitialAd = interstitialAd
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, isBackAction: Boolean = false, onDismissed: () -> Unit = {}) {
        val currentTime = System.currentTimeMillis()
        
        // Apply frequency cap for back button ads
        if (isBackAction && (currentTime - lastBackAdTime < BACK_AD_INTERVAL)) {
            Log.d(TAG, "Back action ad skipped due to frequency cap")
            onDismissed()
            return
        }

        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial dismissed")
                    if (isBackAction) lastBackAdTime = System.currentTimeMillis()
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.d(TAG, "Interstitial failed to show: ${adError.message}")
                    interstitialAd = null
                    onDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "Interstitial not ready yet")
            loadInterstitial(activity)
            onDismissed()
        }
    }

    fun loadRewarded(context: Context, onLoaded: ((Boolean) -> Unit)? = null) {
        if (rewardedAd != null) {
            onLoaded?.invoke(true)
            return
        }
        if (isRewardedAdLoading) return
        
        isRewardedAdLoading = true
        val adRequest = AdRequest.Builder().build()
        Log.d(TAG, "Loading rewarded ad...")
        RewardedAd.load(
            context,
            REWARDED_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${adError.message} (Code: ${adError.code})")
                    rewardedAd = null
                    isRewardedAdLoading = false
                    onLoaded?.invoke(false)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded successfully")
                    rewardedAd = ad
                    isRewardedAdLoading = false
                    onLoaded?.invoke(true)
                }
            }
        )
    }

    fun showRewarded(activity: Activity, onComplete: (Boolean) -> Unit) {
        if (rewardedAd != null) {
            var rewardEarned = false
            rewardedAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed")
                    rewardedAd = null
                    loadRewarded(activity)
                    onComplete(rewardEarned)
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.d(TAG, "Rewarded ad failed to show: ${adError.message}")
                    rewardedAd = null
                    loadRewarded(activity)
                    onComplete(false)
                }
            }
            rewardedAd?.show(activity) { _ ->
                Log.d(TAG, "User earned reward")
                rewardEarned = true
            }
        } else {
            Log.d(TAG, "Rewarded ad not ready")
            loadRewarded(activity)
            onComplete(false)
        }
    }

    private fun loadNativeAds(context: Context) {
        Log.d(TAG, "Loading native ads (Current pool size: ${nativeAds.size})")
        val adLoader = AdLoader.Builder(context, NATIVE_ID)
            .forNativeAd { ad : NativeAd ->
                Log.d(TAG, "Native ad received")
                nativeAds.add(ad)
                if (nativeAds.size > 5) {
                    val oldAd = nativeAds.removeAt(0)
                    oldAd.destroy()
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Native ad failed to load: ${adError.message} (Code: ${adError.code})")
                }
                
                override fun onAdClicked() {
                    Log.d(TAG, "Native ad clicked")
                }
            })
            .build()
        adLoader.loadAds(AdRequest.Builder().build(), 3)
    }

    fun getNativeAdsCount(): Int = nativeAds.size

    fun getNativeAd(context: Context): NativeAd? {
        if (nativeAds.isEmpty()) {
            loadNativeAds(context)
            return null
        }
        val ad = nativeAds.removeAt(0)
        loadNativeAds(context) // Refresh the pool
        return ad
    }
}
