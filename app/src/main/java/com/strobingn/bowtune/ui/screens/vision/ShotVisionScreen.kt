package com.strobingn.bowtune.ui.screens.vision

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.CoachingLevel
import com.strobingn.bowtune.data.CoachingTip
import com.strobingn.bowtune.data.FormAnalysis
import com.strobingn.bowtune.data.FormAnalysisResult
import com.strobingn.bowtune.data.FormMetricsSnapshot
import com.strobingn.bowtune.data.GroupCalculator
import com.strobingn.bowtune.data.GroupPoint
import com.strobingn.bowtune.data.PaperTearGuidance
import com.strobingn.bowtune.data.PaperTearLog
import com.strobingn.bowtune.data.SessionKind
import com.strobingn.bowtune.data.TearType
import com.strobingn.bowtune.data.TuneSession
import com.strobingn.bowtune.data.toDoubleOrNullLenient
import com.strobingn.bowtune.ui.common.tickHaptic
import com.strobingn.bowtune.ui.theme.Grey20
import com.strobingn.bowtune.ui.theme.Grey30
import com.strobingn.bowtune.ui.theme.Grey90
import com.strobingn.bowtune.ui.theme.Grey95
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class VisionMode { FORM, PAPER_TEAR, ARROW_GROUP }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShotVisionScreen(
    initialMode: String? = null,
    onOpenPaperTear: (TearType) -> Unit = {}
) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as BowTuneApp
    val dao = remember { app.database.tuneSessionDao() }
    val tearDao = remember { app.database.paperTearLogDao() }
    val setups by app.database.bowSetupDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val activeId by app.preferences.activeSetupId.collectAsStateWithLifecycle(0L)
    val coaching by app.preferences.coachingLevel.collectAsStateWithLifecycle(CoachingLevel.STANDARD)
    val hapticOn by app.preferences.hapticEnabled.collectAsStateWithLifecycle(true)
    val activeName = setups.firstOrNull { it.id == activeId }?.name.orEmpty()

    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamPermission = granted }

    var mode by remember(initialMode) {
        mutableStateOf(
            when (initialMode) {
                "group" -> VisionMode.ARROW_GROUP
                "paper" -> VisionMode.PAPER_TEAR
                else -> VisionMode.FORM
            }
        )
    }
    var livePose by remember { mutableStateOf<Pose?>(null) }
    var imageWidth by remember { mutableIntStateOf(1) }
    var imageHeight by remember { mutableIntStateOf(1) }
    var frozenAnalysis by remember { mutableStateOf<FormAnalysisResult?>(null) }
    var savedMsg by remember { mutableStateOf<String?>(null) }
    var paperCaptured by remember { mutableStateOf(false) }
    var paperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var suggestedTear by remember { mutableStateOf(TearType.BULLET) }
    var confirmedTear by remember { mutableStateOf<TearType?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Arrow Group mode state
    var groupCaptured by remember { mutableStateOf(false) }
    var groupBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var groupVirtualMode by remember { mutableStateOf(false) }
    var groupTapTool by remember { mutableStateOf(GroupTapTool.MARK_ARROW) }
    val groupArrows = remember { mutableStateListOf<GroupPoint>() }
    var canvasSize by remember { mutableStateOf(IntSize(400, 400)) }
    var refStart by remember { mutableStateOf(GroupPoint(100f, 320f)) }
    var refEnd by remember { mutableStateOf(GroupPoint(300f, 320f)) }
    var nextScaleEndpointIsStart by remember { mutableStateOf(true) }
    var refLengthText by remember { mutableStateOf("4.0") }
    var groupDistanceText by remember { mutableStateOf("20") }
    var groupNotesText by remember { mutableStateOf("") }
    var groupAimPoint by remember { mutableStateOf<GroupPoint?>(null) }
    var groupSavedMsg by remember { mutableStateOf<String?>(null) }

    fun ensureDefaultScaleForSize(sz: IntSize) {
        if (sz.width <= 0 || sz.height <= 0) return
        val cx = sz.width / 2f
        val cy = sz.height / 2f
        val span = min(sz.width, sz.height) * 0.48f
        if (refStart.x == 100f && refEnd.x == 300f) {
            refStart = GroupPoint(cx - span / 2f, cy + span * 0.52f)
            refEnd = GroupPoint(cx + span / 2f, cy + span * 0.52f)
            if (groupAimPoint == null) {
                groupAimPoint = GroupPoint(cx, cy)
            }
        }
    }

    fun loadSampleGroup() {
        val w = canvasSize.width.coerceAtLeast(360).toFloat()
        val h = canvasSize.height.coerceAtLeast(320).toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val span = min(w, h) * 0.45f
        refStart = GroupPoint(cx - span / 2f, cy + span * 0.52f)
        refEnd = GroupPoint(cx + span / 2f, cy + span * 0.52f)
        refLengthText = "4.0"
        groupAimPoint = GroupPoint(cx, cy)
        val pxPerIn = span / 4.0f
        groupArrows.clear()
        // Realistic 5-arrow 20 yd group (~1.85" ES, slightly high-left of aim)
        groupArrows.addAll(
            listOf(
                GroupPoint(cx - 0.35f * pxPerIn, cy - 0.40f * pxPerIn),
                GroupPoint(cx + 0.45f * pxPerIn, cy - 0.15f * pxPerIn),
                GroupPoint(cx - 0.15f * pxPerIn, cy + 0.55f * pxPerIn),
                GroupPoint(cx - 0.75f * pxPerIn, cy + 0.25f * pxPerIn),
                GroupPoint(cx + 0.10f * pxPerIn, cy - 0.70f * pxPerIn)
            )
        )
        groupVirtualMode = true
        groupCaptured = true
        groupSavedMsg = null
    }

    val groupMetrics = remember(
        groupArrows.toList(),
        refStart,
        refEnd,
        refLengthText,
        groupAimPoint,
        groupDistanceText
    ) {
        GroupCalculator.calculate(
            arrows = groupArrows.toList(),
            refStart = refStart,
            refEnd = refEnd,
            refLengthIn = refLengthText.toDoubleOrNullLenient() ?: 4.0,
            aimPoint = groupAimPoint,
            distanceYd = groupDistanceText.toDoubleOrNullLenient()
        )
    }

    LaunchedEffect(Unit) {
        if (!hasCamPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Shot Vision") }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == VisionMode.FORM,
                    onClick = {
                        mode = VisionMode.FORM
                        paperCaptured = false
                        confirmedTear = null
                        frozenAnalysis = null
                        savedMsg = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(0, 3)
                ) { Text("Form") }
                SegmentedButton(
                    selected = mode == VisionMode.PAPER_TEAR,
                    onClick = {
                        mode = VisionMode.PAPER_TEAR
                        frozenAnalysis = null
                        savedMsg = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(1, 3)
                ) { Text("Paper tear") }
                SegmentedButton(
                    selected = mode == VisionMode.ARROW_GROUP,
                    onClick = {
                        mode = VisionMode.ARROW_GROUP
                        frozenAnalysis = null
                        savedMsg = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(2, 3)
                ) { Text("Arrow group") }
            }

            Spacer(Modifier.height(8.dp))

            if (!hasCamPermission && !(mode == VisionMode.ARROW_GROUP && groupVirtualMode)) {
                PermissionDeniedCard(
                    showVirtualTargetOption = mode == VisionMode.ARROW_GROUP,
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onUseVirtualTarget = {
                        groupVirtualMode = true
                        groupCaptured = true
                    },
                    onLoadSampleGroup = { loadSampleGroup() }
                )
                return@Column
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Grey20)
            ) {
                val showCameraPreview = hasCamPermission && !(mode == VisionMode.ARROW_GROUP && groupVirtualMode)
                if (showCameraPreview) {
                    CameraPosePreview(
                        enablePose = mode == VisionMode.FORM && frozenAnalysis == null,
                        onPose = { pose, w, h ->
                            livePose = pose
                            imageWidth = w
                            imageHeight = h
                        },
                        onImageCaptureReady = { imageCapture = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                when (mode) {
                    VisionMode.FORM -> {
                        PoseSkeletonOverlay(
                            pose = livePose,
                            sourceWidth = imageWidth,
                            sourceHeight = imageHeight,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    VisionMode.PAPER_TEAR -> {
                        if (paperCaptured) {
                            paperBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Captured paper tear",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.40f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Frame frozen — confirm tear type below",
                                    color = Grey95,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                    VisionMode.ARROW_GROUP -> {
                        ArrowGroupCanvasOverlay(
                            captured = groupCaptured,
                            capturedBitmap = groupBitmap,
                            virtualTargetMode = groupVirtualMode,
                            tapTool = groupTapTool,
                            arrows = groupArrows,
                            refStart = refStart,
                            refEnd = refEnd,
                            refLengthIn = refLengthText.toDoubleOrNullLenient() ?: 4.0,
                            aimPoint = groupAimPoint,
                            metrics = groupMetrics,
                            onCanvasReady = { sz ->
                                canvasSize = sz
                                ensureDefaultScaleForSize(sz)
                            },
                            onTapCanvas = { pt ->
                                context.tickHaptic(hapticOn)
                                groupSavedMsg = null
                                when (groupTapTool) {
                                    GroupTapTool.MARK_ARROW -> {
                                        groupArrows.add(pt)
                                    }
                                    GroupTapTool.SET_SCALE -> {
                                        if (nextScaleEndpointIsStart) {
                                            refStart = pt
                                        } else {
                                            refEnd = pt
                                        }
                                        nextScaleEndpointIsStart = !nextScaleEndpointIsStart
                                    }
                                    GroupTapTool.SET_AIM -> {
                                        groupAimPoint = pt
                                        groupTapTool = GroupTapTool.MARK_ARROW
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when (mode) {
                VisionMode.FORM -> FormBottomPanel(
                    frozen = frozenAnalysis,
                    liveReady = livePose != null,
                    savedMsg = savedMsg,
                    dark = dark,
                    onCapture = {
                        val pose = livePose
                        if (pose != null) {
                            val result = FormAnalysis.analyze(pose, level = coaching)
                            frozenAnalysis = result
                            context.tickHaptic(hapticOn)
                            if (result.tips.any { it.severity == CoachingTip.Severity.CAUTION }) {
                                context.tickHaptic(hapticOn)
                            }
                            scope.launch {
                                val notes = buildString {
                                    appendLine("Shot Vision — form analysis (auto-attached)")
                                    appendLine("Coaching: ${coaching.label}")
                                    result.confidenceNote?.let { appendLine(it) }
                                    result.tips.forEach { tip ->
                                        appendLine("• ${tip.title}: ${tip.detail}")
                                    }
                                }.trim()
                                dao.upsert(
                                    TuneSession(
                                        dateEpochMs = System.currentTimeMillis(),
                                        distanceYd = "—",
                                        scoreOrGroup = "Vision form",
                                        notes = notes,
                                        setupName = activeName.ifBlank { "Shot Vision" },
                                        sessionKind = SessionKind.VISION
                                    )
                                )
                                savedMsg = "Auto-attached to Sessions"
                            }
                        }
                    },
                    onResume = {
                        frozenAnalysis = null
                        savedMsg = null
                    },
                    onSave = { result ->
                        scope.launch {
                            val notes = buildString {
                                appendLine("Shot Vision — form analysis")
                                result.confidenceNote?.let { appendLine(it) }
                                result.tips.forEach { tip ->
                                    appendLine("• ${tip.title}: ${tip.detail}")
                                }
                            }.trim()
                            dao.upsert(
                                TuneSession(
                                    dateEpochMs = System.currentTimeMillis(),
                                    distanceYd = "—",
                                    scoreOrGroup = "Vision form",
                                    notes = notes,
                                    setupName = activeName.ifBlank { "Shot Vision" },
                                    sessionKind = SessionKind.VISION
                                )
                            )
                            savedMsg = "Saved to Sessions"
                        }
                    }
                )
                VisionMode.PAPER_TEAR -> PaperTearBottomPanel(
                    captured = paperCaptured,
                    suggested = suggestedTear,
                    confirmed = confirmedTear,
                    dark = dark,
                    onCapture = {
                        val capture = imageCapture
                        if (capture == null) {
                            paperCaptured = true
                            suggestedTear = TearType.BULLET
                            confirmedTear = null
                            return@PaperTearBottomPanel
                        }
                        val file = File(context.cacheDir, "paper_tear_${System.currentTimeMillis()}.jpg")
                        val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                        capture.takePicture(
                            opts,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    scope.launch {
                                        paperBitmap = withContext(Dispatchers.IO) {
                                            decodeRotatedBitmap(file)
                                        }
                                        paperCaptured = true
                                        suggestedTear = TearType.BULLET
                                        confirmedTear = null
                                    }
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    paperCaptured = true
                                    suggestedTear = TearType.BULLET
                                }
                            }
                        )
                    },
                    onSelect = { suggestedTear = it },
                    onConfirm = { tear ->
                        confirmedTear = tear
                        context.tickHaptic(hapticOn)
                        scope.launch {
                            tearDao.upsert(
                                PaperTearLog(
                                    dateEpochMs = System.currentTimeMillis(),
                                    tearType = tear.name,
                                    setupName = activeName,
                                    distanceFt = "4-6",
                                    notes = "Confirmed from Shot Vision"
                                )
                            )
                        }
                    },
                    onOpenGuidance = { tear -> onOpenPaperTear(tear) },
                    onRetake = {
                        paperCaptured = false
                        paperBitmap = null
                        confirmedTear = null
                    }
                )
                VisionMode.ARROW_GROUP -> ArrowGroupControlPanel(
                    captured = groupCaptured,
                    virtualTargetMode = groupVirtualMode,
                    tapTool = groupTapTool,
                    onSelectTool = { groupTapTool = it },
                    refLengthText = refLengthText,
                    onRefLengthChange = { refLengthText = it },
                    distanceText = groupDistanceText,
                    onDistanceChange = { groupDistanceText = it },
                    notesText = groupNotesText,
                    onNotesChange = { groupNotesText = it },
                    arrowCount = groupArrows.size,
                    metrics = groupMetrics,
                    savedMsg = groupSavedMsg,
                    dark = dark,
                    onCaptureCamera = {
                        val capture = imageCapture
                        if (capture == null) {
                            groupVirtualMode = true
                            groupCaptured = true
                            groupSavedMsg = null
                            return@ArrowGroupControlPanel
                        }
                        val file = File(context.cacheDir, "arrow_group_${System.currentTimeMillis()}.jpg")
                        val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                        capture.takePicture(
                            opts,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    scope.launch {
                                        groupBitmap = withContext(Dispatchers.IO) {
                                            decodeRotatedBitmap(file)
                                        }
                                        groupVirtualMode = false
                                        groupCaptured = true
                                        groupSavedMsg = null
                                    }
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    groupVirtualMode = true
                                    groupCaptured = true
                                    groupSavedMsg = null
                                }
                            }
                        )
                    },
                    onStartVirtualGrid = {
                        groupVirtualMode = true
                        groupCaptured = true
                        groupSavedMsg = null
                    },
                    onLoadSampleGroup = { loadSampleGroup() },
                    onUndoArrow = {
                        if (groupArrows.isNotEmpty()) {
                            groupArrows.removeAt(groupArrows.lastIndex)
                            groupSavedMsg = null
                        }
                    },
                    onClearArrows = {
                        groupArrows.clear()
                        groupSavedMsg = null
                    },
                    onRetake = {
                        groupCaptured = false
                        groupVirtualMode = false
                        groupBitmap = null
                        groupSavedMsg = null
                    },
                    onRecordGroup = { metrics ->
                        context.tickHaptic(hapticOn)
                        scope.launch {
                            val detailNotes = buildString {
                                appendLine(
                                    "Shot Vision Arrow Group (${metrics.arrowCount} arrows @ ${groupDistanceText.ifBlank { "20" }} yd):"
                                )
                                appendLine(
                                    "• Extreme Spread (C-C): ${fmt2(metrics.extremeSpreadIn)}\" (Outer: ${fmt2(metrics.outerSpreadIn)}\")"
                                )
                                appendLine(
                                    "• Width × Height: ${fmt1(metrics.widthIn)}\" × ${fmt1(metrics.heightIn)}\""
                                )
                                appendLine(
                                    "• Mean Radius: ${fmt2(metrics.meanRadiusIn)}\" · CEP50: ${fmt2(metrics.cep50In)}\" · Radial SD: ${fmt2(metrics.radialStdDevIn)}\""
                                )
                                metrics.moa?.let { appendLine("• Angular spread: ${fmt2(it)} MOA") }
                                appendLine("• Diagnosis: ${metrics.shapeAdvice}")
                                if (groupNotesText.isNotBlank()) {
                                    appendLine("• Notes: ${groupNotesText.trim()}")
                                }
                            }.trim()
                            dao.upsert(
                                TuneSession(
                                    dateEpochMs = System.currentTimeMillis(),
                                    distanceYd = groupDistanceText.trim().ifBlank { "20" },
                                    scoreOrGroup = metrics.compactSummary,
                                    notes = detailNotes,
                                    setupName = activeName.ifBlank { "Shot Vision" },
                                    sessionKind = SessionKind.GROUP
                                )
                            )
                            groupSavedMsg = "Recorded ${metrics.compactSummary} to Sessions!"
                        }
                    }
                )
            }
        }
    }
}

private fun decodeRotatedBitmap(file: File): Bitmap? = runCatching {
    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
    val raw = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
    val exif = ExifInterface(file.absolutePath)
    val deg = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (deg == 0f) raw else {
        val m = Matrix().apply { postRotate(deg) }
        Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
    }
}.getOrNull()

@Composable
private fun PermissionDeniedCard(
    showVirtualTargetOption: Boolean,
    onRequest: () -> Unit,
    onUseVirtualTarget: () -> Unit,
    onLoadSampleGroup: () -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Text("Camera permission needed", fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Shot Vision uses the camera on-device for pose landmarks, paper-tear photos, and arrow group measurement. Nothing is uploaded.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text("Grant camera access")
            }
            if (showVirtualTargetOption) {
                OutlinedButton(onClick = onUseVirtualTarget, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.GridOn, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Use interactive target grid without camera")
                }
                TextButton(onClick = onLoadSampleGroup, modifier = Modifier.fillMaxWidth()) {
                    Text("Load sample 5-arrow group (demo measurement)")
                }
            }
        }
    }
}

@Composable
private fun FormBottomPanel(
    frozen: FormAnalysisResult?,
    liveReady: Boolean,
    savedMsg: String?,
    dark: Boolean,
    onCapture: () -> Unit,
    onResume: () -> Unit,
    onSave: (FormAnalysisResult) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        if (frozen == null) {
            Text(
                if (liveReady) "Pose detected — hold full draw, then freeze"
                else "Point camera at archer (side-ish angle, good light)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onCapture,
                enabled = liveReady,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("Freeze & analyze")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "On-device ML Kit pose — scale-normalized angles & alignment heuristics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onResume, modifier = Modifier.weight(1f)) { Text("Resume") }
                Button(onClick = { onSave(frozen) }, modifier = Modifier.weight(1f)) { Text("Save notes") }
            }
            savedMsg?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            FormMetricsRow(frozen.metrics)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                frozen.confidenceNote?.let { note ->
                    item {
                        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(frozen.tips) { tip ->
                    CoachingCard(tip, dark)
                }
            }
        }
    }
}

@Composable
private fun FormMetricsRow(metrics: FormMetricsSnapshot) {
    val items = buildList {
        metrics.shoulderTiltDeg?.let { add("Shoulders" to "${it.roundToInt()}°") }
        metrics.drawElbowDeg?.let { add("Draw elbow" to "${it.roundToInt()}°") }
        metrics.bowElbowDeg?.let { add("Bow elbow" to "${it.roundToInt()}°") }
        metrics.torsoLeanDeg?.let { add("Torso lean" to "${it.roundToInt()}°") }
    }
    if (items.isEmpty()) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items.forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachingCard(tip: CoachingTip, dark: Boolean) {
    val container = when (tip.severity) {
        CoachingTip.Severity.CAUTION -> if (dark) Grey30 else Grey90
        CoachingTip.Severity.LOW_CONFIDENCE -> MaterialTheme.colorScheme.surfaceVariant
        CoachingTip.Severity.INFO -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (tip.severity == CoachingTip.Severity.INFO) Icons.Filled.Info else Icons.Filled.Warning,
                    contentDescription = null
                )
                Text(tip.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(tip.detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaperTearBottomPanel(
    captured: Boolean,
    suggested: TearType,
    confirmed: TearType?,
    dark: Boolean,
    onCapture: () -> Unit,
    onSelect: (TearType) -> Unit,
    onConfirm: (TearType) -> Unit,
    onOpenGuidance: (TearType) -> Unit,
    onRetake: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(240.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (!captured) {
            Text(
                "Paper tear assist — capture a still of the paper. No trained tear CV; you confirm the type.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onCapture, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("Capture paper photo")
            }
        } else {
            Text(
                "Assist suggestion (manual confirm required)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TearType.entries.forEach { type ->
                    FilterChip(
                        selected = suggested == type,
                        onClick = { onSelect(type) },
                        label = { Text(type.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (dark) Grey30 else Grey90,
                            selectedLabelColor = if (dark) Grey95 else Grey20
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) { Text("Retake") }
                Button(
                    onClick = { onConfirm(suggested) },
                    modifier = Modifier.weight(1f)
                ) { Text("Confirm") }
            }
            confirmed?.let { tear ->
                Spacer(Modifier.height(8.dp))
                Text(tear.shortDescription, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onOpenGuidance(tear) }) {
                    Text("Open Paper Tear guidance")
                }
                PaperTearGuidance.stepsFor(tear, CoachingLevel.STANDARD).take(2).forEachIndexed { i, step ->
                    Text("${i + 1}. $step", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CameraPosePreview(
    enablePose: Boolean,
    onPose: (Pose, Int, Int) -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val busy = remember { AtomicBoolean(false) }
    val detector = remember {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            detector.close()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier,
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                onImageCaptureReady(capture)

                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(480, 640))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    if (!enablePose || busy.get()) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    busy.set(true)
                    processPose(imageProxy, detector) { pose, w, h ->
                        if (pose != null) onPose(pose, w, h)
                        busy.set(false)
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture,
                        analysis
                    )
                } catch (_: Exception) {
                    // Camera may be in use; ignore bind races during recomposition
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

private fun processPose(
    imageProxy: ImageProxy,
    detector: com.google.mlkit.vision.pose.PoseDetector,
    onDone: (Pose?, Int, Int) -> Unit
) {
    val mediaImage = imageProxy.image
    val rot = imageProxy.imageInfo.rotationDegrees
    val rotatedW = if (rot == 90 || rot == 270) imageProxy.height else imageProxy.width
    val rotatedH = if (rot == 90 || rot == 270) imageProxy.width else imageProxy.height
    if (mediaImage == null) {
        imageProxy.close()
        onDone(null, rotatedW.coerceAtLeast(1), rotatedH.coerceAtLeast(1))
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, rot)
    detector.process(image)
        .addOnSuccessListener { pose -> onDone(pose, rotatedW, rotatedH) }
        .addOnFailureListener { onDone(null, rotatedW, rotatedH) }
        .addOnCompleteListener { imageProxy.close() }
}

@Composable
private fun PoseSkeletonOverlay(
    pose: Pose?,
    sourceWidth: Int,
    sourceHeight: Int,
    modifier: Modifier = Modifier
) {
    if (pose == null || sourceWidth <= 0 || sourceHeight <= 0) return
    val pairs = remember {
        listOf(
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.NOSE,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.NOSE
        )
    }
    Canvas(modifier) {
        val sx = size.width / sourceWidth.toFloat()
        val sy = size.height / sourceHeight.toFloat()
        val stroke = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
            setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
        }
        val fill = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
            setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
        }
        val labelPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
            setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
        }
        val native = drawContext.canvas.nativeCanvas
        for ((a, b) in pairs) {
            val la = pose.getPoseLandmark(a) ?: continue
            val lb = pose.getPoseLandmark(b) ?: continue
            if (la.inFrameLikelihood < 0.4f || lb.inFrameLikelihood < 0.4f) continue
            native.drawLine(
                la.position.x * sx,
                la.position.y * sy,
                lb.position.x * sx,
                lb.position.y * sy,
                stroke
            )
        }
        for (lm in pose.allPoseLandmarks) {
            if (lm.inFrameLikelihood < 0.4f) continue
            native.drawCircle(lm.position.x * sx, lm.position.y * sy, 7f, fill)
        }

        // Draw live joint angle badges at elbows and shoulder line
        val ls = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val le = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val lw = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        if (ls != null && le != null && lw != null &&
            ls.inFrameLikelihood >= 0.4f && le.inFrameLikelihood >= 0.4f && lw.inFrameLikelihood >= 0.4f
        ) {
            val ang = FormAnalysis.angleAt(ls, le, lw).roundToInt()
            native.drawText("${ang}°", le.position.x * sx, le.position.y * sy - 16f, labelPaint)
        }
        val rs = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val re = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rw = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        if (rs != null && re != null && rw != null &&
            rs.inFrameLikelihood >= 0.4f && re.inFrameLikelihood >= 0.4f && rw.inFrameLikelihood >= 0.4f
        ) {
            val ang = FormAnalysis.angleAt(rs, re, rw).roundToInt()
            native.drawText("${ang}°", re.position.x * sx, re.position.y * sy - 16f, labelPaint)
        }
        if (ls != null && rs != null && ls.inFrameLikelihood >= 0.4f && rs.inFrameLikelihood >= 0.4f) {
            val dy = abs(rs.position.y - ls.position.y)
            val dx = abs(rs.position.x - ls.position.x).coerceAtLeast(20f)
            val tilt = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).roundToInt()
            val midX = (ls.position.x + rs.position.x) * 0.5f * sx
            val midY = (ls.position.y + rs.position.y) * 0.5f * sy - 14f
            native.drawText("Tilt ${tilt}°", midX, midY, labelPaint)
        }
    }
}

private fun fmt1(v: Double): String = ((v * 10.0).roundToInt() / 10.0).toString()
private fun fmt2(v: Double): String = ((v * 100.0).roundToInt() / 100.0).toString()
