package chromahub.rhythm.app.features.field.presentation.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import chromahub.rhythm.app.features.field.data.ai.AssistantTask
import chromahub.rhythm.app.features.field.data.ai.GeminiResearchAssistant
import chromahub.rhythm.app.features.field.data.database.entity.*
import chromahub.rhythm.app.features.field.data.export.FieldMindExport
import chromahub.rhythm.app.features.field.data.learn.GuidedPath
import chromahub.rhythm.app.features.field.data.learn.GuidedPaths
import chromahub.rhythm.app.features.field.data.learn.LearnCategory
import chromahub.rhythm.app.features.field.data.learn.LearnLibrary
import chromahub.rhythm.app.features.field.data.learn.LearnResource
import chromahub.rhythm.app.features.field.data.learn.SuggestedOnlineApis
import chromahub.rhythm.app.features.field.data.location.CapturedLocation
import chromahub.rhythm.app.features.field.data.location.FieldLocationProvider
import chromahub.rhythm.app.features.field.presentation.components.*
import chromahub.rhythm.app.features.field.presentation.navigation.FieldMindScreen
import chromahub.rhythm.app.features.field.presentation.theme.FieldMindTheme
import chromahub.rhythm.app.features.field.presentation.viewmodel.DraftEvidenceAttachment
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val observationCategories = listOf("Bird", "Animal", "Insect", "Plant", "Rock", "Weather", "Water", "Human Behavior", "Reading Insight", "Other")
internal val confidenceOptions = listOf("Sure", "Guess", "Needs Verification")
private val sourceTypes = listOf("Observation", "Reading", "Video", "Thought", "Discussion")
private val questionStatuses = listOf("New", "Researching", "Tested", "Answered", "Abandoned")
private val sourceLibraryTypes = listOf("Article", "Paper", "Book", "Video", "Website", "PDF", "Image", "Local document", "Note")
private val readingStatuses = listOf("Not started", "In progress", "Read", "Skimmed", "To revisit")
private val sourceImportanceLevels = listOf("Normal", "Important", "Critical")
private val dataTools = listOf("Counter", "Measurement Log", "Checklist", "Event Log", "Weather Log", "Site Log", "Species Tracker", "Comparison Table")
private val reportTypes = listOf("Summary", "Field Report", "Literature Review", "Project Draft", "Findings Note", "Final Report")
internal val learningModules = listOf(
    "Beginner" to listOf("Scientific thinking", "Observation", "Note-taking", "Identifying bias", "Basic biology", "Basic geology", "Reading graphs", "Asking testable questions", "Variables", "Simple data collection"),
    "Intermediate" to listOf("Research design", "Sampling", "Comparison", "Classification", "Literature review", "Writing summaries", "Data interpretation"),
    "Advanced" to listOf("Proposal writing", "Structured projects", "Analysis", "Citations", "Presentation", "Field methods", "Ethics")
)

private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

// ══════════════════════════════════════════════════════════════════════
//  Onboarding
// ══════════════════════════════════════════════════════════════════════

@Composable
fun FieldMindOnboardingScreen(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple(FieldMindIcons.Nature, "Welcome to FieldMind", "A local-first research notebook for Observe → Question → Research → Hypothesize → Collect Data → Analyze → Conclude → Archive."),
        Triple(FieldMindIcons.Observation, "Research begins with evidence", "Capture facts, media, location, questions, and confidence without inventing conclusions."),
        Triple(FieldMindIcons.Project, "Build projects", "Turn repeated curiosity into objectives, methods, data, sources, reports, and next steps."),
        Triple(FieldMindIcons.Bolt, "Capture in two taps", "Field mode logs an observation instantly with one big button, so you never miss a moment outdoors."),
        Triple(FieldMindIcons.Export, "Own your work", "Everything core works offline and can be exported as Markdown, CSV, JSON, or plain text.")
    )
    val totalSteps = pages.size + 1
    val isPermissionStep = step == pages.size
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f, fill = false).padding(top = 48.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(
                    Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon = if (isPermissionStep) FieldMindIcons.Lock else pages[step].first, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 38.dp) }
                Text("Step ${step + 1} of $totalSteps", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                if (isPermissionStep) {
                    Text("Optional permissions", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text("FieldMind works fully offline. Grant only what helps you — you can skip all of these and the app still works. Change them anytime in system settings.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OnboardingPermissions()
                } else {
                    Text(pages[step].second, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(pages[step].third, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(totalSteps) { i ->
                        Box(
                            Modifier
                                .height(6.dp)
                                .weight(1f)
                                .background(
                                    if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onFinish, Modifier.weight(1f)) { Text(if (isPermissionStep) "Skip all" else "Skip") }
                    Button(onClick = { if (step < totalSteps - 1) step++ else onFinish() }, Modifier.weight(1f)) { Text(if (isPermissionStep) "Enter FieldMind" else "Next") }
                }
            }
        }
    }
}

/**
 * Optional permission requests shown on the last onboarding page. Each is independent and may be
 * skipped; nothing here is required for the app to function. Granted state updates live so the
 * user sees confirmation.
 */
@Composable
private fun OnboardingPermissions() {
    val context = LocalContext.current
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    var locationGranted by remember { mutableStateOf(granted(Manifest.permission.ACCESS_FINE_LOCATION)) }
    var micGranted by remember { mutableStateOf(granted(Manifest.permission.RECORD_AUDIO)) }
    val mediaPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
    var mediaGranted by remember { mutableStateOf(granted(mediaPerm)) }
    var notifGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || granted("android.permission.POST_NOTIFICATIONS")) }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { locationGranted = it }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micGranted = it }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { mediaGranted = it }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifGranted = it }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PermissionRow(FieldMindIcons.Location, "Location", "Tag observations with GPS coordinates and place names.", locationGranted) { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        PermissionRow(FieldMindIcons.Gallery, "Photos & media", "Attach images as visual evidence to observations.", mediaGranted) { mediaLauncher.launch(mediaPerm) }
        PermissionRow(FieldMindIcons.Mic, "Microphone", "Record short audio notes in the field.", micGranted) { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow(FieldMindIcons.Notifications, "Notifications", "Optional reminders to keep your research streak.", notifGranted) { notifLauncher.launch("android.permission.POST_NOTIFICATIONS") }
        }
    }
}

@Composable
private fun PermissionRow(icon: MaterialSymbolIcon, title: String, desc: String, granted: Boolean, onRequest: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon = FieldMindIcons.Check, contentDescription = null, tint = FieldMindTheme.colors.positive, size = 18.dp)
                Text("Allowed", style = MaterialTheme.typography.labelMedium, color = FieldMindTheme.colors.positive, fontWeight = FontWeight.SemiBold)
            }
        } else {
            FilledTonalButton(onClick = onRequest, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) { Text("Allow") }
        }
    }
}

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
    val todayKey = remember { today() }
    val todayCount = observations.count { it.date == todayKey }
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
        item { DailyGoalCard(todayCount, goal, observations.map { it.date }.distinct().size) { onNavigate(FieldMindScreen.Observe) } }
        item { HomeWidgetGrid(observations, notes, questions, sources, projects, reports, data) { onNavigate(it) } }
        item { SectionHeader("Primary quick capture", "Start with Snap or Note, then continue only if needed.") }
        item {
            QuickActionGrid(
                listOf(
                    QuickAction("Snap evidence", FieldMindIcons.Camera, FieldMindTheme.colors.observation, FieldMindScreen.Observe),
                    QuickAction("New note", FieldMindIcons.Note, FieldMindTheme.colors.source, FieldMindScreen.Observe),
                    QuickAction("Field mode", FieldMindIcons.Bolt, FieldMindTheme.colors.warning, FieldMindScreen.FieldMode),
                    QuickAction("New project", FieldMindIcons.Project, FieldMindTheme.colors.project, FieldMindScreen.Projects),
                    QuickAction("Add source", FieldMindIcons.Source, FieldMindTheme.colors.source, FieldMindScreen.Library),
                    QuickAction("Question bank", FieldMindIcons.Question, FieldMindTheme.colors.question, FieldMindScreen.Questions),
                    QuickAction("Search archive", FieldMindIcons.Search, FieldMindTheme.colors.info, FieldMindScreen.Search)
                ),
                onNavigate
            )
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

private fun recentRelativeTime(time: Long): String {
    val diff = System.currentTimeMillis() - time
    val mins = diff / 60_000
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 1440 -> "${mins / 60}h ago"
        mins < 1440 * 7 -> "${mins / 1440}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(time))
    }
}

@Composable
private fun DailyGoalCard(todayCount: Int, goal: Int, sessions: Int, onClick: () -> Unit) {
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
                    GoalStatChip(FieldMindIcons.Streak, "$sessions day${if (sessions == 1) "" else "s"}", colors.warning)
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

// ══════════════════════════════════════════════════════════════════════
//  Capture / Field mode
// ══════════════════════════════════════════════════════════════════════

private enum class CaptureState { Idle, ChooseMode, ChooseCategory, Snap, Note }

@Composable
fun ObserveScreen(
    viewModel: FieldMindViewModel,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    compactFieldMode: Boolean = false,
    onBack: (() -> Unit)? = null
) {
    if (compactFieldMode) { FieldModeScreen(viewModel, onBack ?: {}); return }
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val defaultCategory by viewModel.fieldSettings.defaultCategory.collectAsState()
    var captureState by remember { mutableStateOf(CaptureState.Idle) }
    var selectedCategory by remember(defaultCategory) { mutableStateOf(defaultCategory) }
    val haptics = rememberFieldMindHaptics()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { FieldScreenHeader("Capture", "Start with Snap evidence or a free-form Note, then add category and details.", icon = FieldMindIcons.Capture) }
        item { PrimaryCaptureEntry(captureState, onStart = { haptics.light(); captureState = CaptureState.ChooseMode }, onClose = { haptics.light(); captureState = CaptureState.Idle }) }
        when (captureState) {
            CaptureState.ChooseMode -> item { CaptureModeChooser(
                onSnap = { haptics.light(); captureState = CaptureState.ChooseCategory },
                onNote = { haptics.light(); captureState = CaptureState.Note }
            ) }
            CaptureState.ChooseCategory -> item { CategoryFirstCard(selectedCategory, onCategory = { selectedCategory = it }, onSnap = { haptics.light(); captureState = CaptureState.Snap }) }
            CaptureState.Snap -> item { ObservationCaptureCard(viewModel = viewModel, compact = false, initialCategory = selectedCategory, snapFirst = true) { captureState = CaptureState.Idle } }
            CaptureState.Note -> item { NoteCaptureCard(viewModel = viewModel, initialCategory = selectedCategory) { captureState = CaptureState.Idle } }
            CaptureState.Idle -> Unit
        }
        item { SectionHeader("Recent captures", "${observations.size} observations • ${notes.size} notes") }
        if (observations.isEmpty() && notes.isEmpty()) item { EmptyState("No captures yet", "Snap factual evidence or draft a note. Observations stay facts-only; notes stay free-form.", icon = FieldMindIcons.Observation, actionLabel = "Start capture") { captureState = CaptureState.ChooseMode } }
        items(notes.take(6), key = { "note-${it.id}" }) { item ->
            EntityCard(
                title = item.title,
                kind = "note",
                body = item.body.take(140).ifBlank { "No body yet." },
                meta = listOf(item.category, recentRelativeTime(item.updatedAt)),
                onClick = { onOpenDetail("note", item.id) }
            )
        }
        items(observations.take(10), key = { "obs-${it.id}" }) { item ->
            EntityCard(
                title = item.subject,
                kind = "observation",
                body = item.factsOnlyNotes.take(140).ifBlank { "No factual notes recorded." },
                confidence = item.confidenceLevel,
                meta = buildList { add(item.category); add("${item.date} ${item.time}"); if (item.manualLocation.isNotBlank()) add(item.manualLocation); if (item.tags.isNotBlank()) add(item.tags) },
                onClick = { onOpenDetail("observation", item.id) }
            )
        }
    }
}

@Composable
private fun PrimaryCaptureEntry(state: CaptureState, onStart: () -> Unit, onClose: () -> Unit) {
    Button(onClick = if (state == CaptureState.Idle) onStart else onClose, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Icon(icon = if (state == CaptureState.Idle) FieldMindIcons.Add else FieldMindIcons.Close, contentDescription = null, size = 20.dp)
        Spacer(Modifier.size(8.dp))
        Text(if (state == CaptureState.Idle) "Quick capture" else "Close capture")
    }
}

@Composable
private fun CaptureModeChooser(onSnap: () -> Unit, onNote: () -> Unit) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("What are you capturing?", "Choose the smallest path that matches the moment.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CaptureModeTile("Snap", "Photo, gallery, or file first", FieldMindIcons.Camera, FieldMindTheme.colors.observation, Modifier.weight(1f), onSnap)
                CaptureModeTile("Note", "Title, facts, tags first", FieldMindIcons.Note, FieldMindTheme.colors.source, Modifier.weight(1f), onNote)
            }
        }
    }
}

@Composable
private fun CaptureModeTile(title: String, body: String, icon: MaterialSymbolIcon, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(132.dp).clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = if (FieldMindTheme.colors.isDark) 0.24f else 0.14f)), contentAlignment = Alignment.Center) { Icon(icon = icon, contentDescription = null, tint = color, size = 26.dp) }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CategoryFirstCard(category: String, onCategory: (String) -> Unit, onSnap: () -> Unit) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader("Choose a category", "This labels the evidence before the full snap form.")
            ChoiceChips(observationCategories, category, onSelected = onCategory)
            Button(onClick = onSnap, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Continue to snap evidence") }
        }
    }
}

