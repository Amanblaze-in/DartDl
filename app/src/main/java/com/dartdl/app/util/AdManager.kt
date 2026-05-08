package com.dartdl.app.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.unity3d.ads.UnityAds

object AdManager {
    private const val TAG = "AdManager"

    // Ad Unit IDs
    const val INTERSTITIAL_ID = "ca-app-pub-1760578259657250/3677467306"
    const val REWARDED_ID = "ca-app-pub-1760578259657250/1674120498"
    const val APP_OPEN_ID = "ca-app-pub-1760578259657250/7891315537"
    const val NATIVE_ID = "ca-app-pub-1760578259657250/5981723095"
    const val BANNER_ID = "ca-app-pub-1760578259657250/8936254101"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var nativeAds = mutableListOf<NativeAd>()
    private val interstitialCallbacks = mutableListOf<() -> Unit>()
    private val rewardedCallbacks = mutableListOf<(Boolean) -> Unit>()
    
    var isInterstitialLoading = false
        private set

    var isRewardedAdLoading = false
        private set

    val isRewardedAdReady: Boolean
        get() = rewardedAd != null

    private var _consentInformation: ConsentInformation? = null
    private val consentInformation: ConsentInformation
        get() = _consentInformation ?: throw IllegalStateException("AdManager not initialized with activity context")
    
    private var sessionInterstitialCount = 0
    private const val MAX_INTERSTITIALS_PER_SESSION = 50

    private var lastInterAdTime: Long = 0
    private const val INTER_AD_INTERVAL = 15 * 1000L // Reduced to 15 seconds

    private var isConsentInProgress = false

    var isInitialized = false
        private set

