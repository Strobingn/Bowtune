package com.strobingn.bowtune.ui.screens.vision

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.strobingn.bowtune.data.GroupCalculator
import com.strobingn.bowtune.data.GroupMetrics
import com.strobingn.bowtune.data.GroupPoint
import com.strobingn.bowtune.ui.theme.Grey10
import com.strobingn.bowtune.ui.theme.Grey20
import com.strobingn.bowtune.ui.theme.Grey30
import com.strobingn.bowtune.ui.theme.Grey50
import com.strobingn.bowtune.ui.theme.Grey80
import com.strobingn.bowtune.ui.theme.Grey90
import com.strobingn.bowtune.ui.theme.Grey95
import com.strobingn.bowtune.ui.theme.Grey100
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

enum class GroupTapTool(val label: String) {
    MARK_ARROW("Tap arrows"),
    SET_SCALE("Set scale ruler"),
    SET_AIM("Set aim point")
}

/**
 * Interactive overlay drawn over the live camera preview (when framing) or over the captured
 * target photo / virtual target face (when measuring and calculating group size).
 */
@Composable
fun ArrowGroupCanvasOverlay(
    captured: Boolean,
    capturedBitmap: Bitmap?,
    virtualTargetMode: Boolean,
    tapTool: GroupTapTool,
    arrows: List<GroupPoint>,
    refStart: GroupPoint,
    refEnd: GroupPoint,
    refLengthIn: Double,
    aimPoint: GroupPoint?,
    metrics: GroupMetrics?,
    onCanvasReady: (IntSize) -> Unit,
    onTapCanvas: (GroupPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (captured && capturedBitmap != null && !virtualTargetMode) {
            Image(
                bitmap = capturedBitmap.asImageBitmap(),
                contentDescription = "Captured arrow group",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle dim scrim so grayscale caliper lines and arrow numbers stay legible on any photo
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    if (size.width > 0 && size.height > 0) {
                        onCanvasReady(size)
                    }
                }
                .pointerInput(captured, tapTool, refStart, refEnd) {
                    if (!captured) return@pointerInput
                    detectTapGestures { offset ->
                        onTapCanvas(GroupPoint(offset.x, offset.y))
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val minDim = min(w, h)

            if (!captured) {
                // Live camera framing guide: target rings + reference bar guide
                val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                drawCircle(
                    color = Grey95.copy(alpha = 0.65f),
                    radius = minDim * 0.34f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = dash)
                )
                drawCircle(
                    color = Grey95.copy(alpha = 0.45f),
                    radius = minDim * 0.17f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = dash)
                )
                drawLine(
                    color = Grey95.copy(alpha = 0.5f),
                    start = Offset(cx - 28f, cy),
                    end = Offset(cx + 28f, cy),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Grey95.copy(alpha = 0.5f),
                    start = Offset(cx, cy - 28f),
                    end = Offset(cx, cy + 28f),
                    strokeWidth = 2f
                )
                val bannerPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = Paint.Align.CENTER
                    textSize = 30f
                    isAntiAlias = true
                    setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "Frame arrow group & tap Capture",
                    cx,
                    h - 32f,
                    bannerPaint
                )
                return@Canvas
            }

            // If virtual target mode or no camera bitmap, draw a crisp grayscale target face + 1" grid
            if (virtualTargetMode || capturedBitmap == null) {
                drawVirtualTargetBackdrop(refStart, refEnd, refLengthIn)
            }

            // 1. Draw Reference Scale Ruler (Ref A <-> Ref B)
            drawReferenceRuler(
                refStart = refStart,
                refEnd = refEnd,
                refLengthIn = refLengthIn,
                isActive = tapTool == GroupTapTool.SET_SCALE
            )

            // 2. Draw Aim Point (POA) if set
            aimPoint?.let { aim ->
                drawAimCrosshair(aim, metrics?.centroidPx)
            }

            // 3. Draw Group Geometry (Bounding Box, Mean Radius Circle, Centroid, Extreme Spread Caliper)
            if (metrics != null && arrows.size >= 2) {
                drawGroupGeometryOverlay(arrows, metrics)
            }

            // 4. Draw Individual Numbered Arrow Impacts
            drawArrowImpacts(arrows, metrics?.suspectedFlyerNumber)
        }
    }
}