@Composable
private fun NoteCaptureCard(viewModel: FieldMindViewModel, initialCategory: String, onSaved: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var tags by remember { mutableStateOf("") }
    var projectId by remember { mutableStateOf<Long?>(null) }
    var sourceId by remember { mutableStateOf<Long?>(null) }
    var attachments by remember { mutableStateOf<List<DraftEvidenceAttachment>>(emptyList()) }
    val haptics = rememberFieldMindHaptics()
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        attachments = attachments + uris.map { DraftEvidenceAttachment("Gallery", it.toString(), "Note media") }
        scope.launch { snackbar.showSnackbar(if (uris.isEmpty()) "Gallery selection cancelled." else "Media attached.") }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) scope.launch { snackbar.showSnackbar("File selection cancelled.") } else {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            attachments = attachments + DraftEvidenceAttachment("File", uri.toString(), "Note attachment")
            scope.launch { snackbar.showSnackbar("File attached.") }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SnackbarHost(snackbar)
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(icon = FieldMindIcons.Note, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 24.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("New note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Free-form notes are separate from facts-only observations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                CaptureStep("Category", "Label the note before writing.", FieldMindIcons.Category) {
                    ChoiceChips(observationCategories, category) { category = it }
                }
                CaptureStep("Title, body & tags", "Prioritize what you want to remember.", FieldMindIcons.Edit) {
                    FieldTextField(title, { title = it }, "Title")
                    FieldTextField(body, { body = it }, "Body / facts / reflection", minLines = 5, supportingText = "Free-form note. Use observations for facts-only evidence.")
                    FieldTextField(tags, { tags = it }, "Tags", supportingText = "Comma-separated tags")
                }
                CaptureStep("Links", "Optionally connect a project or source.", FieldMindIcons.Link) {
                    if (projects.isNotEmpty()) {
                        Text("Project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ChoiceChips(listOf("No project") + projects.map { it.title }, projects.firstOrNull { it.id == projectId }?.title ?: "No project") { selected -> projectId = projects.firstOrNull { it.title == selected }?.id }
                    }
                    if (sources.isNotEmpty()) {
                        Text("Source", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ChoiceChips(listOf("No source") + sources.map { it.title }, sources.firstOrNull { it.id == sourceId }?.title ?: "No source") { selected -> sourceId = sources.firstOrNull { it.title == selected }?.id }
                    }
                }
                CaptureStep("Optional evidence", "Attach supporting files without turning the note into an observation.", FieldMindIcons.File) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { haptics.light(); mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, Modifier.weight(1f)) { Icon(icon = FieldMindIcons.Gallery, contentDescription = "Gallery", size = 18.dp) }
                        OutlinedButton(onClick = { haptics.light(); filePicker.launch(arrayOf("application/pdf", "text/*", "image/*", "video/*", "audio/*")) }, Modifier.weight(1f)) { Icon(icon = FieldMindIcons.File, contentDescription = "File", size = 18.dp) }
                    }
                    AttachmentPreviewList(attachments, onCaptionChange = { index, caption -> attachments = attachments.mapIndexed { i, item -> if (i == index) item.copy(caption = caption) else item } }, onRemove = { remove -> attachments = attachments.filterIndexed { index, _ -> index != remove } })
                }
                Button(onClick = {
                    if (title.isBlank() && body.isBlank()) scope.launch { snackbar.showSnackbar("Add a title or body before saving.") } else { haptics.confirm(); viewModel.addNote(title.ifBlank { body.take(36) }, body, category, tags, projectId, sourceId, attachments) {
                        title = ""; body = ""; tags = ""; attachments = emptyList(); projectId = null; sourceId = null
                        scope.launch { snackbar.showSnackbar("Note saved to your library.") }
                        onSaved()
                    }
                    }
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Icon(icon = FieldMindIcons.Check, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Save note")
                }
            }
        }
    }
}

@Composable
private fun FieldModeScreen(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val defaultConfidence by viewModel.fieldSettings.defaultConfidence.collectAsState()
    val haptics = rememberFieldMindHaptics()
    var showFull by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { FieldScreenHeader("Field mode", "One tap logs an observation. Add details later.", icon = FieldMindIcons.Bolt, actionIcon = FieldMindIcons.Close, onAction = onBack) }
            if (showFull) {
                item { ObservationCaptureCard(viewModel = viewModel, compact = true) { showFull = false } }
            } else {
                item {
                    Text("Tap a type to save instantly", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(observationCategories.chunked(2)) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { category ->
                            FieldModeButton(category, Modifier.weight(1f)) {
                                haptics.confirm()
                                viewModel.addObservation(
                                    subject = category,
                                    category = category,
                                    facts = "Quick field capture — add details later.",
                                    confidence = defaultConfidence,
                                    manualLocation = "",
                                    tags = "",
                                    evidence = "",
                                    context = ""
                                ) { savedId ->
                                    scope.launch {
                                        val result = snackbar.showSnackbar("Saved $category", actionLabel = "Undo", duration = SnackbarDuration.Short)
                                        if (result == SnackbarResult.ActionPerformed) viewModel.archiveObservation(savedId)
                                    }
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                item {
                    OutlinedButton(onClick = { showFull = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Icon(icon = FieldMindIcons.Edit, contentDescription = null, size = 18.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("Add full details instead")
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldModeButton(category: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = FieldMindTheme.colors.accentFor("observation")
    Card(
        modifier = modifier.height(104.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon = FieldMindIcons.iconForCategory(category), contentDescription = null, tint = accent, size = 24.dp)
            }
            Text(category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ObservationCaptureCard(viewModel: FieldMindViewModel, compact: Boolean, initialCategory: String? = null, snapFirst: Boolean = false, onSaved: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val projects by viewModel.projects.collectAsState()
    val defaultCategory by viewModel.fieldSettings.defaultCategory.collectAsState()
    val defaultConfidence by viewModel.fieldSettings.defaultConfidence.collectAsState()
    val mediaEnabled by viewModel.fieldSettings.mediaAttachmentsEnabled.collectAsState()
    val audioEnabled by viewModel.fieldSettings.audioRecordingEnabled.collectAsState()
    var subject by remember { mutableStateOf("") }
    var category by remember(defaultCategory, initialCategory) { mutableStateOf(initialCategory ?: defaultCategory) }
    var facts by remember { mutableStateOf("") }
    var confidence by remember(defaultConfidence) { mutableStateOf(defaultConfidence) }
    var manualLocation by remember { mutableStateOf("") }
    var capturedLocation by remember { mutableStateOf<CapturedLocation?>(null) }
    var tags by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    var fieldContext by remember { mutableStateOf("") }
    var projectId by remember { mutableStateOf<Long?>(null) }
    var attachments by remember { mutableStateOf<List<DraftEvidenceAttachment>>(emptyList()) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }
    var locating by remember { mutableStateOf(false) }
    val locationProvider = remember { FieldLocationProvider(context) }
    val haptics = rememberFieldMindHaptics()

    val startLocating = {
        locating = true
        locationProvider.requestCurrentLocation { captured ->
            locating = false
            if (captured != null) {
                capturedLocation = captured
                manualLocation = captured.asDisplayText()
                locationProvider.resolvePlaceName(captured.latitude, captured.longitude) { place ->
                    if (!place.isNullOrBlank()) {
                        val withPlace = captured.copy(placeName = place)
                        capturedLocation = withPlace
                        manualLocation = withPlace.asDisplayText()
                    }
                }
            }
            scope.launch { snackbar.showSnackbar(captured?.let { "Location captured." } ?: "Couldn't get a fix. Check that location is on, then try again or type a place.") }
        }
    }

    LaunchedEffect(recording) {
        if (recording) {
            recordSeconds = 0
            while (recording) { kotlinx.coroutines.delay(1000); recordSeconds++ }
        }
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createFieldMindFileUri(context, "photo", ".jpg")
            pendingPhotoUri = uri
        } else scope.launch { snackbar.showSnackbar("Camera permission denied. Text notes and gallery/file attachments still work.") }
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingPhotoUri
        if (saved && uri != null) attachments = attachments + DraftEvidenceAttachment("Photo", uri.toString(), "Camera photo")
        scope.launch { snackbar.showSnackbar(if (saved) "Photo attached." else "Camera capture cancelled.") }
        pendingPhotoUri = null
    }
    LaunchedEffect(pendingPhotoUri) { pendingPhotoUri?.let { takePicture.launch(it) } }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isEmpty()) scope.launch { snackbar.showSnackbar("Gallery selection cancelled.") }
        attachments = attachments + uris.map { DraftEvidenceAttachment("Gallery", it.toString(), "Selected media") }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) scope.launch { snackbar.showSnackbar("File selection cancelled.") } else {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            attachments = attachments + DraftEvidenceAttachment("File", uri.toString(), "Reference file / PDF")
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result.values.any { it }
        if (granted) startLocating()
        else scope.launch { snackbar.showSnackbar("Location denied. Manual place names remain available.") }
    }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = createFieldMindFile(context, "audio", ".m4a")
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            runCatching {
                newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                newRecorder.setOutputFile(file.absolutePath)
                newRecorder.prepare()
                newRecorder.start()
                audioFile = file
                recorder = newRecorder
                recording = true
            }.onFailure {
                newRecorder.release()
                scope.launch { snackbar.showSnackbar("Could not start audio recording: ${it.localizedMessage ?: "unknown error"}") }
            }
        } else scope.launch { snackbar.showSnackbar("Audio permission denied.") }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SnackbarHost(snackbar)
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(icon = FieldMindIcons.Observation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 24.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (compact) "Quick field note" else if (snapFirst) "Snap evidence" else "New observation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (snapFirst) "Evidence first, then facts-only observation notes." else "Date and time are stamped automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (snapFirst && mediaEnabled) {
                    CaptureStep("Evidence first", "Start with camera, gallery, or files before writing facts.", FieldMindIcons.Camera) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { val uri = createFieldMindFileUri(context, "photo", ".jpg"); pendingPhotoUri = uri } else cameraPermission.launch(Manifest.permission.CAMERA) }, Modifier.weight(1f)) {
                                Icon(icon = FieldMindIcons.Camera, contentDescription = "Camera", size = 18.dp)
                            }
                            OutlinedButton(onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, Modifier.weight(1f)) {
                                Icon(icon = FieldMindIcons.Gallery, contentDescription = "Gallery", size = 18.dp)
                            }
                            OutlinedButton(onClick = { filePicker.launch(arrayOf("application/pdf", "text/*", "image/*", "video/*", "audio/*")) }, Modifier.weight(1f)) {
                                Icon(icon = FieldMindIcons.File, contentDescription = "File", size = 18.dp)
                            }
                        }
                        AttachmentPreviewList(attachments, onCaptionChange = { index, caption -> attachments = attachments.mapIndexed { i, item -> if (i == index) item.copy(caption = caption) else item } }, onRemove = { remove -> attachments = attachments.filterIndexed { index, _ -> index != remove } })
                    }
                }

                CaptureStep(if (snapFirst) "Subject & confidence" else "Subject", "What did you observe, and how sure are you?", FieldMindIcons.iconForCategory(category)) {
                    FieldTextField(subject, { subject = it }, "Subject", supportingText = "Example: Crow on wire")
                    if (!compact) {
                        Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ChoiceChips(observationCategories, category) { category = it }
                    }
                    Text("Confidence", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChoiceChips(confidenceOptions, confidence) { confidence = it }
                }

                CaptureStep(if (snapFirst) "Facts after evidence" else "Facts", "Record only what you observed — keep guesses out.", FieldMindIcons.Edit) {
                    FactsInterpretationBanner()
                    FieldTextField(facts, { facts = it }, "Facts-only notes", minLines = if (compact) 3 else 5, supportingText = "Write only what you saw/heard/measured. Put guesses in a question or hypothesis.")
                    if (!compact) FieldTextField(fieldContext, { fieldContext = it }, "Mood / field context", supportingText = "Weather, light, surrounding activity, or constraints.")
                }

                CaptureStep("Location", "GPS is optional; manual place names work offline.", FieldMindIcons.Location) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { manualLocation = ""; capturedLocation = null }, Modifier.weight(1f)) { Text("Manual") }
                        FilledTonalButton(
                            onClick = {
                                if (locating) return@FilledTonalButton
                                if (locationProvider.hasAnyLocationPermission()) startLocating()
                                else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !locating
                        ) {
                            if (locating) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.size(6.dp)); Text("Locating…")
                            } else {
                                Icon(icon = FieldMindIcons.Location, contentDescription = null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Use GPS")
                            }
                        }
                    }
                    capturedLocation?.let { loc ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(icon = FieldMindIcons.Check, contentDescription = null, tint = FieldMindTheme.colors.confidenceSure, size = 16.dp)
                            Text(loc.asDisplayText(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    FieldTextField(manualLocation, { manualLocation = it }, "Place / GPS note")
                }

                if (mediaEnabled && !snapFirst) {
                    CaptureStep("Evidence", "Back your observation with photos, files, or a voice note.", FieldMindIcons.Camera) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { val uri = createFieldMindFileUri(context, "photo", ".jpg"); pendingPhotoUri = uri } else cameraPermission.launch(Manifest.permission.CAMERA) }, Modifier.weight(1f)) {
                                Icon(icon = FieldMindIcons.Camera, contentDescription = "Camera", size = 18.dp)
                            }
                            OutlinedButton(onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, Modifier.weight(1f)) {
                                Icon(icon = FieldMindIcons.Gallery, contentDescription = "Gallery", size = 18.dp)
                            }
                            OutlinedButton(onClick = { filePicker.launch(arrayOf("application/pdf", "text/*", "image/*", "video/*", "audio/*")) }, Modifier.weight(1f)) {
                                Icon(icon = FieldMindIcons.File, contentDescription = "File", size = 18.dp)
                            }
                        }
                        if (audioEnabled) {
                            if (recording) RecordingIndicator(recordSeconds)
                            Button(onClick = {
                                if (recording) {
                                    val file = audioFile
                                    runCatching { recorder?.stop() }
                                    recorder?.release(); recorder = null; recording = false
                                    file?.let { attachments = attachments + DraftEvidenceAttachment("Audio", Uri.fromFile(it).toString(), "Voice note", localPath = it.absolutePath, mimeType = "audio/mp4") }
                                    scope.launch { snackbar.showSnackbar("Voice note attached.") }
                                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) audioPermission.launch(Manifest.permission.RECORD_AUDIO) else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }, modifier = Modifier.fillMaxWidth(), colors = if (recording) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) else ButtonDefaults.buttonColors()) {
                                Icon(icon = if (recording) FieldMindIcons.Stop else FieldMindIcons.Mic, contentDescription = null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text(if (recording) "Stop voice note" else "Record voice note")
                            }
                        }
                        AttachmentPreviewList(attachments, onCaptionChange = { index, caption -> attachments = attachments.mapIndexed { i, item -> if (i == index) item.copy(caption = caption) else item } }, onRemove = { remove -> attachments = attachments.filterIndexed { index, _ -> index != remove } })
                    }
                }

                CaptureStep("Connect & tag", "Summarize the evidence, tag it, and link a project.", FieldMindIcons.Link) {
                    FieldTextField(evidence, { evidence = it }, "Evidence summary")
                    FieldTextField(tags, { tags = it }, "Tags", supportingText = "Comma-separated: birds, behavior, evening")
                    if (projects.isNotEmpty()) {
                        Text("Link to project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ChoiceChips(listOf("No project") + projects.map { it.title }, projects.firstOrNull { it.id == projectId }?.title ?: "No project") { selected -> projectId = projects.firstOrNull { it.title == selected }?.id }
                    }
                }

                Button(onClick = {
                    if (subject.isBlank() || facts.isBlank()) scope.launch { snackbar.showSnackbar("Subject and factual notes are required.") } else { haptics.confirm(); viewModel.addObservation(subject, category, facts, confidence, manualLocation, tags, evidence, fieldContext, projectId, capturedLocation?.latitude, capturedLocation?.longitude, attachments) {
                        subject = ""; facts = ""; manualLocation = ""; tags = ""; evidence = ""; fieldContext = ""; attachments = emptyList(); capturedLocation = null
                        scope.launch { snackbar.showSnackbar("Observation saved to your archive.") }
                        onSaved()
                    }
                    }
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Icon(icon = FieldMindIcons.Check, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Save observation")
                }
            }
        }
    }
}

/** A labeled capture step: an icon + title/subtitle header above its grouped inputs. */
@Composable
private fun CaptureStep(title: String, subtitle: String, icon: MaterialSymbolIcon, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, size = 18.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.padding(start = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** Reminds the researcher to separate observed facts from interpretation. */
@Composable
private fun FactsInterpretationBanner() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon = FieldMindIcons.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, size = 18.dp)
        Text("Facts vs. interpretation: log what you sensed here; save guesses as a question or hypothesis.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

/** A blinking red dot + elapsed timer shown while a voice note is being recorded. */
@Composable
private fun RecordingIndicator(seconds: Int) {
    val transition = rememberInfiniteTransition(label = "rec")
    val alpha by transition.animateFloat(
        initialValue = 1f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "recDot"
    )
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(12.dp).graphicsLayer { this.alpha = alpha }.clip(CircleShape).background(MaterialTheme.colorScheme.error))
        Text("Recording…", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.weight(1f))
        Text("%d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

private fun DraftEvidenceAttachment.isImage(): Boolean =
    mimeType?.startsWith("image/") == true ||
        type.equals("Photo", true) || type.equals("Gallery", true) ||
        Regex("\\.(jpg|jpeg|png|webp|gif|heic|bmp)(\\?.*)?$", RegexOption.IGNORE_CASE).containsMatchIn(uri)

@Composable
private fun AttachmentPreviewList(items: List<DraftEvidenceAttachment>, onCaptionChange: (Int, String) -> Unit, onRemove: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, item ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (item.isImage()) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = item.caption.ifBlank { "Attached image" },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )
                        } else {
                            Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                                Icon(icon = if (item.type.equals("Audio", true)) FieldMindIcons.Mic else FieldMindIcons.File, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 26.dp)
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            InfoChip(item.type, icon = if (item.isImage()) FieldMindIcons.Gallery else FieldMindIcons.File)
                            Text(item.uri, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton({ onRemove(index) }) { Text("Remove") }
                    }
                    FieldTextField(item.caption, { onCaptionChange(index, it) }, "Caption")
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Projects workspace
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ProjectsScreen(viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit = { _, _ -> }, startTab: Int = 0) {
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val reports by viewModel.reports.collectAsState()
    var tab by remember(startTab) { mutableIntStateOf(startTab) }
    val tabs = listOf("Projects", "Questions", "Hypotheses", "Data", "Reports")
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)) {
            FieldScreenHeader("Projects", "Tie questions, evidence, data, and reports together.", icon = FieldMindIcons.Projects)
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 20.dp, containerColor = MaterialTheme.colorScheme.background) {
            tabs.forEachIndexed { i, label -> Tab(tab == i, { tab = i }, text = { Text(label) }) }
        }
        when (tab) {
            0 -> ProjectPanel(viewModel, projects, onOpenDetail)
            1 -> QuestionPanel(viewModel, questions, onOpenDetail)
            2 -> HypothesisPanel(viewModel, hypotheses, questions, onOpenDetail)
            3 -> DataToolPanel(viewModel, data, onOpenDetail)
            4 -> ReportPanel(viewModel, reports, onOpenDetail)
        }
    }
}

@Composable
private fun AddButton(label: String, onClick: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Button(onClick = { haptics.light(); onClick() }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Icon(icon = FieldMindIcons.Add, contentDescription = null, size = 20.dp); Spacer(Modifier.size(8.dp)); Text(label)
    }
}

private fun panelPadding() = PaddingValues(20.dp, 4.dp, 20.dp, 96.dp)

@Composable
private fun ProjectPanel(viewModel: FieldMindViewModel, items: List<ProjectEntity>, onOpenDetail: (String, Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton("Create project") { show = true } }
        if (items.isEmpty()) item { EmptyState("No projects yet", "A workspace ties questions, observations, sources, data, analysis, conclusions, and reports together.", icon = FieldMindIcons.Project) }
        items(items) { EntityCard(it.title, "project", body = it.objective.ifBlank { it.researchQuestion.ifBlank { "Open project workspace" } }, meta = listOf(it.status, it.topicType)) { onOpenDetail("project", it.id) } }
    }
    if (show) NewProjectDialog(viewModel) { show = false }
}

@Composable
private fun QuestionPanel(viewModel: FieldMindViewModel, items: List<QuestionEntity>, onOpenDetail: (String, Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton("Create question") { show = true } }
        if (items.isEmpty()) item { EmptyState("No questions yet", "Researchers collect questions before answers. Keep them testable and linked to evidence.", icon = FieldMindIcons.Question) }
        items(items) { EntityCard(it.questionText, "question", meta = listOf(it.status, it.priority, it.sourceType)) { onOpenDetail("question", it.id) } }
    }
    if (show) NewQuestionDialog(viewModel) { show = false }
}

@Composable
private fun HypothesisPanel(viewModel: FieldMindViewModel, items: List<HypothesisEntity>, questions: List<QuestionEntity>, onOpenDetail: (String, Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton("Create hypothesis") { show = true } }
        if (items.isEmpty()) item { EmptyState("No hypotheses yet", "State predictions, support/weaken criteria, and test method before looking for results.", icon = FieldMindIcons.Hypothesis) }
        items(items) { EntityCard(it.prediction, "hypothesis", body = "Evidence: ${it.evidenceNeeded}", meta = listOf(it.resultStatus, "confidence ${it.confidencePercent}%")) { onOpenDetail("hypothesis", it.id) } }
    }
    if (show) NewHypothesisDialog(viewModel, questions) { show = false }
}

