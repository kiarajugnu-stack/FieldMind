package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  TASK DETAIL SCREEN
// ══════════════════════════════════════════════════════════════════════

@Composable
fun TaskDetailScreen(
    taskId: Long,
    viewModel: FieldMindViewModel,
    onBack: () -> Unit = {},
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    val tasks by viewModel.tasks.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val haptics = rememberFieldMindHaptics()

    val task = tasks.firstOrNull { it.id == taskId }

    if (task == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(MaterialSymbolIcon("checklist"), null, size = 48.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                Text("Task not found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    // ── Parse checklist from JSON ──
    data class ChecklistItem(val text: String, val done: Boolean)
    val checklistItems = remember(task.checklistJson) {
        if (task.checklistJson.isNotBlank()) {
            try {
                val arr = JSONArray(task.checklistJson)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    ChecklistItem(obj.optString("text", ""), obj.optBoolean("done", false))
                }
            } catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    val doneCount = checklistItems.count { it.done }
    val progress = if (checklistItems.isNotEmpty()) (doneCount * 100 / checklistItems.size) else task.progress

    // ── Parse attachment URIs ──
    val attachments = remember(task.attachmentUris) {
        if (task.attachmentUris.isNotBlank()) task.attachmentUris.split(",").filter { it.isNotBlank() }
        else emptyList()
    }

    // ── Linked observations ──
    val linkedObs = remember(observations, task) {
        if (task.linkedObservationId != null) observations.filter { it.id == task.linkedObservationId }.take(5)
        else emptyList()
    }

    // ── Priority colors ──
    val priorityColor = when (task.priority) {
        "High" -> MaterialTheme.colorScheme.error
        "Medium" -> FieldMindTheme.colors.warning
        else -> FieldMindTheme.colors.positive
    }

    // ── Animated progress ──
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "taskProgress"
    )

    // ── Overflow menu ──
    var showOverflow by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ════════════════════════════════════════════════════════════
        //  Header: Back + title + overflow
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
                Box {
                    IconButton(onClick = { showOverflow = true }, modifier = Modifier.size(40.dp)) {
                        Icon(MaterialSymbolIcon("more_vert"), "Menu", size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit task") },
                            onClick = { showOverflow = false },
                            leadingIcon = { Icon(MaterialSymbolIcon("edit"), null, size = 18.dp) }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            onClick = { showOverflow = false },
                            leadingIcon = { Icon(MaterialSymbolIcon("content_copy"), null, size = 18.dp) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (task.status == "Done") "Reopen" else "Mark done") },
                            onClick = {
                                showOverflow = false
                                haptics.confirm()
                                viewModel.updateTaskEntity(task.copy(status = if (task.status == "Done") "Pending" else "Done"))
                            },
                            leadingIcon = { Icon(MaterialSymbolIcon("check_circle"), null, size = 18.dp) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },                                onClick = {
                                    showOverflow = false
                                    haptics.confirm()
                                    viewModel.deleteTask(task.id)
                                    onBack()
                                },
                            leadingIcon = { Icon(MaterialSymbolIcon("delete"), null, size = 18.dp, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Title + Priority badge + Due date
        // ════════════════════════════════════════════════════════════
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // ── Title ──
                Text(
                    task.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // ── Badge row: priority + due date + time ──
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "${task.priority} Priority",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp
                        )
                    }

                    // Due date badge
                    if (task.dueDate.isNotBlank()) {
                        val isOverdue = try {
                            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val due = fmt.parse(task.dueDate)
                            due != null && due.before(Date(System.currentTimeMillis() - 86400000L)) && task.status != "Done"
                        } catch (_: Exception) { false }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    MaterialSymbolIcon("calendar_today"),
                                    null,
                                    size = 12.dp,
                                    tint = if (isOverdue) MaterialTheme.colorScheme.error
                                           else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    buildString {
                                        append(formatFriendlyDate(task.dueDate))
                                        if (task.dueTime.isNotBlank()) append(" ${task.dueTime}")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOverdue) MaterialTheme.colorScheme.error
                                           else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Status badge
                    if (task.status == "Done") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FieldMindTheme.colors.positive.copy(alpha = 0.12f)
                        ) {
                            Text(
                                "Completed",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = FieldMindTheme.colors.positive,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Progress bar
        // ════════════════════════════════════════════════════════════
        if (task.status != "Done") {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Progress", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text("$progress%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = priorityColor)
                        }
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = priorityColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                        if (checklistItems.isNotEmpty()) {
                            Text(
                                "$doneCount of ${checklistItems.size} items completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Description
        // ════════════════════════════════════════════════════════════
        if (task.description.isNotBlank()) {
            item {
                SectionCard(
                    icon = MaterialSymbolIcon("description"),
                    title = "Description"
                ) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Checklist
        // ════════════════════════════════════════════════════════════
        if (checklistItems.isNotEmpty()) {
            item {
                SectionCard(
                    icon = MaterialSymbolIcon("checklist"),
                    title = "Checklist"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        checklistItems.forEach { item ->
                            Surface(
                                onClick = {
                                    haptics.light()
                                    // Toggle item
                                    val arr = try { JSONArray(task.checklistJson) } catch (_: Exception) { JSONArray() }
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.getJSONObject(i)
                                        if (obj.optString("text", "") == item.text) {
                                            obj.put("done", !item.done)
                                        }
                                    }
                                    viewModel.updateTaskEntity(task.copy(checklistJson = arr.toString()))
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Checkbox
                                    Surface(
                                        shape = CircleShape,
                                        color = if (item.done) FieldMindTheme.colors.positive.copy(alpha = 0.14f)
                                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (item.done) {
                                                Icon(MaterialSymbolIcon("check_circle", filled = true), "Done", size = 18.dp, tint = FieldMindTheme.colors.positive)
                                            } else {
                                                Box(
                                                    Modifier.size(16.dp).clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        item.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (item.done) FontWeight.Normal else FontWeight.Medium,
                                        textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (item.done) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Linked Records
        // ════════════════════════════════════════════════════════════
        if (linkedObs.isNotEmpty() || task.linkedQuestionId != null || task.linkedSpeciesId != null) {
            item {
                SectionCard(
                    icon = MaterialSymbolIcon("link"),
                    title = "Linked Records"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        linkedObs.forEach { obs ->
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
                                            .background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(FieldMindIcons.Observation, null, tint = FieldMindTheme.colors.observation, size = 16.dp)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("Observation #${obs.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(obs.subject, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(MaterialSymbolIcon("chevron_right"), null, size = 16.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Attachments
        // ════════════════════════════════════════════════════════════
        if (attachments.isNotEmpty()) {
            item {
                SectionCard(
                    icon = MaterialSymbolIcon("attach_file"),
                    title = "Attachments"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        attachments.forEach { uri ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val ext = uri.substringAfterLast(".").lowercase()
                                    val icon = when (ext) {
                                        "jpg", "jpeg", "png", "gif", "webp" -> MaterialSymbolIcon("image")
                                        "pdf" -> MaterialSymbolIcon("picture_as_pdf")
                                        "mp3", "wav", "m4a", "ogg" -> MaterialSymbolIcon("audio_file")
                                        "mp4", "mov" -> MaterialSymbolIcon("videocam")
                                        else -> MaterialSymbolIcon("attachment")
                                    }
                                    Box(
                                        Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                            .background(FieldMindTheme.colors.flashcard.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, null, tint = FieldMindTheme.colors.flashcard, size = 16.dp)
                                    }
                                    Text(
                                        uri.substringAfterLast("/").take(30),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Activity Log
        // ════════════════════════════════════════════════════════════
        item {
            SectionCard(
                icon = MaterialSymbolIcon("history"),
                title = "Activity Log"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActivityLogRow(
                        icon = MaterialSymbolIcon("add_circle"),
                        text = "Created",
                        date = task.createdAt
                    )
                    if (task.updatedAt > task.createdAt) {
                        ActivityLogRow(
                            icon = MaterialSymbolIcon("edit"),
                            text = "Edited",
                            date = task.updatedAt
                        )
                    }
                    if (task.status == "Done") {
                        ActivityLogRow(
                            icon = MaterialSymbolIcon("check_circle"),
                            text = "Completed",
                            date = task.updatedAt
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Reusable Components
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun SectionCard(
    icon: MaterialSymbolIcon,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, size = 18.dp, tint = FieldMindTheme.colors.flashcard)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun ActivityLogRow(icon: MaterialSymbolIcon, text: String, date: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, size = 14.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(
            formatTimestamp(date),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Helpers
// ══════════════════════════════════════════════════════════════════════

private fun formatFriendlyDate(dateStr: String): String {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = fmt.parse(dateStr) ?: return dateStr
        val now = Date()
        val diffMs = date.time - now.time
        val diffDays = (diffMs / 86400000L).toInt()
        when {
            diffDays < 0 -> "Overdue"
            diffDays == 0 -> "Today"
            diffDays == 1 -> "Tomorrow"
            diffDays <= 7 -> "In $diffDays days"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        }
    } catch (_: Exception) { dateStr }
}

private fun formatTimestamp(ts: Long): String {
    return try {
        val fmt = SimpleDateFormat("MMM dd", Locale.getDefault())
        fmt.format(Date(ts))
    } catch (_: Exception) { "" }
}
