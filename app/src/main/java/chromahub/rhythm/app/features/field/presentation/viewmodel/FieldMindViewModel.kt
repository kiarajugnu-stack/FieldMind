package fieldmind.research.app.features.field.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fieldmind.research.app.features.field.data.database.FieldMindDatabase
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.repository.FieldMindRepository
import fieldmind.research.app.features.field.data.export.FieldMindExport
import fieldmind.research.app.features.field.data.settings.FieldMindSettings
import fieldmind.research.app.features.field.data.weather.WeatherSnapshot
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
    init { fieldSettings.initializeBackgroundWork() }

    val observations: StateFlow<List<ObservationEntity>> = repository.observations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notes: StateFlow<List<NoteEntity>> = repository.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
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
        weather: WeatherSnapshot? = null,
        structuredDetailsJson: String = "",
        startedAt: Long? = null,
        endedAt: Long? = null,
        durationMs: Long? = null,
        changeObservedAt: Long? = null,
        changeDurationMs: Long? = null,
        timeNote: String = "",
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
                longitude = longitude,
                weatherTemperature = weather?.temperature,
                weatherCondition = weather?.weatherDescription.orEmpty(),
                weatherHumidity = weather?.humidity,
                weatherWindSpeed = weather?.windSpeed,
                weatherCloudCover = weather?.cloudCover,
                weatherPressure = weather?.pressure,
                weatherSnapshotAt = weather?.fetchedAt,
                structuredDetailsJson = structuredDetailsJson.trim(),
                startedAt = startedAt,
                endedAt = endedAt,
                durationMs = durationMs,
                changeObservedAt = changeObservedAt,
                changeDurationMs = changeDurationMs,
                timeNote = timeNote.trim()
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

    fun addAttachmentToObservation(observationId: Long, attachment: DraftEvidenceAttachment) = viewModelScope.launch {
        repository.addAttachment(
            EvidenceAttachmentEntity(
                observationId = observationId,
                type = attachment.type,
                uri = attachment.uri,
                localPath = attachment.localPath,
                caption = attachment.caption
            )
        )
    }

    fun addNote(title: String, body: String, category: String, tags: String, projectId: Long? = null, sourceId: Long? = null, attachments: List<DraftEvidenceAttachment> = emptyList(), onSaved: ((Long) -> Unit)? = null) = viewModelScope.launch {
        val id = repository.addNote(
            NoteEntity(
                title = title.trim(),
                body = body.trim(),
                category = category,
                tags = tags.trim(),
                projectId = projectId,
                sourceId = sourceId,
                attachmentUris = attachments.joinToString("\n") { listOf(it.type, it.caption, it.localPath ?: it.uri).joinToString("|") }
            )
        )
        onSaved?.invoke(id)
    }

    fun updateNoteEntity(entity: NoteEntity) = viewModelScope.launch { repository.updateNote(entity) }

    fun archiveObservation(id: Long) = viewModelScope.launch { repository.archiveObservation(id) }

    fun updateQuestionEntity(entity: QuestionEntity) = viewModelScope.launch { repository.updateQuestion(entity) }
    fun setQuestionAnswer(entity: QuestionEntity, answer: String) = viewModelScope.launch {
        val trimmed = answer.trim()
        repository.updateQuestion(
            entity.copy(
                answer = trimmed,
                answeredAt = if (trimmed.isBlank()) null else System.currentTimeMillis(),
                status = if (trimmed.isBlank()) entity.status else "Answered"
            )
        )
    }
    fun updateHypothesisEntity(entity: HypothesisEntity) = viewModelScope.launch { repository.updateHypothesis(entity) }
    fun updateProjectEntity(entity: ProjectEntity) = viewModelScope.launch { repository.updateProject(entity) }
    fun updateSourceEntity(entity: SourceEntity) = viewModelScope.launch { repository.updateSource(entity) }
    fun updateDataRecordEntity(entity: DataRecordEntity) = viewModelScope.launch { repository.updateDataRecord(entity) }
    fun updateReportEntity(entity: ReportEntity) = viewModelScope.launch { repository.updateReport(entity) }
    fun updateFlashcardEntity(entity: FlashcardEntity) = viewModelScope.launch { repository.updateFlashcard(entity) }

    fun deleteObservation(id: Long) = viewModelScope.launch { repository.deleteObservation(id) }
    fun deleteNote(id: Long) = viewModelScope.launch { repository.deleteNote(id) }
    fun deleteQuestion(id: Long) = viewModelScope.launch { repository.deleteQuestion(id) }
    fun deleteHypothesis(id: Long) = viewModelScope.launch { repository.deleteHypothesis(id) }
    fun deleteProject(id: Long) = viewModelScope.launch { repository.deleteProject(id) }
    fun deleteSource(id: Long) = viewModelScope.launch { repository.deleteSource(id) }
    fun deleteDataRecord(id: Long) = viewModelScope.launch { repository.deleteDataRecord(id) }
    fun deleteReport(id: Long) = viewModelScope.launch { repository.deleteReport(id) }
    fun deleteFlashcard(id: Long) = viewModelScope.launch { repository.deleteFlashcard(id) }

    fun observeQuestion(id: Long) = repository.observeQuestion(id)
    fun observeHypothesis(id: Long) = repository.observeHypothesis(id)
    fun observeProject(id: Long) = repository.observeProject(id)
    fun observeSource(id: Long) = repository.observeSource(id)
    fun observeReport(id: Long) = repository.observeReport(id)
    fun observeDataRecord(id: Long) = repository.observeDataRecord(id)
    fun observeFlashcard(id: Long) = repository.observeFlashcard(id)
    fun observeObservation(id: Long) = repository.observeObservation(id)
    fun observeNote(id: Long) = repository.observeNote(id)

    fun attachmentsForObservation(id: Long) = repository.observeAttachmentsForObservation(id)

    fun addQuestion(question: String, category: String, sourceType: String, status: String, priority: String, observationId: Long? = null, sourceId: Long? = null, projectId: Long? = null) = viewModelScope.launch {
        val id = repository.addQuestion(QuestionEntity(questionText = question.trim(), category = category, sourceType = sourceType, status = status, priority = priority, relatedProjectId = projectId))
        observationId?.let { repository.linkQuestionObservation(id, it) }
        sourceId?.let { repository.linkQuestionSource(id, it) }
    }

    fun addProject(
        title: String,
        topicType: String,
        objective: String,
        researchQuestion: String,
        methods: String = "",
        futureQuestions: String = "",
        backgroundNotes: String = "",
        hypothesisSummary: String = "",
        dataSummary: String = "",
        analysis: String = "",
        conclusion: String = ""
    ) = viewModelScope.launch {
        repository.addProject(
            ProjectEntity(
                title = title.trim(),
                topicType = topicType.trim().ifBlank { "General" },
                objective = objective.trim(),
                researchQuestion = researchQuestion.trim(),
                backgroundNotes = backgroundNotes.trim(),
                methods = methods.trim(),
                hypothesisSummary = hypothesisSummary.trim(),
                dataSummary = dataSummary.trim(),
                analysis = analysis.trim(),
                conclusion = conclusion.trim(),
                futureQuestions = futureQuestions.trim()
            )
        )
    }

    fun addSource(
        type: String,
        title: String,
        author: String,
        link: String,
        summary: String,
        taught: String,
        reliability: Int,
        keyFindings: String = "",
        questionsGenerated: String = "",
        paperNotes: String = "",
        projectId: Long? = null,
        dateOrYear: String = "",
        doiOrIsbn: String = "",
        publisherOrJournal: String = "",
        accessDate: String = "",
        fileUri: String = "",
        citationStyleNote: String = "",
        importance: String = "Normal",
        readingStatus: String = "In progress"
    ) = viewModelScope.launch {
        repository.addSource(
            SourceEntity(
                type = type,
                title = title.trim(),
                author = author.trim(),
                dateOrYear = dateOrYear.trim(),
                link = link.trim(),
                doiOrIsbn = doiOrIsbn.trim(),
                publisherOrJournal = publisherOrJournal.trim(),
                accessDate = accessDate.trim(),
                fileUri = fileUri.trim(),
                citationStyleNote = citationStyleNote.trim(),
                importance = importance,
                personalSummary = summary.trim(),
                keyFindings = keyFindings.trim(),
                whatThisSourceTaughtMe = taught.trim(),
                questionsGenerated = questionsGenerated.trim(),
                reliabilityScore = reliability,
                readingStatus = readingStatus,
                paperNotes = paperNotes.trim(),
                relatedProjectId = projectId
            )
        )
    }

    fun toggleSourceImportant(source: SourceEntity) = viewModelScope.launch {
        repository.toggleSourceImportant(source)
    }

    fun markSourceRead(source: SourceEntity) = viewModelScope.launch {
        repository.markSourceRead(source)
    }

    fun linkSourceToProject(source: SourceEntity, projectId: Long?) = viewModelScope.launch {
        repository.linkSourceToProject(source, projectId)
    }

    fun updateSourceFile(source: SourceEntity, fileUri: String) = viewModelScope.launch {
        repository.updateSourceFile(source, fileUri)
    }

    fun buildSourceCitation(source: SourceEntity): String = FieldMindExport.sourceCitation(source)


    fun restoreArchiveJson(raw: String, onResult: (Result<FieldMindExport.ArchivePreview>) -> Unit) = viewModelScope.launch {
        runCatching {
            val bundle = FieldMindExport.parseArchiveJson(raw)
            bundle.projects.forEach { repository.addProject(it) }
            bundle.sources.forEach { repository.addSource(it) }
            bundle.observations.forEach { repository.addObservation(it) }
            bundle.notes.forEach { repository.addNote(it) }
            bundle.questions.forEach { repository.addQuestion(it) }
            bundle.hypotheses.forEach { repository.addHypothesis(it) }
            bundle.dataRecords.forEach { repository.addDataRecord(it) }
            bundle.reports.forEach { repository.addReport(it) }
            bundle.flashcards.forEach { repository.addFlashcard(it) }
            bundle.preview
        }.also(onResult)
    }

    fun addHypothesis(questionId: Long?, prediction: String, evidenceNeeded: String, confidence: Int, reasoning: String = "", supportCriteria: String = "", weakeningCriteria: String = "", testMethod: String = "") = viewModelScope.launch {
        repository.addHypothesis(HypothesisEntity(linkedQuestionId = questionId, prediction = prediction.trim(), reasoning = reasoning.trim(), evidenceNeeded = evidenceNeeded.trim(), supportCriteria = supportCriteria.trim(), weakeningCriteria = weakeningCriteria.trim(), testMethod = testMethod.trim(), confidencePercent = confidence))
    }

    fun addDataRecord(toolType: String, label: String, value: String, unit: String = "", notes: String = "", location: String = "", projectId: Long? = null, observationId: Long? = null, datasetKind: String = "Manual", chartPreference: String = "Line") = viewModelScope.launch {
        repository.addDataRecord(DataRecordEntity(toolType = toolType, label = label.trim(), value = value.trim(), unit = unit.trim(), notes = notes.trim(), location = location.trim(), projectId = projectId, observationId = observationId, datasetKind = datasetKind, chartPreference = chartPreference))
    }

    fun addCounter(label: String, count: Int, notes: String) = addDataRecord("Counter", label, count.toString(), "count", notes)

    fun addReport(type: String, title: String, background: String, question: String, methods: String, observations: String, results: String, interpretation: String, conclusion: String, limitations: String, nextSteps: String, projectId: Long? = null) = viewModelScope.launch {
        val markdown = buildMarkdown(type, title, background, question, methods, observations, results, interpretation, conclusion, limitations, nextSteps)
        repository.addReport(ReportEntity(type = type, title = title.trim(), background = background.trim(), question = question.trim(), methods = methods.trim(), observations = observations.trim(), results = results.trim(), interpretation = interpretation.trim(), conclusion = conclusion.trim(), limitations = limitations.trim(), nextSteps = nextSteps.trim(), markdownDraft = markdown, projectId = projectId))
    }

    fun addReport(title: String, question: String, methods: String, conclusion: String) = addReport("Field Report", title, "", question, methods, "", "", "", conclusion, "", "")

    // ── Research Session methods ──
    fun addResearchSession(name: String, projectId: Long? = null, onSaved: ((Long) -> Unit)? = null) = viewModelScope.launch {
        val id = repository.addResearchSession(ResearchSessionEntity(name = name, projectId = projectId))
        onSaved?.invoke(id)
    }
    fun endResearchSession(sessionId: Long, observationCount: Int, durationMs: Long) = viewModelScope.launch {
        repository.endResearchSession(sessionId, observationCount, durationMs)
    }
    fun linkObservationToSession(sessionId: Long, observationId: Long) = viewModelScope.launch {
        repository.linkSessionObservation(sessionId, observationId)
    }
    val researchSessions: StateFlow<List<ResearchSessionEntity>> = repository.researchSessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Weather (Open-Meteo) ──
    private val weatherService = fieldmind.research.app.features.field.data.weather.WeatherApiService()
    var lastWeatherSnapshot: fieldmind.research.app.features.field.data.weather.WeatherSnapshot? = null
        private set
    private var lastWeatherFetchTime: Long = 0L
        private set
    private var isWeatherFetching = false
        private set

    fun fetchWeatherForLocation(latitude: Double, longitude: Double) = viewModelScope.launch {
        lastWeatherSnapshot = weatherService.fetchWeather(latitude, longitude)
    }

    suspend fun fetchWeatherSnapshot(latitude: Double, longitude: Double): WeatherSnapshot? {
        return weatherService.fetchWeather(latitude, longitude).also { lastWeatherSnapshot = it }
    }

    /**
     * Returns the configured weather refresh interval in milliseconds.
     */
    private fun getWeatherRefreshIntervalMs(): Long {
        val setting = fieldSettings.weatherRefreshInterval.value
        return when (setting) {
            "15 min" -> 15 * 60 * 1000L
            "60 min" -> 60 * 60 * 1000L
            else -> 30 * 60 * 1000L // default 30 min
        }
    }

    /**
     * Fetches current weather using the device's last known location.
     * Respects the configured refresh interval — returns the cached snapshot
     * without making a network request if the interval hasn't elapsed.
     * Set [forceRefresh] = true to bypass the cooldown (e.g. on manual refresh tap).
     */
    suspend fun refreshWeatherFromLocation(forceRefresh: Boolean = false): WeatherSnapshot? {
        val intervalMs = getWeatherRefreshIntervalMs()
        val elapsed = System.currentTimeMillis() - lastWeatherFetchTime

        // Return cached snapshot if within cooldown period (unless forced)
        if (!forceRefresh && elapsed < intervalMs && lastWeatherSnapshot != null) {
            return lastWeatherSnapshot
        }

        // Avoid concurrent fetches
        if (isWeatherFetching && !forceRefresh) {
            return lastWeatherSnapshot
        }

        val provider = runCatching {
            fieldmind.research.app.features.field.data.location.FieldLocationProvider(getApplication())
        }.getOrNull() ?: return null

        if (!provider.hasAnyLocationPermission()) return null

        isWeatherFetching = true
        val result = try {
            provider.lastKnownLocation()?.let { loc ->
                weatherService.fetchWeather(loc.latitude, loc.longitude)
            }
        } finally {
            isWeatherFetching = false
        }

        if (result != null) {
            lastWeatherSnapshot = result
            lastWeatherFetchTime = System.currentTimeMillis()
        }
        return result
    }

    // ── Species Registry methods ──
    fun addSpecies(
        commonName: String,
        scientificName: String = "",
        kingdom: String = "",
        phylum: String = "",
        classs: String = "",
        order: String = "",
        family: String = "",
        genus: String = "",
        species: String = "",
        conservationStatus: String = "Not Evaluated",
        targetCount: Int = 0,
        autoCountTracking: Boolean = false,
        projectId: Long? = null,
        notes: String = ""
    ) = viewModelScope.launch {
        repository.addSpecies(
            SpeciesEntity(
                commonName = commonName.trim(),
                scientificName = scientificName.trim(),
                kingdom = kingdom.trim(),
                phylum = phylum.trim(),
                classs = classs.trim(),
                order = order.trim(),
                family = family.trim(),
                genus = genus.trim(),
                species = species.trim(),
                conservationStatus = conservationStatus,
                targetCount = targetCount,
                autoCountTracking = autoCountTracking,
                projectId = projectId,
                notes = notes.trim()
            )
        )
    }

    val speciesRegistry: StateFlow<List<SpeciesEntity>> = repository.species.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun observeSpeciesForProject(projectId: Long) = repository.observeSpeciesForProject(projectId)

    // ── Task methods ──
    fun addTask(
        title: String,
        description: String = "",
        taskType: String = "Field Survey",
        priority: String = "Medium",
        dueDate: String = "",
        assignedTo: String = "",
        status: String = "Pending",
        linkedQuestionId: Long? = null,
        linkedObservationId: Long? = null,
        linkedSpeciesId: Long? = null,
        projectId: Long? = null,
        parentTaskId: Long? = null
    ) = viewModelScope.launch {
        repository.addTask(
            TaskEntity(
                title = title.trim(),
                description = description.trim(),
                taskType = taskType,
                priority = priority,
                dueDate = dueDate.trim(),
                assignedTo = assignedTo.trim(),
                status = status,
                linkedQuestionId = linkedQuestionId,
                linkedObservationId = linkedObservationId,
                linkedSpeciesId = linkedSpeciesId,
                projectId = projectId,
                parentTaskId = parentTaskId
            )
        )
    }

    fun updateTaskEntity(entity: TaskEntity) = viewModelScope.launch { repository.updateTask(entity) }
    fun deleteTask(id: Long) = viewModelScope.launch { repository.deleteTask(id) }
    val tasks: StateFlow<List<TaskEntity>> = repository.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun observeTasksForProject(projectId: Long) = repository.observeTasksForProject(projectId)

    fun addFlashcard(front: String, back: String, type: String, sourceId: Long? = null, projectId: Long? = null, deckMode: String = "basic") = viewModelScope.launch {
        repository.addFlashcard(FlashcardEntity(front = front.trim(), back = back.trim(), type = type, sourceId = sourceId, projectId = projectId, deckMode = deckMode))
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
