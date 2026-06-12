package chromahub.rhythm.app.features.field.presentation.screens

import android.app.KeyguardManager
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import chromahub.rhythm.app.features.field.data.database.entity.*
import chromahub.rhythm.app.features.field.presentation.components.*
import chromahub.rhythm.app.features.field.presentation.theme.FieldMindTheme
import chromahub.rhythm.app.features.field.presentation.viewmodel.DraftEvidenceAttachment
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import androidx.compose.animation.animateContentSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.Manifest
import androidx.compose.ui.platform.LocalUriHandler
import chromahub.rhythm.app.features.field.data.location.FieldLocationProvider
// ══════════════════════════════════════════════════════════════════════
//  Settings rows + dialogs + helpers
// ══════════════════════════════════════════════════════════════════════

/** A grouped settings section: a header above a single rounded card holding related rows. */
@Composable
internal fun NewQuestionDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var question by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Other") }; var source by remember { mutableStateOf("Observation") }; var status by remember { mutableStateOf("New") }; var priority by remember { mutableStateOf("Medium") }
    FormDialog("New Question", onDismiss, { if (question.isNotBlank()) { viewModel.addQuestion(question, category, source, status, priority); onDismiss() } }) {
        SourceFormHero("New research question", "Turn curiosity into something observable, measurable, comparable, or verifiable.")
        CaptureStep("Question", "Use one clear sentence and avoid assuming the answer.", FieldMindIcons.Question) {
            FieldTextField(question, { question = it }, "What do you want to find out?", minLines = 3, supportingText = "Example: Do bird visits increase after rain at this site?")
        }
        CaptureStep("Classify", "Presets keep searching, charts, and reports cleaner.", FieldMindIcons.Category) {
            ChoiceChips(observationCategories, category) { category = it }
            Text("Source", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(sourceTypes, source) { source = it }
            Text("Priority", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(listOf("Low", "Medium", "High"), priority) { priority = it }
            Text("Status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(questionStatuses, status) { status = it }
        }
    }
}

@Composable
internal fun NewProjectDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }; var topic by remember { mutableStateOf("Biology") }; var objective by remember { mutableStateOf("") }; var question by remember { mutableStateOf("") }
    var background by remember { mutableStateOf("") }; var methods by remember { mutableStateOf("") }; var hypothesis by remember { mutableStateOf("") }; var dataPlan by remember { mutableStateOf("") }; var analysis by remember { mutableStateOf("") }; var conclusion by remember { mutableStateOf("") }; var nextAction by remember { mutableStateOf("") }
    FormDialog("New Project", onDismiss, {
        if (title.isNotBlank()) {
            viewModel.addProject(title, topic, objective, question, methods, nextAction, background, hypothesis, dataPlan, analysis, conclusion)
            onDismiss()
        }
    }) {
        SourceFormHero("Build a project workspace", "Define the question, evidence plan, data fields, and report direction before collecting more data.")
        CaptureStep("Topic & title", "Name the project and choose the broad research area.", FieldMindIcons.Project) {
            FieldTextField(title, { title = it }, "Project title")
            ChoiceChips(listOf("Biology", "Geology", "Wildlife", "Ecology", "Plant Study", "Weather", "Human Pattern", "Other"), topic) { topic = it }
        }
        CaptureStep("Research purpose", "Write a concrete objective and a question that can guide observations.", FieldMindIcons.Question) {
            FieldTextField(objective, { objective = it }, "Objective", minLines = 2)
            FieldTextField(question, { question = it }, "Research question", minLines = 2)
            FieldTextField(background, { background = it }, "Background / context", minLines = 2)
        }
        CaptureStep("Evidence plan", "Add the planned method, hypothesis, and category-specific data fields.", FieldMindIcons.Data) {
            FieldTextField(methods, { methods = it }, "Method / data plan", minLines = 3)
            FieldTextField(hypothesis, { hypothesis = it }, "Hypothesis summary", minLines = 2)
            FieldTextField(dataPlan, { dataPlan = it }, "Data fields / units", supportingText = "Example: temperature °C, height cm, water clarity, count")
        }
        CaptureStep("Report direction", "Keep analysis, conclusion, and next action visible without cluttering the card.", FieldMindIcons.Report) {
            FieldTextField(analysis, { analysis = it }, "Analysis plan", minLines = 2)
            FieldTextField(conclusion, { conclusion = it }, "Early conclusion / expected output", minLines = 2)
            FieldTextField(nextAction, { nextAction = it }, "Next action", supportingText = "Example: observe the same site at sunset for 3 days")
        }
    }
}

@Composable
private fun SourceFormHero(title: String, body: String) {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(FieldMindIcons.Source, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 26.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
            }
        }
    }
}

