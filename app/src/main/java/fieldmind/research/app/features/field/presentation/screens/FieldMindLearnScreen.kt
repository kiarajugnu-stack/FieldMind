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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.learn.LearnCategory
import fieldmind.research.app.features.field.data.learn.LearnLibrary
import fieldmind.research.app.features.field.data.learn.LearnResource
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.SectionHeader
import fieldmind.research.app.features.field.presentation.components.BackButton
import fieldmind.research.app.features.field.presentation.components.StandardScreenHeader
import fieldmind.research.app.features.field.presentation.components.rememberFieldMindHaptics
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

private val beginnerResearchMilestones = listOf(
    ResearchMilestone("Observe carefully", "Separate facts from interpretation, then document time, place, context, and evidence.", FieldMindIcons.Observation, LearnResource("Understanding Science", "Guide", "https://undsci.berkeley.edu/", "Science starts with careful observation and honest uncertainty.")),
    ResearchMilestone("Ask researchable questions", "Turn curiosity into a question you can observe, compare, measure, or verify.", FieldMindIcons.Question, LearnResource("Research as inquiry", "Framework", "https://www.ala.org/acrl/standards/ilframework", "Research grows from increasingly focused questions.")),
    ResearchMilestone("Evaluate sources", "Use DOI/ISBN, venue, author, and evidence quality before trusting a claim.", FieldMindIcons.Source, LearnResource("Crossref REST API", "Reference", "https://www.production.crossref.org/documentation/retrieve-metadata/rest-api/", "Look up DOI metadata and verify bibliographic details.")),
    ResearchMilestone("Plan a small investigation", "Define method, site, sample, bias controls, and safety before collecting data.", FieldMindIcons.Project, LearnResource("Framework for Science Education", "Guide", "https://nap.nationalacademies.org/resource/13165/interactive/", "Science practices include planning investigations and analyzing evidence.")),
    ResearchMilestone("Collect usable data", "Record measurements, counts, checklists, and metadata consistently.", FieldMindIcons.Data, LearnResource("OpenIntro Statistics", "Book", "https://www.openintro.org/book/os/", "A free introduction to variation, sampling, and data summaries.")),
    ResearchMilestone("Explain with evidence", "Write claim, evidence, reasoning, limits, and next questions without overstating certainty.", FieldMindIcons.Report, LearnResource("Purdue OWL citation resources", "Guide", "https://owl.purdue.edu/owl/research_and_citation/resources.html", "Guides for writing, citing, and communicating research."))
)

// ══════════════════════════════════════════════════════════════════════
//  REDESIGNED LEARN SCREEN
//  Layout: Hero → Personalized recs → Compact collapsible categories
// ══════════════════════════════════════════════════════════════════════