private fun DrawScope.drawVirtualTargetBackdrop(
    refStart: GroupPoint,
    refEnd: GroupPoint,
    refLengthIn: Double
) {
    drawRect(color = Grey10)
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val refPx = hypot((refEnd.x - refStart.x).toDouble(), (refEnd.y - refStart.y).toDouble()).toFloat()
    val pxPerIn = if (refLengthIn > 0.0 && refPx > 10f) (refPx / refLengthIn.toFloat()) else (min(w, h) / 10f)

    // 1-inch subtle grid lines
    val gridColor = Grey20
    var gx = cx % pxPerIn
    while (gx < w) {
        drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
        gx += pxPerIn
    }
    var gy = cy % pxPerIn
    while (gy < h) {
        drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
        gy += pxPerIn
    }

    // Concentric target rings (4", 2", 1", X)
    val rings = listOf(4f to Grey30, 3f to Grey30, 2f to Grey50, 1f to Grey80)
    rings.forEach { (diaIn, col) ->
        val r = (diaIn * 0.5f * pxPerIn).coerceAtMost(min(w, h) * 0.46f)
        drawCircle(
            color = col.copy(alpha = 0.35f),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )
    }
    // Center X cross
    drawLine(Grey50, Offset(cx - 12f, cy), Offset(cx + 12f, cy), strokeWidth = 2f)
    drawLine(Grey50, Offset(cx, cy - 12f), Offset(cx, cy + 12f), strokeWidth = 2f)
}

private fun DrawScope.drawReferenceRuler(
    refStart: GroupPoint,
    refEnd: GroupPoint,
    refLengthIn: Double,
    isActive: Boolean
) {
    val start = Offset(refStart.x, refStart.y)
    val end = Offset(refEnd.x, refEnd.y)
    val rulerColor = if (isActive) Grey100 else Grey80
    val strokeW = if (isActive) 5f else 3.5f

    drawLine(
        color = Color.Black.copy(alpha = 0.7f),
        start = start,
        end = end,
        strokeWidth = strokeW + 4f
    )
    drawLine(
        color = rulerColor,
        start = start,
        end = end,
        strokeWidth = strokeW
    )

    // End caps
    listOf(start to "A", end to "B").forEach { (pt, tag) ->
        drawCircle(Color.Black, radius = 14f, center = pt)
        drawCircle(rulerColor, radius = 11f, center = pt)
        val p = Paint().apply {
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText(tag, pt.x, pt.y + 6f, p)
    }

    // Label at midpoint
    val midX = (start.x + end.x) / 2f
    val midY = (start.y + end.y) / 2f
    val label = "Scale: ${((refLengthIn * 100).roundToInt() / 100.0)}\""
    val labelPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 24f
        isFakeBoldText = true
        isAntiAlias = true
        setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText(label, midX, (midY - 14f).coerceAtLeast(28f), labelPaint)
}

private fun DrawScope.drawAimCrosshair(aim: GroupPoint, centroid: GroupPoint?) {
    val pt = Offset(aim.x, aim.y)
    val r = 18f
    drawCircle(Color.Black.copy(alpha = 0.7f), radius = r, center = pt, style = Stroke(width = 5f))
    drawCircle(Grey90, radius = r, center = pt, style = Stroke(width = 2.5f))
    drawLine(Grey90, Offset(pt.x - r - 6f, pt.y), Offset(pt.x + r + 6f, pt.y), strokeWidth = 2.5f)
    drawLine(Grey90, Offset(pt.x, pt.y - r - 6f), Offset(pt.x, pt.y + r + 6f), strokeWidth = 2.5f)

    if (centroid != null) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        drawLine(
            color = Grey80,
            start = pt,
            end = Offset(centroid.x, centroid.y),
            strokeWidth = 2f,
            pathEffect = dash
        )
    }
}

