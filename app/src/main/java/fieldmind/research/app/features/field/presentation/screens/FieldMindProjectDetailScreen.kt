package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onNavigate: ((FieldMindScreen) -> Unit)? = null
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

    val project = projects.firstOrNull { it.id == projectId }
    val relatedObs = observations.filter { it.projectId == projectId }
    val relatedQs = questions.filter { it.relatedProjectId == projectId }
    val relatedNotes = notes.filter { it.projectId == projectId }
    val relatedSources = sources.filter { it.relatedProjectId == projectId }
    val relatedData = data.filter { it.projectId == projectId }
    val relatedReports = reports.filter { it.projectId == projectId }
    val colors = FieldMindTheme.colors

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
                        IconButton(onClick = { /* Rename */ }, modifier = Modifier.size(40.dp)) {
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

        // ── Action buttons: Add Observation, Note, Question, Source ──
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Add to project", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProjectActionButton(
                            onClick = { showNewObservation = true },
                            icon = FieldMindIcons.Observation,
                            label = "Observation",
                            accent = colors.observation,
                            modifier = Modifier.weight(1f)
                        )
                        ProjectActionButton(
                            onClick = { showNewNote = true },
                            icon = MaterialSymbolIcon("edit_note"),
                            label = "Note",
                            accent = colors.project,
                            modifier = Modifier.weight(1f)
                        )
                        ProjectActionButton(
                            onClick = { showNewQuestion = true },
                            icon = FieldMindIcons.Question,
                            label = "Question",
                            accent = colors.question,
                            modifier = Modifier.weight(1f)
                        )
                        ProjectActionButton(
                            onClick = { showNewSource = true },
                            icon = FieldMindIcons.Source,
                            label = "Source",
                            accent = colors.source,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                                    // Kind icon
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
                        onClick = { /* Open export */ }
                    )
                    ProjectActionTile(
                        icon = MaterialSymbolIcon("share"),
                        label = "Share Project",
                        subtitle = "Link / File / App",
                        onClick = { /* Open share */ }
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
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
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
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(FieldMindTheme.colors.project.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
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
        Text(
            status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun StatItem(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
