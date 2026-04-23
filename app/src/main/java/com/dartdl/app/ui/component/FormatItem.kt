package com.dartdl.app.ui.component

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.VerticalSplit
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dartdl.app.R
import com.dartdl.app.ui.theme.DartDLTheme
import com.dartdl.app.util.Format
import com.dartdl.app.util.VideoInfo
import com.dartdl.app.util.connectWithBlank
import com.dartdl.app.util.connectWithDelimiter
import com.dartdl.app.util.toBitrateText
import com.dartdl.app.util.toDurationText
import com.dartdl.app.util.toFileSizeText

/**
 * Issue 4 Fix: Converts raw yt-dlp format data into a human-readable label.
 * Examples:
 *   - 1920x1080, vp9+opus  -> "1080p (VP9)"
 *   - 1280x720, avc1+none  -> "720p (AVC)"
 *   - audio only, m4a      -> "Audio Only (M4A)"
 *   - audio only, opus     -> "Audio Only (OPUS)"
 */
fun Format.toFriendlyLabel(): String {
    val heightPx = height?.toInt()
    val isAudio = vcodec == null || vcodec == "none"
    val isVideo = !isAudio

    return when {
        isVideo && heightPx != null -> {
            // Map height to standard resolution label
            val resLabel = when {
                heightPx >= 2160 -> "4K"
                heightPx >= 1440 -> "1440p"
                heightPx >= 1080 -> "1080p"
                heightPx >= 720  -> "720p"
                heightPx >= 480  -> "480p"
                heightPx >= 360  -> "360p"
                heightPx >= 240  -> "240p"
                else             -> "${heightPx}p"
            }
            // Append the codec cleanly (strip dot-version like 'avc1.42001f' -> 'AVC')
            val codec = vcodec?.substringBefore(".")?.uppercase()?.take(4) ?: ""
            if (codec.isNotBlank() && codec != "NONE") "$resLabel ($codec)" else resLabel
        }
        isAudio -> {
            // Show ext (m4a, opus, mp3) or acodec
            val audioLabel = ext?.uppercase()
                ?: acodec?.substringBefore(".")?.uppercase()
                ?: audioExt?.uppercase()
                ?: "Audio"
            "Audio Only ($audioLabel)"
        }
        else -> {
            // Fallback: use format_note if available, otherwise resolution string
            formatNote?.takeIf { it.isNotBlank() }
                ?: resolution?.takeIf { it.isNotBlank() }
                ?: format?.substringBefore("-")?.trim()
                ?: "Unknown"
        }
    }
}

