package fieldmind.research.app.features.field.presentation.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.media.MediaPlayer
import android.net.Uri
import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.learn.BookSuggestions
import fieldmind.research.app.features.field.data.learn.GuidedPath
import fieldmind.research.app.features.field.data.learn.GuidedPaths
import fieldmind.research.app.features.field.data.learn.LearnCategory
import fieldmind.research.app.features.field.data.learn.LearnLibrary
import fieldmind.research.app.features.field.data.learn.LearnResource
import fieldmind.research.app.features.field.data.learn.SuggestedOnlineApis
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.activity.compose.BackHandler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyRow
// ══════════════════════════════════════════════════════════════════════
//  Library (Sources / Reading / Flashcards / Learn)
// ══════════════════════════════════════════════════════════════════════

@Composable
fun KnowledgeLibraryScreen(
    viewModel: FieldMindViewModel,
    onNavigate: (FieldMindScreen) -> Unit = {},
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    onOpenReader: (String, String) -> Unit = { _, _ -> },
    startTab: Int = 0
) {
    val sources by viewModel.sources.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    var tab by remember(startTab) { mutableIntStateOf(startTab) }
    val tabs = listOf("Sources", "Notes", "Reading", "Flashcards", "Learn")
    val haptics = rememberFieldMindHaptics()
    fun selectTab(next: Int) {
        val bounded = next.coerceIn(0, tabs.lastIndex)
        if (bounded != tab) { tab = bounded; haptics.light() }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)) {
            FieldScreenHeader("Knowledge Hub", "Sources, notes, reading, flashcards, and learning.", icon = FieldMindIcons.Library)
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 20.dp, containerColor = MaterialTheme.colorScheme.background) {
            tabs.forEachIndexed { i, label -> Tab(tab == i, { selectTab(i) }, text = { Text(label) }) }
        }
        Box(
            modifier = Modifier.fillMaxSize().pointerInput(tab) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                    onDragEnd = { if (abs(totalDrag) > 96f) { if (totalDrag < 0) selectTab(tab + 1) else selectTab(tab - 1) } }
                )
            }
        ) {
            when (tab) {
                0 -> SourcePanel(viewModel, sources, onOpenDetail)
                1 -> NotePanel(viewModel, notes, onOpenDetail)
                2 -> PaperReadingPanel(sources, onOpenDetail)
                3 -> FlashcardPanel(viewModel, flashcards, sources, notes, onOpenDetail) { onNavigate(FieldMindScreen.Flashcards) }
                4 -> LearnPanel(viewModel, onOpenReader)
            }
        }
    }
}

@Composable
private fun SourcePanel(viewModel: FieldMindViewModel, items: List<SourceEntity>, onOpenDetail: (String, Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    val projects by viewModel.projects.collectAsState()
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AddButton("Add source") { show = true } }
        if (items.isEmpty()) {
            item { EmptyState("No sources yet", "Save articles, videos, PDFs, books, summaries, citations, and what each source taught you.", icon = FieldMindIcons.Source) }
        }
        items(items) { EntityCard(it.title, "source", body = it.whatThisSourceTaughtMe.ifBlank { it.personalSummary }, meta = listOf(it.type, it.author.ifBlank { "Unknown author" }, it.readingStatus, it.importance, "reliability ${it.reliabilityScore}/5"), onClick = { onOpenDetail("source", it.id) }) }
    }
    if (show) {
        NewSourceDialog(viewModel, onDismiss = { show = false })
    }
}

