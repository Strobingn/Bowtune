package com.strobingn.bowtune.ui.screens.papertear

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.AppPreferences
import com.strobingn.bowtune.data.ChartData
import com.strobingn.bowtune.data.CoachingLevel
import com.strobingn.bowtune.data.PaperTearGuidance
import com.strobingn.bowtune.data.PaperTearLog
import com.strobingn.bowtune.data.TearType
import com.strobingn.bowtune.data.advanced.AdvancedTuneIds
import com.strobingn.bowtune.data.formatNumber
import com.strobingn.bowtune.data.inchFraction
import com.strobingn.bowtune.ui.charts.BarChart
import com.strobingn.bowtune.ui.charts.BarDatum
import com.strobingn.bowtune.ui.charts.ChartLegend
import com.strobingn.bowtune.ui.charts.ChartPoint
import com.strobingn.bowtune.ui.charts.LegendItem
import com.strobingn.bowtune.ui.charts.LineChart
import com.strobingn.bowtune.ui.charts.LineSeries
import com.strobingn.bowtune.ui.charts.LineStyle
import com.strobingn.bowtune.ui.charts.MarkerShape
import com.strobingn.bowtune.ui.charts.PolarTearPlot
import com.strobingn.bowtune.ui.charts.TearGlyph
import com.strobingn.bowtune.ui.charts.TearPlotPoint
import com.strobingn.bowtune.ui.charts.rememberChartPalette
import com.strobingn.bowtune.ui.charts.seriesPatterns
import com.strobingn.bowtune.ui.common.ChartCard
import com.strobingn.bowtune.ui.common.NumberSlider
import com.strobingn.bowtune.ui.common.SegmentedChoice
import com.strobingn.bowtune.ui.common.StepState
import com.strobingn.bowtune.ui.common.TimelineRow
import com.strobingn.bowtune.ui.common.formatWhen
import com.strobingn.bowtune.ui.common.shortDate
import com.strobingn.bowtune.ui.theme.Grey20
import com.strobingn.bowtune.ui.theme.Grey30
import com.strobingn.bowtune.ui.theme.Grey90
import com.strobingn.bowtune.ui.theme.Grey95
import com.strobingn.bowtune.ui.theme.WarningContainerDark
import com.strobingn.bowtune.ui.theme.WarningContainerLight
import java.util.Locale
import kotlinx.coroutines.launch

