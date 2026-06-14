package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.learn.LearnResource
import fieldmind.research.app.features.field.data.learn.LearnLibrary
import fieldmind.research.app.features.field.data.stats.FieldMindStreaks
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

// ══════════════════════════════════════════════════════════════════════
//  Today (Home) — Redesigned with Hero Section & Research Pulse
// ══════════════════════════════════════════════════════════════════════

@Composable
fun HomeScreen(
    viewModel: FieldMindViewModel,
    onOpenSettings: () -> Unit,
    onNavigate: (FieldMindScreen) -> Unit,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    onOpenReader: (String, String) -> Unit = { _, _ -> }
) {
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val goal by viewModel.fieldSettings.dailyObservationGoal.collectAsState()
    val streaksEnabled by viewModel.fieldSettings.streaksEnabled.collectAsState()
    val todayKey = remember { today() }
    val todayCount = observations.count { it.date == todayKey }
    val currentStreak = remember(observations, streaksEnabled) { if (streaksEnabled) FieldMindStreaks.currentStreakDays(observations.map { it.date }) else 0 }
    val activeProject = projects.firstOrNull { it.status == "Active" } ?: projects.firstOrNull()
    val learnSignals = remember(observations, questions, projects) {
        buildList {
            observations.sortedByDescending { it.timestamp }.take(10).forEach { add(it.category); add(it.subject); add(it.tags) }
            questions.sortedByDescending { it.updatedAt }.take(10).forEach { add(it.category); add(it.questionText) }
            projects.take(6).forEach { add(it.topicType); add(it.title) }
        }
    }
    val researchSessions by viewModel.researchSessions.collectAsState()
    val recommendations = remember(learnSignals) { recommendedResources(learnSignals) }

    val lastSession = remember(researchSessions) {
        researchSessions.filter { it.status == "Completed" }.maxByOrNull { it.endedAt ?: it.createdAt }
    }


    val weatherObs = remember(observations) { observations.firstOrNull { it.weatherTemperature != null } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // ── Hero Section ──
        item { HomeHeroSection(todayCount, goal, currentStreak, observations.size, questions.size, onOpenSettings, onNavigate) }

        // ── Weather Card (when observations have weather data) ──
        if (weatherObs != null) {
            item { WeatherStatusCard(observations, viewModel, onNavigate) }
        }

        // ── Daily Goal ──
        item { DailyGoalCard(todayCount, goal, currentStreak) { onNavigate(FieldMindScreen.Observe) } }

        // ── Research Session CTA ──
        item {
            val activeSession = remember(researchSessions) { researchSessions.firstOrNull { it.status == "Active" } }
            var timerMs by remember { mutableStateOf(0L) }
            
            LaunchedEffect(activeSession) {
                if (activeSession != null) {
                    while (true) {
                        timerMs = System.currentTimeMillis() - (activeSession.createdAt)
                        delay(100)
                    }
                }
            }
            
            ResearchSessionCtaCard(
                lastSessionLabel = if (lastSession != null) "Resume your last session" else null,
                activeSessionName = activeSession?.name,
                timerMs = timerMs,
                onStartSession = { onNavigate(FieldMindScreen.ResearchSession) }
            )
        }

        // ── Widget Grid ──
        item { SectionHeader("Research areas", "Quick overview of your work") }
        item { HomeWidgetGrid(observations, notes, questions, sources, projects, reports, data) { onNavigate(it) } }
        item { HomeDataOptionsCard(data) { onNavigate(FieldMindScreen.DataTools) } }
        
        // ── Recent Captures ──
        if (observations.isNotEmpty()) {
            item { RecentCapturesCard(observations, onOpenDetail) }
        }

        // ── Learning & Reading ──
        item { RecommendedLearningCard(recommendations, onOpenReader, onSeeAll = { onNavigate(FieldMindScreen.Learn) }) }
        item { ReadingReviewCard(sources, flashcards, onNavigate) }

        // ── Observation Timeline ──
        item {
            ObservationTimelinePreview(
                observations = observations.sortedByDescending { it.timestamp },
                notes = notes.sortedByDescending { it.updatedAt },
                onOpenDetail = onOpenDetail
            )
        }

        // ── Current Project ──
        if (activeProject != null) {
            item {
                CurrentProjectResearchCard(
                    project = activeProject,
                    observations = observations,
                    sources = sources,
                    data = data,
                    hypotheses = hypotheses,
                    reports = reports,
                    onOpen = { onOpenDetail("project", activeProject.id) }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Hero Section — Welcome + Animated Stats + Settings
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun HomeHeroSection(
    todayCount: Int,
    goal: Int,
    streakDays: Int,
    totalObs: Int,
    totalQuestions: Int,
    onOpenSettings: () -> Unit,
    onNavigate: (FieldMindScreen) -> Unit
) {
    val colors = FieldMindTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.positive.copy(alpha = if (colors.isDark) 0.34f else 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.Nature, null, tint = colors.positive, size = 34.dp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "FieldMind",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Observe. Question. Research clearly.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 0.dp,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(FieldMindIcons.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 28.dp)
                    }
                }
            }

            Text(
                if (totalObs == 0) "Start your first observation to begin your research story."
                else "You have $totalObs observation${if (totalObs != 1) "s" else ""} across your research.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroActionChip(
                    icon = FieldMindIcons.Camera,
                    label = "Capture",
                    accent = colors.observation,
                    modifier = Modifier.weight(1f)
                ) { onNavigate(FieldMindScreen.Observe) }
                HeroActionChip(
                    icon = FieldMindIcons.Note,
                    label = "Note",
                    accent = colors.source,
                    modifier = Modifier.weight(1f)
                ) { onNavigate(FieldMindScreen.Library) }
            }
        }
    }
}

