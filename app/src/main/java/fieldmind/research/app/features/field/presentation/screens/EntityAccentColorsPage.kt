package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.presentation.components.BackButton
import fieldmind.research.app.features.field.presentation.components.StandardScreenHeader
import fieldmind.research.app.features.field.presentation.components.rememberFieldMindHaptics
import fieldmind.research.app.features.field.data.settings.FieldMindSettings
import fieldmind.research.app.features.field.presentation.theme.*
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

// ── Preset color swatches for the picker ──

private val colorPresets = listOf(
    0xFF2E7D32L, 0xFF1565C0L, 0xFF8B5000L, 0xFF5E35B1L,
    0xFF8E24AAL, 0xFF00897BL, 0xFF6D4C41L, 0xFF43A047L,
    0xFFE91E63L, 0xFF00A86BL, 0xFFE67E22L, 0xFF546E7AL,
    0xFF27AE60L, 0xFFF39C12L, 0xFFE53935L, 0xFF1F6B4CL,
    0xFFD84315L, 0xFF455A64L, 0xFFAD1457L, 0xFF00695CL,
    0xFF2196F3L, 0xFF4CAF50L, 0xFF9C27B0L, 0xFFFF9800L,
    0xFFF44336L, 0xFF607D8BL, 0xFF795548L, 0xFF009688L
)

private val presetLabels = mapOf(
    0xFF2E7D32L to "Observation Green",
    0xFF1565C0L to "Question Blue",
    0xFF8B5000L to "Hypothesis Amber",
    0xFF5E35B1L to "Source Violet",
    0xFF8E24AAL to "Note Purple",
    0xFF00897BL to "Task Teal",
    0xFF6D4C41L to "Folder Brown",
    0xFF43A047L to "Species Green",
    0xFFE91E63L to "Flashcard Pink",
    0xFF00A86BL to "Positive Jade",
    0xFFE67E22L to "Warning Orange",
    0xFF546E7AL to "Info Slate",
    0xFF1F6B4CL to "Brand Green",
    0xFF00695CL to "Project Teal",
    0xFF2196F3L to "Sky Blue",
    0xFF4CAF50L to "Material Green",
    0xFF9C27B0L to "Deep Purple",
    0xFFFF9800L to "Amber",
    0xFF455A64L to "Slate",
    0xFFAD1457L to "Magenta",
    0xFFD84315L to "Burnt Orange",
    0xFF27AE60L to "Confidence Green",
    0xFFF39C12L to "Gold",
    0xFFE53935L to "Verification Red",
    0xFFF44336L to "Red",
    0xFF607D8BL to "Blue Grey",
    0xFF795548L to "Brown",
    0xFF009688L to "Teal"
)

