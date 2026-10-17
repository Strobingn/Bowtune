package com.strobingn.bowtune.ui.screens.home

import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.AppPreferences
import com.strobingn.bowtune.data.ArrowShaft
import com.strobingn.bowtune.data.BackupManager
import com.strobingn.bowtune.data.Calculators
import com.strobingn.bowtune.data.ChartData
import com.strobingn.bowtune.data.ChecklistTemplates
import com.strobingn.bowtune.data.CoachingLevel
import com.strobingn.bowtune.data.ReportBuilder
import com.strobingn.bowtune.data.SessionKind
import com.strobingn.bowtune.data.TearType
import com.strobingn.bowtune.data.formatNumber
import com.strobingn.bowtune.data.toDoubleOrNullLenient
import com.strobingn.bowtune.ui.charts.ArcGauge
import com.strobingn.bowtune.ui.charts.ArrowWeightBreakdown
import com.strobingn.bowtune.ui.charts.FocBands
import com.strobingn.bowtune.ui.charts.arrowSegments
import com.strobingn.bowtune.ui.charts.BarChart
import com.strobingn.bowtune.ui.charts.BarDatum
import com.strobingn.bowtune.ui.charts.ChartPoint
import com.strobingn.bowtune.ui.charts.FillPattern
import com.strobingn.bowtune.ui.charts.LineChart
import com.strobingn.bowtune.ui.charts.LineSeries
import com.strobingn.bowtune.ui.charts.LineStyle
import com.strobingn.bowtune.ui.charts.MarkerShape
import com.strobingn.bowtune.ui.charts.ProgressRing
import com.strobingn.bowtune.ui.charts.RangeScale
import com.strobingn.bowtune.ui.charts.ReferenceLine
import com.strobingn.bowtune.ui.charts.TearGlyph
import com.strobingn.bowtune.ui.common.ArrowBalanceSketch
import com.strobingn.bowtune.ui.common.ChartCard
import com.strobingn.bowtune.ui.common.EnergyGauge
import com.strobingn.bowtune.ui.common.FormSection
import com.strobingn.bowtune.ui.common.HintCard
import com.strobingn.bowtune.ui.common.LabeledStepSlider
import com.strobingn.bowtune.ui.common.NumberSlider
import com.strobingn.bowtune.ui.common.NumberSliderField
import com.strobingn.bowtune.ui.common.SegmentedChoice
import com.strobingn.bowtune.ui.common.StatRow
import com.strobingn.bowtune.ui.common.ToggleRow
import com.strobingn.bowtune.ui.common.formatDay
import com.strobingn.bowtune.ui.common.formatWhen
import com.strobingn.bowtune.ui.common.shareText
import com.strobingn.bowtune.ui.common.shortDate
import com.strobingn.bowtune.ui.common.tickHaptic
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class HomeTool { DASH, FOC, SPINE, KE, BACKUP, SETTINGS, REPORT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPaperTear: () -> Unit,
    onOpenGear: () -> Unit,
    onOpenChecklist: () -> Unit,
    onOpenSessions: () -> Unit,
    onOpenAdvancedLibrary: () -> Unit = {},
    onOpenGroupMeasure: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as BowTuneApp
    val scope = rememberCoroutineScope()
    val prefs = app.preferences
    val db = app.database

    val setups by db.bowSetupDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val sessions by db.tuneSessionDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val guided by db.guidedTuneSessionDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val tears by db.paperTearLogDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val adjustments by db.adjustmentLogDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val arrows by db.arrowShaftDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val maintenance by db.maintenanceLogDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val activeId by prefs.activeSetupId.collectAsStateWithLifecycle(0L)
    val templateId by prefs.checklistTemplateId.collectAsStateWithLifecycle(ChecklistTemplates.FULL.id)
    val checked by app.checklistStore.checkedIds(templateId).collectAsStateWithLifecycle(emptySet())
    val coaching by prefs.coachingLevel.collectAsStateWithLifecycle(CoachingLevel.STANDARD)
    val haptic by prefs.hapticEnabled.collectAsStateWithLifecycle(true)
    val hapticStrength by prefs.hapticStrength.collectAsStateWithLifecycle(AppPreferences.DEFAULT_HAPTIC_STRENGTH)
    val tts by prefs.ttsEnabled.collectAsStateWithLifecycle(false)
    val ttsRate by prefs.ttsRate.collectAsStateWithLifecycle(AppPreferences.DEFAULT_TTS_RATE)
    val largeText by prefs.largeText.collectAsStateWithLifecycle(false)
    val highContrast by prefs.highContrast.collectAsStateWithLifecycle(false)

    val active = setups.firstOrNull { it.id == activeId }
    val template = ChecklistTemplates.byId(templateId)
    val percent = if (template.items.isEmpty()) 0 else (checked.size * 100 / template.items.size)
    val lastTear = tears.firstOrNull()
    val nextDue = listOfNotNull(active?.nextTuneDueEpochMs)
        .plus(maintenance.mapNotNull { it.nextDueEpochMs })
        .minOrNull()

    var tool by rememberSaveable { mutableStateOf(HomeTool.DASH) }
    var status by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            status = runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                        ?: error("Could not read file")
                }
                BackupManager.importJson(text, db, prefs, app.checklistStore)
                "Restore complete."
            }.getOrElse { "Restore failed: ${it.message}" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (tool) {
                            HomeTool.DASH -> "Bow Tune"
                            HomeTool.FOC -> "FOC calculator"
                            HomeTool.SPINE -> "Spine helper"
                            HomeTool.KE -> "KE & momentum"
                            HomeTool.BACKUP -> "Backup & restore"
                            HomeTool.SETTINGS -> "Coaching & access"
                            HomeTool.REPORT -> "Tuning report"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (tool != HomeTool.DASH) {
                        IconButton(onClick = { tool = HomeTool.DASH }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (tool == HomeTool.DASH) {
                        IconButton(onClick = { tool = HomeTool.SETTINGS }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (tool) {
            HomeTool.DASH -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ActiveBowHero(
                        title = active?.name ?: "No active bow",
                        subtitle = if (active == null) {
                            "Add a setup in Gear and tap Set active. Logs and calculators will use it."
                        } else {
                            listOf(active.brand, active.model).filter { it.isNotBlank() }.joinToString(" · ")
                                .ifBlank { "Open Gear to edit draw weight, Limb Shift, and due dates." }
                        },
                        stats = active?.let {
                            listOf(
                                Triple("Draw wt", it.drawWeightLbs.ifBlank { "—" }, "lb"),
                                Triple("Draw len", it.drawLengthIn.ifBlank { "—" }, "in"),
                                Triple(
                                    if (it.arrowSpeedFps.isNotBlank()) "Chrono" else "IBO",
                                    it.arrowSpeedFps.ifBlank { it.iboSpeedFps }.ifBlank { "—" },
                                    "fps"
                                )
                            )
                        },
                        onClick = onOpenGear
                    )
                }
                item {
                    val bullet = ChartData.bulletRate(tears)
                    val daysLeft = nextDue?.let { ChartData.daysUntil(it, System.currentTimeMillis()) }
                    ChartCard(title = "Tune readiness", subtitle = "Checklist · paper bullets (last 10) · next service") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            RingWithLabel(
                                label = template.name,
                                onClick = onOpenChecklist
                            ) {
                                ProgressRing(
                                    progress = percent / 100f,
                                    size = 92.dp,
                                    segments = template.items.size,
                                    filledSegments = checked.size,
                                    centerText = "$percent%",
                                    caption = "Checklist"
                                )
                            }
                            RingWithLabel(label = "Bullet holes", onClick = onOpenPaperTear) {
                                ProgressRing(
                                    progress = (bullet ?: 0.0).toFloat(),
                                    size = 92.dp,
                                    centerText = bullet?.let { "${(it * 100).toInt()}%" } ?: "—",
                                    caption = "Bullet holes",
                                    dashedTrack = bullet == null
                                )
                            }
                            RingWithLabel(
                                label = when {
                                    daysLeft == null -> "No due date"
                                    daysLeft < 0 -> "Overdue"
                                    else -> "Until service"
                                },
                                onClick = onOpenGear
                            ) {
                                ProgressRing(
                                    progress = ((daysLeft ?: 0L).coerceIn(0L, 30L) / 30f),
                                    size = 92.dp,
                                    centerText = when {
                                        daysLeft == null -> "—"
                                        daysLeft < 0 -> "Due"
                                        else -> "${daysLeft}d"
                                    },
                                    caption = "Service",
                                    dashedTrack = daysLeft == null
                                )
                            }
                        }
                    }
                }
                item {
                    val scores = ChartData.scoreTrend(sessions)
                    val groups = ChartData.groupTrend(sessions, guided)
                    var showScores by rememberSaveable { mutableStateOf(scores.size >= groups.size) }
                    ChartCard(
                        title = "Shooting trend",
                        subtitle = if (showScores) "Score per session" else "Group size (inches, lower is better)",
                        actionLabel = "Logs",
                        onAction = onOpenSessions
                    ) {
                        SegmentedChoice(
                            options = listOf(true to "Score", false to "Group"),
                            selected = showScores,
                            onSelect = { showScores = it }
                        )
                        if (showScores) {
                            LineChart(
                                series = listOf(
                                    LineSeries("Score", scores.map { ChartPoint(it.epochMs.toDouble(), it.value) })
                                ),
                                height = 150.dp,
                                xLabel = { shortDate(it.toLong()) },
                                emptyText = "Log a Group / score session with a score to see your trend."
                            )
                        } else {
                            LineChart(
                                series = listOf(
                                    LineSeries("Group", groups.map { ChartPoint(it.first.toDouble(), it.second.size) })
                                ),
                                height = 150.dp,
                                yUnit = "\"",
                                xLabel = { shortDate(it.toLong()) },
                                emptyText = "Log a group size (or finish LIFT 29.5) to see group trends."
                            )
                        }
                    }
                }
                item {
                    ChartCard(title = "Last paper tear", onClick = onOpenPaperTear) {
                        val lastType = TearType.fromStored(lastTear?.tearType)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (lastType != null) {
                                TearGlyph(lastType, size = 64.dp, magnitudeIn = lastTear?.magnitudeIn, emphasized = true)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    lastType?.label ?: "No tear logged yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    lastTear?.let {
                                        "${formatWhen(it.dateEpochMs)} · ${it.setupName.ifBlank { "no setup" }}" +
                                            (it.magnitudeIn?.let { m -> " · ${formatNumber(m)}\"" } ?: "")
                                    } ?: "Diagnose on the Tear tab, then Save tear.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (tears.size > 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                tears.take(8).reversed().forEach { log ->
                                    TearType.fromStored(log.tearType)?.let {
                                        TearGlyph(it, size = 32.dp, magnitudeIn = log.magnitudeIn)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    val now = System.currentTimeMillis()
                    val weekly = ChartData.perWeek(sessions.map { it.dateEpochMs } + tears.map { it.dateEpochMs }, now)
                    ChartCard(
                        title = "Range activity",
                        subtitle = "Sessions + tears logged per week (last 8 weeks)",
                        onClick = onOpenSessions
                    ) {
                        BarChart(
                            bars = weekly.mapIndexed { i, n ->
                                BarDatum(
                                    label = if (i == weekly.lastIndex) "now" else "-${weekly.lastIndex - i}w",
                                    value = n.toDouble(),
                                    pattern = if (i == weekly.lastIndex) FillPattern.SOLID else FillPattern.DIAGONAL,
                                    emphasized = i == weekly.lastIndex
                                )
                            },
                            height = 130.dp,
                            valueFormatter = { formatNumber(it, 0) },
                            description = "Weekly activity: ${weekly.joinToString()}"
                        )
                    }
                }
                item {
                    ChartCard(
                        title = "Measure a group",
                        subtitle = "Photograph the arrow group, fit a face or a ruler, tap each shaft, and record the center-to-center size.",
                        onClick = onOpenGroupMeasure
                    ) {}
                }
                item {
                    ChartCard(title = "Up next", onClick = onOpenSessions) {
                        Text(
                            nextDue?.let { "Next cam / tune check ${formatDay(it)}" }
                                ?: "No due date. Set one on a setup or a Service log in Gear.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            sessions.firstOrNull()?.let {
                                "Last session ${formatWhen(it.dateEpochMs)} · ${SessionKind.label(it.sessionKind)} · " +
                                    "${it.distanceYd} yd · ${it.scoreOrGroup.ifBlank { "—" }}"
                            } ?: "No sessions yet. Log a group in Logs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    ChartCard(
                        title = "Advanced Tuning library",
                        subtitle = "Bare shaft walkthroughs, nock clocking, French, walk-back, planing, broadheads, CR paper depth. Guides tab has the same entry.",
                        onClick = onOpenAdvancedLibrary
                    ) {}
                }
                item {
                    Text("Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                item {
                    val tools = listOf(
                        Triple("FOC", Icons.Filled.Balance, HomeTool.FOC),
                        Triple("Spine", Icons.Filled.Straighten, HomeTool.SPINE),
                        Triple("KE", Icons.Filled.Speed, HomeTool.KE),
                        Triple("Report", Icons.Filled.Description, HomeTool.REPORT),
                        Triple("Backup", Icons.Filled.Save, HomeTool.BACKUP),
                        Triple("Settings", Icons.Filled.Settings, HomeTool.SETTINGS)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tools.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (label, icon, dest) ->
                                    ToolTile(label, icon, Modifier.weight(1f)) { tool = dest }
                                }
                            }
                        }
                    }
                }
                item {
                    Text(
                        "Coaching: ${coaching.label}. Paper distance 4-6 ft. Grayscale theme stays put.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HomeTool.FOC -> ToolPane(padding) {
                FocTool(arrows.firstOrNull())
            }
            HomeTool.SPINE -> ToolPane(padding) {
                SpineTool(
                    defaultDw = active?.drawWeightLbs.orEmpty(),
                    drawLength = active?.drawLengthIn.orEmpty(),
                    defaultIbo = active?.iboSpeedFps.orEmpty()
                )
            }
            HomeTool.KE -> ToolPane(padding) {
                EnergyTool(defaultSpeed = active?.arrowSpeedFps?.ifBlank { active.iboSpeedFps }.orEmpty())
            }
            HomeTool.BACKUP -> ToolPane(padding) {
                Text("Export writes a JSON snapshot of setups, sessions, tears, arrows, logs, checklist, and preferences. Restore replaces local data.")
                StatRow(
                    listOf(
                        Triple("Setups", setups.size.toString(), ""),
                        Triple("Sessions", sessions.size.toString(), ""),
                        Triple("Tears", tears.size.toString(), "")
                    )
                )
                status?.let { Text(it, fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                val json = BackupManager.exportJson(db, prefs, app.checklistStore)
                                context.shareText("Bow Tune backup", json)
                                status = "Share sheet opened."
                            }.onFailure { status = it.message }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null)
                    Text("  Export / share backup")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null)
                    Text("  Restore from file")
                }
            }
            HomeTool.SETTINGS -> ToolPane(padding) {
                SettingsPane(
                    coaching = coaching,
                    haptic = haptic,
                    hapticStrength = hapticStrength,
                    tts = tts,
                    ttsRate = ttsRate,
                    largeText = largeText,
                    highContrast = highContrast,
                    prefs = prefs,
                    scope = scope
                )
            }
            HomeTool.REPORT -> ToolPane(padding) {
                val report = remember(active, sessions, tears, adjustments, arrows, maintenance, percent, template) {
                    ReportBuilder.build(
                        active, sessions, tears, adjustments, arrows, maintenance, percent, template.name
                    )
                }
                StatRow(
                    listOf(
                        Triple("Checklist", "$percent", "%"),
                        Triple("Tears", tears.size.toString(), ""),
                        Triple("Adjust.", adjustments.size.toString(), "")
                    )
                )
                Button(
                    onClick = { context.shareText("Bow Tune report", report) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text("  Share report")
                }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Text(report, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp))
                }
            }
        }
    }
}

