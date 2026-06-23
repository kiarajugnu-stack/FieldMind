package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity
import fieldmind.research.app.features.field.data.canvas.DrawingEntity
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.abs

// A4 aspect ratio: width/height ≈ 1/1.414 = 0.707
private const val A4_ASPECT = 1.414f
// Fixed A4 page width in dp — does NOT resize with screen viewport
private val PAGE_WIDTH_DP = 400.dp
private val PAGE_HEIGHT_DP = PAGE_WIDTH_DP * A4_ASPECT

/**
 * Page-based canvas mode that renders blocks on A4-sized pages
 * stacked vertically with page breaks, like a document / PDF viewer.
 *
 * Features:
 * - White A4 pages on gray background (fixed A4 size, doesn't resize with screen)
 * - Vertical scrolling through pages
 * - Page breaks with shadow between pages
 * - Blocks positioned within page boundaries
 * - Full editing: drag, resize, tap selection, toolbar
 * - Zoom slider magnifies content within the fixed-size page
 * - Per-page drawing overlay (only active when drawing toolbar is shown)
 * - Tap empty page area to deselect blocks
 * - Pinch-to-zoom gesture on the page surface
 *
 * @param canvasState shared canvas state (zoom scales page size, pan tracks scroll)
 * @param blocks list of canvas blocks to render
 * @param modifier standard Compose modifier
 * @param blockContent composable factory for each block's content
 * @param onBlockMoved called when a block is dragged (intermediate positions, no undo)
 * @param onBlockMovedFinal called when a block drag ends (final position, records undo)
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
    onBlockMovedFinal: ((Long, Float, Float, Float, Float) -> Unit)? = null,
    onBlockResized: ((Long, Float, Float) -> Unit)? = null,
    onBlockTapped: ((Long) -> Unit)? = null,
    onBlockDelete: ((Long) -> Unit)? = null,
    onBlockDuplicate: ((Long) -> Unit)? = null,
    onBlockMoveForward: ((Long) -> Unit)? = null,
    onBlockMoveBackward: ((Long) -> Unit)? = null,
    onBlockCopy: ((Long) -> Unit)? = null,
    onBlockLinkToEntity: ((Long) -> Unit)? = null,
    onBlockOpenLinkedEntity: ((Long) -> Unit)? = null,
    currentPage: (Int) -> Unit = {},
    totalPages: (Int) -> Unit = {},
    viewportSize: Size = Size(0f, 0f),
    drawingState: DrawingState? = null,
    drawings: List<DrawingEntity> = emptyList(),
    onStrokeComplete: (InProgressStroke) -> Unit = {},
    onEraseDrawing: (Long) -> Unit = {}
) {
    // ── Compute page dimensions (FIXED A4 size — does NOT resize with screen) ──
    val density = LocalDensity.current
    val zoom = canvasState.zoom
    // Page has a fixed dp size — zoom only scales content within the page
    val pageWidthPx = with(density) { PAGE_WIDTH_DP.toPx() }
    val pageHeightPx = with(density) { PAGE_HEIGHT_DP.toPx() }
    val gapPx = with(density) { 20.dp.toPx() }

    // ── Compute content-relative dimensions (document coords) ──
    val docWidth = pageWidthPx / zoom
    val docHeight = pageHeightPx / zoom

    // ── Scroll state ──
    val scrollState = rememberScrollState()

    // Sync canvasState panX/panY with scroll + zoom for BlockToolbar transforms
    val computedPanX = (viewportSize.width - pageWidthPx) / 2f
    val computedPanY = -scrollState.value.toFloat()
    canvasState.setPan(computedPanX, computedPanY)

    // ── Compute pages from block positions (using doc space) ──
    data class PageInfo(
        val index: Int,
        val startY: Float,  // document-space Y of page top
        val blocks: List<CanvasBlockEntity>
    )

    val pages = remember(blocks, docHeight) {
        if (blocks.isEmpty()) {
            listOf(PageInfo(0, 0f, emptyList()))
        } else {
            val maxY = blocks.maxOf { it.positionY + it.height }
            val count = ((maxY / docHeight).toInt() + 1).coerceAtLeast(1)
            (0 until count).map { pageIdx ->
                val pageStart = pageIdx * docHeight
                val pageEnd = (pageIdx + 1) * docHeight
                val pageBlocks = blocks.filter { b ->
                    b.positionY < pageEnd && b.positionY + b.height > pageStart
                }.sortedBy { it.zIndex }
                PageInfo(pageIdx, pageStart, pageBlocks)
            }
        }
    }

    // ── Compute and emit current page from scroll position ──
    val totalPageStep = pageHeightPx + gapPx
    val currentPageIndex = remember(scrollState.value, pages.size) {
        val idx = (scrollState.value.toFloat() / totalPageStep).toInt()
        idx.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    }
    LaunchedEffect(currentPageIndex) {
        currentPage(currentPageIndex)
    }

    LaunchedEffect(pages.size) {
        totalPages(pages.size)
    }

    // ── Outer container ──
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE8E8E8))
    ) {
        // ── Layer 1: Scrollable pages with pinch-to-zoom ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            pages.forEach { page ->
                // ── Page surface (fixed A4 size) ──
                Surface(
                    modifier = Modifier
                        .width(PAGE_WIDTH_DP)
                        .height(PAGE_HEIGHT_DP)
                        .shadow(6.dp, RoundedCornerShape(2.dp), clip = false),
                    shape = RoundedCornerShape(2.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(Modifier.fillMaxSize()) {
                        // ── Tap empty area on page → deselect ──
                        // Only active when NOT drawing
                        if (drawingState?.showToolbar != true) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures { tapOffset ->
                                            val docX = tapOffset.x / zoom
                                            val docY = (tapOffset.y / zoom) + page.startY
                                            // Check if tap is on a block — if not, clear selection
                                            val hitBlock = page.blocks.lastOrNull { block ->
                                                val bx = canvasState.liveBlockPositions[block.id]?.x ?: block.positionX
                                                val by = canvasState.liveBlockPositions[block.id]?.y ?: block.positionY
                                                docX in bx..(bx + block.width) &&
                                                docY in by..(by + block.height)
                                            }
                                            if (hitBlock == null) {
                                                canvasState.clearSelection()
                                            }
                                        }
                                    }
                            )
                        }

                        // ── Pinch-to-zoom gesture layer (on the page surface) ──
                        // Single-finger drag handled by verticalScroll.
                        // Two-finger pinch adjusts zoom via canvasState.
                        if (drawingState?.showToolbar != true) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { centroid, pan, zoomDelta, _ ->
                                            // Apply pinch zoom centered on the touch point
                                            if (abs(zoomDelta - 1f) > 0.01f) {
                                                // Convert centroid from page-local to screen coords
                                                // (centroid is relative to this Box which fills the page)
                                                val screenCentroid = Offset(
                                                    computedPanX + centroid.x,
                                                    computedPanY + centroid.y
                                                )
                                                canvasState.applyZoom(zoomDelta, screenCentroid)
                                            }
                                        }
                                    }
                            )
                        }

                        // ── Render blocks on this page ──
                        page.blocks.forEach { block ->
                            val livePos = canvasState.liveBlockPositions[block.id]
                            val posX = livePos?.x ?: block.positionX
                            val posY = livePos?.y ?: block.positionY
                            val pageRelativeY = posY - page.startY
                            val isSelected = block.id in canvasState.selectedBlockIds
                            PageBlock(
                                block = block,
                                pageX = posX,
                                pageY = pageRelativeY,
                                isSelected = isSelected,
                                canvasState = canvasState,
                                onTapped = { id ->
                                    canvasState.selectBlock(id)
                                    onBlockTapped?.invoke(id)
                                },
                                onMoved = { newX, newY ->
                                    val docY = newY + page.startY
                                    onBlockMoved?.invoke(block.id, newX, docY)
                                },
                                onMovedFinal = { startX, startY, finalX, finalY ->
                                    val docStartY = startY + page.startY
                                    val docFinalY = finalY + page.startY
                                    onBlockMovedFinal?.invoke(block.id, startX, docStartY, finalX, docFinalY)
                                },
                                onResized = { w, h ->
                                    onBlockResized?.invoke(block.id, w, h)
                                }
                            ) {
                                blockContent(block, isSelected)
                            }
                        }

                        // ── Per-page drawing overlay (only when drawing toolbar is shown) ──
                        if (drawingState?.showToolbar == true) {
                            PageDrawingOverlay(
                                pageStartY = page.startY,
                                zoom = zoom,
                                drawingState = drawingState,
                                drawings = drawings,
                                onStrokeComplete = onStrokeComplete,
                                onEraseDrawing = onEraseDrawing,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // ── Empty page hint ──
                        if (page.blocks.isEmpty() && pages.size == 1 && drawingState?.showToolbar != true) {
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
                                        size = 40.dp,
                                        tint = Color(0x22000000)
                                    )
                                    Text(
                                        "Tap + to add blocks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0x44000000)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Page break (fixed gap between pages) ──
                if (page.index < pages.lastIndex) {
                    Spacer(Modifier.height(20.dp))
                }
            }

            // ── Bottom spacer for overscroll ──
            Spacer(Modifier.height(40.dp))
        }

        // ── Layer 2: BlockToolbar ──
        val selectedBlock by remember(blocks, canvasState.selectedBlockIds) {
            derivedStateOf {
                val entity = blocks.firstOrNull { it.id in canvasState.selectedBlockIds } ?: return@derivedStateOf null
                val livePos = canvasState.liveBlockPositions[entity.id]
                val liveSize = canvasState.liveBlockSizes[entity.id]
                if (livePos != null || liveSize != null) {
                    entity.copy(
                        positionX = livePos?.x ?: entity.positionX,
                        positionY = livePos?.y ?: entity.positionY,
                        width = liveSize?.width ?: entity.width,
                        height = liveSize?.height ?: entity.height
                    )
                } else {
                    entity
                }
            }
        }
        BlockToolbar(
            selectedBlock = selectedBlock,
            canvasState = canvasState,
            onToggleCollapse = { canvasState.toggleBlockCollapse(it) },
            onDelete = { selectedBlock?.let { onBlockDelete?.invoke(it.id) } },
            onDuplicate = { selectedBlock?.let { onBlockDuplicate?.invoke(it.id) } },
            onMoveForward = { selectedBlock?.let { onBlockMoveForward?.invoke(it.id) } },
            onMoveBackward = { selectedBlock?.let { onBlockMoveBackward?.invoke(it.id) } },
            onCopy = { selectedBlock?.let { onBlockCopy?.invoke(it.id) } },
            onLink = { selectedBlock?.let { onBlockLinkToEntity?.invoke(it.id) } },
            onOpenLinked = { selectedBlock?.let { onBlockOpenLinkedEntity?.invoke(it.id) } }
        )

        // ── Layer 3: ZoomSlider (always visible in page mode) ──
        ZoomSlider(
            canvasState = canvasState,
            viewportSize = viewportSize,
            show = true,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Per-Page Drawing Overlay
// ══════════════════════════════════════════════════════════════════════

/**
 * A per-page drawing Canvas that renders saved drawings (scoped to this page's
 * Y range) and handles drawing/erasing gestures in page-local coordinates.
 *
 * Coordinates:
 * - Gesture offsets are in page-local pixels (relative to the page top-left).
 * - Document Y = pageY / zoom + pageStartY.
 * - Saved drawings are in document coordinates; subtract pageStartY when rendering.
 */
