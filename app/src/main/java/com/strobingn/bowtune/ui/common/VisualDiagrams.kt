package com.strobingn.bowtune.ui.common

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strobingn.bowtune.data.EnergyResult
import com.strobingn.bowtune.data.FocResult
import com.strobingn.bowtune.data.GroupCalculator
import com.strobingn.bowtune.data.SpineResult
import com.strobingn.bowtune.data.TuneSession
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interactive grayscale schematic of an arrow showing Nock, Vanes, Shaft, Insert, Point,
 * Geometric Center (L/2), Balance Fulcrum, and FOC % Zone Spectrum.
 */
@Composable
fun ArrowFocDiagram(
    lengthIn: Double?,
    result: FocResult,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    val inkArgb = android.graphics.Color.argb(
        (ink.alpha * 255).toInt(),
        (ink.red * 255).toInt(),
        (ink.green * 255).toInt(),
        (ink.blue * 255).toInt()
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Arrow Balance & FOC Visualizer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                result.focPercent?.let { foc ->
                    Text(
                        "${if (result.isEstimatedFromComponents) "~" else ""}${fmt1(foc)}% FOC",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
            ) {
                val w = size.width
                val h = size.height
                val leftX = 28f
                val rightX = w - 28f
                val shaftLenPx = (rightX - leftX).coerceAtLeast(100f)
                val shaftY = 38f

                val textPaint = Paint().apply {
                    color = inkArgb
                    textAlign = Paint.Align.CENTER
                    textSize = 21f
                    isAntiAlias = true
                }
                val boldPaint = Paint().apply {
                    color = inkArgb
                    textAlign = Paint.Align.CENTER
                    textSize = 22f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val native = drawContext.canvas.nativeCanvas

                // 1. Nock (left end)
                drawLine(
                    color = ink,
                    start = Offset(leftX - 10f, shaftY - 6f),
                    end = Offset(leftX, shaftY),
                    strokeWidth = 4f
                )
                drawLine(
                    color = ink,
                    start = Offset(leftX - 10f, shaftY + 6f),
                    end = Offset(leftX, shaftY),
                    strokeWidth = 4f
                )

                // 2. Vanes near nock end
                val vanePath = Path().apply {
                    moveTo(leftX + 14f, shaftY - 3f)
                    lineTo(leftX + 32f, shaftY - 16f)
                    lineTo(leftX + 68f, shaftY - 14f)
                    lineTo(leftX + 74f, shaftY - 3f)
                    close()
                }
                drawPath(vanePath, color = muted.copy(alpha = 0.65f))
                drawPath(vanePath, color = ink, style = Stroke(width = 2f))

                // 3. Shaft body
                drawRect(
                    color = ink,
                    topLeft = Offset(leftX, shaftY - 3.5f),
                    size = Size(shaftLenPx - 18f, 7f)
                )

                // 4. Insert + Point (right end)
                val pointPath = Path().apply {
                    moveTo(rightX - 18f, shaftY - 7f)
                    lineTo(rightX + 8f, shaftY)
                    lineTo(rightX - 18f, shaftY + 7f)
                    close()
                }
                drawPath(pointPath, color = fill)

                // 5. Geometric Midpoint (L/2) dashed line
                val midX = leftX + shaftLenPx * 0.5f
                val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                drawLine(
                    color = muted,
                    start = Offset(midX, shaftY - 18f),
                    end = Offset(midX, shaftY + 26f),
                    strokeWidth = 2f,
                    pathEffect = dash
                )
                native.drawText("L/2", midX, shaftY - 22f, textPaint)

                // 6. Balance Point Fulcrum Triangle
                val balRatio = if (lengthIn != null && lengthIn > 0 && result.balancePointIn != null) {
                    (result.balancePointIn / lengthIn).toFloat().coerceIn(0.1f, 0.92f)
                } else {
                    0.62f
                }
                val balX = leftX + shaftLenPx * balRatio
                val tri = Path().apply {
                    moveTo(balX, shaftY + 5f)
                    lineTo(balX - 12f, shaftY + 25f)
                    lineTo(balX + 12f, shaftY + 25f)
                    close()
                }
                drawPath(tri, color = fill)
                val balLabel = result.balancePointIn?.let { "Bal ${fmt1(it)}\"" } ?: "Balance pt"
                native.drawText(balLabel, balX, shaftY + 46f, boldPaint)

                // 7. FOC % Zone Spectrum Bar at bottom (0% to 24% scale)
                val barTop = h - 30f
                val barH = 12f
                drawRoundRect(
                    color = muted.copy(alpha = 0.28f),
                    topLeft = Offset(leftX, barTop),
                    size = Size(shaftLenPx, barH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Highlight Hunting Band (11% to 16% out of 24%)
                val huntStart = leftX + shaftLenPx * (11f / 24f)
                val huntWidth = shaftLenPx * (5f / 24f)
                drawRect(
                    color = fill.copy(alpha = 0.35f),
                    topLeft = Offset(huntStart, barTop),
                    size = Size(huntWidth, barH)
                )
                native.drawText("Target 7–11%", leftX + shaftLenPx * 0.32f, barTop - 5f, textPaint)
                native.drawText("Hunt 11–16%", leftX + shaftLenPx * 0.58f, barTop - 5f, textPaint)
                native.drawText("Ext 16%+", leftX + shaftLenPx * 0.85f, barTop - 5f, textPaint)

                result.focPercent?.let { foc ->
                    val frac = (foc.toFloat() / 24f).coerceIn(0f, 1f)
                    val markerX = leftX + shaftLenPx * frac
                    drawCircle(color = ink, radius = 8f, center = Offset(markerX, barTop + barH / 2f))
                }
            }

            if (result.totalGrains != null) {
                val parts = listOfNotNull(
                    result.rearGrains?.let { "Rear ${fmt1(it)} gr" },
                    result.shaftGrainsResolved?.let { "Shaft ${fmt1(it)} gr" },
                    result.frontGrains?.let { "Front ${fmt1(it)} gr" }
                )
                Text(
                    "Total: ${fmt1(result.totalGrains)} gr" +
                        if (parts.isNotEmpty()) " (${parts.joinToString(" · ")})" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }
        }
    }
}

/**
 * Visual Spine Stiffness Spectrum Bar (700 Weak -> 150 Stiff) showing where the suggested
 * spine band sits and the effective draw weight load.
 */
@Composable
fun SpineSpectrumDiagram(
    result: SpineResult,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    val inkArgb = android.graphics.Color.argb(
        (ink.alpha * 255).toInt(),
        (ink.red * 255).toInt(),
        (ink.green * 255).toInt(),
        (ink.blue * 255).toInt()
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Static Spine Stiffness Spectrum",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                result.effectiveDrawWeightLbs?.let { eff ->
                    Text(
                        "Eff. load ~${fmt1(eff)} lb",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
            ) {
                val w = size.width
                val leftX = 24f
                val rightX = w - 24f
                val span = (rightX - leftX).coerceAtLeast(100f)
                val barY = 24f
                val barH = 18f

                // Map spine 700 (left, weak) to 150 (right, stiff)
                fun spineToX(spine: Int): Float {
                    val frac = ((700f - spine.coerceIn(150, 700)) / (700f - 150f)).coerceIn(0f, 1f)
                    return leftX + span * frac
                }

                drawRoundRect(
                    color = muted.copy(alpha = 0.25f),
                    topLeft = Offset(leftX, barY),
                    size = Size(span, barH),
                    cornerRadius = CornerRadius(9f, 9f)
                )

                val xWeak = spineToX(result.weakSpine)
                val xStiff = spineToX(result.stiffSpine)
                val winLeft = min(xWeak, xStiff)
                val winWidth = kotlin.math.abs(xStiff - xWeak).coerceAtLeast(22f)

                drawRoundRect(
                    color = fill,
                    topLeft = Offset(winLeft, barY - 3f),
                    size = Size(winWidth, barH + 6f),
                    cornerRadius = CornerRadius(9f, 9f)
                )

                val tickPaint = Paint().apply {
                    color = inkArgb
                    textAlign = Paint.Align.CENTER
                    textSize = 20f
                    isAntiAlias = true
                }
                val native = drawContext.canvas.nativeCanvas
                listOf(700, 500, 400, 340, 300, 250, 200).forEach { sp ->
                    val tx = spineToX(sp)
                    drawLine(
                        color = ink,
                        start = Offset(tx, barY + barH),
                        end = Offset(tx, barY + barH + 6f),
                        strokeWidth = 2f
                    )
                    native.drawText(sp.toString(), tx, barY + barH + 24f, tickPaint)
                }
                native.drawText("Weaker", leftX + 26f, barY - 6f, tickPaint)
                native.drawText("Stiffer", rightX - 24f, barY - 6f, tickPaint)
            }
        }
    }
}

/**
 * Dual horizontal gauges for Kinetic Energy (ft-lb) and Momentum (slug·ft/s).
 */
@Composable
fun BallisticEnergyGauge(
    result: EnergyResult,
    modifier: Modifier = Modifier
) {
    val ke = result.kineticFtLbs ?: return
    val mom = result.momentumSlugFtS ?: return
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    val inkArgb = android.graphics.Color.argb(
        (ink.alpha * 255).toInt(),
        (ink.red * 255).toInt(),
        (ink.green * 255).toInt(),
        (ink.blue * 255).toInt()
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Ballistic Energy & Momentum Spectrum",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
            ) {
                val w = size.width
                val leftX = 20f
                val rightX = w - 20f
                val span = (rightX - leftX).coerceAtLeast(100f)

                val p = Paint().apply {
                    color = inkArgb
                    textSize = 20f
                    isAntiAlias = true
                }
                val native = drawContext.canvas.nativeCanvas

                // 1. KE bar (0..90 ft-lb)
                val keY = 22f
                val barH = 14f
                val keFrac = (ke.toFloat() / 90f).coerceIn(0f, 1f)
                native.drawText(
                    "Kinetic Energy: ${fmt1(ke)} ft-lb (Small <25 · Deer 25–42 · Elk 42–65 · Heavy 65+)",
                    leftX,
                    keY - 6f,
                    p
                )
                drawRoundRect(
                    color = muted.copy(alpha = 0.25f),
                    topLeft = Offset(leftX, keY),
                    size = Size(span, barH),
                    cornerRadius = CornerRadius(7f, 7f)
                )
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(leftX, keY),
                    size = Size(span * keFrac, barH),
                    cornerRadius = CornerRadius(7f, 7f)
                )
                listOf(25f, 42f, 65f).forEach { threshold ->
                    val tx = leftX + span * (threshold / 90f)
                    drawLine(ink, Offset(tx, keY - 2f), Offset(tx, keY + barH + 2f), strokeWidth = 2.5f)
                }

                // 2. Momentum bar (0..0.85 slug·ft/s)
                val momY = 74f
                val momFrac = (mom.toFloat() / 0.85f).coerceIn(0f, 1f)
                native.drawText(
                    "Momentum: ${fmt2(mom)} slug·ft/s${result.grainsPerPound?.let { " · ${fmt1(it)} gr/lb" }.orEmpty()}",
                    leftX,
                    momY - 6f,
                    p
                )
                drawRoundRect(
                    color = muted.copy(alpha = 0.25f),
                    topLeft = Offset(leftX, momY),
                    size = Size(span, barH),
                    cornerRadius = CornerRadius(7f, 7f)
                )
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(leftX, momY),
                    size = Size(span * momFrac, barH),
                    cornerRadius = CornerRadius(7f, 7f)
                )
            }
        }
    }
}

