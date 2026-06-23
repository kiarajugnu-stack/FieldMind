package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.theme.FieldMindColors
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ══════════════════════════════════════════════════════════════════════
//  Research Dashboard — Insights Redesign
//  Sections: Profile, Metrics, Timeseries, Category, KG, Health, Weather, Achievements, Export
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(
    viewModel: FieldMindViewModel,
    onNavigate: (FieldMindScreen) -> Unit = {},
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    val observations by viewModel.observations.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val dataRecords by viewModel.dataRecords.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val tags by viewModel.commonTags.collectAsState()
    val researchSessions by viewModel.researchSessions.collectAsState()
    val profileName by viewModel.fieldSettings.profileName.collectAsState()
    val profileRole by viewModel.fieldSettings.profileRole.collectAsState()
    val profileFocus by viewModel.fieldSettings.profileFocus.collectAsState()
    val goal by viewModel.fieldSettings.dailyObservationGoal.collectAsState()
    val colors = FieldMindTheme.colors

    // ── Computed analytics ──
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayCount = observations.count { it.date == todayKey }
    val weekCount = observations.count { it.date >= LocalDate.now().minusDays(7).toString() }

    val categoryCounts = observations.groupingBy { it.category }.eachCount().entries
        .sortedByDescending { it.value }.take(8).map { it.key to it.value.toFloat() }
    val totalCats = categoryCounts.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)

    val trend = observations.groupingBy { it.date }.eachCount().toSortedMap()
    val trendValues = trend.values.toList().takeLast(14).map { it.toFloat() }

    val confidenceParts = listOf(
        Triple("Certain", observations.count { it.confidenceLevel in listOf("Certain", "Sure") }.toFloat(), colors.confidenceSure),
        Triple("Likely", observations.count { it.confidenceLevel in listOf("Likely", "Guess") }.toFloat(), colors.confidenceGuess),
        Triple("Unsure", observations.count { it.confidenceLevel in listOf("Unsure", "Needs Verification") }.toFloat(), colors.confidenceVerify)
    ).filter { it.second > 0f }

    val mapPoints = observations.mapNotNull { o ->
        o.latitude?.let { lat -> o.longitude?.let { lon -> lat to lon } }
    }

    // ── Tags co-occurrence ──
    val tagPairs = remember(observations) {
        observations.flatMap { obs ->
            val t = obs.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
            t.flatMap { a -> t.mapNotNull { b -> if (a < b) Pair(a, b) else null } }
        }
    }

    // ── Hourly activity ──
    val hourlyActivity = remember(observations) {
        val hours = mutableMapOf<Int, Int>()
        observations.forEach { obs ->
            try {
                val h = obs.time.split(":").firstOrNull()?.toIntOrNull()
                if (h != null) hours[h] = (hours[h] ?: 0) + 1
            } catch (_: Exception) {}
        }
        hours.toMap()
    }

    // ── Day-of-week counts ──
    val dayOfWeekCounts = remember(observations) {
        val days = mutableMapOf<Int, Int>()
        observations.forEach { obs ->
            try {
                val dateParts = obs.date.split("-")
                if (dateParts.size == 3) {
                    val ld = LocalDate.of(dateParts[0].toInt(), dateParts[1].toInt(), dateParts[2].toInt())
                    val dow = ld.dayOfWeek.value // 1=Mon...7=Sun
                    days[dow] = (days[dow] ?: 0) + 1
                }
            } catch (_: Exception) {}
        }
        days.toMap()
    }

    // ── Daily values for moving average ──
    val dailyValues = remember(observations) {
        val dates = observations.groupingBy { it.date }.eachCount()
        // Fill in last 14 days with 0s for gaps
        val result = mutableMapOf<String, Int>()
        val today = LocalDate.now()
        (0 until 14).forEach { i ->
            val date = today.minusDays(i.toLong()).toString()
            result[date] = dates[date] ?: 0
        }
        result.toMap()
    }

    // ── Weather correlation data ──
    val weatherData = remember(observations) {
        observations.mapNotNull { o ->
            o.weatherTemperature?.let { temp -> (temp.toFloat() to 1f) }
        }.take(50)
    }
    // If we have weather temps aggregated with counts, make correlation plot
    val weatherCorrelation = remember(weatherData) {
        weatherData.groupBy({ it.first.toInt() }, { it.second }).map { (temp, counts) ->
            temp.toFloat() to counts.size.toFloat()
        }.sortedBy { it.first }
    }

    // ── Knowledge graph ──
    val graphData = remember(observations, questions, hypotheses, projects, sources) {
        val nodes = mutableListOf<GraphNode>()
        val edges = mutableListOf<Pair<Int, Int>>()
        val nodeTimestamps = mutableListOf<Long>()
        val index = HashMap<String, Int>()
        fun add(key: String, label: String, color: Color, emphasis: Boolean, time: Long = System.currentTimeMillis()): Int =
            index.getOrPut(key) { nodes.add(GraphNode(label, color, emphasis)); nodeTimestamps.add(time); nodes.size - 1 }
        projects.forEach { p -> add("p:${p.id}", p.title, colors.project, true, p.createdAt) }
        questions.filter { it.relatedProjectId != null }.forEach { q ->
            val pIdx = index["p:${q.relatedProjectId}"]
            if (pIdx != null) edges.add(add("q:${q.id}", q.questionText, colors.question, false, q.createdAt) to pIdx)
        }
        observations.filter { it.projectId != null }.forEach { o ->
            val pIdx = index["p:${o.projectId}"]
            if (pIdx != null) edges.add(add("o:${o.id}", o.subject, colors.observation, false, o.timestamp) to pIdx)
        }
        sources.filter { it.relatedProjectId != null }.forEach { s ->
            val pIdx = index["p:${s.relatedProjectId}"]
            if (pIdx != null) edges.add(add("s:${s.id}", s.title, colors.source, false, s.createdAt) to pIdx)
        }
        hypotheses.filter { it.linkedQuestionId != null }.forEach { h ->
            val qIdx = index["q:${h.linkedQuestionId}"]
            if (qIdx != null) edges.add(add("h:${h.id}", h.prediction, colors.hypothesis, false, h.createdAt) to qIdx)
        }
        Triple(nodes.toList(), edges.toList(), nodeTimestamps.toList())
    }

    // ── Data quality score ──
    val dataQualityScore = remember(observations, questions, hypotheses, sources, tags) {
        if (observations.isEmpty()) return@remember Triple(0, emptyList<Pair<String, Float>>(), "No data yet")
        val obsSize = observations.size.coerceAtLeast(1)
        val withEvidence = observations.count { it.evidenceSummary.isNotBlank() }.toFloat() / obsSize
        val withQuestions = if (observations.isEmpty()) 0f else questions.size.toFloat() / obsSize
        val withHypotheses = if (questions.isEmpty()) 1f else hypotheses.size.toFloat() / questions.size.coerceAtLeast(1)
        val withTags = if (observations.isEmpty()) 0f else tags.size.toFloat() / obsSize.coerceAtMost(10)
        val withGps = if (observations.isEmpty()) 0f else observations.count { it.latitude != null }.toFloat() / obsSize
        val metrics = listOf(
            "Evidence" to withEvidence.coerceIn(0f, 1f),
            "Questions/Obs" to withQuestions.coerceIn(0f, 1f),
            "Hypotheses/Qs" to withHypotheses.coerceIn(0f, 1f),
            "Tags" to withTags.coerceIn(0f, 1f),
            "GPS" to withGps.coerceIn(0f, 1f)
        )
        val score = (metrics.sumOf { it.second.toDouble() } / metrics.size * 100).toInt()
        Triple(score, metrics, "Based on ${observations.size} observations")
    }

    // ── Achievements ──
    val achievements = remember(observations, questions, projects, sources, reports, notes, dataRecords, flashcards, researchSessions) {
        listOf(
            ResearchAchievement("First Observation", "Save your first field observation", FieldMindIcons.Observation, colors.observation, observations.size, 1),
            ResearchAchievement("7-Day Observer", "Log observations across 7 different days", FieldMindIcons.Streak, colors.warning, observations.map { it.date }.distinct().size, 7),
            ResearchAchievement("Evidence Collector", "Attach evidence to 10 observations", FieldMindIcons.Camera, colors.observation, observations.count { it.evidenceSummary.isNotBlank() }, 10),
            ResearchAchievement("Source Curator", "Save 5 sources with metadata", FieldMindIcons.Source, colors.source, sources.size, 5),
            ResearchAchievement("Question Builder", "Write 5 research questions", FieldMindIcons.Question, colors.question, questions.size, 5),
            ResearchAchievement("Project Starter", "Create a research project", FieldMindIcons.Project, colors.project, projects.size, 1),
            ResearchAchievement("Data Logger", "Record 10 data entries", FieldMindIcons.Data, colors.data, dataRecords.size, 10),
            ResearchAchievement("Report Writer", "Draft a research report", FieldMindIcons.Report, colors.report, reports.size, 1),
            ResearchAchievement("Note Maker", "Save 10 free-form notes", FieldMindIcons.Note, colors.source, notes.size, 10),
            ResearchAchievement("Flashcard Master", "Create 20 flashcards", FieldMindIcons.Flashcard, colors.flashcard, flashcards.size, 20),
            ResearchAchievement("Session Runner", "Complete 3 research sessions", FieldMindIcons.Session, colors.positive, researchSessions.count { it.status == "Completed" }, 3),
            ResearchAchievement("Map Explorer", "Tag 15 observations with GPS", FieldMindIcons.Map, colors.info, observations.count { it.latitude != null }, 15),
            ResearchAchievement("Streak Master", "Maintain a 30-day streak", FieldMindIcons.Streak, colors.confidenceSure, observations.map { it.date }.distinct().size, 30),
            ResearchAchievement("Hypothesis Tester", "Create 5 hypotheses", FieldMindIcons.Hypothesis, colors.hypothesis, hypotheses.size, 5),
            ResearchAchievement("Century Observer", "Log 100 observations", FieldMindIcons.Observation, colors.observation, observations.size, 100)
        )
    }

    val scope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }

    // ── Calculate today's daily calendar map ──
    val calendarData = remember(observations) {
        observations.groupingBy { it.date }.eachCount().mapKeys { (dateStr, _) ->
            try {
                val parts = dateStr.split("-")
                LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } catch (_: Exception) { LocalDate.now() }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            val insightsScrollState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
            LazyColumn(
                state = insightsScrollState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // ═══════════ SECTION 1: Header & Profile ═══════════
            item {
                StandardScreenHeader(
                    title = "Research Dashboard",
                    subtitle = "Your research at a glance — ${if (observations.isEmpty()) "start capturing to see analytics" else "${observations.size} observations analyzed"}",
                    icon = FieldMindIcons.Insights,
                    trailing = {
                        BackButton(onClick = { onNavigate(FieldMindScreen.Search) }, icon = FieldMindIcons.Search, contentDescription = "Search")
                    }
                )
            }

            item { ResearchProfileCard(profileName, profileRole, profileFocus, todayCount, weekCount, goal) }

            if (observations.isEmpty()) {
                item {
                    EmptyState(
                        "No data yet",
                        "Insights, charts, and your offline map appear as you log observations. Start capturing to build your research dashboard.",
                        icon = FieldMindIcons.Insights,
                        actionLabel = "Capture first observation"
                    ) { onNavigate(FieldMindScreen.Observe) }
                }
                return@LazyColumn
            }

            // ═══════════ SECTION 2: Research Journey Metrics ═══════════
            item {
                SectionHeader("Research journey", "Question → observations → patterns → findings")
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        "Observations", observations.size.toString(),
                        FieldMindIcons.Observation, Modifier.weight(1f), colors.observation,
                        trend = if (weekCount > 0) "+$weekCount this week" else null,
                        onClick = { onNavigate(FieldMindScreen.Observe) }
                    )
                    MetricTile(
                        "Questions", "${questions.count { it.status != "Answered" }}/${questions.size}",
                        FieldMindIcons.Question, Modifier.weight(1f), colors.question,
                        onClick = { onNavigate(FieldMindScreen.Questions) }
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        "Projects", projects.size.toString(),
                        FieldMindIcons.Project, Modifier.weight(1f), colors.project,
                        onClick = { onNavigate(FieldMindScreen.Projects) }
                    )
                    MetricTile(
                        "Reports", reports.size.toString(),
                        FieldMindIcons.Report, Modifier.weight(1f), colors.report,
                        onClick = { onNavigate(FieldMindScreen.Reports) }
                    )
                }
            }

            // ═══════════ SECTION 3: Time-Series Analytics ═══════════
            item { SectionHeader("Time-series analytics", "When and how often you capture") }

            // Calendar heatmap
            if (calendarData.isNotEmpty()) {
                item {
                    InsightCard("Activity calendar", FieldMindIcons.Calendar) {
                        CalendarHeatmap(
                            dailyCounts = calendarData,
                            accentColor = colors.observation,
                            onTapDay = { _, _ -> }
                        )
                    }
                }
            }

            // Activity by hour + Day of week as readable rankings
            item { InsightCard("Most active hours", FieldMindIcons.Timer) { HorizontalActivityRanking(hourlyActivity.mapKeys { "%02d:00".format(it.key) }, colors.warning) } }
            item { InsightCard("Most active days", FieldMindIcons.Calendar) { HorizontalActivityRanking(dayOfWeekCounts.mapKeys { dayName(it.key) }, colors.info) } }


            // Moving average trend
            if (trendValues.size >= 2) {
                item {
                    InsightCard("Daily trend (14-day)", FieldMindIcons.Trend) {
                        MovingAverageChart(dailyValues, barColor = colors.observation.copy(alpha = 0.3f), lineColor = colors.positive)
                    }
                }
            }

            // ═══════════ SECTION 4: Category & Tag Analytics ═══════════
            item { SectionHeader("Categories & tags", "What you research most") }

            if (categoryCounts.isNotEmpty()) {
                item { InsightCard("Category ranking", FieldMindIcons.Data) { CategoryRanking(categoryCounts, colors) } }
                if (categoryCounts.size >= 3) item { CollapsibleRadar(categoryCounts, totalCats, colors) }
            }


            // Confidence breakdown
            if (confidenceParts.isNotEmpty()) {
                item { ResearchConfidenceCard(confidenceParts) }
            }

            // Tag co-occurrence matrix
            if (tagPairs.isNotEmpty()) {
                item {
                    InsightCard("Tag co-occurrence", FieldMindIcons.Tag) {
                        TagCoOccurrenceMatrix(tagPairs, accentColor = colors.info)
                    }
                }
            }

            // Tags list
            if (tags.isNotEmpty()) {
                item { SectionHeader("Top tags", "${tags.size} total") }
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tags.take(10).forEach { tag ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    TagChip(tag.name)
                                    Text(tag.observationCount.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (tag != tags.last()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }

            // ═══════════ SECTION 5: Knowledge Graph ═══════════
            item { SectionHeader("Knowledge graph", "${graphData.first.size} connected entities") }
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NetworkGraphTimeline(
                            allNodes = graphData.first,
                            allEdges = graphData.second,
                            nodeTimestamps = graphData.third,
                            edgeColor = colors.project.copy(alpha = 0.4f)
                        )
                        // Filter chips
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            GraphLegend("Projects", colors.project)
                            GraphLegend("Questions", colors.question)
                            GraphLegend("Observations", colors.observation)
                            GraphLegend("Sources", colors.source)
                            GraphLegend("Hypotheses", colors.hypothesis)
                        }
                    }
                }
            }

            // OSM Map
            if (mapPoints.isNotEmpty()) {
                item {
                    InsightCard("Field map (${mapPoints.size} points)", FieldMindIcons.Map) {
                        OsmMapView(
                            points = mapPoints,
                            showEmptyState = false,
                            modifier = Modifier.fillMaxWidth().height(250.dp)
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onNavigate(FieldMindScreen.MapScreen) }) {
                                Text("Open full map")
                                Spacer(Modifier.size(4.dp))
                                Icon(icon = FieldMindIcons.Forward, contentDescription = null, size = 18.dp)
                            }
                        }
                    }
                }
            }

            // ═══════════ SECTION 6: Research Health ═══════════
            item { ResearchHealthSummary(dataQualityScore, observations, questions, hypotheses, colors) }


            // ═══════════ SECTION 7: Weather Integration ═══════════
            if (weatherCorrelation.size >= 3) {
                item { SectionHeader("Weather correlation", "Temperature vs observations") }
                item {
                    InsightCard("Temperature correlation", FieldMindIcons.Weather) {
                        WeatherCorrelationChart(
                            dataPoints = weatherCorrelation,
                            pointColor = colors.observation,
                            trendColor = colors.positive
                        )
                    }
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon = FieldMindIcons.Weather, contentDescription = null, tint = colors.info, size = 22.dp)
                            Column(Modifier.weight(1f)) {
                                Text("Weather correlation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Enable weather + GPS on capture to see temperature trends", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ═══════════ SECTION 8: Achievements ═══════════
            item { SectionHeader("Achievements", "${achievements.count { it.unlocked }}/${achievements.size} unlocked") }
            item { CollapsibleAchievements(achievements, snackbarState, scope) }

            // ═══════════ SECTION 9: Open Questions & Active Projects ═══════════
            if (questions.isNotEmpty()) {
                item { SectionHeader("Open questions", "${questions.count { it.status != "Answered" }} unanswered") }
                itemsIndexed(questions.filter { it.status != "Answered" }.take(5)) { i, q ->
                    EntityCard(q.questionText, "question", meta = listOf(q.status, q.priority), onClick = { onOpenDetail("question", q.id) }, index = i, animate = true)
                }
            }
            if (projects.isNotEmpty()) {
                item { SectionHeader("Active projects", "${projects.count { it.status == "Active" }} active") }
                itemsIndexed(projects.filter { it.status == "Active" }.take(4)) { i, p ->
                    EntityCard(p.title, "project", body = p.objective.ifBlank { p.researchQuestion }, meta = listOf(p.status, p.topicType), onClick = { onOpenDetail("project", p.id) }, index = i, animate = true)
                }
            }

            // ═══════════ Data Records Table ═══════════
            if (dataRecords.isNotEmpty()) {
                item { SectionHeader("Data records", "${dataRecords.size} entries") }
                itemsIndexed(dataRecords.take(8)) { _, record -> DataRecordInsightCard(record, onClick = { onOpenDetail("data", record.id) }) }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }

            // ── Top snackbar overlay for achievements ──
            FieldMindSnackbarOverlay(
                hostState = snackbarState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}

@Composable
private fun ResearchJourneyCard(observations: List<ObservationEntity>, questions: List<QuestionEntity>, hypotheses: List<HypothesisEntity>, projects: List<ProjectEntity>) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Research journey", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Question → observations → patterns → hypothesis → findings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("${questions.size} questions") }, leadingIcon = { Icon(FieldMindIcons.Question, null, size = 16.dp) })
                AssistChip(onClick = {}, label = { Text("${observations.size} observations") }, leadingIcon = { Icon(FieldMindIcons.Observation, null, size = 16.dp) })
                AssistChip(onClick = {}, label = { Text("${hypotheses.size} hypotheses") }, leadingIcon = { Icon(FieldMindIcons.Hypothesis, null, size = 16.dp) })
                AssistChip(onClick = {}, label = { Text("${projects.size} projects") }, leadingIcon = { Icon(FieldMindIcons.Project, null, size = 16.dp) })
            }
        }
    }
}

