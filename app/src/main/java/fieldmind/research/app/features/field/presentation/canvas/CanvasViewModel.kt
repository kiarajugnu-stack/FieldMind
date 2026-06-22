package fieldmind.research.app.features.field.presentation.canvas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity
import fieldmind.research.app.features.field.data.canvas.CanvasRepository
import fieldmind.research.app.features.field.data.database.FieldMindDatabase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the infinite canvas feature.
 *
 * Responsibilities:
 * - Wires [CanvasBlockEntity] objects from Room to [InfiniteCanvas]
 * - Auto-saves block content with 500 ms debounce (avoids DB writes on every keystroke)
 * - Persists zoom/pan state to [NoteEntity.canvasZoomLevel] / [canvasPanX] / [canvasPanY]
 * - Provides undo/redo via [CommandHistory]
 *
 * **Usage:**
 * ```kotlin
 * val vm: CanvasViewModel = viewModel()
 * vm.setNoteId(noteId)         // must be called once after creation
 * ```
 */
@OptIn(FlowPreview::class)
class CanvasViewModel(application: Application) : AndroidViewModel(application) {

    // ── Data sources ──────────────────────────────────────────────

    private val db = FieldMindDatabase.getInstance(application)
    private val repository = CanvasRepository(
        blockDao = db.canvasBlockDao(),
        drawingDao = db.drawingDao(),
        figureDao = db.figureMetaDao()
    )

    // ── State ─────────────────────────────────────────────────────

    /** Shared camera + selection state. */
    val canvasState = CanvasState()

    /** Undo/redo history. */
    val undoRedo = CommandHistory()

    /** The note currently being edited. Set via [setNoteId]. */
    private val _noteId = MutableStateFlow<Long?>(null)

    /** Live block list observed from Room. */
    private val _blocks = MutableStateFlow<List<CanvasBlockEntity>>(emptyList())
    val blocks: StateFlow<List<CanvasBlockEntity>> = _blocks.asStateFlow()

    /** True while a debounced save is pending/in-flight. */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Emits the ID of the most recently added block (for the caller to select it). */
    private val _lastAddedBlockId = MutableStateFlow<Long?>(null)
    val lastAddedBlockId: StateFlow<Long?> = _lastAddedBlockId.asStateFlow()

    // ── Debounce pipelines ────────────────────────────────────────

    /**
     * Debounced content saves: (blockId, contentJson) pairs emitted here
     * are flushed to Room after 500 ms of inactivity.
     */
    private val _contentSaveChannel = MutableSharedFlow<Pair<Long, String>>(
        extraBufferCapacity = 16
    )

    /**
     * Signals that zoom or pan has changed and should be persisted.
     */
    private val _zoomPanSaveSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        // 1. Observe blocks for the current note
        viewModelScope.launch {
            _noteId.flatMapLatest { id ->
                if (id != null) repository.observeBlocksForNote(id)
                else flowOf(emptyList())
            }.collect { list ->
                _blocks.value = list
            }
        }

        // 2. Debounced content saves (500 ms)
        viewModelScope.launch {
            _contentSaveChannel
                .debounce(500)
                .collect { (blockId, contentJson) ->
                    repository.updateBlockContent(blockId, contentJson)
                    _isSaving.value = false
                }
        }

