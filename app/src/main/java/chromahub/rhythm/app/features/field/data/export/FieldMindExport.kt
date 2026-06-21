package fieldmind.research.app.features.field.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import fieldmind.research.app.features.field.data.database.entity.*

object FieldMindExport {

    data class ArchivePreview(
        val observations: Int = 0,
        val notes: Int = 0,
        val questions: Int = 0,
        val hypotheses: Int = 0,
        val projects: Int = 0,
        val sources: Int = 0,
        val dataRecords: Int = 0,
        val reports: Int = 0,
        val flashcards: Int = 0
    ) {
        val total: Int get() = observations + notes + questions + hypotheses + projects + sources + dataRecords + reports + flashcards
        fun summary(): String = listOf(
            "$observations observations",
            "$notes notes",
            "$questions questions",
            "$hypotheses hypotheses",
            "$projects projects",
            "$sources sources",
            "$dataRecords data records",
            "$reports reports",
            "$flashcards flashcards"
        ).joinToString(" • ")
    }

    data class ArchiveImportBundle(
        val preview: ArchivePreview,
        val observations: List<ObservationEntity>,
        val notes: List<NoteEntity>,
        val questions: List<QuestionEntity>,
        val hypotheses: List<HypothesisEntity>,
        val projects: List<ProjectEntity>,
        val sources: List<SourceEntity>,
        val dataRecords: List<DataRecordEntity>,
        val reports: List<ReportEntity>,
        val flashcards: List<FlashcardEntity>
    )

    /**
     * Bundle of pre-loaded base64-encoded media for inline embedding in HTML/PDF exports.
     * Media is collected from EvidenceAttachmentEntity (observations), NoteEntity.attachmentUris,
     * ProjectEntity.attachmentUris, and SourceEntity.fileUri.
     */
    data class ExportMediaBundle(
        val observationImages: Map<Long, List<Pair<String, String>>> = emptyMap(),
        val noteImages: Map<Long, List<Pair<String, String>>> = emptyMap(),
        val projectImages: Map<Long, List<Pair<String, String>>> = emptyMap(),
        val sourceImages: Map<Long, List<Pair<String, String>>> = emptyMap()
    ) {
        val hasMedia: Boolean get() = observationImages.isNotEmpty() || noteImages.isNotEmpty() ||
            projectImages.isNotEmpty() || sourceImages.isNotEmpty()

        companion object {
            /**
             * Collect media from all entity types and load them into base64 data URIs.
             * @param context Android context for ContentResolver
             * @param observations list of observations (attachments loaded separately)
             * @param notes list of notes (attachmentUris is newline-delimited "type|caption|uri")
             * @param projects list of projects (attachmentUris is comma-separated URIs)
             * @param sources list of sources (fileUri is a single URI)
             * @param observationAttachments map of observationId -> list of EvidenceAttachmentEntity
             */
            suspend fun collect(
                context: android.content.Context,
                observations: List<ObservationEntity>,
                notes: List<NoteEntity>,
                projects: List<ProjectEntity>,
                sources: List<SourceEntity>,
                observationAttachments: Map<Long, List<EvidenceAttachmentEntity>> = emptyMap()
            ): ExportMediaBundle {
                val obsImages = mutableMapOf<Long, MutableList<Pair<String, String>>>()
                val noteImages = mutableMapOf<Long, MutableList<Pair<String, String>>>()
                val projImages = mutableMapOf<Long, MutableList<Pair<String, String>>>()
                val srcImages = mutableMapOf<Long, MutableList<Pair<String, String>>>()

                val resolver = context.contentResolver

                // Collect from EvidenceAttachmentEntity (observations)
                observationAttachments.forEach { (obsId, attachments) ->
                    attachments.forEach { att ->
                        val uri = att.localPath?.let { android.net.Uri.fromFile(java.io.File(it)) }
                            ?: android.net.Uri.parse(att.uri)
                        val dataUri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            readContentUriToBase64(resolver, uri)
                        }
                        if (dataUri != null) {
                            obsImages.getOrPut(obsId) { mutableListOf() }
                                .add(att.caption.ifBlank { "Attachment" } to dataUri)
                        }
                    }
                }

                // Collect from notes ("type|caption|uri\n...")
                notes.forEach { note ->
                    if (note.attachmentUris.isNotBlank()) {
                        val list = mutableListOf<Pair<String, String>>()
                        note.attachmentUris.split("\n").filter { it.isNotBlank() }.forEach { line ->
                            val parts = line.split("|", limit = 3)
                            if (parts.size >= 3) {
                                val uri = android.net.Uri.parse(parts[2])
                                val dataUri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    readContentUriToBase64(resolver, uri)
                                }
                                if (dataUri != null) {
                                    list.add((parts[1].ifBlank { "Attachment" }) to dataUri)
                                }
                            }
                        }
                        if (list.isNotEmpty()) noteImages[note.id] = list
                    }
                }

                // Collect from projects (comma-separated URIs)
                projects.forEach { project ->
                    if (project.attachmentUris.isNotBlank()) {
                        val list = mutableListOf<Pair<String, String>>()
                        project.attachmentUris.split(",").filter { it.isNotBlank() }.forEach { uriStr ->
                            val uri = android.net.Uri.parse(uriStr.trim())
                            val dataUri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                readContentUriToBase64(resolver, uri)
                            }
                            if (dataUri != null) {
                                list.add("Attachment" to dataUri)
                            }
                        }
                        if (list.isNotEmpty()) projImages[project.id] = list
                    }
                }