@Composable
private fun PageDrawingOverlay(
    pageStartY: Float,
    zoom: Float,
    drawingState: DrawingState?,
    drawings: List<DrawingEntity>,
    onStrokeComplete: (InProgressStroke) -> Unit = {},
    onEraseDrawing: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (drawingState == null) return

    var currentStroke by remember { mutableStateOf<InProgressStroke?>(null) }

    // ── Gesture handlers ──
    val gestureModifier = if (!drawingState.isEraser) {
        Modifier.pointerInput(drawingState.activeTool, drawingState.strokeWidth, drawingState.color, drawingState.shapeType) {
            detectDragGestures(
                onDragStart = { offset ->
                    val docX = offset.x / zoom
                    val docY = offset.y / zoom + pageStartY
                    currentStroke = InProgressStroke(
                        points = mutableListOf(StrokePoint(docX, docY)),
                        tool = drawingState.activeTool,
                        color = drawingState.color,
                        strokeWidth = drawingState.effectiveStrokeWidth,
                        shapeType = drawingState.shapeType
                    )
                },
                onDrag = { change, _ ->
                    change.consume()
                    val docX = change.position.x / zoom
                    val docY = change.position.y / zoom + pageStartY
                    currentStroke?.let { stroke ->
                        stroke.points.add(StrokePoint(docX, docY, change.pressure))
                    }
                },
                onDragEnd = {
                    currentStroke?.let { stroke ->
                        if (stroke.points.size >= 2) {
                            onStrokeComplete(stroke)
                        }
                    }
                    currentStroke = null
                },
                onDragCancel = {
                    currentStroke = null
                }
            )
        }
    } else {
        // Eraser mode — tap to remove entire stroke
        Modifier.pointerInput(drawings) {
            detectTapGestures { offset ->
                val docX = offset.x / zoom
                val docY = offset.y / zoom + pageStartY
                val hit = findPageHitStroke(drawings, docX, docY)
                if (hit != null) {
                    onEraseDrawing(hit.id)
                }
            }
        }
    }

    // ── Render ──
    Canvas(modifier = modifier.then(gestureModifier)) {
        // 1. Render saved drawings scoped to this page
        val pageDrawings = drawings.filter { drawing ->
            val strokes = parseStrokeData(drawing.strokeDataJson) ?: return@filter false
            strokes.any { stroke ->
                stroke.points.any { pt ->
                    pt.y in pageStartY..(pageStartY + (size.height / zoom))
                }
            }
        }

        pageDrawings.forEach { drawing ->
            drawDrawingOnPage(drawing, pageStartY, zoom)
        }

        // 2. Render in-progress stroke
        currentStroke?.let { stroke ->
            drawPageStroke(stroke, pageStartY, zoom)
        }
    }
}

