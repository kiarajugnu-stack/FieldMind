package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fieldmind.research.app.features.field.data.species.TaxonomyData
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

/**
 * A filterable taxonomy picker dialog. Shows a search field at the top and
 * a scrollable list of taxonomy options. Supports context-aware suggestions
 * when a parent rank is provided (e.g. only phyla from a selected kingdom).
 *
 * @param rank The taxonomic rank being picked ("kingdom", "phylum", "class", "order", "family", "genus")
 * @param currentValue The currently selected value (pre-filled)
 * @param parentValue The parent rank value for context-aware filtering (e.g. kingdom when picking phylum)
 * @param onSelected Called when the user confirms a selection
 * @param onDismiss Called when the dialog is dismissed
 */
@Composable
fun TaxonomyPickerDialog(
    rank: String,
    currentValue: String,
    parentValue: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val rankLabel = rank.replaceFirstChar { it.uppercase() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedValue by remember { mutableStateOf(currentValue) }

    // Get options based on rank and parent context
    val options = remember(rank, parentValue) {
        when (rank) {
            "kingdom" -> TaxonomyData.KINGDOMS
            "phylum" -> TaxonomyData.phylaForKingdom(parentValue)
            "class" -> TaxonomyData.classesForPhylum(parentValue)
            "order" -> TaxonomyData.ordersForClass(parentValue)
            "family" -> TaxonomyData.familiesForOrder(parentValue)
            else -> TaxonomyData.ALL_VALUES[rank] ?: emptyList()
        }
    }

    val filteredOptions = remember(options, searchQuery) {
        if (searchQuery.isBlank()) options
        else options.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Select $rankLabel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (parentValue.isNotBlank()) {
                            Text(
                                "Showing $rankLabel in $parentValue",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(FieldMindIcons.Close, null, size = 22.dp)
                    }
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search $rankLabel...") },
                    leadingIcon = { Icon(FieldMindIcons.Search, null, size = 20.dp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Options count
                if (filteredOptions.isNotEmpty()) {
                    Text(
                        "${filteredOptions.size} ${rankLabel.lowercase()} found",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Options list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (filteredOptions.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        FieldMindIcons.Search,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        size = 32.dp
                                    )
                                    Text(
                                        "No matching $rankLabel",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Try a different search term",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    items(filteredOptions, key = { it }) { option ->
                        val isSelected = selectedValue == option
                        Surface(
                            onClick = {
                                selectedValue = option
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    option,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        FieldMindIcons.Check,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        size = 20.dp
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom action row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        // Clear selection
                        onSelected("")
                        onDismiss()
                    }) {
                        Text("Clear")
                    }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = {
                            onSelected(selectedValue)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        enabled = selectedValue.isNotBlank()
                    ) {
                        Text("Select $rankLabel")
                    }
                }
            }
        }
    }
}

/**
 * A text field for entering taxonomic names that don't have predefined datasets
 * (like genus, which has too many values for a picker dialog).
 */
@Composable
fun TaxonTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("Enter $label...") },
        supportingText = supportingText?.let { { Text(it) } },
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}

/**
 * A clickable taxonomy field that opens the picker dialog.
 * Shows the selected value and a dropdown hint icon.
 */
@Composable
fun TaxonomyPickerField(
    label: String,
    value: String,
    rank: String,
    parentValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        onClick = { showPicker = true },
        shape = RoundedCornerShape(14.dp),
        color = if (value.isNotBlank())
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (value.isNotBlank()) value else "Select $label...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (value.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (value.isNotBlank()) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Icon(
                FieldMindIcons.Down,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 18.dp
            )
        }
    }

    if (showPicker) {
        TaxonomyPickerDialog(
            rank = rank,
            currentValue = value,
            parentValue = parentValue,
            onSelected = onValueChange,
            onDismiss = { showPicker = false }
        )
    }
}
