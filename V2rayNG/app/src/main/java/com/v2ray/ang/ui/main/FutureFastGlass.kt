package com.v2ray.ang.ui.main

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.LocalDarkTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A persistent, accessible navigation surface. Lens/velocity deformation follows Kyant's
 * Apache-2.0 LiquidBottomTabs example (revision 65ab177e90e5c1d8c62e70cf7755841982da65f6).
 * The blue optical coating and taller icon/label layout match Future Fast's reference.
 * Only presentation/gesture state lives here; the screen owns the selected destination.
 */
@Composable
internal fun FutureFastNavigation(backdrop: Backdrop, page: String, onSelect: (String) -> Unit,
    modifier: Modifier = Modifier) {
    val tabs = remember { listOf("home", "nodes", "ai") }
    val titles = listOf(R.string.ff_home, R.string.ff_nodes, R.string.ff_ai)
    val interactions = remember { tabs.map { MutableInteractionSource() } }
    val homePressed by interactions[0].collectIsPressedAsState()
    val nodesPressed by interactions[1].collectIsPressedAsState()
    val aiPressed by interactions[2].collectIsPressedAsState()
    val latestPage by rememberUpdatedState(page)
    val latestSelect by rememberUpdatedState(onSelect)
    val index = tabs.indexOf(page).coerceAtLeast(0)
    val position = remember { Animatable(index.toFloat()) }
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    val press by animateFloatAsState(
        if (homePressed || nodesPressed || aiPressed || dragPosition != null) 1f else 0f,
        spring(dampingRatio = 0.65f, stiffness = 380f), label = "navigation-press")
    LaunchedEffect(index, dragPosition) {
        val drag = dragPosition
        if (drag != null) position.snapTo(drag)
        else position.animateTo(index.toFloat(), spring(dampingRatio = 0.72f,
            stiffness = 420f, visibilityThreshold = 0.001f))
    }
    val dark = LocalDarkTheme.current
    val ink = LocalContentColor.current
    val barBackdrop = rememberLayerBackdrop()
    val lensBackdrop = rememberCombinedBackdrop(backdrop, barBackdrop)
    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val tabWidth = (maxWidth - 12.dp) / 3
        val tabPx = with(LocalDensity.current) { tabWidth.toPx() }
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(Modifier.fillMaxWidth().height(76.dp)
            .graphicsLayer {
                scaleX = 1f + press * 0.018f
                scaleY = 1f + press * 0.035f
            }
            .layerBackdrop(barBackdrop)
            .navigationGlass(backdrop, selected = false, press = { press })
            .clearAndSetSemantics {})
        // A single lens moves between destinations; it is never recreated per selected tab.
        Box(Modifier.padding(horizontal = 6.dp).width(tabWidth).height(64.dp)
            .graphicsLayer {
                translationX = position.value * tabPx * if (rtl) -1f else 1f
                val stretch = (abs(position.velocity) * 0.018f).coerceIn(0f, 0.16f)
                scaleX = 1f + press * 0.075f + stretch
                scaleY = 1f + press * 0.09f - stretch * 0.35f
            }
            .navigationGlass(lensBackdrop, selected = true, press = { press })
            .clearAndSetSemantics {})
        Row(Modifier.fillMaxWidth().selectableGroup()
            .pointerInput(tabPx, rtl) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragPosition = tabs.indexOf(latestPage).coerceAtLeast(0).toFloat()
                    },
                    onDragEnd = {
                        val target = futureFastDragDestination(dragPosition ?: index.toFloat())
                        dragPosition = null
                        latestSelect(target)
                    },
                    onDragCancel = { dragPosition = null },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        if (tabPx > 0f) dragPosition = ((dragPosition ?: index.toFloat()) +
                            amount / tabPx * if (rtl) -1f else 1f).coerceIn(0f, 2f)
                    })
            }
            .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { tabIndex, id ->
                key(id) {
                    val selected = page == id
                    val focused by interactions[tabIndex].collectIsFocusedAsState()
                    val tint by animateColorAsState(
                        if (selected) Color.White else ink.copy(alpha = if (dark) 0.82f else 0.68f),
                        tween(180), label = "navigation-ink")
                    Column(Modifier.weight(1f).heightIn(min = 64.dp)
                        .clip(CircleShape)
                        .border(if (focused) 2.dp else 0.dp,
                            if (focused) Color(0xFF287BFA) else Color.Transparent, CircleShape)
                        .selectable(selected, interactionSource = interactions[tabIndex],
                            indication = null, role = Role.Tab, onClick = { onSelect(id) })
                        .semantics(mergeDescendants = true) {}
                        .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
                        GlassIcon(when (id) {
                            "home" -> GlassGlyph.LINK
                            "nodes" -> GlassGlyph.NODES
                            else -> GlassGlyph.AI
                        }, Modifier.size(25.dp), tint)
                        Text(stringResource(titles[tabIndex]), color = tint,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

internal fun futureFastDragDestination(position: Float): String = when {
    !position.isFinite() -> "home"
    position.roundToInt().coerceIn(0, 2) == 0 -> "home"
    position.roundToInt().coerceIn(0, 2) == 1 -> "nodes"
    else -> "ai"
}

@Composable
private fun Modifier.navigationGlass(backdrop: Backdrop, selected: Boolean,
    press: () -> Float): Modifier {
    val dark = LocalDarkTheme.current
    val coating = if (selected) Brush.verticalGradient(listOf(
        Color(0xFF6AD9FF).copy(alpha = 0.78f), Color(0xFF2584FA).copy(alpha = 0.85f),
        Color(0xFF3268EC).copy(alpha = 0.78f)))
    else Brush.verticalGradient(listOf(
        Color.White.copy(alpha = if (dark) 0.13f else 0.52f),
        Color(0xFFC5E2FF).copy(alpha = if (dark) 0.08f else 0.16f),
        Color.White.copy(alpha = if (dark) 0.07f else 0.35f)))
    // RuntimeShader lenses require Android 13. Remove this fallback when minSdk >= 33.
    // Older devices keep the same moving capsule and accessible tabs without invoking the shader.
    val optical = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        drawBackdrop(backdrop = backdrop, shape = { CircleShape }, effects = {
            vibrancy()
            blur((if (selected) 1f else 8f).dp.toPx())
            lens((if (selected) 10f + press() * 6f else 24f).dp.toPx(),
                (if (selected) 18f + press() * 12f else 24f).dp.toPx(),
                chromaticAberration = selected, depthEffect = true)
        }, highlight = { Highlight(width = if (selected) 1.5.dp else 1.dp, blurRadius = 0.6.dp) },
            shadow = { Shadow(radius = if (selected) 8.dp else 16.dp,
                color = Color(0xFF486BA8), alpha = if (dark) 0.28f else 0.16f) },
            innerShadow = { InnerShadow(radius = if (selected) 6.dp else 10.dp,
                color = Color(0xFF4D73BC).copy(alpha = 0.15f), offset = DpOffset(0.dp, 2.dp)) },
            onDrawSurface = { drawRect(coating) })
    } else background(coating, CircleShape)
    return optical.border(1.dp, Brush.linearGradient(listOf(
        Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.12f),
        Color(0xFFBDEBFF).copy(alpha = 0.7f))), CircleShape)
}

/** Decorative geometry lives in the sampled backdrop so the lens refracts actual content. */
@Composable
internal fun FutureFastBackdrop(backdrop: LayerBackdrop, dark: Boolean) {
    val motion = rememberInfiniteTransition(label = "glass-light")
    val phase by motion.animateFloat(0f, 1f,
        infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse),
        label = "glass-light-position")
    Canvas(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
        val w = size.width
        val h = size.height
        val drift = (phase - 0.5f) * w * 0.09f
        val base = if (dark) listOf(Color(0xFF081325), Color(0xFF192743), Color(0xFF10293B))
            else listOf(Color(0xFFF6FBFF), Color(0xFFE7F3FF), Color(0xFFF3F0FF), Color(0xFFDCF7FC))
        drawRect(Brush.linearGradient(base, end = Offset(w, h)))
        fun glow(color: Color, x: Float, y: Float, radius: Float) {
            drawCircle(Brush.radialGradient(listOf(color, Color.Transparent),
                Offset(w * x + drift, h * y), w * radius),
                w * radius, Offset(w * x + drift, h * y))
        }
        glow(Color(0xFF80DFFF).copy(alpha = if (dark) 0.19f else 0.45f), 0.02f, 0.43f, 0.9f)
        glow(Color(0xFFB1A4F8).copy(alpha = if (dark) 0.24f else 0.34f), 0.98f, 0.21f, 0.8f)
        glow(Color(0xFF82E7F1).copy(alpha = if (dark) 0.16f else 0.36f), 0.8f, 0.76f, 0.68f)
        glow(Color(0xFFB9A2F7).copy(alpha = if (dark) 0.16f else 0.32f), 0.16f, 0.95f, 0.75f)

        val right = Path().apply {
            moveTo(w * 0.93f + drift, -h * 0.12f)
            cubicTo(w * 0.40f, h * 0.15f, w * 0.87f, h * 0.27f, w * 1.24f, h * 0.35f)
            lineTo(w * 1.35f, -h * 0.1f); close()
        }
        val left = Path().apply {
            moveTo(-w * 0.35f, h * 0.29f)
            cubicTo(-w * 0.4f, h * 0.61f, w * 0.82f + drift, h * 0.61f, w * 0.38f, h * 1.19f)
            lineTo(-w * 0.3f, h * 1.15f); close()
        }
        val bottom = Path().apply {
            moveTo(w * 1.22f, h * 0.61f)
            cubicTo(w * 0.50f, h * 0.74f, w * 0.49f + drift, h * 0.92f, w * 1.07f, h * 1.12f)
            close()
        }
        listOf(right, left, bottom).forEachIndexed { index, path ->
            drawPath(path, Brush.linearGradient(listOf(
                Color.White.copy(alpha = if (dark) 0.06f else 0.54f),
                Color(0xFF82DFFC).copy(alpha = if (dark) 0.10f else 0.20f),
                Color(0xFFC2B1F8).copy(alpha = if (dark) 0.16f else 0.28f),
                Color.White.copy(alpha = if (dark) 0.04f else 0.48f)),
                start = Offset(w * 0.1f, h * index * 0.2f), end = Offset(w, h)))
            drawPath(path, Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) 0.12f else 0.8f),
                Color(0xFF9BE9FC).copy(alpha = 0.12f), Color.White.copy(alpha = if (dark) 0.06f else 0.6f))),
                style = Stroke(1.3.dp.toPx()))
        }
    }
}

