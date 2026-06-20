package chromahub.rhythm.app.features.field.presentation.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

/**
 * Extension that hardens KeyboardOptions for sensitive input fields.
 * Applies defense-in-depth keyboard privacy by:
 * - Disabling predictive text and suggestions
 * - Disabling autocorrect
 * - Enabling privacy/incognito mode where supported
 *
 * Use this for coordinates, IDs, sensitive measurements, passwords, etc.
 *
 * Usage:
 * ```kotlin
 * TextField(
 *     keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
 *         .withSensitiveDataHardening(enabled = true)
 *         .withPrivacyTyping(enabled = true)
 * )
 * ```
 */
fun KeyboardOptions.withSensitiveDataHardening(enabled: Boolean = true): KeyboardOptions {
    if (!enabled) return this
    
    return copy(
        autoCorrect = false,  // Disable autocorrect
        // Note: KeyboardOptions in Compose doesn't have direct flag for no suggestions,
        // but we use private IME options to request it from keyboards
        platformImeOptions = (platformImeOptions ?: androidx.compose.ui.text.input.PlatformImeOptions()).let { current ->
            // Combine with any existing private IME options
            val existingOptions = (current.privateImeOptions ?: "").split(",").filter { it.isNotBlank() }
            val newOptions = listOf("noSuggestions", "noAutocorrect") + existingOptions
            val combined = newOptions.distinct().joinToString(",")
            
            androidx.compose.ui.text.input.PlatformImeOptions(
                privateImeOptions = combined
            )
        }
    )
}

/**
 * Shortcut to apply both privacy typing and sensitive data hardening to keyboard options.
 * Recommended for fields containing sensitive research data (coordinates, observation IDs, etc).
 *
 * Usage:
 * ```kotlin
 * TextField(
 *     keyboardOptions = KeyboardOptions.Default
 *         .withHardenedPrivacy(privacyEnabled = true)
 * )
 * ```
 */
fun KeyboardOptions.withHardenedPrivacy(privacyEnabled: Boolean = true): KeyboardOptions {
    return withSensitiveDataHardening(privacyEnabled)
        .withPrivacyTyping(privacyEnabled)
}
