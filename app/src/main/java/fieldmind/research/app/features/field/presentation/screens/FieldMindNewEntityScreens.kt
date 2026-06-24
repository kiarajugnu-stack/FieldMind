package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

// ══════════════════════════════════════════════════════════════════════
//  NEW PROJECT SCREEN — Full-screen creation form
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewProjectScreen(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("Biology") }
    var objective by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var background by remember { mutableStateOf("") }
    var methods by remember { mutableStateOf("") }
    var hypothesis by remember { mutableStateOf("") }
    var dataPlan by remember { mutableStateOf("") }
    var analysis by remember { mutableStateOf("") }
    var conclusion by remember { mutableStateOf("") }
    var nextAction by remember { mutableStateOf("") }
    var projectType by remember { mutableStateOf("Observation") }
    var selectedMethods by remember { mutableStateOf("") }
    var connectionMap by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }

    fun save() {
        if (title.isNotBlank()) {
            viewModel.addProject(title, topic, objective, question, methods, nextAction, background, hypothesis, dataPlan, analysis, conclusion, projectType = projectType, selectedMethods = selectedMethods, connectionMap = connectionMap)
            onBack()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        StandardScreenHeader(
            title = "New Project",
            subtitle = "Define the question, evidence plan, data fields, and report direction.",
            icon = FieldMindIcons.Project,
            heroColor = FieldMindTheme.colors.project,
            trailing = { BackButton(onClick = onBack) },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FieldTextField(title, { title = it }, "Project title")
            ChoiceChipsField("Topic", listOf("Biology", "Geology", "Wildlife", "Ecology", "Plant Study", "Weather", "Human Pattern", "Other"), topic) { topic = it }
            DividerSection("Research purpose", FieldMindIcons.School, FieldMindTheme.colors.project)
            FieldTextField(objective, { objective = it }, "Objective", minLines = 2)
            FieldTextField(question, { question = it }, "Research question", minLines = 2)
            FieldTextField(background, { background = it }, "Background / context", minLines = 2)
            DividerSection("Evidence plan", FieldMindIcons.Data, FieldMindTheme.colors.project)
            FieldTextField(methods, { methods = it }, "Method / data plan", minLines = 3)
            FieldTextField(hypothesis, { hypothesis = it }, "Hypothesis summary", minLines = 2)
            FieldTextField(dataPlan, { dataPlan = it }, "Data fields / units", minLines = 2, supportingText = "Example: temperature °C, height cm, water clarity, count")
            DividerSection("Report direction", FieldMindIcons.Report, FieldMindTheme.colors.project)
            FieldTextField(analysis, { analysis = it }, "Analysis plan", minLines = 2)
            FieldTextField(conclusion, { conclusion = it }, "Early conclusion / expected output", minLines = 2)
            FieldTextField(nextAction, { nextAction = it }, "Next action", supportingText = "Example: observe the same site at sunset for 3 days")
            CollapsibleSection("Advanced options", "Methods, connection map, and tags", expanded = showAdvanced, onToggle = { showAdvanced = !showAdvanced }) {
                FieldTextField(selectedMethods, { selectedMethods = it }, "Selected methods", supportingText = "e.g. transect, quadrat, interview, water test")
                FieldTextField(connectionMap, { connectionMap = it }, "Connection map", minLines = 2)
                FieldTextField(tags, { tags = it }, "Tags", supportingText = "Comma-separated tags")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), enabled = title.isNotBlank()) {
                Icon(FieldMindIcons.Check, null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Create project")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  NEW QUESTION SCREEN — Full-screen creation form
// ══════════════════════════════════════════════════════════════════════

@Composable
fun NewQuestionScreen(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    var question by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var source by remember { mutableStateOf("Observation") }
    var status by remember { mutableStateOf("New") }
    var priority by remember { mutableStateOf("Medium") }
    var answer by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }

    fun save() {
        if (question.isNotBlank()) {
            viewModel.addQuestion(question, category, source, status, priority, answer = answer)
            onBack()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        StandardScreenHeader(
            title = "New Question",
            subtitle = "Turn curiosity into something observable, measurable, comparable, or verifiable.",
            icon = FieldMindIcons.Question,
            heroColor = FieldMindTheme.colors.question,
            trailing = { BackButton(onClick = onBack) }
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FieldTextField(question, { question = it }, "What do you want to find out?", minLines = 3, supportingText = "Example: Do bird visits increase after rain at this site?")
            DividerSection("Classification", FieldMindIcons.Category)
            ChoiceChipsField("Category", observationCategories, category) { category = it }
            ChoiceChipsField("Source", sourceTypes, source) { source = it }
            ChoiceChipsField("Priority", listOf("Low", "Medium", "High"), priority) { priority = it }
            ChoiceChipsField("Status", questionStatuses, status) { status = it }
            CollapsibleSection("Advanced options", "Answer, cross-links, and metadata", expanded = showAdvanced, onToggle = { showAdvanced = !showAdvanced }) {
                FieldTextField(answer, { answer = it }, "Preliminary answer", minLines = 2, supportingText = "Optional — add if you already have a working answer")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), enabled = question.isNotBlank()) {
                Icon(FieldMindIcons.Check, null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Create question")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  NEW HYPOTHESIS SCREEN — Full-screen creation form
// ══════════════════════════════════════════════════════════════════════

@Composable
fun NewHypothesisScreen(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val questions by viewModel.questions.collectAsState()
    var prediction by remember { mutableStateOf("") }
    var reasoning by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    var support by remember { mutableStateOf("") }
    var weaken by remember { mutableStateOf("") }
    var test by remember { mutableStateOf("") }
    var confidence by remember { mutableStateOf(50f) }
    var linkedId by remember { mutableStateOf<Long?>(null) }
    var resultStatus by remember { mutableStateOf("Unknown") }
    var showAdvanced by remember { mutableStateOf(false) }

    fun save() {
        if (prediction.isNotBlank()) {
            viewModel.addHypothesis(linkedId, prediction, evidence, confidence.toInt(), reasoning, support, weaken, test, resultStatus = resultStatus)
            onBack()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        StandardScreenHeader(
            title = "New Hypothesis",
            subtitle = "State the prediction, what would support it, and what would weaken it.",
            icon = FieldMindIcons.Hypothesis,
            heroColor = FieldMindTheme.colors.hypothesis,
            trailing = { BackButton(onClick = onBack) },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (questions.isNotEmpty()) {
                ChoiceChipsField("Linked question", listOf("No question") + questions.take(8).map { it.questionText.take(28) }, questions.firstOrNull { it.id == linkedId }?.questionText?.take(28) ?: "No question") { picked ->
                    linkedId = questions.firstOrNull { it.questionText.startsWith(picked) }?.id
                }
            }
            FieldTextField(prediction, { prediction = it }, "Prediction", minLines = 3)
            FieldTextField(reasoning, { reasoning = it }, "Why this might happen", minLines = 2)
            DividerSection("Evidence rules", FieldMindIcons.Done, FieldMindTheme.colors.hypothesis)
            Text("Decide success/failure before you bias yourself.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FieldTextField(evidence, { evidence = it }, "Evidence needed", minLines = 2)
            FieldTextField(support, { support = it }, "Support criteria")
            FieldTextField(weaken, { weaken = it }, "Weakening criteria")
            FieldTextField(test, { test = it }, "Test method")
            DividerSection("Confidence", FieldMindIcons.Streak, FieldMindTheme.colors.hypothesis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Confidence", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${confidence.toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(confidence, { confidence = it }, valueRange = 0f..100f)
            LinearProgressIndicator(progress = { confidence / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = MaterialTheme.colorScheme.primary)
            CollapsibleSection("Advanced options", "Result status tracking", expanded = showAdvanced, onToggle = { showAdvanced = !showAdvanced }) {
                ChoiceChipsField("Result status", listOf("Unknown", "Supported", "Weakened", "Inconclusive"), resultStatus) { resultStatus = it }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), enabled = prediction.isNotBlank()) {
                Icon(FieldMindIcons.Check, null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Create hypothesis")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  NEW DATA RECORD SCREEN — Full-screen creation form
// ══════════════════════════════════════════════════════════════════════

@Composable
fun NewDataRecordScreen(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    var tool by remember { mutableStateOf("Counter") }
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("0") }
    var unit by remember { mutableStateOf(defaultUnitForTool("Counter")) }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    fun save() {
        if (label.isNotBlank()) {
            viewModel.addDataRecord(tool, label, value, unit, notes, location)
            onBack()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        StandardScreenHeader(
            title = "New Data Record",
            subtitle = "Choose a preset so units and labels match the kind of thing you measured.",
            icon = FieldMindIcons.Data,
            heroColor = FieldMindTheme.colors.data,
            trailing = { BackButton(onClick = onBack) },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DividerSection("Preset", FieldMindIcons.Settings, FieldMindTheme.colors.data)
            ChoiceChipsField("Tool", dataTools, tool) { tool = it; unit = defaultUnitForTool(it); label = defaultLabelForTool(it) }
            FieldTextField(label, { label = it }, "Label")
            if (tool == "Counter" || tool == "Species Tracker") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton({ value = ((value.toIntOrNull() ?: 0) - 1).toString() }) { Text("−1") }
                    Text(value, style = MaterialTheme.typography.headlineSmall)
                    Button({ value = ((value.toIntOrNull() ?: 0) + 1).toString() }) { Text("+1") }
                    TextButton({ value = "0" }) { Text("Reset") }
                }
            }
            DividerSection("Measurement", FieldMindIcons.Line, FieldMindTheme.colors.data)
            FieldTextField(value, { value = it }, "Value / items / samples", keyboardType = KeyboardType.Number)
            FieldTextField(unit, { unit = it }, "Unit", supportingText = "Suggested for $tool: ${defaultUnitForTool(tool)}")
            FieldTextField(location, { location = it }, "Location / site")
            DividerSection("Context", FieldMindIcons.Note, FieldMindTheme.colors.data)
            ChoiceChips(contextPresets, notes) { notes = if (notes.isBlank()) it else "$notes, $it" }
            FieldTextField(notes, { notes = it }, "Notes", minLines = 3)
            Spacer(Modifier.height(8.dp))
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), enabled = label.isNotBlank()) {
                Icon(FieldMindIcons.Check, null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Save record")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  NEW TASK SCREEN — Full-screen creation form (mockup v2)
// ══════════════════════════════════════════════════════════════════════

@Composable
fun NewTaskScreen(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    // ── Form state ──
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var projectId by remember { mutableStateOf<Long?>(null) }
    var dueDate by remember { mutableStateOf("") }
    var dueTime by remember { mutableStateOf("") }
    var reminder by remember { mutableStateOf(0) }
    var reminderUnit by remember { mutableStateOf("minute") }
    var repeatInterval by remember { mutableStateOf(0) }
    var repeatUnit by remember { mutableStateOf("") }
    var checklistItems by remember { mutableStateOf(listOf("")) }
    var attachmentUris by remember { mutableStateOf<List<String>>(emptyList()) }

    val projects by viewModel.projects.collectAsState()
    val haptics = rememberFieldMindHaptics()

    // ── Priority colors ──
    val priorityColor = mapOf(
        "Low" to FieldMindTheme.colors.positive,
        "Medium" to FieldMindTheme.colors.warning,
        "High" to MaterialTheme.colorScheme.error
    )

    fun save() {
        if (title.isNotBlank()) {
            val checklistArr = org.json.JSONArray()
            checklistItems.filter { it.isNotBlank() }.forEach { item ->
                checklistArr.put(org.json.JSONObject().apply {
                    put("text", item.trim())
                    put("done", false)
                })
            }
            viewModel.addTask(
                title = title,
                description = description,
                priority = priority,
                dueDate = dueDate,
                dueTime = dueTime,
                projectId = projectId,
                checklistJson = checklistArr.toString(),
                attachmentUris = attachmentUris.joinToString(","),
                reminder = reminder,
                reminderUnit = "minute",
                repeatInterval = repeatInterval,
                repeatUnit = repeatUnit
            )
            onBack()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        StandardScreenHeader(
            title = "New Task",
            subtitle = "Define a field task, survey, or to-do.",
            icon = MaterialSymbolIcon("checklist"),
            heroColor = FieldMindTheme.colors.flashcard,
            trailing = { BackButton(onClick = onBack) }
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── Task Name ──
            FieldTextField(title, { title = it }, "Task Name", supportingText = "Short, actionable title")

            // ── Description ──
            FieldTextField(description, { description = it }, "Description", minLines = 3, supportingText = "Details, context, or step-by-step instructions")

            // ── Priority (radio-style) ──
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
                                // Radio circle
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        .then(
                                            if (isSelected) Modifier
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(accent)
                                        )
                                    }
                                }
                                Text(level, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ── Project ──
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Project", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (projects.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        ChoiceChips(listOf("None") + projects.take(6).map { it.title.take(20) }, projects.firstOrNull { it.id == projectId }?.title?.take(20) ?: "None") { selected ->
                            projectId = projects.firstOrNull { it.title.startsWith(selected) }?.id
                        }
                    }
                } else {
                    Text("No projects yet. Create a project first.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // ── Due Date & Time ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldTextField(dueDate, { dueDate = it }, "Due Date", supportingText = "YYYY-MM-DD", modifier = Modifier.weight(1f))
                FieldTextField(dueTime, { dueTime = it }, "Due Time", supportingText = "HH:MM", modifier = Modifier.weight(1f))
            }

            // ── Reminder ──
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Reminder", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val reminderOptions = listOf("None" to 0, "5 min" to 5, "15 min" to 15, "30 min" to 30, "1 hour" to 60, "1 day" to 1440)
                    reminderOptions.forEach { (label, mins) ->
                        val isSelected = reminder == mins
                        Surface(
                            onClick = { haptics.light(); reminder = mins },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // ── Repeat ──
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Repeat", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val repeatOptions = listOf("None" to "" to 0, "Daily" to "day" to 1, "Weekly" to "week" to 1, "Monthly" to "month" to 1, "Yearly" to "year" to 1)
                    repeatOptions.forEach { (pair, interval) ->
                        val (label, unit) = pair
                        val isSelected = repeatUnit == unit
                        Surface(
                            onClick = { haptics.light(); repeatUnit = unit; repeatInterval = interval },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // ── Checklist ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Checklist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { haptics.light(); checklistItems = checklistItems + "" }) {
                        Icon(MaterialSymbolIcon("add"), null, size = 16.dp)
                        Spacer(Modifier.size(4.dp))
                        Text("Add item", style = MaterialTheme.typography.labelSmall)
                    }
                }
                checklistItems.forEachIndexed { index, item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = item,
                            onValueChange = { newVal ->
                                checklistItems = checklistItems.toMutableList().also { it[index] = newVal }
                            },
                            placeholder = { Text("Checklist item", style = MaterialTheme.typography.bodySmall) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        if (checklistItems.size > 1) {
                            IconButton(onClick = { haptics.light(); checklistItems = checklistItems.toMutableList().also { it.removeAt(index) } }, modifier = Modifier.size(32.dp)) {
                                Icon(MaterialSymbolIcon("close"), "Remove", size = 16.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            // ── Attachments ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Attachments", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { haptics.light() /* File picker would go here */ }) {
                        Icon(MaterialSymbolIcon("attach_file"), null, size = 16.dp)
                        Spacer(Modifier.size(4.dp))
                        Text("Add file", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (attachmentUris.isEmpty()) {
                    Text(
                        "No attachments yet. Tap \"Add file\" to attach images, PDFs, or audio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                } else {
                    attachmentUris.forEach { uri ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(MaterialSymbolIcon("attachment"), null, size = 16.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(uri.substringAfterLast("/").take(30), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { haptics.light(); attachmentUris = attachmentUris - uri }, modifier = Modifier.size(24.dp)) {
                                Icon(MaterialSymbolIcon("close"), "Remove", size = 14.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Save ──
            Button(
                onClick = ::save,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = title.isNotBlank()
            ) {
                Icon(FieldMindIcons.Check, null, size = 20.dp)
                Spacer(Modifier.size(8.dp))
                Text("Save", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  NEW REPORT SCREEN — Full-screen creation form
// ══════════════════════════════════════════════════════════════════════

@Composable
fun NewReportScreen(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    var type by remember { mutableStateOf("Field Report") }
    var title by remember { mutableStateOf("") }
    var background by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var methods by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }
    var results by remember { mutableStateOf("") }
    var interpretation by remember { mutableStateOf("") }
    var conclusion by remember { mutableStateOf("") }
    var limitations by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }

    fun save() {
        if (title.isNotBlank()) {
            viewModel.addReport(type, title, background, question, methods, observations, results, interpretation, conclusion, limitations, next)
            onBack()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        StandardScreenHeader(
            title = "Report Builder",
            subtitle = "Create a clean local draft: claim, evidence, reasoning, limitations, and next steps.",
            icon = FieldMindIcons.Report,
            heroColor = FieldMindTheme.colors.report,
            trailing = { BackButton(onClick = onBack) },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DividerSection("Type & title", FieldMindIcons.Category, FieldMindTheme.colors.report)
            ChoiceChipsField("Report type", reportTypes, type) { type = it }
            FieldTextField(title, { title = it }, "Title")
            DividerSection("Setup", FieldMindIcons.School, FieldMindTheme.colors.report)
            FieldTextField(background, { background = it }, "Background", minLines = 2)
            FieldTextField(question, { question = it }, "Question", minLines = 2)
            FieldTextField(methods, { methods = it }, "Methods", minLines = 2)
            DividerSection("Evidence", FieldMindIcons.Data, FieldMindTheme.colors.report)
            FieldTextField(observations, { observations = it }, "Observations", minLines = 2)
            FieldTextField(results, { results = it }, "Data / results", minLines = 2)
            FieldTextField(interpretation, { interpretation = it }, "Interpretation", minLines = 2)
            DividerSection("Conclusion", FieldMindIcons.Check, FieldMindTheme.colors.report)
            FieldTextField(conclusion, { conclusion = it }, "Conclusion", minLines = 2)
            FieldTextField(limitations, { limitations = it }, "Limitations", minLines = 2)
            FieldTextField(next, { next = it }, "Next steps", minLines = 2)
            Spacer(Modifier.height(8.dp))
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), enabled = title.isNotBlank()) {
                Icon(FieldMindIcons.Check, null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Build report")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Shared helpers
// ══════════════════════════════════════════════════════════════════════

private fun defaultUnitForTool(tool: String): String = when (tool) {
    "Weather Log" -> "°C"
    "Measurement Log" -> "cm"
    "Counter", "Species Tracker" -> "count"
    "Event Log" -> "event"
    "Site Log" -> "site"
    "Checklist" -> "done/total"
    "Comparison Table" -> "score"
    else -> ""
}

private fun defaultLabelForTool(tool: String): String = when (tool) {
    "Weather Log" -> "Air temperature"
    "Measurement Log" -> "Measured length"
    "Species Tracker" -> "Species count"
    "Checklist" -> "Checklist item"
    "Event Log" -> "Observed event"
    "Site Log" -> "Site condition"
    "Comparison Table" -> "Comparison variable"
    else -> ""
}

@Composable
private fun ChoiceChipsField(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChoiceChips(options, selected, onSelected = onSelected)
    }
}

@Composable
private fun DividerSection(title: String, icon: MaterialSymbolIcon? = null, accent: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) { Icon(icon, null, tint = accent, size = 18.dp) }
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
}