@Composable
internal fun SourcePreviewCard(link: String, fileUri: String, modifier: Modifier = Modifier) {
    val trimmedLink = link.trim()
    val videoId = remember(trimmedLink) { youtubeVideoId(trimmedLink) }
    if (trimmedLink.isBlank() && fileUri.isBlank()) return
    Card(modifier = modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon = if (videoId != null) FieldMindIcons.Play else if (fileUri.isNotBlank()) FieldMindIcons.File else FieldMindIcons.OpenLink, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                Column(Modifier.weight(1f)) {
                    Text(if (videoId != null) "YouTube preview" else if (fileUri.isNotBlank()) "Local file reference" else "Link preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(if (videoId != null) "youtube.com/embed/$videoId" else fileUri.ifBlank { trimmedLink }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            videoId?.let { id ->
                AsyncImage(
                    model = "https://img.youtube.com/vi/$id/hqdefault.jpg",
                    contentDescription = "YouTube thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            }
            if (trimmedLink.isNotBlank() && videoId == null) {
                InfoChip(trimmedLink.substringAfter("://", trimmedLink).substringBefore('/'), icon = FieldMindIcons.OpenLink)
            }
            if (fileUri.isNotBlank()) InfoChip(fileUri.substringAfterLast('/').take(48).ifBlank { "Attached file" }, icon = FieldMindIcons.File)
        }
    }
}


@Composable
internal fun NewSourceDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    val haptics = rememberFieldMindHaptics()
    var type by remember { mutableStateOf("Article") }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var dateOrYear by remember { mutableStateOf("") }
    var doiOrIsbn by remember { mutableStateOf("") }
    var publisherOrJournal by remember { mutableStateOf("") }
    var accessDate by remember { mutableStateOf(today()) }
    var link by remember { mutableStateOf("") }
    var fileUri by remember { mutableStateOf("") }
    var citationStyleNote by remember { mutableStateOf("") }
    var importance by remember { mutableStateOf("Normal") }
    var readingStatus by remember { mutableStateOf("In progress") }
    var summary by remember { mutableStateOf("") }
    var taught by remember { mutableStateOf("") }
    var findings by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var reliability by remember { mutableStateOf(3f) }
    var projectId by remember { mutableStateOf<Long?>(null) }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            fileUri = uri.toString()
            if (type !in listOf("PDF", "Image")) type = "Local document"
            haptics.light()
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fileUri = uri.toString()
            type = "Image"
            haptics.light()
        }
    }
    FormDialog("Add Source", onDismiss, {
        if (title.isNotBlank()) {
            viewModel.addSource(
                type = type,
                title = title,
                author = author,
                link = link,
                summary = summary,
                taught = taught,
                reliability = reliability.toInt(),
                keyFindings = findings,
                questionsGenerated = questions,
                paperNotes = notes,
                projectId = projectId,
                dateOrYear = dateOrYear,
                doiOrIsbn = doiOrIsbn,
                publisherOrJournal = publisherOrJournal,
                accessDate = accessDate,
                fileUri = fileUri,
                citationStyleNote = citationStyleNote,
                importance = importance,
                readingStatus = readingStatus
            )
            onDismiss()
        }
    }) {
        SourceFormHero("Add a research source", "Save citation details, files, links, reading notes, and project context in one place.")
        CaptureStep("Source type", "Choose what kind of material this is.", FieldMindIcons.Source) {
            ChoiceChips(sourceLibraryTypes, type) { type = it }
        }
        CaptureStep("Identity", "Capture citation identifiers now so export stays useful later.", FieldMindIcons.Article) {
            FieldTextField(title, { title = it }, "Title")
            FieldTextField(author, { author = it }, "Author / creator")
            FieldTextField(doiOrIsbn, { doiOrIsbn = it }, "DOI / ISBN", supportingText = "Crossref DOI or Open Library ISBN metadata can be checked later.")
            FieldTextField(publisherOrJournal, { publisherOrJournal = it }, "Publisher / journal")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldTextField(dateOrYear, { dateOrYear = it }, "Date / year", modifier = Modifier.weight(1f))
                FieldTextField(accessDate, { accessDate = it }, "Accessed", modifier = Modifier.weight(1f))
            }
        }
        CaptureStep("Link or file", "Attach PDFs, images, local documents, or a web link.", FieldMindIcons.Link) {
            FieldTextField(link, { link = it }, "Web link", supportingText = "Supports normal links and YouTube preview cards.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { haptics.light(); docPicker.launch(arrayOf("application/pdf", "text/*", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "image/*")) }, modifier = Modifier.weight(1f)) {
                    Icon(FieldMindIcons.File, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Document")
                }
                OutlinedButton(onClick = { haptics.light(); imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Icon(FieldMindIcons.Gallery, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Image")
                }
            }
            if (fileUri.isNotBlank()) FieldTextField(fileUri, { fileUri = it }, "Attached file URI")
            SourcePreviewCard(link = link, fileUri = fileUri)
        }
        CaptureStep("Reading notes", "Use Cornell-style cues: main idea, evidence, questions, and takeaways.", FieldMindIcons.Edit) {
            FieldTextField(summary, { summary = it }, "Main idea", minLines = 2)
            FieldTextField(findings, { findings = it }, "Key findings / definitions", minLines = 2)
            FieldTextField(taught, { taught = it }, "What this source taught me", minLines = 2)
            FieldTextField(questions, { questions = it }, "New questions / cue column", minLines = 2)
            FieldTextField(notes, { notes = it }, "Paper / Cornell notes", minLines = 4)
            FieldTextField(citationStyleNote, { citationStyleNote = it }, "Citation style note", supportingText = "APA/MLA/manual citation reminder.")
        }
        CaptureStep("Review & project", "Mark priority, reading state, credibility, and where it belongs.", FieldMindIcons.Check) {
            Text("Reading status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(readingStatuses, readingStatus) { readingStatus = it }
            Text("Importance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(sourceImportanceLevels, importance) { importance = it }
            Text("Credibility: ${reliability.toInt()}/5")
            Slider(reliability, { reliability = it }, valueRange = 1f..5f, steps = 3)
            if (projects.isNotEmpty()) {
                Text("Link to project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChoiceChips(listOf("No project") + projects.map { it.title }, projects.firstOrNull { it.id == projectId }?.title ?: "No project") { selected -> projectId = projects.firstOrNull { it.title == selected }?.id }
            }
        }
    }
}

@Composable
internal fun NewHypothesisDialog(viewModel: FieldMindViewModel, questions: List<QuestionEntity>, onDismiss: () -> Unit) {
    var prediction by remember { mutableStateOf("") }; var reasoning by remember { mutableStateOf("") }; var evidence by remember { mutableStateOf("") }; var support by remember { mutableStateOf("") }; var weaken by remember { mutableStateOf("") }; var test by remember { mutableStateOf("") }; var confidence by remember { mutableStateOf(50f) }; var linkedId by remember { mutableStateOf(questions.firstOrNull()?.id) }
    FormDialog("New Hypothesis", onDismiss, { if (prediction.isNotBlank()) { viewModel.addHypothesis(linkedId, prediction, evidence, confidence.toInt(), reasoning, support, weaken, test); onDismiss() } }) {
        SourceFormHero("Build a testable hypothesis", "State the prediction, what would support it, and what would weaken it before collecting results.")
        CaptureStep("Link & prediction", "Connect it to a question if one exists.", FieldMindIcons.Hypothesis) {
            if (questions.isNotEmpty()) ChoiceChips(listOf("No question") + questions.take(8).map { it.questionText.take(28) }, questions.firstOrNull { it.id == linkedId }?.questionText?.take(28) ?: "No question") { picked -> linkedId = questions.firstOrNull { it.questionText.startsWith(picked) }?.id }
            FieldTextField(prediction, { prediction = it }, "Prediction", minLines = 3)
            FieldTextField(reasoning, { reasoning = it }, "Why this might happen", minLines = 2)
        }
        CaptureStep("Evidence rules", "Decide success/failure before you bias yourself.", FieldMindIcons.Check) {
            FieldTextField(evidence, { evidence = it }, "Evidence needed", minLines = 2)
            FieldTextField(support, { support = it }, "Support criteria")
            FieldTextField(weaken, { weaken = it }, "Weakening criteria")
            FieldTextField(test, { test = it }, "Test method")
            Text("Confidence: ${confidence.toInt()}%")
            Slider(confidence, { confidence = it }, valueRange = 0f..100f)
        }
    }
}


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

private fun valueLabelForTool(tool: String): String = when (tool) {
    "Weather Log" -> "Temperature / humidity / wind value"
    "Measurement Log" -> "Measurement value"
    "Checklist" -> "Done / total"
    "Event Log" -> "Event count or time"
    else -> "Value / items / samples"
}

private fun notesLabelForTool(tool: String): String = when (tool) {
    "Weather Log" -> "Sky, wind, precipitation, pressure notes"
    "Measurement Log" -> "Method, instrument, calibration notes"
    "Species Tracker" -> "Species, behavior, confidence, evidence"
    "Site Log" -> "Habitat, substrate, disturbance, access notes"
    else -> "Notes"
}

@Composable
internal fun NewDataRecordDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var tool by remember { mutableStateOf("Counter") }; var label by remember { mutableStateOf("") }; var value by remember { mutableStateOf("0") }; var unit by remember { mutableStateOf(defaultUnitForTool("Counter")) }; var location by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    FormDialog("Data Collection Tool", onDismiss, { if (label.isNotBlank()) { viewModel.addDataRecord(tool, label, value, unit, notes, location); onDismiss() } }) {
        SourceFormHero("Data entry", "Choose a preset so units and labels match the kind of thing you measured.")
        CaptureStep("Preset", "Each tool adapts labels and units for the category.", FieldMindIcons.Data) {
            ChoiceChips(dataTools, tool) { tool = it; unit = defaultUnitForTool(it); label = defaultLabelForTool(it) }
            FieldTextField(label, { label = it }, "Label")
            if (tool == "Counter" || tool == "Species Tracker") Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedButton({ value = ((value.toIntOrNull() ?: 0) - 1).toString() }) { Text("−1") }; Text(value, style = MaterialTheme.typography.headlineSmall); Button({ value = ((value.toIntOrNull() ?: 0) + 1).toString() }) { Text("+1") }; TextButton({ value = "0" }) { Text("Reset") } }
        }
        CaptureStep("Measurement", "Use the suggested unit or type a better one.", FieldMindIcons.Graph) {
            FieldTextField(value, { value = it }, valueLabelForTool(tool))
            FieldTextField(unit, { unit = it }, "Unit", supportingText = "Suggested for $tool: ${defaultUnitForTool(tool)}")
            FieldTextField(location, { location = it }, "Location / site")
        }
        CaptureStep("Context", "Add conditions, instrument notes, mood, or quality flags.", FieldMindIcons.Note) {
            ChoiceChips(contextPresets, notes) { notes = if (notes.isBlank()) it else "$notes, $it" }
            FieldTextField(notes, { notes = it }, notesLabelForTool(tool), minLines = 3)
        }
    }
}

@Composable
internal fun NewReportDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf("Field Report") }; var title by remember { mutableStateOf("") }; var background by remember { mutableStateOf("") }; var question by remember { mutableStateOf("") }; var methods by remember { mutableStateOf("") }; var observations by remember { mutableStateOf("") }; var results by remember { mutableStateOf("") }; var interpretation by remember { mutableStateOf("") }; var conclusion by remember { mutableStateOf("") }; var limitations by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }
    FormDialog("Report Builder", onDismiss, { if (title.isNotBlank()) { viewModel.addReport(type, title, background, question, methods, observations, results, interpretation, conclusion, limitations, next); onDismiss() } }) {
        SourceFormHero("Report builder", "Create a clean local draft: claim, evidence, reasoning, limitations, and next steps.")
        CaptureStep("Type & title", "Pick a report preset for export organization.", FieldMindIcons.Report) {
            ChoiceChips(reportTypes, type) { type = it }
            FieldTextField(title, { title = it }, "Title")
        }
        CaptureStep("Setup", "Frame the research before results.", FieldMindIcons.Question) {
            FieldTextField(background, { background = it }, "Background", minLines = 2)
            FieldTextField(question, { question = it }, "Question", minLines = 2)
            FieldTextField(methods, { methods = it }, "Methods", minLines = 2)
        }
        CaptureStep("Evidence", "Summarize observations and data clearly.", FieldMindIcons.Data) {
            FieldTextField(observations, { observations = it }, "Observations", minLines = 2)
            FieldTextField(results, { results = it }, "Data / results", minLines = 2)
            FieldTextField(interpretation, { interpretation = it }, "Interpretation", minLines = 2)
        }
        CaptureStep("Conclusion", "Be honest about uncertainty and what comes next.", FieldMindIcons.Check) {
            FieldTextField(conclusion, { conclusion = it }, "Conclusion", minLines = 2)
            FieldTextField(limitations, { limitations = it }, "Limitations", minLines = 2)
            FieldTextField(next, { next = it }, "Next steps", minLines = 2)
            Text("Save generates a local Markdown draft for export.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun NewFlashcardDialog(viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf("concept") }; var front by remember { mutableStateOf("") }; var back by remember { mutableStateOf("") }; var useSm2 by remember { mutableStateOf(false) }
    FormDialog("Create Flashcard", onDismiss, { if (front.isNotBlank() && back.isNotBlank()) { viewModel.addFlashcard(front, back, type, deckMode = if (useSm2) "sm2" else "basic"); onDismiss() } }) {
        SourceFormHero("Create review card", "Design one card that flips cleanly during review — one prompt, one answer.")
        CaptureStep("Card preset", "Choose how this should be practiced.", FieldMindIcons.Flashcard) {
            ChoiceChips(listOf("term", "definition", "concept", "question-answer", "mistake card", "field ID", "method step"), type) { type = it }
        }
        CaptureStep("Front", "Make the prompt short enough to answer from memory.", FieldMindIcons.Question) {
            FieldTextField(front, { front = it }, "Prompt / front", minLines = 2)
        }
        CaptureStep("Back", "Add the answer, evidence, or correction.", FieldMindIcons.Answer) {
            FieldTextField(back, { back = it }, "Answer / back", minLines = 4)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Spaced repetition (SM-2)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Review at optimal intervals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = useSm2, onCheckedChange = { useSm2 = it })
        }
    }
}

@Composable
internal fun EditEntityDialog(kind: String, id: Long, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    when (kind) {
        "observation" -> viewModel.observations.collectAsState().value.firstOrNull { it.id == id }?.let { EditObservationDialog(it, viewModel, onDismiss) }
        "note" -> viewModel.notes.collectAsState().value.firstOrNull { it.id == id }?.let { EditNoteDialog(it, viewModel, onDismiss) }
        "question" -> viewModel.questions.collectAsState().value.firstOrNull { it.id == id }?.let { EditQuestionDialog(it, viewModel, onDismiss) }
        "hypothesis" -> viewModel.hypotheses.collectAsState().value.firstOrNull { it.id == id }?.let { EditHypothesisDialog(it, viewModel, onDismiss) }
        "project" -> viewModel.projects.collectAsState().value.firstOrNull { it.id == id }?.let { EditProjectDialog(it, viewModel, onDismiss) }
        "source" -> viewModel.sources.collectAsState().value.firstOrNull { it.id == id }?.let { EditSourceDialog(it, viewModel, onDismiss) }
        "data" -> viewModel.dataRecords.collectAsState().value.firstOrNull { it.id == id }?.let { EditDataRecordDialog(it, viewModel, onDismiss) }
        "report" -> viewModel.reports.collectAsState().value.firstOrNull { it.id == id }?.let { EditReportDialog(it, viewModel, onDismiss) }
        "flashcard" -> viewModel.flashcards.collectAsState().value.firstOrNull { it.id == id }?.let { EditFlashcardDialog(it, viewModel, onDismiss) }
        else -> onDismiss()
    }
}

@Composable
private fun EditNoteDialog(entity: NoteEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(entity.title) }; var body by remember { mutableStateOf(entity.body) }; var category by remember { mutableStateOf(entity.category) }; var tags by remember { mutableStateOf(entity.tags) }; var attachments by remember { mutableStateOf(entity.attachmentUris) }
    FormDialog("Edit Note", onDismiss, {
        if (title.isNotBlank() || body.isNotBlank()) { viewModel.updateNoteEntity(entity.copy(title = title.trim().ifBlank { body.take(36) }, body = body.trim(), category = category, tags = tags.trim(), attachmentUris = attachments.trim())); onDismiss() }
    }) {
        FormChoice("Category", observationCategories, category) { category = it }
        FieldTextField(title, { title = it }, "Title")
        FieldTextField(body, { body = it }, "Body", minLines = 5)
        FieldTextField(tags, { tags = it }, "Tags")
        FieldTextField(attachments, { attachments = it }, "Attachments", minLines = 2, supportingText = "One stored attachment per line: type|caption|uri")
    }
}

@Composable
private fun EditObservationDialog(entity: ObservationEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    val appContext = LocalContext.current
    var subject by remember { mutableStateOf(entity.subject) }
    var category by remember { mutableStateOf(entity.category) }
    var facts by remember { mutableStateOf(entity.factsOnlyNotes) }
    var confidence by remember { mutableStateOf(entity.confidenceLevel) }
    var location by remember { mutableStateOf(entity.manualLocation) }
    var latitude by remember { mutableStateOf(entity.latitude?.toString().orEmpty()) }
    var longitude by remember { mutableStateOf(entity.longitude?.toString().orEmpty()) }
    var tags by remember { mutableStateOf(entity.tags) }
    var evidence by remember { mutableStateOf(entity.evidenceSummary) }
    var fieldContext by remember { mutableStateOf(entity.moodOrContext) }
    var attachments by remember { mutableStateOf<List<DraftEvidenceAttachment>>(emptyList()) }
    var showEditCamera by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    val locationProvider = remember { FieldLocationProvider(appContext) }

    fun startLocating() {
        locating = true
        locationProvider.requestCurrentLocation { captured ->
            locating = false
            if (captured != null) {
                latitude = captured.latitude.toString()
                longitude = captured.longitude.toString()
                location = captured.asDisplayText()
                locationProvider.resolvePlaceName(captured.latitude, captured.longitude) { place ->
                    if (!place.isNullOrBlank()) location = captured.copy(placeName = place).asDisplayText()
                }
            }
            android.widget.Toast.makeText(appContext, if (captured != null) "GPS updated" else "Could not get GPS fix", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) startLocating() else android.widget.Toast.makeText(appContext, "Location permission denied", android.widget.Toast.LENGTH_SHORT).show()
    }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        attachments = attachments + uris.map { DraftEvidenceAttachment("Gallery", it.toString(), "Edited media") }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { appContext.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            attachments = attachments + DraftEvidenceAttachment("File", it.toString(), "Edited file / PDF")
        }
    }

    FormDialog("Edit Observation", onDismiss, {
        if (subject.isNotBlank()) {
            viewModel.updateObservation(entity.copy(subject = subject.trim(), category = category, factsOnlyNotes = facts.trim(), confidenceLevel = confidence, manualLocation = location.trim(), latitude = latitude.toDoubleOrNull(), longitude = longitude.toDoubleOrNull(), evidenceSummary = evidence.trim(), moodOrContext = fieldContext.trim()), tags)
            attachments.forEach { viewModel.addAttachmentToObservation(entity.id, it) }
            onDismiss()
        }
    }) {
        SourceFormHero("Edit observation", "Fix facts, add GPS, and attach missing photos/files without recreating the record.")
        CaptureStep("Identity", "Keep the subject and category easy to scan later.", FieldMindIcons.Observation) {
            FieldTextField(subject, { subject = it }, "Subject")
            ChoiceChips(observationCategories, category) { category = it }
            ChoiceChips(confidenceOptions, confidence) { confidence = it }
        }
        CaptureStep("Facts & context", "Separate facts from mood, context, or field conditions.", FieldMindIcons.Edit) {
            FieldTextField(facts, { facts = it }, "Facts only", minLines = 3)
            Text("Context presets", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(contextPresets, fieldContext) { fieldContext = if (fieldContext.isBlank()) it else "$fieldContext, $it" }
            FieldTextField(fieldContext, { fieldContext = it }, "Context / mood", minLines = 2)
        }
        CaptureStep("GPS", "Use automatic GPS or repair coordinates manually.", FieldMindIcons.Location) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = { if (locationProvider.hasAnyLocationPermission()) startLocating() else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                    modifier = Modifier.weight(1f),
                    enabled = !locating
                ) {
                    if (locating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(FieldMindIcons.Location, null, size = 18.dp)
                    Spacer(Modifier.size(6.dp)); Text(if (locating) "Locating…" else "Use GPS")
                }
                OutlinedButton(onClick = { latitude = ""; longitude = ""; location = "" }, modifier = Modifier.weight(1f)) { Text("Clear") }
            }
            FieldTextField(location, { location = it }, "Location")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldTextField(latitude, { latitude = it }, "Latitude", modifier = Modifier.weight(1f))
                FieldTextField(longitude, { longitude = it }, "Longitude", modifier = Modifier.weight(1f))
            }
        }
        CaptureStep("Evidence attachments", "Add photos, gallery images, PDFs, or documents to the existing observation.", FieldMindIcons.Camera) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showEditCamera = true }, Modifier.weight(1f)) { Icon(FieldMindIcons.Camera, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Photo") }
                OutlinedButton(onClick = { mediaPicker.launch("image/*") }, Modifier.weight(1f)) { Icon(FieldMindIcons.Gallery, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Gallery") }
                OutlinedButton(onClick = { filePicker.launch(arrayOf("application/pdf", "text/*", "image/*", "video/*", "audio/*")) }, Modifier.weight(1f)) { Icon(FieldMindIcons.File, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("File") }
            }
            AttachmentPreviewList(attachments, onCaptionChange = { index, caption -> attachments = attachments.mapIndexed { i, item -> if (i == index) item.copy(caption = caption) else item } }, onRemove = { remove -> attachments = attachments.filterIndexed { index, _ -> index != remove } })
        }
        FieldTextField(tags, { tags = it }, "Tags (comma separated)")
        FieldTextField(evidence, { evidence = it }, "Evidence summary", minLines = 2)
    }
    // In-app camera overlay for edit dialog (separate window to overlay FormDialog)
    if (showEditCamera) {
        Dialog(onDismissRequest = { showEditCamera = false }) {
            FieldMindCameraCapture(
                onPhotoCaptured = { uri, mimeType ->
                    attachments = attachments + DraftEvidenceAttachment("Photo", uri, "Edited observation photo", mimeType = mimeType)
                    showEditCamera = false
                    android.widget.Toast.makeText(appContext, "Photo captured.", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showEditCamera = false }
            )
        }
    }
}

@Composable
private fun EditQuestionDialog(entity: QuestionEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var question by remember { mutableStateOf(entity.questionText) }; var category by remember { mutableStateOf(entity.category) }; var source by remember { mutableStateOf(entity.sourceType) }; var status by remember { mutableStateOf(entity.status) }; var priority by remember { mutableStateOf(entity.priority) }; var answer by remember { mutableStateOf(entity.answer) }
    FormDialog("Edit Question", onDismiss, {
        if (question.isNotBlank()) { viewModel.updateQuestionEntity(entity.copy(questionText = question.trim(), category = category, sourceType = source, status = status, priority = priority, answer = answer.trim(), answeredAt = if (answer.isBlank()) null else (entity.answeredAt ?: System.currentTimeMillis()))); onDismiss() }
    }) {
        FieldTextField(question, { question = it }, "Question", minLines = 2)
        FormSectionLabel("Classification")
        FormChoice("Category", observationCategories, category) { category = it }
        FormChoice("Source type", sourceTypes, source) { source = it }
        FormChoice("Status", questionStatuses, status) { status = it }
        FormChoice("Priority", listOf("Low", "Medium", "High"), priority) { priority = it }
        FormSectionLabel("Answer")
        FieldTextField(answer, { answer = it }, "Answer", minLines = 2)
    }
}

@Composable
private fun EditHypothesisDialog(entity: HypothesisEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var prediction by remember { mutableStateOf(entity.prediction) }; var reasoning by remember { mutableStateOf(entity.reasoning) }; var evidence by remember { mutableStateOf(entity.evidenceNeeded) }; var support by remember { mutableStateOf(entity.supportCriteria) }; var weaken by remember { mutableStateOf(entity.weakeningCriteria) }; var test by remember { mutableStateOf(entity.testMethod) }; var result by remember { mutableStateOf(entity.resultStatus) }; var confidence by remember { mutableStateOf(entity.confidencePercent.toFloat()) }
    FormDialog("Edit Hypothesis", onDismiss, {
        if (prediction.isNotBlank()) { viewModel.updateHypothesisEntity(entity.copy(prediction = prediction.trim(), reasoning = reasoning.trim(), evidenceNeeded = evidence.trim(), supportCriteria = support.trim(), weakeningCriteria = weaken.trim(), testMethod = test.trim(), resultStatus = result, confidencePercent = confidence.toInt())); onDismiss() }
    }) {
        FieldTextField(prediction, { prediction = it }, "Prediction", minLines = 2); FieldTextField(reasoning, { reasoning = it }, "Reasoning", minLines = 2); FieldTextField(evidence, { evidence = it }, "Evidence needed", minLines = 2); FieldTextField(support, { support = it }, "Support criteria"); FieldTextField(weaken, { weaken = it }, "Weakening criteria"); FieldTextField(test, { test = it }, "Test method")
        FormChoice("Result", listOf("Unknown", "Supported", "Weakened", "Inconclusive"), result) { result = it }
        Text("Confidence: ${confidence.toInt()}%"); Slider(confidence, { confidence = it }, valueRange = 0f..100f)
    }
}

@Composable
private fun EditProjectDialog(entity: ProjectEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(entity.title) }; var topic by remember { mutableStateOf(entity.topicType) }; var objective by remember { mutableStateOf(entity.objective) }; var question by remember { mutableStateOf(entity.researchQuestion) }; var background by remember { mutableStateOf(entity.backgroundNotes) }; var methods by remember { mutableStateOf(entity.methods) }; var hypothesis by remember { mutableStateOf(entity.hypothesisSummary) }; var dataSummary by remember { mutableStateOf(entity.dataSummary) }; var analysis by remember { mutableStateOf(entity.analysis) }; var conclusion by remember { mutableStateOf(entity.conclusion) }; var future by remember { mutableStateOf(entity.futureQuestions) }
    FormDialog("Edit Project", onDismiss, {
        if (title.isNotBlank()) { viewModel.updateProjectEntity(entity.copy(title = title.trim(), topicType = topic.trim().ifBlank { "General" }, objective = objective.trim(), researchQuestion = question.trim(), backgroundNotes = background.trim(), methods = methods.trim(), hypothesisSummary = hypothesis.trim(), dataSummary = dataSummary.trim(), analysis = analysis.trim(), conclusion = conclusion.trim(), futureQuestions = future.trim())); onDismiss() }
    }) {
        FieldTextField(title, { title = it }, "Project title")
        FormChoice("Topic / category", listOf("Biology", "Geology", "Wildlife", "Ecology", "Plant Study", "Weather", "Human Pattern", "Other"), topic) { topic = it }
        FieldTextField(objective, { objective = it }, "Objective", minLines = 2)
        FieldTextField(question, { question = it }, "Research question", minLines = 2)
        FieldTextField(background, { background = it }, "Background notes", minLines = 2)
        FieldTextField(methods, { methods = it }, "Methods", minLines = 2)
        FieldTextField(hypothesis, { hypothesis = it }, "Hypothesis summary", minLines = 2)
        FieldTextField(dataSummary, { dataSummary = it }, "Data fields / summary", minLines = 2)
        FieldTextField(analysis, { analysis = it }, "Analysis", minLines = 2)
        FieldTextField(conclusion, { conclusion = it }, "Conclusion", minLines = 2)
        FieldTextField(future, { future = it }, "Future questions", minLines = 2)
    }
}

@Composable
private fun EditSourceDialog(entity: SourceEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    val projects by viewModel.projects.collectAsState()
    var type by remember { mutableStateOf(entity.type) }
    var title by remember { mutableStateOf(entity.title) }
    var author by remember { mutableStateOf(entity.author) }
    var dateOrYear by remember { mutableStateOf(entity.dateOrYear) }
    var doiOrIsbn by remember { mutableStateOf(entity.doiOrIsbn) }
    var publisherOrJournal by remember { mutableStateOf(entity.publisherOrJournal) }
    var accessDate by remember { mutableStateOf(entity.accessDate) }
    var link by remember { mutableStateOf(entity.link) }
    var fileUri by remember { mutableStateOf(entity.fileUri) }
    var citationStyleNote by remember { mutableStateOf(entity.citationStyleNote) }
    var importance by remember { mutableStateOf(entity.importance) }
    var readingStatus by remember { mutableStateOf(entity.readingStatus) }
    var projectId by remember { mutableStateOf(entity.relatedProjectId) }
    var summary by remember { mutableStateOf(entity.personalSummary) }
    var findings by remember { mutableStateOf(entity.keyFindings) }
    var taught by remember { mutableStateOf(entity.whatThisSourceTaughtMe) }
    var questions by remember { mutableStateOf(entity.questionsGenerated) }
    var notes by remember { mutableStateOf(entity.paperNotes) }
    var reliability by remember { mutableStateOf(entity.reliabilityScore.toFloat()) }
    FormDialog("Edit Source", onDismiss, {
        if (title.isNotBlank()) {
            viewModel.updateSourceEntity(
                entity.copy(
                    type = type,
                    title = title.trim(),
                    author = author.trim(),
                    dateOrYear = dateOrYear.trim(),
                    link = link.trim(),
                    doiOrIsbn = doiOrIsbn.trim(),
                    publisherOrJournal = publisherOrJournal.trim(),
                    accessDate = accessDate.trim(),
                    fileUri = fileUri.trim(),
                    citationStyleNote = citationStyleNote.trim(),
                    importance = importance,
                    personalSummary = summary.trim(),
                    keyFindings = findings.trim(),
                    whatThisSourceTaughtMe = taught.trim(),
                    questionsGenerated = questions.trim(),
                    paperNotes = notes.trim(),
                    reliabilityScore = reliability.toInt(),
                    readingStatus = readingStatus,
                    relatedProjectId = projectId
                )
            )
            onDismiss()
        }
    }) {
        SourceFormHero("Edit source", "Keep source identity, file/link, notes, and status synchronized.")
        FormChoice("Source type", sourceLibraryTypes, type) { type = it }
        FormSectionLabel("Identity")
        FieldTextField(title, { title = it }, "Title")
        FieldTextField(author, { author = it }, "Author / creator")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldTextField(dateOrYear, { dateOrYear = it }, "Date / year", modifier = Modifier.weight(1f))
            FieldTextField(accessDate, { accessDate = it }, "Accessed", modifier = Modifier.weight(1f))
        }
        FieldTextField(doiOrIsbn, { doiOrIsbn = it }, "DOI / ISBN")
        FieldTextField(publisherOrJournal, { publisherOrJournal = it }, "Publisher / journal")
        FormSectionLabel("Link and file")
        FieldTextField(link, { link = it }, "Web link")
        FieldTextField(fileUri, { fileUri = it }, "File URI")
        SourcePreviewCard(link, fileUri)
        FormSectionLabel("Reading notes")
        FieldTextField(summary, { summary = it }, "Main idea", minLines = 2)
        FieldTextField(findings, { findings = it }, "Key findings", minLines = 2)
        FieldTextField(taught, { taught = it }, "What this taught me", minLines = 2)
        FieldTextField(questions, { questions = it }, "New questions", minLines = 2)
        FieldTextField(notes, { notes = it }, "Paper / Cornell notes", minLines = 3)
        FieldTextField(citationStyleNote, { citationStyleNote = it }, "Citation style note")
        FormSectionLabel("Status")
        Text("Reading status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChoiceChips(readingStatuses, readingStatus) { readingStatus = it }
        Text("Importance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChoiceChips(sourceImportanceLevels, importance) { importance = it }
        Text("Credibility: ${reliability.toInt()}/5")
        Slider(reliability, { reliability = it }, valueRange = 1f..5f, steps = 3)
        if (projects.isNotEmpty()) {
            Text("Project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(listOf("No project") + projects.map { it.title }, projects.firstOrNull { it.id == projectId }?.title ?: "No project") { selected -> projectId = projects.firstOrNull { it.title == selected }?.id }
        }
    }
}

@Composable
private fun EditDataRecordDialog(entity: DataRecordEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var tool by remember { mutableStateOf(entity.toolType) }; var label by remember { mutableStateOf(entity.label) }; var value by remember { mutableStateOf(entity.value) }; var unit by remember { mutableStateOf(entity.unit) }; var location by remember { mutableStateOf(entity.location) }; var notes by remember { mutableStateOf(entity.notes) }
    FormDialog("Edit Data Record", onDismiss, {
        if (label.isNotBlank()) { viewModel.updateDataRecordEntity(entity.copy(toolType = tool, label = label.trim(), value = value.trim(), unit = unit.trim(), location = location.trim(), notes = notes.trim())); onDismiss() }
    }) {
        FormChoice("Tool", dataTools, tool) { tool = it }
        FieldTextField(label, { label = it }, "Label"); FieldTextField(value, { value = it }, "Value"); FieldTextField(unit, { unit = it }, "Unit"); FieldTextField(location, { location = it }, "Location / site"); FieldTextField(notes, { notes = it }, "Notes", minLines = 3)
    }
}

@Composable
private fun EditReportDialog(entity: ReportEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(entity.type) }; var title by remember { mutableStateOf(entity.title) }; var question by remember { mutableStateOf(entity.question) }; var methods by remember { mutableStateOf(entity.methods) }; var results by remember { mutableStateOf(entity.results) }; var conclusion by remember { mutableStateOf(entity.conclusion) }; var next by remember { mutableStateOf(entity.nextSteps) }
    FormDialog("Edit Report", onDismiss, {
        if (title.isNotBlank()) { viewModel.updateReportEntity(entity.copy(type = type, title = title.trim(), question = question.trim(), methods = methods.trim(), results = results.trim(), conclusion = conclusion.trim(), nextSteps = next.trim())); onDismiss() }
    }) {
        FormChoice("Report type", reportTypes, type) { type = it }
        FieldTextField(title, { title = it }, "Title"); FieldTextField(question, { question = it }, "Question", minLines = 2); FieldTextField(methods, { methods = it }, "Methods", minLines = 2); FieldTextField(results, { results = it }, "Data / results", minLines = 2); FieldTextField(conclusion, { conclusion = it }, "Conclusion", minLines = 2); FieldTextField(next, { next = it }, "Next steps", minLines = 2)
    }
}

@Composable
private fun EditFlashcardDialog(entity: FlashcardEntity, viewModel: FieldMindViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(entity.type) }; var front by remember { mutableStateOf(entity.front) }; var back by remember { mutableStateOf(entity.back) }; var useSm2 by remember { mutableStateOf(entity.deckMode == "sm2") }
    FormDialog("Edit Flashcard", onDismiss, {
        if (front.isNotBlank() && back.isNotBlank()) { viewModel.updateFlashcardEntity(entity.copy(type = type, front = front.trim(), back = back.trim(), deckMode = if (useSm2) "sm2" else "basic")); onDismiss() }
    }) {
        FormChoice("Card type", listOf("term", "definition", "concept", "question-answer", "mistake card"), type) { type = it }
        FieldTextField(front, { front = it }, "Front"); FieldTextField(back, { back = it }, "Back", minLines = 3)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Spaced repetition (SM-2)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(if (useSm2) "SM-2 scheduling active" else "Basic flip mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = useSm2, onCheckedChange = { useSm2 = it })
        }
    }
}

/**
 * Map card for an observation detail: a small offline preview marker, the resolved place name
 * (when known), the exact coordinates, and a link out to the device map app.
 */
@Composable
internal fun ObservationLocationCard(latitude: Double, longitude: Double, manualLocation: String) {
    val colors = FieldMindTheme.colors
    val uriHandler = LocalUriHandler.current
    val coords = "%.5f, %.5f".format(latitude, longitude)
    val placeName = manualLocation.substringBefore(" • GPS").trim().takeIf { it.isNotBlank() && !it.startsWith("GPS") }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon = FieldMindIcons.Location, contentDescription = null, tint = colors.observation, size = 20.dp)
                Text("Location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            if (placeName != null) Text(placeName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
                OsmMap(listOf(latitude to longitude), markerColor = colors.observation)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(coords, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val label = Uri.encode(placeName ?: "Observation")
                    runCatching { uriHandler.openUri("geo:$latitude,$longitude?q=$latitude,$longitude($label)") }
                }) {
                    Icon(icon = FieldMindIcons.Location, contentDescription = null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Open in maps")
                }
            }
        }
    }
}

