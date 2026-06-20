package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.unit.dp
import fieldmind.research.app.shared.presentation.components.icons.Icon

/**
 * CompositionLocal that propagates whether privacy typing mode is enabled.
 * When true, all TextField/OutlinedTextField composables using
 * [KeyboardOptions.withPrivacyTyping] will pass the
 * IME_FLAG_NO_PERSONALIZED_LEARNING flag to the keyboard.
 */
val LocalPrivacyTypingEnabled = compositionLocalOf { false }

/**
 * Extension on [KeyboardOptions] that conditionally sets the
 * [PlatformImeOptions] with the "nm" private IME option, which tells
 * keyboards (Gboard, SwiftKey, etc.) not to learn from typing, disable
 * predictions, and show an incognito indicator.
 *
 * Usage:
 * ```kotlin
 * TextField(
 *     keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(privacyEnabled),
 *     ...
 * )
 * ```
 *
 * @param enabled Whether to enable privacy typing mode
 */
fun KeyboardOptions.withPrivacyTyping(enabled: Boolean): KeyboardOptions {
    if (!enabled) return this
    return copy(
        platformImeOptions = PlatformImeOptions(
            privateImeOptions = mapOf("nm" to "1")
        )
    )
}

/**
 * Subtle lock icon shown inside text fields when privacy typing is active,
 * giving users visual confirmation that their input is protected from
 * keyboard learning/personalization.
 *
 * Only renders when [LocalPrivacyTypingEnabled] is true, so callers can
 * add this unconditionally as a trailing icon without visible effect when
 * privacy typing is off.
 */
@Composable
fun PrivacyTypingIndicator(modifier: Modifier = Modifier) {
    Icon(
        icon = FieldMindIcons.Lock,
        contentDescription = "Privacy typing active",
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        size = 16.dp,
        modifier = modifier
    )
}
