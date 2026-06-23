package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import fieldmind.research.app.features.field.data.canvas.DrawingEntity
import kotlin.math.*

/**
 * A single point in a drawing stroke, in canvas (logical) coordinates.
 *
 * @param x canvas-space X
 * @param y canvas-space Y
 * @param pressure normalized pressure 0..1 (default 0.5)
 */
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f
)

/**
 * An in-memory stroke currently being drawn (not yet saved).
 *
 * @param points the captured points
 * @param tool the tool used
 * @param color ARGB color
 * @param strokeWidth logical px width
 * @param shapeType shape sub-type when tool is SHAPE
 */
data class InProgressStroke(
    val points: MutableList<StrokePoint> = mutableListOf(),
    val tool: DrawingTool = DrawingTool.PEN,
    val color: Long = 0xFF1C1B19,
    val strokeWidth: Float = 3f,
    val shapeType: ShapeType = ShapeType.RECTANGLE
)

/**
 * Compose Canvas overlay that renders all saved drawings plus the current
 * in-progress stroke, and handles touch gestures for drawing.
 *
 * Layer architecture within InfiniteCanvas:
 * 1. [CanvasBackground] — Dot grid + selection highlights
 * 2. **DrawingOverlay** ← YOU ARE HERE
 * 3. SubcomposeLayout — Block composables
 * 4. BlockToolbar / DrawingToolbar
 * 5. Pan/Zoom gesture layer
 *
 * @param canvasState for coordinate transforms
 * @param drawingState for tool settings
 * @param drawings list of saved [DrawingEntity] to render
 * @param onStrokeComplete called when a stroke is finished (for DB save)
 * @param onEraseStroke called when eraser deletes a stroke (passes drawing ID)
 * @param modifier standard modifier
 */