@Composable
private fun NotePanel(viewModel: FieldMindViewModel, items: List<NoteEntity>, onOpenDetail: (String, Long) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var categoriesExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember(items) { listOf("All") + items.map { it.category.ifBlank { "Other" } }.distinct().sorted() }
    val filtered = remember(items, selectedCategory) { if (selectedCategory == "All") items else items.filter { it.category == selectedCategory } }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionHeader("Notes", "Add quick notes here. Categories stay collapsed until you need filtering.") }
        item { AddButton(if (showAdd) "Close note composer" else "Add note") { showAdd = !showAdd } }
        if (showAdd) item { NoteCaptureCard(viewModel = viewModel, initialCategory = selectedCategory.takeIf { it != "All" } ?: observationCategories.last()) { showAdd = false } }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { categoriesExpanded = !categoriesExpanded }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(icon = FieldMindIcons.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                        Text("Categories", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        InfoChip(selectedCategory)
                        Icon(icon = if (categoriesExpanded) FieldMindIcons.Up else FieldMindIcons.Down, contentDescription = null, size = 20.dp)
                    }
                    AnimatedVisibility(categoriesExpanded) { ChoiceChips(categories, selectedCategory) { selectedCategory = it } }
                }
            }
        }
        if (filtered.isEmpty()) item { EmptyState("No notes yet", "Create one from this Notes tab or Capture → Note.", icon = FieldMindIcons.Note) }
        items(filtered) { EntityCard(it.title, "note", body = it.body.ifBlank { "No body yet." }, meta = listOf(it.category, recentRelativeTime(it.updatedAt), if (it.attachmentUris.isBlank()) "No attachments" else "Attachments"), onClick = { onOpenDetail("note", it.id) }) }
    }
}

@Composable
private fun NoteCaptureCard(
    viewModel: FieldMindViewModel,
    initialCategory: String,
    onSaved: () -> Unit
) {
    val haptics = rememberFieldMindHaptics()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var tags by remember { mutableStateOf("") }

    NewNoteDialog(viewModel, onDismiss = onSaved)
}

@Composable
private fun PaperReadingPanel(items: List<SourceEntity>, onOpenDetail: (String, Long) -> Unit) {
    val readCount = items.count { it.readingStatus == "Read" }
    val inProgressCount = items.count { it.readingStatus == "In progress" }
    val total = items.size.coerceAtLeast(1)
    val progressFraction = readCount.toFloat() / total

    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SectionHeader("Paper reading mode", "Track your reading progress across saved sources.")
        }
        if (items.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(FieldMindIcons.Book, null, tint = FieldMindTheme.colors.source, size = 22.dp)
                            Column(Modifier.weight(1f)) {
                                Text("Reading progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("$readCount read, $inProgressCount in progress, ${total - readCount - inProgressCount} not started", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${(progressFraction * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = FieldMindTheme.colors.source)
                        }
                        LinearProgressIndicator(
                            progress = progressFraction,
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = FieldMindTheme.colors.source,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatPill("${items.size}", "Total", FieldMindIcons.Library)
                            StatPill("$readCount", "Read", FieldMindIcons.Done)
                            StatPill("$inProgressCount", "In progress", FieldMindIcons.Timer)
                        }
                    }
                }
            }
        }
        if (items.isEmpty()) {
            item { EmptyState("Add a source first", "Paper prompts are saved inside each source note.", icon = FieldMindIcons.Source) }
        }

        items(items) { source ->
            val readingColor = when (source.readingStatus) {
                "Read" -> FieldMindTheme.colors.positive
                "In progress" -> FieldMindTheme.colors.flashcard
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            EntityCard(
                source.title,
                "read",
                body = source.paperNotes.ifBlank {
                    if (source.readingStatus == "Read") "Completed reading" else "Open source detail to read and take notes."
                },
                meta = listOf(
                    source.readingStatus.ifBlank { "Not started" },
                    "${source.personalSummary.length.coerceAtMost(2000)} chars"
                ),
                onClick = { onOpenDetail("source", source.id) }
            )
        }
    }
}

