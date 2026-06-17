package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
// FieldMindIcons is in the same package (components.FieldMindIcons)


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
// ══════════════════════════════════════════════════════════════════════
//  OptionPickerDialog — Beautiful Material 3 dialog for picking options
//  Replaces ChoiceChips, ExposedDropdownMenu, and other inline pickers
//  with a full-screen-style dialog that shows options as large touch targets.
// ══════════════════════════════════════════════════════════════════════

/**
 * A stunning Material 3 dialog that presents a list of options as large, tappable cards.
 * Use this to replace ChoiceChips and ExposedDropdownMenu.
 *
 * Features:
 * - Beautiful card-based option layout with icons
 * - Selected option shown with check mark and accent color
 * - Smooth scroll for many options
 * - Optional header with title and subtitle
 * - Optionally shows check icon next to the selected value
 */
@Composable
fun OptionPickerDialog(
    title: String,
    subtitle: String = "",
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    iconProvider: ((String) -> MaterialSymbolIcon?)? = null,
    showSearch: Boolean = false,
    multiSelect: Boolean = false,
    selectedMulti: Set<String> = emptySet(),
    onMultiSelect: ((Set<String>) -> Unit)? = null
) {
    val haptics = rememberFieldMindHaptics()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredOptions = remember(options, searchQuery) {
        if (searchQuery.isBlank()) options
        else options.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(FieldMindIcons.Question, null, tint = accentColor, size = 24.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (subtitle.isNotBlank()) {
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(FieldMindIcons.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
                    }
                }

                // Optional search
                if (showSearch && options.size > 6) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search options…") },
                        leadingIcon = { Icon(FieldMindIcons.Search, null, size = 20.dp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // Options list
                filteredOptions.forEach { option ->
                    val isSelected = if (multiSelect) option in selectedMulti else option == selected
                    val icon = iconProvider?.invoke(option)
                    
                    Surface(
                        onClick = {
                            haptics.light()
                            if (multiSelect && onMultiSelect != null) {
                                val newSet = if (isSelected) selectedMulti - option else selectedMulti + option
                                onMultiSelect(newSet)
                            } else {
                                onSelect(option)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) BorderStroke(1.5.dp, accentColor) else null,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (icon != null) {
                                Box(
                                    Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                                        .background(accentColor.copy(alpha = if (isSelected) 0.18f else 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null, tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                                }
                            }
                            
                            Column(Modifier.weight(1f)) {
                                Text(
                                    option,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            if (isSelected) {
                                Box(
                                    Modifier.size(28.dp).clip(CircleShape)
                                        .background(accentColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(FieldMindIcons.Check, null, tint = MaterialTheme.colorScheme.surface, size = 16.dp)
                                }
                            }
                        }
                    }
                }

                if (filteredOptions.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No matching options", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (multiSelect && onMultiSelect != null) {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            haptics.confirm()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        enabled = selectedMulti.isNotEmpty()
                    ) {
                        Icon(FieldMindIcons.Check, null, size = 18.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("Done (${selectedMulti.size} selected)")
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Multi-select variant of OptionPickerField. Opens a dialog with checkboxes.
 */
@Composable
fun MultiSelectPickerField(
    label: String,
    selected: Set<String>,
    options: List<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    icon: MaterialSymbolIcon? = null,
    subtitle: String = "",
    iconProvider: ((String) -> MaterialSymbolIcon?)? = null,
    showSearch: Boolean = false
) {
    val haptics = rememberFieldMindHaptics()
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Surface(
            onClick = { haptics.light(); showDialog = true },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = accentColor, size = 20.dp)
                }

                val displayText = when {
                    selected.isEmpty() -> "Select…"
                    selected.size <= 2 -> selected.joinToString(", ")
                    else -> {
                        val firstTwo = selected.take(2).joinToString(", ")
                        "$firstTwo +${selected.size - 2}"
                    }
                }
                Text(
                    displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Icon(FieldMindIcons.Down, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
            }
        }

        if (showDialog) {
            OptionPickerDialog(
                title = label,
                subtitle = subtitle,
                options = options,
                selected = "",
                onSelect = {},
                onDismiss = { showDialog = false },
                accentColor = accentColor,
                iconProvider = iconProvider,
                showSearch = showSearch,
                multiSelect = true,
                selectedMulti = selected,
                onMultiSelect = onSelectionChanged
            )
        }
    }
}

/**
 * Trigger button that opens an OptionPickerDialog when clicked.
 * Shows the current selected value with a dropdown-style appearance.
 */
@Composable
fun OptionPickerField(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    icon: MaterialSymbolIcon? = null,
    subtitle: String = "",
    iconProvider: ((String) -> MaterialSymbolIcon?)? = null,
    showSearch: Boolean = false
) {
    val haptics = rememberFieldMindHaptics()
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Surface(
            onClick = { haptics.light(); showDialog = true },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = accentColor, size = 20.dp)
                }
                
                Text(
                    selected,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Icon(FieldMindIcons.Down, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
            }
        }
        
        if (showDialog) {
            OptionPickerDialog(
                title = label,
                subtitle = subtitle,
                options = options,
                selected = selected,
                onSelect = onSelected,
                onDismiss = { showDialog = false },
                accentColor = accentColor,
                iconProvider = iconProvider,
                showSearch = showSearch
            )
        }
    }
}

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
//  Standardized Screen Header — Used across every screen
// ──────────────────────────────────────────────────────────────────────

/**
 * Standardized screen header for the redesign.
 * A rounded card at the top of every screen with:
 * - Semantic accent color icon
 * - Title + optional subtitle
 * - Optional trailing action
 * Consistent height and padding across all screens.
 */
@Composable
fun StandardScreenHeader(
    title: String,
    subtitle: String? = null,
    icon: MaterialSymbolIcon,
    heroColor: Color = FieldMindTheme.colors.accentFor(title),
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor.copy(alpha = 0.25f),
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(heroColor.copy(alpha = if (FieldMindTheme.colors.isDark) 0.28f else 0.14f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon = icon, contentDescription = null, tint = heroColor, size = 24.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing?.invoke()
        }
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
    onClick: (() -> Unit)? = null,
    index: Int = 0,
    animate: Boolean = false
) {
    val accent = FieldMindTheme.colors.accentFor(kind)

    // Stagger-enter animation: items slide up + fade in with staggered delay
    var visible by remember { mutableStateOf(!animate) }
    LaunchedEffect(animate, index) {
        if (animate) {
            delay(FieldMindMotion.staggerDelay(index).toLong())
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            ),
            initialOffsetY = { it / 3 }
        ) + fadeIn(animationSpec = tween(FieldMindMotion.durationSubtle))
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )
                )
                .then(
                    if (onClick != null) Modifier.expressiveCardPress(liftDp = 1.5f, scaleDown = 0.985f)
                    else Modifier
                )
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
                    Icon(
                        icon = FieldMindIcons.Forward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 22.dp
                    )
                }
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
        modifier = modifier
            .then(if (onClick != null) Modifier.expressivePress(scaleDown = 0.96f) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
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
// ─────────────────────────────────────────���────────────────────────────

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
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val displayLabel = if (required) "$label *" else label
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(displayLabel) },
        minLines = minLines,
        isError = error != null,
        keyboardOptions = if (keyboardType != KeyboardType.Text) KeyboardOptions(keyboardType = keyboardType) else KeyboardOptions.Default,
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
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
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
        enabled = enabled,
        keyboardType = keyboardType
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
//  Field Protocol System — Step-by-step survey protocols
// ──────────────────────────────────────────────────────────────────────

/**
 * Protocol definition for structured field surveys.
 * A protocol specifies a series of steps to follow during a capture session.
 */
data class FieldProtocol(
    val id: String,
    val name: String,
    val description: String,
    val icon: MaterialSymbolIcon,
    val steps: List<ProtocolStep>,
    val suggestedCategory: String = "Other",
    val defaultTags: String = ""
)

data class ProtocolStep(
    val id: String,
    val label: String,
    val instruction: String,
    val inputType: ProtocolInputType = ProtocolInputType.Text,
    val placeholder: String = "",
    val options: List<String> = emptyList(),
    val required: Boolean = false
)

enum class ProtocolInputType {
    Text, Number, Decimal, Choice, MultiChoice, YesNo, Photo, Location
}

/** Built-in field protocols. */
object FieldProtocols {
    val all: List<FieldProtocol> = listOf(
        FieldProtocol(
            id = "point_count",
            name = "Point Count",
            description = "Bird survey: count all birds seen/heard at a single point over a fixed time.",
            icon = FieldMindIcons.Observation,
            suggestedCategory = "Bird",
            defaultTags = "protocol, point-count, bird-survey",
            steps = listOf(
                ProtocolStep("location", "Location", "Record your exact position.", ProtocolInputType.Location, "GPS coordinates", required = true),
                ProtocolStep("start_time", "Start time", "Note the exact start time of this count.", ProtocolInputType.Text, "HH:MM", required = true),
                ProtocolStep("duration", "Duration (min)", "How many minutes did you count?", ProtocolInputType.Number, "10", required = true),
                ProtocolStep("distance", "Distance radius (m)", "What was the counting radius?", ProtocolInputType.Number, "50", required = true),
                ProtocolStep("weather", "Weather conditions", "Describe wind, precipitation, visibility.", ProtocolInputType.Text, "Calm, clear, 100m visibility"),
                ProtocolStep("species_seen", "Species seen", "List the species and counts you observed.", ProtocolInputType.Text, "Crow: 2, Sparrow: 5", required = true),
                ProtocolStep("species_heard", "Species heard only", "List species heard but not seen.", ProtocolInputType.Text, "Blue Jay call"),
                ProtocolStep("notes", "Notes", "Any additional observations or disturbances.", ProtocolInputType.Text, "Dog walker passed at 5 min")
            )
        ),
        FieldProtocol(
            id = "transect_survey",
            name = "Transect Survey",
            description = "Walk a fixed path and record all observations within a set distance.",
            icon = FieldMindIcons.Map,
            suggestedCategory = "Other",
            defaultTags = "protocol, transect, survey",
            steps = listOf(
                ProtocolStep("transect_name", "Transect name", "Name or identifier for this path.", ProtocolInputType.Text, "North Meadow Loop", required = true),
                ProtocolStep("length", "Length (m)", "How long was the transect?", ProtocolInputType.Number, "200", required = true),
                ProtocolStep("width", "Width (m)", "Detection width on each side.", ProtocolInputType.Number, "25", required = true),
                ProtocolStep("start_location", "Start point", "Location where the transect begins.", ProtocolInputType.Location, "GPS or landmark", required = true),
                ProtocolStep("end_location", "End point", "Location where the transect ends.", ProtocolInputType.Location, "GPS or landmark", required = true),
                ProtocolStep("habitat_type", "Habitat type", "Dominant habitat along the transect.", ProtocolInputType.Text, "Grassland / woodland edge"),
                ProtocolStep("observations", "Observations", "Log each observation with distance and behavior.", ProtocolInputType.Text, "Red Fox at 10m, moving east", required = true)
            )
        ),
        FieldProtocol(
            id = "water_quality",
            name = "Water Quality",
            description = "Log water conditions: clarity, flow, chemistry, and life.",
            icon = FieldMindIcons.Water,
            suggestedCategory = "Water",
            defaultTags = "protocol, water-quality, monitoring",
            steps = listOf(
                ProtocolStep("water_body", "Water body name", "Pond, stream, river, or lake name.", ProtocolInputType.Text, "Mill Pond", required = true),
                ProtocolStep("clarity", "Clarity", "How clear is the water?", ProtocolInputType.Choice, options = listOf("Clear", "Slightly turbid", "Turbid", "Opaque"), required = true),
                ProtocolStep("flow", "Flow rate", "Describe the water flow.", ProtocolInputType.Choice, options = listOf("Still", "Slow", "Moderate", "Fast"), required = true),
                ProtocolStep("depth", "Depth estimate (cm)", "Approximate depth at sampling point.", ProtocolInputType.Number, "30", required = true),
                ProtocolStep("temperature", "Temperature (°C)", "Water temperature.", ProtocolInputType.Decimal, "15.5"),
                ProtocolStep("organisms", "Organisms found", "List any aquatic life observed.", ProtocolInputType.Text, "Caddisfly larvae, water striders"),
                ProtocolStep("notes", "Notes", "Odors, color, nearby land use, weather.", ProtocolInputType.Text, "Slight algae bloom near edges")
            )
        ),
        FieldProtocol(
            id = "phenology_check",
            name = "Phenology Check",
            description = "Track seasonal changes: leaf-out, flowering, fruiting, migration.",
            icon = FieldMindIcons.Nature,
            suggestedCategory = "Plant",
            defaultTags = "protocol, phenology, seasonal",
            steps = listOf(
                ProtocolStep("species", "Species / plant", "What species are you monitoring?", ProtocolInputType.Text, "Oak, Maple", required = true),
                ProtocolStep("stage", "Phenological stage", "What stage has been reached?", ProtocolInputType.Choice, options = listOf("Bud burst", "Leaf-out", "Flowering", "Fruiting", "Senescence", "Dormant"), required = true),
                ProtocolStep("percent", "Percent complete", "Approximate percentage of individuals at this stage.", ProtocolInputType.Number, "50", required = true),
                ProtocolStep("evidence", "Photo evidence", "Take a photo showing the stage clearly.", ProtocolInputType.Photo, "Photo required"),
                ProtocolStep("notes", "Notes", "Weather, unusual timing, insect activity.", ProtocolInputType.Text, "First flowering seen this year")
            )
        ),
        FieldProtocol(
            id = "soil_pit",
            name = "Soil Pit",
            description = "Dig a small pit and log soil horizons, texture, moisture, and organisms.",
            icon = FieldMindIcons.Data,
            suggestedCategory = "Soil",
            defaultTags = "protocol, soil, geology",
            steps = listOf(
                ProtocolStep("location", "Pit location", "Where was the pit dug?", ProtocolInputType.Location, "GPS coordinates", required = true),
                ProtocolStep("depth", "Depth (cm)", "How deep was the pit?", ProtocolInputType.Number, "30", required = true),
                ProtocolStep("horizons", "Horizons observed", "Describe the soil layers.", ProtocolInputType.Text, "O: 2cm leaf litter, A: 10cm dark loam", required = true),
                ProtocolStep("texture", "Texture", "Soil texture by feel.", ProtocolInputType.Choice, options = listOf("Sandy", "Loamy", "Clay", "Silty", "Peaty"), required = true),
                ProtocolStep("moisture", "Moisture", "How moist was the soil?", ProtocolInputType.Choice, options = listOf("Dry", "Moist", "Wet", "Saturated"), required = true),
                ProtocolStep("organisms", "Soil organisms", "Any worms, insects, roots found.", ProtocolInputType.Text, "Earthworms at 10cm depth")
            )
        )
    )
}

/** Composable that renders the input field for a ProtocolStep. */
@Composable
fun ProtocolStepField(
    step: ProtocolStep,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (step.inputType) {
        ProtocolInputType.Number -> NumberField(
            value = value,
            onValueChange = onValueChange,
            label = step.label,
            modifier = modifier,
            suffix = "",
            decimalPlaces = 0,
            supportingText = step.instruction
        )
        ProtocolInputType.Decimal -> NumberField(
            value = value,
            onValueChange = onValueChange,
            label = step.label,
            modifier = modifier,
            suffix = "",
            decimalPlaces = 2,
            supportingText = step.instruction
        )
        ProtocolInputType.Choice -> {
            Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(step.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(step.instruction, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (step.options.isNotEmpty()) {
                    ChoiceChips(step.options, value, modifier = Modifier.fillMaxWidth(), onSelected = onValueChange)
                }
            }
        }
        ProtocolInputType.Location -> FieldTextField(
            value = value,
            onValueChange = onValueChange,
            label = step.label,
            modifier = modifier,
            supportingText = step.instruction
        )
        ProtocolInputType.Photo -> {
            Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(step.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(step.instruction, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(FieldMindIcons.Camera, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                        Text("Add photo evidence", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        else -> FieldTextField(
            value = value,
            onValueChange = onValueChange,
            label = step.label,
            modifier = modifier,
            supportingText = step.instruction
        )
    }
}

/** Protocol selector dialog. */
@Composable
fun ProtocolPicker(
    selectedId: String?,
    onSelect: (FieldProtocol?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f).wrapContentHeight().padding(vertical = 24.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Choose a protocol", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Protocols guide you through structured field surveys step by step.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FieldProtocols.all.forEach { protocol ->
                    val isSelected = selectedId == protocol.id
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(protocol) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                                Icon(protocol.icon, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(protocol.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(protocol.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${protocol.steps.size} steps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                            if (isSelected) {
                                Icon(FieldMindIcons.Check, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onSelect(null) }) { Text("No protocol (manual entry)") }
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Note Templates & Composer (Redesign)
// ══════════════════════════════════════════════════════════════════════

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
