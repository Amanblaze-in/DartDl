package com.dartdl.app.ui.page.downloadv2.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dartdl.app.R
import com.dartdl.app.download.Task
import com.dartdl.app.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.dartdl.app.util.Format

@Composable
fun VideoCardV2(
    modifier: Modifier = Modifier,
    viewState: Task.ViewState,
    actionButton: @Composable () -> Unit = {},
    stateIndicator: @Composable () -> Unit = {},
    onButtonClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { onButtonClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            Box(modifier = Modifier.height(160.dp).fillMaxWidth()) {
                AsyncImage(
                    model = viewState.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(viewState.duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    stateIndicator()
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = viewState.title.ifBlank { "Untitled Video" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = viewState.uploader,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    actionButton()
                }
            }
        }
    }
}

@Composable
fun VideoListItem(
    modifier: Modifier = Modifier,
    viewState: Task.ViewState,
    stateIndicator: @Composable () -> Unit = {},
    onButtonClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onButtonClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = viewState.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp, 60.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = viewState.title.ifBlank { "Untitled Video" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            stateIndicator()
        }
        IconButton(onClick = onButtonClick) {
            Icon(Icons.Default.MoreVert, contentDescription = null)
        }
    }
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    downloadState: Task.DownloadState,
    onActionPost: (UiAction) -> Unit,
) {
    when (downloadState) {
        is Task.DownloadState.Running -> {
            IconButton(onClick = { onActionPost(UiAction.Cancel) }, modifier = modifier.size(32.dp)) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
            }
        }
        is Task.DownloadState.Error, is Task.DownloadState.Canceled -> {
            IconButton(onClick = { onActionPost(UiAction.Restart) }, modifier = modifier.size(32.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Retry")
            }
        }
        else -> {
             // No action needed for Completed or Idle here as it's handled by card click
        }
    }
}

@Composable
fun CardStateIndicator(
    modifier: Modifier = Modifier,
    downloadState: Task.DownloadState,
) {
    when (downloadState) {
        is Task.DownloadState.Running -> {
            CircularProgressIndicator(
                progress = { if (downloadState.progress > 0) downloadState.progress else 0f },
                modifier = modifier.size(24.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        is Task.DownloadState.Completed -> {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = Color(0xFF4CAF50),
                modifier = modifier.size(24.dp)
            )
        }
        is Task.DownloadState.Error -> {
            Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = modifier.size(24.dp)
            )
        }
        else -> {}
    }
}

@Composable
fun ListItemStateText(
    modifier: Modifier = Modifier,
    downloadState: Task.DownloadState,
) {
    val text = when (downloadState) {
        is Task.DownloadState.Running -> "Downloading... ${if(downloadState.progress > 0) (downloadState.progress * 100).toInt().toString() + "%" else ""}"
        is Task.DownloadState.Completed -> "Completed"
        is Task.DownloadState.Error -> "Error"
        is Task.DownloadState.Canceled -> "Canceled"
        is Task.DownloadState.FetchingInfo -> "Fetching info..."
        else -> ""
    }
    if (text.isNotEmpty()) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (downloadState is Task.DownloadState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

sealed interface UiAction {
    data object Pause : UiAction
    data object Resume : UiAction
    data object Cancel : UiAction
    data object Restart : UiAction
    data object Delete : UiAction
    data class OpenFile(val path: String?) : UiAction
    data class OpenExternalPlayer(val path: String?) : UiAction
    data class ShareFile(val path: String?) : UiAction
    data class CopyErrorReport(val throwable: Throwable?) : UiAction
    data object CopyVideoURL : UiAction
    data class OpenVideoURL(val url: String) : UiAction
    data class OpenThumbnailURL(val url: String) : UiAction
}
