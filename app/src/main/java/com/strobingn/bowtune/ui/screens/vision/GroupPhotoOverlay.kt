package com.strobingn.bowtune.ui.screens.vision

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.strobingn.bowtune.data.GroupMeasurementResult
import com.strobingn.bowtune.data.MeasurementPoint
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
internal fun GroupPhotoOverlay(
    bitmap: Bitmap,
    referencePoints: List<MeasurementPoint>,
    arrowCenters: List<MeasurementPoint>,
    result: GroupMeasurementResult?,
    onImageTap: (MeasurementPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Canvas(
        modifier.pointerInput(bitmap, referencePoints.size, arrowCenters.size) {
            detectTapGestures { tap ->
                val fit = fittedImage(
                    canvasWidth = size.width.toFloat(),
                    canvasHeight = size.height.toFloat(),
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height
                )
                if (tap.x in fit.left..fit.right && tap.y in fit.top..fit.bottom) {
                    onImageTap(
                        MeasurementPoint(
                            x = (tap.x - fit.left) / fit.scale,
                            y = (tap.y - fit.top) / fit.scale
                        )
                    )
                }
            }
        }
    ) {
        val fit = fittedImage(
            canvasWidth = size.width,
            canvasHeight = size.height,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height
        )
        drawImage(
            image = image,
            dstOffset = IntOffset(fit.left.roundToInt(), fit.top.roundToInt()),
            dstSize = IntSize(fit.width.roundToInt(), fit.height.roundToInt())
        )

        fun toCanvas(point: MeasurementPoint) =
            Offset(fit.left + point.x * fit.scale, fit.top + point.y * fit.scale)

        if (referencePoints.size == 2) {
            drawLine(
                color = Color.White,
                start = toCanvas(referencePoints[0]),
                end = toCanvas(referencePoints[1]),
                strokeWidth = 5f
            )
        }
        referencePoints.forEach { point ->
            val center = toCanvas(point)
            drawCircle(Color.Black.copy(alpha = 0.7f), 13f, center)
            drawCircle(Color.White, 10f, center, style = Stroke(width = 3f))
        }

        result?.widestPair?.let { (first, second) ->
            drawLine(
                color = Color.White,
                start = toCanvas(arrowCenters[first]),
                end = toCanvas(arrowCenters[second]),
                strokeWidth = 5f
            )
        }

        val labelPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
        }
        arrowCenters.forEachIndexed { index, point ->
            val center = toCanvas(point)
            drawCircle(Color.Black.copy(alpha = 0.65f), 17f, center)
            drawCircle(Color.White, 14f, center, style = Stroke(width = 3f))
            drawContext.canvas.nativeCanvas.drawText(
                (index + 1).toString(),
                center.x,
                center.y + 8f,
                labelPaint
            )
        }
    }
}

@Composable
internal fun GroupCameraReticle(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = this.center
        val radius = min(size.width, size.height) * 0.28f
        drawCircle(Color.White.copy(alpha = 0.8f), radius, center, style = Stroke(width = 3f))
        drawCircle(Color.White.copy(alpha = 0.6f), radius * 0.5f, center, style = Stroke(width = 2f))
        drawLine(
            Color.White.copy(alpha = 0.55f),
            Offset(center.x - radius, center.y),
            Offset(center.x + radius, center.y),
            2f
        )
        drawLine(
            Color.White.copy(alpha = 0.55f),
            Offset(center.x, center.y - radius),
            Offset(center.x, center.y + radius),
            2f
        )
    }
}

private data class ImageFit(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val scale: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

private fun fittedImage(
    canvasWidth: Float,
    canvasHeight: Float,
    imageWidth: Int,
    imageHeight: Int
): ImageFit {
    val scale = min(canvasWidth / imageWidth, canvasHeight / imageHeight)
    val width = imageWidth * scale
    val height = imageHeight * scale
    return ImageFit(
        left = (canvasWidth - width) / 2f,
        top = (canvasHeight - height) / 2f,
        width = width,
        height = height,
        scale = scale
    )
}
