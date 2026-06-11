package chromahub.rhythm.app.features.field.data.repository

import chromahub.rhythm.app.features.field.data.database.dao.FieldMindDao
import chromahub.rhythm.app.features.field.data.database.entity.DataRecordEntity
import chromahub.rhythm.app.features.field.data.database.entity.HypothesisEntity
import chromahub.rhythm.app.features.field.data.database.entity.ObservationEntity
import chromahub.rhythm.app.features.field.data.database.entity.ProjectEntity
import chromahub.rhythm.app.features.field.data.database.entity.QuestionEntity
import chromahub.rhythm.app.features.field.data.database.entity.ReportEntity
import chromahub.rhythm.app.features.field.data.database.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

class FieldMindRepository(private val dao: FieldMindDao) {
    val observations: Flow<List<ObservationEntity>> = dao.observeObservations()
    val questions: Flow<List<QuestionEntity>> = dao.observeQuestions()
    val hypotheses: Flow<List<HypothesisEntity>> = dao.observeHypotheses()
    val projects: Flow<List<ProjectEntity>> = dao.observeProjects()
    val sources: Flow<List<SourceEntity>> = dao.observeSources()
    val dataRecords: Flow<List<DataRecordEntity>> = dao.observeDataRecords()
    val reports: Flow<List<ReportEntity>> = dao.observeReports()

    suspend fun addObservation(entity: ObservationEntity) = dao.insertObservation(entity)
    suspend fun addQuestion(entity: QuestionEntity) = dao.insertQuestion(entity)
    suspend fun addHypothesis(entity: HypothesisEntity) = dao.insertHypothesis(entity)
    suspend fun addProject(entity: ProjectEntity) = dao.insertProject(entity)
    suspend fun addSource(entity: SourceEntity) = dao.insertSource(entity)
    suspend fun addDataRecord(entity: DataRecordEntity) = dao.insertDataRecord(entity)
    suspend fun addReport(entity: ReportEntity) = dao.insertReport(entity)

    fun searchObservations(query: String) = dao.searchObservations(query)
    fun searchQuestions(query: String) = dao.searchQuestions(query)
    fun searchProjects(query: String) = dao.searchProjects(query)
    fun searchSources(query: String) = dao.searchSources(query)
}
