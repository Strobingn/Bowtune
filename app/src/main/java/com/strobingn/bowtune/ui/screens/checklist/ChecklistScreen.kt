package com.strobingn.bowtune.ui.screens.checklist

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.ChecklistItem
import com.strobingn.bowtune.data.ChecklistTemplates
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChecklistScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BowTuneApp
    val store = remember { app.checklistStore }
    val prefs = app.preferences
    val templateId by prefs.checklistTemplateId.collectAsStateWithLifecycle(ChecklistTemplates.FULL.id)
    val template = ChecklistTemplates.byId(templateId)
    val checked by store.checkedIds(templateId).collectAsStateWithLifecycle(emptySet())
    val scope = rememberCoroutineScope()
    val total = template.items.size
    val done = checked.size
    val progress = if (total == 0) 0f else done.toFloat() / total.toFloat()
    val percent = (progress * 100).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tune Checklist") },
                actions = {
                    IconButton(
                        onClick = { scope.launch { store.clearTemplate(templateId) } }
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "Reset this template")
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Templates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChecklistTemplates.all.forEach { t ->
                        FilterChip(
                            selected = t.id == templateId,
                            onClick = { scope.launch { prefs.setChecklistTemplateId(t.id) } },
                            label = { Text(t.name) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(template.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Text(
                    "$percent% · $done of $total steps complete",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Progress is saved per template on this device. Reset clears only ${template.name}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(template.items, key = { it.id }) { item ->
                ChecklistCard(
                    item = item,
                    checked = item.id in checked,
                    onCheckedChange = { value ->
                        scope.launch { store.setChecked(templateId, item.id, value) }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChecklistCard(
    item: ChecklistItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                item.phase,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = checked, onCheckedChange = onCheckedChange)
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                item.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
