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
 * Layout:
 *   ▶ Privacy & encryption  (collapsible header)
 *   ───────────────────────────────
 *    GPS: Exact / Approx / Remove
 *    Exclude media: [switch]
 *   ─── (divider) ─────────────────
 *    Encrypt export/backup: [switch]
 *    [password field]
 *    [████████░░] Strong
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
            // ── Collapsible header ──
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    FieldMindIcons.Lock,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 20.dp
                )
                Text(
                    "Privacy & encryption",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // Active indicators
                if (!expanded) {
                    val activeCount = listOfNotNull(
                        if (gpsPrivacy != "Exact") "GPS" else null,
                        if (excludeMedia) "No media" else null,
                        if (encrypt) "Encrypt" else null
                    )
                    if (activeCount.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                activeCount.joinToString(", "),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Icon(
                    if (expanded) FieldMindIcons.ExpandLess else FieldMindIcons.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 22.dp
                )
            }

            // ── Expanded content ──
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Spacer(Modifier.height(4.dp))

                    // ── GPS precision ──
                    Text(
                        "GPS precision in export",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gpsModes.forEach { (mode, label, desc) ->
                            val selected = gpsPrivacy == mode
                            Surface(
                                onClick = { onGpsPrivacyChange(mode) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // ── Exclude media toggle ──
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            FieldMindIcons.Camera,
                            null,
                            tint = if (excludeMedia) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            size = 18.dp
                        )
                        Text(
                            "Exclude media attachments",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = !excludeMedia, onCheckedChange = { onExcludeMediaChange(!it) })
                    }

                    // ── Divider ──
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // ── Encrypt toggle ──
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            FieldMindIcons.Lock,
                            null,
                            tint = if (encrypt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 18.dp
                        )
                        Text(
                            if (isBackup) "Encrypt backup" else "Encrypt export",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = encrypt, onCheckedChange = onEncryptChange)
                    }

                    // ── Password field (when encrypt is on) ──
                    if (encrypt) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            label = { Text(if (isBackup) "Backup password" else "Export password") },
                            placeholder = { Text("Enter a strong password") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                            trailingIcon = {
                                if (LocalPrivacyTypingEnabled.current) {
                                    PrivacyTypingIndicator()
                                }
                            }
                        )

                        // ── Password strength ──
                        if (password.isNotBlank()) {
                            val strength = remember(password) { FieldMindExportEncryption.PasswordStrength.evaluate(password) }
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { strength.score / 5f },
                                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = Color(strength.color),
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                Text(
                                    strength.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(strength.color)
                                )
                            }
                        }

                        // ── Password confirmation (backup mode only) ──
                        if (isBackup) {
                            OutlinedTextField(
                                value = passwordConfirm,
                                onValueChange = onPasswordConfirmChange,
                                label = { Text("Confirm password") },
                                placeholder = { Text("Re-enter password") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                                trailingIcon = {
                                    if (LocalPrivacyTypingEnabled.current) {
                                        PrivacyTypingIndicator()
                                    }
                                },
                                isError = passwordConfirm.isNotEmpty() && !passwordsMatch
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
                                        color = if (passwordsMatch) colors.positive else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
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
