package com.dartdl.app.ui.page.downloadv2.configure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartdl.app.App
import com.dartdl.app.database.objects.CommandTemplate
import com.dartdl.app.download.DownloaderV2
import com.dartdl.app.download.Task
import com.dartdl.app.util.DownloadUtil
import com.dartdl.app.util.PlaylistResult
import com.dartdl.app.util.PreferenceUtil
import com.dartdl.app.util.VideoInfo
import com.dartdl.app.util.makeToast
import com.dartdl.app.util.NotificationUtil
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "DownloadDialogViewModel"

class DownloadDialogViewModel(private val downloader: DownloaderV2) : ViewModel() {

    sealed interface SelectionState {
        data object Idle : SelectionState

        data class PlaylistSelection(val result: PlaylistResult) : SelectionState

        data class FormatSelection(val info: VideoInfo) : SelectionState
    }

    sealed interface SheetState {
        data object InputUrl : SheetState

        data class Configure(val urlList: List<String>) : SheetState

        data class Loading(val taskKey: String, val job: Job) : SheetState

        data class Error(val action: Action, val throwable: Throwable) : SheetState
    }

    sealed interface SheetValue {
        data object Expanded : SheetValue

        data object Hidden : SheetValue
    }

    sealed interface Action {
        data object HideSheet : Action

        data class ShowSheet(val urlList: List<String>? = null) : Action

        data class ProceedWithURLs(val urlList: List<String>) : Action

        data object Reset : Action

        data class FetchPlaylist(
                val url: String,
                val preferences: DownloadUtil.DownloadPreferences,
        ) : Action

        data class FetchFormats(
                val url: String,
                val audioOnly: Boolean,
                val preferences: DownloadUtil.DownloadPreferences,
        ) : Action

        data class DownloadWithPreset(
                val urlList: List<String>,
                val preferences: DownloadUtil.DownloadPreferences,
        ) : Action

        data class RunCommand(
                val url: String,
                val template: CommandTemplate,
                val preferences: DownloadUtil.DownloadPreferences,
        ) : Action

        data object Cancel : Action
    }

    private val mSelectionStateFlow: MutableStateFlow<SelectionState> =
            MutableStateFlow(SelectionState.Idle)
    private val mSheetStateFlow: MutableStateFlow<SheetState> =
            MutableStateFlow(SheetState.InputUrl)
    private val mSheetValueFlow: MutableStateFlow<SheetValue> = MutableStateFlow(SheetValue.Hidden)

    val selectionStateFlow = mSelectionStateFlow.asStateFlow()
    val sheetStateFlow = mSheetStateFlow.asStateFlow()
    val sheetValueFlow = mSheetValueFlow.asStateFlow()

    private val sheetState
        get() = sheetStateFlow.value

    fun postAction(action: Action) {
        with(action) {
            when (this) {
                is Action.ProceedWithURLs -> proceedWithUrls(this)
                is Action.FetchFormats -> fetchFormat(this)
                is Action.FetchPlaylist -> fetchPlaylist(this)
                is Action.DownloadWithPreset -> downloadWithPreset(urlList, preferences)
                is Action.RunCommand -> runCommand(url, template, preferences)
                Action.HideSheet -> hideDialog()
                is Action.ShowSheet -> showDialog(this)
                Action.Cancel -> cancel()
                Action.Reset -> resetSelectionState()
            }
        }
    }

    private fun proceedWithUrls(action: Action.ProceedWithURLs) {
        mSheetStateFlow.update { SheetState.Configure(action.urlList) }
    }

    private fun fetchPlaylist(action: Action.FetchPlaylist) {
        val (url, preferences) = action

        if (PreferenceUtil.isPlayStoreBuild() && isYouTubeUrl(url)) {
            App.context.makeToast("YouTube downloads are not supported due to Google Policy.")
            hideDialog()
            return
        }

        val job =
                viewModelScope.launch(Dispatchers.IO) {
                    if (App.initializeEngines().await() != true) {
                        withContext(Dispatchers.Main) {
                            mSheetStateFlow.update {
                                SheetState.Error(action = action, throwable = IllegalStateException("yt-dlp Init failed. Check Settings -> Troubleshooting."))
                            }
                        }
                        return@launch
                    }
                    DownloadUtil.getPlaylistOrVideoInfo(
                                    playlistURL = url,
                                    downloadPreferences = preferences,
                            )
                            .onSuccess { info ->
                                withContext(Dispatchers.Main) {
                                    when (info) {
                                        is PlaylistResult -> {
                                            mSelectionStateFlow.update {
                                                SelectionState.PlaylistSelection(result = info)
                                            }
                                        }
                                        is VideoInfo -> {
                                            mSelectionStateFlow.update {
                                                SelectionState.FormatSelection(info = info)
                                            }
                                        }
                                    }
                                    hideDialog()
                                }
                            }
                            .onFailure { th ->
                                mSheetStateFlow.update {
                                    SheetState.Error(action = action, throwable = th)
                                }
                            }
                }
        mSheetStateFlow.update { SheetState.Loading(taskKey = "FetchPlaylist_$url", job = job) }
    }

