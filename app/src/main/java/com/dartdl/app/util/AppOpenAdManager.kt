package com.dartdl.app.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.dartdl.app.CrashReportActivity
import java.util.Date

/**
 * Handles AdMob App Open ads.
 */
class AppOpenAdManager(private val myApplication: Application) :
    Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null
    private var isFirstLaunch = true

    init {
        myApplication.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * Request an ad.
     */
    fun fetchAd() {
        if (!AdManager.isInitialized) return
        if (isAdAvailable()) return

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            myApplication,
            AdManager.APP_OPEN_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Log.d(LOG_TAG, "App Open Ad loaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Log.d(LOG_TAG, "App Open Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Utility method that checks if ad exists and can be shown.
     */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanFourHoursAgo()
    }

    private fun wasLoadTimeLessThanFourHoursAgo(): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * 4
    }

    /**
     * Show the ad if one is available or fetch one.
     */
    fun showAdIfAvailable(activity: Activity) {
        if (isShowingAd) return

        val currentAd = appOpenAd
        if (currentAd == null) {
            fetchAd()
            return
        }

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                fetchAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                appOpenAd = null
                isShowingAd = false
                fetchAd()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }
        try {
            currentAd.show(activity)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to show App Open Ad", e)
            appOpenAd = null
            isShowingAd = false
            fetchAd()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (isFirstLaunch) {
            Log.d(LOG_TAG, "First launch detected, skipping app open ad")
            isFirstLaunch = false
            return
        }
        
        // Additional safety: ensure we have an activity and it's not the SplashActivity
        val activity = currentActivity
        if (activity != null && activity !is CrashReportActivity) {
            showAdIfAvailable(activity)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
    }

    companion object {
        private const val LOG_TAG = "AppOpenAdManager"
    }
}
