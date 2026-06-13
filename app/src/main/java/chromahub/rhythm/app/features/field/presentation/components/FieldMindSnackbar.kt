package fieldmind.research.app.features.field.presentation.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Centralized snackbar system for FieldMind screens.
 * Replaces all android.widget.Toast calls with Material3 Snackbars.
 *
 * Usage:
 *   val snackbar = LocalFieldMindSnackbar.current
 *   scope.launch { snackbar.showSnackbar("Observation saved") }
 */
val LocalFieldMindSnackbar = compositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided. Wrap your FieldMind content with FieldMindSnackbarProvider.")
}

@Composable
fun FieldMindSnackbarProvider(content: @Composable () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    CompositionLocalProvider(LocalFieldMindSnackbar provides snackbarHostState) {
        content()
    }
}

/**
 * Helper to show a snackbar from any composable with access to LocalFieldMindSnackbar.
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
    fun show(message: String) {
        scope.launch { hostState.showSnackbar(message, duration = SnackbarDuration.Short) }
    }

    fun showWithAction(
        message: String,
        actionLabel: String,
        onAction: () -> Unit
    ) {
        scope.launch {
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    fun showPersistent(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        scope.launch {
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }

    fun showError(message: String) {
        scope.launch { hostState.showSnackbar("⚠ $message", duration = SnackbarDuration.Long) }
    }
}
