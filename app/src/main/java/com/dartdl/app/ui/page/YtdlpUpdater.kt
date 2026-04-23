package com.dartdl.app.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dartdl.app.App
import com.dartdl.app.Downloader
import com.dartdl.app.R
import com.dartdl.app.util.PreferenceUtil
import com.dartdl.app.util.PreferenceUtil.getBoolean
import com.dartdl.app.util.PreferenceUtil.getLong
import com.dartdl.app.util.PreferenceUtil.getString
import com.dartdl.app.util.UpdateUtil
import com.dartdl.app.util.YT_DLP_AUTO_UPDATE
import com.dartdl.app.util.YT_DLP_UPDATE_INTERVAL
import com.dartdl.app.util.YT_DLP_UPDATE_TIME
import com.dartdl.app.util.YT_DLP_VERSION
import com.dartdl.app.util.makeToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun YtdlpUpdater() {

    val downloaderState by Downloader.downloaderState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (downloaderState !is Downloader.State.Idle) return@LaunchedEffect

        val isYtDlpInstalled = YT_DLP_VERSION.getString().isNotEmpty()
        val autoUpdateEnabled = YT_DLP_AUTO_UPDATE.getBoolean()
        val lastUpdateTime = YT_DLP_UPDATE_TIME.getLong()
        val currentTime = System.currentTimeMillis()

        // Only skip if yt-dlp is already installed AND auto-update is turned off 
        // AND it's not the first launch (lastUpdateTime > 0)
        if (!autoUpdateEnabled && isYtDlpInstalled && lastUpdateTime != 0L) return@LaunchedEffect

        if (!PreferenceUtil.isNetworkAvailableForDownload()) {
            return@LaunchedEffect
        }

        // first install (lastUpdateTime == 0) always runs, regardless of interval
        if (isYtDlpInstalled && lastUpdateTime != 0L && currentTime < lastUpdateTime + YT_DLP_UPDATE_INTERVAL.getLong()) {
            return@LaunchedEffect
        }

        kotlin.runCatching {
                    Downloader.updateState(state = Downloader.State.Updating)
                    withContext(Dispatchers.Main) {
                        if (!isYtDlpInstalled) {
                            App.context.makeToast(R.string.yt_dlp_initializing)
                        }
                    }
                    withContext(Dispatchers.IO) { UpdateUtil.updateYtDlp() }
                    withContext(Dispatchers.Main) {
                        if (!isYtDlpInstalled) {
                            App.context.makeToast(R.string.yt_dlp_update_success)
                        }
                    }
                    Unit
                }
                .onFailure { it.printStackTrace() }
        Downloader.updateState(state = Downloader.State.Idle)
    }
}