@Composable
private fun DataToolPanel(viewModel: FieldMindViewModel, items: List<DataRecordEntity>, onOpenDetail: (String, Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    val grouped = items.groupBy { it.toolType.ifBlank { "Other" } }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton("Open data collection tools") { show = true } }
        if (items.isEmpty()) {
            item { EmptyState("Offline data tools", "Counter, measurement log, checklist, event log, weather log, site log, species tracker, comparison table.", icon = FieldMindIcons.Data) }
        } else {
            grouped.forEach { (tool, records) ->
                item { SectionHeader(tool, "${records.size} ${if (records.size == 1) "entry" else "entries"}") }
                items(records) { EntityCard(it.label, "data", body = it.notes.ifBlank { it.location }, meta = listOf("${it.value} ${it.unit}".trim())) { onOpenDetail("data", it.id) } }
            }
        }
    }
    if (show) NewDataRecordDialog(viewModel) { show = false }
}

@Composable
private fun ReportPanel(viewModel: FieldMindViewModel, items: List<ReportEntity>, onOpenDetail: (String, Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton("Build report") { show = true } }
        if (items.isEmpty()) item { EmptyState("No reports yet", "Write background, question, method, observations, results, interpretation, conclusion, limitations, and next steps.", icon = FieldMindIcons.Report) }
        items(items) { EntityCard(it.title, "report", body = it.conclusion.ifBlank { it.question }, meta = listOf(it.type, it.status)) { onOpenDetail("report", it.id) } }
    }
    if (show) NewReportDialog(viewModel) { show = false }
}

// ══════════════════════════════════════════════════════════════════════
//  Library (Sources / Reading / Flashcards / Learn)
// ══════════════════════════════════════════════════════════════════════

@Composable
fun KnowledgeLibraryScreen(
    viewModel: FieldMindViewModel,
    onNavigate: (FieldMindScreen) -> Unit = {},
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    onOpenReader: (String, String) -> Unit = { _, _ -> },
    startTab: Int = 0
) {
    val sources by viewModel.sources.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    var tab by remember(startTab) { mutableIntStateOf(startTab) }
    val tabs = listOf("Sources", "Notes", "Reading", "Flashcards", "Learn")
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)) {
            FieldScreenHeader("Library", "Sources, active reading, review cards, and skills.", icon = FieldMindIcons.Library)
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 20.dp, containerColor = MaterialTheme.colorScheme.background) {
            tabs.forEachIndexed { i, label -> Tab(tab == i, { tab = i }, text = { Text(label) }) }
        }
        when (tab) {
            0 -> SourcePanel(viewModel, sources, onOpenDetail)
            1 -> NotePanel(notes, onOpenDetail)
            2 -> PaperReadingPanel(sources, onOpenDetail)
            3 -> FlashcardPanel(viewModel, flashcards, onOpenDetail) { onNavigate(FieldMindScreen.Flashcards) }
            4 -> LearnPanel(viewModel, onOpenReader)
        }
    }
}

@Composable
private fun SourcePanel(viewModel: FieldMindViewModel, items: List<SourceEntity>, onOpenDetail: (String, Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton("Add source") { show = true } }
        if (items.isEmpty()) item { EmptyState("No sources yet", "Save articles, videos, PDFs, books, summaries, citations, and what each source taught you.", icon = FieldMindIcons.Source) }
        items(items) { EntityCard(it.title, "source", body = it.whatThisSourceTaughtMe.ifBlank { it.personalSummary }, meta = listOf(it.type, it.author.ifBlank { "Unknown author" }, it.readingStatus, it.importance, "reliability ${it.reliabilityScore}/5")) { onOpenDetail("source", it.id) } }
    }
    if (show) NewSourceDialog(viewModel) { show = false }
}

@Composable
private fun NotePanel(items: List<NoteEntity>, onOpenDetail: (String, Long) -> Unit) {
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionHeader("Notes", "Free-form thinking, separate from facts-only observations.") }
        if (items.isEmpty()) item { EmptyState("No notes yet", "Create one from Capture → Note.", icon = FieldMindIcons.Note) }
        items(items) { EntityCard(it.title, "note", body = it.body.ifBlank { "No body yet." }, meta = listOf(it.category, recentRelativeTime(it.updatedAt), if (it.attachmentUris.isBlank()) "No attachments" else "Attachments")) { onOpenDetail("note", it.id) } }
    }
}

@Composable
private fun PaperReadingPanel(items: List<SourceEntity>, onOpenDetail: (String, Long) -> Unit) {
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionHeader("Paper reading mode", "Prompts: topic, problem, method, result, unclear points, new question, next verification.") }
        if (items.isEmpty()) item { EmptyState("Add a source first", "Paper prompts are saved inside each source note.", icon = FieldMindIcons.Source) }
        items(items) { EntityCard(it.title, "read", body = it.paperNotes.ifBlank { "Open source detail and answer active-reading prompts." }, meta = listOf(it.readingStatus.ifBlank { "Not started" })) { onOpenDetail("source", it.id) } }
    }
}

@Composable
private fun FlashcardPanel(viewModel: FieldMindViewModel, items: List<FlashcardEntity>, onOpenDetail: (String, Long) -> Unit, onStartReview: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onStartReview, Modifier.weight(1f), shape = RoundedCornerShape(16.dp), enabled = items.isNotEmpty()) {
                    Icon(icon = FieldMindIcons.Flip, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Review (${items.size})")
                }
                OutlinedButton(onClick = { show = true }, Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Icon(icon = FieldMindIcons.Add, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("New card")
                }
            }
        }
        if (items.isEmpty()) item { EmptyState("No flashcards yet", "Turn terms, definitions, mistakes, source concepts, and questions into review cards.", icon = FieldMindIcons.Flashcard) }
        items(items) { LibraryFlashcard(it) { onOpenDetail("flashcard", it.id) } }
    }
    if (show) NewFlashcardDialog(viewModel) { show = false }
}

/** A single library flashcard: shows the prompt; the answer stays hidden until tapped. */
@Composable
private fun LibraryFlashcard(card: FlashcardEntity, onOpenDetail: () -> Unit) {
    var revealed by remember(card.id) { mutableStateOf(false) }
    val accent = FieldMindTheme.colors.flashcard
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { revealed = !revealed },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon = FieldMindIcons.Flashcard, contentDescription = null, tint = accent, size = 18.dp) }
                Text(card.type, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Icon(
                    icon = if (revealed) FieldMindIcons.VisibilityOff else FieldMindIcons.Visibility,
                    contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp
                )
            }
            Text(card.front, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (revealed) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(card.back, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = onOpenDetail, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text("Open card"); Spacer(Modifier.size(4.dp)); Icon(icon = FieldMindIcons.Forward, contentDescription = null, size = 16.dp)
                }
            } else {
                Text("Tap to reveal answer", style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }
    }
}

private data class ResearchMilestone(val title: String, val body: String, val icon: MaterialSymbolIcon, val resource: LearnResource)

private val beginnerResearchMilestones = listOf(
    ResearchMilestone("Observe carefully", "Separate facts from interpretation, then document time, place, context, and evidence.", FieldMindIcons.Observation, LearnResource("Understanding Science", "Guide", "https://undsci.berkeley.edu/", "Science starts with careful observation and honest uncertainty.")),
    ResearchMilestone("Ask researchable questions", "Turn curiosity into a question you can observe, compare, measure, or verify.", FieldMindIcons.Question, LearnResource("Research as inquiry", "Framework", "https://www.ala.org/acrl/standards/ilframework", "Research grows from increasingly focused questions.")),
    ResearchMilestone("Evaluate sources", "Use DOI/ISBN, venue, author, and evidence quality before trusting a claim.", FieldMindIcons.Source, LearnResource("Crossref REST API", "Reference", "https://www.production.crossref.org/documentation/retrieve-metadata/rest-api/", "Look up DOI metadata and verify bibliographic details.")),
    ResearchMilestone("Plan a small investigation", "Define method, site, sample, bias controls, and safety before collecting data.", FieldMindIcons.Project, LearnResource("Framework for Science Education", "Guide", "https://nap.nationalacademies.org/resource/13165/interactive/", "Science practices include planning investigations and analyzing evidence.")),
    ResearchMilestone("Collect usable data", "Record measurements, counts, checklists, and metadata consistently.", FieldMindIcons.Data, LearnResource("OpenIntro Statistics", "Book", "https://www.openintro.org/book/os/", "A free introduction to variation, sampling, and data summaries.")),
    ResearchMilestone("Explain with evidence", "Write claim, evidence, reasoning, limits, and next questions without overstating certainty.", FieldMindIcons.Report, LearnResource("Purdue OWL citation resources", "Guide", "https://owl.purdue.edu/owl/research_and_citation/resources.html", "Guides for writing, citing, and communicating research."))
)