/**
 * Trend chart plotting recorded group sizes (in inches) across recent sessions,
 * plus a shortcut to open the Shot Vision Arrow Group camera analyzer.
 */
@Composable
fun GroupTrendChartCard(
    sessions: List<TuneSession>,
    onOpenGroupCamera: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Parse chronological group sizes (oldest -> newest, up to 10)
    val points = sessions
        .mapNotNull { s ->
            val inches = GroupCalculator.parseGroupSizeInches(s.scoreOrGroup) ?: return@mapNotNull null
            Triple(s.distanceYd.ifBlank { "20" }, inches, s.dateEpochMs)
        }
        .take(10)
        .reversed()

    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    val inkArgb = android.graphics.Color.argb(
        (ink.alpha * 255).toInt(),
        (ink.red * 255).toInt(),
        (ink.green * 255).toInt(),
        (ink.blue * 255).toInt()
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Arrow Group Size Tracker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (points.isNotEmpty()) {
                        val best = points.minOf { it.second }
                        val avg = points.map { it.second }.average()
                        Text(
                            "Best: ${fmt2(best)}\" · Avg: ${fmt2(avg)}\" (${points.size} logged groups)",
                            style = MaterialTheme.typography.bodySmall,
                            color = muted
                        )
                    } else {
                        Text(
                            "Capture & measure target arrow groups with the camera or log group sizes in inches.",
                            style = MaterialTheme.typography.bodySmall,
                            color = muted
                        )
                    }
                }
                if (onOpenGroupCamera != null) {
                    OutlinedButton(onClick = onOpenGroupCamera) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Measure group")
                    }
                }
            }

            if (points.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val padL = 28f
                    val padR = 28f
                    val padTop = 24f
                    val padBot = 24f
                    val plotW = (w - padL - padR).coerceAtLeast(60f)
                    val plotH = (h - padTop - padBot).coerceAtLeast(35f)

                    val maxIn = (points.maxOf { it.second }.coerceAtLeast(2.0) * 1.15).toFloat()
                    val minIn = 0f

                    val labelPaint = Paint().apply {
                        color = inkArgb
                        textAlign = Paint.Align.CENTER
                        textSize = 20f
                        isAntiAlias = true
                    }
                    val native = drawContext.canvas.nativeCanvas

                    // Baseline
                    drawLine(
                        color = muted.copy(alpha = 0.4f),
                        start = Offset(padL, padTop + plotH),
                        end = Offset(w - padR, padTop + plotH),
                        strokeWidth = 2f
                    )

                    val coords = points.mapIndexed { idx, (_, inches, _) ->
                        val x = if (points.size == 1) {
                            padL + plotW / 2f
                        } else {
                            padL + plotW * (idx.toFloat() / (points.size - 1).toFloat())
                        }
                        val frac = ((inches.toFloat() - minIn) / (maxIn - minIn)).coerceIn(0f, 1f)
                        val y = padTop + plotH * (1f - frac)
                        Offset(x, y)
                    }

                    for (i in 0 until coords.size - 1) {
                        drawLine(
                            color = fill,
                            start = coords[i],
                            end = coords[i + 1],
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }

                    coords.forEachIndexed { idx, pt ->
                        val (distYd, inches, _) = points[idx]
                        drawCircle(color = fill, radius = 7f, center = pt)
                        native.drawText("${fmt2(inches)}\"", pt.x, pt.y - 10f, labelPaint)
                        native.drawText("${distYd}yd", pt.x, padTop + plotH + 19f, labelPaint)
                    }
                }
            }
        }
    }
}

