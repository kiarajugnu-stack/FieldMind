package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.input.PlatformImeOptions

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