@Composable
private fun HorizontalActivityRanking(values: Map<String, Int>, accent: Color) {
    val ranked = values.entries.sortedByDescending { it.value }.take(7)
    val max = ranked.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ranked.isEmpty()) Text("No activity yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ranked.forEach { (label, count) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(label, modifier = Modifier.width(72.dp), style = MaterialTheme.typography.labelMedium)
                Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Box(Modifier.fillMaxWidth(count.toFloat() / max).fillMaxHeight().background(accent, RoundedCornerShape(99.dp)))
                }
                Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun dayName(day: Int): String = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").getOrElse(day) { "Day $day" }

@Composable
private fun CategoryRanking(categoryCounts: List<Pair<String, Float>>, colors: FieldMindColors) {
    val max = categoryCounts.maxOfOrNull { it.second }?.coerceAtLeast(1f) ?: 1f
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        categoryCounts.forEach { (category, count) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(category, modifier = Modifier.width(112.dp), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Box(Modifier.fillMaxWidth(count / max).fillMaxHeight().background(colors.categoryColor(category), RoundedCornerShape(99.dp)))
                }
                Text(count.toInt().toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CollapsibleRadar(categoryCounts: List<Pair<String, Float>>, totalCats: Float, colors: FieldMindColors) {
    var expanded by remember { mutableStateOf(false) }
    InsightCard("Category radar", FieldMindIcons.Graph) {
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide radar" else "Show radar") }
        if (expanded) RadarChart(categoryCounts.map { (label, count) -> label to (count / totalCats) }, accentColor = colors.observation, height = 180.dp)
    }
}

@Composable
private fun ResearchConfidenceCard(parts: List<Triple<String, Float, Color>>) {
    val total = parts.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)
    val certain = parts.firstOrNull { it.first == "Certain" }?.second ?: 0f
    val score = ((certain / total) * 100).toInt()
    InsightCard("Research confidence", FieldMindIcons.Check) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("$score%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = FieldMindTheme.colors.confidenceSure)
            Column(Modifier.weight(1f)) {
                Text(if (score >= 70) "Strong evidence base" else "Needs stronger evidence", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                BreakdownBar(parts)
            }
        }
    }
}

@Composable
private fun ResearchHealthSummary(dataQualityScore: Triple<Int, List<Pair<String, Float>>, String>, observations: List<ObservationEntity>, questions: List<QuestionEntity>, hypotheses: List<HypothesisEntity>, colors: FieldMindColors) {
    val issues = listOfNotNull(
        "Add more evidence".takeIf { observations.count { it.evidenceSummary.isNotBlank() } < observations.size },
        "Enable GPS collection".takeIf { observations.any { it.latitude == null } },
        "Link questions to hypotheses".takeIf { questions.any { q -> hypotheses.none { h -> h.linkedQuestionId == q.id } } },
        "Add weather to observations".takeIf { observations.any { it.weatherTemperature == null } }
    )
    InsightCard("Research health", FieldMindIcons.Alert) {
        Text("${issues.size} issues need attention", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (issues.isEmpty()) colors.positive else colors.warning)
        issues.take(4).forEach { Text("⚠ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        DataQualityMeter(score = dataQualityScore.first, metrics = dataQualityScore.second, accentColor = if (dataQualityScore.first >= 80) colors.positive else if (dataQualityScore.first >= 50) colors.warning else MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun DataRecordInsightCard(record: DataRecordEntity, onClick: () -> Unit) {
    EntityCard(record.label.ifBlank { record.toolType }, "data", body = "Value: ${record.value} ${record.unit}\nNotes: ${record.notes}", meta = listOf(record.toolType, SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(record.timestamp))), onClick = onClick, animate = true)
}

// ══════════════════════════════════════════════════════════════════════
//  Research Profile Card
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ResearchProfileCard(name: String, role: String, focus: String, todayCount: Int, weekCount: Int, goal: Int) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(FieldMindIcons.Insights, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 28.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (name.isBlank()) "Researcher" else name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("$role • $focus", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WeeklyStat("Today", todayCount.toString(), FieldMindIcons.Today)
                    WeeklyStat("This week", weekCount.toString(), FieldMindIcons.Trend)
                    if (goal > 0) WeeklyStat("Daily goal", "$todayCount/$goal", FieldMindIcons.Streak)
                }
            }
        }
    }
}

@Composable
private fun WeeklyStat(label: String, value: String, icon: MaterialSymbolIcon) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), size = 12.dp)
        Text("$label: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Achievement System
// ══════════════════════════════════════════════════════════════════════

data class ResearchAchievement(
    val title: String,
    val description: String,
    val icon: MaterialSymbolIcon,
    val accent: Color,
    val progress: Int,
    val target: Int
) {
    val unlocked: Boolean get() = progress >= target
    val fraction: Float get() = (progress.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
}

@Composable
private fun CollapsibleAchievements(
    items: List<ResearchAchievement>,
    snackbarState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var expanded by remember { mutableStateOf(false) }
    val unlockedCount = items.count { it.unlocked }
    val context = LocalContext.current

    // Track first-time unlocks
    LaunchedEffect(unlockedCount) {
        if (unlockedCount > 0) {
            val prefs = context.getSharedPreferences("fieldmind_achievements_v2", 0)
            items.filter { it.unlocked && !prefs.getBoolean(it.title, false) }.forEach { a ->
                showFastSnackbar(snackbarState, scope, "🏆 ${a.title} unlocked!")
                prefs.edit().putBoolean(a.title, true).apply()
            }
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.pressScale(scaleDown = 0.98f).clickable { expanded = !expanded }.animateContentSize()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(FieldMindIcons.Streak, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                Column(Modifier.weight(1f)) {
                    Text("Achievements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$unlockedCount/${items.size} unlocked • tap to ${if (expanded) "collapse" else "explore"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) FieldMindIcons.Up else FieldMindIcons.Down, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
            }
            if (expanded) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                    items.forEach { achievement -> AchievementCardV2(achievement, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun AchievementCardV2(item: ResearchAchievement, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(targetValue = item.fraction, animationSpec = tween(600), label = "achieve")
    Card(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.unlocked) item.accent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                        .background(item.accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.3f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, null, tint = if (item.unlocked) item.accent else item.accent.copy(alpha = 0.6f), size = 20.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.unlocked) item.accent else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (item.unlocked) "Unlocked 🎉" else "${item.progress}/${item.target}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.unlocked) item.accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)),
                color = item.accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Shared helpers
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun GraphLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InsightCard(title: String, icon: MaterialSymbolIcon, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}
