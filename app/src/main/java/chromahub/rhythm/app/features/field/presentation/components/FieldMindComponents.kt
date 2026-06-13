package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon


@Composable
fun rememberFieldMindHaptics(): FieldMindHaptics {
    val haptics = LocalHapticFeedback.current
    return FieldMindHaptics(
        light = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
        confirm = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
    )
}

class FieldMindHaptics internal constructor(
    val light: () -> Unit,
    val confirm: () -> Unit
)

// ──────────────────────────────────────────────────────────────────────
//  Headers
// ──────────────────────────────────────────────────────────────────────

/** Large screen header with optional leading icon and a trailing icon action. */
@Composable
fun FieldScreenHeader(
    title: String,
    subtitle: String? = null,
    icon: MaterialSymbolIcon? = null,
    actionIcon: MaterialSymbolIcon? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 24.dp) }
            Spacer(Modifier.size(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (actionIcon != null && onAction != null) {
            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(44.dp)
            ) { Box(contentAlignment = Alignment.Center) { Icon(icon = actionIcon, contentDescription = title, size = 22.dp) } }
        }
    }
}

/** In-list section header. */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (trailing != null) trailing()
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Badges & chips
// ──────────────────────────────────────────────────────────────────────

/** Pill badge for an entity kind, colored by its semantic accent and prefixed with its icon. */
@Composable
fun EntityBadge(kind: String, modifier: Modifier = Modifier) {
    val accent = FieldMindTheme.colors.accentFor(kind)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f),
        contentColor = accent
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon = FieldMindIcons.iconFor(kind), contentDescription = null, tint = accent, size = 14.dp)
            Text(kind.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Small metadata chip with optional leading icon. */
@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, icon: MaterialSymbolIcon? = null, color: Color? = null) {
    val content = color ?: MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = content
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) Icon(icon = icon, contentDescription = null, tint = content, size = 13.dp)
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ConfidenceChip(level: String, modifier: Modifier = Modifier) {
    val color = FieldMindTheme.colors.confidenceColor(level)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f),
        contentColor = color
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon = FieldMindIcons.Check, contentDescription = null, tint = color, size = 13.dp)
            Text(level, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun TagChip(text: String, modifier: Modifier = Modifier) {
    InfoChip(text = text, modifier = modifier, icon = FieldMindIcons.Tag)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChoiceChips(
    options: List<String>,
    selected: String,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    val haptics = rememberFieldMindHaptics()
    FlowRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { if (selected != option) haptics.light(); onSelected(option) },
                label = { Text(option) }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Cards
// ──────────────────────────────────────────────────────────────────────

/**
 * The primary content card. Shows a leading accent icon for the entity kind, a title, an
 * optional body, and a row of metadata chips. Tapping opens the detail when [onClick] is set.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntityCard(
    title: String,
    kind: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    meta: List<String> = emptyList(),
    confidence: String? = null,
    onClick: (() -> Unit)? = null
) {
    val accent = FieldMindTheme.colors.accentFor(kind)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon = FieldMindIcons.iconFor(kind), contentDescription = null, tint = accent, size = 22.dp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!body.isNullOrBlank()) {
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                if (meta.isNotEmpty() || confidence != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        confidence?.takeIf { it.isNotBlank() }?.let { ConfidenceChip(it) }
                        meta.filter { it.isNotBlank() }.forEach { InfoChip(it) }
                    }
                }
            }
            if (onClick != null) {
                Icon(icon = FieldMindIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
            }
        }
    }
}

/** Prominent metric tile with an icon and optional trend caption. */
@Composable
fun MetricTile(
    label: String,
    value: String,
    icon: MaterialSymbolIcon,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    trend: String? = null,
    onClick: (() -> Unit)? = null
) {
    val tint = accent ?: MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon = icon, contentDescription = null, tint = tint, size = 22.dp)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!trend.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(icon = FieldMindIcons.Trend, contentDescription = null, tint = FieldMindTheme.colors.positive, size = 14.dp)
                    Text(trend, style = MaterialTheme.typography.labelSmall, color = FieldMindTheme.colors.positive)
                }
            }
        }
    }
}

