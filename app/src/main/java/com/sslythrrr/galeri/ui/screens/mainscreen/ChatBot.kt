package com.sslythrrr.galeri.ui.screens.mainscreen

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sslythrrr.galeri.ui.theme.BlueAccent
import com.sslythrrr.galeri.ui.theme.CardDark
import com.sslythrrr.galeri.ui.theme.DarkBackground
import com.sslythrrr.galeri.ui.theme.GoldAccent
import com.sslythrrr.galeri.ui.theme.LightBackground
import com.sslythrrr.galeri.ui.theme.SearchBarBackground
import com.sslythrrr.galeri.ui.theme.SurfaceDark
import com.sslythrrr.galeri.ui.theme.TextBlack
import com.sslythrrr.galeri.ui.theme.TextGray
import com.sslythrrr.galeri.ui.theme.TextGrayDark
import com.sslythrrr.galeri.ui.theme.TextLightGray
import com.sslythrrr.galeri.ui.theme.TextWhite
import com.sslythrrr.galeri.viewmodel.ChatMessage
import com.sslythrrr.galeri.viewmodel.ChatbotViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.Button
import androidx.compose.foundation.lazy.LazyRow
import com.sslythrrr.galeri.ui.media.MediaItem
import com.sslythrrr.galeri.viewmodel.MediaViewModel

@Suppress("DEPRECATION")
@Composable
fun Chatbot(
    query: String,
    onQueryChange: (String) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    chatbotViewModel: ChatbotViewModel = viewModel(),
    onImageClick: (String) -> Unit = {},        // Tambahkan ini
    onShowAllImages: () -> Unit = {}
) {
    val context = LocalContext.current
    val window = remember { (context as? Activity)?.window }
    val messages by chatbotViewModel.messages.collectAsState()
    val isLoading by chatbotViewModel.isLoading.collectAsState()


    LaunchedEffect(Unit) {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        chatbotViewModel.initializeProcessors(context)
    }

    Box(
        modifier
            .fillMaxSize()
            .background(if (isDarkTheme) DarkBackground else LightBackground)
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatContent(
                messages = messages,
                isLoading = isLoading,
                isDarkTheme = isDarkTheme,
                onImageClick = onImageClick,
                onShowAllImages = onShowAllImages,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
        ) {
            BottomBar(
                query = query,
                onQueryChange = onQueryChange,
                onSendMessage = { message ->
                    chatbotViewModel.sendMessage(message)
                    onQueryChange("")
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ChatContent(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit = {}, // Tambah parameter
    onShowAllImages: () -> Unit = {} // Tambah parameter
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Box(modifier = modifier.padding(bottom = 64.dp)) {
        if (messages.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Mulai interaksi",
                    color = if (isDarkTheme) TextLightGray else TextGrayDark,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Ketik untuk memulai pencarian",
                    color = if (isDarkTheme) TextGray else TextGrayDark,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(messages) { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    ChatBubble(
                        message = message,
                        isDarkTheme = isDarkTheme,
                        onImageClick = onImageClick, // Pass callback
                        onShowAllImages = onShowAllImages // Pass callback
                    )
                }

                if (isLoading) {
                    item {
                        LoadingBubble(isDarkTheme = isDarkTheme)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isDarkTheme: Boolean,
    onImageClick: (String) -> Unit = {},
    onShowAllImages: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .let {
                        if (message.isUser) it.widthIn(max = 280.dp)
                        else it.fillMaxWidth()
                    }
                    .background(
                        color = if (message.isUser) {
                            if (isDarkTheme) GoldAccent else BlueAccent
                        } else {
                            if (isDarkTheme) CardDark else Color.LightGray.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (message.isUser) 16.dp else 4.dp,
                            bottomEnd = if (message.isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = if (message.isUser) Color.White else if (isDarkTheme) TextWhite else TextBlack,
                        fontSize = 14.sp
                    )

                    if (!message.isUser && message.images.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Kita butuh akses ke MediaViewModel untuk menggunakan fungsi konverter .toMedia()
                        val mediaViewModel: MediaViewModel = viewModel()

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Loop List<ScannedImage>
                            items(message.images, key = { it.uri }) { scannedImage ->
                                // 1. Ubah ScannedImage menjadi objek Media yang lengkap
                                val media = mediaViewModel.run { scannedImage.toMedia() }

                                // 2. Gunakan komponen MediaItem yang sudah terbukti bekerja
                                MediaItem(
                                    media = media,
                                    onClick = { onImageClick(media.uri.toString()) },
                                    isDarkTheme = isDarkTheme,
                                    modifier = Modifier.size(120.dp) // Sesuaikan ukuran di sini
                                )
                            }
                        }

                        if (message.showAllImagesButton) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onShowAllImages,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Lihat Semua")
                            }
                        }
                    }
                }
            }

            Text(
                text = formatTimestamp(message.timestamp),
                color = if (isDarkTheme) TextGray else TextGrayDark,
                fontSize = 11.sp,
                modifier = Modifier.padding(
                    start = if (message.isUser) 0.dp else 8.dp,
                    end = if (message.isUser) 8.dp else 0.dp,
                    top = 2.dp
                )
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun LoadingBubble(isDarkTheme: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isDarkTheme) SurfaceDark else Color.LightGray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = if (isDarkTheme) GoldAccent else BlueAccent,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Sedang memproses...",
                    color = if (isDarkTheme) TextLightGray else TextGrayDark,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            //.background((if (isDarkTheme) SurfaceDark else SurfaceLight).copy(alpha = 0.95f))
            .padding(bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        color = if (isDarkTheme)
                            TextGray.copy(alpha = 0.3f)
                        else
                            TextGrayDark.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .background(if (isDarkTheme) SearchBarBackground else TextWhite)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = if (isDarkTheme) TextGray else TextGrayDark,
                    modifier = Modifier.size(22.dp)
                )

                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            "Ketik disini...",
                            color = if (isDarkTheme) TextGray else TextGrayDark,
                            fontSize = 14.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = if (isDarkTheme) GoldAccent else BlueAccent,
                        unfocusedTextColor = if (isDarkTheme) TextWhite else TextBlack,
                        focusedTextColor = if (isDarkTheme) TextWhite else TextBlack
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (query.isNotBlank()) {
                                onSendMessage(query)
                            }
                        }
                    )
                )

                if (query.isNotBlank()) {
                    IconButton(
                        onClick = { onSendMessage(query) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (isDarkTheme) GoldAccent else BlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}