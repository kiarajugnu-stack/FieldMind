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
 * 1. Subtle dot grid (40px spacing, light gray dots)
 * 2. Block selection highlights (semi-transparent rounded rectangles)
 *
 * Performance is achieved through Compose's built-in hardware acceleration
 * (Canvas renders via Skia on the GPU) — no NDK or OpenGL boilerplate needed.
 *
 * @param canvasState shared camera state (zoom, pan)
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
        drawDotGrid(canvasState)
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
private const val MAX_DOTS = 100_000    // safety cap

/**
 * Draw the dot grid within the visible viewport.
 *
 * Calculates which grid cells are visible given the current zoom/pan,
 * then draws dots only for those cells — no off-screen rendering.
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

    // Constant screen-space dot radius (matches original OpenGL gl_PointSize = 3px)

    for (row in startRow..endRow) {
        for (col in startCol..endCol) {
            val canvasX = col * GRID_SPACING
            val canvasY = row * GRID_SPACING
            val screenPt = canvasState.canvasToScreen(canvasX, canvasY)

            drawCircle(
                color = Color(0x1A000000),  // ~10% opacity black
                radius = DOT_RADIUS,
                center = screenPt
            )
        }
    }
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