                // Collect from sources (fileUri)
                sources.forEach { source ->
                    if (source.fileUri.isNotBlank()) {
                        val uri = android.net.Uri.parse(source.fileUri)
                        val dataUri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            readContentUriToBase64(resolver, uri)
                        }
                        if (dataUri != null) {
                            srcImages[source.id] = mutableListOf("Source file" to dataUri)
                        }
                    }
                }

                return ExportMediaBundle(
                    observationImages = obsImages,
                    noteImages = noteImages,
                    projectImages = projImages,
                    sourceImages = srcImages
                )
            }

            /**
             * Read a content:// or file:// URI and return a base64 data URI string.
             */
            fun readContentUriToBase64(resolver: android.content.ContentResolver, uri: android.net.Uri): String? {
                return try {
                    val inputStream = resolver.openInputStream(uri) ?: return null
                    val bytes = inputStream.use { it.readBytes() }
                    if (bytes.isEmpty()) return null
                    val mimeType = resolver.getType(uri) ?: "image/jpeg"
                    "data:$mimeType;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                } catch (e: Exception) { null }
            }
        }
    }

    fun previewArchiveJson(raw: String): ArchivePreview = parseArchiveJson(raw).preview

    fun parseArchiveJson(raw: String): ArchiveImportBundle {
        val root = JSONObject(raw)
        require(root.optString("format").startsWith("fieldmind-archive")) { "This is not a FieldMind archive." }
        val observations = root.optJSONArray("observations").toObjects { o ->
            ObservationEntity(
                subject = o.optString("subject", "Imported observation"),
                category = o.optString("category", "Other"),
                factsOnlyNotes = o.optString("factsOnlyNotes"),
                timestamp = System.currentTimeMillis(),
                date = o.optString("date"),
                time = o.optString("time"),
                confidenceLevel = o.optString("confidenceLevel", "Needs Verification"),
                evidenceSummary = o.optString("evidenceSummary"),
                tags = o.optString("tags"),
                projectId = o.optNullableLong("projectId")
            )
        }
        val notes = root.optJSONArray("notes").toObjects { o ->
            NoteEntity(
                title = o.optString("title", "Imported note"),
                body = o.optString("body"),
                category = o.optString("category", "Field Note"),
                tags = o.optString("tags"),
                projectId = o.optNullableLong("projectId"),
                sourceId = o.optNullableLong("sourceId"),
                attachmentUris = o.optString("attachmentUris")
            )
        }
        val questions = root.optJSONArray("questions").toObjects { o ->
            QuestionEntity(
                questionText = o.optString("questionText", "Imported question"),
                category = o.optString("category", "General"),
                sourceType = o.optString("sourceType", "Imported"),
                status = o.optString("status", "New"),
                priority = o.optString("priority", "Medium")
            )
        }
        val hypotheses = root.optJSONArray("hypotheses").toObjects { o ->
            HypothesisEntity(
                prediction = o.optString("prediction", "Imported hypothesis"),
                resultStatus = o.optString("resultStatus", "Unknown"),
                confidencePercent = o.optInt("confidencePercent", 50)
            )
        }
        val projects = root.optJSONArray("projects").toObjects { o ->
            ProjectEntity(
                title = o.optString("title", "Imported project"),
                topicType = o.optString("topicType", "General"),
                objective = o.optString("objective"),
                researchQuestion = o.optString("researchQuestion"),
                status = o.optString("status", "Active")
            )
        }
        val sources = root.optJSONArray("sources").toObjects { o ->
            SourceEntity(
                type = o.optString("type", "Website"),
                title = o.optString("title", "Imported source"),
                author = o.optString("author"),
                dateOrYear = o.optString("dateOrYear"),
                link = o.optString("link"),
                doiOrIsbn = o.optString("doiOrIsbn"),
                publisherOrJournal = o.optString("publisherOrJournal"),
                accessDate = o.optString("accessDate"),
                fileUri = o.optString("fileUri"),
                citationStyleNote = o.optString("citationStyleNote"),
                importance = o.optString("importance", "Normal"),
                personalSummary = o.optString("personalSummary"),
                keyFindings = o.optString("keyFindings"),
                whatThisSourceTaughtMe = o.optString("whatThisSourceTaughtMe"),
                questionsGenerated = o.optString("questionsGenerated"),
                reliabilityScore = o.optInt("reliabilityScore", 3),
                readingStatus = o.optString("readingStatus", "In progress"),
                paperNotes = o.optString("paperNotes"),
                relatedProjectId = o.optNullableLong("relatedProjectId")
            )
        }
        val dataRecords = root.optJSONArray("dataRecords").toObjects { o ->
            DataRecordEntity(
                toolType = o.optString("toolType", "Imported"),
                label = o.optString("label", "Imported data"),
                value = o.optString("value"),
                unit = o.optString("unit")
            )
        }
        val reports = root.optJSONArray("reports").toObjects { o ->
            ReportEntity(
                type = o.optString("type", "Imported"),
                title = o.optString("title", "Imported report"),
                status = o.optString("status", "Draft"),
                markdownDraft = o.optString("markdownDraft")
            )
        }
        val flashcards = root.optJSONArray("flashcards").toObjects { o ->
            FlashcardEntity(
                front = o.optString("front", "Imported card"),
                back = o.optString("back"),
                type = o.optString("type", "imported")
            )
        }
        val preview = ArchivePreview(observations.size, notes.size, questions.size, hypotheses.size, projects.size, sources.size, dataRecords.size, reports.size, flashcards.size)
        require(preview.total > 0) { "The archive did not contain importable FieldMind records." }
        return ArchiveImportBundle(preview, observations, notes, questions, hypotheses, projects, sources, dataRecords, reports, flashcards)
    }


    fun singleObservationMarkdown(observation: ObservationEntity): String = buildString {
        appendLine("# ${observation.subject}")
        appendLine()
        appendLine("Category: ${observation.category}")
        appendLine("Captured: ${observation.date} ${observation.time}")
        if (observation.durationMs != null) appendLine("Duration: ${observation.durationMs / 1000}s")
        if (observation.manualLocation.isNotBlank()) appendLine("Location: ${observation.manualLocation}")
        if (observation.weatherCondition.isNotBlank() || observation.weatherTemperature != null) appendLine("Weather: ${observation.weatherTemperature ?: ""} ${observation.weatherCondition}")
        appendLine("\n## Facts\n${observation.factsOnlyNotes}")
        if (observation.structuredDetailsJson.isNotBlank()) appendLine("\n## Structured details\n`${observation.structuredDetailsJson}`")
        if (observation.evidenceSummary.isNotBlank()) appendLine("\n## Evidence\n${observation.evidenceSummary}")
    }

    fun singleProjectMarkdown(project: ProjectEntity): String = buildString {
        appendLine("# ${project.title}")
        appendLine("Status: ${project.status}")
        appendLine("\n## Research question\n${project.researchQuestion}")
        appendLine("\n## Objective\n${project.objective}")
        appendLine("\n## Methods\n${project.methods}")
        appendLine("\n## Evidence connections\n${project.connectionMap}")
        appendLine("\n## Findings\n${project.conclusion}")
    }

    fun singleReportMarkdown(report: ReportEntity): String = buildMarkdownReport(report)

    fun singleDataRecordMarkdown(record: DataRecordEntity): String = """# ${record.label}

Tool: ${record.toolType}
Dataset: ${record.datasetKind}
Value: ${record.value} ${record.unit}
Location: ${record.location}
Notes: ${record.notes}
""".trimIndent()

    fun singleDataRecordCsv(record: DataRecordEntity): String = "id,toolType,label,value,unit,timestamp,location,notes\n${record.id},${csv(record.toolType)},${csv(record.label)},${csv(record.value)},${csv(record.unit)},${record.timestamp},${csv(record.location)},${csv(record.notes)}"

    fun pdfReadyHtmlForMarkdown(title: String, markdown: String): String = """<!doctype html><html><head><meta charset="utf-8"><title>${html(title)}</title><style>body{font-family:sans-serif;line-height:1.55;padding:32px;max-width:820px;margin:auto}pre{white-space:pre-wrap;background:#f6f8f4;padding:16px;border-radius:16px}</style></head><body><pre>${html(markdown)}</pre></body></html>"""

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
    fun buildMarkdownReport(report: ReportEntity): String = report.markdownDraft.ifBlank {
        """# ${report.title}

Type: ${report.type}
Status: ${report.status}

## Background
${report.background}

## Question
${report.question}

## Methods
${report.methods}

## Observations
${report.observations}

## Results
${report.results}

## Interpretation
${report.interpretation}

## Conclusion
${report.conclusion}

## Limitations
${report.limitations}

## Next steps
${report.nextSteps}
""".trimIndent()
    }

    fun sourceCitation(source: SourceEntity): String = buildString {
        append(source.author.ifBlank { "Unknown author" })
        if (source.dateOrYear.isNotBlank()) append(" (${source.dateOrYear}).") else append(".")
        append(" ${source.title}.")
        if (source.publisherOrJournal.isNotBlank()) append(" ${source.publisherOrJournal}.")
        if (source.doiOrIsbn.isNotBlank()) append(" ${source.doiOrIsbn}.")
        if (source.link.isNotBlank()) append(" ${source.link}")
        if (source.accessDate.isNotBlank()) append(" Accessed ${source.accessDate}.")
        if (source.citationStyleNote.isNotBlank()) append(" ${source.citationStyleNote}")
    }.trim()

    fun observationsCsv(items: List<ObservationEntity>): String = buildString {
        appendLine("id,date,time,subject,category,confidence,manualLocation,latitude,longitude,tags,notes")
        items.forEach { item ->
            appendLine(listOf(item.id, item.date, item.time, item.subject, item.category, item.confidenceLevel, item.manualLocation, item.latitude ?: "", item.longitude ?: "", item.tags, item.factsOnlyNotes).joinToString(",") { csvEscape(it.toString()) })
        }
    }

    fun sourcesCsv(items: List<SourceEntity>): String = buildString {
        appendLine("id,type,title,author,dateOrYear,doiOrIsbn,publisherOrJournal,link,accessDate,fileUri,citationStyleNote,importance,readingStatus,reliability,projectId,summary,keyFindings,taught,questions,paperNotes,citation")
        items.forEach { item ->
            appendLine(listOf(item.id, item.type, item.title, item.author, item.dateOrYear, item.doiOrIsbn, item.publisherOrJournal, item.link, item.accessDate, item.fileUri, item.citationStyleNote, item.importance, item.readingStatus, item.reliabilityScore, item.relatedProjectId ?: "", item.personalSummary, item.keyFindings, item.whatThisSourceTaughtMe, item.questionsGenerated, item.paperNotes, sourceCitation(item)).joinToString(",") { csvEscape(it.toString()) })
        }
    }


    fun pdfReadyHtml(
        projects: List<ProjectEntity>,
        observations: List<ObservationEntity>,
        sources: List<SourceEntity>,
        reports: List<ReportEntity>,
        notes: List<NoteEntity> = emptyList(),
        media: ExportMediaBundle = ExportMediaBundle()
    ): String = buildString {
        appendLine("<!doctype html><html><head><meta charset=\"utf-8\"><title>FieldMind Research Export</title>")
        appendLine(ENHANCED_HTML_STYLE)
        appendLine("</head><body>")
        appendLine("<header class=\"hero\"><h1>FieldMind Research Export</h1><p class=\"subtitle\">Comprehensive offline research archive</p><p class=\"meta\">Generated " + java.text.SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault()).format(java.util.Date(System.currentTimeMillis())) + "</p></header>")
        
        // Overview stats
        appendLine("<section id=\"overview\"><h2>Overview</h2><div class=\"stats-grid\">")
        appendLine(statCard("Observations", observations.size, "#2E7D32"))
        appendLine(statCard("Projects", projects.size, "#1B5E20"))
        appendLine(statCard("Sources", sources.size, "#1565C0"))
        appendLine(statCard("Reports", reports.size, "#6A1B9A"))
        appendLine("</div></section>")

        // Projects
        if (projects.isNotEmpty()) {
            appendLine("<section id=\"projects\"><h2>Projects (" + projects.size + ")")
            projects.forEach { project ->
                val projectObs = observations.filter { it.projectId == project.id }
                appendLine("""<div class="card project-card">
                <h3>${"""🚀"""} ${html(project.title)}</h3>
                <div class="meta-row"><span class="badge">${html(project.topicType)}</span><span class="badge status-${html(project.status.lowercase())}">${html(project.status)}</span></div>
                <div class="card-body">
                  ${if (project.objective.isNotBlank()) "<p><strong>Objective:</strong> ${html(project.objective)}</p>" else ""}
                  ${if (project.researchQuestion.isNotBlank()) "<p><strong>Research question:</strong> ${html(project.researchQuestion)}</p>" else ""}
                  ${if (project.methods.isNotBlank()) "<p><strong>Methods:</strong> ${html(project.methods)}</p>" else ""}
                  ${if (project.conclusion.isNotBlank()) "<p><strong>Findings:</strong> ${html(project.conclusion)}</p>" else ""}
                  ${if (projectObs.isNotEmpty()) "<p class=\"meta\"><strong>" + projectObs.size + " observation" + (if (projectObs.size != 1) "s" else "") + "</strong> linked to this project</p>" else ""}
                </div>
                ${media.projectImages[project.id]?.let { images ->
                  if (images.isNotEmpty()) """<div class="att-gallery">${renderAttachmentThumbs(images)}</div>""" else ""
                } ?: ""}
                </div></div>""")
            }
            appendLine("</section>")
        }

        // Observations
        if (observations.isNotEmpty()) {
            appendLine("<section id=\"observations\"><h2>Observations (" + observations.size + ")")
            observations.forEach { observation ->
                val confidenceColor = when (observation.confidenceLevel) {
                    "Sure" -> "#2E7D32"; "Reasonably sure" -> "#558B2F"; "Somewhat sure" -> "#F57F17"
                    "Needs Verification" -> "#E65100"; else -> "#757575"
                }
                appendLine("""<div class="card obs-card">
                <div class="obs-header">
                  <h3 class="obs-subject">${html(observation.subject)}</h3>
                  <span class="confidence-chip" style="background:${confidenceColor}18;color:$confidenceColor;border:1px solid ${confidenceColor}44">${html(observation.confidenceLevel)}</span>
                </div>
                <div class="meta-row">
                  <span>📅 ${html(observation.date)} ${html(observation.time)}</span>
                  <span>🏷️ ${html(observation.category)}</span>
                  ${if (observation.durationMs != null) "<span>⏱️ ${observation.durationMs / 1000}s</span>" else ""}
                  ${if (observation.manualLocation.isNotBlank()) "<span>📍 ${html(observation.manualLocation)}</span>" else ""}
                </div>
                <div class="card-body">
                  ${if (observation.factsOnlyNotes.isNotBlank()) """<div class="field"><span class="field-label">📝 Facts</span><p>${html(observation.factsOnlyNotes)}</p></div>""" else ""}
                  ${if (observation.evidenceSummary.isNotBlank()) """<div class="field"><span class="field-label">🔬 Evidence</span><p>${html(observation.evidenceSummary)}</p></div>""" else ""}
                  ${if (observation.moodOrContext.isNotBlank()) """<div class="field"><span class="field-label">🌿 Context</span><p>${html(observation.moodOrContext)}</p></div>""" else ""}
                </div>
                <div class="obs-footer">
                  ${if (observation.weatherTemperature != null || observation.weatherCondition.isNotBlank()) """<span class="meta">🌤️ ${observation.weatherTemperature?.let { "%.1f°C".format(it) + " " } ?: ""}${html(observation.weatherCondition)}</span>""" else ""}
                  ${if (observation.tags.isNotBlank()) """<span class="tags">🏷️ ${observation.tags.split(",").filter { it.isNotBlank() }.joinToString(" ") { "<code>${html(it.trim())}</code>" }}</span>""" else ""}
                </div>
                ${media.observationImages[observation.id]?.let { images ->
                  if (images.isNotEmpty()) """<div class="att-gallery">${renderAttachmentThumbs(images)}</div>""" else ""
                } ?: ""}
                </div></div>""")
            }
            appendLine("</section>")
        }

        // Sources
        if (sources.isNotEmpty()) {
            appendLine("<section id=\"sources\"><h2>Sources (" + sources.size + ")")
            sources.forEach { source ->
                val importanceColor = when (source.importance) {
                    "Essential" -> "#C62828"; "Key" -> "#E65100"; "Supporting" -> "#2E7D32"; else -> "#757575"
                }
                appendLine("""<div class="card source-card">
                <h3>${html(source.title)}</h3>
                <div class="meta-row">
                  <span class="badge" style="background:${importanceColor}18;color:$importanceColor">${html(source.importance)}</span>
                  <span class="badge">${html(source.type)}</span>
                  ${if (source.readingStatus.isNotBlank()) "<span class=\"badge\">${html(source.readingStatus)}</span>" else ""}
                </div>
                <p class=\"citation\">📖 ${html(sourceCitation(source))}</p>
                <div class="card-body">
                  ${if (source.personalSummary.isNotBlank()) """<div class="field"><span class="field-label">📄 Summary</span><p>${html(source.personalSummary)}</p></div>""" else ""}
                  ${if (source.keyFindings.isNotBlank()) """<div class="field"><span class="field-label">🔑 Key findings</span><p>${html(source.keyFindings)}</p></div>""" else ""}
                  ${if (source.whatThisSourceTaughtMe.isNotBlank()) """<div class="field"><span class="field-label">💡 What this taught me</span><p>${html(source.whatThisSourceTaughtMe)}</p></div>""" else ""}
                  ${if (source.paperNotes.isNotBlank()) """<div class="field"><span class="field-label">📝 Notes</span><p>${html(source.paperNotes)}</p></div>""" else ""}
                </div>
                ${if (source.link.isNotBlank()) "<p class=\"meta\">🔗 <a href=\"${xml(source.link)}\">${html(source.link)}</a></p>" else ""}
                <p class=\"meta\">Reliability: "★".repeat(source.reliabilityScore) + "☆☆☆☆☆".drop(source.reliabilityScore)</p>
                ${media.sourceImages[source.id]?.let { images ->
                  if (images.isNotEmpty()) """<div class="att-gallery">${renderAttachmentThumbs(images)}</div>""" else ""
                } ?: ""}
                </div>""")
            }
            appendLine("</section>")
        }

        // Notes
        if (notes.isNotEmpty()) {
            appendLine("<section id=\"notes\"><h2>Notes (" + notes.size + ")</h2>")
            notes.forEach { note ->
                appendLine(""""<div class="card note-card">
                <h3>${html(note.title)}</h3>
                <div class="meta-row">
                  <span>🏷️ ${html(note.category)}</span>
                  ${if (note.tags.isNotBlank()) """<span class="tags">${note.tags.split(",").filter { it.isNotBlank() }.joinToString(" ") { "<code>${html(it.trim())}</code>" }}</span>""" else ""}
                </div>
                <div class="card-body">
                  ${if (note.body.isNotBlank()) """<p>${html(note.body)}</p>""" else ""}
                </div>
                ${media.noteImages[note.id]?.let { images ->
                  if (images.isNotEmpty()) """<div class="att-gallery">${renderAttachmentThumbs(images)}</div>""" else ""
                } ?: ""}
                </div>"""")
            }
            appendLine("</section>")
        }

        // Reports
        if (reports.isNotEmpty()) {
            appendLine("<section id=\"reports\"><h2>Reports (" + reports.size + ")</h2>")
            reports.forEach { report ->
                appendLine("""<div class="card report-card">
                <h3>${html(report.title)}</h3>
                <div class="meta-row"><span class="badge">${html(report.type)}</span><span class="badge status-${html(report.status.lowercase())}">${html(report.status)}</span></div>
                <div class="card-body"><pre class="report-pre">${html(buildMarkdownReport(report))}</pre></div></div>""")
            }
            appendLine("</section>")
        }

        appendLine("<footer><p>Generated by FieldMind — ${projects.size + observations.size + sources.size + reports.size} total records</p></footer>")
        appendLine("</body></html>")
    }

    private fun statCard(label: String, value: Int, color: String): String = """<div class="stat-card" style="border-left:4px solid $color"><span class="stat-value">$value</span><span class="stat-label">$label</span></div>"""

    private val ENHANCED_HTML_STYLE: String = """<style>
      * { margin: 0; padding: 0; box-sizing: border-box; }
      body { font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; line-height: 1.6; color: #1a1a1a; background: #f0f4ec; padding: 0; }
      .hero { background: linear-gradient(135deg, #1B5E20 0%, #2E7D32 40%, #388E3C 100%); color: white; padding: 48px 40px; text-align: center; }
      .hero h1 { font-size: 2.2em; font-weight: 700; margin-bottom: 8px; }
      .hero .subtitle { font-size: 1.1em; opacity: 0.85; margin-bottom: 4px; }
      .hero .meta { font-size: 0.85em; opacity: 0.65; }
      section { max-width: 960px; margin: 0 auto; padding: 32px 24px; }
      h2 { font-size: 1.5em; font-weight: 700; color: #1B5E20; margin-bottom: 20px; padding-bottom: 8px; border-bottom: 2px solid #2E7D3244; }
      .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-bottom: 8px; }
      .stat-card { background: white; border-radius: 16px; padding: 24px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
      .stat-value { display: block; font-size: 2.4em; font-weight: 800; line-height: 1; margin-bottom: 4px; }
      .stat-label { font-size: 0.9em; color: #666; }
      .card { background: white; border-radius: 20px; padding: 20px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); transition: box-shadow 0.2s; }
      .card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
      .card h3 { font-size: 1.15em; font-weight: 700; color: #1a1a1a; margin-bottom: 6px; }
      .meta-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 10px; font-size: 0.85em; color: #666; }
      .badge { display: inline-block; background: #f0f4ec; padding: 2px 10px; border-radius: 12px; font-size: 0.82em; font-weight: 600; color: #2E7D32; }
      .status-active { background: #E8F5E9; color: #2E7D32; }
      .status-draft { background: #FFF8E1; color: #F57F17; }
      .status-archived { background: #ECEFF1; color: #546E7A; }
      .card-body { margin-top: 8px; }
      .field { margin-bottom: 10px; }
      .field-label { display: inline-block; font-size: 0.82em; font-weight: 600; color: #2E7D32; margin-bottom: 4px; }
      .field p { font-size: 0.92em; color: #333; line-height: 1.65; }
      .obs-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
      .confidence-chip { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 0.78em; font-weight: 600; white-space: nowrap; }
      .tags { margin-top: 6px; }
      .tags code { display: inline-block; background: #E8F5E9; padding: 1px 8px; border-radius: 8px; font-size: 0.82em; margin: 2px; color: #2E7D32; }
      .citation { font-style: italic; font-size: 0.9em; color: #444; padding: 8px 12px; background: #FAFCF9; border-radius: 12px; margin: 8px 0; }
      .report-pre { background: #f6f8f4; padding: 16px; border-radius: 14px; font-size: 0.85em; line-height: 1.5; overflow-x: auto; white-space: pre-wrap; }
      a { color: #1B5E20; text-decoration: none; }
      a:hover { text-decoration: underline; }
      .att-gallery { display: flex; flex-wrap: wrap; gap: 8px; margin: 10px 0 4px 0; }
      .att-thumb { width: 100px; height: 100px; border-radius: 12px; overflow: hidden; position: relative; background: #f0f4ec; flex-shrink: 0; cursor: pointer; transition: transform 0.15s; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
      .att-thumb:hover { transform: scale(1.05); }
      .att-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
      .att-thumb.att-more { display: flex; align-items: center; justify-content: center; font-size: 0.85em; font-weight: 600; color: #2E7D32; background: #E8F5E9; }
      .att-caption { position: absolute; bottom: 0; left: 0; right: 0; padding: 2px 6px; font-size: 0.65em; background: rgba(0,0,0,0.55); color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
      footer { text-align: center; padding: 32px; font-size: 0.82em; color: #999; }
      @media print { body { background: white; } .hero { background: #1B5E20 !important; -webkit-print-color-adjust: exact; } .card { break-inside: avoid; box-shadow: none; border: 1px solid #eee; } .att-thumb { break-inside: avoid; } }
    </style>"""

    fun simplePdfBytes(title: String, body: String, embeddedImageBytes: ByteArray? = null): ByteArray {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true; color = Color.rgb(27, 94, 32) }
        val sectionPaint = Paint().apply { textSize = 16f; isFakeBoldText = true; color = Color.rgb(46, 125, 50) }
        val bodyPaint = Paint().apply { textSize = 10f; isAntiAlias = true; color = Color.rgb(51, 51, 51) }
        val metaPaint = Paint().apply { textSize = 8.5f; isAntiAlias = true; color = Color.rgb(130, 130, 130) }
        
        val lines = body.lines()
        val wrappedLines = mutableListOf<Pair<String, String>>() // type, text
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> { wrappedLines.add("title" to line.removePrefix("# ")) }
                line.startsWith("## ") -> { wrappedLines.add("section" to line.removePrefix("## ")) }
                line.startsWith("---") -> { wrappedLines.add("divider" to "") }
                line.isBlank() -> { wrappedLines.add("blank" to "") }
                else -> { line.chunked(88).forEach { chunk -> wrappedLines.add("body" to chunk) } }
            }
        }
        
        var pageNumber = 1
        var index = 0
        do {
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            val canvas = page.canvas
            
            // Page background
            canvas.drawColor(Color.rgb(250, 252, 249))
            
            var y = 44f
            
            // Draw title only on first page
            if (pageNumber == 1) {
                // Title background
                val titleBgPaint = Paint().apply { color = Color.rgb(27, 94, 32); alpha = 20 }
                canvas.drawRoundRect(28f, 20f, 567f, 68f, 16f, 16f, titleBgPaint)
                canvas.drawText(title, 40f, y, titlePaint)
                y += 32f
                // Subtitle
                canvas.drawText("Generated PDF export — FieldMind", 40f, y, metaPaint)
                y += 22f
            }
            
            while (index < wrappedLines.size && y < 800f) {
                val (type, text) = wrappedLines[index]
                when (type) {
                    "title" -> { canvas.drawText(text, 40f, y, titlePaint); y += 28f }
                    "section" -> { canvas.drawText(text, 40f, y, sectionPaint); y += 20f }
                    "divider" -> { canvas.drawLine(40f, y, 555f, y, Paint().apply { color = Color.rgb(200, 210, 195); strokeWidth = 1f }); y += 12f }
                    "blank" -> { y += 8f }
                    "body" -> { canvas.drawText(text, 40f, y, bodyPaint); y += 15f }
                }
                index++
            }
            
            // Draw embedded image if available (on first page only, after text)
            if (pageNumber == 1 && embeddedImageBytes != null && y < 720f) {
                try {
                    val imgBitmap = android.graphics.BitmapFactory.decodeByteArray(embeddedImageBytes, 0, embeddedImageBytes.size)
                    if (imgBitmap != null) {
                        // Scale to fit within page width, max height 200dp
                        val maxW = 515f
                        val maxH = 200f
                        val scale = minOf(maxW / imgBitmap.width, maxH / imgBitmap.height, 1f)
                        val drawW = (imgBitmap.width * scale).toInt()
                        val drawH = (imgBitmap.height * scale).toInt()
                        val drawX = (595 - drawW) / 2f
                        val drawY = y + 8f
                        
                        // Draw image border/background
                        val imgBgPaint = Paint().apply { color = Color.rgb(240, 244, 236) }
                        canvas.drawRoundRect(drawX - 4f, drawY - 4f, drawX + drawW + 4f, drawY + drawH + 4f, 8f, 8f, imgBgPaint)
                        
                        canvas.drawBitmap(
                            Bitmap.createScaledBitmap(imgBitmap, drawW, drawH, true),
                            drawX, drawY, null
                        )
                        y += drawH + 24f
                        imgBitmap.recycle()
                    }
                } catch (_: Exception) { }
            }
            
            // Footer
            val footerPaint = Paint().apply { textSize = 7f; color = Color.rgb(180, 190, 175) }
            canvas.drawText("FieldMind • Page $pageNumber", 40f, 825f, footerPaint)
            
            document.finishPage(page)
            pageNumber++
        } while (index < wrappedLines.size)
        
        return ByteArrayOutputStream().use { out -> document.writeTo(out); document.close(); out.toByteArray() }
    }

    fun dashboardPngBytes(observations: List<ObservationEntity>, sources: List<SourceEntity>, projects: List<ProjectEntity>, notes: List<NoteEntity>): ByteArray {
        val bitmap = Bitmap.createBitmap(1200, 675, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(237, 246, 236))
        paint.color = Color.rgb(38, 87, 63)
        paint.textSize = 54f
        paint.isFakeBoldText = true
        canvas.drawText("FieldMind dashboard", 56f, 96f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 24f
        paint.color = Color.rgb(79, 99, 80)
        canvas.drawText("Offline research summary export", 58f, 136f, paint)
        val stats = listOf(
            Triple("Observations", observations.size, Color.rgb(47, 125, 91)),
            Triple("Sources", sources.size, Color.rgb(68, 102, 205)),
            Triple("Projects", projects.size, Color.rgb(150, 95, 38)),
            Triple("Notes", notes.size, Color.rgb(130, 87, 172))
        )
        stats.forEachIndexed { index, stat ->
            val x = 70f + index * 275f
            val y = 245f
            paint.color = Color.WHITE
            canvas.drawRoundRect(x, y, x + 230f, y + 190f, 32f, 32f, paint)
            paint.color = stat.third
            canvas.drawCircle(x + 58f, y + 58f, 24f, paint)
            paint.textSize = 58f
            paint.isFakeBoldText = true
            canvas.drawText(stat.second.toString(), x + 34f, y + 125f, paint)
            paint.textSize = 24f
            paint.isFakeBoldText = false
            paint.color = Color.rgb(79, 99, 80)
            canvas.drawText(stat.first, x + 34f, y + 162f, paint)
        }
        paint.textSize = 26f
        paint.color = Color.rgb(47, 125, 91)
        canvas.drawText(if (observations.isNotEmpty()) "Keep collecting: your evidence base is growing." else "Start by capturing one observation.", 70f, 560f, paint)
        return ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); bitmap.recycle(); out.toByteArray() }
    }

    fun dashboardSvg(observations: List<ObservationEntity>, sources: List<SourceEntity>, projects: List<ProjectEntity>, notes: List<NoteEntity>): String = """
        <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="675" viewBox="0 0 1200 675">
          <defs><linearGradient id="g" x1="0" x2="1"><stop stop-color="#1B5E20"/><stop offset="1" stop-color="#1565C0"/></linearGradient></defs>
          <rect width="1200" height="675" rx="42" fill="#F7FBF3"/>
          <rect x="36" y="36" width="1128" height="603" rx="36" fill="url(#g)" opacity=".10"><animate attributeName="opacity" values=".08;.16;.08" dur="6s" repeatCount="indefinite"/></rect>
          <text x="72" y="110" font-family="sans-serif" font-size="48" font-weight="700" fill="#243424">FieldMind Snapshot</text>
          <text x="72" y="155" font-family="sans-serif" font-size="22" fill="#4F6350">Observation • Source • Project • Note overview</text>
          ${svgStat(72, 230, "Observations", observations.size, "#2E7D32")}
          ${svgStat(334, 230, "Sources", sources.size, "#1565C0")}
          ${svgStat(596, 230, "Projects", projects.size, "#6A1B9A")}
          ${svgStat(858, 230, "Notes", notes.size, "#00838F")}
          <text x="72" y="560" font-family="sans-serif" font-size="24" fill="#243424">Newest observation: ${xml(observations.firstOrNull()?.subject ?: "Start capturing")}</text>
        </svg>
    """.trimIndent()

    private fun svgStat(x: Int, y: Int, label: String, value: Int, color: String): String = """
        <g><rect x="$x" y="$y" width="220" height="190" rx="30" fill="white" opacity=".86"/>
        <circle cx="${x + 52}" cy="${y + 58}" r="22" fill="$color" opacity=".18"/>
        <text x="${x + 32}" y="${y + 118}" font-family="sans-serif" font-size="52" font-weight="700" fill="$color">$value</text>
        <text x="${x + 32}" y="${y + 154}" font-family="sans-serif" font-size="22" fill="#4F6350">$label</text></g>
    """.trimIndent()

    fun dataCsv(items: List<DataRecordEntity>): String = buildString {
        appendLine("id,timestamp,tool,label,value,unit,location,notes,projectId,observationId")
        items.forEach { item ->
            appendLine(listOf(item.id, item.timestamp, item.toolType, item.label, item.value, item.unit, item.location, item.notes, item.projectId ?: "", item.observationId ?: "").joinToString(",") { csvEscape(it.toString()) })
        }
    }

    fun archiveJson(
        observations: List<ObservationEntity>,
        notes: List<NoteEntity>,
        questions: List<QuestionEntity>,
        hypotheses: List<HypothesisEntity>,
        projects: List<ProjectEntity>,
        sources: List<SourceEntity>,
        dataRecords: List<DataRecordEntity>,
        reports: List<ReportEntity>,
        flashcards: List<FlashcardEntity>,
        mediaManifest: String? = null  // Optional: JSON array of media entries for .fieldmind package
    ): String = buildString {
        appendLine("{")
        appendLine("  \"format\": \"fieldmind-archive-v2\",")
        appendLine("  \"exportedAt\": ${System.currentTimeMillis()},")
        appendLine("  \"appName\": \"FieldMind\",")
        appendLine("  \"appVersion\": \"4.3.0\",")
        appendLine("  \"counts\": {\"observations\": ${observations.size}, \"notes\": ${notes.size}, \"questions\": ${questions.size}, \"hypotheses\": ${hypotheses.size}, \"projects\": ${projects.size}, \"sources\": ${sources.size}, \"dataRecords\": ${dataRecords.size}, \"reports\": ${reports.size}, \"flashcards\": ${flashcards.size}},")
        appendJsonArray("observations", observations) { o -> "{\"id\":${o.id},\"date\":\"${json(o.date)}\",\"time\":\"${json(o.time)}\",\"subject\":\"${json(o.subject)}\",\"category\":\"${json(o.category)}\",\"factsOnlyNotes\":\"${json(o.factsOnlyNotes)}\",\"evidenceSummary\":\"${json(o.evidenceSummary)}\",\"tags\":\"${json(o.tags)}\",\"projectId\":${o.projectId ?: "null"}}" }
        appendLine(",")
        appendJsonArray("notes", notes) { n -> "{\"id\":${n.id},\"title\":\"${json(n.title)}\",\"body\":\"${json(n.body)}\",\"category\":\"${json(n.category)}\",\"tags\":\"${json(n.tags)}\",\"projectId\":${n.projectId ?: "null"},\"sourceId\":${n.sourceId ?: "null"},\"attachmentUris\":\"${json(n.attachmentUris)}\"}" }
        appendLine(",")
        appendJsonArray("projects", projects) { p -> "{\"id\":${p.id},\"title\":\"${json(p.title)}\",\"topicType\":\"${json(p.topicType)}\",\"objective\":\"${json(p.objective)}\",\"researchQuestion\":\"${json(p.researchQuestion)}\",\"status\":\"${json(p.status)}\"}" }
        appendLine(",")
        appendJsonArray("sources", sources) { s -> "{\"id\":${s.id},\"type\":\"${json(s.type)}\",\"title\":\"${json(s.title)}\",\"author\":\"${json(s.author)}\",\"dateOrYear\":\"${json(s.dateOrYear)}\",\"doiOrIsbn\":\"${json(s.doiOrIsbn)}\",\"publisherOrJournal\":\"${json(s.publisherOrJournal)}\",\"link\":\"${json(s.link)}\",\"accessDate\":\"${json(s.accessDate)}\",\"fileUri\":\"${json(s.fileUri)}\",\"citationStyleNote\":\"${json(s.citationStyleNote)}\",\"importance\":\"${json(s.importance)}\",\"readingStatus\":\"${json(s.readingStatus)}\",\"reliabilityScore\":${s.reliabilityScore},\"relatedProjectId\":${s.relatedProjectId ?: "null"},\"personalSummary\":\"${json(s.personalSummary)}\",\"keyFindings\":\"${json(s.keyFindings)}\",\"whatThisSourceTaughtMe\":\"${json(s.whatThisSourceTaughtMe)}\",\"questionsGenerated\":\"${json(s.questionsGenerated)}\",\"paperNotes\":\"${json(s.paperNotes)}\",\"citation\":\"${json(sourceCitation(s))}\"}" }
        appendLine(",")
        appendJsonArray("questions", questions) { q -> "{\"id\":${q.id},\"questionText\":\"${json(q.questionText)}\",\"category\":\"${json(q.category)}\",\"status\":\"${json(q.status)}\",\"priority\":\"${json(q.priority)}\"}" }
        appendLine(",")
        appendJsonArray("hypotheses", hypotheses) { h -> "{\"id\":${h.id},\"prediction\":\"${json(h.prediction)}\",\"resultStatus\":\"${json(h.resultStatus)}\",\"confidencePercent\":${h.confidencePercent}}" }
        appendLine(",")
        appendJsonArray("dataRecords", dataRecords) { d -> "{\"id\":${d.id},\"toolType\":\"${json(d.toolType)}\",\"label\":\"${json(d.label)}\",\"value\":\"${json(d.value)}\",\"unit\":\"${json(d.unit)}\"}" }
        appendLine(",")
        appendJsonArray("reports", reports) { r -> "{\"id\":${r.id},\"type\":\"${json(r.type)}\",\"title\":\"${json(r.title)}\",\"status\":\"${json(r.status)}\",\"markdownDraft\":\"${json(FieldMindExport.buildMarkdownReport(r))}\"}" }
        appendLine(",")
        appendJsonArray("flashcards", flashcards) { f -> "{\"id\":${f.id},\"front\":\"${json(f.front)}\",\"back\":\"${json(f.back)}\",\"type\":\"${json(f.type)}\"}" }
        if (mediaManifest != null) {
            appendLine(",")
            appendLine("  \"mediaManifest\": $mediaManifest")
        }
        appendLine()
        appendLine("}")
    }

    private fun <T> StringBuilder.appendJsonArray(name: String, items: List<T>, render: (T) -> String) {
        appendLine("  \"$name\": [")
        items.forEachIndexed { index, item ->
            append("    ").append(render(item))
            appendLine(if (index == items.lastIndex) "" else ",")
        }
        append("  ]")
    }


    private inline fun <T> org.json.JSONArray?.toObjects(block: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) optJSONObject(i)?.let { add(block(it)) }
        }
    }

    private fun JSONObject.optNullableLong(name: String): Long? = if (isNull(name) || !has(name)) null else optLong(name).takeIf { it > 0L }

    // ══════════════════════════════════════════════════════════════════════
    //  Export privacy utilities
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Apply GPS privacy mode to a list of observations before export.
     *
     * @param observations  The raw observations from the database.
     * @param gpsPrivacy    One of "Exact", "Approximate", or "Remove".
     *                      "Exact"       – pass through unchanged.
     *                      "Approximate" – round lat/lon to 2 decimal places (~1 km) and
     *                                      replace manualLocation with a generic locality
     *                                      marker so no street-level detail leaks.
     *                      "Remove"      – null out lat/lon and blank manualLocation.
     */
    fun applyGpsPrivacy(
        observations: List<ObservationEntity>,
        gpsPrivacy: String
    ): List<ObservationEntity> = when (gpsPrivacy) {
        "Approximate" -> observations.map { obs ->
            obs.copy(
                latitude = obs.latitude?.let { kotlin.math.round(it * 100.0) / 100.0 },
                longitude = obs.longitude?.let { kotlin.math.round(it * 100.0) / 100.0 },
                // Replace any manually-entered street-level address with a cleared field
                // to avoid leaking precise location via the text field while approximate
                // coordinates are still present.
                manualLocation = if (obs.manualLocation.isNotBlank()) "" else ""
            )
        }
        "Remove" -> observations.map { obs ->
            obs.copy(
                latitude = null,
                longitude = null,
                manualLocation = ""
            )
        }
        else -> observations // "Exact" or unknown — no change
    }

    /**
     * Strip media attachment URIs from notes before export.
     * EvidenceAttachment rows are handled separately by passing an empty list
     * to the export path; this covers the attachmentUris field on NoteEntity.
     */
    fun applyMediaExclusion(notes: List<NoteEntity>): List<NoteEntity> =
        notes.map { it.copy(attachmentUris = "") }

    private fun html(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")

    private fun renderAttachmentThumbs(images: List<Pair<String, String>>, maxInline: Int = 3): String {
        return images.take(maxInline).joinToString("") { (caption, dataUri) ->
            """<div class="att-thumb"><img src="$dataUri" alt="${html(caption)}" loading="lazy"/><span class="att-caption">${html(caption)}</span></div>"""
        } + if (images.size > maxInline) {
            """<div class="att-thumb att-more"><span>+${images.size - maxInline} more</span></div>"""
        } else ""
    }
    private fun xml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun csvEscape(value: String): String = "\"${value.replace("\"", "\"\"")}\""
    private fun json(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
}