    private fun fetchFormat(action: Action.FetchFormats) {
        val (url, audioOnly, preferences) = action

        if (PreferenceUtil.isPlayStoreBuild() && isYouTubeUrl(url)) {
            App.context.makeToast("YouTube downloads are not supported due to Google Policy.")
            hideDialog()
            return
        }

        val job =
                viewModelScope.launch(Dispatchers.IO) {
                    if (App.initializeEngines().await() != true) {
                        withContext(Dispatchers.Main) {
                            mSheetStateFlow.update {
                                SheetState.Error(action, throwable = IllegalStateException("yt-dlp Init failed. Check Settings -> Troubleshooting."))
                            }
                        }
                        return@launch
                    }
                    DownloadUtil.fetchVideoInfoFromUrl(
                                    url = url,
                                    preferences = preferences.copy(extractAudio = audioOnly),
                                    taskKey = "FetchFormat_$url",
                            )
                            .onSuccess { info ->
                                withContext(Dispatchers.Main) {
                                    mSelectionStateFlow.update {
                                        SelectionState.FormatSelection(info = info)
                                    }
                                    hideDialog()
                                }
                            }
                            .onFailure { th ->
                                withContext(Dispatchers.Main) {
                                    mSheetStateFlow.update {
                                        SheetState.Error(action, throwable = th)
                                    }
                                }
                            }
                }

        mSheetStateFlow.update { SheetState.Loading(taskKey = "FetchFormat_$url", job = job) }
    }

    private fun downloadWithPreset(
            urlList: List<String>,
            preferences: DownloadUtil.DownloadPreferences,
    ) {
        if (PreferenceUtil.isPlayStoreBuild()) {
            val blockedUrls = urlList.filter { isYouTubeUrl(it) }
            if (blockedUrls.isNotEmpty()) {
                App.context.makeToast("YouTube and YouTube Music downloads are not supported due to Google Policy.")
                // If all URLs are blocked, just hide. Otherwise, we could filter them out, 
                // but for now, let's just block the whole action if it contains any YouTube links for simplicity.
                hideDialog()
                return
            }
        }
        urlList.forEach { downloader.enqueue(Task(url = it, preferences = preferences)) }
        hideDialog()
    }

    private fun runCommand(
            url: String,
            template: CommandTemplate,
            preferences: DownloadUtil.DownloadPreferences,
    ) {
        val task =
                Task(
                        url = url,
                        type = Task.TypeInfo.CustomCommand(template = template),
                        preferences = preferences,
                )
        downloader.enqueue(task)
    }

    private fun hideDialog() {
        mSheetValueFlow.update { SheetValue.Hidden }
        when (sheetState) {
            is SheetState.Loading -> {
                cancel()
            }
            else -> {}
        }
    }

    private fun showDialog(action: Action.ShowSheet) {
        val urlList = action.urlList
        if (!urlList.isNullOrEmpty()) {
            mSheetStateFlow.update { SheetState.Configure(urlList) }
        } else {
            mSheetStateFlow.update { SheetState.InputUrl }
        }
        mSheetValueFlow.update { SheetValue.Expanded }
    }

    private fun cancel(): Boolean {
        return when (val state = sheetState) {
            is SheetState.Loading -> {
                NotificationUtil.cancelNotification(state.taskKey)
                val res = YoutubeDL.destroyProcessById(id = state.taskKey)
                if (res) {
                    state.job.cancel()
                }
                return res
            }
            else -> false
        }
    }

    private fun resetSelectionState() {
        mSelectionStateFlow.update { SelectionState.Idle }
    }

    companion object {
        private fun isYouTubeUrl(url: String): Boolean {
            val lowerUrl = url.lowercase()
            return lowerUrl.contains("youtube.com") || 
                   lowerUrl.contains("youtu.be") || 
                   lowerUrl.contains("music.youtube.com")
        }
    }
}
