package com.sslythrrr.voe.ui.screens.kelola

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sslythrrr.voe.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(navController: NavController, viewModel: MediaViewModel) {
    val collections by viewModel.collections.collectAsState()
    val context = LocalContext.current

    var collectionToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadCollections(context)
    }

    if (collectionToDelete != null) {
        AlertDialog(
            onDismissRequest = { collectionToDelete = null },
            title = { Text("Hapus Koleksi") },
            text = { Text("Yakin ingin menghapus koleksi '${collectionToDelete}'? Foto di dalamnya tidak akan ikut terhapus.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCollection(context, collectionToDelete!!)
                        collectionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { collectionToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Koleksi Album") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        if (collections.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum punya koleksi")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = padding,
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(collections) { collectionName ->
                    CollectionCard(
                        name = collectionName,
                        onClick = {
                            navController.navigate("albumDetail/$collectionName?isCollection=true")
                        },
                        onLongClick = {
                            collectionToDelete = collectionName
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionCard(name: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(48.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}