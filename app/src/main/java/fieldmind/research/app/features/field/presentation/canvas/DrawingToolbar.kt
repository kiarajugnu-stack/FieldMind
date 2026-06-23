package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

/**
 * A premium iOS-style floating drawing toolbar.
 *
 * Features:
 * - Compact pill-shaped tool selector with smooth animations
 * - Color presets with circular swatches and selected ring
 * - Stroke width slider with live preview
 * - Press-and-hold on Shape tool to reveal shape sub-types
 * - Polished haptic-like animations and spring transitions
 * - Blurred glass effect background
 *
 * Designed to sit at the bottom of the canvas as a floating panel.
 *
 * @param drawingState tool state
 * @param onDismiss called to hide the toolbar
 * @param modifier standard modifier
 */
@Composable
fun DrawingToolbar(
    drawingState: DrawingState,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // ── Local state for UI interactions ──
    var showColorPicker by remember { mutableStateOf(false) }
    var showWidthSlider by remember { mutableStateOf(false) }
    var showShapePicker by remember { mutableStateOf(false) }

    // Animated visibility for expand/collapse
    val expandedFraction by animateFloatAsState(
        targetValue = if (showColorPicker || showWidthSlider || showShapePicker) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "toolbarExpand"
    )

    Surface(
        modifier = modifier
            .widthIn(min = 200.dp, max = 420.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Top row: tool buttons + dismiss ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tool row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawingTool.entries.forEach { tool ->
                        ToolButton(
                            tool = tool,
                            isActive = drawingState.activeTool == tool,
                            onClick = {
                                drawingState.setTool(tool)
                                showColorPicker = false
                                showWidthSlider = false
                                showShapePicker = false
                            },
                            onLongPress = {
                                if (tool == DrawingTool.SHAPE) {
                                    showShapePicker = !showShapePicker
                                    showColorPicker = false
                                    showWidthSlider = false
                                }
                            }
                        )
                    }
                }

                // Dismiss button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        MaterialSymbolIcon("keyboard_arrow_down"),
                        "Close toolbar",
                        size = 20.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Expandable sections ──
            AnimatedVisibility(
                visible = showColorPicker || showWidthSlider || showShapePicker,
                enter = expandVertically(animationSpec = spring(dampingRatio = 0.7f)) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Shape picker
                    if (showShapePicker) {
                        ShapePickerRow(
                            selectedType = drawingState.shapeType,
                            onSelect = { type ->
                                drawingState.setShapeType(type)
                                drawingState.setTool(DrawingTool.SHAPE)
                            }
                        )
                    }

                    // Color picker
                    if (showColorPicker) {
                        ColorPickerRow(
                            colors = DrawingState.presetColors,
                            selectedColor = drawingState.color,
                            onSelect = { color ->
                                drawingState.setColor(color)
                            }
                        )
                    }

                    // Stroke width slider
                    if (showWidthSlider) {
                        StrokeWidthSlider(
                            width = drawingState.strokeWidth,
                            onWidthChange = { drawingState.setStrokeWidth(it) },
                            color = drawingState.composeColor
                        )
                    }
                }
            }

            // ── Bottom action row: color + width triggers ──
            if (!showColorPicker && !showWidthSlider && !showShapePicker) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color swatch (taps open color picker)
                    Surface(
                        onClick = {
                            showColorPicker = !showColorPicker
                            showWidthSlider = false
                            showShapePicker = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(drawingState.composeColor)
                            .then(
                                if (drawingState.composeColor.luminance() > 0.5f)
                                    Modifier.background(Color.Transparent)
                                else Modifier
                            )
                            )
                            Text(
                                "Color",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Stroke width preview
                    Surface(
                        onClick = {
                            showWidthSlider = !showWidthSlider
                            showColorPicker = false
                            showShapePicker = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Live stroke preview
                            Canvas(modifier = Modifier.size(24.dp)) {
                                val w = drawingState.effectiveStrokeWidth.coerceIn(1f, 16f)
                                drawCircle(
                                    color = drawingState.composeColor,
                                    radius = size.minDimension / 2f - 2f,
                                    style = Stroke(width = w)
                                )
                                drawCircle(
                                    color = drawingState.composeColor,
                                    radius = size.minDimension / 2f - 2f - w / 2f,
                                    style = Stroke(width = 1.5f)
                                )
                            }
                            Text(
                                "${drawingState.strokeWidth.toInt()}px",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Tool Button
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ToolButton(
    tool: DrawingTool,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "toolBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "toolContent"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier
            .size(40.dp)
            .then(
                if (tool == DrawingTool.SHAPE) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { onLongPress() }
                        )
                    }
                } else Modifier
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                MaterialSymbolIcon(tool.icon, defaultWeight = if (isActive) 500 else 300),
                tool.displayName,
                size = 22.dp,
                tint = contentColor
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Color Picker Row
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ColorPickerRow(
    colors: List<Long>,
    selectedColor: Long,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Colors",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        // Two rows of color swatches
        val mid = (colors.size + 1) / 2
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.take(mid).forEach { colorLong ->
                ColorSwatch(
                    color = Color(colorLong),
                    isSelected = colorLong == selectedColor,
                    onClick = { onSelect(colorLong) }
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.drop(mid).forEach { colorLong ->
                ColorSwatch(
                    color = Color(colorLong),
                    isSelected = colorLong == selectedColor,
                    onClick = { onSelect(colorLong) }
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "swatchScale"
    )

    Box(
        modifier = Modifier
            .size(26.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier
                        .border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else Modifier
            )
    )
}

// ══════════════════════════════════════════════════════════════════════
//  Shape Picker Row
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ShapePickerRow(
    selectedType: ShapeType,
    onSelect: (ShapeType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Shape",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShapeType.entries.forEach { type ->
                Surface(
                    onClick = { onSelect(type) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (type == selectedType)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            MaterialSymbolIcon(type.icon),
                            type.displayName,
                            size = 16.dp,
                            tint = if (type == selectedType)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            type.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (type == selectedType)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Stroke Width Slider
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun StrokeWidthSlider(
    width: Float,
    onWidthChange: (Float) -> Unit,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Thickness",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                "${width.toInt()}px",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Custom slider with live preview
        Slider(
            value = width,
            onValueChange = onWidthChange,
            valueRange = 1f..20f,
            modifier = Modifier.height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color.copy(alpha = 0.5f),
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        // Stroke preview (thin → thick)
        Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            val w = width.coerceIn(1f, 20f)
            val y = size.height / 2f
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = w,
                cap = StrokeCap.Round
            )
        }
    }
}
