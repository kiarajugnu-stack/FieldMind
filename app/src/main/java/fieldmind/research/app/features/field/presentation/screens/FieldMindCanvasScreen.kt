package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import fieldmind.research.app.features.field.presentation.canvas.blocks.*
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.rememberFieldMindHaptics
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import org.json.JSONObject

/**
 * Full-screen canvas editor for a single note.
 *
 * Wires [InfiniteCanvas] to [CanvasViewModel], providing:
 * - Block rendering for all supported block types (text, image, sticky, table)
 * - Top bar with note title, undo/redo buttons, and save indicator
 * - Keyboard shortcuts: Ctrl+Z (undo), Ctrl+Shift+Z / Ctrl+Y (redo)
 * - Add-block floating button
 * - Block content auto-save with 500ms debounce
 *
 * @param noteId the ID of the note to edit
 * @param fieldViewModel the app-level ViewModel (for note title + entity linking)
 * @param onBack called to navigate back
 */
@Composable
fun CanvasScreen(
    noteId: Long,
    fieldViewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val canvasViewModel: CanvasViewModel = viewModel()
    val haptics = rememberFieldMindHaptics()

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

    // Get the note title from FieldMindViewModel
    val notes by fieldViewModel.notes.collectAsState()
    val note = remember(noteId, notes) { notes.firstOrNull { it.id == noteId } }

    // Track viewport size for minimap
    var viewportSize by remember { mutableStateOf(Size(0f, 0f)) }

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
                onBack = onBack,
                onUndo = { haptics.light(); canvasViewModel.undo() },
                onRedo = { haptics.light(); canvasViewModel.redo() }
            )

            // ── Infinite canvas ──
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
                InfiniteCanvas(
                    canvasState = canvasViewModel.canvasState,
                    blocks = blocks,
                    modifier = Modifier.fillMaxSize(),
                    blockContent = { block, isSelected ->
                        CanvasBlockContent(
                            block = block,
                            isSelected = isSelected,
                            onContentChanged = { contentJson ->
                                canvasViewModel.updateBlockContent(block.id, contentJson)
                            },
                            onInsertBlock = { type ->
                                // Insert a new block below the current text block
                                canvasViewModel.addBlock(
                                    type = type,
                                    positionX = block.positionX,
                                    positionY = block.positionY + block.height + 16f,
                                    width = 300f,
                                    height = 200f
                                )
                            }
                        )
                    },
                    onBlockMoved = { id, x, y -> canvasViewModel.moveBlock(id, x, y) },
                    onBlockResized = { id, w, h -> canvasViewModel.resizeBlock(id, w, h) },
                    onBlockTapped = { id ->
                        canvasViewModel.selectBlock(id)
                        // Open FigureSidePanel for image/figure/pdf blocks
                        val block = blocks.firstOrNull { it.id == id }
                        if (block != null && block.type in listOf("image", "figure", "pdf")) {
                            figurePanelBlockId = id
                        }
                    },
                    onBlockDelete = { id -> canvasViewModel.deleteBlock(id) },
                    onBlockDuplicate = { id -> canvasViewModel.duplicateBlock(id) },
                    onBlockMoveForward = { id -> canvasViewModel.moveBlockForward(id) },
                    onBlockMoveBackward = { id -> canvasViewModel.moveBlockBackward(id) },
                    onBlockCopy = { id ->
                        val block = blocks.firstOrNull { it.id == id }
                        if (block != null) {
                            val clipboard = LocalClipboardManager.current
                            clipboard.setText(AnnotatedString(block.contentJson))
                            haptics.confirm()
                        }
                    },
                    onBlockLinkToEntity = { id ->
                        linkDialogBlockId = id
                    },
                    showMinimap = true,
                    viewportSize = viewportSize
                )

                // ── Signal pan/zoom persistence when viewport changes ──
                LaunchedEffect(viewportSize, canvasViewModel.canvasState.zoom, canvasViewModel.canvasState.panX, canvasViewModel.canvasState.panY) {
                    if (viewportSize != Size.Zero) {
                        canvasViewModel.onCanvasViewChanged()
                    }
                }
            }
        }

        // ── FAB: Add block ──
        if (!showAddMenu) {
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
//  Canvas Top Bar
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun CanvasTopBar(
    note: NoteEntity?,
    isSaving: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    undoLabel: String?,
    redoLabel: String?,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Back button
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(FieldMindIcons.Back, null, size = 22.dp)
            }

            // Note title
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = note?.title?.ifBlank { "Untitled canvas" } ?: "Canvas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Save indicator
                    if (isSaving) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    "Saving…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                )
                                Text(
                                    "Saved",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                }
            }

            // Undo / Redo buttons
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // Undo
                Surface(
                    onClick = onUndo,
                    enabled = canUndo,
                    shape = RoundedCornerShape(12.dp),
                    color = if (canUndo)
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            MaterialSymbolIcon("undo"),
                            undoLabel ?: "Undo",
                            size = 18.dp,
                            tint = if (canUndo)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }

                // Redo
                Surface(
                    onClick = onRedo,
                    enabled = canRedo,
                    shape = RoundedCornerShape(12.dp),
                    color = if (canRedo)
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            MaterialSymbolIcon("redo"),
                            redoLabel ?: "Redo",
                            size = 18.dp,
                            tint = if (canRedo)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
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
    onInsertBlock: (String) -> Unit
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
    // The contentJson is the Markdown text itself (stored as a JSON string)
    val text = remember(contentJson) {
        if (contentJson.isNotBlank() && contentJson.startsWith("\"")) {
            try {
                org.json.JSONTokener(contentJson).nextValue().toString()
            } catch (_: Exception) {
                contentJson
            }
        } else {
            contentJson
        }
    }

    TextBlock(
        text = text,
        onTextChange = { newText ->
            // Store as a JSON string to maintain valid contentJson format
            val json = try {
                JSONObject().apply { put("text", newText) }.toString()
            } catch (_: Exception) {
                "\"$newText\""
            }
            onContentChanged(json)
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
        "table" to "Table"
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
                    onClick = { onSelect(type) },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
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
