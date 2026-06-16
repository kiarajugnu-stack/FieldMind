package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Centralized snackbar system for FieldMind screens.
 * Replaces all android.widget.Toast calls with Material3 Snackbars.
 * Supports swipe-to-dismiss and auto-dismiss after Short duration.
 *
 * Usage:
 *   val snackbar = LocalFieldMindSnackbar.current
 *   scope.launch { snackbar.showSnackbar("Observation saved") }
 */
val LocalFieldMindSnackbar = compositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided. Wrap your FieldMind content with FieldMindSnackbarProvider.")
}

@Composable
fun FieldMindSnackbarProvider(content: @Composable (PaddingValues) -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    CompositionLocalProvider(LocalFieldMindSnackbar provides snackbarHostState) {
        Scaffold(
            snackbarHost = {
                SwipeableSnackbarHost(snackbarHostState)
            }
        ) { padding ->
            content(padding)
        }
    }
}

/**
 * A SnackbarHost wrapper that adds swipe-to-dismiss gesture.
 * Users can swipe the snackbar horizontally to dismiss it immediately.
 */
@Composable
private fun SwipeableSnackbarHost(hostState: SnackbarHostState) {
    var offsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    SnackbarHost(hostState = hostState) { data ->
        val dismissThreshold = with(density) { 100.dp.toPx() }

        Surface(
            modifier = Modifier
                .offset { IntOffset(offsetX.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (kotlin.math.abs(offsetX) > dismissThreshold) {
                                data.dismiss()
                            }
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-dismissThreshold * 2, dismissThreshold * 2)
                        }
                    )
                },
            shape = RoundedCornerShape(16.dp)
        ) {
            Snackbar(
                modifier = Modifier.offset { IntOffset(offsetX.toInt(), 0) }
            ) {
                Text(text = data.visuals.message)
            }
        }
    }
}

/**
 * Helper to show a snackbar from any composable with access to LocalFieldMindSnackbar.
 * All messages automatically dismiss after Short duration and dismiss any previous message first.
 */
@Composable
fun rememberFieldMindSnackbar(): FieldMindSnackbarHelper {
    val snackbar = LocalFieldMindSnackbar.current
    val scope = rememberCoroutineScope()
    return remember(snackbar, scope) { FieldMindSnackbarHelper(snackbar, scope) }
}

class FieldMindSnackbarHelper(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    /**
     * Show a snackbar for Short duration. Dismisses any previous snackbar first.
     */
    fun show(message: String) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    /**
     * Show a snackbar with an action button. Dismisses any previous snackbar first.
     */
    fun showWithAction(
        message: String,
        actionLabel: String,
        onAction: () -> Unit
    ) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    /**
     * Show a snackbar for a slightly longer duration. Dismisses any previous first.
     */
    fun showPersistent(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }

    /**
     * Show an error snackbar. Dismisses any previous snackbar first.
     */
    fun showError(message: String) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar("⚠ $message", duration = SnackbarDuration.Long)
        }
    }
}
