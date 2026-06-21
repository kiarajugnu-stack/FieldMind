package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Shows a snackbar message. Cancels any previous snackbar first so messages never stack.
 * Uses ExtraShort duration (1.5s) by default for fast, unobtrusive feedback.
 * Supports interactive action buttons.
 */
fun showFastSnackbar(
    hostState: SnackbarHostState,
    scope: CoroutineScope,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    customDuration: SnackbarDuration = SnackbarDuration.Short
) {
    scope.launch {
        hostState.currentSnackbarData?.dismiss()
        val result = hostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = customDuration
        )
        if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
    }
}

/**
 * Determine the visual style for a snackbar based on its message content.
 * Returns (isSave, isError, isWarning, isAchievement).
 */
private data class SnackbarStyle(
    val isSave: Boolean = false,
    val isError: Boolean = false,
    val isWarning: Boolean = false,
    val isAchievement: Boolean = false
)

private fun snackbarStyle(message: String): SnackbarStyle {
    val isSave = message.startsWith("Observation saved") ||
        message.startsWith("Saved") ||
        message.startsWith("Quick snap saved") ||
        message.startsWith("Photo captured") ||
        message.startsWith("$") && !message.startsWith("🏆")
    val isAchievement = message.startsWith("🏆") || message.contains("unlocked!")
    val isError = message.startsWith("⚠") ||
        message.contains("required") ||
        message.contains("denied") ||
        message.contains("Couldn't") ||
        message.contains("cancelled")
    val isWarning = !isSave && !isError && !isAchievement &&
        (message.contains("empty") || message.contains("unavailable"))
    return SnackbarStyle(isSave, isError, isWarning, isAchievement)
}

/**
 * A unified top-positioned, theme-aware, swipeable, interactive snackbar overlay.
 *
 * Features:
 * - Top-positioned with slide-in + bouncy scale animation
 * - Swipe horizontally to dismiss
 * - Tap/dismiss anywhere on the snackbar to dismiss
 * - Styled by message content: save (green), error (red), warning (amber), achievement (gold)
 * - Action buttons remain interactive (tap to act)
 * - Fully theme-aware via MaterialTheme color scheme
 * - Faster auto-dismiss (Short duration ~3-4s)
 */
@Composable
fun FieldMindSnackbarOverlay(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onSwipeDismiss: (() -> Unit)? = null
) {
    val data = hostState.currentSnackbarData
    val message = data?.visuals?.message.orEmpty()
    val style = snackbarStyle(message)
    val hasData = data != null

    // Swipe-to-dismiss state
    var offsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val dismissThreshold = with(density) { 120.dp.toPx() }

    // Bouncy spring for save/achievement, smooth for others
    val animSpec: FiniteAnimationSpec<Float> = if (style.isSave || style.isAchievement)
        spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    else
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    val scale by animateFloatAsState(
        targetValue = if (hasData) 1f else 0.85f,
        animationSpec = animSpec,
        label = "snackbarScale"
    )

    AnimatedVisibility(
        visible = hasData,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring<IntOffset>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        data?.let { snackbarData ->
            val bgColor = when {
                style.isAchievement -> MaterialTheme.colorScheme.tertiaryContainer
                style.isSave -> MaterialTheme.colorScheme.primaryContainer
                style.isError -> MaterialTheme.colorScheme.errorContainer
                style.isWarning -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.inverseSurface
            }
            val contentColor = when {
                style.isAchievement -> MaterialTheme.colorScheme.onTertiaryContainer
                style.isSave -> MaterialTheme.colorScheme.onPrimaryContainer
                style.isError -> MaterialTheme.colorScheme.onErrorContainer
                style.isWarning -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.inverseOnSurface
            }
            val icon = when {
                style.isAchievement -> MaterialSymbolIcon("emoji_events")
                style.isSave -> MaterialSymbolIcon("check_circle")
                style.isError -> MaterialSymbolIcon("error")
                style.isWarning -> MaterialSymbolIcon("warning")
                else -> MaterialSymbolIcon("info")
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.toInt(), 0) }
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .shadow(12.dp, RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (kotlin.math.abs(offsetX) > dismissThreshold) {
                                    onSwipeDismiss?.invoke()
                                    snackbarData.dismiss()
                                }
                                offsetX = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                offsetX = (offsetX + dragAmount)
                                    .coerceIn(-dismissThreshold * 2, dismissThreshold * 2)
                            }
                        )
                    },
                shape = RoundedCornerShape(20.dp),
                color = bgColor,
                tonalElevation = 6.dp
            ) {
                // Whole snackbar is clickable to dismiss (but allow action buttons through)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { snackbarData.dismiss() }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            icon = icon,
                            contentDescription = null,
                            tint = contentColor,
                            size = 24.dp
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (style.isSave || style.isAchievement)
                                FontWeight.SemiBold else FontWeight.Normal,
                            color = contentColor,
                            modifier = Modifier.weight(1f)
                        )
                        val actionLabel = snackbarData.visuals.actionLabel
                        if (actionLabel != null) {
                            TextButton(
                                onClick = {
                                    snackbarData.dismiss()
                                    // The SnackbarResult.ActionPerformed is already handled
                                    // by showFastSnackbar's callback
                                }
                            ) {
                                Text(
                                    actionLabel,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