@Composable
private fun StatPill(value: String, label: String, icon: MaterialSymbolIcon) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 14.dp)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FlashcardPanel(
    viewModel: FieldMindViewModel,
    items: List<FlashcardEntity>,
    sources: List<SourceEntity>,
    notes: List<NoteEntity>,
    onOpenDetail: (String, Long) -> Unit,
    onStartReview: () -> Unit
) {
    var show by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val localEnabled by viewModel.fieldSettings.localModelEnabled.collectAsState()
    val localDownloaded by viewModel.fieldSettings.localModelDownloaded.collectAsState()
    val localModel by viewModel.fieldSettings.localModelOption.collectAsState()
    var type by remember { mutableStateOf("concept") }; var front by remember { mutableStateOf("") }; var back by remember { mutableStateOf("") }; var useSm2 by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onStartReview, Modifier.weight(1f), shape = RoundedCornerShape(16.dp), enabled = items.isNotEmpty()) {
                    Icon(icon = FieldMindIcons.Flip, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Review (${items.size})")
                }
                OutlinedButton(onClick = { show = !show; if (!show) { front = ""; back = "" } }, Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Icon(icon = if (show) FieldMindIcons.Close else FieldMindIcons.Add, contentDescription = null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text(if (show) "Cancel" else "New card")
                }
            }
        }
        item {
            LocalStudyModelCard(localEnabled, localDownloaded, localModel) {
                val generated = autoFlashcardsFromLibrary(sources, notes).take(6)
                generated.forEach { (front, back) -> viewModel.addFlashcard(front, back, "Local model") }
                // Auto-flashcard generation completed — UI updates automatically
            }
        }
        if (items.isEmpty()) item { EmptyState("No flashcards yet", "Turn terms, definitions, mistakes, source concepts, and questions into review cards.", icon = FieldMindIcons.Flashcard) }
        item {
            if (items.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { card ->
                                LibraryFlashcard(card, Modifier.weight(1f)) { onOpenDetail("flashcard", card.id) }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
    if (show) {
        NewFlashcardDialog(viewModel, onDismiss = { show = false })
    }
    }


@Composable
private fun LocalStudyModelCard(enabled: Boolean, downloaded: Boolean, model: String, onGenerate: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon = FieldMindIcons.Sparkle, contentDescription = null, tint = FieldMindTheme.colors.flashcard, size = 24.dp)
                Column(Modifier.weight(1f)) {
                    Text("Offline study generator", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(if (enabled && downloaded) "$model is ready inside the app" else "Download a local model in Settings to make cards without servers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onGenerate, enabled = enabled && downloaded, shape = RoundedCornerShape(14.dp)) { Text("Auto cards") }
            }
        }
    }
}

private fun autoFlashcardsFromLibrary(sources: List<SourceEntity>, notes: List<NoteEntity>): List<Pair<String, String>> {
    val sourceCards = sources.take(4).mapNotNull { source ->
        val answer = source.personalSummary.ifBlank { source.keyFindings }.ifBlank { source.whatThisSourceTaughtMe }.ifBlank { source.publisherOrJournal }
        if (source.title.isBlank() || answer.isBlank()) null else "What is the key idea from ${source.title}?" to answer.take(260)
    }
    val noteCards = notes.take(4).mapNotNull { note ->
        val body = note.body.ifBlank { note.title }
        if (body.isBlank()) null else "Review note: ${note.title.ifBlank { note.category }}" to body.take(260)
    }
    return (sourceCards + noteCards).distinctBy { it.first }
}

/** A single library flashcard: shows the prompt; the answer stays hidden until tapped. */
@Composable
private fun LibraryFlashcard(card: FlashcardEntity, modifier: Modifier = Modifier, onOpenDetail: () -> Unit) {
    var revealed by remember(card.id) { mutableStateOf(false) }
    val accent = FieldMindTheme.colors.flashcard
    val rotation by androidx.compose.animation.core.animateFloatAsState(if (revealed) 180f else 0f, animationSpec = tween(360), label = "libraryCardFlip")
    Card(
        modifier = modifier.fillMaxWidth().animateContentSize().graphicsLayer { rotationY = rotation; cameraDistance = 28f }.clickable { revealed = !revealed },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().graphicsLayer { if (rotation > 90f) rotationY = 180f }.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon = FieldMindIcons.Flashcard, contentDescription = null, tint = accent, size = 18.dp) }
                Text(card.type, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Icon(
                    icon = if (revealed) FieldMindIcons.VisibilityOff else FieldMindIcons.Visibility,
                    contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp
                )
            }
            Text(card.front, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (revealed) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(card.back, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = onOpenDetail, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text("Open card"); Spacer(Modifier.size(4.dp)); Icon(icon = FieldMindIcons.Forward, contentDescription = null, size = 16.dp)
                }
            } else {
                Text("Tap to reveal answer", style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }
    }
}

private data class ResearchMilestone(val title: String, val body: String, val icon: MaterialSymbolIcon, val resource: LearnResource)

