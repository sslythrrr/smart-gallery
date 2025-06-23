package com.sslythrrr.galeri.navigation
//v
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sslythrrr.galeri.ui.media.Media
import com.sslythrrr.galeri.ui.screens.AlbumDetailScreen
import com.sslythrrr.galeri.ui.screens.ChatbotResults
import com.sslythrrr.galeri.ui.screens.MainScreen
import com.sslythrrr.galeri.ui.screens.MediaDetailScreen
import com.sslythrrr.galeri.ui.screens.SplashScreen
import com.sslythrrr.galeri.ui.screens.mainscreen.SemuaMedia
import com.sslythrrr.galeri.ui.screens.management.FavoriteMediaScreen
import com.sslythrrr.galeri.ui.screens.management.LargeSizeMediaScreen
import com.sslythrrr.galeri.ui.screens.management.VideoFilesScreen
import com.sslythrrr.galeri.ui.screens.settings.AboutScreen
import com.sslythrrr.galeri.ui.screens.settings.ContactScreen
import com.sslythrrr.galeri.ui.screens.settings.SettingsScreen
import com.sslythrrr.galeri.viewmodel.ChatbotViewModel
import com.sslythrrr.galeri.viewmodel.MediaViewModel
import com.sslythrrr.galeri.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
fun Navigation(
    viewModel: MediaViewModel,
    themeViewModel: ThemeViewModel,
    chatbotViewModel: ChatbotViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val currentAlbum by viewModel.currentAlbum.collectAsState()
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    val allMediaState = remember { mutableStateOf<List<Media>>(emptyList()) }
    val navigationState = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        allMediaState.value = viewModel.fetchMedia(context)
        viewModel.initializeFavoriteRepository(context)
    }

    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.route == "main") {
                viewModel.setCurrentAlbum(null, context, shouldLoadMedia = false)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splashscreen"
    ) {
        composable("splashscreen") {
            SplashScreen(
                context = context,
                viewModel = viewModel,
                onLoadComplete = {
                    navController.navigate("main") {
                        popUpTo("splashscreen") { inclusive = true }
                    }

                },
                isDarkTheme = isDarkTheme
            )
        }
        composable("main") {
            var isMainScreenLoading by remember { mutableStateOf(false) }
            MainScreen(
                context = context,
                onMediaClick = { media ->
                    if (!navigationState.value) {
                        navigationState.value = true
                        navController.navigate("mediaDetail/${media.id}")
                        coroutineScope.launch {
                            navigationState.value = false
                        }
                    }
                },
                onAlbumClick = { album ->
                    if (!navigationState.value) {
                        navigationState.value = true
                        viewModel.setCurrentAlbum(album, context)
                        navController.navigate("albumDetail")
                        coroutineScope.launch {
                            navigationState.value = false
                        }
                    }
                },
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                chatbotViewModel = chatbotViewModel,
                navController = navController,
                onNavigationStateChange = { isLoading ->
                    isMainScreenLoading = isLoading
                },
                onImageClick = { path ->
                    coroutineScope.launch {
                        val allMedia = viewModel.fetchMedia(context)
                        val foundMedia = allMedia.find { it.uri.toString() == path }
                        foundMedia?.let { media ->
                            if (!navigationState.value) {
                                navigationState.value = true
                                navController.navigate("mediaDetail/${media.id}")
                                coroutineScope.launch {
                                    navigationState.value = false
                                }
                            }
                        }
                    }
                },
                onShowAllImages = {
                    navController.navigate("filteredImages")
                },
                onThemeChange = { isDark ->
                    themeViewModel.toggleTheme(isDark)
                },
                onAboutClick = {
                    navController.navigate("about")
                },
                onContactClick = {
                    navController.navigate("contact")
                },
            )
        }
        composable(
            "allMedia",
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { it } }
        ) {
            SemuaMedia(
                onBack = { navController.popBackStack() },
                onMediaClick = { media ->
                    navController.navigate("mediaDetail/${media.id}")
                },
                viewModel = viewModel,
                isDarkTheme = isDarkTheme
            )
        }
        composable("filteredImages") {
            ChatbotResults(
                onBack = { navController.popBackStack() },
                onImageClick = { path ->
                    coroutineScope.launch {
                        val allMedia = viewModel.fetchMedia(context)
                        val foundMedia = allMedia.find { it.uri.toString() == path }
                        foundMedia?.let { media ->
                            if (!navigationState.value) {
                                navigationState.value = true
                                navController.navigate("mediaDetail/${media.id}")
                                coroutineScope.launch {
                                    navigationState.value = false
                                }
                            }
                        }
                    }
                },
                viewModel = chatbotViewModel,
                isDarkTheme = isDarkTheme
            )
        }
        composable(
            "albumDetail",
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { it } }
        ) {
            AlbumDetailScreen(
                album = currentAlbum,
                onBack = { navController.popBackStack() },
                onMediaClick = { media ->
                    navController.navigate("mediaDetail/${media.id}")
                },
                viewModel = viewModel,
                isDarkTheme = isDarkTheme
            )
        }
        composable(
            "videoFiles",
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { it } }
        ) {
            VideoFilesScreen(
                onBack = { navController.popBackStack() },
                onMediaClick = { media ->
                    navController.navigate("mediaDetail/${media.id}")
                },
                viewModel = viewModel,
                isDarkTheme = isDarkTheme
            )
        }

        composable(
            "favoriteMedia",
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { it } }
        ) {
            FavoriteMediaScreen(
                context = context,
                onBack = { navController.popBackStack() },
                onMediaClick = { media ->
                    navController.navigate("mediaDetail/${media.id}")
                },
                viewModel = viewModel,
                isDarkTheme = isDarkTheme
            )
        }

        composable("mediaDetail/{mediaId}") { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId")?.toLongOrNull()
            val contextualMediaList = if (currentAlbum != null) {
                remember(currentAlbum) {
                    runBlocking { viewModel.fetchMediaAlbum(context, currentAlbum!!.id) }
                }
            } else {
                allMediaState.value
            }
            val currentIndex =
                contextualMediaList.indexOfFirst { it.id == mediaId }.takeIf { it >= 0 } ?: 0
            MediaDetailScreen(
                isDarkTheme = isDarkTheme,
                mediaList = contextualMediaList,
                initialMediaPosition = currentIndex,
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable(
            "largeSizeMedia",
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { it } }
        ) {
            LargeSizeMediaScreen(
                context = context,
                onBack = { navController.popBackStack() },
                onMediaClick = { media ->
                    navController.navigate("mediaDetail/${media.id}")
                },
                viewModel = viewModel,
                isDarkTheme = isDarkTheme
            )
        }

        composable("settings") {
            SettingsScreen(
                isDarkTheme = isDarkTheme,
                onThemeChange = { isDark ->
                    themeViewModel.toggleTheme(isDark)
                },
                onBackPressed = {
                    navController.navigateUp()
                },
                onAboutClick = {
                    navController.navigate("about")
                },
                onContactClick = {
                    navController.navigate("contact")
                }
            )
        }

        composable("about") {
            AboutScreen(
                isDarkTheme = isDarkTheme,
                onBackPressed = {
                    navController.navigateUp()
                }
            )
        }
        composable("contact") {
            ContactScreen(
                isDarkTheme = isDarkTheme,
                onBackPressed = {
                    navController.navigateUp()
                }
            )
        }
    }
}