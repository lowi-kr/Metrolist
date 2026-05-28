/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.arubr.smsvcodes.ui.screens

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.arubr.smsvcodes.constants.DarkModeKey
import com.arubr.smsvcodes.constants.PureBlackKey
import com.arubr.smsvcodes.ui.screens.artist.ArtistAlbumsScreen
import com.arubr.smsvcodes.ui.screens.artist.ArtistItemsScreen
import com.arubr.smsvcodes.ui.screens.artist.ArtistScreen
import com.arubr.smsvcodes.ui.screens.artist.ArtistSongsScreen
import com.arubr.smsvcodes.ui.screens.equalizer.EqScreen
import com.arubr.smsvcodes.ui.screens.equalizer.wizard.WizardScreen
import com.arubr.smsvcodes.ui.screens.library.LibraryScreen
import com.arubr.smsvcodes.ui.screens.playlist.AutoPlaylistScreen
import com.arubr.smsvcodes.ui.screens.playlist.CachePlaylistScreen
import com.arubr.smsvcodes.ui.screens.playlist.LocalPlaylistScreen
import com.arubr.smsvcodes.ui.screens.playlist.OnlinePlaylistScreen
import com.arubr.smsvcodes.ui.screens.playlist.TopPlaylistScreen
import com.arubr.smsvcodes.ui.screens.podcast.OnlinePodcastScreen
import com.arubr.smsvcodes.ui.screens.recognition.RecognitionHistoryScreen
import com.arubr.smsvcodes.ui.screens.recognition.RecognitionScreen
import com.arubr.smsvcodes.ui.screens.search.OnlineSearchResult
import com.arubr.smsvcodes.ui.screens.search.SearchScreen
import com.arubr.smsvcodes.ui.screens.settings.AboutScreen
import com.arubr.smsvcodes.ui.screens.settings.AiSettings
import com.arubr.smsvcodes.ui.screens.settings.AndroidAutoSettings
import com.arubr.smsvcodes.ui.screens.settings.AppearanceSettings
import com.arubr.smsvcodes.ui.screens.settings.BackupAndRestore
import com.arubr.smsvcodes.ui.screens.settings.ContentSettings
import com.arubr.smsvcodes.ui.screens.settings.DarkMode
import com.arubr.smsvcodes.ui.screens.settings.DiscordLoginScreen
import com.arubr.smsvcodes.ui.screens.settings.PlayerSettings
import com.arubr.smsvcodes.ui.screens.settings.PrivacySettings
import com.arubr.smsvcodes.ui.screens.settings.RomanizationSettings
import com.arubr.smsvcodes.ui.screens.settings.SettingsScreen
import com.arubr.smsvcodes.ui.screens.settings.StorageSettings
import com.arubr.smsvcodes.ui.screens.settings.ThemeScreen
import com.arubr.smsvcodes.ui.screens.settings.UpdaterScreen
import com.arubr.smsvcodes.ui.screens.settings.integrations.DiscordSettings
import com.arubr.smsvcodes.ui.screens.settings.integrations.IntegrationScreen
import com.arubr.smsvcodes.ui.screens.settings.integrations.LastFMSettings
import com.arubr.smsvcodes.ui.screens.settings.integrations.ListenTogetherSettings

