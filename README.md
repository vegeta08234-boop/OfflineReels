# Offline Reels

An Android app (Kotlin + Jetpack Compose + Media3 ExoPlayer) that plays the videos
stored on your phone in a full-screen, swipe-up reel feed. No internet needed.

## Features
- Swipe vertically to move between videos (newest first)
- Pick which folder to watch (📁 button), remembered between launches
- Like videos (double-tap), favourites-only filter, shuffle, mute, draggable seek bar
- Tap to pause/resume, with a progress bar along the bottom
- Videos loop; playback pauses when the app is backgrounded
- One shared player, so only the visible video is decoded

## Build and run
1. Open this folder in Android Studio (Koala or newer). Let it sync; it will
   generate the Gradle wrapper and download dependencies.
2. Run on a device or emulator with some videos on it (API 24+).
3. Allow video access when prompted.

## Files
- `MainActivity.kt`: permission flow and screen states
- `VideoRepository.kt`: reads videos from MediaStore
- `ReelsScreen.kt`: VerticalPager and the ExoPlayer reel UI

## Ideas for next steps
Mute toggle, favourites, picking a specific folder, caching thumbnails.
