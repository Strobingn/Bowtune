package com.strobingn.bowtune.ui.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.strobingn.bowtune.data.formatNumber
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Immutable
data class ChartPoint(val x: Double, val y: Double)

@Immutable
data class LineSeries(
    val label: String,
    val points: List<ChartPoint>,
    val style: LineStyle = LineStyle.SOLID,
    val marker: MarkerShape = MarkerShape.CIRCLE,
    /** Null = picked from the palette by series index. */
    val color: Color? = null,
    val strokeWidth: Dp = 2.dp
)

@Immutable
data class ReferenceLine(val y: Double, val label: String, val style: LineStyle = LineStyle.DOTTED)

@Composable
fun ChartEmpty(text: String, modifier: Modifier = Modifier, height: Dp = 120.dp) {
    val palette = rememberChartPalette()
    Box(
        modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                palette.soft,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                style = Stroke(1.dp.toPx(), pathEffect = LineStyle.DASHED.pathEffect(this))
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

@Composable
private fun rememberEntrance(key: Any?): Animatable<Float, *> {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(key) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(650))
    }
    return anim
}

/**
 * Multi-series line chart. X values are plotted proportionally; Y gets nice ticks.
 * [xLabel] formats tick values along the bottom (first, middle, last point).
 */
@Composable
fun LineChart(
    series: List<LineSeries>,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    yUnit: String = "",
    xLabel: (Double) -> String = { formatNumber(it, 0) },
    references: List<ReferenceLine> = emptyList(),
    band: ClosedFloatingPointRange<Double>? = null,
    bandLabel: String = "",
    yMinOverride: Double? = null,
    yMaxOverride: Double? = null,
    emptyText: String = "No data yet",
    description: String = ""
) {
    val all = series.flatMap { it.points }
    if (all.isEmpty()) {
        ChartEmpty(emptyText, modifier, height = height * 0.6f)
        return
    }
    val palette = rememberChartPalette()
    val measurer = rememberTextMeasurer()
    val progress = rememberEntrance(series)

    val ys = all.map { it.y } + references.map { it.y } + listOfNotNull(band?.start, band?.endInclusive)
    var yMin = yMinOverride ?: ys.min()
    var yMax = yMaxOverride ?: ys.max()
    if (abs(yMax - yMin) < 1e-6) {
        yMin -= 1.0
        yMax += 1.0
    }
    val pad = (yMax - yMin) * 0.08
    if (yMinOverride == null) yMin -= pad
    if (yMaxOverride == null) yMax += pad
    val ticks = niceTicks(yMin, yMax)
    val xMin = all.minOf { it.x }
    val xMax = all.maxOf { it.x }.let { if (it - xMin < 1e-9) xMin + 1.0 else it }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { if (description.isNotEmpty()) contentDescription = description }
    ) {
        val left = 38.dp.toPx()
        val right = 10.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = 22.dp.toPx()
        val w = size.width - left - right
        val h = size.height - top - bottom
        fun px(x: Double) = left + ((x - xMin) / (xMax - xMin)).toFloat() * w
        fun py(y: Double) = top + (1f - ((y - yMin) / (yMax - yMin)).toFloat()) * h

        band?.let {
            val y1 = py(min(it.endInclusive, yMax))
            val y2 = py(max(it.start, yMin))
            drawPatternRect(Rect(left, y1, left + w, y2), FillPattern.DIAGONAL, palette.faint)
            if (bandLabel.isNotEmpty()) {
                drawChartText(measurer, bandLabel, Offset(left + w - 4.dp.toPx(), y1 + 2.dp.toPx()), palette.mid, 9f, 1f, 0f)
            }
        }
        ticks.forEach { t ->
            val y = py(t)
            drawLine(palette.faint, Offset(left, y), Offset(left + w, y), 1.dp.toPx())
            drawChartText(measurer, formatNumber(t, 2) + yUnit, Offset(left - 6.dp.toPx(), y), palette.mid, 9f, 1f, 0.5f)
        }
        drawLine(palette.soft, Offset(left, top + h), Offset(left + w, top + h), 1.dp.toPx())
        if (yMin < 0 && yMax > 0) {
            drawLine(palette.mid, Offset(left, py(0.0)), Offset(left + w, py(0.0)), 1.2.dp.toPx())
        }
        references.forEach { ref ->
            val y = py(ref.y)
            drawLine(palette.mid, Offset(left, y), Offset(left + w, y), 1.2.dp.toPx(), pathEffect = ref.style.pathEffect(this))
            drawChartText(measurer, ref.label, Offset(left + 4.dp.toPx(), y - 2.dp.toPx()), palette.strong, 9f, 0f, 1f)
        }

        val distinctX = all.map { it.x }.distinct().sorted()
        val labelXs = when {
            distinctX.size <= 3 -> distinctX
            else -> listOf(distinctX.first(), distinctX[distinctX.size / 2], distinctX.last())
        }
        labelXs.forEach { x ->
            drawChartText(measurer, xLabel(x), Offset(px(x), top + h + 4.dp.toPx()), palette.mid, 9f, 0.5f, 0f)
        }

        clipToProgress(left, w, progress.value) {
            series.forEachIndexed { i, s ->
                val color = s.color ?: palette.series(i)
                val pts = s.points.sortedBy { it.x }
                if (pts.size > 1) {
                    val path = Path()
                    pts.forEachIndexed { idx, p ->
                        if (idx == 0) path.moveTo(px(p.x), py(p.y)) else path.lineTo(px(p.x), py(p.y))
                    }
                    drawPath(
                        path,
                        color,
                        style = Stroke(
                            s.strokeWidth.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = s.style.pathEffect(this)
                        )
                    )
                }
                pts.forEach { p ->
                    drawMarker(s.marker, Offset(px(p.x), py(p.y)), 3.6.dp.toPx(), color, palette.surface)
                }
            }
        }
    }
}

