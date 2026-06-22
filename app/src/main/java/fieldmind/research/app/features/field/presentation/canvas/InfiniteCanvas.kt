package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.IntOffset
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity

/**
 * The main infinite canvas composable.
 *
 * Architecture (bottom → top):
 * 1. [GpuCanvasSurface] — OpenGL ES 2.0 surface rendering dot-grid + block outlines
 * 2. [SubcomposeLayout] — Positions [CanvasBlock] composables in a Compose overlay
 * 3. Gesture layer — Pan, zoom, and tap detection
 *
 * **Gesture coordination:** This layer only handles pan/zoom/tap for touches that
 * do NOT start on a block. If a touch starts on a block, the event propagates to
 * the block's own drag handler (in [CanvasBlock]).
 *
 * @param canvasState shared camera state (zoom, pan, selection)
 * @param blocks list of canvas blocks to render
 * @param blockContent composable factory for rendering each block's content
 * @param modifier standard Compose modifier
 * @param onBlockMoved called when a block is dragged to a new position
 * @param onBlockResized called when a block's size changes
 * @param onBlockTapped called when a block is tapped
 */
@Composable
fun InfiniteCanvas(
    canvasState: CanvasState,
    blocks: List<CanvasBlockEntity>,
    modifier: Modifier = Modifier,
    blockContent: @Composable (CanvasBlockEntity, Boolean) -> Unit = { _, _ -> },
    onBlockMoved: ((Long, Float, Float) -> Unit)? = null,
    onBlockResized: ((Long, Float, Float) -> Unit)? = null,
    onBlockTapped: ((Long) -> Unit)? = null
) {
    // Convert selected blocks to BlockRect for GPU highlight rendering
    val selectedBlockRects = remember(blocks, canvasState.selectedBlockIds) {
        blocks
            .filter { it.id in canvasState.selectedBlockIds }
            .map { block ->
                GpuCanvasRenderer.BlockRect(
                    x = block.positionX,
                    y = block.positionY,
                    w = block.width,
                    h = block.height,
                    r = 8f
                )
            }
    }

    /**
     * Returns the topmost block at the given canvas-space coordinate, or null.
     * Blocks are evaluated in reverse z-order (highest z-index first).
     */
    fun blockAtCanvasPoint(x: Float, y: Float): CanvasBlockEntity? {
        return blocks.lastOrNull { block ->
            x in block.positionX..(block.positionX + block.width) &&
            y in block.positionY..(block.positionY + block.height)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // ── Tap detection (for block selection + deselection) ──
            .then(
                Modifier.pointerInput(blocks) {
                    detectTapGestures { tapOffset ->
                        val canvasPos = canvasState.screenToCanvas(tapOffset.x, tapOffset.y)
                        val tappedBlock = blockAtCanvasPoint(canvasPos.x, canvasPos.y)
                        if (tappedBlock != null) {
                            canvasState.selectBlock(tappedBlock.id)
                            onBlockTapped?.invoke(tappedBlock.id)
                        } else {
                            canvasState.clearSelection()
                        }
                    }
                }
            )
    ) {
        // Layer 1: GPU surface for grid + block outlines
        GpuCanvasSurface(
            canvasState = canvasState,
            selectedBlockRects = selectedBlockRects,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Compose block overlay
        SubcomposeLayout(
            modifier = Modifier.fillMaxSize()
        ) { constraints ->
            val placeables = blocks.map { block ->
                subcompose("block_${block.id}") {
                    val isSelected = block.id in canvasState.selectedBlockIds
                    CanvasBlock(
                        block = block,
                        canvasState = canvasState,
                        isSelected = isSelected,
                        onMoved = { x, y -> onBlockMoved?.invoke(block.id, x, y) },
                        onResized = { w, h -> onBlockResized?.invoke(block.id, w, h) },
                        onTapped = { id ->
                            canvasState.selectBlock(id)
                            onBlockTapped?.invoke(id)
                        }
                    ) {
                        blockContent(block, isSelected)
                    }
                }.first()
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEachIndexed { index, placeable ->
                    val block = blocks[index]
                    val screenPos = canvasState.canvasToScreen(block.positionX, block.positionY)

                    placeable.place(
                        position = IntOffset(screenPos.x.toInt(), screenPos.y.toInt()),
                        zIndex = block.zIndex.toFloat()
                    )
                }
            }
        }

        // Layer 3: Pan/Zoom gesture layer (sits above everything)
        // Only activates for touches that do NOT start on a block
        PanZoomLayer(
            canvasState = canvasState,
            blocks = blocks,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Handles pan and zoom gestures for the canvas.
 *
 * This is a separate composable layered on top so it can intercept pointer events
 * without conflicting with block-level drag handlers. It checks whether the initial
 * touch down is on a block — if yes, it passes through; if no, it consumes the
 * gesture for canvas pan/zoom.
 */
@Composable
private fun PanZoomLayer(
    canvasState: CanvasState,
    blocks: List<CanvasBlockEntity>,
    modifier: Modifier = Modifier
) {
    val panZoomModifier = Modifier.pointerInput(blocks) {
        awaitPointerEventScope {
            while (true) {
                val downEvent = awaitPointerEvent()
                if (downEvent.type != PointerEventType.Press) continue

                // Check if the initial touch is on a block
                val firstChange = downEvent.changes.firstOrNull() ?: continue
                val canvasPos = canvasState.screenToCanvas(
                    firstChange.position.x,
                    firstChange.position.y
                )

                val hitBlock = blocks.lastOrNull { block ->
                    canvasPos.x in block.positionX..(block.positionX + block.width) &&
                    canvasPos.y in block.positionY..(block.positionY + block.height)
                }

                if (hitBlock != null) {
                    // Touch started on a block — pass through, let the block's drag handler process it
                    continue
                }

                // Touch started on empty canvas — consume and handle pan/zoom
                downEvent.changes.forEach { it.consume() }

                // Track initial state for 2+ finger zoom
                var previousCentroid = Offset.Zero
                var previousSpan = 0f

                while (true) {
                    val event = awaitPointerEvent()
                    val changes = event.changes

                    if (changes.size >= 2) {
                        // Multi-touch: handle zoom + pan
                        val centroid = changes.map { it.position }
                            .let { positions ->
                                Offset(
                                    positions.map { it.x }.average().toFloat(),
                                    positions.map { it.y }.average().toFloat()
                                )
                            }
                        val span = changes.map { it.position }
                            .let { positions ->
                                val dx = positions.maxOf { it.x } - positions.minOf { it.x }
                                val dy = positions.maxOf { it.y } - positions.minOf { it.y }
                                kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            }

                        if (previousSpan > 0f && previousCentroid != Offset.Zero) {
                            val zoomDelta = span / previousSpan
                            val panDelta = centroid - previousCentroid
                            canvasState.applyZoom(zoomDelta, centroid)
                            canvasState.applyPan(panDelta.x, panDelta.y)
                        }

                        previousCentroid = centroid
                        previousSpan = span
                        changes.forEach { it.consume() }
                    } else if (changes.size == 1) {
                        // Single touch: only pan (zoom handled above)
                        val change = changes.first()
                        val delta = change.position - change.previousPosition
                        canvasState.applyPan(delta.x, delta.y)
                        change.consume()
                    } else {
                        // No touches remaining — exit the gesture loop
                        break
                    }
                }
            }
        }
    }
    Box(modifier = modifier.then(panZoomModifier))
}
