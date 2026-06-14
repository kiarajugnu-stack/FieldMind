package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Shows a snackbar message. Cancels any previous snackbar first so messages never stack.
 * Uses Short duration by default.
 */
fun showFastSnackbar(
    hostState: SnackbarHostState,
    scope: CoroutineScope,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    scope.launch {
        hostState.currentSnackbarData?.dismiss()
        val result = hostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
    }
}

private fun snackbarStyle(message: String): Triple<Boolean, Boolean, Boolean> {
    val isSave = message.startsWith("Observation saved") || message.startsWith("Saved") || message.startsWith("Quick snap saved")
    val isError = message.startsWith("⚠") || message.contains("required") || message.contains("denied") || message.contains("Couldn't") || message.contains("cancelled")
    val isWarning = !isSave && !isError && (message.contains("empty") || message.contains("unavailable"))
    return Triple(isSave, isError, isWarning)
}

/**
 * A custom top-positioned snackbar overlay.
 *
 * - Appears at the top with slide-in + bouncy scale animation
 * - Save confirmations get check icon + primary container
 * - Errors get error icon + error container
 * - Dark/light mode via MaterialTheme
 */
@Composable
fun FieldMindSnackbarOverlay(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val data = hostState.currentSnackbarData
    val message = data?.visuals?.message.orEmpty()
    val (isSave, isError, isWarning) = snackbarStyle(message)
    val hasData = data != null

    // Bouncy spring for save confirmations, smooth for others
    val animSpec = if (isSave)
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    else
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    val scale by animateFloatAsState(
        targetValue = if (hasData) 1f else 0.85f,
        animationSpec = animSpec,
        label = "snackbarScale"
    )

    AnimatedVisibility(
        visible = hasData,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeIn(),
        modifier = modifier
    ) {
        data?.let {
            val bgColor = when {
                isSave -> MaterialTheme.colorScheme.primaryContainer
                isError -> MaterialTheme.colorScheme.errorContainer
                isWarning -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.inverseSurface
            }
            val contentColor = when {
                isSave -> MaterialTheme.colorScheme.onPrimaryContainer
                isError -> MaterialTheme.colorScheme.onErrorContainer
                isWarning -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onInverseSurface
            }
            val icon = when {
                isSave -> MaterialSymbolIcon("check_circle")
                isError -> MaterialSymbolIcon("error")
                isWarning -> MaterialSymbolIcon("warning")
                else -> MaterialSymbolIcon("info")
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .shadow(8.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = bgColor,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(icon = icon, contentDescription = null, tint = contentColor, size = 22.dp)
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSave) FontWeight.SemiBold else FontWeight.Normal,
                        color = contentColor,
                        modifier = Modifier.weight(1f)
                    )
                    val actionLabel = it.visuals.actionLabel
                    if (actionLabel != null) {
                        TextButton(onClick = { it.dismiss() }) {
                            Text(actionLabel, fontWeight = FontWeight.Bold, color = contentColor)
                        }
                    }
                }
            }
        }
    }
}
