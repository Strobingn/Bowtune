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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import com.strobingn.bowtune.data.CoachingLevel
import com.strobingn.bowtune.data.PaperTearGuidance
import com.strobingn.bowtune.data.PaperTearLog
import com.strobingn.bowtune.data.TearType
import com.strobingn.bowtune.data.advanced.AdvancedTuneIds
import com.strobingn.bowtune.ui.common.formatWhen
import com.strobingn.bowtune.ui.theme.Grey20
import com.strobingn.bowtune.ui.theme.Grey30
import com.strobingn.bowtune.ui.theme.Grey90
import com.strobingn.bowtune.ui.theme.Grey95
import com.strobingn.bowtune.ui.theme.WarningContainerDark
import com.strobingn.bowtune.ui.theme.WarningContainerLight
import java.util.Locale
import kotlinx.coroutines.launch

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
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "paper-tear")
        speaking = true
    }

    LaunchedEffect(tear, ttsOn, coaching) {
        if (ttsOn) speakSteps()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paper Tear Diagnoser") },
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
                    Column(Modifier.padding(16.dp)) {
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
            item {
                Text(
                    "Fix steps (in order)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            itemsIndexed(steps) { index, step ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Step ${index + 1}") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.height(18.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(step, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Log this tear", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Saves to paper-tear history and attaches the active setup (${activeName.ifBlank { "none" }}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            distanceFt = "4-6",
                                            notes = logNotes.trim()
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
                items(history, key = { it.id }) { log ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                val label = TearType.entries.firstOrNull { it.name == log.tearType }?.label
                                    ?: log.tearType
                                Text(label, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${formatWhen(log.dateEpochMs)} · ${log.setupName.ifBlank { "no setup" }} · ${log.distanceFt} ft",
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
