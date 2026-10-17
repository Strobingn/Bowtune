package com.strobingn.bowtune.ui.charts

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.strobingn.bowtune.data.ArrowShaft
import com.strobingn.bowtune.data.formatNumber
import com.strobingn.bowtune.data.toDoubleOrNullLenient
import com.strobingn.bowtune.ui.common.ChartCard

/** FOC bands from [com.strobingn.bowtune.data.Calculators.foc]. */
val FocBands = listOf(
    GaugeBand(0.0, 7.0, "Low", FillPattern.DOTS),
    GaugeBand(7.0, 11.0, "Target", FillPattern.DIAGONAL),
    GaugeBand(11.0, 16.0, "Hunt", FillPattern.CROSSHATCH),
    GaugeBand(16.0, 25.0, "High", FillPattern.HORIZONTAL)
)

/** Point → nock order, each component with its own texture. */
fun arrowSegments(
    shaft: Double?,
    point: Double?,
    insert: Double?,
    nock: Double?,
    vane: Double?,
    wrap: Double?
): List<StackSegment> = listOf(
    StackSegment("Point", point ?: 0.0, FillPattern.SOLID),
    StackSegment("Insert", insert ?: 0.0, FillPattern.CROSSHATCH),
    StackSegment("Shaft", shaft ?: 0.0, FillPattern.DIAGONAL),
    StackSegment("Wrap", wrap ?: 0.0, FillPattern.HORIZONTAL),
    StackSegment("Vanes", vane ?: 0.0, FillPattern.DOTS),
    StackSegment("Nock", nock ?: 0.0, FillPattern.OUTLINE)
).filter { it.value > 0.0 }

fun arrowSegments(arrow: ArrowShaft): List<StackSegment> = arrowSegments(
    arrow.gpi.toDoubleOrNullLenient(),
    arrow.pointWeightGr.toDoubleOrNullLenient(),
    arrow.insertWeightGr.toDoubleOrNullLenient(),
    arrow.nockWeightGr.toDoubleOrNullLenient(),
    arrow.vaneWeightGr.toDoubleOrNullLenient(),
    arrow.wrapWeightGr.toDoubleOrNullLenient()
)

@Composable
fun ArrowWeightBreakdown(segments: List<StackSegment>, modifier: Modifier = Modifier) {
    val total = segments.sumOf { it.value }
    ChartCard(
        title = "Total ${formatNumber(total, 1)} gr",
        subtitle = "Weight breakdown, point → nock",
        modifier = modifier
    ) {
        if (segments.isEmpty()) {
            Text("Add component weights to see the breakdown.", style = MaterialTheme.typography.bodySmall)
        } else {
            StackedBar(segments, unit = " gr")
        }
    }
}
