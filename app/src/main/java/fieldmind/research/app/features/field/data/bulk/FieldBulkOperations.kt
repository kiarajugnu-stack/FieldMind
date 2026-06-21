package fieldmind.research.app.features.field.data.bulk

import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel

/**
 * Bulk operations for multi-selection on entity list screens.
 * Supports bulk delete, archive, tag operations.
 */
class FieldBulkOperationManager(private val viewModel: FieldMindViewModel) {

    private var _selectedIds: MutableSet<Long> = mutableSetOf()
    val selectedIds: Set<Long> get() = _selectedIds.toSet()
    val selectionCount: Int get() = _selectedIds.size
    val isSelectionActive: Boolean get() = _selectedIds.isNotEmpty()

    fun toggle(id: Long) {
        if (_selectedIds.contains(id)) _selectedIds.remove(id) else _selectedIds.add(id)
    }

    fun selectAll(ids: List<Long>) {
        _selectedIds.addAll(ids)
    }

    fun clearSelection() {
        _selectedIds.clear()
    }

    fun bulkDelete(kind: String) {
        _selectedIds.forEach { id -> deleteEntity(kind, id) }
        _selectedIds.clear()
    }

    fun bulkArchive(kind: String) {
        _selectedIds.forEach { id ->
            if (kind == "observation") viewModel.archiveObservation(id)
            else deleteEntity(kind, id)
        }
        _selectedIds.clear()
    }

    fun bulkTag(kind: String, tag: String) {
        _selectedIds.forEach { id -> tagEntity(kind, id, tag) }
        _selectedIds.clear()
    }

    private fun deleteEntity(kind: String, id: Long) {
        when (kind) {
            "observation" -> viewModel.deleteObservation(id)
            "note" -> viewModel.deleteNote(id)
            "question" -> viewModel.deleteQuestion(id)
            "project" -> viewModel.deleteProject(id)
            "source" -> viewModel.deleteSource(id)
            "data" -> viewModel.deleteDataRecord(id)
            "report" -> viewModel.deleteReport(id)
            "flashcard" -> viewModel.deleteFlashcard(id)
        }
    }

    private fun tagEntity(kind: String, id: Long, tag: String) {
        when (kind) {
            "observation" -> {
                val obs = viewModel.observations.value.find { it.id == id }
                if (obs != null) {
                    val merged = if (obs.tags.isBlank()) tag else "${obs.tags}, $tag"
                    viewModel.updateObservation(obs.copy(tags = merged))
                }
            }
            "note" -> {
                val note = viewModel.notes.value.find { it.id == id }
                if (note != null) {
                    val merged = if (note.tags.isBlank()) tag else "${note.tags}, $tag"
                    viewModel.updateNoteEntity(note.copy(tags = merged))
                }
            }
        }
    }

    companion object {
        private const val MAX_BULK_SELECTION = 100
    }
}
