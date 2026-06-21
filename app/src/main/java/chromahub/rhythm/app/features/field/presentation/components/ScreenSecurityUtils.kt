package fieldmind.research.app.features.field.presentation.components

import android.app.Activity
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Composable that applies FLAG_SECURE to the current window to prevent
 * screenshots, screen recordings, and preview in Recent Apps.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun SensitiveScreen() {
 *     ScreenCaptureProtection(enabled = true)
 *     // ... screen content
 * }
 * ```
 *
 * @param enabled Whether to enable screen capture protection. When false, removes the flag.
 * @param onDispose Optional callback when the effect is disposed
 */
@Composable
fun ScreenCaptureProtection(enabled: Boolean = true, onDispose: (() -> Unit)? = null) {
    val window = LocalContext.current.findActivity()?.window

    DisposableEffect(window, enabled) {
        window?.let { applyScreenCaptureProtection(it, enabled) }

        onDispose {
            if (enabled) {
                window?.let { applyScreenCaptureProtection(it, false) }
            }
            onDispose?.invoke()
        }
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Helper function to apply FLAG_SECURE from a non-Composable context.
 * Use this in Activities or ViewModels that need direct access.
 *
 * @param window The window to protect
 * @param enabled Whether to enable or disable screen capture protection
 */
fun applyScreenCaptureProtection(window: android.view.Window, enabled: Boolean) {
    if (enabled) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
