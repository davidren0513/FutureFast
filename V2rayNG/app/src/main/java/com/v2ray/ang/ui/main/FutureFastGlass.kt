package com.v2ray.ang.ui.main

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.v2ray.ang.ui.compose.LocalDarkTheme
import com.v2ray.ang.ui.main.liquid.LiquidButton

/** Decorative geometry lives in the sampled backdrop so the lens refracts actual content. */
@Composable
internal fun FutureFastBackdrop(backdrop: LayerBackdrop, dark: Boolean) {
    // Sample a static backdrop; animate the interactive lenses without continuous full-screen redraws.
    Canvas(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
        val w = size.width
        val h = size.height
        val drift = 0f
        val base = if (dark) listOf(Color(0xFF081325), Color(0xFF192743), Color(0xFF10293B))
            else listOf(Color(0xFFF6FBFF), Color(0xFFE7F3FF), Color(0xFFF3F0FF), Color(0xFFDCF7FC))
        drawRect(Brush.linearGradient(base, end = Offset(w, h)))
        fun glow(color: Color, x: Float, y: Float, radius: Float) {
            drawCircle(Brush.radialGradient(listOf(color, Color.Transparent),
                Offset(w * x + drift, h * y), w * radius),
                w * radius, Offset(w * x + drift, h * y))
        }
        glow(Color(0xFF40DDBE).copy(alpha = if (dark) 0.25f else 0.65f), 0.02f, 0.43f, 0.9f)
        glow(Color(0xFF4B83F8).copy(alpha = if (dark) 0.30f else 0.48f), 0.98f, 0.21f, 0.8f)
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
                Color.White.copy(alpha = if (dark) 0.06f else 0.20f),
                Color(0xFF20CDB6).copy(alpha = if (dark) 0.20f else 0.38f),
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
    val base = this.shadow(if (prominent) 18.dp else 8.dp, shape, clip = false,
        ambientColor = Color(0xFF496DA9).copy(alpha = 0.2f), spotColor = Color(0xFF32669B).copy(alpha = 0.22f))
    val material = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        base.drawBackdrop(backdrop = backdrop, shape = { shape },
            effects = {
                vibrancy()
                blur((if (prominent) 2f else 6f).dp.toPx())
                lens((if (prominent) 28f else 16f).dp.toPx(),
                    (if (prominent) 58f else 30f).dp.toPx(),
                    depthEffect = true, chromaticAberration = prominent)
            },
            highlight = { Highlight(width = if (prominent) 1.7.dp else 1.2.dp, blurRadius = 0.8.dp) },
            innerShadow = { InnerShadow(radius = if (prominent) 18.dp else 10.dp,
                offset = DpOffset(0.dp, 3.dp),
                color = Color(0xFF6685BD).copy(alpha = if (dark) 0.32f else 0.22f)) },
            onDrawSurface = {
                drawRect(Brush.linearGradient(listOf(surface.copy(alpha = if (dark) 0.20f else 0.28f),
                    surface.copy(alpha = 0.04f), surface.copy(alpha = if (dark) 0.08f else 0.06f))))
            })
    } else base.background(Brush.linearGradient(listOf(surface.copy(alpha = 0.60f),
        surface.copy(alpha = 0.20f), surface.copy(alpha = 0.45f))), shape)
    return material.clip(shape).border(if (prominent) 1.4.dp else 0.8.dp, rim, shape)
}

@Composable
internal fun GlassPowerButton(backdrop: Backdrop, label: String,
    enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dark = LocalDarkTheme.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth * 0.72f, maxHeight, 236.dp).coerceAtLeast(72.dp)
        val content: @Composable RowScope.() -> Unit = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Canvas(Modifier.size(minOf(diameter * 0.31f, 72.dp))) {
                    val brush = Brush.verticalGradient(listOf(Color(0xFF36B8FF), Color(0xFF006BFF)))
                    val stroke = size.width * 0.10f
                    drawArc(brush, -49f, 278f, false,
                        Offset(stroke, stroke * 1.4f), Size(size.width - 2 * stroke, size.height - 2.4f * stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round))
                    drawLine(brush, Offset(size.width / 2f, stroke * 0.42f),
                        Offset(size.width / 2f, size.height * 0.43f), stroke, StrokeCap.Round)
                }
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    color = if (dark) Color(0xFFE7F2FF) else Color(0xFF12458B))
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LiquidButton(onClick = onClick, backdrop = backdrop, enabled = enabled,
                modifier = Modifier.size(diameter).shadow(24.dp, CircleShape, clip = false,
                    ambientColor = Color(0xFF669EBD).copy(alpha = 0.3f), spotColor = Color(0xFF527AA9).copy(alpha = 0.3f))
                    .drawWithContent {
                        drawContent()
                        drawCircle(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.05f), Color(0xFFB4E8FF), Color.White.copy(alpha = 0.8f))),
                            radius = size.minDimension / 2f - 2.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                        drawArc(Color.White.copy(alpha = 0.65f), 196f, 78f, false,
                            Offset(8.dp.toPx(), 8.dp.toPx()), Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()),
                            style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                        drawOval(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.08f))),
                            Offset(size.width * 0.17f, size.height * 0.12f), Size(size.width * 0.23f, size.height * 0.055f))
                    }.semantics(mergeDescendants = true) {},
                surfaceColor = Color.White.copy(alpha = if (dark) 0.04f else 0.06f), content = content)
        } else {
            Row(Modifier.size(diameter).glass(backdrop, CircleShape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically, content = content)
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
