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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity
import fieldmind.research.app.features.field.data.canvas.DrawingEntity

/**
 * The main infinite canvas composable.
 *
 * Architecture (bottom → top):
 * 1. [CanvasBackground] — Compose Canvas rendering dot-grid + selection highlights
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
 * @param drawingState shared drawing tool state (null = no drawing enabled)
 * @param drawings saved drawing entities for rendering
 * @param onStrokeComplete called when user finishes a stroke
 * @param onEraseDrawing called when user taps a stroke with eraser
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
    viewportSize: Size = Size(0f, 0f),
    drawingState: DrawingState? = null,
    drawings: List<DrawingEntity> = emptyList(),
    onStrokeComplete: (InProgressStroke) -> Unit = {},
    onEraseDrawing: (Long) -> Unit = {}
) {
    // Convert selected blocks to BlockRect for background highlight rendering
    val selectedBlockRects = remember(blocks, canvasState.selectedBlockIds) {
        blocks
            .filter { it.id in canvasState.selectedBlockIds }
            .map { block ->
                BlockRect(
                    x = block.positionX,
                    y = block.positionY,
                    w = block.width,
                    h = block.height,
                    r = 8f
                )
            }
    }

    // Zoom slider visibility — shown only in Infinite mode
    val showZoomSlider = canvasState.canvasMode == CanvasMode.INFINITE

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
        // Layer 1: Canvas background (dot grid + selection highlights)
        CanvasBackground(
            canvasState = canvasState,
            selectedBlockRects = selectedBlockRects,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Drawing overlay (pen, highlighter, shapes, eraser)
        if (drawingState != null) {
            DrawingOverlay(
                canvasState = canvasState,
                drawingState = drawingState,
                drawings = drawings,
                onStrokeComplete = onStrokeComplete,
                onEraseStroke = onEraseDrawing,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Layer 3: Compose block overlay (with viewport culling for performance)
        SubcomposeLayout(
            modifier = Modifier.fillMaxSize()
        ) { constraints ->
            // Only render blocks that are visible in the current viewport
            val visibleBlocks = blocks.filter { block ->
                val screenPos = canvasState.canvasToScreen(block.positionX, block.positionY)
                val screenSize = Size(block.width * canvasState.zoom, block.height * canvasState.zoom)
                
                // Check if block is within viewport (with 100px padding for smooth scrolling)
                val padding = 100f
                screenPos.x + screenSize.width > -padding &&
                screenPos.x < constraints.maxWidth + padding &&
                screenPos.y + screenSize.height > -padding &&
                screenPos.y < constraints.maxHeight + padding
            }
            
            val placeables = visibleBlocks.map { block ->
                subcompose("block_${block.id}") {
                    val isSelected = block.id in canvasState.selectedBlockIds
                    val isCollapsed = block.id in canvasState.collapsedBlockIds
                    CanvasBlock(
                        block = block,
                        canvasState = canvasState,
                        isSelected = isSelected,
                        isCollapsed = isCollapsed,
                        onToggleCollapse = { canvasState.toggleBlockCollapse(block.id) },
                        onMoved = { x, y -> onBlockMoved?.invoke(block.id, x, y) },
                        onResized = { w, h -> onBlockResized?.invoke(block.id, w, h) },
                        onTapped = { id ->
                            canvasState.selectBlock(id)
                            onBlockTapped?.invoke(id)
                        }
                    ) {
                        blockContent(block, isSelected)
                    }
                }.first().measure(constraints)
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEachIndexed { index, placeable ->
                    val block = visibleBlocks[index]
                    val screenPos = canvasState.canvasToScreen(block.positionX, block.positionY)

                    placeable.place(
                        position = IntOffset(screenPos.x.toInt(), screenPos.y.toInt()),
                        zIndex = block.zIndex.toFloat()
                    )
                }
            }
        }

        // Layer 4: BlockToolbar (floating action bar above selected block)
        val selectedBlock = remember(blocks, canvasState.selectedBlockIds) {
            blocks.firstOrNull { it.id in canvasState.selectedBlockIds }
        }
        BlockToolbar(
            selectedBlock = selectedBlock,
            canvasState = canvasState,
            onToggleCollapse = { blockId ->
                canvasState.toggleBlockCollapse(blockId)
            },
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

        // Layer 5: Pan/Zoom gesture layer (sits above everything)
        // Only activates for touches that do NOT start on a block
        PanZoomLayer(
            canvasState = canvasState,
            blocks = blocks,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 6: ZoomSlider (floating widget, right-center)
        ZoomSlider(
            canvasState = canvasState,
            viewportSize = viewportSize,
            show = showZoomSlider,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )

        // Layer 7: CanvasMinimap (floating widget, bottom-right)
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
    val touchModifier = Modifier.pointerInput(blocks) {
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
    Box(modifier = modifier.then(touchModifier))
}
