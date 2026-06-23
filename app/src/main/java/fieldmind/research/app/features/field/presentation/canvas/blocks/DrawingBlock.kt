package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.canvas.DrawingEntity
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.sqrt

/**
 * Inline drawing block for the canvas — a mini freehand drawing surface
 * scoped to this specific block. Strokes are stored as [DrawingEntity] with
 * the block's ID.
 *
 * Content JSON format:
 * ```json
 * { "color": 0xFF1C1B19, "strokeWidth": 3.0 }
 * ```
 *
 * The actual stroke data lives in the [DrawingEntity] table with `blockId` set.
 *
 * @param blockId the ID of this canvas block (used to scope drawings)
 * @param drawings list of [DrawingEntity] filtered to this block
 * @param onContentChanged called when settings change
 * @param isSelected whether this block is selected
 * @param onSaveDrawing called with (blockId, strokeDataJson) to persist a stroke
 */
@Composable
fun DrawingBlock(
    blockId: Long,
    drawings: List<DrawingEntity>,
    onContentChanged: (String) -> Unit,
    isSelected: Boolean,
    onSaveDrawing: ((Long, String) -> Unit)? = null
) {
    // ── Drawing state ──
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var isDrawing by remember { mutableStateOf(false) }
    val strokeColor = Color(0xFF1C1B19)
    val strokeWidth = 3f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFAFAFA))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSelected) {
                        Modifier.pointerInput(blockId) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints = listOf(offset)
                                    isDrawing = true
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPoints = currentPoints + change.position
                                },
                                onDragEnd = {
                                    isDrawing = false
                                    if (currentPoints.size >= 2) {
                                        val json = serializeMiniStroke(currentPoints)
                                        onSaveDrawing?.invoke(blockId, json)
                                    }
                                    currentPoints = emptyList()
                                },
                                onDragCancel = {
                                    isDrawing = false
                                    currentPoints = emptyList()
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            // ── Render saved drawings ──
            drawings.forEach { drawing ->
                drawMiniDrawing(drawing)
            }

            // ── Render current in-progress stroke ──
            if (currentPoints.size >= 2) {
                val path = Path().apply {
                    moveTo(currentPoints[0].x, currentPoints[0].y)
                    for (i in 1 until currentPoints.size) {
                        lineTo(currentPoints[i].x, currentPoints[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // ── Empty state hint (no drawings and not selected) ──
        if (drawings.isEmpty() && !isDrawing) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    MaterialSymbolIcon("draw", defaultWeight = 300),
                    "Drawing",
                    size = 24.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isSelected) "Draw here" else "Tap block to draw",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        // ── Drawing indicator (top-right shows pen icon when has strokes) ──
        if (drawings.isNotEmpty()) {
            Icon(
                MaterialSymbolIcon("draw"),
                "Has drawings",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                size = 14.dp,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Render a saved [DrawingEntity] stroke onto the block's canvas.
 * Parses the stroke data and draws the path.
 */
private fun DrawScope.drawMiniDrawing(drawing: DrawingEntity) {
    val points = parseMiniStroke(drawing.strokeDataJson) ?: return
    if (points.size < 2) return

    val color = Color(drawing.color)
    val width = drawing.strokeWidth
    val alpha = if (drawing.toolType == "highlighter") 0.35f else 1f

    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            // Use quadratic bezier for smooth curves
            val mid = Offset(
                (points[i - 1].x + points[i].x) / 2f,
                (points[i - 1].y + points[i].y) / 2f
            )
            quadraticBezierTo(points[i - 1].x, points[i - 1].y, mid.x, mid.y)
        }
        lineTo(points.last().x, points.last().y)
    }

    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(
            width = width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/**
 * Serialize a mini stroke (list of screen-space points) to JSON for storage.
 * Format: [{"x": 100, "y": 200}, ...]
 */
private fun serializeMiniStroke(points: List<Offset>): String {
    val arr = JSONArray()
    points.forEach { pt ->
        arr.put(JSONObject().apply {
            put("x", pt.x.toDouble())
            put("y", pt.y.toDouble())
        })
    }
    return arr.toString()
}

/**
 * Parse a mini stroke from JSON.
 */
private fun parseMiniStroke(json: String): List<Offset>? {
    if (json.isBlank()) return null
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            Offset(
                obj.getDouble("x").toFloat(),
                obj.getDouble("y").toFloat()
            )
        }
    } catch (_: Exception) { null }
}
