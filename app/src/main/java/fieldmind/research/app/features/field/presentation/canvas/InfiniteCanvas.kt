package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity

/**
 * The main infinite canvas composable.
 *
 * Architecture (bottom → top):
 * 1. [GpuCanvasSurface] — OpenGL ES 2.0 surface rendering dot-grid + block outlines
 * 2. [SubcomposeLayout] — Positions [CanvasBlock] composables in a Compose overlay
 * 3. [BlockToolbar] — Floating action bar above the selected block
 * 4. Gesture layer — Pan, zoom, and tap detection
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
 * @param onBlockDelete called when the delete action is triggered
 * @param onBlockDuplicate called when the duplicate action is triggered
 * @param onBlockMoveForward called when the move-forward (increase z-index) action is triggered
 * @param onBlockMoveBackward called when the move-backward (decrease z-index) action is triggered
 * @param onBlockCopy called when the copy action is triggered
 * @param onBlockLinkToEntity called when the link-to-entity action is triggered
 * @param onBlockOpenLinkedEntity called when the open-linked-entity action is triggered
 * @param showMinimap whether the minimap widget is visible
 * @param onToggleMinimap called when the minimap visibility should be toggled
 * @param viewportSize the current viewport size in screen pixels (needed by minimap)
 */
@Composable
fun InfiniteCanvas(
    canvasState: CanvasState,
    blocks: List<CanvasBlockEntity>,
    modifier: Modifier = Modifier,
    blockContent: @Composable (CanvasBlockEntity, Boolean) -> Unit = { _, _ -> },
    onBlockMoved: ((Long, Float, Float) -> Unit)? = null,
    onBlockResized: ((Long, Float, Float) -> Unit)? = null,
    onBlockTapped: ((Long) -> Unit)? = null,
    onBlockDelete: ((Long) -> Unit)? = null,
    onBlockDuplicate: ((Long) -> Unit)? = null,
    onBlockMoveForward: ((Long) -> Unit)? = null,
    onBlockMoveBackward: ((Long) -> Unit)? = null,
    onBlockCopy: ((Long) -> Unit)? = null,
    onBlockLinkToEntity: ((Long) -> Unit)? = null,
    onBlockOpenLinkedEntity: ((Long) -> Unit)? = null,
    showMinimap: Boolean = true,
    onToggleMinimap: (() -> Unit)? = null,
    viewportSize: Size = Size(0f, 0f)
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
                        x = screenPos.x.toInt(),
                        y = screenPos.y.toInt(),
                        zIndex = block.zIndex.toFloat()
                    )
                }
            }
        }

        // Layer 3: BlockToolbar (floating action bar above selected block)
        val selectedBlock = remember(blocks, canvasState.selectedBlockIds) {
            blocks.firstOrNull { it.id in canvasState.selectedBlockIds }
        }
        BlockToolbar(
            selectedBlock = selectedBlock,
            canvasState = canvasState,
            onDelete = {
                selectedBlock?.let { onBlockDelete?.invoke(it.id) }
            },
            onDuplicate = {
                selectedBlock?.let { onBlockDuplicate?.invoke(it.id) }
            },
            onMoveForward = {
                selectedBlock?.let { onBlockMoveForward?.invoke(it.id) }
            },
            onMoveBackward = {
                selectedBlock?.let { onBlockMoveBackward?.invoke(it.id) }
            },
            onCopy = {
                selectedBlock?.let { onBlockCopy?.invoke(it.id) }
            },
            onLink = {
                selectedBlock?.let { onBlockLinkToEntity?.invoke(it.id) }
            },
            onOpenLinked = {
                selectedBlock?.let { onBlockOpenLinkedEntity?.invoke(it.id) }
            }
        )

        // Layer 4: Pan/Zoom gesture layer (sits above everything)
        // Only activates for touches that do NOT start on a block
        PanZoomLayer(
            canvasState = canvasState,
            blocks = blocks,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 5: CanvasMinimap (floating widget, bottom-right)
        CanvasMinimap(
            blocks = blocks,
            canvasState = canvasState,
            viewportWidth = viewportSize.width,
            viewportHeight = viewportSize.height,
            show = showMinimap,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
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
    // Use a custom pointerInput that detects when touch starts on empty canvas space
    Modifier.pointerInput(blocks) {
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
    Box(modifier = modifier.then(this))
}
