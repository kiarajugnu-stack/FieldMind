package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.QuestionEntity
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  Questions Screen — Dedicated question management with auto-builder
// ══════════════════════════════════════════════════════════════════════

@Composable
fun QuestionsScreen(
    viewModel: FieldMindViewModel,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    val questions by viewModel.questions.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val haptics = rememberFieldMindHaptics()

    var showBuilder by remember { mutableStateOf(false) }
    var questionText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var selectedSourceType by remember { mutableStateOf("Thought") }
    var priority by remember { mutableStateOf("Medium") }
    var sourceNotes by remember { mutableStateOf("") }

    // Auto-builder states
    var autoBuild by remember { mutableStateOf(false) }
    var autoQuestion by remember { mutableStateOf("") }
    var autoContext by remember { mutableStateOf("") }
    var suggestedCauses by remember { mutableStateOf(listOf<String>()) }
    var suggestedPredictions by remember { mutableStateOf(listOf<String>()) }

    // Filter
    var filterStatus by remember { mutableStateOf("All") }
    var filterCategory by remember { mutableStateOf("All") }
    val categories = remember(questions) { listOf("All") + questions.map { it.category }.distinct().sorted() }
    val statuses = listOf("All", "Open", "Answered", "Investigating", "Archived")

    val filtered = remember(questions, filterStatus, filterCategory) {
        questions.filter { q ->
            (filterStatus == "All" || q.status == filterStatus) &&
            (filterCategory == "All" || q.category == filterCategory)
        }
    }

    // Stats
    val totalCount = questions.size
    val answeredCount = questions.count { it.status == "Answered" }
    val openCount = questions.count { it.status == "Open" || it.status == "Investigating" }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { FieldScreenHeader("Questions", "All research questions, auto-generated from observations, and your evidence tracking.", icon = FieldMindIcons.Question) }

        // Stats row
        if (totalCount > 0) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("${totalCount}", "Total", FieldMindIcons.Question, FieldMindTheme.colors.observation, Modifier.weight(1f))
                    StatCard("${openCount}", "Open", FieldMindIcons.Question, FieldMindTheme.colors.flashcard, Modifier.weight(1f))
                    StatCard("${answeredCount}", "Answered", FieldMindIcons.Question, FieldMindTheme.colors.positive, Modifier.weight(1f))
                }
            }
        }

        // Auto-build suggestion
        item {
            var showAuto by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { showAuto = !showAuto },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(FieldMindIcons.Sparkle, null, tint = FieldMindTheme.colors.observation, size = 22.dp) }
                        Column(Modifier.weight(1f)) {
                            Text("Auto-builder", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Generate questions from your observations and gaps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (showAuto) FieldMindIcons.Up else FieldMindIcons.Down, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                    }
                    AnimatedVisibility(showAuto) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Step 1: Enter topic or let it detect from recent observations
                            FieldTextField(autoQuestion, { autoQuestion = it }, "What are you curious about? (e.g., \"bird activity at dawn\")", minLines = 2)

                            if (autoQuestion.isNotBlank()) {
                                // Step 2: Auto-suggested causes based on text
                                suggestedCauses = listOf(
                                    "Time of day / seasonality",
                                    "Environmental conditions",
                                    "Presence of food/water sources",
                                    "Human activity / disturbance"
                                )
                                suggestedPredictions = listOf(
                                    "It changes with temperature",
                                    "It varies by location type",
                                    "It correlates with specific conditions",
                                    "Pattern repeats at regular intervals"
                                )

                                Text("Possible causes to investigate:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                SuggestionChips(suggestedCauses) { cause ->
                                    if (!autoContext.contains(cause)) {
                                        autoContext = if (autoContext.isBlank()) cause else "$autoContext, $cause"
                                    }
                                }

                                Text("Testable predictions:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                SuggestionChips(suggestedPredictions) { prediction ->
                                    sourceNotes = prediction
                                }
                            }

                            FieldTextField(autoContext, { autoContext = it }, "Context / observations that led to this question", minLines = 2)
                            FieldTextField(sourceNotes, { sourceNotes = it }, "Testable prediction or hypothesis", minLines = 1)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ChoiceChips(listOf("Thought", "Observation", "Reading", "Data gap", "Discussion"), selectedSourceType) { selectedSourceType = it }
                            }

                            Button(
                                onClick = {
                                    haptics.confirm()
                                    val question = autoQuestion.ifBlank { "Investigate: ${autoContext.take(80)}" }
                                    viewModel.addQuestion(question, category, selectedSourceType, "Open", priority)
                                    autoQuestion = ""
                                    autoContext = ""
                                    sourceNotes = ""
                                    showAuto = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                enabled = autoQuestion.isNotBlank() || autoContext.isNotBlank()
                            ) { Text("Create research question") }
                        }
                    }
                }
            }
        }

        // Quick add
        item {
            AddButton(if (showBuilder) "Cancel" else "Quick question") {
                showBuilder = !showBuilder
                if (!showBuilder) { questionText = ""; category = "Other" }
            }
        }
        if (showBuilder) item {
            InlineFormCard("Add Question", onDismiss = { showBuilder = false; questionText = "" }, onSave = {
                if (questionText.isNotBlank()) {                                viewModel.addQuestion(questionText, category, selectedSourceType, "Open", priority)
                    showBuilder = false; questionText = ""; sourceNotes = ""
                }
            }, saveEnabled = questionText.isNotBlank()) {
                FieldTextField(questionText, { questionText = it }, "Write your question", minLines = 3)
                ChoiceChips(listOf("Other", "Ecology", "Behavior", "Climate", "Site", "Method", "Taxonomy"), category) { category = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChips(listOf("Low", "Medium", "High", "Critical"), priority) { priority = it }
                }
            }
        }

        // Filters
        if (categories.size > 1) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(FieldMindIcons.Filter, null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                            Text("Filters", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilterChipsRow(statuses, filterStatus) { filterStatus = it }
                        Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilterChipsRow(categories, filterCategory) { filterCategory = it }
                    }
                }
            }
        }

        // Empty state
        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    if (questions.isEmpty()) "No questions yet" else "No matching questions",
                    if (questions.isEmpty()) "Questions drive your research. Build questions from observations, data gaps, and curiosity. Use the auto-builder above to generate structured questions." else "Try changing the filters.",
                    icon = FieldMindIcons.Question
                )
            }
        }

        // Question cards
        items(filtered, key = { it.id }) { q ->
            QuestionCard(q, observations.count { obs -> obs.category == q.category }, viewModel, onOpenDetail)
        }

        // Evidence correlation note
        if (questions.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(FieldMindIcons.Insights, null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                            Text("Quick evidence correlation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Text("Questions with observations in matching categories: ${questions.filter { q -> observations.any { it.category == q.category } }.size}/${questions.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tip: Link each question to observations by using matching categories. This enables pattern detection across entries.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: QuestionEntity,
    matchingObsCount: Int,
    viewModel: FieldMindViewModel,
    onOpenDetail: (String, Long) -> Unit
) {
    var expanded by remember(question.id) { mutableStateOf(false) }
    val accent = when (question.status) {
        "Answered" -> FieldMindTheme.colors.positive
        "Open" -> FieldMindTheme.colors.flashcard
        "Investigating" -> FieldMindTheme.colors.data
        "Archived" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(FieldMindIcons.Question, null, tint = accent, size = 20.dp) }

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        question.questionText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(question.status, icon = FieldMindIcons.Check)
                        InfoChip(question.priority, icon = FieldMindIcons.Alert)
                        InfoChip(question.category)
                    }
                    if (question.answer.isNotBlank()) {
                        Text(
                            "Answer: ${question.answer}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (expanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(if (expanded) FieldMindIcons.Up else FieldMindIcons.Down, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
            }

            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Source: ${question.sourceType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Created: ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(question.createdAt))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (matchingObsCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(FieldMindIcons.Observation, null, tint = FieldMindTheme.colors.observation, size = 14.dp)
                            Text("$matchingObsCount related observation${if (matchingObsCount != 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = FieldMindTheme.colors.observation)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.updateQuestionEntity(question.copy(
                                    status = if (question.status == "Answered") "Open" else "Answered",
                                    answeredAt = if (question.status == "Answered") null else System.currentTimeMillis()
                                ))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (question.status == "Answered") "Reopen" else "Mark answered")
                        }
                        OutlinedButton(
                            onClick = { onOpenDetail("question", question.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Detail")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowChips(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.take(4).forEach { item ->
            FilterChip(
                selected = selected == item,
                onClick = { onSelect(item) },
                label = { Text(item, fontSize = 10.sp) }
            )
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, icon: MaterialSymbolIcon, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
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

private val FlowChips = @Composable { items: List<String>, onSelect: (String) -> Unit ->
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(items) { item ->
            FilterChip(
                selected = false,
                onClick = { onSelect(item) },
                label = { Text(item, fontSize = 10.sp) }
            )
        }
    }
}