/**
 * Find the drawing whose stroke contains the given document-space point
 * and is within this page's Y range.
 */
private fun findPageHitStroke(
    drawings: List<DrawingEntity>,
    docX: Float,
    docY: Float
): DrawingEntity? {
    val hitRadius = 12f
    return drawings.lastOrNull { drawing ->
        val strokes = parseStrokeData(drawing.strokeDataJson) ?: return@lastOrNull false
        strokes.any { stroke ->
            stroke.points.any { pt ->
                val dx = pt.x - docX
                val dy = pt.y - docY
                sqrt(dx * dx + dy * dy) < hitRadius
            }
        }
    }
}

/**
 * Render a [DrawingEntity] onto a page Canvas, offsetting Y by pageStartY
 * and scaling by zoom.
 */
private fun DrawScope.drawDrawingOnPage(
    drawing: DrawingEntity,
    pageStartY: Float,
    zoom: Float
) {
    val strokes = parseStrokeData(drawing.strokeDataJson) ?: return
    val color = Color(drawing.color)
    val alpha = if (drawing.toolType == "highlighter") 0.35f else 1f
    val tool = when (drawing.toolType) {
        "highlighter" -> DrawingTool.HIGHLIGHTER
        "shape" -> DrawingTool.SHAPE
        else -> DrawingTool.PEN
    }

    strokes.forEach { parsedStroke ->
        if (parsedStroke.points.size < 2) return@forEach

        val pagePoints = parsedStroke.points.map { pt ->
            Offset(pt.x * zoom, (pt.y - pageStartY) * zoom)
        }

        if (tool == DrawingTool.SHAPE && parsedStroke.points.size >= 2) {
            drawPageShape(
                points = pagePoints,
                color = color,
                width = drawing.strokeWidth,
                alpha = alpha,
                shapeType = parsedStroke.shapeType ?: ShapeType.RECTANGLE
            )
        } else {
            drawPageSmoothPath(
                points = pagePoints,
                color = color,
                width = drawing.strokeWidth,
                alpha = alpha
            )
        }
    }
}