@Composable
private fun LearnPanel(viewModel: FieldMindViewModel, onOpenReader: (String, String) -> Unit = { _, _ -> }) {
    val observations by viewModel.observations.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val signals = remember(observations, questions, sources, projects) {
        buildList {
            observations.take(8).forEach { add(it.category); add(it.tags); add(it.subject) }
            questions.take(6).forEach { add(it.category); add(it.questionText) }
            sources.take(6).forEach { add(it.type); add(it.title); add(it.publisherOrJournal) }
            projects.take(4).forEach { add(it.topicType); add(it.title) }
        }.joinToString(" ")
    }
    val next = remember(observations.size, questions.size, sources.size, projects.size, reports.size) {
        when {
            observations.isEmpty() -> beginnerResearchMilestones[0]
            questions.isEmpty() -> beginnerResearchMilestones[1]
            sources.isEmpty() -> beginnerResearchMilestones[2]
            projects.isEmpty() -> beginnerResearchMilestones[3]
            reports.isEmpty() -> beginnerResearchMilestones[5]
            else -> beginnerResearchMilestones[4]
        }
    }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ResearchJourneyHero(next, signals, onOpenReader) }
        item { SectionHeader("Beginner researcher path", "A guided path from observation to evidence-based communication.") }
        items(beginnerResearchMilestones) { milestone -> ResearchMilestoneCard(milestone, onOpenReader) }
        item { SectionHeader("Based on your activity", if (signals.isBlank()) "Start capturing to personalize this section" else "Recent topics shape these suggestions") }
        if (signals.isBlank()) {
            item { EntityCard("Start with one observation", "observation", body = "Capture one facts-only observation, then return here for a tailored next step.") }
        } else {
            items(recommendedResources(listOf(signals))) { rec -> EntityCard(rec.resource.title, "learn", body = rec.resource.why, meta = listOf(rec.resource.kind, rec.path)) { onOpenReader(rec.resource.url, rec.resource.title) } }
        }
        item { SectionHeader("Curated reference library", "Expand when you want deeper subject-specific learning.") }
        items(LearnLibrary) { category -> LearnCategoryCard(category) { res -> onOpenReader(res.url, res.title) } }
        item { SectionHeader("Optional online discovery", "Use verified metadata sources; never trust generated citations without checking.") }
        item { AssistantPanel(viewModel) }
        item { OnlineApiProposalCard() }
    }
}

@Composable
private fun ResearchJourneyHero(next: ResearchMilestone, signals: String, onOpenReader: (String, String) -> Unit) {
    Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(next.icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 30.dp) }
                Column(Modifier.weight(1f)) {
                    Text("Recommended next step", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f), fontWeight = FontWeight.SemiBold)
                    Text(next.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            }
            Text(next.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f))
            if (signals.isNotBlank()) InfoChip("Personalized from recent activity", icon = FieldMindIcons.Sparkle, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Button(onClick = { onOpenReader(next.resource.url, next.resource.title) }, shape = RoundedCornerShape(16.dp)) { Text("Open starter resource") }
        }
    }
}

@Composable
private fun ResearchMilestoneCard(milestone: ResearchMilestone, onOpenReader: (String, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    EntityCard(milestone.title, "learn", body = milestone.body, meta = listOf(milestone.resource.kind), onClick = { expanded = !expanded })
    AnimatedVisibility(expanded) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(milestone.resource.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(milestone.resource.why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { onOpenReader(milestone.resource.url, milestone.resource.title) }, contentPadding = PaddingValues(0.dp)) { Text("Open resource"); Spacer(Modifier.size(4.dp)); Icon(FieldMindIcons.Forward, null, size = 18.dp) }
            }
        }
    }
}

