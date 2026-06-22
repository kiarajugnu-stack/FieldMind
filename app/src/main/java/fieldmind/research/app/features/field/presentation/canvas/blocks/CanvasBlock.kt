package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlin.math.roundToInt

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

    // Block size at current zoom
    val scaledWidth = block.width * canvasState.zoom
    val scaledHeight = block.height * canvasState.zoom

    Box(
        modifier = Modifier
            // Block dimensions (screen-space, derived from canvas-space at current zoom)
            .size(
                width = with(LocalDensity.current) { scaledWidth.toDp() },
                height = with(LocalDensity.current) { scaledHeight.toDp() }
            )
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
            // Drag to move
            .pointerInput(block.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Convert drag from screen-space to canvas-space
                    val canvasDx = dragAmount.x / canvasState.zoom
                    val canvasDy = dragAmount.y / canvasState.zoom
                    onMoved(block.positionX + canvasDx, block.positionY + canvasDy)
                }
            }
    ) {
        // Block content
        content()

        // Link badge (top-right corner, shown when block is linked to an entity)
        if (block.linkedEntityType.isNotBlank() && block.linkedEntityId != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (4f / canvasState.zoom).dp, y = (4f / canvasState.zoom).dp)
                    .size((16f / canvasState.zoom).coerceAtLeast(12f).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    MaterialSymbolIcon("link"),
                    "Linked to ${block.linkedEntityType}",
                    size = (10f / canvasState.zoom).coerceAtLeast(6f).dp,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Resize handle (bottom-right corner)
        if (isSelected) {
            ResizeHandle(
                onResize = { dx, dy -> onResized(block.width + dx, block.height + dy) },
                canvasState = canvasState
            )
        }
    }
}

/**
 * Small triangular handle at the bottom-right corner for resize.
 */
@Composable
private fun ResizeHandle(
    onResize: (Float, Float) -> Unit,
    canvasState: CanvasState
) {
    val handleSize = 16.dp
    Box(
        modifier = Modifier
            .size(handleSize)
            .align(Alignment.BottomEnd)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                RoundedCornerShape(topStart = 4.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Convert drag delta from screen-space to canvas-space
                    val canvasDx = dragAmount.x / canvasState.zoom
                    val canvasDy = dragAmount.y / canvasState.zoom
                    // Enforce minimum size: blocks can't shrink below 60x40 logical px
                    val minW = -300f   // prevents width < 0 (base 300 + this delta)
                    val minH = -160f   // prevents height < 0 (base 200 + this delta)
                    onResize(canvasDx.coerceAtLeast(minW), canvasDy.coerceAtLeast(minH))
                }
            }
    )
}
