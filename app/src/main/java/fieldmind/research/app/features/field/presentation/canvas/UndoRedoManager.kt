package fieldmind.research.app.features.field.presentation.canvas

import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity
import fieldmind.research.app.features.field.data.canvas.CanvasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A single undoable action on the canvas.
 *
 * Each command captures the before/after state at creation time so it can
 * be replayed forward and reversed independently of the ViewModel lifecycle.
 */
sealed interface CanvasCommand {

    /** Apply the forward action. */
    suspend fun execute(repo: CanvasRepository)

    /** Reverse the action, restoring the previous state. */
    suspend fun undo(repo: CanvasRepository)

    /** Human-readable label for the undo/redo menu (e.g. "Move block", "Delete block"). */
    val description: String

    // ────────────────────────────────────────────────
    //  Concrete command types
    // ────────────────────────────────────────────────

    /** Insert a brand-new canvas block. */
    data class InsertBlock(
        val block: CanvasBlockEntity,
        override val description: String = "Add block"
    ) : CanvasCommand {
        override suspend fun execute(repo: CanvasRepository) {
            repo.upsertBlock(block)
        }
        override suspend fun undo(repo: CanvasRepository) {
            // Hard-delete so the undo doesn't collide with a re-insert
            repo.hardDeleteBlock(block.id)
        }
    }

    /** Soft-delete a block (restore it on undo). */
    data class DeleteBlock(
        val block: CanvasBlockEntity,
        override val description: String = "Delete block"
    ) : CanvasCommand {
        override suspend fun execute(repo: CanvasRepository) {
            repo.hardDeleteBlock(block.id)
        }
        override suspend fun undo(repo: CanvasRepository) {
            repo.upsertBlock(block)
        }
    }

    /** Move a block to new coordinates. */
    data class MoveBlock(
        val id: Long,
        val oldX: Float,
        val oldY: Float,
        val newX: Float,
        val newY: Float,
        override val description: String = "Move block"
    ) : CanvasCommand {
        override suspend fun execute(repo: CanvasRepository) {
            repo.updateBlockPosition(id, newX, newY)
        }
        override suspend fun undo(repo: CanvasRepository) {
            repo.updateBlockPosition(id, oldX, oldY)
        }
    }

    /** Resize a block to new dimensions. */
    data class ResizeBlock(
        val id: Long,
        val oldW: Float,
        val oldH: Float,
        val newW: Float,
        val newH: Float,
        override val description: String = "Resize block"
    ) : CanvasCommand {
        override suspend fun execute(repo: CanvasRepository) {
            repo.updateBlockSize(id, newW, newH)
        }
        override suspend fun undo(repo: CanvasRepository) {
            repo.updateBlockSize(id, oldW, oldH)
        }
    }

    /** Change a block's z-order (forward = higher z). */
    data class ReorderBlock(
        val id: Long,
        val oldZ: Int,
        val newZ: Int,
        override val description: String
    ) : CanvasCommand {
        override suspend fun execute(repo: CanvasRepository) {
            repo.updateBlockZIndex(id, newZ)
        }
        override suspend fun undo(repo: CanvasRepository) {
            repo.updateBlockZIndex(id, oldZ)
        }
    }

    /** Duplicate a block (creates a new entity with offset position). */
    data class DuplicateBlock(
        val originalId: Long,
        val newBlock: CanvasBlockEntity,
        override val description: String = "Duplicate block"
    ) : CanvasCommand {
        override suspend fun execute(repo: CanvasRepository) {
            repo.upsertBlock(newBlock)
        }
        override suspend fun undo(repo: CanvasRepository) {
            repo.hardDeleteBlock(newBlock.id)
        }
    }

    /** Atomic group of commands executed together (single undo/redo operation). */
    data class Batch(
        val commands: List<CanvasCommand>,
        override val description: String
    ) : CanvasCommand {
        override suspend fun execute(repo: CanvasRepository) {
            commands.forEach { it.execute(repo) }
        }
        override suspend fun undo(repo: CanvasRepository) {
            commands.reversed().forEach { it.undo(repo) }
        }
    }
}

/**
 * Bounded undo/redo history stack.
 *
 * Stores up to [maxHistory] commands in each direction. When a new command is
 * executed via [execute], the redo stack is cleared (standard undo branching model).
 *
 * Exposes observable [canUndo], [canRedo], [undoDescription], and [redoDescription]
 * for driving UI (e.g. enabling/disabling buttons, showing tooltips).
 */
class CommandHistory(
    private val maxHistory: Int = 100
) {
    private val undoStack = mutableListOf<CanvasCommand>()
    private val redoStack = mutableListOf<CanvasCommand>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _undoDescription = MutableStateFlow<String?>(null)
    val undoDescription: StateFlow<String?> = _undoDescription.asStateFlow()

    private val _redoDescription = MutableStateFlow<String?>(null)
    val redoDescription: StateFlow<String?> = _redoDescription.asStateFlow()

    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size

    /**
     * Record and execute a command.
     * The caller is responsible for calling [CanvasCommand.execute] separately if needed.
     */
    fun push(command: CanvasCommand) {
        undoStack.add(command)
        if (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateFlows()
    }

    /** Pop the most recent command for undo. Returns null if stack is empty. */
    fun popUndo(): CanvasCommand? {
        if (undoStack.isEmpty()) return null
        val command = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(command)
        updateFlows()
        return command
    }

    /** Pop the most recent undone command for redo. Returns null if stack is empty. */
    fun popRedo(): CanvasCommand? {
        if (redoStack.isEmpty()) return null
        val command = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(command)
        updateFlows()
        return command
    }

    /** Clear both stacks (e.g. when switching notes). */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateFlows()
    }

    private fun updateFlows() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _undoDescription.value = undoStack.lastOrNull()?.description
        _redoDescription.value = redoStack.lastOrNull()?.description
    }
}