import com.arubr.smsvcodes.ui.screens.wrapped.WrappedScreen
import com.arubr.smsvcodes.utils.rememberEnumPreference
import com.arubr.smsvcodes.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    composable(Screens.Home.route) {
        HomeScreen(navController = navController, snackbarHostState = snackbarHostState)
    }

    composable(Screens.Search.route) { backStackEntry ->
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme =
            remember(darkTheme, isSystemInDarkTheme) {
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            }
        val pureBlack =
            remember(pureBlackEnabled, useDarkTheme) {
                pureBlackEnabled && useDarkTheme
            }
        SearchScreen(
            navController = navController,
            pureBlack = pureBlack,
            savedStateHandle = backStackEntry.savedStateHandle
        )
    }

    composable(Screens.Library.route) {
        LibraryScreen(navController)
    }

    composable(Screens.ListenTogether.route) {
        ListenTogetherScreen(navController, showTopBar = false)
    }

    composable(
        route = "listen_together_from_topbar",
    ) {
        ListenTogetherScreen(navController, showTopBar = true)
    }

    composable("history") {
        HistoryScreen(navController)
    }

    composable("stats") {
        StatsScreen(navController)
    }

    composable("mood_and_genres") {
        MoodAndGenresScreen(navController)
    }

    composable("account") {
        AccountScreen(navController)
    }

    composable("new_release") {
        NewReleaseScreen(navController)
    }

    composable("charts_screen") {
        ChartsScreen(navController)
    }

    composable(
        route = "browse/{browseId}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                },
            ),
    ) {
        BrowseScreen(
            navController,
            it.arguments?.getString("browseId"),
        )
    }

    composable(
        route = "search/{query}",
        arguments =
            listOf(
                navArgument("query") {
                    type = NavType.StringType
                },
            ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        },
    ) { backStackEntry ->
        OnlineSearchResult(
            navController = navController,
            savedStateHandle = backStackEntry.savedStateHandle
        )

    }

    composable(
        route = "album/{albumId}",
        arguments =
            listOf(
                navArgument("albumId") {
                    type = NavType.StringType
                },
            ),
    ) {
        AlbumScreen(navController)
    }

    composable(
        route = "artist/{artistId}?isPodcastChannel={isPodcastChannel}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("isPodcastChannel") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) {
        ArtistScreen(navController)
    }

    composable(
        route = "artist/{artistId}/songs",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistSongsScreen(navController)
    }

    composable(
        route = "artist/{artistId}/albums",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        ArtistItemsScreen(navController)
    }

    composable(
        route = "online_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePlaylistScreen(navController)
    }

    composable(
        route = "online_podcast/{podcastId}",
        arguments =
            listOf(
                navArgument("podcastId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePodcastScreen(navController, scrollBehavior)
    }

    composable(
        route = "local_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        LocalPlaylistScreen(navController)
    }

    composable(
        route = "auto_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        AutoPlaylistScreen(navController)
    }

    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        CachePlaylistScreen(navController)
    }

    composable(
        route = "top_playlist/{top}",
        arguments =
            listOf(
                navArgument("top") {
                    type = NavType.StringType
                },
            ),
    ) {
        TopPlaylistScreen(navController)
    }

    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        YouTubeBrowseScreen(navController)
    }

    composable("settings") {
        SettingsScreen(navController, latestVersionName)
    }

    composable("settings/appearance") {
        AppearanceSettings(navController, activity, snackbarHostState)
    }

    composable("settings/appearance/theme") {
        ThemeScreen(navController)
    }

    composable("settings/content") {
        ContentSettings(navController)
    }

    composable("settings/content/romanization") {
        RomanizationSettings(navController)
    }

    composable("settings/ai") {
        AiSettings(navController)
    }

    composable("settings/player") {
        PlayerSettings(navController)
    }

    composable("settings/storage") {
        StorageSettings(navController)
    }

    composable("settings/privacy") {
        PrivacySettings(navController)
    }

    composable("settings/backup_restore") {
        BackupAndRestore(navController)
    }

    composable("settings/integrations") {
        IntegrationScreen(navController)
    }

    composable("settings/integrations/discord") {
        DiscordSettings(navController, snackbarHostState)
    }

    composable("settings/integrations/lastfm") {
        LastFMSettings(navController)
    }

    composable(route = "settings/integrations/listen_together") {
        ListenTogetherSettings(navController)
    }

    composable("settings/discord/login") {
        DiscordLoginScreen(navController)
    }

    composable("settings/updater") {
        UpdaterScreen(navController)
    }

    composable("settings/about") {
        AboutScreen(navController)
    }

    composable("login") {
        LoginScreen(navController)
    }

    composable("wrapped") {
        WrappedScreen(navController)
    }

    composable("equalizer") {
        EqScreen(navController = navController)
    }

    composable("eq_wizard") {
        WizardScreen(onNavigateBack = {
            navController.popBackStack()
        })
    }

    composable(
        route = "recognition?autoStart={autoStart}",
        arguments =
            listOf(
                navArgument("autoStart") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) {
        RecognitionScreen(navController, it.arguments?.getBoolean("autoStart") ?: false)
    }

    composable("recognition_history") {
        RecognitionHistoryScreen(navController)
    }
    composable("settings/android_auto") {
        AndroidAutoSettings(navController, scrollBehavior)
    }
}
