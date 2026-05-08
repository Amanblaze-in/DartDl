package com.dartdl.app.ui.page.downloadv2

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.*
import com.dartdl.app.R
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dartdl.app.download.DownloaderV2
import com.dartdl.app.download.Task
import com.dartdl.app.download.Task.State
import com.dartdl.app.ui.common.HapticFeedback.slightHapticFeedback
import com.dartdl.app.ui.common.LocalFixedColorRoles
import com.dartdl.app.ui.component.*
import com.dartdl.app.ui.page.downloadv2.component.*
import com.dartdl.app.ui.page.downloadv2.component.UiAction
import com.dartdl.app.ui.page.downloadv2.configure.*
import com.dartdl.app.ui.page.downloadv2.configure.DownloadDialogViewModel.Action
import com.dartdl.app.ui.page.downloadv2.configure.DownloadDialogViewModel.UiEvent
import com.dartdl.app.util.DownloadUtil
import com.dartdl.app.util.FileUtil
import com.dartdl.app.util.makeToast
import com.dartdl.app.ui.svg.DynamicColorImageVectors
import com.dartdl.app.ui.svg.drawablevectors.download
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadPageV2(
    dialogViewModel: DownloadDialogViewModel,
    onMenuOpen: () -> Unit = {},
    onNavigateToPlayer: (String, String) -> Unit = { _, _ -> },
    onShare: (String) -> Unit = {},
) {
    val downloader: DownloaderV2 = koinInject()
    val taskDownloadStateMap = downloader.getTaskStateMap()
    val urlText by dialogViewModel.urlTextFlow.collectAsStateWithLifecycle()
    val sheetValue by dialogViewModel.sheetValueFlow.collectAsStateWithLifecycle()
    val sheetStateFlow by dialogViewModel.sheetStateFlow.collectAsStateWithLifecycle()
    val selectionState by dialogViewModel.selectionStateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var preferences by remember {
        mutableStateOf(DownloadUtil.DownloadPreferences.createFromPreferences())
    }

    // Observe UiEvents for side-effects
    LaunchedEffect(Unit) {
        dialogViewModel.uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.NavigateToPlayer -> {
                    onNavigateToPlayer(event.path, event.title)
                }
                is UiEvent.ShareFile -> {
                    onShare(event.path)
                }
                is UiEvent.OpenExternalPlayer -> {
                    try {
                        val intent = FileUtil.createIntentForOpeningFile(event.path)
                        if (intent != null) {
                            val chooserIntent = Intent.createChooser(intent, "Open with")
                            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            intent.clipData?.let { chooserIntent.clipData = it }
                            context.startActivity(chooserIntent)
                        } else {
                            context.makeToast("File not available")
                        }
                    } catch (e: Exception) {
                        context.makeToast("No external player found")
                    }
                }
                is UiEvent.OpenBrowser -> {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.makeToast("Cannot open URL")
                    }
                }
                is UiEvent.ShowToast -> {
                    context.makeToast(event.message)
                }
            }
        }
    }

    DownloadPageImplV2(
        downloader = downloader,
        taskDownloadStateMap = taskDownloadStateMap,
        urlText = urlText,
        onUrlTextChange = { dialogViewModel.postAction(Action.UpdateUrlText(it)) },
        onProceed = { dialogViewModel.postAction(Action.Proceed) },
        onMenuOpen = onMenuOpen,
        onActionPost = { task, action -> dialogViewModel.postAction(Action.PostUiAction(task, action)) },
    )

    if (sheetValue == DownloadDialogViewModel.SheetValue.Expanded) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        DownloadDialog(
            config = Config(),
            sheetState = sheetState,
            preferences = preferences,
            onPreferencesUpdate = { preferences = it },
            state = sheetStateFlow,
            onActionPost = { dialogViewModel.postAction(it) }
        )
    }

    when (val selection = selectionState) {
        is DownloadDialogViewModel.SelectionState.FormatSelection -> {
            FormatPage(
                state = selection,
                onDismissRequest = { dialogViewModel.postAction(Action.Reset) }
            )
        }
        is DownloadDialogViewModel.SelectionState.PlaylistSelection -> {
            PlaylistSelectionPage(
                state = selection,
                onDismissRequest = { dialogViewModel.postAction(Action.Reset) }
            )
        }
        DownloadDialogViewModel.SelectionState.Idle -> {}
    }
}

