package com.v2ray.ang.ui.compose

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ripple
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.v2ray.ang.ui.main.glass

/** LiquidToggle catalog's sampled track, press deformation and refracting thumb.
 * Native toggleable/draggable supply one accessible action and respect scrolling cancellation.
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    interactionSource: MutableInteractionSource? = null,
    @Suppress("UNUSED_PARAMETER") colors: SwitchColors = SwitchDefaults.colors(),
) {
    val checkedState by rememberUpdatedState(checked)
    val onChange by rememberUpdatedState(onCheckedChange)
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    val selectedFraction by animateFloatAsState(if (checked) 1f else 0f, tween(140), label = "switch position")
    val scale by animateFloatAsState(if (pressed || dragging) 1.18f else 1f, tween(100), label = "switch press")
    val fraction = if (dragging) dragFraction else selectedFraction
    val pressProgress = ((scale - 1f) / 0.18f).coerceIn(0f, 1f)
    val density = LocalDensity.current
    val travel = with(density) { 24.dp.toPx() }
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val track = rememberLayerBackdrop()
    val backdrop = LocalGlassBackdrop.current
    val accent = Color(0xFF287BFA)
    val dark = LocalDarkTheme.current
    val shape = RoundedCornerShape(50)
    val interactive = enabled && onCheckedChange != null
    Box(modifier.then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier).size(64.dp, 48.dp).alpha(if (enabled) 1f else 0.45f)
        .then(if (onCheckedChange != null) Modifier.clip(shape).toggleable(checked, interaction, indication = ripple(bounded = true), enabled = enabled, role = Role.Switch) { onChange?.invoke(it) } else Modifier)
        .draggable(rememberDraggableState { delta ->
            dragFraction = (dragFraction + delta / travel * if (ltr) 1f else -1f).coerceIn(0f, 1f)
        }, Orientation.Horizontal, enabled = interactive,
            onDragStarted = { dragFraction = if (checkedState) 1f else 0f; dragging = true },
            onDragStopped = {
                val target = dragFraction >= 0.5f
                dragging = false
                if (target != checkedState) onChange?.invoke(target)
            }), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(32.dp).layerBackdrop(track)
            .glass(backdrop, shape).background(accent.copy(alpha = 0.08f + fraction * 0.64f), shape))
        val thumb = Modifier.padding(horizontal = 3.dp).graphicsLayer {
            translationX = fraction * travel * if (ltr) 1f else -1f
        }
        val material = if (Build.VERSION.SDK_INT >= 33) {
            thumb.drawBackdrop(rememberCombinedBackdrop(backdrop, track), shape = { shape },
                effects = { blur(3.dp.toPx() * (1f - pressProgress));
                    lens(6.dp.toPx(), 12.dp.toPx(), chromaticAberration = true) },
                highlight = { Highlight.Default },
                shadow = { Shadow(radius = 5.dp, color = Color.Black.copy(alpha = 0.16f)) },
                innerShadow = { InnerShadow(radius = 3.dp, alpha = 0.15f) },
                layerBlock = { scaleX = scale; scaleY = scale },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.82f - pressProgress * 0.5f)) })
        } else thumb.background(if (dark) Color(0xFFEAF3FF) else Color.White, shape)
        Box(material.size(34.dp, 26.dp))
    }
}
