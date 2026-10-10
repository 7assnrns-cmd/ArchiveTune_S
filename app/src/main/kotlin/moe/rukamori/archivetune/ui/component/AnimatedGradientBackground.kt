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
 * The three circles are drawn with [BlendMode.Screen] so their colors mix
 * additively — that is what gives the HyperOS "breathing" feel rather than
 * three hard-edged discs.
 */
fun DrawScope.drawHyperOsGradient(phase: Float, dark: Boolean) {
    val colors = if (dark) HyperOsDarkColors else HyperOsLightColors
    val base = if (dark) HyperOsDarkBase else HyperOsLightBase

    // Solid base color — the Screen blend modes composit against this.
    drawRect(color = base)

    val w = size.width
    val h = size.height
    // Circle radius scales with the larger dimension so the effect is
    // consistent across phone and tablet layouts.
    val radius = maxOf(w, h) * 0.85f

    colors.forEachIndexed { index, color ->
        val offsetPhase = phase + (index * 2f * PI.toFloat() / 3f)
        // Lissajous-ish path: x and y use different multipliers so the circles
        // don't trace the same ellipse.
        val cx = w / 2f + cos(offsetPhase) * w * 0.28f
        val cy = h / 2f + sin(offsetPhase * 0.7f) * h * 0.22f
        val center = Offset(cx, cy)

        // Radial gradient with an alpha falloff so edges fade out instead of
        // ending abruptly. Alpha adjusted for light vs dark so contrast stays
        // consistent with the base.
        val alpha = if (dark) 0.85f else 0.75f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
            blendMode = BlendMode.Screen,
        )
    }
}
