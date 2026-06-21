package fieldmind.research.app.features.field.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * State holder for a validated form field.
 * Tracks value, error state, and validation logic.
 *
 * Usage:
 *   val nameState = rememberRequiredFieldState(validator = { if (it.isBlank()) "Required" else null })
 */
class RequiredFieldState(
    initialValue: String = "",
    private val validator: (String) -> String? = { null },
    private val label: String = "Field"
) {
    var value by mutableStateOf(initialValue)
        private set

    var isTouched by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    val isValid: Boolean get() = error == null

    val isRequired: Boolean get() = validator !== EMPTY_VALIDATOR

    fun onValueChange(newValue: String) {
        value = newValue
        if (isTouched) validate()
    }

    fun onTouch() {
        isTouched = true
        validate()
    }

    fun validate(): Boolean {
        error = validator(value)
        isTouched = true
        return error == null
    }

    fun reset() {
        value = ""
        isTouched = false
        error = null
    }

    companion object {
        private val EMPTY_VALIDATOR: (String) -> String? = { null }
    }
}

@Composable
fun rememberRequiredFieldState(
    initialValue: String = "",
    label: String = "Field",
    validator: (String) -> String? = { null }
): RequiredFieldState = remember { RequiredFieldState(initialValue, validator, label) }

/**
 * Validates all provided states and returns true if all are valid.
 * Triggers validation on all fields.
 */
fun validateAll(vararg states: RequiredFieldState): Boolean {
    return states.all { it.validate() }
}
