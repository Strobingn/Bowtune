package com.strobingn.bowtune.ui.screens.papertear

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strobingn.bowtune.data.PaperTearGuidance
import com.strobingn.bowtune.data.TearType
import com.strobingn.bowtune.ui.theme.Grey20
import com.strobingn.bowtune.ui.theme.Grey30
import com.strobingn.bowtune.ui.theme.Grey90
import com.strobingn.bowtune.ui.theme.Grey95
import com.strobingn.bowtune.ui.theme.WarningContainerDark
import com.strobingn.bowtune.ui.theme.WarningContainerLight

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaperTearScreen(initialTear: String? = null) {
    var selected by rememberSaveable(initialTear) {
        mutableStateOf(
            initialTear?.takeIf { name -> TearType.entries.any { it.name == name } }
                ?: TearType.BULLET.name
        )
    }
    val tear = TearType.entries.firstOrNull { it.name == selected } ?: TearType.BULLET
    val steps = PaperTearGuidance.stepsFor(tear)
    val dark = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Paper Tear Diagnoser") })
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
                    "Select the tear you see",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
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
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                TipCard(
                    icon = Icons.Filled.Info,
                    title = "Order of attack",
                    body = PaperTearGuidance.GENERAL_HARDWARE
                )
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
