package chromahub.rhythm.app.features.field.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import chromahub.rhythm.app.features.field.data.database.FieldMindDatabase
import chromahub.rhythm.app.features.field.data.database.entity.*
import chromahub.rhythm.app.features.field.data.repository.FieldMindRepository
import chromahub.rhythm.app.features.field.data.settings.FieldMindSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DraftEvidenceAttachment(
    val type: String,
    val uri: String,
    val caption: String = "",
    val localPath: String? = null,
    val mimeType: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

class FieldMindViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FieldMindRepository(FieldMindDatabase.getInstance(application).fieldMindDao())
    val fieldSettings: FieldMindSettings = FieldMindSettings.getInstance(application)

    val observations: StateFlow<List<ObservationEntity>> = repository.observations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val questions: StateFlow<List<QuestionEntity>> = repository.questions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val hypotheses: StateFlow<List<HypothesisEntity>> = repository.hypotheses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val projects: StateFlow<List<ProjectEntity>> = repository.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sources: StateFlow<List<SourceEntity>> = repository.sources.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dataRecords: StateFlow<List<DataRecordEntity>> = repository.dataRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reports: StateFlow<List<ReportEntity>> = repository.reports.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val flashcards: StateFlow<List<FlashcardEntity>> = repository.flashcards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tags: StateFlow<List<TagEntity>> = repository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val commonTags: StateFlow<List<TagStatistic>> = repository.commonTags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addObservation(
        subject: String,
        category: String,
        facts: String,
        confidence: String,
        manualLocation: String,
        tags: String,
        evidence: String,
        context: String,
        projectId: Long? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        attachments: List<DraftEvidenceAttachment> = emptyList(),
        onSaved: ((Long) -> Unit)? = null
    ) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val id = repository.addObservation(
            ObservationEntity(
                subject = subject.trim(),
                category = category,
                factsOnlyNotes = facts.trim(),
                timestamp = now,
                date = dateFormatter.format(Date(now)),
                time = timeFormatter.format(Date(now)),
                confidenceLevel = confidence,
                manualLocation = manualLocation.trim(),
                tags = tags.trim(),
                evidenceSummary = evidence.trim(),
                moodOrContext = context.trim(),
                projectId = projectId,
                latitude = latitude,
                longitude = longitude
            )
        )
        repository.setObservationTags(id, tags)
        projectId?.let { repository.linkProjectObservation(it, id) }
        attachments.forEach { attachment ->
            repository.addAttachment(
                EvidenceAttachmentEntity(
                    observationId = id,
                    type = attachment.type,
                    uri = attachment.uri,
                    localPath = attachment.localPath,
                    caption = attachment.caption
                )
            )
        }
        onSaved?.invoke(id)
    }

    fun updateObservation(entity: ObservationEntity, tags: String = entity.tags) = viewModelScope.launch {
        repository.updateObservation(entity.copy(tags = tags))
        repository.setObservationTags(entity.id, tags)
    }

    fun archiveObservation(id: Long) = viewModelScope.launch { repository.archiveObservation(id) }

    fun addQuestion(question: String, category: String, sourceType: String, status: String, priority: String, observationId: Long? = null, sourceId: Long? = null, projectId: Long? = null) = viewModelScope.launch {
        val id = repository.addQuestion(QuestionEntity(questionText = question.trim(), category = category, sourceType = sourceType, status = status, priority = priority, relatedProjectId = projectId))
        observationId?.let { repository.linkQuestionObservation(id, it) }
        sourceId?.let { repository.linkQuestionSource(id, it) }
    }

    fun addProject(title: String, topicType: String, objective: String, researchQuestion: String) = viewModelScope.launch {
        repository.addProject(ProjectEntity(title = title.trim(), topicType = topicType.trim().ifBlank { "General" }, objective = objective.trim(), researchQuestion = researchQuestion.trim()))
    }

    fun addSource(type: String, title: String, author: String, link: String, summary: String, taught: String, reliability: Int, keyFindings: String = "", questionsGenerated: String = "", paperNotes: String = "", projectId: Long? = null) = viewModelScope.launch {
        repository.addSource(SourceEntity(type = type, title = title.trim(), author = author.trim(), link = link.trim(), personalSummary = summary.trim(), keyFindings = keyFindings.trim(), whatThisSourceTaughtMe = taught.trim(), questionsGenerated = questionsGenerated.trim(), reliabilityScore = reliability, paperNotes = paperNotes.trim(), relatedProjectId = projectId))
    }

    fun addHypothesis(questionId: Long?, prediction: String, evidenceNeeded: String, confidence: Int, reasoning: String = "", supportCriteria: String = "", weakeningCriteria: String = "", testMethod: String = "") = viewModelScope.launch {
        repository.addHypothesis(HypothesisEntity(linkedQuestionId = questionId, prediction = prediction.trim(), reasoning = reasoning.trim(), evidenceNeeded = evidenceNeeded.trim(), supportCriteria = supportCriteria.trim(), weakeningCriteria = weakeningCriteria.trim(), testMethod = testMethod.trim(), confidencePercent = confidence))
    }

    fun addDataRecord(toolType: String, label: String, value: String, unit: String = "", notes: String = "", location: String = "", projectId: Long? = null, observationId: Long? = null) = viewModelScope.launch {
        repository.addDataRecord(DataRecordEntity(toolType = toolType, label = label.trim(), value = value.trim(), unit = unit.trim(), notes = notes.trim(), location = location.trim(), projectId = projectId, observationId = observationId))
    }

    fun addCounter(label: String, count: Int, notes: String) = addDataRecord("Counter", label, count.toString(), "count", notes)

    fun addReport(type: String, title: String, background: String, question: String, methods: String, observations: String, results: String, interpretation: String, conclusion: String, limitations: String, nextSteps: String, projectId: Long? = null) = viewModelScope.launch {
        val markdown = buildMarkdown(type, title, background, question, methods, observations, results, interpretation, conclusion, limitations, nextSteps)
        repository.addReport(ReportEntity(type = type, title = title.trim(), background = background.trim(), question = question.trim(), methods = methods.trim(), observations = observations.trim(), results = results.trim(), interpretation = interpretation.trim(), conclusion = conclusion.trim(), limitations = limitations.trim(), nextSteps = nextSteps.trim(), markdownDraft = markdown, projectId = projectId))
    }

    fun addReport(title: String, question: String, methods: String, conclusion: String) = addReport("Field Report", title, "", question, methods, "", "", "", conclusion, "", "")

    fun addFlashcard(front: String, back: String, type: String, sourceId: Long? = null, projectId: Long? = null) = viewModelScope.launch {
        repository.addFlashcard(FlashcardEntity(front = front.trim(), back = back.trim(), type = type, sourceId = sourceId, projectId = projectId))
    }

    private fun buildMarkdown(type: String, title: String, background: String, question: String, methods: String, observations: String, results: String, interpretation: String, conclusion: String, limitations: String, nextSteps: String): String = buildString {
        appendLine("# ${title.trim()}")
        appendLine()
        appendLine("Type: ${type.trim()}")
        listOf(
            "Background" to background,
            "Question" to question,
            "Methods" to methods,
            "Observations" to observations,
            "Data / Results" to results,
            "Interpretation" to interpretation,
            "Conclusion" to conclusion,
            "Limitations" to limitations,
            "Next Steps" to nextSteps
        ).forEach { (heading, body) -> appendLine("\n## $heading\n${body.trim()}") }
    }

    companion object {
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    }
}
