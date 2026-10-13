package com.example.offlinereels

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class FolderItem(val key: String, val name: String, val count: Int)

/**
 * Full-screen list of the folders that contain videos.
 * [onCancel] is null on first launch, when a choice is required.
 */
@Composable
fun FolderPickerScreen(
    videos: List<LocalVideo>,
    currentKey: String?,
    onPick: (String) -> Unit,
    onCancel: (() -> Unit)?,
) {
    val folders = remember(videos) {
        videos
            .groupBy { it.bucketId }
            .map { (id, list) -> FolderItem(id.toString(), list.first().bucketName, list.size) }
            .sortedBy { it.name.lowercase() }
    }

    BackHandler(enabled = onCancel != null) { onCancel?.invoke() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Choose a folder",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
            )
            if (onCancel != null) {
                TextButton(onClick = onCancel) { Text("Back") }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                FolderRow(
                    name = "All videos",
                    count = videos.size,
                    selected = currentKey == "all",
                    onClick = { onPick("all") },
                )
            }
            items(folders, key = { it.key }) { folder ->
                FolderRow(
                    name = folder.name,
                    count = folder.count,
                    selected = currentKey == folder.key,
                    onClick = { onPick(folder.key) },
                )
            }
        }
    }
}

@Composable
private fun FolderRow(name: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "📁", fontSize = 26.sp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(text = name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (count == 1) "1 video" else "$count videos",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (selected) {
            Text(text = "✓", color = Color.White, fontSize = 20.sp)
        }
    }
}