private fun DrawScope.drawGroupGeometryOverlay(
    arrows: List<GroupPoint>,
    metrics: GroupMetrics
) {
    val minX = arrows.minOf { it.x }
    val maxX = arrows.maxOf { it.x }
    val minY = arrows.minOf { it.y }
    val maxY = arrows.maxOf { it.y }
    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)

    // 1. W x H bounding box
    if (maxX - minX > 4f || maxY - minY > 4f) {
        drawRect(
            color = Grey80.copy(alpha = 0.65f),
            topLeft = Offset(minX, minY),
            size = Size((maxX - minX).coerceAtLeast(4f), (maxY - minY).coerceAtLeast(4f)),
            style = Stroke(width = 2f, pathEffect = dash)
        )
    }

    val center = Offset(metrics.centroidPx.x, metrics.centroidPx.y)

    // 2. Outer enclosing circle around centroid
    if (metrics.maxRadiusFromCentroidPx > 6f) {
        drawCircle(
            color = Grey90.copy(alpha = 0.45f),
            radius = metrics.maxRadiusFromCentroidPx,
            center = center,
            style = Stroke(width = 2f)
        )
    }

    // 3. Mean radius dashed ring
    if (metrics.meanRadiusPx > 4f) {
        drawCircle(
            color = Grey95.copy(alpha = 0.75f),
            radius = metrics.meanRadiusPx,
            center = center,
            style = Stroke(width = 2.5f, pathEffect = dash)
        )
    }

    // 4. Extreme spread caliper line between the two farthest arrows
    metrics.extremePairIndices?.let { (i, j) ->
        if (i in arrows.indices && j in arrows.indices) {
            val a = Offset(arrows[i].x, arrows[i].y)
            val b = Offset(arrows[j].x, arrows[j].y)
            drawLine(
                color = Color.Black.copy(alpha = 0.8f),
                start = a,
                end = b,
                strokeWidth = 7f
            )
            drawLine(
                color = Grey100,
                start = a,
                end = b,
                strokeWidth = 3.5f
            )
            val midX = (a.x + b.x) / 2f
            val midY = (a.y + b.y) / 2f
            val esText = "ES ${((metrics.extremeSpreadIn * 100).roundToInt() / 100.0)}\""
            val paint = Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = 26f
                isFakeBoldText = true
                isAntiAlias = true
                setShadowLayer(8f, 0f, 2f, android.graphics.Color.BLACK)
            }
            drawContext.canvas.nativeCanvas.drawText(esText, midX, midY - 12f, paint)
        }
    }

    // 5. Group Centroid (MPI) marker
    drawCircle(Color.Black, radius = 9f, center = center)
    drawCircle(Grey100, radius = 7f, center = center, style = Stroke(width = 2.5f))
    drawCircle(Grey100, radius = 2.5f, center = center)
}

