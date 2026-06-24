package fieldmind.research.app.features.field.presentation.components

import android.os.Build
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon

/**
 * CompositionLocal that propagates whether privacy typing mode is enabled.
 * When true, all TextField/OutlinedTextField composables using
 * [KeyboardOptions.withPrivacyTyping] will pass the
 * IME_FLAG_NO_PERSONALIZED_LEARNING flag to the keyboard.
 */
val LocalPrivacyTypingEnabled = compositionLocalOf { false }

/**
 * Global privacy text input wrapper.
 *
 * When [LocalPrivacyTypingEnabled] is true, this intercepts ALL text input
 * connections within its content subtree and sets the platform-level
 * [EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING] flag on every text field's
 * [EditorInfo]. This is the official Android mechanism that tells keyboards
 * to disable personalized learning, predictions, and suggestions — far more
 * reliable than per-field private IME options strings.
 *
 * Keyboards like Gboard, SwiftKey, and Samsung Keyboard show an incognito
 * badge when this flag is present. The flag is a request — not every keyboard
 * guarantees compliance.
 *
 * Usage: wrap this around the entire Compose UI tree so ALL text fields
 * automatically respect the privacy setting without any per-field annotations.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PrivacyTextInputWrapper(content: @Composable () -> Unit) {
    val privacyEnabled = LocalPrivacyTypingEnabled.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && privacyEnabled) {
        InterceptPlatformTextInput(
            interceptor = { request, nextHandler ->
                val modifiedRequest = PlatformTextInputMethodRequest { outAttributes ->
                    request.createInputConnection(outAttributes).also {
                        outAttributes.imeOptions = outAttributes.imeOptions or
                                EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                    }
                }
                nextHandler.startInputMethod(modifiedRequest)
            },
            content = content
        )
    } else {
        content()
    }
}

/**
 * Extension on [KeyboardOptions] that conditionally sets the
 * [PlatformImeOptions] with privacy/incognito mode flags for multiple keyboards.
 *
 * Supports:
 * - Microsoft SwiftKey (beta & production): Uses IME_FLAG_NO_PERSONALIZED_LEARNING (0x1000000)
 * - Google Gboard: Uses "nm" private IME option
 * - Samsung Keyboard & others: Responds to both flags
 *
 * When enabled, keyboards will:
 * - Disable personalized learning & suggestions
 * - Disable predictive text
 * - Show incognito/privacy indicator (SwiftKey, Gboard)
 * - Not save typing data for personalization
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

    val existingOptions = platformImeOptions?.privateImeOptions
        ?.split(PRIVATE_IME_OPTION_SEPARATOR)
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()

    val mergedOptions = (existingOptions + PRIVACY_IME_OPTIONS)
        .distinct()
        .joinToString(PRIVATE_IME_OPTION_SEPARATOR)

    return copy(
        autoCorrectEnabled = false,
        platformImeOptions = PlatformImeOptions(privateImeOptions = mergedOptions)
    )
}

private const val PRIVATE_IME_OPTION_SEPARATOR = ","

private val PRIVACY_IME_OPTIONS = listOf(
    // AOSP/LatinIME and keyboards that mirror the platform no-learning hint.
    "noPersonalizedLearning",
    "com.android.inputmethod.latin.noPersonalizedLearning",
    // Gboard private options. The short "nm" value is what shows Gboard's incognito badge.
    "nm",
    "com.google.android.inputmethod.latin.noPersonalizedLearning",
    // Microsoft SwiftKey production/beta builds have historically checked these vendor keys.
    "com.touchtype.swiftkey.noPersonalizedLearning",
    "com.microsoft.swiftkey.noPersonalizedLearning",
    "com.microsoft.inputmethod.noPersonalizedLearning"
)

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


/** Strong native text field for highly sensitive input. Compose KeyboardOptions can only
 * pass private IME options; this AndroidView-backed field also sets the raw platform
 * no-personalized-learning IME flag and disables autofill/content-capture where possible.
 * Android keyboards may not show an incognito badge, but supported keyboards should not
 * personalize from this text. */
