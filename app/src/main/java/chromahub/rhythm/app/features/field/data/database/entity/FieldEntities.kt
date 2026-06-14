package fieldmind.research.app.features.field.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

data class TagStatistic(val name: String, val observationCount: Int)
data class AttachmentSearchResult(val id: Long, val observationId: Long, val type: String, val caption: String, val uri: String)

@Entity(tableName = "field_notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String = "",
    val category: String = "Other",
    val tags: String = "",
    val projectId: Long? = null,
    val sourceId: Long? = null,
    val attachmentUris: String = "",
    val status: String = "Active",
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

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
    val structuredDetailsJson: String = "",
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val durationMs: Long? = null,
    val changeObservedAt: Long? = null,
    val changeDurationMs: Long? = null,
    val timeNote: String = "",
    val weatherTemperature: Double? = null,
    val weatherCondition: String = "",
    val weatherHumidity: Int? = null,
    val weatherWindSpeed: Double? = null,
    val weatherCloudCover: Int? = null,
    val weatherPressure: Double? = null,
    val weatherSnapshotAt: Long? = null,
    val tags: String = "",
    val projectId: Long? = null,
    val qualityScore: Int = 0,
    val parentObservationId: Long? = null,
    val followUpScheduledAt: Long? = null,
    val status: String = "Active",
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "field_evidence_attachments")
data class EvidenceAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val observationId: Long,
    val type: String,
    val uri: String,
    val localPath: String? = null,
    val caption: String = "",
    val status: String = "Active",
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
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
    val answer: String = "",
    val answeredAt: Long? = null,
    val relatedObservationIds: String = "",
    val relatedSourceIds: String = "",
    val relatedProjectId: Long? = null,
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
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
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
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
    val connectionMap: String = "",
    val attachmentUris: String = "",
    val projectType: String = "Observation",
    val selectedMethods: String = "",
    val status: String = "Active",
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
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
    val doiOrIsbn: String = "",
    val publisherOrJournal: String = "",
    val accessDate: String = "",
    val fileUri: String = "",
    val citationStyleNote: String = "",
    val importance: String = "Normal",
    val personalSummary: String = "",
    val keyFindings: String = "",
    val whatThisSourceTaughtMe: String = "",
    val questionsGenerated: String = "",
    val reliabilityScore: Int = 3,
    val readingStatus: String = "In progress",
    val paperNotes: String = "",
    val relatedProjectId: Long? = null,
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
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
    val datasetKind: String = "Manual",
    val chartPreference: String = "Line",
    val linkedSessionId: Long? = null,
    val status: String = "Active",
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "field_reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long? = null,
    val type: String = "Field Report",
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
    val markdownDraft: String = "",
    val templateId: String = "field_report",
    val preset: String = "Personal log",
    val status: String = "Draft",
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "field_flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val front: String,
    val back: String,
    val type: String,
    val sourceId: Long? = null,
    val projectId: Long? = null,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long? = null,
    val nextReviewAt: Long? = null,
    /** SM-2 ease factor — lower means harder card (minimum 1.3). */
    val easeFactor: Double = 2.5,
    /** SM-2 interval in days until next review. */
    val intervalDays: Int = 0,
    /** Number of successful consecutive reviews (SM-2 repetition count). */
    val repetitionCount: Int = 0,
    /** Per-deck flashcard mode: "basic" = simple flip, "sm2" = spaced repetition. */
    val deckMode: String = "basic",
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "field_tags", indices = [Index(value = ["normalizedName"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String = name.trim().lowercase(),
    val color: Long = 0xFF5F7F52,
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
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

@Entity(tableName = "field_project_data_records", primaryKeys = ["projectId", "dataRecordId"])
data class ProjectDataRecordCrossRef(val projectId: Long, val dataRecordId: Long)

@Entity(tableName = "field_hypothesis_evidence", primaryKeys = ["hypothesisId", "observationId"])
data class HypothesisEvidenceCrossRef(val hypothesisId: Long, val observationId: Long)

data class ObservationWithTagsAndAttachments(
    val observation: ObservationEntity,
    val tags: List<TagEntity>,
    val attachments: List<EvidenceAttachmentEntity>
)

data class QuestionWithEvidence(
    val question: QuestionEntity,
    val observations: List<ObservationEntity>,
    val sources: List<SourceEntity>,
    val hypotheses: List<HypothesisEntity>
)

data class ProjectWorkspace(
    val project: ProjectEntity,
    val questions: List<QuestionEntity>,
    val observations: List<ObservationEntity>,
    val sources: List<SourceEntity>,
    val dataRecords: List<DataRecordEntity>,
    val reports: List<ReportEntity>
)

data class SourceWithLinks(
    val source: SourceEntity,
    val questions: List<QuestionEntity>,
    val projects: List<ProjectEntity>,
    val reports: List<ReportEntity>
)

data class ReportWithSources(
    val report: ReportEntity,
    val sources: List<SourceEntity>
)

@Entity(tableName = "field_research_sessions")
data class ResearchSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val projectId: Long? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val totalDurationMs: Long = 0,
    val observationCount: Int = 0,
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String = "Active",
    val notes: String = "",
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "field_session_observations", primaryKeys = ["sessionId", "observationId"])
data class SessionObservationCrossRef(val sessionId: Long, val observationId: Long)
