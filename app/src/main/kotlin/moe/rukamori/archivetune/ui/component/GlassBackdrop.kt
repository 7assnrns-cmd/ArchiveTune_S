package moe.rukamori.archivetune.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy

/**
 * Backdrop captured from the app's colorful background layer.
 * Null means glass is disabled and surfaces keep their normal look.
 */
val LocalGlassBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * Frosted-glass panel: blurs and saturates whatever the [backdrop] shows
 * behind this element, then lays a translucent [tint] on top.
 * Returns the receiver unchanged when [backdrop] is null.
 */
fun Modifier.glassPanel(
    backdrop: Backdrop?,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.White.copy(alpha = 0.22f),
    blurRadius: Dp = 12.dp,
): Modifier =
    if (backdrop == null) {
        this
    } else {
        this.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurRadius.toPx())
            },
            onDrawSurface = { drawRect(tint) },
        )
    }