// ══════════════════════════════════════════════════════════════════════
//  Entity Accent Colors Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun EntityAccentColorsPage(
    settings: FieldMindSettings,
    onBack: () -> Unit
) {
    val overrides by settings.entityColors.collectAsState()
    val haptics = rememberFieldMindHaptics()
    var editingKey by remember { mutableStateOf<String?>(null) }
    var tempColor by remember { mutableStateOf(0xFF2E7D32L) }

    // Build the full merged map: defaults + overrides
    val mergedColors = remember(overrides) { DEFAULT_ENTITY_COLORS + overrides }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StandardScreenHeader(
                title = "Entity Accent Colors",
                subtitle = "Customize accent colors for each research entity type",
                icon = MaterialSymbolIcon("palette"),
                trailing = { BackButton(onClick = onBack) }
            )
        }

        // ── Info card ──
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(MaterialSymbolIcon("palette"), null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                        Text("Customize entity colors", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Changes apply immediately across all screens. Colors that aren't customized use the default palette.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Entity color rows ──
        val entityKeys = listOf(
            "observation", "question", "hypothesis", "project", "source",
            "note", "task", "folder", "species", "data", "report", "flashcard",
            "positive", "warning", "info",
            "confidenceSure", "confidenceGuess", "confidenceVerify"
        )

        items(entityKeys, key = { it }) { key ->
            val label = ENTITY_COLOR_LABELS[key] ?: key
            val icon = ENTITY_COLOR_ICONS[key] ?: MaterialSymbolIcon("circle")
            val currentColor = mergedColors[key] ?: 0xFF546E7AL

            val isOverridden = overrides.containsKey(key)
            val isEditing = editingKey == key

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOverridden)
                        Color(currentColor).copy(alpha = 0.06f)
                    else
                        MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = if (isEditing) androidx.compose.foundation.BorderStroke(
                    2.dp, MaterialTheme.colorScheme.primary
                ) else if (isOverridden) androidx.compose.foundation.BorderStroke(
                    1.dp, Color(currentColor).copy(alpha = 0.3f)
                ) else null
            ) {
                if (!isEditing) {
                    // ── Display row ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.light()
                                editingKey = key
                                tempColor = currentColor
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Color swatch
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(currentColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = Color.White, size = 22.dp)
                        }
                        // Label and status
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            if (isOverridden) {
                                Text(
                                    "Customized",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(currentColor)
                                )
                            } else {
                                Text(
                                    "Default",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Hex value
                        Text(
                            "#%06X".format(currentColor and 0xFFFFFF),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Icon(
                            MaterialSymbolIcon("chevron_right"),
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            size = 20.dp
                        )
                    }
                } else {
                    // ── Color picker inline editor ──
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(tempColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null, tint = Color.White, size = 20.dp)
                                }
                                Column {
                                    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "#%06X".format(tempColor and 0xFFFFFF),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(tempColor)
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Reset to default button
                                val defaultColor = DEFAULT_ENTITY_COLORS[key] ?: 0xFF546E7AL
                                if (tempColor != defaultColor) {
                                    Surface(
                                        onClick = { tempColor = defaultColor },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Icon(MaterialSymbolIcon("restart_alt"), "Reset", size = 18.dp, modifier = Modifier.padding(8.dp))
                                    }
                                }
                                // Apply button
                                Surface(
                                    onClick = {
                                        haptics.confirm()
                                        val updated = overrides.toMutableMap()
                                        updated[key] = tempColor
                                        settings.setEntityColors(updated)
                                        editingKey = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(tempColor)
                                ) {
                                    Icon(MaterialSymbolIcon("check"), "Apply", tint = Color.White, size = 18.dp, modifier = Modifier.padding(8.dp))
                                }
                                // Cancel
                                Surface(
                                    onClick = { editingKey = null },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Icon(MaterialSymbolIcon("close"), "Cancel", size = 18.dp, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }

                        // ── Color preset grid ──
                        Text("Presets", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        // Grid of color swatches
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            colorPresets.chunked(7).forEach { row ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    row.forEach { colorLong ->
                                        val isSelected = tempColor == colorLong
                                        val colorName = presetLabels[colorLong] ?: ""
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(colorLong))
                                                .then(
                                                    if (isSelected) Modifier.border(
                                                        3.dp, MaterialTheme.colorScheme.onSurface,
                                                        RoundedCornerShape(10.dp)
                                                    ) else Modifier
                                                )
                                                .clickable {
                                                    haptics.light()
                                                    tempColor = colorLong
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    MaterialSymbolIcon("check"),
                                                    null,
                                                    tint = if (Color(colorLong).luminance() > 0.5f) Color.Black else Color.White,
                                                    size = 18.dp
                                                )
                                            } else {
                                                // Show tooltip-like label on long press not available,
                                                // but show a faint label on the color
                                            }
                                        }
                                    }
                                    // Fill remaining space in the row
                                    repeat(7 - row.size) {
                                        Spacer(Modifier.size(40.dp))
                                    }
                                }
                            }
                        }

                        // ── Custom hex input ──
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Custom hex:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = "#%06X".format(tempColor and 0xFFFFFF),
                                onValueChange = { input ->
                                    val hex = input.trimStart('#').take(6)
                                    if (hex.length == 6 && hex.all { it in "0123456789abcdefABCDEF" }) {
                                        tempColor = (0xFF000000L or hex.toLong(16))
                                    }
                                },
                                modifier = Modifier.width(120.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // ── Reset all button ──
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    haptics.confirm()
                    settings.setEntityColors(emptyMap())
                    editingKey = null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(MaterialSymbolIcon("restart_alt"), null, size = 18.dp)
                Spacer(Modifier.size(8.dp))
                Text("Reset All to Defaults", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
