package com.dartdl.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.dartdl.app.App.Companion.context
import com.dartdl.app.ui.common.LocalDarkTheme
import com.dartdl.app.ui.common.SettingsProvider
import com.dartdl.app.ui.page.AppEntry
import com.dartdl.app.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.dartdl.app.ui.theme.DartDLTheme
import com.dartdl.app.util.AdManager
import com.dartdl.app.util.PreferenceUtil
import com.dartdl.app.util.matchUrlFromSharedText
import com.dartdl.app.util.setLanguage
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.KoinContext

class MainActivity : AppCompatActivity() {
    private val dialogViewModel: DownloadDialogViewModel by viewModel()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < 33) {
            setLanguage(PreferenceUtil.getLocaleFromPreference())
        }
        enableEdgeToEdge()

        context = this.baseContext
        setContent {
            KoinContext {
                val windowSizeClass = calculateWindowSizeClass(this)
                SettingsProvider(windowWidthSizeClass = windowSizeClass.widthSizeClass) {
                    DartDLTheme(
                            darkTheme = LocalDarkTheme.current.isDarkTheme(),
                            isHighContrastModeEnabled =
                                    LocalDarkTheme.current.isHighContrastModeEnabled,
                    ) {
                        AppEntry(dialogViewModel = dialogViewModel)
                    }
                }
            }
        }

        // Issue 6 Fix: Process share intent from cold start.
        // onNewIntent handles the case when app is already running.
        // This handles TikTok, Instagram, YouTube, etc. when the app is CLOSED.
        // savedInstanceState == null prevents re-processing on activity recreation (rotation).
        if (savedInstanceState == null) {
            val url = intent.getSharedURL()
            if (url != null) {
                dialogViewModel.postAction(DownloadDialogViewModel.Action.ShowSheet(listOf(url)))
            }
        }

        // Preload ads
        AdManager.loadInterstitial(this)
        AdManager.loadRewarded(this)
    }

    fun showInterstitialAd(onDismissed: () -> Unit = {}) {
        AdManager.showInterstitial(this, onDismissed = onDismissed)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val url = intent.getSharedURL()
        if (url != null) {
            dialogViewModel.postAction(DownloadDialogViewModel.Action.ShowSheet(listOf(url)))
        }
    }

    private fun Intent.getSharedURL(): String? {
        val intent = this

        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.dataString
            }
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedContent ->
                    intent.removeExtra(Intent.EXTRA_TEXT)
                    matchUrlFromSharedText(sharedContent).also { matchedUrl ->
                        if (sharedUrlCached != matchedUrl) {
                            sharedUrlCached = matchedUrl
                        }
                    }
                }
            }
            else -> {
                null
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private var sharedUrlCached = ""
    }
}
