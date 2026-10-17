package com.strobingn.bowtune.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Series are told apart by dash pattern, never by hue. */
enum class LineStyle { SOLID, DASHED, DOTTED, DASH_DOT }

enum class MarkerShape { CIRCLE, SQUARE, TRIANGLE, DIAMOND, RING, CROSS, NONE }

/** Bar / area fills told apart by texture, never by hue. */
enum class FillPattern { SOLID, DIAGONAL, CROSSHATCH, DOTS, HORIZONTAL, OUTLINE }

/** Grey tones pulled from the active (grayscale) color scheme. */
@Immutable
data class ChartPalette(
    val ink: Color,
    val strong: Color,
    val mid: Color,
    val soft: Color,
    val faint: Color,
    val track: Color,
    val surface: Color
) {
    /** Distinct grey per series index, strongest first. */
    fun series(index: Int): Color = listOf(ink, strong, mid, soft)[index.mod(4)]
}

@Composable
fun rememberChartPalette(): ChartPalette {
    val cs = MaterialTheme.colorScheme
    return ChartPalette(
        ink = cs.onSurface,
        strong = cs.onSurfaceVariant,
        mid = cs.outline,
        soft = cs.outlineVariant,
        faint = cs.outlineVariant.copy(alpha = 0.45f),
        track = cs.surfaceContainerHighest,
        surface = cs.surfaceContainerLow
    )
}

val seriesLineStyles = listOf(LineStyle.SOLID, LineStyle.DASHED, LineStyle.DOTTED, LineStyle.DASH_DOT)
val seriesMarkers = listOf(MarkerShape.CIRCLE, MarkerShape.SQUARE, MarkerShape.TRIANGLE, MarkerShape.DIAMOND)
val seriesPatterns = listOf(
    FillPattern.SOLID,
    FillPattern.DIAGONAL,
    FillPattern.CROSSHATCH,
    FillPattern.DOTS,
    FillPattern.HORIZONTAL,
    FillPattern.OUTLINE
)

fun LineStyle.pathEffect(density: Density): PathEffect? = with(density) {
    when (this@pathEffect) {
        LineStyle.SOLID -> null
        LineStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))
        LineStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(1.5.dp.toPx(), 4.dp.toPx()))
        LineStyle.DASH_DOT -> PathEffect.dashPathEffect(
            floatArrayOf(9.dp.toPx(), 4.dp.toPx(), 1.5.dp.toPx(), 4.dp.toPx())
        )
    }
}

fun DrawScope.drawMarker(
    shape: MarkerShape,
    center: Offset,
    radius: Float,
    color: Color,
    background: Color
) {
    when (shape) {
        MarkerShape.NONE -> Unit
        MarkerShape.CIRCLE -> drawCircle(color, radius, center)
        MarkerShape.RING -> {
            drawCircle(background, radius, center)
            drawCircle(color, radius, center, style = Stroke(radius * 0.45f))
        }
        MarkerShape.SQUARE -> drawRect(
            color,
            Offset(center.x - radius * 0.85f, center.y - radius * 0.85f),
            Size(radius * 1.7f, radius * 1.7f)
        )
        MarkerShape.TRIANGLE -> {
            val p = Path().apply {
                moveTo(center.x, center.y - radius * 1.1f)
                lineTo(center.x + radius, center.y + radius * 0.8f)
                lineTo(center.x - radius, center.y + radius * 0.8f)
                close()
            }
            drawPath(p, color, style = Fill)
        }
        MarkerShape.DIAMOND -> {
            val p = Path().apply {
                moveTo(center.x, center.y - radius * 1.15f)
                lineTo(center.x + radius * 1.15f, center.y)
                lineTo(center.x, center.y + radius * 1.15f)
                lineTo(center.x - radius * 1.15f, center.y)
                close()
            }
            drawPath(p, color, style = Fill)
        }
        MarkerShape.CROSS -> {
            val s = radius
            val w = radius * 0.5f
            drawLine(color, Offset(center.x - s, center.y - s), Offset(center.x + s, center.y + s), w, StrokeCap.Round)
            drawLine(color, Offset(center.x - s, center.y + s), Offset(center.x + s, center.y - s), w, StrokeCap.Round)
        }
    }
}

