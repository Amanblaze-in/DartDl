package com.dartdl.app.ui.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dartdl.app.ui.common.HapticFeedback.slightHapticFeedback
import com.dartdl.app.ui.common.LocalWindowWidthState
import com.dartdl.app.ui.common.Route
import com.dartdl.app.ui.common.animatedComposable
import com.dartdl.app.ui.page.downloadv2.DownloadPageV2
import com.dartdl.app.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.dartdl.app.ui.page.settings.settingsGraph
import com.dartdl.app.ui.page.videolist.VideoListPage
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.launch

@Composable
fun AppEntry(dialogViewModel: DownloadDialogViewModel) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val view = LocalView.current

    val onNavigateBack: () -> Unit = {
        view.slightHapticFeedback()
        if (!navController.popBackStack()) {
            // Handle root back
        }
    }

    val onNavigateTo: (String) -> Unit = { route ->
        view.slightHapticFeedback()
        navController.navigate(route) {
            if (route == Route.HOME) {
                popUpTo(Route.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    CompositionLocalProvider(LocalWindowWidthState provides LocalWindowWidthState.current) {
        NavigationDrawer(
            drawerState = drawerState,
            currentRoute = currentRoute,
            currentTopDestination = currentRoute,
            onNavigateToRoute = onNavigateTo,
            onDismissRequest = { drawerState.close() },
            gesturesEnabled = drawerState.isOpen,
        ) {
            NavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                startDestination = Route.HOME,
            ) {
                animatedComposable(Route.HOME) {
                    DownloadPageV2(
                        dialogViewModel = dialogViewModel,
                        onMenuOpen = {
                            view.slightHapticFeedback()
                            scope.launch { drawerState.open() }
                        },
                        onNavigateToPlayer = { path, title ->
                            val encodedUri = android.net.Uri.encode(path)
                            val encodedTitle = android.net.Uri.encode(title.ifBlank { " " })
                            navController.navigate("${Route.PLAYER}/$encodedUri/$encodedTitle")
                        },
                    )
                }
                animatedComposable(Route.DOWNLOADS) {
                    VideoListPage(
                        onNavigateBack = { onNavigateBack() },
                        onNavigateToPlayer = { path, title ->
                            val encodedUri = android.net.Uri.encode(path)
                            val encodedTitle = android.net.Uri.encode(title.ifBlank { " " })
                            navController.navigate("${Route.PLAYER}/$encodedUri/$encodedTitle")
                        },
                    )
                }

                settingsGraph(
                    onNavigateBack = onNavigateBack,
                    onNavigateTo = onNavigateTo,
                )

                animatedComposable(
                    route = "${Route.PLAYER}/{uri}/{title}",
                    arguments = listOf(
                        navArgument("uri") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    val uri = backStackEntry.arguments?.getString("uri") ?: ""
                    val title = backStackEntry.arguments?.getString("title") ?: ""

                    PlayerPage(
                        fileUri = uri,
                        title = title,
                        onBack = { onNavigateBack() },
                    )
                }
            }
        }
    }
}
