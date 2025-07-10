package com.sslythrrr.galeri.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun AddToCollectionDialog(
    collections: List<String>,
    onDismiss: () -> Unit,
    onCollectionSelected: (String) -> Unit,
    onNewCollection: (String) -> Unit
) {
    var newCollectionName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tambahkan ke Koleksi", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = newCollectionName,
                    onValueChange = { newCollectionName = it },
                    label = { Text("Nama koleksi baru") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newCollectionName.isNotBlank()) {
                            onNewCollection(newCollectionName)
                        }
                    },
                    enabled = newCollectionName.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Buat & Tambahkan")
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                Text("Atau pilih yang sudah ada:")
                LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                    items(collections) { collection ->
                        Text(
                            text = collection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCollectionSelected(collection) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}