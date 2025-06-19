package com.sslythrrr.galeri.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sslythrrr.galeri.ui.theme.BlueAccent
import com.sslythrrr.galeri.ui.theme.GoldAccent
import com.sslythrrr.galeri.ui.theme.SearchBarBackground
import com.sslythrrr.galeri.ui.theme.SurfaceDark
import com.sslythrrr.galeri.ui.theme.SurfaceLight
import com.sslythrrr.galeri.ui.theme.TextBlack
import com.sslythrrr.galeri.ui.theme.TextGray
import com.sslythrrr.galeri.ui.theme.TextGrayDark
import com.sslythrrr.galeri.ui.theme.TextWhite
import kotlinx.coroutines.delay

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onAIAssistantClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    isSearching: Boolean = false
) {
    val height by animateDpAsState(
        targetValue = if (isActive) 52.dp else 52.dp,
        animationSpec = tween(300),
        label = "searchBarHeight"
    )
    var internalQuery by remember { mutableStateOf(query) }
    LaunchedEffect(query) {
        internalQuery = query
    }
    LaunchedEffect(internalQuery) {
        if (internalQuery != query) {
            delay(300) // Debounce delay
            onQueryChange(internalQuery)
        }
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(50.dp))
            .background(if (isDarkTheme) SearchBarBackground else SurfaceLight)
            .clickable {
                if (!isActive) {
                    onActiveChange(true)
                    focusRequester.requestFocus()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = if (isDarkTheme) TextGray else TextGrayDark,
                modifier = Modifier.size(22.dp)
            )

            TextField(
                value = internalQuery,
                onValueChange = { newValue ->
                    internalQuery = newValue
                },
                placeholder = {
                    Text(
                        "Cari media...",
                        color = if (isDarkTheme) TextGray else TextGrayDark,
                        fontSize = 14.sp,
                        letterSpacing = 0.3.sp
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
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .focusRequester(focusRequester)
            )
            if (internalQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Outlined.Clear,
                    contentDescription = "Clear search",
                    tint = if (isDarkTheme) TextGray else TextGrayDark,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable {
                            internalQuery = ""
                            onQueryChange("")
                        }
                )
            }

            // Show loading indicator during search
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(22.dp)
                        .padding(end = 8.dp),
                    color = if (isDarkTheme) GoldAccent else BlueAccent,
                    strokeWidth = 2.dp
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDarkTheme) SurfaceLight.copy(alpha = 0.9f) else SurfaceDark.copy(
                            alpha = 0.8f
                        )
                    )
                    .clickable { onAIAssistantClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = "AI Assistant",
                    tint = if (isDarkTheme) TextBlack else TextWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}