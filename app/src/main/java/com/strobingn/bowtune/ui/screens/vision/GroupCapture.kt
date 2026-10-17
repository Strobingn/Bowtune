package com.strobingn.bowtune.ui.screens.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.strobingn.bowtune.data.GroupMeasure
import com.strobingn.bowtune.data.GroupPoint
import com.strobingn.bowtune.data.GroupReference
import com.strobingn.bowtune.data.SessionKind
import com.strobingn.bowtune.data.TuneSession
import com.strobingn.bowtune.data.formatInches
import com.strobingn.bowtune.data.TuneMetrics
import com.strobingn.bowtune.data.toDoubleOrNullLenient
import com.strobingn.bowtune.ui.common.NumberSliderField
import com.strobingn.bowtune.ui.common.SegmentedChoice
import com.strobingn.bowtune.ui.common.GroupShapeSketch
import com.strobingn.bowtune.ui.theme.Grey20
import java.io.File
import kotlin.math.hypot
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class MarkTool { SCALE, ARROWS, AIM }

private enum class ScaleShape { CIRCLE, LINE }

private data class ScaleCircle(val cx: Float, val cy: Float, val radius: Float)

private data class ScaleLine(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

private data class PhotoFit(
    val left: Float,
    val top: Float,
    /** Screen pixels per bitmap pixel. */
    val scale: Float,
    val drawnWidth: Float,
    val drawnHeight: Float
) {
    fun toScreen(x: Float, y: Float) = Offset(left + x * scale, top + y * scale)

    fun toBitmap(screen: Offset, bitmapWidth: Float, bitmapHeight: Float): Offset? {
        if (scale <= 0f) return null
        val x = (screen.x - left) / scale
        val y = (screen.y - top) / scale
        if (x < -20f || y < -20f || x > bitmapWidth + 20f || y > bitmapHeight + 20f) return null
        return Offset(x.coerceIn(0f, bitmapWidth), y.coerceIn(0f, bitmapHeight))
    }

    companion object {
        fun of(view: IntSize, bitmapWidth: Int, bitmapHeight: Int): PhotoFit {
            if (view.width == 0 || view.height == 0 || bitmapWidth == 0 || bitmapHeight == 0) {
                return PhotoFit(0f, 0f, 1f, 1f, 1f)
            }
            val scale = min(
                view.width.toFloat() / bitmapWidth.toFloat(),
                view.height.toFloat() / bitmapHeight.toFloat()
            )
            val drawnWidth = bitmapWidth * scale
            val drawnHeight = bitmapHeight * scale
            return PhotoFit(
                left = (view.width - drawnWidth) / 2f,
                top = (view.height - drawnHeight) / 2f,
                scale = scale,
                drawnWidth = drawnWidth,
                drawnHeight = drawnHeight
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GroupCaptureMode(
    hasCamera: Boolean,
    onRequestCamera: () -> Unit,
    setupName: String,
    onSave: (TuneSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var tool by remember { mutableStateOf(MarkTool.SCALE) }
    var shape by remember { mutableStateOf(ScaleShape.CIRCLE) }
    var reference by remember { mutableStateOf(GroupReference.FACE_40) }
    var customInches by remember { mutableStateOf("") }
    var circle by remember { mutableStateOf(ScaleCircle(0f, 0f, 1f)) }
    var line by remember { mutableStateOf(ScaleLine(0f, 0f, 1f, 0f)) }
    var arrows by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var aim by remember { mutableStateOf<Offset?>(null) }
    var distance by remember { mutableStateOf("20") }
    var shaftDiameter by remember { mutableStateOf("") }
    var indoor by remember { mutableStateOf("indoor") }

    fun showPhoto(decoded: Bitmap) {
        circle = ScaleCircle(
            decoded.width / 2f,
            decoded.height / 2f,
            min(decoded.width, decoded.height) * 0.32f
        )
        line = ScaleLine(
            decoded.width * 0.18f,
            decoded.height * 0.5f,
            decoded.width * 0.82f,
            decoded.height * 0.5f
        )
        bitmap = decoded
        arrows = emptyList()
        aim = null
        status = null
        tool = MarkTool.SCALE
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { loadPickedPhoto(context, uri) }
            if (loaded == null) {
                status = "Could not read that photo."
            } else {
                showPhoto(loaded)
            }
        }
    }

    val photo = bitmap
    Column(modifier.fillMaxSize()) {
        if (photo == null) {
            if (hasCamera) {
                CameraPosePreview(
                    enablePose = false,
                    onPose = { _, _, _ -> },
                    onImageCaptureReady = { imageCapture = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Grey20)
                )
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Camera permission is off. You can still measure a photo you already took.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(onClick = onRequestCamera) { Text("Grant camera access") }
                }
            }
            Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Photograph the group straight-on. You will fit a known face or a ruler, then tap each arrow center.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        val capture = imageCapture
                        if (capture == null) {
                            status = "Camera is not ready yet."
                            return@Button
                        }
                        val file = File(context.cacheDir, "group_${System.currentTimeMillis()}.jpg")
                        capture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    scope.launch {
                                        val decoded = withContext(Dispatchers.IO) {
                                            decodeGroupPhoto(file.absolutePath)
                                        }
                                        if (decoded == null) {
                                            status = "Photo saved but could not be opened."
                                        } else {
                                            showPhoto(decoded)
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    status = "Capture failed. Try again."
                                }
                            }
                        )
                    },
                    enabled = hasCamera,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Text("  Capture group")
                }
                OutlinedButton(
                    onClick = { pickPhoto.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Text("  Choose a photo")
                }
                status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        } else {
            val inches = reference.resolvedInches(customInches.toDoubleOrNullLenient())
            val referencePixels = when (shape) {
                ScaleShape.CIRCLE -> circle.radius * 2.0
                ScaleShape.LINE -> hypot(
                    (line.x2 - line.x1).toDouble(),
                    (line.y2 - line.y1).toDouble()
                )
            }
            val inchesPerPixel = inches?.let { GroupMeasure.inchesPerPixel(it, referencePixels) }
            val distanceYd = distance.toDoubleOrNullLenient()
            val shaft = shaftDiameter.toDoubleOrNullLenient()
            val points = arrows.map { GroupPoint(it.x.toDouble(), it.y.toDouble()) }
            val aimPoint = aim?.let { GroupPoint(it.x.toDouble(), it.y.toDouble()) }
            val readout = inchesPerPixel?.let {
                GroupMeasure.readout(points, it, distanceYd, shaft, aimPoint)
            }

            GroupPhotoMarker(
                bitmap = photo,
                tool = tool,
                shape = shape,
                circle = circle,
                line = line,
                arrows = arrows,
                aim = aim,
                readoutLabel = readout?.let { "${it.scoreLabel()} center to center" },
                onCircle = { circle = it },
                onLine = { line = it },
                onArrows = { arrows = it },
                onAim = { aim = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(tool == MarkTool.SCALE, { tool = MarkTool.SCALE }, { Text("Scale") })
                    FilterChip(tool == MarkTool.ARROWS, { tool = MarkTool.ARROWS }, { Text("Arrows") })
                    FilterChip(tool == MarkTool.AIM, { tool = MarkTool.AIM }, { Text("Aim mark") })
                }
                if (tool == MarkTool.SCALE) {
                    Text(
                        "Fit the circle to the outer ring of a round face, or stretch the line across a known distance. That is the only scale the inches come from.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(shape == ScaleShape.CIRCLE, { shape = ScaleShape.CIRCLE }, { Text("Round face") })
                        FilterChip(shape == ScaleShape.LINE, { shape = ScaleShape.LINE }, { Text("Straight reference") })
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GroupReference.entries.forEach { preset ->
                            FilterChip(
                                selected = reference == preset,
                                onClick = { reference = preset },
                                label = { Text(preset.label) }
                            )
                        }
                    }
                    if (reference == GroupReference.CUSTOM) {
                        NumberSliderField(
                            label = "Reference length",
                            text = customInches,
                            onTextChange = { customInches = it },
                            min = 1.0,
                            max = 60.0,
                            step = 0.25,
                            unit = "in",
                            defaultValue = 12.0
                        )
                    }
                }
                if (tool == MarkTool.ARROWS) {
                    Text(
                        "Tap each arrow center. Tap a mark again to remove it. Up to 12 arrows. Group size is the longest center-to-center gap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (arrows.isNotEmpty()) {
                        TextButton(onClick = { arrows = arrows.dropLast(1) }) { Text("Undo last arrow") }
                    }
                }
                if (tool == MarkTool.AIM) {
                    Text(
                        "Tap the spot you were aiming at. The app compares the group center to that mark and tells you which way to move the sight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (aim != null) {
                        TextButton(onClick = { aim = null }) { Text("Clear aim mark") }
                    }
                }
                readout?.let { result ->
                    Text(
                        result.scoreLabel(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "H ${formatInches(result.horizontalInches)} in · V ${formatInches(result.verticalInches)} in" +
                            (result.moa?.let { " · ${formatInches(it)} MOA" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    GroupShapeSketch(
                        points = points,
                        aim = aimPoint,
                        extremeA = result.metrics.extremeIndexA,
                        extremeB = result.metrics.extremeIndexB,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                    Text(result.patternNote, style = MaterialTheme.typography.bodySmall)
                    result.sizeNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    result.aimNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    result.outsideToOutsideInches?.let {
                        Text(
                            "Outside-to-outside ${formatInches(it)} in (adds one shaft diameter).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } ?: Text(
                    if (arrows.size < 2) "Mark at least two arrow centers."
                    else "Set a scale length before the inches can be calculated.",
                    style = MaterialTheme.typography.bodySmall
                )
                NumberSliderField(
                    label = "Distance",
                    text = distance,
                    onTextChange = { distance = it },
                    min = 3.0,
                    max = 100.0,
                    step = 1.0,
                    unit = "yd",
                    defaultValue = 20.0
                )
                NumberSliderField(
                    label = "Shaft diameter (optional)",
                    text = shaftDiameter,
                    onTextChange = { shaftDiameter = it },
                    min = 0.15,
                    max = 0.45,
                    step = 0.005,
                    unit = "in",
                    defaultValue = 0.246
                )
                SegmentedChoice(
                    options = listOf("indoor" to "Indoor", "outdoor" to "Outdoor"),
                    selected = indoor,
                    onSelect = { indoor = it }
                )
                status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            bitmap = null
                            arrows = emptyList()
                            aim = null
                            status = null
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Retake") }
                    Button(
                        onClick = {
                            val result = readout
                            if (result == null) {
                                status = "Need a scale and at least two arrows."
                                return@Button
                            }
                            val referenceLabel = buildString {
                                append(reference.label)
                                inches?.let { append(" · ${formatInches(it)} in") }
                                append(if (shape == ScaleShape.CIRCLE) " circle" else " line")
                            }
                            onSave(
                                TuneSession(
                                    dateEpochMs = System.currentTimeMillis(),
                                    distanceYd = distance.trim().ifBlank { "—" },
                                    scoreOrGroup = result.scoreLabel(),
                                    notes = result.logNotes(referenceLabel, distanceYd, shaft),
                                    setupName = setupName.ifBlank { "Shot Vision" },
                                    sessionKind = SessionKind.GROUP,
                                    indoorOutdoor = indoor,
                                    metricsJson = TuneMetrics.encode(
                                        mapOf(
                                            TuneMetrics.GROUP_SPREAD to result.extremeInches,
                                            TuneMetrics.GROUP_W to result.horizontalInches,
                                            TuneMetrics.GROUP_H to result.verticalInches
                                        )
                                    )
                                )
                            )
                            status = "Recorded ${result.scoreLabel()} in Logs."
                        },
                        enabled = readout != null,
                        modifier = Modifier.weight(1f)
                    ) { Text("Record") }
                }
            }
        }
    }
}

@Composable
private fun GroupPhotoMarker(
    bitmap: Bitmap,
    tool: MarkTool,
    shape: ScaleShape,
    circle: ScaleCircle,
    line: ScaleLine,
    arrows: List<Offset>,
    aim: Offset?,
    readoutLabel: String?,
    onCircle: (ScaleCircle) -> Unit,
    onLine: (ScaleLine) -> Unit,
    onArrows: (List<Offset>) -> Unit,
    onAim: (Offset?) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val circleNow = rememberUpdatedState(circle)
    val lineNow = rememberUpdatedState(line)
    val arrowsNow = rememberUpdatedState(arrows)
    val onCircleNow = rememberUpdatedState(onCircle)
    val onLineNow = rememberUpdatedState(onLine)
    val onArrowsNow = rememberUpdatedState(onArrows)
    val onAimNow = rememberUpdatedState(onAim)
    val gestureModifier = when (tool) {
        MarkTool.SCALE -> Modifier.pointerInput(shape, viewSize, bitmap.width, bitmap.height) {
            val fit = PhotoFit.of(viewSize, bitmap.width, bitmap.height)
            var resizing = false
            var lineEnd = 0
            detectDragGestures(
                onDragStart = { start ->
                    val bmp = fit.toBitmap(start, bitmap.width.toFloat(), bitmap.height.toFloat())
                        ?: return@detectDragGestures
                    if (shape == ScaleShape.CIRCLE) {
                        val current = circleNow.value
                        val fromCenter = hypot(bmp.x - current.cx, bmp.y - current.cy)
                        resizing = kotlin.math.abs(fromCenter - current.radius) < 48f / fit.scale
                    } else {
                        val current = lineNow.value
                        val d1 = hypot(bmp.x - current.x1, bmp.y - current.y1)
                        val d2 = hypot(bmp.x - current.x2, bmp.y - current.y2)
                        lineEnd = when {
                            d1 <= d2 && d1 < 56f / fit.scale -> 1
                            d2 < 56f / fit.scale -> 2
                            else -> 0
                        }
                    }
                },
                onDrag = { change, drag ->
                    change.consume()
                    val dx = drag.x / fit.scale
                    val dy = drag.y / fit.scale
                    if (shape == ScaleShape.CIRCLE) {
                        val current = circleNow.value
                        if (resizing) {
                            val pointer = fit.toBitmap(
                                change.position,
                                bitmap.width.toFloat(),
                                bitmap.height.toFloat()
                            ) ?: return@detectDragGestures
                            val radius = hypot(pointer.x - current.cx, pointer.y - current.cy)
                                .coerceIn(24f, min(bitmap.width, bitmap.height) * 0.75f)
                            onCircleNow.value(current.copy(radius = radius))
                        } else {
                            onCircleNow.value(
                                current.copy(
                                    cx = (current.cx + dx).coerceIn(0f, bitmap.width.toFloat()),
                                    cy = (current.cy + dy).coerceIn(0f, bitmap.height.toFloat())
                                )
                            )
                        }
                    } else {
                        val current = lineNow.value
                        val moved = when (lineEnd) {
                            1 -> current.copy(
                                x1 = (current.x1 + dx).coerceIn(0f, bitmap.width.toFloat()),
                                y1 = (current.y1 + dy).coerceIn(0f, bitmap.height.toFloat())
                            )
                            2 -> current.copy(
                                x2 = (current.x2 + dx).coerceIn(0f, bitmap.width.toFloat()),
                                y2 = (current.y2 + dy).coerceIn(0f, bitmap.height.toFloat())
                            )
                            else -> current.copy(
                                x1 = (current.x1 + dx).coerceIn(0f, bitmap.width.toFloat()),
                                y1 = (current.y1 + dy).coerceIn(0f, bitmap.height.toFloat()),
                                x2 = (current.x2 + dx).coerceIn(0f, bitmap.width.toFloat()),
                                y2 = (current.y2 + dy).coerceIn(0f, bitmap.height.toFloat())
                            )
                        }
                        onLineNow.value(moved)
                    }
                }
            )
        }
        MarkTool.ARROWS -> Modifier.pointerInput(viewSize, bitmap.width, bitmap.height) {
            val fit = PhotoFit.of(viewSize, bitmap.width, bitmap.height)
            detectTapGestures { tap ->
                val bmp = fit.toBitmap(tap, bitmap.width.toFloat(), bitmap.height.toFloat())
                    ?: return@detectTapGestures
                val current = arrowsNow.value
                val hit = current.indexOfFirst { hypot(it.x - bmp.x, it.y - bmp.y) * fit.scale < 32f }
                onArrowsNow.value(
                    when {
                        hit >= 0 -> current.filterIndexed { index, _ -> index != hit }
                        current.size >= 12 -> current
                        else -> current + bmp
                    }
                )
            }
        }
        MarkTool.AIM -> Modifier.pointerInput(viewSize, bitmap.width, bitmap.height) {
            val fit = PhotoFit.of(viewSize, bitmap.width, bitmap.height)
            detectTapGestures { tap ->
                val bmp = fit.toBitmap(tap, bitmap.width.toFloat(), bitmap.height.toFloat())
                    ?: return@detectTapGestures
                onAimNow.value(bmp)
            }
        }
    }

    Canvas(
        modifier
            .background(Color.Black)
            .onSizeChanged { viewSize = it }
            .then(gestureModifier)
    ) {
        val dst = PhotoFit.of(IntSize(size.width.toInt(), size.height.toInt()), bitmap.width, bitmap.height)
        drawImage(
            image = bitmap.asImageBitmapSafe(),
            dstOffset = androidx.compose.ui.unit.IntOffset(dst.left.toInt(), dst.top.toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(
                dst.drawnWidth.toInt().coerceAtLeast(1),
                dst.drawnHeight.toInt().coerceAtLeast(1)
            )
        )
        val metrics = if (arrows.size >= 2) {
            GroupMeasure.metrics(arrows.map { GroupPoint(it.x.toDouble(), it.y.toDouble()) })
        } else {
            null
        }
        if (shape == ScaleShape.CIRCLE) {
            val center = dst.toScreen(circle.cx, circle.cy)
            val radius = circle.radius * dst.scale
            drawCircle(Color.White.copy(alpha = 0.9f), radius, center, style = Stroke(3f))
            drawLine(
                Color.White,
                Offset(center.x - radius, center.y),
                Offset(center.x + radius, center.y),
                strokeWidth = 2f
            )
        }
        if (shape == ScaleShape.LINE) {
            val a = dst.toScreen(line.x1, line.y1)
            val b = dst.toScreen(line.x2, line.y2)
            drawLine(Color.White, a, b, strokeWidth = 4f)
            drawCircle(Color.White, 10f, a)
            drawCircle(Color.White, 10f, b)
        }
        metrics?.let { group ->
            val cover = dst.toScreen(group.covering.cx.toFloat(), group.covering.cy.toFloat())
            drawCircle(
                Color.White.copy(alpha = 0.7f),
                group.covering.radius.toFloat() * dst.scale,
                cover,
                style = Stroke(2f)
            )
            val a = arrows[group.extremeIndexA]
            val b = arrows[group.extremeIndexB]
            drawLine(Color.White, dst.toScreen(a.x, a.y), dst.toScreen(b.x, b.y), strokeWidth = 4f)
        }
        arrows.forEachIndexed { index, arrow ->
            val p = dst.toScreen(arrow.x, arrow.y)
            drawCircle(Color.Black, 11f, p)
            drawCircle(Color.White, 8f, p)
            drawContext.canvas.nativeCanvas.drawText(
                (index + 1).toString(),
                p.x + 12f,
                p.y - 12f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 28f
                    isAntiAlias = true
                }
            )
        }
        aim?.let { mark ->
            val p = dst.toScreen(mark.x, mark.y)
            val arm = 14f
            drawLine(Color.White, Offset(p.x - arm, p.y - arm), Offset(p.x + arm, p.y + arm), strokeWidth = 3f)
            drawLine(Color.White, Offset(p.x - arm, p.y + arm), Offset(p.x + arm, p.y - arm), strokeWidth = 3f)
        }
        readoutLabel?.let { label ->
            drawContext.canvas.nativeCanvas.drawText(
                label,
                16f,
                size.height - 18f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 36f
                    isAntiAlias = true
                    setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
                }
            )
        }
    }
}

private fun Bitmap.asImageBitmapSafe(): androidx.compose.ui.graphics.ImageBitmap = asImageBitmap()

private fun loadPickedPhoto(context: android.content.Context, uri: Uri): Bitmap? {
    val file = File(context.cacheDir, "group_pick_${System.currentTimeMillis()}.jpg")
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
        } ?: return null
        decodeGroupPhoto(file.absolutePath)
    } catch (_: Exception) {
        null
    }
}

internal fun decodeGroupPhoto(path: String, maxEdge: Int = 1800): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxEdge || bounds.outHeight / sample > maxEdge) {
        sample *= 2
    }
    val decoded = BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply { inSampleSize = sample }
    ) ?: return null
    val orientation = try {
        ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } catch (_: Exception) {
        return decoded
    }
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return decoded
    }
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}
