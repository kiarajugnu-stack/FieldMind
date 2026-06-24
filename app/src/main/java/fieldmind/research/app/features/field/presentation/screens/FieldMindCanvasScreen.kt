package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity
import fieldmind.research.app.features.field.data.database.entity.NoteEntity
import fieldmind.research.app.features.field.data.database.entity.ObservationEntity
import fieldmind.research.app.features.field.presentation.canvas.*
import fieldmind.research.app.features.field.presentation.canvas.FigureSidePanel
import fieldmind.research.app.features.field.presentation.canvas.LinkToEntityDialog
import fieldmind.research.app.features.field.presentation.canvas.PdfBlock
import fieldmind.research.app.features.field.presentation.canvas.DrawingBlock
import fieldmind.research.app.features.field.presentation.canvas.VoiceBlock
import fieldmind.research.app.features.field.presentation.canvas.EquationBlock
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.SwipeableAlertDialog
import fieldmind.research.app.features.field.presentation.components.pressScale
import fieldmind.research.app.features.field.presentation.components.rememberFieldMindHaptics
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import org.json.JSONObject
import androidx.activity.compose.BackHandler

/**
 * Full-screen canvas editor for a single note.
 *
 * Renders blocks on A4-sized pages in a document-style layout via [PageCanvas].
 * - Block rendering for all supported block types (text, image, sticky, table)
 * - Top bar with note title, undo/redo buttons, and save indicator
 * - Keyboard shortcuts: Ctrl+Z (undo), Ctrl+Shift+Z / Ctrl+Y (redo)
 * - Add-block floating button
 * - Block content auto-save with 500ms debounce
 *
 * @param noteId the ID of the note to edit
 * @param fieldViewModel the app-level ViewModel (for note title + entity linking)
 * @param onBack called to navigate back
 * @param onOpenLinkedEntity called to navigate to a linked entity's detail screen (kind, id)
 */
