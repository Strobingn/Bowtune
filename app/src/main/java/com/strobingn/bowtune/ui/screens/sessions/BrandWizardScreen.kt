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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.BrandWizardCatalog
import com.strobingn.bowtune.data.GuidedTuneSession
import com.strobingn.bowtune.data.SessionKind
import com.strobingn.bowtune.data.TuneSession
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandWizardScreen(
    wizardId: String,
    onBack: () -> Unit
) {
    val wizard = BrandWizardCatalog.byId(wizardId)
    val context = LocalContext.current
    val app = context.applicationContext as BowTuneApp
    val guidedDao = remember { app.database.guidedTuneSessionDao() }
    val tuneDao = remember { app.database.tuneSessionDao() }
    val setups by app.database.bowSetupDao().observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val activeId by app.preferences.activeSetupId
        .collectAsStateWithLifecycle(initialValue = 0L)
    val scope = rememberCoroutineScope()

    if (wizard == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Wizard") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Text("Wizard not found.", modifier = Modifier.padding(padding).padding(16.dp))
        }
        return
    }

    val steps = wizard.steps
    val lastIndex = steps.lastIndex
    var sessionId by remember { mutableStateOf<Long?>(null) }
    var blockIndex by remember { mutableIntStateOf(0) }
    var setupName by remember { mutableStateOf(setups.firstOrNull { it.id == activeId }?.name ?: wizard.brand) }
    val fieldValues = remember { mutableStateMapOf<String, String>() }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }

    fun fieldKey(stepId: String, key: String) = "$stepId.$key"

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

    suspend fun persist(status: String = GuidedTuneSession.STATUS_IN_PROGRESS, completed: Boolean = false) {
        val now = System.currentTimeMillis()
        val existing = sessionId?.let { guidedDao.getById(it) }
        val base = GuidedTuneSession(
            id = existing?.id ?: 0L,
            protocolId = wizard.id,
            title = wizard.title,
            setupName = setupName,
            startedAtEpochMs = existing?.startedAtEpochMs ?: now,
            updatedAtEpochMs = now,
            completedAtEpochMs = if (completed) now else existing?.completedAtEpochMs,
            currentBlockIndex = blockIndex,
            blockLogsJson = logsToJson(),
            status = status
        )
        sessionId = guidedDao.upsert(base)
    }

    LaunchedEffect(wizard.id) {
        val existing = guidedDao.findInProgress(wizard.id)
        if (existing != null) {
            sessionId = existing.id
            blockIndex = existing.currentBlockIndex.coerceIn(0, lastIndex)
            setupName = existing.setupName.ifBlank { setupName }
            applyJson(existing.blockLogsJson)
        } else {
            persist()
        }
        loaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(wizard.brand) },
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
        val step = steps[blockIndex]
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(wizard.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(wizard.summary, style = MaterialTheme.typography.bodyMedium)
            Text("Step ${blockIndex + 1} of ${steps.size}", style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(
                progress = { (blockIndex + 1f) / steps.size },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                setupName,
                { setupName = it },
                label = { Text("Setup name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(step.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text(step.instructions)
                    step.fields.forEach { field ->
                        val key = fieldKey(step.id, field.key)
                        OutlinedTextField(
                            value = fieldValues[key].orEmpty(),
                            onValueChange = { fieldValues[key] = it },
                            label = { Text(field.label) },
                            placeholder = if (field.hint.isNotBlank()) {{ Text(field.hint) }} else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            savedMessage?.let { Text(it, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                ) { Text(if (blockIndex == 0) "Save & exit" else "Back") }
                Button(
                    onClick = {
                        scope.launch {
                            if (blockIndex < lastIndex) {
                                blockIndex++
                                persist()
                            } else {
                                persist(GuidedTuneSession.STATUS_COMPLETED, completed = true)
                                tuneDao.upsert(
                                    TuneSession(
                                        dateEpochMs = System.currentTimeMillis(),
                                        distanceYd = "4-6 / 20",
                                        scoreOrGroup = "Wizard complete",
                                        notes = "${wizard.title} finished. ${logsToJson()}",
                                        setupName = setupName,
                                        sessionKind = SessionKind.GUIDED
                                    )
                                )
                                savedMessage = "Saved wizard + session log."
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(if (blockIndex < lastIndex) "Next" else "Complete & save") }
            }
        }
    }
}
