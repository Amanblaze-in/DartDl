package com.dartdl.app.ui.page.status

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dartdl.app.R
import com.dartdl.app.ui.component.NativeAdView
import com.dartdl.app.util.AdManager
import com.dartdl.app.util.FileUtil
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import android.provider.DocumentsContract
import com.dartdl.app.util.findActivity
import com.dartdl.app.util.makeToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StatusSaverPage(
    viewModel: StatusViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tabs = listOf("WhatsApp", "WhatsApp Business")
    val pagerState = rememberPagerState { tabs.size }
    
    val whatsappStatuses by viewModel.whatsappStatuses.collectAsState()
    val businessStatuses by viewModel.businessStatuses.collectAsState()
    val isWhatsAppGranted by viewModel.isWhatsAppPermissionGranted.collectAsState()
    val isBusinessGranted by viewModel.isBusinessPermissionGranted.collectAsState()

    var showRewardDialog by remember { mutableStateOf<StatusMedia?>(null) }
    var isAdLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
        // Pre-load ad
        AdManager.loadRewarded(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Status Saver") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val isWhatsApp = pageIndex == 0
                val isGranted = if (isWhatsApp) isWhatsAppGranted else isBusinessGranted
                val statuses = if (isWhatsApp) whatsappStatuses else businessStatuses

                if (!isGranted) {
                    PermissionRequestUI(
                        isWhatsApp = isWhatsApp,
                        onPermissionGranted = { viewModel.checkPermissions(context) }
                    )
                } else {
                    StatusGrid(
                        statuses = statuses,
                        onDownloadClick = { showRewardDialog = it }
                    )
                }
            }
        }
    }

    // Rewarded Ad Dialog
    if (showRewardDialog != null) {
        AlertDialog(
            onDismissRequest = { if (!isAdLoading) showRewardDialog = null },
            title = { Text("Download Status") },
            text = { 
                Column {
                    Text("Watch a short ad to download and save this status to your gallery.")
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
                        val media = showRewardDialog!!
                        val activity = context.findActivity()
                        if (activity != null) {
                            if (AdManager.isRewardedAdReady) {
                                AdManager.showRewarded(activity) { success ->
                                    if (success) {
                                        viewModel.saveStatus(context, media) { showRewardDialog = null }
                                    } else {
                                        context.makeToast("Ad not completed. Please watch the full ad to save.")
                                    }
                                }
                            } else {
                                // Ad not ready, trigger load and wait
                                isAdLoading = true
                                AdManager.loadRewarded(context) { ready ->
                                    scope.launch {
                                        if (ready) {
                                            // Wait a tiny bit for the object to be fully available
                                            delay(500)
                                            isAdLoading = false
                                            AdManager.showRewarded(activity) { success ->
                                                if (success) {
                                                    viewModel.saveStatus(context, media) { showRewardDialog = null }
                                                } else {
                                                    context.makeToast("Ad not completed.")
                                                }
                                            }
                                        } else {
                                            isAdLoading = false
                                            context.makeToast("Failed to load ad. Please check your internet connection and try again.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(if (isAdLoading) "Loading..." else "Watch Ad & Save")
                }
            },
            dismissButton = {
                TextButton(enabled = !isAdLoading, onClick = { showRewardDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusGrid(
    statuses: List<StatusMedia>,
    onDownloadClick: (StatusMedia) -> Unit
) {
    if (statuses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No statuses found. Open WhatsApp first.", color = Color.Gray)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Correct way to handle ads in a LazyGrid is to iterate and call 'item' for ads and 'item' for statuses
            statuses.forEachIndexed { index, status ->
                if (index > 0 && index % 6 == 0) {
                    item(span = { GridItemSpan(3) }) {
                        NativeAdView(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 8.dp))
                    }
                }
                item(key = status.uri.toString()) {
                    StatusItem(status, onDownloadClick)
                }
            }
        }
    }
}

@Composable
fun StatusItem(
    status: StatusMedia,
    onDownloadClick: (StatusMedia) -> Unit
) {
    val context = LocalContext.current
    // Remember the image request to prevent redundant frame extraction on scroll
    val model = remember(status.uri) {
        ImageRequest.Builder(context)
            .data(status.uri)
            .apply {
                if (status.isVideo) {
                    decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                }
            }
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.DarkGray)
            .clickable { 
                context.findActivity()?.let { activity ->
                    AdManager.showInterstitial(activity) {
                        FileUtil.openFile(status.uri.toString()) { 
                            android.widget.Toast.makeText(context, "Cannot open file", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (status.isVideo) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Center)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(topStart = 8.dp))
        ) {
            IconButton(
                onClick = { 
                    context.findActivity()?.let { activity ->
                        AdManager.showInterstitial(activity) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = if (status.isVideo) "video/mp4" else "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, status.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Status"))
                        }
                    }
                }
            ) {
                Icon(Icons.Filled.Share, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            
            IconButton(
                onClick = { onDownloadClick(status) }
            ) {
                Icon(Icons.Filled.Download, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun PermissionRequestUI(
    isWhatsApp: Boolean,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                onPermissionGranted()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permission Required",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "To show WhatsApp ${if (isWhatsApp) "" else "Business "}statuses, you need to grant permission to the .Statuses folder.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                val uriStr = if (isWhatsApp) StatusViewModel.WHATSAPP_URI else StatusViewModel.BUSINESS_URI
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    val rootUri = Uri.parse(uriStr.replace("tree", "document"))
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, rootUri)
                }
                launcher.launch(intent)
            }
        ) {
            Text("Grant Permission")
        }
    }
}