@Composable
private fun HeroStatBubble(
    value: String,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    // Simple count-up animation
    val animatedValue = remember { Animatable(0f) }
    val targetFloat = value.toFloatOrNull() ?: 0f
    LaunchedEffect(targetFloat) {
        animatedValue.animateTo(
            targetValue = targetFloat,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.1f),
        tonalElevation = 0.dp
    ) {
        Column(
            Modifier.padding(16.dp, 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                if (value.toIntOrNull() != null) animatedValue.value.roundToInt().toString() else value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeroActionChip(
    icon: MaterialSymbolIcon,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = rememberFieldMindHaptics()
    Surface(
        modifier = modifier,
        onClick = { haptics.light(); onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = accent, size = 20.dp)
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Weather Status Card — Current conditions + weather correlation
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun WeatherStatusCard(
    observations: List<ObservationEntity>,
    viewModel: FieldMindViewModel,
    onNavigate: (FieldMindScreen) -> Unit
) {
    val colors = FieldMindTheme.colors
    val weatherObs = remember(observations) {
        observations.filter { it.weatherTemperature != null }.sortedByDescending { it.timestamp }
    }
    val lastWeather = weatherObs.firstOrNull()
    val tempRange = remember(weatherObs) {
        val temps = weatherObs.mapNotNull { it.weatherTemperature }
        if (temps.isEmpty()) null else (temps.minOrNull() to temps.maxOrNull())
    }
    val weatherSummary = remember(weatherObs) {
        val conditions = weatherObs.map { it.weatherCondition }.filter { it.isNotBlank() }.distinct()
        conditions.take(3).joinToString(" • ")
    }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                        .background(colors.info.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(FieldMindIcons.Weather, null, tint = colors.info, size = 24.dp) }
                Column(Modifier.weight(1f)) {
                    Text("Weather observations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${weatherObs.size} observations with weather data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onNavigate(FieldMindScreen.WeatherDatabase) }) { Text("View all") }
            }

            if (lastWeather != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Temperature
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${lastWeather.weatherTemperature?.let { "%.0f°".format(it) } ?: "--"}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.info
                        )
                        Text("Latest temp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (tempRange != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${tempRange.first?.let { "%.0f".format(it) } ?: "--"}° — ${tempRange.second?.let { "%.0f".format(it) } ?: "--"}°",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.warning
                            )
                            Text("Range", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    // Humidity
                    lastWeather.weatherHumidity?.let { hum ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$hum%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.data)
                            Text("Humidity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (weatherSummary.isNotBlank()) {
                InfoChip(weatherSummary, icon = FieldMindIcons.Weather, color = colors.info)
            }

            if (lastWeather?.manualLocation.isNullOrBlank()) {
                Text("Weather captured across multiple locations",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Weather at ${lastWeather?.manualLocation ?: "current location"}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun timeOfDay(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        else -> "evening"
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Research Pulse Card — Animated snapshot of recent activity
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ResearchPulseCard(
    observations: List<ObservationEntity>,
    questions: List<QuestionEntity>,
    projects: List<ProjectEntity>,
    onNavigate: (FieldMindScreen) -> Unit
) {
    val colors = FieldMindTheme.colors
    val weekObs = observations.count { it.date >= LocalDate.now().minusDays(7).toString() }
    val openQs = questions.count { it.status != "Answered" }
    val activeProjects = projects.count { it.status == "Active" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.observation.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.Streak, null, tint = colors.observation, size = 24.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Research pulse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Your 7-day research activity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onNavigate(FieldMindScreen.Insights) }) { Text("Details") }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PulseMetric(
                    value = "$weekObs",
                    label = "Obs / week",
                    accent = colors.observation,
                    icon = FieldMindIcons.Observation
                )
                PulseMetric(
                    value = "$openQs",
                    label = "Open Qs",
                    accent = colors.question,
                    icon = FieldMindIcons.Question
                )
                PulseMetric(
                    value = "$activeProjects",
                    label = "Projects",
                    accent = colors.project,
                    icon = FieldMindIcons.Project
                )
            }
        }
    }
}

@Composable
private fun PulseMetric(
    value: String,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    icon: MaterialSymbolIcon
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.2f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, size = 22.dp)
        }
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = accent)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Quick Actions Row
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun QuickActionsRow(onNavigate: (FieldMindScreen) -> Unit) {            SectionHeader("Quick actions", "Map, Export, Search, Flashcards")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { QuickActionChip("Map", FieldMindIcons.Map, FieldMindTheme.colors.info, FieldMindScreen.MapScreen, onNavigate) }
        item { QuickActionChip("Export", FieldMindIcons.Export, FieldMindTheme.colors.report, FieldMindScreen.ExportStudio, onNavigate) }
        item { QuickActionChip("Search", FieldMindIcons.Search, FieldMindTheme.colors.question, FieldMindScreen.Search, onNavigate) }
        item { QuickActionChip("Review", FieldMindIcons.Flashcard, FieldMindTheme.colors.flashcard, FieldMindScreen.Learn, onNavigate) }
        item { QuickActionChip("Insights", FieldMindIcons.Insights, FieldMindTheme.colors.data, FieldMindScreen.Insights, onNavigate) }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: MaterialSymbolIcon,
    accent: androidx.compose.ui.graphics.Color,
    screen: FieldMindScreen,
    onNavigate: (FieldMindScreen) -> Unit
) {
    val haptics = rememberFieldMindHaptics()
    Surface(
        onClick = { haptics.light(); onNavigate(screen) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(36.dp)
                    .background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = accent, size = 20.dp) }
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Reading Review Card
// ═════════════════════════════════��════════════════════════════════════

@Composable
private fun ReadingReviewCard(sources: List<SourceEntity>, flashcards: List<FlashcardEntity>, onNavigate: (FieldMindScreen) -> Unit) {
    val current = sources.firstOrNull { it.readingStatus != "Read" } ?: sources.firstOrNull()
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(FieldMindIcons.Book, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                Column(Modifier.weight(1f)) {
                    Text("Reading & review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Continue a source, then turn ideas into memory.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniActionTile("Reading", current?.title ?: "Add a source", current?.readingStatus ?: "No source yet", FieldMindIcons.Source, Modifier.weight(1f)) { onNavigate(FieldMindScreen.Library) }
                MiniActionTile("Review", "${flashcards.size} cards", if (flashcards.isEmpty()) "Create cards" else "Start session", FieldMindIcons.Flashcard, Modifier.weight(1f)) { onNavigate(FieldMindScreen.Learn) }
            }
        }
    }
}

@Composable
private fun MiniActionTile(title: String, value: String, subtitle: String, icon: MaterialSymbolIcon, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ObservationTimelinePreview(
    observations: List<ObservationEntity>,
    notes: List<NoteEntity>,
    onOpenDetail: (String, Long) -> Unit
) {
    val events = buildList {
        observations.take(8).forEach { add(TimelinePreviewEvent("observation", it.id, it.date, it.time, it.subject.ifBlank { "Observation" }, it.category)) }
        notes.take(4).forEach { note ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(note.updatedAt))
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
            add(TimelinePreviewEvent("note", note.id, date, time, note.title.ifBlank { "Untitled note" }, "Journal"))
        }
    }.sortedWith(compareByDescending<TimelinePreviewEvent> { it.date }.thenByDescending { it.time }).take(8)

    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(FieldMindIcons.Calendar, null, tint = FieldMindTheme.colors.project, size = 22.dp)
                Column(Modifier.weight(1f)) {
                    Text("Observation timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Date-grouped research story preview", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (events.isEmpty()) {
                Text("Your timeline will group observations and notes by day.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                events.groupBy { it.date }.entries.take(3).forEach { (date, dayEvents) ->
                    Text(date, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = FieldMindTheme.colors.project)
                    dayEvents.take(3).forEach { event ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onOpenDetail(event.kind, event.id) }.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (event.kind == "note") FieldMindTheme.colors.source else FieldMindTheme.colors.observation))
                            Column(Modifier.weight(1f)) {
                                Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${event.time} • ${event.meta}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class TimelinePreviewEvent(val kind: String, val id: Long, val date: String, val time: String, val title: String, val meta: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrentProjectResearchCard(
    project: ProjectEntity,
    observations: List<ObservationEntity>,
    sources: List<SourceEntity>,
    data: List<DataRecordEntity>,
    hypotheses: List<HypothesisEntity>,
    reports: List<ReportEntity>,
    onOpen: () -> Unit
) {
    val projectObservations = observations.filter { it.projectId == project.id }
    val connectedObservations = projectObservations.size
    val evidenceCount = projectObservations.count { it.evidenceSummary.isNotBlank() } + project.attachmentUris.split(",").count { it.trim().isNotBlank() }
    val connectedData = data.count { it.projectId == project.id }
    val connectedSources = sources.count { it.relatedProjectId == project.id }
    val connectedReports = reports.count { it.projectId == project.id }

    Card(onClick = onOpen, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(FieldMindIcons.Project, null, tint = FieldMindTheme.colors.project, size = 24.dp)
                Column(Modifier.weight(1f)) {
                    Text("Current project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(project.status, style = MaterialTheme.typography.labelSmall, color = FieldMindTheme.colors.project)
            }
            Text(project.researchQuestion.ifBlank { project.objective.ifBlank { "Open the workspace to define the research question." } }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectAssetChip("Observations", connectedObservations, FieldMindIcons.Observation, FieldMindTheme.colors.observation)
                ProjectAssetChip("Evidence", evidenceCount, FieldMindIcons.Camera, FieldMindTheme.colors.observation)
                ProjectAssetChip("Data", connectedData, FieldMindIcons.Data, FieldMindTheme.colors.data)
                ProjectAssetChip("Sources", connectedSources, FieldMindIcons.Source, FieldMindTheme.colors.source)
                ProjectAssetChip("Hypotheses", hypotheses.size, FieldMindIcons.Hypothesis, FieldMindTheme.colors.hypothesis)
                ProjectAssetChip("Reports", connectedReports, FieldMindIcons.Report, FieldMindTheme.colors.report)
            }
        }
    }
}

@Composable
private fun ProjectAssetChip(label: String, count: Int, icon: MaterialSymbolIcon, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.clip(RoundedCornerShape(99.dp)).background(color.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.12f)).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = color, size = 14.dp)
        Text("$count $label", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

private data class RecentEntry(val kind: String, val id: Long, val time: Long, val title: String, val sub: String, val group: String)

@Composable
private fun RecentActivityGroupCard(group: List<RecentEntry>, onOpenDetail: (String, Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val newest = group.first()
    val more = group.size - 1
    Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EntityCard(
            title = newest.title,
            kind = newest.kind,
            body = newest.sub,
            meta = buildList { add(newest.group); add(recentRelativeTime(newest.time)); if (more > 0) add("+$more more ${newest.kind}${if (more == 1) "" else "s"}") },
            onClick = { onOpenDetail(newest.kind, newest.id) }
        )
        if (more > 0) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.align(Alignment.End)) {
                Text(if (expanded) "Collapse group" else "Show $more more")
                Spacer(Modifier.size(4.dp))
                Icon(icon = if (expanded) FieldMindIcons.Up else FieldMindIcons.Down, contentDescription = null, size = 18.dp)
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.drop(1).forEach { entry ->
                        EntityCard(entry.title, entry.kind, body = entry.sub, meta = listOf(recentRelativeTime(entry.time))) { onOpenDetail(entry.kind, entry.id) }
                    }
                }
            }
        }
    }
}

internal data class LearnRecommendation(val resource: LearnResource, val path: String)

internal fun recommendedResources(signals: List<String>): List<LearnRecommendation> {
    val all = LearnLibrary.flatMap { c -> c.topics.flatMap { t -> t.resources.map { Triple(c, t, it) } } }
    val words = signals.joinToString(" ").lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 3 }.toSet()
    if (words.isEmpty()) {
        return all.take(3).map { (c, t, r) -> LearnRecommendation(r, "${c.name} · ${t.name}") }
    }
    return all.map { (c, t, r) ->
        val hay = "${c.name} ${c.description} ${t.name} ${t.summary} ${r.title} ${r.why}".lowercase()
        Triple(LearnRecommendation(r, "${c.name} · ${t.name}"), words.count { hay.contains(it) }, c)
    }.sortedByDescending { it.second }
        .let { scored -> if (scored.any { it.second > 0 }) scored.filter { it.second > 0 } else scored }
        .take(3)
        .map { it.first }
}

@Composable
private fun RecommendedLearningCard(items: List<LearnRecommendation>, onOpenReader: (String, String) -> Unit, onSeeAll: () -> Unit) {
    val accent = FieldMindTheme.colors.accentFor("learn")
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { rec ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onOpenReader(rec.resource.url, rec.resource.title) }.background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f)), contentAlignment = Alignment.Center) {
                        Icon(icon = learnKindIcon(rec.resource.kind), contentDescription = null, tint = accent, size = 18.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(rec.resource.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${rec.resource.kind} · ${rec.path}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(icon = FieldMindIcons.Book, contentDescription = null, tint = accent, size = 18.dp)
                }
            }
            TextButton(onClick = onSeeAll, modifier = Modifier.align(Alignment.End)) { Text("Open Learn"); Spacer(Modifier.size(4.dp)); Icon(icon = FieldMindIcons.Forward, contentDescription = null, size = 18.dp) }
        }
    }
}

@Composable
private fun DailyGoalCard(todayCount: Int, goal: Int, streakDays: Int, onClick: () -> Unit) {
    val colors = FieldMindTheme.colors
    val complete = todayCount >= goal && goal > 0
    val progress = if (goal > 0) todayCount.toFloat() / goal else 0f
    val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
    val remaining = (goal - todayCount).coerceAtLeast(0)
    val ringGradient = if (complete)
        listOf(colors.positive, colors.confidenceSure, MaterialTheme.colorScheme.primary, colors.positive)
    else
        listOf(MaterialTheme.colorScheme.primary, colors.data, colors.hypothesis, MaterialTheme.colorScheme.primary)
    val bg = Brush.linearGradient(
        if (complete) listOf(MaterialTheme.colorScheme.primaryContainer, colors.confidenceSure.copy(alpha = 0.30f))
        else listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    )
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .animateContentSize()
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            GradientProgressRing(
                progress = progress,
                centerValue = "$todayCount/$goal",
                caption = "$percent%",
                gradient = ringGradient,
                size = 104.dp
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (complete) "Daily goal met" else "Today's observations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    if (complete) "Great discipline — keep the streak going." else "$remaining more to hit today's goal of $goal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalStatChip(FieldMindIcons.Streak, "$streakDays day${if (streakDays == 1) "" else "s"} streak", colors.warning)
                    GoalStatChip(if (complete) FieldMindIcons.Check else FieldMindIcons.Observation, if (complete) "Done" else "$todayCount logged", if (complete) colors.positive else colors.observation)
                }
            }
        }
    }
}

@Composable
private fun ResearchSessionCtaCard(
    lastSessionLabel: String?,
    activeSessionName: String?,
    timerMs: Long,
    onStartSession: () -> Unit
) {
    val haptics = rememberFieldMindHaptics()
    val isActive = activeSessionName != null
    
    fun formatTimer(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 60000) % 60
        val hours = ms / 3600000
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isActive) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(FieldMindIcons.Timer, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 26.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (isActive) "Live Session Active" else "Research Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    when {
                        isActive -> "${activeSessionName ?: "Untitled"} • ${formatTimer(timerMs)}"
                        lastSessionLabel != null -> lastSessionLabel
                        else -> "Structured capture with timer, live feed, and summary"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
            }
            Button(
                onClick = { haptics.confirm(); onStartSession() },
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(if (isActive) FieldMindIcons.Capture else FieldMindIcons.Add, null, size = 18.dp)
                Spacer(Modifier.size(4.dp))
                Text(if (isActive) "Continue" else "Start")
            }
        }
    }

