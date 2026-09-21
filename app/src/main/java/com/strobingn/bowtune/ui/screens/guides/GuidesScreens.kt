package com.strobingn.bowtune.ui.screens.guides

import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidesListScreen(
    onOpenGuide: (String) -> Unit,
    onOpenWizard: (String) -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Brand Tune Guides") }) }
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
                    "Manufacturer-specific horizontal tune systems. Pair with Paper Tear + Checklist.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

private fun wizardIdForGuide(guideId: String): String? = when (guideId) {
    "pse_ez220" -> "pse_ez220_short"
    "hoyt_xts" -> "hoyt_xts_short"
    "bowtech_deadlock" -> "bowtech_deadlock_short"
    "elite_set" -> "elite_set_short"
    else -> null
}
