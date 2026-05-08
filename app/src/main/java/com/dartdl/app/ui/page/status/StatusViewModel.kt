package com.dartdl.app.ui.page.status

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartdl.app.App.Companion.context
import com.dartdl.app.util.AdManager
import com.dartdl.app.util.FileUtil
import com.dartdl.app.util.PreferenceUtil
import com.dartdl.app.util.makeToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StatusMedia(
    val uri: Uri,
    val name: String,
    val isVideo: Boolean,
    val size: Long,
    val lastModified: Long
)

class StatusViewModel : ViewModel() {
    private val _whatsappStatuses = MutableStateFlow<List<StatusMedia>>(emptyList())
    val whatsappStatuses = _whatsappStatuses.asStateFlow()

    private val _businessStatuses = MutableStateFlow<List<StatusMedia>>(emptyList())
    val businessStatuses = _businessStatuses.asStateFlow()

    private val _isWhatsAppPermissionGranted = MutableStateFlow(false)
    val isWhatsAppPermissionGranted = _isWhatsAppPermissionGranted.asStateFlow()

    private val _isBusinessPermissionGranted = MutableStateFlow(false)
    val isBusinessPermissionGranted = _isBusinessPermissionGranted.asStateFlow()

    fun checkPermissions(context: Context) {
        viewModelScope.launch {
            _isWhatsAppPermissionGranted.value = hasPermission(context, WHATSAPP_URI)
            _isBusinessPermissionGranted.value = hasPermission(context, BUSINESS_URI)
            
            if (_isWhatsAppPermissionGranted.value) loadStatuses(context, WHATSAPP_URI, true)
            if (_isBusinessPermissionGranted.value) loadStatuses(context, BUSINESS_URI, false)
        }
    }

    private fun hasPermission(context: Context, uriString: String): Boolean {
        val uri = Uri.parse(uriString)
        val persistedUriPermissions = context.contentResolver.persistedUriPermissions
        return persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
    }

    private fun loadStatuses(context: Context, uriString: String, isWhatsApp: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val treeUri = Uri.parse(uriString)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                val files = rootDoc?.listFiles()?.filter { 
                    it.isFile && (it.name?.endsWith(".jpg") == true || it.name?.endsWith(".mp4") == true)
                }?.map {
                    StatusMedia(
                        uri = it.uri,
                        name = it.name ?: "unknown",
                        isVideo = it.name?.endsWith(".mp4") == true,
                        size = it.length(),
                        lastModified = it.lastModified()
                    )
                }?.sortedByDescending { it.lastModified } ?: emptyList()

                if (isWhatsApp) {
                    _whatsappStatuses.value = files
                } else {
                    _businessStatuses.value = files
                }
            } catch (e: Exception) {
                Log.e("StatusViewModel", "Failed to load statuses", e)
            }
        }
    }

    fun saveStatus(context: Context, media: StatusMedia, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempDir = with(FileUtil) { context.getInternalTempDir() }
                tempDir.mkdirs()
                val tempFile = File(tempDir, media.name)
                
                context.contentResolver.openInputStream(media.uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                val destinationUri = PreferenceUtil.getDownloadDirectory()
                val result = FileUtil.moveFilesToDestination(tempDir, destinationUri)
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        context.makeToast("Saved successfully")
                        onComplete(true)
                    } else {
                        context.makeToast("Failed to save status")
                        onComplete(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("StatusViewModel", "Failed to save status", e)
                withContext(Dispatchers.Main) {
                    context.makeToast("Failed to save status")
                    onComplete(false)
                }
            }
        }
    }

    companion object {
        const val WHATSAPP_URI = "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
        const val BUSINESS_URI = "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp%20Business%2FMedia%2F.Statuses"
    }
}