/** Android 13+ uses Backdrop's live lens; older versions retain the same drawn material silhouette. */
@Composable
internal fun Modifier.glass(backdrop: Backdrop, shape: Shape = RoundedCornerShape(28.dp),
    prominent: Boolean = false): Modifier {
    val dark = LocalDarkTheme.current
    val surface = if (dark) Color(0xFF1C3554) else Color.White
    val rim = Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) 0.5f else 0.95f),
        Color.White.copy(alpha = 0.10f), Color(0xFFBEECFF).copy(alpha = 0.5f),
        Color.White.copy(alpha = if (dark) 0.4f else 0.95f)))
    val material = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        drawBackdrop(backdrop = backdrop, shape = { shape },
            effects = {
                vibrancy()
                blur((if (prominent) 2f else 3f).dp.toPx())
                lens((if (prominent) 28f else 16f).dp.toPx(),
                    (if (prominent) 58f else 30f).dp.toPx(),
                    depthEffect = true, chromaticAberration = prominent)
            },
            highlight = { Highlight(width = if (prominent) 1.7.dp else 1.2.dp, blurRadius = 0.8.dp) },
            innerShadow = { InnerShadow(radius = if (prominent) 14.dp else 8.dp,
                offset = DpOffset(0.dp, 3.dp),
                color = Color(0xFF6685BD).copy(alpha = if (dark) 0.28f else 0.13f)) },
            onDrawSurface = {
                drawRect(Brush.linearGradient(listOf(surface.copy(alpha = if (dark) 0.18f else 0.36f),
                    surface.copy(alpha = 0.04f), surface.copy(alpha = if (dark) 0.12f else 0.23f))))
            })
    } else background(Brush.linearGradient(listOf(surface.copy(alpha = 0.60f),
        surface.copy(alpha = 0.20f), surface.copy(alpha = 0.45f))), shape)
    return material.clip(shape).border(if (prominent) 1.4.dp else 1.dp, rim, shape)
}