private val beginnerResearchMilestones = listOf(
    ResearchMilestone("Observe carefully", "Separate facts from interpretation, then document time, place, context, and evidence.", FieldMindIcons.Observation, LearnResource("Understanding Science", "Guide", "https://undsci.berkeley.edu/", "Science starts with careful observation and honest uncertainty.")),
    ResearchMilestone("Ask researchable questions", "Turn curiosity into a question you can observe, compare, measure, or verify.", FieldMindIcons.Question, LearnResource("Research as inquiry", "Framework", "https://www.ala.org/acrl/standards/ilframework", "Research grows from increasingly focused questions.")),
    ResearchMilestone("Evaluate sources", "Use DOI/ISBN, venue, author, and evidence quality before trusting a claim.", FieldMindIcons.Source, LearnResource("Crossref REST API", "Reference", "https://www.production.crossref.org/documentation/retrieve-metadata/rest-api/", "Look up DOI metadata and verify bibliographic details.")),
    ResearchMilestone("Plan a small investigation", "Define method, site, sample, bias controls, and safety before collecting data.", FieldMindIcons.Project, LearnResource("Framework for Science Education", "Guide", "https://nap.nationalacademies.org/resource/13165/interactive/", "Science practices include planning investigations and analyzing evidence.")),
    ResearchMilestone("Collect usable data", "Record measurements, counts, checklists, and metadata consistently.", FieldMindIcons.Data, LearnResource("OpenIntro Statistics", "Book", "https://www.openintro.org/book/os/", "A free introduction to variation, sampling, and data summaries.")),
    ResearchMilestone("Explain with evidence", "Write claim, evidence, reasoning, limits, and next questions without overstating certainty.", FieldMindIcons.Report, LearnResource("Purdue OWL citation resources", "Guide", "https://owl.purdue.edu/owl/research_and_citation/resources.html", "Guides for writing, citing, and communicating research."))
)

@Composable
private fun LearnPanel(viewModel: FieldMindViewModel, onOpenReader: (String, String) -> Unit = { _, _ -> }) {
    val observations by viewModel.observations.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val signals = remember(observations, questions, sources, projects) {
        buildList {
            observations.take(8).forEach { add(it.category); add(it.tags); add(it.subject) }
            questions.take(6).forEach { add(it.category); add(it.questionText) }
            sources.take(6).forEach { add(it.type); add(it.title); add(it.publisherOrJournal) }
            projects.take(4).forEach { add(it.topicType); add(it.title) }
        }.joinToString(" ")
    }
    val next = remember(observations.size, questions.size, sources.size, projects.size, reports.size) {
        when {
            observations.isEmpty() -> beginnerResearchMilestones[0]
            questions.isEmpty() -> beginnerResearchMilestones[1]
            sources.isEmpty() -> beginnerResearchMilestones[2]
            projects.isEmpty() -> beginnerResearchMilestones[3]
            reports.isEmpty() -> beginnerResearchMilestones[5]
            else -> beginnerResearchMilestones[4]
        }
    }
    LazyColumn(contentPadding = panelPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ResearchJourneyHero(next, signals, onOpenReader) }
        item { SectionHeader("Beginner researcher path", "A guided path from observation to evidence-based communication.") }
        items(beginnerResearchMilestones) { milestone -> ResearchMilestoneCard(milestone, onOpenReader) }
        item { SectionHeader("Based on your activity", if (signals.isBlank()) "Start capturing to personalize this section" else "Recent topics shape these suggestions") }
        if (signals.isBlank()) {
            item { EntityCard("Start with one observation", "observation", body = "Capture one facts-only observation, then return here for a tailored next step.") }
        } else {
            items(recommendedResources(listOf(signals))) { rec -> EntityCard(rec.resource.title, "learn", body = rec.resource.why, meta = listOf(rec.resource.kind, rec.path), onClick = { onOpenReader(rec.resource.url, rec.resource.title) }) }
        }
        item { SectionHeader("Book suggestions", "Free first: OpenStax, Project Gutenberg, BHL, NCBI, and Open Library subject shelves.") }
        items(BookSuggestions.filter { signals.isBlank() || signals.contains(it.category, ignoreCase = true) || signals.contains(it.genre, ignoreCase = true) }.ifEmpty { BookSuggestions.take(6) }) { book ->
            BookSuggestionCard(book.title, book.category, book.genre, book.author, book.why, book.freeUrl, book.buyUrl, onOpenReader)
        }
        item { SectionHeader("Curated reference library", "Expand when you want deeper subject-specific learning.") }
        items(LearnLibrary) { category -> LearnCategoryCard(category) { res -> onOpenReader(res.url, res.title) } }
        item { SectionHeader("Optional online discovery", "Use verified metadata sources; never trust generated citations without checking.") }
        item { ResearchAssistantCard() }
        item { OnlineApiProposalCard() }
    }
}



