package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.export.FieldMindExportEncryption
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

/**
 * Unified collapsible "Privacy & encryption" card used in both Export and Backup tabs.
 *
 * Layout matches the user's spec:
 * ┌───────────────────────────────┐
 * │  ▶ Privacy & encryption       │  ← Collapsible
 * ├───────────────────────────────┤
 * │  GPS: [Exact] [Approx] [Rem]  │
 * │  Exclude media: [switch]      │
 * ├───────────────────────────────┤
 * │  Encrypt export: [switch]     │
 * │  [password field]             │
 * │  [████████░░] Strong          │
 * └───────────────────────────────┘
 *
 * In backup mode (isBackup=true), also shows a password confirmation field.
 */
@Composable
fun ExportPrivacyOptionsCard(
    gpsPrivacy: String = "Exact",
    onGpsPrivacyChange: (String) -> Unit = {},
    excludeMedia: Boolean = false,
    onExcludeMediaChange: (Boolean) -> Unit = {},
    encrypt: Boolean = false,
    onEncryptChange: (Boolean) -> Unit = {},
    password: String = "",
    onPasswordChange: (String) -> Unit = {},
    isBackup: Boolean = false,
    passwordConfirm: String = "",
    onPasswordConfirmChange: (String) -> Unit = {},
    passwordsMatch: Boolean = true
) {
    val gpsModes = listOf(
        Triple("Exact", "Full coords", "Include precise latitude/longitude"),
        Triple("Approximate", "~1 km", "Round to 2 decimal places"),
        Triple("Remove", "No location", "Strip all GPS and location text")
    )
    val colors = FieldMindTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            // ── Collapsible header: ▶ Privacy & encryption ──
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    MaterialSymbolIcon(if (expanded) "expand_more" else "play_arrow"),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 20.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Privacy & encryption",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                // Active indicator badge (when collapsed)
                if (!expanded) {
                    val activeItems = buildList {
                        if (gpsPrivacy != "Exact") add("GPS")
                        if (excludeMedia) add("No media")
                        if (encrypt) add("Encrypted")
                    }
                    if (activeItems.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.data.copy(alpha = 0.15f)
                        ) {
                            Text(
                                activeItems.joinToString(", "),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = colors.data
                            )
                        }
                    }
                }
            }

            // ── Expanded content ──
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(Modifier.height(4.dp))

                    // ── GPS: Exact / Approx / Remove ──
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "GPS:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(36.dp)
                            )
                            Row(
                                Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                gpsModes.forEach { (mode, label, _) ->
                                    val selected = gpsPrivacy == mode
                                    Surface(
                                        onClick = { onGpsPrivacyChange(mode) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) colors.data.copy(alpha = 0.14f)
                                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = if (selected) BorderStroke(1.2.dp, colors.data) else null,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            label,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) colors.data
                                                    else MaterialTheme.colorScheme.onSurface,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // ── Exclude media ──
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(Modifier.width(36.dp))
                            Icon(
                                MaterialSymbolIcon(if (excludeMedia) "no_photography" else "photo_camera"),
                                null,
                                tint = if (excludeMedia) MaterialTheme.colorScheme.onSurfaceVariant else colors.data,
                                size = 18.dp
                            )
                            Text(
                                "Exclude media",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = !excludeMedia,
                                onCheckedChange = { onExcludeMediaChange(!it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.data,
                                    checkedTrackColor = colors.data.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    // ── Divider ──
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // ── Encrypt toggle ──
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Spacer(Modifier.width(36.dp))
                        Icon(
                            MaterialSymbolIcon("lock"),
                            null,
                            tint = if (encrypt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 18.dp
                        )
                        Text(
                            if (isBackup) "Encrypt backup" else "Encrypt export",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = encrypt,
                            onCheckedChange = onEncryptChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.error,
                                checkedTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // ── Password field ──
                    if (encrypt) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = onPasswordChange,
                                label = { Text(if (isBackup) "Backup password" else "Export password") },
                                placeholder = { Text("Enter a strong password") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                                trailingIcon = {
                                    if (LocalPrivacyTypingEnabled.current) {
                                        PrivacyTypingIndicator()
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            // ── Password strength ──
                            if (password.isNotBlank()) {
                                val strength = remember(password) {
                                    FieldMindExportEncryption.PasswordStrength.evaluate(password)
                                }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { strength.score / 5f },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = Color(strength.color),
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    )
                                    Text(
                                        strength.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(strength.color)
                                    )
                                }
                            }

                            // ── Password confirmation (backup only) ──
                            if (isBackup) {
                                OutlinedTextField(
                                    value = passwordConfirm,
                                    onValueChange = onPasswordConfirmChange,
                                    label = { Text("Confirm password") },
                                    placeholder = { Text("Re-enter password") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                                    trailingIcon = {
                                        if (LocalPrivacyTypingEnabled.current) {
                                            PrivacyTypingIndicator()
                                        }
                                    },
                                    isError = passwordConfirm.isNotEmpty() && !passwordsMatch,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        errorBorderColor = MaterialTheme.colorScheme.error
                                    )
                                )
                                if (passwordConfirm.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            if (passwordsMatch) FieldMindIcons.Check else FieldMindIcons.Close,
                                            null,
                                            tint = if (passwordsMatch) colors.positive else MaterialTheme.colorScheme.error,
                                            size = 14.dp
                                        )
                                        Text(
                                            if (passwordsMatch) "Passwords match" else "Passwords do not match",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (passwordsMatch) colors.positive else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
