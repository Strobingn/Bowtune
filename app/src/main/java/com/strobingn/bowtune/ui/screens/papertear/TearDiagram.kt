package com.strobingn.bowtune.ui.screens.papertear

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strobingn.bowtune.data.TearType
import com.strobingn.bowtune.ui.charts.TearGlyph

/**
 * 3×3 tear picker laid out the way the nock end moves (top row = nock high).
 * Each cell draws the tear as it looks on paper.
 */
@Composable
fun TearPickerDiagram(
    selected: TearType,
    onSelect: (TearType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Tap the hole you see (nock direction). Center is a bullet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        for (row in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0..2) {
                    val tear = zoneAt(col, row)
                    TearCell(
                        tear = tear,
                        selected = tear == selected,
                        onClick = { onSelect(tear) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Text(
            selected.label + " — " + selected.shortDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TearCell(
    tear: TearType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) cs.inverseSurface else cs.surfaceContainerLow,
            contentColor = if (selected) cs.inverseOnSurface else cs.onSurface
        ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) cs.onSurface else cs.outlineVariant)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            if (selected) {
                MaterialTheme(colorScheme = cs.copy(onSurface = cs.inverseOnSurface, onSurfaceVariant = cs.inverseOnSurface, outlineVariant = cs.outline)) {
                    TearGlyph(tear, size = 52.dp, emphasized = true)
                }
            } else {
                TearGlyph(tear, size = 52.dp)
            }
            Text(
                shortLabel(tear),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

fun shortLabel(tear: TearType): String = when (tear) {
    TearType.BULLET -> "Bullet"
    TearType.NOCK_HIGH -> "High"
    TearType.NOCK_LOW -> "Low"
    TearType.LEFT -> "Left"
    TearType.RIGHT -> "Right"
    TearType.HIGH_LEFT -> "High L"
    TearType.HIGH_RIGHT -> "High R"
    TearType.LOW_LEFT -> "Low L"
    TearType.LOW_RIGHT -> "Low R"
}

private fun zoneAt(col: Int, row: Int): TearType = when (row) {
    0 -> when (col) {
        0 -> TearType.HIGH_LEFT
        1 -> TearType.NOCK_HIGH
        else -> TearType.HIGH_RIGHT
    }
    1 -> when (col) {
        0 -> TearType.LEFT
        1 -> TearType.BULLET
        else -> TearType.RIGHT
    }
    else -> when (col) {
        0 -> TearType.LOW_LEFT
        1 -> TearType.NOCK_LOW
        else -> TearType.LOW_RIGHT
    }
}
