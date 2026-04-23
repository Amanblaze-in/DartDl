package com.dartdl.app.util

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.CheckResult
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.dartdl.app.App.Companion.context
import com.dartdl.app.R
import java.io.Closeable
import java.io.File

const val AUDIO_REGEX = "(mp3|aac|opus|m4a)$"
const val THUMBNAIL_REGEX = "\\.(jpg|png)$"
const val SUBTITLE_REGEX = "\\.(lrc|vtt|srt|ass|json3|srv.|ttml)$"
private const val PRIVATE_DIRECTORY_SUFFIX = ".DartDL"

object FileUtil {
    fun openFileFromResult(downloadResult: Result<List<String>>) {
        val filePaths = downloadResult.getOrNull()
        if (filePaths.isNullOrEmpty()) return
        openFile(filePaths.first()) {
            context.makeToast(context.getString(R.string.file_unavailable))
        }
    }

    inline fun openFile(path: String, onFailureCallback: (Throwable) -> Unit) =
            path
                    .runCatching {
                        createIntentForOpeningFile(this)?.run { 
                            val chooserIntent = Intent.createChooser(this, "Open with")
                            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(chooserIntent) 
                        } ?: throw Exception()
                    }
                    .onFailure { onFailureCallback(it) }

    private fun createIntentForFile(path: String?): Intent? {
        if (path.isNullOrEmpty()) return null

        val uri =
                path.runCatching {
                    if (startsWith("content://")) {
                        Uri.parse(this)
                    } else {
                        DocumentFile.fromSingleUri(context, Uri.parse(this)).run {
                            if (this?.exists() == true) {
                                this.uri
                            } else if (File(this@runCatching).exists()) {
                                FileProvider.getUriForFile(
                                        context,
                                        context.getFileProvider(),
                                        File(this@runCatching),
                                )
                            } else null
                        }
                    }
                }
                        .getOrNull() ?: return null

        return Intent().apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            data = uri
        }
    }

    fun createIntentForOpeningFile(path: String?): Intent? =
            createIntentForFile(path)?.let { intent ->
                intent.apply {
                    action = Intent.ACTION_VIEW
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    val extension =
                            if (path?.startsWith("content://") == true) {
                                // For content URIs, try to get extension from display name if possible
                                DocumentFile.fromSingleUri(context, Uri.parse(path))
                                        ?.name
                                        ?.substringAfterLast('.', "")
                                        ?.lowercase() ?: ""
                            } else {
                                java.io.File(path ?: "").extension.lowercase()
                            }

                    var mimeType = if (path?.startsWith("content://") == true) {
                        context.contentResolver.getType(Uri.parse(path)) ?: getMimeType(extension)
                    } else {
                        getMimeType(extension)
                    }
                    
                    if (mimeType == "*/*" || mimeType == "application/octet-stream") {
                        mimeType = "video/*"
                    }
                    setDataAndType(data, mimeType)
                }
            }

    fun getMimeType(extension: String): String {
        val ext = extension.lowercase().removePrefix(".")

        // Fast-path: explicit map for common media formats
        val knownTypes = mapOf(
            // Video
            "mp4"  to "video/mp4",
            "mkv"  to "video/x-matroska",
            "webm" to "video/webm",
            "avi"  to "video/x-msvideo",
            "mov"  to "video/quicktime",
            "flv"  to "video/x-flv",
            "3gp"  to "video/3gpp",
            "ts"   to "video/mp2ts",
            "m4v"  to "video/x-m4v",
            // Audio
            "mp3"  to "audio/mpeg",
            "m4a"  to "audio/mp4",
            "aac"  to "audio/aac",
            "wav"  to "audio/wav",
            "ogg"  to "audio/ogg",
            "opus" to "audio/opus",
            "flac" to "audio/flac",
            "wma"  to "audio/x-ms-wma",
        )
        knownTypes[ext]?.let { return it }

        // Fallback: system MIME map
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)?.let { return it }

        // Ultimate fallback: prevents generic */* which triggers wallet/archiver choosers
        return if (ext.isNotEmpty()) "application/octet-stream" else "*/*"
    }

    fun createIntentForSharingFile(path: String?): Intent? {
        // Issue 5 Fix: Guard against sharing hidden/temp files like .nomedia.
        // The temp directory contains a .nomedia marker so media scanners skip it.
        // We MUST only share actual media files, not those hidden markers.
        if (path == null) return null
        
        // Only apply File-based name/extension checks to actual file paths, not content URIs.
        if (!path.startsWith("content://")) {
            val file = java.io.File(path)
            if (file.name.startsWith(".") || file.extension.isBlank()) {
                Log.w(TAG, "Attempted to share hidden/invalid file: $path")
                return null
            }
        }
        // For content URIs, check display name via DocumentFile
        if (path.startsWith("content://")) {
            val docFile = DocumentFile.fromSingleUri(context, Uri.parse(path))
            val name = docFile?.name ?: ""
            if (name.startsWith(".")) {
                Log.w(TAG, "Attempted to share hidden content URI: $path")
                return null
            }
        }

        return createIntentForFile(path)?.apply {
            action = Intent.ACTION_SEND
            // Determine MIME type from the path
            val extension = when {
                path.startsWith("content://") ->
                    DocumentFile.fromSingleUri(context, Uri.parse(path))
                        ?.name?.substringAfterLast('.', "")?.lowercase() ?: ""
                else -> java.io.File(path).extension.lowercase()
            }
            val mimeType = if (path.startsWith("content://")) {
                context.contentResolver.getType(Uri.parse(path)) ?: getMimeType(extension)
            } else {
                getMimeType(extension)
            }
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, data)
            clipData = ClipData(null, arrayOf(mimeType), ClipData.Item(data))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun Context.getFileProvider() = "$packageName.provider"

    fun String.getFileSize(): Long =
            this.run {
                val length = File(this).length()
                if (length == 0L)
                        DocumentFile.fromSingleUri(context, Uri.parse(this))?.length() ?: 0L
                else length
            }

    fun String.isValidDirectory(): Boolean {
        if (isEmpty()) return false
        if (startsWith("content://")) {
            return try {
                val uri = Uri.parse(this)
                true // If it's a valid URI string, we assume it's valid if we can parse it
                // Ideally we'd check if we still have permission, but this is used for UI validation
            } catch (e: Exception) {
                false
            }
        }
        val file = File(this)
        return file.exists() && file.isDirectory
    }

    fun String.getFileName(): String =
            this.run {
                File(this).nameWithoutExtension.ifEmpty {
                    DocumentFile.fromSingleUri(context, Uri.parse(this))?.name ?: "video"
                }
            }

    fun deleteFile(path: String) =
            path.runCatching {
                if (!File(path).delete())
                        DocumentFile.fromSingleUri(context, Uri.parse(this))?.delete()
            }

    @CheckResult
    fun scanFileToMediaLibraryPostDownload(title: String, downloadDir: String): List<String> =
            File(downloadDir)
                    .walkTopDown()
                    .filter { it.isFile && it.absolutePath.contains(title) }
                    .map { it.absolutePath }
                    .toMutableList()
                    .apply {
                        MediaScannerConnection.scanFile(
                                context,
                                this.toList().toTypedArray(),
                                null,
                                null
                        )
                        removeAll {
                            it.contains(Regex(THUMBNAIL_REGEX)) ||
                                    it.contains(Regex(SUBTITLE_REGEX))
                        }
                    }

    fun scanDownloadDirectoryToMediaLibrary(downloadDir: String) =
            File(downloadDir).walkTopDown().filter { it.isFile }.map { it.absolutePath }.run {
                MediaScannerConnection.scanFile(context, this.toList().toTypedArray(), null, null)
            }

    @CheckResult
    fun moveFilesToDestination(tempPath: File, destinationUri: String): Result<List<String>> {
        val uriList = mutableListOf<String>()
        val isContentUri = destinationUri.startsWith("content://")
        
        val res = tempPath.runCatching {
            if (isContentUri) {
                // Storage Access Framework (SAF) path
                val destDir = Uri.parse(destinationUri).run {
                    DocumentsContract.buildDocumentUriUsingTree(
                        this,
                        DocumentsContract.getTreeDocumentId(this),
                    )
                }
                walkTopDown().forEach {
                    if (it.isDirectory) return@forEach
                    val mimeType = getMimeType(it.extension)
                    val destUri = DocumentsContract.createDocument(
                        context.contentResolver,
                        destDir,
                        mimeType,
                        it.name,
                    ) ?: return@forEach

                    it.inputStream().use { input ->
                        context.contentResolver.openOutputStream(destUri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    uriList.add(destUri.toString())
                }
            } else {
                // Standard file path (MediaStore fallback for Play Store compliance)
                walkTopDown().forEach {
                    if (it.isDirectory) return@forEach
                    val savedUri = saveToMediaStore(it, destinationUri)
                    if (savedUri != null) {
                        uriList.add(savedUri.toString())
                    } else {
                        Log.e(TAG, "Failed to save ${it.name} to MediaStore at $destinationUri")
                    }
                }
            }
            uriList
        }
        tempPath.deleteRecursively()
        return res
    }

    private fun saveToMediaStore(file: File, destinationPath: String): Uri? {
        val resolver = context.contentResolver
        val mimeType = getMimeType(file.extension)
        
        // Android 16 (API 36) enforcement: 
        // Videos MUST be in DCIM/Movies/Pictures. 
        // Audio MUST be in Music/Alarms/Notifications/Podcasts/Ringtones/Download.
        // We will map them to the most appropriate folders for all versions.
        val primaryDirectory = when {
            mimeType.startsWith("video/") -> Environment.DIRECTORY_MOVIES
            mimeType.startsWith("audio/") -> Environment.DIRECTORY_MUSIC
            else -> Environment.DIRECTORY_DOWNLOADS
        }

        val collection = when {
            mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("audio/") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            
            val currentTimestamp = System.currentTimeMillis() / 1000
            put(MediaStore.MediaColumns.DATE_ADDED, currentTimestamp)
            put(MediaStore.MediaColumns.DATE_MODIFIED, currentTimestamp)
            
            if (mimeType.startsWith("video/")) {
                put(MediaStore.Video.VideoColumns.DATE_TAKEN, System.currentTimeMillis())
            }
            
            // Reconstruct relative path to start with the correct primary directory for the collection
            val relativePath = if (destinationPath.contains("Download") || destinationPath.contains("Movies") || destinationPath.contains("Music")) {
                val subPath = destinationPath.substringAfterLast('/').trim('/')
                if (subPath.isEmpty() || subPath == "DartDL") "$primaryDirectory/DartDL/" else "$primaryDirectory/$subPath/"
            } else {
                "$primaryDirectory/DartDL/"
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val itemUri = resolver.insert(collection, contentValues) ?: return null

        try {
            resolver.openOutputStream(itemUri)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }
            return itemUri
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore insert failed", e)
            resolver.delete(itemUri, null, null)
            return null
        }
    }

    fun clearTempFiles(downloadDir: File): Int {
        var count = 0
        downloadDir.walkTopDown().forEach {
            if (it.isFile && !it.isHidden) {
                if (it.delete()) count++
            }
        }
        return count
    }

    private fun Closeable?.closeQuietly() {
        try {
            this?.close()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun Context.getConfigDirectory(): File = cacheDir

    fun Context.getConfigFile(suffix: String = "") = File(getConfigDirectory(), "config$suffix.txt")

    fun Context.getCookiesFile() = File(getConfigDirectory(), "cookies.txt")

    fun Context.getExternalTempDir() =
            File(getExternalFilesDir(null), "tmp").apply {
                mkdirs()
                createEmptyFile(".nomedia")
            }

    fun Context.getSdcardTempDir(child: String?): File =
            getExternalTempDir().run { child?.let { resolve(it) } ?: this }

    fun Context.getArchiveFile(): File = filesDir.createEmptyFile("archive.txt").getOrThrow()

    fun Context.getInternalTempDir() = File(filesDir, "tmp")

    internal fun getExternalDownloadDirectory(): File {
        val publicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "DartDL"
        )
        return try {
            if (publicDir.exists() || publicDir.mkdirs()) {
                publicDir
            } else {
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            }
        } catch (e: Exception) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        }
    }

    internal fun getExternalPrivateDownloadDirectory() =
            File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    PRIVATE_DIRECTORY_SUFFIX,
            )

    fun File.createEmptyFile(fileName: String): Result<File> =
            this
                    .runCatching {
                        mkdirs()
                        resolve(fileName).apply { this@apply.createNewFile() }
                    }
                    .onFailure { it.printStackTrace() }

    fun writeContentToFile(content: String, file: File): File = file.apply { writeText(content) }

    fun getRealPath(treeUri: Uri): String {
        val path: String = treeUri.path.toString()
        Log.d(TAG, "getRealPath treeUri.path: $path")
        
        // Handle various storage volume patterns (e.g., primary:Download/DartDL, 1234-ABCD:Movies)
        val parts = path.split(":")
        if (parts.size < 2) {
            Log.e(TAG, "Invalid path format: $path")
            return getExternalDownloadDirectory().absolutePath
        }

        val volumeId = parts[0].split("/").last()
        val relativePath = parts[1]

        return if (volumeId == "primary") {
            Environment.getExternalStorageDirectory().absolutePath + "/$relativePath"
        } else {
            // For SD cards, the system usually mounts them under /storage/VOLUME_ID
            val sdCardPath = "/storage/$volumeId/$relativePath"
            if (File(sdCardPath).exists()) {
                sdCardPath
            } else {
                // Fallback: try to find the mount point if possible, or use default
                "/storage/$volumeId/$relativePath"
            }
        }
    }

    private const val TAG = "FileUtil"
}
