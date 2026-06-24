package fieldmind.research.app.features.field.presentation.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  PROJECT SETTINGS SCREEN
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ProjectSettingsScreen(
    projectId: Long,
    viewModel: FieldMindViewModel,
    onBack: () -> Unit = {},
    onOpenBackupSettings: (() -> Unit)? = null
) {
    val projects by viewModel.projects.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val haptics = rememberFieldMindHaptics()
    val context = LocalContext.current

    val project = projects.firstOrNull { it.id == projectId }
    val colors = FieldMindTheme.colors

    // ── Editable fields ──
    var projectName by remember(project) { mutableStateOf(project?.title ?: "") }
    var projectDescription by remember(project) { mutableStateOf(project?.objective ?: "") }

    // ── Theme color state ──
    val projectColorKey = "project_theme_$projectId"
    val settings = viewModel.fieldSettings
    // We'll use a simple in-memory color state; defaults to project theme color
    val defaultAccent = colors.project
    val colorOptions = listOf(
        colors.project to "Teal",
        colors.observation to "Blue",
        colors.question to "Purple",
        colors.source to "Orange",
        colors.hypothesis to "Pink",
        colors.positive to "Green",
        colors.data to "Cyan",
        colors.warning to "Amber"
    )
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    // ── Default observation fields ──
    var defaultCategory by remember { mutableStateOf("") }
    var defaultConfidence by remember { mutableStateOf("") }

    // ── Export format ──
    val exportFormats = listOf("Markdown", "Plain Text", "JSON")
    var exportFormat by remember { mutableStateOf("Markdown") }
    var showExportFormatPicker by remember { mutableStateOf(false) }

    // ── Backup ──
    var autoBackup by remember { mutableStateOf(false) }

    // ── Dialogs ──
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (project == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Project not found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val relatedObs = observations.filter { it.projectId == project.id }
    val relatedNotes = notes.filter { it.projectId == project.id }
    val relatedQs = questions.filter { it.relatedProjectId == project.id }
    val relatedSources = sources.filter { it.relatedProjectId == project.id }

    // ── Has any changes? ──
    val hasNameChanges = projectName.trim() != project.title
    val hasDescChanges = projectDescription.trim() != project.objective
    val hasUnsaved = hasNameChanges || hasDescChanges

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ════════════════════════════════════════════════════════════
        //  Header
        // ════════════════════════════════════════════════════════════
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(MaterialSymbolIcon("arrow_back"), "Back", size = 22.dp)
                }
                Text(
                    "Project Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Save button (visible when there are unsaved changes)
                if (hasUnsaved) {
                    TextButton(
                        onClick = {
                            haptics.confirm()
                            if (hasNameChanges) {
                                viewModel.updateProjectEntity(project.copy(title = projectName.trim()))
                            }
                            if (hasDescChanges) {
                                viewModel.updateProjectEntity(project.copy(objective = projectDescription.trim()))
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(MaterialSymbolIcon("save"), null, size = 16.dp)
                        Spacer(Modifier.size(4.dp))
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Spacer(Modifier.size(40.dp))
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  SECTION: Project Name
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("badge"),
                title = "Project Name",
                iconTint = colors.project
            ) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = colors.project.copy(alpha = 0.3f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                )
            }
        }

        // ════════════════════════════════════════════════════════════
        //  SECTION: Description
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("description"),
                title = "Description",
                iconTint = colors.project
            ) {
                OutlinedTextField(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    placeholder = {
                        Text(
                            "Describe the project's objective...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = colors.project.copy(alpha = 0.3f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                )
            }
        }

        // ════════════════════════════════════════════════════════════
        //  SECTION: Theme Color
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("palette"),
                title = "Theme Color",
                iconTint = colors.project
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Choose an accent color for this project.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.forEachIndexed { index, (color, _) ->
                            Surface(
                                onClick = { selectedColorIndex = index },
                                shape = CircleShape,
                                color = if (selectedColorIndex == index) color else color.copy(alpha = 0.35f),
                                modifier = Modifier.size(36.dp),
                                border = if (selectedColorIndex == index)
                                    androidx.compose.foundation.BorderStroke(2.dp, color)
                                else null
                            ) {
                                if (selectedColorIndex == index) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            MaterialSymbolIcon("check"),
                                            null,
                                            size = 16.dp,
                                            tint = if (color.luminance() > 0.5f) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  SECTION: Default Observation Fields
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("fact_check"),
                title = "Default Observation Fields",
                iconTint = colors.observation
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Pre-fill default values when adding observations to this project.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = defaultCategory,
                        onValueChange = { defaultCategory = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Default category") },
                        placeholder = { Text("e.g. Bird, Mammal, Plant") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                    OutlinedTextField(
                        value = defaultConfidence,
                        onValueChange = { defaultConfidence = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Default confidence") },
                        placeholder = { Text("e.g. Sure, Likely, Unsure") },
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

        // ════════════════════════════════════════════════════════════
        //  SECTION: Sharing
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("share"),
                title = "Sharing",
                iconTint = colors.observation
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Share this project with others as text or a file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
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
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Project: ${project.title}")
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Project"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(MaterialSymbolIcon("text_snippet"), null, size = 16.dp)
                            Spacer(Modifier.size(6.dp))
                            Text("Share as text")
                        }
                        OutlinedButton(
                            onClick = {
                                // Same as the existing export in ProjectDetailScreen
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
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/markdown"
                                    putExtra(Intent.EXTRA_SUBJECT, "${project.title} — FieldMind Export")
                                    putExtra(Intent.EXTRA_TEXT, exportText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export Project As"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(MaterialSymbolIcon("file_download"), null, size = 16.dp)
                            Spacer(Modifier.size(6.dp))
                            Text("Export file")
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  SECTION: Export Preferences
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("file_download"),
                title = "Export Preferences",
                iconTint = colors.data
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Format", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Choose the default export format", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            onClick = { showExportFormatPicker = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(exportFormat, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Icon(MaterialSymbolIcon("expand_more"), null, size = 16.dp)
                            }
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  SECTION: Backup Settings
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("backup"),
                title = "Backup Settings",
                iconTint = colors.data
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Auto-backup this project", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Periodically save project data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoBackup,
                            onCheckedChange = { autoBackup = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = colors.project)
                        )
                    }
                    if (onOpenBackupSettings != null) {
                        TextButton(
                            onClick = onOpenBackupSettings,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(MaterialSymbolIcon("settings"), null, size = 16.dp)
                            Spacer(Modifier.size(6.dp))
                            Text("Advanced backup settings")
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  SECTION: Archive Project
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("archive"),
                title = "Archive Project",
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (project.status == "Archived") "This project is currently archived."
                        else "Archive this project to hide it from the active list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    OutlinedButton(
                        onClick = {
                            if (project.status == "Archived") {
                                haptics.confirm()
                                viewModel.updateProjectEntity(project.copy(status = "Active", archivedAt = null))
                            } else {
                                showArchiveConfirm = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (project.status == "Archived") MaterialSymbolIcon("unarchive") else MaterialSymbolIcon("archive"),
                            null, size = 16.dp
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(if (project.status == "Archived") "Unarchive project" else "Archive project")
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  SECTION: Delete Project
        // ════════════════════════════════════════════════════════════
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(MaterialSymbolIcon("delete_forever"), null, tint = MaterialTheme.colorScheme.error, size = 20.dp)
                        Text("Delete Project", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        "Permanently delete this project and all its linked data. This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Button(
                        onClick = { showDeleteConfirm = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(MaterialSymbolIcon("delete"), null, size = 16.dp)
                        Spacer(Modifier.size(6.dp))
                        Text("Delete project")
                    }
                }
            }
        }

        // ── Footer ──
        item {
            Text(
                "Project created ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(project.createdAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                fontWeight = FontWeight.Normal
            )
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Export Format Picker Dialog
    // ════════════════════════════════════════════════════════════════
    if (showExportFormatPicker) {
        Dialog(
            onDismissRequest = { showExportFormatPicker = false },
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
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Export Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    exportFormats.forEach { format ->
                        Surface(
                            onClick = {
                                exportFormat = format
                                showExportFormatPicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (exportFormat == format) colors.project.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(format, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                if (exportFormat == format) {
                                    Icon(MaterialSymbolIcon("check_circle", filled = true), null, tint = colors.project, size = 20.dp)
                                }
                            }
                        }
                    }
                    TextButton(onClick = { showExportFormatPicker = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Archive Confirmation Dialog
    // ════════════════════════════════════════════════════════════════
    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            icon = { Icon(MaterialSymbolIcon("archive"), null, size = 28.dp, tint = colors.project) },
            title = { Text("Archive project?") },
            text = {
                Text("This project will be moved to the archive and hidden from the active list. You can unarchive it later from settings.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.confirm()
                        viewModel.updateProjectEntity(project.copy(status = "Archived", archivedAt = System.currentTimeMillis()))
                        showArchiveConfirm = false
                    },
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ════════════════════════════════════════════════════════════════
    //  Delete Confirmation Dialog
    // ════════════════════════════════════════════════════════════════
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(MaterialSymbolIcon("delete_forever"), null, size = 28.dp, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete this project?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This action is permanent and cannot be undone. All project data will be soft-deleted:")
                    Text(
                        "• ${relatedObs.size} observations\n• ${relatedNotes.size} notes\n• ${relatedQs.size} questions\n• ${relatedSources.size} sources",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.confirm()
                        viewModel.deleteProject(project.id)
                        showDeleteConfirm = false
                        onBack()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Reusable Components
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun SectionCard(
    icon: MaterialSymbolIcon,
    title: String,
    iconTint: Color = FieldMindTheme.colors.flashcard,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, size = 18.dp, tint = iconTint)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}
