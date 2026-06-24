package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fieldmind.research.app.features.field.data.database.entity.ProjectEntity
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

// ══════════════════════════════════════════════════════════════════════
//  Create Entity Bottom Sheet — Grouped create menu for Workspace
// ══════════════════════════════════════════════════════════════════════

/** Maps entity kinds to their best existing navigation screen. */
private fun fallbackScreen(kind: String): FieldMindScreen = when (kind) {
    "Observation" -> FieldMindScreen.Observe
    "Note" -> FieldMindScreen.Library
    "Question" -> FieldMindScreen.NewQuestion
    "Source" -> FieldMindScreen.Library
    "Task" -> FieldMindScreen.NewTask
    else -> FieldMindScreen.NewProject
}

@Composable
fun CreateEntitySheet(
    projects: List<ProjectEntity>,
    onNavigate: (FieldMindScreen) -> Unit,
    onDismiss: () -> Unit
) {
    var showProjectSelector by remember { mutableStateOf(false) }
    var pendingEntityKind by remember { mutableStateOf("") }
    val colors = FieldMindTheme.colors

    // ── Show Project Selector Dialog ──
    if (showProjectSelector) {
        val entityKind = pendingEntityKind
        ProjectSelectorDialog(
            title = "Select Project",
            subtitle = "Link this ${entityKind.lowercase()} to a project to keep related research together.",
            projects = projects,
            entityKind = entityKind,
            onSelectProject = { project ->
                showProjectSelector = false
                pendingEntityKind = ""
                onDismiss()
                // Navigate to the entity screen after project selection
                onNavigate(fallbackScreen(entityKind))
            },
            onCreateNew = {
                showProjectSelector = false
                pendingEntityKind = ""
                onDismiss()
                onNavigate(FieldMindScreen.NewProject)
            },
            onSkip = {
                showProjectSelector = false
                pendingEntityKind = ""
                onDismiss()
                onNavigate(fallbackScreen(entityKind))
            },
            onDismiss = {
                showProjectSelector = false
                pendingEntityKind = ""
            }
        )
        return
    }

    // ── Create Sheet ──
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Create", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Add a new research entity", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // ── Projects ──
                    SectionTitle("Projects", colors.project)
                    CreateItemCard("New Project", FieldMindIcons.Project, colors.project) {
                        onDismiss(); onNavigate(FieldMindScreen.NewProject)
                    }

                    // ── Research ──
                    SectionTitle("Research", colors.observation)
                    CreateItemCard("Observation", FieldMindIcons.Observation, colors.observation) {
                        pendingEntityKind = "Observation"; showProjectSelector = true
                    }
                    CreateItemCard("Note", FieldMindIcons.Note, colors.source) {
                        pendingEntityKind = "Note"; showProjectSelector = true
                    }
                    CreateItemCard("Question", MaterialSymbolIcon("question_answer"), colors.question) {
                        pendingEntityKind = "Question"; showProjectSelector = true
                    }
                    CreateItemCard("Source", FieldMindIcons.Source, colors.source) {
                        pendingEntityKind = "Source"; showProjectSelector = true
                    }

                    // ── Planning ──
                    SectionTitle("Planning", colors.hypothesis)
                    CreateItemCard("Task", MaterialSymbolIcon("checklist"), colors.flashcard) {
                        pendingEntityKind = "Task"; showProjectSelector = true
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, accent: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accent)
        HorizontalDivider(modifier = Modifier.weight(1f), color = accent.copy(alpha = 0.2f))
    }
}

@Composable
private fun CreateItemCard(label: String, icon: MaterialSymbolIcon, accent: Color, onClick: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Card(
        modifier = Modifier.fillMaxWidth().clickable { haptics.light(); onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon = icon, contentDescription = null, tint = accent, size = 24.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Create a new ${label.lowercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(MaterialSymbolIcon("add_circle"), contentDescription = "Add $label", tint = accent, size = 24.dp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Project Selector Dialog
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ProjectSelectorDialog(
    title: String,
    subtitle: String,
    projects: List<ProjectEntity>,
    entityKind: String,
    onSelectProject: (ProjectEntity) -> Unit,
    onCreateNew: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 24.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Entity-specific label
                val label = if (entityKind == "Observation") "Create $entityKind" else "Project Selector"
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                HorizontalDivider()

                // Existing projects
                if (projects.isNotEmpty()) {
                    Text("Link to existing project", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    projects.take(5).forEach { project ->
                        Surface(
                            onClick = { onSelectProject(project) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(FieldMindTheme.colors.project.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                                    Icon(FieldMindIcons.Project, null, tint = FieldMindTheme.colors.project, size = 22.dp)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(project.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(project.topicType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(FieldMindIcons.Forward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                            }
                        }
                    }
                    if (projects.size > 5) {
                        Text("+${projects.size - 5} more projects", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCreateNew, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Icon(FieldMindIcons.Add, null, size = 18.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("Create New Project")
                    }
                    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                        Text("Skip — create without project", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
