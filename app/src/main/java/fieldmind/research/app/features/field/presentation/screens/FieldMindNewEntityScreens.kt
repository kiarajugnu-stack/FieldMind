package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

// ══════════════════════════════════════════════════════════════════════
//  NEW PROJECT SCREEN — Redesigned: name, description, icon, color, template
// ══════════════════════════════════════════════════════════════════════

@Composable
fun NewProjectScreen(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("🌿") }
    var selectedColor by remember { mutableStateOf(0xFF4CAF50L) }
    var selectedTemplate by remember { mutableStateOf("Empty Project") }
    var showTemplatePicker by remember { mutableStateOf(false) }

    val projectIcons = listOf("🌿", "🦋", "🐦", "🌲", "📷")
    val colorOptions = listOf(
        0xFF4CAF50L to Color(0xFF4CAF50L),  // Green
        0xFF2196F3L to Color(0xFF2196F3L),  // Blue
        0xFF9C27B0L to Color(0xFF9C27B0L),  // Purple
        0xFFFF9800L to Color(0xFFFF9800L),  // Orange
        0xFFF44336L to Color(0xFFF44336L)   // Red
    )
    val templates = listOf(
        "Empty Project",
        "Field Survey",
        "Species Observation",
        "Weather Log",
        "Site Monitoring",
        "Literature Review",
        "Experiment"
    )

    fun save() {
        if (title.isNotBlank()) {
            // Store icon in tags, color as hex in connectionMap, template as topicType
            val colorHex = "#%06X".format(selectedColor and 0xFFFFFF)
            viewModel.addProject(
                title = title,
                topic = selectedTemplate,
                objective = description,
                question = "",
                methods = "",
                nextAction = "",
                background = "",
                hypothesis = "",
                dataPlan = "",
                analysis = "",
                conclusion = "",
                projectType = selectedTemplate,
                selectedMethods = selectedIcon,
                connectionMap = colorHex
            )
            onBack()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        // ── Custom header: back button + title/subtitle ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f),
            tonalElevation = 0.dp
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(FieldMindIcons.Back, null, tint = MaterialTheme.colorScheme.onSurface, size = 22.dp)
                    }
                }
                Column {
                    Text("New Project", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Create a research workspace", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Project Name ──
            FieldTextField(
                title, { title = it },
                "Project Name",
                supportingText = "Short, descriptive name for your research"
            )

            // ── Description (Optional) ──
            FieldTextField(
                description, { description = it },
                "Description (Optional)",
                minLines = 3,
                supportingText = "What is this project about?"
            )

            // ── Project Icon ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Project Icon", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    projectIcons.forEach { icon ->
                        val isSelected = selectedIcon == icon
                        Surface(
                            onClick = { haptics.light(); selectedIcon = icon },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) FieldMindTheme.colors.project.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, FieldMindTheme.colors.project) else null,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(icon, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            }

            // ── Project Color ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Project Color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorOptions.forEach { (colorLong, color) ->
                        val isSelected = selectedColor == colorLong
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(16.dp))
                                    else Modifier
                                )
                                .clickable { haptics.light(); selectedColor = colorLong },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(MaterialSymbolIcon("check"), null, tint = Color.White, size = 24.dp)
                            }
                        }
                    }
                }
            }

            // ── Template Selector ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Template", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    onClick = { haptics.light(); showTemplatePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                    .background(FieldMindTheme.colors.project.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(FieldMindIcons.Project, null, tint = FieldMindTheme.colors.project, size = 22.dp)
                            }
                            Column {
                                Text(selectedTemplate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Pre-filled fields for $selectedTemplate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(MaterialSymbolIcon("chevron_right"), null, size = 20.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Create Button ──
            Button(
                onClick = ::save,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                enabled = title.isNotBlank()
            ) {
                Icon(FieldMindIcons.Project, null, size = 20.dp)
                Spacer(Modifier.size(8.dp))
                Text("Create", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // ── Template Picker Dialog ──
    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            icon = { Icon(FieldMindIcons.Project, null, size = 28.dp) },
            title = { Text("Choose a template") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pre-filled templates help you get started faster.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    templates.forEach { template ->
                        val isSelected = selectedTemplate == template
                        Surface(
                            onClick = { haptics.light(); selectedTemplate = template; showTemplatePicker = false },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) FieldMindTheme.colors.project.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(template, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                if (isSelected) {
                                    Icon(FieldMindIcons.Check, null, tint = FieldMindTheme.colors.project, size = 18.dp)
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTemplatePicker = false }) { Text("Cancel") }
            }
        )
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
    val context = LocalContext.current
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            attachmentUris = attachmentUris + it.toString()
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            attachmentUris = attachmentUris + it.toString()
        }
    }

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
                    Box {
                        TextButton(onClick = { haptics.light(); showAttachmentMenu = true }) {
                            Icon(MaterialSymbolIcon("attach_file"), null, size = 16.dp)
                            Spacer(Modifier.size(4.dp))
                            Text("Add file", style = MaterialTheme.typography.labelSmall)
                        }
                        DropdownMenu(
                            expanded = showAttachmentMenu,
                            onDismissRequest = { showAttachmentMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Photo from gallery") },
                                onClick = {
                                    showAttachmentMenu = false
                                    haptics.light()
                                    imagePicker.launch("image/*")
                                },
                                leadingIcon = { Icon(MaterialSymbolIcon("photo_library"), null, size = 18.dp) }
                            )
                            DropdownMenuItem(
                                text = { Text("Document / PDF") },
                                onClick = {
                                    showAttachmentMenu = false
                                    haptics.light()
                                    filePicker.launch(arrayOf(
                                        "application/pdf",
                                        "text/*",
                                        "application/msword",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "audio/*",
                                        "image/*"
                                    ))
                                },
                                leadingIcon = { Icon(MaterialSymbolIcon("description"), null, size = 18.dp) }
                            )
                        }
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
