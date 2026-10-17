package com.strobingn.bowtune.ui.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.strobingn.bowtune.data.ChartData
import com.strobingn.bowtune.data.OffsetPoint
import com.strobingn.bowtune.data.TearType
import com.strobingn.bowtune.data.TearVector
import com.strobingn.bowtune.data.WalkBackPass
import com.strobingn.bowtune.data.formatNumber
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Donut progress ring. With [segments] > 0 the ring is split into that many pieces
 * and the first [filledSegments] are solid (checklist style).
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    stroke: Dp = 10.dp,
    centerText: String = "${(progress * 100).toInt()}%",
    caption: String? = null,
    segments: Int = 0,
    filledSegments: Int = 0,
    dashedTrack: Boolean = false
) {
    val palette = rememberChartPalette()
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(
        modifier
            .size(size)
            .semantics { contentDescription = listOfNotNull(caption, centerText).joinToString(": ") },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val sw = stroke.toPx()
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            val tl = Offset(sw / 2, sw / 2)
            if (segments > 0) {
                val gapDeg = if (segments > 1) 6f else 0f
                val sweep = 360f / segments
                for (i in 0 until segments) {
                    drawArc(
                        if (i < filledSegments) palette.ink else palette.track,
                        -90f + i * sweep + gapDeg / 2,
                        sweep - gapDeg,
                        false,
                        tl,
                        arcSize,
                        style = Stroke(sw, cap = StrokeCap.Butt)
                    )
                }
            } else {
                drawArc(
                    palette.track,
                    0f,
                    360f,
                    false,
                    tl,
                    arcSize,
                    style = Stroke(sw, pathEffect = if (dashedTrack) LineStyle.DOTTED.pathEffect(this) else null)
                )
                drawArc(palette.ink, -90f, 360f * animated, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerText,
                style = if (size >= 100.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (caption != null && size >= 100.dp) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Immutable
data class GaugeBand(val from: Double, val to: Double, val label: String, val pattern: FillPattern)

/** 240° arc gauge with textured bands and a needle. */
@Composable
fun ArcGauge(
    value: Double?,
    min: Double,
    max: Double,
    bands: List<GaugeBand>,
    modifier: Modifier = Modifier,
    valueText: String = value?.let { formatNumber(it, 1) } ?: "—",
    unit: String = "",
    caption: String = ""
) {
    val palette = rememberChartPalette()
    val measurer = rememberTextMeasurer()
    val target = value?.let { ((it - min) / (max - min)).toFloat().coerceIn(0f, 1f) }
    val animated by animateFloatAsState(target ?: 0f, tween(700), label = "gauge")
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.55f)
            .semantics { contentDescription = "$caption $valueText$unit" },
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(Modifier.matchParentSize()) {
            val start = 150f
            val sweepAll = 240f
            val sw = 18.dp.toPx()
            val r = min(size.width / 2, size.height / 1.25f) - sw
            val c = Offset(size.width / 2, r + sw * 0.9f)
            val tl = Offset(c.x - r, c.y - r)
            val arc = Size(r * 2, r * 2)
            drawArc(palette.track, start, sweepAll, false, tl, arc, style = Stroke(sw))
            bands.forEachIndexed { i, b ->
                val a0 = start + sweepAll * ((b.from - min) / (max - min)).toFloat().coerceIn(0f, 1f)
                val a1 = start + sweepAll * ((b.to - min) / (max - min)).toFloat().coerceIn(0f, 1f)
                drawBandArc(c, r, sw, a0, a1, b.pattern, palette.series(i % 2 + 1))
                val mid = Math.toRadians(((a0 + a1) / 2).toDouble())
                val lr = r - sw * 1.35f
                drawChartText(
                    measurer,
                    b.label,
                    Offset(c.x + (lr * cos(mid)).toFloat(), c.y + (lr * sin(mid)).toFloat()),
                    palette.mid,
                    9f
                )
                val edge = Math.toRadians(a0.toDouble())
                if (i > 0) {
                    drawChartText(
                        measurer,
                        formatNumber(b.from, 0),
                        Offset(c.x + ((r + sw * 1.0f) * cos(edge)).toFloat(), c.y + ((r + sw * 1.0f) * sin(edge)).toFloat()),
                        palette.mid,
                        9f
                    )
                }
            }
            if (target != null) {
                val ang = Math.toRadians((start + sweepAll * animated).toDouble())
                val tip = Offset(c.x + ((r + sw * 0.4f) * cos(ang)).toFloat(), c.y + ((r + sw * 0.4f) * sin(ang)).toFloat())
                drawLine(palette.ink, c, tip, 3.dp.toPx(), StrokeCap.Round)
                drawCircle(palette.ink, 7.dp.toPx(), c)
                drawCircle(palette.surface, 3.dp.toPx(), c)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                valueText + unit,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (caption.isNotEmpty()) {
                Text(caption, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun DrawScope.drawBandArc(
    c: Offset,
    r: Float,
    sw: Float,
    a0: Float,
    a1: Float,
    pattern: FillPattern,
    color: androidx.compose.ui.graphics.Color
) {
    val tl = Offset(c.x - r, c.y - r)
    val arc = Size(r * 2, r * 2)
    when (pattern) {
        FillPattern.SOLID -> drawArc(color, a0, a1 - a0, false, tl, arc, style = Stroke(sw))
        FillPattern.OUTLINE -> Unit
        else -> {
            drawArc(color.copy(alpha = 0.18f), a0, a1 - a0, false, tl, arc, style = Stroke(sw))
            val stepDeg = when (pattern) {
                FillPattern.DOTS -> 4f
                FillPattern.CROSSHATCH -> 3f
                else -> 5f
            }
            var a = a0 + stepDeg / 2
            while (a < a1) {
                val rad = Math.toRadians(a.toDouble())
                val inner = Offset(c.x + ((r - sw / 2) * cos(rad)).toFloat(), c.y + ((r - sw / 2) * sin(rad)).toFloat())
                val outer = Offset(c.x + ((r + sw / 2) * cos(rad)).toFloat(), c.y + ((r + sw / 2) * sin(rad)).toFloat())
                if (pattern == FillPattern.DOTS) {
                    drawCircle(color, 1.4.dp.toPx(), Offset((inner.x + outer.x) / 2, (inner.y + outer.y) / 2))
                } else {
                    drawLine(color, inner, outer, 1.4.dp.toPx())
                }
                a += stepDeg
            }
        }
    }
    val e0 = Math.toRadians(a0.toDouble())
    drawLine(
        color,
        Offset(c.x + ((r - sw / 2) * cos(e0)).toFloat(), c.y + ((r - sw / 2) * sin(e0)).toFloat()),
        Offset(c.x + ((r + sw / 2) * cos(e0)).toFloat(), c.y + ((r + sw / 2) * sin(e0)).toFloat()),
        1.dp.toPx()
    )
}

/**
 * Horizontal scale with a highlighted range, e.g. spine band. With [reversed] the high
 * numbers sit on the left (weaker → stiffer reads left → right).
 */
@Composable
fun RangeScale(
    min: Double,
    max: Double,
    low: Double?,
    high: Double?,
    modifier: Modifier = Modifier,
    reversed: Boolean = false,
    marker: Double? = null,
    leftCaption: String = "",
    rightCaption: String = ""
) {
    val palette = rememberChartPalette()
    val measurer = rememberTextMeasurer()
    val ticks = niceTicks(min, max, 6)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        val pad = 14.dp.toPx()
        val w = size.width - pad * 2
        val trackY = 30.dp.toPx()
        val trackH = 14.dp.toPx()
        fun px(v: Double): Float {
            val f = ((v - min) / (max - min)).toFloat().coerceIn(0f, 1f)
            return pad + (if (reversed) 1f - f else f) * w
        }
        drawRoundRect(
            palette.track,
            Offset(pad, trackY),
            Size(w, trackH),
            androidx.compose.ui.geometry.CornerRadius(trackH / 2)
        )
        if (low != null && high != null) {
            val x0 = min(px(low), px(high))
            val x1 = max(px(low), px(high))
            drawPatternRect(Rect(x0, trackY - 4.dp.toPx(), x1, trackY + trackH + 4.dp.toPx()), FillPattern.DIAGONAL, palette.ink)
            drawChartText(measurer, formatNumber(high, 0), Offset(px(high), trackY - 6.dp.toPx()), palette.ink, 10f, 0.5f, 1f, bold = true)
            drawChartText(measurer, formatNumber(low, 0), Offset(px(low), trackY - 6.dp.toPx()), palette.ink, 10f, 0.5f, 1f, bold = true)
        }
        ticks.forEach { t ->
            val x = px(t)
            drawLine(palette.mid, Offset(x, trackY + trackH + 6.dp.toPx()), Offset(x, trackY + trackH + 10.dp.toPx()), 1.dp.toPx())
            drawChartText(measurer, formatNumber(t, 0), Offset(x, trackY + trackH + 12.dp.toPx()), palette.mid, 9f, 0.5f, 0f)
        }
        marker?.let {
            val x = px(it)
            val p = Path().apply {
                moveTo(x, trackY + trackH)
                lineTo(x - 6.dp.toPx(), trackY + trackH + 9.dp.toPx())
                lineTo(x + 6.dp.toPx(), trackY + trackH + 9.dp.toPx())
                close()
            }
            drawPath(p, palette.ink, style = Fill)
        }
        drawChartText(measurer, leftCaption, Offset(pad, 0f), palette.strong, 9f, 0f, 0f)
        drawChartText(measurer, rightCaption, Offset(size.width - pad, 0f), palette.strong, 9f, 1f, 0f)
    }
}

@Immutable
data class TearPlotPoint(val vector: TearVector, val recency: Float, val latest: Boolean)

/**
 * Polar plot of paper-tear vectors (nock direction × length). Rings every 0.5".
 * Older tears fade; tears without a logged length use a hollow marker at the default radius.
 */
@Composable
fun PolarTearPlot(
    points: List<TearPlotPoint>,
    modifier: Modifier = Modifier,
    maxRadiusIn: Double = 2.0
) {
    val palette = rememberChartPalette()
    val measurer = rememberTextMeasurer()
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = "Polar plot of ${points.size} paper tears" }
    ) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = min(size.width, size.height) / 2 - 22.dp.toPx()
        val rings = (1..(maxRadiusIn / 0.5).toInt()).map { it * 0.5 }
        rings.forEach { ring ->
            val rr = (ring / maxRadiusIn).toFloat() * r
            drawCircle(
                palette.soft,
                rr,
                c,
                style = Stroke(1.dp.toPx(), pathEffect = if (ring % 1.0 == 0.0) null else LineStyle.DOTTED.pathEffect(this))
            )
            drawChartText(measurer, formatNumber(ring, 1) + "\"", Offset(c.x + rr * 0.72f + 2.dp.toPx(), c.y - rr * 0.72f), palette.mid, 8f, 0f, 1f)
        }
        for (deg in 0 until 360 step 45) {
            val rad = Math.toRadians(deg.toDouble())
            drawLine(
                palette.faint,
                c,
                Offset(c.x + (r * cos(rad)).toFloat(), c.y + (r * sin(rad)).toFloat()),
                1.dp.toPx(),
                pathEffect = if (deg % 90 == 0) null else LineStyle.DOTTED.pathEffect(this)
            )
        }
        drawChartText(measurer, "NOCK HIGH", Offset(c.x, 2.dp.toPx()), palette.strong, 9f, 0.5f, 0f, bold = true)
        drawChartText(measurer, "NOCK LOW", Offset(c.x, size.height - 2.dp.toPx()), palette.strong, 9f, 0.5f, 1f, bold = true)
        drawChartText(measurer, "LEFT", Offset(2.dp.toPx(), c.y), palette.strong, 9f, 0f, 0.5f, bold = true)
        drawChartText(measurer, "RIGHT", Offset(size.width - 2.dp.toPx(), c.y), palette.strong, 9f, 1f, 0.5f, bold = true)

        fun toPx(v: TearVector): Offset {
            val len = v.length
            val scale = if (len > maxRadiusIn) maxRadiusIn / len else 1.0
            return Offset(
                c.x + (v.x * scale / maxRadiusIn).toFloat() * r,
                c.y - (v.y * scale / maxRadiusIn).toFloat() * r
            )
        }
        points.sortedBy { it.recency }.forEach { p ->
            val pos = toPx(p.vector)
            val alpha = 0.25f + 0.75f * p.recency
            val color = palette.ink.copy(alpha = alpha)
            val radius = (3f + 2.5f * p.recency).dp.toPx()
            if (p.latest) {
                if (p.vector.length > 0.01) {
                    drawArrow(c, pos, palette.ink, 2.4.dp.toPx())
                }
                drawCircle(palette.ink, radius + 4.dp.toPx(), pos, style = Stroke(1.5.dp.toPx()))
            }
            drawMarker(
                if (p.vector.magnitudeKnown) MarkerShape.CIRCLE else MarkerShape.RING,
                pos,
                radius,
                color,
                palette.surface
            )
        }
        drawCircle(palette.ink, 2.5.dp.toPx(), c)
    }
}

private fun DrawScope.drawArrow(from: Offset, to: Offset, color: androidx.compose.ui.graphics.Color, width: Float) {
    drawLine(color, from, to, width, StrokeCap.Round)
    val ang = atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())
    val head = 9.dp.toPx()
    listOf(ang + Math.PI * 0.82, ang - Math.PI * 0.82).forEach { a ->
        drawLine(color, to, Offset(to.x + (head * cos(a)).toFloat(), to.y + (head * sin(a)).toFloat()), width, StrokeCap.Round)
    }
}

/**
 * A single paper tear drawn as it appears on paper: round point hole with the tear
 * tail running toward the nock direction and three vane slits at its end.
 */
@Composable
fun TearGlyph(
    type: TearType,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    magnitudeIn: Double? = null,
    emphasized: Boolean = false
) {
    val palette = rememberChartPalette()
    Canvas(modifier.size(size)) {
        drawTearGlyph(type, magnitudeIn, if (emphasized) palette.ink else palette.strong, palette.soft)
    }
}

fun DrawScope.drawTearGlyph(
    type: TearType,
    magnitudeIn: Double?,
    ink: androidx.compose.ui.graphics.Color,
    paperEdge: androidx.compose.ui.graphics.Color
) {
    val c = Offset(size.width / 2, size.height / 2)
    val unit = min(size.width, size.height) / 2
    drawCircle(paperEdge, unit * 0.92f, c, style = Stroke(1.dp.toPx(), pathEffect = LineStyle.DOTTED.pathEffect(this)))
    val v = ChartData.tearVector(type, magnitudeIn)
    val holeR = unit * 0.17f
    if (v.length < 0.01) {
        drawCircle(ink, holeR * 1.25f, c)
        listOf(90.0, 210.0, 330.0).forEach { deg ->
            val rad = Math.toRadians(deg)
            drawLine(
                ink,
                Offset(c.x + (holeR * cos(rad)).toFloat(), c.y - (holeR * sin(rad)).toFloat()),
                Offset(c.x + (holeR * 2.4f * cos(rad)).toFloat(), c.y - (holeR * 2.4f * sin(rad)).toFloat()),
                2.dp.toPx(),
                StrokeCap.Round
            )
        }
        return
    }
    val lenFrac = (0.42f + 0.35f * ((magnitudeIn ?: ChartData.DEFAULT_TEAR_MAGNITUDE_IN) / 2.0).toFloat()).coerceIn(0.42f, 0.78f)
    val dir = Offset((v.x / v.length).toFloat(), (-v.y / v.length).toFloat())
    val end = Offset(c.x + dir.x * unit * lenFrac, c.y + dir.y * unit * lenFrac)
    val perp = Offset(-dir.y, dir.x)
    val tail = Path().apply {
        moveTo(c.x + perp.x * holeR * 0.9f, c.y + perp.y * holeR * 0.9f)
        lineTo(end.x + perp.x * holeR * 0.35f, end.y + perp.y * holeR * 0.35f)
        lineTo(end.x - perp.x * holeR * 0.35f, end.y - perp.y * holeR * 0.35f)
        lineTo(c.x - perp.x * holeR * 0.9f, c.y - perp.y * holeR * 0.9f)
        close()
    }
    drawPath(tail, ink.copy(alpha = 0.55f), style = Fill)
    drawCircle(ink, holeR * 1.25f, c)
    val vaneLen = holeR * 1.6f
    listOf(0.0, 2.0944, -2.0944).forEach { a ->
        val ca = cos(a).toFloat()
        val sa = sin(a).toFloat()
        val d = Offset(perp.x * ca + dir.x * sa, perp.y * ca + dir.y * sa)
        drawLine(ink, end, Offset(end.x + d.x * vaneLen, end.y + d.y * vaneLen), 2.dp.toPx(), StrokeCap.Round)
    }
}

/**
 * Walk-back target: the vertical tape line down the middle, impacts stacked by distance
 * (near at top, far at bottom). Pass 1 solid / circles, pass 2 dashed / squares.
 */
@Composable
fun WalkBackPlot(
    passes: List<WalkBackPass>,
    modifier: Modifier = Modifier,
    distances: List<Int> = listOf(10, 20, 30, 40)
) {
    if (passes.isEmpty() || passes.all { it.points.isEmpty() }) {
        ChartEmpty("Log walk-back offsets at 10 / 20 / 30 yd to see the line.", modifier)
        return
    }
    val palette = rememberChartPalette()
    val measurer = rememberTextMeasurer()
    val used = (passes.flatMap { p -> p.points.map { it.first } } + distances.take(3)).distinct().sorted()
    val maxOff = max(2.0, passes.flatMap { p -> p.points.map { abs(it.second) } }.maxOrNull() ?: 0.0) * 1.2
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .semantics { contentDescription = "Walk-back plot with ${passes.size} passes" }
        ) {
            val left = 44.dp.toPx()
            val right = 12.dp.toPx()
            val top = 14.dp.toPx()
            val bottom = 20.dp.toPx()
            val w = size.width - left - right
            val h = size.height - top - bottom
            val cx = left + w / 2
            fun px(off: Double) = cx + (off / maxOff).toFloat() * (w / 2)
            fun py(yd: Int): Float {
                val i = used.indexOf(yd).coerceAtLeast(0)
                return if (used.size <= 1) top + h / 2 else top + h * i / (used.size - 1)
            }
            drawRoundRect(palette.track, Offset(left, top - 8.dp.toPx()), Size(w, h + 16.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()))
            listOf(0.75, -0.75).forEach { tol ->
                drawLine(palette.soft, Offset(px(tol), top - 8.dp.toPx()), Offset(px(tol), top + h + 8.dp.toPx()), 1.dp.toPx(), pathEffect = LineStyle.DOTTED.pathEffect(this))
            }
            drawLine(palette.ink, Offset(cx, top - 8.dp.toPx()), Offset(cx, top + h + 8.dp.toPx()), 2.dp.toPx())
            used.forEach { yd ->
                drawChartText(measurer, "$yd yd", Offset(left - 6.dp.toPx(), py(yd)), palette.strong, 10f, 1f, 0.5f)
                drawLine(palette.faint, Offset(left, py(yd)), Offset(left + w, py(yd)), 1.dp.toPx())
            }
            drawChartText(measurer, "← left", Offset(left + 4.dp.toPx(), size.height), palette.mid, 9f, 0f, 1f)
            drawChartText(measurer, "right →", Offset(left + w - 4.dp.toPx(), size.height), palette.mid, 9f, 1f, 1f)
            drawChartText(measurer, "±¾\"", Offset(px(0.75) + 3.dp.toPx(), top - 6.dp.toPx()), palette.mid, 8f, 0f, 0f)
            passes.forEachIndexed { i, pass ->
                val style = seriesLineStyles[i % seriesLineStyles.size]
                val marker = seriesMarkers[i % seriesMarkers.size]
                val color = palette.series(i)
                val pts = pass.points.sortedBy { it.first }
                if (pts.size > 1) {
                    val path = Path()
                    pts.forEachIndexed { idx, (yd, off) ->
                        if (idx == 0) path.moveTo(px(off), py(yd)) else path.lineTo(px(off), py(yd))
                    }
                    drawPath(path, color, style = Stroke(2.dp.toPx(), pathEffect = style.pathEffect(this)))
                }
                pts.forEach { (yd, off) ->
                    drawMarker(marker, Offset(px(off), py(yd)), 5.dp.toPx(), color, palette.surface)
                }
            }
        }
        ChartLegend(
            passes.mapIndexed { i, p ->
                LegendItem(p.label, palette.series(i), seriesLineStyles[i % 4], seriesMarkers[i % 4])
            } + LegendItem("±¾\" tolerance", palette.soft, LineStyle.DOTTED)
        )
    }
}

/**
 * Bare-shaft / broadhead impact offsets relative to the fletched (field-point) group at
 * the center. Markers: bare = circle, broadhead = triangle, LIFT wrap-up = square.
 */
@Composable
fun OffsetScatter(
    points: List<OffsetPoint>,
    modifier: Modifier = Modifier,
    centerLabel: String = "Fletched group"
) {
    if (points.isEmpty()) {
        ChartEmpty("Log bare-shaft or broadhead offsets to see impacts vs your fletched group.", modifier)
        return
    }
    val palette = rememberChartPalette()
    val measurer = rememberTextMeasurer()
    val maxR = max(3.0, points.maxOf { hypot(it.x, it.y) } * 1.15)
    val sorted = points.sortedBy { it.epochMs }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .semantics { contentDescription = "Impact offsets for ${points.size} logged groups" }
        ) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = min(size.width, size.height) / 2 - 16.dp.toPx()
            fun toPx(x: Double, y: Double) = Offset(c.x + (x / maxR).toFloat() * r, c.y - (y / maxR).toFloat() * r)
            val ringStep = if (maxR > 8) 4.0 else if (maxR > 4) 2.0 else 1.0
            var ring = ringStep
            while (ring <= maxR) {
                val rr = (ring / maxR).toFloat() * r
                drawCircle(palette.soft, rr, c, style = Stroke(1.dp.toPx()))
                drawChartText(measurer, formatNumber(ring, 0) + "\"", Offset(c.x + rr + 2.dp.toPx(), c.y - 2.dp.toPx()), palette.mid, 8f, 0f, 1f)
                ring += ringStep
            }
            drawLine(palette.faint, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1.dp.toPx())
            drawLine(palette.faint, Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1.dp.toPx())
            drawCircle(palette.ink, 6.dp.toPx(), c, style = Stroke(2.dp.toPx()))
            drawCircle(palette.ink, 2.dp.toPx(), c)
            drawChartText(measurer, "HIGH", Offset(c.x, 0f), palette.strong, 9f, 0.5f, 0f, bold = true)
            drawChartText(measurer, "LOW", Offset(c.x, size.height), palette.strong, 9f, 0.5f, 1f, bold = true)
            drawChartText(measurer, "L", Offset(c.x - r - 10.dp.toPx(), c.y), palette.strong, 9f, 0f, 0.5f, bold = true)
            drawChartText(measurer, "R", Offset(c.x + r + 10.dp.toPx(), c.y), palette.strong, 9f, 1f, 0.5f, bold = true)
            sorted.forEachIndexed { i, p ->
                val recency = if (sorted.size == 1) 1f else i / (sorted.size - 1f)
                val marker = when (p.source) {
                    ChartData.SOURCE_BROADHEAD -> MarkerShape.TRIANGLE
                    ChartData.SOURCE_LIFT -> MarkerShape.SQUARE
                    else -> MarkerShape.CIRCLE
                }
                drawMarker(marker, toPx(p.x, p.y), (3.5f + 2f * recency).dp.toPx(), palette.ink.copy(alpha = 0.3f + 0.7f * recency), palette.surface)
            }
        }
        ChartLegend(
            listOf(
                LegendItem(centerLabel, palette.ink, marker = MarkerShape.RING),
                LegendItem("Bare shaft", palette.ink, marker = MarkerShape.CIRCLE),
                LegendItem("Broadhead", palette.ink, marker = MarkerShape.TRIANGLE),
                LegendItem("LIFT wrap-up", palette.ink, marker = MarkerShape.SQUARE)
            )
        )
        Text(
            "Newer impacts are darker and larger.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
