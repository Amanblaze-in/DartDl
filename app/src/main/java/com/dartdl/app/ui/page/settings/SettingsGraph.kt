package com.dartdl.app.ui.page.settings

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.dartdl.app.ui.common.Route
import com.dartdl.app.ui.common.animatedComposable
import com.dartdl.app.ui.page.PrivacyPolicyPage
import com.dartdl.app.ui.page.settings.about.AboutPage
import com.dartdl.app.ui.page.settings.about.CreditsPage
import com.dartdl.app.ui.page.settings.about.SponsorsPage
import com.dartdl.app.ui.page.settings.about.UpdatePage
import com.dartdl.app.ui.page.settings.appearance.AppearancePreferences
import com.dartdl.app.ui.page.settings.appearance.DarkThemePreferences
import com.dartdl.app.ui.page.settings.appearance.LanguagePage
import com.dartdl.app.ui.page.settings.command.TemplateEditPage
import com.dartdl.app.ui.page.settings.command.TemplateListPage
import com.dartdl.app.ui.page.settings.directory.DownloadDirectoryPreferences
import com.dartdl.app.ui.page.settings.format.DownloadFormatPreferences
import com.dartdl.app.ui.page.settings.format.SubtitlePreference
import com.dartdl.app.ui.page.settings.general.GeneralDownloadPreferences
import com.dartdl.app.ui.page.settings.interaction.InteractionPreferencePage
import com.dartdl.app.ui.page.settings.network.CookieProfilePage
import com.dartdl.app.ui.page.settings.network.NetworkPreferences
import com.dartdl.app.ui.page.settings.network.WebViewPage
import com.dartdl.app.ui.page.settings.troubleshooting.TroubleShootingPage
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.settingsGraph(
    onNavigateBack: () -> Unit,
    onNavigateTo: (String) -> Unit,
) {
    // Map both SETTINGS and SETTINGS_PAGE to the main settings page for consistency
    val settingsPage: NavGraphBuilder.() -> Unit = {
        animatedComposable(Route.SETTINGS) {
            SettingsPage(onNavigateBack = onNavigateBack, onNavigateTo = onNavigateTo)
        }
        animatedComposable(Route.SETTINGS_PAGE) {
            SettingsPage(onNavigateBack = onNavigateBack, onNavigateTo = onNavigateTo)
        }
    }
    settingsPage()

    animatedComposable(Route.GENERAL_DOWNLOAD_PREFERENCES) {
        GeneralDownloadPreferences(
            onNavigateBack = onNavigateBack,
            navigateToTemplate = { onNavigateTo(Route.TEMPLATE) }
        )
    }

    animatedComposable(Route.DOWNLOAD_DIRECTORY) {
        DownloadDirectoryPreferences(onNavigateBack = onNavigateBack)
    }

    animatedComposable(Route.DOWNLOAD_FORMAT) {
        DownloadFormatPreferences(
            onNavigateBack = onNavigateBack,
            navigateToSubtitlePage = { onNavigateTo(Route.SUBTITLE_PREFERENCES) }
        )
    }

    animatedComposable(Route.NETWORK_PREFERENCES) {
        NetworkPreferences(
            navigateToCookieProfilePage = { onNavigateTo(Route.COOKIE_PROFILE) },
            onNavigateBack = onNavigateBack
        )
    }

    animatedComposable(Route.COOKIE_PROFILE) {
        CookieProfilePage(
            cookiesViewModel = koinViewModel(),
            navigateToCookieGeneratorPage = { onNavigateTo(Route.COOKIE_GENERATOR_WEBVIEW) },
            onNavigateBack = onNavigateBack
        )
    }

    animatedComposable(Route.COOKIE_GENERATOR_WEBVIEW) {
        WebViewPage(
            cookiesViewModel = koinViewModel(),
            onDismissRequest = onNavigateBack
        )
    }

    // Map both TEMPLATE and TASK_LIST to the template list page
    val templateListPage: NavGraphBuilder.() -> Unit = {
        animatedComposable(Route.TEMPLATE) {
            TemplateListPage(
                onNavigateBack = onNavigateBack,
                onNavigateToEditPage = { id -> onNavigateTo("${Route.TEMPLATE_EDIT}/$id") }
            )
        }
        animatedComposable(Route.TASK_LIST) {
            TemplateListPage(
                onNavigateBack = onNavigateBack,
                onNavigateToEditPage = { id -> onNavigateTo("${Route.TEMPLATE_EDIT}/$id") }
            )
        }
    }
    templateListPage()

    animatedComposable(
        route = "${Route.TEMPLATE_EDIT}/{${Route.TEMPLATE_ID}}",
        arguments = listOf(navArgument(Route.TEMPLATE_ID) { type = NavType.IntType })
    ) { backStackEntry ->
        val templateId = backStackEntry.arguments?.getInt(Route.TEMPLATE_ID) ?: 0
        TemplateEditPage(onDismissRequest = onNavigateBack, templateId = templateId)
    }

    animatedComposable(Route.APPEARANCE) {
        AppearancePreferences(onNavigateBack = onNavigateBack, onNavigateTo = onNavigateTo)
    }

    animatedComposable(Route.DARK_THEME) {
        DarkThemePreferences(onNavigateBack = onNavigateBack)
    }

    animatedComposable(Route.LANGUAGES) {
        LanguagePage(onNavigateBack = onNavigateBack)
    }

    animatedComposable(Route.INTERACTION) {
        InteractionPreferencePage(onBack = onNavigateBack)
    }

    animatedComposable(Route.TROUBLESHOOTING) {
        TroubleShootingPage(
            modifier = Modifier,
            onNavigateTo = onNavigateTo,
            onBack = onNavigateBack
        )
    }

    animatedComposable(Route.ABOUT) {
        AboutPage(
            onNavigateBack = onNavigateBack,
            onNavigateToCreditsPage = { onNavigateTo(Route.CREDITS) },
            onNavigateToUpdatePage = { onNavigateTo(Route.AUTO_UPDATE) },
            onNavigateToDonatePage = { onNavigateTo(Route.DONATE) },
        )
    }

    animatedComposable(Route.CREDITS) {
        CreditsPage(onNavigateBack = onNavigateBack)
    }

    animatedComposable(Route.AUTO_UPDATE) {
        UpdatePage(onNavigateBack = onNavigateBack)
    }

    animatedComposable(Route.DONATE) {
        SponsorsPage(onNavigateBack = onNavigateBack)
    }

    animatedComposable(Route.SUBTITLE_PREFERENCES) {
        SubtitlePreference(onNavigateBack = onNavigateBack)
    }

    animatedComposable(Route.PRIVACY_POLICY) {
        PrivacyPolicyPage(onNavigateBack = onNavigateBack)
    }
}