@Composable
internal fun GlassPowerButton(backdrop: Backdrop, label: String, running: Boolean,
    enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.965f else 1f,
        spring(dampingRatio = 0.6f, stiffness = 450f), label = "power-press")
    val dark = LocalDarkTheme.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth * 0.79f, 280.dp)
        Box(Modifier.size(diameter).graphicsLayer { scaleX = scale; scaleY = scale }
            .glass(backdrop, CircleShape, prominent = true)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled,
                role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val rim = Brush.sweepGradient(listOf(Color(0xFF929FFA), Color.White,
                    Color(0xFF87DFF7), Color.White, Color(0xFFE9CBFC), Color(0xFF929FFA)))
                drawCircle(rim, r - 2.dp.toPx(), style = Stroke(1.2.dp.toPx()))
                drawCircle(Color.White.copy(alpha = if (dark) 0.35f else 0.85f),
                    r - 7.dp.toPx(), style = Stroke(1.dp.toPx()))
                drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent,
                    Color(0xFF7EA7EC).copy(alpha = 0.12f), Color.White.copy(alpha = 0.48f)),
                    radius = r), r - 4.dp.toPx())
                val inset = 11.dp.toPx()
                drawArc(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.05f))),
                    191f, 104f, false, Offset(inset, inset),
                    Size(size.width - 2 * inset, size.height - 2 * inset),
                    style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                drawArc(Color.White.copy(alpha = 0.8f), 18f, 74f, false,
                    Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset),
                    style = Stroke(2.3.dp.toPx(), cap = StrokeCap.Round))
                rotate(-28f, Offset(r * 0.55f, r * 0.32f)) {
                    drawOval(Color.White.copy(alpha = 0.83f), Offset(r * 0.30f, r * 0.27f),
                        Size(r * 0.50f, r * 0.09f))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Canvas(Modifier.size(82.dp)) {
                    val color = if (running) listOf(Color(0xFF47DCC6), Color(0xFF008D85))
                        else listOf(Color(0xFF43C9FF), Color(0xFF1259FF))
                    val brush = Brush.verticalGradient(color)
                    val stroke = size.width * 0.125f
                    drawArc(brush, -49f, 278f, false,
                        Offset(stroke, stroke * 1.4f),
                        Size(size.width - 2 * stroke, size.height - 2.4f * stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round))
                    drawLine(brush, Offset(size.width / 2f, stroke * 0.42f),
                        Offset(size.width / 2f, size.height * 0.43f), stroke, StrokeCap.Round)
                }
                Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    color = if (dark) Color(0xFFD6E9FF) else Color(0xFF082A96))
            }
        }
    }
}

