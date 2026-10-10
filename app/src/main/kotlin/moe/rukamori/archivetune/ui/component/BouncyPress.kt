package moe.rukamori.archivetune.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Animates a scale factor that shrinks while the given [interactionSource]
 * is pressed and springs back with a small overshoot on release.
 */
@Composable
fun rememberBouncyPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.95f,
): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "bouncyPressScale",
    )
}

/** Applies the scale from [rememberBouncyPressScale] without recomposing. */
fun Modifier.bouncyPress(scale: State<Float>): Modifier =
    graphicsLayer {
        val s = scale.value
        scaleX = s
        scaleY = s
    }
