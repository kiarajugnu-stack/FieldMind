package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.*

/**
 * Pure Compose Canvas background for the infinite canvas.
 *
 * Replaces the OpenGL ES 2.0 [GpuCanvasSurface] with a simpler, more stable
 * Compose Canvas that renders:
 * 1. Subtle dot grid (40px spacing, light gray dots) — toggleable
 * 2. Block selection highlights (semi-transparent rounded rectangles)
 *
 * Performance is achieved through Compose's built-in hardware acceleration
 * (Canvas renders via Skia on the GPU) — no NDK or OpenGL boilerplate needed.
 *
 * @param canvasState shared camera state (zoom, pan, showGrid)
 * @param selectedBlockRects block rectangles to highlight
 * @param modifier standard Compose modifier
 */
@Composable
fun CanvasBackground(
    canvasState: CanvasState,
    selectedBlockRects: List<BlockRect>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (canvasState.showGrid) {
            drawDotGrid(canvasState)
        }
        drawSelectionHighlights(selectedBlockRects, canvasState)
    }
}

/**
 * A rectangle in canvas (logical) coordinates used for selection highlighting.
 */
data class BlockRect(
    val x: Float, val y: Float,
    val w: Float, val h: Float,
    val r: Float = 8f  // corner radius in logical px
)

// ══════════════════════════════════════════════════════════════════════
//  Dot Grid
// ══════════════════════════════════════════════════════════════════════

private const val GRID_SPACING = 40f    // logical px between dots
private const val DOT_RADIUS = 1.5f     // dot radius in screen px
private const val MAX_DOTS = 30_000     // safety cap — reduced from 100K for perf

/**
 * Draw the dot grid within the visible viewport.
 *
 * Calculates which grid cells are visible given the current zoom/pan,
 * then renders all dots with a single [drawPoints] call (batched GPU draw).
 * Previously used individual [drawCircle] per dot (up to 100K draw calls per frame).
 */
private fun DrawScope.drawDotGrid(canvasState: CanvasState) {
    val invZoom = 1f / canvasState.zoom
    val visibleLeft = (-canvasState.panX) * invZoom - GRID_SPACING
    val visibleTop = (-canvasState.panY) * invZoom - GRID_SPACING
    val visibleRight = (size.width - canvasState.panX) * invZoom + GRID_SPACING
    val visibleBottom = (size.height - canvasState.panY) * invZoom + GRID_SPACING

    val startCol = (visibleLeft / GRID_SPACING).toInt() - 1
    val endCol = (visibleRight / GRID_SPACING).toInt() + 1
    val startRow = (visibleTop / GRID_SPACING).toInt() - 1
    val endRow = (visibleBottom / GRID_SPACING).toInt() + 1

    // Safety cap — skip rendering if zoomed out too far
    val totalDots = (endCol - startCol + 1).toLong() * (endRow - startRow + 1).toLong()
    if (totalDots > MAX_DOTS || totalDots <= 0) return

    // Batched dot rendering: collect all dot screen positions, then draw
    // them as a single [drawPoints] call. This replaces 100K individual
    // drawCircle calls with a single GPU-batched draw call.
    val points = mutableListOf<Offset>()
    val capacity = endCol - startCol + 1
    points.ensureCapacity(capacity * capacity)

    for (row in startRow..endRow) {
        for (col in startCol..endCol) {
            val canvasX = col * GRID_SPACING
            val canvasY = row * GRID_SPACING
            points.add(canvasState.canvasToScreen(canvasX, canvasY))
        }
    }

    drawPoints(
        points = points,
        pointMode = androidx.compose.ui.graphics.drawscope.PointMode.Points,
        color = Color(0x1A000000),  // ~10% opacity black
        strokeWidth = DOT_RADIUS * 2f
    )
}

// ══════════════════════════════════════════════════════════════════════
//  Selection Highlights
// ══════════════════════════════════════════════════════════════════════

/**
 * Draw semi-transparent rounded rectangles behind selected blocks.
 */
private fun DrawScope.drawSelectionHighlights(
    rects: List<BlockRect>,
    canvasState: CanvasState
) {
    val highlightColor = Color(0x1F82B1FF)  // Material primary blue at ~12% alpha

    rects.forEach { rect ->
        val screenPos = canvasState.canvasToScreen(rect.x, rect.y)
        val screenW = rect.w * canvasState.zoom
        val screenH = rect.h * canvasState.zoom
        val screenRadius = rect.r * canvasState.zoom

        drawRoundRect(
            color = highlightColor,
            topLeft = screenPos,
            size = Size(screenW, screenH),
            cornerRadius = CornerRadius(screenRadius, screenRadius)
        )
    }
}