/** Fills [rect] with a texture. OUTLINE draws only the border. */
fun DrawScope.drawPatternRect(rect: Rect, pattern: FillPattern, color: Color) {
    if (rect.width <= 0f || rect.height <= 0f) return
    val line = 1.4.dp.toPx()
    val gap = 6.dp.toPx()
    when (pattern) {
        FillPattern.SOLID -> drawRect(color, rect.topLeft, rect.size)
        FillPattern.OUTLINE -> Unit
        else -> clipRect(rect.left, rect.top, rect.right, rect.bottom) {
            drawRect(color.copy(alpha = 0.14f), rect.topLeft, rect.size)
            when (pattern) {
                FillPattern.DIAGONAL, FillPattern.CROSSHATCH -> {
                    val span = rect.width + rect.height
                    var d = -rect.height
                    while (d < span) {
                        drawLine(
                            color,
                            Offset(rect.left + d, rect.bottom),
                            Offset(rect.left + d + rect.height, rect.top),
                            line
                        )
                        if (pattern == FillPattern.CROSSHATCH) {
                            drawLine(
                                color,
                                Offset(rect.left + d, rect.top),
                                Offset(rect.left + d + rect.height, rect.bottom),
                                line
                            )
                        }
                        d += gap
                    }
                }
                FillPattern.DOTS -> {
                    var y = rect.top + gap / 2
                    var row = 0
                    while (y < rect.bottom) {
                        var x = rect.left + gap / 2 + if (row % 2 == 0) 0f else gap / 2
                        while (x < rect.right) {
                            drawCircle(color, line, Offset(x, y))
                            x += gap
                        }
                        y += gap
                        row++
                    }
                }
                FillPattern.HORIZONTAL -> {
                    var y = rect.top + gap / 2
                    while (y < rect.bottom) {
                        drawLine(color, Offset(rect.left, y), Offset(rect.right, y), line)
                        y += gap
                    }
                }
                else -> Unit
            }
        }
    }
    if (pattern != FillPattern.SOLID) {
        drawRect(color, rect.topLeft, rect.size, style = Stroke(line))
    }
}

fun DrawScope.drawChartText(
    measurer: TextMeasurer,
    text: String,
    anchor: Offset,
    color: Color,
    fontSizeSp: Float = 10f,
    alignX: Float = 0.5f,
    alignY: Float = 0.5f,
    bold: Boolean = false
) {
    if (text.isEmpty()) return
    val layout = measurer.measure(
        text,
        TextStyle(
            color = color,
            fontSize = fontSizeSp.sp,
            fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.SemiBold else null
        )
    )
    val x = (anchor.x - layout.size.width * alignX)
        .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
    val y = (anchor.y - layout.size.height * alignY)
        .coerceIn(0f, (size.height - layout.size.height).coerceAtLeast(0f))
    drawText(layout, topLeft = Offset(x, y))
}

/** Nice round tick values covering [min]..[max]. */
fun niceTicks(min: Double, max: Double, target: Int = 4): List<Double> {
    if (!min.isFinite() || !max.isFinite()) return emptyList()
    val span = (max - min).takeIf { it > 1e-9 } ?: 1.0
    val raw = span / target
    val mag = Math.pow(10.0, Math.floor(Math.log10(raw)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * mag }.first { it >= raw }
    val start = Math.ceil(min / step) * step
    val out = mutableListOf<Double>()
    var v = start
    while (v <= max + step * 1e-6) {
        out += if (kotlin.math.abs(v) < step * 1e-6) 0.0 else v
        v += step
    }
    return out
}

@Immutable
data class LegendItem(
    val label: String,
    val color: Color,
    val lineStyle: LineStyle? = null,
    val marker: MarkerShape = MarkerShape.NONE,
    val pattern: FillPattern? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartLegend(items: List<LegendItem>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    val bg = MaterialTheme.colorScheme.surface
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Canvas(Modifier.size(width = 26.dp, height = 12.dp)) {
                    val mid = size.height / 2
                    when {
                        item.pattern != null -> drawPatternRect(
                            Rect(0f, 0f, size.width, size.height),
                            item.pattern,
                            item.color
                        )
                        item.lineStyle != null -> drawLine(
                            item.color,
                            Offset(0f, mid),
                            Offset(size.width, mid),
                            2.dp.toPx(),
                            pathEffect = item.lineStyle.pathEffect(this)
                        )
                    }
                    if (item.marker != MarkerShape.NONE) {
                        drawMarker(item.marker, Offset(size.width / 2, mid), 3.5.dp.toPx(), item.color, bg)
                    }
                }
                Text(item.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
