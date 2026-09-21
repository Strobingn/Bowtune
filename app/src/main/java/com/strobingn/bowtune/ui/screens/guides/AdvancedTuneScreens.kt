package com.strobingn.bowtune.ui.screens.guides

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strobingn.bowtune.data.advanced.AdvancedGuide
import com.strobingn.bowtune.data.advanced.AdvancedSection
import com.strobingn.bowtune.data.advanced.AdvancedSectionKind
import com.strobingn.bowtune.data.advanced.AdvancedTuneCatalog
import com.strobingn.bowtune.data.advanced.TuneWalkStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTuneListScreen(
    onOpenGuide: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Tuning") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "For experienced archers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            AdvancedTuneCatalog.DISCLAIMER,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            AdvancedTuneCatalog.ONE_VARIABLE,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            AdvancedTuneCatalog.groups.forEach { group ->
                item {
                    Text(
                        group.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        group.blurb,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(group.guideIds, key = { it }) { id ->
                    val guide = AdvancedTuneCatalog.byId(id) ?: return@items
                    AdvancedGuideCard(guide = guide, onClick = { onOpenGuide(guide.id) })
                }
            }
        }
    }
}

@Composable
fun AdvancedLibraryFeatureCard(onOpenLibrary: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenLibrary),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "Advanced Tuning library",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Bare shaft walkthroughs, nock clocking, French, walk-back, planing, broadheads, CR paper depth.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun AdvancedGuideCard(guide: AdvancedGuide, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                guide.audience,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTuneDetailScreen(
    guideId: String,
    onBack: () -> Unit,
    onOpenRelated: (String) -> Unit,
    onOpenWalkthrough: (guideId: String, sectionId: String) -> Unit
) {
    val guide = AdvancedTuneCatalog.byId(guideId)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(guide?.title ?: "Advanced") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (guide == null) {
            Text("Guide not found.", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    guide.audience,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(guide.summary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pairs with: ${guide.relatedMethods}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        AdvancedTuneCatalog.DISCLAIMER,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            items(guide.sections, key = { it.id }) { section ->
                ExpandableTuneSection(
                    section = section,
                    defaultExpanded = section.kind == AdvancedSectionKind.WALKTHROUGH ||
                        section.kind == AdvancedSectionKind.CALLOUT,
                    onWalkthrough = if (section.steps.isNotEmpty()) {
                        { onOpenWalkthrough(guide.id, section.id) }
                    } else {
                        null
                    },
                    onOpenRelated = onOpenRelated
                )
            }
        }
    }
}

@Composable
private fun ExpandableTuneSection(
    section: AdvancedSection,
    defaultExpanded: Boolean,
    onWalkthrough: (() -> Unit)?,
    onOpenRelated: (String) -> Unit
) {
    var expanded by rememberSaveable(section.id) { mutableStateOf(defaultExpanded) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        sectionKindLabel(section.kind),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        section.heading,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            if (!expanded) {
                val preview = section.paragraphs.firstOrNull()
                    ?: section.steps.firstOrNull()?.what
                    ?: section.bullets.firstOrNull()
                if (preview != null) {
                    Text(
                        preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
            } else {
                section.paragraphs.forEach { para ->
                    Text(para, style = MaterialTheme.typography.bodyMedium)
                }
                section.bullets.forEach { bullet ->
                    Text("• $bullet", style = MaterialTheme.typography.bodyMedium)
                }
                section.steps.forEach { step ->
                    WalkStepCard(step)
                }
                if (onWalkthrough != null) {
                    Button(onClick = onWalkthrough, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text("  Walk through on the range")
                    }
                }
                if (section.relatedIds.isNotEmpty()) {
                    Text("Related methods", fontWeight = FontWeight.SemiBold)
                    section.relatedIds.forEach { id ->
                        OutlinedButton(
                            onClick = { onOpenRelated(id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(AdvancedTuneCatalog.titleOf(id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalkStepCard(step: TuneWalkStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(step.title, fontWeight = FontWeight.SemiBold)
            LabeledBlock("What", step.what)
            LabeledBlock("Why", step.why)
            Text("How", fontWeight = FontWeight.SemiBold)
            step.how.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            if (step.lookFor.isNotEmpty()) {
                Text("Look for", fontWeight = FontWeight.SemiBold)
                step.lookFor.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
            if (step.changeNext.isNotBlank()) {
                Text(
                    "Next: ${step.changeNext}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LabeledBlock(label: String, body: String) {
    Text(label, fontWeight = FontWeight.SemiBold)
    Text(body, style = MaterialTheme.typography.bodyMedium)
}

private fun sectionKindLabel(kind: AdvancedSectionKind): String = when (kind) {
    AdvancedSectionKind.PHYSICS -> "Physics"
    AdvancedSectionKind.PROCEDURE -> "Procedure"
    AdvancedSectionKind.READ -> "How to read it"
    AdvancedSectionKind.CORRECT -> "Correction"
    AdvancedSectionKind.WALKTHROUGH -> "Follow-along"
    AdvancedSectionKind.CROSSLINK -> "Related"
    AdvancedSectionKind.CALLOUT -> "Notes"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTuneWalkthroughScreen(
    guideId: String,
    sectionId: String,
    onBack: () -> Unit
) {
    val guide = AdvancedTuneCatalog.byId(guideId)
    val section = guide?.sections?.firstOrNull { it.id == sectionId }
    val steps = section?.steps.orEmpty()
    var index by rememberSaveable(guideId, sectionId) { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(section?.heading ?: "Walkthrough") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (guide == null || section == null || steps.isEmpty()) {
            Text(
                "No stepped walkthrough for this section — use the expandable cards.",
                modifier = Modifier.padding(padding).padding(16.dp)
            )
            return@Scaffold
        }
        val step = steps[index.coerceIn(0, steps.lastIndex)]
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(guide.title, style = MaterialTheme.typography.labelLarge)
            Text(
                "Step ${index + 1} of ${steps.size}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            LinearProgressIndicator(
                progress = { (index + 1f) / steps.size },
                modifier = Modifier.fillMaxWidth()
            )
            AssistChip(onClick = {}, label = { Text("One variable at a time") })
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { WalkStepCard(step) }
                item {
                    Text(
                        AdvancedTuneCatalog.ONE_VARIABLE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (index > 0) index-- else onBack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (index == 0) "Close" else "Back")
                }
                Button(
                    onClick = {
                        if (index < steps.lastIndex) index++ else onBack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (index < steps.lastIndex) "Next" else "Done")
                }
            }
        }
    }
}