private inline fun DrawScope.clipToProgress(
    left: Float,
    width: Float,
    progress: Float,
    block: DrawScope.() -> Unit
) {
    clipRect(
        left = 0f,
        top = 0f,
        right = left + width * progress + 8.dp.toPx(),
        bottom = size.height,
        block = block
    )
}

@Immutable
data class BarDatum(
    val label: String,
    val value: Double,
    val pattern: FillPattern = FillPattern.SOLID,
    val emphasized: Boolean = false,
    val valueLabel: String? = null
)

/** Vertical bars; texture (not hue) separates categories. */
@Composable
fun BarChart(
    bars: List<BarDatum>,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
    references: List<ReferenceLine> = emptyList(),
    maxValue: Double? = null,
    valueFormatter: (Double) -> String = { formatNumber(it, 1) },
    emptyText: String = "No data yet",
    description: String = ""
) {
    if (bars.isEmpty()) {
        ChartEmpty(emptyText, modifier, height * 0.6f)
        return
    }
    val palette = rememberChartPalette()
    val measurer = rememberTextMeasurer()
    val progress = rememberEntrance(bars)
    val top = maxValue ?: max(bars.maxOf { it.value }, references.maxOfOrNull { it.y } ?: 0.0).let {
        if (it <= 0.0) 1.0 else it * 1.12
    }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { if (description.isNotEmpty()) contentDescription = description }
    ) {
        val labelBand = 30.dp.toPx()
        val valueBand = 14.dp.toPx()
        val chartH = size.height - labelBand - valueBand
        val slot = size.width / bars.size
        val barW = min(slot * 0.62f, 44.dp.toPx())
        fun py(v: Double) = valueBand + chartH * (1f - (v / top).toFloat().coerceIn(0f, 1f))

        drawLine(palette.soft, Offset(0f, valueBand + chartH), Offset(size.width, valueBand + chartH), 1.dp.toPx())
        references.forEach { ref ->
            val y = py(ref.y)
            drawLine(palette.mid, Offset(0f, y), Offset(size.width, y), 1.2.dp.toPx(), pathEffect = ref.style.pathEffect(this))
            drawChartText(measurer, ref.label, Offset(size.width - 2.dp.toPx(), y - 2.dp.toPx()), palette.strong, 9f, 1f, 1f)
        }
        bars.forEachIndexed { i, bar ->
            val cx = slot * i + slot / 2
            val full = py(bar.value)
            val y = valueBand + chartH - (valueBand + chartH - full) * progress.value
            val color = if (bar.emphasized) palette.ink else palette.strong
            drawPatternRect(Rect(cx - barW / 2, y, cx + barW / 2, valueBand + chartH), bar.pattern, color)
            drawChartText(
                measurer,
                bar.valueLabel ?: valueFormatter(bar.value),
                Offset(cx, y - 2.dp.toPx()),
                palette.ink,
                10f,
                0.5f,
                1f,
                bold = bar.emphasized
            )
            drawChartText(measurer, bar.label, Offset(cx, valueBand + chartH + 4.dp.toPx()), palette.mid, 9f, 0.5f, 0f)
        }
    }
}