@Composable
private fun BookSuggestionCard(title: String, category: String, genre: String, author: String, why: String, freeUrl: String, buyUrl: String, onOpenReader: (String, String) -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(FieldMindTheme.colors.source.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(icon = FieldMindIcons.Book, contentDescription = null, tint = FieldMindTheme.colors.source, size = 22.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(listOf(author, category, genre).filter { it.isNotBlank() }.joinToString(" • "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOpenReader(freeUrl, title) }, shape = RoundedCornerShape(14.dp)) { Text("Free/read") }
                OutlinedButton(onClick = { onOpenReader(buyUrl, "$title buying options") }, shape = RoundedCornerShape(14.dp)) { Text("Buy options") }
            }
        }
    }
}

@Composable
private fun LearnCategoryCard(category: LearnCategory, onOpenResource: (LearnResource) -> Unit) {
    var expanded by rememberSaveable(category.name) { mutableStateOf(false) }
    val accent = FieldMindTheme.colors.accentFor(category.name)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon = FieldMindIcons.School, contentDescription = null, tint = accent, size = 24.dp) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(category.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (expanded) 4 else 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(icon = if (expanded) FieldMindIcons.Up else FieldMindIcons.Down, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
            }

            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    category.topics.forEach { topic ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(topic.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(topic.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                topic.resources.forEach { resource ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onOpenResource(resource) }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(icon = learnKindIcon(resource.kind), contentDescription = null, tint = accent, size = 18.dp)
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(resource.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text(resource.why, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                        Icon(icon = FieldMindIcons.OpenLink, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineApiProposalCard() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon = FieldMindIcons.Source, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, size = 23.dp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Verified discovery sources", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Metadata APIs for DOI, ISBN, papers, books, biodiversity, and field context.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(icon = if (expanded) FieldMindIcons.Up else FieldMindIcons.Down, contentDescription = null, size = 20.dp)
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestedOnlineApis.forEach { api ->
                        EntityCard(
                            title = api.name,
                            kind = "source",
                            body = api.notes,
                            meta = listOf(api.baseUrl)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ResearchAssistantCard() {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon = FieldMindIcons.Sparkle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 24.dp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Research assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Turn observations into structured reports. Use templates to build consistent field notes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(icon = FieldMindIcons.Ask, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
            }
            Text(
                "Available tools: citation lookup via DOI/ISBN, field report templates, PDF annotation, and structured data export.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResearchJourneyHero(next: ResearchMilestone, signals: String, onOpenReader: (String, String) -> Unit) {
    Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(next.icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 30.dp) }
                Column(Modifier.weight(1f)) {
                    Text("Recommended next step", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f), fontWeight = FontWeight.SemiBold)
                    Text(next.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            }
            Text(next.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f))
            if (signals.isNotBlank()) InfoChip("Personalized from recent activity", icon = FieldMindIcons.Sparkle, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Button(onClick = { onOpenReader(next.resource.url, next.resource.title) }, shape = RoundedCornerShape(16.dp)) { Text("Open starter resource") }
        }
    }
}

@Composable
private fun ResearchMilestoneCard(milestone: ResearchMilestone, onOpenReader: (String, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    EntityCard(milestone.title, "learn", body = milestone.body, meta = listOf(milestone.resource.kind), onClick = { expanded = !expanded })
    AnimatedVisibility(expanded) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(milestone.resource.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(milestone.resource.why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { onOpenReader(milestone.resource.url, milestone.resource.title) }, contentPadding = PaddingValues(0.dp)) { Text("Open resource"); Spacer(Modifier.size(4.dp)); Icon(FieldMindIcons.Forward, null, size = 18.dp) }
            }
        }
    }
}

@Composable
private fun GuidedPathCard(path: GuidedPath, onOpenReader: (String, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val accent = FieldMindTheme.colors.accentFor("learn")
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(icon = FieldMindIcons.School, contentDescription = null, tint = accent, size = 22.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(path.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(path.goal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(icon = if (expanded) FieldMindIcons.Up else FieldMindIcons.Down, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
            }
            if (expanded) {
                path.steps.forEachIndexed { i, res ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onOpenReader(res.url, res.title) }.background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = accent)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(res.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("${res.kind} · ${res.why}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(icon = FieldMindIcons.Book, contentDescription = null, tint = accent, size = 18.dp)
                    }
                }
            } else {
                Text("${path.steps.size} steps", style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }
    }
}

/**
 * In-app reader for any file type (URL, image, audio, video, PDF) using native viewers.
 * Uses AsyncImage for images, MediaPlayer for audio, VideoView for video,
 * external Intent for PDFs, and WebView only for http/https URLs.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LearnReaderScreen(url: String, title: String, onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val readerUrl = remember(url) {
        if (uriLooksPdf(url) && url.startsWith("http", ignoreCase = true)) {
            "https://docs.google.com/gview?embedded=1&url=${Uri.encode(url)}"
        } else url
    }
    var loading by remember(readerUrl) { mutableStateOf(!uriLooksImage(url) && !uriLooksAudio(url) && !uriLooksVideo(url)) }
    var errorMessage by remember(readerUrl) { mutableStateOf<String?>(null) }
    var retryKey by remember(readerUrl) { mutableIntStateOf(0) }
    var showReaderFallback by remember(readerUrl) { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val background = MaterialTheme.colorScheme.background

    // Determine if this is a local file or a web URL
    val isLocalFile = remember(url) {
        url.startsWith("content://") || url.startsWith("file://") || url.startsWith("/")
    }
    val isWebUrl = remember(url) {
        url.startsWith("http://") || url.startsWith("https://")
    }

    BackHandler(enabled = true) {
        val wv = webView
        when {
            showReaderFallback && errorMessage != null -> showReaderFallback = false
            wv != null && wv.canGoBack() -> wv.goBack()
            else -> onBack()
        }
    }
    Column(Modifier.fillMaxSize().background(background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(icon = FieldMindIcons.Back, contentDescription = "Back", size = 22.dp) }
            Text(title.ifBlank { "Reader" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = { runCatching { uriHandler.openUri(url) } }) { Icon(icon = FieldMindIcons.OpenLink, contentDescription = "Open externally", size = 22.dp) }
        }
        Box(Modifier.fillMaxSize()) {
            when {
                // ── Image viewer ──
                uriLooksImage(url) -> AsyncImage(
                    model = url,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
                )

                // ── Audio player ──
                uriLooksAudio(url) -> NativeAudioPlayer(
                    uri = Uri.parse(url),
                    title = title,
                    modifier = Modifier.fillMaxSize().padding(24.dp)
                )

                // ── Video player ──
                uriLooksVideo(url) -> NativeVideoPlayer(
                    uri = Uri.parse(url),
                    modifier = Modifier.fillMaxSize()
                )

                // ── PDF — open in system viewer ──
                uriLooksPdf(url) -> PdfOpenCard(url, uriHandler, context)

                // ── Local files (non-image/non-audio/non-video) — open externally ──
                isLocalFile && !isWebUrl -> OpenExternallyCard(url, uriHandler, context)

                // ── Web URLs — WebView ──
                else -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, startedUrl: String?, favicon: android.graphics.Bitmap?) { loading = true; errorMessage = null; showReaderFallback = true }
                                    override fun onPageFinished(view: WebView?, finishedUrl: String?) { loading = false }
                                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                        if (request?.isForMainFrame != false) { loading = false; errorMessage = error?.description?.toString() ?: "Could not load this page." }
                                    }
                                }
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                loadUrl(readerUrl)
                                webView = this
                            }
                        },
                        update = { if (it.url != readerUrl) it.loadUrl(readerUrl) }
                    )
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                    if (showReaderFallback) errorMessage?.let { message ->
                        ReaderFallbackCard(message, url, { retryKey++; webView?.reload() }, Modifier.align(Alignment.TopCenter), onDismiss = { showReaderFallback = false })
                    }
                }
            }
        }
    }
}

/**
 * Native audio player using Android MediaPlayer with play/pause controls.
 */
@Composable
private fun NativeAudioPlayer(uri: Uri, title: String, modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val mediaPlayer = remember {
        MediaPlayer().apply {
            try {
                setDataSource(context, uri)
                setOnPreparedListener { isPrepared = true; isPlaying = false }
                setOnErrorListener { _, _, _ -> errorMsg = "Could not play this audio file."; true }
                setOnCompletionListener { isPlaying = false }
                prepareAsync()
            } catch (e: Exception) {
                errorMsg = "Could not load audio: ${e.message}"
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { mediaPlayer.release() }
        }
    }
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier
    ) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon = FieldMindIcons.Mic,
                contentDescription = null,
                tint = FieldMindTheme.colors.source,
                size = 64.dp,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(32.dp)).background(FieldMindTheme.colors.source.copy(alpha = 0.12f)).padding(16.dp)
            )
            Spacer(Modifier.size(24.dp))
            Text(
                title.ifBlank { "Audio Playback" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(8.dp))

            if (errorMsg != null) {
                Text(errorMsg!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            } else if (!isPrepared) {
                LinearProgressIndicator(Modifier.fillMaxWidth(0.6f))
                Spacer(Modifier.size(8.dp))
                Text("Loading audio...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Spacer(Modifier.size(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = {
                            if (isPlaying) {
                                runCatching { mediaPlayer.pause() }; isPlaying = false
                            } else {
                                runCatching { mediaPlayer.start() }; isPlaying = true
                            }
                        },
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            icon = if (isPlaying) FieldMindIcons.Pause else FieldMindIcons.Play,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            size = 32.dp
                        )
                    }
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    if (isPlaying) "Playing..." else "Paused",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Native video player using Android VideoView with built-in MediaController controls.
 */
@Composable
private fun NativeVideoPlayer(uri: Uri, modifier: Modifier = Modifier) {
    var errorMsg by remember { mutableStateOf<String?>(null) }
    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(uri)
                    val controller = MediaController(ctx)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                    setOnErrorListener { _, _, _ ->
                        errorMsg = "Could not play this video."
                        true
                    }
                    requestFocus()
                    start()
                }
            },
            update = { }
        )
        if (errorMsg != null) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    errorMsg!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Card prompting the user to open a PDF in the system viewer.
 */
@Composable
private fun PdfOpenCard(url: String, uriHandler: androidx.compose.ui.platform.UriHandler, context: android.content.Context) {
    Card(
        Modifier.fillMaxSize().padding(32.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(FieldMindIcons.Article, null, tint = MaterialTheme.colorScheme.primary, size = 64.dp)
            Spacer(Modifier.size(24.dp))
            Text("PDF Document", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(8.dp))
            Text("Open this PDF in your device's PDF viewer for the best reading experience.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.size(24.dp))
            Button(onClick = {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(url), "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        uriHandler.openUri(url)
                    }
                }
            }, shape = RoundedCornerShape(16.dp)) { Text("Open PDF"); Spacer(Modifier.size(8.dp)); Icon(FieldMindIcons.OpenLink, null, size = 18.dp) }
        }
    }
}

/**
 * Card for files that cannot be rendered in-app — opens externally.
 */
@Composable
private fun OpenExternallyCard(url: String, uriHandler: androidx.compose.ui.platform.UriHandler, context: android.content.Context) {
    val fileName = url.substringAfterLast("/").substringBefore("?").ifBlank { "File" }
    Card(
        Modifier.fillMaxSize().padding(32.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(FieldMindIcons.File, null, tint = MaterialTheme.colorScheme.primary, size = 64.dp)
            Spacer(Modifier.size(24.dp))
            Text(fileName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.size(8.dp))
            Text("This file type cannot be displayed inside FieldMind. Open it with an external app.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.size(24.dp))
            Button(onClick = {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(url), "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        uriHandler.openUri(url)
                    }
                }
            }, shape = RoundedCornerShape(16.dp)) { Text("Open externally"); Spacer(Modifier.size(8.dp)); Icon(FieldMindIcons.OpenLink, null, size = 18.dp) }
        }
    }
}

@Composable
private fun ReaderFallbackCard(message: String, url: String, onPrimary: () -> Unit, modifier: Modifier = Modifier, onDismiss: () -> Unit = {}) {
    val uriHandler = LocalUriHandler.current
    Card(modifier.padding(20.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                Text("Reader fallback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Icon(FieldMindIcons.Close, contentDescription = "Close reader fallback", size = 18.dp) }
            }
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onPrimary, shape = RoundedCornerShape(14.dp)) { Text("Retry") }
                Button(onClick = { runCatching { uriHandler.openUri(url) } }, shape = RoundedCornerShape(14.dp)) { Text("Open externally") }
            }
        }
    }
}