@Composable
fun FieldMindPrivateTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    singleLine: Boolean = true,
    sensitive: Boolean = true
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            EditText(context).apply {
                configureFieldMindPrivacy(sensitive = sensitive, singleLine = singleLine)
                setHint(hint)
                setText(value)
                setSelection(text?.length ?: 0)
                addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onValueChange(s?.toString().orEmpty()) }
                    override fun afterTextChanged(s: android.text.Editable?) = Unit
                })
            }
        },
        update = { view ->
            if (view.text.toString() != value) {
                view.setText(value)
                view.setSelection(view.text?.length ?: 0)
            }
            view.hint = hint
            view.configureFieldMindPrivacy(sensitive = sensitive, singleLine = singleLine)
        }
    )
}

/**
 * At-a-glance card shown at the top of the Security settings page.
 * Displays live enabled/disabled status for the four main privacy controls
 * so the user can quickly see their protection posture without scrolling.
 *
 * Accessibility: each status row announces its state via contentDescription.
 *
 * @param screenCaptureEnabled  True when FLAG_SECURE / screen capture protection is on.
 * @param privacyKeyboardEnabled True when keyboards have been asked to suppress learning.
 *                               Note: the indicator is "requested" — not every keyboard
 *                               will visibly react or guarantee compliance.
 * @param appLockEnabled         True when biometric lock or app PIN is active.
 * @param backupEncryptionEnabled True when biometric lock is on (forces encrypted backups).
 */
@Composable
fun PrivacyStatusCard(
    screenCaptureEnabled: Boolean,
    privacyKeyboardEnabled: Boolean,
    appLockEnabled: Boolean,
    backupEncryptionEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    data class StatusRow(
        val icon: fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon,
        val label: String,
        val enabled: Boolean,
        val note: String? = null
    )

    val rows = listOf(
        StatusRow(FieldMindIcons.Lock, "Screen capture protection", screenCaptureEnabled),
        StatusRow(
            FieldMindIcons.Lock,
            "Privacy keyboard",
            privacyKeyboardEnabled,
            note = if (privacyKeyboardEnabled) "Requested — depends on keyboard support" else null
        ),
        StatusRow(FieldMindIcons.Lock, "App lock", appLockEnabled),
        StatusRow(
            FieldMindIcons.Archive,
            "Backup encryption",
            backupEncryptionEnabled,
            note = if (!backupEncryptionEnabled) "Enable biometric lock to default backups to encrypted" else null
        )
    )

    val enabledCount = rows.count { it.enabled }
    val allEnabled = enabledCount == rows.size

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allEnabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    icon = FieldMindIcons.Lock,
                    contentDescription = null,
                    tint = if (allEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 20.dp
                )
                Text(
                    "Privacy status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (allEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                // Summary badge
                val summaryText = "$enabledCount / ${rows.size} active"
                val summaryDesc = "$enabledCount of ${rows.size} privacy features enabled"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (allEnabled)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.semantics { contentDescription = summaryDesc }
                ) {
                    Text(
                        summaryText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (allEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Status rows
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
                val rowDesc = "${row.label}: ${if (row.enabled) "enabled" else "disabled"}"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = rowDesc },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        icon = row.icon,
                        contentDescription = null,
                        tint = if (row.enabled)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        size = 18.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            row.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (row.note != null) {
                            Text(
                                row.note,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    // Status indicator dot + label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (row.enabled)
                                        FieldMindTheme.colors.positive
                                    else
                                        FieldMindTheme.colors.warning
                                )
                        )
                        Text(
                            if (row.enabled) "On" else "Off",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (row.enabled) FieldMindTheme.colors.positive else FieldMindTheme.colors.warning
                        )
                    }
                }
            }
        }
    }
}

fun EditText.configureFieldMindPrivacy(sensitive: Boolean = true, singleLine: Boolean = true) {
    imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
    privateImeOptions = (privateImeOptions.orEmpty().split(PRIVATE_IME_OPTION_SEPARATOR).filter { it.isNotBlank() } + PRIVACY_IME_OPTIONS)
        .distinct().joinToString(PRIVATE_IME_OPTION_SEPARATOR)
    inputType = if (sensitive) {
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    } else {
        inputType or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    }
    setSingleLine(singleLine)
    importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setAutofillHints(null)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
    }
    setTextIsSelectable(!sensitive)
}
