package com.strobingn.bowtune.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.strobingn.bowtune.data.formatNumber
import com.strobingn.bowtune.data.parseLooseNumber
import com.strobingn.bowtune.data.snapToStep
import com.strobingn.bowtune.data.strictNumberOrNull
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.max

private const val MAX_TICKED_STEPS = 40

fun decimalsFor(step: Double): Int =
    if (step >= 1.0) 0 else ceil(-log10(step) - 1e-9).toInt().coerceIn(1, 3)

/**
 * Slider + −/+ stepper + tap-the-value to type an exact number.
 * [value] null means "not set": the thumb rests at [defaultValue] and the first touch sets it.
 * Typed values outside [min]..[max] are kept as typed; the slider just pins to the end.
 */
@Composable
fun NumberSlider(
    label: String,
    value: Double?,
    onValueChange: (Double?) -> Unit,
    min: Double,
    max: Double,
    step: Double,
    modifier: Modifier = Modifier,
    unit: String = "",
    defaultValue: Double = (min + max) / 2,
    allowClear: Boolean = true,
    supporting: String? = null,
    valueLabel: (Double) -> String = { formatNumber(it, decimalsFor(step)) + if (unit.isBlank()) "" else " $unit" },
    onFinished: () -> Unit = {}
) {
    var editing by remember { mutableStateOf(false) }
    val shown = (value ?: defaultValue).coerceIn(min, max)
    val totalSteps = ((max - min) / step).toInt()
    val tickSteps = if (totalSteps in 2..MAX_TICKED_STEPS) totalSteps - 1 else 0
    val display = value?.let(valueLabel) ?: "Not set"

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (value == null) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clickable { editing = true }
                    .semantics { contentDescription = "$label $display. Tap to type an exact value." }
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        display,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
            if (allowClear && value != null) {
                IconButton(onClick = { onValueChange(null) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear $label", modifier = Modifier.size(18.dp))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = { onValueChange(snapToStep((value ?: defaultValue) - step, min, max, step)) },
                modifier = Modifier.size(34.dp)
            ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease $label") }
            Slider(
                value = shown.toFloat(),
                onValueChange = { onValueChange(snapToStep(it.toDouble(), min, max, step)) },
                onValueChangeFinished = onFinished,
                valueRange = min.toFloat()..max.toFloat(),
                steps = tickSteps,
                colors = if (value == null) {
                    SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.outline,
                        activeTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                } else {
                    SliderDefaults.colors()
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .semantics { stateDescription = display }
            )
            FilledTonalIconButton(
                onClick = { onValueChange(snapToStep((value ?: defaultValue) + step, min, max, step)) },
                modifier = Modifier.size(34.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = "Increase $label") }
        }
        if (supporting != null) {
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (editing) {
        ExactValueDialog(
            label = label,
            unit = unit,
            initial = value?.let { formatNumber(it, max(decimalsFor(step), 2)) }.orEmpty(),
            onDismiss = { editing = false },
            onConfirm = {
                onValueChange(it)
                editing = false
                onFinished()
            }
        )
    }
}

@Composable
private fun ExactValueDialog(
    label: String,
    unit: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (Double?) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val parsed = parseLooseNumber(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(if (unit.isBlank()) "Exact value" else "Exact value ($unit)") },
                supportingText = { Text("Decimals or fractions like 1/8. Leave blank to clear.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(if (text.isBlank()) null else parsed) },
                enabled = text.isBlank() || parsed != null
            ) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * [NumberSlider] bound to the app's String-typed Room fields. Text that is not a plain number
 * (e.g. older free-text notes) is shown as an editable text field so nothing is lost.
 */
@Composable
fun NumberSliderField(
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
    min: Double,
    max: Double,
    step: Double,
    modifier: Modifier = Modifier,
    unit: String = "",
    defaultValue: Double = (min + max) / 2,
    supporting: String? = null,
    valueLabel: ((Double) -> String)? = null
) {
    val parsed = strictNumberOrNull(text)
    if (text.isNotBlank() && parsed == null) {
        Column(modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text(label) },
                supportingText = { Text("Free text kept from an earlier version — clear it to use the slider.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }
    val decimals = decimalsFor(step)
    NumberSlider(
        label = label,
        value = parsed,
        onValueChange = { onTextChange(it?.let { v -> formatNumber(v, max(decimals, 3)) }.orEmpty()) },
        min = min,
        max = max,
        step = step,
        modifier = modifier,
        unit = unit,
        defaultValue = defaultValue,
        supporting = supporting,
        valueLabel = valueLabel ?: { formatNumber(it, max(decimals, 2)) + if (unit.isBlank()) "" else " $unit" }
    )
}

/** Integer −/+ stepper with a tappable value for typing. */
@Composable
fun IntStepper(
    label: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
    step: Int = 1,
    unit: String = "",
    defaultValue: Int = min,
    supporting: String? = null
) {
    var editing by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            FilledTonalIconButton(
                onClick = { onValueChange(((value ?: defaultValue) - step).coerceIn(min, max)) },
                modifier = Modifier.size(36.dp)
            ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease $label") }
            Text(
                value?.let { "$it${if (unit.isBlank()) "" else " $unit"}" } ?: "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(86.dp)
                    .clickable { editing = true }
                    .padding(vertical = 6.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            FilledTonalIconButton(
                onClick = { onValueChange(((value ?: defaultValue) + step).coerceIn(min, max)) },
                modifier = Modifier.size(36.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = "Increase $label") }
        }
        if (supporting != null) {
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (editing) {
        ExactValueDialog(
            label = label,
            unit = unit,
            initial = value?.toString().orEmpty(),
            onDismiss = { editing = false },
            onConfirm = {
                onValueChange(it?.toInt())
                editing = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedChoice(
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, text) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

/** Discrete slider whose stops are named (e.g. Beginner · Standard · Pro). */
@Composable
fun LabeledStepSlider(
    label: String,
    stops: List<String>,
    index: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(
                stops.getOrElse(index) { "" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = index.toFloat(),
            onValueChange = { onIndexChange(it.toInt().coerceIn(0, stops.lastIndex)) },
            valueRange = 0f..stops.lastIndex.toFloat(),
            steps = (stops.size - 2).coerceAtLeast(0),
            modifier = Modifier.semantics { stateDescription = stops.getOrElse(index) { "" } }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            stops.forEachIndexed { i, s ->
                Text(
                    s,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (i == index) FontWeight.Bold else FontWeight.Normal,
                    color = if (i == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (supporting != null) {
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