@Composable
private fun ActiveBowHero(
    title: String,
    subtitle: String,
    stats: List<Triple<String, String, String>>?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "ACTIVE BOW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
            )
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f))
            if (stats != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.forEach { (label, value, unit) ->
                        Column(
                            Modifier
                                .weight(1f)
                        ) {
                            Text(
                                label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.65f)
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    " $unit",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(bottom = 3.dp),
                                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RingWithLabel(label: String, onClick: () -> Unit, ring: @Composable () -> Unit) {
    Column(
        Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ring()
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ToolTile(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ToolPane(padding: PaddingValues, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsPane(
    coaching: CoachingLevel,
    haptic: Boolean,
    hapticStrength: Int,
    tts: Boolean,
    ttsRate: Float,
    largeText: Boolean,
    highContrast: Boolean,
    prefs: AppPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val created = TextToSpeech(context) { st ->
            if (st == TextToSpeech.SUCCESS) engine?.language = Locale.US
        }
        engine = created
        onDispose {
            created.stop()
            created.shutdown()
        }
    }

    FormSection("Coaching depth") {
        LabeledStepSlider(
            label = "Coaching level",
            stops = CoachingLevel.entries.map { it.label },
            index = coaching.ordinal,
            onIndexChange = { scope.launch { prefs.setCoachingLevel(CoachingLevel.entries[it]) } },
            supporting = coaching.blurb
        )
        Text(
            "Changes Paper Tear step depth and Vision tip density.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    FormSection("Feedback") {
        ToggleRow(
            title = "Haptics",
            subtitle = "Vibrate on Vision freeze / caution tips",
            checked = haptic,
            onCheckedChange = { scope.launch { prefs.setHapticEnabled(it) } },
            icon = Icons.Filled.Vibration
        )
        if (haptic) {
            LabeledStepSlider(
                label = "Haptic strength",
                stops = listOf("Light", "Medium", "Strong"),
                index = hapticStrength - 1,
                onIndexChange = {
                    scope.launch { prefs.setHapticStrength(it + 1) }
                    context.tickHaptic(true, it + 1)
                }
            )
        }
        ToggleRow(
            title = "Read tear steps aloud",
            subtitle = "Speak fix steps when a tear is selected",
            checked = tts,
            onCheckedChange = { scope.launch { prefs.setTtsEnabled(it) } },
            icon = Icons.Filled.RecordVoiceOver
        )
        NumberSlider(
            label = "Speech rate",
            value = ttsRate.toDouble(),
            onValueChange = { v -> scope.launch { prefs.setTtsRate((v ?: 1.0).toFloat()) } },
            min = AppPreferences.MIN_TTS_RATE.toDouble(),
            max = AppPreferences.MAX_TTS_RATE.toDouble(),
            step = 0.1,
            allowClear = false,
            defaultValue = 1.0,
            valueLabel = { "${formatNumber(it, 1)}×" }
        )
        OutlinedButton(
            onClick = {
                engine?.setSpeechRate(ttsRate)
                engine?.speak("Nock high. Step 1. Lower the nocking point slightly.", TextToSpeech.QUEUE_FLUSH, null, "preview")
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Preview voice") }
    }
    FormSection("Display") {
        ToggleRow(
            title = "Larger text",
            checked = largeText,
            onCheckedChange = { scope.launch { prefs.setLargeText(it) } },
            icon = Icons.Filled.FormatSize
        )
        ToggleRow(
            title = "High-contrast grayscale",
            checked = highContrast,
            onCheckedChange = { scope.launch { prefs.setHighContrast(it) } },
            icon = Icons.Filled.Contrast
        )
        GreyRamp()
        Text(
            "Bow Tune is grayscale only. Charts use grey tones, line styles, markers and hatch patterns.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GreyRamp() {
    val cs = MaterialTheme.colorScheme
    val swatches = listOf(cs.onSurface, cs.onSurfaceVariant, cs.outline, cs.outlineVariant, cs.surfaceContainerHighest, cs.surface)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val w = size.width / swatches.size
        swatches.forEachIndexed { i, c ->
            drawRect(c, topLeft = androidx.compose.ui.geometry.Offset(w * i, 0f), size = androidx.compose.ui.geometry.Size(w, size.height))
        }
        drawRect(cs.outline, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
    }
}

@Composable
private fun FocTool(sample: ArrowShaft?) {
    var length by rememberSaveable { mutableStateOf(sample?.lengthIn.orEmpty().ifBlank { "29" }) }
    var balance by rememberSaveable { mutableStateOf(sample?.balanceFromNockIn.orEmpty()) }
    var shaft by rememberSaveable { mutableStateOf(sample?.gpi.orEmpty()) }
    var point by rememberSaveable { mutableStateOf(sample?.pointWeightGr.orEmpty().ifBlank { "100" }) }
    var insert by rememberSaveable { mutableStateOf(sample?.insertWeightGr.orEmpty()) }
    var nock by rememberSaveable { mutableStateOf(sample?.nockWeightGr.orEmpty()) }
    var vane by rememberSaveable { mutableStateOf(sample?.vaneWeightGr.orEmpty()) }
    var wrap by rememberSaveable { mutableStateOf(sample?.wrapWeightGr.orEmpty()) }
    val result = Calculators.foc(
        length.toDoubleOrNullLenient(),
        balance.toDoubleOrNullLenient(),
        shaft.toDoubleOrNullLenient(),
        point.toDoubleOrNullLenient(),
        insert.toDoubleOrNullLenient(),
        nock.toDoubleOrNullLenient(),
        vane.toDoubleOrNullLenient(),
        wrap.toDoubleOrNullLenient()
    )
    ArcGauge(
        value = result.focPercent,
        min = 0.0,
        max = 25.0,
        bands = FocBands,
        unit = "%",
        caption = "Front of center"
    )
    ArrowBalanceSketch(length.toDoubleOrNullLenient(), balance.toDoubleOrNullLenient())
    ArrowWeightBreakdown(
        arrowSegments(
            shaft = shaft.toDoubleOrNullLenient(),
            point = point.toDoubleOrNullLenient(),
            insert = insert.toDoubleOrNullLenient(),
            nock = nock.toDoubleOrNullLenient(),
            vane = vane.toDoubleOrNullLenient(),
            wrap = wrap.toDoubleOrNullLenient()
        )
    )
    HintCard(result.note)
    FormSection("Arrow") {
        NumberSliderField("Arrow length", length, { length = it }, 24.0, 34.0, 0.125, unit = "in", defaultValue = 29.0)
        NumberSliderField(
            "Balance point from nock",
            balance,
            { balance = it },
            8.0,
            24.0,
            0.125,
            unit = "in",
            defaultValue = (length.toDoubleOrNullLenient() ?: 29.0) * 0.6,
            supporting = "Measure from the nock throat to where the arrow balances."
        )
    }
    FormSection("Component weights") {
        NumberSliderField("Shaft (GPI × length)", shaft, { shaft = it }, 100.0, 600.0, 1.0, unit = "gr", defaultValue = 270.0)
        NumberSliderField("Point", point, { point = it }, 50.0, 300.0, 5.0, unit = "gr", defaultValue = 100.0)
        NumberSliderField("Insert", insert, { insert = it }, 0.0, 120.0, 1.0, unit = "gr", defaultValue = 16.0)
        NumberSliderField("Nock", nock, { nock = it }, 0.0, 40.0, 0.5, unit = "gr", defaultValue = 10.0)
        NumberSliderField("Vanes total", vane, { vane = it }, 0.0, 60.0, 0.5, unit = "gr", defaultValue = 18.0)
        NumberSliderField("Wrap", wrap, { wrap = it }, 0.0, 40.0, 0.5, unit = "gr", defaultValue = 0.0)
    }
}

@Composable
private fun SpineTool(defaultDw: String, drawLength: String, defaultIbo: String) {
    var dw by rememberSaveable { mutableStateOf(defaultDw.ifBlank { "60" }) }
    var length by rememberSaveable { mutableStateOf("") }
    var point by rememberSaveable { mutableStateOf("100") }
    var insert by rememberSaveable { mutableStateOf("") }
    var speed by rememberSaveable { mutableStateOf(defaultIbo) }
    val result = Calculators.spine(
        dw.toDoubleOrNullLenient(),
        length.toDoubleOrNullLenient(),
        point.toDoubleOrNullLenient(),
        insert.toDoubleOrNullLenient(),
        speed.toDoubleOrNullLenient()
    )
    ChartCard(title = result.suggestedRange, subtitle = "Easton hunting chart 301055-A start — confirm on paper at 4-6 ft") {
        RangeScale(
            min = 150.0,
            max = 750.0,
            low = result.stiffer?.toDouble(),
            high = result.weaker?.toDouble(),
            reversed = true,
            leftCaption = "← weaker (higher #)",
            rightCaption = "stiffer (lower #) →"
        )
    }
    FormSection("Inputs") {
        NumberSliderField("Peak draw weight", dw, { dw = it }, 15.0, 110.0, 1.0, unit = "lb", defaultValue = 60.0)
        NumberSliderField(
            "Cut length, nock throat to shaft end",
            length,
            { length = it },
            23.0,
            34.0,
            0.25,
            unit = "in",
            defaultValue = 28.0,
            supporting = if (drawLength.isNotBlank()) {
                "This bow's draw length is $drawLength in. That is not the cut length — measure the shaft."
            } else {
                "Measure the shaft, not your draw length; the point is not included."
            }
        )
        NumberSliderField("Point weight", point, { point = it }, 50.0, 300.0, 5.0, unit = "gr", defaultValue = 100.0)
        NumberSliderField(
            "Insert / outsert (optional)",
            insert,
            { insert = it },
            0.0,
            120.0,
            1.0,
            unit = "gr",
            defaultValue = 16.0
        )
        NumberSliderField(
            "ATA / IBO speed (optional)",
            speed,
            { speed = it },
            250.0,
            380.0,
            1.0,
            unit = "fps",
            defaultValue = 330.0
        )
    }
    result.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
}

/** Rule of thumb: roughly 1 fps lost per 3 grains added. */
private const val GRAINS_PER_FPS = 3.0

@Composable
private fun EnergyTool(defaultSpeed: String) {
    var grains by rememberSaveable { mutableStateOf("400") }
    var fps by rememberSaveable { mutableStateOf(defaultSpeed.ifBlank { "" }) }
    val g = grains.toDoubleOrNullLenient()
    val v = fps.toDoubleOrNullLenient()
    val result = Calculators.energy(g, v)
    StatRow(
        listOf(
            Triple("Kinetic energy", result.kineticFtLbs?.let { formatNumber(it, 1) } ?: "—", "ft-lb"),
            Triple("Momentum", result.momentumSlugFtS?.let { formatNumber(it, 3) } ?: "—", "slug·ft/s")
        )
    )
    EnergyGauge(result.kineticFtLbs)
    if (g != null && v != null && g > 0 && v > 0) {
        val deltas = listOf(-100, -50, 0, 50, 100)
        val whatIf = deltas.mapNotNull { d ->
            val wg = g + d
            val wv = v - d / GRAINS_PER_FPS
            if (wg <= 0 || wv <= 0) null else Triple(d, wg, Calculators.energy(wg, wv))
        }
        ChartCard(
            title = "KE by arrow weight",
            subtitle = "Estimated at ~1 fps lost per 3 gr added. Reference lines are common guidance, not law."
        ) {
            BarChart(
                bars = whatIf.map { (d, wg, e) ->
                    BarDatum(
                        label = "${formatNumber(wg, 0)} gr",
                        value = e.kineticFtLbs ?: 0.0,
                        pattern = if (d == 0) FillPattern.SOLID else FillPattern.DIAGONAL,
                        emphasized = d == 0
                    )
                },
                references = listOf(
                    ReferenceLine(25.0, "25 small game", LineStyle.DOTTED),
                    ReferenceLine(40.0, "40 deer", LineStyle.DASHED),
                    ReferenceLine(65.0, "65 elk", LineStyle.DASH_DOT)
                ),
                description = "Kinetic energy at different arrow weights"
            )
        }
        ChartCard(title = "Momentum by arrow weight", subtitle = "Heavier arrows carry more momentum even as KE flattens.") {
            BarChart(
                bars = whatIf.map { (d, wg, e) ->
                    BarDatum(
                        label = "${formatNumber(wg, 0)} gr",
                        value = e.momentumSlugFtS ?: 0.0,
                        pattern = if (d == 0) FillPattern.SOLID else FillPattern.CROSSHATCH,
                        emphasized = d == 0
                    )
                },
                references = listOf(
                    ReferenceLine(0.40, "0.40 deer", LineStyle.DASHED),
                    ReferenceLine(0.50, "0.50 elk", LineStyle.DASH_DOT)
                ),
                valueFormatter = { formatNumber(it, 2) },
                description = "Momentum at different arrow weights"
            )
        }
        ChartCard(title = "KE vs speed", subtitle = "This arrow from 200 to 400 fps; the marker is your speed") {
            val curve = (200..400 step 20).map { s -> ChartPoint(s.toDouble(), (g * s * s) / 450240.0) }
            LineChart(
                series = listOf(
                    LineSeries("KE", curve, LineStyle.SOLID, MarkerShape.NONE),
                    LineSeries("You", listOf(ChartPoint(v, result.kineticFtLbs ?: 0.0)), LineStyle.SOLID, MarkerShape.DIAMOND)
                ),
                height = 150.dp,
                xLabel = { "${formatNumber(it, 0)} fps" }
            )
        }
    } else {
        HintCard(result.note)
    }
    FormSection("Inputs") {
        NumberSliderField("Finished arrow weight", grains, { grains = it }, 250.0, 800.0, 1.0, unit = "gr", defaultValue = 400.0)
        NumberSliderField("Arrow speed", fps, { fps = it }, 150.0, 420.0, 1.0, unit = "fps", defaultValue = 280.0)
    }
    if (g != null && v != null) HintCard(result.note)
}
