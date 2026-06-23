package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity
import fieldmind.research.app.features.field.data.canvas.DrawingEntity
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlin.math.roundToInt

// A4 aspect ratio: width/height ≈ 1/1.414 = 0.707
private const val A4_ASPECT = 1.414f

/**
 * Page-based canvas mode that renders blocks on A4-sized pages
 * stacked vertically with page breaks, like a document / PDF viewer.
 *
 * Features:
 * - White A4 pages on gray background
 * - Vertical scrolling through pages
 * - Page breaks with shadow between pages
 * - Blocks positioned within page boundaries
 * - Full editing: drag, resize, tap selection, toolbar
 * - Page count indicator in the top-left of each page
 *
 * @param canvasState shared canvas state (zoom is forced to 1, pan tracks scroll)
 * @param blocks list of canvas blocks to render
 * @param currentPage current visible page index (0-based), emitted on scroll
 * @param totalPages total number of pages (emitted once on init and when pages change)
 * @param viewportSize the current viewport size in pixels
 * @param blockContent composable factory for each block's content
 * @param onBlockMoved called when a block is dragged
 * @param onBlockResized called when a block is resized
 * @param onBlockTapped called when a block is tapped
 * @param onBlockDelete delete callback
 * @param onBlockDuplicate duplicate callback
 * @param onBlockMoveForward z-index forward callback
 * @param onBlockMoveBackward z-index backward callback
 * @param onBlockCopy copy callback
 * @param onBlockLinkToEntity link callback
 * @param onBlockOpenLinkedEntity open linked entity callback
 * @param currentPage current visible page index (0-based), emitted on scroll
 * @param totalPages total number of pages (emitted when pages change)
 * @param viewportSize viewport size in pixels
 * @param drawingState shared drawing tool state (null = no drawing enabled)
 * @param drawings saved drawing entities for rendering
 * @param onStrokeComplete called when user finishes a stroke
 * @param onEraseDrawing called when user taps a stroke with eraser
 */
