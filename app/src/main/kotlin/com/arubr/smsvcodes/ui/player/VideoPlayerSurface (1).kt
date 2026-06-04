/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.arubr.smsvcodes.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.arubr.smsvcodes.LocalPlayerConnection
import com.arubr.smsvcodes.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Renders the video stream for an OMV music video using a dedicated UI-layer [ExoPlayer].
 *
 * Why a separate player instead of the service player?
 * The service [ExoPlayer] is audio-only — it uses a custom renderers factory that only
 * overrides [buildAudioSink], and it runs in a [MediaSessionService] with no attached
 * [Surface]. Trying to inject video into that pipeline causes timing races and silent
 * failures. Instead, this composable owns a lightweight ExoPlayer whose sole job is to
 * decode and render the video track; audio continues playing from the service player.
 *
 * Sync strategy:
 *   • When a video URL arrives, the video player seeks to the service player's current
 *     position and starts playing (or stays paused, matching the service player's state).
 *   • A coroutine polls the service player's position every 500 ms and corrects drift
 *     larger than 800 ms, keeping video and audio in sync without overloading the thread.
 *   • On track change (videoUrl becomes null or changes), the video player is released
 *     and recreated for the next OMV track.
 */
@Composable
fun VideoPlayerSurface(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val videoUrl by playerConnection.videoUrl.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val playbackState by playerConnection.playbackState.collectAsStateWithLifecycle()

    var isLandscapeForced by remember { mutableStateOf(false) }

    // The dedicated video ExoPlayer. Recreated whenever the video URL changes.
    val videoPlayer = remember(videoUrl) {
        val url = videoUrl ?: return@remember null
        ExoPlayer.Builder(context).build().apply {
            val item = MediaItem.fromUri(url)
            setMediaItem(item)
            // Mute — audio comes from the service player
            volume = 0f
            prepare()
        }
    }

    // Sync: seek video player to service player position on URL load, then correct drift.
    LaunchedEffect(videoPlayer, isPlaying) {
        val vp = videoPlayer ?: return@LaunchedEffect
        val servicePlayer = playerConnection.player

        // Initial seek to match service player position
        val initialPos = servicePlayer.currentPosition
        if (initialPos > 0) vp.seekTo(initialPos)

        // Set play/pause to match service player
        vp.playWhenReady = isPlaying

        // Drift correction loop — runs while this effect is active
        while (isActive) {
            delay(500)
            val servicePos = servicePlayer.currentPosition
            val videoPos = vp.currentPosition
            val drift = kotlin.math.abs(servicePos - videoPos)
            if (drift > 800) {
                vp.seekTo(servicePos)
            }
            vp.playWhenReady = servicePlayer.playWhenReady &&
                servicePlayer.playbackState != Player.STATE_ENDED
        }
    }

    // Release video player when URL changes or composable leaves.
    DisposableEffect(videoPlayer) {
        onDispose {
            videoPlayer?.release()
        }
    }

    // Restore orientation when composable leaves composition.
    DisposableEffect(Unit) {
        onDispose {
            if (isLandscapeForced) {
                (context as? Activity)?.let { activity ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    val window = activity.window
                    val ctl = WindowCompat.getInsetsController(window, window.decorView)
                    ctl.show(WindowInsetsCompat.Type.statusBars())
                    ctl.show(WindowInsetsCompat.Type.navigationBars())
                }
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black),
    ) {
        // ── Video surface ─────────────────────────────────────────────────────
        if (videoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        videoPlayer.setVideoSurfaceView(this)
                    }
                },
                update = { view ->
                    videoPlayer.setVideoSurfaceView(view)
                },
                onRelease = {
                    videoPlayer.clearVideoSurface()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── Fullscreen toggle ─────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    val activity = context as? Activity ?: return@clickable
                    val window = activity.window
                    val ctl = WindowCompat.getInsetsController(window, window.decorView)

                    if (isLandscapeForced) {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        ctl.show(WindowInsetsCompat.Type.statusBars())
                        ctl.show(WindowInsetsCompat.Type.navigationBars())
                        isLandscapeForced = false
                    } else {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        ctl.hide(WindowInsetsCompat.Type.statusBars())
                        ctl.hide(WindowInsetsCompat.Type.navigationBars())
                        ctl.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        isLandscapeForced = true
                    }
                },
        ) {
            Icon(
                painter = painterResource(R.drawable.fullscreen),
                contentDescription = if (isLandscapeForced) "Exit fullscreen" else "Fullscreen",
                tint = if (isLandscapeForced) Color.Yellow else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
