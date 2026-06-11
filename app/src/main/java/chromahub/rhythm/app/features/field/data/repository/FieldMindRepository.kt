package chromahub.rhythm.app.features.field.data.repository

import chromahub.rhythm.app.features.field.data.database.dao.FieldMindDao
import chromahub.rhythm.app.features.field.data.database.entity.*
import kotlinx.coroutines.flow.Flow

class FieldMindRepository(private val dao: FieldMindDao) {
    val observations: Flow<List<ObservationEntity>> = dao.observeObservations()
    val notes: Flow<List<NoteEntity>> = dao.observeNotes()
    val questions: Flow<List<QuestionEntity>> = dao.observeQuestions()
    val hypotheses: Flow<List<HypothesisEntity>> = dao.observeHypotheses()
    val projects: Flow<List<ProjectEntity>> = dao.observeProjects()
    val sources: Flow<List<SourceEntity>> = dao.observeSources()
    val dataRecords: Flow<List<DataRecordEntity>> = dao.observeDataRecords()
    val reports: Flow<List<ReportEntity>> = dao.observeReports()
    val flashcards: Flow<List<FlashcardEntity>> = dao.observeFlashcards()
    val tags: Flow<List<TagEntity>> = dao.observeTags()
    val commonTags: Flow<List<TagStatistic>> = dao.observeCommonTagStatistics()

    fun observeObservation(id: Long) = dao.observeObservation(id)
    fun observeNote(id: Long) = dao.observeNote(id)
    fun observeQuestion(id: Long) = dao.observeQuestion(id)
    fun observeHypothesis(id: Long) = dao.observeHypothesis(id)
    fun observeProject(id: Long) = dao.observeProject(id)
    fun observeSource(id: Long) = dao.observeSource(id)
    fun observeReport(id: Long) = dao.observeReport(id)
    fun observeDataRecord(id: Long) = dao.observeDataRecord(id)
    fun observeFlashcard(id: Long) = dao.observeFlashcard(id)
    fun observeTagsForObservation(id: Long) = dao.observeTagsForObservation(id)
    fun observeAttachmentsForObservation(id: Long) = dao.observeAttachmentsForObservation(id)

    suspend fun addObservation(entity: ObservationEntity): Long = dao.insertObservation(entity)
    suspend fun addNote(entity: NoteEntity): Long = dao.insertNote(entity)
    suspend fun updateNote(entity: NoteEntity) = dao.updateNote(entity.copy(updatedAt = System.currentTimeMillis()))
    suspend fun updateObservation(entity: ObservationEntity) = dao.updateObservation(entity.copy(updatedAt = System.currentTimeMillis()))
    suspend fun addQuestion(entity: QuestionEntity): Long = dao.insertQuestion(entity)
    suspend fun updateQuestion(entity: QuestionEntity) = dao.updateQuestion(entity.copy(updatedAt = System.currentTimeMillis()))
    suspend fun addHypothesis(entity: HypothesisEntity): Long = dao.insertHypothesis(entity)
    suspend fun updateHypothesis(entity: HypothesisEntity) = dao.updateHypothesis(entity.copy(updatedAt = System.currentTimeMillis()))
    suspend fun addProject(entity: ProjectEntity): Long = dao.insertProject(entity)
    suspend fun updateProject(entity: ProjectEntity) = dao.updateProject(entity.copy(updatedAt = System.currentTimeMillis()))
    suspend fun addSource(entity: SourceEntity): Long = dao.insertSource(entity)
    suspend fun updateSource(entity: SourceEntity) = dao.updateSource(entity.copy(updatedAt = System.currentTimeMillis()))
    suspend fun toggleSourceImportant(source: SourceEntity) = updateSource(source.copy(importance = if (source.importance == "Normal") "Important" else "Normal"))
    suspend fun markSourceRead(source: SourceEntity) = updateSource(source.copy(readingStatus = "Read"))
    suspend fun linkSourceToProject(source: SourceEntity, projectId: Long?) = updateSource(source.copy(relatedProjectId = projectId))
    suspend fun updateSourceFile(source: SourceEntity, fileUri: String) = updateSource(source.copy(fileUri = fileUri.trim()))
    suspend fun addDataRecord(entity: DataRecordEntity): Long = dao.insertDataRecord(entity)
    suspend fun updateDataRecord(entity: DataRecordEntity) = dao.updateDataRecord(entity.copy(updatedAt = System.currentTimeMillis()))
    suspend fun addReport(entity: ReportEntity): Long = dao.insertReport(entity)
    suspend fun updateReport(entity: ReportEntity) = dao.updateReport(entity.copy(updatedAt = System.currentTimeMillis()))
    suspend fun addFlashcard(entity: FlashcardEntity): Long = dao.insertFlashcard(entity)
    suspend fun updateFlashcard(entity: FlashcardEntity) = dao.updateFlashcard(entity.copy(updatedAt = System.currentTimeMillis()))
    suspend fun addAttachment(entity: EvidenceAttachmentEntity): Long = dao.insertEvidenceAttachment(entity)