@Composable
private fun GuidedPathCard(path: GuidedPath, onOpenReader: (String, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val accent = FieldMindTheme.colors.accentFor("learn")
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(icon = FieldMindIcons.School, contentDescription = null, tint = accent, size = 22.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(path.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(path.goal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(icon = if (expanded) FieldMindIcons.Up else FieldMindIcons.Down, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
            }
            if (expanded) {
                path.steps.forEachIndexed { i, res ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onOpenReader(res.url, res.title) }.background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = accent)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(res.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("${res.kind} · ${res.why}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(icon = FieldMindIcons.Book, contentDescription = null, tint = accent, size = 18.dp)
                    }
                }
            } else {
                Text("${path.steps.size} steps", style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }
    }
}

/**
 * In-app reader for a Learn resource. Loads the page in a WebView so users can read without
 * leaving FieldMind, with a top-bar action to open it in their browser for further reading.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LearnReaderScreen(url: String, title: String, onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var loading by remember(url) { mutableStateOf(true) }
    var errorMessage by remember(url) { mutableStateOf<String?>(null) }
    var retryKey by remember(url) { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val background = MaterialTheme.colorScheme.background
    LaunchedEffect(url, retryKey) {
        loading = true
        errorMessage = null
        kotlinx.coroutines.delay(10_000)
        if (loading) errorMessage = "Still loading. This page may block embedded readers."
    }
    BackHandler(enabled = true) {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
    }
    Column(Modifier.fillMaxSize().background(background)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) { Icon(icon = FieldMindIcons.Back, contentDescription = "Back", size = 22.dp) }
            Text(title.ifBlank { "Reader" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = { runCatching { uriHandler.openUri(url) } }) { Icon(icon = FieldMindIcons.OpenLink, contentDescription = "Open in browser", size = 22.dp) }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, startedUrl: String?, favicon: android.graphics.Bitmap?) {
                                loading = true
                                errorMessage = null
                            }
                            override fun onPageFinished(view: WebView?, finishedUrl: String?) { loading = false }
                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                if (request?.isForMainFrame != false) {
                                    loading = false
                                    errorMessage = error?.description?.toString() ?: "Could not load this page inside FieldMind."
                                }
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        loadUrl(url)
                        webView = this
                    }
                },
                update = { if (it.url != url) it.loadUrl(url) }
            )
            errorMessage?.let { message ->
                Card(Modifier.align(Alignment.TopCenter).padding(20.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                            Text("Reader fallback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { retryKey++; webView?.reload() }, shape = RoundedCornerShape(14.dp)) { Text("Retry") }
                            Button(onClick = { runCatching { uriHandler.openUri(url) } }, shape = RoundedCornerShape(14.dp)) { Text("Open browser") }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Search archive
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ArchiveScreen(viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit = { _, _ -> }) {
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    var query by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { FieldScreenHeader("Search archive", "Search forever by topic, date, place, source, project, and keyword.", icon = FieldMindIcons.Search) }
        item {
            OutlinedTextField(query, { query = it }, label = { Text("Search") }, leadingIcon = { Icon(icon = FieldMindIcons.Search, contentDescription = null, size = 20.dp) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true)
        }
        val q = query.trim().lowercase()
        fun matches(vararg parts: String) = q.isBlank() || parts.any { it.lowercase().contains(q) }
        items(observations.filter { matches(it.subject, it.category, it.factsOnlyNotes, it.manualLocation, it.tags) }) { EntityCard(it.subject, "observation", body = it.factsOnlyNotes.take(120), confidence = it.confidenceLevel, meta = listOf(it.category)) { onOpenDetail("observation", it.id) } }
        items(notes.filter { matches(it.title, it.body, it.category, it.tags) }) { EntityCard(it.title, "note", body = it.body.take(120), meta = listOf(it.category, recentRelativeTime(it.updatedAt))) { onOpenDetail("note", it.id) } }
        items(questions.filter { matches(it.questionText, it.category, it.status) }) { EntityCard(it.questionText, "question", meta = listOf(it.status)) { onOpenDetail("question", it.id) } }
        items(projects.filter { matches(it.title, it.topicType, it.objective, it.researchQuestion) }) { EntityCard(it.title, "project", body = it.objective, meta = listOf(it.topicType)) { onOpenDetail("project", it.id) } }
        items(sources.filter { matches(it.title, it.author, it.type, it.dateOrYear, it.link, it.doiOrIsbn, it.publisherOrJournal, it.accessDate, it.fileUri, it.citationStyleNote, it.importance, it.readingStatus, it.personalSummary, it.keyFindings, it.questionsGenerated, it.paperNotes) }) { EntityCard(it.title, "source", body = it.whatThisSourceTaughtMe, meta = listOf(it.type)) { onOpenDetail("source", it.id) } }
        items(reports.filter { matches(it.title, it.type, it.question, it.conclusion) }) { EntityCard(it.title, "report", body = it.conclusion, meta = listOf(it.type)) { onOpenDetail("report", it.id) } }
        items(flashcards.filter { matches(it.front, it.back, it.type) }) { EntityCard(it.front, "flashcard", body = it.back, meta = listOf(it.type)) { onOpenDetail("flashcard", it.id) } }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Backup & export
// ══════════════════════════════════════════════════════════════════════

@Composable
fun BackupExportScreen(viewModel: FieldMindViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val attachmentMode by viewModel.fieldSettings.attachmentExportMode.collectAsState()
    var pendingText by remember { mutableStateOf("") }
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) scope.launch { snackbar.showSnackbar("Export cancelled.") } else runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(pendingText.toByteArray()) } }.onSuccess { scope.launch { snackbar.showSnackbar("Export written.") } }.onFailure { scope.launch { snackbar.showSnackbar("Export failed: ${it.localizedMessage}") } }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { FieldScreenHeader("Backup & export", "Your research notes remain portable and owned by you.", icon = FieldMindIcons.Export) }
            item { EntityCard("Attachment mode", "data", body = "$attachmentMode. Choose a different default in Settings.") }
            item { ExportRow("Observations CSV", FieldMindIcons.Observation) { pendingText = FieldMindExport.observationsCsv(observations); createDoc.launch("fieldmind-observations.csv") } }
            item { ExportRow("Data CSV", FieldMindIcons.Data) { pendingText = FieldMindExport.dataCsv(data); createDoc.launch("fieldmind-data.csv") } }
            item { ExportRow("Sources CSV", FieldMindIcons.Source) { pendingText = FieldMindExport.sourcesCsv(sources); createDoc.launch("fieldmind-sources.csv") } }
            item { ExportRow("PDF-ready HTML", FieldMindIcons.Article) { pendingText = FieldMindExport.pdfReadyHtml(projects, observations, sources, reports); createDoc.launch("fieldmind-print-export.html") } }
            item { ExportRow("Dashboard SVG", FieldMindIcons.Graph) { pendingText = FieldMindExport.dashboardSvg(observations, sources, projects, notes); createDoc.launch("fieldmind-dashboard.svg") } }
            item { ExportRow("Archive JSON", FieldMindIcons.Archive) { pendingText = FieldMindExport.archiveJson(observations, notes, questions, hypotheses, projects, sources, data, reports, flashcards); createDoc.launch("fieldmind-archive.json") } }
            item { ExportRow("Reports Markdown", FieldMindIcons.Report) { pendingText = reports.joinToString("\n\n---\n\n") { FieldMindExport.buildMarkdownReport(it) }; createDoc.launch("fieldmind-reports.md") } }
        }
    }
}

@Composable
private fun ExportRow(label: String, icon: MaterialSymbolIcon, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Icon(icon = FieldMindIcons.Export, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Settings
// ══════════════════════════════════════════════════════════════════════

@Composable
fun FieldMindSettingsScreen(viewModel: FieldMindViewModel? = null, onBack: () -> Unit, onResetOnboarding: () -> Unit) {
    val context = LocalContext.current
    val settings = viewModel?.fieldSettings ?: chromahub.rhythm.app.features.field.data.settings.FieldMindSettings.getInstance(context)
    val goal by settings.dailyObservationGoal.collectAsState()
    val category by settings.defaultCategory.collectAsState()
    val confidence by settings.defaultConfidence.collectAsState()
    val locationMode by settings.locationMode.collectAsState()
    val media by settings.mediaAttachmentsEnabled.collectAsState()
    val audio by settings.audioRecordingEnabled.collectAsState()
    val exportMode by settings.attachmentExportMode.collectAsState()
    val ai by settings.geminiEnabled.collectAsState()
    val key by settings.geminiApiKey.collectAsState()
    val model by settings.geminiModel.collectAsState()
    val confirm by settings.aiRequireConfirmBeforeSave.collectAsState()
    val sendAttachments by settings.aiSendAttachments.collectAsState()
    val reminders by settings.remindersEnabled.collectAsState()
    val streaks by settings.streaksEnabled.collectAsState()
    val exportFormat by settings.defaultExportFormat.collectAsState()
    val privacy by settings.privacyLockEnabled.collectAsState()
    val dynamicColor by settings.dynamicColorEnabled.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { FieldScreenHeader("Settings", "Capture, appearance, AI, export, and privacy.", icon = FieldMindIcons.Settings, actionIcon = FieldMindIcons.Back, onAction = onBack) }

        item {
            SettingsGroup("Appearance") {
                ToggleItem("Material You dynamic color", "Use system wallpaper colors that auto-adapt to light/dark. Off keeps the FieldMind brand palette.", dynamicColor, settings::setDynamicColorEnabled, FieldMindIcons.Palette)
            }
        }

        item {
            SettingsGroup("Capture defaults") {
                StepperItem("Daily observation goal", "Drives the Today dashboard and progress ring.", goal, FieldMindIcons.Today) { settings.setDailyObservationGoal(it) }
                SettingDivider()
                ChoiceItem("Default category", observationCategories, category, FieldMindIcons.Observation, settings::setDefaultCategory)
                SettingDivider()
                ChoiceItem("Default confidence", confidenceOptions, confidence, FieldMindIcons.Check, settings::setDefaultConfidence)
                SettingDivider()
                ChoiceItem("Location mode", listOf("Manual only", "Approximate", "Precise"), locationMode, FieldMindIcons.Location, settings::setLocationMode)
            }
        }

        item {
            SettingsGroup("Evidence & media") {
                ToggleItem("Media attachments", "Enable camera, gallery, and file evidence tools.", media, settings::setMediaAttachmentsEnabled, FieldMindIcons.Camera)
                SettingDivider()
                ToggleItem("Audio recording", "Enable voice-note evidence capture with an in-app recording indicator.", audio, settings::setAudioRecordingEnabled, FieldMindIcons.Mic)
                SettingDivider()
                ChoiceItem("Attachment export", listOf("Reference URIs", "Copy media later", "Skip media"), exportMode, FieldMindIcons.Export, settings::setAttachmentExportMode)
            }
        }

        item {
            SettingsGroup("Gemini assistant", "Optional. Nothing is sent without an explicit action.") {
                ToggleItem("Enable Gemini", "Review factuality, suggest papers, and answer questions.", ai, settings::setGeminiEnabled, FieldMindIcons.Sparkle)
                if (ai) {
                    SettingDivider()
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = key, onValueChange = settings::setGeminiApiKey, label = { Text("Gemini API key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true, supportingText = { Text(if (key.isBlank()) "No key saved — get one at aistudio.google.com/apikey." else "Key saved locally on this device only.") })
                    }
                    SettingDivider()
                    ChoiceItem("Model", listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash"), model, FieldMindIcons.Bolt, settings::setGeminiModel)
                    SettingDivider()
                    ToggleItem("Confirm before saving AI output", "AI suggestions stay as previews unless you apply them.", confirm, settings::setAiRequireConfirmBeforeSave, FieldMindIcons.Check)
                    SettingDivider()
                    ToggleItem("Allow attachment context", "Off by default to protect field evidence privacy.", sendAttachments, settings::setAiSendAttachments, FieldMindIcons.File)
                }
            }
        }

        item {
            SettingsGroup("Discipline & ownership") {
                ToggleItem("Reminders", "Notification permission is only requested when enabled.", reminders, settings::setRemindersEnabled, FieldMindIcons.Notifications)
                SettingDivider()
                ToggleItem("Streaks", "Discipline made visible without replacing real work.", streaks, settings::setStreaksEnabled, FieldMindIcons.Streak)
            }
        }

        item {
            SettingsGroup("Export & privacy") {
                ChoiceItem("Default export format", listOf("Markdown", "CSV", "JSON", "Plain text"), exportFormat, FieldMindIcons.Export, settings::setDefaultExportFormat)
                SettingDivider()
                ToggleItem("Privacy lock", "Persisted toggle; wires to the app lock flow when available.", privacy, settings::setPrivacyLockEnabled, FieldMindIcons.Lock)
            }
        }

        if (viewModel != null) item { SettingsExportSection(viewModel) }

        item { AboutSection() }

        item { OutlinedButton(onClick = onResetOnboarding, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Reset onboarding") } }
    }
}

@Composable
private fun SettingsExportSection(viewModel: FieldMindViewModel) {
    val context = LocalContext.current
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    var pendingText by remember { mutableStateOf("") }
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(pendingText.toByteArray()) } }
            .onSuccess { android.widget.Toast.makeText(context, "Export written.", android.widget.Toast.LENGTH_SHORT).show() }
            .onFailure { android.widget.Toast.makeText(context, "Export failed: ${it.localizedMessage}", android.widget.Toast.LENGTH_LONG).show() }
    }
    SettingsGroup("Export data", "Your research notes stay portable and owned by you.") {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportRow("Observations CSV", FieldMindIcons.Observation) { pendingText = FieldMindExport.observationsCsv(observations); createDoc.launch("fieldmind-observations.csv") }
            ExportRow("Data CSV", FieldMindIcons.Data) { pendingText = FieldMindExport.dataCsv(data); createDoc.launch("fieldmind-data.csv") }
            ExportRow("Sources CSV", FieldMindIcons.Source) { pendingText = FieldMindExport.sourcesCsv(sources); createDoc.launch("fieldmind-sources.csv") }
            ExportRow("PDF-ready HTML", FieldMindIcons.Article) { pendingText = FieldMindExport.pdfReadyHtml(projects, observations, sources, reports); createDoc.launch("fieldmind-print-export.html") }
            ExportRow("Dashboard SVG", FieldMindIcons.Graph) { pendingText = FieldMindExport.dashboardSvg(observations, sources, projects, notes); createDoc.launch("fieldmind-dashboard.svg") }
            ExportRow("Archive JSON", FieldMindIcons.Archive) { pendingText = FieldMindExport.archiveJson(observations, notes, questions, hypotheses, projects, sources, data, reports, flashcards); createDoc.launch("fieldmind-archive.json") }
            ExportRow("Reports Markdown", FieldMindIcons.Report) { pendingText = reports.joinToString("\n\n---\n\n") { FieldMindExport.buildMarkdownReport(it) }; createDoc.launch("fieldmind-reports.md") }
        }
    }
}

@Composable
private fun AboutSection() {
    val uriHandler = LocalUriHandler.current
    SettingsGroup("About", "FieldMind — observe, question, research clearly.") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("FieldMind is a free, offline-first research notebook for curious naturalists, students, and citizen scientists.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Credits & acknowledgements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            CreditRow("Rhythm", "The open-source app this project is built upon.", "https://github.com/cromaguy/Rhythm", uriHandler)
            CreditRow("Material Symbols & Material 3", "Google's icon set and design system.", "https://fonts.google.com/icons", uriHandler)
            CreditRow("Jetpack Compose", "Android's modern declarative UI toolkit.", "https://developer.android.com/jetpack/compose", uriHandler)
            CreditRow("Crossref", "Free scholarly metadata API.", "https://www.crossref.org", uriHandler)
            CreditRow("OpenAlex", "Open catalog of papers, authors, and venues.", "https://openalex.org", uriHandler)
            CreditRow("arXiv", "Open-access research preprints.", "https://arxiv.org", uriHandler)
            CreditRow("Open Library", "Open, editable library catalog.", "https://openlibrary.org", uriHandler)
            CreditRow("Semantic Scholar", "AI-powered research paper search.", "https://www.semanticscholar.org", uriHandler)
            Text("Made with care for people who learn by looking closely.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreditRow(title: String, subtitle: String, url: String, uriHandler: androidx.compose.ui.platform.UriHandler) {
    Row(Modifier.fillMaxWidth().clickable { runCatching { uriHandler.openUri(url) } }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon = FieldMindIcons.OpenLink, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Detail + backlinks
// ══════════════════════════════════════════════════════════════════════

@Composable
fun DetailScreen(kind: String, id: Long, viewModel: FieldMindViewModel, onBack: () -> Unit, onOpenDetail: (String, Long) -> Unit = { _, _ -> }) {
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val title = kind.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    var showEdit by remember(kind, id) { mutableStateOf(false) }
    var showDelete by remember(kind, id) { mutableStateOf(false) }
    val editable = kind in setOf("observation", "note", "question", "hypothesis", "project", "source", "data", "report", "flashcard")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { FieldScreenHeader(title, "Record detail workspace", icon = FieldMindIcons.iconFor(kind), actionIcon = FieldMindIcons.Back, onAction = onBack) }
        if (editable) item { DetailActions(onEdit = { showEdit = true }, onDelete = { showDelete = true }) }
        when (kind) {
            "note" -> notes.firstOrNull { it.id == id }?.let { n ->
                item { DetailBody(n.title, "note", listOf("Category" to n.category, "Tags" to n.tags, "Body" to n.body, "Attachments" to n.attachmentUris, "Updated" to recentRelativeTime(n.updatedAt))) }
                item { BacklinksPanel(buildList {
                    projects.firstOrNull { it.id == n.projectId }?.let { add(Triple("project", it.title, it.id)) }
                    sources.firstOrNull { it.id == n.sourceId }?.let { add(Triple("source", it.title, it.id)) }
                }, onOpenDetail) }
            }
            "observation" -> observations.firstOrNull { it.id == id }?.let { o ->
                item { DetailBody(o.subject, "observation", listOf("Date" to "${o.date} ${o.time}", "Category" to o.category, "Confidence" to o.confidenceLevel, "Location" to o.manualLocation.ifBlank { "None" }, "GPS" to (o.latitude?.let { "${o.latitude}, ${o.longitude}" } ?: "Not captured"), "Tags" to o.tags, "Facts" to o.factsOnlyNotes, "Evidence" to o.evidenceSummary, "Context" to o.moodOrContext)) }
                if (o.latitude != null && o.longitude != null) item { ObservationLocationCard(o.latitude, o.longitude, o.manualLocation) }
                item { ObservationAttachmentsPanel(viewModel, o.id) }
                item { BacklinksPanel(buildList {
                    projects.firstOrNull { it.id == o.projectId }?.let { add(Triple("project", it.title, it.id)) }
                    data.filter { it.observationId == o.id }.forEach { add(Triple("data", it.label, it.id)) }
                }, onOpenDetail) }
            }
            "question" -> questions.firstOrNull { it.id == id }?.let { qn ->
                item { DetailBody(qn.questionText, "question", listOf("Category" to qn.category, "Source" to qn.sourceType, "Status" to qn.status, "Priority" to qn.priority)) }
                item { QuestionAnswerCard(qn) { ans -> viewModel.setQuestionAnswer(qn, ans) } }
                item { BacklinksPanel(buildList {
                    projects.firstOrNull { it.id == qn.relatedProjectId }?.let { add(Triple("project", it.title, it.id)) }
                    hypotheses.filter { it.linkedQuestionId == qn.id }.forEach { add(Triple("hypothesis", it.prediction, it.id)) }
                }, onOpenDetail) }
            }
            "hypothesis" -> hypotheses.firstOrNull { it.id == id }?.let { h ->
                item { DetailBody(h.prediction, "hypothesis", listOf("Reasoning" to h.reasoning, "Support" to h.supportCriteria, "Weaken" to h.weakeningCriteria, "Test" to h.testMethod, "Result" to h.resultStatus, "Confidence" to "${h.confidencePercent}%")) }
                item { BacklinksPanel(buildList {
                    questions.firstOrNull { it.id == h.linkedQuestionId }?.let { add(Triple("question", it.questionText, it.id)) }
                }, onOpenDetail) }
            }
            "project" -> projects.firstOrNull { it.id == id }?.let { p ->
                item { DetailBody(p.title, "project", listOf("Objective" to p.objective, "Question" to p.researchQuestion, "Background" to p.backgroundNotes, "Methods" to p.methods, "Data" to p.dataSummary, "Analysis" to p.analysis, "Conclusion" to p.conclusion, "Future" to p.futureQuestions)) }
                item { BacklinksPanel(buildList {
                    observations.filter { it.projectId == p.id }.forEach { add(Triple("observation", it.subject, it.id)) }
                    questions.filter { it.relatedProjectId == p.id }.forEach { add(Triple("question", it.questionText, it.id)) }
                    sources.filter { it.relatedProjectId == p.id }.forEach { add(Triple("source", it.title, it.id)) }
                    data.filter { it.projectId == p.id }.forEach { add(Triple("data", it.label, it.id)) }
                    reports.filter { it.projectId == p.id }.forEach { add(Triple("report", it.title, it.id)) }
                }, onOpenDetail) }
            }
            "source" -> sources.firstOrNull { it.id == id }?.let { s ->
                item { DetailBody(s.title, "source", listOf("Type" to s.type, "Author" to s.author, "Year" to s.dateOrYear, "DOI / ISBN" to s.doiOrIsbn, "Publisher / journal" to s.publisherOrJournal, "Link" to s.link, "Access date" to s.accessDate, "File" to s.fileUri, "Citation note" to s.citationStyleNote, "Importance" to s.importance, "Reading status" to s.readingStatus, "Project" to (projects.firstOrNull { it.id == s.relatedProjectId }?.title ?: "None"), "Main idea" to s.personalSummary, "Key findings" to s.keyFindings, "Taught me" to s.whatThisSourceTaughtMe, "Paper prompts" to s.paperNotes, "Questions" to s.questionsGenerated)) }
                item { SourceActionPanel(s, projects, viewModel, onOpenDetail) }
                item { BacklinksPanel(buildList {
                    projects.firstOrNull { it.id == s.relatedProjectId }?.let { add(Triple("project", it.title, it.id)) }
                    flashcards.filter { it.sourceId == s.id }.forEach { add(Triple("flashcard", it.front, it.id)) }
                }, onOpenDetail) }
            }
            "data" -> data.firstOrNull { it.id == id }?.let { d ->
                item { DetailBody(d.label, "data", listOf("Tool" to d.toolType, "Value" to "${d.value} ${d.unit}".trim(), "Location" to d.location, "Notes" to d.notes)) }
                item { BacklinksPanel(buildList {
                    projects.firstOrNull { it.id == d.projectId }?.let { add(Triple("project", it.title, it.id)) }
                    observations.firstOrNull { it.id == d.observationId }?.let { add(Triple("observation", it.subject, it.id)) }
                }, onOpenDetail) }
            }
            "report" -> reports.firstOrNull { it.id == id }?.let { r ->
                item { DetailBody(r.title, "report", listOf("Type" to r.type, "Status" to r.status, "Question" to r.question, "Conclusion" to r.conclusion)) }
                item { Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) { Text(FieldMindExport.buildMarkdownReport(r), Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall) } }
                item { BacklinksPanel(buildList { projects.firstOrNull { it.id == r.projectId }?.let { add(Triple("project", it.title, it.id)) } }, onOpenDetail) }
            }
            "flashcard" -> flashcards.firstOrNull { it.id == id }?.let { f ->
                item { DetailBody(f.front, "flashcard", listOf("Type" to f.type, "Back" to f.back)) }
                item { BacklinksPanel(buildList {
                    sources.firstOrNull { it.id == f.sourceId }?.let { add(Triple("source", it.title, it.id)) }
                    projects.firstOrNull { it.id == f.projectId }?.let { add(Triple("project", it.title, it.id)) }
                }, onOpenDetail) }
            }
        }
    }
    if (showEdit) EditEntityDialog(kind, id, viewModel) { showEdit = false }
    if (showDelete) ConfirmDeleteDialog(kind, onDismiss = { showDelete = false }) {
        deleteEntityByKind(kind, id, viewModel); showDelete = false; onBack()
    }
}

@Composable
private fun DetailActions(onEdit: () -> Unit, onDelete: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledTonalButton(onClick = { haptics.light(); onEdit() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
            Icon(icon = FieldMindIcons.Edit, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Edit")
        }
        OutlinedButton(
            onClick = { haptics.light(); onDelete() },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(icon = FieldMindIcons.Delete, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Delete")
        }
    }
}

/** Inline answer editor on a question detail screen. */
@Composable
private fun SourceActionPanel(source: SourceEntity, projects: List<ProjectEntity>, viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptics = rememberFieldMindHaptics()
    var showProjects by remember { mutableStateOf(false) }
    val citation = remember(source) { viewModel.buildSourceCitation(source) }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Source actions", "Open, cite, prioritize, and turn reading into review cards.")
            SourcePreviewCard(source.link, source.fileUri)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { haptics.light(); runCatching { uriHandler.openUri(source.link) } }, enabled = source.link.isNotBlank(), modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(FieldMindIcons.OpenLink, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Open link")
                }
                FilledTonalButton(onClick = {
                    haptics.light()
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.fileUri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    }
                }, enabled = source.fileUri.isNotBlank(), modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(FieldMindIcons.File, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Open file")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { haptics.light(); clipboard.setText(AnnotatedString(citation)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(FieldMindIcons.Article, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Copy citation")
                }
                OutlinedButton(onClick = { haptics.confirm(); viewModel.toggleSourceImportant(source) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(FieldMindIcons.Favorite, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text(if (source.importance == "Normal") "Important" else "Normal")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { haptics.confirm(); viewModel.markSourceRead(source) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), enabled = source.readingStatus != "Read") {
                    Icon(FieldMindIcons.Done, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Mark read")
                }
                OutlinedButton(onClick = {
                    haptics.confirm()
                    if (source.title.isNotBlank() && source.whatThisSourceTaughtMe.isNotBlank()) viewModel.addFlashcard(source.title, source.whatThisSourceTaughtMe, "source-takeaway", source.id, source.relatedProjectId)
                    if (source.keyFindings.isNotBlank()) viewModel.addFlashcard("Key finding from ${source.title}", source.keyFindings, "source-finding", source.id, source.relatedProjectId)
                    if (source.questionsGenerated.isNotBlank()) viewModel.addFlashcard("Question from ${source.title}", source.questionsGenerated, "source-question", source.id, source.relatedProjectId)
                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(FieldMindIcons.Flashcard, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Cards")
                }
            }
            TextButton(onClick = { haptics.light(); showProjects = !showProjects }) {
                Icon(FieldMindIcons.Project, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Link to project")
            }
            AnimatedVisibility(showProjects) {
                ChoiceChips(listOf("No project") + projects.map { it.title }, projects.firstOrNull { it.id == source.relatedProjectId }?.title ?: "No project") { selected ->
                    haptics.confirm()
                    viewModel.linkSourceToProject(source, projects.firstOrNull { it.title == selected }?.id)
                    showProjects = false
                }
            }
            if (source.relatedProjectId != null) {
                projects.firstOrNull { it.id == source.relatedProjectId }?.let { project ->
                    EntityCard(project.title, "project", body = project.objective.ifBlank { project.researchQuestion }, meta = listOf("Linked project")) { onOpenDetail("project", project.id) }
                }
            }
        }
    }
}

@Composable
private fun QuestionAnswerCard(question: QuestionEntity, onSave: (String) -> Unit) {
    var editing by remember(question.id, question.answer) { mutableStateOf(question.answer.isBlank()) }
    var draft by remember(question.id) { mutableStateOf(question.answer) }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon = FieldMindIcons.Answer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                Text("Answer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (editing) {
                FieldTextField(draft, { draft = it }, "What did you conclude?", minLines = 3, supportingText = "Saving marks this question as Answered.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onSave(draft); editing = false }, shape = RoundedCornerShape(14.dp), enabled = draft.isNotBlank()) { Text("Save answer") }
                    if (question.answer.isNotBlank()) TextButton(onClick = { draft = question.answer; editing = false }) { Text("Cancel") }
                }
            } else {
                Text(question.answer, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { editing = true }, contentPadding = PaddingValues(0.dp)) {
                    Icon(icon = FieldMindIcons.Edit, contentDescription = null, size = 16.dp); Spacer(Modifier.size(6.dp)); Text("Edit answer")
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(kind: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon = FieldMindIcons.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete $kind?") },
        text = { Text("This removes the $kind from your active records. This can't be undone from the app.") },
        confirmButton = { Button(onClick = { haptics.confirm(); onConfirm() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun deleteEntityByKind(kind: String, id: Long, viewModel: FieldMindViewModel) {
    when (kind) {
        "observation" -> viewModel.deleteObservation(id)
        "note" -> viewModel.deleteNote(id)
        "question" -> viewModel.deleteQuestion(id)
        "hypothesis" -> viewModel.deleteHypothesis(id)
        "project" -> viewModel.deleteProject(id)
        "source" -> viewModel.deleteSource(id)
        "data" -> viewModel.deleteDataRecord(id)
        "report" -> viewModel.deleteReport(id)
        "flashcard" -> viewModel.deleteFlashcard(id)
    }
}

@Composable
private fun DetailBody(title: String, kind: String, fields: List<Pair<String, String>>) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EntityBadge(kind)
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            fields.filter { it.second.isNotBlank() }.forEach { (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Evidence gallery for a saved observation: image thumbnails plus file/audio chips. */
@Composable
private fun ObservationAttachmentsPanel(viewModel: FieldMindViewModel, observationId: Long) {
    val attachments by viewModel.attachmentsForObservation(observationId).collectAsState(initial = emptyList())
    if (attachments.isEmpty()) return
    val images = attachments.filter { it.type.equals("Photo", true) || it.type.equals("Gallery", true) || it.uri.contains(Regex("\\.(jpg|jpeg|png|webp|gif|heic|bmp)", RegexOption.IGNORE_CASE)) }
    val others = attachments - images.toSet()
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon = FieldMindIcons.Gallery, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                Text("Evidence (${attachments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (images.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(images) { img ->
                        Column(Modifier.width(140.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AsyncImage(
                                model = img.uri,
                                contentDescription = img.caption.ifBlank { "Observation photo" },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )
                            if (img.caption.isNotBlank()) Text(img.caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            others.forEach { att ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                        Icon(icon = if (att.type.equals("Audio", true)) FieldMindIcons.Mic else FieldMindIcons.File, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(att.caption.ifBlank { att.type }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(att.uri, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun BacklinksPanel(links: List<Triple<String, String, Long>>, onOpenDetail: (String, Long) -> Unit) {
    if (links.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon = FieldMindIcons.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
            Text("Linked records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${links.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        links.forEach { (lkKind, lkTitle, lkId) ->
            EntityCard(lkTitle, lkKind, onClick = { onOpenDetail(lkKind, lkId) })
        }
    }
}

@Composable
private fun AssistantPanel(viewModel: FieldMindViewModel, seedText: String = "") {
    val enabled by viewModel.fieldSettings.geminiEnabled.collectAsState()
    if (!enabled) return
    val key by viewModel.fieldSettings.geminiApiKey.collectAsState()
    val model by viewModel.fieldSettings.geminiModel.collectAsState()
    val assistant = remember(enabled, key, model) {
        GeminiResearchAssistant(enabled = enabled, apiKeyProvider = { key }, modelProvider = { model })
    }
    val available = assistant.isAvailable()
    val scope = rememberCoroutineScope()
    var input by remember(seedText) { mutableStateOf(seedText) }
    var selectedTask by remember { mutableStateOf(AssistantTask.PAPER_BOOK_SUGGESTIONS) }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    val tasks = listOf(
        AssistantTask.PAPER_BOOK_SUGGESTIONS to FieldMindIcons.Book,
        AssistantTask.FACTUALITY to FieldMindIcons.Check,
        AssistantTask.TESTABILITY to FieldMindIcons.Question,
        AssistantTask.HYPOTHESIS to FieldMindIcons.Hypothesis,
        AssistantTask.NEXT_STEPS to FieldMindIcons.Trend,
        AssistantTask.WRITING to FieldMindIcons.Edit
    )

    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
                    Icon(icon = FieldMindIcons.Sparkle, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, size = 20.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Gemini research assistant", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(if (available) "Helps you think — never invents evidence." else "Disabled — enable it in Settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            ChoiceChips(tasks.map { it.first.title }, selectedTask.title) { title -> tasks.firstOrNull { it.first.title == title }?.let { selectedTask = it.first } }

            FieldTextField(
                input, { input = it },
                if (selectedTask == AssistantTask.PAPER_BOOK_SUGGESTIONS) "Topic for papers & books" else "Text to review",
                minLines = 2,
                supportingText = "Stays on device until you press Ask Gemini."
            )

            Button(
                onClick = {
                    if (!available) return@Button
                    loading = true; result = null
                    scope.launch {
                        val suggestion = assistant.generateContent(selectedTask, input.trim())
                        result = suggestion.body
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                enabled = available && !loading && (selectedTask == AssistantTask.NEXT_STEPS || input.isNotBlank())
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.size(8.dp)); Text("Asking Gemini…")
                } else {
                    Icon(icon = FieldMindIcons.Send, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Ask Gemini")
                }
            }

            if (!available) {
                Text("Add a Gemini API key in Settings → Gemini assistant to enable live suggestions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            result?.let { text ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(selectedTask.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                    Text("Draft only. Verify any titles, authors, or links before trusting them.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Settings rows + dialogs + helpers
// ══════════════════════════════════════════════════════════════════════

/** A grouped settings section: a header above a single rounded card holding related rows. */
@Composable
private fun SettingsGroup(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.padding(start = 4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingDivider() = HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

@Composable
private fun SettingLeading(icon: MaterialSymbolIcon?) {
    if (icon == null) { Spacer(Modifier.size(40.dp)); return }
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
        Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
    }
}

/** A whole-row clickable toggle inside a [SettingsGroup]. */
@Composable
private fun ToggleItem(title: String, body: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, icon: MaterialSymbolIcon? = null) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingLeading(icon)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StepperItem(title: String, body: String, value: Int, icon: MaterialSymbolIcon? = null, onValueChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        SettingLeading(icon)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(onClick = { onValueChange((value - 1).coerceAtLeast(0)) }) { Icon(icon = MaterialSymbolIcon("remove"), contentDescription = "Decrease", size = 18.dp) }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 24.dp), textAlign = TextAlign.Center)
            FilledTonalIconButton(onClick = { onValueChange(value + 1) }) { Icon(icon = FieldMindIcons.Add, contentDescription = "Increase", size = 18.dp) }
        }
    }
}

@Composable
private fun ChoiceItem(title: String, options: List<String>, selected: String, icon: MaterialSymbolIcon? = null, onSelected: (String) -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            SettingLeading(icon)
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        }
        ChoiceChips(options, selected, onSelected = onSelected)
    }
}

@Composable
private fun NewQuestionDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var question by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Other") }; var source by remember { mutableStateOf("Observation") }; var status by remember { mutableStateOf("New") }; var priority by remember { mutableStateOf("Medium") }
    FormDialog("New Question", onDismiss, { if (question.isNotBlank()) { viewModel.addQuestion(question, category, source, status, priority); onDismiss() } }) {
        FieldTextField(question, { question = it }, "Question", minLines = 2)
        FormSectionLabel("Classification")
        FormChoice("Category", observationCategories, category) { category = it }
        FormChoice("Source type", sourceTypes, source) { source = it }
        FormChoice("Status", questionStatuses, status) { status = it }
        FormChoice("Priority", listOf("Low", "Medium", "High"), priority) { priority = it }
        Text("Testable questions name something you can observe, measure, compare, or verify.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NewProjectDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }; var topic by remember { mutableStateOf("Biology") }; var objective by remember { mutableStateOf("") }; var question by remember { mutableStateOf("") }
    var methods by remember { mutableStateOf("") }; var nextAction by remember { mutableStateOf("") }
    FormDialog("New Project", onDismiss, { if (title.isNotBlank()) { viewModel.addProject(title, topic, objective, question, methods, nextAction); onDismiss() } }) {
        SourceFormHero("Build a project workspace", "Define the question, evidence plan, and next action before collecting more data.")
        CaptureStep("Topic & title", "Name the project and choose the broad research area.", FieldMindIcons.Project) {
            FieldTextField(title, { title = it }, "Project title")
            ChoiceChips(listOf("Biology", "Geology", "Wildlife", "Ecology", "Plant Study", "Weather", "Human Pattern", "Other"), topic) { topic = it }
        }
        CaptureStep("Research purpose", "Write a concrete objective and a question that can guide observations.", FieldMindIcons.Question) {
            FieldTextField(objective, { objective = it }, "Objective", minLines = 2)
            FieldTextField(question, { question = it }, "Research question", minLines = 2)
        }
        CaptureStep("Plan", "Optional first-pass method and next action. You can expand this in project detail later.", FieldMindIcons.Data) {
            FieldTextField(methods, { methods = it }, "Method / data plan", minLines = 3)
            FieldTextField(nextAction, { nextAction = it }, "Next action", supportingText = "Example: observe the same site at sunset for 3 days")
        }
    }
}

@Composable
private fun SourceFormHero(title: String, body: String) {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(FieldMindIcons.Source, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 26.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
            }
        }
    }
}

@Composable
private fun SourcePreviewCard(link: String, fileUri: String, modifier: Modifier = Modifier) {
    val trimmedLink = link.trim()
    val videoId = remember(trimmedLink) { youtubeVideoId(trimmedLink) }
    if (trimmedLink.isBlank() && fileUri.isBlank()) return
    Card(modifier = modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon = if (videoId != null) FieldMindIcons.Play else if (fileUri.isNotBlank()) FieldMindIcons.File else FieldMindIcons.OpenLink, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                Column(Modifier.weight(1f)) {
                    Text(if (videoId != null) "YouTube preview" else if (fileUri.isNotBlank()) "Local file reference" else "Link preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(if (videoId != null) "youtube.com/embed/$videoId" else fileUri.ifBlank { trimmedLink }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            videoId?.let { id ->
                AsyncImage(
                    model = "https://img.youtube.com/vi/$id/hqdefault.jpg",
                    contentDescription = "YouTube thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            }
            if (trimmedLink.isNotBlank() && videoId == null) {
                InfoChip(trimmedLink.substringAfter("://", trimmedLink).substringBefore('/'), icon = FieldMindIcons.OpenLink)
            }
            if (fileUri.isNotBlank()) InfoChip(fileUri.substringAfterLast('/').take(48).ifBlank { "Attached file" }, icon = FieldMindIcons.File)
        }
    }
}

private fun youtubeVideoId(url: String): String? {
    val value = url.trim()
    return when {
        "youtu.be/" in value -> value.substringAfter("youtu.be/").substringBefore('?').substringBefore('&').takeIf { it.isNotBlank() }
        "youtube.com/embed/" in value -> value.substringAfter("youtube.com/embed/").substringBefore('?').substringBefore('&').takeIf { it.isNotBlank() }
        "youtube.com/watch" in value && "v=" in value -> value.substringAfter("v=").substringBefore('&').takeIf { it.isNotBlank() }
        else -> null
    }
}

@Composable
private fun NewSourceDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    val haptics = rememberFieldMindHaptics()
    var type by remember { mutableStateOf("Article") }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var dateOrYear by remember { mutableStateOf("") }
    var doiOrIsbn by remember { mutableStateOf("") }
    var publisherOrJournal by remember { mutableStateOf("") }
    var accessDate by remember { mutableStateOf(today()) }
    var link by remember { mutableStateOf("") }
    var fileUri by remember { mutableStateOf("") }
    var citationStyleNote by remember { mutableStateOf("") }
    var importance by remember { mutableStateOf("Normal") }
    var readingStatus by remember { mutableStateOf("In progress") }
    var summary by remember { mutableStateOf("") }
    var taught by remember { mutableStateOf("") }
    var findings by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var reliability by remember { mutableStateOf(3f) }
    var projectId by remember { mutableStateOf<Long?>(null) }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            fileUri = uri.toString()
            if (type !in listOf("PDF", "Image")) type = "Local document"
            haptics.light()
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            fileUri = uri.toString()
            type = "Image"
            haptics.light()
        }
    }
    FormDialog("Add Source", onDismiss, {
        if (title.isNotBlank()) {
            viewModel.addSource(
                type = type,
                title = title,
                author = author,
                link = link,
                summary = summary,
                taught = taught,
                reliability = reliability.toInt(),
                keyFindings = findings,
                questionsGenerated = questions,
                paperNotes = notes,
                projectId = projectId,
                dateOrYear = dateOrYear,
                doiOrIsbn = doiOrIsbn,
                publisherOrJournal = publisherOrJournal,
                accessDate = accessDate,
                fileUri = fileUri,
                citationStyleNote = citationStyleNote,
                importance = importance,
                readingStatus = readingStatus
            )
            onDismiss()
        }
    }) {
        SourceFormHero("Add a research source", "Save citation details, files, links, reading notes, and project context in one place.")
        CaptureStep("Source type", "Choose what kind of material this is.", FieldMindIcons.Source) {
            ChoiceChips(sourceLibraryTypes, type) { type = it }
        }
        CaptureStep("Identity", "Capture citation identifiers now so export stays useful later.", FieldMindIcons.Article) {
            FieldTextField(title, { title = it }, "Title")
            FieldTextField(author, { author = it }, "Author / creator")
            FieldTextField(doiOrIsbn, { doiOrIsbn = it }, "DOI / ISBN", supportingText = "Crossref DOI or Open Library ISBN metadata can be checked later.")
            FieldTextField(publisherOrJournal, { publisherOrJournal = it }, "Publisher / journal")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldTextField(dateOrYear, { dateOrYear = it }, "Date / year", modifier = Modifier.weight(1f))
                FieldTextField(accessDate, { accessDate = it }, "Accessed", modifier = Modifier.weight(1f))
            }
        }
        CaptureStep("Link or file", "Attach PDFs, images, local documents, or a web link.", FieldMindIcons.Link) {
            FieldTextField(link, { link = it }, "Web link", supportingText = "Supports normal links and YouTube preview cards.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { haptics.light(); docPicker.launch(arrayOf("application/pdf", "text/*", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "image/*")) }, modifier = Modifier.weight(1f)) {
                    Icon(FieldMindIcons.File, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Document")
                }
                OutlinedButton(onClick = { haptics.light(); imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) {
                    Icon(FieldMindIcons.Gallery, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Image")
                }
            }
            if (fileUri.isNotBlank()) FieldTextField(fileUri, { fileUri = it }, "Attached file URI")
            SourcePreviewCard(link = link, fileUri = fileUri)
        }
        CaptureStep("Reading notes", "Use Cornell-style cues: main idea, evidence, questions, and takeaways.", FieldMindIcons.Edit) {
            FieldTextField(summary, { summary = it }, "Main idea", minLines = 2)
            FieldTextField(findings, { findings = it }, "Key findings / definitions", minLines = 2)
            FieldTextField(taught, { taught = it }, "What this source taught me", minLines = 2)
            FieldTextField(questions, { questions = it }, "New questions / cue column", minLines = 2)
            FieldTextField(notes, { notes = it }, "Paper / Cornell notes", minLines = 4)
            FieldTextField(citationStyleNote, { citationStyleNote = it }, "Citation style note", supportingText = "APA/MLA/manual citation reminder.")
        }
        CaptureStep("Review & project", "Mark priority, reading state, credibility, and where it belongs.", FieldMindIcons.Check) {
            Text("Reading status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(readingStatuses, readingStatus) { readingStatus = it }
            Text("Importance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(sourceImportanceLevels, importance) { importance = it }
            Text("Credibility: ${reliability.toInt()}/5")
            Slider(reliability, { reliability = it }, valueRange = 1f..5f, steps = 3)
            if (projects.isNotEmpty()) {
                Text("Link to project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChoiceChips(listOf("No project") + projects.map { it.title }, projects.firstOrNull { it.id == projectId }?.title ?: "No project") { selected -> projectId = projects.firstOrNull { it.title == selected }?.id }
            }
        }
    }
}

@Composable
private fun NewHypothesisDialog(viewModel: FieldMindViewModel, questions: List<QuestionEntity>, onDismiss: () -> Unit) {
    var prediction by remember { mutableStateOf("") }; var reasoning by remember { mutableStateOf("") }; var evidence by remember { mutableStateOf("") }; var support by remember { mutableStateOf("") }; var weaken by remember { mutableStateOf("") }; var test by remember { mutableStateOf("") }; var confidence by remember { mutableStateOf(50f) }; val linked = questions.firstOrNull()
    FormDialog("New Hypothesis", onDismiss, { if (prediction.isNotBlank()) { viewModel.addHypothesis(linked?.id, prediction, evidence, confidence.toInt(), reasoning, support, weaken, test); onDismiss() } }) {
        linked?.let { Text("Linked question: ${it.questionText}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }; FieldTextField(prediction, { prediction = it }, "Prediction", minLines = 2); FieldTextField(reasoning, { reasoning = it }, "Reasoning", minLines = 2); FieldTextField(evidence, { evidence = it }, "Evidence needed", minLines = 2); FieldTextField(support, { support = it }, "Support criteria"); FieldTextField(weaken, { weaken = it }, "Weakening criteria"); FieldTextField(test, { test = it }, "Test method"); Text("Confidence: ${confidence.toInt()}%"); Slider(confidence, { confidence = it }, valueRange = 0f..100f)
    }
}

@Composable
private fun NewDataRecordDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var tool by remember { mutableStateOf("Counter") }; var label by remember { mutableStateOf("") }; var value by remember { mutableStateOf("0") }; var unit by remember { mutableStateOf("") }; var location by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    FormDialog("Data Collection Tool", onDismiss, { if (label.isNotBlank()) { viewModel.addDataRecord(tool, label, value, unit, notes, location); onDismiss() } }) {
        FormChoice("Tool", dataTools, tool) { tool = it }
        FieldTextField(label, { label = it }, "Label"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedButton({ value = ((value.toIntOrNull() ?: 0) - 1).toString() }) { Text("−1") }; Text(value, style = MaterialTheme.typography.headlineSmall); Button({ value = ((value.toIntOrNull() ?: 0) + 1).toString() }) { Text("+1") }; TextButton({ value = "0" }) { Text("Reset") } }; FieldTextField(value, { value = it }, "Value / items / samples"); FieldTextField(unit, { unit = it }, "Unit", supportingText = "count, cm, °C, minutes"); FieldTextField(location, { location = it }, "Location / site"); FieldTextField(notes, { notes = it }, "Notes", minLines = 3)
    }
}

@Composable
private fun NewReportDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf("Field Report") }; var title by remember { mutableStateOf("") }; var background by remember { mutableStateOf("") }; var question by remember { mutableStateOf("") }; var methods by remember { mutableStateOf("") }; var observations by remember { mutableStateOf("") }; var results by remember { mutableStateOf("") }; var interpretation by remember { mutableStateOf("") }; var conclusion by remember { mutableStateOf("") }; var limitations by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }
    FormDialog("Report Builder", onDismiss, { if (title.isNotBlank()) { viewModel.addReport(type, title, background, question, methods, observations, results, interpretation, conclusion, limitations, next); onDismiss() } }) {
        FormChoice("Report type", reportTypes, type) { type = it }
        FieldTextField(title, { title = it }, "Title"); FieldTextField(background, { background = it }, "Background", minLines = 2); FieldTextField(question, { question = it }, "Question", minLines = 2); FieldTextField(methods, { methods = it }, "Methods", minLines = 2); FieldTextField(observations, { observations = it }, "Observations", minLines = 2); FieldTextField(results, { results = it }, "Data / results", minLines = 2); FieldTextField(interpretation, { interpretation = it }, "Interpretation", minLines = 2); FieldTextField(conclusion, { conclusion = it }, "Conclusion", minLines = 2); FieldTextField(limitations, { limitations = it }, "Limitations", minLines = 2); FieldTextField(next, { next = it }, "Next steps", minLines = 2); Text("Save generates a local Markdown draft for export.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NewFlashcardDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf("concept") }; var front by remember { mutableStateOf("") }; var back by remember { mutableStateOf("") }
    FormDialog("Create Flashcard", onDismiss, { if (front.isNotBlank() && back.isNotBlank()) { viewModel.addFlashcard(front, back, type); onDismiss() } }) {
        FormChoice("Card type", listOf("term", "definition", "concept", "question-answer", "mistake card"), type) { type = it }
        FieldTextField(front, { front = it }, "Front"); FieldTextField(back, { back = it }, "Back", minLines = 3)
    }
}

@Composable
private fun EditEntityDialog(kind: String, id: Long, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    when (kind) {
        "observation" -> viewModel.observations.collectAsState().value.firstOrNull { it.id == id }?.let { EditObservationDialog(it, viewModel, onDismiss) }
        "note" -> viewModel.notes.collectAsState().value.firstOrNull { it.id == id }?.let { EditNoteDialog(it, viewModel, onDismiss) }
        "question" -> viewModel.questions.collectAsState().value.firstOrNull { it.id == id }?.let { EditQuestionDialog(it, viewModel, onDismiss) }
        "hypothesis" -> viewModel.hypotheses.collectAsState().value.firstOrNull { it.id == id }?.let { EditHypothesisDialog(it, viewModel, onDismiss) }
        "project" -> viewModel.projects.collectAsState().value.firstOrNull { it.id == id }?.let { EditProjectDialog(it, viewModel, onDismiss) }
        "source" -> viewModel.sources.collectAsState().value.firstOrNull { it.id == id }?.let { EditSourceDialog(it, viewModel, onDismiss) }
        "data" -> viewModel.dataRecords.collectAsState().value.firstOrNull { it.id == id }?.let { EditDataRecordDialog(it, viewModel, onDismiss) }
        "report" -> viewModel.reports.collectAsState().value.firstOrNull { it.id == id }?.let { EditReportDialog(it, viewModel, onDismiss) }
        "flashcard" -> viewModel.flashcards.collectAsState().value.firstOrNull { it.id == id }?.let { EditFlashcardDialog(it, viewModel, onDismiss) }
        else -> onDismiss()
    }
}

@Composable
private fun EditNoteDialog(entity: NoteEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(entity.title) }; var body by remember { mutableStateOf(entity.body) }; var category by remember { mutableStateOf(entity.category) }; var tags by remember { mutableStateOf(entity.tags) }; var attachments by remember { mutableStateOf(entity.attachmentUris) }
    FormDialog("Edit Note", onDismiss, {
        if (title.isNotBlank() || body.isNotBlank()) { viewModel.updateNoteEntity(entity.copy(title = title.trim().ifBlank { body.take(36) }, body = body.trim(), category = category, tags = tags.trim(), attachmentUris = attachments.trim())); onDismiss() }
    }) {
        FormChoice("Category", observationCategories, category) { category = it }
        FieldTextField(title, { title = it }, "Title")
        FieldTextField(body, { body = it }, "Body", minLines = 5)
        FieldTextField(tags, { tags = it }, "Tags")
        FieldTextField(attachments, { attachments = it }, "Attachments", minLines = 2, supportingText = "One stored attachment per line: type|caption|uri")
    }
}

@Composable
private fun EditObservationDialog(entity: ObservationEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var subject by remember { mutableStateOf(entity.subject) }; var category by remember { mutableStateOf(entity.category) }; var facts by remember { mutableStateOf(entity.factsOnlyNotes) }; var confidence by remember { mutableStateOf(entity.confidenceLevel) }; var location by remember { mutableStateOf(entity.manualLocation) }; var tags by remember { mutableStateOf(entity.tags) }; var evidence by remember { mutableStateOf(entity.evidenceSummary) }; var context by remember { mutableStateOf(entity.moodOrContext) }
    FormDialog("Edit Observation", onDismiss, {
        if (subject.isNotBlank()) { viewModel.updateObservation(entity.copy(subject = subject.trim(), category = category, factsOnlyNotes = facts.trim(), confidenceLevel = confidence, manualLocation = location.trim(), evidenceSummary = evidence.trim(), moodOrContext = context.trim()), tags); onDismiss() }
    }) {
        FieldTextField(subject, { subject = it }, "Subject")
        FormChoice("Category", observationCategories, category) { category = it }
        FieldTextField(facts, { facts = it }, "Facts only", minLines = 3)
        FormChoice("Confidence", confidenceOptions, confidence) { confidence = it }
        FieldTextField(location, { location = it }, "Location"); FieldTextField(tags, { tags = it }, "Tags (comma separated)"); FieldTextField(evidence, { evidence = it }, "Evidence summary", minLines = 2); FieldTextField(context, { context = it }, "Context / mood", minLines = 2)
    }
}

@Composable
private fun EditQuestionDialog(entity: QuestionEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var question by remember { mutableStateOf(entity.questionText) }; var category by remember { mutableStateOf(entity.category) }; var source by remember { mutableStateOf(entity.sourceType) }; var status by remember { mutableStateOf(entity.status) }; var priority by remember { mutableStateOf(entity.priority) }; var answer by remember { mutableStateOf(entity.answer) }
    FormDialog("Edit Question", onDismiss, {
        if (question.isNotBlank()) { viewModel.updateQuestionEntity(entity.copy(questionText = question.trim(), category = category, sourceType = source, status = status, priority = priority, answer = answer.trim(), answeredAt = if (answer.isBlank()) null else (entity.answeredAt ?: System.currentTimeMillis()))); onDismiss() }
    }) {
        FieldTextField(question, { question = it }, "Question", minLines = 2)
        FormSectionLabel("Classification")
        FormChoice("Category", observationCategories, category) { category = it }
        FormChoice("Source type", sourceTypes, source) { source = it }
        FormChoice("Status", questionStatuses, status) { status = it }
        FormChoice("Priority", listOf("Low", "Medium", "High"), priority) { priority = it }
        FormSectionLabel("Answer")
        FieldTextField(answer, { answer = it }, "Answer", minLines = 2)
    }
}

@Composable
private fun EditHypothesisDialog(entity: HypothesisEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var prediction by remember { mutableStateOf(entity.prediction) }; var reasoning by remember { mutableStateOf(entity.reasoning) }; var evidence by remember { mutableStateOf(entity.evidenceNeeded) }; var support by remember { mutableStateOf(entity.supportCriteria) }; var weaken by remember { mutableStateOf(entity.weakeningCriteria) }; var test by remember { mutableStateOf(entity.testMethod) }; var result by remember { mutableStateOf(entity.resultStatus) }; var confidence by remember { mutableStateOf(entity.confidencePercent.toFloat()) }
    FormDialog("Edit Hypothesis", onDismiss, {
        if (prediction.isNotBlank()) { viewModel.updateHypothesisEntity(entity.copy(prediction = prediction.trim(), reasoning = reasoning.trim(), evidenceNeeded = evidence.trim(), supportCriteria = support.trim(), weakeningCriteria = weaken.trim(), testMethod = test.trim(), resultStatus = result, confidencePercent = confidence.toInt())); onDismiss() }
    }) {
        FieldTextField(prediction, { prediction = it }, "Prediction", minLines = 2); FieldTextField(reasoning, { reasoning = it }, "Reasoning", minLines = 2); FieldTextField(evidence, { evidence = it }, "Evidence needed", minLines = 2); FieldTextField(support, { support = it }, "Support criteria"); FieldTextField(weaken, { weaken = it }, "Weakening criteria"); FieldTextField(test, { test = it }, "Test method")
        FormChoice("Result", listOf("Unknown", "Supported", "Weakened", "Inconclusive"), result) { result = it }
        Text("Confidence: ${confidence.toInt()}%"); Slider(confidence, { confidence = it }, valueRange = 0f..100f)
    }
}

@Composable
private fun EditProjectDialog(entity: ProjectEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(entity.title) }; var topic by remember { mutableStateOf(entity.topicType) }; var objective by remember { mutableStateOf(entity.objective) }; var question by remember { mutableStateOf(entity.researchQuestion) }; var background by remember { mutableStateOf(entity.backgroundNotes) }; var methods by remember { mutableStateOf(entity.methods) }; var conclusion by remember { mutableStateOf(entity.conclusion) }
    FormDialog("Edit Project", onDismiss, {
        if (title.isNotBlank()) { viewModel.updateProjectEntity(entity.copy(title = title.trim(), topicType = topic.trim().ifBlank { "General" }, objective = objective.trim(), researchQuestion = question.trim(), backgroundNotes = background.trim(), methods = methods.trim(), conclusion = conclusion.trim())); onDismiss() }
    }) {
        FieldTextField(title, { title = it }, "Project title")
        FormChoice("Topic / category", listOf("Biology", "Geology", "Wildlife", "Ecology", "Plant Study", "Weather", "Human Pattern", "Other"), topic) { topic = it }
        FieldTextField(objective, { objective = it }, "Objective", minLines = 2); FieldTextField(question, { question = it }, "Research question", minLines = 2); FieldTextField(background, { background = it }, "Background notes", minLines = 2); FieldTextField(methods, { methods = it }, "Methods", minLines = 2); FieldTextField(conclusion, { conclusion = it }, "Conclusion", minLines = 2)
    }
}

@Composable
private fun EditSourceDialog(entity: SourceEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    val projects by viewModel.projects.collectAsState()
    var type by remember { mutableStateOf(entity.type) }
    var title by remember { mutableStateOf(entity.title) }
    var author by remember { mutableStateOf(entity.author) }
    var dateOrYear by remember { mutableStateOf(entity.dateOrYear) }
    var doiOrIsbn by remember { mutableStateOf(entity.doiOrIsbn) }
    var publisherOrJournal by remember { mutableStateOf(entity.publisherOrJournal) }
    var accessDate by remember { mutableStateOf(entity.accessDate) }
    var link by remember { mutableStateOf(entity.link) }
    var fileUri by remember { mutableStateOf(entity.fileUri) }
    var citationStyleNote by remember { mutableStateOf(entity.citationStyleNote) }
    var importance by remember { mutableStateOf(entity.importance) }
    var readingStatus by remember { mutableStateOf(entity.readingStatus) }
    var projectId by remember { mutableStateOf(entity.relatedProjectId) }
    var summary by remember { mutableStateOf(entity.personalSummary) }
    var findings by remember { mutableStateOf(entity.keyFindings) }
    var taught by remember { mutableStateOf(entity.whatThisSourceTaughtMe) }
    var questions by remember { mutableStateOf(entity.questionsGenerated) }
    var notes by remember { mutableStateOf(entity.paperNotes) }
    var reliability by remember { mutableStateOf(entity.reliabilityScore.toFloat()) }
    FormDialog("Edit Source", onDismiss, {
        if (title.isNotBlank()) {
            viewModel.updateSourceEntity(
                entity.copy(
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
                    keyFindings = findings.trim(),
                    whatThisSourceTaughtMe = taught.trim(),
                    questionsGenerated = questions.trim(),
                    paperNotes = notes.trim(),
                    reliabilityScore = reliability.toInt(),
                    readingStatus = readingStatus,
                    relatedProjectId = projectId
                )
            )
            onDismiss()
        }
    }) {
        SourceFormHero("Edit source", "Keep source identity, file/link, notes, and status synchronized.")
        FormChoice("Source type", sourceLibraryTypes, type) { type = it }
        FormSectionLabel("Identity")
        FieldTextField(title, { title = it }, "Title")
        FieldTextField(author, { author = it }, "Author / creator")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldTextField(dateOrYear, { dateOrYear = it }, "Date / year", modifier = Modifier.weight(1f))
            FieldTextField(accessDate, { accessDate = it }, "Accessed", modifier = Modifier.weight(1f))
        }
        FieldTextField(doiOrIsbn, { doiOrIsbn = it }, "DOI / ISBN")
        FieldTextField(publisherOrJournal, { publisherOrJournal = it }, "Publisher / journal")
        FormSectionLabel("Link and file")
        FieldTextField(link, { link = it }, "Web link")
        FieldTextField(fileUri, { fileUri = it }, "File URI")
        SourcePreviewCard(link, fileUri)
        FormSectionLabel("Reading notes")
        FieldTextField(summary, { summary = it }, "Main idea", minLines = 2)
        FieldTextField(findings, { findings = it }, "Key findings", minLines = 2)
        FieldTextField(taught, { taught = it }, "What this taught me", minLines = 2)
        FieldTextField(questions, { questions = it }, "New questions", minLines = 2)
        FieldTextField(notes, { notes = it }, "Paper / Cornell notes", minLines = 3)
        FieldTextField(citationStyleNote, { citationStyleNote = it }, "Citation style note")
        FormSectionLabel("Status")
        Text("Reading status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChoiceChips(readingStatuses, readingStatus) { readingStatus = it }
        Text("Importance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChoiceChips(sourceImportanceLevels, importance) { importance = it }
        Text("Credibility: ${reliability.toInt()}/5")
        Slider(reliability, { reliability = it }, valueRange = 1f..5f, steps = 3)
        if (projects.isNotEmpty()) {
            Text("Project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(listOf("No project") + projects.map { it.title }, projects.firstOrNull { it.id == projectId }?.title ?: "No project") { selected -> projectId = projects.firstOrNull { it.title == selected }?.id }
        }
    }
}

@Composable
private fun EditDataRecordDialog(entity: DataRecordEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var tool by remember { mutableStateOf(entity.toolType) }; var label by remember { mutableStateOf(entity.label) }; var value by remember { mutableStateOf(entity.value) }; var unit by remember { mutableStateOf(entity.unit) }; var location by remember { mutableStateOf(entity.location) }; var notes by remember { mutableStateOf(entity.notes) }
    FormDialog("Edit Data Record", onDismiss, {
        if (label.isNotBlank()) { viewModel.updateDataRecordEntity(entity.copy(toolType = tool, label = label.trim(), value = value.trim(), unit = unit.trim(), location = location.trim(), notes = notes.trim())); onDismiss() }
    }) {
        FormChoice("Tool", dataTools, tool) { tool = it }
        FieldTextField(label, { label = it }, "Label"); FieldTextField(value, { value = it }, "Value"); FieldTextField(unit, { unit = it }, "Unit"); FieldTextField(location, { location = it }, "Location / site"); FieldTextField(notes, { notes = it }, "Notes", minLines = 3)
    }
}

@Composable
private fun EditReportDialog(entity: ReportEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(entity.type) }; var title by remember { mutableStateOf(entity.title) }; var question by remember { mutableStateOf(entity.question) }; var methods by remember { mutableStateOf(entity.methods) }; var results by remember { mutableStateOf(entity.results) }; var conclusion by remember { mutableStateOf(entity.conclusion) }; var next by remember { mutableStateOf(entity.nextSteps) }
    FormDialog("Edit Report", onDismiss, {
        if (title.isNotBlank()) { viewModel.updateReportEntity(entity.copy(type = type, title = title.trim(), question = question.trim(), methods = methods.trim(), results = results.trim(), conclusion = conclusion.trim(), nextSteps = next.trim())); onDismiss() }
    }) {
        FormChoice("Report type", reportTypes, type) { type = it }
        FieldTextField(title, { title = it }, "Title"); FieldTextField(question, { question = it }, "Question", minLines = 2); FieldTextField(methods, { methods = it }, "Methods", minLines = 2); FieldTextField(results, { results = it }, "Data / results", minLines = 2); FieldTextField(conclusion, { conclusion = it }, "Conclusion", minLines = 2); FieldTextField(next, { next = it }, "Next steps", minLines = 2)
    }
}

@Composable
private fun EditFlashcardDialog(entity: FlashcardEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(entity.type) }; var front by remember { mutableStateOf(entity.front) }; var back by remember { mutableStateOf(entity.back) }
    FormDialog("Edit Flashcard", onDismiss, {
        if (front.isNotBlank() && back.isNotBlank()) { viewModel.updateFlashcardEntity(entity.copy(type = type, front = front.trim(), back = back.trim())); onDismiss() }
    }) {
        FormChoice("Card type", listOf("term", "definition", "concept", "question-answer", "mistake card"), type) { type = it }
        FieldTextField(front, { front = it }, "Front"); FieldTextField(back, { back = it }, "Back", minLines = 3)
    }
}

/**
 * Map card for an observation detail: a small offline preview marker, the resolved place name
 * (when known), the exact coordinates, and a link out to the device map app.
 */
@Composable
private fun ObservationLocationCard(latitude: Double, longitude: Double, manualLocation: String) {
    val colors = FieldMindTheme.colors
    val uriHandler = LocalUriHandler.current
    val coords = "%.5f, %.5f".format(latitude, longitude)
    val placeName = manualLocation.substringBefore(" • GPS").trim().takeIf { it.isNotBlank() && !it.startsWith("GPS") }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon = FieldMindIcons.Location, contentDescription = null, tint = colors.observation, size = 20.dp)
                Text("Location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            if (placeName != null) Text(placeName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
                MiniMap(listOf(latitude to longitude), pointColor = colors.observation, height = 160.dp)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(coords, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val label = Uri.encode(placeName ?: "Observation")
                    runCatching { uriHandler.openUri("geo:$latitude,$longitude?q=$latitude,$longitude($label)") }
                }) {
                    Icon(icon = FieldMindIcons.Location, contentDescription = null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Open in maps")
                }
            }
        }
    }
}

/**
 * Full-screen form page (presented as an edge-to-edge dialog). Replaces the old cramped
 * AlertDialog so add/edit flows read as real pages: a top bar with close + Save, then a
 * scrolling body of clearly separated, ordered sections.
 */
@Composable
private fun FormDialog(title: String, onDismiss: () -> Unit, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss) { Icon(icon = MaterialSymbolIcon("close"), contentDescription = "Close", size = 22.dp) }
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Button(onClick = { haptics.confirm(); onSave() }) { Text("Save") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * A labelled, collapsible single-choice section used inside [FormDialog]. Collapsed it shows the
 * field label and current value; tapping reveals the chips. Keeps long forms tidy and ordered.
 */
@Composable
private fun FormChoice(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    val haptics = rememberFieldMindHaptics()
    var expanded by rememberSaveable(label) { mutableStateOf(false) }
    val rotation by androidx.compose.animation.core.animateFloatAsState(if (expanded) 180f else 0f, label = "chev")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .animateContentSize()
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { haptics.light(); expanded = !expanded }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selected, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Icon(icon = MaterialSymbolIcon("expand_more"), contentDescription = null, size = 22.dp, modifier = Modifier.graphicsLayer { rotationZ = rotation })
        }
        AnimatedVisibility(expanded) {
            ChoiceChips(options, selected, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) { onSelected(it); expanded = false }
        }
    }
}

/** A small section caption to group related fields inside a [FormDialog]. */
@Composable
private fun FormSectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.8.sp)
}

internal fun learningModuleBody(module: String): String = when (module) {
    "Observation" -> "Practice separating facts from guesses in the observation journal."
    "Asking testable questions" -> "Convert curiosity into questions that can be observed, compared, or verified."
    "Literature review" -> "Use sources, paper prompts, and what-this-taught-me summaries."
    "Analysis" -> "Use tags, dates, projects, subjects, and data tools to find patterns."
    else -> "Track this skill by applying it in notes, sources, projects, reports, and revision cards."
}

private fun createFieldMindFile(context: Context, prefix: String, suffix: String): File {
    val dir = File(context.getExternalFilesDir(null), "fieldmind").apply { mkdirs() }
    return File(dir, "$prefix-${System.currentTimeMillis()}$suffix")
}

private fun createFieldMindFileUri(context: Context, prefix: String, suffix: String): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.provider", createFieldMindFile(context, prefix, suffix))
