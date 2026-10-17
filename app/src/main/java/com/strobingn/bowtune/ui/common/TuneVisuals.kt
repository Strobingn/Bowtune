package com.strobingn.bowtune.ui.common

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.min

private val SpineTicks = listOf(700, 600, 500, 400, 350, 300, 250, 200, 150)

@Composable
fun FocGauge(focPercent: Double?, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val mark = MaterialTheme.colorScheme.onSurface
    val band = MaterialTheme.colorScheme.outline
    Column(modifier) {
        Text("FOC on a 0–20% scale", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(top = 6.dp)
        ) {
            val top = size.height * 0.35f
            val height = size.height * 0.4f
            drawRoundRect(track, Offset(0f, top), Size(size.width, height), CornerRadius(8f, 8f))
            // 10% and 15% are the hunting window most of the copy talks about.
            val start = (10f / 20f) * size.width
            val end = (15f / 20f) * size.width
            drawRect(band.copy(alpha = 0.45f), Offset(start, top), Size(end - start, height))
            focPercent?.let { foc ->
                val x = (foc.coerceIn(0.0, 20.0) / 20.0).toFloat() * size.width
                drawLine(mark, Offset(x, 2f), Offset(x, size.height - 2f), strokeWidth = 4f)
                drawCircle(mark, radius = 6f, center = Offset(x, top + height / 2f))
            }
        }
        Text(
            "Shaded band is 10–15%. The marker is your arrow.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ArrowBalanceSketch(
    lengthIn: Double?,
    balanceFromNockIn: Double?,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.outline
    Column(modifier) {
        Text("Balance on the shaft", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            val y = size.height * 0.55f
            val left = 16.dp.toPx()
            val right = size.width - 28.dp.toPx()
            drawLine(ink, Offset(left, y), Offset(right, y), strokeWidth = 6f)
            drawCircle(ink, radius = 7f, center = Offset(left, y))
            val point = android.graphics.Path().apply {
                moveTo(right, y)
                lineTo(right + 16.dp.toPx(), y - 8.dp.toPx())
                lineTo(right + 16.dp.toPx(), y + 8.dp.toPx())
                close()
            }
            drawContext.canvas.nativeCanvas.drawPath(
                point,
                Paint().apply {
                    color = android.graphics.Color.argb(
                        (ink.alpha * 255).toInt(),
                        (ink.red * 255).toInt(),
                        (ink.green * 255).toInt(),
                        (ink.blue * 255).toInt()
                    )
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
            )
            val midX = (left + right) / 2f
            drawLine(muted, Offset(midX, y - 16.dp.toPx()), Offset(midX, y + 16.dp.toPx()), strokeWidth = 2f)
            if (lengthIn != null && lengthIn > 0 && balanceFromNockIn != null) {
                val fraction = (balanceFromNockIn / lengthIn).toFloat().coerceIn(0f, 1f)
                val bx = left + (right - left) * fraction
                drawCircle(ink, radius = 8f, center = Offset(bx, y - 18.dp.toPx()))
                drawLine(ink, Offset(bx, y - 10.dp.toPx()), Offset(bx, y + 14.dp.toPx()), strokeWidth = 3f)
            }
        }
        Text(
            "Nock at the left, point at the right. The tick is the midpoint. The dot is the balance point.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EnergyGauge(kineticFtLbs: Double?, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val mark = MaterialTheme.colorScheme.onSurface
    val band = MaterialTheme.colorScheme.outline
    Column(modifier) {
        Text("Kinetic energy, 0–80 ft-lb", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(top = 6.dp)
        ) {
            val top = size.height * 0.35f
            val height = size.height * 0.4f
            drawRoundRect(track, Offset(0f, top), Size(size.width, height), CornerRadius(8f, 8f))
            val deerStart = (40f / 80f) * size.width
            val deerEnd = (65f / 80f) * size.width
            drawRect(band.copy(alpha = 0.45f), Offset(deerStart, top), Size(deerEnd - deerStart, height))
            kineticFtLbs?.let { ke ->
                val x = (ke.coerceIn(0.0, 80.0) / 80.0).toFloat() * size.width
                drawLine(mark, Offset(x, 2f), Offset(x, size.height - 2f), strokeWidth = 4f)
                drawCircle(mark, radius = 6f, center = Offset(x, top + height / 2f))
            }
        }
        Text(
            "Shaded band is 40–65 ft-lb, a common deer range. Elk talk usually starts at the right edge.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SpineRuler(weaker: Int?, stiffer: Int?, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.outline
    val fill = MaterialTheme.colorScheme.primaryContainer
    Column(modifier) {
        Text("Easton groups on the chart", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SpineTicks.forEach { size ->
                val inside = weaker != null && stiffer != null && size in stiffer..weaker
                Text(
                    size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (inside) FontWeight.Bold else FontWeight.Normal,
                    color = if (inside) ink else muted
                )
            }
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val y = size.height / 2f
            drawLine(muted, Offset(0f, y), Offset(size.width, y), strokeWidth = 3f)
            if (weaker != null && stiffer != null) {
                val left = spineX(weaker, size.width)
                val right = spineX(stiffer, size.width)
                val start = min(left, right)
                drawRoundRect(
                    fill,
                    Offset(start, y - 7.dp.toPx()),
                    Size(kotlin.math.abs(right - left).coerceAtLeast(8f), 14.dp.toPx()),
                    CornerRadius(6f, 6f)
                )
            }
            SpineTicks.forEach { tick ->
                val x = spineX(tick, size.width)
                drawCircle(ink, radius = 3.5f, center = Offset(x, y))
            }
        }
        Text(
            "Weaker is on the left. Highlighted ticks are inside the suggested group. 340 carbon sits next to the chart's 350, and 260 sits next to 250.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun spineX(spine: Int, width: Float): Float {
    val index = SpineTicks.indexOf(spine)
    if (index >= 0) {
        if (SpineTicks.size == 1) return width / 2f
        return width * index / (SpineTicks.size - 1).toFloat()
    }
    val higher = SpineTicks.indexOfFirst { it < spine }.let { if (it <= 0) 0 else it - 1 }
    val next = (higher + 1).coerceAtMost(SpineTicks.lastIndex)
    val span = (SpineTicks[higher] - SpineTicks[next]).toFloat().coerceAtLeast(1f)
    val t = (SpineTicks[higher] - spine) / span
    val x0 = width * higher / (SpineTicks.size - 1).toFloat()
    val x1 = width * next / (SpineTicks.size - 1).toFloat()
    return x0 + (x1 - x0) * t
}

@Composable
fun GroupShapeSketch(
    points: List<com.strobingn.bowtune.data.GroupPoint>,
    aim: com.strobingn.bowtune.data.GroupPoint?,
    extremeA: Int,
    extremeB: Int,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.outline
    val field = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        drawRoundRect(field, cornerRadius = CornerRadius(12f, 12f))
        if (points.size < 2) return@Canvas
        val cx = points.map { it.x }.average()
        val cy = points.map { it.y }.average()
        var maxR = points.maxOf { kotlin.math.hypot(it.x - cx, it.y - cy) }
        if (aim != null) {
            maxR = maxOf(maxR, kotlin.math.hypot(aim.x - cx, aim.y - cy))
        }
        maxR = maxR.coerceAtLeast(1.0)
        val pad = 16.dp.toPx()
        val half = min(size.width, size.height) / 2f - pad
        fun map(x: Double, y: Double): Offset {
            val nx = ((x - cx) / maxR).toFloat()
            val ny = ((y - cy) / maxR).toFloat()
            return Offset(size.width / 2f + nx * half, size.height / 2f + ny * half)
        }
        drawCircle(muted, radius = half, center = Offset(size.width / 2f, size.height / 2f), style = Stroke(2f))
        val a = map(points[extremeA].x, points[extremeA].y)
        val b = map(points[extremeB].x, points[extremeB].y)
        drawLine(ink, a, b, strokeWidth = 3f)
        points.forEach { point ->
            drawCircle(ink, radius = 6f, center = map(point.x, point.y))
        }
        aim?.let {
            val p = map(it.x, it.y)
            val arm = 8.dp.toPx()
            drawLine(muted, Offset(p.x - arm, p.y - arm), Offset(p.x + arm, p.y + arm), strokeWidth = 2f)
            drawLine(muted, Offset(p.x - arm, p.y + arm), Offset(p.x + arm, p.y - arm), strokeWidth = 2f)
        }
    }
}