@Composable
private operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        top = calculateTopPadding() + other.calculateTopPadding(),
        bottom = calculateBottomPadding() + other.calculateBottomPadding(),
        start = calculateStartPadding(layoutDirection) + other.calculateStartPadding(layoutDirection),
        end = calculateEndPadding(layoutDirection) + other.calculateEndPadding(layoutDirection),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadPageImplV2(
    modifier: Modifier = Modifier,
    downloader: DownloaderV2,
    taskDownloadStateMap: SnapshotStateMap<Task, State>,
    urlText: String = "",
    onUrlTextChange: (String) -> Unit = {},
    onProceed: () -> Unit = {},
    onMenuOpen: (() -> Unit) = {},
    onActionPost: (Task, UiAction) -> Unit,
) {
    var activeFilter by remember { mutableStateOf(Filter.All) }
    val filteredMap by remember(activeFilter, taskDownloadStateMap.size) {
        derivedStateOf { 
            taskDownloadStateMap.filter { entry ->
                activeFilter.predict(entry.key to entry.value)
            } 
        }
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val view = LocalView.current

    fun showActionSheet(task: Task) {
        view.slightHapticFeedback()
        scope.launch {
            selectedTask = task
            delay(50)
            sheetState.show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        floatingActionButton = {
            FABs(
                modifier = Modifier,
                onPaste = {
                    val clipboardManager = (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                    clipboardManager.primaryClip?.getItemAt(0)?.text?.let { clipText ->
                        onUrlTextChange(clipText.toString())
                        context.makeToast("URL pasted")
                    }
                },
                onProceed = onProceed
            )
        },
    ) { windowInsetsPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            val lazyListState = rememberLazyGridState()
        var isGridView by rememberSaveable { mutableStateOf(true) }

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            columns = GridCells.Adaptive(240.dp),
            contentPadding = windowInsetsPadding + PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Header(
                    onMenuOpen = onMenuOpen,
                    onHelpClick = { showHelpDialog = true },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SmartInputSection(
                    modifier = Modifier.padding(bottom = 16.dp),
                    urlText = urlText,
                    onUrlTextChange = onUrlTextChange,
                    onProceed = onProceed
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SelectionGroupRow(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 16.dp)
                ) {
                    Filter.entries.forEach { filter ->
                        SelectionGroupItem(
                            colors = SelectionGroupDefaults.colors(
                                activeContainerColor = LocalFixedColorRoles.current.tertiaryFixed,
                                activeContentColor = LocalFixedColorRoles.current.onTertiaryFixed,
                            ),
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter },
                        ) { Text(filter.label()) }
                    }
                }
            }

            if (filteredMap.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DownloadQueuePlaceholder()
                    }
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val videoCount = filteredMap.count { !it.value.viewState.videoFormats.isNullOrEmpty() }
                    SubHeader(
                        modifier = Modifier.padding(bottom = 8.dp),
                        videoCount = videoCount,
                        audioCount = filteredMap.size - videoCount,
                        isGridView = isGridView,
                        onToggleView = { isGridView = !isGridView },
                        onClearAll = {
                            showClearAllConfirmDialog = true
                        },
                        onClearCompleted = {
                            taskDownloadStateMap.entries.filter { it.value.downloadState is Task.DownloadState.Completed }
                                .forEach { downloader.remove(it.key) }
                        },
                        onRestartFailed = {
                            taskDownloadStateMap.entries.filter { it.value.downloadState is Task.DownloadState.Error }
                                .forEach { downloader.restart(it.key) }
                        }
                    )
                }

                val sortedList = filteredMap.toList().sortedByDescending { it.first.timeCreated }
                
                if (isGridView) {
                    sortedList.forEachIndexed { index, (task, state) ->
                        if (index > 0 && index % 2 == 0) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                com.dartdl.app.ui.component.NativeAdView(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                                )
                            }
                        }
                        item(key = task.id) {
                            VideoCardV2(
                                modifier = Modifier.padding(bottom = 20.dp),
                                viewState = state.viewState,
                                actionButton = {
                                    ActionButton(
                                        modifier = Modifier,
                                        downloadState = state.downloadState,
                                    ) { onActionPost(task, it) }
                                },
                                stateIndicator = {
                                    CardStateIndicator(
                                        modifier = Modifier,
                                        downloadState = state.downloadState,
                                    )
                                },
                                onButtonClick = { showActionSheet(task) },
                            )
                        }
                    }
                } else {
                    sortedList.forEachIndexed { index, (task, state) ->
                        if (index > 0 && index % 2 == 0) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                com.dartdl.app.ui.component.NativeAdView(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                )
                            }
                        }
                        item(key = task.id, span = { GridItemSpan(maxLineSpan) }) {
                            VideoListItem(
                                modifier = Modifier.padding(bottom = 16.dp),
                                viewState = state.viewState,
                                stateIndicator = {
                                    ListItemStateText(
                                        modifier = Modifier.padding(top = 3.dp),
                                        downloadState = state.downloadState,
                                    )
                                },
                                onButtonClick = { showActionSheet(task) },
                            )
                        }
                    }
                }
            }
            
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(140.dp))
            }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            AdmobBanner(isCollapsible = true)
        }
        }

        if (selectedTask != null) {
            val task = selectedTask!!
            val state = taskDownloadStateMap[task] ?: return@Scaffold
            DartDLModalBottomSheet(
                sheetState = sheetState,
                contentPadding = PaddingValues(),
                onDismissRequest = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedTask = null }
                },
            ) {
                SheetContent(
                    task = task,
                    downloadState = state.downloadState,
                    viewState = state.viewState,
                    onDismissRequest = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            selectedTask = null
                        }
                    },
                    onActionPost = onActionPost,
                )
            }
        }
    }

    if (showClearAllConfirmDialog) {
        DartDLDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, null) },
            title = { Text(stringResource(com.dartdl.app.R.string.clear_all)) },
            text = { Text(stringResource(com.dartdl.app.R.string.clear_all_confirmation)) },
            confirmButton = {
                ConfirmButton {
                    taskDownloadStateMap.keys.toList().forEach { task ->
                        downloader.cancel(task)
                        downloader.remove(task)
                    }
                    showClearAllConfirmDialog = false
                }
            },
            dismissButton = { DismissButton { showClearAllConfirmDialog = false } }
        )
    }

    if (showHelpDialog) {
        DartDLDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(com.dartdl.app.R.string.how_to_use)) },
            text = { Text(stringResource(com.dartdl.app.R.string.user_guide_content)) },
            confirmButton = {
                ConfirmButton(text = stringResource(com.dartdl.app.R.string.got_it)) {
                    showHelpDialog = false
                }
            }
        )
    }
}

