package com.strobingn.bowtune.ui.screens.vision

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.CoachingLevel
import com.strobingn.bowtune.data.CoachingTip
import com.strobingn.bowtune.data.FormAnalysis
import com.strobingn.bowtune.data.FormAnalysisResult
import com.strobingn.bowtune.data.PaperTearGuidance
import com.strobingn.bowtune.data.PaperTearLog
import com.strobingn.bowtune.data.SessionKind
import com.strobingn.bowtune.data.TearType
import com.strobingn.bowtune.data.TuneSession
import com.strobingn.bowtune.ui.common.tickHaptic
import com.strobingn.bowtune.ui.theme.Grey20
import com.strobingn.bowtune.ui.theme.Grey30
import com.strobingn.bowtune.ui.theme.Grey90
import com.strobingn.bowtune.ui.theme.Grey95
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.launch

private enum class VisionMode { FORM, PAPER_TEAR }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShotVisionScreen(
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

    var mode by remember { mutableStateOf(VisionMode.FORM) }
    var livePose by remember { mutableStateOf<Pose?>(null) }
    var imageWidth by remember { mutableStateOf(1) }
    var imageHeight by remember { mutableStateOf(1) }
    var frozenAnalysis by remember { mutableStateOf<FormAnalysisResult?>(null) }
    var savedMsg by remember { mutableStateOf<String?>(null) }
    var paperCaptured by remember { mutableStateOf(false) }
    var suggestedTear by remember { mutableStateOf(TearType.BULLET) }
    var confirmedTear by remember { mutableStateOf<TearType?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

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
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Form") }
                SegmentedButton(
                    selected = mode == VisionMode.PAPER_TEAR,
                    onClick = {
                        mode = VisionMode.PAPER_TEAR
                        frozenAnalysis = null
                        savedMsg = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Paper tear") }
            }

            Spacer(Modifier.height(8.dp))

            if (!hasCamPermission) {
                PermissionDeniedCard(
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
                return@Column
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Grey20)
            ) {
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
                if (mode == VisionMode.FORM && frozenAnalysis == null) {
                    PoseSkeletonOverlay(
                        pose = livePose,
                        sourceWidth = imageWidth,
                        sourceHeight = imageHeight,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (mode == VisionMode.PAPER_TEAR && paperCaptured) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
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
                            // Still allow assist flow without file write
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
                                    paperCaptured = true
                                    // No trained tear CV model — assist default only.
                                    suggestedTear = TearType.BULLET
                                    confirmedTear = null
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
                        confirmedTear = null
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionDeniedCard(onRequest: () -> Unit) {
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
                "Shot Vision uses the camera on-device for pose landmarks and paper-tear assist photos. Nothing is uploaded.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRequest) { Text("Grant camera access") }
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
            .height(220.dp)
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
                "On-device ML Kit pose — no API keys. Tips are heuristics.",
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
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
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
    if (mediaImage == null) {
        imageProxy.close()
        onDone(null, imageProxy.width.coerceAtLeast(1), imageProxy.height.coerceAtLeast(1))
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    val w = imageProxy.width
    val h = imageProxy.height
    detector.process(image)
        .addOnSuccessListener { pose -> onDone(pose, w, h) }
        .addOnFailureListener { onDone(null, w, h) }
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
            strokeWidth = 4f
            isAntiAlias = true
        }
        val fill = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
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
            native.drawCircle(lm.position.x * sx, lm.position.y * sy, 6f, fill)
        }
    }
}
