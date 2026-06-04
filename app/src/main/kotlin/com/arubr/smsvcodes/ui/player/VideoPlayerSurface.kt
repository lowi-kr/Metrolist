/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.arubr.smsvcodes.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arubr.smsvcodes.LocalPlayerConnection
import com.arubr.smsvcodes.R

@Composable
fun VideoPlayerSurface(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    var isLandscapeForced by remember { mutableStateOf(false) }
    var surfaceAttached by remember { mutableStateOf(false) }

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
            player.clearVideoSurface()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    player.setVideoSurfaceView(this)
                    surfaceAttached = true
                }
            },
            update = { view ->
                player.setVideoSurfaceView(view)
                surfaceAttached = true
            },
            onRelease = {
                player.clearVideoSurface()
                surfaceAttached = false
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Temporary debug overlay — remove once video is confirmed working ──
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(4.dp),
        ) {
            val videoSize = player.videoSize
            Text(
                text = "isVideoSong: ${mediaMetadata?.isVideoSong}",
                color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "videoSize: ${videoSize.width}x${videoSize.height}",
                color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "surfaceAttached: $surfaceAttached",
                color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "musicVideoType: ${mediaMetadata?.musicVideoType ?: "null"}",
                color = Color.Yellow, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            )
        }
        // ── End debug overlay ─────────────────────────────────────────────────

        // Fullscreen toggle
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