@Composable
private fun GoalStatChip(icon: MaterialSymbolIcon, label: String, tint: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = if (FieldMindTheme.colors.isDark) 0.20f else 0.13f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon = icon, contentDescription = null, tint = tint, size = 14.dp)
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeWidgetGrid(
    observations: List<ObservationEntity>,
    notes: List<NoteEntity>,
    questions: List<QuestionEntity>,
    sources: List<SourceEntity>,
    projects: List<ProjectEntity>,
    reports: List<ReportEntity>,
    data: List<DataRecordEntity>,
    onNavigate: (FieldMindScreen) -> Unit
) {
    val widgets = listOf(
        HomeWidget("Observations", "${observations.size} captured", FieldMindIcons.Observation, FieldMindTheme.colors.observation, FieldMindScreen.Insights),
        HomeWidget("Questions", "${questions.count { it.status != "Answered" }} open", FieldMindIcons.Question, FieldMindTheme.colors.question, FieldMindScreen.Questions),
        HomeWidget("Sources", "${sources.count { it.readingStatus == "Read" }}/${sources.size} read", FieldMindIcons.Source, FieldMindTheme.colors.source, FieldMindScreen.Library),
        HomeWidget("Projects", "${projects.count { it.status == "Active" }} active", FieldMindIcons.Project, FieldMindTheme.colors.project, FieldMindScreen.Projects),
        HomeWidget("Data", "${data.size} records", FieldMindIcons.Data, FieldMindTheme.colors.data, FieldMindScreen.DataTools),
        HomeWidget("Reports", "${reports.size} drafts", FieldMindIcons.Report, FieldMindTheme.colors.report, FieldMindScreen.Reports)
    )
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 2) {
        widgets.forEach { widget -> HomeWidgetCard(widget, Modifier.weight(1f)) { onNavigate(widget.screen) } }
    }
}
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeDataOptionsCard(data: List<DataRecordEntity>, onOpenData: () -> Unit) {
    val tools = listOf(
        Triple("Count", "Track totals", FieldMindIcons.Add),
        Triple("Measure", "Log values", FieldMindIcons.Graph),
        Triple("Weather", "Conditions", FieldMindIcons.Weather),
        Triple("Species", "Survey data", FieldMindIcons.Nature)
    )
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(FieldMindIcons.Data, null, tint = FieldMindTheme.colors.data, size = 22.dp)
                Column(Modifier.weight(1f)) {
                    Text("Data tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${data.size} records • choose what you are tracking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onOpenData) { Text("Open") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                tools.forEach { (title, body, icon) ->
                    Surface(onClick = onOpenData, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(icon, null, tint = FieldMindTheme.colors.data, size = 20.dp)
                            Column { Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold); Text(body, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}

private data class HomeWidget(val title: String, val value: String, val icon: MaterialSymbolIcon, val color: androidx.compose.ui.graphics.Color, val screen: FieldMindScreen)

@Composable
private fun HomeWidgetCard(widget: HomeWidget, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Card(modifier = modifier.height(112.dp).clickable { haptics.light(); onClick() }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.fillMaxSize().padding(16.dp, 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(widget.icon, null, tint = widget.color, size = 40.dp)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(widget.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(widget.value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RecentCapturesCard(observations: List<ObservationEntity>, onOpenDetail: (String, Long) -> Unit) {
    if (observations.isEmpty()) return
    val recentObs = observations.sortedByDescending { it.timestamp }.take(3)
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Recent captures", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recentObs.forEach { obs ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenDetail("observation", obs.id) }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            FieldMindIcons.Observation,
                            null,
                            tint = FieldMindTheme.colors.observation,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                obs.subject.takeIf { it.isNotBlank() } ?: obs.category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    obs.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (obs.latitude != null && obs.longitude != null) {
                                    Text("📍", style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    obs.time.takeIf { it.isNotBlank() } ?: "–",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