@Composable
fun Header(
    modifier: Modifier = Modifier,
    onMenuOpen: () -> Unit = {},
    onHelpClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.height(64.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuOpen) {
            Icon(Icons.Default.Menu, contentDescription = null)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            stringResource(com.dartdl.app.R.string.video_downloader),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onHelpClick) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(com.dartdl.app.R.string.how_to_use),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SmartInputSection(
    modifier: Modifier = Modifier,
    urlText: String,
    onUrlTextChange: (String) -> Unit,
    onProceed: () -> Unit
) {
    OutlinedTextField(
        value = urlText,
        onValueChange = onUrlTextChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(com.dartdl.app.R.string.video_audio_link), style = MaterialTheme.typography.bodyLarge) },
        maxLines = 1,
        shape = MaterialTheme.shapes.medium,
        leadingIcon = {
            Icon(Icons.Rounded.Download, contentDescription = null)
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        ),
        trailingIcon = {
            if (urlText.isNotEmpty()) {
                ClearButton { onUrlTextChange("") }
            }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Go
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onGo = { onProceed() }
        )
    )
}

@Composable
fun FABs(
    modifier: Modifier = Modifier,
    onPaste: () -> Unit = {},
    onProceed: () -> Unit = {}
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SmallFloatingActionButton(
            onClick = onPaste,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Outlined.ContentPaste, contentDescription = stringResource(com.dartdl.app.R.string.paste))
        }
        FloatingActionButton(
            onClick = onProceed,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Outlined.FileDownload, contentDescription = stringResource(com.dartdl.app.R.string.download))
        }
    }
}

@Composable
private fun DownloadQueuePlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberVectorPainter(image = DynamicColorImageVectors.download()),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(com.dartdl.app.R.string.video_downloader),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Paste a link to start downloading",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SubHeader(
    modifier: Modifier = Modifier,
    videoCount: Int,
    audioCount: Int,
    isGridView: Boolean,
    onToggleView: () -> Unit,
    onClearAll: () -> Unit,
    onClearCompleted: () -> Unit,
    onRestartFailed: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val videoText = pluralStringResource(com.dartdl.app.R.plurals.video_count, videoCount, videoCount)
        val audioText = pluralStringResource(com.dartdl.app.R.plurals.audio_count, audioCount, audioCount)
        Text(
            text = stringResource(com.dartdl.app.R.string.downloads_count_format, videoText, audioText),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            IconButton(onClick = onToggleView) {
                Icon(
                    if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Filled.GridView,
                    contentDescription = "Toggle View"
                )
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, null) },
                        text = { Text(stringResource(com.dartdl.app.R.string.clear_all)) },
                        onClick = { 
                            onClearAll()
                            expanded = false 
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.DoneAll, null) },
                        text = { Text(stringResource(com.dartdl.app.R.string.clear_completed)) },
                        onClick = { 
                            onClearCompleted()
                            expanded = false 
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                        text = { Text(stringResource(com.dartdl.app.R.string.restart_failed)) },
                        onClick = { 
                            onRestartFailed()
                            expanded = false 
                        }
                    )
                }
            }
        }
    }
}

enum class Filter {
    All,
    Downloading,
    Completed,
    Error;

    @Composable
    fun label() = when (this) {
        All -> stringResource(com.dartdl.app.R.string.all)
        Downloading -> stringResource(com.dartdl.app.R.string.status_downloading)
        Completed -> stringResource(com.dartdl.app.R.string.status_completed)
        Error -> stringResource(com.dartdl.app.R.string.status_error)
    }

    fun predict(pair: Pair<Task, State>): Boolean {
        val (_, state) = pair
        return when (this) {
            All -> true
            Downloading -> state.downloadState is Task.DownloadState.Running || state.downloadState is Task.DownloadState.FetchingInfo
            Completed -> state.downloadState is Task.DownloadState.Completed
            Error -> state.downloadState is Task.DownloadState.Error
        }
    }
}
