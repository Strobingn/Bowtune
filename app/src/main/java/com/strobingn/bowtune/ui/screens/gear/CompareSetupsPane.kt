package com.strobingn.bowtune.ui.screens.gear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strobingn.bowtune.data.BowSetup
import com.strobingn.bowtune.ui.common.EmptyState
import com.strobingn.bowtune.ui.common.formatDay

@Composable
fun CompareSetupsPane(setups: List<BowSetup>) {
    if (setups.size < 2) {
        Column(Modifier.padding(24.dp)) {
            EmptyState("Need two setups", "Add a second bow in Setups, then compare draw weight, Limb Shift, speeds, and due dates side by side.")
        }
        return
    }
    var leftId by remember { mutableStateOf(setups[0].id) }
    var rightId by remember { mutableStateOf(setups[1].id) }
    val left = setups.firstOrNull { it.id == leftId } ?: setups[0]
    val right = setups.firstOrNull { it.id == rightId } ?: setups[1]

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Left", fontWeight = FontWeight.SemiBold)
        setups.forEach { s ->
            FilterChip(selected = leftId == s.id, onClick = { leftId = s.id }, label = { Text(s.name) })
        }
        Text("Right", fontWeight = FontWeight.SemiBold)
        setups.forEach { s ->
            FilterChip(selected = rightId == s.id, onClick = { rightId = s.id }, label = { Text(s.name) })
        }
        val rows = listOf(
            "Name" to (left.name to right.name),
            "Brand" to (left.brand to right.brand),
            "Model" to (left.model to right.model),
            "Draw weight" to (left.drawWeightLbs to right.drawWeightLbs),
            "Draw length" to (left.drawLengthIn to right.drawLengthIn),
            "Rest" to (left.rest to right.rest),
            "Sight" to (left.sight to right.sight),
            "Arrows" to (left.arrows to right.arrows),
            "Limb Shift" to (left.limbShiftSetting to right.limbShiftSetting),
            "Yoke" to (left.yokeTwistNotes to right.yokeTwistNotes),
            "Cams" to (left.camSystem to right.camSystem),
            "IBO fps" to (left.iboSpeedFps to right.iboSpeedFps),
            "Chrono fps" to (left.arrowSpeedFps to right.arrowSpeedFps),
            "Next due" to (
                left.nextTuneDueEpochMs?.let { formatDay(it) }.orEmpty() to
                    right.nextTuneDueEpochMs?.let { formatDay(it) }.orEmpty()
                ),
            "Cam notes" to (left.camTimingNotes to right.camTimingNotes),
            "Cable notes" to (left.cableStretchNotes to right.cableStretchNotes)
        )
        rows.forEach { (label, pair) ->
            val differs = pair.first.trim() != pair.second.trim()
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (differs) "$label (differs)" else label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (differs) FontWeight.Bold else FontWeight.Medium
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(pair.first.ifBlank { "—" }, modifier = Modifier.weight(1f))
                        Text(pair.second.ifBlank { "—" }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
