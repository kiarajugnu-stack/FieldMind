package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Phase 12: Library/Sources - Native PDF Viewer & Source Management
 */

enum class SourceType(val displayName: String, val icon: String) {
    PDF("PDF", "📄"),
    IMAGE("Image", "🖼️"),
    AUDIO("Audio", "🎙️"),
    VIDEO("Video", "🎬"),
    DOCUMENT("Document", "📋"),
    SPREADSHEET("Spreadsheet", "📊"),
    PRESENTATION("Presentation", "📈"),
    WEB_LINK("Web Link", "🔗")
}

/**
 * Source Card - displays a source with metadata
 */
@Composable
fun SourceCard(
    title: String,
    author: String,
    sourceType: String,
    dateOrYear: String,
    highlights: Int = 0,
    credibility: String = "Normal",
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📚", style = MaterialTheme.typography.titleMedium)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(author.ifBlank { "Unknown author" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                AssistChip(onClick = {}, label = { Text(sourceType) })
                AssistChip(onClick = {}, label = { Text(dateOrYear.ifBlank { "Date unknown" }) })
                if (highlights > 0) {
                    AssistChip(onClick = {}, label = { Text("$highlights highlights") })
                }
                AssistChip(onClick = {}, label = { Text(credibility) })
            }
        }
    }
}

/**
 * Highlight/Annotation Card - displays a highlight from a source
 */
@Composable
fun HighlightCard(
    text: String,
    page: Int? = null,
    note: String = "",
    tags: List<String> = emptyList(),
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("❝", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.tertiary)
                Column(Modifier.weight(1f)) {
                    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer, maxLines = 3)
                    if (page != null) {
                        Text("p. $page", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f))
                    }
                }
            }

            if (note.isNotBlank()) {
                Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, maxLines = 2)
            }

            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    tags.forEach { tag ->
                        AssistChip(onClick = {}, label = { Text(tag) })
                    }
                }
            }
        }
    }
}

/**
 * Source Metadata Panel - displays detailed source information
 */
@Composable
fun SourceMetadataPanel(
    title: String,
    author: String,
    publisher: String,
    dateOrYear: String,
    doi: String? = null,
    isbn: String? = null,
    credibility: String = "Normal",
    personalSummary: String = "",
    keyFindings: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Source Information", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

            MetadataRow("Title", title)
            MetadataRow("Author", author.ifBlank { "Unknown" })
            if (publisher.isNotBlank()) MetadataRow("Publisher", publisher)
            MetadataRow("Date", dateOrYear)
            if (doi != null && doi.isNotBlank()) MetadataRow("DOI", doi)
            if (isbn != null && isbn.isNotBlank()) MetadataRow("ISBN", isbn)
            MetadataRow("Credibility", credibility)

            if (personalSummary.isNotBlank()) {
                Divider()
                Text("My Summary", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Text(personalSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (keyFindings.isNotBlank()) {
                Divider()
                Text("Key Findings", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Text(keyFindings, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(100.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
    }
}

/**
 * Knowledge Extraction Menu - actions after highlighting text
 */
@Composable
fun KnowledgeExtractionMenu(
    onCreateNote: () -> Unit,
    onCreateFlashcard: () -> Unit,
    onCreateQuestion: () -> Unit,
    onAddToProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Extract as", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ExtractionButton("📝 Note", onClick = onCreateNote, Modifier.weight(1f))
                ExtractionButton("📇 Card", onClick = onCreateFlashcard, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ExtractionButton("❓ Q&A", onClick = onCreateQuestion, Modifier.weight(1f))
                ExtractionButton("📌 Project", onClick = onAddToProject, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ExtractionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(36.dp), shape = RoundedCornerShape(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Backlinks Panel - shows where a source is referenced
 */
@Composable
fun BacklinksPanel(
    noteReferences: Int = 0,
    projectReferences: Int = 0,
    hypothesisReferences: Int = 0,
    flashcardReferences: Int = 0,
    onNoteReferencesClick: () -> Unit = {},
    onProjectReferencesClick: () -> Unit = {},
    onHypothesisReferencesClick: () -> Unit = {},
    onFlashcardReferencesClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Referenced in", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (noteReferences > 0) {
                    BacklinkChip("$noteReferences notes", onClick = onNoteReferencesClick, Modifier.weight(1f))
                }
                if (projectReferences > 0) {
                    BacklinkChip("$projectReferences projects", onClick = onProjectReferencesClick, Modifier.weight(1f))
                }
            }
            
            if (hypothesisReferences > 0 || flashcardReferences > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hypothesisReferences > 0) {
                        BacklinkChip("$hypothesisReferences hypotheses", onClick = onHypothesisReferencesClick, Modifier.weight(1f))
                    }
                    if (flashcardReferences > 0) {
                        BacklinkChip("$flashcardReferences cards", onClick = onFlashcardReferencesClick, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BacklinkChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
