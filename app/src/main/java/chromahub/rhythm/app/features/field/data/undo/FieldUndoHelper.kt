package fieldmind.research.app.features.field.data.undo

import android.content.Context
import fieldmind.research.app.features.field.data.database.FieldMindDatabase
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel

/**
 * Helper to integrate the undo manager with the ViewModel.
 * Takes JSON snapshots of entities before mutations and restores on undo.
 */
object FieldUndoHelper {

    private val undoManager get() = FieldUndoManager.getInstance()

    /** Take a snapshot of an entity before making changes. */
    fun snapshotBeforeEdit(
        kind: String,
        id: Long,
        viewModel: FieldMindViewModel
    ) {
        // Use the ViewModel's state to get the current entity
        val json = when (kind) {
            "observation" -> viewModel.observations.value.find { it.id == id }?.run { toSnapshotJson() }
            "note" -> viewModel.notes.value.find { it.id == id }?.run { toSnapshotJson() }
            "question" -> viewModel.questions.value.find { it.id == id }?.run { toSnapshotJson() }
            "project" -> viewModel.projects.value.find { it.id == id }?.run { toSnapshotJson() }
            "source" -> viewModel.sources.value.find { it.id == id }?.run { toSnapshotJson() }
            "data" -> viewModel.dataRecords.value.find { it.id == id }?.run { toSnapshotJson() }
            "report" -> viewModel.reports.value.find { it.id == id }?.run { toSnapshotJson() }
            "flashcard" -> viewModel.flashcards.value.find { it.id == id }?.run { toSnapshotJson() }
            else -> null
        }
        if (json != null) {
            undoManager.record(kind, id, json)
        }
    }

    /** Show a human-readable undo description. */
    fun undoDescription(): String? {
        val snapshot = undoManager.peek() ?: return null
        return "Undo ${snapshot.label()} edit?"
    }

    // Simple JSON-like serialization for snapshot purposes
    private fun ObservationEntity.toSnapshotJson(): String =
        """{"subject":"$subject","category":"$category","factsOnlyNotes":"${factsOnlyNotes.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}","confidenceLevel":"$confidenceLevel","tags":"${tags.replace("\\", "\\\\").replace("\"", "\\\"")}"}"""

    private fun NoteEntity.toSnapshotJson(): String =
        """{"title":"${title.replace("\\", "\\\\").replace("\"", "\\\"")}","body":"${body.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}","category":"$category","tags":"${tags.replace("\\", "\\\\").replace("\"", "\\\"")}"}"""

    private fun QuestionEntity.toSnapshotJson(): String =
        """{"questionText":"${questionText.replace("\\", "\\\\").replace("\"", "\\\"")}","status":"$status","priority":"$priority","answer":"${answer.replace("\\", "\\\\").replace("\"", "\\\"")}"}"""

    private fun ProjectEntity.toSnapshotJson(): String =
        """{"title":"${title.replace("\\", "\\\\").replace("\"", "\\\"")}","objective":"${objective.replace("\\", "\\\\").replace("\"", "\\\"")}","status":"$status"}"""

    private fun SourceEntity.toSnapshotJson(): String =
        """{"title":"${title.replace("\\", "\\\\").replace("\"", "\\\"")}","type":"$type","readingStatus":"$readingStatus","importance":"$importance"}"""

    private fun DataRecordEntity.toSnapshotJson(): String =
        """{"label":"${label.replace("\\", "\\\\").replace("\"", "\\\"")}","toolType":"$toolType","value":"$value","unit":"$unit"}"""

    private fun ReportEntity.toSnapshotJson(): String =
        """{"title":"${title.replace("\\", "\\\\").replace("\"", "\\\"")}","type":"$type","status":"$status"}"""

    private fun FlashcardEntity.toSnapshotJson(): String =
        """{"front":"${front.replace("\\", "\\\\").replace("\"", "\\\"")}","back":"${back.replace("\\", "\\\\").replace("\"", "\\\"")}","type":"$type"}"""
}
