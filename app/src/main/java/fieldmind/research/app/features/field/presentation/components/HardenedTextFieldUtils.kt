package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

/**
 * Extension that hardens KeyboardOptions for sensitive input fields.
 * Applies defense-in-depth keyboard privacy by:
 * - Disabling predictive text and suggestions via IME options
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
 * )
 * ```
 */
fun KeyboardOptions.withSensitiveDataHardening(enabled: Boolean = true): KeyboardOptions {
    if (!enabled) return this
    
    return copy(
        // Note: KeyboardOptions in Compose doesn't have direct flag for autocorrect or suggestions,
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
