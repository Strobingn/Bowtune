package com.strobingn.bowtune.ui.screens.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.ChecklistItem
import com.strobingn.bowtune.data.ChecklistTemplate
import com.strobingn.bowtune.data.ChecklistTemplates
import com.strobingn.bowtune.ui.charts.ProgressRing
import com.strobingn.bowtune.ui.common.StepState
import com.strobingn.bowtune.ui.common.TimelineRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BowTuneApp
    val store = remember { app.checklistStore }
    val prefs = app.preferences
    val templateId by prefs.checklistTemplateId.collectAsStateWithLifecycle(ChecklistTemplates.FULL.id)
    val template = ChecklistTemplates.byId(templateId)
    val checked by store.checkedIds(templateId).collectAsStateWithLifecycle(emptySet())
    val perTemplate = ChecklistTemplates.all.associate { t ->
        val ids by store.checkedIds(t.id).collectAsStateWithLifecycle(emptySet())
        t.id to ids.size
    }
    val scope = rememberCoroutineScope()
    val total = template.items.size
    val done = checked.size
    val progress = if (total == 0) 0f else done.toFloat() / total.toFloat()
    val percent = (progress * 100).toInt()
    val firstOpen = template.items.indexOfFirst { it.id !in checked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tune Checklist", fontWeight = FontWeight.SemiBold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProgressRing(
                            progress = progress,
                            size = 128.dp,
                            stroke = 14.dp,
                            segments = total,
                            filledSegments = done,
                            centerText = "$percent%",
                            caption = "$done of $total"
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(template.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "$percent% · $done of $total steps complete",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                template.blurb,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                Text("Templates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(ChecklistTemplates.all, key = { it.id }) { t ->
                        TemplateCard(
                            template = t,
                            done = perTemplate[t.id] ?: 0,
                            selected = t.id == templateId,
                            onClick = { scope.launch { prefs.setChecklistTemplateId(t.id) } }
                        )
                    }
                }
                Text(
                    "Progress is saved per template on this device. Reset clears only ${template.name}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            itemsIndexed(template.items, key = { _, it -> it.id }) { index, item ->
                val isChecked = item.id in checked
                TimelineRow(
                    number = index + 1,
                    state = when {
                        isChecked -> StepState.DONE
                        index == firstOpen -> StepState.CURRENT
                        else -> StepState.TODO
                    },
                    isLast = index == template.items.lastIndex
                ) {
                    ChecklistCard(
                        item = item,
                        checked = isChecked,
                        onCheckedChange = { value ->
                            scope.launch { store.setChecked(templateId, item.id, value) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: ChecklistTemplate,
    done: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val total = template.items.size.coerceAtLeast(1)
    Card(
        modifier = Modifier
            .width(112.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) cs.primaryContainer else cs.surfaceContainerLow
        ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) cs.onSurface else cs.outlineVariant)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProgressRing(
                progress = done.toFloat() / total,
                size = 52.dp,
                stroke = 6.dp,
                centerText = "${done * 100 / total}%"
            )
            Text(
                template.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChecklistCard(
    item: ChecklistItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                item.phase,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
