package fieldmind.research.app.features.field.data.export

import fieldmind.research.app.features.field.data.database.entity.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline-first report generator that produces rich HTML and Markdown reports
 * with embedded SVG data visualizations (pie charts, bar charts, line charts).
 * All statistics and charts are computed from in-memory lists — no network, no dependencies.
 */
object FieldMindReportGenerator {

    // ══════════════════════════════════════════════════════════════════════
    //  Report data structures
    // ══════════════════════════════════════════════════════════════════════

    data class ReportData(
        val title: String,
        val subtitle: String = "",
        val author: String = "FieldMind User",
        val date: String = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date()),
        val overview: OverviewSection,
        val charts: List<ChartSection> = emptyList(),
        val entityTables: List<EntityTable> = emptyList(),
        val reports: List<ReportEntity> = emptyList(),
        val raw: RawDataSection? = null
    )

    data class OverviewSection(
        val totalObservations: Int = 0,
        val totalProjects: Int = 0,
        val totalSources: Int = 0,
        val totalNotes: Int = 0,
        val totalReports: Int = 0,
        val totalDataRecords: Int = 0,
        val dateRange: String = "",
        val topCategories: List<Pair<String, Int>> = emptyList(),
        val topTags: List<Pair<String, Int>> = emptyList(),
        val confidenceBreakdown: Map<String, Int> = emptyMap(),
        val mostActiveLocation: String = ""
    )

    data class ChartSection(
        val title: String,
        val description: String,
        val svgContent: String,
        val csvData: String = ""
    )

    data class EntityTable(
        val title: String,
        val headers: List<String>,
        val rows: List<List<String>>,
        val totalCount: Int
    )

    data class RawDataSection(
        val observationsCsv: String = "",
        val dataRecordsCsv: String = "",
        val sourcesCsv: String = ""
    )

    // ══════════════════════════════════════════════════════════════════════
    //  SVG Chart Generators
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Generate an SVG donut/pie chart.
     * @param parts List of (label, value, hexColor) triples
     * @param width SVG width in pixels
     * @param height SVG height in pixels
     */
    fun pieChartSvg(
        parts: List<Triple<String, Float, String>>,
        width: Int = 500,
        height: Int = 300
    ): String {
        val total = parts.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)
        val cx = 160f
        val cy = height / 2f
        val outerR = 100f
        val innerR = 55f
        val labelX = 280f

        val sb = StringBuilder()
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height" viewBox="0 0 $width $height">""")
        sb.appendLine("<defs>")
        parts.forEachIndexed { i, (label, _, color) ->
            sb.appendLine("""<filter id="shadow$i"><feDropShadow dx="1" dy="1" stdDeviation="2" flood-color="$color" flood-opacity="0.25"/></filter>""")
        }
        sb.appendLine("""<linearGradient id="bgGrad" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#f8faf7"/><stop offset="100%" stop-color="#f0f4ec"/></linearGradient>""")
        sb.appendLine("</defs>")
        sb.appendLine("""<rect width="$width" height="$height" rx="16" fill="url(#bgGrad)"/>""")

        // Draw slices
        var startAngle = -90f
        parts.forEachIndexed { i, (label, value, color) ->
            val sweep = (value / total) * 360f
            if (sweep > 0.5f) {
                val startRad = Math.toRadians(startAngle.toDouble())
                val endRad = Math.toRadians((startAngle + sweep).toDouble())
                val x1 = cx + outerR * Math.cos(startRad).toFloat()
                val y1 = cy + outerR * Math.sin(startRad).toFloat()
                val x2 = cx + outerR * Math.cos(endRad).toFloat()
                val y2 = cy + outerR * Math.sin(endRad).toFloat()
                val largeArc = if (sweep > 180f) 1 else 0

                // Outer arc path
                val outerPath = "M $cx,$cy L $x1,$y1 A $outerR,$outerR 0 $largeArc,1 $x2,$y2 Z"
                // Inner cutout
                val innerStartX = cx + innerR * Math.cos(startRad).toFloat()
                val innerStartY = cy + innerR * Math.sin(startRad).toFloat()
                val innerEndX = cx + innerR * Math.cos(endRad).toFloat()
                val innerEndY = cy + innerR * Math.sin(endRad).toFloat()

                sb.appendLine("""<path d="M $innerStartX,$innerStartY L $x1,$y1 A $outerR,$outerR 0 $largeArc,1 $x2,$y2 L $innerEndX,$innerEndY A $innerR,$innerR 0 $largeArc,0 $innerStartX,$innerStartY Z" fill="$color" opacity="0.88" filter="url(#shadow$i)"/>""")

                // Label line to outside
                val midAngle = startAngle + sweep / 2f
                val midRad = Math.toRadians(midAngle.toDouble())
                val labelR = outerR + 18f
                val lx = cx + labelR * Math.cos(midRad).toFloat()
                val ly = cy + labelR * Math.sin(midRad).toFloat()
                sb.appendLine("""<line x1="$lx" y1="$ly" x2="$labelX" y2="$ly" stroke="$color" stroke-width="1" opacity="0.5"/>""")
            }
            startAngle += sweep
        }

        // Center hole
        sb.appendLine("""<circle cx="$cx" cy="$cy" r="$innerR" fill="white" opacity="0.95"/>""")
        sb.appendLine("""<text x="$cx" y="${cy - 6}" text-anchor="middle" font-family="sans-serif" font-size="22" font-weight="700" fill="#1a1a1a">${total.toInt()}</text>""")
        sb.appendLine("""<text x="$cx" y="${cy + 12}" text-anchor="middle" font-family="sans-serif" font-size="11" fill="#666">total</text>""")

        // Legend
        var ly = height * 0.08f
        parts.forEach { (label, value, color) ->
            val pct = (value / total * 100).toInt()
            sb.appendLine("""<rect x="$labelX" y="$ly" width="10" height="10" rx="3" fill="$color"/>""")
            sb.appendLine("""<text x="${labelX + 16}" y="${ly + 9}" font-family="sans-serif" font-size="11" fill="#333">${htmlEscape(label)}</text>""")
            sb.appendLine("""<text x="${width - 20}" y="${ly + 9}" text-anchor="end" font-family="sans-serif" font-size="11" font-weight="600" fill="#1a1a1a">${value.toInt()} ($pct%)</text>""")
            ly += 20f
        }

        sb.appendLine("</svg>")
        return sb.toString()
    }

    /**
     * Generate an SVG vertical bar chart.
     * @param data List of (label, value) pairs
     * @param width SVG width
     * @param height SVG height
     * @param barColor hex color for bars
     */
    fun barChartSvg(
        data: List<Pair<String, Float>>,
        width: Int = 600,
        height: Int = 320,
        barColor: String = "#2E7D32"
    ): String {
        if (data.isEmpty()) return emptyChartSvg(width, height, "No data available")
        val max = data.maxOf { it.second }.coerceAtLeast(1f)
        val barCount = data.size
        val leftMargin = 60f
        val rightMargin = 20f
        val topMargin = 20f
        val bottomMargin = 60f
        val chartW = width - leftMargin - rightMargin
        val chartH = height - topMargin - bottomMargin
        val gap = chartW * 0.12f / barCount.coerceAtLeast(1)
        val barW = (chartW - gap * (barCount + 1)) / barCount

        val sb = StringBuilder()
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height" viewBox="0 0 $width $height">""")
        sb.appendLine("""<defs><linearGradient id="barBg" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#f8faf7"/><stop offset="100%" stop-color="#f0f4ec"/></linearGradient></defs>""")
        sb.appendLine("""<rect width="$width" height="$height" rx="16" fill="url(#barBg)"/>""")

        // Y-axis gridlines
        val ySteps = 4
        for (i in 0..ySteps) {
            val y = topMargin + chartH * (1f - i.toFloat() / ySteps)
            val valLabel = (max * i / ySteps).toInt()
            sb.appendLine("""<line x1="$leftMargin" y1="$y" x2="${width - rightMargin}" y2="$y" stroke="#ddd" stroke-width="0.5"/>""")
            sb.appendLine("""<text x="${leftMargin - 8}" y="${y + 4}" text-anchor="end" font-family="sans-serif" font-size="10" fill="#999">$valLabel</text>""")
        }

        // Bars
        data.forEachIndexed { i, (label, value) ->
            val x = leftMargin + gap + i * (barW + gap)
            val h = (value / max) * chartH
            val y = topMargin + chartH - h
            val shade = barColor
            sb.appendLine("""<rect x="$x" y="$y" width="$barW" height="$h" rx="4" fill="$shade" opacity="0.85"><title>${htmlEscape(label)}: ${value.toInt()}</title></rect>""")
            sb.appendLine("""<rect x="$x" y="$y" width="$barW" height="$h" rx="4" fill="$shade" opacity="0.1"/>""")
        }

        // X-axis labels
        val labelStep = (barCount / 12f).coerceAtLeast(1f).toInt()
        data.forEachIndexed { i, (label, _) ->
            if (i % labelStep == 0 || barCount <= 12) {
                val x = leftMargin + gap + i * (barW + gap) + barW / 2
                sb.appendLine("""<text x="$x" y="${height - bottomMargin + 18}" text-anchor="end" transform="rotate(-35, $x, ${height - bottomMargin + 18})" font-family="sans-serif" font-size="9" fill="#666">${htmlEscape(label.take(15))}</text>""")
            }
        }

        sb.appendLine("</svg>")
        return sb.toString()
    }

    /**
     * Generate an SVG line/area chart for time-series data.
     */
    fun lineChartSvg(
        values: List<Float>,
        labels: List<String> = emptyList(),
        width: Int = 600,
        height: Int = 280,
        lineColor: String = "#1565C0",
        fillColor: String = "#1565C0"
    ): String {
        if (values.size < 2) return emptyChartSvg(width, height, "Need at least 2 data points")
        val max = values.max().coerceAtLeast(1f)
        val min = values.min()
        val range = (max - min).coerceAtLeast(1f)
        val leftMargin = 50f
        val rightMargin = 20f
        val topMargin = 20f
        val bottomMargin = 50f
        val chartW = width - leftMargin - rightMargin
        val chartH = height - topMargin - bottomMargin
        val stepX = chartW / (values.size - 1).coerceAtLeast(1)

        val sb = StringBuilder()
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height" viewBox="0 0 $width $height">""")
        sb.appendLine("""<defs><linearGradient id="lineBg" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#f8faf7"/><stop offset="100%" stop-color="#f0f4ec"/></linearGradient></defs>""")
        sb.appendLine("""<rect width="$width" height="$height" rx="16" fill="url(#lineBg)"/>""")

        // Gridlines
        for (i in 0..4) {
            val y = topMargin + chartH * (1f - i / 4f)
            val valLabel = (min + range * i / 4f).toInt()
            sb.appendLine("""<line x1="$leftMargin" y1="$y" x2="${width - rightMargin}" y2="$y" stroke="#ddd" stroke-width="0.5"/>""")
            sb.appendLine("""<text x="${leftMargin - 8}" y="${y + 4}" text-anchor="end" font-family="sans-serif" font-size="10" fill="#999">$valLabel</text>""")
        }

        // Area fill
        val points = values.mapIndexed { i, v ->
            val x = leftMargin + i * stepX
            val y = topMargin + chartH - ((v - min) / range) * chartH
            Pair(x, y)
        }
        val areaD = StringBuilder("M ${points[0].first},${height - bottomMargin} L ${points[0].first},${points[0].second}")
        points.drop(1).forEach { (x, y) -> areaD.append(" L $x,$y") }
        areaD.append(" L ${points.last().first},${height - bottomMargin} Z")
        sb.appendLine("""<path d="$areaD" fill="$fillColor" opacity="0.08"/>""")

        // Line
        val lineD = StringBuilder("M ${points[0].first},${points[0].second}")
        points.drop(1).forEach { (x, y) -> lineD.append(" L $x,$y") }
        sb.appendLine("""<path d="$lineD" stroke="$lineColor" stroke-width="2.5" fill="none" stroke-linejoin="round" stroke-linecap="round"/>""")

        // Dots
        points.forEach { (x, y) ->
            sb.appendLine("""<circle cx="$x" cy="$y" r="3.5" fill="white" stroke="$lineColor" stroke-width="2"/>""")
        }

        // X-axis labels (show some)
        val labelStep = (values.size / 8f).coerceAtLeast(1f).toInt()
        values.forEachIndexed { i, _ ->
            if (i % labelStep == 0 || i == values.lastIndex) {
                val x = leftMargin + i * stepX
                val l = labels.getOrElse(i) { i.toString() }
                sb.appendLine("""<text x="$x" y="${height - bottomMargin + 18}" text-anchor="middle" font-family="sans-serif" font-size="9" fill="#666">${htmlEscape(l.take(10))}</text>""")
            }
        }

        sb.appendLine("</svg>")
        return sb.toString()
    }

    private fun emptyChartSvg(width: Int, height: Int, message: String): String = """
        <svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height" viewBox="0 0 $width $height">
          <rect width="$width" height="$height" rx="16" fill="#f8faf7"/>
          <text x="${width / 2}" y="${height / 2}" text-anchor="middle" font-family="sans-serif" font-size="14" fill="#999">$message</text>
        </svg>
    """.trimIndent()

    // ══════════════════════════════════════════════════════════════════════
    //  Data aggregation helpers
    // ══════════════════════════════════════════════════════════════════════

    fun buildReportData(
        observations: List<ObservationEntity>,
        notes: List<NoteEntity> = emptyList(),
        projects: List<ProjectEntity> = emptyList(),
        sources: List<SourceEntity> = emptyList(),
        dataRecords: List<DataRecordEntity> = emptyList(),
        reports: List<ReportEntity> = emptyList(),
        title: String = "FieldMind Research Report"
    ): ReportData {
        // Category breakdown
        val categoryCounts = observations.groupBy { it.category }.mapValues { it.value.size }
            .entries.sortedByDescending { it.value }.take(10)

        // Confidence breakdown
        val confidenceCounts = observations.groupBy { it.confidenceLevel }.mapValues { it.value.size }

        // Tag extraction
        val tagCounts = observations.flatMap { obs ->
            obs.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(15)

        // Location analysis
        val topLocation = observations.map { it.manualLocation }.filter { it.isNotBlank() }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: ""

        // Date range
        val dates = observations.mapNotNull { it.date }.filter { it.isNotBlank() }.sorted()
        val dateRange = when {
            dates.size >= 2 -> "${dates.first()} — ${dates.last()}"
            dates.size == 1 -> dates.first()
            else -> "No date data"
        }

        return ReportData(
            title = title,
            overview = OverviewSection(
                totalObservations = observations.size,
                totalProjects = projects.size,
                totalSources = sources.size,
                totalNotes = notes.size,
                totalReports = reports.size,
                totalDataRecords = dataRecords.size,
                dateRange = dateRange,
                topCategories = categoryCounts,
                topTags = tagCounts,
                confidenceBreakdown = confidenceCounts,
                mostActiveLocation = topLocation
            ),
            reports = reports,
            entityTables = buildEntityTables(observations, notes, projects, sources),
            raw = RawDataSection(
                observationsCsv = FieldMindExport.observationsCsv(observations),
                dataRecordsCsv = if (dataRecords.isNotEmpty()) FieldMindExport.dataCsv(dataRecords) else "",
                sourcesCsv = if (sources.isNotEmpty()) FieldMindExport.sourcesCsv(sources) else ""
            )
        )
    }

    private fun buildEntityTables(
        observations: List<ObservationEntity>,
        notes: List<NoteEntity>,
        projects: List<ProjectEntity>,
        sources: List<SourceEntity>
    ): List<EntityTable> {
        val tables = mutableListOf<EntityTable>()

        if (observations.isNotEmpty()) {
            tables.add(EntityTable(
                title = "Observations",
                headers = listOf("Subject", "Category", "Date", "Confidence", "Location"),
                rows = observations.map { obs ->
                    listOf(
                        obs.subject.take(60),
                        obs.category,
                        "${obs.date} ${obs.time}",
                        obs.confidenceLevel,
                        obs.manualLocation.ifBlank { "—" }
                    )
                },
                totalCount = observations.size
            ))
        }

        if (notes.isNotEmpty()) {
            tables.add(EntityTable(
                title = "Notes",
                headers = listOf("Title", "Category", "Tags"),
                rows = notes.map { note ->
                    listOf(
                        note.title.take(60),
                        note.category,
                        note.tags.split(",").take(3).joinToString(", ").ifBlank { "—" }
                    )
                },
                totalCount = notes.size
            ))
        }

        if (projects.isNotEmpty()) {
            tables.add(EntityTable(
                title = "Projects",
                headers = listOf("Title", "Type", "Status"),
                rows = projects.map { proj ->
                    listOf(proj.title.take(60), proj.topicType, proj.status)
                },
                totalCount = projects.size
            ))
        }

        if (sources.isNotEmpty()) {
            tables.add(EntityTable(
                title = "Sources",
                headers = listOf("Title", "Type", "Author", "Importance"),
                rows = sources.map { src ->
                    listOf(
                        src.title.take(60),
                        src.type,
                        src.author.ifBlank { "—" }.take(30),
                        src.importance
                    )
                },
                totalCount = sources.size
            ))
        }

        return tables
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HTML Report Generator
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Generate a complete self-contained HTML report with embedded SVG charts, data tables,
     * and research overview. Includes a modern, print-ready stylesheet.
     */
    fun generateHtmlReport(
        data: ReportData,
        showRawData: Boolean = false
    ): String = buildString {
        appendLine("<!doctype html><html><head><meta charset=\"utf-8\">")
        appendLine("<title>${htmlEscape(data.title)} — FieldMind Report</title>")
        appendLine(REPORT_HTML_STYLE)
        appendLine("</head><body>")

        // ── Hero header ──
        appendLine("""
        <header class="hero">
          <div class="hero-content">
            <h1>${htmlEscape(data.title)}</h1>
            <p class="subtitle">${htmlEscape(data.subtitle)}</p>
            <p class="meta">${htmlEscape(data.author)} • ${htmlEscape(data.date)}</p>
          </div>
        </header>
        """.trimIndent())

        // ── Executive summary ──
        appendLine("<section class=\"section\">")
        appendLine("<h2>Executive Summary</h2>")
        appendLine("<p>This report summarizes <strong>${data.overview.totalObservations}</strong> observations across <strong>${data.overview.totalProjects}</strong> projects, drawing from <strong>${data.overview.totalSources}</strong> sources and <strong>${data.overview.totalNotes}</strong> notes over the period <strong>${htmlEscape(data.overview.dateRange)}</strong>.</p>")
        appendLine("</section>")

        // ── Overview stats grid ──
        appendLine("<section class=\"section\">")
        appendLine("<h2>Research Overview</h2>")
        appendLine("<div class=\"stats-grid\">")
        appendLine(statCardSvg("Observations", data.overview.totalObservations, "#2E7D32"))
        appendLine(statCardSvg("Projects", data.overview.totalProjects, "#1B5E20"))
        appendLine(statCardSvg("Sources", data.overview.totalSources, "#1565C0"))
        appendLine(statCardSvg("Notes", data.overview.totalNotes, "#6A1B9A"))
        appendLine(statCardSvg("Reports", data.overview.totalReports, "#E65100"))
        appendLine(statCardSvg("Data Records", data.overview.totalDataRecords, "#00838F"))
        appendLine("</div>")
        appendLine("</section>")

        // ── Category breakdown ──
        if (data.overview.topCategories.isNotEmpty()) {
            appendLine("<section class=\"section\">")
            appendLine("<h2>Category Breakdown</h2>")
            val catData = data.overview.topCategories.map { (cat, count) ->
                Triple(cat, count.toFloat(), categoryColor(cat))
            }
            appendLine("<div class=\"chart-container\">")
            appendLine(pieChartSvg(catData))
            appendLine("</div>")
            appendLine("</section>")
        }

        // ── Confidence distribution ──
        if (data.overview.confidenceBreakdown.isNotEmpty()) {
            appendLine("<section class=\"section\">")
            appendLine("<h2>Confidence Distribution</h2>")
            val confData = data.overview.confidenceBreakdown.map { (level, count) ->
                Triple(level, count.toFloat(), confidenceColor(level))
            }
            appendLine("<div class=\"chart-container\">")
            appendLine(pieChartSvg(confData))
            appendLine("</div>")
            appendLine("</section>")
        }

        // ── Top tags ──
        if (data.overview.topTags.isNotEmpty()) {
            appendLine("<section class=\"section\">")
            appendLine("<h2>Top Tags</h2>")
            val tagData = data.overview.topTags.map { (tag, count) -> tag to count.toFloat() }
            appendLine("<div class=\"chart-container\">")
            appendLine(barChartSvg(tagData))
            appendLine("</div>")
            appendLine("</section>")
        }

        // ── Additional charts section ──
        data.charts.forEach { chart ->
            appendLine("<section class=\"section\">")
            appendLine("<h2>${htmlEscape(chart.title)}</h2>")
            appendLine("<p class=\"chart-desc\">${htmlEscape(chart.description)}</p>")
            appendLine("<div class=\"chart-container\">")
            appendLine(chart.svgContent)
            appendLine("</div>")
            if (chart.csvData.isNotBlank()) {
                appendLine("<details class=\"raw-data\"><summary>View data</summary><pre>${htmlEscape(chart.csvData)}</pre></details>")
            }
            appendLine("</section>")
        }

        // ── Entity data tables ──
        data.entityTables.forEach { table ->
            appendLine("<section class=\"section\">")
            appendLine("<h2>${htmlEscape(table.title)} <span class=\"count-badge\">${table.totalCount}</span></h2>")
            appendLine("<div class=\"table-wrapper\">")
            appendLine("<table><thead><tr>")
            table.headers.forEach { header -> appendLine("<th>${htmlEscape(header)}</th>") }
            appendLine("</tr></thead><tbody>")
            table.rows.forEach { row ->
                appendLine("<tr>")
                row.forEach { cell -> appendLine("<td>${htmlEscape(cell)}</td>") }
                appendLine("</tr>")
            }
            appendLine("</tbody></table>")
            appendLine("</div>")
            appendLine("</section>")
        }

        // ── Reports section ──
        if (data.reports.isNotEmpty()) {
            appendLine("<section class=\"section\">")
            appendLine("<h2>Project Reports <span class=\"count-badge\">${data.reports.size}</span></h2>")
            data.reports.forEach { report ->
                val preview = FieldMindExport.buildMarkdownReport(report).take(200)
                appendLine("""
                <div class="report-card">
                  <h3>${htmlEscape(report.title)}</h3>
                  <div class="meta-row"><span class="badge">${htmlEscape(report.type)}</span><span class="badge badge-${htmlEscape(report.status.lowercase())}">${htmlEscape(report.status)}</span></div>
                  <pre class="report-preview">${htmlEscape(preview)}${if (preview.length >= 200) "..." else ""}</pre>
                </div>
                """.trimIndent())
            }
            appendLine("</section>")
        }

        // ── Raw data appendix ──
        if (showRawData && data.raw != null) {
            appendLine("<section class=\"section\">")
            appendLine("<h2>Raw Data Appendix</h2>")
            if (data.raw.observationsCsv.isNotBlank()) {
                appendLine("<details class=\"raw-data\"><summary>Observations CSV (${data.overview.totalObservations} rows)</summary><pre>${htmlEscape(data.raw.observationsCsv.take(3000))}</pre></details>")
            }
            if (data.raw.dataRecordsCsv.isNotBlank()) {
                appendLine("<details class=\"raw-data\"><summary>Data Records CSV</summary><pre>${htmlEscape(data.raw.dataRecordsCsv.take(2000))}</pre></details>")
            }
            if (data.raw.sourcesCsv.isNotBlank()) {
                appendLine("<details class=\"raw-data\"><summary>Sources CSV</summary><pre>${htmlEscape(data.raw.sourcesCsv.take(2000))}</pre></details>")
            }
            appendLine("</section>")
        }

        // ── Footer ──
        appendLine("""
        <footer class="footer">
          <p>Generated by FieldMind — ${data.overview.totalObservations + data.overview.totalProjects + data.overview.totalSources} total records</p>
          <p class="meta">FieldMind v4.3.0 — Offline research notebook</p>
        </footer>
        """.trimIndent())

        appendLine("</body></html>")
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Markdown Report Generator
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Generate a Markdown report with data tables and chart summaries.
     */
    fun generateMarkdownReport(data: ReportData): String = buildString {
        appendLine("# ${data.title}")
        appendLine()
        appendLine("*${data.author} — ${data.date}*")
        appendLine()
        appendLine("---")
        appendLine()

        // Overview
        appendLine("## Executive Summary")
        appendLine()
        appendLine("| Metric | Count |")
        appendLine("|--------|------:|")
        appendLine("| Observations | ${data.overview.totalObservations} |")
        appendLine("| Projects | ${data.overview.totalProjects} |")
        appendLine("| Sources | ${data.overview.totalSources} |")
        appendLine("| Notes | ${data.overview.totalNotes} |")
        appendLine("| Reports | ${data.overview.totalReports} |")
        appendLine("| Data Records | ${data.overview.totalDataRecords} |")
        appendLine()
        appendLine("**Date range:** ${data.overview.dateRange}")
        appendLine()

        // Categories
        if (data.overview.topCategories.isNotEmpty()) {
            appendLine("## Category Breakdown")
            appendLine()
            appendLine("| Category | Count |")
            appendLine("|----------|------:|")
            data.overview.topCategories.forEach { (cat, count) ->
                appendLine("| $cat | $count |")
            }
            appendLine()
        }

        // Confidence
        if (data.overview.confidenceBreakdown.isNotEmpty()) {
            appendLine("## Confidence Distribution")
            appendLine()
            appendLine("| Level | Count |")
            appendLine("|-------|------:|")
            data.overview.confidenceBreakdown.forEach { (level, count) ->
                appendLine("| $level | $count |")
            }
            appendLine()
        }

        // Tags
        if (data.overview.topTags.isNotEmpty()) {
            appendLine("## Most Used Tags")
            appendLine()
            appendLine("| Tag | Observations |")
            appendLine("|-----|-------------:|")
            data.overview.topTags.take(20).forEach { (tag, count) ->
                appendLine("| $tag | $count |")
            }
            appendLine()
        }

        // Entity tables
        data.entityTables.forEach { table ->
            appendLine("## ${table.title}")
            appendLine()
            if (table.totalCount > 50) {
                appendLine("*${table.totalCount} records — showing first 50*")
                appendLine()
            }
            appendLine("| ${table.headers.joinToString(" | ")} |")
            appendLine("| ${table.headers.joinToString(" | ") { "---" }} |")
            table.rows.take(50).forEach { row ->
                appendLine("| ${row.joinToString(" | ")} |")
            }
            appendLine()
        }

        // Reports
        if (data.reports.isNotEmpty()) {
            appendLine("## Reports")
            appendLine()
            data.reports.forEach { report ->
                appendLine("### ${report.title}")
                appendLine()
                appendLine("- **Type:** ${report.type} — **Status:** ${report.status}")
                appendLine()
                val body = FieldMindExport.buildMarkdownReport(report)
                if (body.isNotBlank()) {
                    appendLine(body.take(500))
                    if (body.length > 500) appendLine("*... (truncated)*")
                    appendLine()
                }
            }
        }

        appendLine("---")
        appendLine()
        appendLine("*Generated by FieldMind v4.3.0*")
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Internal helpers
    // ══════════════════════════════════════════════════════════════════════

    private val REPORT_HTML_STYLE: String = """<style>
      * { margin: 0; padding: 0; box-sizing: border-box; }
      body { font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; line-height: 1.65; color: #1a1a1a; background: #f5f7f3; padding: 0; }
      .hero { background: linear-gradient(135deg, #1B5E20 0%, #2E7D32 40%, #388E3C 100%); color: white; padding: 48px 40px; text-align: center; }
      .hero h1 { font-size: 2.2em; font-weight: 700; margin-bottom: 6px; }
      .hero .subtitle { font-size: 1.1em; opacity: 0.85; }
      .hero .meta { font-size: 0.85em; opacity: 0.65; margin-top: 4px; }
      .section { max-width: 960px; margin: 0 auto; padding: 28px 24px; }
      h2 { font-size: 1.5em; font-weight: 700; color: #1B5E20; margin-bottom: 16px; padding-bottom: 8px; border-bottom: 2px solid #2E7D3244; display: flex; align-items: center; gap: 10px; }
      h2 .count-badge { display: inline-block; background: #2E7D3218; color: #2E7D32; padding: 2px 10px; border-radius: 12px; font-size: 0.65em; font-weight: 700; }
      .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 14px; margin: 8px 0; }
      .stat-card { background: white; border-radius: 16px; padding: 20px; text-align: center; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
      .stat-value { display: block; font-size: 2.2em; font-weight: 800; line-height: 1.1; }
      .stat-label { font-size: 0.85em; color: #666; margin-top: 2px; }
      .chart-container { background: white; border-radius: 20px; padding: 16px; margin: 8px 0; box-shadow: 0 1px 4px rgba(0,0,0,0.06); overflow: hidden; }
      .chart-container svg { display: block; max-width: 100%; height: auto; }
      .chart-desc { font-size: 0.92em; color: #555; margin-bottom: 8px; }
      .table-wrapper { overflow-x: auto; background: white; border-radius: 16px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); }
      table { width: 100%; border-collapse: collapse; font-size: 0.85em; }
      th { background: #f0f4ec; color: #1B5E20; font-weight: 600; text-align: left; padding: 10px 12px; white-space: nowrap; }
      td { padding: 8px 12px; border-bottom: 1px solid #edf1e8; }
      tr:last-child td { border-bottom: none; }
      tr:hover td { background: #f8faf6; }
      .report-card { background: white; border-radius: 16px; padding: 16px; margin-bottom: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); }
      .report-card h3 { font-size: 1.05em; font-weight: 700; margin-bottom: 4px; }
      .meta-row { display: flex; gap: 6px; margin-bottom: 8px; }
      .badge { display: inline-block; background: #f0f4ec; padding: 2px 10px; border-radius: 10px; font-size: 0.78em; font-weight: 600; color: #2E7D32; }
      .badge-draft { background: #FFF8E1; color: #F57F17; }
      .badge-active { background: #E8F5E9; color: #2E7D32; }
      .badge-archived { background: #ECEFF1; color: #546E7A; }
      .report-preview { background: #f6f8f4; padding: 12px; border-radius: 10px; font-size: 0.82em; line-height: 1.5; white-space: pre-wrap; max-height: 120px; overflow-y: auto; }
      .raw-data { margin: 8px 0; }
      .raw-data summary { font-size: 0.85em; font-weight: 600; color: #2E7D32; cursor: pointer; padding: 6px 0; }
      .raw-data pre { background: #f6f8f4; padding: 12px; border-radius: 10px; font-size: 0.78em; line-height: 1.4; overflow-x: auto; max-height: 300px; }
      .footer { text-align: center; padding: 32px; font-size: 0.82em; color: #999; border-top: 1px solid #e0e4dc; margin-top: 16px; }
      .footer .meta { font-size: 0.85em; opacity: 0.7; }
      @media print { body { background: white; } .hero { -webkit-print-color-adjust: exact; print-color-adjust: exact; } .chart-container { break-inside: avoid; } .section { break-inside: avoid; } }
    </style>"""

    private fun statCardSvg(label: String, value: Int, color: String): String = """
      <div class="stat-card" style="border-left: 4px solid $color">
        <span class="stat-value" style="color: $color">$value</span>
        <span class="stat-label">$label</span>
      </div>
    """.trimIndent()

    private fun categoryColor(category: String): String = when (category.lowercase()) {
        "bird" -> "#2E7D32"; "plant" -> "#1B5E20"; "insect" -> "#E65100"; "mammal" -> "#6A1B9A"
        "weather" -> "#1565C0"; "geology" -> "#795548"; "water" -> "#00838F"; "reading insight" -> "#F57F17"
        "fungi" -> "#4E342E"; "reptile" -> "#558B2F"; else -> "#546E7A"
    }

    private fun confidenceColor(level: String): String = when (level) {
        "Sure" -> "#2E7D32"; "Reasonably sure" -> "#558B2F"; "Somewhat sure" -> "#F57F17"
        "Needs Verification" -> "#E65100"; else -> "#757575"
    }

    private fun htmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("\n", " ")
}