/**
 * Render the current in-progress stroke on the page.
 */
private fun DrawScope.drawPageStroke(
    stroke: InProgressStroke,
    pageStartY: Float,
    zoom: Float
) {
    if (stroke.points.size < 2) return

    val pagePoints = stroke.points.map { pt ->
        Offset(pt.x * zoom, (pt.y - pageStartY) * zoom)
    }

    val color = Color(stroke.color)
    val alpha = if (stroke.tool == DrawingTool.HIGHLIGHTER) 0.35f else 1f

    if (stroke.tool == DrawingTool.SHAPE) {
        drawPageShape(
            points = pagePoints,
            color = color,
            width = stroke.strokeWidth,
            alpha = alpha,
            shapeType = stroke.shapeType
        )
    } else {
        drawPageSmoothPath(
            points = pagePoints,
            color = color,
            width = stroke.strokeWidth,
            alpha = alpha
        )
    }
}

/**
 * Draw a smooth quadratic bezier path through the given page-local points.
 */
private fun DrawScope.drawPageSmoothPath(
    points: List<Offset>,
    color: Color,
    width: Float,
    alpha: Float
) {
    if (points.size < 2) return

    val path = Path()
    path.moveTo(points[0].x, points[0].y)

    for (i in 1 until points.size) {
        val mid = Offset(
            (points[i - 1].x + points[i].x) / 2f,
            (points[i - 1].y + points[i].y) / 2f
        )
        path.quadraticBezierTo(
            points[i - 1].x, points[i - 1].y,
            mid.x, mid.y
        )
    }

    path.lineTo(points.last().x, points.last().y)

    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(
            width = width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/**
 * Draw a shape based on the first and last points.
 */
private fun DrawScope.drawPageShape(
    points: List<Offset>,
    color: Color,
    width: Float,
    alpha: Float,
    shapeType: ShapeType = ShapeType.RECTANGLE
) {
    if (points.size < 2) return

    val start = points.first()
    val end = points.last()

    val rect = androidx.compose.ui.geometry.Rect(
        left = minOf(start.x, end.x),
        top = minOf(start.y, end.y),
        right = maxOf(start.x, end.x),
        bottom = maxOf(start.y, end.y)
    )

    when (shapeType) {
        ShapeType.RECTANGLE -> {
            drawRect(
                topLeft = rect.topLeft,
                size = rect.size,
                color = color.copy(alpha = alpha * 0.2f),
                style = androidx.compose.ui.graphics.drawscope.Fill
            )
            drawRect(
                topLeft = rect.topLeft,
                size = rect.size,
                color = color.copy(alpha = alpha),
                style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        ShapeType.CIRCLE -> {
            val centerX = (start.x + end.x) / 2f
            val centerY = (start.y + end.y) / 2f
            val radius = maxOf(kotlin.math.abs(end.x - start.x) / 2f, kotlin.math.abs(end.y - start.y) / 2f, 1f)
            drawCircle(
                color = color.copy(alpha = alpha * 0.2f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Fill
            )
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = width, cap = StrokeCap.Round)
            )
        }
        ShapeType.LINE -> {
            drawLine(
                color = color.copy(alpha = alpha),
                start = start,
                end = end,
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
        ShapeType.ARROW -> {
            drawLine(
                color = color.copy(alpha = alpha),
                start = start,
                end = end,
                strokeWidth = width,
                cap = StrokeCap.Round
            )
            val angle = kotlin.math.atan2(
                (end.y - start.y).toDouble(),
                (end.x - start.x).toDouble()
            )
            val arrowLen = 20f + width * 2f
            val arrowAngle = kotlin.math.PI / 6
            val p1 = Offset(
                (end.x - arrowLen * kotlin.math.cos(angle - arrowAngle)).toFloat(),
                (end.y - arrowLen * kotlin.math.sin(angle - arrowAngle)).toFloat()
            )
            val p2 = Offset(
                (end.x - arrowLen * kotlin.math.cos(angle + arrowAngle)).toFloat(),
                (end.y - arrowLen * kotlin.math.sin(angle + arrowAngle)).toFloat()
            )
            val arrowPath = Path().apply {
                moveTo(end.x, end.y)
                lineTo(p1.x, p1.y)
                moveTo(end.x, end.y)
                lineTo(p2.x, p2.y)
            }
            drawPath(
                path = arrowPath,
                color = color.copy(alpha = alpha),
                style = Stroke(width = width + 2f, cap = StrokeCap.Round)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  PageBlock
// ══════════════════════════════════════════════════════════════════════

/**
 * A single block rendered within a page in PAGES mode, with zoom support.
 *
 * Block positions and sizes are scaled by [canvasState.zoom]. Drag and resize
 * deltas are divided by zoom to convert screen-pixel deltas to document-space deltas.
 *
 * @param block the block entity
 * @param pageX X position within the page (document X coordinate)
 * @param pageY Y position relative to the page top (0 = top of this page)
 * @param isSelected whether this block is currently selected
 * @param onTapped called when the block is tapped
 * @param onMoved called with new (pageX, pageY) during drag (intermediate, no undo)
 * @param onMovedFinal called with (startX, startY, finalX, finalY) when drag ends
 * @param onResized called with new (width, height)
 * @param content the block's inner content
 */
@Composable
private fun PageBlock(
    block: CanvasBlockEntity,
    pageX: Float,
    pageY: Float,
    isSelected: Boolean,
    canvasState: CanvasState,
    onTapped: (Long) -> Unit,
    onMoved: (Float, Float) -> Unit,
    onMovedFinal: ((Float, Float, Float, Float) -> Unit)? = null,
    onResized: (Float, Float) -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val zoom = canvasState.zoom

    Box(
        modifier = Modifier
            .offset { IntOffset((pageX * zoom).roundToInt(), (pageY * zoom).roundToInt()) }
            .size(
                width = with(density) { (block.width * zoom).toDp() },
                height = with(density) { (block.height * zoom).toDp() }
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(block.id) {
                var cumulativeDx = 0f
                var cumulativeDy = 0f
                var startX = 0f
                var startY = 0f

                detectDragGestures(
                    onDragStart = {
                        startX = pageX
                        startY = pageY
                        cumulativeDx = 0f
                        cumulativeDy = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        cumulativeDx += dragAmount.x / zoom
                        cumulativeDy += dragAmount.y / zoom
                        val newX = (startX + cumulativeDx).coerceAtLeast(0f)
                        val newY = startY + cumulativeDy
                        canvasState.setLiveBlockPosition(block.id, newX, newY)
                        onMoved(newX, newY)
                    },
                    onDragEnd = {
                        val finalX = (startX + cumulativeDx).coerceAtLeast(0f)
                        val finalY = startY + cumulativeDy
                        canvasState.setLiveBlockPosition(block.id, finalX, finalY)
                        onMovedFinal?.invoke(startX, startY, finalX, finalY)
                    },
                    onDragCancel = {
                        canvasState.removeLiveBlockPosition(block.id)
                    }
                )
            }
            .pointerInput(block.id) {
                detectTapGestures { onTapped(block.id) }
            }
    ) {
        // ── Clean up stale live overrides when entity catches up ──
        LaunchedEffect(block.id, block.positionX, block.positionY, block.width, block.height) {
            val livePos = canvasState.liveBlockPositions[block.id]
            if (livePos != null && livePos.x == block.positionX && livePos.y == block.positionY) {
                canvasState.removeLiveBlockPosition(block.id)
            }
            val liveSize = canvasState.liveBlockSizes[block.id]
            if (liveSize != null && liveSize.width == block.width && liveSize.height == block.height) {
                canvasState.removeLiveBlockSize(block.id)
            }
        }

        // Block content
        Box(Modifier.fillMaxSize()) {
            content()
        }

        // Resize handle (bottom-right, when selected)
        if (isSelected) {
            val liveSize = canvasState.liveBlockSizes[block.id]
            val displayWidth = liveSize?.width ?: block.width
            val displayHeight = liveSize?.height ?: block.height
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        RoundedCornerShape(topStart = 4.dp)
                    )
                    .pointerInput(block.id) {
                        var cumulativeDx = 0f
                        var cumulativeDy = 0f
                        detectDragGestures(
                            onDragStart = {
                                cumulativeDx = 0f
                                cumulativeDy = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                cumulativeDx += dragAmount.x / zoom
                                cumulativeDy += dragAmount.y / zoom
                                val newW = (displayWidth + cumulativeDx).coerceAtLeast(60f)
                                val newH = (displayHeight + cumulativeDy).coerceAtLeast(60f)
                                canvasState.setLiveBlockSize(block.id, newW, newH)
                                onResized(newW, newH)
                            },
                            onDragEnd = {
                                val finalW = (displayWidth + cumulativeDx).coerceAtLeast(60f)
                                val finalH = (displayHeight + cumulativeDy).coerceAtLeast(60f)
                                canvasState.setLiveBlockSize(block.id, finalW, finalH)
                                onResized(finalW, finalH)
                            },
                            onDragCancel = {
                                canvasState.removeLiveBlockSize(block.id)
                            }
                        )
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