@Composable
fun DrawingOverlay(
    canvasState: CanvasState,
    drawingState: DrawingState,
    drawings: List<DrawingEntity>,
    onStrokeComplete: (InProgressStroke) -> Unit = {},
    onEraseStroke: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // ── In-progress stroke (built during drag) ──
    var currentStroke by remember { mutableStateOf<InProgressStroke?>(null) }

    // ── Determine gesture modifier ──
    val gestureModifier: Modifier = if (!drawingState.isEraser) {
        Modifier.pointerInput(drawingState.activeTool, drawingState.strokeWidth, drawingState.color, drawingState.shapeType) {
            detectDragGestures(
                            onDragStart = { offset ->
                                val canvasPt = canvasState.screenToCanvas(offset.x, offset.y)
                                currentStroke = InProgressStroke(
                                    points = mutableListOf(StrokePoint(canvasPt.x, canvasPt.y)),
                                    tool = drawingState.activeTool,
                                    color = drawingState.color,
                                    strokeWidth = drawingState.effectiveStrokeWidth,
                                    shapeType = drawingState.shapeType
                                )
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val canvasPt = canvasState.screenToCanvas(change.position.x, change.position.y)
                                currentStroke?.let { stroke ->
                                    stroke.points.add(StrokePoint(canvasPt.x, canvasPt.y, change.pressure))
                                }
                            },
                            onDragEnd = {
                                currentStroke?.let { stroke ->
                                    if (stroke.points.size >= 2) {
                                        onStrokeComplete(stroke)
                                    }
                                }
                                currentStroke = null
                            },
                            onDragCancel = {
                                currentStroke = null
                            }
                        )
                    }
    } else {
        // Eraser gesture handler — tap to remove entire stroke
        Modifier.pointerInput(drawings) {
            detectTapGestures { offset ->
                val canvasPt = canvasState.screenToCanvas(offset.x, offset.y)
                // Find the topmost drawing whose stroke contains this point
                val hit = findHitStroke(drawings, canvasPt, canvasState)
                if (hit != null) {
                    onEraseStroke(hit.id)
                }
            }
        }
    }

    // ── Render ──
    Canvas(modifier = modifier.fillMaxSize().then(gestureModifier)) {
        // ── 1. Render saved drawings ──
        drawings.forEach { drawing ->
            drawDrawingEntity(drawing, canvasState)
        }

        // ── 2. Render in-progress stroke ──
        currentStroke?.let { stroke ->
            drawInProgressStroke(stroke, canvasState)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Rendering helpers
// ══════════════════════════════════════════════════════════════════════

/**
 * Render a saved [DrawingEntity] into the current Canvas scope.
 */
private fun DrawScope.drawDrawingEntity(
    drawing: DrawingEntity,
    canvasState: CanvasState
) {
    val strokes = parseStrokeData(drawing.strokeDataJson) ?: return
    val color = Color(drawing.color)
    val alpha = if (drawing.toolType == "highlighter") 0.35f else 1f
    val width = drawing.strokeWidth
    val tool = when (drawing.toolType) {
        "highlighter" -> DrawingTool.HIGHLIGHTER
        "shape" -> DrawingTool.SHAPE
        else -> DrawingTool.PEN
    }

    strokes.forEach { parsedStroke ->
        if (parsedStroke.points.size < 2) return@forEach

        val screenPoints = parsedStroke.points.map { pt ->
            canvasState.canvasToScreen(pt.x, pt.y)
        }

        if (tool == DrawingTool.SHAPE && parsedStroke.points.size >= 2) {
            drawShape(
                points = screenPoints,
                color = color,
                width = width,
                alpha = alpha,
                shapeType = parsedStroke.shapeType ?: ShapeType.RECTANGLE
            )
        } else {
            drawSmoothPath(
                points = screenPoints,
                color = color,
                width = width,
                alpha = alpha
            )
        }
    }
}

/**
 * Render the current in-progress stroke.
 */
private fun DrawScope.drawInProgressStroke(
    stroke: InProgressStroke,
    canvasState: CanvasState
) {
    if (stroke.points.size < 2) return

    val screenPoints = stroke.points.map { pt ->
        canvasState.canvasToScreen(pt.x, pt.y)
    }

    val color = Color(stroke.color)
    val alpha = if (stroke.tool == DrawingTool.HIGHLIGHTER) 0.35f else 1f

    if (stroke.tool == DrawingTool.SHAPE) {
        drawShape(
            points = screenPoints,
            color = color,
            width = stroke.strokeWidth,
            alpha = alpha,
            shapeType = stroke.shapeType
        )
    } else {
        drawSmoothPath(
            points = screenPoints,
            color = color,
            width = stroke.strokeWidth,
            alpha = alpha
        )
    }
}

/**
 * Draw a smooth quadratic bezier path through the given screen-space points.
 */
private fun DrawScope.drawSmoothPath(
    points: List<Offset>,
    color: Color,
    width: Float,
    alpha: Float
) {
    if (points.size < 2) return

    val path = Path()
    path.moveTo(points[0].x, points[0].y)

    // Use quadratic bezier curves for smooth interpolation
    for (i in 1 until points.size) {
        val mid = Offset(
            (points[i - 1].x + points[i].x) / 2f,
            (points[i - 1].y + points[i].y) / 2f
        )
        path.quadraticBezierTo(
            points[i - 1].x, points[i - 1].y,
            mid.x, mid.y
        )
    }

    // Final segment to the last point
    path.lineTo(points.last().x, points.last().y)

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
 * Draw a shape based on the first and last points (drag start → end).
 * Supports rectangle, circle, line, and arrow.
 *
 * @param shapeType the type of shape to draw (default RECTANGLE)
 */
private fun DrawScope.drawShape(
    points: List<Offset>,
    color: Color,
    width: Float,
    alpha: Float,
    shapeType: ShapeType = ShapeType.RECTANGLE
) {
    if (points.size < 2) return

    val start = points.first()
    val end = points.last()

    // For drag-based shapes, use bounding rect
    val rect = Rect(
        left = minOf(start.x, end.x),
        top = minOf(start.y, end.y),
        right = maxOf(start.x, end.x),
        bottom = maxOf(start.y, end.y)
    )

    when (shapeType) {
        ShapeType.RECTANGLE -> {
            drawRect(
                topLeft = rect.topLeft,
                size = rect.size,
                color = color.copy(alpha = alpha * 0.2f),
                style = Fill
            )
            drawRect(
                topLeft = rect.topLeft,
                size = rect.size,
                color = color.copy(alpha = alpha),
                style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        ShapeType.CIRCLE -> {
            val centerX = (start.x + end.x) / 2f
            val centerY = (start.y + end.y) / 2f
            val radiusX = kotlin.math.abs(end.x - start.x) / 2f
            val radiusY = kotlin.math.abs(end.y - start.y) / 2f
            val radius = maxOf(radiusX, radiusY, 1f)
            drawCircle(
                color = color.copy(alpha = alpha * 0.2f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Fill
            )
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = width, cap = StrokeCap.Round)
            )
        }
        ShapeType.LINE -> {
            drawLine(
                color = color.copy(alpha = alpha),
                start = start,
                end = end,
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
        ShapeType.ARROW -> {
            // Draw the main line
            drawLine(
                color = color.copy(alpha = alpha),
                start = start,
                end = end,
                strokeWidth = width,
                cap = StrokeCap.Round
            )
            // Draw arrowhead
            val angle = kotlin.math.atan2(
                (end.y - start.y).toDouble(),
                (end.x - start.x).toDouble()
            )
            val arrowLen = 20f + width * 2f
            val arrowAngle = kotlin.math.PI / 6  // 30 degrees
            val p1 = Offset(
                (end.x - arrowLen * kotlin.math.cos(angle - arrowAngle)).toFloat(),
                (end.y - arrowLen * kotlin.math.sin(angle - arrowAngle)).toFloat()
            )
            val p2 = Offset(
                (end.x - arrowLen * kotlin.math.cos(angle + arrowAngle)).toFloat(),
                (end.y - arrowLen * kotlin.math.sin(angle + arrowAngle)).toFloat()
            )
            val arrowPath = Path().apply {
                moveTo(end.x, end.y)
                lineTo(p1.x, p1.y)
                moveTo(end.x, end.y)
                lineTo(p2.x, p2.y)
            }
            drawPath(
                path = arrowPath,
                color = color.copy(alpha = alpha),
                style = Stroke(width = width + 2f, cap = StrokeCap.Round)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Stroke data serialization
// ══════════════════════════════════════════════════════════════════════

/**
 * Parse the [DrawingEntity.strokeDataJson] into a list of stroke point lists.
 * Each stroke in the JSON array produces a list of [Offset] points in canvas space.
 */
/**
 * A parsed stroke with its points and optional shape type.
 */
data class ParsedStroke(
    val points: List<Offset>,
    val shapeType: ShapeType? = null
)

/**
 * Parse the [DrawingEntity.strokeDataJson] into a list of parsed strokes.
 * Each entry in the JSON array is either a point array (freehand) or
 * a metadata object (shapes) containing a "shape" key and "points" array.
 */
fun parseStrokeData(json: String): List<ParsedStroke>? {
    if (json.isBlank()) return null
    return try {
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).map { strokeIdx ->
            val entry = arr.get(strokeIdx)
            if (entry is org.json.JSONObject && entry.has("shape")) {
                // Shape metadata entry
                val shapeType = try {
                    ShapeType.valueOf(entry.getString("shape"))
                } catch (_: Exception) {
                    null
                }
                val pointsArr = entry.getJSONArray("points")
                val points = (0 until pointsArr.length()).map { ptIdx ->
                    val ptObj = pointsArr.getJSONObject(ptIdx)
                    Offset(
                        ptObj.getDouble("x").toFloat(),
                        ptObj.getDouble("y").toFloat()
                    )
                }
                ParsedStroke(points, shapeType)
            } else {
                // Point array (freehand stroke)
                val strokeArr = entry as org.json.JSONArray
                val points = (0 until strokeArr.length()).map { ptIdx ->
                    val ptObj = strokeArr.getJSONObject(ptIdx)
                    Offset(
                        ptObj.getDouble("x").toFloat(),
                        ptObj.getDouble("y").toFloat()
                    )
                }
                ParsedStroke(points, null)
            }
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Serialize an [InProgressStroke] to a JSON string for storage in [DrawingEntity.strokeDataJson].
 * Each stroke's point array becomes one entry in the outer JSON array.
 * If the stroke is a shape, include shape type metadata.
 */
fun serializeStroke(stroke: InProgressStroke): String {
    val jsonArr = org.json.JSONArray()
    val pointsArr = org.json.JSONArray()

    stroke.points.forEach { pt ->
        val ptObj = org.json.JSONObject().apply {
            put("x", pt.x.toDouble())
            put("y", pt.y.toDouble())
            put("p", pt.pressure.toDouble())
        }
        pointsArr.put(ptObj)
    }

    // Store stroke metadata: shape type if applicable
    if (stroke.tool == DrawingTool.SHAPE) {
        val meta = org.json.JSONObject().apply {
            put("shape", stroke.shapeType.name)
            put("points", pointsArr)
        }
        jsonArr.put(meta)
    } else {
        jsonArr.put(pointsArr)
    }

    return jsonArr.toString()
}

/**
 * Serialize a list of strokes (for batch saving during undo/redo).
 */
fun serializeStrokes(strokes: List<List<StrokePoint>>): String {
    val jsonArr = org.json.JSONArray()
    strokes.forEach { points ->
        val pointsArr = org.json.JSONArray()
        points.forEach { pt ->
            val ptObj = org.json.JSONObject().apply {
                put("x", pt.x.toDouble())
                put("y", pt.y.toDouble())
                put("p", pt.pressure.toDouble())
            }
            pointsArr.put(ptObj)
        }
        jsonArr.put(pointsArr)
    }
    return jsonArr.toString()
}

// ══════════════════════════════════════════════════════════════════════
//  Hit testing for eraser
// ══════════════════════════════════════════════════════════════════════

/**
 * Find the drawing whose stroke contains the given canvas-space point.
 * Returns null if no stroke is near the point.
 */
private fun findHitStroke(
    drawings: List<DrawingEntity>,
    point: Offset,
    canvasState: CanvasState
): DrawingEntity? {
    val hitRadius = 12f / canvasState.zoom  // hit radius in canvas space
    return drawings.lastOrNull { drawing ->
        val strokes = parseStrokeData(drawing.strokeDataJson) ?: return@lastOrNull false
        strokes.any { strokePoints ->
            strokePoints.points.any { pt ->
                val dx = pt.x - point.x
                val dy = pt.y - point.y
                sqrt(dx * dx + dy * dy) < hitRadius
            }
        }
    }
}
