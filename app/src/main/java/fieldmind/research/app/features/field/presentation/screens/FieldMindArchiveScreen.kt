package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.learn.LearnLibrary
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import androidx.activity.compose.BackHandler

// ══════════════════════════════════════════════════════════════════════
//  Search archive
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ArchiveScreen(viewModel: FieldMindViewModel, onOpenDetail: (String, Long) -> Unit = { _, _ -> }, onOpenReader: (String, String) -> Unit = { _, _ -> }, onBack: () -> Unit = {}) {
    // Handle device back button
    BackHandler(enabled = true) { onBack() }
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    var query by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StandardScreenHeader(
                title = "Search Archive",
                subtitle = "Search forever by topic, date, place, source, project, and keyword.",
                icon = FieldMindIcons.Search
            )
        }
        item {
            OutlinedTextField(query, { query = it }, label = { Text("Search") }, leadingIcon = { Icon(icon = FieldMindIcons.Search, contentDescription = null, size = 20.dp) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true)
        }
        val q = query.trim().lowercase()
        if (q.length < 2) {
            item { EmptyState("Search smarter", "Type at least 2 characters. FieldMind limits in-memory search results so large archives stay responsive; full FTS indexing is the next database step.", icon = FieldMindIcons.Search) }
        } else {
            fun matches(vararg parts: String) = parts.any { it.lowercase().contains(q) }
            itemsIndexed(observations.filter { matches(it.subject, it.category, it.factsOnlyNotes, it.manualLocation, it.tags) }.take(30)) { i, it -> EntityCard(it.subject, "observation", body = it.factsOnlyNotes.take(120), confidence = it.confidenceLevel, meta = listOf(it.category), onClick = { onOpenDetail("observation", it.id) }, index = i, animate = true) }
            itemsIndexed(notes.filter { matches(it.title, it.body, it.category, it.tags) }.take(30)) { i, it -> EntityCard(it.title, "note", body = it.body.take(120), meta = listOf(it.category, recentRelativeTime(it.updatedAt)), onClick = { onOpenDetail("note", it.id) }, index = i, animate = true) }
            itemsIndexed(questions.filter { matches(it.questionText, it.category, it.status) }.take(30)) { i, it -> EntityCard(it.questionText, "question", meta = listOf(it.status), onClick = { onOpenDetail("question", it.id) }, index = i, animate = true) }
            itemsIndexed(projects.filter { matches(it.title, it.topicType, it.objective, it.researchQuestion) }.take(30)) { i, it -> EntityCard(it.title, "project", body = it.objective, meta = listOf(it.topicType), onClick = { onOpenDetail("project", it.id) }, index = i, animate = true) }
            itemsIndexed(sources.filter { matches(it.title, it.author, it.type, it.dateOrYear, it.link, it.doiOrIsbn, it.publisherOrJournal, it.accessDate, it.fileUri, it.citationStyleNote, it.importance, it.readingStatus, it.personalSummary, it.keyFindings, it.questionsGenerated, it.paperNotes) }.take(30)) { i, it -> EntityCard(it.title, "source", body = it.whatThisSourceTaughtMe, meta = listOf(it.type), onClick = { onOpenDetail("source", it.id) }, index = i, animate = true) }
            itemsIndexed(reports.filter { matches(it.title, it.type, it.question, it.conclusion) }.take(30)) { i, it -> EntityCard(it.title, "report", body = it.conclusion, meta = listOf(it.type), onClick = { onOpenDetail("report", it.id) }, index = i, animate = true) }
            val learnMatches = LearnLibrary.flatMap { category -> category.topics.flatMap { topic -> topic.resources.map { Triple(category, topic, it) } } }
                .filter { (category, topic, resource) -> matches(category.name, category.description, topic.name, topic.summary, resource.title, resource.kind, resource.why, resource.author, resource.url) }
                .take(20)
            if (learnMatches.isNotEmpty()) item { SectionHeader("Learn resources", "${learnMatches.size} free articles, guides, books, or tools") }
            itemsIndexed(learnMatches) { i, (_, topic, resource) -> EntityCard(resource.title, "learn", body = resource.why, meta = listOf(resource.kind, topic.name, resource.author.ifBlank { "Free access" }), onClick = { onOpenReader(resource.url, resource.title) }, index = i, animate = true) }
            itemsIndexed(flashcards.filter { matches(it.front, it.back, it.type) }.take(30)) { i, it -> EntityCard(it.front, "flashcard", body = it.back, meta = listOf(it.type), onClick = { onOpenDetail("flashcard", it.id) }, index = i, animate = true) }
        }
    }
}

