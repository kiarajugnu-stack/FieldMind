package chromahub.rhythm.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.features.field.data.database.entity.*
import chromahub.rhythm.app.features.field.data.learn.LearnResource
import chromahub.rhythm.app.features.field.data.learn.LearnLibrary
import chromahub.rhythm.app.features.field.data.stats.FieldMindStreaks
import chromahub.rhythm.app.features.field.presentation.components.*
import chromahub.rhythm.app.features.field.presentation.navigation.FieldMindScreen
import chromahub.rhythm.app.features.field.presentation.theme.FieldMindTheme
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// ══════════════════════════════════════════════════════════════════════
//  Today (Home)
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
    val flashcards by viewModel.flashcards.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val tags by viewModel.commonTags.collectAsState()
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
    val recommendations = remember(learnSignals) { recommendedResources(learnSignals) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { FieldScreenHeader("FieldMind", "Observe. Question. Research clearly.", icon = FieldMindIcons.Nature, actionIcon = FieldMindIcons.Settings, onAction = onOpenSettings) }
        item { DailyGoalCard(todayCount, goal, currentStreak) { onNavigate(FieldMindScreen.Observe) } }
        item { HomeWidgetGrid(observations, notes, questions, sources, projects, reports, data) { onNavigate(it) } }
        item { RecommendedLearningCard(recommendations, onOpenReader, onSeeAll = { onNavigate(FieldMindScreen.Learn) }) }
        item { ReadingReviewCard(sources, flashcards, onNavigate) }
        item {
            SectionHeader("Quick actions", "Go to Map, Export, Search, or Flashcards")
            QuickActionGrid(listOf(
                QuickAction("Map", FieldMindIcons.Map, FieldMindTheme.colors.info, FieldMindScreen.MapScreen),
                QuickAction("Export", FieldMindIcons.Export, FieldMindTheme.colors.report, FieldMindScreen.ExportStudio),
                QuickAction("Search", FieldMindIcons.Search, FieldMindTheme.colors.question, FieldMindScreen.Search),
                QuickAction("Review", FieldMindIcons.Flashcard, FieldMindTheme.colors.flashcard, FieldMindScreen.Flashcards)
            )) { onNavigate(it) }
        }
        if (activeProject != null) {
            item { SectionHeader("Current project") }
            item {
                EntityCard(
                    title = activeProject.title,
                    kind = "project",
                    body = activeProject.objective.ifBlank { activeProject.researchQuestion.ifBlank { "Open the project workspace." } },
                    meta = listOf(activeProject.status, activeProject.topicType),
                    onClick = { onOpenDetail("project", activeProject.id) }
                )
            }
        }
        item { SectionHeader("Recent activity", if (tags.isNotEmpty()) "Top tag: ${tags.first().name}" else "Collapsed by type and category") }
        val activity = buildList {
            observations.forEach { add(RecentEntry("observation", it.id, it.timestamp, it.subject.ifBlank { "Observation" }, "${it.category} • ${it.date} ${it.time}", it.category)) }
            notes.forEach { add(RecentEntry("note", it.id, it.updatedAt, it.title.ifBlank { "Untitled note" }, it.body.ifBlank { it.category }, it.category)) }
            questions.forEach { add(RecentEntry("question", it.id, it.updatedAt, it.questionText, "${it.status} • ${it.priority}", it.status)) }
            sources.forEach { add(RecentEntry("source", it.id, it.updatedAt, it.title, "${it.type} • ${it.readingStatus}", it.type)) }
            data.forEach { add(RecentEntry("data", it.id, it.timestamp, it.label, "${it.toolType} • ${it.value} ${it.unit}".trim(), it.toolType)) }
            reports.forEach { add(RecentEntry("report", it.id, it.updatedAt, it.title, "${it.type} • ${it.status}", it.type)) }
        }.sortedByDescending { it.time }
        val groups = activity.groupBy { "${it.kind}:${it.group}" }.values.map { it.sortedByDescending { entry -> entry.time } }.sortedByDescending { it.first().time }.take(10)
        if (activity.isEmpty()) {
            item { EmptyState("No activity yet", "Start with one factual observation or a free-form note. Both stay clearly separated.", icon = FieldMindIcons.Observation, actionLabel = "Open capture") { onNavigate(FieldMindScreen.Observe) } }
        } else {
            items(groups, key = { "${it.first().kind}-${it.first().group}" }) { group ->
                RecentActivityGroupCard(group, onOpenDetail)
            }
        }
    }
}


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
                MiniActionTile("Review", "${flashcards.size} cards", if (flashcards.isEmpty()) "Create cards" else "Start session", FieldMindIcons.Flashcard, Modifier.weight(1f)) { onNavigate(if (flashcards.isEmpty()) FieldMindScreen.Library else FieldMindScreen.Flashcards) }
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

/** A learn resource paired with the category/topic path it came from. */
internal data class LearnRecommendation(val resource: LearnResource, val path: String)

/**
 * Picks up to three Learn resources relevant to the user's recent activity by scoring every
 * resource against keyword overlap with [signals] (recent categories, subjects, tags, topics).
 * With no signals it falls back to foundational "scientific thinking" resources so the Today
 * screen always has a useful suggestion.
 */
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
        HomeWidget("Capture", "${observations.size} observations", FieldMindIcons.Camera, FieldMindTheme.colors.observation, FieldMindScreen.Observe),
        HomeWidget("Notes", "${notes.size} free-form", FieldMindIcons.Note, FieldMindTheme.colors.source, FieldMindScreen.Library),
        HomeWidget("Questions", "${questions.count { it.status != "Answered" }} open", FieldMindIcons.Question, FieldMindTheme.colors.question, FieldMindScreen.Questions),
        HomeWidget("Sources", "${sources.count { it.readingStatus == "Read" }}/${sources.size} read", FieldMindIcons.Source, FieldMindTheme.colors.source, FieldMindScreen.Library),
        HomeWidget("Projects", "${projects.count { it.status == "Active" }} active", FieldMindIcons.Project, FieldMindTheme.colors.project, FieldMindScreen.Projects),
        HomeWidget("Outputs", "${data.size} data • ${reports.size} reports", FieldMindIcons.Report, FieldMindTheme.colors.report, FieldMindScreen.Insights)
    )
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 2) {
        widgets.forEach { widget -> HomeWidgetCard(widget, Modifier.weight(1f)) { onNavigate(widget.screen) } }
    }
}

private data class HomeWidget(val title: String, val value: String, val icon: MaterialSymbolIcon, val color: androidx.compose.ui.graphics.Color, val screen: FieldMindScreen)

@Composable
private fun HomeWidgetCard(widget: HomeWidget, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Card(modifier = modifier.height(112.dp).clickable { haptics.light(); onClick() }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(widget.color.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f)), contentAlignment = Alignment.Center) {
                Icon(widget.icon, null, tint = widget.color, size = 21.dp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(widget.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(widget.value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private data class QuickAction(val label: String, val icon: MaterialSymbolIcon, val color: androidx.compose.ui.graphics.Color, val screen: FieldMindScreen)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionGrid(actions: List<QuickAction>, onNavigate: (FieldMindScreen) -> Unit) {
    val haptics = rememberFieldMindHaptics()
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 3) {
        actions.forEach { action ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { haptics.light(); onNavigate(action.screen) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(38.dp).background(action.color.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(icon = action.icon, contentDescription = null, tint = action.color, size = 20.dp) }
                    Text(action.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

