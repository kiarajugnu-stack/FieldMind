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

    fun observationsCsv(items: List<ObservationEntity>): String = buildString {
        appendLine("id,date,time,subject,category,confidence,manualLocation,latitude,longitude,tags,notes")
        items.forEach { item ->
            appendLine(listOf(item.id, item.date, item.time, item.subject, item.category, item.confidenceLevel, item.manualLocation, item.latitude ?: "", item.longitude ?: "", item.tags, item.factsOnlyNotes).joinToString(",") { csvEscape(it.toString()) })
        }
    }

    fun dataCsv(items: List<DataRecordEntity>): String = buildString {
        appendLine("id,timestamp,tool,label,value,unit,location,notes,projectId,observationId")
        items.forEach { item ->
            appendLine(listOf(item.id, item.timestamp, item.toolType, item.label, item.value, item.unit, item.location, item.notes, item.projectId ?: "", item.observationId ?: "").joinToString(",") { csvEscape(it.toString()) })
        }
    }

    fun archiveJson(
        observations: List<ObservationEntity>,
        questions: List<QuestionEntity>,
        hypotheses: List<HypothesisEntity>,
        projects: List<ProjectEntity>,
        sources: List<SourceEntity>,
        dataRecords: List<DataRecordEntity>,
        reports: List<ReportEntity>,
        flashcards: List<FlashcardEntity>
    ): String = buildString {
        appendLine("{")
        appendLine("  \"format\": \"fieldmind-archive-v1\",")
        appendLine("  \"exportedAt\": ${System.currentTimeMillis()},")
        appendLine("  \"counts\": {")
        appendLine("    \"observations\": ${observations.size}, \"questions\": ${questions.size}, \"hypotheses\": ${hypotheses.size},")
        appendLine("    \"projects\": ${projects.size}, \"sources\": ${sources.size}, \"dataRecords\": ${dataRecords.size},")
        appendLine("    \"reports\": ${reports.size}, \"flashcards\": ${flashcards.size}")
        appendLine("  },")
        appendLine("  \"observations\": [")
        observations.forEachIndexed { index, o ->
            append("    {\"id\":${o.id},\"date\":\"${json(o.date)}\",\"time\":\"${json(o.time)}\",\"subject\":\"${json(o.subject)}\",\"category\":\"${json(o.category)}\",\"notes\":\"${json(o.factsOnlyNotes)}\"}")
            appendLine(if (index == observations.lastIndex) "" else ",")
        }
        appendLine("  ]")
        appendLine("}")
    }

    private fun csvEscape(value: String): String = "\"${value.replace("\"", "\"\"")}\""
    private fun json(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
}
