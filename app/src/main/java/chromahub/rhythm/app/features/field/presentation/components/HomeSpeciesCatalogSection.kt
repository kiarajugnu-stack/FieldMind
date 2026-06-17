package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.vision.SpeciesDatabase
import fieldmind.research.app.features.field.data.vision.SpeciesRecord
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color

// ── Category accent colors (mirrors SpeciesBrowserScreen) ──
@Composable
private fun categoryColor(category: String): Color {
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

@Composable
private fun categoryIcon(category: String): MaterialSymbolIcon = when {
    category.equals("Bird", ignoreCase = true) -> FieldMindIcons.Bird
    category.equals("Mammal", ignoreCase = true) -> FieldMindIcons.Nature
    category.equals("Plant", ignoreCase = true) -> FieldMindIcons.Plant
    category.equals("Insect", ignoreCase = true) -> FieldMindIcons.Insect
    category.equals("Fungi", ignoreCase = true) -> FieldMindIcons.Plant
    category.equals("Reptile", ignoreCase = true) -> FieldMindIcons.Nature
    category.equals("Amphibian", ignoreCase = true) -> FieldMindIcons.Nature
    category.equals("Fish", ignoreCase = true) -> FieldMindIcons.Water
    category.equals("Arachnid", ignoreCase = true) -> FieldMindIcons.Insect
    category.equals("Crustacean", ignoreCase = true) -> FieldMindIcons.Water
    category.equals("Mollusk", ignoreCase = true) -> FieldMindIcons.Water
    else -> FieldMindIcons.Nature
}

// ══════════════════════════════════════════════════════════════════════
//  Home Species Catalog Section — Browsable catalog on the home screen
// ══════════════════════════════════════════════════════════════════════

@Composable
fun HomeSpeciesCatalogSection(
    onNavigate: (FieldMindScreen) -> Unit
) {
    val context = LocalContext.current
    val database = remember { SpeciesDatabase(context) }

    var allSpecies by remember { mutableStateOf<List<SpeciesRecord>>(emptyList()) }
    var kingdoms by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedKingdom by remember { mutableStateOf<String?>(null) }
    var selectedGenus by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var totalCount by remember { mutableStateOf(0) }

    // Load all species and kingdoms on first composition
    LaunchedEffect(Unit) {
        val records = database.search("", limit = 300)
        allSpecies = records
        totalCount = records.size
        kingdoms = records.map { it.kingdom }.filter { it.isNotBlank() }.distinct().sorted()
        isLoading = false
    }

    // Filtered species based on selected kingdom and genus
    val filteredSpecies = remember(selectedKingdom, selectedGenus, allSpecies) {
        allSpecies.filter { s ->
            (selectedKingdom == null || s.kingdom.equals(selectedKingdom, ignoreCase = true)) &&
            (selectedGenus == null || s.genus.equals(selectedGenus, ignoreCase = true))
        }
    }

    // Genera available for the selected kingdom
    val genera = remember(selectedKingdom, allSpecies) {
        allSpecies
            .filter { selectedKingdom == null || it.kingdom.equals(selectedKingdom, ignoreCase = true) }
            .map { it.genus }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .take(24)
    }

    // Reset genus when kingdom changes
    LaunchedEffect(selectedKingdom) { selectedGenus = null }

    val colors = FieldMindTheme.colors
    val accentColor = colors.observation

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(300))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // ── Header ──
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.Nature, null, tint = accentColor, size = 22.dp)
                }
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Species catalog", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (!isLoading) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = accentColor.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "$totalCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text("Browse by kingdom and genus", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Browse all button
                TextButton(onClick = { onNavigate(FieldMindScreen.SpeciesBrowser) }) {
                    Text("Browse")
                }
            }

            if (isLoading) {
                Box(
                    Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            } else {
                // ── Kingdom filter chips ──
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "All kingdoms" chip
                    FilterChip(
                        selected = selectedKingdom == null,
                        onClick = { selectedKingdom = null },
                        label = { Text("All", fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(20.dp)
                    )
                    kingdoms.forEach { kingdom ->
                        val isSelected = selectedKingdom == kingdom
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedKingdom = if (isSelected) null else kingdom },
                            label = { Text(kingdom, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.12f),
                                selectedLabelColor = accentColor
                            )
                        )
                    }
                }

                // ── Genus filter chips (when kingdom selected) ──
                if (selectedKingdom != null && genera.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // "All genera" chip
                        FilterChip(
                            selected = selectedGenus == null,
                            onClick = { selectedGenus = null },
                            label = { Text("All genera", fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.info.copy(alpha = 0.12f),
                                selectedLabelColor = colors.info
                            )
                        )
                        genera.forEach { genus ->
                            val isGenusSelected = selectedGenus == genus
                            FilterChip(
                                selected = isGenusSelected,
                                onClick = { selectedGenus = if (isGenusSelected) null else genus },
                                label = { Text(genus, fontWeight = FontWeight.Medium, maxLines = 1) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.info.copy(alpha = 0.12f),
                                    selectedLabelColor = colors.info
                                )
                            )
                        }
                    }
                }

                // ── Species cards (up to 5) ──
                val displaySpecies = filteredSpecies.take(5)
                if (displaySpecies.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(FieldMindIcons.Nature, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), size = 32.dp)
                            Text(
                                "No species match your filters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        displaySpecies.forEach { record ->
                            HomeSpeciesCard(
                                record = record,
                                onClick = { onNavigate(FieldMindScreen.SpeciesBrowser) }
                            )
                        }
                    }

                    // ── View all link ──
                    if (filteredSpecies.size > 5) {
                        TextButton(
                            onClick = { onNavigate(FieldMindScreen.SpeciesBrowser) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("+${filteredSpecies.size - 5} more")
                            Spacer(Modifier.size(4.dp))
                            Icon(FieldMindIcons.Forward, null, size = 16.dp)
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Home Species Card — Compact inline species display
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun HomeSpeciesCard(
    record: SpeciesRecord,
    onClick: () -> Unit
) {
    val accent = categoryColor(record.category)
    val colors = FieldMindTheme.colors

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon badge
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIcon(record.category), null, tint = accent, size = 22.dp)
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Common name + scientific name
                Text(
                    record.commonName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (record.scientificName.isNotBlank()) {
                        Text(
                            record.scientificName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Taxonomy badge
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
                }
                // Description snippet
                if (record.description.isNotBlank()) {
                    Text(
                        record.description.take(80).replace("\n", " ") + if (record.description.length > 80) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Forward indicator
            Icon(
                FieldMindIcons.Forward,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                size = 16.dp
            )
        }
    }
}


