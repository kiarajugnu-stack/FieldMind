package chromahub.rhythm.app.features.field.data.export

import chromahub.rhythm.app.features.field.data.database.entity.*

object FieldMindExport {
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
        flashcards: List<FlashcardEntity>
    ): String = buildString {
        appendLine("{")
        appendLine("  \"format\": \"fieldmind-archive-v2\",")
        appendLine("  \"exportedAt\": ${System.currentTimeMillis()},")
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

    private fun html(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
    private fun xml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun csvEscape(value: String): String = "\"${value.replace("\"", "\"\"")}\""
    private fun json(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
}