/**
 * 2D Target Plot for Bare Shaft vs Fletched (or Broadhead vs Field Point) showing the
 * tolerance zone and the impact offset vector.
 */
@Composable
fun BareAndBroadheadTargetPlot(
    verticalHighIn: Double?,
    horizontalLeftIn: Double?,
    toleranceIn: Double,
    centerLabel: String,
    impactLabel: String,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    val inkArgb = android.graphics.Color.argb(
        (ink.alpha * 255).toInt(),
        (ink.red * 255).toInt(),
        (ink.green * 255).toInt(),
        (ink.blue * 255).toInt()
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val maxScaleIn = 3.5f
        val radiusPx = min(w, h) * 0.42f
        val pxPerIn = radiusPx / maxScaleIn

        // Crosshairs
        drawLine(muted.copy(alpha = 0.4f), Offset(cx - radiusPx, cy), Offset(cx + radiusPx, cy), strokeWidth = 1.5f)
        drawLine(muted.copy(alpha = 0.4f), Offset(cx, cy - radiusPx), Offset(cx, cy + radiusPx), strokeWidth = 1.5f)

        // Outer target ring (3.5")
        drawCircle(muted.copy(alpha = 0.35f), radius = radiusPx, center = Offset(cx, cy), style = Stroke(width = 2f))

        // Tolerance ring (e.g., 0.75" for bare shaft, 1.0" for broadhead)
        val tolPx = (toleranceIn.toFloat() * pxPerIn).coerceAtMost(radiusPx)
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        drawCircle(ink.copy(alpha = 0.75f), radius = tolPx, center = Offset(cx, cy), style = Stroke(width = 2.5f, pathEffect = dash))

        // Reference center (Fletched / Field point)
        drawCircle(muted, radius = 7f, center = Offset(cx, cy))

        val vIn = (verticalHighIn ?: 0.0).toFloat().coerceIn(-maxScaleIn, maxScaleIn)
        val lIn = (horizontalLeftIn ?: 0.0).toFloat().coerceIn(-maxScaleIn, maxScaleIn)
        // +horizontalLeftIn is LEFT of center (-X on target); +verticalHighIn is HIGH (-Y on screen)
        val impX = cx - lIn * pxPerIn
        val impY = cy - vIn * pxPerIn

        if (verticalHighIn != null || horizontalLeftIn != null) {
            drawLine(fill, Offset(cx, cy), Offset(impX, impY), strokeWidth = 4f, cap = StrokeCap.Round)
            drawCircle(fill, radius = 9f, center = Offset(impX, impY))
        }

        val p = Paint().apply {
            color = inkArgb
            textSize = 20f
            isAntiAlias = true
        }
        val native = drawContext.canvas.nativeCanvas
        native.drawText("Center = $centerLabel (tol ±${toleranceIn}\")", 12f, 22f, p)
        if (verticalHighIn != null || horizontalLeftIn != null) {
            native.drawText("● $impactLabel", 12f, h - 10f, p)
        }
    }
}

