package com.sslythrrr.galeri.navigation
//v
import android.content.Intent
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
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sslythrrr.galeri.ui.media.Media
import com.sslythrrr.galeri.ui.screens.AiAlbumDetailScreen
import com.sslythrrr.galeri.ui.screens.AlbumDetailScreen
import com.sslythrrr.galeri.ui.screens.ChatbotResults
import com.sslythrrr.galeri.ui.screens.MainScreen
import com.sslythrrr.galeri.ui.screens.MediaDetailScreen
import com.sslythrrr.galeri.ui.screens.SplashScreen
import com.sslythrrr.galeri.ui.screens.management.FavoriteMediaScreen
import com.sslythrrr.galeri.ui.screens.management.LargeSizeMediaScreen
import com.sslythrrr.galeri.ui.screens.management.TrashScreen
import com.sslythrrr.galeri.ui.screens.management.VideoFilesScreen
import com.sslythrrr.galeri.ui.screens.settings.AboutScreen
import com.sslythrrr.galeri.ui.screens.settings.ContactScreen
import com.sslythrrr.galeri.ui.screens.settings.SettingsScreen
import com.sslythrrr.galeri.viewmodel.ChatbotViewModel
import com.sslythrrr.galeri.viewmodel.MediaViewModel
import com.sslythrrr.galeri.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavType // <-- TAMBAHKAN IMPORT INI
import com.sslythrrr.galeri.data.entity.ScannedImage
import com.sslythrrr.galeri.ui.media.Album
import com.sslythrrr.galeri.ui.media.MediaType
import com.sslythrrr.galeri.ui.screens.management.CollectionsScreen
import com.sslythrrr.galeri.ui.screens.management.DuplicateMediaScreen
import com.sslythrrr.galeri.ui.screens.management.SearchHistoryScreen

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
        composable(
            route = "main?page={pageIndex}",
            arguments = listOf(navArgument("pageIndex") {
                type = NavType.IntType
                defaultValue = 1
            })
        ) { backStackEntry ->

            val initialPage = backStackEntry.arguments?.getInt("pageIndex") ?: 1

            var isMainScreenLoading by remember { mutableStateOf(false) }

            MainScreen(
                initialPage = initialPage, // <--- INI SATU-SATUNYA TAMBAHAN DI SINI
                context = context,
                onAlbumClick = { album ->
                    if (!navigationState.value) {
                        navigationState.value = true
                        viewModel.setCurrentAlbum(album, context)
                        navController.navigate("albumDetail/${album.name}")
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
                onTrashClick = {
                    navController.navigate("trash")
                },
                onImageClick = { path ->
                    val imageListInBubble = chatbotViewModel.messages.value
                        .lastOrNull { it.images.isNotEmpty() }?.images ?: emptyList()

                    if (imageListInBubble.isNotEmpty()) {
                        val fullMediaListForPager = imageListInBubble.map { it.toMedia() }
                        viewModel.setMediaPagerFromList(fullMediaListForPager)
                        val clickedMedia = fullMediaListForPager.find { it.uri.toString() == path }
                        clickedMedia?.let {
                            navController.navigate("mediaDetail/${it.id}")
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
        composable("filteredImages") {
            ChatbotResults(
                onBack = { navController.popBackStack() },
                onImageClick = { media ->
                    val allFilteredScannedImages = chatbotViewModel.allFilteredImages.value
                    val fullMediaList = allFilteredScannedImages.map { it.toMedia() }

                    viewModel.setMediaPagerFromList(fullMediaList)
                    navController.navigate("mediaDetail/${media.id}")
                },
                chatbotViewModel = chatbotViewModel,
                mediaViewModel = viewModel,
                isDarkTheme = isDarkTheme
            )
        }
        composable(
            route = "albumDetail/{albumName}?isCollection={isCollection}",
            arguments = listOf(
                navArgument("albumName") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("isCollection") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            ),
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { it } }
        ) { backStackEntry ->
            val albumName = backStackEntry.arguments?.getString("albumName")
            val isCollection = backStackEntry.arguments?.getBoolean("isCollection") == true
            LaunchedEffect(albumName, isCollection) {
                if (isCollection) {
                    albumName?.let { name ->
                        val collectionAsAlbum = Album(
                            id = name.hashCode().toLong(),
                            name = name,
                            uri = android.net.Uri.EMPTY,
                            mediaCount = 0,
                            latestMediaDate = 0
                        )
                        viewModel.setCurrentAlbum(collectionAsAlbum, context, shouldLoadMedia = false)
                        viewModel.loadMediaForCollection(context, name)
                    }
                } else {
                    val albumToShow = viewModel.albums.value.find { it.name == albumName }
                    viewModel.setCurrentAlbum(albumToShow, context)
                }
            }
            AlbumDetailScreen(
                album = currentAlbum,
                onBack = { navController.popBackStack() },
                onMediaClick = { media ->
                    navController.navigate("mediaDetail/${media.id}")
                },
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                isCollection = isCollection
            )
        }
        composable(
            "searchHistory", // Rute baru kita
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { it } }
        ) {
            SearchHistoryScreen(
                onBackPressed = { navController.popBackStack() },
                onHistoryClick = { query ->
                    chatbotViewModel.resendMessage(query)
                    navController.navigate("main?page=0") {
                        popUpTo("main?page={pageIndex}") { inclusive = true }
                    }
                },
                isDarkTheme = isDarkTheme
            )
        }
        composable("collections") {
            CollectionsScreen(navController = navController, viewModel = viewModel)
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
            if (mediaId != null) {
                MediaDetailScreen(
                    initialMediaId = mediaId,
                    onBack = { navController.popBackStack() },
                    onShare = { media ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (media.type == MediaType.IMAGE) "image/*" else "video/*"
                            putExtra(Intent.EXTRA_STREAM, media.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan ${media.title}"))
                    },
                    isDarkTheme = isDarkTheme,
                    viewModel = viewModel
                )
            } else {
                // Kembali jika mediaId tidak valid
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
        composable(
            "aiAlbumDetail/{albumName}",
            arguments = listOf(navArgument("albumName") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { it } }
        ) { backStackEntry ->
            val albumName = backStackEntry.arguments?.getString("albumName") ?: ""
            AiAlbumDetailScreen(
                albumName = albumName,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onMediaClick = { media ->
                    navController.navigate("mediaDetail/${media.id}")
                },
                isDarkTheme = isDarkTheme
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

        composable(
            "duplicateMedia", // Nama rute baru kita
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { it } }
        ) {
            DuplicateMediaScreen(
                context = context,
                onBack = { navController.popBackStack() },
                onMediaClick = { media -> // Media object sudah benar
                    val fullList = viewModel.duplicateMedia.value
                    viewModel.setMediaPagerFromList(fullList)
                    navController.navigate("mediaDetail/${media.id}")
                },
                viewModel = viewModel,
                isDarkTheme = isDarkTheme
            )
        }
        composable("trash") {
            TrashScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
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
private fun ScannedImage.toMedia(): Media {
    return Media(
        id = this.uri.hashCode().toLong(),
        title = this.nama,
        uri = this.uri.toUri(),
        type = if (this.type.startsWith("video")) MediaType.VIDEO else MediaType.IMAGE,
        albumId = this.album.hashCode().toLong(),
        albumName = this.album,
        dateTaken = this.tanggal,
        dateAdded = this.tanggal,
        size = this.ukuran,
        relativePath = this.path,
        thumbnailPath = this.thumbnailPath,
        isFavorite = this.isFavorite,
        fileHash = this.fileHash,
        width = this.resolusi.substringBefore("x").toIntOrNull() ?: 0,
        height = this.resolusi.substringAfter("x").toIntOrNull() ?: 0
    )
}