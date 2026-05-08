package com.dartdl.app.ui.page.downloadv2.configure

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.RangeSliderState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi as LayoutApiExperimental

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dartdl.app.R
import com.dartdl.app.download.DownloaderV2
import com.dartdl.app.download.TaskFactory
import com.dartdl.app.ui.component.*
import com.dartdl.app.ui.page.download.VideoClipDialog
import com.dartdl.app.ui.page.download.VideoSelectionSlider
import com.dartdl.app.ui.page.settings.general.DialogCheckBoxItem
import com.dartdl.app.ui.theme.DartDLTheme
import com.dartdl.app.ui.theme.generateLabelColor
import com.dartdl.app.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import org.koin.compose.koinInject
import kotlin.math.min
import kotlin.math.roundToInt

private const val NOT_SELECTED = -1

data class FormatConfig(
    val formatList: List<Format>,
    val videoClips: List<VideoClip>,
    val splitByChapter: Boolean,
    val newTitle: String,
    val selectedSubtitles: List<String>,
    val selectedAutoCaptions: List<String>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatPage(
    state: DownloadDialogViewModel.SelectionState.FormatSelection,
    onDismissRequest: () -> Unit,
) {
    val downloader: DownloaderV2 = koinInject()
    val videoInfo = state.info

    FormatPageImpl(
        videoInfo = videoInfo,
        onNavigateBack = onDismissRequest,
        onDownloadPressed = { config ->
            val taskWithState = TaskFactory.createWithConfigurations(
                videoInfo = videoInfo,
                formatList = config.formatList,
                videoClips = config.videoClips,
                splitByChapter = config.splitByChapter,
                newTitle = config.newTitle,
                selectedSubtitles = config.selectedSubtitles,
                selectedAutoCaptions = config.selectedAutoCaptions
            )
            // BUG FIX: Pass taskWithState to reuse fetched VideoInfo and avoid Instagram rate limit
            downloader.enqueue(taskWithState)
            onDismissRequest()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun FormatPageImpl(
    videoInfo: VideoInfo,
    onNavigateBack: () -> Unit,
    onDownloadPressed: (FormatConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val hapticFeedback = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val audioOnly = videoInfo.requestedFormats?.all { it.vcodec == "none" } ?: false
    val isSuggestedFormatAvailable = videoInfo.requestedFormats != null || videoInfo.requestedDownloads != null

    var isSuggestedFormatSelected by remember { mutableStateOf(isSuggestedFormatAvailable) }
    val selectedAudioOnlyFormats = remember { mutableStateListOf<Int>() }
    var selectedVideoAudioFormat by remember { mutableIntStateOf(NOT_SELECTED) }
    var selectedVideoOnlyFormat by remember { mutableIntStateOf(NOT_SELECTED) }

    val audioOnlyFormats = remember(videoInfo) { videoInfo.formats?.filter { it.vcodec == "none" }?.sortedByDescending { it.abr } ?: emptyList() }
    val videoAudioFormats = remember(videoInfo) { videoInfo.formats?.filter { it.vcodec != "none" && it.acodec != "none" }?.sortedByDescending { it.height } ?: emptyList() }
    val videoOnlyFormats = remember(videoInfo) { videoInfo.formats?.filter { it.vcodec != "none" && it.acodec == "none" }?.sortedByDescending { it.height } ?: emptyList() }

    var audioOnlyItemLimit by remember { mutableIntStateOf(6) }
    var videoOnlyItemLimit by remember { mutableIntStateOf(6) }
    var videoAudioItemLimit by remember { mutableIntStateOf(6) }

    val mergeAudioStream = true // Always merge if possible
    var isClippingVideo by remember { mutableStateOf(false) }
    var isSplittingVideo by remember { mutableStateOf(false) }

    val isClippingAvailable = videoInfo.duration != null
    val isSplitByChapterAvailable = videoInfo.chapters?.isNotEmpty() == true

    val selectedSubtitleCodes = videoInfo.subtitles.keys.take(1) // Default to first subtitle if any

    val videoDurationRange = 0f..(videoInfo.duration?.toFloat() ?: 0f)
    var showVideoClipDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSubtitleSelectionDialog by remember { mutableStateOf(false) }
    var showOpusInfoDialog by remember { mutableStateOf(false) }

    var videoClipDuration by remember { mutableStateOf(videoDurationRange) }
    var videoTitle by remember { mutableStateOf("") }

    var showRewardAdDialog by remember { mutableStateOf(false) }
    var isAdLoading by remember { mutableStateOf(false) }
    var pendingFormatConfig by remember { mutableStateOf<FormatConfig?>(null) }

    val suggestedSubtitleMap: Map<String, List<SubtitleFormat>> =
        videoInfo.subtitles.takeIf { it.isNotEmpty() }
            ?: videoInfo.automaticCaptions.filterKeys { it.endsWith("-orig") }

    val otherSubtitleMap: Map<String, List<SubtitleFormat>> =
        videoInfo.subtitles + videoInfo.automaticCaptions - suggestedSubtitleMap.keys

    LaunchedEffect(isClippingVideo) {
        delay(200)
        videoClipDuration = videoDurationRange
    }
    
    LaunchedEffect(Unit) {
        // Pre-load ad
        AdManager.loadRewarded(context)
    }

    val lazyGridState = rememberLazyGridState()
    val isFabExpanded by remember { derivedStateOf { lazyGridState.firstVisibleItemIndex > 0 } }

    val selectedSubtitles = remember {
        mutableStateListOf<String>().apply { addAll(selectedSubtitleCodes) }
    }
    val selectedAutoCaptions = remember { mutableStateListOf<String>() }

    val formatList: List<Format> by remember {
        derivedStateOf {
            mutableListOf<Format>().apply {
                if (isSuggestedFormatSelected) {
                    videoInfo.requestedFormats?.let { addAll(it) }
                        ?: videoInfo.requestedDownloads?.forEach {
                            it.requestedFormats?.let { addAll(it) }
                        }
                } else {
                    selectedAudioOnlyFormats.forEach { index ->
                        add(audioOnlyFormats.elementAt(index))
                    }
                    videoAudioFormats.getOrNull(selectedVideoAudioFormat)?.let { add(it) }
                    videoOnlyFormats.getOrNull(selectedVideoOnlyFormat)?.let { add(it) }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(com.dartdl.app.R.string.format_selection),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.Default.Close, stringResource(com.dartdl.app.R.string.close))
                    }
                },
            )
        },
        floatingActionButton = {
            val isFormatSelected = isSuggestedFormatSelected || formatList.isNotEmpty()
            if (isFormatSelected) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val config = FormatConfig(
                            formatList = formatList,
                            videoClips = if (isClippingVideo) listOf(VideoClip(videoClipDuration)) else emptyList(),
                            splitByChapter = isSplittingVideo,
                            newTitle = videoTitle,
                            selectedSubtitles = selectedSubtitles,
                            selectedAutoCaptions = selectedAutoCaptions,
                        )
                        pendingFormatConfig = config
                        showRewardAdDialog = true
                    },
                    modifier = Modifier.padding(12.dp),
                    icon = { Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(24.dp)) },
                    text = { Text(stringResource(com.dartdl.app.R.string.start_download)) },
                    expanded = isFabExpanded,
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { paddingValues ->
        LazyVerticalGrid(
            modifier = Modifier.padding(paddingValues),
            state = lazyGridState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FormatVideoPreview(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = videoTitle.ifEmpty { videoInfo.title },
                    author = videoInfo.uploader ?: videoInfo.channel ?: videoInfo.uploaderId.toString(),
                    thumbnailUrl = videoInfo.thumbnail.toHttpsUrl(),
                    duration = videoInfo.duration?.roundToInt() ?: 0,
                    isClippingVideo = isClippingVideo,
                    isSplittingVideo = isSplittingVideo,
                    isClippingAvailable = isClippingAvailable,
                    isSplitByChapterAvailable = isSplitByChapterAvailable,
                    onClippingToggled = { isClippingVideo = !isClippingVideo },
                    onSplittingToggled = { isSplittingVideo = !isSplittingVideo },
                    onRename = { showRenameDialog = true },
                    onOpenThumbnail = { uriHandler.openUri(videoInfo.thumbnail.toHttpsUrl()) },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    AnimatedVisibility(visible = isClippingVideo) {
                        Column {
                            val state = remember(isClippingVideo, showVideoClipDialog) {
                                RangeSliderState(
                                    activeRangeStart = videoClipDuration.start,
                                    activeRangeEnd = videoClipDuration.endInclusive,
                                    valueRange = videoDurationRange,
                                    onValueChangeFinished = { videoClipDuration = videoClipDuration },
                                )
                            }
                            VideoSelectionSlider(
                                modifier = Modifier.fillMaxWidth(),
                                state = state,
                                onDiscard = { isClippingVideo = false },
                                onDurationClick = { showVideoClipDialog = true },
                            )
                            HorizontalDivider()
                        }
                    }
                    AnimatedVisibility(visible = isSplittingVideo) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(com.dartdl.app.R.string.split_video_msg, videoInfo.chapters?.size ?: 0),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButtonWithIcon(
                                onClick = { isSplittingVideo = false },
                                icon = Icons.Default.Delete,
                                text = stringResource(com.dartdl.app.R.string.discard),
                                contentColor = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            if (suggestedSubtitleMap.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = stringResource(com.dartdl.app.R.string.subtitle_language),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            ClickableTextAction(visible = true, text = "See all") { showSubtitleSelectionDialog = true }
                        }
                        LazyRow {
                            for ((code, formats) in suggestedSubtitleMap) {
                                item {
                                    VideoFilterChip(
                                        selected = selectedSubtitles.contains(code),
                                        onClick = {
                                            if (selectedSubtitles.contains(code)) selectedSubtitles.remove(code)
                                            else selectedSubtitles.add(code)
                                        },
                                        label = formats.first().run { name ?: protocol ?: code },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isSuggestedFormatAvailable) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FormatSubtitle(text = stringResource(com.dartdl.app.R.string.suggested), modifier = Modifier.padding(horizontal = 12.dp))
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SuggestedFormatItem(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        videoInfo = videoInfo,
                        selected = isSuggestedFormatSelected,
                        onClick = {
                            isSuggestedFormatSelected = true
                            selectedAudioOnlyFormats.clear()
                            selectedVideoAudioFormat = NOT_SELECTED
                            selectedVideoOnlyFormat = NOT_SELECTED
                        },
                    )
                }
            }

            if (audioOnlyFormats.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp, start = 12.dp, end = 12.dp)) {
                        FormatSubtitle(text = stringResource(com.dartdl.app.R.string.audio), color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showOpusInfoDialog = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                itemsIndexed(audioOnlyFormats.subList(0, min(audioOnlyItemLimit, audioOnlyFormats.size))) { index, formatInfo ->
                    FormatItem(
                        formatInfo = formatInfo,
                        duration = videoInfo.duration ?: 0.0,
                        selected = selectedAudioOnlyFormats.contains(index),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        outlineColor = MaterialTheme.colorScheme.secondary,
                        onLongClick = { 
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, formatInfo.url ?: "")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Link"))
                        },
                        onClick = {
                            if (selectedAudioOnlyFormats.contains(index)) selectedAudioOnlyFormats.remove(index)
                            else {
                                if (!mergeAudioStream) selectedAudioOnlyFormats.clear()
                                isSuggestedFormatSelected = false
                                selectedAudioOnlyFormats.add(index)
                            }
                        }
                    )
                }
            }

            if (!audioOnly) {
                if (videoOnlyFormats.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        FormatSubtitle(text = stringResource(com.dartdl.app.R.string.video_only), color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                    itemsIndexed(videoOnlyFormats.subList(0, min(videoOnlyItemLimit, videoOnlyFormats.size))) { index, formatInfo ->
                        FormatItem(
                            formatInfo = formatInfo,
                            duration = videoInfo.duration ?: 0.0,
                            selected = selectedVideoOnlyFormat == index,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            outlineColor = MaterialTheme.colorScheme.tertiary,
                            onLongClick = { 
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, formatInfo.url ?: "")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Link"))
                        },
                            onClick = {
                                selectedVideoOnlyFormat = if (selectedVideoOnlyFormat == index) NOT_SELECTED else {
                                    selectedVideoAudioFormat = NOT_SELECTED
                                    isSuggestedFormatSelected = false
                                    index
                                }
                            }
                        )
                    }
                }
                if (videoAudioFormats.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        FormatSubtitle(text = stringResource(com.dartdl.app.R.string.video), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                    itemsIndexed(videoAudioFormats.subList(0, min(videoAudioItemLimit, videoAudioFormats.size))) { index, formatInfo ->
                        FormatItem(
                            formatInfo = formatInfo,
                            duration = videoInfo.duration ?: 0.0,
                            selected = selectedVideoAudioFormat == index,
                            onLongClick = { 
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, formatInfo.url ?: "")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Link"))
                        },
                            onClick = {
                                selectedVideoAudioFormat = if (selectedVideoAudioFormat == index) NOT_SELECTED else {
                                    selectedAudioOnlyFormats.clear()
                                    selectedVideoOnlyFormat = NOT_SELECTED
                                    isSuggestedFormatSelected = false
                                    index
                                }
                            }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(64.dp)) }
        }
    }

    if (showVideoClipDialog)
        VideoClipDialog(
            onDismissRequest = { showVideoClipDialog = false },
            initialValue = videoClipDuration,
            valueRange = videoDurationRange,
            onConfirm = { videoClipDuration = it },
        )

    if (showRenameDialog)
        RenameDialog(
            initialValue = videoTitle.ifEmpty { videoInfo.title },
            onDismissRequest = { showRenameDialog = false },
            onConfirm = { videoTitle = it }
        )

    if (showSubtitleSelectionDialog)
        SubtitleSelectionDialog(
            suggestedSubtitles = suggestedSubtitleMap,
            autoCaptions = otherSubtitleMap,
            selectedSubtitles = selectedSubtitles,
            onDismissRequest = { showSubtitleSelectionDialog = false },
            onConfirm = { subs, _ ->
                selectedSubtitles.clear()
                selectedSubtitles.addAll(subs)
                showSubtitleSelectionDialog = false
            },
        )

    if (showRewardAdDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAdLoading) showRewardAdDialog = false },
            title = { Text(stringResource(com.dartdl.app.R.string.download)) },
            text = { 
                Column {
                    Text("Watch a short ad to start your download.")
                    if (isAdLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Loading Ad...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isAdLoading,
                    onClick = {
                        val activity = context as? Activity
                        val config = pendingFormatConfig
                        if (activity != null && config != null) {
                            if (AdManager.isRewardedAdReady) {
                                AdManager.showRewarded(activity) { success ->
                                    if (success) {
                                        showRewardAdDialog = false
                                        onDownloadPressed(config)
                                    } else {
                                        context.makeToast("Ad not completed. Please watch the full ad to download.")
                                    }
                                }
                            } else {
                                // Ad not ready, wait and load
                                isAdLoading = true
                                AdManager.loadRewarded(context) { ready ->
                                    scope.launch {
                                        if (ready) {
                                            delay(500)
                                            isAdLoading = false
                                            AdManager.showRewarded(activity) { success ->
                                                if (success) {
                                                    showRewardAdDialog = false
                                                    onDownloadPressed(config)
                                                } else {
                                                    context.makeToast("Ad not completed.")
                                                }
                                            }
                                        } else {
                                            isAdLoading = false
                                            // Fallback to interstitial ONLY if rewarded fails but we still want to monetize
                                            // Actually, the user wants strict ads, so let's show an error message.
                                            context.makeToast("Failed to load ad. Please try again.")
                                        }
                                    }
                                }
                            }
                        } else if (config != null) {
                            onDownloadPressed(config)
                        }
                    }
                ) {
                    Text(if (isAdLoading) "Loading..." else "Watch Ad & Download")
                }
            },
            dismissButton = {
                TextButton(enabled = !isAdLoading, onClick = { showRewardAdDialog = false }) {
                    Text(stringResource(com.dartdl.app.R.string.cancel))
                }
            }
        )
    }
    if (showOpusInfoDialog) {
        DartDLDialog(
            onDismissRequest = { showOpusInfoDialog = false },
            icon = { Icon(Icons.Default.Info, null) },
            title = { Text(stringResource(com.dartdl.app.R.string.opus_vs_mp3_title)) },
            text = { Text(stringResource(com.dartdl.app.R.string.opus_vs_mp3_content)) },
            confirmButton = { ConfirmButton { showOpusInfoDialog = false } }
        )
    }
}

@Composable
private fun RenameDialog(
    initialValue: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var filename by remember { mutableStateOf(initialValue) }
    DartDLDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { ConfirmButton { onConfirm(filename); onDismissRequest() } },
        dismissButton = { DismissButton { onDismissRequest() } },
        title = { Text(stringResource(com.dartdl.app.R.string.rename)) },
        icon = { Icon(Icons.Default.Edit, null) },
        text = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = filename,
                onValueChange = { filename = it },
                label = { Text(stringResource(com.dartdl.app.R.string.title)) },
            )
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubtitleSelectionDialog(
    suggestedSubtitles: Map<String, List<SubtitleFormat>>,
    autoCaptions: Map<String, List<SubtitleFormat>>,
    selectedSubtitles: List<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (subs: List<String>, autoSubs: List<String>) -> Unit,
) {
    val selectedList = remember { mutableStateListOf<String>().apply { addAll(selectedSubtitles) } }
    DartDLDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { ConfirmButton { onConfirm(selectedList, emptyList()) } },
        dismissButton = { DismissButton { onDismissRequest() } },
        title = { Text(stringResource(com.dartdl.app.R.string.subtitle_language)) },
        icon = { Icon(Icons.Default.Subtitles, null) },
        text = {
            LazyColumn {
                item { Text(stringResource(com.dartdl.app.R.string.suggested), style = MaterialTheme.typography.titleSmall) }
                val keys = suggestedSubtitles.keys.toList()
                items(count = keys.size) { index ->
                    val code = keys[index]
                    DialogCheckBoxItem(
                        checked = selectedList.contains(code),
                        onValueChange = { if (selectedList.contains(code)) selectedList.remove(code) else selectedList.add(code) },
                        text = suggestedSubtitles[code]?.firstOrNull()?.name ?: code
                    )
                }
            }
        },
    )
}

@Composable
private fun ClickableTextAction(visible: Boolean, text: String, onClick: () -> Unit) {
    AnimatedVisibility(visible = visible, exit = fadeOut()) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
