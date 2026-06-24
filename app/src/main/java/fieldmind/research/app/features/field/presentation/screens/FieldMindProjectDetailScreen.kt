package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  Project Detail Screen — Per HTML spec: actions, activity, stats
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
    val projects by viewModel.projects.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val notes by viewModel.notes.collectAsState()

    var showNewObservation by remember { mutableStateOf(false) }
    var showNewNote by remember { mutableStateOf(false) }
    var showNewQuestion by remember { mutableStateOf(false) }
    var showNewSource by remember { mutableStateOf(false) }
    var showNewTask by remember { mutableStateOf(false) }
    var showNewAttachment by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    // ── Picker dialogs state ──
    var showObservationPicker by remember { mutableStateOf(false) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var obsPickerSearch by remember { mutableStateOf("") }
    var srcPickerSearch by remember { mutableStateOf("") }

    val project = projects.firstOrNull { it.id == projectId }
    val relatedObs = observations.filter { it.projectId == projectId }
    val relatedQs = questions.filter { it.relatedProjectId == projectId }
    val relatedNotes = notes.filter { it.projectId == projectId }
    val relatedSources = sources.filter { it.relatedProjectId == projectId }
    val relatedData = data.filter { it.projectId == projectId }
    val relatedReports = reports.filter { it.projectId == projectId }
    val colors = FieldMindTheme.colors
    val context = LocalContext.current
    val haptics = rememberFieldMindHaptics()

    if (project == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Project not found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // ── Recent activity: combine observations, notes, questions sorted by timestamp ──
    data class ActivityItem(val kind: String, val label: String, val id: Long, val timestamp: Long)
    val recentActivity = remember(relatedObs, relatedNotes, relatedQs) {
        buildList {
            relatedObs.forEach { add(ActivityItem("Observation", it.subject.ifBlank { "Unnamed observation" }, it.id, it.timestamp)) }
            relatedNotes.take(20).forEach { add(ActivityItem("Note", it.title.ifBlank { "Unnamed note" }, it.id, it.createdAt)) }
            relatedQs.take(20).forEach { add(ActivityItem("Question", it.questionText.ifBlank { "Unnamed question" }, it.id, it.createdAt)) }
        }.sortedByDescending { it.timestamp }.take(15)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Back button + header ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Back + actions row
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(MaterialSymbolIcon("arrow_back"), "Back", size = 22.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // + Create button — opens the create sheet
                        FilledTonalIconButton(
                            onClick = { haptics.light(); showCreateSheet = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(MaterialSymbolIcon("add"), "Add", size = 22.dp)
                        }
                        IconButton(onClick = { showRenameDialog = true }, modifier = Modifier.size(40.dp)) {
                            Icon(MaterialSymbolIcon("more_vert"), "Menu", size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Project icon + name
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                            .background(colors.project.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(FieldMindIcons.Project, null, tint = colors.project, size = 30.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(project.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(project.topicType, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            StatusBadge(project.status)
                        }
                    }
                }

                // Description
                if (project.objective.isNotBlank()) {
                    Text(
                        project.objective,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ── Statistics ──
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(FieldMindIcons.Insights, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                        Text("Statistics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("${relatedObs.size}", "Observations", colors.observation)
                        StatItem("${relatedNotes.size}", "Notes", colors.project)
                        StatItem("${relatedQs.size}", "Questions", colors.question)
                        StatItem("${relatedSources.size}", "Sources", colors.source)
                    }
                    if (relatedData.isNotEmpty() || relatedReports.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            if (relatedData.isNotEmpty()) {
                                Text("${relatedData.size} data records", style = MaterialTheme.typography.labelMedium, color = colors.data)
                                if (relatedReports.isNotEmpty()) Text("  ·  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (relatedReports.isNotEmpty()) {
                                Text("${relatedReports.size} reports", style = MaterialTheme.typography.labelMedium, color = colors.report)
                            }
                        }
                    }
                }
            }
        }

        // ── Recent Activity ──
        if (recentActivity.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(FieldMindIcons.Timer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                            Text("Recent Activity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        recentActivity.take(8).forEach { item ->
                            Surface(
                                onClick = { onOpenDetail(item.kind.lowercase(), item.id) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val kindIcon = when (item.kind) {
                                        "Observation" -> FieldMindIcons.Observation to colors.observation
                                        "Note" -> MaterialSymbolIcon("edit_note") to colors.project
                                        "Question" -> FieldMindIcons.Question to colors.question
                                        else -> FieldMindIcons.Info to colors.data
                                    }
                                    Box(
                                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                            .background(kindIcon.second.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(kindIcon.first, null, tint = kindIcon.second, size = 18.dp)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(item.kind, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            item.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(MaterialSymbolIcon("chevron_right"), null, size = 18.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                        if (relatedObs.size + relatedNotes.size + relatedQs.size > 8) {
                            Text(
                                "View all ${relatedObs.size + relatedNotes.size + relatedQs.size} items",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.project,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth().clickable { /* Expand all */ }.padding(vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // ── Linked Entities ──
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(MaterialSymbolIcon("link"), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                        Text("Link existing entities", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    // Observations
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Observations", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = colors.observation)
                        if (relatedObs.isEmpty()) {
                            Text(
                                "No observations linked yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        } else {
                            relatedObs.take(3).forEach { obs ->
                                Surface(
                                    onClick = { onOpenDetail("observation", obs.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                                .background(colors.observation.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) { Icon(FieldMindIcons.Observation, null, tint = colors.observation, size = 16.dp) }
                                        Column(Modifier.weight(1f)) {
                                            Text(obs.subject.ifBlank { "Observation #${obs.id}" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(obs.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(
                                            onClick = { viewModel.updateObservation(obs.copy(projectId = null)) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(MaterialSymbolIcon("link_off"), "Unlink", size = 14.dp, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                            }
                            if (relatedObs.size > 3) {
                                Text("+${relatedObs.size - 3} more", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(
                            onClick = { showObservationPicker = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(MaterialSymbolIcon("add_link"), null, size = 16.dp)
                            Spacer(Modifier.size(6.dp))
                            Text("Link existing observation")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Sources
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Sources", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = colors.source)
                        if (relatedSources.isEmpty()) {
                            Text(
                                "No sources linked yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        } else {
                            relatedSources.forEach { src ->
                                Surface(
                                    onClick = { onOpenDetail("source", src.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                                .background(colors.source.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) { Icon(FieldMindIcons.Category, null, tint = colors.source, size = 16.dp) }
                                        Column(Modifier.weight(1f)) {
                                            Text(src.title.ifBlank { "Source #${src.id}" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${src.type} • ${src.readingStatus}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(
                                            onClick = { viewModel.updateSourceEntity(src.copy(relatedProjectId = null)) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(MaterialSymbolIcon("link_off"), "Unlink", size = 14.dp, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { showSourcePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(MaterialSymbolIcon("add_link"), null, size = 16.dp)
                            Spacer(Modifier.size(6.dp))
                            Text("Link existing source")
                        }
                    }
                }
            }
        }

        // ── Actions: Export, Share, Archive ──
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Project actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    ProjectActionTile(
                        icon = MaterialSymbolIcon("file_download"),
                        label = "Export Project",
                        subtitle = "PDF / Markdown",
                        onClick = {
                            val exportText = buildString {
                                appendLine("# ${project.title}")
                                appendLine()
                                appendLine("**Topic:** ${project.topicType}")
                                appendLine("**Status:** ${project.status}")
                                appendLine("**Created:** ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(project.createdAt))}")
                                appendLine()
                                if (project.objective.isNotBlank()) appendLine("## Objective\n${project.objective}\n")
                                if (project.researchQuestion.isNotBlank()) appendLine("## Research Question\n${project.researchQuestion}\n")
                                if (project.methods.isNotBlank()) appendLine("## Methods\n${project.methods}\n")
                                if (project.conclusion.isNotBlank()) appendLine("## Conclusion\n${project.conclusion}\n")
                                appendLine("---")
                                appendLine("Observations: ${relatedObs.size}")
                                appendLine("Notes: ${relatedNotes.size}")
                                appendLine("Questions: ${relatedQs.size}")
                                appendLine("Sources: ${relatedSources.size}")
                                if (relatedData.isNotEmpty()) appendLine("Data records: ${relatedData.size}")
                                if (relatedReports.isNotEmpty()) appendLine("Reports: ${relatedReports.size}")
                                appendLine()
                                appendLine("Exported from FieldMind")
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/markdown"
                                putExtra(Intent.EXTRA_SUBJECT, "${project.title} — FieldMind Export")
                                putExtra(Intent.EXTRA_TEXT, exportText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Project As"))
                        }
                    )
                    ProjectActionTile(
                        icon = MaterialSymbolIcon("share"),
                        label = "Share Project",
                        subtitle = "Link / File / App",
                        onClick = {
                            val shareText = buildString {
                                appendLine("📁 ${project.title}")
                                if (project.objective.isNotBlank()) appendLine(project.objective)
                                appendLine()
                                appendLine("Type: ${project.topicType}")
                                appendLine("Status: ${project.status}")
                                appendLine("Observations: ${relatedObs.size}")
                                appendLine("Notes: ${relatedNotes.size}")
                                appendLine("Questions: ${relatedQs.size}")
                                appendLine("Sources: ${relatedSources.size}")
                                appendLine("Updated: ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(project.updatedAt))}")
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Project: ${project.title}")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Project"))
                        }
                    )
                    ProjectActionTile(
                        icon = MaterialSymbolIcon("hub"),
                        label = "View Relations",
                        subtitle = "Linked entities per observation",
                        onClick = { onOpenRelations?.invoke() }
                    )
                    ProjectActionTile(
                        icon = MaterialSymbolIcon("settings"),
                        label = "Project Settings",
                        subtitle = "Name, description, export, archive & more",
                        onClick = { onOpenSettings?.invoke(project.id) }
                    )
                    ProjectActionTile(
                        icon = MaterialSymbolIcon("archive"),
                        label = if (project.status == "Archived") "Unarchive Project" else "Archive Project",
                        subtitle = if (project.status == "Archived") "Restore to active projects" else "Move to archive",
                        onClick = {
                            viewModel.updateProjectEntity(
                                project.copy(
                                    status = if (project.status == "Archived") "Active" else "Archived",
                                    archivedAt = if (project.status == "Archived") null else System.currentTimeMillis()
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    // ── New Observation Dialog ──
    if (showNewObservation) {
        NewObservationDialog(
            viewModel = viewModel,
            projectId = project.id,
            onDismiss = { showNewObservation = false }
        )
    }

    // ── New Note Dialog ──
    if (showNewNote) {
        NewNoteDialog(
            viewModel = viewModel,
            projectId = project.id,
            onDismiss = { showNewNote = false }
        )
    }

    // ── New Question Dialog ──
    if (showNewQuestion) {
        NewQuestionDialog(
            viewModel = viewModel,
            projectId = project.id,
            onDismiss = { showNewQuestion = false }
        )
    }

    // ── New Source Dialog ──
    if (showNewSource) {
        NewSourceDialog(
            viewModel = viewModel,
            initialProjectId = project.id,
            onDismiss = { showNewSource = false }
        )
    }

    // ── Rename Dialog ──
    if (showRenameDialog) {
        var renameText by remember { mutableStateOf(project.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            icon = { Icon(FieldMindIcons.Edit, null, size = 28.dp) },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Project name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.updateProjectEntity(project.copy(title = renameText.trim()))
                        }
                        showRenameDialog = false
                    },
                    enabled = renameText.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ════════════════════════════════════════════════════════════════
    //  Project Create Sheet — Grouped create menu
    // ════════════════════════════════════════════════════════════════
    if (showCreateSheet) {
        ProjectCreateSheet(
            onSelect = { kind ->
                showCreateSheet = false
                when (kind) {
                    "Observation" -> showNewObservation = true
                    "Note" -> showNewNote = true
                    "Question" -> showNewQuestion = true
                    "Source" -> showNewSource = true
                    "Task" -> showNewTask = true
                    "Survey Session" -> onNavigate?.invoke(FieldMindScreen.ResearchSession)
                    "Attachment" -> showNewAttachment = true
                    "Folder" -> showNewFolder = true
                }
            },
            onDismiss = { showCreateSheet = false }
        )
    }

    // ── New Task Dialog ──
    if (showNewTask) {
        NewTaskDialog(
            viewModel = viewModel,
            projectId = project.id,
            onDismiss = { showNewTask = false }
        )
    }

    // ── New Attachment Dialog ──
    if (showNewAttachment) {
        NewAttachmentDialog(
            viewModel = viewModel,
            onDismiss = { showNewAttachment = false }
        )
    }

    // ── New Folder Dialog ──
    if (showNewFolder) {
        NewFolderDialog(
            viewModel = viewModel,
            projectId = project.id,
            onDismiss = { showNewFolder = false }
        )
    }

    // ════════════════════════════════════════════════════════════════
    //  Observation Picker Dialog
    // ════════════════════════════════════════════════════════════════
    if (showObservationPicker) {
        EntityPickerDialog(
            title = "Link Observation to Project",
            searchQuery = obsPickerSearch,
            onSearchChange = { obsPickerSearch = it },
            onDismiss = {
                showObservationPicker = false
                obsPickerSearch = ""
            },
            items = observations.filter { obs ->
                obs.deletedAt == null && obs.projectId != project.id &&
                (obsPickerSearch.isBlank() ||
                 obs.subject.contains(obsPickerSearch, ignoreCase = true) ||
                 obs.category.contains(obsPickerSearch, ignoreCase = true))
            },
            itemIcon = { Icon(FieldMindIcons.Observation, null, tint = colors.observation, size = 16.dp) },
            itemPrimaryText = { it.subject.ifBlank { "Observation #${it.id}" } },
            itemSecondaryText = { "${it.category} • ${it.date}" },
            onSelect = { obs ->
                haptics.confirm()
                viewModel.updateObservation(obs.copy(projectId = project.id))
                showObservationPicker = false
                obsPickerSearch = ""
            }
        )
    }

    // ════════════════════════════════════════════════════════════════
    //  Source Picker Dialog
    // ════════════════════════════════════════════════════════════════
    if (showSourcePicker) {
        EntityPickerDialog(
            title = "Link Source to Project",
            searchQuery = srcPickerSearch,
            onSearchChange = { srcPickerSearch = it },
            onDismiss = {
                showSourcePicker = false
                srcPickerSearch = ""
            },
            items = sources.filter { src ->
                src.deletedAt == null && src.relatedProjectId != project.id &&
                (srcPickerSearch.isBlank() ||
                 src.title.contains(srcPickerSearch, ignoreCase = true) ||
                 src.type.contains(srcPickerSearch, ignoreCase = true))
            },
            itemIcon = { Icon(FieldMindIcons.Category, null, tint = colors.source, size = 16.dp) },
            itemPrimaryText = { it.title.ifBlank { "Source #${it.id}" } },
            itemSecondaryText = { "${it.type} • ${it.readingStatus}" },
            onSelect = { src ->
                haptics.confirm()
                viewModel.updateSourceEntity(src.copy(relatedProjectId = project.id))
                showSourcePicker = false
                srcPickerSearch = ""
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Project Create Sheet — Grouped options dialog
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ProjectCreateSheet(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FieldMindTheme.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            // Scrim
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(onClick = onDismiss))
            // Sheet
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Handle bar
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
                    }

                    // Header
                    Text("Create", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    // ── Research ──
                    GroupTitle("Research", colors.observation)
                    CreateOption("Observation", FieldMindIcons.Observation, colors.observation) { onSelect("Observation") }
                    CreateOption("Note", MaterialSymbolIcon("edit_note"), colors.project) { onSelect("Note") }
                    CreateOption("Question", MaterialSymbolIcon("question_answer"), colors.question) { onSelect("Question") }
                    CreateOption("Source", FieldMindIcons.Source, colors.source) { onSelect("Source") }
                    CreateOption("Task", MaterialSymbolIcon("checklist"), colors.flashcard) { onSelect("Task") }

                    // ── Divider ──
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // ── Tools ──
                    GroupTitle("Tools", colors.data)
                    CreateOption("Survey Session", MaterialSymbolIcon("play_circle"), colors.positive) { onSelect("Survey Session") }
                    CreateOption("Attachment", MaterialSymbolIcon("attach_file"), colors.warning) { onSelect("Attachment") }

                    // ── Divider ──
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // ── Organize ──
                    GroupTitle("Organize", colors.hypothesis)
                    CreateOption("Folder", MaterialSymbolIcon("folder"), colors.hypothesis) { onSelect("Folder") }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(title: String, accent: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accent)
        HorizontalDivider(modifier = Modifier.weight(1f), color = accent.copy(alpha = 0.15f))
    }
}

@Composable
private fun CreateOption(label: String, icon: MaterialSymbolIcon, accent: Color, onClick: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Card(
        modifier = Modifier.fillMaxWidth().clickable { haptics.light(); onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, size = 24.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Create a new ${label.lowercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(MaterialSymbolIcon("add_circle"), "Add $label", tint = accent, size = 24.dp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  New Task Dialog — Simplified version for in-project creation
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun NewTaskDialog(
    viewModel: FieldMindViewModel,
    projectId: Long,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var dueDate by remember { mutableStateOf("") }
    val haptics = rememberFieldMindHaptics()

    val priorityColor = mapOf(
        "Low" to FieldMindTheme.colors.positive,
        "Medium" to FieldMindTheme.colors.warning,
        "High" to MaterialTheme.colorScheme.error
    )

    DialogWrapper(onDismiss = onDismiss, fullScreen = true, isDirty = { title.isNotBlank() || description.isNotBlank() }) {
        DialogHeader(
            icon = MaterialSymbolIcon("checklist"),
            title = "New Task",
            subtitle = "Define a field task, survey, or to-do for this project.",
            accent = FieldMindTheme.colors.flashcard
        )
        Text("Linked to project", style = MaterialTheme.typography.labelSmall, color = FieldMindTheme.colors.project, fontWeight = FontWeight.SemiBold)
        FieldTextField(title, { title = it }, "Task Name", supportingText = "Short, actionable title")
        FieldTextField(description, { description = it }, "Description", minLines = 3)
        FieldTextField(dueDate, { dueDate = it }, "Due Date", supportingText = "YYYY-MM-DD")

        // Priority radio
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Priority", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Low", "Medium", "High").forEach { level ->
                    val isSelected = priority == level
                    val accent = priorityColor[level]!!
                    Surface(
                        onClick = { haptics.light(); priority = level },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, accent) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(18.dp).clip(CircleShape)
                                    .background(if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                                }
                            }
                            Text(level, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        DialogActions(onCancel = onDismiss, onSave = {
            if (title.isNotBlank()) {
                viewModel.addTask(title = title, description = description, priority = priority, dueDate = dueDate, projectId = projectId)
                onDismiss()
            }
        }, saveEnabled = title.isNotBlank())
    }
}

// ══════════════════════════════════════════════════════════════════════
//  New Attachment Dialog — Grid of file type pickers
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun NewAttachmentDialog(
    viewModel: FieldMindViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberFieldMindHaptics()
    var capturedUri by remember { mutableStateOf<String?>(null) }
    var capturedType by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            capturedUri = it.toString()
            capturedType = "Image"
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            capturedUri = it.toString()
            capturedType = "Video"
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            capturedUri = it.toString()
            capturedType = "Audio"
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            capturedUri = it.toString()
            capturedType = "File"
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            capturedUri = it.toString()
            capturedType = "PDF"
        }
    }
    val sheetPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            capturedUri = it.toString()
            capturedType = "Sheet"
        }
    }

    DialogWrapper(onDismiss = onDismiss, isDirty = { capturedUri != null }) {
        DialogHeader(
            icon = MaterialSymbolIcon("attach_file"),
            title = "Add Attachment",
            subtitle = "Attach a file to this project.",
            accent = FieldMindTheme.colors.warning
        )
        Text("Choose attachment type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        // 3x2 grid of attachment type buttons
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttachmentTypeButton("Image", MaterialSymbolIcon("photo"), FieldMindTheme.colors.observation, Modifier.weight(1f)) { haptics.light(); imagePicker.launch("image/*") }
                AttachmentTypeButton("Video", MaterialSymbolIcon("videocam"), FieldMindTheme.colors.question, Modifier.weight(1f)) { haptics.light(); videoPicker.launch("video/*") }
                AttachmentTypeButton("Audio", MaterialSymbolIcon("mic"), FieldMindTheme.colors.project, Modifier.weight(1f)) { haptics.light(); audioPicker.launch("audio/*") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttachmentTypeButton("PDF", MaterialSymbolIcon("picture_as_pdf"), MaterialTheme.colorScheme.error, Modifier.weight(1f)) { haptics.light(); pdfPicker.launch(arrayOf("application/pdf")) }
                AttachmentTypeButton("Sheet", MaterialSymbolIcon("table_chart"), FieldMindTheme.colors.data, Modifier.weight(1f)) { haptics.light(); sheetPicker.launch(arrayOf("text/csv", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) }
                AttachmentTypeButton("File", MaterialSymbolIcon("description"), FieldMindTheme.colors.hypothesis, Modifier.weight(1f)) { haptics.light(); filePicker.launch(arrayOf("*/*")) }
            }
        }

        // Captured file preview
        if (capturedUri != null) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = FieldMindTheme.colors.positive.copy(alpha = 0.08f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(MaterialSymbolIcon("check_circle"), null, tint = FieldMindTheme.colors.positive, size = 24.dp)
                    Column(Modifier.weight(1f)) {
                        Text("$capturedType attached", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(capturedUri!!.substringAfterLast("/").take(40), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        DialogActions(onCancel = onDismiss, onSave = {
            // Attach the URI to the project via a note attachment or data record
            if (capturedUri != null) {
                viewModel.addNote(
                    title = "Attachment: $capturedType",
                    body = "Attached on ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())}\nURI: $capturedUri",
                    category = capturedType,
                    tags = "attachment, $capturedType",
                    onSaved = { onDismiss() }
                )
            }
        }, saveEnabled = capturedUri != null, saveLabel = "Attach")
    }
}

@Composable
private fun AttachmentTypeButton(
    label: String,
    icon: MaterialSymbolIcon,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, size = 22.dp)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = accent)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  New Folder Dialog — Create a folder (organizational note)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun NewFolderDialog(
    viewModel: FieldMindViewModel,
    projectId: Long,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF5F7F52) }
    val haptics = rememberFieldMindHaptics()

    val colorOptions = listOf(
        0xFF4CAF50L to "Green",
        0xFF2196F3L to "Blue",
        0xFF9C27B0L to "Purple",
        0xFFFF9800L to "Orange",
        0xFFF44336L to "Red"
    )

    DialogWrapper(onDismiss = onDismiss, isDirty = { folderName.isNotBlank() }) {
        DialogHeader(
            icon = MaterialSymbolIcon("folder"),
            title = "New Folder",
            subtitle = "Organize project entities into a folder.",
            accent = FieldMindTheme.colors.hypothesis
        )
        FieldTextField(folderName, { folderName = it }, "Folder Name", supportingText = "e.g. Butterflies, Water Samples")

        // Color picker
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Color", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colorOptions.forEach { (colorLong, colorName) ->
                    val isSelected = selectedColor == colorLong
                    val color = Color(colorLong)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(color)
                            .then(
                                if (isSelected) Modifier
                                    .border(3.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(14.dp))
                                else Modifier
                            )
                            .clickable { haptics.light(); selectedColor = colorLong },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(MaterialSymbolIcon("check"), null, tint = Color.White, size = 22.dp)
                        }
                    }
                }
            }
        }

        Text("Parent folder", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("None (root folder)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        DialogActions(onCancel = onDismiss, onSave = {
            if (folderName.isNotBlank()) {
                // Create a note to represent the folder (persistable without new entity type)
                viewModel.addNote(
                    title = "📁 $folderName",
                    body = "Folder created on ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())}",
                    category = "Folder",
                    tags = "folder, ${folderName.lowercase().replace(" ", "-")}",
                    projectId = projectId,
                    onSaved = { onDismiss() }
                )
            }
        }, saveEnabled = folderName.isNotBlank(), saveLabel = "Create Folder")
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Entity Picker Dialog (reusable searchable list)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun <T> EntityPickerDialog(
    title: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onDismiss: () -> Unit,
    items: List<T>,
    itemIcon: @Composable (T) -> Unit,
    itemPrimaryText: @Composable (T) -> String,
    itemSecondaryText: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text("Search...") },
                    leadingIcon = { Icon(MaterialSymbolIcon("search"), null, size = 18.dp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
                if (items.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No items found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(items) { item ->
                            Surface(
                                onClick = { onSelect(item) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                                        itemIcon(item)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(itemPrimaryText(item), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(itemSecondaryText(item), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(end = 8.dp, bottom = 8.dp)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun ProjectActionButton(
    onClick: () -> Unit,
    icon: MaterialSymbolIcon,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, size = 20.dp)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun ProjectActionTile(
    icon: MaterialSymbolIcon,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(FieldMindTheme.colors.project.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = FieldMindTheme.colors.project, size = 22.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(MaterialSymbolIcon("chevron_right"), null, size = 20.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
internal fun StatusBadge(status: String) {
    val color = when (status) {
        "Active" -> FieldMindTheme.colors.positive
        "Archived" -> MaterialTheme.colorScheme.onSurfaceVariant
        "Planning" -> FieldMindTheme.colors.warning
        "Complete" -> FieldMindTheme.colors.observation
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        tonalElevation = 0.dp
    ) {
        Text(status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun StatItem(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
