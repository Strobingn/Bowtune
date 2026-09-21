package com.strobingn.bowtune.ui.screens.papertear

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.strobingn.bowtune.data.TearType

@Composable
fun TearPickerDiagram(
    selected: TearType,
    onSelect: (TearType) -> Unit,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outline
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val labelColor = MaterialTheme.colorScheme.onSurface
    Column(modifier) {
        Text(
            "Tap the hole you see (nock direction). Center is a bullet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        val outlineArgb = android.graphics.Color.argb(
            (outline.alpha * 255).toInt(),
            (outline.red * 255).toInt(),
            (outline.green * 255).toInt(),
            (outline.blue * 255).toInt()
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .pointerInput(selected) {
                    detectTapGestures { tap ->
                        val col = (tap.x / size.width * 3).toInt().coerceIn(0, 2)
                        val row = (tap.y / size.height * 3).toInt().coerceIn(0, 2)
                        onSelect(zoneAt(col, row))
                    }
                }
        ) {
            val cw = size.width / 3f
            val ch = size.height / 3f
            val paint = Paint().apply {
                color = outlineArgb
                textAlign = Paint.Align.CENTER
                textSize = 28f
                isAntiAlias = true
            }
            val native = drawContext.canvas.nativeCanvas
            for (row in 0..2) {
                for (col in 0..2) {
                    val tear = zoneAt(col, row)
                    val topLeft = Offset(col * cw, row * ch)
                    if (tear == selected) {
                        drawRect(fill, topLeft, Size(cw, ch))
                    }
                    drawRect(
                        outline,
                        topLeft,
                        Size(cw, ch),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    native.drawText(
                        shortLabel(tear),
                        topLeft.x + cw / 2f,
                        topLeft.y + ch / 2f + 10f,
                        paint
                    )
                }
            }
        }
        Text(
            selected.label + " — " + selected.shortDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private fun shortLabel(tear: TearType): String = when (tear) {
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
