package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.features.field.data.settings.FieldMindSettings
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.platform.LocalDensity

// ══════════════════════════════════════════════════════════════════════
//  Security Score Detail Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun SecurityScoreDetailPage(settings: FieldMindSettings, onBack: () -> Unit) {
    val privacyLock by settings.privacyLockEnabled.collectAsState()
    val appPinEnabled by settings.appPinEnabled.collectAsState()
    val appPinHash by settings.appPinHash.collectAsState()
    val screenCapture by settings.screenCaptureProtectionEnabled.collectAsState()
    val backupEncryption by settings.autoBackupEnabled.collectAsState()
    val clipboardCleanup by settings.clipboardAutoCleanupEnabled.collectAsState()
    val appLockActive = privacyLock || (appPinEnabled && appPinHash.isNotBlank())

    val enabledItems = listOfNotNull(
        "Device Lock" to appLockActive,
        "Backup Encryption" to backupEncryption,
        "Clipboard Protection" to clipboardCleanup,
        "Privacy Keyboard" to settings.privacyTypingEnabled.value,
        "Screen Capture Block" to screenCapture
    )
    val totalItems = 5
    val enabledCount = enabledItems.count { it.second }
    val score = (enabledCount * 100) / totalItems

    val recommendedItems = listOfNotNull(
        if (!appLockActive) "Enable App PIN" else null,
        if (!screenCapture) "Enable Screenshot Blocking" else null,
        if (!backupEncryption) "Enable Backup Encryption" else null
    )

    SettingsSubPage(onBack = onBack, title = "Security Score", icon = MaterialSymbolIcon("security")) {
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Score circle
                    val scoreColor = when {
                        score >= 80 -> Color(0xFF4CAF50)
                        score >= 50 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                    Box(
                        Modifier.size(120.dp).clip(CircleShape).background(scoreColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$score",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor
                            )
                            Text(
                                "/ 100",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = scoreColor,
                        trackColor = scoreColor.copy(alpha = 0.12f)
                    )

                    Text(
                        "$enabledCount of $totalItems protections active",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Enabled items
        if (enabledItems.any { it.second }) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Enabled", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        enabledItems.filter { it.second }.forEach { (name, _) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(MaterialSymbolIcon("check_circle"), null, tint = Color(0xFF4CAF50), size = 18.dp)
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        // Recommended items
        if (recommendedItems.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Recommended", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                        recommendedItems.forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(MaterialSymbolIcon("warning"), null, tint = Color(0xFFFF9800), size = 18.dp)
                                Text(item, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Export Protection Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ExportProtectionPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val passwordProtection by settings.exportPasswordProtectionEnabled.collectAsState()
    val encryptionLevel by settings.exportEncryptionLevel.collectAsState()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    SettingsSubPage(onBack = onBack, title = "Export Protection", icon = FieldMindIcons.Export) {
        item {
            SettingsGroupCard {
                ToggleItem(
                    "Password Protected Exports",
                    "Protect exported files with a password",
                    passwordProtection,
                    settings::setExportPasswordProtectionEnabled,
                    MaterialSymbolIcon("lock")
                )
                if (passwordProtection) {
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Surface(
                        onClick = { showPasswordDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(MaterialSymbolIcon("password"), null, tint = FieldMindTheme.colors.hypothesis, size = 20.dp)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (settings.exportPasswordHash.value.isNotBlank()) "Change export password" else "Set export password",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (settings.exportPasswordHash.value.isNotBlank()) "••••••••••" else "No password set",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(FieldMindIcons.Forward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                        }
                    }
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
        item {
            SettingsGroupCard {
                ChoiceItemForm(
                    "Encryption Level",
                    listOf("Standard", "Strong", "Maximum"),
                    encryptionLevel,
                    FieldMindIcons.Lock,
                    settings::setExportEncryptionLevel
                )
                if (encryptionLevel == "Maximum") {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(MaterialSymbolIcon("warning"), null, tint = MaterialTheme.colorScheme.error, size = 18.dp)
                            Text(
                                "Maximum encryption may reduce export/import speed on large datasets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false; passwordInput = ""; passwordConfirm = ""; passwordError = false },
            icon = { Icon(FieldMindIcons.Lock, null, size = 28.dp) },
            title = { Text(if (settings.exportPasswordHash.value.isNotBlank()) "Change export password" else "Set export password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter a password to protect exported files. You'll need this to open them later.")
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { if (it.length <= 32) { passwordInput = it; passwordError = false } },
                        label = { Text("Password") },
                        singleLine = true,
                        isError = passwordError,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        textStyle = MaterialTheme.typography.titleMedium.copy(letterSpacing = 4.sp, textAlign = TextAlign.Center)
                    )
                    OutlinedTextField(
                        value = passwordConfirm,
                        onValueChange = { if (it.length <= 32) { passwordConfirm = it; passwordError = false } },
                        label = { Text("Confirm password") },
                        singleLine = true,
                        isError = passwordError,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        textStyle = MaterialTheme.typography.titleMedium.copy(letterSpacing = 4.sp, textAlign = TextAlign.Center)
                    )
                    if (passwordError) {
                        Text("Passwords don't match or too short", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordInput.length >= 4 && passwordInput == passwordConfirm) {
                            settings.setExportPasswordHash(settings.hashExportPassword(passwordInput))
                            showPasswordDialog = false
                            passwordInput = ""; passwordConfirm = ""; passwordError = false
                        } else {
                            passwordError = true
                        }
                    },
                    enabled = passwordInput.length >= 4 && passwordConfirm.length >= 4
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false; passwordInput = ""; passwordConfirm = ""; passwordError = false }) { Text("Cancel") }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  App Preview Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun AppPreviewPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val previewMode by settings.appPreviewMode.collectAsState()

    SettingsSubPage(onBack = onBack, title = "App Preview", icon = MaterialSymbolIcon("visibility")) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("How should FieldMind appear in recent apps?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))

                    listOf(
                        "Normal" to "Show content normally in the app switcher",
                        "Blur Content" to "Blur the preview thumbnail in recent apps",
                        "Privacy Screen" to "Show a generic screen with no data visible"
                    ).forEach { (mode, desc) ->
                        val selected = previewMode == mode
                        Surface(
                            onClick = { settings.setAppPreviewMode(mode) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selected, onClick = { settings.setAppPreviewMode(mode) })
                                Column(Modifier.weight(1f)) {
                                    Text(mode, fontWeight = FontWeight.SemiBold)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Preview card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    when (previewMode) {
                        "Normal" -> {
                            Box(
                                Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("FieldMind • Recent observation data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        "Blur Content" -> {
                            Box(
                                Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("FieldMind", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.blur(radius = 8.dp))
                            }
                        }
                        "Privacy Screen" -> {
                            Box(
                                Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp))
                                    .background(FieldMindTheme.colors.hypothesis.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(MaterialSymbolIcon("lock"), null, tint = FieldMindTheme.colors.hypothesis, size = 24.dp)
                                    Text("FieldMind", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = FieldMindTheme.colors.hypothesis)
                                    Text("Research data hidden", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("NewApi")
private fun Modifier.blur(radius: androidx.compose.ui.unit.Dp): Modifier = this.then(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.composed {
            val density = LocalDensity.current
            val radiusPx = with(density) { radius.toPx() }
            val renderEffect = remember(radiusPx) {
                android.graphics.RenderEffect.createBlurEffect(
                    radiusPx,
                    radiusPx,
                    android.graphics.Shader.TileMode.MIRROR
                ).asComposeRenderEffect()
            }
            Modifier.graphicsLayer { this.renderEffect = renderEffect }
        }
    } else {
        Modifier.alpha(0.4f)
    }
)

// ══════════════════════════════════════════════════════════════════════
//  Failed Unlocks Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun FailedUnlocksPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val cooldown by settings.failedUnlockCooldown.collectAsState()
    val requireBiometrics by settings.failedUnlockRequireBiometrics.collectAsState()
    val panicLock by settings.failedUnlockPanicLock.collectAsState()

    SettingsSubPage(onBack = onBack, title = "Failed Unlocks", icon = MaterialSymbolIcon("lock_person")) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("After 5 failed attempts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))

                    listOf("Do Nothing", "30 Second Cooldown", "5 Minute Cooldown").forEach { option ->
                        val selected = cooldown == option
                        Surface(
                            onClick = { settings.setFailedUnlockCooldown(option) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selected, onClick = { settings.setFailedUnlockCooldown(option) })
                                Text(option, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsGroupCard {
                ToggleItem(
                    "Require Biometrics",
                    "After failed attempts, require fingerprint or face to unlock",
                    requireBiometrics,
                    settings::setFailedUnlockRequireBiometrics,
                    MaterialSymbolIcon("fingerprint")
                )
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem(
                    "Panic Lock",
                    "After failed attempts, wipe sensitive data and reset the app",
                    panicLock,
                    settings::setFailedUnlockPanicLock,
                    MaterialSymbolIcon("warning")
                )
            }
        }

        if (panicLock) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(MaterialSymbolIcon("warning"), null, tint = MaterialTheme.colorScheme.error, size = 18.dp)
                        Text(
                            "Panic lock will permanently delete locally cached data after repeated failed unlock attempts. Use with caution.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Metadata Protection Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun MetadataProtectionPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val removeGps by settings.metadataRemoveGps.collectAsState()
    val removeCamera by settings.metadataRemoveCamera.collectAsState()
    val removeDevice by settings.metadataRemoveDevice.collectAsState()
    val removeExif by settings.metadataRemoveExif.collectAsState()

    SettingsSubPage(onBack = onBack, title = "Metadata Protection", icon = MaterialSymbolIcon("perm_media")) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(MaterialSymbolIcon("info"), null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                    Text(
                        "Strip sensitive metadata from photos and files when exporting. This helps protect your privacy when sharing research data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        item {
            SettingsGroupCard {
                ToggleItem("Remove GPS Coordinates", "Strip location data from exported media files", removeGps, settings::setMetadataRemoveGps, MaterialSymbolIcon("location_off"))
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Remove Camera Information", "Strip camera model, make, and settings", removeCamera, settings::setMetadataRemoveCamera, MaterialSymbolIcon("camera_alt"))
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Remove Device Information", "Strip device name and identifiers", removeDevice, settings::setMetadataRemoveDevice, MaterialSymbolIcon("smartphone"))
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Remove EXIF Data", "Strip all EXIF metadata from images", removeExif, settings::setMetadataRemoveExif, MaterialSymbolIcon("image"))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Decoy PIN Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun DecoyPinPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val decoyEnabled by settings.decoyPinEnabled.collectAsState()
    val decoyLabel by settings.decoyPinLabel.collectAsState()
    var showSetupDialog by remember { mutableStateOf(false) }
    var decoyInput by remember { mutableStateOf("") }
    var decoyConfirm by remember { mutableStateOf("") }
    var decoyLabelInput by remember { mutableStateOf("") }
    var decoyError by remember { mutableStateOf(false) }

    SettingsSubPage(onBack = onBack, title = "Decoy PIN", icon = MaterialSymbolIcon("lock_open")) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(MaterialSymbolIcon("info"), null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                    Text(
                        "A decoy PIN opens a fake, empty version of the app when entered instead of your real PIN. " +
                        "This protects your data if someone forces you to unlock the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        item {
            SettingsGroupCard {
                ToggleItem(
                    "Enable Decoy PIN",
                    "Set a secondary PIN that opens a clean version of the app",
                    decoyEnabled,
                    settings::setDecoyPinEnabled,
                    MaterialSymbolIcon("lock_open")
                )
                if (decoyEnabled) {
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Surface(
                        onClick = { showSetupDialog = true; decoyLabelInput = decoyLabel },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(MaterialSymbolIcon("password"), null, tint = FieldMindTheme.colors.hypothesis, size = 20.dp)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (settings.decoyPinHash.value.isNotBlank()) "Change decoy PIN" else "Set decoy PIN",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (decoyLabel.isNotBlank()) "Label: $decoyLabel" else "No decoy PIN set",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(FieldMindIcons.Forward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                        }
                    }
                }
            }
        }
    }

    if (showSetupDialog) {
        AlertDialog(
            onDismissRequest = { showSetupDialog = false; decoyInput = ""; decoyConfirm = ""; decoyLabelInput = ""; decoyError = false },
            icon = { Icon(MaterialSymbolIcon("lock_open"), null, size = 28.dp) },
            title = { Text("Set decoy PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter a 4-6 digit PIN that opens a clean, empty version of FieldMind.")
                    OutlinedTextField(
                        value = decoyInput,
                        onValueChange = { if (it.length <= 6) { decoyInput = it; decoyError = false } },
                        label = { Text("Decoy PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 8.sp, textAlign = TextAlign.Center)
                    )
                    OutlinedTextField(
                        value = decoyConfirm,
                        onValueChange = { if (it.length <= 6) { decoyConfirm = it; decoyError = false } },
                        label = { Text("Confirm decoy PIN") },
                        singleLine = true,
                        isError = decoyError,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 8.sp, textAlign = TextAlign.Center)
                    )
                    OutlinedTextField(
                        value = decoyLabelInput,
                        onValueChange = { if (it.length <= 40) decoyLabelInput = it },
                        label = { Text("Label (shown when decoy is active)") },
                        placeholder = { Text("e.g. \"Guest mode\"") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    )
                    if (decoyError) {
                        Text("PINs don't match. Try again.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (decoyInput.length >= 4 && decoyInput == decoyConfirm) {
                            val hash = settings.hashAppPin(decoyInput)
                            settings.setDecoyPinHash(hash)
                            settings.setDecoyPinLabel(decoyLabelInput.trim())
                            showSetupDialog = false
                            decoyInput = ""; decoyConfirm = ""; decoyLabelInput = ""; decoyError = false
                        } else {
                            decoyError = true
                        }
                    },
                    enabled = decoyInput.length >= 4 && decoyConfirm.length >= 4
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSetupDialog = false; decoyInput = ""; decoyConfirm = ""; decoyLabelInput = ""; decoyError = false }) { Text("Cancel") }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  App PIN Length Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun AppPinLengthPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val pinLength by settings.appPinLength.collectAsState()

    SettingsSubPage(onBack = onBack, title = "PIN Length", icon = MaterialSymbolIcon("dialpad")) {
        item {
            SettingsGroupCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose the number of digits for your app PIN.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf("4 digits", "5 digits", "6 digits").forEach { option ->
                        val selected = pinLength == option
                        Surface(
                            onClick = { settings.setAppPinLength(option) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selected, onClick = { settings.setAppPinLength(option) })
                                Column(Modifier.weight(1f)) {
                                    Text(option, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        when (option) {
                                            "4 digits" -> "Standard length, 10,000 possible combinations"
                                            "5 digits" -> "100,000 possible combinations"
                                            else -> "1,000,000 possible combinations, most secure"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
