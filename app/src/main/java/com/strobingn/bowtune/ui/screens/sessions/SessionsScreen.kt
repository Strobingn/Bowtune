package com.strobingn.bowtune.ui.screens.sessions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.BrandWizardCatalog
import com.strobingn.bowtune.data.ChartData
import com.strobingn.bowtune.data.ChecklistTemplates
import com.strobingn.bowtune.data.GuidedTuneSession
import com.strobingn.bowtune.data.LiftVerticalTunePlan
import com.strobingn.bowtune.data.OffsetPoint
import com.strobingn.bowtune.data.ReportBuilder
import com.strobingn.bowtune.data.ScoreOrGroup
import com.strobingn.bowtune.data.SessionKind
import com.strobingn.bowtune.data.TearType
import com.strobingn.bowtune.data.TuneHints
import com.strobingn.bowtune.data.TuneMetrics
import com.strobingn.bowtune.data.TuneSession
import com.strobingn.bowtune.data.WalkBackPass
import com.strobingn.bowtune.data.WalkBackPattern
import com.strobingn.bowtune.data.advanced.AdvancedTuneIds
import com.strobingn.bowtune.data.describeLeftPositive
import com.strobingn.bowtune.data.describeRightPositive
import com.strobingn.bowtune.data.describeVertical
import com.strobingn.bowtune.data.formatNumber
import com.strobingn.bowtune.ui.charts.BarChart
import com.strobingn.bowtune.ui.charts.BarDatum
import com.strobingn.bowtune.ui.charts.ChartLegend
import com.strobingn.bowtune.ui.charts.ChartPoint
import com.strobingn.bowtune.ui.charts.FillPattern
import com.strobingn.bowtune.ui.charts.LegendItem
import com.strobingn.bowtune.ui.charts.LineChart
import com.strobingn.bowtune.ui.charts.LineSeries
import com.strobingn.bowtune.ui.charts.LineStyle
import com.strobingn.bowtune.ui.charts.MarkerShape
import com.strobingn.bowtune.ui.charts.OffsetScatter
import com.strobingn.bowtune.ui.charts.Sparkline
import com.strobingn.bowtune.ui.charts.TearGlyph
import com.strobingn.bowtune.ui.charts.WalkBackPlot
import com.strobingn.bowtune.ui.charts.rememberChartPalette
import com.strobingn.bowtune.ui.charts.seriesPatterns
import com.strobingn.bowtune.ui.common.ChartCard
import com.strobingn.bowtune.ui.common.EmptyState
import com.strobingn.bowtune.ui.common.FormSection
import com.strobingn.bowtune.ui.common.HintCard
import com.strobingn.bowtune.ui.common.NumberSlider
import com.strobingn.bowtune.ui.common.SegmentedChoice
import com.strobingn.bowtune.ui.common.StatRow
import com.strobingn.bowtune.ui.common.formatWhen
import com.strobingn.bowtune.ui.common.shareText
import com.strobingn.bowtune.ui.common.shortDate
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionsScreen(
    onOpenLiftVerticalTune: () -> Unit = {},
    onOpenWizard: (String) -> Unit = {},
    onOpenAdvanced: (String) -> Unit = {},
    onOpenAdvancedLibrary: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as BowTuneApp
    val dao = remember { app.database.tuneSessionDao() }
    val sessions by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    val guided by app.database.guidedTuneSessionDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val setups by app.database.bowSetupDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val tears by app.database.paperTearLogDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val adjustments by app.database.adjustmentLogDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val arrows by app.database.arrowShaftDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val maintenance by app.database.maintenanceLogDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val activeId by app.preferences.activeSetupId.collectAsStateWithLifecycle(0L)
    val templateId by app.preferences.checklistTemplateId.collectAsStateWithLifecycle(ChecklistTemplates.FULL.id)
    val checked by app.checklistStore.checkedIds(templateId).collectAsStateWithLifecycle(emptySet())
    val scope = rememberCoroutineScope()
    var showEditor by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("all") }
    val active = setups.firstOrNull { it.id == activeId }
    val template = ChecklistTemplates.byId(templateId)
    val percent = if (template.items.isEmpty()) 0 else checked.size * 100 / template.items.size

    val visible = sessions.filter { filter == "all" || it.sessionKind == filter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tune Sessions", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(
                        onClick = {
                            val report = ReportBuilder.build(
                                active, sessions, tears, adjustments, arrows, maintenance, percent, template.name
                            )
                            context.shareText("Bow Tune report", report)
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share tuning report")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEditor = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Log session") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SessionChartsPager(sessions, guided) }
            item {
                WizardCard(
                    title = "Advanced Tuning library",
                    subtitle = "Bare shaft walkthroughs, French, walk-back, nock clocking, CR paper depth.",
                    onClick = onOpenAdvancedLibrary
                )
            }
            item {
                WizardCard(
                    title = LiftVerticalTunePlan.CARD_LABEL,
                    subtitle = "Guided bare-shaft-high vertical tune · ${LiftVerticalTunePlan.BOW}",
                    onClick = onOpenLiftVerticalTune
                )
            }
            item {
                Text("Brand wizards", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(BrandWizardCatalog.all, key = { it.id }) { wizard ->
                        Card(
                            modifier = Modifier
                                .width(220.dp)
                                .clickable { onOpenWizard(wizard.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text(wizard.brand, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp))
                                }
                                Text(wizard.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    wizard.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text("Filter", fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val filters = listOf("all" to "All") + SessionKind.all.map { it to SessionKind.label(it) }
                    items(filters, key = { it.first }) { (id, label) ->
                        val count = if (id == "all") sessions.size else sessions.count { it.sessionKind == id }
                        FilterChip(
                            selected = filter == id,
                            onClick = { filter = id },
                            label = { Text("$label · $count") },
                            leadingIcon = if (id != "all") {
                                { Icon(kindIcon(id), contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
            if (visible.isEmpty()) {
                item {
                    EmptyState(
                        "No sessions in this filter",
                        "Start a guided wizard above, or tap + to log a group, paper, bare shaft, walk-back, or broadhead comparison."
                    )
                }
            } else {
                items(visible, key = { it.id }) { session ->
                    SessionRow(session, onDelete = { scope.launch { dao.delete(session) } })
                }
            }
        }
    }

    if (showEditor) {
        SessionEditorDialog(
            defaultSetup = active?.name.orEmpty(),
            onDismiss = { showEditor = false },
            onOpenAdvanced = { id ->
                showEditor = false
                onOpenAdvanced(id)
            },
            onSave = { session ->
                scope.launch {
                    dao.upsert(session)
                    showEditor = false
                }
            }
        )
    }
}

fun kindIcon(kind: String): ImageVector = when (kind) {
    SessionKind.GROUP -> Icons.Filled.TrackChanges
    SessionKind.PAPER -> Icons.Filled.Description
    SessionKind.BARE_SHAFT -> Icons.Filled.Straighten
    SessionKind.WALKBACK -> Icons.AutoMirrored.Filled.DirectionsWalk
    SessionKind.BROADHEAD -> Icons.Filled.ChangeHistory
    SessionKind.VISION -> Icons.Filled.CameraAlt
    else -> Icons.Filled.PlayCircle
}

internal fun flatJson(json: String): Map<String, String> = runCatching {
    val o = JSONObject(json)
    o.keys().asSequence().associateWith { o.optString(it, "") }
}.getOrDefault(emptyMap())

private data class WalkBackSnapshot(val atMs: Long, val source: String, val passes: List<WalkBackPass>)

private fun latestWalkBack(sessions: List<TuneSession>, guided: List<GuidedTuneSession>): WalkBackSnapshot? {
    val fromSessions = sessions.mapNotNull { s ->
        ChartData.walkBackFromSession(s)?.let { WalkBackSnapshot(s.dateEpochMs, "Walk-back log", listOf(it)) }
    }
    val fromLift = guided.filter { it.protocolId == LiftVerticalTunePlan.PROTOCOL_ID }.mapNotNull { g ->
        val passes = ChartData.walkBackFromLiftLogs(flatJson(g.blockLogsJson))
        if (passes.isEmpty()) null else WalkBackSnapshot(g.updatedAtEpochMs, "LIFT 29.5 session", passes)
    }
    return (fromSessions + fromLift).maxByOrNull { it.atMs }
}

@Composable
private fun SessionChartsPager(sessions: List<TuneSession>, guided: List<GuidedTuneSession>) {
    val titles = listOf("Score", "Groups", "Walk-back", "Impacts", "Mix")
    val pager = rememberPagerState { titles.size }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedChoice(
            options = titles.indices.map { it to titles[it] },
            selected = pager.currentPage,
            onSelect = { scope.launch { pager.animateScrollToPage(it) } }
        )
        HorizontalPager(
            state = pager,
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> ScoreTrendCard(sessions)
                1 -> GroupTrendCard(sessions, guided)
                2 -> WalkBackCard(sessions, guided)
                3 -> ChartCard(title = "Bare shaft & broadhead impacts", subtitle = "Offsets vs your fletched / field-point group (inches)") {
                    OffsetScatter(ChartData.offsetPoints(sessions, guided))
                }
                else -> SessionMixCard(sessions)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            val palette = rememberChartPalette()
            titles.indices.forEach { i ->
                Canvas(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (i == pager.currentPage) 18.dp else 6.dp, 6.dp)
                ) {
                    drawRoundRect(
                        if (i == pager.currentPage) palette.ink else palette.soft,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreTrendCard(sessions: List<TuneSession>) {
    val scores = ChartData.scoreTrend(sessions)
    val values = scores.map { it.value }
    val avg = ChartData.movingAverage(values, 3)
    val palette = rememberChartPalette()
    ChartCard(title = "Score trend", subtitle = "Each scored session, with a 3-session moving average") {
        StatRow(
            listOf(
                Triple("Best", values.maxOrNull()?.let { formatNumber(it, 0) } ?: "—", ""),
                Triple("Average", if (values.isEmpty()) "—" else formatNumber(values.average(), 1), ""),
                Triple("Last", values.lastOrNull()?.let { formatNumber(it, 0) } ?: "—", "")
            )
        )
        LineChart(
            series = listOf(
                LineSeries("Score", scores.map { ChartPoint(it.epochMs.toDouble(), it.value) }, LineStyle.SOLID, MarkerShape.CIRCLE, palette.ink),
                LineSeries("3-session avg", scores.mapIndexed { i, s -> ChartPoint(s.epochMs.toDouble(), avg[i]) }, LineStyle.DASHED, MarkerShape.NONE, palette.mid)
            ),
            xLabel = { shortDate(it.toLong()) },
            emptyText = "Log a Group / score session in Score mode to start this chart.",
            description = "Score trend over ${scores.size} sessions"
        )
        ChartLegend(
            listOf(
                LegendItem("Score", palette.ink, LineStyle.SOLID, MarkerShape.CIRCLE),
                LegendItem("3-session avg", palette.mid, LineStyle.DASHED)
            )
        )
    }
}

@Composable
private fun GroupTrendCard(sessions: List<TuneSession>, guided: List<GuidedTuneSession>) {
    val groups = ChartData.groupTrend(sessions, guided)
    val sizes = groups.map { it.second.size }
    val palette = rememberChartPalette()
    ChartCard(title = "Group size trend", subtitle = "Inches — lower is tighter. Includes LIFT final-30 groups.") {
        StatRow(
            listOf(
                Triple("Tightest", sizes.minOrNull()?.let { formatNumber(it, 2) } ?: "—", "in"),
                Triple("Average", if (sizes.isEmpty()) "—" else formatNumber(sizes.average(), 2), "in"),
                Triple("Last", sizes.lastOrNull()?.let { formatNumber(it, 2) } ?: "—", "in")
            )
        )
        LineChart(
            series = listOf(
                LineSeries("Width", groups.map { ChartPoint(it.first.toDouble(), it.second.width) }, LineStyle.SOLID, MarkerShape.CIRCLE, palette.ink),
                LineSeries(
                    "Height",
                    groups.mapNotNull { g -> g.second.height?.let { ChartPoint(g.first.toDouble(), it) } },
                    LineStyle.DASHED,
                    MarkerShape.SQUARE,
                    palette.mid
                )
            ),
            yUnit = "\"",
            xLabel = { shortDate(it.toLong()) },
            yMinOverride = 0.0,
            emptyText = "Log a group size (W × H) to start this chart.",
            description = "Group size over ${groups.size} sessions"
        )
        ChartLegend(
            listOf(
                LegendItem("Width", palette.ink, LineStyle.SOLID, MarkerShape.CIRCLE),
                LegendItem("Height", palette.mid, LineStyle.DASHED, MarkerShape.SQUARE)
            )
        )
    }
}

@Composable
private fun WalkBackCard(sessions: List<TuneSession>, guided: List<GuidedTuneSession>) {
    val latest = latestWalkBack(sessions, guided)
    val pattern = latest?.let { ChartData.classifyWalkBack(it.passes.flatMap { p -> p.points }) }
    ChartCard(
        title = "Walk-back / French line",
        subtitle = latest?.let { "${it.source} · ${formatWhen(it.atMs)}" } ?: "Latest walk-back session or LIFT walk-back block"
    ) {
        WalkBackPlot(latest?.passes.orEmpty())
        if (pattern != null && pattern != WalkBackPattern.UNKNOWN) {
            HintCard("${pattern.label}: ${pattern.advice}")
        }
    }
}

@Composable
private fun SessionMixCard(sessions: List<TuneSession>) {
    val counts = ChartData.sessionKindCounts(sessions)
    val weekly = ChartData.perWeek(sessions.map { it.dateEpochMs }, System.currentTimeMillis())
    ChartCard(title = "Session mix", subtitle = "${sessions.size} logged sessions by type") {
        BarChart(
            bars = counts.entries.mapIndexed { i, (kind, n) ->
                BarDatum(
                    label = shortKind(kind),
                    value = n.toDouble(),
                    pattern = seriesPatterns[i % seriesPatterns.size]
                )
            },
            height = 150.dp,
            valueFormatter = { formatNumber(it, 0) },
            description = "Session counts by type"
        )
        Text("Per week (last 8)", style = MaterialTheme.typography.labelLarge)
        Sparkline(weekly.map { it.toDouble() }, height = 40.dp)
    }
}

private fun shortKind(kind: String): String = when (kind) {
    SessionKind.GROUP -> "Group"
    SessionKind.PAPER -> "Paper"
    SessionKind.BARE_SHAFT -> "Bare"
    SessionKind.WALKBACK -> "Walk"
    SessionKind.BROADHEAD -> "BH"
    SessionKind.VISION -> "Vision"
    SessionKind.GUIDED -> "Wizard"
    else -> kind
}

@Composable
private fun SessionRow(session: TuneSession, onDelete: () -> Unit) {
    val metrics = TuneMetrics.decode(session.metricsJson)
    val result = ChartData.scoreOrGroupFor(session)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(64.dp)
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val tear = TearType.fromStored(session.tearType)
                    when {
                        result is ScoreOrGroup.Score -> {
                            Text(formatNumber(result.value, 0), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("score", style = MaterialTheme.typography.labelSmall)
                        }
                        result is ScoreOrGroup.Group -> {
                            Text(formatNumber(result.size, 2) + "\"", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("group", style = MaterialTheme.typography.labelSmall)
                        }
                        tear != null -> TearGlyph(tear, size = 48.dp, emphasized = true)
                        else -> Icon(kindIcon(session.sessionKind), contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(kindIcon(session.sessionKind), contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        SessionKind.label(session.sessionKind),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .weight(1f)
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                    }
                }
                Text(formatWhen(session.dateEpochMs), fontWeight = FontWeight.Bold)
                Text(
                    buildString {
                        append(session.distanceYd.ifBlank { "?" })
                        append(" yd")
                        if (session.setupName.isNotBlank()) {
                            append(" · ")
                            append(session.setupName)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.scoreOrGroup.isNotBlank()) {
                    Text("Group / score: ${session.scoreOrGroup}", style = MaterialTheme.typography.bodySmall)
                }
                val metricBits = buildList {
                    metrics[TuneMetrics.BARE_V]?.let { add("Bare ${describeVertical(it)}") }
                    metrics[TuneMetrics.BARE_H]?.let { add("Bare ${describeLeftPositive(it)}") }
                    metrics[TuneMetrics.BH_V]?.let { add("BH ${describeVertical(it)}") }
                    metrics[TuneMetrics.BH_H]?.let { add("BH ${describeLeftPositive(it)}") }
                }
                if (metricBits.isNotEmpty()) {
                    Text(metricBits.joinToString(" · "), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                ChartData.walkBackFromSession(session)?.let { wb ->
                    Text(
                        "Walk-back " + wb.points.joinToString("  ") { (yd, off) -> "$yd:${describeRightPositive(off)}" },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Sparkline(wb.points.map { it.second }, height = 24.dp)
                }
                val cond = listOf(session.indoorOutdoor, session.windNote, session.tempNote)
                    .filter { it.isNotBlank() }
                if (cond.isNotEmpty()) {
                    Text("Conditions: ${cond.joinToString(" · ")}", style = MaterialTheme.typography.bodySmall)
                }
                if (session.tearType.isNotBlank()) {
                    Text("Tear: ${session.tearType}", style = MaterialTheme.typography.bodySmall)
                }
                if (session.notes.isNotBlank()) {
                    Text(
                        session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

private enum class ResultMode { SCORE, GROUP, TEXT }

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SessionEditorDialog(
    defaultSetup: String,
    onDismiss: () -> Unit,
    onOpenAdvanced: (String) -> Unit = {},
    onSave: (TuneSession) -> Unit
) {
    var kind by remember { mutableStateOf(SessionKind.GROUP) }
    var distance by remember { mutableStateOf<Double?>(20.0) }
    var mode by remember { mutableStateOf(ResultMode.GROUP) }
    var score by remember { mutableStateOf<Double?>(null) }
    var groupW by remember { mutableStateOf<Double?>(null) }
    var groupH by remember { mutableStateOf<Double?>(null) }
    var scoreText by remember { mutableStateOf("") }
    var setupName by remember { mutableStateOf(defaultSetup) }
    var notes by remember { mutableStateOf("") }
    var indoor by remember { mutableStateOf("indoor") }
    var windMph by remember { mutableStateOf<Double?>(null) }
    var tempF by remember { mutableStateOf<Double?>(null) }
    var tearType by remember { mutableStateOf<TearType?>(null) }
    var bareV by remember { mutableStateOf<Double?>(null) }
    var bareH by remember { mutableStateOf<Double?>(null) }
    val walkBack = remember { mutableStateMapOf<Int, Double>() }
    var fan by remember { mutableStateOf<Boolean?>(null) }
    var offset by remember { mutableStateOf<Boolean?>(null) }
    var bhH by remember { mutableStateOf<Double?>(null) }
    var bhV by remember { mutableStateOf<Double?>(null) }

    val walkPoints = TuneMetrics.WALKBACK_DISTANCES.mapNotNull { yd -> walkBack[yd]?.let { yd to it } }
    val pattern = ChartData.classifyWalkBack(walkPoints)
    val derivedFan = when (pattern) {
        WalkBackPattern.FAN -> true
        WalkBackPattern.PARALLEL, WalkBackPattern.STACKED -> false
        WalkBackPattern.UNKNOWN -> null
    }
    val derivedOffset = when (pattern) {
        WalkBackPattern.PARALLEL -> true
        WalkBackPattern.STACKED -> false
        else -> null
    }

    val hint = when (kind) {
        SessionKind.BARE_SHAFT -> TuneHints.bareShaft(bareV, bareH)
        SessionKind.WALKBACK -> TuneHints.walkBack(fan ?: derivedFan, offset ?: derivedOffset)
        SessionKind.BROADHEAD -> TuneHints.broadhead(bhH, bhV)
        else -> null
    }

    fun buildSession(): TuneSession {
        val metrics = buildMap {
            when (mode) {
                ResultMode.SCORE -> score?.let { put(TuneMetrics.SCORE, it) }
                ResultMode.GROUP -> {
                    groupW?.let { put(TuneMetrics.GROUP_W, it) }
                    if (groupW != null) groupH?.let { put(TuneMetrics.GROUP_H, it) }
                }
                ResultMode.TEXT -> Unit
            }
            windMph?.let { put(TuneMetrics.WIND_MPH, it) }
            tempF?.let { put(TuneMetrics.TEMP_F, it) }
            when (kind) {
                SessionKind.BARE_SHAFT -> {
                    bareV?.let { put(TuneMetrics.BARE_V, it) }
                    bareH?.let { put(TuneMetrics.BARE_H, it) }
                }
                SessionKind.WALKBACK -> walkPoints.forEach { (yd, v) -> put(TuneMetrics.walkBackKey(yd), v) }
                SessionKind.BROADHEAD -> {
                    bhH?.let { put(TuneMetrics.BH_H, it) }
                    bhV?.let { put(TuneMetrics.BH_V, it) }
                }
            }
        }
        val scoreOrGroup = when (mode) {
            ResultMode.SCORE -> score?.let { formatNumber(it, 1) }.orEmpty()
            ResultMode.GROUP -> groupW?.let { w ->
                groupH?.let { h -> "${formatNumber(w)}×${formatNumber(h)} in" } ?: "${formatNumber(w)} in"
            }.orEmpty()
            ResultMode.TEXT -> scoreText.trim()
        }
        val extra = hint?.let { "Hint: $it" }.orEmpty()
        return TuneSession(
            dateEpochMs = System.currentTimeMillis(),
            distanceYd = distance?.let { formatNumber(it, 0) }.orEmpty(),
            scoreOrGroup = scoreOrGroup,
            notes = listOf(notes.trim(), extra).filter { it.isNotBlank() }.joinToString("\n"),
            setupName = setupName.trim(),
            sessionKind = kind,
            indoorOutdoor = indoor,
            windNote = windMph?.let { "${formatNumber(it, 0)} mph" }.orEmpty(),
            tempNote = tempF?.let { "${formatNumber(it, 0)}°F" }.orEmpty(),
            tearType = if (kind == SessionKind.PAPER) tearType?.label.orEmpty() else "",
            metricsJson = TuneMetrics.encode(metrics)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Log session") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
                    },
                    actions = {
                        TextButton(onClick = { onSave(buildSession()) }) { Text("Save", fontWeight = FontWeight.Bold) }
                    }
                )
            }
        ) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        SessionKind.GROUP,
                        SessionKind.PAPER,
                        SessionKind.BARE_SHAFT,
                        SessionKind.WALKBACK,
                        SessionKind.BROADHEAD
                    ).forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(SessionKind.label(k)) },
                            leadingIcon = { Icon(kindIcon(k), contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                FormSection("Basics") {
                    NumberSlider("Distance", distance, { distance = it }, 3.0, 100.0, 1.0, unit = "yd", defaultValue = 20.0)
                    OutlinedTextField(setupName, { setupName = it }, label = { Text("Setup name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                FormSection("Score / group size") {
                    SegmentedChoice(
                        options = listOf(ResultMode.SCORE to "Score", ResultMode.GROUP to "Group size", ResultMode.TEXT to "Text"),
                        selected = mode,
                        onSelect = { mode = it }
                    )
                    when (mode) {
                        ResultMode.SCORE -> NumberSlider("Score", score, { score = it }, 0.0, 600.0, 1.0, defaultValue = 300.0)
                        ResultMode.GROUP -> {
                            NumberSlider("Group width", groupW, { groupW = it }, 0.0, 24.0, 0.25, unit = "in", defaultValue = 3.0)
                            NumberSlider("Group height", groupH, { groupH = it }, 0.0, 24.0, 0.25, unit = "in", defaultValue = 3.0)
                        }
                        ResultMode.TEXT -> OutlinedTextField(
                            scoreText,
                            { scoreText = it },
                            label = { Text("Score / group size") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                FormSection("Conditions") {
                    SegmentedChoice(
                        options = listOf("indoor" to "Indoor", "outdoor" to "Outdoor"),
                        selected = indoor,
                        onSelect = { indoor = it }
                    )
                    NumberSlider("Wind", windMph, { windMph = it }, 0.0, 30.0, 1.0, unit = "mph", defaultValue = 5.0)
                    NumberSlider("Temperature", tempF, { tempF = it }, -10.0, 110.0, 1.0, unit = "°F", defaultValue = 65.0)
                }
                when (kind) {
                    SessionKind.PAPER -> FormSection("Tear type") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            TearType.entries.forEach { t ->
                                FilterChip(
                                    selected = tearType == t,
                                    onClick = { tearType = if (tearType == t) null else t },
                                    label = { Text(t.label) },
                                    leadingIcon = { TearGlyph(t, size = 20.dp) }
                                )
                            }
                        }
                    }
                    SessionKind.BARE_SHAFT -> FormSection("Bare vs fletched (inches)") {
                        NumberSlider(
                            "High / low", bareV, { bareV = it }, -6.0, 6.0, 0.125, unit = "in", defaultValue = 0.0,
                            valueLabel = { describeVertical(it) + "\"" }
                        )
                        NumberSlider(
                            "Left / right", bareH, { bareH = it }, -6.0, 6.0, 0.125, unit = "in", defaultValue = 0.0,
                            valueLabel = { describeLeftPositive(it) + "\"" }
                        )
                        if (bareV != null || bareH != null) {
                            OffsetScatter(
                                listOf(OffsetPoint(0L, -(bareH ?: 0.0), bareV ?: 0.0, ChartData.SOURCE_BARE))
                            )
                        }
                    }
                    SessionKind.WALKBACK -> FormSection("Walk-back offsets") {
                        TuneMetrics.WALKBACK_DISTANCES.forEach { yd ->
                            NumberSlider(
                                "@ $yd yd", walkBack[yd], { v -> if (v == null) walkBack.remove(yd) else walkBack[yd] = v },
                                -12.0, 12.0, 0.25, unit = "in", defaultValue = 0.0,
                                valueLabel = { describeRightPositive(it) + "\"" }
                            )
                        }
                        WalkBackPlot(listOf(WalkBackPass("This session", walkPoints)))
                        if (pattern != WalkBackPattern.UNKNOWN) {
                            Text("Data suggests: ${pattern.label}", fontWeight = FontWeight.SemiBold)
                        }
                        Text("Did the error grow with distance?")
                        SegmentedChoice(
                            options = listOf(true to "Fan / grows", false to "No fan"),
                            selected = fan,
                            onSelect = { fan = if (fan == it) null else it }
                        )
                        Text("Whole line offset equally?")
                        SegmentedChoice(
                            options = listOf(true to "Parallel offset", false to "Not parallel"),
                            selected = offset,
                            onSelect = { offset = if (offset == it) null else it }
                        )
                    }
                    SessionKind.BROADHEAD -> FormSection("Broadhead vs field point (inches)") {
                        NumberSlider(
                            "Left / right", bhH, { bhH = it }, -8.0, 8.0, 0.25, unit = "in", defaultValue = 0.0,
                            valueLabel = { describeLeftPositive(it) + "\"" }
                        )
                        NumberSlider(
                            "High / low", bhV, { bhV = it }, -8.0, 8.0, 0.25, unit = "in", defaultValue = 0.0,
                            valueLabel = { describeVertical(it) + "\"" }
                        )
                        if (bhV != null || bhH != null) {
                            OffsetScatter(
                                listOf(OffsetPoint(0L, -(bhH ?: 0.0), bhV ?: 0.0, ChartData.SOURCE_BROADHEAD)),
                                centerLabel = "Field points"
                            )
                        }
                    }
                    else -> Unit
                }
                hint?.let { HintCard(it) }
                when (kind) {
                    SessionKind.BARE_SHAFT -> TextButton(onClick = { onOpenAdvanced(AdvancedTuneIds.BARE_SHAFT) }) {
                        Text("Open bare shaft walkthrough")
                    }
                    SessionKind.WALKBACK -> TextButton(onClick = { onOpenAdvanced(AdvancedTuneIds.WALKBACK) }) {
                        Text("Open walk-back / French guides")
                    }
                    SessionKind.BROADHEAD -> TextButton(onClick = { onOpenAdvanced(AdvancedTuneIds.BROADHEAD) }) {
                        Text("Open broadhead tuning")
                    }
                    SessionKind.PAPER -> TextButton(onClick = { onOpenAdvanced(AdvancedTuneIds.PAPER) }) {
                        Text("Open advanced paper (CR)")
                    }
                    else -> { }
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onSave(buildSession()) }, modifier = Modifier.fillMaxWidth()) { Text("Save session") }
                Box(Modifier.height(24.dp))
            }
        }
    }
}
