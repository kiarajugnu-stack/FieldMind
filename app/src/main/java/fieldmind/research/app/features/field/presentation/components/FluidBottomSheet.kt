package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * iOS-style fluid bottom sheet with interactive drag-to-dismiss,
 * spring physics, a grab handle, and animated backdrop scrim.
 *
 * Features:
 * - Drag down to dismiss with iOS-style rubber-band spring physics
 * - Smooth backdrop scrim that fades with drag progress
 * - Grab handle at the top center
 * - Content area with rounded top corners (iOS style)
 * - Scale-down effect on the content as it's dismissed
 * - Haptic on dismissal commit
 * - Spring snap-back if released before threshold
 * - Configurable height fraction
 */
@Composable
fun FluidBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    heightFraction: Float = 0.85f,
    content: @Composable () -> Unit
) {
    val reduceMotion = FieldMindMotion.isReduceMotion()
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()
    val density = LocalDensity.current

    var sheetOffset by remember { mutableFloatStateOf(0f) }
    var sheetHeight by remember { mutableFloatStateOf(1f) }
    var parentHeight by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }

    val dismissThreshold = (parentHeight * 0.25f)
    val fullSheetHeight = parentHeight * heightFraction

    // Animate offset with spring for smooth drag physics
    val animatedOffset by animateFloatAsState(
        targetValue = if (visible && !isDragging) {
            parentHeight - fullSheetHeight + sheetOffset
        } else if (!visible && !isDragging) {
            parentHeight
        } else {
            parentHeight - fullSheetHeight + sheetOffset
        },
        animationSpec = if (isDragging)
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 800f)
        else
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 300f),
        label = "sheetY"
    )

    // Progress for backdrop scrim
    val dragProgress = (sheetOffset / dismissThreshold).coerceIn(0f, 1f)
    val scrimAlpha = if (visible) (1f - dragProgress * 0.6f).coerceIn(0f, 1f) else 0f
    val contentScale = 1f - dragProgress * 0.05f
    val cornerRadius = (28 - dragProgress * 12).coerceAtLeast(16)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = FieldMindMotion.expressiveFloat),
        exit = fadeOut(animationSpec = FieldMindMotion.fadeTween)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    parentHeight = coords.size.height.toFloat().coerceAtLeast(1f)
                }
        ) {
            // Backdrop scrim — tap to dismiss
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha * 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (!isDragging) {
                                scope.launch {
                                    sheetOffset = parentHeight
                                    onDismiss()
                                }
                            }
                        }
                    )
            )

            // Sheet content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, animatedOffset.roundToInt()) }
                    .onGloballyPositioned { coords ->
                        sheetHeight = coords.size.height.toFloat().coerceAtLeast(1f)
                    }
                    .then(
                        if (!reduceMotion) {
                            Modifier.pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val newOffset = sheetOffset + dragAmount
                                        // iOS-style rubber band: resistance increases with downward drag
                                        sheetOffset = if (newOffset > 0) {
                                            dismissThreshold * 0.6f * (1f - 1f / (newOffset / dismissThreshold + 1f))
                                        } else {
                                            newOffset
                                        }
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        if (sheetOffset >= dismissThreshold) {
                                            haptics.confirm()
                                            scope.launch {
                                                sheetOffset = parentHeight
                                                onDismiss()
                                            }
                                        } else {
                                            sheetOffset = 0f
                                        }
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        sheetOffset = 0f
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val shape = RoundedCornerShape(
                                topStart = cornerRadius.dp,
                                topEnd = cornerRadius.dp
                            )
                            clip = true
                            scaleX = contentScale
                            scaleY = contentScale
                            translationY = -animatedOffset * 0.02f
                        }
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        // Grab handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            )
                        }

                        // Content
                        content()
                    }
                }
            }
        }
    }
}
