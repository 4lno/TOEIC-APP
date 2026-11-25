package com.luudev.vocalens.presentation.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.luudev.vocalens.R
import com.luudev.vocalens.domain.model.RecognizedObject
import com.luudev.vocalens.presentation.theme.KoiColors
import kotlin.math.min
import kotlin.math.roundToInt

/** Overlay uses the same centred ContentScale.Fit transform as the source photo. */
@Composable
internal fun DetectionOverlay(imageWidth: Int, imageHeight: Int, objects: List<RecognizedObject>,
    onWordSelected: (RecognizedObject) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.testTag("detection-overlay")) {
        Canvas(Modifier.fillMaxSize()) {
            val frame = fittedImageFrame(imageWidth, imageHeight, size)
            objects.forEach { detection ->
                val box = detection.photoRect(frame)
                val radius = CornerRadius(min(8.dp.toPx(), min(box.width, box.height) / 4))
                drawRoundRect(KoiColors.Primary.copy(alpha = .08f), box.topLeft, box.size, radius)
                drawRoundRect(Color.Black.copy(alpha = .28f), box.topLeft, box.size, radius, style = Stroke(5.dp.toPx()))
                drawRoundRect(KoiColors.SoftWater, box.topLeft, box.size, radius, style = Stroke(1.5.dp.toPx()))
                val length = min(22.dp.toPx(), min(box.width, box.height) / 3)
                val inset = 4.dp.toPx().coerceAtMost(length / 3)
                // Short orange accents make the corners visible on both dark and bright photos.
                for ((point, direction) in listOf(
                    box.topLeft to Offset(1f, 1f), box.topRight to Offset(-1f, 1f),
                    box.bottomLeft to Offset(1f, -1f), box.bottomRight to Offset(-1f, -1f)
                )) {
                    drawLine(KoiColors.Orange, point + Offset(direction.x * inset, 0f),
                        point + Offset(direction.x * length, 0f), 3.dp.toPx(), StrokeCap.Round)
                    drawLine(KoiColors.Orange, point + Offset(0f, direction.y * inset),
                        point + Offset(0f, direction.y * length), 3.dp.toPx(), StrokeCap.Round)
                }
            }
        }
        Layout(modifier = Modifier.fillMaxSize(), content = {
            objects.forEachIndexed { index, detection ->
                val confidence = (detection.confidence * 100).roundToInt().coerceIn(0, 100)
                val description = stringResource(R.string.camera_detection_label, detection.label, confidence)
                Surface(onClick = { onWordSelected(detection) }, color = KoiColors.DeepWater,
                    contentColor = KoiColors.Surface, shape = RoundedCornerShape(13.dp),
                    shadowElevation = 4.dp, border = BorderStroke(1.dp, KoiColors.SoftWater.copy(alpha = .65f)),
                    modifier = Modifier.testTag("detection-label-$index").semantics { contentDescription = description }) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(6.dp).background(KoiColors.Orange, CircleShape))
                        Text(detection.label, modifier = Modifier.weight(1f, fill = false), maxLines = 1,
                            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold)
                        Text("$confidence%", color = KoiColors.SoftWater, maxLines = 1,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val frame = fittedImageFrame(imageWidth, imageHeight, Size(width.toFloat(), height.toFloat()))
            val maxLabelWidth = min(frame.width.roundToInt(), 260.dp.roundToPx()).coerceAtLeast(0)
            val labelConstraints = Constraints(maxWidth = maxLabelWidth, maxHeight = frame.height.roundToInt().coerceAtLeast(0))
            val placeables = measurables.map { it.measure(labelConstraints) }
            layout(width, height) {
                val occupied = mutableListOf<Rect>()
                placeables.forEachIndexed { index, label ->
                    val box = objects[index].photoRect(frame)
                    val gap = 8.dp.toPx()
                    val candidates = listOf(
                        Offset(box.left + gap, box.top + gap),
                        Offset(box.left + gap, box.bottom - label.height - gap),
                        Offset(box.right - label.width - gap, box.top + gap),
                        Offset(box.right - label.width - gap, box.bottom - label.height - gap)
                    ).map { origin ->
                        Offset(origin.x.coerceIn(frame.left, (frame.right - label.width).coerceAtLeast(frame.left)),
                            origin.y.coerceIn(frame.top, (frame.bottom - label.height).coerceAtLeast(frame.top)))
                    }
                    val location = candidates.firstOrNull { candidate ->
                        val area = Rect(candidate, Size(label.width.toFloat(), label.height.toFloat()))
                        occupied.none { it.overlaps(area) }
                    } ?: candidates.first()
                    occupied += Rect(location, Size(label.width.toFloat(), label.height.toFloat()))
                    label.place(location.x.roundToInt(), location.y.roundToInt())
                }
            }
        }
    }
}

private fun fittedImageFrame(width: Int, height: Int, viewport: Size): Rect {
    val scale = min(viewport.width / width.coerceAtLeast(1), viewport.height / height.coerceAtLeast(1))
    val size = Size(width * scale, height * scale)
    return Rect(Offset((viewport.width - size.width) / 2, (viewport.height - size.height) / 2), size)
}

private fun RecognizedObject.photoRect(frame: Rect) = Rect(
    frame.left + bounds.left * frame.width, frame.top + bounds.top * frame.height,
    frame.left + bounds.right * frame.width, frame.top + bounds.bottom * frame.height
)
