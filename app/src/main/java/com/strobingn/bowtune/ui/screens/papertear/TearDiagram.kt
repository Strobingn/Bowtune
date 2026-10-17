package com.strobingn.bowtune.ui.screens.papertear

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strobingn.bowtune.data.TearType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun TearPickerDiagram(
    selected: TearType,
    onSelect: (TearType) -> Unit,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outline
    val selectedFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    val inkColor = MaterialTheme.colorScheme.onSurface
    val subColor = MaterialTheme.colorScheme.onSurfaceVariant

    val inkArgb = android.graphics.Color.argb(
        (inkColor.alpha * 255).toInt(),
        (inkColor.red * 255).toInt(),
        (inkColor.green * 255).toInt(),
        (inkColor.blue * 255).toInt()
    )

    Column(modifier) {
        Text(
            "Tap the paper tear shape you see (● = point entry, 3 slits = nock/vanes). Center is a clean bullet hole.",
            style = MaterialTheme.typography.bodySmall,
            color = subColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
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
            val labelPaint = Paint().apply {
                color = inkArgb
                textAlign = Paint.Align.CENTER
                textSize = 24f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val native = drawContext.canvas.nativeCanvas
            for (row in 0..2) {
                for (col in 0..2) {
                    val tear = zoneAt(col, row)
                    val topLeft = Offset(col * cw, row * ch)
                    val isSelected = tear == selected
                    if (isSelected) {
                        drawRect(selectedFill, topLeft, Size(cw, ch))
                    }
                    drawRect(
                        outline,
                        topLeft,
                        Size(cw, ch),
                        style = Stroke(width = if (isSelected) 3.dp.toPx() else 1.5.dp.toPx())
                    )

                    val center = Offset(topLeft.x + cw / 2f, topLeft.y + ch * 0.43f)
                    val scale = min(cw, ch) * 0.27f
                    drawPaperTearGlyph(
                        tear = tear,
                        center = center,
                        radius = scale,
                        color = inkColor
                    )

                    native.drawText(
                        shortLabel(tear),
                        topLeft.x + cw / 2f,
                        topLeft.y + ch - 14f,
                        labelPaint
                    )
                }
            }
        }
        Text(
            selected.label + " — " + selected.shortDescription,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = inkColor,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Draws the realistic paper hole + vane tear geometry inside a cell.
 * Nock vector `(nx, ny)` points from the point entry toward the nock tear end.
 */
private fun DrawScope.drawPaperTearGlyph(
    tear: TearType,
    center: Offset,
    radius: Float,
    color: Color
) {
    val (nx, ny) = nockDirectionVector(tear)
    if (tear == TearType.BULLET) {
        // Symmetrical 3-vane star + center point hole
        val angles = listOf(-90.0, 30.0, 150.0)
        angles.forEach { deg ->
            val rad = Math.toRadians(deg)
            val end = Offset(
                center.x + (cos(rad) * radius * 0.9f).toFloat(),
                center.y + (sin(rad) * radius * 0.9f).toFloat()
            )
            drawLine(color, center, end, strokeWidth = 3.5f, cap = StrokeCap.Round)
        }
        drawCircle(color, radius = radius * 0.30f, center = center)
        return
    }

    // Point entry sits opposite the nock direction; nock sits in the tear direction
    val pointPos = Offset(
        center.x - nx * radius * 0.55f,
        center.y - ny * radius * 0.55f
    )
    val nockPos = Offset(
        center.x + nx * radius * 0.55f,
        center.y + ny * radius * 0.55f
    )

    // Jagged shaft tear connecting point entry to nock exit
    drawLine(
        color = color,
        start = pointPos,
        end = nockPos,
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )

    // Point entry circle (solid dot)
    drawCircle(color = color, radius = radius * 0.25f, center = pointPos)

    // 3 Vane slits radiating at the nock end
    val vaneLen = radius * 0.52f
    val angles = listOf(-90.0, 30.0, 150.0)
    angles.forEach { deg ->
        val rad = Math.toRadians(deg)
        val vEnd = Offset(
            nockPos.x + (cos(rad) * vaneLen).toFloat(),
            nockPos.y + (sin(rad) * vaneLen).toFloat()
        )
        drawLine(color, nockPos, vEnd, strokeWidth = 3f, cap = StrokeCap.Round)
    }
}

private fun nockDirectionVector(tear: TearType): Pair<Float, Float> = when (tear) {
    TearType.BULLET -> 0f to 0f
    TearType.NOCK_HIGH -> 0f to -1f
    TearType.NOCK_LOW -> 0f to 1f
    TearType.LEFT -> -1f to 0f
    TearType.RIGHT -> 1f to 0f
    TearType.HIGH_LEFT -> -0.72f to -0.72f
    TearType.HIGH_RIGHT -> 0.72f to -0.72f
    TearType.LOW_LEFT -> -0.72f to 0.72f
    TearType.LOW_RIGHT -> 0.72f to 0.72f
}

/**
 * Visual schematic showing both the tear geometry on paper and the exact bow hardware move
 * (Nock point / Rest / Limb Shift) according to the Easton CR standard.
 */
@Composable
fun TearCorrectionVisualCard(
    tear: TearType,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val inkArgb = android.graphics.Color.argb(
        (ink.alpha * 255).toInt(),
        (ink.red * 255).toInt(),
        (ink.green * 255).toInt(),
        (ink.blue * 255).toInt()
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Visual diagnosis & hardware correction (RH CR)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
            ) {
                val w = size.width
                val h = size.height
                val leftCx = w * 0.24f
                val rightCx = w * 0.72f
                val cy = h * 0.48f

                val textPaint = Paint().apply {
                    color = inkArgb
                    textAlign = Paint.Align.CENTER
                    textSize = 23f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val subPaint = Paint().apply {
                    color = inkArgb
                    textAlign = Paint.Align.CENTER
                    textSize = 20f
                    isAntiAlias = true
                }
                val native = drawContext.canvas.nativeCanvas

                // Divider between Paper View and Bow Adjustment View
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                drawLine(
                    color = muted.copy(alpha = 0.5f),
                    start = Offset(w * 0.46f, 8f),
                    end = Offset(w * 0.46f, h - 8f),
                    strokeWidth = 2f,
                    pathEffect = dash
                )

                // LEFT SIDE: Paper sheet + magnified tear glyph
                val sheetW = min(w * 0.34f, h * 0.68f)
                drawRect(
                    color = muted.copy(alpha = 0.4f),
                    topLeft = Offset(leftCx - sheetW / 2f, cy - sheetW / 2f - 6f),
                    size = Size(sheetW, sheetW),
                    style = Stroke(width = 2f)
                )
                drawPaperTearGlyph(
                    tear = tear,
                    center = Offset(leftCx, cy - 6f),
                    radius = sheetW * 0.34f,
                    color = ink
                )
                native.drawText("Paper @ 4–6 ft", leftCx, h - 8f, subPaint)

                // RIGHT SIDE: Bow Rest / Nock Point Schematic + Directional Move Arrows
                // Draw riser vertical column & berger rest launcher
                val riserX = rightCx - 42f
                drawLine(
                    color = muted,
                    start = Offset(riserX, 16f),
                    end = Offset(riserX, h - 32f),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
                // Rest launcher V-fork at center
                val restCenter = Offset(rightCx, cy - 6f)
                val forkPath = Path().apply {
                    moveTo(restCenter.x - 18f, restCenter.y - 10f)
                    lineTo(restCenter.x, restCenter.y + 8f)
                    lineTo(restCenter.x + 18f, restCenter.y - 10f)
                }
                drawPath(forkPath, color = ink, style = Stroke(width = 4f, cap = StrokeCap.Round))
                drawCircle(color = accent, radius = 6f, center = Offset(restCenter.x, restCenter.y - 2f))

                // Draw directional adjustment arrow(s) based on TearType
                val (primaryLabel, secondaryLabel) = when (tear) {
                    TearType.BULLET -> {
                        drawCircle(
                            color = ink,
                            radius = 26f,
                            center = restCenter,
                            style = Stroke(width = 3f)
                        )
                        "Clean bullet hole" to "Next: 20 yd bare shaft"
                    }
                    TearType.NOCK_HIGH -> {
                        drawMoveArrow(
                            start = Offset(restCenter.x + 42f, restCenter.y - 26f),
                            end = Offset(restCenter.x + 42f, restCenter.y + 22f),
                            color = ink
                        )
                        "Nock ↓ DOWN 1/32\"" to "or Rest ↑ UP slightly"
                    }
                    TearType.NOCK_LOW -> {
                        drawMoveArrow(
                            start = Offset(restCenter.x + 42f, restCenter.y + 22f),
                            end = Offset(restCenter.x + 42f, restCenter.y - 26f),
                            color = ink
                        )
                        "Nock ↑ UP 1/32\"" to "or Rest ↓ DOWN (check shelf)"
                    }
                    TearType.LEFT -> {
                        drawMoveArrow(
                            start = Offset(restCenter.x - 26f, restCenter.y - 26f),
                            end = Offset(restCenter.x + 36f, restCenter.y - 26f),
                            color = ink
                        )
                        "Rest → RIGHT 1/64\"" to "or Limb Shift → L (chase)"
                    }
                    TearType.RIGHT -> {
                        drawMoveArrow(
                            start = Offset(restCenter.x + 36f, restCenter.y - 26f),
                            end = Offset(restCenter.x - 26f, restCenter.y - 26f),
                            color = ink
                        )
                        "Rest ← LEFT (in)" to "or Limb Shift → R + clearance"
                    }
                    TearType.HIGH_LEFT -> {
                        drawMoveArrow(
                            start = Offset(restCenter.x + 44f, restCenter.y - 24f),
                            end = Offset(restCenter.x + 44f, restCenter.y + 18f),
                            color = ink
                        )
                        drawMoveArrow(
                            start = Offset(restCenter.x - 22f, restCenter.y - 28f),
                            end = Offset(restCenter.x + 28f, restCenter.y - 28f),
                            color = muted
                        )
                        "1. Nock ↓ (or Rest ↑)" to "2. Then Rest → RIGHT / LS L"
                    }
                    TearType.HIGH_RIGHT -> {
                        drawMoveArrow(
                            start = Offset(restCenter.x + 44f, restCenter.y - 24f),
                            end = Offset(restCenter.x + 44f, restCenter.y + 18f),
                            color = ink
                        )
                        drawMoveArrow(
                            start = Offset(restCenter.x + 28f, restCenter.y - 28f),
                            end = Offset(restCenter.x - 22f, restCenter.y - 28f),
                            color = muted
                        )
                        "1. Nock ↓ (or Rest ↑)" to "2. Then Rest ← LEFT / LS R"
                    }
                    TearType.LOW_LEFT -> {
                        drawMoveArrow(
                            start = Offset(restCenter.x + 44f, restCenter.y + 18f),
                            end = Offset(restCenter.x + 44f, restCenter.y - 24f),
                            color = ink
                        )
                        drawMoveArrow(
                            start = Offset(restCenter.x - 22f, restCenter.y - 28f),
                            end = Offset(restCenter.x + 28f, restCenter.y - 28f),
                            color = muted
                        )
                        "1. Nock ↑ (or Rest ↓)" to "2. Then Rest → RIGHT / LS L"
                    }
                    TearType.LOW_RIGHT -> {
                        drawMoveArrow(
                            start = Offset(restCenter.x + 44f, restCenter.y + 18f),
                            end = Offset(restCenter.x + 44f, restCenter.y - 24f),
                            color = ink
                        )
                        drawMoveArrow(
                            start = Offset(restCenter.x + 28f, restCenter.y - 28f),
                            end = Offset(restCenter.x - 22f, restCenter.y - 28f),
                            color = muted
                        )
                        "1. Nock ↑ (or Rest ↓)" to "2. Then Rest ← LEFT / LS R"
                    }
                }

                native.drawText(primaryLabel, rightCx, h - 28f, textPaint)
                native.drawText(secondaryLabel, rightCx, h - 6f, subPaint)
            }
        }
    }
}

private fun DrawScope.drawMoveArrow(
    start: Offset,
    end: Offset,
    color: Color
) {
    drawLine(color = color, start = start, end = end, strokeWidth = 4.5f, cap = StrokeCap.Round)
    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val headLen = 13f
    val a1 = angle + Math.toRadians(150.0)
    val a2 = angle - Math.toRadians(150.0)
    drawLine(
        color = color,
        start = end,
        end = Offset(end.x + (cos(a1) * headLen).toFloat(), end.y + (sin(a1) * headLen).toFloat()),
        strokeWidth = 4.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = end,
        end = Offset(end.x + (cos(a2) * headLen).toFloat(), end.y + (sin(a2) * headLen).toFloat()),
        strokeWidth = 4.5f,
        cap = StrokeCap.Round
    )
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
