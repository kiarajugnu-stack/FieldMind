package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Sticky note block for the infinite canvas.
 *
 * Features:
 * - Yellow card with soft drop shadow (default color)
 * - Slight random rotation on creation (-3° to +3°)
 * - Editable text that auto-expands the block height
 * - Color picker: yellow, green, pink, blue, orange, purple
 * - Rotation handle at top — drag to rotate freely
 * - "Pin" icon at top-right to suggest a pinned note
 *
 * The sticky note color and rotation are stored in [CanvasBlockEntity.contentJson]
 * as a simple JSON object: `{"color":"yellow","text":"..."}` or via dedicated
 * callback parameters.
 *
 * @param text the sticky note text content
 * @param onTextChange called when the text changes
 * @param colorIndex index into the color palette (0 = yellow, 1 = green, etc.)
 * @param onColorChange called when a new color is selected
 * @param rotation the rotation angle in degrees
 * @param onRotationChange called when the rotation handle is dragged
 * @param isSelected whether the containing block is selected (shows controls)
 * @param onHeightChanged called when text content causes the height to change
 * @param modifier standard Compose modifier
 */
@Composable
fun StickyNoteBlock(
    text: String,
    onTextChange: (String) -> Unit = {},
    colorIndex: Int = 0,
    onColorChange: (Int) -> Unit = {},
    rotation: Float = 0f,
    onRotationChange: (Float) -> Unit = {},
    isSelected: Boolean = false,
    onHeightChanged: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // ── Sticky note colors ──
    val colors = remember {
        listOf(
            0xFFFFF9C4 to "Yellow",   // default
            0xFFC8E6C9 to "Green",
            0xFFF8BBD0 to "Pink",
            0xFFBBDEFB to "Blue",
            0xFFFFE0B2 to "Orange",
            0xFFE1BEE7 to "Purple"
        )
    }

    val currentColor = colors.getOrElse(colorIndex.coerceIn(0, colors.lastIndex)) { colors[0] }
    val bgColor by animateColorAsState(
        targetValue = Color(currentColor.first),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "stickyColor"
    )

    // Animate shadow elevation on selection
    val elevation by animateFloatAsState(
        targetValue = if (isSelected) 8f else 3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "stickyElevation"
    )

    // ── Text state with auto-height ──
    var textState by remember(text) { mutableStateOf(text) }
    var measuredHeightPx by remember { mutableStateOf(0f) }

    // ── Color picker expanded state ──
    var showColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationZ = rotation
                // Slight perspective tilt for a more physical card feel
                cameraDistance = 12f * density.density
            }
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color(0x40000000),
                spotColor = Color(0x28000000),
                clip = false
            )
    ) {
        // ── Top bar: pin icon (left), rotation handle (center), color picker toggle (right) ──
        if (isSelected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        bgColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pin icon (decorative)
                Icon(
                    MaterialSymbolIcon("push_pin", defaultWeight = 200),
                    "Pinned note",
                    size = 14.dp,
                    tint = Color(0x66000000)
                )

                // Rotation handle — small circle that can be dragged to rotate
                RotationHandle(
                    currentRotation = rotation,
                    onRotationChange = onRotationChange
                )

                // Color picker toggle
                IconButton(
                    onClick = { showColorPicker = !showColorPicker },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        MaterialSymbolIcon("palette"),
                        "Change color",
                        size = 16.dp
                    )
                }
            }
        }

        // ── Color picker row ──
        if (isSelected && showColorPicker) {
            ColorPickerRow(
                colors = colors,
                selectedIndex = colorIndex,
                onColorSelected = { index ->
                    onColorChange(index)
                    showColorPicker = false
                }
            )
        }

        // ── Main card body ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = bgColor,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 0.dp
        ) {
            // ── Text editor ──
            BasicTextField(
                value = textState,
                onValueChange = { newText ->
                    textState = newText
                    onTextChange(newText)
                },
                cursorBrush = SolidColor(Color(0x88000000)),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xDD000000),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .onGloballyPositioned { coordinates ->
                        val newHeight = coordinates.size.height.toFloat()
                        if (newHeight != measuredHeightPx && newHeight > 0f) {
                            measuredHeightPx = newHeight
                            // Report content height back to parent
                            onHeightChanged?.invoke(newHeight + 24f) // 24px buffer for top bar + padding
                        }
                    }
                    .heightIn(min = with(density) { 80.toDp() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (textState.isEmpty()) {
                            Text(
                                "Type here…",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0x66000000),
                                    fontSize = 14.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Rotation Handle
// ══════════════════════════════════════════════════════════════════════

/**
 * A small circular handle at the top center of the sticky note.
 * Drag left/right to rotate the note freely.
 *
 * The handle visual: a small filled circle with a subtle ring.
 */
@Composable
private fun RotationHandle(
    currentRotation: Float,
    onRotationChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .size(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(1.5.dp, Color(0x66000000), CircleShape)
                .background(Color(0x22000000), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Convert horizontal drag to rotation change
                        // Drag right = rotate clockwise (positive)
                        val sensitivity = 0.5f // degrees per pixel of drag
                        val rotationDelta = dragAmount.x * sensitivity
                        // Also respond to vertical drag (slightly less sensitive)
                        val verticalDelta = dragAmount.y * sensitivity * 0.3f
                        val newRotation = (currentRotation + rotationDelta + verticalDelta) % 360f
                        onRotationChange(newRotation)
                    }
                }
        )
        // Inner dot
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(Color(0x88000000), CircleShape)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Color Picker Row
// ══════════════════════════════════════════════════════════════════════

/**
 * Horizontal row of colored circles for selecting the sticky note color.
 * The currently selected color has a thicker border and slightly larger size.
 */
@Composable
private fun ColorPickerRow(
    colors: List<Pair<Int, String>>,
    selectedIndex: Int,
    onColorSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0x11000000),
        shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEachIndexed { index, (color, name) ->
                val isSelected = index == selectedIndex
                val colorCircleSize by animateFloatAsState(
                    targetValue = if (isSelected) 28f else 24f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "colorCircleSize"
                )

                Box(
                    modifier = Modifier
                        .size(with(LocalDensity.current) { colorCircleSize.toDp() })
                        .clip(CircleShape)
                        .background(Color(color), CircleShape)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    CircleShape
                                )
                            } else Modifier
                        )
                        .clickable { onColorSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {                            Icon(
                                MaterialSymbolIcon("check", defaultWeight = 400),
                                name,
                                size = 14.dp,
                                tint = Color(0xAA000000)
                            )
                    }
                }
            }
        }
    }
}