/** Tiny trend line for dashboard tiles. */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    style: LineStyle = LineStyle.SOLID
) {
    val palette = rememberChartPalette()
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (values.isEmpty()) {
            drawLine(
                palette.soft,
                Offset(0f, size.height / 2),
                Offset(size.width, size.height / 2),
                1.5.dp.toPx(),
                pathEffect = LineStyle.DOTTED.pathEffect(this)
            )
            return@Canvas
        }
        val lo = values.min()
        val hi = values.max().let { if (it - lo < 1e-9) lo + 1.0 else it }
        val inset = 4.dp.toPx()
        fun px(i: Int) = if (values.size == 1) size.width / 2 else inset + (size.width - 2 * inset) * i / (values.size - 1)
        fun py(v: Double) = inset + (size.height - 2 * inset) * (1f - ((v - lo) / (hi - lo)).toFloat())
        val path = Path()
        values.forEachIndexed { i, v -> if (i == 0) path.moveTo(px(i), py(v)) else path.lineTo(px(i), py(v)) }
        drawPath(path, palette.strong, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = style.pathEffect(this)))
        drawCircle(palette.ink, 3.5.dp.toPx(), Offset(px(values.lastIndex), py(values.last())))
    }
}

@Immutable
data class StackSegment(val label: String, val value: Double, val pattern: FillPattern)

/** Horizontal 100% stacked bar with a pattern legend. */
@Composable
fun StackedBar(
    segments: List<StackSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 26.dp,
    unit: String = "",
    showLegend: Boolean = true
) {
    val palette = rememberChartPalette()
    val total = segments.sumOf { max(it.value, 0.0) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .semantics {
                    contentDescription = segments.joinToString { "${it.label} ${formatNumber(it.value, 1)}$unit" }
                }
        ) {
            drawRoundRect(palette.track, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            if (total <= 0.0) return@Canvas
            var x = 0f
            segments.forEachIndexed { i, s ->
                val w = (s.value.coerceAtLeast(0.0) / total).toFloat() * size.width
                drawPatternRect(Rect(x, 0f, x + w, size.height), s.pattern, palette.series(i % 2))
                x += w
            }
        }
        if (showLegend) {
            ChartLegend(
                segments.mapIndexed { i, s ->
                    LegendItem("${s.label} ${formatNumber(s.value, 1)}$unit", palette.series(i % 2), pattern = s.pattern)
                }
            )
        }
    }
}

@Immutable
data class MirrorRow(val label: String, val left: Double?, val right: Double?, val unit: String = "")

/** Butterfly chart: left setup extends left (solid), right setup extends right (hatched). */
@Composable
fun MirrorBars(
    rows: List<MirrorRow>,
    leftName: String,
    rightName: String,
    modifier: Modifier = Modifier
) {
    val palette = rememberChartPalette()
    val measurer = rememberTextMeasurer()
    val rowH = 34.dp
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(rowH * rows.size)
        ) {
            val mid = size.width / 2
            val labelGap = 4.dp.toPx()
            val rh = size.height / rows.size
            rows.forEachIndexed { i, r ->
                val top = rh * i
                val scale = max(r.left ?: 0.0, r.right ?: 0.0).takeIf { it > 0 } ?: 1.0
                val barH = rh * 0.38f
                val by = top + rh * 0.52f
                drawChartText(measurer, r.label, Offset(mid, top + 1.dp.toPx()), palette.strong, 10f, 0.5f, 0f, bold = true)
                r.left?.let { v ->
                    val w = (v / scale).toFloat() * (mid - 44.dp.toPx())
                    drawPatternRect(Rect(mid - labelGap - w, by, mid - labelGap, by + barH), FillPattern.SOLID, palette.strong)
                    drawChartText(measurer, formatNumber(v, 1) + r.unit, Offset(mid - labelGap - w - 4.dp.toPx(), by + barH / 2), palette.ink, 10f, 1f, 0.5f)
                }
                r.right?.let { v ->
                    val w = (v / scale).toFloat() * (mid - 44.dp.toPx())
                    drawPatternRect(Rect(mid + labelGap, by, mid + labelGap + w, by + barH), FillPattern.DIAGONAL, palette.strong)
                    drawChartText(measurer, formatNumber(v, 1) + r.unit, Offset(mid + labelGap + w + 4.dp.toPx(), by + barH / 2), palette.ink, 10f, 0f, 0.5f)
                }
                if (r.left == null && r.right == null) {
                    drawChartText(measurer, "—", Offset(mid, by + barH / 2), palette.mid, 10f)
                }
            }
            drawLine(palette.soft, Offset(mid, 14.dp.toPx()), Offset(mid, size.height), 1.dp.toPx())
        }
        ChartLegend(
            listOf(
                LegendItem(leftName, palette.strong, pattern = FillPattern.SOLID),
                LegendItem(rightName, palette.strong, pattern = FillPattern.DIAGONAL)
            )
        )
    }
}