/**
 * Walk-back Plumb Line visual diagram showing Fan (rest error) vs Parallel (sight windage) vs Stacked.
 */
@Composable
fun WalkBackPatternDiagram(
    errorGrowsWithDistance: Boolean?,
    wholeLineOffset: Boolean?,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    val inkArgb = android.graphics.Color.argb(
        (ink.alpha * 255).toInt(),
        (ink.red * 255).toInt(),
        (ink.green * 255).toInt(),
        (ink.blue * 255).toInt()
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(125.dp)
    ) {
        val w = size.width
        val h = size.height
        val plumbX = w * 0.45f
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

        // Plumb line
        drawLine(
            color = muted,
            start = Offset(plumbX, 12f),
            end = Offset(plumbX, h - 12f),
            strokeWidth = 2.5f,
            pathEffect = dash
        )

        val distances = listOf("10 yd" to 28f, "20 yd" to 64f, "30 yd" to 100f)
        val p = Paint().apply {
            color = inkArgb
            textSize = 20f
            isAntiAlias = true
        }
        val native = drawContext.canvas.nativeCanvas
        native.drawText("Plumb line", plumbX - 95f, 24f, p)

        distances.forEachIndexed { idx, (label, y) ->
            val offsetX = when {
                errorGrowsWithDistance == true -> (idx + 1) * 24f // Fanning out with distance
                wholeLineOffset == true -> 36f // Parallel offset
                else -> 0f // Stacked on the line
            }
            val pt = Offset(plumbX + offsetX, y)
            drawCircle(fill, radius = 7.5f, center = pt)
            native.drawText(label, pt.x + 16f, y + 6f, p)
        }
    }
}