@Composable
fun FieldMindLearnScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit = {},
    onOpenReader: (String, String) -> Unit = { _, _ -> },
    onNavigateToLibrary: () -> Unit = {}
) {
    val observations by viewModel.observations.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val haptics = rememberFieldMindHaptics()

    // ── Compute signals from user activity ──
    val signals = remember(observations, questions, sources, projects) {
        buildList {
            observations.take(10).forEach { add(it.category); add(it.tags); add(it.subject) }
            questions.take(8).forEach { add(it.category); add(it.questionText) }
            sources.take(8).forEach { add(it.type); add(it.title); add(it.publisherOrJournal) }
            projects.take(6).forEach { add(it.topicType); add(it.title) }
        }.joinToString(" ")
    }

    // ── Pick the personalized next milestone ──
    val nextMilestone = remember(observations.size, questions.size, sources.size, projects.size, reports.size) {
        when {
            observations.isEmpty() -> beginnerResearchMilestones[0]
            questions.isEmpty() -> beginnerResearchMilestones[1]
            sources.isEmpty() -> beginnerResearchMilestones[2]
            projects.isEmpty() -> beginnerResearchMilestones[3]
            reports.isEmpty() -> beginnerResearchMilestones[5]
            else -> beginnerResearchMilestones[4]
        }
    }

    // ── Personalized recommendations ──
    val recommendations = remember(signals) {
        recommendedResources(listOf(signals))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Screen header ──
        item {
            StandardScreenHeader(
                title = "Learn",
                subtitle = "Discover resources matched to your research journey.",
                icon = FieldMindIcons.School,
                trailing = {
                    BackButton(onClick = onBack, shape = RoundedCornerShape(12.dp), containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f), contentDescription = "Back")
                }
            )
        }

        // ── Section 1: Recommended next step (personalized hero) ──
        item { NextStepHero(milestone = nextMilestone, signals = signals, onOpenReader = onOpenReader) }

        // ── Section 2: Based on your activity ──
        if (recommendations.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Based on your activity",
                    subtitle = if (signals.isBlank()) "Start capturing to personalize this section"
                    else "Recommended from recent observations and sources"
                )
            }
            items(recommendations) { rec ->
                ActivityRecommendationCard(
                    title = rec.resource.title,
                    kind = rec.resource.kind,
                    description = rec.resource.why,
                    url = rec.resource.url,
                    onClick = { onOpenReader(rec.resource.url, rec.resource.title) }
                )
            }
        }

        // ── Section 3: Learn categories (collapsible) ──
        item {
            SectionHeader(
                title = "Learn categories",
                subtitle = "${LearnLibrary.size} topics — tap to expand"
            )
        }
        items(LearnLibrary) { category ->
            LearnCategoryCardCompact(
                category = category,
                onOpenResource = { res -> onOpenReader(res.url, res.title) }
            )
        }

        // ── Bottom spacer ──
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  HERO: Recommended Next Step
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun NextStepHero(
    milestone: ResearchMilestone,
    signals: String,
    onOpenReader: (String, String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Label + icon row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon = milestone.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = 26.dp
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Recommended next step",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    )
                    Text(
                        milestone.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Description
            Text(
                milestone.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
            )

            // Personalized badge
            if (signals.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            icon = FieldMindIcons.Sparkle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            size = 14.dp
                        )
                        Text(
                            "Personalized from recent activity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // CTA button
            Button(
                onClick = { onOpenReader(milestone.resource.url, milestone.resource.title) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Open starter resource")
                Spacer(Modifier.size(6.dp))
                Icon(icon = FieldMindIcons.Forward, contentDescription = null, size = 18.dp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  ACTIVITY RECOMMENDATION CARD
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ActivityRecommendationCard(
    title: String,
    kind: String,
    description: String,
    url: String,
    onClick: () -> Unit
) {
    val accent = FieldMindTheme.colors.accentFor("learn")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail / icon box
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon = learnKindIcon(kind),
                    contentDescription = null,
                    tint = accent,
                    size = 24.dp
                )
            }

            // Text content
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            kind,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            Icon(
                icon = FieldMindIcons.Forward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                size = 20.dp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  COMPACT LEARN CATEGORY CARD
//  Smaller, cleaner version of the original LearnCategoryCard
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun LearnCategoryCardCompact(
    category: LearnCategory,
    onOpenResource: (LearnResource) -> Unit
) {
    var expanded by rememberSaveable(category.name) { mutableStateOf(false) }
    val accent = FieldMindTheme.colors.accentFor(category.name)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Category header row (always visible) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = !expanded
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Compact icon — 36dp instead of 44dp
                Box(
                    Modifier
                        .size(36.dp)
                        .background(
                            accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f),
                            RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon = FieldMindIcons.School,
                        contentDescription = null,
                        tint = accent,
                        size = 20.dp
                    )
                }

                // Title + description
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        category.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) 3 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Topic count badge + expand arrow
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                    ) {
                        Text(
                            "${category.topics.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Icon(
                        icon = if (expanded) FieldMindIcons.Up else FieldMindIcons.Down,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        size = 18.dp
                    )
                }
            }

            // ── Expanded topics ──
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        category.topics.forEach { topic ->
                            TopicCard(topic = topic, accent = accent, onOpenResource = onOpenResource)
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  TOPIC CARD (inside expanded category)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun TopicCard(
    topic: fieldmind.research.app.features.field.data.learn.LearnTopic,
    accent: androidx.compose.ui.graphics.Color,
    onOpenResource: (LearnResource) -> Unit
) {
    var topicExpanded by rememberSaveable(topic.name) { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { topicExpanded = !topicExpanded }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Topic header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon = if (topicExpanded) FieldMindIcons.Down else FieldMindIcons.Up,
                        contentDescription = null,
                        tint = accent,
                        size = 14.dp
                    )
                }
                Text(
                    topic.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accent.copy(alpha = 0.10f)
                ) {
                    Text(
                        "${topic.resources.size} resources",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                topic.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (topicExpanded) 6 else 1,
                overflow = TextOverflow.Ellipsis
            )

            // Resources
            AnimatedVisibility(visible = topicExpanded) {
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    topic.resources.forEach { resource ->
                        ResourceRow(resource = resource, accent = accent, onClick = { onOpenResource(resource) })
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  RESOURCE ROW (individual link within a topic)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ResourceRow(
    resource: LearnResource,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon = learnKindIcon(resource.kind),
            contentDescription = null,
            tint = accent,
            size = 18.dp
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                resource.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    resource.kind,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                if (resource.author.isNotBlank()) {
                    Text(
                        "· ${resource.author}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
        Icon(
            icon = FieldMindIcons.OpenLink,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            size = 16.dp
        )
    }
}