        // 3. Debounced zoom/pan persistence to NoteEntity (500 ms)
        viewModelScope.launch {
            _zoomPanSaveSignal
                .debounce(500)
                .combine(_noteId) { _, id -> id }
                .collect { id ->
                    if (id != null) {
                        _isSaving.value = true
                        try {
                            persistZoomPanToNote(id)
                        } finally {
                            _isSaving.value = false
                        }
                    }
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═══════════════════════════════════════════════════════════════

    /**
     * Set the note this canvas is editing.
     * Must be called at least once before performing any CRUD operations.
     *
     * Restores the saved zoom/pan from the note entity (if any).
     */
    fun setNoteId(id: Long) {
        if (_noteId.value == id) return
        _noteId.value = id
        undoRedo.clear()
        // Restore saved camera state
        viewModelScope.launch {
            val note = db.fieldMindDao().observeNote(id).first()
            if (note != null) {
                canvasState.setZoom(note.canvasZoomLevel)
                canvasState.setPan(note.canvasPanX, note.canvasPanY)
            }
        }
    }

    /** The current note ID, or null if not yet set. */
    val noteId: Long? get() = _noteId.value

    // ═══════════════════════════════════════════════════════════════
    //  Block CRUD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create a new block on the current note.
     *
     * @param type block type string (e.g. "text", "image", "sticky", "table")
     * @param positionX canvas-space X
     * @param positionY canvas-space Y
     * @param width initial width (default 200 dp-equivalent px)
     * @param height initial height (default 150 dp-equivalent px)
     * @param contentJson initial serialised content
     *
     * The new block's ID is emitted via [lastAddedBlockId] after the DB write completes.
     */
    fun addBlock(
        type: String,
        positionX: Float,
        positionY: Float,
        width: Float = 200f,
        height: Float = 150f,
        contentJson: String = "{}"
    ) {
        val nid = _noteId.value ?: return
        viewModelScope.launch {
            val maxZ = repository.maxZIndexForNote(nid)
            val block = CanvasBlockEntity(
                noteId = nid,
                type = type,
                contentJson = contentJson,
                positionX = positionX,
                positionY = positionY,
                width = width,
                height = height,
                zIndex = (maxZ ?: -1) + 1
            )
            val blockId = repository.upsertBlock(block)
            if (blockId > 0L) {
                undoRedo.push(CanvasCommand.InsertBlock(block.copy(id = blockId)))
                _lastAddedBlockId.value = blockId
            }
        }
    }

    /**
     * Soft-delete a block. Can be undone.
     */
    fun deleteBlock(id: Long) {
        viewModelScope.launch {
            val block = repository.getBlock(id) ?: return@launch
            undoRedo.push(CanvasCommand.DeleteBlock(block))
            repository.hardDeleteBlock(id)
        }
    }

    /**
     * Duplicate a block with a slight offset so the copy is visible.
     * The new block's ID is emitted via [lastAddedBlockId] after the DB write completes.
     */
    fun duplicateBlock(id: Long) {
        val original = _blocks.value.firstOrNull { it.id == id } ?: return
        val nid = _noteId.value ?: return
        viewModelScope.launch {
            val maxZ = repository.maxZIndexForNote(nid)
            val copy = original.copy(
                id = 0L,
                positionX = original.positionX + 24f,
                positionY = original.positionY + 24f,
                zIndex = (maxZ ?: -1) + 1,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newId = repository.upsertBlock(copy)
            if (newId > 0L) {
                undoRedo.push(CanvasCommand.DuplicateBlock(originalId = id, newBlock = copy.copy(id = newId)))
                _lastAddedBlockId.value = newId
            }
        }
    }

    /**
     * Update block content (debounced to 500 ms).
     * Call this on every keystroke / content change.
     */
    fun updateBlockContent(blockId: Long, contentJson: String) {
        _isSaving.value = true
        _contentSaveChannel.tryEmit(blockId to contentJson)
    }

    /**
     * Move a block to new canvas-space coordinates.
     * Saves immediately and records undo.
     */
    fun moveBlock(id: Long, x: Float, y: Float) {
        val block = _blocks.value.firstOrNull { it.id == id } ?: return
        val oldX = block.positionX
        val oldY = block.positionY
        if (oldX == x && oldY == y) return

        viewModelScope.launch {
            repository.updateBlockPosition(id, x, y)
            undoRedo.push(CanvasCommand.MoveBlock(id, oldX, oldY, x, y))
        }
    }

    /**
     * Move a block during active dragging (intermediate positions).
     * Saves to DB immediately but does NOT record undo for each drag frame.
     * Use [moveBlock] for the final drag-end position to record undo.
     */
    fun moveBlockIntermediate(id: Long, x: Float, y: Float) {
        viewModelScope.launch {
            repository.updateBlockPosition(id, x, y)
        }
    }

    /**
     * Resize a block to new dimensions. Records undo.
     */
    fun resizeBlock(id: Long, w: Float, h: Float) {
        val block = _blocks.value.firstOrNull { it.id == id } ?: return
        val oldW = block.width
        val oldH = block.height
        if (oldW == w && oldH == h) return

        viewModelScope.launch {
            repository.updateBlockSize(id, w, h)
            undoRedo.push(CanvasCommand.ResizeBlock(id, oldW, oldH, w, h))
        }
    }

    /**
     * Increase the block's z-index by 1 (move visual layer forward).
     */
    fun moveBlockForward(id: Long) {
        val block = _blocks.value.firstOrNull { it.id == id } ?: return
        val newZ = block.zIndex + 1
        viewModelScope.launch {
            repository.updateBlockZIndex(id, newZ)
            undoRedo.push(
                CanvasCommand.ReorderBlock(id, block.zIndex, newZ, "Move forward")
            )
        }
    }

    /**
     * Decrease the block's z-index by 1 (move visual layer backward).
     */
    fun moveBlockBackward(id: Long) {
        val block = _blocks.value.firstOrNull { it.id == id } ?: return
        val newZ = (block.zIndex - 1).coerceAtLeast(0)
        viewModelScope.launch {
            repository.updateBlockZIndex(id, newZ)
            undoRedo.push(
                CanvasCommand.ReorderBlock(id, block.zIndex, newZ, "Move backward")
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Undo / Redo
    // ═══════════════════════════════════════════════════════════════

    /** Undo the most recent action. */
    fun undo() {
        val command = undoRedo.popUndo() ?: return
        viewModelScope.launch {
            command.undo(repository)
        }
    }

    /** Redo the most recently undone action. */
    fun redo() {
        val command = undoRedo.popRedo() ?: return
        viewModelScope.launch {
            command.execute(repository)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Canvas view persistence
    // ═══════════════════════════════════════════════════════════════

    /**
     * Signal that the user has panned/zoomed.
     * Triggers a debounced save of [canvasState] to the current note entity.
     */
    fun onCanvasViewChanged() {
        _zoomPanSaveSignal.tryEmit(Unit)
    }

    private suspend fun persistZoomPanToNote(noteId: Long) {
        val note = db.fieldMindDao().observeNote(noteId).first() ?: return
        if (note.canvasZoomLevel != canvasState.zoom ||
            note.canvasPanX != canvasState.panX ||
            note.canvasPanY != canvasState.panY
        ) {
            db.fieldMindDao().updateNote(
                note.copy(
                    canvasZoomLevel = canvasState.zoom,
                    canvasPanX = canvasState.panX,
                    canvasPanY = canvasState.panY,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Selection (delegated to CanvasState)
    // ═══════════════════════════════════════════════════════════════

    fun selectBlock(id: Long) = canvasState.selectBlock(id)
    fun clearSelection() = canvasState.clearSelection()
    fun toggleBlockSelection(id: Long) = canvasState.toggleBlockSelection(id)
}
