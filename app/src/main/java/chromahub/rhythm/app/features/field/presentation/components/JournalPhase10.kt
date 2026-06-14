package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Phase 10: Notes/Journal Redesign - Rich Text and Block-based Editing
 */

enum class JournalBlockType(val displayName: String, val icon: String) {
    TEXT("Text", "📝"),
    IMAGE("Image", "🖼️"),
    DRAWING("Drawing", "✏️"),
    AUDIO("Audio", "🎙️"),
    OBSERVATION("Observation", "👁️"),
    CHECKLIST("Checklist", "☑️"),
    QUOTE("Quote", "❝"),
    TABLE("Table", "📊"),
    MAP("Map", "🗺️"),
    LINK("Reference", "🔗"),
    HANDWRITTEN("Handwritten", "✍️")
}

/**
 * Journal/Note Editor Header
 */
@Composable
fun JournalEditorHeader(
    title: String,
    onTitleChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text("Untitled note...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category,
                    onValueChange = onCategoryChange,
                    label = { Text("Category") },
                    modifier = Modifier.weight(0.4f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = onTagsChange,
                    label = { Text("Tags") },
                    modifier = Modifier.weight(0.6f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        }
    }
}

/**
 * Journal Block Palette - add different block types
 */
@Composable
fun JournalBlockPalette(
    onBlockAdd: (JournalBlockType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (expanded) "Hide blocks" else "+ Add block", fontWeight = FontWeight.SemiBold)
        }

        if (expanded) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(JournalBlockType.values()) { blockType ->
                        BlockTypeButton(blockType) { onBlockAdd(blockType) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockTypeButton(blockType: JournalBlockType, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(blockType.icon, style = MaterialTheme.typography.titleLarge)
            Text(blockType.displayName.take(4), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

/**
 * Journal Entry Card - for displaying notes in list
 */
@Composable
fun JournalEntryCard(
    title: String,
    preview: String,
    date: String,
    category: String = "",
    tags: List<String> = emptyList(),
    blockCount: Int = 0,
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(title.ifBlank { "Untitled" }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (blockCount > 0) {
                    Badge { Text("$blockCount blocks") }
                }
            }

            if (preview.isNotBlank()) {
                Text(preview.take(100), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                if (category.isNotBlank()) {
                    AssistChip(onClick = {}, label = { Text(category) })
                }
                tags.take(2).forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag) })
                }
                if (tags.size > 2) {
                    AssistChip(onClick = {}, label = { Text("+${tags.size - 2}") })
                }
            }
        }
    }
}

/**
 * Rich Text Formatting Toolbar
 */
@Composable
fun RichTextFormattingToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrikethrough: () -> Unit,
    onLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBold, modifier = Modifier.size(36.dp)) { Text("B", fontWeight = FontWeight.Bold) }
            IconButton(onClick = onItalic, modifier = Modifier.size(36.dp)) { Text("I", fontWeight = FontWeight.Bold) }
            IconButton(onClick = onUnderline, modifier = Modifier.size(36.dp)) { Text("U", fontWeight = FontWeight.Bold) }
            IconButton(onClick = onStrikethrough, modifier = Modifier.size(36.dp)) { Text("S", fontWeight = FontWeight.Bold) }
            Divider(modifier = Modifier
                .width(1.dp)
                .height(24.dp))
            IconButton(onClick = onLink, modifier = Modifier.size(36.dp)) { Text("🔗") }
        }
    }
}

/**
 * Observation Embed Block - for embedding live observation data
 */
@Composable
fun ObservationEmbedBlock(
    observationId: Long,
    subject: String,
    date: String,
    facts: String,
    confidence: String,
    onRemove: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("👁️", style = MaterialTheme.typography.titleMedium)
                Text("Observation", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) { Text("✕", fontWeight = FontWeight.Bold) }
            }
            
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(subject, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(facts.take(80), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                AssistChip(onClick = {}, label = { Text("Confidence: $confidence") })
            }
        }
    }
}