private val PAPER_DISTANCES = listOf("4-6" to "4–6 ft", "8" to "8 ft", "10" to "10 ft", "12" to "12+ ft")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaperTearScreen(
    initialTear: String? = null,
    onOpenAdvanced: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as BowTuneApp
    val scope = rememberCoroutineScope()
    val setups by app.database.bowSetupDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val activeId by app.preferences.activeSetupId.collectAsStateWithLifecycle(0L)
    val coaching by app.preferences.coachingLevel.collectAsStateWithLifecycle(CoachingLevel.STANDARD)
    val ttsOn by app.preferences.ttsEnabled.collectAsStateWithLifecycle(false)
    val ttsRate by app.preferences.ttsRate.collectAsStateWithLifecycle(AppPreferences.DEFAULT_TTS_RATE)
    val history by app.database.paperTearLogDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val activeName = setups.firstOrNull { it.id == activeId }?.name.orEmpty()

    var selected by rememberSaveable(initialTear) {
        mutableStateOf(
            initialTear?.takeIf { name -> TearType.entries.any { it.name == name } }
                ?: TearType.BULLET.name
        )
    }
    val tear = TearType.entries.firstOrNull { it.name == selected } ?: TearType.BULLET
    val steps = PaperTearGuidance.stepsFor(tear, coaching)
    val dark = isSystemInDarkTheme()
    var logNotes by rememberSaveable { mutableStateOf("") }
    var magnitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var distance by rememberSaveable { mutableStateOf("4-6") }
    var speaking by remember { mutableStateOf(false) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    fun speakSteps() {
        val engine = tts ?: return
        val text = buildString {
            append(tear.label)
            append(". ")
            steps.forEachIndexed { i, step ->
                append("Step ${i + 1}. ")
                append(step)
                append(". ")
            }
        }
        engine.setSpeechRate(ttsRate)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "paper-tear")
        speaking = true
    }

    LaunchedEffect(tear, ttsOn, coaching) {
        if (ttsOn) speakSteps()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paper Tear Diagnoser", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(
                        onClick = {
                            if (speaking) {
                                tts?.stop()
                                speaking = false
                            } else {
                                speakSteps()
                            }
                        }
                    ) {
                        Icon(
                            if (speaking) Icons.Filled.Stop else Icons.Filled.RecordVoiceOver,
                            contentDescription = if (speaking) "Stop reading" else "Read steps aloud"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TipCard(
                    icon = Icons.Filled.Info,
                    title = "Distance",
                    body = PaperTearGuidance.DISTANCE_TIP
                )
            }
            item {
                TipCard(
                    icon = Icons.Filled.Warning,
                    title = "Grip torque",
                    body = PaperTearGuidance.GRIP_TORQUE_NOTE,
                    warningStyle = true
                )
            }
            item {
                Text(
                    "Tap the tear you see",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                TearPickerDiagram(
                    selected = tear,
                    onSelect = { selected = it.name }
                )
            }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TearType.entries.forEach { type ->
                        FilterChip(
                            selected = tear == type,
                            onClick = { selected = type.name },
                            label = { Text(type.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (dark) Grey30 else Grey90,
                                selectedLabelColor = if (dark) Grey95 else Grey20,
                                selectedLeadingIconColor = if (dark) Grey95 else Grey20
                            )
                        )
                    }
                }
                Text(
                    "Coaching: ${coaching.label} — change depth on Home → settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TearGlyph(tear, size = 72.dp, magnitudeIn = magnitude, emphasized = true)
                        Column(Modifier.weight(1f)) {
                            Text(
                                tear.label,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                tear.shortDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "Fix steps (in order)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            itemsIndexed(steps) { index, step ->
                TimelineRow(
                    number = index + 1,
                    state = if (index == 0) StepState.CURRENT else StepState.TODO,
                    isLast = index == steps.lastIndex
                ) {
                    Text("Step ${index + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(step, style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Log this tear", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Saves to paper-tear history and attaches the active setup (${activeName.ifBlank { "none" }}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (tear != TearType.BULLET) {
                            NumberSlider(
                                label = "Tear length (hole → nock end)",
                                value = magnitude,
                                onValueChange = { magnitude = it },
                                min = 0.0,
                                max = 3.0,
                                step = 0.125,
                                unit = "in",
                                defaultValue = 0.5,
                                valueLabel = { "${inchFraction(it)}\"" },
                                supporting = "Optional — feeds the tear map and drift charts."
                            )
                        }
                        Text("Paper distance", style = MaterialTheme.typography.labelLarge)
                        SegmentedChoice(
                            options = PAPER_DISTANCES,
                            selected = distance,
                            onSelect = { distance = it }
                        )
                        OutlinedTextField(
                            logNotes,
                            { logNotes = it },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    app.database.paperTearLogDao().upsert(
                                        PaperTearLog(
                                            dateEpochMs = System.currentTimeMillis(),
                                            tearType = tear.name,
                                            setupName = activeName,
                                            distanceFt = distance,
                                            notes = logNotes.trim(),
                                            magnitudeIn = if (tear == TearType.BULLET) 0.0 else magnitude
                                        )
                                    )
                                    logNotes = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save tear") }
                    }
                }
            }
            item {
                Text("Tear history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (history.isEmpty()) {
                item {
                    Text(
                        "No saved tears yet. Diagnose, then tap Save tear.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item { TearHistoryCharts(history) }
                items(history, key = { it.id }) { log ->
                    val type = TearType.fromStored(log.tearType)
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (type != null) TearGlyph(type, size = 40.dp, magnitudeIn = log.magnitudeIn)
                            Column(Modifier.weight(1f)) {
                                Text(type?.label ?: log.tearType, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${formatWhen(log.dateEpochMs)} · ${log.setupName.ifBlank { "no setup" }} · ${log.distanceFt} ft" +
                                        (log.magnitudeIn?.takeIf { it > 0 }?.let { " · ${inchFraction(it)}\"" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (log.notes.isNotBlank()) {
                                    Text(log.notes, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = { scope.launch { app.database.paperTearLogDao().delete(log) } }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                TipCard(
                    icon = Icons.Filled.Info,
                    title = "Order of attack",
                    body = PaperTearGuidance.GENERAL_HARDWARE
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onOpenAdvanced(AdvancedTuneIds.PAPER) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Advanced paper — paradox, minnowing, false tears") }
                OutlinedButton(
                    onClick = { onOpenAdvanced(AdvancedTuneIds.BARE_SHAFT) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Next: bare shaft walkthrough (field confirm)") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        tts?.stop()
                        speaking = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Stop read-aloud") }
            }
        }
    }
}

@Composable
private fun TearHistoryCharts(history: List<PaperTearLog>) {
    val palette = rememberChartPalette()
    val oldestFirst = history.sortedBy { it.dateEpochMs }
    val vectors = oldestFirst.mapNotNull { log -> ChartData.tearVector(log)?.let { log to it } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChartCard(
            title = "Tear map",
            subtitle = "Nock direction × tear length. Newest is ringed with an arrow; hollow = no length logged."
        ) {
            PolarTearPlot(
                points = vectors.mapIndexed { i, (_, v) ->
                    TearPlotPoint(
                        vector = v,
                        recency = if (vectors.size <= 1) 1f else i / (vectors.size - 1f),
                        latest = i == vectors.lastIndex
                    )
                }
            )
        }
        ChartCard(
            title = "Tear drift over sessions",
            subtitle = "Inches from a bullet hole. + = nock right / nock high. Aim for both lines on zero."
        ) {
            LineChart(
                series = listOf(
                    LineSeries(
                        "Left ↔ right",
                        vectors.map { (log, v) -> ChartPoint(log.dateEpochMs.toDouble(), v.x) },
                        LineStyle.SOLID,
                        MarkerShape.CIRCLE,
                        palette.ink
                    ),
                    LineSeries(
                        "Low ↔ high",
                        vectors.map { (log, v) -> ChartPoint(log.dateEpochMs.toDouble(), v.y) },
                        LineStyle.DASHED,
                        MarkerShape.SQUARE,
                        palette.mid
                    )
                ),
                yUnit = "\"",
                xLabel = { shortDate(it.toLong()) },
                band = -0.25..0.25,
                bandLabel = "bullet zone",
                description = "Tear drift over ${vectors.size} logs"
            )
            ChartLegend(
                listOf(
                    LegendItem("Left ↔ right", palette.ink, LineStyle.SOLID, MarkerShape.CIRCLE),
                    LegendItem("Low ↔ high", palette.mid, LineStyle.DASHED, MarkerShape.SQUARE)
                )
            )
        }
        val counts = ChartData.tearTypeCounts(history)
        ChartCard(title = "Tear types", subtitle = "${history.size} logged · ${formatNumber((ChartData.bulletRate(history, history.size) ?: 0.0) * 100, 0)}% bullet") {
            BarChart(
                bars = counts.entries.mapIndexed { i, (type, n) ->
                    BarDatum(
                        label = shortLabel(type),
                        value = n.toDouble(),
                        pattern = if (type == TearType.BULLET) com.strobingn.bowtune.ui.charts.FillPattern.SOLID else seriesPatterns[1 + i % (seriesPatterns.size - 1)],
                        emphasized = type == TearType.BULLET
                    )
                },
                valueFormatter = { formatNumber(it, 0) },
                description = "Tear type counts"
            )
        }
    }
}

@Composable
private fun TipCard(
    icon: ImageVector,
    title: String,
    body: String,
    warningStyle: Boolean = false
) {
    val dark = isSystemInDarkTheme()
    val container: Color
    val content: Color
    if (warningStyle) {
        container = if (dark) WarningContainerDark else WarningContainerLight
        content = if (dark) Grey95 else Grey20
    } else {
        container = MaterialTheme.colorScheme.surfaceVariant
        content = MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = content)
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = content
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = content)
        }
    }
}
