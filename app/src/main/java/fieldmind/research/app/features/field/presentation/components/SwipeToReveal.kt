package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

/**
 * iOS Mail-style swipe-to-reveal actions on list items.
 *
 * Wraps content and detects left-swipe to reveal contextual action buttons.
 * Uses spring physics for snap behavior and haptic feedback on full reveal.
 *
 * @param actions The list of swipe actions to reveal (max 3 recommended)
 * @param onDismiss Called after an action is taken or swipe is cancelled
 * @param content The list item content
 */
@Composable
fun SwipeToRevealActions(
    actions: List<SwipeAction>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()
    val density = LocalDensity.current

    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var contentWidth by remember { mutableFloatStateOf(1f) }
    var revealed by remember { mutableStateOf(false) }

    // Calculate the total action width (sum of all actions)
    val actionButtonWidth = 72.dp
    val totalActionsWidthPx = with(density) { (actionButtonWidth * actions.size).toPx() }

    val snapThreshold = totalActionsWidthPx * 0.5f

    val animatedOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = if (revealed)
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 400f)
        else
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 400f),
        label = "swipeReveal"
    )

    // Progress 0..1 for revealing actions
    val revealProgress = (abs(animatedOffset) / totalActionsWidthPx).coerceIn(0f, 1f)
    val isIOS = true // Use iOS-style overscroll resistance

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .onGloballyPositioned { coords ->
                contentWidth = coords.size.width.toFloat().coerceAtLeast(1f)
            }
    ) {
        // Action buttons revealed behind the content
        if (revealProgress > 0.01f) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions.forEachIndexed { index, action ->
                    val buttonScale = ((revealProgress - index * 0.33f) * 3f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .width(actionButtonWidth)
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleX = buttonScale
                                scaleY = buttonScale
                                this.alpha = buttonScale
                            }
                            .background(action.backgroundColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    haptics.confirm()
                                    action.onClick()
                                    scope.launch {
                                        swipeOffset = 0f
                                        revealed = false
                                        onDismiss()
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                icon = action.icon,
                                contentDescription = null,
                                tint = action.iconTint,
                                size = 22.dp
                            )
                            Text(
                                action.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = action.iconTint,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Main content — moves with swipe gesture
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = animatedOffset.coerceAtMost(0f)
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            // Start tracking
                        },
                        onHorizontalDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Float ->
                            change.consume()
                            val newOffset = swipeOffset + dragAmount
                            // Only allow left swipe (negative)
                            swipeOffset = if (newOffset > 0) newOffset * 0.3f else newOffset.coerceAtLeast(-totalActionsWidthPx * 1.1f)
                        },
                        onDragEnd = {
                            if (abs(swipeOffset) >= snapThreshold) {
                                haptics.confirm()
                                // Snap to fully revealed
                                scope.launch {
                                    swipeOffset = -totalActionsWidthPx
                                    revealed = true
                                }
                            } else {
                                // Snap back
                                scope.launch {
                                    swipeOffset = 0f
                                    revealed = false
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                swipeOffset = 0f
                                revealed = false
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

/**
 * Defines a swipe action button shown when a list item is swiped left.
 */
data class SwipeAction(
    val label: String,
    val icon: MaterialSymbolIcon,
    val backgroundColor: Color = Color(0xFFFFDAD6),
    val iconTint: Color = Color(0xFF410002),
    val onClick: () -> Unit
)
