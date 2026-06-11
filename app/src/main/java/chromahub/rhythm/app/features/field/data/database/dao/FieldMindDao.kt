package chromahub.rhythm.app.features.field.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import chromahub.rhythm.app.features.field.data.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldMindDao {
    @Query("SELECT * FROM field_observations WHERE deletedAt IS NULL ORDER BY timestamp DESC") fun observeObservations(): Flow<List<ObservationEntity>>
    @Query("SELECT * FROM field_questions WHERE deletedAt IS NULL ORDER BY updatedAt DESC") fun observeQuestions(): Flow<List<QuestionEntity>>
    @Query("SELECT * FROM field_hypotheses WHERE deletedAt IS NULL ORDER BY updatedAt DESC") fun observeHypotheses(): Flow<List<HypothesisEntity>>
    @Query("SELECT * FROM field_projects WHERE deletedAt IS NULL ORDER BY updatedAt DESC") fun observeProjects(): Flow<List<ProjectEntity>>
    @Query("SELECT * FROM field_sources WHERE deletedAt IS NULL ORDER BY updatedAt DESC") fun observeSources(): Flow<List<SourceEntity>>
    @Query("SELECT * FROM field_data_records WHERE deletedAt IS NULL ORDER BY timestamp DESC") fun observeDataRecords(): Flow<List<DataRecordEntity>>
    @Query("SELECT * FROM field_reports WHERE deletedAt IS NULL ORDER BY updatedAt DESC") fun observeReports(): Flow<List<ReportEntity>>
    @Query("SELECT * FROM field_flashcards WHERE deletedAt IS NULL ORDER BY updatedAt DESC") fun observeFlashcards(): Flow<List<FlashcardEntity>>
    @Query("SELECT * FROM field_tags WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE ASC") fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM field_observations WHERE id = :id LIMIT 1") fun observeObservation(id: Long): Flow<ObservationEntity?>
    @Query("SELECT * FROM field_questions WHERE id = :id LIMIT 1") fun observeQuestion(id: Long): Flow<QuestionEntity?>
    @Query("SELECT * FROM field_hypotheses WHERE id = :id LIMIT 1") fun observeHypothesis(id: Long): Flow<HypothesisEntity?>
    @Query("SELECT * FROM field_projects WHERE id = :id LIMIT 1") fun observeProject(id: Long): Flow<ProjectEntity?>
    @Query("SELECT * FROM field_sources WHERE id = :id LIMIT 1") fun observeSource(id: Long): Flow<SourceEntity?>
    @Query("SELECT * FROM field_reports WHERE id = :id LIMIT 1") fun observeReport(id: Long): Flow<ReportEntity?>
    @Query("SELECT * FROM field_data_records WHERE id = :id LIMIT 1") fun observeDataRecord(id: Long): Flow<DataRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertObservation(entity: ObservationEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertQuestion(entity: QuestionEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHypothesis(entity: HypothesisEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProject(entity: ProjectEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSource(entity: SourceEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDataRecord(entity: DataRecordEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReport(entity: ReportEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFlashcard(entity: FlashcardEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTag(entity: TagEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEvidenceAttachment(entity: EvidenceAttachmentEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun linkObservationTag(ref: ObservationTagCrossRef)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun linkQuestionObservation(ref: QuestionObservationCrossRef)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun linkQuestionSource(ref: QuestionSourceCrossRef)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun linkProjectObservation(ref: ProjectObservationCrossRef)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun linkProjectSource(ref: ProjectSourceCrossRef)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun linkReportSource(ref: ReportSourceCrossRef)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun linkProjectDataRecord(ref: ProjectDataRecordCrossRef)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun linkHypothesisEvidence(ref: HypothesisEvidenceCrossRef)

    @Update suspend fun updateObservation(entity: ObservationEntity)
    @Update suspend fun updateQuestion(entity: QuestionEntity)
    @Update suspend fun updateHypothesis(entity: HypothesisEntity)
    @Update suspend fun updateProject(entity: ProjectEntity)
    @Update suspend fun updateSource(entity: SourceEntity)
    @Update suspend fun updateDataRecord(entity: DataRecordEntity)
    @Update suspend fun updateReport(entity: ReportEntity)
    @Update suspend fun updateFlashcard(entity: FlashcardEntity)

    @Query("SELECT * FROM field_tags WHERE normalizedName = :normalizedName LIMIT 1") suspend fun findTagByNormalizedName(normalizedName: String): TagEntity?
    @Query("DELETE FROM field_observation_tags WHERE observationId = :observationId AND tagId = :tagId") suspend fun unlinkObservationTag(observationId: Long, tagId: Long)
    @Query("DELETE FROM field_observation_tags WHERE observationId = :observationId") suspend fun clearObservationTags(observationId: Long)
    @Query("SELECT t.* FROM field_tags t INNER JOIN field_observation_tags x ON x.tagId = t.id WHERE x.observationId = :observationId ORDER BY t.name") fun observeTagsForObservation(observationId: Long): Flow<List<TagEntity>>
    @Query("SELECT o.* FROM field_observations o INNER JOIN field_observation_tags x ON x.observationId = o.id INNER JOIN field_tags t ON t.id = x.tagId WHERE t.normalizedName = :normalizedName AND o.deletedAt IS NULL ORDER BY o.timestamp DESC") fun observeObservationsByTag(normalizedName: String): Flow<List<ObservationEntity>>
    @Query("SELECT t.name AS name, COUNT(x.observationId) AS observationCount FROM field_tags t INNER JOIN field_observation_tags x ON x.tagId = t.id GROUP BY t.id ORDER BY observationCount DESC, t.name ASC") fun observeCommonTagStatistics(): Flow<List<TagStatistic>>

    @Query("SELECT * FROM field_evidence_attachments WHERE observationId = :observationId AND deletedAt IS NULL ORDER BY createdAt DESC") fun observeAttachmentsForObservation(observationId: Long): Flow<List<EvidenceAttachmentEntity>>
    @Query("SELECT id, observationId, type, caption, uri FROM field_evidence_attachments WHERE deletedAt IS NULL AND (caption LIKE '%' || :query || '%' OR uri LIKE '%' || :query || '%' OR localPath LIKE '%' || :query || '%' OR type LIKE '%' || :query || '%') ORDER BY updatedAt DESC") fun searchAttachmentCaptions(query: String): Flow<List<AttachmentSearchResult>>
    @Query("UPDATE field_evidence_attachments SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id") suspend fun softDeleteAttachment(id: Long, deletedAt: Long)

    @Query("UPDATE field_observations SET archivedAt = :time, status = 'Archived', updatedAt = :time WHERE id = :id") suspend fun archiveObservation(id: Long, time: Long)
    @Query("UPDATE field_questions SET archivedAt = :time, status = 'Archived', updatedAt = :time WHERE id = :id") suspend fun archiveQuestion(id: Long, time: Long)
    @Query("UPDATE field_projects SET archivedAt = :time, status = 'Archived', updatedAt = :time WHERE id = :id") suspend fun archiveProject(id: Long, time: Long)

    @Query("SELECT o.* FROM field_observations o INNER JOIN field_question_observations x ON x.observationId = o.id WHERE x.questionId = :questionId") fun observeObservationsForQuestion(questionId: Long): Flow<List<ObservationEntity>>
    @Query("SELECT s.* FROM field_sources s INNER JOIN field_question_sources x ON x.sourceId = s.id WHERE x.questionId = :questionId") fun observeSourcesForQuestion(questionId: Long): Flow<List<SourceEntity>>
    @Query("SELECT q.* FROM field_questions q WHERE q.relatedProjectId = :projectId OR q.id IN (SELECT questionId FROM field_question_observations WHERE observationId IN (SELECT observationId FROM field_project_observations WHERE projectId = :projectId)) ORDER BY q.updatedAt DESC") fun observeQuestionsForProject(projectId: Long): Flow<List<QuestionEntity>>
    @Query("SELECT o.* FROM field_observations o WHERE o.projectId = :projectId OR o.id IN (SELECT observationId FROM field_project_observations WHERE projectId = :projectId) ORDER BY o.timestamp DESC") fun observeObservationsForProject(projectId: Long): Flow<List<ObservationEntity>>
    @Query("SELECT s.* FROM field_sources s WHERE s.relatedProjectId = :projectId OR s.id IN (SELECT sourceId FROM field_project_sources WHERE projectId = :projectId) ORDER BY s.updatedAt DESC") fun observeSourcesForProject(projectId: Long): Flow<List<SourceEntity>>

    @Query("SELECT * FROM field_observations WHERE deletedAt IS NULL AND (subject LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR factsOnlyNotes LIKE '%' || :query || '%' OR manualLocation LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY timestamp DESC") fun searchObservations(query: String): Flow<List<ObservationEntity>>
    @Query("SELECT * FROM field_questions WHERE deletedAt IS NULL AND (questionText LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR sourceType LIKE '%' || :query || '%' OR status LIKE '%' || :query || '%') ORDER BY updatedAt DESC") fun searchQuestions(query: String): Flow<List<QuestionEntity>>
    @Query("SELECT * FROM field_hypotheses WHERE deletedAt IS NULL AND (prediction LIKE '%' || :query || '%' OR reasoning LIKE '%' || :query || '%' OR evidenceNeeded LIKE '%' || :query || '%' OR resultStatus LIKE '%' || :query || '%') ORDER BY updatedAt DESC") fun searchHypotheses(query: String): Flow<List<HypothesisEntity>>
    @Query("SELECT * FROM field_projects WHERE deletedAt IS NULL AND (title LIKE '%' || :query || '%' OR topicType LIKE '%' || :query || '%' OR objective LIKE '%' || :query || '%' OR researchQuestion LIKE '%' || :query || '%') ORDER BY updatedAt DESC") fun searchProjects(query: String): Flow<List<ProjectEntity>>
    @Query("SELECT * FROM field_sources WHERE deletedAt IS NULL AND (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR type LIKE '%' || :query || '%' OR personalSummary LIKE '%' || :query || '%' OR keyFindings LIKE '%' || :query || '%') ORDER BY updatedAt DESC") fun searchSources(query: String): Flow<List<SourceEntity>>
}
