package com.example.offlinereels

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                App()
            }
        }
    }
}

private val videoPermission: String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private sealed interface LoadState {
    data object Loading : LoadState
    data class Ready(val videos: List<LocalVideo>) : LoadState
}

@Composable
private fun App() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, videoPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        granted = result
        asked = true
    }

    if (!granted) {
        CenteredMessage(
            text = if (asked)
                "Offline Reels needs access to your videos. Grant permission in system settings or try again."
            else
                "Offline Reels plays the videos stored on your phone in a swipeable reel feed.",
            buttonLabel = "Allow video access",
            onClick = { launcher.launch(videoPermission) },
        )
        return
    }

    var state by remember { mutableStateOf<LoadState>(LoadState.Loading) }
    LaunchedEffect(Unit) {
        state = LoadState.Ready(VideoRepository.loadVideos(context))
    }

    when (val s = state) {
        LoadState.Loading -> Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { CircularProgressIndicator() }

        is LoadState.Ready ->
            if (s.videos.isEmpty()) {
                CenteredMessage(text = "No videos found on this device.")
            } else {
                ReelsScreen(videos = s.videos)
            }
    }
}

@Composable
private fun CenteredMessage(
    text: String,
    buttonLabel: String? = null,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = text, color = Color.White, textAlign = TextAlign.Center)
        if (buttonLabel != null) {
            Button(onClick = onClick, modifier = Modifier.padding(top = 24.dp)) {
                Text(buttonLabel)
            }
        }
    }
}