    fun gatherConsentAndInitialize(activity: Activity, onComplete: () -> Unit = {}) {
        if (isConsentInProgress) return
        
        _consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val info = _consentInformation!!
        
        isConsentInProgress = true
        
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        Log.w(TAG, "Consent error: ${loadAndShowError.errorCode} - ${loadAndShowError.message}")
                    }
                    initialize(activity)
                    isConsentInProgress = false
                    onComplete()
                }
            },
            { requestConsentError ->
                Log.w(TAG, "Consent request error: ${requestConsentError.errorCode} - ${requestConsentError.message}")
                if (info.canRequestAds()) {
                    initialize(activity)
                }
                isConsentInProgress = false
                onComplete()
            }
        )
        
        if (info.canRequestAds()) {
            initialize(activity)
        }
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        
        // Initialize Meta Audience Network
        try {
            com.facebook.ads.AudienceNetworkAds.initialize(context)
            Log.d(TAG, "Meta Audience Network SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Meta Init Failed: ${e.message}", e)
        }
        
        // Initialize Unity Ads
        try {
            com.unity3d.ads.UnityAds.initialize(context, "6102678", false,
                object : com.unity3d.ads.IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        Log.d(TAG, "Unity Ads initialized successfully")
                    }
                    override fun onInitializationFailed(
                        error: com.unity3d.ads.UnityAds.UnityAdsInitializationError?,
                        message: String?
                    ) {
                        Log.e(TAG, "Unity Ads init failed: $error - $message")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unity Init Failed: ${e.message}", e)
        }
        
        MobileAds.initialize(context) { status ->
            isInitialized = true
            
            // Log detailed mediation adapter status
            val adapterMap = status.adapterStatusMap
            for ((adapterClass, adapterStatus) in adapterMap) {
                Log.d(TAG, "Adapter: $adapterClass | State: ${adapterStatus.initializationState} | Description: ${adapterStatus.description} | Latency: ${adapterStatus.latency}ms")
            }
            
            Log.d(TAG, "AdMob Mediation Initialized. Total adapters: ${adapterMap.size}")
            loadInterstitial(context)
            loadRewarded(context)
            loadNativeAds(context)
        }
    }

    fun loadInterstitial(context: Context, onLoaded: (() -> Unit)? = null) {
        if (!isInitialized) {
            onLoaded?.invoke()
            return
        }
        if (interstitialAd != null) {
            onLoaded?.invoke()
            return
        }
        
        onLoaded?.let { interstitialCallbacks.add(it) }
        
        if (isInterstitialLoading) return
        
        Log.d(TAG, "Loading interstitial ad...")
        isInterstitialLoading = true
        
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Interstitial failed to load: code=${adError.code}, message=${adError.message}")
                    interstitialAd = null
                    isInterstitialLoading = false
                    notifyInterstitialCallbacks()
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial loaded successfully")
                    interstitialAd = ad
                    isInterstitialLoading = false
                    notifyInterstitialCallbacks()
                }
            }
        )

        // Safety timeout for interstitial
        if (context is Activity) {
            context.window.decorView.postDelayed({
                if (isInterstitialLoading) {
                    Log.w(TAG, "Interstitial ad load timeout reached")
                    isInterstitialLoading = false
                    notifyInterstitialCallbacks()
                }
            }, 8000) // 8 second timeout
        }
    }

    private fun notifyInterstitialCallbacks() {
        val callbacks = ArrayList(interstitialCallbacks)
        interstitialCallbacks.clear()
        callbacks.forEach { it.invoke() }
    }

    fun showInterstitial(activity: Activity, isBackAction: Boolean = false, onDismissed: () -> Unit = {}) {
        if (!isBackAction && sessionInterstitialCount >= MAX_INTERSTITIALS_PER_SESSION) {
            onDismissed()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (!isBackAction && currentTime - lastInterAdTime < INTER_AD_INTERVAL) {
            Log.d(TAG, "Interstitial interval not reached yet")
            onDismissed()
            return
        }

        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    lastInterAdTime = System.currentTimeMillis()
                    sessionInterstitialCount++
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.e(TAG, "Interstitial failed to show: ${adError.message}")
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDismissed()
                }
            }
            try {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    interstitialAd?.show(activity)
                } else {
                    onDismissed()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing interstitial ad", e)
                interstitialAd = null
                onDismissed()
            }
        } else {
            Log.d(TAG, "Interstitial ad not ready, loading and waiting...")
            loadInterstitial(activity) {
                // Try showing again if it loaded successfully during the wait
                if (interstitialAd != null) {
                    showInterstitial(activity, isBackAction, onDismissed)
                } else {
                    onDismissed()
                }
            }
        }
    }

    fun loadRewarded(context: Context, onLoaded: ((Boolean) -> Unit)? = null) {
        if (!isInitialized) {
            onLoaded?.invoke(false)
            return
        }
        if (rewardedAd != null) {
            onLoaded?.invoke(true)
            return
        }
        
        onLoaded?.let { rewardedCallbacks.add(it) }
        
        if (isRewardedAdLoading) return
        
        Log.d(TAG, "Loading rewarded ad...")
        isRewardedAdLoading = true
        
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: code=${adError.code}, message=${adError.message}, domain=${adError.domain}")
                    rewardedAd = null
                    isRewardedAdLoading = false
                    notifyRewardedCallbacks(false)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded successfully")
                    rewardedAd = ad
                    isRewardedAdLoading = false
                    notifyRewardedCallbacks(true)
                }
            }
        )

        // Safety timeout: if ad doesn't load in 10 seconds, notify failure to avoid hanging UI
        if (context is Activity) {
            context.window.decorView.postDelayed({
                if (isRewardedAdLoading) {
                    Log.w(TAG, "Rewarded ad load timeout reached")
                    isRewardedAdLoading = false
                    notifyRewardedCallbacks(false)
                }
            }, 10000) // 10 second timeout
        }
    }

    private fun notifyRewardedCallbacks(success: Boolean) {
        val callbacks = ArrayList(rewardedCallbacks)
        rewardedCallbacks.clear()
        callbacks.forEach { it.invoke(success) }
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
                    Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                    rewardedAd = null
                    loadRewarded(activity)
                    onComplete(false)
                }
            }
            try {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    rewardedAd?.show(activity) { _ ->
                        Log.d(TAG, "User earned reward")
                        rewardEarned = true
                    }
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing rewarded ad", e)
                rewardedAd = null
                onComplete(false)
            }
        } else {
            Log.d(TAG, "Rewarded ad not ready when showRewarded called, loading...")
            loadRewarded(activity) { success ->
                // Note: we don't auto-show here because it might be unexpected UX
                // The UI should handle the waiting state.
                onComplete(false)
            }
        }
    }

    fun loadNativeAds(context: Context) {
        if (!isInitialized) return

        val adLoader = AdLoader.Builder(context, NATIVE_ID)
            .forNativeAd { ad : NativeAd ->
                nativeAds.add(ad)
                if (nativeAds.size > 5) {
                    val oldAd = nativeAds.removeAt(0)
                    oldAd.destroy()
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Native ad failed to load: code=${adError.code}, message=${adError.message}")
                }
            })
            .build()
        adLoader.loadAds(AdRequest.Builder().build(), 3)
    }

    fun getNativeAd(context: Context): NativeAd? {
        if (nativeAds.isEmpty()) {
            loadNativeAds(context)
            return null
        }
        val ad = nativeAds.removeAt(0)
        loadNativeAds(context)
        return ad
    }
}
