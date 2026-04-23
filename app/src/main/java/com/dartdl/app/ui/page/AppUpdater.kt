package com.dartdl.app.ui.page

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dartdl.app.R
import com.dartdl.app.util.PreferenceUtil
import com.dartdl.app.util.UpdateUtil
import com.dartdl.app.util.makeToast
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AppUpdater"

@Composable
fun AppUpdater() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Non-PlayStore Update State
    var showGithubUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var currentDownloadStatus by remember {
        mutableStateOf(UpdateUtil.DownloadStatus.NotYet as UpdateUtil.DownloadStatus)
    }
    var updateJob: Job? = null
    var release by remember { mutableStateOf(UpdateUtil.Release()) }

    // Play Store Update State
    val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }
    var showPlayStoreRestartDialog by remember { mutableStateOf(false) }

    val playStoreUpdateListener = remember {
        InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                showPlayStoreRestartDialog = true
            }
        }
    }

    // Launchers for Non-PlayStore (APK) Installation
    val settingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            UpdateUtil.installLatestApk()
        }
    val apkInstallLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                UpdateUtil.installLatestApk()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!context.packageManager.canRequestPackageInstalls()) {
                        settingsLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:${context.packageName}"),
                            )
                        )
                    } else {
                        UpdateUtil.installLatestApk()
                    }
                }
            }
        }

    // Register/Unregister Play Store listener
    DisposableEffect(appUpdateManager) {
        if (PreferenceUtil.isPlayStoreBuild()) {
            appUpdateManager.registerListener(playStoreUpdateListener)
        }
        onDispose {
            if (PreferenceUtil.isPlayStoreBuild()) {
                appUpdateManager.unregisterListener(playStoreUpdateListener)
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(2000) // Small delay to avoid blocking initial UI layout

        if (!PreferenceUtil.isAutoUpdateEnabled()) return@LaunchedEffect

        if (PreferenceUtil.isPlayStoreBuild()) {
            // Check for Play Store updates
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                // 1. Check if an update is already downloaded but not installed
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    showPlayStoreRestartDialog = true
                    return@addOnSuccessListener
                }

                // 2. Check for available updates
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    val isImmediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                    val isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

                    // Usually we prefer Flexible for better UX, but Immediate for critical updates
                    // For now, let's use Flexible if allowed, otherwise fallback to Immediate
                    val updateType = when {
                        isFlexibleAllowed -> AppUpdateType.FLEXIBLE
                        isImmediateAllowed -> AppUpdateType.IMMEDIATE
                        else -> null
                    }

                    updateType?.let { type ->
                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                type,
                                context as Activity,
                                1001 // Request code
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error starting Play Store update flow", e)
                        }
                    }
                }
            }.addOnFailureListener {
                Log.e(TAG, "Failed to fetch Play Store update info", it)
            }
        } else {
            // Check for GitHub updates (APK version)
            if (!PreferenceUtil.isNetworkAvailableForDownload()) return@LaunchedEffect

            withContext(Dispatchers.IO) {
                runCatching {
                    UpdateUtil.checkForUpdate()?.let {
                        release = it
                        showGithubUpdateDialog = true
                    }
                }.onFailure { Log.e(TAG, "Failed to check for GitHub updates", it) }
            }
        }
    }

    // UI: Play Store Restart Dialog (For Flexible Updates)
    if (showPlayStoreRestartDialog) {
        AlertDialog(
            onDismissRequest = { showPlayStoreRestartDialog = false },
            title = { Text(stringResource(R.string.update_downloaded)) },
            text = { Text(stringResource(R.string.update_restart)) },
            confirmButton = {
                TextButton(onClick = {
                    showPlayStoreRestartDialog = false
                    appUpdateManager.completeUpdate()
                }) {
                    Text(stringResource(R.string.restart))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlayStoreRestartDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // UI: GitHub (APK) Update Dialog
    if (showGithubUpdateDialog) {
        UpdateDialogImpl(
            onDismissRequest = {
                showGithubUpdateDialog = false
                updateJob?.cancel()
            },
            title = release.name.toString(),
            onConfirmUpdate = {
                updateJob = scope.launch(Dispatchers.IO) {
                    runCatching {
                        UpdateUtil.downloadApk(release = release).collect { downloadStatus ->
                            currentDownloadStatus = downloadStatus
                            if (downloadStatus is UpdateUtil.DownloadStatus.Finished) {
                                apkInstallLauncher.launch(Manifest.permission.REQUEST_INSTALL_PACKAGES)
                            }
                        }
                    }.onFailure {
                        Log.e(TAG, "GitHub APK download failed", it)
                        currentDownloadStatus = UpdateUtil.DownloadStatus.NotYet
                        context.makeToast(context.getString(R.string.app_update_failed))
                    }
                }
            },
            releaseNote = release.body.toString(),
            downloadStatus = currentDownloadStatus,
        )
    }
}
