/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import moe.rukamori.archivetune.constants.DeveloperFpsOverlayKey
import moe.rukamori.archivetune.utils.rememberPreference

/**
 * Real-time frame-rate overlay intended for developer use. When the
 * DeveloperFpsOverlayKey preference is on, this composable renders the
 * current frame rate. The caller positions it.
 *
 * The counter uses withFrameNanos directly so it observes the actual
 * frame cadence rather than a wall-clock estimate. The visible Text is
 * updated at most twice per second to avoid turning the counter itself
 * into a measurable load.
 */
@Composable
fun FpsOverlay(modifier: Modifier = Modifier) {
    val (enabled) = rememberPreference(DeveloperFpsOverlayKey, defaultValue = false)
    if (!enabled) return

    var fps by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var frameCount = 0
        var windowStartNanos = 0L
        while (isActive) {
            withFrameNanos { nowNanos ->
                if (windowStartNanos == 0L) {
                    windowStartNanos = nowNanos
                    frameCount = 0
                } else {
                    frameCount++
                    val elapsed = nowNanos - windowStartNanos
                    if (elapsed >= FPS_WINDOW_NANOS) {
                        val measured = (frameCount * 1_000_000_000L / elapsed).toInt()
                        if (measured != fps) fps = measured
                        frameCount = 0
                        windowStartNanos = nowNanos
                    }
                }
            }
        }
    }

    Box(
        modifier =
            modifier
                .background(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(6.dp),
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = "$fps FPS",
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private const val FPS_WINDOW_NANOS = 500_000_000L
