package chromahub.rhythm.app.features.field.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import chromahub.rhythm.app.features.field.data.database.entity.DataRecordEntity
import chromahub.rhythm.app.features.field.data.database.entity.HypothesisEntity
import chromahub.rhythm.app.features.field.data.database.entity.ObservationEntity
import chromahub.rhythm.app.features.field.data.database.entity.ProjectEntity
import chromahub.rhythm.app.features.field.data.database.entity.QuestionEntity
import chromahub.rhythm.app.features.field.data.database.entity.ReportEntity
import chromahub.rhythm.app.features.field.data.database.entity.SourceEntity
import chromahub.rhythm.app.features.field.data.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldMindDao {
    @Query("SELECT * FROM field_observations ORDER BY timestamp DESC")
    fun observeObservations(): Flow<List<ObservationEntity>>

    @Query("SELECT * FROM field_questions ORDER BY updatedAt DESC")
    fun observeQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM field_hypotheses ORDER BY updatedAt DESC")
    fun observeHypotheses(): Flow<List<HypothesisEntity>>

    @Query("SELECT * FROM field_projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM field_sources ORDER BY updatedAt DESC")
    fun observeSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM field_data_records ORDER BY timestamp DESC")
    fun observeDataRecords(): Flow<List<DataRecordEntity>>

    @Query("SELECT * FROM field_reports ORDER BY updatedAt DESC")
    fun observeReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM field_tags ORDER BY name COLLATE NOCASE ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(entity: ObservationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(entity: QuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHypothesis(entity: HypothesisEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(entity: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(entity: SourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataRecord(entity: DataRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(entity: ReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(entity: TagEntity): Long

    @Update suspend fun updateObservation(entity: ObservationEntity)
    @Update suspend fun updateQuestion(entity: QuestionEntity)
    @Update suspend fun updateHypothesis(entity: HypothesisEntity)
    @Update suspend fun updateProject(entity: ProjectEntity)
    @Update suspend fun updateSource(entity: SourceEntity)
    @Update suspend fun updateDataRecord(entity: DataRecordEntity)
    @Update suspend fun updateReport(entity: ReportEntity)

    @Query(
        """
        SELECT * FROM field_observations
        WHERE subject LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
           OR factsOnlyNotes LIKE '%' || :query || '%'
           OR manualLocation LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
        """
    )
    fun searchObservations(query: String): Flow<List<ObservationEntity>>

    @Query(
        """
        SELECT * FROM field_questions
        WHERE questionText LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
           OR sourceType LIKE '%' || :query || '%'
           OR status LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun searchQuestions(query: String): Flow<List<QuestionEntity>>

    @Query(
        """
        SELECT * FROM field_projects
        WHERE title LIKE '%' || :query || '%'
           OR topicType LIKE '%' || :query || '%'
           OR objective LIKE '%' || :query || '%'
           OR researchQuestion LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun searchProjects(query: String): Flow<List<ProjectEntity>>

    @Query(
        """
        SELECT * FROM field_sources
        WHERE title LIKE '%' || :query || '%'
           OR author LIKE '%' || :query || '%'
           OR type LIKE '%' || :query || '%'
           OR personalSummary LIKE '%' || :query || '%'
           OR keyFindings LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun searchSources(query: String): Flow<List<SourceEntity>>
}
