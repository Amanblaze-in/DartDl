package com.dartdl.app.database.backup

import com.dartdl.app.database.objects.CommandTemplate
import com.dartdl.app.database.objects.DownloadedVideoInfo
import com.dartdl.app.database.objects.OptionShortcut
import kotlinx.serialization.Serializable

@Serializable
data class Backup(
    val templates: List<CommandTemplate>? = null,
    val shortcuts: List<OptionShortcut>? = null,
    val downloadHistory: List<DownloadedVideoInfo>? = null,
)
