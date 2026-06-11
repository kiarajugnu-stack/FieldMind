package chromahub.rhythm.app.features.field.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import chromahub.rhythm.app.features.field.data.database.FieldMindDatabase
import chromahub.rhythm.app.features.field.data.database.entity.DataRecordEntity
import chromahub.rhythm.app.features.field.data.database.entity.HypothesisEntity
import chromahub.rhythm.app.features.field.data.database.entity.ObservationEntity
import chromahub.rhythm.app.features.field.data.database.entity.ProjectEntity
import chromahub.rhythm.app.features.field.data.database.entity.QuestionEntity
import chromahub.rhythm.app.features.field.data.database.entity.ReportEntity
import chromahub.rhythm.app.features.field.data.database.entity.SourceEntity
import chromahub.rhythm.app.features.field.data.repository.FieldMindRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FieldMindViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FieldMindRepository(
        FieldMindDatabase.getInstance(application).fieldMindDao()
    )

    val observations: StateFlow<List<ObservationEntity>> = repository.observations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val questions: StateFlow<List<QuestionEntity>> = repository.questions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val hypotheses: StateFlow<List<HypothesisEntity>> = repository.hypotheses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val projects: StateFlow<List<ProjectEntity>> = repository.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sources: StateFlow<List<SourceEntity>> = repository.sources.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dataRecords: StateFlow<List<DataRecordEntity>> = repository.dataRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reports: StateFlow<List<ReportEntity>> = repository.reports.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addObservation(
        subject: String,
        category: String,
        facts: String,
        confidence: String,
        manualLocation: String,
        tags: String,
        evidence: String,
        context: String
    ) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        repository.addObservation(
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
                moodOrContext = context.trim()
            )
        )
    }

    fun addQuestion(question: String, category: String, sourceType: String, status: String, priority: String) = viewModelScope.launch {
        repository.addQuestion(
            QuestionEntity(
                questionText = question.trim(),
                category = category,
                sourceType = sourceType,
                status = status,
                priority = priority
            )
        )
    }

    fun addProject(title: String, topicType: String, objective: String, researchQuestion: String) = viewModelScope.launch {
        repository.addProject(
            ProjectEntity(
                title = title.trim(),
                topicType = topicType.trim().ifBlank { "General" },
                objective = objective.trim(),
                researchQuestion = researchQuestion.trim()
            )
        )
    }

    fun addSource(type: String, title: String, author: String, link: String, summary: String, taught: String, reliability: Int) = viewModelScope.launch {
        repository.addSource(
            SourceEntity(
                type = type,
                title = title.trim(),
                author = author.trim(),
                link = link.trim(),
                personalSummary = summary.trim(),
                whatThisSourceTaughtMe = taught.trim(),
                reliabilityScore = reliability
            )
        )
    }

    fun addHypothesis(questionId: Long?, prediction: String, evidenceNeeded: String, confidence: Int) = viewModelScope.launch {
        repository.addHypothesis(
            HypothesisEntity(
                linkedQuestionId = questionId,
                prediction = prediction.trim(),
                evidenceNeeded = evidenceNeeded.trim(),
                confidencePercent = confidence
            )
        )
    }

    fun addCounter(label: String, count: Int, notes: String) = viewModelScope.launch {
        repository.addDataRecord(
            DataRecordEntity(
                toolType = "Counter",
                label = label.trim(),
                value = count.toString(),
                unit = "count",
                notes = notes.trim()
            )
        )
    }

    fun addReport(title: String, question: String, methods: String, conclusion: String) = viewModelScope.launch {
        repository.addReport(
            ReportEntity(
                title = title.trim(),
                question = question.trim(),
                methods = methods.trim(),
                conclusion = conclusion.trim()
            )
        )
    }

    companion object {
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    }
}
