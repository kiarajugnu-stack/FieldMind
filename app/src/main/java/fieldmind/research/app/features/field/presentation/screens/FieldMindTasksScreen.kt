package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import fieldmind.research.app.features.field.data.database.entity.TaskEntity
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  TASKS SCREEN — Full task management with sections and filtering
// ══════════════════════════════════════════════════════════════════════

@Composable
fun TasksScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit = {},
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    onNavigate: (String) -> Unit = {}
) {
    val tasks by viewModel.tasks.collectAsState()
    val haptics = rememberFieldMindHaptics()

    // Track checked-off tasks locally for optimistic UI
    val completedTaskIds = remember { mutableStateMapOf<Long, Boolean>() }

    // ── Compute sections ──
    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis()))
    }

    val todayTasks = remember(tasks, completedTaskIds) {
        tasks.filter { t ->
            t.dueDate == todayDate && t.status != "Done" && completedTaskIds[t.id] != true
        }.sortedBy { it.priority }
    }

    val upcomingTasks = remember(tasks, completedTaskIds) {
        tasks.filter { t ->
            t.dueDate.isNotBlank() && t.dueDate != todayDate && t.status != "Done" && completedTaskIds[t.id] != true
        }.sortedBy { it.dueDate }
    }

    val doneTasks = remember(tasks, completedTaskIds) {
        tasks.filter { t ->
            t.status == "Done" || completedTaskIds[t.id] == true
        }.sortedByDescending { it.updatedAt }
    }

    // ── Section expand state ──
    var expandedToday by remember { mutableStateOf(true) }
    var expandedUpcoming by remember { mutableStateOf(true) }
    var expandedDone by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header ──
        item {
            StandardScreenHeader(
                title = "Tasks",
                subtitle = "Track field tasks, surveys, and to-dos.",
                icon = MaterialSymbolIcon("checklist"),
                heroColor = FieldMindTheme.colors.flashcard,
                trailing = {
                    IconButton(onClick = { onNavigate("field_new_task") }, modifier = Modifier.size(40.dp)) {
                        Icon(MaterialSymbolIcon("add"), "Add task", size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        // ── Stats row ──
        if (tasks.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        value = "${todayTasks.size + upcomingTasks.size}",
                        label = "Pending",
                        icon = MaterialSymbolIcon("pending_actions"),
                        color = FieldMindTheme.colors.flashcard,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${doneTasks.size}",
                        label = "Completed",
                        icon = MaterialSymbolIcon("check_circle"),
                        color = FieldMindTheme.colors.positive,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ════════════════════════════════════════════════════════
        //  TODAY section
        // ════════════════════════════════════════════════════════
        item {
            TaskSectionHeader(
                title = "Today",
                icon = MaterialSymbolIcon("today"),
                count = todayTasks.size,
                accentColor = FieldMindTheme.colors.flashcard,
                expanded = expandedToday,
                onToggle = { expandedToday = !expandedToday }
            )
        }

        if (expandedToday) {
            if (todayTasks.isEmpty()) {
                item {
                    EmptyTaskHint("No tasks due today. Tap + to add one.")
                }
            } else {
                items(todayTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        isChecked = false,
                        accentColor = FieldMindTheme.colors.flashcard,
                        onToggle = {
                            haptics.light()
                            completedTaskIds[task.id] = true
                            viewModel.updateTaskEntity(task.copy(status = "Done"))
                        }
                    )
                }
            }
        }

        // ════════════════════════════════════════════════════════
        //  UPCOMING section
        // ════════════════════════════════════════════════════════
        item {
            TaskSectionHeader(
                title = "Upcoming",
                icon = MaterialSymbolIcon("calendar_month"),
                count = upcomingTasks.size,
                accentColor = FieldMindTheme.colors.observation,
                expanded = expandedUpcoming,
                onToggle = { expandedUpcoming = !expandedUpcoming }
            )
        }

        if (expandedUpcoming) {
            if (upcomingTasks.isEmpty()) {
                item {
                    EmptyTaskHint("No upcoming tasks.")
                }
            } else {
                items(upcomingTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        isChecked = false,
                        accentColor = FieldMindTheme.colors.observation,
                        onToggle = {
                            haptics.light()
                            completedTaskIds[task.id] = true
                            viewModel.updateTaskEntity(task.copy(status = "Done"))
                        }
                    )
                }
            }
        }

        // ════════════════════════════════════════════════════════
        //  DONE section
        // ════════════════════════════════════════════════════════
        item {
            TaskSectionHeader(
                title = "Done",
                icon = MaterialSymbolIcon("check_circle"),
                count = doneTasks.size,
                accentColor = FieldMindTheme.colors.positive,
                expanded = expandedDone,
                onToggle = { expandedDone = !expandedDone }
            )
        }

        if (expandedDone) {
            if (doneTasks.isEmpty()) {
                item {
                    EmptyTaskHint("No completed tasks yet.")
                }
            } else {
                items(doneTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        isChecked = true,
                        accentColor = FieldMindTheme.colors.positive,
                        onToggle = {
                            haptics.light()
                            completedTaskIds.remove(task.id)
                            viewModel.updateTaskEntity(task.copy(status = "Pending"))
                        }
                    )
                }
            }
        }
    }

}

// ══════════════════════════════════════════════════════════════════════
//  TASK SECTION HEADER — Collapsible with count badge
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun TaskSectionHeader(
    title: String,
    icon: MaterialSymbolIcon,
    count: Int,
    accentColor: Color,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, size = 18.dp)
            }

            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            // Count badge
            if (count > 0) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Icon(
                if (expanded) MaterialSymbolIcon("expand_less") else MaterialSymbolIcon("expand_more"),
                if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 20.dp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  TASK CARD — Checkable task item with priority badge and due date
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun TaskCard(
    task: TaskEntity,
    isChecked: Boolean,
    accentColor: Color,
    onToggle: () -> Unit
) {
    val priorityColor = when (task.priority) {
        "High" -> MaterialTheme.colorScheme.error
        "Medium" -> FieldMindTheme.colors.warning
        "Low" -> FieldMindTheme.colors.positive
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val animatedCheck by animateFloatAsState(
        targetValue = if (isChecked) 1f else 0f,
        animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "taskCheck"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked)
                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Checkbox ──
            Surface(
                onClick = onToggle,
                shape = CircleShape,
                color = if (isChecked)
                    accentColor.copy(alpha = 0.14f)
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isChecked) {
                        Icon(
                            MaterialSymbolIcon("check_circle", filled = true),
                            "Toggle done",
                            size = 22.dp,
                            tint = accentColor
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            // ── Task content ──
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isChecked)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                // Description snippet
                if (task.description.isNotBlank() && !isChecked) {
                    Text(
                        task.description.take(80),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Meta row: priority + due date + type
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            task.priority,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp
                        )
                    }

                    // Due date
                    if (task.dueDate.isNotBlank()) {
                        val isOverdue = try {
                            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val due = fmt.parse(task.dueDate)
                            due != null && due.before(Date(System.currentTimeMillis() - 86400000L)) && !isChecked
                        } catch (_: Exception) { false }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                MaterialSymbolIcon("calendar_today"),
                                null,
                                size = 10.dp,
                                tint = if (isOverdue) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                formatDueDate(task.dueDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Task type
                    if (task.taskType.isNotBlank() && !isChecked) {
                        Text(
                            task.taskType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  EMPTY STATE HINT
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyTaskHint(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  STAT CARD (reused from QuestionsScreen pattern)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun StatCard(value: String, label: String, icon: MaterialSymbolIcon, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, size = 16.dp)
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  HELPERS
// ══════════════════════════════════════════════════════════════════════

private fun formatDueDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
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
    } catch (_: Exception) {
        dateStr
    }
}
