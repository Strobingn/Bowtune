package com.strobingn.bowtune.ui.screens.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.GuidedTuneSession
import com.strobingn.bowtune.data.LiftVerticalTunePlan
import com.strobingn.bowtune.data.TuneSession
import com.strobingn.bowtune.data.ChartData
import com.strobingn.bowtune.data.LEFT_WORDS
import com.strobingn.bowtune.data.OffsetPoint
import com.strobingn.bowtune.data.RIGHT_WORDS
import com.strobingn.bowtune.data.ScoreOrGroup
import com.strobingn.bowtune.data.VERTICAL_DOWN_WORDS
import com.strobingn.bowtune.data.VERTICAL_UP_WORDS
import com.strobingn.bowtune.data.WalkBackPattern
import com.strobingn.bowtune.data.describeLeftPositive
import com.strobingn.bowtune.data.describeRightPositive
import com.strobingn.bowtune.data.describeVertical
import com.strobingn.bowtune.data.formatNumber
import com.strobingn.bowtune.data.parseSignedOffset
import com.strobingn.bowtune.ui.charts.OffsetScatter
import com.strobingn.bowtune.ui.charts.SegmentedTimeline
import com.strobingn.bowtune.ui.charts.WalkBackPlot
import com.strobingn.bowtune.ui.common.ChartCard
import com.strobingn.bowtune.ui.common.FormSection
import com.strobingn.bowtune.ui.common.NumberSlider
import com.strobingn.bowtune.ui.common.NumberSliderField
import com.strobingn.bowtune.ui.common.StepState
import com.strobingn.bowtune.ui.common.TimelineRow
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftVerticalTuneSessionScreen(
    onBack: () -> Unit,
    resumeSessionId: Long? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as BowTuneApp
    val guidedDao = remember { app.database.guidedTuneSessionDao() }
    val tuneDao = remember { app.database.tuneSessionDao() }
    val scope = rememberCoroutineScope()

    val blocks = LiftVerticalTunePlan.blocks
    val wrapUpIndex = blocks.size

    var sessionId by remember { mutableStateOf<Long?>(resumeSessionId) }
    var blockIndex by remember { mutableIntStateOf(0) }
    var setupName by remember { mutableStateOf("Mathews LIFT 29.5") }
    val fieldValues = remember { mutableStateMapOf<String, String>() }
    var bareShaftHl20 by remember { mutableStateOf("") }
    var bareShaftLr20 by remember { mutableStateOf("") }
    var walkBackLr30 by remember { mutableStateOf("") }
    var final30GroupSize by remember { mutableStateOf("") }
    var decisionNotes by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }

    fun fieldKey(blockId: String, key: String) = "$blockId.$key"

    fun logsToJson(): String {
        val o = JSONObject()
        fieldValues.forEach { (k, v) -> o.put(k, v) }
        return o.toString()
    }

    fun applyJson(json: String) {
        fieldValues.clear()
        runCatching {
            val o = JSONObject(json)
            o.keys().forEach { k -> fieldValues[k] = o.optString(k, "") }
        }
    }

    suspend fun persist(partial: GuidedTuneSession.() -> GuidedTuneSession = { this }) {
        val now = System.currentTimeMillis()
        val base = GuidedTuneSession(
            id = sessionId ?: 0L,
            protocolId = LiftVerticalTunePlan.PROTOCOL_ID,
            title = LiftVerticalTunePlan.TITLE,
            setupName = setupName,
            startedAtEpochMs = now,
            updatedAtEpochMs = now,
            completedAtEpochMs = null,
            currentBlockIndex = blockIndex,
            blockLogsJson = logsToJson(),
            status = GuidedTuneSession.STATUS_IN_PROGRESS,
            bareShaftHl20 = bareShaftHl20,
            bareShaftLr20 = bareShaftLr20,
            walkBackLr30 = walkBackLr30,
            final30GroupSize = final30GroupSize,
            decisionNotes = decisionNotes
        ).partial()
        val existing = sessionId?.let { guidedDao.getById(it) }
        val toSave = if (existing != null) {
            base.copy(
                id = existing.id,
                startedAtEpochMs = existing.startedAtEpochMs,
                updatedAtEpochMs = now
            )
        } else {
            base.copy(startedAtEpochMs = now, updatedAtEpochMs = now)
        }
        val id = guidedDao.upsert(toSave)
        sessionId = id
    }

    LaunchedEffect(Unit) {
        val existing = when {
            resumeSessionId != null -> guidedDao.getById(resumeSessionId)
            else -> guidedDao.findInProgress(LiftVerticalTunePlan.PROTOCOL_ID)
        }
        if (existing != null) {
            sessionId = existing.id
            blockIndex = existing.currentBlockIndex.coerceIn(0, wrapUpIndex)
            setupName = existing.setupName.ifBlank { "Mathews LIFT 29.5" }
            applyJson(existing.blockLogsJson)
            bareShaftHl20 = existing.bareShaftHl20
            bareShaftLr20 = existing.bareShaftLr20
            walkBackLr30 = existing.walkBackLr30
            final30GroupSize = existing.final30GroupSize
            decisionNotes = existing.decisionNotes
        } else {
            persist()
        }
        loaded = true
    }

    fun wrapUpComplete(): Boolean =
        bareShaftHl20.isNotBlank() &&
            bareShaftLr20.isNotBlank() &&
            walkBackLr30.isNotBlank() &&
            final30GroupSize.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LIFT 29.5 Vertical") },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            if (loaded) persist()
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!loaded) {
            Text("Loading…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    LiftVerticalTunePlan.PRIORITY_BANNER,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            AssistChip(
                onClick = {},
                label = { Text(LiftVerticalTunePlan.RULE_CHIP, maxLines = 3) }
            )

            OutlinedTextField(
                value = setupName,
                onValueChange = { setupName = it },
                label = { Text("Setup name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                if (blockIndex < wrapUpIndex) {
                    "Block ${blockIndex + 1} of ${blocks.size} · ${blocks[blockIndex].timeRange} min"
                } else {
                    "Wrap-up & decision tree"
                },
                style = MaterialTheme.typography.labelLarge
            )
            SegmentedTimeline(
                weights = blocks.map { it.suggestedMinutes } + WRAP_UP_WEIGHT,
                currentIndex = blockIndex
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("90 min · wrap-up", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (blockIndex < wrapUpIndex) {
                val block = blocks[blockIndex]
                Text(
                    "${block.timeRange} · ${block.title} (~${block.suggestedMinutes} min)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(block.instructions, style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(4.dp))
                Text("Log table", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                block.logFields.forEach { field ->
                    val key = fieldKey(block.id, field.key)
                    val spec = field.numeric
                    if (spec != null) {
                        NumberSliderField(
                            label = field.label,
                            text = fieldValues[key].orEmpty(),
                            onTextChange = { fieldValues[key] = it },
                            min = spec.min,
                            max = spec.max,
                            step = spec.step,
                            unit = spec.unit,
                            defaultValue = if (spec.min < 0) 0.0 else spec.min,
                            supporting = field.hint.ifBlank { null },
                            valueLabel = { v -> (if (v > 0 && spec.min < 0) "+" else "") + formatNumber(v, 3) + " " + spec.unit }
                        )
                    } else {
                        OutlinedTextField(
                            value = fieldValues[key].orEmpty(),
                            onValueChange = { fieldValues[key] = it },
                            label = { Text(field.label) },
                            placeholder = if (field.hint.isNotBlank()) {
                                { Text(field.hint) }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                when (block.id) {
                    "bare_vertical" -> ChartCard(title = "Bare vs fletched", subtitle = "Pass 1 and 2 at 20 yd") {
                        OffsetScatter(bareOffsetsFromLogs(fieldValues))
                    }
                    "walkback" -> ChartCard(title = "Walk-back line", subtitle = "Pass 1 solid · pass 2 dashed") {
                        val passes = ChartData.walkBackFromLiftLogs(fieldValues)
                        WalkBackPlot(passes, distances = listOf(10, 20, 30))
                        val pattern = ChartData.classifyWalkBack(passes.flatMap { it.points })
                        if (pattern != WalkBackPattern.UNKNOWN) {
                            Text("${pattern.label}: ${pattern.advice}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                Text(
                    "Decision tree",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                LiftVerticalTunePlan.decisionTree.forEachIndexed { i, node ->
                    TimelineRow(
                        number = i + 1,
                        state = StepState.TODO,
                        isLast = i == LiftVerticalTunePlan.decisionTree.lastIndex
                    ) {
                        Text(node.condition, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("→ ${node.action}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Required wrap-up (four numbers)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "All four fields required to complete and save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FormSection("Wrap-up numbers") {
                    SignedOffsetField(
                        label = "bareShaftHl20 — bare H/L inches @20",
                        text = bareShaftHl20,
                        onText = { bareShaftHl20 = it },
                        limit = 6.0,
                        step = 0.125,
                        parse = { parseSignedOffset(it, VERTICAL_UP_WORDS, VERTICAL_DOWN_WORDS) },
                        describe = ::describeVertical
                    )
                    SignedOffsetField(
                        label = "bareShaftLr20 — bare L/R inches @20",
                        text = bareShaftLr20,
                        onText = { bareShaftLr20 = it },
                        limit = 6.0,
                        step = 0.125,
                        parse = { parseSignedOffset(it, LEFT_WORDS, RIGHT_WORDS) },
                        describe = ::describeLeftPositive
                    )
                    SignedOffsetField(
                        label = "walkBackLr30 — walk-back L/R @30",
                        text = walkBackLr30,
                        onText = { walkBackLr30 = it },
                        limit = 12.0,
                        step = 0.25,
                        parse = { parseSignedOffset(it, RIGHT_WORDS, LEFT_WORDS) },
                        describe = ::describeRightPositive
                    )
                    GroupSizeField(
                        label = "final30GroupSize — final 30 group",
                        text = final30GroupSize,
                        onText = { final30GroupSize = it }
                    )
                }
                ChartCard(title = "Session picture", subtitle = "Bare offsets from the wrap-up and both walk-back passes") {
                    val wrapPoint = run {
                        val v = parseSignedOffset(bareShaftHl20, VERTICAL_UP_WORDS, VERTICAL_DOWN_WORDS)
                        val h = parseSignedOffset(bareShaftLr20, LEFT_WORDS, RIGHT_WORDS)
                        if (v == null && h == null) emptyList() else listOf(OffsetPoint(1L, -(h ?: 0.0), v ?: 0.0, ChartData.SOURCE_LIFT))
                    }
                    OffsetScatter(bareOffsetsFromLogs(fieldValues) + wrapPoint)
                    WalkBackPlot(ChartData.walkBackFromLiftLogs(fieldValues), distances = listOf(10, 20, 30))
                }
                OutlinedTextField(
                    decisionNotes,
                    { decisionNotes = it },
                    label = { Text("Decision / next-session notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (savedMessage != null) {
                    Text(
                        savedMessage!!,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            if (blockIndex > 0) {
                                blockIndex--
                                persist()
                            } else {
                                persist()
                                onBack()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (blockIndex == 0) "Save & exit" else "Back")
                }

                if (blockIndex < wrapUpIndex) {
                    Button(
                        onClick = {
                            scope.launch {
                                blockIndex++
                                persist()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Next") }
                } else {
                    Button(
                        onClick = {
                            if (!wrapUpComplete()) {
                                savedMessage = "Fill all four wrap-up numbers before saving."
                                return@Button
                            }
                            scope.launch {
                                val now = System.currentTimeMillis()
                                persist {
                                    copy(
                                        status = GuidedTuneSession.STATUS_COMPLETED,
                                        completedAtEpochMs = now,
                                        updatedAtEpochMs = now,
                                        currentBlockIndex = wrapUpIndex,
                                        bareShaftHl20 = bareShaftHl20.trim(),
                                        bareShaftLr20 = bareShaftLr20.trim(),
                                        walkBackLr30 = walkBackLr30.trim(),
                                        final30GroupSize = final30GroupSize.trim(),
                                        decisionNotes = decisionNotes.trim()
                                    )
                                }
                                val summaryNotes = buildString {
                                    append("LIFT 29.5 vertical tune wrap-up: ")
                                    append("bareShaftHl20=")
                                    append(bareShaftHl20.trim())
                                    append("; bareShaftLr20=")
                                    append(bareShaftLr20.trim())
                                    append("; walkBackLr30=")
                                    append(walkBackLr30.trim())
                                    append("; final30GroupSize=")
                                    append(final30GroupSize.trim())
                                    if (decisionNotes.isNotBlank()) {
                                        append(". ")
                                        append(decisionNotes.trim())
                                    }
                                }
                                tuneDao.upsert(
                                    TuneSession(
                                        dateEpochMs = now,
                                        distanceYd = "20–30",
                                        scoreOrGroup = final30GroupSize.trim(),
                                        notes = summaryNotes,
                                        setupName = setupName.trim().ifBlank { LiftVerticalTunePlan.BOW }
                                    )
                                )
                                savedMessage = "Saved guided session + TuneSession summary with four wrap-up numbers."
                            }
                        },
                        enabled = wrapUpComplete(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Complete & save") }
                }
            }
        }
    }
}

private const val WRAP_UP_WEIGHT = 6

/** Pass 1 / pass 2 bare offsets from the bare-vertical block (stored + = high, + = left). */
private fun bareOffsetsFromLogs(fields: Map<String, String>): List<OffsetPoint> =
    listOf(1, 2).mapNotNull { pass ->
        val v = parseSignedOffset(fields["bare_vertical.pass${pass}BareVH"], VERTICAL_UP_WORDS, VERTICAL_DOWN_WORDS)
        val h = parseSignedOffset(fields["bare_vertical.pass${pass}BareHL"], LEFT_WORDS, RIGHT_WORDS)
        if (v == null && h == null) null else OffsetPoint(pass.toLong(), -(h ?: 0.0), v ?: 0.0, ChartData.SOURCE_BARE)
    }

/**
 * Slider for a required wrap-up offset stored as readable text ("0.5 high").
 * Text that cannot be parsed stays editable as text.
 */
@Composable
private fun SignedOffsetField(
    label: String,
    text: String,
    onText: (String) -> Unit,
    limit: Double,
    step: Double,
    parse: (String) -> Double?,
    describe: (Double) -> String
) {
    val value = parse(text)
    if (text.isNotBlank() && value == null) {
        OutlinedTextField(text, onText, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
        return
    }
    NumberSlider(
        label = label,
        value = value,
        onValueChange = { onText(it?.let(describe).orEmpty()) },
        min = -limit,
        max = limit,
        step = step,
        unit = "in",
        defaultValue = 0.0,
        valueLabel = { describe(it) + "\"" }
    )
}

@Composable
private fun GroupSizeField(label: String, text: String, onText: (String) -> Unit) {
    val parsed = ChartData.parseScoreOrGroup(text) as? ScoreOrGroup.Group
    if (text.isNotBlank() && parsed == null) {
        OutlinedTextField(text, onText, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
        return
    }
    fun emit(w: Double?, h: Double?) {
        onText(
            when {
                w == null -> ""
                h == null -> formatNumber(w)
                else -> "${formatNumber(w)}×${formatNumber(h)}"
            }
        )
    }
    Text(label, style = MaterialTheme.typography.labelLarge)
    NumberSlider("Width", parsed?.width, { emit(it, parsed?.height) }, 0.0, 12.0, 0.25, unit = "in", defaultValue = 2.5)
    NumberSlider(
        "Height",
        parsed?.height,
        { emit(parsed?.width ?: it, it) },
        0.0,
        12.0,
        0.25,
        unit = "in",
        defaultValue = 3.0
    )
}
