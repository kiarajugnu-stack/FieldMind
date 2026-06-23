package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * An AlertDialog that supports horizontal swipe-to-dismiss with spring physics,
 * rubber-band resistance, scale-down effect, scrim fade, and haptic feedback.
 *
 * Swipe left or right past the threshold (35% of dialog width) to dismiss with
 * a confirm haptic. The dialog rubber-bands with iOS-style resistance and snaps
 * back if the threshold is not reached.
 */
@Composable
fun SwipeableAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation: androidx.compose.ui.unit.Dp = 6.dp
) {
    val reduceMotion = FieldMindMotion.isReduceMotion()
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isAnimatingAway by remember { mutableStateOf(false) }
    var dialogWidth by remember { mutableFloatStateOf(1f) }

    val dismissThreshold: Float
        get() {
            return dialogWidth * 0.35f
        }

    // Rubber-band: compute the effective (resisted) offset so position,
    // scale, and alpha all respond consistently beyond the threshold.
    fun rubberBanded(absRaw: Float): Float = if (absRaw > dismissThreshold) {
        val overscroll = absRaw - dismissThreshold
        dismissThreshold * (1f + 0.5f * (1f - 1f / (overscroll / dismissThreshold + 1f)))
    } else absRaw

    val effectiveSign: Float = if (dragOffset < 0) -1f else 1f
    val effectiveAbs = rubberBanded(abs(dragOffset))
    val effectiveDragOffset = effectiveSign * effectiveAbs

    // Animate the drag offset with spring physics
    val animatedDragOffset by animateFloatAsState(
        targetValue = if (isAnimatingAway) effectiveSign * dialogWidth * 1.1f
                    else if (isDragging) effectiveDragOffset
                    else 0f,
        animationSpec = if (isDragging)
            FieldMindMotion.expressiveFloat
        else if (isAnimatingAway)
            FieldMindMotion.expressiveDramatic
        else
            FieldMindMotion.swipeBackSpring,
        label = "dialogSwipe"
    )

    // Drag progress for visual effects — based on rubber-banded offset
    val dragProgress = (effectiveAbs / dismissThreshold).coerceIn(0f, 1f)
    val scrimAlpha = (1f - dragProgress * 0.6f).coerceIn(0f, 1f)
    val contentScale = 1f - dragProgress * 0.06f
    val contentAlpha = 1f - dragProgress * 0.3f
    val swipeCornerRadius = (dragProgress * FieldMindMotion.swipeCornerRadiusDp).dp

    // Dismiss the dialog after the dismiss animation completes
    LaunchedEffect(isAnimatingAway) {
        if (isAnimatingAway) {
            kotlinx.coroutines.delay(250)
            onDismissRequest()
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isAnimatingAway) {
                onDismissRequest()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha.coerceIn(0f, 1f) * 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!isAnimatingAway) {
                            onDismissRequest()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth(0.88f)
                    .onGloballyPositioned { coords ->
                        dialogWidth = coords.size.width.toFloat().coerceAtLeast(1f)
                    }
                    .clip(RoundedCornerShape(swipeCornerRadius))
                    .graphicsLayer {
                        translationX = animatedDragOffset
                        scaleX = contentScale.coerceIn(0.7f, 1f)
                        scaleY = contentScale.coerceIn(0.7f, 1f)
                        this.alpha = contentAlpha.coerceIn(0.3f, 1f)
                    }
                    .then(
                        if (!reduceMotion) {
                            Modifier.pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset = dragOffset + dragAmount
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        if (abs(dragOffset) >= dismissThreshold) {
                                            haptics.confirm()
                                            isAnimatingAway = true
                                        } else {
                                            dragOffset = 0f
                                        }
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        dragOffset = 0f
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    ),
                shape = shape,
                color = containerColor,
                tonalElevation = tonalElevation,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon
                    if (icon != null) {
                        icon()
                        Spacer(Modifier.height(8.dp))
                    }

                    // Title
                    if (title != null) {
                        title()
                        Spacer(Modifier.height(4.dp))
                    }

                    // Body text
                    if (text != null) {
                        text()
                        Spacer(Modifier.height(16.dp))
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                        }
                        confirmButton()
                    }
                }
            }
        }
    }
}
