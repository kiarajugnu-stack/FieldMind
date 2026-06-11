package chromahub.rhythm.app.features.field.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "field_observations")
data class ObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val category: String,
    val factsOnlyNotes: String,
    val timestamp: Long,
    val date: String,
    val time: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val manualLocation: String = "",
    val confidenceLevel: String,
    val evidenceSummary: String = "",
    val moodOrContext: String = "",
    val tags: String = "",
    val projectId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "field_questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionText: String,
    val category: String = "Other",
    val sourceType: String,
    val status: String,
    val priority: String = "Medium",
    val relatedObservationIds: String = "",
    val relatedSourceIds: String = "",
    val relatedProjectId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "field_hypotheses")
data class HypothesisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val linkedQuestionId: Long? = null,
    val prediction: String,
    val reasoning: String = "",
    val evidenceNeeded: String = "",
    val supportCriteria: String = "",
    val weakeningCriteria: String = "",
    val testMethod: String = "",
    val confidencePercent: Int = 50,
    val resultStatus: String = "Unknown",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "field_projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val topicType: String = "General",
    val objective: String = "",
    val researchQuestion: String = "",
    val backgroundNotes: String = "",
    val hypothesisSummary: String = "",
    val methods: String = "",
    val dataSummary: String = "",
    val analysis: String = "",
    val conclusion: String = "",
    val futureQuestions: String = "",
    val status: String = "Active",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "field_sources")
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val author: String = "",
    val dateOrYear: String = "",
    val link: String = "",
    val personalSummary: String = "",
    val keyFindings: String = "",
    val whatThisSourceTaughtMe: String = "",
    val questionsGenerated: String = "",
    val reliabilityScore: Int = 3,
    val relatedProjectId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "field_data_records")
data class DataRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long? = null,
    val observationId: Long? = null,
    val toolType: String,
    val label: String,
    val value: String = "",
    val unit: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val location: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "field_reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long? = null,
    val title: String,
    val background: String = "",
    val question: String = "",
    val methods: String = "",
    val observations: String = "",
    val results: String = "",
    val interpretation: String = "",
    val conclusion: String = "",
    val limitations: String = "",
    val nextSteps: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "field_tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long = 0xFF5F7F52,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "field_observation_tags", primaryKeys = ["observationId", "tagId"])
data class ObservationTagCrossRef(val observationId: Long, val tagId: Long)

@Entity(tableName = "field_question_observations", primaryKeys = ["questionId", "observationId"])
data class QuestionObservationCrossRef(val questionId: Long, val observationId: Long)

@Entity(tableName = "field_question_sources", primaryKeys = ["questionId", "sourceId"])
data class QuestionSourceCrossRef(val questionId: Long, val sourceId: Long)

@Entity(tableName = "field_project_observations", primaryKeys = ["projectId", "observationId"])
data class ProjectObservationCrossRef(val projectId: Long, val observationId: Long)

@Entity(tableName = "field_project_sources", primaryKeys = ["projectId", "sourceId"])
data class ProjectSourceCrossRef(val projectId: Long, val sourceId: Long)

@Entity(tableName = "field_report_sources", primaryKeys = ["reportId", "sourceId"])
data class ReportSourceCrossRef(val reportId: Long, val sourceId: Long)
