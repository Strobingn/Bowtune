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
import androidx.compose.material3.LinearProgressIndicator
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

            val progress = (blockIndex.toFloat() / wrapUpIndex.toFloat()).coerceIn(0f, 1f)
            Text(
                if (blockIndex < wrapUpIndex) {
                    "Block ${blockIndex + 1} of ${blocks.size} · ${blocks[blockIndex].timeRange} min"
                } else {
                    "Wrap-up & decision tree"
                },
                style = MaterialTheme.typography.labelLarge
            )
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())

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
            } else {
                Text(
                    "Decision tree",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                LiftVerticalTunePlan.decisionTree.forEach { node ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(node.condition, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(node.action, style = MaterialTheme.typography.bodyMedium)
                        }
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
                OutlinedTextField(
                    bareShaftHl20,
                    { bareShaftHl20 = it },
                    label = { Text("bareShaftHl20 — bare H/L inches @20") },
                    placeholder = { Text("e.g. +0.5 high") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    bareShaftLr20,
                    { bareShaftLr20 = it },
                    label = { Text("bareShaftLr20 — bare L/R inches @20") },
                    placeholder = { Text("e.g. 0.25 left") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    walkBackLr30,
                    { walkBackLr30 = it },
                    label = { Text("walkBackLr30 — walk-back L/R @30") },
                    placeholder = { Text("e.g. 0.5 right") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    final30GroupSize,
                    { final30GroupSize = it },
                    label = { Text("final30GroupSize — final 30 group") },
                    placeholder = { Text("e.g. 2.5×3.0") },
                    modifier = Modifier.fillMaxWidth()
                )
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