@Composable
fun PageCanvas(
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
    currentPage: (Int) -> Unit = {},  /* emits 0-based page index when scroll changes */
    totalPages: (Int) -> Unit = {},   /* emits total page count when pages change */
    viewportSize: Size = Size(0f, 0f),
    drawingState: DrawingState? = null,
    drawings: List<DrawingEntity> = emptyList(),
    onStrokeComplete: (InProgressStroke) -> Unit = {},
    onEraseDrawing: (Long) -> Unit = {}
) {
    // ── Compute page dimensions ──
    val density = LocalDensity.current
    val pageWidthPx = with(density) {
        (viewportSize.width - 48.dp.toPx()).coerceAtLeast(300f)
    }
    val pageHeightPx = pageWidthPx * A4_ASPECT

    // ── Force zoom to 1x in pages mode ──
    LaunchedEffect(Unit) {
        canvasState.zoomTo(1f)
    }

    // ── Scroll state — update canvas panY to track scroll offset ──
    val scrollState = rememberScrollState()

    // Sync canvasState panY with scroll so CanvasBlock transforms work
    LaunchedEffect(scrollState.value) {
        canvasState.setPan(
            (viewportSize.width - pageWidthPx) / 2f, // center page horizontally
            -scrollState.value.toFloat()              // track vertical scroll
        )
    }

    // ── Compute pages from block positions ──
    data class PageInfo(
        val index: Int,
        val startY: Float,
        val blocks: List<CanvasBlockEntity>
    )

    val pages = remember(blocks, pageHeightPx) {
        if (blocks.isEmpty()) {
            listOf(PageInfo(0, 0f, emptyList()))
        } else {
            val maxY = blocks.maxOf { it.positionY + it.height }
            val count = ((maxY / pageHeightPx).toInt() + 1).coerceAtLeast(1)
            (0 until count).map { pageIdx ->
                val pageStart = pageIdx * pageHeightPx
                val pageEnd = (pageIdx + 1) * pageHeightPx
                val pageBlocks = blocks.filter { b ->
                    b.positionY < pageEnd && b.positionY + b.height > pageStart
                }.sortedBy { it.zIndex }
                PageInfo(pageIdx, pageStart, pageBlocks)
            }
        }
    }

    // ── Compute and emit current page from scroll position ──
    val gapPx = with(density) { 28.dp.toPx() }
    val totalPageStep = pageHeightPx + gapPx
    val currentPageIndex = remember(scrollState.value, pages.size) {
        val idx = (scrollState.value.toFloat() / totalPageStep).toInt()
        idx.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    }
    LaunchedEffect(currentPageIndex) {
        currentPage(currentPageIndex)
    }

    // ── Emit total pages ──
    LaunchedEffect(pages.size) {
        totalPages(pages.size)
    }

    // ── Compute document positions for toolbar placement ──
    val paddingTopPx = with(density) { 24.dp.toPx() }
    val paddingHoriPx = with(density) { 24.dp.toPx() }

    // Tile the page background + page columns
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE8E8E8)) // light gray like PDF reader
    ) {
        // Layer 1: Drawing overlay (rendered behind scrollable pages)
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

        // Layer 2: Scrollable pages
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            pages.forEach { page ->
                // ── Page surface ──
                Surface(
                    modifier = Modifier
                        .width(with(density) { pageWidthPx.toDp() })
                        .height(with(density) { pageHeightPx.toDp() })
                        .shadow(6.dp, RoundedCornerShape(2.dp), clip = false),
                    shape = RoundedCornerShape(2.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(Modifier.fillMaxSize()) {
                        // ── Page number (top-right corner) ──
                        Text(
                            "${page.index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0x44000000),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        )

                        // ── Render blocks on this page ──
                        page.blocks.forEach { block ->
                            val pageRelativeY = block.positionY - page.startY
                            val isSelected = block.id in canvasState.selectedBlockIds

                            PageBlock(
                                block = block,
                                pageX = block.positionX,
                                pageY = pageRelativeY,
                                isSelected = isSelected,
                                onTapped = { id ->
                                    canvasState.selectBlock(id)
                                    onBlockTapped?.invoke(id)
                                },
                                onMoved = { newX, newY ->
                                    // Convert page-relative coords back to document coords
                                    val docY = newY + page.startY
                                    onBlockMoved?.invoke(block.id, newX, docY)
                                },
                                onResized = { w, h ->
                                    onBlockResized?.invoke(block.id, w, h)
                                }
                            ) {
                                blockContent(block, isSelected)
                            }
                        }

                        // ── Empty page hint ──
                        if (page.blocks.isEmpty() && pages.size == 1) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        MaterialSymbolIcon("description", defaultWeight = 300),
                                        "Empty page",
                                        size = 48.dp,
                                        tint = Color(0x22000000)
                                    )
                                    Text(
                                        "Tap + to add blocks",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0x44000000)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Page break (gap between pages) ──
                if (page.index < pages.lastIndex) {
                    Spacer(Modifier.height(28.dp))
                }
            }

            // ── Bottom spacer for overscroll ──
            Spacer(Modifier.height(40.dp))
        }

        // Layer 2: BlockToolbar (overlay, positioned absolutely in the outer Box)
        val selectedBlock = remember(blocks, canvasState.selectedBlockIds) {
            blocks.firstOrNull { it.id in canvasState.selectedBlockIds }
        }
        if (selectedBlock != null) {
            // Find which page the selected block is on
            val selPageIdx = remember(selectedBlock, pages) {
                pages.indexOfFirst { page ->
                    selectedBlock.positionY < page.startY + pageHeightPx &&
                    selectedBlock.positionY + selectedBlock.height > page.startY
                }.coerceAtLeast(0)
            }

            // Compute toolbar position: the block's position within the document,
            // minus the scroll offset, gives its screen position within the outer Box.
            val docBlockY = paddingTopPx + selPageIdx * totalPageStep +
                (selectedBlock.positionY - pages.getOrNull(selPageIdx)?.startY ?: 0f)
            val toolBarY = docBlockY - scrollState.value

            Box(
                modifier = Modifier
                    .offset {
                        // Page left edge in outer Box = paddingHoriPx from Column padding
                        val toolBarX = paddingHoriPx + selectedBlock.positionX
                        // Clamp Y so toolbar stays within viewport (at minimum 0)
                        val toolBarYClamped = (toolBarY - 48.dp.toPx()).coerceAtLeast(0f)
                        IntOffset(toolBarX.roundToInt(), toolBarYClamped.roundToInt())
                    }
                    .zIndex(999f)
            ) {
                BlockToolbar(
                    selectedBlock = selectedBlock,
                    canvasState = canvasState,
                    onToggleCollapse = { canvasState.toggleBlockCollapse(it) },
                    onDelete = { onBlockDelete?.invoke(selectedBlock.id) },
                    onDuplicate = { onBlockDuplicate?.invoke(selectedBlock.id) },
                    onMoveForward = { onBlockMoveForward?.invoke(selectedBlock.id) },
                    onMoveBackward = { onBlockMoveBackward?.invoke(selectedBlock.id) },
                    onCopy = { onBlockCopy?.invoke(selectedBlock.id) },
                    onLink = { onBlockLinkToEntity?.invoke(selectedBlock.id) },
                    onOpenLinked = { onBlockOpenLinkedEntity?.invoke(selectedBlock.id) }
                )
            }
        }
    }
}

/**
 * A single block rendered within a page in PAGES mode.
 *
 * Simpler than [CanvasBlock] — no zoom/pan transforms needed since
 * pages are rendered at 1:1 scale. Supports drag-to-move, resize,
 * tap selection, and a resize handle at the bottom-right.
 *
 * @param block the block entity
 * @param pageX X position within the page (document X coordinate)
 * @param pageY Y position relative to the page top (0 = top of this page)
 * @param isSelected whether this block is currently selected
 * @param onTapped called when the block is tapped
 * @param onMoved called with new (documentX, documentY) — Y includes page offset
 * @param onResized called with new (width, height)
 * @param content the block's inner content
 */
@Composable
private fun PageBlock(
    block: CanvasBlockEntity,
    pageX: Float,
    pageY: Float,
    isSelected: Boolean,
    onTapped: (Long) -> Unit,
    onMoved: (Float, Float) -> Unit,
    onResized: (Float, Float) -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .offset { IntOffset(pageX.roundToInt(), pageY.roundToInt()) }
            .size(
                width = with(density) { block.width.toDp() },
                height = with(density) { block.height.toDp() }
            )
            // Selection border
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            // Background
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            // Tap to select
            .pointerInput(block.id) {
                detectTapGestures { onTapped(block.id) }
            }
            // Drag to move (no Y clamping — allows dragging blocks between pages)
            .pointerInput(block.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val newX = (pageX + dragAmount.x).coerceAtLeast(0f)
                    val newY = pageY + dragAmount.y
                    onMoved(newX, newY)
                }
            }
    ) {
        // Block content
        Box(Modifier.fillMaxSize()) {
            content()
        }

        // Resize handle (bottom-right, when selected)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        RoundedCornerShape(topStart = 4.dp)
                    )
                    .pointerInput(block.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newW = (block.width + dragAmount.x).coerceAtLeast(60f)
                            val newH = (block.height + dragAmount.y).coerceAtLeast(60f)
                            onResized(newW, newH)
                        }
                    }
            )
        }

        // Link badge (top-right)
        if (block.linkedEntityType.isNotBlank() && block.linkedEntityId != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    MaterialSymbolIcon("link"),
                    "Linked to ${block.linkedEntityType}",
                    size = 10.dp,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
