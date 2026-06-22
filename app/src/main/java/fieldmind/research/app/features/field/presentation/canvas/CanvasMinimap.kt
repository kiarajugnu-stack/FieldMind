package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity

/**
 * A minimap widget for the infinite canvas.
 *
 * Shows a scaled-down overview of all blocks on the canvas as colored
 * rectangles, with a red-bordered rectangle indicating the current viewport.
 *
 * Tap or drag on the minimap to pan the main canvas to the corresponding
 * position. The minimap also adapts its scale based on the bounding box
 * of all blocks.
 *
 * @param blocks all blocks on the canvas
 * @param canvasState the shared camera state (zoom, pan)
 * @param viewportWidth the width of the canvas viewport in screen pixels
 * @param viewportHeight the height of the canvas viewport in screen pixels
 * @param modifier standard Compose modifier
 * @param show whether the minimap should be visible
 */
@Composable
fun CanvasMinimap(
    blocks: List<CanvasBlockEntity>,
    canvasState: CanvasState,
    viewportWidth: Float,
    viewportHeight: Float,
    modifier: Modifier = Modifier,
    show: Boolean = true
) {
    if (!show || blocks.isEmpty()) return

    val density = LocalDensity.current
    val minimapSize = with(density) { Size(120.dp.toPx(), 80.dp.toPx()) }
    val padding = 8f // padding inside minimap in px

    // Compute bounding box of all blocks
    val bounds = remember(blocks) {
        if (blocks.isEmpty()) return@remember null
        val minX = blocks.minOf { it.positionX }
        val minY = blocks.minOf { it.positionY }
        val maxX = blocks.maxOf { it.positionX + it.width }
        val maxY = blocks.maxOf { it.positionY + it.height }
        androidx.compose.ui.geometry.Rect(
            left = minX, top = minY,
            right = maxX, bottom = maxY
        )
    }

    if (bounds == null) return

    // Compute scale factor to fit all blocks in minimap with padding
    val scaleX = (minimapSize.width - 2 * padding) / bounds.width
    val scaleY = (minimapSize.height - 2 * padding) / bounds.height
    val mapScale = minOf(scaleX, scaleY).coerceAtMost(1f) // cap at 1:1

    Box(
        modifier = modifier
            .width(120.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
            )
    ) {
        // ── Minimap canvas ──
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(blocks) {
                    detectTapGestures { tapOffset ->
                        // Convert tap position to canvas space
                        val canvasX = bounds.left + (tapOffset.x - padding) / mapScale
                        val canvasY = bounds.top + (tapOffset.y - padding) / mapScale
                        // Center the viewport on the tapped position
                        val newPanX = -(canvasX * canvasState.zoom - viewportWidth / 2f)
                        val newPanY = -(canvasY * canvasState.zoom - viewportHeight / 2f)
                        canvasState.setPan(newPanX, newPanY)
                    }
                }
                .pointerInput(blocks, mapScale) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Convert drag delta from minimap space to canvas space, then to pan delta.
                        // The viewport indicator on the minimap should follow the finger:
                        // dragging right (positive dx) means the viewport shows content to the right,
                        // which in canvas coordinates means canvasLeft increases → panX decreases.
                        //   canvasLeft = (0 - panX) / zoom  →  ΔpanX = -ΔcanvasLeft * zoom
                        //   ΔcanvasLeft = dragAmount.x / mapScale
                        //   ∴ ΔpanX = -dragAmount.x * zoom / mapScale
                        canvasState.applyPan(
                            -dragAmount.x * canvasState.zoom / mapScale,
                            -dragAmount.y * canvasState.zoom / mapScale
                        )
                    }
                }
        ) {
            // ── Background grid dots (subtle) ──
            val gridSpacing = 8f * mapScale // scaled grid spacing
            var gx = bounds.left + (bounds.left % gridSpacing)
            while (gx < bounds.right) {
                var gy = bounds.top + (bounds.top % gridSpacing)
                while (gy < bounds.bottom) {
                    val sx = padding + (gx - bounds.left) * mapScale
                    val sy = padding + (gy - bounds.top) * mapScale
                    drawCircle(
                        color = Color(0x22000000),
                        radius = 0.5f,
                        center = Offset(sx, sy)
                    )
                    gy += gridSpacing
                }
                gx += gridSpacing
            }

            // ── Block rectangles ──
            for (block in blocks) {
                val bx = padding + (block.positionX - bounds.left) * mapScale
                val by = padding + (block.positionY - bounds.top) * mapScale
                val bw = block.width * mapScale
                val bh = block.height * mapScale

                val blockColor = when (block.type) {
                    "text" -> Color(0xFF5F7F52)
                    "image" -> Color(0xFF4A90D9)
                    "pdf" -> Color(0xFFE67E22)
                    "figure" -> Color(0xFF9B59B6)
                    "table" -> Color(0xFF1ABC9C)
                    "sticky" -> Color(0xFFF1C40F)
                    "drawing" -> Color(0xFFE74C3C)
                    "voice" -> Color(0xFF3498DB)
                    "equation" -> Color(0xFF2ECC71)
                    else -> Color(0xFF95A5A6)
                }

                drawRect(
                    color = if (block.id in canvasState.selectedBlockIds)
                        MaterialTheme.colorScheme.primary
                    else
                        blockColor.copy(alpha = 0.6f),
                    topLeft = Offset(bx, by),
                    size = Size(bw.coerceAtLeast(2f), bh.coerceAtLeast(2f))
                )
            }

            // ── Viewport indicator (red rectangle) ──
            drawViewportIndicator(
                canvasState = canvasState,
                bounds = bounds,
                mapScale = mapScale,
                padding = padding,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight
            )
        }
    }
}

/**
 * Draws the red viewport indicator rectangle on the minimap.
 *
 * The viewport is derived from the current camera pan and zoom:
 * - The visible canvas area is the screen rect transformed to canvas space
 * - This is drawn as a red outlined rectangle on the minimap
 */
private fun DrawScope.drawViewportIndicator(
    canvasState: CanvasState,
    bounds: androidx.compose.ui.geometry.Rect,
    mapScale: Float,
    padding: Float,
    viewportWidth: Float,
    viewportHeight: Float
) {
    val zoom = canvasState.zoom
    val panX = canvasState.panX
    val panY = canvasState.panY

    // Convert screen viewport corners to canvas coordinates using inverse of canvasToScreen
    // screenX = canvasX * zoom + panX  =>  canvasX = (screenX - panX) / zoom
    val canvasLeft = (0f - panX) / zoom
    val canvasTop = (0f - panY) / zoom
    val canvasRight = (viewportWidth - panX) / zoom
    val canvasBottom = (viewportHeight - panY) / zoom

    // Map to minimap coordinates
    val vx = padding + (canvasLeft - bounds.left) * mapScale
    val vy = padding + (canvasTop - bounds.top) * mapScale
    val vw = (canvasRight - canvasLeft) * mapScale
    val vh = (canvasBottom - canvasTop) * mapScale

    // Clamp to minimap bounds
    val clampedX = vx.coerceIn(padding, size.width - padding)
    val clampedY = vy.coerceIn(padding, size.height - padding)
    val clampedW = vw.coerceAtMost(size.width - padding - clampedX)
    val clampedH = vh.coerceAtMost(size.height - padding - clampedY)

    if (clampedW > 2f && clampedH > 2f) {
        drawRect(
            color = Color(0xFFFF5252),
            topLeft = Offset(clampedX, clampedY),
            size = Size(clampedW, clampedH),
            style = Stroke(width = 1.5f)
        )
    }
}