/**
 * Visual schematic header for Guides and Advanced Tuning pages.
 */
@Composable
fun TuningConceptDiagram(
    guideId: String,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    val inkArgb = android.graphics.Color.argb(
        (ink.alpha * 255).toInt(),
        (ink.red * 255).toInt(),
        (ink.green * 255).toInt(),
        (ink.blue * 255).toInt()
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .padding(12.dp)
        ) {
            val w = size.width
            val h = size.height
            val cy = h / 2f
            val p = Paint().apply {
                color = inkArgb
                textAlign = Paint.Align.CENTER
                textSize = 21f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val sub = Paint().apply {
                color = inkArgb
                textAlign = Paint.Align.CENTER
                textSize = 18f
                isAntiAlias = true
            }
            val native = drawContext.canvas.nativeCanvas

            when {
                guideId.contains("nock") -> {
                    // 4-clock Nock Indexing compass (0, 90, 180, 270)
                    val cx = w / 2f
                    val r = min(w, h) * 0.34f
                    drawCircle(muted, radius = r, center = Offset(cx, cy), style = Stroke(width = 2.5f))
                    listOf(0.0 to "0°", 90.0 to "90°", 180.0 to "180°", 270.0 to "270°").forEach { (deg, tag) ->
                        val rad = Math.toRadians(deg - 90.0)
                        val px = cx + (cos(rad) * r).toFloat()
                        val py = cy + (sin(rad) * r).toFloat()
                        drawCircle(fill, radius = 6f, center = Offset(px, py))
                        val tx = cx + (cos(rad) * (r + 22f)).toFloat()
                        val ty = cy + (sin(rad) * (r + 22f)).toFloat() + 6f
                        native.drawText(tag, tx, ty, sub)
                    }
                    native.drawText("Nock Clocking", w * 0.20f, cy, p)
                }
                guideId.contains("walk") || guideId.contains("french") -> {
                    // Fan vs Parallel side-by-side comparison
                    val xFan = w * 0.26f
                    val xPar = w * 0.74f
                    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    drawLine(muted, Offset(xFan, 6f), Offset(xFan, h - 22f), strokeWidth = 2f, pathEffect = dash)
                    drawLine(muted, Offset(xPar, 6f), Offset(xPar, h - 22f), strokeWidth = 2f, pathEffect = dash)
                    listOf(14f, 38f, 62f).forEachIndexed { idx, y ->
                        drawCircle(fill, radius = 6f, center = Offset(xFan + (idx + 1) * 16f, y))
                        drawCircle(fill, radius = 6f, center = Offset(xPar + 24f, y))
                    }
                    native.drawText("Fan = Rest move", xFan + 16f, h - 2f, p)
                    native.drawText("Parallel = Sight", xPar + 16f, h - 2f, p)
                }
                else -> {
                    // Horizontal CR Map schematic: Left Tear -> Rest Right / Cam L
                    val leftX = w * 0.27f
                    val rightX = w * 0.73f
                    drawCircle(muted, radius = 26f, center = Offset(leftX, cy - 8f), style = Stroke(width = 2.5f))
                    drawLine(fill, Offset(leftX + 12f, cy - 8f), Offset(leftX - 14f, cy - 8f), strokeWidth = 5f, cap = StrokeCap.Round)
                    drawCircle(fill, radius = 5f, center = Offset(leftX + 12f, cy - 8f))
                    native.drawText("RH Left Tear / Bare L", leftX, h - 2f, sub)

                    drawLine(ink, Offset(rightX - 30f, cy - 8f), Offset(rightX + 30f, cy - 8f), strokeWidth = 5f, cap = StrokeCap.Round)
                    drawCircle(fill, radius = 7f, center = Offset(rightX + 30f, cy - 8f))
                    native.drawText("Rest → RIGHT / Cam → L", rightX, h - 2f, p)
                }
            }
        }
    }
}

private fun fmt1(v: Double): String = ((v * 10.0).roundToInt() / 10.0).toString()
private fun fmt2(v: Double): String = ((v * 100.0).roundToInt() / 100.0).toString()