/** Empty-state block with an icon, message, and an optional primary action. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: MaterialSymbolIcon = FieldMindIcons.Nature,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(24.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 28.dp) }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            androidx.compose.material3.Button(onClick = onAction, modifier = Modifier.padding(top = 4.dp)) {
                Icon(icon = FieldMindIcons.Add, contentDescription = null, size = 18.dp)
                Spacer(Modifier.size(6.dp))
                Text(actionLabel)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Inputs
// ──────────────────────────────────────────────────────────────────────

@Composable
fun FieldTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    supportingText: String? = null,
    required: Boolean = false,
    error: String? = null,
    enabled: Boolean = true
) {
    val displayLabel = if (required) "$label *" else label
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(displayLabel) },
        minLines = minLines,
        isError = error != null,
        supportingText = {
            when {
                error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                supportingText != null -> Text(supportingText)
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        enabled = enabled
    )
}

/** Overload accepting a RequiredFieldState for automatic validation. */
@Composable
fun FieldTextField(
    fieldState: RequiredFieldState,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    supportingText: String? = null,
    enabled: Boolean = true
) {
    FieldTextField(
        value = fieldState.value,
        onValueChange = { fieldState.onValueChange(it) },
        label = label,
        modifier = modifier,
        minLines = minLines,
        supportingText = supportingText,
        required = fieldState.isRequired,
        error = if (fieldState.isTouched) fieldState.error else null,
        enabled = enabled
    )
}

// ── Number-Only Input Field (Phase 1) ──

