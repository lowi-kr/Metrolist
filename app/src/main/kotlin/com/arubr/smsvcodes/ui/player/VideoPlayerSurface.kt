package com.arubr.smsvcodes.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.arubr.smsvcodes.LocalPlayerConnection
import com.arubr.smsvcodes.R

/**
 * Embedded video surface for music-video playback.
 *
 * Shows the ExoPlayer video output in a 16:9 box. A fullscreen button in the
 * bottom-right corner forces landscape and hides the system bars; tapping it
 * again restores portrait and the bars.
 *
 * Drop-in replacement for the album-art [AsyncImage] inside [BottomSheetPlayer]
 * when [isVideoTrack] is true and the user has enabled video playback in settings.
 *
 * @param modifier Applied to the outermost [Box].
 */
@Composable
fun VideoPlayerSurface(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player

    var isLandscapeForced by remember { mutableStateOf(false) }

    // Always restore orientation when this composable is removed from composition.
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
                isLandscapeForced = false
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
        // ── Video surface ──────────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false                           // app draws its own transport controls
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { view ->
                // Re-attach in case service was restarted and player instance changed.
                if (view.player !== player) view.player = player
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Fullscreen toggle ──────────────────────────────────────────────────
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
                painter = painterResource(
                    if (isLandscapeForced) R.drawable.fullscreen_exit else R.drawable.fullscreen,
                ),
                contentDescription = if (isLandscapeForced) "Exit fullscreen" else "Fullscreen",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