    suspend fun deleteObservation(id: Long) = dao.softDeleteObservation(id, System.currentTimeMillis())
    suspend fun deleteNote(id: Long) = dao.softDeleteNote(id, System.currentTimeMillis())
    suspend fun deleteQuestion(id: Long) = dao.softDeleteQuestion(id, System.currentTimeMillis())
    suspend fun deleteHypothesis(id: Long) = dao.softDeleteHypothesis(id, System.currentTimeMillis())
    suspend fun deleteProject(id: Long) = dao.softDeleteProject(id, System.currentTimeMillis())
    suspend fun deleteSource(id: Long) = dao.softDeleteSource(id, System.currentTimeMillis())
    suspend fun deleteDataRecord(id: Long) = dao.softDeleteDataRecord(id, System.currentTimeMillis())
    suspend fun deleteReport(id: Long) = dao.softDeleteReport(id, System.currentTimeMillis())
    suspend fun deleteFlashcard(id: Long) = dao.softDeleteFlashcard(id, System.currentTimeMillis())

    suspend fun findOrCreateTag(rawName: String): Long {
        val display = rawName.trim().replace(Regex("\\s+"), " ")
        val normalized = display.lowercase()
        if (display.isBlank()) return 0
        dao.findTagByNormalizedName(normalized)?.let { return it.id }
        val inserted = dao.insertTag(TagEntity(name = display, normalizedName = normalized))
        return if (inserted == -1L) dao.findTagByNormalizedName(normalized)?.id ?: 0 else inserted
    }

    suspend fun setObservationTags(observationId: Long, rawTags: String) {
        dao.clearObservationTags(observationId)
        rawTags.split(',', '#')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .forEach { raw ->
                val tagId = findOrCreateTag(raw)
                if (tagId > 0) dao.linkObservationTag(ObservationTagCrossRef(observationId, tagId))
            }
    }

    suspend fun linkQuestionObservation(questionId: Long, observationId: Long) = dao.linkQuestionObservation(QuestionObservationCrossRef(questionId, observationId))
    suspend fun linkQuestionSource(questionId: Long, sourceId: Long) = dao.linkQuestionSource(QuestionSourceCrossRef(questionId, sourceId))
    suspend fun linkProjectObservation(projectId: Long, observationId: Long) = dao.linkProjectObservation(ProjectObservationCrossRef(projectId, observationId))
    suspend fun linkProjectSource(projectId: Long, sourceId: Long) = dao.linkProjectSource(ProjectSourceCrossRef(projectId, sourceId))
    suspend fun linkReportSource(reportId: Long, sourceId: Long) = dao.linkReportSource(ReportSourceCrossRef(reportId, sourceId))
    suspend fun linkProjectDataRecord(projectId: Long, dataRecordId: Long) = dao.linkProjectDataRecord(ProjectDataRecordCrossRef(projectId, dataRecordId))
    suspend fun linkHypothesisEvidence(hypothesisId: Long, observationId: Long) = dao.linkHypothesisEvidence(HypothesisEvidenceCrossRef(hypothesisId, observationId))
    suspend fun archiveObservation(id: Long) = dao.archiveObservation(id, System.currentTimeMillis())
    suspend fun archiveNote(id: Long) = dao.archiveNote(id, System.currentTimeMillis())
    suspend fun archiveQuestion(id: Long) = dao.archiveQuestion(id, System.currentTimeMillis())
    suspend fun archiveProject(id: Long) = dao.archiveProject(id, System.currentTimeMillis())

    fun searchObservations(query: String) = dao.searchObservations(query)
    fun searchNotes(query: String) = dao.searchNotes(query)
    fun searchQuestions(query: String) = dao.searchQuestions(query)
    fun searchHypotheses(query: String) = dao.searchHypotheses(query)
    fun searchProjects(query: String) = dao.searchProjects(query)
    fun searchSources(query: String) = dao.searchSources(query)
    fun searchAttachments(query: String) = dao.searchAttachmentCaptions(query)
}
