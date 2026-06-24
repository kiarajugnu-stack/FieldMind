package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.presentation.components.FieldTextField
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.rememberFieldMindHaptics
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import fieldmind.research.app.features.field.presentation.theme.FieldMindColors
import androidx.compose.runtime.ReadOnlyComposable

private enum class ProjectTab(val label: String) {
    All("All"), Observations("Obs"), Notes("Notes"),
    Questions("Questions"), Sources("Sources"), Tasks("Tasks")
}

// ══════════════════════════════════════════════════════════════════════
//  Project Detail Screen — Redesigned with header, stats, tabs, feed
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ProjectDetailScreen(
    projectId: Long,
    viewModel: FieldMindViewModel,
    onBack: () -> Unit = {},
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    onNavigate: ((FieldMindScreen) -> Unit)? = null,
    onOpenRelations: (() -> Unit)? = null,
    onOpenSettings: ((Long) -> Unit)? = null
) {
    // ── Collect state ──
    val projects by viewModel.projects.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val reports by viewModel.reports.collectAsState()

    val project = projects.firstOrNull { it.id == projectId }
    val relatedObs = observations.filter { it.projectId == projectId }
    val relatedNotes = notes.filter { it.projectId == projectId }
    val relatedQs = questions.filter { it.relatedProjectId == projectId }
    val relatedSources = sources.filter { it.relatedProjectId == projectId }
    val relatedTasks = tasks.filter { it.projectId == projectId }
    val relatedFolders = folders.filter { it.projectId == projectId }
    val relatedData = data.filter { it.projectId == projectId }
    val relatedReports = reports.filter { it.projectId == projectId }

    // ── UI state ──
    var selectedTab by remember { mutableStateOf(ProjectTab.All) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showObservationPicker by remember { mutableStateOf(false) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var obsPickerSearch by remember { mutableStateOf("") }
    var srcPickerSearch by remember { mutableStateOf("") }

    // ── Dialog states ──
    var showNewObservation by remember { mutableStateOf(false) }
    var showNewNote by remember { mutableStateOf(false) }
    var showNewQuestion by remember { mutableStateOf(false) }
    var showNewSource by remember { mutableStateOf(false) }
    var showNewTask by remember { mutableStateOf(false) }
    var showNewAttachment by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var showNewHypothesis by remember { mutableStateOf(false) }

    val colors = FieldMindTheme.colors
    val context = LocalContext.current
    val haptics = rememberFieldMindHaptics()

    if (project == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Project not found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val totalRecords = relatedObs.size + relatedNotes.size + relatedQs.size + relatedSources.size + relatedTasks.size
    val timeAgo = getRelativeTime(project.updatedAt)

    // ── Filtered items per tab ──
    val displayObs = remember(relatedObs, searchQuery, selectedTab) {
        if (selectedTab == ProjectTab.All || selectedTab == ProjectTab.Observations) {
            relatedObs.filter { searchQuery.isBlank() || it.subject.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
        } else emptyList()
    }
    val displayNotes = remember(relatedNotes, searchQuery, selectedTab) {
        if (selectedTab == ProjectTab.All || selectedTab == ProjectTab.Notes) {
            relatedNotes.filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.body.contains(searchQuery, ignoreCase = true) }
        } else emptyList()
    }
    val displayQs = remember(relatedQs, searchQuery, selectedTab) {
        if (selectedTab == ProjectTab.All || selectedTab == ProjectTab.Questions) {
            relatedQs.filter { searchQuery.isBlank() || it.questionText.contains(searchQuery, ignoreCase = true) }
        } else emptyList()
    }
    val displaySources = remember(relatedSources, searchQuery, selectedTab) {
        if (selectedTab == ProjectTab.All || selectedTab == ProjectTab.Sources) {
            relatedSources.filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.author.contains(searchQuery, ignoreCase = true) }
        } else emptyList()
    }
    val displayTasks = remember(relatedTasks, searchQuery, selectedTab) {
        if (selectedTab == ProjectTab.All || selectedTab == ProjectTab.Tasks) {
            relatedTasks.filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) }
        } else emptyList()
    }
    val displayFolders = remember(relatedFolders, selectedTab) {
        if (selectedTab == ProjectTab.All) relatedFolders else emptyList()
    }

    // ── Combined feed for All tab ──
    data class FeedItem(
        val kind: String, val id: Long, val title: String, val subtitle: String,
        val body: String, val meta: String, val timestamp: Long, val tags: List<String> = emptyList(),
        val linkedCount: Int = 0, val linkedLabel: String = "", val accentColor: Color
    )
    val feedItems = remember(displayObs, displayNotes, displayQs, displaySources, displayTasks, displayFolders, selectedTab) {
        when (selectedTab) {
            ProjectTab.All -> buildList {
                relatedFolders.forEach { f ->
                    add(FeedItem("Folder", f.id, f.name, "", "", "", f.createdAt, accentColor = colors.hypothesis))
                }
                displayObs.forEach { o ->
                    add(FeedItem("Observation", o.id, o.subject.ifBlank { "Observation" }, o.category,
                        o.factsOnlyNotes, "Location: ${o.manualLocation.ifBlank { o.moodOrContext.ifBlank { "N/A" } }}", o.timestamp,
                        accentColor = colors.observation))
                }
                displayNotes.forEach { n ->
                    add(FeedItem("Note", n.id, n.title.ifBlank { "Untitled" }, n.category,
                        n.body, "", n.createdAt, accentColor = colors.source))
                }
                displayQs.forEach { q ->
                    add(FeedItem("Question", q.id, q.questionText.take(80), q.category,
                        q.answer.ifBlank { "Status: ${q.status}" }, q.status, q.createdAt, accentColor = colors.question))
                }
                displaySources.forEach { s ->
                    add(FeedItem("Source", s.id, s.title.ifBlank { "Untitled" }, s.type,
                        s.author, "${s.type} • ${s.readingStatus}", s.createdAt, accentColor = colors.source))
                }
                displayTasks.forEach { t ->
                    val dueStr = if (t.dueDate.isNotBlank()) "Due ${t.dueDate}" else "No due date"
                    add(FeedItem("Task", t.id, t.title, t.priority,
                        t.description.ifBlank { t.taskType }, dueStr, t.createdAt, accentColor = colors.flashcard))
                }
            }.sortedByDescending { it.timestamp }
            ProjectTab.Observations -> displayObs.map { o ->
                val linkedNotes = relatedNotes.count { n -> n.projectId == projectId }
                val linkedSrcs = relatedSources.count { s -> s.relatedProjectId == projectId }
                val linkedParts = buildList {
                    if (linkedNotes > 0) add("$linkedNotes Notes")
                    if (linkedSrcs > 0) add("$linkedSrcs Source")
                }
                FeedItem("Observation", o.id, o.subject.ifBlank { "Observation" }, o.category,
                    o.factsOnlyNotes, "Location: ${o.manualLocation.ifBlank { "N/A" }}", o.timestamp,
                    tags = o.tags.split(",", "#").map { it.trim() }.filter { it.isNotBlank() },
                    linkedCount = linkedNotes + linkedSrcs, linkedLabel = linkedParts.joinToString(" • "),
                    accentColor = colors.observation)
            }
            ProjectTab.Notes -> displayNotes.map { n ->
                FeedItem("Note", n.id, n.title.ifBlank { "Untitled" }, n.category,
                    n.body, "", n.createdAt, tags = n.tags.split(",", "#").map { it.trim() }.filter { it.isNotBlank() }, accentColor = colors.source)
            }
            ProjectTab.Questions -> displayQs.map { q ->
                FeedItem("Question", q.id, q.questionText.take(80), q.category,
                    q.answer.ifBlank { "Status: ${q.status}" }, q.status, q.createdAt, accentColor = colors.question)
            }
            ProjectTab.Sources -> displaySources.map { s ->
                FeedItem("Source", s.id, s.title.ifBlank { "Untitled" }, s.type,
                    s.author.ifBlank { s.publisherOrJournal }, "${s.type} • ${s.readingStatus}", s.createdAt, accentColor = colors.source)
            }
            ProjectTab.Tasks -> displayTasks.map { t ->
                val dueStr = if (t.dueDate.isNotBlank()) "Due ${t.dueDate}" else "No due date"
                FeedItem("Task", t.id, t.title, t.priority,
                    t.description.ifBlank { t.taskType }, dueStr, t.createdAt, accentColor = colors.flashcard)
            }
        }
    }

    // ── Search results ──
    val showEmpty = searchQuery.isNotBlank() && feedItems.isEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ════════════════════════════════════════════════════════════════
        //  Header: back, project icon, name, subtitle, search, + menu
        // ════════════════════════════════════════════════════════════════
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Back + actions row
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            onClick = onBack,
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(MaterialSymbolIcon("arrow_back"), "Back", size = 22.dp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                onClick = { haptics.light(); showSearch = !showSearch },
                                shape = RoundedCornerShape(14.dp),
                                color = if (showSearch) colors.project.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(MaterialSymbolIcon("search"), "Search", size = 22.dp,
                                        tint = if (showSearch) colors.project else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Surface(
                                onClick = { haptics.light(); showCreateSheet = true },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(MaterialSymbolIcon("add"), "Add", size = 22.dp, tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Surface(
                                onClick = { showRenameDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(MaterialSymbolIcon("more_vert"), "Menu", size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Project identity
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                                .background(colors.project.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(MaterialSymbolIcon("science"), null, tint = colors.project, size = 28.dp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(project.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(project.topicType, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                StatusBadge(project.status)
                            }
                        }
                    }

                    // Stats line
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("$totalRecords Records • Updated $timeAgo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Description
                    if (project.objective.isNotBlank() && !showSearch) {
                        Text(project.objective, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }

                    // Search bar
                    AnimatedVisibility(showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search all project records…") },
                            leadingIcon = { Icon(MaterialSymbolIcon("search"), null, size = 18.dp) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════════
        //  Stats row
        // ════════════════════════════════════════════════════════════════
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("${relatedObs.size}", "Obs", colors.observation)
                        StatItem("${relatedNotes.size}", "Notes", colors.source)
                        StatItem("${relatedQs.size}", "Qs", colors.question)
                        StatItem("${relatedSources.size}", "Src", colors.info)
                        StatItem("${relatedTasks.size}", "Tasks", colors.flashcard)
                    }
                    if (relatedFolders.isNotEmpty() || relatedData.isNotEmpty() || relatedReports.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            if (relatedFolders.isNotEmpty()) Text("${relatedFolders.size} folders", style = MaterialTheme.typography.labelSmall, color = colors.hypothesis)
                            if (relatedData.isNotEmpty()) { Text("  ·  ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${relatedData.size} data", style = MaterialTheme.typography.labelSmall, color = colors.data) }
                            if (relatedReports.isNotEmpty()) { Text("  ·  ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${relatedReports.size} reports", style = MaterialTheme.typography.labelSmall, color = colors.report) }
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════════
        //  Filter tabs
        // ════════════════════════════════════════════════════════════════
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProjectTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        onClick = { haptics.light(); selectedTab = tab; searchQuery = "" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.project.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, colors.project.copy(alpha = 0.4f)) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.project else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════════
        //  Empty / Search results header
        // ════════════════════════════════════════════════════════════════
        if (showEmpty) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Text("No results for \"$searchQuery\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (feedItems.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(MaterialSymbolIcon("inbox"), null, size = 48.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Text("No ${selectedTab.label.lowercase()} yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tap + to add your first ${selectedTab.label.lowercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        } else {
            // ── Grouped feed header ──
            item {
                Text(
                    if (searchQuery.isNotBlank()) "Search results (${feedItems.size})" else "Today",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // ════════════════════════════════════════════════════════════
            //  Feed items
            // ════════════════════════════════════════════════════════════
            items(feedItems.take(50), key = { "${it.kind}_${it.id}" }) { item ->
                FeedItemCard(item = item, onClick = {
                    when (item.kind.lowercase()) {
                        "observation" -> onOpenDetail("observation", item.id)
                        "note" -> onOpenDetail("note", item.id)
                        "question" -> onOpenDetail("question", item.id)
                        "source" -> onOpenDetail("source", item.id)
                        "task" -> onOpenDetail("task", item.id)
                        "folder" -> onOpenDetail("folder", item.id)
                    }
                })
            }

            // ── All Tasks button (on All tab or Tasks tab) ──
            if ((selectedTab == ProjectTab.All || selectedTab == ProjectTab.Tasks) && relatedTasks.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = { onNavigate?.invoke(FieldMindScreen.Tasks) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(MaterialSymbolIcon("checklist"), null, size = 18.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("View all ${relatedTasks.size} tasks", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════════
        //  [+ Add Record] button
        // ════════════════════════════════════════════════════════════════
        item {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { haptics.light(); showCreateSheet = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(MaterialSymbolIcon("add"), null, size = 20.dp)
                Spacer(Modifier.size(8.dp))
                Text("+ Add Record", fontWeight = FontWeight.Bold)
            }
        }

        // Bottom padding
        item { Spacer(Modifier.height(24.dp)) }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Dialogs
    // ══════════════════════════════════════════════════════════════════

    if (showNewObservation) {
        NewObservationDialog(viewModel = viewModel, projectId = project.id, onDismiss = { showNewObservation = false })
    }
    if (showNewNote) {
        NewNoteDialog(viewModel = viewModel, projectId = project.id, onDismiss = { showNewNote = false })
    }
    if (showNewQuestion) {
        NewQuestionDialog(viewModel = viewModel, projectId = project.id, onDismiss = { showNewQuestion = false })
    }
    if (showNewSource) {
        NewSourceDialog(viewModel = viewModel, initialProjectId = project.id, onDismiss = { showNewSource = false })
    }
    if (showNewTask) {
        NewTaskDialog(viewModel = viewModel, projectId = project.id, onDismiss = { showNewTask = false })
    }
    if (showNewAttachment) {
        NewAttachmentDialog(viewModel = viewModel, onDismiss = { showNewAttachment = false })
    }
    if (showNewFolder) {
        NewFolderDialog(viewModel = viewModel, projectId = project.id, onDismiss = { showNewFolder = false })
    }
    if (showNewHypothesis) {
        NewHypothesisDialog(viewModel = viewModel, questions = relatedQs, onDismiss = { showNewHypothesis = false })
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            icon = { Icon(FieldMindIcons.Edit, null, size = 28.dp) },
            title = { Text("Project options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { showRenameDialog = false; onOpenRelations?.invoke() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(FieldMindIcons.Search, null, size = 18.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("View Relations", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = { showRenameDialog = false; onOpenSettings?.invoke(project.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(FieldMindIcons.Settings, null, size = 18.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("Project Settings", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    TextButton(
                        onClick = { showRenameDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(MaterialSymbolIcon("close"), null, size = 18.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("Close", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // ══════════════════════════════════════════════════════════════════
    //  Observation Picker
    // ══════════════════════════════════════════════════════════════════
    if (showObservationPicker) {
        EntityPickerDialog(
            title = "Link Observation to Project",
            searchQuery = obsPickerSearch,
            onSearchChange = { obsPickerSearch = it },
            onDismiss = { showObservationPicker = false; obsPickerSearch = "" },
            items = observations.filter { obs -> obs.deletedAt == null && obs.projectId != project.id && (obsPickerSearch.isBlank() || obs.subject.contains(obsPickerSearch, ignoreCase = true) || obs.category.contains(obsPickerSearch, ignoreCase = true)) },
            itemIcon = { Icon(FieldMindIcons.Observation, null, tint = colors.observation, size = 16.dp) },
            itemPrimaryText = { it.subject.ifBlank { "Observation #${it.id}" } },
            itemSecondaryText = { "${it.category} • ${it.date}" },
            onSelect = { obs -> haptics.confirm(); viewModel.updateObservation(obs.copy(projectId = project.id)); showObservationPicker = false; obsPickerSearch = "" }
        )
    }
    if (showSourcePicker) {
        EntityPickerDialog(
            title = "Link Source to Project",
            searchQuery = srcPickerSearch,
            onSearchChange = { srcPickerSearch = it },
            onDismiss = { showSourcePicker = false; srcPickerSearch = "" },
            items = sources.filter { src -> src.deletedAt == null && src.relatedProjectId != project.id && (srcPickerSearch.isBlank() || src.title.contains(srcPickerSearch, ignoreCase = true) || src.type.contains(srcPickerSearch, ignoreCase = true)) },
            itemIcon = { Icon(FieldMindIcons.Category, null, tint = colors.source, size = 16.dp) },
            itemPrimaryText = { it.title.ifBlank { "Source #${it.id}" } },
            itemSecondaryText = { "${it.type} • ${it.readingStatus}" },
            onSelect = { src -> haptics.confirm(); viewModel.updateSourceEntity(src.copy(relatedProjectId = project.id)); showSourcePicker = false; srcPickerSearch = "" }
        )
    }

    // ══════════════════════════════════════════════════════════════════
    //  New Create Sheet — COLLECT / ANALYZE / EVIDENCE / PLAN
    // ══════════════════════════════════════════════════════════════════
    if (showCreateSheet) {
        ProjectCreateSheetV2(
            projectTitle = project.title,
            onSelect = { kind ->
                showCreateSheet = false
                when (kind) {
                    "Observation" -> showNewObservation = true
                    "Photo Observation" -> showNewObservation = true
                    "Voice Note" -> showNewNote = true
                    "Location" -> showNewObservation = true
                    "Note" -> showNewNote = true
                    "Question" -> showNewQuestion = true
                    "Hypothesis" -> showNewHypothesis = true
                    "Experiment" -> showNewTask = true
                    "Source" -> showNewSource = true
                    "Document" -> showNewAttachment = true
                    "Citation" -> showNewSource = true
                    "Task" -> showNewTask = true
                    "Survey Session" -> onNavigate?.invoke(FieldMindScreen.ResearchSession)
                    "Field Visit" -> showNewTask = true
                    "Folder" -> showNewFolder = true
                }
            },
            onDismiss = { showCreateSheet = false }
        )
    }

}

// ══════════════════════════════════════════════════════════════════════
//  Feed Item Card — Renders a single activity item
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun FeedItemCard(item: FeedItem, onClick: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { haptics.light(); onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Kind icon
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(item.accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (item.kind) {
                            "Observation" -> FieldMindIcons.Observation
                            "Note" -> MaterialSymbolIcon("edit_note")
                            "Question" -> FieldMindIcons.Question
                            "Source" -> FieldMindIcons.Source
                            "Task" -> MaterialSymbolIcon("checklist")
                            "Folder" -> MaterialSymbolIcon("folder")
                            else -> MaterialSymbolIcon("circle")
                        },
                        null, tint = item.accentColor, size = 22.dp
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    // Title + meta row
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Text(getRelativeTimeShort(item.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    if (item.body.isNotBlank()) {
                        Text(item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (item.subtitle.isNotBlank()) {
                        Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = item.accentColor.copy(alpha = 0.8f))
                    }
                    if (item.meta.isNotBlank()) {
                        Text(item.meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // Tags
                    if (item.tags.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            item.tags.take(4).forEach { tag ->
                                Surface(shape = RoundedCornerShape(6.dp), color = item.accentColor.copy(alpha = 0.1f)) {
                                    Text(tag, style = MaterialTheme.typography.labelSmall, color = item.accentColor, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                    // Linked info
                    if (item.linkedCount > 0) {
                        Text("Linked: ${item.linkedLabel}", style = MaterialTheme.typography.labelSmall, color = colors.info)
                    }
                }
            }
        }
    }
}

private val colors: FieldMindColors
    @Composable
    @ReadOnlyComposable
    get() = FieldMindTheme.colors

// ══════════════════════════════════════════════════════════════════════
//  Project Create Sheet V2 — COLLECT / ANALYZE / EVIDENCE / PLAN
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ProjectCreateSheetV2(
    projectTitle: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            // Scrim
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(onClick = onDismiss))
            // Sheet
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Handle
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
                    }
                    // Header
                    Text("Create", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Add to $projectTitle", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // ── COLLECT ──
                    CreateSectionTitle("COLLECT", colors.observation)
                    CreateOptionRow(FieldMindIcons.Observation, "Observation", "Record something you saw", colors.observation) { onSelect("Observation") }
                    CreateOptionRow(MaterialSymbolIcon("photo_camera"), "Photo Observation", "Capture and annotate", colors.observation) { onSelect("Photo Observation") }
                    CreateOptionRow(MaterialSymbolIcon("mic"), "Voice Note", "Record in the field", colors.project) { onSelect("Voice Note") }
                    CreateOptionRow(MaterialSymbolIcon("location_on"), "Location Record", "Save a place", colors.info) { onSelect("Location") }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // ── ANALYZE ──
                    CreateSectionTitle("ANALYZE", colors.question)
                    CreateOptionRow(MaterialSymbolIcon("edit_note"), "Note", "Write findings", colors.source) { onSelect("Note") }
                    CreateOptionRow(FieldMindIcons.Question, "Question", "Track unknowns", colors.question) { onSelect("Question") }
                    CreateOptionRow(FieldMindIcons.Hypothesis, "Hypothesis", "Proposed explanation", colors.hypothesis) { onSelect("Hypothesis") }
                    CreateOptionRow(MaterialSymbolIcon("science"), "Experiment", "Test an idea", colors.flashcard) { onSelect("Experiment") }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // ── EVIDENCE ──
                    CreateSectionTitle("EVIDENCE", colors.source)
                    CreateOptionRow(FieldMindIcons.Source, "Source", "Paper, website, book", colors.source) { onSelect("Source") }
                    CreateOptionRow(MaterialSymbolIcon("description"), "Document", "PDF or file", colors.warning) { onSelect("Document") }
                    CreateOptionRow(MaterialSymbolIcon("format_quote"), "Citation", "Reference material", colors.info) { onSelect("Citation") }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // ── PLAN ──
                    CreateSectionTitle("PLAN", colors.flashcard)
                    CreateOptionRow(MaterialSymbolIcon("checklist"), "Task", "Action item", colors.flashcard) { onSelect("Task") }
                    CreateOptionRow(MaterialSymbolIcon("play_circle"), "Survey Session", "Collect responses", colors.positive) { onSelect("Survey Session") }
                    CreateOptionRow(MaterialSymbolIcon("event"), "Field Visit", "Plan a trip", colors.data) { onSelect("Field Visit") }
                    CreateOptionRow(MaterialSymbolIcon("folder"), "Folder", "Organize entities", colors.hypothesis) { onSelect("Folder") }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CreateSectionTitle(title: String, accent: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accent)
        HorizontalDivider(modifier = Modifier.weight(1f), color = accent.copy(alpha = 0.15f))
    }
}

@Composable
private fun CreateOptionRow(icon: MaterialSymbolIcon, label: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Surface(
        onClick = { haptics.light(); onClick() },
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, size = 22.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(MaterialSymbolIcon("add_circle_outline"), "Add", tint = accent.copy(alpha = 0.6f), size = 22.dp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Helpers
// ══════════════════════════════════════════════════════════════════════

@Composable
internal fun StatusBadge(status: String) {
    val color = when (status) {
        "Active" -> colors.positive
        "Archived" -> MaterialTheme.colorScheme.onSurfaceVariant
        "Planning" -> colors.warning
        "Complete" -> colors.observation
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f), tonalElevation = 0.dp) {
        Text(status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun getRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun getRelativeTimeShort(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3600_000}h"
        else -> "${diff / 86_400_000}d"
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Preserved dialogs from original file
// ══════════════════════════════════════════════════════════════════════

//  New Task Dialog — Simplified version for in-project creation
@Composable
private fun NewTaskDialog(viewModel: FieldMindViewModel, projectId: Long, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var dueDate by remember { mutableStateOf("") }
    val haptics = rememberFieldMindHaptics()

    val priorityColor = mapOf("Low" to colors.positive, "Medium" to colors.warning, "High" to MaterialTheme.colorScheme.error)

    DialogWrapper(onDismiss = onDismiss, fullScreen = true, isDirty = { title.isNotBlank() || description.isNotBlank() }) {
        DialogHeader(icon = MaterialSymbolIcon("checklist"), title = "New Task", subtitle = "Define a field task, survey, or to-do for this project.", accent = colors.flashcard)
        Text("Linked to project", style = MaterialTheme.typography.labelSmall, color = colors.project, fontWeight = FontWeight.SemiBold)
        FieldTextField(title, { title = it }, "Task Name", supportingText = "Short, actionable title")
        FieldTextField(description, { description = it }, "Description", minLines = 3)
        FieldTextField(dueDate, { dueDate = it }, "Due Date", supportingText = "YYYY-MM-DD")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Priority", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Low", "Medium", "High").forEach { level ->
                    val isSelected = priority == level; val accent = priorityColor[level]!!
                    Surface(onClick = { haptics.light(); priority = level }, shape = RoundedCornerShape(14.dp), color = if (isSelected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh, border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, accent) else null, modifier = Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) { if (isSelected) Box(Modifier.size(8.dp).clip(CircleShape).background(accent)) }
                            Text(level, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        DialogActions(onCancel = onDismiss, onSave = { if (title.isNotBlank()) { viewModel.addTask(title = title, description = description, priority = priority, dueDate = dueDate, projectId = projectId); onDismiss() } }, saveEnabled = title.isNotBlank())
    }
}

//  New Attachment Dialog — Grid of file type pickers
@Composable
private fun NewAttachmentDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val haptics = rememberFieldMindHaptics()
    var capturedUri by remember { mutableStateOf<String?>(null) }
    var capturedType by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }; capturedUri = it.toString(); capturedType = "Image" } }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }; capturedUri = it.toString(); capturedType = "Video" } }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }; capturedUri = it.toString(); capturedType = "Audio" } }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }; capturedUri = it.toString(); capturedType = "File" } }

    DialogWrapper(onDismiss = onDismiss, isDirty = { capturedUri != null }) {
        DialogHeader(icon = MaterialSymbolIcon("attach_file"), title = "Add Attachment", subtitle = "Attach a file to this project.", accent = colors.warning)
        Text("Choose attachment type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttachmentTypeButton("Image", MaterialSymbolIcon("photo"), colors.observation, Modifier.weight(1f)) { haptics.light(); imagePicker.launch("image/*") }
                AttachmentTypeButton("Video", MaterialSymbolIcon("videocam"), colors.question, Modifier.weight(1f)) { haptics.light(); videoPicker.launch("video/*") }
                AttachmentTypeButton("Audio", MaterialSymbolIcon("mic"), colors.project, Modifier.weight(1f)) { haptics.light(); audioPicker.launch("audio/*") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttachmentTypeButton("PDF", MaterialSymbolIcon("picture_as_pdf"), MaterialTheme.colorScheme.error, Modifier.weight(1f)) { haptics.light(); filePicker.launch(arrayOf("application/pdf")) }
                AttachmentTypeButton("Sheet", MaterialSymbolIcon("table_chart"), colors.data, Modifier.weight(1f)) { haptics.light(); filePicker.launch(arrayOf("text/csv", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) }
                AttachmentTypeButton("File", MaterialSymbolIcon("description"), colors.hypothesis, Modifier.weight(1f)) { haptics.light(); filePicker.launch(arrayOf("*/*")) }
            }
        }
        if (capturedUri != null) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = colors.positive.copy(alpha = 0.08f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(MaterialSymbolIcon("check_circle"), null, tint = colors.positive, size = 24.dp)
                    Column(Modifier.weight(1f)) { Text("$capturedType attached", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); Text(capturedUri!!.substringAfterLast("/").take(40), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        DialogActions(onCancel = onDismiss, onSave = { if (capturedUri != null) { viewModel.addNote(title = "Attachment: $capturedType", body = "Attached on ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())}\nURI: $capturedUri", category = capturedType, tags = "attachment, $capturedType", onSaved = { onDismiss() }) } }, saveEnabled = capturedUri != null, saveLabel = "Attach")
    }
}

@Composable
private fun AttachmentTypeButton(label: String, icon: MaterialSymbolIcon, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, size = 22.dp) }
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = accent)
        }
    }
}

//  New Folder Dialog
@Composable
private fun NewFolderDialog(viewModel: FieldMindViewModel, projectId: Long, onDismiss: () -> Unit) {
    var folderName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF5F7F52) }
    val haptics = rememberFieldMindHaptics()

    val colorOptions = listOf(0xFF4CAF50L to "Green", 0xFF2196F3L to "Blue", 0xFF9C27B0L to "Purple", 0xFFFF9800L to "Orange", 0xFFF44336L to "Red")

    DialogWrapper(onDismiss = onDismiss, isDirty = { folderName.isNotBlank() }) {
        DialogHeader(icon = MaterialSymbolIcon("folder"), title = "New Folder", subtitle = "Organize project entities into a folder.", accent = colors.hypothesis)
        FieldTextField(folderName, { folderName = it }, "Folder Name", supportingText = "e.g. Butterflies, Water Samples")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Color", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colorOptions.forEach { (colorLong, _) ->
                    val isSelected = selectedColor == colorLong; val color = Color(colorLong)
                    val borderMod = if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(14.dp)) else Modifier
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(color).then(borderMod).clickable { haptics.light(); selectedColor = colorLong }, contentAlignment = Alignment.Center) { if (isSelected) Icon(MaterialSymbolIcon("check"), null, tint = Color.White, size = 22.dp) }
                }
            }
        }
        Text("Parent folder", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("None (root folder)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DialogActions(onCancel = onDismiss, onSave = { if (folderName.isNotBlank()) { viewModel.addFolder(name = folderName.trim(), color = selectedColor, projectId = projectId, onSaved = { onDismiss() }) } }, saveEnabled = folderName.isNotBlank(), saveLabel = "Create Folder")
    }
}

//  Entity Picker Dialog
@Composable
private fun <T> EntityPickerDialog(
    title: String, searchQuery: String, onSearchChange: (String) -> Unit, onDismiss: () -> Unit,
    items: List<T>, itemIcon: @Composable (T) -> Unit, itemPrimaryText: @Composable (T) -> String,
    itemSecondaryText: @Composable (T) -> String, onSelect: (T) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                OutlinedTextField(value = searchQuery, onValueChange = onSearchChange, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text("Search...") }, leadingIcon = { Icon(MaterialSymbolIcon("search"), null, size = 18.dp) }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh))
                if (items.isEmpty()) { Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text("No items found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) } }
                else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(items) { item ->
                            Surface(onClick = { onSelect(item) }, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) { itemIcon(item) }
                                    Column(Modifier.weight(1f)) { Text(itemPrimaryText(item), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(itemSecondaryText(item), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(end = 8.dp, bottom = 8.dp)) { Text("Cancel") }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Entity Card (used in record detail view)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ProjectEntityCard(
    kind: String, title: String, subtitle: String = "", body: String = "",
    tags: List<String> = emptyList(), linked: String = "",
    accentColor: Color, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(
                        when (kind) {
                            "observation" -> FieldMindIcons.Observation; "note" -> MaterialSymbolIcon("edit_note")
                            "question" -> FieldMindIcons.Question; "source" -> FieldMindIcons.Source
                            "task" -> MaterialSymbolIcon("checklist"); else -> MaterialSymbolIcon("circle")
                        }, null, tint = accentColor, size = 18.dp
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = accentColor)
                }
            }
            if (body.isNotBlank()) Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (tags.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.take(4).forEach { tag ->
                        Surface(shape = RoundedCornerShape(6.dp), color = accentColor.copy(alpha = 0.1f)) {
                            Text(tag, style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            if (linked.isNotBlank()) Text("Linked: $linked", style = MaterialTheme.typography.labelSmall, color = colors.info)
        }
    }
}