@Composable
fun FormatVideoPreview(
    modifier: Modifier = Modifier,
    title: String,
    author: String,
    thumbnailUrl: String,
    duration: Int,
    isSplittingVideo: Boolean,
    isClippingVideo: Boolean,
    isClippingAvailable: Boolean = false,
    isSplitByChapterAvailable: Boolean = false,
    onRename: () -> Unit = {},
    onOpenThumbnail: () -> Unit = {},
    onClippingToggled: () -> Unit = {},
    onSplittingToggled: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = false)) {
        Row(modifier = modifier.fillMaxWidth()) {
            Box(modifier = Modifier) {
                MediaImage(
                    modifier = Modifier,
                    imageModel = thumbnailUrl,
                    isAudio = false,
                    contentDescription = stringResource(id = R.string.thumbnail),
                )
                Surface(
                    modifier = Modifier.padding(2.dp).align(Alignment.BottomEnd),
                    color = Color.Black.copy(alpha = 0.68f),
                    shape = MaterialTheme.shapes.extraSmall,
                ) {
                    val durationText = duration.toDurationText()
                    Text(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = durationText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Top) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (author != "playlist" && author != "null")
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp).padding(top = 3.dp),
                        text = author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
            }
        }
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    stringResource(id = R.string.show_more_actions),
                    modifier = Modifier.size(18.dp),
                )
            }

            DropdownMenu(
                modifier = Modifier.align(Alignment.BottomEnd),
                expanded = expanded,
                onDismissRequest = { expanded = false },
                scrollState = rememberScrollState(),
            ) {
                DropdownMenuItem(
                    leadingIcon = { Icon(imageVector = Icons.Outlined.Edit, null) },
                    text = { Text(text = stringResource(id = R.string.rename)) },
                    onClick = {
                        onRename()
                        expanded = false
                    },
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(imageVector = Icons.Outlined.Image, null) },
                    text = { Text(text = stringResource(id = R.string.thumbnail)) },
                    onClick = {
                        onOpenThumbnail()
                        expanded = false
                    },
                )
                if (isClippingAvailable && !isClippingVideo && !isSplittingVideo) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Outlined.ContentCut, null) },
                        text = { Text(text = stringResource(id = R.string.clip_video)) },
                        onClick = {
                            onClippingToggled()
                            expanded = false
                        },
                    )
                }
                if (isSplitByChapterAvailable && !isClippingVideo && !isSplittingVideo) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Outlined.VerticalSplit, null) },
                        text = { Text(text = stringResource(id = R.string.split_video)) },
                        onClick = {
                            onSplittingToggled()
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun VideoInfoPreview() {
    DartDLTheme {
        Surface {
            Column {
                FormatVideoPreview(
                    title = stringResource(id = R.string.video_title_sample_text),
                    author = stringResource(id = R.string.video_creator_sample_text),
                    thumbnailUrl = "",
                    duration = 7890,
                    isSplittingVideo = false,
                    isClippingVideo = false,
                    isSplitByChapterAvailable = true,
                    isClippingAvailable = true,
                )
            }
        }
    }
}

@Composable
fun SuggestedFormatItem(
    modifier: Modifier = Modifier,
    videoInfo: VideoInfo,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val requestedFormats =
        videoInfo.requestedFormats
            ?: videoInfo.requestedDownloads?.map { it.toFormat() }
            ?: emptyList()
    val duration = videoInfo.duration ?: 0.0

    val containsVideo = requestedFormats.any { it.containsVideo() }
    val containsAudio = requestedFormats.any { it.containsVideo() }

    val title = requestedFormats.joinToString(separator = " + ") { it.toFriendlyLabel() }

    val totalFileSize =
        requestedFormats.fold(initial = 0.0) { acc: Double, format: Format ->
            acc +
                (format.fileSize ?: format.fileSizeApprox ?: (duration * (format.tbr ?: 0.0) * 125))
            // kbps -> bytes 1000/8
        }
    val fileSizeText = totalFileSize.toFileSizeText()

    val totalTbr =
        requestedFormats.fold(initial = 0.0) { acc: Double, format: Format ->
            acc + (format.tbr ?: 0.0)
        }

    val tbrText = totalTbr.toBitrateText()

    val firstLineText = connectWithDelimiter(fileSizeText, tbrText, delimiter = " ")

    val vcodecText = videoInfo.vcodec?.substringBefore(delimiter = ".") ?: ""
    val acodecText = videoInfo.acodec?.substringBefore(delimiter = ".") ?: ""

    val codecText =
        connectWithBlank(vcodecText, acodecText).run { if (isNotBlank()) "($this)" else this }

    val secondLineText = connectWithDelimiter(videoInfo.ext, codecText, delimiter = " ").uppercase()

    FormatItem(
        modifier = modifier,
        title = title,
        containsAudio = containsAudio,
        containsVideo = containsVideo,
        firstLineText = firstLineText,
        secondLineText = secondLineText,
        selected = selected,
        onClick = onClick,
    )
}

@Composable
fun FormatItem(
    modifier: Modifier = Modifier,
    formatInfo: Format,
    duration: Double,
    selected: Boolean = false,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {

    with(formatInfo) {
        val vcodecText = vcodec?.substringBefore(delimiter = ".") ?: ""
        val acodecText = acodec?.substringBefore(delimiter = ".") ?: ""

        val codec =
            connectWithBlank(vcodecText, acodecText).run { if (isNotBlank()) "($this)" else this }

        val tbrText =
            when {
                tbr == null -> "" // i don't care
                tbr < 1024f -> "%.1f Kbps".format(tbr)

                else -> "%.2f Mbps".format(tbr / 1024f)
            }

        val fileSize = fileSize ?: fileSizeApprox ?: (tbr?.times(duration * 125))
        val fileSizeText = fileSize.toFileSizeText()

        val firstLineText = connectWithDelimiter(fileSizeText, tbrText, delimiter = " ")

        val secondLineText = connectWithDelimiter(ext, codec, delimiter = " ").uppercase()

        FormatItem(
            modifier = modifier,
            title = formatInfo.toFriendlyLabel(),
            containsAudio = formatInfo.containsAudio(),
            containsVideo = formatInfo.containsVideo(),
            firstLineText = firstLineText,
            secondLineText = secondLineText,
            outlineColor = outlineColor,
            containerColor = containerColor,
            selected = selected,
            onLongClick = onLongClick,
            onClick = onClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FormatItem(
    modifier: Modifier = Modifier,
    title: String = "247 - 1280x720 (720p)",
    containsAudio: Boolean = false,
    containsVideo: Boolean = false,
    firstLineText: String,
    secondLineText: String,
    selected: Boolean = false,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {

    val animatedTitleColor by
        animateColorAsState(
            if (selected) outlineColor else MaterialTheme.colorScheme.onSurface,
            animationSpec = tween(100),
            label = "",
        )

    val animatedContainerColor by
        animateColorAsState(
            if (selected) containerColor else MaterialTheme.colorScheme.surface,
            animationSpec = tween(100),
            label = "",
        )

    val animatedOutlineColor by
        animateColorAsState(
            targetValue = if (selected) outlineColor else MaterialTheme.colorScheme.outlineVariant,
            animationSpec = tween(100),
            label = "",
        )

    Box(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .selectable(selected = selected) { onClick() }
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = onLongClick,
                    onLongClickLabel = stringResource(R.string.copy_link),
                )
                .border(
                    width = 1.dp,
                    color = animatedOutlineColor,
                    shape = MaterialTheme.shapes.medium,
                )
                .background(animatedContainerColor)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.Start) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                minLines = 2,
                maxLines = 2,
                color = animatedTitleColor,
                overflow = TextOverflow.Clip,
            )

            Text(
                text = firstLineText,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )

            Text(
                text = secondLineText,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 2.dp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Row(modifier = Modifier.padding(bottom = 6.dp, end = 6.dp).align(Alignment.BottomEnd)) {
            if (containsVideo)
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    tint = outlineColor,
                    contentDescription = stringResource(id = R.string.video),
                    modifier = Modifier.size(16.dp),
                )
            if (containsAudio)
                Icon(
                    imageVector = Icons.Rounded.Audiotrack,
                    tint = outlineColor,
                    contentDescription = stringResource(id = R.string.audio),
                    modifier = Modifier.size(16.dp),
                )
            if (!containsVideo && !containsAudio) {
                Icon(
                    imageVector = Icons.Rounded.QuestionMark,
                    tint = outlineColor,
                    contentDescription = stringResource(id = R.string.unknown),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Preview(name = "Light")
fun PreviewFormat() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        var selected by remember { mutableStateOf(-1) }
        Surface {
            Column() {
                //                FormatSubtitle(text = stringResource(R.string.video_only))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FormatPreviewContent(selected) { selected = it }
                }
            }
        }
    }
}

fun LazyGridScope.FormatPreviewContent(selected: Int = 0, onClick: (Int) -> Unit = {}) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        FormatSubtitle(
            text = "Suggested",
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).padding(horizontal = 12.dp),
        )
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
        FormatItem(
            selected = selected == 1,
            containsAudio = true,
            containsVideo = true,
            firstLineText = "? MB + 16.00 MB, (? + 200) Kbps",
            secondLineText = "MKV (Unknown + OPUS)",
        ) {
            onClick(1)
        }
    }

    item(span = { GridItemSpan(maxLineSpan) }) {
        FormatSubtitle(
            text = stringResource(R.string.audio),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).padding(horizontal = 12.dp),
        )
    }
    for (i in 0..1) {
        item {
            FormatItem(
                selected = selected == i,
                outlineColor = MaterialTheme.colorScheme.tertiary,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                containsVideo = false,
                containsAudio = true,
                firstLineText = "",
                secondLineText = "OPUS (OPUS)",
            ) {
                onClick(i)
            }
        }
    }
    item {
        FormatItem(
            selected = selected == 2,
            outlineColor = MaterialTheme.colorScheme.tertiary,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            containsVideo = false,
            containsAudio = true,
            firstLineText = "",
            secondLineText = "Unknown (Unknown)",
        ) {
            onClick(2)
        }
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
        FormatSubtitle(
            text = stringResource(R.string.video_only),
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).padding(horizontal = 12.dp),
        )
    }
    for (i in 0..2) {
        item {
            FormatItem(
                selected = selected == i,
                containsVideo = true,
                containsAudio = false,
                firstLineText = "69.00MB 745.7Kbps",
                secondLineText = "MP4 (AVC1)",
            ) {
                onClick(i)
            }
        }
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
        FormatSubtitle(
            text = stringResource(R.string.video),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).padding(horizontal = 12.dp),
        )
    }
    for (i in 0..3) {
        item {
            FormatItem(
                selected = selected == i,
                outlineColor = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                containsVideo = true,
                containsAudio = true,
                firstLineText = "",
                secondLineText = "",
            ) {
                onClick(i)
            }
        }
    }
}

@Composable
fun FormatSubtitle(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Preview
@Composable
fun FormatItemPreview() {
    FormatItem(formatInfo = Format(), duration = 20.0)
}
