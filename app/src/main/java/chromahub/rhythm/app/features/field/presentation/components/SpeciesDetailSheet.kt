package fieldmind.research.app.features.field.presentation.components

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fieldmind.research.app.features.field.data.vision.SpeciesRecord
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

// ══════════════════════════════════════════════════════════════════════
//  Species Detail Bottom Sheet — Full species info for observation form
// ══════════════════════════════════════════════════════════════════════

@Composable
fun SpeciesDetailSheet(
    record: SpeciesRecord,
    onDismiss: () -> Unit
) {
    val accent = categoryColor(record.category)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Scrim (tap to dismiss)
            Box(
                Modifier.fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
                    .clickable { onDismiss() }
            )

            // Sheet content
            Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ── Drag handle ──
                    Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.width(40.dp).height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        )
                    }

                    // ── Header ──
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            Modifier.size(56.dp).clip(RoundedCornerShape(18.dp))
                                .background(accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(FieldMindIcons.Nature, null, tint = accent, size = 30.dp)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Category badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = accent.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    record.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                record.commonName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (record.scientificName.isNotBlank()) {
                                Text(
                                    record.scientificName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // ── Description ──
                    if (record.description.isNotBlank()) {
                        DetailField(title = "Description", icon = FieldMindIcons.Info) {
                            Text(
                                record.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }

                    // ── Full Taxonomy ──
                    TaxonomySection(record = record)

                    // ── Habitat & Diet ──
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (record.habitat.isNotBlank()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(FieldMindIcons.Nature, null, tint = accent, size = 20.dp)
                                    Text("Habitat", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(record.habitat, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (record.diet.isNotBlank()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(FieldMindIcons.Water, null, tint = accent, size = 20.dp)
                                    Text("Diet", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(record.diet, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // ── Conservation Status ──
                    if (record.conservationStatus.isNotBlank()) {
                        val statusColor = when {
                            record.conservationStatus.contains("Endangered", ignoreCase = true) || record.conservationStatus.contains("Critically", ignoreCase = true) -> MaterialTheme.colorScheme.error
                            record.conservationStatus.contains("Vulnerable", ignoreCase = true) || record.conservationStatus.contains("Near", ignoreCase = true) -> FieldMindTheme.colors.warning
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(FieldMindIcons.Info, null, tint = statusColor, size = 16.dp)
                            Text(
                                record.conservationStatus,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                        }
                    }

                    // ── Close button ──
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Close", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Taxonomy Section — Full hierarchical taxonomy display
// ══════════════════════════════════════════════════════════════════════

@Composable
fun TaxonomySection(
    record: SpeciesRecord,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val colors = FieldMindTheme.colors

    val taxonomyLevels = buildList {
        record.kingdom.takeIf { it.isNotBlank() }?.let { add("Kingdom" to it) }
        record.phylum.takeIf { it.isNotBlank() }?.let { add("Phylum" to it) }
        record.order.takeIf { it.isNotBlank() }?.let { add("Order" to it) }
        record.family.takeIf { it.isNotBlank() }?.let { add("Family" to it) }
        record.genus.takeIf { it.isNotBlank() }?.let { add("Genus" to it) }
        record.scientificName.takeIf { it.isNotBlank() }?.let { add("Species" to it) }
    }

    if (taxonomyLevels.isEmpty()) return

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(FieldMindIcons.Category, null, tint = colors.observation, size = 18.dp)
                Text("Taxonomy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    "${taxonomyLevels.size} levels",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            taxonomyLevels.forEachIndexed { index, (level, value) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Level label
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.observation.copy(alpha = 0.08f)
                    ) {
                        Text(
                            level,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.observation,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    // Value
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (level == "Species") FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // Connector line
                    if (index < taxonomyLevels.size - 1) {
                        Box(
                            Modifier.width(1.dp).height(12.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Species Info Card — Compact inline card for observation form
// ══════════════════════════════════════════════════════════════════════

@Composable
fun SpeciesInfoCard(
    record: SpeciesRecord,
    showTaxonomy: Boolean,
    onToggleTaxonomy: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val colors = FieldMindTheme.colors
    val accent = categoryColor(record.category)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.observation.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.Nature, null, tint = accent, size = 22.dp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        record.commonName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (record.scientificName.isNotBlank()) {
                        Text(
                            record.scientificName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // Detail button
                Surface(
                    onClick = onOpenDetail,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(FieldMindIcons.Info, null, tint = accent, size = 18.dp)
                    }
                }
            }

            // Taxonomy toggle
            if (showTaxonomy) {
                TaxonomySection(record = record)
            }

            // Description snippet
            if (record.description.isNotBlank()) {
                Text(
                    record.description.take(100).replace("\n", " ") + if (record.description.length > 100) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Toggle taxonomy button
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Genus + species compact display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (record.genus.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.info.copy(alpha = 0.1f)
                        ) {
                            Text(
                                record.genus,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.info,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (record.conservationStatus.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = if (record.conservationStatus.contains("Least Concern", ignoreCase = true)) 0.0f else 0.1f)
                        ) {
                            Text(
                                record.conservationStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                TextButton(onClick = onToggleTaxonomy) {
                    Icon(
                        icon = if (showTaxonomy) FieldMindIcons.Up else FieldMindIcons.Down,
                        contentDescription = null,
                        size = 14.dp
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        if (showTaxonomy) "Hide taxonomy" else "Show taxonomy",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

// ── Category color helper ──
@Composable
private fun categoryColor(category: String): androidx.compose.ui.graphics.Color {
    val colors = FieldMindTheme.colors
    return when {
        category.equals("Bird", ignoreCase = true) -> colors.info
        category.equals("Mammal", ignoreCase = true) -> colors.warning
        category.equals("Plant", ignoreCase = true) -> colors.info
        category.equals("Insect", ignoreCase = true) -> colors.data
        category.equals("Fungi", ignoreCase = true) -> colors.report
        category.equals("Reptile", ignoreCase = true) -> colors.hypothesis
        category.equals("Amphibian", ignoreCase = true) -> colors.project
        category.equals("Fish", ignoreCase = true) -> colors.info
        category.equals("Arachnid", ignoreCase = true) -> colors.warning
        category.equals("Crustacean", ignoreCase = true) -> colors.data
        category.equals("Mollusk", ignoreCase = true) -> colors.report
        else -> MaterialTheme.colorScheme.primary
    }
}

// ── Detail field helper ──
@Composable
private fun DetailField(
    title: String,
    icon: MaterialSymbolIcon? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (icon != null || title.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, size = 16.dp)
                }
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
        content()
    }
}