@Immutable
data class TimelineEvent(val row: Int, val epochMs: Long)

/** Dot plot over time with one lane per category; each lane gets its own marker shape. */
@Composable
fun EventTimeline(
    rows: List<String>,
    events: List<TimelineEvent>,
    modifier: Modifier = Modifier,
    dateLabel: (Long) -> String
) {
    if (events.isEmpty()) {
        ChartEmpty("Nothing logged yet", modifier)
        return
    }
    val palette = rememberChartPalette()
    val measurer = rememberTextMeasurer()
    val markers = listOf(
        MarkerShape.CIRCLE, MarkerShape.SQUARE, MarkerShape.TRIANGLE, MarkerShape.DIAMOND, MarkerShape.RING, MarkerShape.CROSS
    )
    val tMin = events.minOf { it.epochMs }
    val tMax = events.maxOf { it.epochMs }.let { if (it == tMin) tMin + 1 else it }
    Canvas(
        modifier
            .fillMaxWidth()
            .height((rows.size * 26 + 24).dp)
    ) {
        val left = 86.dp.toPx()
        val right = 10.dp.toPx()
        val laneH = 26.dp.toPx()
        val w = size.width - left - right
        rows.forEachIndexed { i, name ->
            val y = laneH * i + laneH / 2
            drawLine(palette.faint, Offset(left, y), Offset(left + w, y), 1.dp.toPx())
            drawMarker(markers[i % markers.size], Offset(10.dp.toPx(), y), 4.dp.toPx(), palette.strong, palette.surface)
            drawChartText(measurer, name, Offset(20.dp.toPx(), y), palette.strong, 10f, 0f, 0.5f)
        }
        events.forEach { e ->
            val x = left + ((e.epochMs - tMin).toDouble() / (tMax - tMin)).toFloat() * w
            val y = laneH * e.row + laneH / 2
            drawMarker(markers[e.row % markers.size], Offset(x, y), 4.5.dp.toPx(), palette.ink, palette.surface)
        }
        val baseY = laneH * rows.size + 4.dp.toPx()
        drawChartText(measurer, dateLabel(tMin), Offset(left, baseY), palette.mid, 9f, 0f, 0f)
        drawChartText(measurer, dateLabel(tMax), Offset(left + w, baseY), palette.mid, 9f, 1f, 0f)
    }
}

/** Proportional block bar (e.g. LIFT 90-minute timeline); completed blocks solid, current hatched. */
@Composable
fun SegmentedTimeline(
    weights: List<Int>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp
) {
    val palette = rememberChartPalette()
    val total = weights.sum().coerceAtLeast(1)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val gap = 3.dp.toPx()
        var x = 0f
        val usable = size.width - gap * (weights.size - 1)
        weights.forEachIndexed { i, wgt ->
            val w = usable * wgt / total
            val rect = Rect(x, 0f, x + w, size.height)
            when {
                i < currentIndex -> drawRoundRect(palette.ink, rect.topLeft, rect.size, androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                i == currentIndex -> {
                    drawPatternRect(rect, FillPattern.DIAGONAL, palette.strong)
                }
                else -> drawRoundRect(palette.track, rect.topLeft, rect.size, androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
            }
            x += w + gap
        }
    }
}
