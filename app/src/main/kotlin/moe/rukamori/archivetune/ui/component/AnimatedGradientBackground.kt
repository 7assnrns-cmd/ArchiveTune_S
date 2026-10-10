/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * HyperOS-style animated background palette for the update screen.
 * Three colors orbit slowly behind the content.
 *
 * Dark  — purple / blue / pink
 * Light — pink / red / sky blue
 */
private val HyperOsDarkColors = listOf(
    Color(0xFF7C4DFF), // purple
    Color(0xFF2196F3), // blue
    Color(0xFFFF4081), // pink
)

private val HyperOsLightColors = listOf(
    Color(0xFFFF4081), // pink
    Color(0xFFFF5252), // red
    Color(0xFF03A9F4), // sky blue
)

private val HyperOsDarkBase = Color(0xFF14121F)
private val HyperOsLightBase = Color(0xFFFFF7FA)

/**
 * Draws the animated gradient. [phase] is a value in [0, 2π) that drives the
 * orbits. When animations are disabled by the caller, pass 0f for a static
 * gradient.
 *
 * The three circles use a multi-stop radial gradient with SrcOver blending
 * so they read as soft regions of color rather than hard-edged discs. An
 * earlier version used [BlendMode.Screen]; that formula reduces to near
 * white on light backgrounds and erased the colors entirely.
 */
fun DrawScope.drawHyperOsGradient(phase: Float, dark: Boolean) {
    val colors = if (dark) HyperOsDarkColors else HyperOsLightColors
    val base = if (dark) HyperOsDarkBase else HyperOsLightBase

    drawRect(color = base)

    val w = size.width
    val h = size.height
    // Blobs are larger than the screen so their fade tails wash over most
    // of the surface. Small blobs read as discs, not as a gradient.
    val radius = maxOf(w, h) * 1.15f

    colors.forEachIndexed { index, color ->
        val offsetPhase = phase + (index * 2f * PI.toFloat() / 3f)
        val cx = w / 2f + cos(offsetPhase) * w * 0.32f
        val cy = h / 2f + sin(offsetPhase * 0.65f) * h * 0.28f
        val center = Offset(cx, cy)

        // Multi-stop gradient: hold the color for the first stretch, then
        // fade. Without a flat core the shape reads as a circle outline;
        // with it, it reads as a soft region of color.
        //
        // SrcOver (the default). Screen was masking the colors on light
        // backgrounds because its formula keeps the result near white when
        // the destination is already light.
        val alpha = if (dark) 0.75f else 0.80f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to color.copy(alpha = alpha),
                    0.35f to color.copy(alpha = alpha * 0.85f),
                    0.75f to color.copy(alpha = alpha * 0.30f),
                    1.00f to Color.Transparent,
                ),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