/** Decorative icons inherit the single accessible action label from their parent row. */
internal enum class GlassGlyph { LINK, NODES, AI, LAYERS, CLOCK, DOWNLOAD, SPEED, CHAT, CLAUDE, GEMINI, ARROW }

@Composable
internal fun GlassIcon(kind: GlassGlyph, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun path(points: List<Offset>, closed: Boolean = false) {
            val p = Path().apply { moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }; if (closed) close() }
            drawPath(p, tint, style = stroke)
        }
        when (kind) {
            GlassGlyph.LINK -> {
                rotate(-43f) {
                    drawRoundRect(tint, Offset(w * 0.10f, h * 0.32f), Size(w * 0.48f, h * 0.36f),
                        androidx.compose.ui.geometry.CornerRadius(w * 0.16f), style = stroke)
                    drawRoundRect(tint, Offset(w * 0.42f, h * 0.32f), Size(w * 0.48f, h * 0.36f),
                        androidx.compose.ui.geometry.CornerRadius(w * 0.16f), style = stroke)
                }
            }
            GlassGlyph.NODES -> {
                listOf(0.12f, 0.58f).forEach { y ->
                    drawRoundRect(tint, Offset(w * 0.15f, h * y), Size(w * 0.70f, h * 0.27f),
                        androidx.compose.ui.geometry.CornerRadius(w * 0.06f), style = stroke)
                    drawCircle(tint, w * 0.035f, Offset(w * 0.27f, h * (y + 0.135f)))
                }
            }
            GlassGlyph.LAYERS -> {
                path(listOf(Offset(w * 0.1f, h * 0.3f), Offset(w * 0.5f, h * 0.08f),
                    Offset(w * 0.9f, h * 0.3f), Offset(w * 0.5f, h * 0.52f)), true)
                listOf(0.49f, 0.68f).forEach { y ->
                    path(listOf(Offset(w * 0.1f, h * y), Offset(w * 0.5f, h * (y + 0.22f)), Offset(w * 0.9f, h * y)))
                }
            }
            GlassGlyph.AI, GlassGlyph.GEMINI -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.04f)
                    quadraticTo(w * 0.57f, h * 0.40f, w * 0.94f, h * 0.5f)
                    quadraticTo(w * 0.57f, h * 0.60f, w * 0.5f, h * 0.96f)
                    quadraticTo(w * 0.43f, h * 0.60f, w * 0.06f, h * 0.5f)
                    quadraticTo(w * 0.43f, h * 0.40f, w * 0.5f, h * 0.04f); close()
                }
                drawPath(p, Brush.linearGradient(listOf(tint.copy(alpha = 0.55f), tint)))
            }
            GlassGlyph.CLOCK -> {
                drawCircle(tint, w * 0.39f, style = stroke)
                path(listOf(Offset(w * 0.5f, h * 0.22f), Offset(w * 0.5f, h * 0.5f), Offset(w * 0.70f, h * 0.61f)))
            }
            GlassGlyph.DOWNLOAD -> {
                drawArc(tint, 150f, 240f, false, Offset(w * 0.14f, h * 0.1f), Size(w * 0.72f, h * 0.66f), style = stroke)
                path(listOf(Offset(w * 0.5f, h * 0.48f), Offset(w * 0.5f, h * 0.91f)))
                path(listOf(Offset(w * 0.33f, h * 0.74f), Offset(w * 0.5f, h * 0.91f), Offset(w * 0.67f, h * 0.74f)))
            }
            GlassGlyph.SPEED -> {
                drawArc(tint, 145f, 250f, false, Offset(w * 0.1f, h * 0.1f), Size(w * 0.8f, h * 0.8f), style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.55f), Offset(w * 0.73f, h * 0.28f), stroke.width, StrokeCap.Round)
            }
            GlassGlyph.CHAT -> {
                drawRoundRect(tint, Offset(w * 0.1f, h * 0.1f), Size(w * 0.8f, h * 0.65f),
                    androidx.compose.ui.geometry.CornerRadius(w * 0.18f), style = stroke)
                path(listOf(Offset(w * 0.26f, h * 0.75f), Offset(w * 0.25f, h * 0.92f), Offset(w * 0.45f, h * 0.75f)))
                listOf(0.32f, 0.5f, 0.68f).forEach { drawCircle(tint, w * 0.035f, Offset(w * it, h * 0.42f)) }
            }
            GlassGlyph.CLAUDE -> repeat(10) { i -> rotate(i * 36f) {
                drawLine(tint, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.8f), stroke.width, StrokeCap.Round)
            } }
            GlassGlyph.ARROW -> path(listOf(Offset(w * 0.36f, h * 0.22f), Offset(w * 0.64f, h * 0.5f), Offset(w * 0.36f, h * 0.78f)))
        }
    }
}
