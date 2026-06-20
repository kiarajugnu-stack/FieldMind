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


    fun pdfReadyHtml(projects: List<ProjectEntity>, observations: List<ObservationEntity>, sources: List<SourceEntity>, reports: List<ReportEntity>): String = buildString {
        appendLine("<!doctype html><html><head><meta charset=\"utf-8\"><title>FieldMind Research Export</title>")
        appendLine("<style>body{font-family:serif;margin:40px;line-height:1.5}h1,h2{font-family:sans-serif}.card{border:1px solid #ddd;border-radius:16px;padding:16px;margin:12px 0}.meta{color:#555;font-size:12px}</style></head><body>")
        appendLine("<h1>FieldMind Research Export</h1><p class=\"meta\">Generated ${System.currentTimeMillis()}</p>")
        appendLine("<h2>Projects</h2>")
        projects.forEach { project -> appendLine("<div class=\"card\"><h3>${html(project.title)}</h3><p>${html(project.objective.ifBlank { project.researchQuestion })}</p><p class=\"meta\">${html(project.topicType)} · ${html(project.status)}</p></div>") }
        appendLine("<h2>Observations</h2>")
        observations.forEach { observation -> appendLine("<div class=\"card\"><h3>${html(observation.subject)}</h3><p>${html(observation.factsOnlyNotes)}</p><p class=\"meta\">${html(observation.category)} · ${html(observation.date)} ${html(observation.time)} · ${html(observation.confidenceLevel)}</p></div>") }
        appendLine("<h2>Sources</h2>")
        sources.forEach { source -> appendLine("<div class=\"card\"><h3>${html(source.title)}</h3><p>${html(sourceCitation(source))}</p><p>${html(source.whatThisSourceTaughtMe.ifBlank { source.personalSummary })}</p></div>") }
        appendLine("<h2>Reports</h2>")
        reports.forEach { report -> appendLine("<div class=\"card\"><pre>${html(buildMarkdownReport(report))}</pre></div>") }
        appendLine("</body></html>")
    }

    fun simplePdfBytes(title: String, body: String): ByteArray {
        val document = PdfDocument()
        val paint = Paint().apply { textSize = 12f; isAntiAlias = true }
        val titlePaint = Paint(paint).apply { textSize = 20f; isFakeBoldText = true }
        val lines = body.lines().flatMap { line -> line.chunked(92).ifEmpty { listOf("") } }
        var pageNumber = 1
        var index = 0
        do {
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            val canvas = page.canvas
            var y = 48f
            if (pageNumber == 1) { canvas.drawText(title, 40f, y, titlePaint); y += 34f }
            while (index < lines.size && y < 800f) {
                canvas.drawText(lines[index], 40f, y, paint)
                y += 17f
                index++
            }
            document.finishPage(page)
            pageNumber++
        } while (index < lines.size)
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

    private fun html(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
    private fun xml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun csvEscape(value: String): String = "\"${value.replace("\"", "\"\"")}\""
    private fun json(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
}
