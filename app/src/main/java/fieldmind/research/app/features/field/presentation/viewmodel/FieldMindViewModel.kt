package fieldmind.research.app.features.field.presentation.viewmodel

import android.app.Application
import android.os.Parcelable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.parcelize.Parcelize
import fieldmind.research.app.features.field.data.database.FieldMindDatabase
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.repository.FieldMindRepository
import fieldmind.research.app.features.field.data.export.FieldMindExport
import fieldmind.research.app.features.field.data.export.FieldMindExportMediaPacker
import fieldmind.research.app.features.field.data.settings.FieldMindSettings
import fieldmind.research.app.features.field.data.weather.WeatherSnapshot
import fieldmind.research.app.features.field.data.analysis.DetectedPattern
import fieldmind.research.app.features.field.data.analysis.PatternDetectionEngine
import fieldmind.research.app.features.field.data.flashcard.SmartFlashcardGenerator
import fieldmind.research.app.features.field.data.question.QuestionGenerator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.jvm.JvmName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Parcelize
data class DraftEvidenceAttachment(
    val type: String,
    val uri: String,
    val caption: String = "",
    val localPath: String? = null,
    val mimeType: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

class FieldMindViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FieldMindRepository(FieldMindDatabase.getInstance(application).fieldMindDao())
    val fieldSettings: FieldMindSettings = FieldMindSettings.getInstance(application)
    val observations: StateFlow<List<ObservationEntity>> = repository.observations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notes: StateFlow<List<NoteEntity>> = repository.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val questions: StateFlow<List<QuestionEntity>> = repository.questions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val hypotheses: StateFlow<List<HypothesisEntity>> = repository.hypotheses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val projects: StateFlow<List<ProjectEntity>> = repository.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sources: StateFlow<List<SourceEntity>> = repository.sources.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dataRecords: StateFlow<List<DataRecordEntity>> = repository.dataRecords.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val reports: StateFlow<List<ReportEntity>> = repository.reports.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val flashcards: StateFlow<List<FlashcardEntity>> = repository.flashcards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tags: StateFlow<List<TagEntity>> = repository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val commonTags: StateFlow<List<TagStatistic>> = repository.commonTags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Pattern detection (reactive, computed from observations) ──
    val detectedPatterns: StateFlow<List<DetectedPattern>>

    /** True while the user has an active capture session timer running on the Observe screen. */
    @set:JvmName("setCaptureSessionActiveState")
    var captureSessionActive by mutableStateOf(false)
        private set
    fun setCaptureSessionActive(active: Boolean) { captureSessionActive = active }

    init {
        fieldSettings.initializeBackgroundWork()
        detectedPatterns = combine(
            observations, fieldSettings.autoPatternDetectionEnabled
        ) { obs, enabled ->
            if (enabled && obs.isNotEmpty()) PatternDetectionEngine.detectAll(obs) else emptyList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        startAutoGeneration()
    }

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
    fun linkHypothesisEvidence(hypothesisId: Long, observationId: Long) = viewModelScope.launch {
        repository.linkHypothesisEvidence(hypothesisId, observationId)
    }
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

    // ── Weather Catalog ──
    fun observeWeatherCatalog(lat: Double, lon: Double) = repository.observeWeatherCatalog(lat, lon)
    fun observeWeatherCatalogAll() = repository.observeWeatherCatalogAll()

    /**
     * Fetches weather for the given location and saves it to the offline weather catalog
     * so it can be reused in observation sessions without re-fetching.
     * Returns the fetched snapshot, or cached snapshot if already available.
     */
    suspend fun fetchAndSaveWeatherSnapshot(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean = false,
        placeName: String = ""
    ): WeatherSnapshot? {
        val snapshot = fetchWeatherSnapshot(latitude, longitude)
        if (snapshot != null) {
            repository.addWeatherCatalog(
                WeatherCatalogEntity(
                    latitude = latitude,
                    longitude = longitude,
                    temperature = snapshot.temperature,
                    weatherCode = snapshot.weatherCode,
                    weatherDescription = snapshot.weatherDescription,
                    humidity = snapshot.humidity,
                    windSpeed = snapshot.windSpeed,
                    windDirection = snapshot.windDirection,
                    cloudCover = snapshot.cloudCover,
                    pressure = snapshot.pressure,
                    sunrise = snapshot.sunrise,
                    sunset = snapshot.sunset,
                    placeName = placeName,
                    fetchedAt = snapshot.fetchedAt
                )
            )
        }
        return snapshot
    }

    /**
     * Saves an already-fetched WeatherSnapshot to the offline weather catalog.
     * This is used by observe and research session screens that have already fetched weather.
     */
    suspend fun saveWeatherSnapshot(snapshot: WeatherSnapshot, latitude: Double, longitude: Double, placeName: String = "") {
        repository.addWeatherCatalog(
            WeatherCatalogEntity(
                latitude = latitude,
                longitude = longitude,
                temperature = snapshot.temperature,
                weatherCode = snapshot.weatherCode,
                weatherDescription = snapshot.weatherDescription,
                humidity = snapshot.humidity,
                windSpeed = snapshot.windSpeed,
                windDirection = snapshot.windDirection,
                cloudCover = snapshot.cloudCover,
                pressure = snapshot.pressure,
                sunrise = snapshot.sunrise,
                sunset = snapshot.sunset,
                placeName = placeName,
                fetchedAt = snapshot.fetchedAt
            )
        )
    }

    /**
     * Fetches GPS location, then fetches weather and saves to catalog.
     * Returns (location, weather) pair.
     */
    suspend fun fetchLocationAndWeather(
        locationProvider: fieldmind.research.app.features.field.data.location.FieldLocationProvider,
        forceRefresh: Boolean = false
    ): Pair<fieldmind.research.app.features.field.data.location.CapturedLocation?, WeatherSnapshot?> {
        val captured = suspendCancellableCoroutine<fieldmind.research.app.features.field.data.location.CapturedLocation?> { cont ->
            locationProvider.requestCurrentLocation { loc ->
                if (cont.isActive) cont.resume(loc)
            }
        }
        if (captured == null) return null to null
        val weather = fetchAndSaveWeatherSnapshot(captured.latitude, captured.longitude, forceRefresh, captured.placeName ?: "")
        return captured to weather
    }

    fun addQuestion(question: String, category: String, sourceType: String, status: String, priority: String, observationId: Long? = null, sourceId: Long? = null, projectId: Long? = null, answer: String = "") = viewModelScope.launch {
        val trimmedAnswer = answer.trim()
        val id = repository.addQuestion(QuestionEntity(questionText = question.trim(), category = category, sourceType = sourceType, status = status, priority = priority, relatedProjectId = projectId, answer = trimmedAnswer))
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
        conclusion: String = "",
        projectType: String = "Observation",
        selectedMethods: String = "",
        connectionMap: String = ""
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
                futureQuestions = futureQuestions.trim(),
                projectType = projectType,
                selectedMethods = selectedMethods.trim(),
                connectionMap = connectionMap.trim()
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


    fun restoreArchiveJson(
        raw: String,
        mediaFiles: List<FieldMindExportMediaPacker.MediaEntry> = emptyList(),
        onResult: (Result<FieldMindExport.ArchivePreview>) -> Unit
    ) = viewModelScope.launch {
        runCatching {
            val bundle = FieldMindExport.parseArchiveJson(raw)

            // Phase 1: Import all entities, capturing old→new ID mapping for media relinking
            val oldToNewObsId = mutableMapOf<Long, Long>()
            bundle.observations.forEach { entity ->
                val newId = repository.addObservation(entity)
                oldToNewObsId[entity.id] = newId
            }

            val oldToNewNoteId = mutableMapOf<Long, Long>()
            bundle.notes.forEach { entity ->
                val newId = repository.addNote(entity)
                oldToNewNoteId[entity.id] = newId
            }

            val oldToNewProjId = mutableMapOf<Long, Long>()
            bundle.projects.forEach { entity ->
                val newId = repository.addProject(entity)
                oldToNewProjId[entity.id] = newId
            }

            val oldToNewSrcId = mutableMapOf<Long, Long>()
            bundle.sources.forEach { entity ->
                val newId = repository.addSource(entity)
                oldToNewSrcId[entity.id] = newId
            }

            bundle.questions.forEach { repository.addQuestion(it) }
            bundle.hypotheses.forEach { repository.addHypothesis(it) }
            bundle.dataRecords.forEach { repository.addDataRecord(it) }
            bundle.reports.forEach { repository.addReport(it) }
            bundle.flashcards.forEach { repository.addFlashcard(it) }

            // ── Import new entity types (species, weather, sessions, tasks) ──
            bundle.species.forEach { repository.addSpecies(it) }
            bundle.weatherCatalog.forEach { repository.addWeatherCatalog(it) }
            bundle.researchSessions.forEach { repository.addResearchSession(it) }
            bundle.tasks.forEach { repository.addTask(it) }

            // Phase 2: Relink extracted media files to the newly imported entities.
            // Copy temp files to a permanent app-local directory so they survive
            // cleanupExtractedPackage() which runs in the caller's finally block.
            if (mediaFiles.isNotEmpty()) {
                val appContext = getApplication<android.app.Application>()

                // ── Group media entries by entity type and old ID ──
                val obsMedia = mutableMapOf<Long, MutableList<FieldMindExportMediaPacker.MediaEntry>>()
                val noteMedia = mutableMapOf<Long, MutableList<FieldMindExportMediaPacker.MediaEntry>>()
                val projectMedia = mutableMapOf<Long, MutableList<FieldMindExportMediaPacker.MediaEntry>>()
                val sourceMedia = mutableMapOf<Long, MutableList<FieldMindExportMediaPacker.MediaEntry>>()

                mediaFiles.forEach { media ->
                    when (media.entityType) {
                        "observation" -> obsMedia.getOrPut(media.entityId) { mutableListOf() }.add(media)
                        "note" -> noteMedia.getOrPut(media.entityId) { mutableListOf() }.add(media)
                        "project" -> projectMedia.getOrPut(media.entityId) { mutableListOf() }.add(media)
                        "source" -> sourceMedia.getOrPut(media.entityId) { mutableListOf() }.add(media)
                    }
                }

                // ── Observations: create EvidenceAttachmentEntity for each media file ──
                obsMedia.forEach { (oldId, entries) ->
                    val newId = oldToNewObsId[oldId] ?: return@forEach
                    entries.forEach { media ->
                        val (permUri, permPath) = copyMediaToPermanentLocation(appContext, media, newId)
                        repository.addAttachment(
                            EvidenceAttachmentEntity(
                                observationId = newId,
                                type = media.mimeType,
                                uri = permUri,
                                localPath = permPath,
                                caption = media.caption
                            )
                        )
                    }
                }

                // ── Notes: reconstruct attachmentUris (type|caption|uri)\n... ──
                noteMedia.forEach { (oldId, entries) ->
                    val newId = oldToNewNoteId[oldId] ?: return@forEach
                    val newLines = entries.map { media ->
                        val (permUri, _) = copyMediaToPermanentLocation(appContext, media, newId)
                        val type = inferAttachmentType(media)
                        "$type|${media.caption}|$permUri"
                    }
                    if (newLines.isNotEmpty()) {
                        // Fetch the imported note and update its attachmentUris
                        val noteEntity = bundle.notes.firstOrNull { it.id == oldId }
                        if (noteEntity != null) {
                            repository.updateNote(
                                noteEntity.copy(
                                    attachmentUris = newLines.joinToString("\\n"),
                                    id = newId
                                )
                            )
                        }
                    }
                }

                // ── Projects: reconstruct attachmentUris (comma-separated URIs) ──
                projectMedia.forEach { (oldId, entries) ->
                    val newId = oldToNewProjId[oldId] ?: return@forEach
                    val newUris = entries.map { media ->
                        val (permUri, _) = copyMediaToPermanentLocation(appContext, media, newId)
                        permUri
                    }
                    if (newUris.isNotEmpty()) {
                        val projectEntity = bundle.projects.firstOrNull { it.id == oldId }
                        if (projectEntity != null) {
                            repository.updateProject(
                                projectEntity.copy(
                                    attachmentUris = newUris.joinToString(","),
                                    id = newId
                                )
                            )
                        }
                    }
                }

                // ── Sources: update fileUri (single URI per source) ──
                sourceMedia.forEach { (oldId, entries) ->
                    val newId = oldToNewSrcId[oldId] ?: return@forEach
                    entries.forEach { media ->
                        val (permUri, _) = copyMediaToPermanentLocation(appContext, media, newId)
                        val sourceEntity = bundle.sources.firstOrNull { it.id == oldId }
                        if (sourceEntity != null) {
                            // updateSourceFile internally copies the entity with the new fileUri
                            repository.updateSourceFile(sourceEntity, fileUri = permUri)
                        }
                    }
                }
            }

            bundle.preview
        }.also(onResult)
    }

    /**
     * Copy an extracted media file from its temp location to a permanent app-local
     * directory so it survives [FieldMindExportMediaPacker.cleanupExtractedPackage].
     * Returns (permanentUriString, localFilePath) pair. Sets both [uri] and [localPath]
     * on the restored [EvidenceAttachmentEntity] so the media is accessible via either path.
     */
    private suspend fun copyMediaToPermanentLocation(
        appContext: android.app.Application,
        media: FieldMindExportMediaPacker.MediaEntry,
        newEntityId: Long
    ): Pair<String, String> {
        val permDir = java.io.File(appContext.filesDir, "imported_attachments/${media.entityType}s/$newEntityId")
        permDir.mkdirs()
        val permFile = java.io.File(permDir, media.fileName)
        try {
            val tempUri = android.net.Uri.parse(media.uri)
            val srcFile = java.io.File(tempUri.path!!)
            if (srcFile.exists()) {
                srcFile.copyTo(permFile, overwrite = true)
                return Pair(android.net.Uri.fromFile(permFile).toString(), permFile.absolutePath)
            }
        } catch (_: Exception) { }
        // Fallback: use original URI (may become stale after cleanup, but better than nothing)
        return Pair(media.uri, android.net.Uri.parse(media.uri).path ?: "")
    }

    fun addHypothesis(questionId: Long?, prediction: String, evidenceNeeded: String, confidence: Int, reasoning: String = "", supportCriteria: String = "", weakeningCriteria: String = "", testMethod: String = "", resultStatus: String = "Unknown") = viewModelScope.launch {
        repository.addHypothesis(HypothesisEntity(linkedQuestionId = questionId, prediction = prediction.trim(), reasoning = reasoning.trim(), evidenceNeeded = evidenceNeeded.trim(), supportCriteria = supportCriteria.trim(), weakeningCriteria = weakeningCriteria.trim(), testMethod = testMethod.trim(), confidencePercent = confidence, resultStatus = resultStatus))
    }

    fun addDataRecord(toolType: String, label: String, value: String, unit: String = "", notes: String = "", location: String = "", projectId: Long? = null, observationId: Long? = null, datasetKind: String = "Manual", chartPreference: String = "Line", onResult: ((Boolean) -> Unit)? = null) = viewModelScope.launch {
        try {
            repository.addDataRecord(DataRecordEntity(toolType = toolType, label = label.trim(), value = value.trim(), unit = unit.trim(), notes = notes.trim(), location = location.trim(), projectId = projectId, observationId = observationId, datasetKind = datasetKind, chartPreference = chartPreference))
            onResult?.invoke(true)
        } catch (e: Exception) {
            android.util.Log.e("FieldMindVM", "Failed to save data record", e)
            onResult?.invoke(false)
        }
    }

    fun addCounter(label: String, count: Int, notes: String, onResult: ((Boolean) -> Unit)? = null) = addDataRecord("Counter", label, count.toString(), "count", notes, onResult = onResult)

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
    fun unlinkObservationFromSession(sessionId: Long, observationId: Long) = viewModelScope.launch {
        repository.unlinkSessionObservation(sessionId, observationId)
    }
    val researchSessions: StateFlow<List<ResearchSessionEntity>> = repository.researchSessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sessionObservationCrossRefs: StateFlow<List<SessionObservationCrossRef>> = repository.sessionObservationCrossRefs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val hypothesisEvidenceCrossRefs: StateFlow<List<HypothesisEvidenceCrossRef>> = repository.hypothesisEvidenceCrossRefs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Weather (multi-provider) ──
    private fun getWeatherProvider(): fieldmind.research.app.features.field.data.weather.WeatherProvider {
        val slug = fieldSettings.weatherProvider.value
        return fieldmind.research.app.features.field.data.weather.WeatherProviders.getProvider(slug)
    }
    var lastWeatherSnapshot: fieldmind.research.app.features.field.data.weather.WeatherSnapshot? = null
        private set
    private var lastWeatherFetchTime: Long = 0L
        private set
    private var isWeatherFetching = false
        private set

    /** Configure per-provider API keys and apply Open-Meteo custom config. */
    private fun buildWeatherApiKeys(): Map<String, String> = buildMap {
        val owm = fieldSettings.openWeatherMapApiKey.value.trim()
        if (owm.isNotBlank()) put("openweathermap", owm)
        val wapi = fieldSettings.weatherApiDotComApiKey.value.trim()
        if (wapi.isNotBlank()) put("weatherapi", wapi)
        val imd = fieldSettings.imdApiKey.value.trim()
        if (imd.isNotBlank()) put("imd-india", imd)

        // Apply Open-Meteo custom config to the singleton provider instance
        val omConfig = fieldSettings.openMeteoApiConfig.value.trim()
        if (omConfig.isNotBlank()) {
            (fieldmind.research.app.features.field.data.weather.WeatherProviders.fromSlug("open-meteo") as? fieldmind.research.app.features.field.data.weather.OpenMeteoProvider)?.configureWithJson(omConfig)
        }
    }

    fun fetchWeatherForLocation(latitude: Double, longitude: Double) = viewModelScope.launch {
        val legacyKey = fieldSettings.weatherApiKey.value.ifBlank { null }
        val perProviderKeys = buildWeatherApiKeys()
        lastWeatherSnapshot = fieldmind.research.app.features.field.data.weather.WeatherProviders.fetchMergedWeather(
            fieldSettings.weatherProviders.value, latitude, longitude, perProviderKeys, legacyKey
        )
    }

    suspend fun fetchWeatherSnapshot(latitude: Double, longitude: Double): WeatherSnapshot? {
        val legacyKey = fieldSettings.weatherApiKey.value.ifBlank { null }
        val perProviderKeys = buildWeatherApiKeys()
        return fieldmind.research.app.features.field.data.weather.WeatherProviders.fetchMergedWeather(
            fieldSettings.weatherProviders.value, latitude, longitude, perProviderKeys, legacyKey
        ).also { lastWeatherSnapshot = it }
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
                val legacyKey = fieldSettings.weatherApiKey.value.ifBlank { null }
                val perProviderKeys = buildWeatherApiKeys()
                fieldmind.research.app.features.field.data.weather.WeatherProviders.fetchMergedWeather(
                    fieldSettings.weatherProviders.value, loc.latitude, loc.longitude, perProviderKeys, legacyKey
                )
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
    val weatherCatalog: StateFlow<List<WeatherCatalogEntity>> = repository.weatherCatalog.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addFlashcard(front: String, back: String, type: String, sourceId: Long? = null, projectId: Long? = null, deckMode: String = "basic", dedupKey: String = "") = viewModelScope.launch {
        val key = dedupKey.ifBlank { "${front.lowercase().trim()}:${back.lowercase().trim()}".hashCode().toLong().let { if (it == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(it) }.toString(36) }
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

    // ── Auto-generation: flashcards & questions (runs in background, no button press) ──

    /**
     * Launches a background collector that watches observations, notes, and sources.
     * On data changes, debounces 5 seconds, then auto-generates:
     * 1. Flashcards from observations, notes, and sources (if autoFlashcardsEnabled)
     * 2. Questions from observations (if autoQuestionsEnabled)
     *
     * Deduplication ensures we never create the same flashcard/question twice.
     * A daily cap (AUTO_GEN_DAILY_CAP) prevents unlimited generation.
     *
     * IMPORTANT: Only input flows (observations, notes, sources, settings) are in the combine.
     * The output flows (flashcards, questions) are excluded to prevent an infinite regeneration
     * loop — generating new content would update those flows, triggering another round.
     * The current snapshot of flashcards/questions is captured via .value at trigger time for dedup.
     */
    private fun startAutoGeneration() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var todayDate = dateFormat.format(Date(System.currentTimeMillis()))
        var todayGenCount = 0

        viewModelScope.launch {
            combine(
                observations,
                notes,
                sources,
                fieldSettings.autoFlashcardsEnabled,
                fieldSettings.autoQuestionsEnabled
            ) { obs, nts, srcs, genFlash, genQuest ->
                Triple(obs, nts, srcs) to (genFlash to genQuest)
            }.debounce(5_000)
                .collect { (inputs, settings) ->
                    val (obs, nts, srcs) = inputs
                    val (autoFlash, autoQuest) = settings

                    // Reset daily counter if date changed
                    val nowDate = dateFormat.format(Date(System.currentTimeMillis()))
                    if (nowDate != todayDate) {
                        todayDate = nowDate
                        todayGenCount = 0
                    }

                    // Check cap before generating
                    if (todayGenCount >= AUTO_GEN_DAILY_CAP) return@collect

                    // ── Auto-flashcards (snapshot current flashcards for dedup) ──
                    if (autoFlash && obs.isNotEmpty() && todayGenCount < AUTO_GEN_DAILY_CAP) {
                        val existingCards = flashcards.value
                        runCatching {
                            val generated = SmartFlashcardGenerator.generateAll(
                                observations = obs,
                                notes = nts,
                                sources = srcs,
                                existing = existingCards
                            )
                            val capped = generated.take(AUTO_GEN_DAILY_CAP - todayGenCount)
                            if (capped.isNotEmpty()) {
                                android.util.Log.d("FieldMindVM", "Auto-generating ${capped.size} flashcards from data")
                                capped.forEach { card ->
                                    addFlashcard(
                                        front = card.front,
                                        back = card.back,
                                        type = card.type,
                                        sourceId = if (card.type == "observation") card.sourceId else null,
                                        projectId = card.projectId,
                                        deckMode = "sm2",
                                        dedupKey = card.dedupKey
                                    )
                                }
                                todayGenCount += capped.size
                            }
                        }.onFailure { e ->
                            android.util.Log.e("FieldMindVM", "Auto-flashcard generation failed", e)
                        }
                    }

                    // ── Auto-questions (snapshot current questions for dedup) ──
                    if (autoQuest && obs.isNotEmpty() && todayGenCount < AUTO_GEN_DAILY_CAP) {
                        val existingQuestions = questions.value
                        runCatching {
                            val generated = QuestionGenerator.generateAll(
                                observations = obs,
                                sources = srcs,
                                existing = existingQuestions
                            )
                            val capped = generated.take(AUTO_GEN_DAILY_CAP - todayGenCount)
                            if (capped.isNotEmpty()) {
                                android.util.Log.d("FieldMindVM", "Auto-generating ${capped.size} questions from observations")
                                capped.forEach { q ->
                                    addQuestion(
                                        question = q.questionText,
                                        category = q.category,
                                        sourceType = q.sourceType,
                                        status = "Open",
                                        priority = q.priority,
                                        observationId = q.observationId
                                    )
                                }
                                todayGenCount += capped.size
                            }
                        }.onFailure { e ->
                            android.util.Log.e("FieldMindVM", "Auto-question generation failed", e)
                        }
                    }

                    if (todayGenCount >= AUTO_GEN_DAILY_CAP) {
                        android.util.Log.d("FieldMindVM", "Daily auto-generation cap ($AUTO_GEN_DAILY_CAP) reached — pausing until tomorrow")
                    }
                }
        }
    }

    /**
     * Infer an attachment type label from a MediaEntry for note attachmentUris reconstruction.
     * Falls back to [mimeType] or infers from the file extension.
     */
    private fun inferAttachmentType(media: FieldMindExportMediaPacker.MediaEntry): String {
        if (media.mimeType.isNotBlank() && media.mimeType != "application/octet-stream") {
            return when {
                media.mimeType.startsWith("image/") -> "Photo"
                media.mimeType.startsWith("video/") -> "Video"
                media.mimeType.startsWith("audio/") -> "Audio"
                else -> media.mimeType
            }
        }
        // Infer from file extension
        val name = media.fileName.lowercase()
        return when {
            name.endsWith(".jpg") || name.endsWith(".jpeg") ||
            name.endsWith(".png") || name.endsWith(".webp") ||
            name.endsWith(".gif") || name.endsWith(".heic") ||
            name.endsWith(".bmp") -> "Photo"
            name.endsWith(".mp4") || name.endsWith(".mov") ||
            name.endsWith(".avi") || name.endsWith(".mkv") ||
            name.endsWith(".webm") || name.endsWith(".3gp") -> "Video"
            name.endsWith(".m4a") || name.endsWith(".mp3") ||
            name.endsWith(".wav") || name.endsWith(".ogg") ||
            name.endsWith(".flac") || name.endsWith(".aac") -> "Audio"
            name.endsWith(".pdf") -> "File"
            else -> "File"
        }
    }

    companion object {
        /** Max auto-generated items (flashcards + questions combined) per day. */
        private const val AUTO_GEN_DAILY_CAP = 20
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    }
}
