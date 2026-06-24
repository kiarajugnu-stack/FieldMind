package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.features.field.presentation.components.expressivePress
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlin.math.roundToInt

/**
 * A floating toolbar that appears near the selected block on the canvas.
 *
 * Positioned above the selected block (or below if the block is near the
 * top of the screen) and centered horizontally on the block.
 *
 * Actions:
 * - **Delete** — removes the block
 * - **Duplicate** — creates a copy offset slightly
 * - **Move Forward** — increases z-index by 1
 * - **Move Backward** — decreases z-index by 1
 * - **Copy** — copies block content to clipboard
 * - **Link to Entity** — opens entity picker to link this block to an observation, question, etc.
 *
 * The toolbar auto-hides when no block is selected.
 *
 * @param selectedBlock the currently selected block, or null if none
 * @param canvasState the shared canvas camera state (for coordinate transforms)
 * @param onDelete called to delete the block
 * @param onDuplicate called to duplicate the block
 * @param onMoveForward called to increase z-index
 * @param onMoveBackward called to decrease z-index
 * @param onCopy called to copy block content
 * @param onLink called to link to an entity (or re-link)
 * @param onOpenLinked called to open the linked entity in the detail screen
 * @param modifier standard Compose modifier
 */
@Composable
fun BlockToolbar(
    selectedBlock: CanvasBlockEntity?,
    canvasState: CanvasState,
    onToggleCollapse: (Long) -> Unit = { _ -> },
    onDelete: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onMoveForward: () -> Unit = {},
    onMoveBackward: () -> Unit = {},
    onCopy: () -> Unit = {},
    onLink: () -> Unit = {},
    onOpenLinked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // ── Compute toolbar screen position based on the selected block ──
    val toolbarOffset = remember(selectedBlock, canvasState.zoom, canvasState.panX, canvasState.panY) {
        if (selectedBlock == null) return@remember IntOffset.Zero

        val screenPos = canvasState.canvasToScreen(
            selectedBlock.positionX,
            selectedBlock.positionY
        )
        val blockScreenWidth = selectedBlock.width * canvasState.zoom
        val blockScreenHeight = selectedBlock.height * canvasState.zoom

        // Center the toolbar horizontally above the block
        val toolBarWidth = 280f  // approximate toolbar width in px
        val x = screenPos.x + (blockScreenWidth / 2f) - (toolBarWidth / 2f)

        // Position above the block, or below if the block is near the top
        val y = if (screenPos.y > 120f) {
            screenPos.y - 48f  // above block
        } else {
            screenPos.y + blockScreenHeight + 8f  // below block
        }

        IntOffset(x.roundToInt(), y.roundToInt())
    }

    AnimatedVisibility(
        visible = selectedBlock != null,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialScale = 0.85f
        ) + fadeIn(
            animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
        ) + slideInVertically(
            animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
        ) { -it / 3 },
        exit = scaleOut(
            animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
            targetScale = 0.85f
        ) + fadeOut(
            animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
        ) + slideOutVertically(
            animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
        ) { -it / 3 },
        modifier = modifier
            .offset { toolbarOffset }
    ) {
        BlockToolbarContent(
            onDelete = onDelete,
            onDuplicate = onDuplicate,
            onCollapse = {
                selectedBlock?.let { onToggleCollapse(it.id) }
            },
            isCollapsed = selectedBlock?.let { it.id in canvasState.collapsedBlockIds } ?: false,
            onMoveForward = onMoveForward,
            onMoveBackward = onMoveBackward,
            onCopy = onCopy,
            onLink = onLink,
            onOpenLinked = onOpenLinked,
            hasLinkedEntity = selectedBlock?.linkedEntityType?.isNotBlank() == true && selectedBlock.linkedEntityId != null
        )
    }
}

/**
 * The actual toolbar chip row, wrapped in a [Surface] with shadow.
 */
@Composable
private fun BlockToolbarContent(
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onCollapse: () -> Unit = {},
    isCollapsed: Boolean = false,
    onMoveForward: () -> Unit,
    onMoveBackward: () -> Unit,
    onCopy: () -> Unit,
    onLink: () -> Unit,
    onOpenLinked: () -> Unit = {},
    hasLinkedEntity: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .widthIn(min = 200.dp)
            .wrapContentWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarAction(
                icon = MaterialSymbolIcon("delete"),
                label = "Delete",
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error
            )
            ToolbarDivider()
            ToolbarAction(
                icon = if (isCollapsed) MaterialSymbolIcon("unfold_more") else MaterialSymbolIcon("unfold_less"),
                label = if (isCollapsed) "Expand" else "Minimize",
                onClick = onCollapse
            )
            ToolbarDivider()
            ToolbarAction(
                icon = MaterialSymbolIcon("file_copy"),
                label = "Duplicate",
                onClick = onDuplicate
            )
            ToolbarDivider()
            ToolbarAction(
                icon = MaterialSymbolIcon("content_copy"),
                label = "Copy",
                onClick = onCopy
            )
            ToolbarDivider()
            ToolbarAction(
                icon = MaterialSymbolIcon("arrow_upward"),
                label = "Fwd",
                onClick = onMoveForward
            )
            ToolbarAction(
                icon = MaterialSymbolIcon("arrow_downward"),
                label = "Back",
                onClick = onMoveBackward
            )
            ToolbarDivider()

            // Show "Open linked" instead of "Link" when block already has a link
            if (hasLinkedEntity) {
                ToolbarAction(
                    icon = MaterialSymbolIcon("open_in_new"),
                    label = "Open",
                    onClick = onOpenLinked,
                    tint = MaterialTheme.colorScheme.primary
                )
                ToolbarAction(
                    icon = MaterialSymbolIcon("link"),
                    label = "Re-link",
                    onClick = onLink
                )
            } else {
                ToolbarAction(
                    icon = MaterialSymbolIcon("link"),
                    label = "Link",
                    onClick = onLink
                )
            }
        }
    }
}

/**
 * A single toolbar action chip: icon + label.
 */
@Composable
private fun ToolbarAction(
    icon: MaterialSymbolIcon,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier
            .height(32.dp)
            .expressivePress(scaleDown = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                label,
                size = 16.dp,
                tint = tint
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = tint
            )
        }
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    )
}
