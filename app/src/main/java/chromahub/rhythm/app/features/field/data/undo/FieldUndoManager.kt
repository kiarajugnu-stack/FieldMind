package chromahub.rhythm.app.features.field.data.undo

/**
 * Simple undo manager that records the last N entity state snapshots.
 * Supports undo by restoring the previous snapshot.
 */
class FieldUndoManager(private val maxUndo: Int = 5) {

    private val snapshots = ArrayDeque<UndoSnapshot>(maxUndo + 1)

    fun record(kind: String, id: Long, jsonState: String) {
        if (snapshots.size >= maxUndo) snapshots.removeLast()
        snapshots.addFirst(UndoSnapshot(kind, id, jsonState, System.currentTimeMillis()))
    }

    fun undo(): UndoSnapshot? = snapshots.removeFirstOrNull()

    fun peek(): UndoSnapshot? = snapshots.firstOrNull()

    val hasUndo: Boolean get() = snapshots.isNotEmpty()

    val undoCount: Int get() = snapshots.size

    data class UndoSnapshot(
        val kind: String,
        val id: Long,
        val jsonState: String,
        val timestamp: Long
    ) {
        fun label(): String = when (kind) {
            "observation" -> "Observation"
            "note" -> "Note"
            "question" -> "Question"
            "project" -> "Project"
            "source" -> "Source"
            "data" -> "Data record"
            "report" -> "Report"
            "flashcard" -> "Flashcard"
            else -> kind
        }
    }

    companion object {
        @Volatile private var INSTANCE: FieldUndoManager? = null
        fun getInstance(): FieldUndoManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: FieldUndoManager().also { INSTANCE = it }
        }
        fun resetInstance() { INSTANCE = null }
    }
}