/**
 * Number-only input field with numeric keyboard, optional stepper +/- buttons,
 * configurable decimal places, min/max validation, and unit suffix.
 */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimalPlaces: Int = 0,
    suffix: String = "",
    supportingText: String? = null,
    min: Double? = null,
    max: Double? = null,
    stepper: Boolean = false,
    enabled: Boolean = true
) {
    val isDecimal = decimalPlaces > 0
    val keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number

    // Filter non-numeric characters on input
    val filterValue: (String) -> String = { input ->
        val cleaned = if (isDecimal) {
            input.filter { c -> c.isDigit() || c == '.' || c == '-' }
                .let { s ->
                    // Allow only one decimal point
                    val parts = s.split(".")
                    if (parts.size > 2) parts.take(2).joinToString(".")
                    else s
                }
                .let { s ->
                    // Limit decimal places
                    val dotIndex = s.indexOf('.')
                    if (dotIndex >= 0) s.substring(0, (dotIndex + 1 + decimalPlaces).coerceAtMost(s.length))
                    else s
                }
        } else {
            input.filter { c -> c.isDigit() || c == '-' }
                .let { s -> if (s.count { it == '-' } > 1) s.replaceFirst("-", "") else s }
        }
        cleaned.take(20)
    }

    // Compute error
    val numValue = value.toDoubleOrNull()
    val error = when {
        value.isNotBlank() && numValue == null -> "Not a valid number"
        numValue != null && min != null && numValue < min -> "Minimum: $min"
        numValue != null && max != null && numValue > max -> "Maximum: $max"
        else -> null
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (stepper) {
            // Stepper mode: field with +/- buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val current = value.toDoubleOrNull() ?: 0.0
                        val step = if (isDecimal) 0.1 else 1.0
                        val next = current - step
                        if (min == null || next >= min) {
                            onValueChange(
                                if (isDecimal) "%.${decimalPlaces}f".format(next)
                                else next.toInt().toString()
                            )
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("−", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(filterValue(it)) },
                    label = { Text(label) },
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    singleLine = true,
                    suffix = if (suffix.isNotBlank()) { { Text(suffix, color = MaterialTheme.colorScheme.onSurfaceVariant) } } else null,                supportingText = {
                    if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                    else if (supportingText != null) Text(supportingText)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                enabled = enabled
            )

                OutlinedButton(
                    onClick = {
                        val current = value.toDoubleOrNull() ?: 0.0
                        val step = if (isDecimal) 0.1 else 1.0
                        val next = current + step
                        if (max == null || next <= max) {
                            onValueChange(
                                if (isDecimal) "%.${decimalPlaces}f".format(next)
                                else next.toInt().toString()
                            )
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Simple numeric input field
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(filterValue(it)) },
                label = { Text(label) },
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                suffix = if (suffix.isNotBlank()) { { Text(suffix, color = MaterialTheme.colorScheme.onSurfaceVariant) } } else null,
                supportingText = {
                    if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                    else if (supportingText != null) Text(supportingText)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                enabled = enabled
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
//  Misc
// ──────────────────────────────────────────────────────────────────────

/** Soft outlined section used to group secondary content. */
@Composable
fun OutlinedSection(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) { Box(Modifier.padding(14.dp)) { content() } }
}

@Composable
fun Divider12() { Spacer(Modifier.height(12.dp)) }

// ──────────────────────────────────────────────────────────────────────
//  Note Templates & Composer (Redesign)
// ──────────────────────────────────────────────────────────────────────

/** Pre-built note template definitions for quick-start composition. */
data class NoteTemplate(val title: String, val prompts: List<String>, val icon: MaterialSymbolIcon)

val noteTemplates = listOf(
    NoteTemplate("Blank", emptyList(), FieldMindIcons.Note),
    NoteTemplate("Observation log", listOf("Subject:", "Location:", "Weather:", "What I observed:", "Notes:"), FieldMindIcons.Observation),
    NoteTemplate("Literature notes", listOf("Source:", "Key arguments:", "Evidence:", "My analysis:", "Questions:"), FieldMindIcons.Source),
    NoteTemplate("Meeting notes", listOf("Date:", "Attendees:", "Agenda:", "Decisions:", "Action items:"), FieldMindIcons.Session),
    NoteTemplate("Field journal", listOf("Date:", "Time:", "Location:", "Conditions:", "Findings:", "Follow-up:"), FieldMindIcons.Nature),
    NoteTemplate("Reflection", listOf("What happened:", "What I learned:", "What's next:"), FieldMindIcons.Question)
)

/**
 * Write-first note composer card with template presets.
 * Opens directly to body input, with template prompts as optional hints.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteComposerCard(
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    categories: List<String> = listOf("Other", "Behavior", "Environment", "Ecology", "Social", "Phenology"),
    saveEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showTemplates by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf(noteTemplates.first()) }
    val haptics = rememberFieldMindHaptics()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(FieldMindTheme.colors.source.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(FieldMindIcons.Note, null, tint = FieldMindTheme.colors.source, size = 22.dp) }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("New note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(
                            onClick = { haptics.light(); showTemplates = !showTemplates },
                            shape = RoundedCornerShape(10.dp),
                            color = if (showTemplates) FieldMindTheme.colors.source.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(FieldMindIcons.Article, null, tint = FieldMindTheme.colors.source, size = 14.dp)
                                Text("Templates", style = MaterialTheme.typography.labelSmall, color = FieldMindTheme.colors.source, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Text("Write first, organize later.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onDismiss != null) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(FieldMindIcons.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                    }
                }
            }

            // Template selector (collapsible)
            if (showTemplates) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    noteTemplates.forEach { template ->
                        FilterChip(
                            selected = selectedTemplate.title == template.title,
                            onClick = {
                                haptics.light()
                                selectedTemplate = template
                                if (template.prompts.isNotEmpty() && body.isBlank()) {
                                    onBodyChange(template.prompts.joinToString("\n"))
                                }
                            },
                            label = { Text(template.title, fontSize = 11.sp) },
                            leadingIcon = { Icon(template.icon, null, size = 14.dp) }
                        )
                    }
                }
            }

            // Title field
            FieldTextField(title, onTitleChange, "Title (optional)", supportingText = "Auto-generated from body if left blank")

            // Body field — write first, large
            OutlinedTextField(
                value = body,
                onValueChange = onBodyChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                placeholder = { Text("Start writing your note...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                shape = RoundedCornerShape(18.dp)
            )

            // Category & save row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChoiceChips(categories, category, modifier = Modifier.weight(1f), onSelected = onCategoryChange)
                Button(
                    onClick = { haptics.confirm(); onSave() },
                    shape = RoundedCornerShape(14.dp),
                    enabled = saveEnabled && (title.isNotBlank() || body.isNotBlank())
                ) {
                    Icon(FieldMindIcons.Check, null, size = 18.dp)
                    Spacer(Modifier.size(6.dp))
                    Text("Save")
                }
            }
        }
    }
}
