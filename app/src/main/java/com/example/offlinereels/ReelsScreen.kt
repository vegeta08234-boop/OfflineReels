package com.example.offlinereels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private val HeartRed = Color(0xFFFF3B5C)

@Composable
fun ReelsScreen(videos: List<LocalVideo>) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    var favorites by remember { mutableStateOf(prefs.favorites) }
    var muted by remember { mutableStateOf(prefs.muted) }
    var shuffle by remember { mutableStateOf(prefs.shuffle) }
    var favoritesOnly by remember { mutableStateOf(false) }

    // The list shown in the feed. It only changes when a toggle changes, so
    // un-liking a video while in favourites mode doesn't make it vanish mid-swipe.
    val shown = remember(videos, shuffle, favoritesOnly) {
        val base = if (favoritesOnly) videos.filter { it.id in favorites } else videos
        if (shuffle) base.shuffled() else base
    }

    // One shared player: only the visible page ever decodes video.
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    LaunchedEffect(shown.isEmpty()) { if (shown.isEmpty()) player.pause() }

    // Pause when the app goes to the background, resume on return.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { if (shown.isNotEmpty()) player.play() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (shown.isEmpty()) {
            Text(
                text = "No favourites yet.\nDouble-tap a video to like it.",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
            )
        } else {
            key(shown) {
                ReelsPager(
                    videos = shown,
                    player = player,
                    initialPage = shown.indexOfFirst { it.id == prefs.lastVideoId }
                        .coerceAtLeast(0),
                    favorites = favorites,
                    muted = muted,
                    onPageSettled = { prefs.lastVideoId = it.id },
                    onToggleFavorite = { video ->
                        favorites = if (video.id in favorites) favorites - video.id
                        else favorites + video.id
                        prefs.favorites = favorites
                    },
                    onToggleMute = {
                        muted = !muted
                        prefs.muted = muted
                    },
                )
            }
        }

        // Top-right controls: favourites-only filter and shuffle.
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoundButton(active = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Show favourites only",
                    tint = if (favoritesOnly) HeartRed else Color.White,
                )
            }
            RoundButton(
                active = shuffle,
                onClick = {
                    shuffle = !shuffle
                    prefs.shuffle = shuffle
                },
            ) {
                Text(text = "🔀", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun ReelsPager(
    videos: List<LocalVideo>,
    player: ExoPlayer,
    initialPage: Int,
    favorites: Set<Long>,
    muted: Boolean,
    onPageSettled: (LocalVideo) -> Unit,
    onToggleFavorite: (LocalVideo) -> Unit,
    onToggleMute: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { videos.size })

    // Load the video for whichever page the pager has settled on.
    LaunchedEffect(pagerState.settledPage) {
        val video = videos.getOrNull(pagerState.settledPage) ?: return@LaunchedEffect
        player.setMediaItem(MediaItem.fromUri(video.uri))
        player.prepare()
        player.play()
        onPageSettled(video)
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) { page ->
        val video = videos[page]
        ReelPage(
            video = video,
            player = player,
            isCurrent = page == pagerState.settledPage,
            liked = video.id in favorites,
            muted = muted,
            onToggleLike = { onToggleFavorite(video) },
            onToggleMute = onToggleMute,
        )
    }
}

@Composable
private fun ReelPage(
    video: LocalVideo,
    player: ExoPlayer,
    isCurrent: Boolean,
    liked: Boolean,
    muted: Boolean,
    onToggleLike: () -> Unit,
    onToggleMute: () -> Unit,
) {
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var seeking by remember { mutableStateOf(false) }
    var heartTick by remember { mutableIntStateOf(0) }
    var showHeart by remember { mutableStateOf(false) }

    // Flash a big heart for a moment after a double-tap.
    LaunchedEffect(heartTick) {
        if (heartTick > 0) {
            showHeart = true
            delay(700)
            showHeart = false
        }
    }

    // Track play/pause state and playback progress for the visible page.
    if (isCurrent) {
        DisposableEffect(player) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            }
            player.addListener(listener)
            isPlaying = player.isPlaying
            onDispose { player.removeListener(listener) }
        }
        LaunchedEffect(player) {
            while (true) {
                if (!seeking) {
                    val duration = player.duration
                    progress = if (duration > 0) {
                        (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                    } else 0f
                }
                delay(200)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isCurrent, liked) {
                detectTapGestures(
                    onTap = {
                        if (isCurrent) {
                            if (player.isPlaying) player.pause() else player.play()
                        }
                    },
                    onDoubleTap = {
                        if (isCurrent) {
                            if (!liked) onToggleLike()
                            heartTick++
                        }
                    },
                )
            },
    ) {
        if (isCurrent) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { it.player = player },
                onRelease = { it.player = null },
            )

            if (showHeart) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = HeartRed,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(110.dp),
                )
            } else if (!isPlaying) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(88.dp)
                        .alpha(0.8f),
                )
            }
        }

        // Right-side buttons: like and mute.
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RoundButton(onClick = onToggleLike) {
                Icon(
                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (liked) "Unlike" else "Like",
                    tint = if (liked) HeartRed else Color.White,
                )
            }
            RoundButton(onClick = onToggleMute) {
                Text(
                    text = if (muted) "🔇" else "🔊",
                    fontSize = 20.sp,
                )
            }
        }

        // Bottom: file name, and a draggable progress bar on the current video.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Text(
                text = video.name,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                modifier = Modifier.padding(start = 16.dp, end = 72.dp, bottom = 4.dp),
            )
            if (isCurrent) {
                Slider(
                    value = progress,
                    onValueChange = {
                        seeking = true
                        progress = it
                    },
                    onValueChangeFinished = {
                        val duration = player.duration
                        if (duration > 0) player.seekTo((progress * duration).toLong())
                        seeking = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            } else {
                Box(modifier = Modifier.size(width = 1.dp, height = 24.dp))
            }
        }
    }
}

@Composable
private fun RoundButton(
    onClick: () -> Unit,
    active: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                if (active) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.4f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
