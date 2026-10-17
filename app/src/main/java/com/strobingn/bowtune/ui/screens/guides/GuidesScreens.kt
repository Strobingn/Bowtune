package com.strobingn.bowtune.ui.screens.guides

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strobingn.bowtune.data.BrandWizardCatalog
import com.strobingn.bowtune.data.advanced.AdvancedTuneCatalog
import com.strobingn.bowtune.data.advanced.AdvancedTuneIds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidesListScreen(
    onOpenGuide: (String) -> Unit,
    onOpenWizard: (String) -> Unit = {},
    onOpenAdvancedLibrary: () -> Unit = {},
    onOpenAdvancedGuide: (String) -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Guides") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Advanced methods plus manufacturer horizontal systems. Pair with Tear, Logs, and Checklist.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                TuningOrderFlow(onOpenAdvancedGuide)
            }
            item {
                Text(
                    "Advanced Tuning library",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                AdvancedLibraryFeatureCard(onOpenLibrary = onOpenAdvancedLibrary)
            }
            items(
                listOf(
                    AdvancedTuneIds.BARE_SHAFT,
                    AdvancedTuneIds.FRENCH,
                    AdvancedTuneIds.NOCK_TUNE
                ),
                key = { "adv-$it" }
            ) { id ->
                val guide = AdvancedTuneCatalog.byId(id) ?: return@items
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAdvancedGuide(guide.id) }
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            guide.audience,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(guide.title, fontWeight = FontWeight.SemiBold)
                        Text(guide.summary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Text(
                    "Short guided wizards",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(BrandWizardCatalog.all, key = { "wiz-${it.id}" }) { wizard ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenWizard(wizard.id) }
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(wizard.brand, style = MaterialTheme.typography.labelLarge)
                        Text(wizard.title, fontWeight = FontWeight.SemiBold)
                        Text(wizard.summary, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onOpenWizard(wizard.id) }) { Text("Start wizard") }
                    }
                }
            }
            item {
                Text(
                    "Reference guides",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(GuideCatalog.all, key = { it.id }) { guide ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenGuide(guide.id) }
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            guide.brand,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            guide.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(guide.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideDetailScreen(
    guideId: String,
    onBack: () -> Unit,
    onOpenWizard: (String) -> Unit = {}
) {
    val guide = GuideCatalog.byId(guideId)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(guide?.title ?: "Guide") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (guide == null) {
            Text(
                "Guide not found.",
                modifier = Modifier.padding(padding).padding(16.dp)
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    guide.brand,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(guide.summary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "This is a brand horizontal system. For bare shaft, French, walk-back, and nock clocking open the Advanced Tuning library from the Guides list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                wizardIdForGuide(guide.id)?.let { wiz ->
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onOpenWizard(wiz) }) { Text("Start short wizard") }
                }
            }
            items(guide.sections) { section ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            section.heading,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        section.bullets.forEach { bullet ->
                            Text("• $bullet", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Field-first tuning order as a tappable flow: each node opens its Advanced guide. */
@Composable
private fun TuningOrderFlow(onOpen: (String) -> Unit) {
    val steps = listOf(
        "Paper" to AdvancedTuneIds.PAPER,
        "Bare shaft" to AdvancedTuneIds.BARE_SHAFT,
        "Walk-back" to AdvancedTuneIds.WALKBACK,
        "Broadhead" to AdvancedTuneIds.BROADHEAD
    )
    val cs = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tuning order", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                steps.forEachIndexed { i, (label, id) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { onOpen(id) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(if (i == 0) cs.onSurface else cs.surfaceContainerHighest, CircleShape)
                                .border(1.5.dp, cs.onSurface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${i + 1}",
                                fontWeight = FontWeight.Bold,
                                color = if (i == 0) cs.surface else cs.onSurface
                            )
                        }
                        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    }
                    if (i < steps.lastIndex) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = cs.outline,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(bottom = 18.dp)
                        )
                    }
                }
            }
            Text(
                "Tap a step to open its walkthrough. One variable at a time.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
    }
}

private fun wizardIdForGuide(guideId: String): String? = when (guideId) {
    "pse_ez220" -> "pse_ez220_short"
    "hoyt_xts" -> "hoyt_xts_short"
    "bowtech_deadlock" -> "bowtech_deadlock_short"
    "elite_set" -> "elite_set_short"
    else -> null
}
