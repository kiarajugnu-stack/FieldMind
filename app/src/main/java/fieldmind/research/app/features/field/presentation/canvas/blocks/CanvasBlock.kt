package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlin.math.roundToInt

// Fade block tools smoothly as zoom approaches the hide threshold
private const val TOOL_FADE_START_ZOOM = 0.5f   // tools fully visible above this
private const val TOOL_FADE_END_ZOOM = 0.3f     // tools fully hidden at this zoom

/**
 * A single block on the infinite canvas, rendered in the Compose overlay layer.
 *
 * Features:
 * - Positioned and scaled according to the shared [canvasState] camera transform
 * - **Drag** to move the block (long-press then drag)
 * - **Resize handle** at the bottom-right corner
 * - **Selection** with border highlight
 * - Lift/shadow animation on selection
 *
 * The block's actual content (text, image, drawing, etc.) is provided
 * via the [content] slot.
 *
 * @param block the data entity driving position, size, and type
 * @param canvasState shared camera state for coordinate transforms
 * @param isSelected whether this block is currently selected
 * @param onMoved callback with new canvas-space position (x, y)
 * @param onResized callback with new size (width, height)
 * @param onTapped callback when the block is tapped
 * @param content composable slot for the block's inner content
 */
@Composable
fun CanvasBlock(
    block: CanvasBlockEntity,
    canvasState: CanvasState,
    isSelected: Boolean,
    isCollapsed: Boolean = false,
    onToggleCollapse: (Long) -> Unit = { _ -> },
    onMoved: (Float, Float) -> Unit = { _, _ -> },
    onResized: (Float, Float) -> Unit = { _, _ -> },
    onTapped: (Long) -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    // Animate selection elevation
    val elevation by animateFloatAsState(
        targetValue = if (isSelected) 8f else 2f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "blockElevation"
    )

    // If collapsed, show minimal preview instead of full content
    // Use live block size during active resize (in-memory override, not Room)
    val liveSize = canvasState.liveBlockSizes[block.id]
    val displayWidth = if (isCollapsed) 120f else (liveSize?.width ?: block.width)
    val displayHeight = if (isCollapsed) 100f else (liveSize?.height ?: block.height)

    // Track content's measured size for auto-expand
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    // Determine display height: auto-expand if natural content is taller than entity height.
    // contentSize is in px, displayHeight is in logical px.
    val contentHeightLogical = if (contentSize.height > 0) contentSize.height / density.density else 0f
    val autoHeight = if (!isCollapsed && contentHeightLogical > displayHeight + 15f) {
        contentHeightLogical + 10f // extra padding so text isn't flush with border
    } else {
        displayHeight
    }

    // Sync auto-expanded height to entity (via live size override + onResized)
    LaunchedEffect(autoHeight) {
        if (!isCollapsed && autoHeight > displayHeight && autoHeight - displayHeight > 15f) {
            canvasState.setLiveBlockSize(block.id, displayWidth, autoHeight)
            onResized(displayWidth, autoHeight)
        }
    }

    // Clean up live size when entity catches up
    LaunchedEffect(block.id, block.width, block.height) {
        val live = canvasState.liveBlockSizes[block.id]
        if (live != null && live.width == block.width && live.height == block.height) {
            canvasState.removeLiveBlockSize(block.id)
        }
    }

    // Animated tool alpha: fades from 1.0 at zoom >= FADE_START to 0.0 at zoom <= FADE_END
    val rawToolAlpha = ((canvasState.zoom - TOOL_FADE_END_ZOOM) /
        (TOOL_FADE_START_ZOOM - TOOL_FADE_END_ZOOM)).coerceIn(0f, 1f)
    val toolAlpha by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else rawToolAlpha,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "toolAlpha"
    )

    // Block size at current zoom
    val scaledWidth = displayWidth * canvasState.zoom
    val scaledMinHeight = displayHeight * canvasState.zoom

    Box(
        modifier = Modifier
            // Block dimensions — width is fixed, height has a minimum but grows with content
            .width(with(density) { scaledWidth.toDp() })
            .heightIn(min = with(density) { scaledMinHeight.toDp() })
            .wrapContentHeight()
            // Selection border
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = (2f / canvasState.zoom).coerceAtLeast(1f).dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            // Shadow / elevation
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(8.dp),
                clip = false
            )
            // Background
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            // Long-press + drag to move (taps pass through to child composables)
            .pointerInput(block.id, canvasState.zoom) {
                // Track cumulative drag offset locally so movement is smooth
                // even when Room observation lags behind drag frames
                var cumulativeDx = 0f
                var cumulativeDy = 0f
                var dragStartX = block.positionX
                var dragStartY = block.positionY

                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        // Capture starting position from live override (if active) or entity.
                        // Using live position prevents snap when a new drag starts before
                        // Room finishes writing the previous drag's final position.
                        val livePos = canvasState.liveBlockPositions[block.id]
                        dragStartX = livePos?.x ?: block.positionX
                        dragStartY = livePos?.y ?: block.positionY
                        cumulativeDx = 0f
                        cumulativeDy = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Convert drag from screen-space to canvas-space
                        val canvasDx = dragAmount.x / canvasState.zoom
                        val canvasDy = dragAmount.y / canvasState.zoom
                        cumulativeDx += canvasDx
                        cumulativeDy += canvasDy
                        // Update in-memory live position — no Room write
                        canvasState.setLiveBlockPosition(
                            block.id,
                            dragStartX + cumulativeDx,
                            dragStartY + cumulativeDy
                        )
                    },
                    onDragEnd = {
                        // Flush final position to Room once.
                        // Keep live position until Room emits the updated entity
                        // to prevent visual snap (removeLiveBlockPosition is handled
                        // by LaunchedEffect cleanup below).
                        val finalX = dragStartX + cumulativeDx
                        val finalY = dragStartY + cumulativeDy
                        canvasState.setLiveBlockPosition(block.id, finalX, finalY)
                        onMoved(finalX, finalY)
                    },
                    onDragCancel = {
                        // Gesture cancelled — clear live override, don't write to Room
                        canvasState.removeLiveBlockPosition(block.id)
                    }
                )
            }
    ) {
        // ── Clean up stale live overrides when entity catches up ──
        // Prevents visual snap: live position/size stays until Room emits
        // the updated entity, then this removes the override silently.
        LaunchedEffect(block.id, block.positionX, block.positionY, block.width, block.height) {
            val livePos = canvasState.liveBlockPositions[block.id]
            if (livePos != null && livePos.x == block.positionX && livePos.y == block.positionY) {
                canvasState.removeLiveBlockPosition(block.id)
            }
            val liveSz = canvasState.liveBlockSizes[block.id]
            if (liveSz != null && liveSz.width == block.width && liveSz.height == block.height) {
                canvasState.removeLiveBlockSize(block.id)
            }
        }

        if (isCollapsed) {
            // Collapsed state: show preview with expand button.
            // Use fillMaxWidth + inner wrapContentHeight so the collapsed box
            // naturally sizes to icon+label height via the outer wrapContentHeight.
            // Do NOT use fillMaxSize — inside a wrapContentHeight parent it would
            // fill the full viewport height constraint, making the "minimized" block
            // appear full-height (the bug we're fixing).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        MaterialSymbolIcon("unfold_more"),
                        "Expand",
                        size = 20.dp,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        block.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            // Full content display — measure natural content height for auto-expand
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .onGloballyPositioned { coords ->
                        contentSize = coords.size
                    }
            ) {
                content()
            }
        }

        // ── Tool overlay (fades out smoothly as zoom decreases) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = toolAlpha)
        ) {
            // Minimize button (top-left corner)
            if (isSelected && !isCollapsed) {
                val buttonSize = (20f * canvasState.zoom).coerceIn(10f, 24f).dp
                val iconSize = (10f * canvasState.zoom).coerceIn(6f, 14f).dp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = (4f * canvasState.zoom).coerceAtLeast(2f).dp,
                            top = (4f * canvasState.zoom).coerceAtLeast(2f).dp
                        ),
                    contentAlignment = Alignment.TopStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .pointerInput(Unit) {
                                detectTapGestures { onToggleCollapse(block.id) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            MaterialSymbolIcon("unfold_less"),
                            "Minimize",
                            size = iconSize,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Link badge overlay (top-right corner)
            if (block.linkedEntityType.isNotBlank() && block.linkedEntityId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            end = (4f / canvasState.zoom).dp,
                            top = (4f / canvasState.zoom).dp
                        ),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size((16f * canvasState.zoom).coerceIn(10f, 20f).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            MaterialSymbolIcon("link"),
                            "Linked to ${block.linkedEntityType}",
                            size = (10f * canvasState.zoom).coerceIn(6f, 14f).dp,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Resize handle (bottom-right corner)
            if (isSelected && !isCollapsed) {
                ResizeHandle(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onResize = { cumulativeDx, cumulativeDy ->
                        // Update in-memory live size — no Room write
                        val newW = (displayWidth + cumulativeDx).coerceAtLeast(60f)
                        val newH = (displayHeight + cumulativeDy).coerceAtLeast(60f)
                        canvasState.setLiveBlockSize(block.id, newW, newH)
                    },
                    onResizeEnd = { cumulativeDx, cumulativeDy ->
                        // Flush final size to Room once.
                        // Keep live size until Room emits the updated entity
                        // to prevent visual snap (removeLiveBlockSize is handled
                        // by LaunchedEffect cleanup below).
                        val finalW = (displayWidth + cumulativeDx).coerceAtLeast(60f)
                        val finalH = (displayHeight + cumulativeDy).coerceAtLeast(60f)
                        canvasState.setLiveBlockSize(block.id, finalW, finalH)
                        onResized(finalW, finalH)
                    },
                    canvasState = canvasState
                )
            }
        }
    }
}

/**
 * Small triangular handle at the bottom-right corner for resize.
 * Tracks cumulative drag delta so the caller gets cumulative (not per-frame) values.
 * Calls [onResize] on every frame with cumulative delta, [onResizeEnd] when drag ends.
 */
@Composable
private fun ResizeHandle(
    modifier: Modifier = Modifier,
    onResize: (Float, Float) -> Unit,
    onResizeEnd: (Float, Float) -> Unit = { _, _ -> },
    canvasState: CanvasState
) {
    // Scale handle size with zoom so it stays proportionally visible
    val handleSize = (16f * canvasState.zoom).coerceIn(8f, 24f).dp
    Box(
        modifier = modifier
            .size(handleSize)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                RoundedCornerShape(topStart = 4.dp)
            )
            .pointerInput(Unit) {
                var cumulativeDx = 0f
                var cumulativeDy = 0f

                detectDragGestures(
                    onDragStart = {
                        cumulativeDx = 0f
                        cumulativeDy = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Convert drag delta from screen-space to canvas-space
                        val canvasDx = dragAmount.x / canvasState.zoom
                        val canvasDy = dragAmount.y / canvasState.zoom
                        cumulativeDx += canvasDx
                        cumulativeDy += canvasDy
                        onResize(cumulativeDx, cumulativeDy)
                    },
                    onDragEnd = {
                        onResizeEnd(cumulativeDx, cumulativeDy)
                    },
                    onDragCancel = {
                        // Reset cumulative on cancel so final cleared position
                        // matches the original (no-op)
                        cumulativeDx = 0f
                        cumulativeDy = 0f
                        onResizeEnd(cumulativeDx, cumulativeDy)
                    }
                )
            }
    )
}
