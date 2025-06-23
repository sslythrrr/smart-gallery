package com.sslythrrr.galeri.ui.screens
// vvc
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.sslythrrr.galeri.ui.components.BottomNavigationBar
import com.sslythrrr.galeri.ui.media.Album
import com.sslythrrr.galeri.ui.media.Media
import com.sslythrrr.galeri.ui.screens.mainscreen.Chatbot
import com.sslythrrr.galeri.ui.screens.mainscreen.GalleryScreen
import com.sslythrrr.galeri.ui.screens.mainscreen.Management
import com.sslythrrr.galeri.ui.theme.BlueAccent
import com.sslythrrr.galeri.ui.theme.DarkBackground
import com.sslythrrr.galeri.ui.theme.GoldAccent
import com.sslythrrr.galeri.ui.theme.GoldAccentDark
import com.sslythrrr.galeri.ui.theme.LightBackground
import com.sslythrrr.galeri.ui.theme.SurfaceDark
import com.sslythrrr.galeri.ui.theme.SurfaceLight
import com.sslythrrr.galeri.ui.theme.TextBlack
import com.sslythrrr.galeri.ui.theme.TextGray
import com.sslythrrr.galeri.ui.theme.TextGrayDark
import com.sslythrrr.galeri.ui.theme.TextLightGray
import com.sslythrrr.galeri.ui.theme.TextWhite
import com.sslythrrr.galeri.viewmodel.ChatbotViewModel
import com.sslythrrr.galeri.viewmodel.MediaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    context: Context,
    onMediaClick: (Media) -> Unit,
    onAlbumClick: (Album) -> Unit,
    viewModel: MediaViewModel,
    isDarkTheme: Boolean,
    chatbotViewModel: ChatbotViewModel,
    navController: NavController,
    onNavigationStateChange: (Boolean) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onShowAllImages: () -> Unit = {},
    onThemeChange: (Boolean) -> Unit,
    onAboutClick: () -> Unit,
    onContactClick: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 }, initialPage = 1)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) {
            viewModel.setCurrentAlbum(null, context)
        }
    }

    var isNavigatingBack by remember { mutableStateOf(false) }

    LaunchedEffect(isNavigatingBack) {
        onNavigationStateChange(isNavigatingBack)
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) {
            isNavigatingBack = true
            viewModel.setCurrentAlbum(null, context)
            isNavigatingBack = false
        }
    }

    var isKeyboardVisible by remember { mutableStateOf(false) }
    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    LaunchedEffect(imeInsets) {
        snapshotFlow { imeInsets.getBottom(density) }.collect { imeBottom ->
            isKeyboardVisible = imeBottom > 0
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedMedia by viewModel.selectedMedia.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var isTransitioning by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) { viewModel.clearSelection() }

    val handleMediaLongClick: (Media) -> Unit = { media ->
        if (!isSelectionMode) {
            viewModel.selectionMode(true)
        }
        viewModel.selectingMedia(media)
    }

    val handleMediaClick: (Media) -> Unit = { media ->
        if (isSelectionMode) {
            viewModel.selectingMedia(media)
        } else {
            onMediaClick(media)
        }
    }

    val shareSelectedMedia = {
        val uris = selectedMedia.map { it.uri }.toList()
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan ke"))
    }

    // Fungsi untuk konfirmasi penghapusan
    val confirmDelete = {
        // Implementasi dialog konfirmasi hapus media
        // ...
    }

    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) DarkBackground else LightBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = isSelectionMode, transitionSpec = {
                    fadeIn(animationSpec = tween(200)) + slideInVertically(
                        animationSpec = tween(200), initialOffsetY = { -it }) togetherWith fadeOut(
                        animationSpec = tween(150)
                    ) + slideOutVertically(
                        animationSpec = tween(150), targetOffsetY = { -it })
                }) { inSelectionMode ->
                if (inSelectionMode) {
                    SelectionTopBar(
                        selectedCount = selectedMedia.size,
                        onSelectAll = { viewModel.selectMedia(context) },
                        onClearSelection = { viewModel.clearSelection() },
                        onDelete = confirmDelete,
                        onShare = shareSelectedMedia,
                        isDarkTheme = isDarkTheme
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(if (isDarkTheme) SurfaceDark else SurfaceLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = topbarTitle(pagerState.currentPage),
                                color = if (isDarkTheme) TextWhite else TextBlack,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            )

                            var dropdownExpanded by remember { mutableStateOf(false) }

                            Box {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Settings",
                                    tint = if (isDarkTheme) TextLightGray else TextBlack,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { dropdownExpanded = true }
                                )

                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    if (pagerState.currentPage != 0) {
                                        DropdownToggleItem(
                                            icon = Icons.Default.DarkMode,
                                            title = "Mode Gelap",
                                            subtitle = if (isDarkTheme) "Aktif" else "Nonaktif",
                                            isChecked = isDarkTheme,
                                            onCheckedChange = {
                                                onThemeChange(it)
                                            },
                                            isDarkTheme = isDarkTheme
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .padding(horizontal = 16.dp)
                                                .background(
                                                    if (isDarkTheme) TextGray.copy(alpha = 0.2f) else TextGrayDark.copy(
                                                        alpha = 0.1f
                                                    )
                                                )
                                        )
                                        DropdownClickableItem(
                                            icon = Icons.Default.Contacts,
                                            title = "Hubungi",
                                            subtitle = "Umpan balik dan saran",
                                            onClick = {
                                                dropdownExpanded = false
                                                onContactClick()
                                            },
                                            isDarkTheme = isDarkTheme
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .padding(horizontal = 16.dp)
                                                .background(
                                                    if (isDarkTheme) TextGray.copy(alpha = 0.2f) else TextGrayDark.copy(
                                                        alpha = 0.1f
                                                    )
                                                )
                                        )
                                        DropdownClickableItem(
                                            icon = Icons.Default.Info,
                                            title = "Tentang",
                                            subtitle = "Versi, fitur, dan informasi lain",
                                            onClick = {
                                                dropdownExpanded = false
                                                onAboutClick()
                                            },
                                            isDarkTheme = isDarkTheme
                                        )
                                    }
                                    if (pagerState.currentPage == 0) {
                                        DropdownClickableItem(
                                            icon = Icons.Filled.Delete,
                                            title = "Hapus Pesan",
                                            subtitle = "Bersihkan layar chatbot ",
                                            onClick = {
                                                showDeleteConfirmation = true
                                            },
                                            isDarkTheme = isDarkTheme
                                        )
                                        if (showDeleteConfirmation) {
                                            AlertDialog(
                                                onDismissRequest = {
                                                    showDeleteConfirmation = false
                                                    dropdownExpanded = false
                                                },
                                                title = {
                                                    Text(
                                                        "Hapus",
                                                        color = if (isDarkTheme) TextWhite else TextBlack
                                                    )
                                                },
                                                text = {
                                                    Text(
                                                        "Anda yakin ingin menghapus pesan",
                                                        color = if (isDarkTheme) TextWhite else TextBlack
                                                    )
                                                },
                                                confirmButton = {
                                                    Button(
                                                        onClick = {
                                                            chatbotViewModel.clearMessages()
                                                            showDeleteConfirmation = false
                                                            dropdownExpanded = false
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color.Red
                                                        )
                                                    ) {
                                                        Text(
                                                            "Hapus",
                                                            color = TextWhite
                                                        )
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = {
                                                        showDeleteConfirmation = false
                                                        dropdownExpanded = false
                                                    }) {
                                                        Text(
                                                            "Batal",
                                                            color = if (isDarkTheme) TextGray else TextGrayDark
                                                        )
                                                    }
                                                },
                                                containerColor = if (isDarkTheme) SurfaceDark else SurfaceLight,
                                                tonalElevation = 8.dp
                                            )
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
            HorizontalPager(
                state = pagerState, modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> {
                        Chatbot(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier = Modifier.fillMaxSize(),
                            isDarkTheme = isDarkTheme,
                            chatbotViewModel = chatbotViewModel,
                            onImageClick = onImageClick,
                            onShowAllImages = onShowAllImages
                        )
                    }

                    1 -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                val mediaPager by viewModel.mediaPager.collectAsState()
                                val albumsState by viewModel.albums.collectAsState()
                                val lazyPagingItems = mediaPager?.flow?.collectAsLazyPagingItems()

                                GalleryScreen(
                                    lazyPagingItems = lazyPagingItems,
                                    albums = albumsState,
                                    onMediaClick = handleMediaClick,
                                    onLongClick = handleMediaLongClick,
                                    onAlbumClick = onAlbumClick,
                                    onSeeAllClick = {
                                        if (!isTransitioning) {
                                            isTransitioning = true
                                            coroutineScope.launch {
                                                navController.navigate("allMedia")
                                                isTransitioning = false
                                            }
                                        }
                                    },
                                    selectedMedia = selectedMedia,
                                    isDarkTheme = isDarkTheme,
                                    modifier = Modifier.fillMaxSize(),
                                    viewModel = viewModel
                                )
                            }
                            if (isNavigatingBack) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (isDarkTheme) DarkBackground.copy(alpha = 0.8f)
                                            else LightBackground.copy(alpha = 0.8f)
                                        ), contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = if (isDarkTheme) GoldAccent else BlueAccent,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "Memuat galeri...",
                                            color = if (isDarkTheme) TextLightGray else TextGrayDark,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Management(
                                navController = navController,
                                isDarkTheme = isDarkTheme,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = !isKeyboardVisible || pagerState.currentPage != 0,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()) {
                BottomNavigationBar(
                    currentPage = pagerState.currentPage,
                    isDarkTheme = isDarkTheme,
                    onPageSelected = { page ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page
                            )
                        }
                    })
            }
        }
    }
}

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    isDarkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(if (isDarkTheme) SurfaceDark else SurfaceLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$selectedCount terpilih",
                color = if (isDarkTheme) TextWhite else TextBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = "Pilih Semua",
                    tint = if (isDarkTheme) TextLightGray else TextBlack,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onSelectAll() })

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Batalkan",
                    tint = if (isDarkTheme) TextLightGray else TextBlack,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onClearSelection() })

                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Bagikan",
                    tint = if (isDarkTheme) TextLightGray else TextBlack,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onShare() })

                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDelete() })
            }
        }
    }
}

@Composable
fun DropdownToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isDarkTheme) GoldAccent else BlueAccent,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = if (isDarkTheme) TextWhite else TextBlack,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(
                    text = subtitle,
                    color = if (isDarkTheme) TextGray else TextGrayDark,
                    fontSize = 13.sp
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GoldAccent,
                checkedTrackColor = GoldAccentDark,
                uncheckedThumbColor = if (isDarkTheme) TextGray else TextGrayDark,
                uncheckedTrackColor = if (isDarkTheme) SurfaceDark else SurfaceLight
            ),
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
fun DropdownClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isDarkTheme) GoldAccent else BlueAccent,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                color = if (isDarkTheme) TextWhite else TextBlack,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = if (isDarkTheme) TextGray else TextGrayDark,
                fontSize = 13.sp
            )
        }
    }
}

private fun topbarTitle(page: Int): String = when (page) {
    0 -> "CHATBOT"
    1 -> "GALERI"
    2 -> "KELOLA"
    else -> ""
}