private fun DrawScope.drawArrowImpacts(
    arrows: List<GroupPoint>,
    suspectedFlyerNumber: Int?
) {
    val labelPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 22f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val flyerPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 22f
        isFakeBoldText = true
        isAntiAlias = true
    }

    arrows.forEachIndexed { idx, pt ->
        val num = idx + 1
        val isFlyer = suspectedFlyerNumber == num
        val center = Offset(pt.x, pt.y)
        if (isFlyer) {
            drawCircle(Grey100, radius = 19f, center = center)
            drawCircle(Color.Black, radius = 16f, center = center)
            drawCircle(Grey100, radius = 16f, center = center, style = Stroke(width = 3f))
            drawContext.canvas.nativeCanvas.drawText("$num!", center.x, center.y + 8f, flyerPaint)
        } else {
            drawCircle(Color.Black, radius = 17f, center = center)
            drawCircle(Grey95, radius = 14f, center = center)
            drawContext.canvas.nativeCanvas.drawText("$num", center.x, center.y + 8f, labelPaint)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArrowGroupControlPanel(
    captured: Boolean,
    virtualTargetMode: Boolean,
    tapTool: GroupTapTool,
    onSelectTool: (GroupTapTool) -> Unit,
    refLengthText: String,
    onRefLengthChange: (String) -> Unit,
    distanceText: String,
    onDistanceChange: (String) -> Unit,
    notesText: String,
    onNotesChange: (String) -> Unit,
    arrowCount: Int,
    metrics: GroupMetrics?,
    savedMsg: String?,
    dark: Boolean,
    onCaptureCamera: () -> Unit,
    onStartVirtualGrid: () -> Unit,
    onLoadSampleGroup: () -> Unit,
    onUndoArrow: () -> Unit,
    onClearArrows: () -> Unit,
    onRetake: () -> Unit,
    onRecordGroup: (GroupMetrics) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!captured) {
            Text(
                "Capture a photo of your arrow group on the target, or open the interactive target grid to plot and calculate group size.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCaptureCamera,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Capture group")
                }
                OutlinedButton(
                    onClick = onStartVirtualGrid,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.GridOn, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Target grid")
                }
            }
            TextButton(onClick = onLoadSampleGroup, modifier = Modifier.fillMaxWidth()) {
                Text("Load sample 5-arrow group (demo measurement)")
            }
            return@Column
        }

        // Step 1: Tool selector (Mark arrows vs Set scale ruler vs Set aim point)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GroupTapTool.entries.forEach { tool ->
                FilterChip(
                    selected = tapTool == tool,
                    onClick = { onSelectTool(tool) },
                    label = { Text(tool.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (dark) Grey30 else Grey90,
                        selectedLabelColor = if (dark) Grey95 else Grey20
                    )
                )
            }
        }

        // Step 2: Calibration reference presets when SET_SCALE is active (or compact scale bar)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Straighten, contentDescription = null)
                    Text(
                        if (tapTool == GroupTapTool.SET_SCALE) {
                            "Tap canvas to place Scale A & B endpoints"
                        } else {
                            "Reference A–B = $refLengthText in · Distance = $distanceText yd"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "1.0\"" to "1.0",
                        "3.15\" (8cm)" to "3.15",
                        "4.0\" bull" to "4.0",
                        "8.0\" ring" to "8.0"
                    ).forEach { (label, valStr) ->
                        FilterChip(
                            selected = refLengthText == valStr,
                            onClick = { onRefLengthChange(valStr) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = refLengthText,
                        onValueChange = onRefLengthChange,
                        label = { Text("Ref A–B (in)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = onDistanceChange,
                        label = { Text("Distance (yd)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Step 3: Live Group Size Metrics Card
        if (metrics != null && metrics.arrowCount >= 2) {
            GroupMetricsSummaryCard(metrics = metrics)
        } else {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    if (arrowCount == 0) {
                        "Tap each arrow hole on the target above (at least 2 arrows) to measure group size."
                    } else {
                        "1 arrow marked — tap at least 1 more arrow to calculate Extreme Spread, W×H, and Mean Radius."
                    },
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Step 4: Actions (Undo, Clear, Retake, Sample, Record)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onUndoArrow,
                enabled = arrowCount > 0,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Undo")
            }
            OutlinedButton(
                onClick = onClearArrows,
                enabled = arrowCount > 0,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Clear")
            }
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (virtualTargetMode) "Camera" else "Retake")
            }
        }

        OutlinedTextField(
            value = notesText,
            onValueChange = onNotesChange,
            label = { Text("Session note (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        savedMsg?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = { if (metrics != null) onRecordGroup(metrics) },
            enabled = metrics != null && metrics.arrowCount >= 2,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(
                if (metrics != null && metrics.arrowCount >= 2) {
                    "Record ${metrics.compactSummary} to Sessions"
                } else {
                    "Mark ≥2 arrows to record group"
                }
            )
        }
    }
}

@Composable
fun GroupMetricsSummaryCard(metrics: GroupMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Extreme Spread: ${fmt2(metrics.extremeSpreadIn)}\" C-C",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${metrics.arrowCount} arrows",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricBadge("W × H", "${fmt1(metrics.widthIn)} × ${fmt1(metrics.heightIn)}\"")
                MetricBadge("Mean radius", "${fmt2(metrics.meanRadiusIn)}\"")
                MetricBadge("CEP 50%", "${fmt2(metrics.cep50In)}\"")
                MetricBadge(
                    "Outer-to-outer",
                    "${fmt2(metrics.outerSpreadIn)}\""
                )
            }
            metrics.moa?.let { moaVal ->
                Text(
                    "Angular spread: ${fmt2(moaVal)} MOA · Radial SD: ${fmt2(metrics.radialStdDevIn)}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                metrics.shapeAdvice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MetricBadge(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private fun fmt1(v: Double): String = ((v * 10.0).roundToInt() / 10.0).toString()
private fun fmt2(v: Double): String = ((v * 100.0).roundToInt() / 100.0).toString()