@Composable
fun CanvasScreen(
    noteId: Long,
    fieldViewModel: FieldMindViewModel,
    onBack: () -> Unit,
    onOpenLinkedEntity: ((String, Long) -> Unit)? = null
) {
    val canvasViewModel: CanvasViewModel = viewModel()
    val haptics = rememberFieldMindHaptics()
    val clipboard = LocalClipboardManager.current

    // ── Initialize ViewModel with note ID ──
    LaunchedEffect(noteId) {
        canvasViewModel.setNoteId(noteId)
    }

    // ── Collect state ──
    val blocks by canvasViewModel.blocks.collectAsState()
    val isSaving by canvasViewModel.isSaving.collectAsState()
    val canUndo by canvasViewModel.undoRedo.canUndo.collectAsState()
    val canRedo by canvasViewModel.undoRedo.canRedo.collectAsState()
    val undoLabel by canvasViewModel.undoRedo.undoDescription.collectAsState()
    val redoLabel by canvasViewModel.undoRedo.redoDescription.collectAsState()
    val lastAddedBlockId by canvasViewModel.lastAddedBlockId.collectAsState()
    val drawings by canvasViewModel.drawings.collectAsState()

    // Get the note title from FieldMindViewModel
    val notes by fieldViewModel.notes.collectAsState()
    val note = remember(noteId, notes) { notes.firstOrNull { it.id == noteId } }

    // Track viewport size for minimap
    var viewportSize by remember { mutableStateOf(Size(0f, 0f)) }

    // ── Unsaved-changes confirmation dialog ──
    var showExitConfirm by remember { mutableStateOf(false) }
    var hasEdits by remember { mutableStateOf(false) }
    // Track actual user edits via undo availability — only true after user action
    LaunchedEffect(canUndo) {
        if (canUndo) hasEdits = true
    }

    // Shared back handler: show confirmation if edits pending
    val handleBack = {
        if (hasEdits && !showExitConfirm) {
            showExitConfirm = true
        } else {
            onBack()
        }
    }

    // Handle device back button
    BackHandler(enabled = true) { handleBack() }

    if (showExitConfirm) {
        SwipeableAlertDialog(
            onDismissRequest = { showExitConfirm = false },
            icon = {
                Icon(
                    MaterialSymbolIcon("edit_note"),
                    "Unsaved changes",
                    size = 28.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Discard edits?") },
            text = {
                Text(
                    "You have unsaved changes on the canvas. What would you like to do?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        onBack()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text("Keep editing")
                }
            }
        )
    }

    // Auto-select newly added blocks
    LaunchedEffect(lastAddedBlockId) {
        lastAddedBlockId?.let { id ->
            canvasViewModel.selectBlock(id)
        }
    }

    // ── Figure side panel state ──
    var figurePanelBlockId by remember { mutableStateOf<Long?>(null) }
    val figurePanelMeta = figurePanelBlockId?.let { bid ->
        canvasViewModel.observeFigureMeta(bid).collectAsState(initial = null)
    }

    // ── Link-to-entity dialog state ──
    var linkDialogBlockId by remember { mutableStateOf<Long?>(null) }

    // ── Add-block menu state ──
    var showAddMenu by remember { mutableStateOf(false) }

    // ── Keyboard visibility (hide FAB when typing) ──
    var isKeyboardVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        while (true) {
            val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            isKeyboardVisible = imm?.isAcceptingText ?: false
            kotlinx.coroutines.delay(100)
        }
    }

    // ── Figure gallery state ──
    var showFigureGallery by remember { mutableStateOf(false) }

    // ── PAGES mode: track current page ──
    var currentPage by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(1) }

    // ── Canvas viewport with keyboard shortcuts ──
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when {
                    // Ctrl+Z → Undo
                    event.key == Key.Z && event.isCtrlPressed && !event.isShiftPressed -> {
                        haptics.light()
                        canvasViewModel.undo()
                        true
                    }
                    // Ctrl+Shift+Z or Ctrl+Y → Redo
                    (event.key == Key.Z && event.isCtrlPressed && event.isShiftPressed) ||
                    (event.key == Key.Y && event.isCtrlPressed) -> {
                        haptics.light()
                        canvasViewModel.redo()
                        true
                    }
                    else -> false
                }
            }
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Top bar ──
            CanvasTopBar(
                    note = note,
                    isSaving = isSaving,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    undoLabel = undoLabel,
                    redoLabel = redoLabel,
                    zoom = canvasViewModel.canvasState.zoom,
                    canvasLocked = canvasViewModel.canvasState.canvasLocked,
                    onBack = { handleBack() },
                    onUndo = { haptics.light(); canvasViewModel.undo() },
                    onRedo = { haptics.light(); canvasViewModel.redo() },
                    onZoomIn = { canvasViewModel.canvasState.applyZoom(1.2f, androidx.compose.ui.geometry.Offset(100f, 100f)) },
                    onZoomOut = { canvasViewModel.canvasState.applyZoom(0.83f, androidx.compose.ui.geometry.Offset(100f, 100f)) },
                    onZoomReset = { canvasViewModel.canvasState.resetView() },
                    onToggleLock = { canvasViewModel.canvasState.toggleCanvasLock() },
                    onToggleGallery = { showFigureGallery = !showFigureGallery },
                    currentPage = currentPage,
                    totalPages = totalPages
                )

            // ── Canvas body (Infinite or Pages mode) ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        viewportSize = with(coords.size) {
                            Size(width.toFloat(), height.toFloat())
                        }
                    }
            ) {
                // ── Shared block content callback (used by both modes) ──
                val blockContentCallback: @Composable (CanvasBlockEntity, Boolean) -> Unit = { block, isSelected ->
                    CanvasBlockContent(
                        block = block,
                        isSelected = isSelected,
                        onContentChanged = { contentJson ->
                            canvasViewModel.updateBlockContent(block.id, contentJson)
                        },
                        onInsertBlock = { type ->
                            canvasViewModel.addBlock(
                                type = type,
                                positionX = block.positionX,
                                positionY = block.positionY + block.height + 16f,
                                width = 300f,
                                height = 200f
                            )
                        },
                        canvasViewModel = canvasViewModel
                    )
                }

                // ── Shared block tap callback ──
                val onBlockTappedCallback: (Long) -> Unit = { id ->
                    canvasViewModel.selectBlock(id)
                    val b = blocks.firstOrNull { it.id == id }
                    if (b != null && b.type in listOf("image", "figure", "pdf")) {
                        figurePanelBlockId = id
                    }
                }

                // ── Shared block copy callback ──
                val onBlockCopyCallback: (Long) -> Unit = { id ->
                    val b = blocks.firstOrNull { it.id == id }
                    if (b != null) {
                        clipboard.setText(AnnotatedString(b.contentJson))
                        haptics.confirm()
                    }
                }

                // ── Shared open linked entity callback ──
                val onBlockOpenLinkedCallback: (Long) -> Unit = { id ->
                    val b = blocks.firstOrNull { it.id == id }
                    if (b != null && b.linkedEntityType.isNotBlank() && b.linkedEntityId != null) {
                        onOpenLinkedEntity?.invoke(b.linkedEntityType, b.linkedEntityId!!)
                    }
                }

                val drawingState = canvasViewModel.drawingState

                PageCanvas(
                    canvasState = canvasViewModel.canvasState,
                    blocks = blocks,
                    modifier = Modifier.fillMaxSize(),
                    blockContent = blockContentCallback,
                    onBlockMoved = { id, x, y -> canvasViewModel.moveBlockIntermediate(id, x, y) },
                    onBlockMovedFinal = { id, sx, sy, fx, fy -> canvasViewModel.moveBlockFinal(id, sx, sy, fx, fy) },
                    onBlockResized = { id, w, h -> canvasViewModel.resizeBlockIntermediate(id, w, h) },
                    onBlockResizedFinal = { id, origW, origH, newW, newH -> canvasViewModel.resizeBlockFinal(id, origW, origH, newW, newH) },
                    onBlockTapped = onBlockTappedCallback,
                    onBlockDelete = { id -> canvasViewModel.deleteBlock(id) },
                    onBlockDuplicate = { id -> canvasViewModel.duplicateBlock(id) },
                    onBlockMoveForward = { id -> canvasViewModel.moveBlockForward(id) },
                    onBlockMoveBackward = { id -> canvasViewModel.moveBlockBackward(id) },
                    onBlockCopy = onBlockCopyCallback,
                    onBlockLinkToEntity = { id -> linkDialogBlockId = id },
                    onBlockOpenLinkedEntity = onBlockOpenLinkedCallback,
                    currentPage = { page -> currentPage = page },
                    totalPages = { pages -> totalPages = pages },
                    viewportSize = viewportSize,
                    drawingState = drawingState,
                    drawings = drawings,
                    onStrokeComplete = { stroke -> canvasViewModel.saveStroke(stroke) },
                    onEraseDrawing = { id -> canvasViewModel.eraseDrawing(id) }
                )

                // ── Signal pan/zoom persistence when viewport changes ──
                LaunchedEffect(viewportSize, canvasViewModel.canvasState.zoom, canvasViewModel.canvasState.panX, canvasViewModel.canvasState.panY) {
                    if (viewportSize != Size.Zero) {
                        canvasViewModel.onCanvasViewChanged()
                    }
                }
            }
        }

        // ── Drawing toolbar (floating, above the FAB stack) ──
        if (canvasViewModel.drawingState.showToolbar) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                DrawingToolbar(
                    drawingState = canvasViewModel.drawingState,
                    onDismiss = { canvasViewModel.drawingState.hideToolbar() }
                )
            }
        }

        // ── Figure Gallery overlay (animated) ──
        AnimatedVisibility(
            visible = showFigureGallery,
            enter = fadeIn() + slideInHorizontally { it / 4 },
            exit = fadeOut() + slideOutHorizontally { it / 4 }
        ) {
            FigureGalleryView(
                items = extractGalleryItems(blocks),
                onFigureSelected = { blockId ->
                    // Center the canvas on the selected figure
                    val block = blocks.firstOrNull { it.id == blockId }
                    if (block != null) {
                        val centerX = block.positionX + block.width / 2f
                        val centerY = block.positionY + block.height / 2f
                        val zoom = canvasViewModel.canvasState.zoom
                        // Pan so the block center is at viewport center, accounting for zoom
                        canvasViewModel.canvasState.setPan(
                            centerX - viewportSize.width / 2f / zoom,
                            centerY - viewportSize.height / 2f / zoom
                        )
                        canvasViewModel.selectBlock(blockId)
                        canvasViewModel.onCanvasViewChanged()
                    }
                    showFigureGallery = false
                },
                onDismiss = { showFigureGallery = false }
            )
        }

        // ── Floating action buttons (bottom-right stack) ──
        val isDrawingActive = canvasViewModel.drawingState.showToolbar
        if (!showAddMenu && !isKeyboardVisible) {
            // Drawing toggle FAB (always visible unless keyboard is open)
            SmallFloatingActionButton(
                onClick = {
                    haptics.light()
                    canvasViewModel.drawingState.toggleToolbar()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 160.dp),
                shape = CircleShape,
                containerColor = if (isDrawingActive)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (isDrawingActive)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = if (isDrawingActive) 6.dp else 3.dp)
            ) {
                Icon(
                    MaterialSymbolIcon(if (isDrawingActive) "close" else "draw"),
                    if (isDrawingActive) "Close drawing tools" else "Open drawing tools",
                    size = 20.dp
                )
            }

            // Add block FAB (primary) — hidden during drawing mode to reduce clutter
            if (!isDrawingActive) {
                FloatingActionButton(
                    onClick = {
                        haptics.light()
                        showAddMenu = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 100.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(MaterialSymbolIcon("add"), "Add block", size = 24.dp)
                }
            }
        }

        // ── Add-block menu ──
        if (showAddMenu) {
            AddBlockMenu(
                onSelect = { type ->
                    haptics.confirm()
                    showAddMenu = false
                    // Place new block at a default position (center-ish of viewport)
                    val vp = viewportSize
                    val canvasPos = canvasViewModel.canvasState.screenToCanvas(
                        vp.width / 2f - 150f,
                        vp.height / 2f - 100f
                    )
                    canvasViewModel.addBlock(
                        type = type,
                        positionX = canvasPos.x.coerceAtLeast(0f),
                        positionY = canvasPos.y.coerceAtLeast(0f),
                        width = 300f,
                        height = if (type == "sticky") 200f else 300f
                    )
                },
                onDismiss = { showAddMenu = false },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 160.dp)
            )
        }
        // ── Link-to-Entity dialog ──
        linkDialogBlockId?.let { blockId ->
            LinkToEntityDialog(
                blockId = blockId,
                viewModel = fieldViewModel,
                canvasViewModel = canvasViewModel,
                onLinked = { entityType, entityId, entityName ->
                    // When linking from the figure panel, also update FigureMetaEntity.relatedIdeas
                    figurePanelBlockId?.let { figBlockId ->
                        val existing = figurePanelMeta?.value?.relatedIdeas ?: ""
                        val arr = if (existing.isNotBlank()) {
                            try { org.json.JSONArray(existing) } catch (_: Exception) { org.json.JSONArray() }
                        } else {
                            org.json.JSONArray()
                        }
                        arr.put(
                            org.json.JSONObject().apply {
                                put("id", entityId)
                                put("type", entityType)
                                put("label", entityName)
                            }
                        )
                        canvasViewModel.updateFigureRelatedIdeas(figBlockId, arr.toString())
                    }
                },
                onDismiss = { linkDialogBlockId = null }
            )
        }

        // ── FigureSidePanel (overlay on the right, inside outer Box) ──
        figurePanelBlockId?.let { blockId ->
            val selectedBlock = blocks.firstOrNull { it.id == blockId }
            if (selectedBlock != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.15f))
                        .clickable { figurePanelBlockId = null },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    FigureSidePanel(
                        block = selectedBlock,
                        figureMeta = figurePanelMeta?.value,
                        canvasViewModel = canvasViewModel,
                        onDismiss = { figurePanelBlockId = null },
                        onLinkToEntity = {
                            linkDialogBlockId = blockId
                        }
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Canvas Top Bar — redesigned with overflow menu to reduce clutter
// ══════════════════════════════════════════════════════════════════════
//
// Layout:
//   [Back]  [Title · save dot]  [Page indicator]  [Undo/Redo]  [Zoom]  [⋮]
//                                                                   └─ overflow menu
//                                                                      - Figure Gallery
//                                                                      - Drawing tools

@Composable
private fun CanvasTopBar(
    note: NoteEntity?,
    isSaving: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    undoLabel: String?,
    redoLabel: String?,
    zoom: Float = 1f,
    canvasLocked: Boolean = false,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onZoomReset: () -> Unit = {},
    onToggleLock: () -> Unit = {},
    onToggleGallery: (() -> Unit)? = null,
    currentPage: Int = 0,
    totalPages: Int = 1 {
    var showOverflow by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // ── Back button ──
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(FieldMindIcons.Back, null, size = 20.dp)
            }

            // ── Note title + compact save dot ──
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = note?.title?.ifBlank { "Untitled canvas" } ?: "Canvas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Compact save dot (no label — just a pulsing/gray dot)
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSaving) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                        )
                )
            }

            // ── Page indicator ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    MaterialSymbolIcon("description"),
                    "Pages",
                    size = 12.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${currentPage + 1} / $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Undo / Redo ──
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                Surface(
                    onClick = onUndo,
                    enabled = canUndo,
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .size(32.dp)
                        .pressScale(scaleDown = 0.9f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            MaterialSymbolIcon("undo"),
                            undoLabel ?: "Undo",
                            size = 18.dp,
                            tint = if (canUndo)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }
                }
                Surface(
                    onClick = onRedo,
                    enabled = canRedo,
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .size(32.dp)
                        .pressScale(scaleDown = 0.9f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            MaterialSymbolIcon("redo"),
                            redoLabel ?: "Redo",
                            size = 18.dp,
                            tint = if (canRedo)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // ── Lock toggle ──
            Surface(
                onClick = onToggleLock,
                shape = RoundedCornerShape(8.dp),
                color = if (canvasLocked)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .size(32.dp)
                    .pressScale(scaleDown = 0.9f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        MaterialSymbolIcon(if (canvasLocked) "lock" else "lock_open"),
                        if (canvasLocked) "Unlock canvas" else "Lock canvas",
                        size = 16.dp,
                        tint = if (canvasLocked)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Zoom controls (always visible) ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                    .padding(horizontal = 2.dp)
            ) {
                // Zoom out
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(MaterialSymbolIcon("zoom_out"), "Zoom out", size = 16.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Zoom level (click to reset)
                Text(
                    "${(zoom * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { onZoomReset() }
                )
                // Zoom in
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(MaterialSymbolIcon("zoom_in"), "Zoom in", size = 16.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Overflow menu (⋮) ──
            Box {
                Surface(
                    onClick = { showOverflow = true },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .size(32.dp)
                        .pressScale(scaleDown = 0.9f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            MaterialSymbolIcon("more_vert"),
                            "More options",
                            size = 20.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = showOverflow,
                    onDismissRequest = { showOverflow = false }
                ) {
                    // Figure Gallery
                    if (onToggleGallery != null) {
                        DropdownMenuItem(
                            text = { Text("Figure gallery") },
                            onClick = {
                                showOverflow = false
                                onToggleGallery()
                            },
                            leadingIcon = {
                                Icon(MaterialSymbolIcon("collections_bookmark"), null, size = 18.dp)
                            }
                        )
                    }

                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Block Content Router
// ══════════════════════════════════════════════════════════════════════

/**
 * Routes to the appropriate content composable based on [block.type].
 * Parses [CanvasBlockEntity.contentJson] into the parameters expected
 * by each block composable.
 */
@Composable
private fun CanvasBlockContent(
    block: CanvasBlockEntity,
    isSelected: Boolean,
    onContentChanged: (String) -> Unit,
    onInsertBlock: (String) -> Unit,
    canvasViewModel: CanvasViewModel? = null
) {
    when (block.type) {
        "text" -> {
            TextBlockContent(
                contentJson = block.contentJson,
                isSelected = isSelected,
                onContentChanged = onContentChanged,
                onInsertBlock = onInsertBlock
            )
        }
        "image" -> {
            ImageBlockContent(
                contentJson = block.contentJson,
                isSelected = isSelected,
                onContentChanged = onContentChanged
            )
        }
        "sticky" -> {
            StickyNoteBlockContent(
                contentJson = block.contentJson,
                isSelected = isSelected,
                rotation = block.rotation,
                onContentChanged = onContentChanged,
                onRotationChange = { rot ->
                    // Rotation is handled at the entity level, not in contentJson
                }
            )
        }
        "table" -> {
            TableBlockContent(
                contentJson = block.contentJson,
                isSelected = isSelected,
                onContentChanged = onContentChanged
            )
        }
        "pdf" -> {
            PdfBlock(
                contentJson = block.contentJson,
                onContentChanged = onContentChanged,
                isSelected = isSelected
            )
        }
        "drawing" -> {
            val vm = canvasViewModel
            if (vm != null) {
                val blockDrawings by vm.observeBlockDrawings(block.id)
                    .collectAsState(initial = emptyList())
                DrawingBlock(
                    blockId = block.id,
                    drawings = blockDrawings,
                    onContentChanged = onContentChanged,
                    isSelected = isSelected,
                    onSaveDrawing = { id, json -> vm.saveBlockDrawing(id, json) }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Drawing block",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
        "voice" -> {
            VoiceBlock(
                contentJson = block.contentJson,
                onContentChanged = onContentChanged,
                isSelected = isSelected
            )
        }
        "equation" -> {
            EquationBlock(
                contentJson = block.contentJson,
                onContentChanged = onContentChanged,
                isSelected = isSelected
            )
        }
        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Unknown block type: ${block.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  TextBlock Content
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun TextBlockContent(
    contentJson: String,
    isSelected: Boolean,
    onContentChanged: (String) -> Unit,
    onInsertBlock: (String) -> Unit
) {
    // Parse contentJson: stored as {"text":"..."} or raw Markdown string
    val text = remember(contentJson) {
        if (contentJson.isNotBlank() && contentJson != "{}") {
            try {
                // Try {"text":"..."} format first
                val obj = JSONObject(contentJson)
                val t = obj.optString("text", null)
                if (t != null) t else contentJson
            } catch (_: Exception) {
                // Fallback: try JSON string literal, else use raw
                if (contentJson.startsWith("\"")) {
                    try {
                        org.json.JSONTokener(contentJson).nextValue().toString()
                    } catch (_: Exception) {
                        contentJson
                    }
                } else {
                    contentJson
                }
            }
        } else {
            ""
        }
    }

    TextBlock(
        text = text,
        onTextChange = { newText ->
            // Always store as {"text":"..."} for consistency
            onContentChanged(JSONObject().apply { put("text", newText) }.toString())
        },
        isSelected = isSelected,
        onInsertBlock = onInsertBlock
    )
}

// ══════════════════════════════════════════════════════════════════════
//  ImageBlock Content
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ImageBlockContent(
    contentJson: String,
    isSelected: Boolean,
    onContentChanged: (String) -> Unit
) {
    val (imageUri, caption) = remember(contentJson) {
        if (contentJson.isNotBlank()) {
            try {
                val obj = JSONObject(contentJson)
                obj.optString("uri", "") to obj.optString("caption", "")
            } catch (_: Exception) {
                contentJson to ""
            }
        } else {
            "" to ""
        }
    }

    ImageBlock(
        imageUri = imageUri,
        caption = caption,
        onImageChange = { newUri ->
            val json = try {
                JSONObject(contentJson).apply { put("uri", newUri) }.toString()
            } catch (_: Exception) {
                JSONObject().apply {
                    put("uri", newUri)
                    put("caption", "")
                }.toString()
            }
            onContentChanged(json)
        },
        onCaptionChange = { newCaption ->
            val json = try {
                JSONObject(contentJson).apply { put("caption", newCaption) }.toString()
            } catch (_: Exception) {
                JSONObject().apply {
                    put("uri", imageUri)
                    put("caption", newCaption)
                }.toString()
            }
            onContentChanged(json)
        },
        isSelected = isSelected
    )
}

// ══════════════════════════════════════════════════════════════════════
//  StickyNoteBlock Content
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun StickyNoteBlockContent(
    contentJson: String,
    isSelected: Boolean,
    rotation: Float,
    onContentChanged: (String) -> Unit,
    onRotationChange: (Float) -> Unit
) {
    val (text, colorIndex) = remember(contentJson) {
        if (contentJson.isNotBlank()) {
            try {
                val obj = JSONObject(contentJson)
                obj.optString("text", "") to obj.optInt("color", 0)
            } catch (_: Exception) {
                "" to 0
            }
        } else {
            "" to 0
        }
    }

    StickyNoteBlock(
        text = text,
        onTextChange = { newText ->
            val json = try {
                JSONObject(contentJson).apply { put("text", newText) }.toString()
            } catch (_: Exception) {
                JSONObject().apply {
                    put("text", newText)
                    put("color", colorIndex)
                }.toString()
            }
            onContentChanged(json)
        },
        colorIndex = colorIndex,
        onColorChange = { newColor ->
            val json = try {
                JSONObject(contentJson).apply { put("color", newColor) }.toString()
            } catch (_: Exception) {
                JSONObject().apply {
                    put("text", text)
                    put("color", newColor)
                }.toString()
            }
            onContentChanged(json)
        },
        rotation = rotation,
        onRotationChange = onRotationChange,
        isSelected = isSelected
    )
}

// ══════════════════════════════════════════════════════════════════════
//  TableBlock Content
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun TableBlockContent(
    contentJson: String,
    isSelected: Boolean,
    onContentChanged: (String) -> Unit
) {
    val tableData = remember(contentJson) {
        TableData.fromJson(contentJson) ?: TableData()
    }

    TableBlock(
        tableData = tableData,
        onTableChange = { newData ->
            val json = TableData.toJson(newData)
            onContentChanged(json)
        },
        isSelected = isSelected
    )
}

// ══════════════════════════════════════════════════════════════════════
//  Add-Block Menu
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun AddBlockMenu(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blockTypes = listOf(
        "text" to "Text",
        "image" to "Image",
        "sticky" to "Sticky Note",
        "table" to "Table",
        "pdf" to "PDF",
        "drawing" to "Drawing",
        "voice" to "Voice Note",
        "equation" to "Equation"
    )

    Surface(
        modifier = modifier.widthIn(min = 160.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "Add block",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            blockTypes.forEach { (type, label) ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(scaleDown = 0.96f)
                        .clickable { onSelect(type) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val icon = when (type) {
                            "text" -> MaterialSymbolIcon("text_fields")
                            "image" -> MaterialSymbolIcon("image")
                            "sticky" -> MaterialSymbolIcon("sticky_note_2")
                            "table" -> MaterialSymbolIcon("table")
                            "pdf" -> MaterialSymbolIcon("picture_as_pdf")
                            "drawing" -> MaterialSymbolIcon("draw")
                            "voice" -> MaterialSymbolIcon("mic")
                            "equation" -> MaterialSymbolIcon("functions")
                            else -> MaterialSymbolIcon("add")
                        }
                        Icon(icon, label, size = 18.dp)
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
