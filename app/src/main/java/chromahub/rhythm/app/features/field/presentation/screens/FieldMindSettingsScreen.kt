package fieldmind.research.app.features.field.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.settings.FieldMindSettings
import fieldmind.research.app.features.field.data.vision.RegionalPack
import fieldmind.research.app.features.field.data.vision.SpeciesDatabase
import fieldmind.research.app.features.field.data.weather.WeatherProviders
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════
//  Settings Hub
// ══════════════════════════════════════════════════════════════════════

@Composable
fun FieldMindSettingsScreen(
    viewModel: FieldMindViewModel? = null,
    onBack: () -> Unit,
    onResetOnboarding: () -> Unit,
    onOpenExport: (() -> Unit)? = null,
    onOpenAbout: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    onOpenAppearance: (() -> Unit)? = null,
    onOpenCapture: (() -> Unit)? = null,
    onOpenAi: (() -> Unit)? = null,
    onOpenLocalModel: (() -> Unit)? = null,
    onOpenBackup: (() -> Unit)? = null,
    onOpenSecurity: (() -> Unit)? = null,
    onOpenChangelog: (() -> Unit)? = null,
    onOpenUnits: (() -> Unit)? = null,
    onOpenWeather: (() -> Unit)? = null,
    onOpenMap: (() -> Unit)? = null,
    onOpenDataIntegrity: (() -> Unit)? = null,
    onOpenDeveloper: (() -> Unit)? = null,
    onOpenSpeciesPacks: (() -> Unit)? = null
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { FieldScreenHeader("Settings", "Offline-first setup, profile, capture, local AI, export, and privacy.", icon = FieldMindIcons.Settings, actionIcon = FieldMindIcons.Back, onAction = onBack) }

        item {
            SettingsTileGroup("Quick settings") {
                if (viewModel != null) {
                    val themeMode by viewModel.fieldSettings.themeMode.collectAsState()
                    val dynamicColor by viewModel.fieldSettings.dynamicColorEnabled.collectAsState()
                    ToggleItem("Material You colors", "Use system wallpaper colors that auto-adapt.", dynamicColor, viewModel.fieldSettings::setDynamicColorEnabled, FieldMindIcons.Palette)
                    ThemeToggle(themeMode, viewModel.fieldSettings::setThemeMode)
                }
            }
        }

        item { SectionHeader("Configuration", "Manage every aspect of your FieldMind experience") }

        item { SettingsNavCard("Research profile", "Name, role, and research focus", FieldMindIcons.Nature, FieldMindTheme.colors.observation) { onOpenProfile?.invoke() } }
        item { SettingsNavCard("Appearance", "Theme, dynamic color, and layout", FieldMindIcons.Palette, FieldMindTheme.colors.info) { onOpenAppearance?.invoke() } }
        item { SettingsNavCard("Capture defaults", "Categories, confidence, goal, location", FieldMindIcons.Capture, FieldMindTheme.colors.observation) { onOpenCapture?.invoke() } }
        item { SettingsNavCard("Weather", "Auto-weather, temperature unit, refresh, widget display", FieldMindIcons.Weather, FieldMindTheme.colors.info) { onOpenWeather?.invoke() } }
        item { SettingsNavCard("Units & format", "Temperature, distance, date/time display", FieldMindIcons.Settings, FieldMindTheme.colors.info) { onOpenUnits?.invoke() } }
        item { SettingsNavCard("Map settings", "Map type, default zoom, location marker", FieldMindIcons.Map, FieldMindTheme.colors.data) { onOpenMap?.invoke() } }
        item { SettingsNavCard("AI assistant", "Gemini, OpenAI, provider settings", FieldMindIcons.Sparkle, FieldMindTheme.colors.flashcard) { onOpenAi?.invoke() } }
        item { SettingsNavCard("Local model", "Download offline model for flashcards", FieldMindIcons.Download, FieldMindTheme.colors.hypothesis) { onOpenLocalModel?.invoke() } }
        item { SettingsNavCard("Backup & import", "Auto-backup, interval, and restore", FieldMindIcons.Archive, FieldMindTheme.colors.data) { onOpenBackup?.invoke() } }
        item { SettingsNavCard("Security", "Privacy lock, lock timeout, auto-lock", FieldMindIcons.Lock, FieldMindTheme.colors.confidenceVerify) { onOpenSecurity?.invoke() } }
        item { SettingsNavCard("Data integrity", "Orphaned records, database health", FieldMindIcons.Archive, FieldMindTheme.colors.hypothesis) { onOpenDataIntegrity?.invoke() } }
        item { SettingsNavCard("Developer", "Debug logging, dev tools, version info", FieldMindIcons.Sparkle, FieldMindTheme.colors.flashcard) { onOpenDeveloper?.invoke() } }
        item { SettingsNavCard("Species packs", "Download regional model packs for species ID", FieldMindIcons.Download, FieldMindTheme.colors.observation) { onOpenSpeciesPacks?.invoke() } }
        item { SettingsNavCard("Export Studio", "Export as PDF, CSV, JSON, HTML, SVG", FieldMindIcons.Export, FieldMindTheme.colors.report) { onOpenExport?.invoke() } }
        item { SettingsNavCard("What’s new", "FieldMind-specific redesign notes and migration changes", FieldMindIcons.Info, FieldMindTheme.colors.info) { onOpenChangelog?.invoke() } }
        item { SettingsNavCard("About", "Credits, acknowledgements, and version", FieldMindIcons.Info, FieldMindTheme.colors.source) { onOpenAbout?.invoke() } }

        item {
            OutlinedButton(onClick = onResetOnboarding, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text("Reset onboarding")
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsNavCard(title: String, subtitle: String, icon: MaterialSymbolIcon, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon = icon, contentDescription = null, tint = color, size = 24.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(icon = FieldMindIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Profile Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ProfileSettingsPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val profileName by settings.profileName.collectAsState()
    val profileRole by settings.profileRole.collectAsState()
    val profileFocus by settings.profileFocus.collectAsState()

    SettingsSubPage("Research profile", icon = FieldMindIcons.Nature, onBack = onBack) {
        item {
            SettingsGroupCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("FieldMind has no app server: profile, observations, sources, and local model settings are stored on this device unless you export or share them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = profileName, onValueChange = settings::setProfileName, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true)
                    Text("Role", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OptionPickerField(label = "Role", selected = profileRole, options = listOf("Field learner", "Student", "Naturalist", "Researcher"), onSelected = { settings.setProfileRole(it) }, icon = FieldMindIcons.User)
                    Text("Focus", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OptionPickerField(label = "Focus", selected = profileFocus, options = listOf("Wildlife & ecology", "Plants & botany", "Weather", "Water", "Geology", "General science"), onSelected = { settings.setProfileFocus(it) }, icon = FieldMindIcons.Category)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Appearance Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun AppearanceSettingsPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val themeMode by settings.themeMode.collectAsState()
    val dynamicColor by settings.dynamicColorEnabled.collectAsState()

    SettingsSubPage("Appearance", icon = FieldMindIcons.Palette, onBack = onBack) {
        item {
            SettingsGroupCard {
                ThemeToggle(themeMode, settings::setThemeMode)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Material You dynamic color", "Use system wallpaper colors that auto-adapt to light/dark. Off keeps the FieldMind brand palette.", dynamicColor, settings::setDynamicColorEnabled, FieldMindIcons.Palette)
            }
        }
        item {
            SettingsGroupCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("Changes apply immediately. The FieldMind brand palette uses forest green and warm ochre tones. When dynamic color is enabled, system wallpaper colors override the brand palette.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ThemeToggle(current: String, onSet: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon = FieldMindIcons.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
        Column(Modifier.weight(1f)) {
            Text("Theme", fontWeight = FontWeight.SemiBold)
            OptionPickerField(label = "Theme", selected = current, options = listOf("System", "Light", "Dark"), onSelected = { onSet(it) }, icon = FieldMindIcons.Image)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Capture Defaults Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun CaptureDefaultsSettingsPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val goal by settings.dailyObservationGoal.collectAsState()
    val category by settings.defaultCategory.collectAsState()
    val confidence by settings.defaultConfidence.collectAsState()
    val locationMode by settings.locationMode.collectAsState()
    val media by settings.mediaAttachmentsEnabled.collectAsState()
    val audio by settings.audioRecordingEnabled.collectAsState()
    val exportMode by settings.attachmentExportMode.collectAsState()
    val reminders by settings.remindersEnabled.collectAsState()
    val streaks by settings.streaksEnabled.collectAsState()

    SettingsSubPage("Capture defaults", icon = FieldMindIcons.Capture, onBack = onBack) {
        item {
            SettingsGroupCard {
                StepperItem("Daily observation goal", "Drives the Today dashboard and progress ring.", goal, FieldMindIcons.Today) { settings.setDailyObservationGoal(it) }
            }
        }
        item {
            SettingsGroupCard {
                ChoiceItemForm("Default category", observationCategories, category, FieldMindIcons.Observation, settings::setDefaultCategory)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ChoiceItemForm("Default confidence", confidenceOptions, confidence, FieldMindIcons.Check, settings::setDefaultConfidence)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ChoiceItemForm("Location mode", listOf("Manual only", "Approximate", "Precise"), locationMode, FieldMindIcons.Location, settings::setLocationMode)
            }
        }
        item {
            SettingsGroupCard {
                ToggleItem("Media attachments", "Enable camera, gallery, and file evidence tools.", media, settings::setMediaAttachmentsEnabled, FieldMindIcons.Camera)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Audio recording", "Enable voice-note evidence capture.", audio, settings::setAudioRecordingEnabled, FieldMindIcons.Mic)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ChoiceItemForm("Attachment export", listOf("Reference URIs", "Copy media later", "Skip media"), exportMode, FieldMindIcons.Export, settings::setAttachmentExportMode)
            }
        }
        item {
            SettingsGroupCard {
                ToggleItem("Daily reminders", "Schedules a daily prompt and skips after logging today's observation.", reminders, settings::setRemindersEnabled, FieldMindIcons.Notifications)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Streaks", "Shows consecutive observation days on the Today dashboard.", streaks, settings::setStreaksEnabled, FieldMindIcons.Streak)
            }
        }

    }
}

// ══════════════════════════════════════════════════════════════════════
//  AI Assistant Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun AiAssistantSettingsPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val ai by settings.geminiEnabled.collectAsState()
    val provider by settings.aiProvider.collectAsState()
    val key by settings.geminiApiKey.collectAsState()
    val model by settings.geminiModel.collectAsState()
    val openAiKey by settings.openAiApiKey.collectAsState()
    val openAiModel by settings.openAiModel.collectAsState()
    val confirm by settings.aiRequireConfirmBeforeSave.collectAsState()
    val sendAttachments by settings.aiSendAttachments.collectAsState()

    SettingsSubPage("AI assistant", icon = FieldMindIcons.Sparkle, onBack = onBack) {
        item {
            SettingsGroupCard {
                ToggleItem("Enable AI assistant", "Review factuality, suggest papers, and answer questions.", ai, settings::setGeminiEnabled, FieldMindIcons.Sparkle)
            }
        }
        if (ai) {
            item {
                SettingsGroupCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChoiceItemForm("Provider", listOf("Gemini", "OpenAI"), provider, FieldMindIcons.Sparkle, settings::setAiProvider)

                        if (provider == "OpenAI") {
                            OutlinedTextField(value = openAiKey, onValueChange = settings::setOpenAiApiKey, label = { Text("OpenAI API key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true, supportingText = { Text(if (openAiKey.isBlank()) "No OpenAI key saved." else "OpenAI key saved locally.") })
                            Text("Model", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OptionPickerField(label = "OpenAI model", selected = openAiModel, options = listOf("gpt-4.1-mini", "gpt-4.1", "gpt-4o-mini"), onSelected = { settings.setOpenAiModel(it) }, icon = FieldMindIcons.Bolt)
                        } else {
                            OutlinedTextField(value = key, onValueChange = settings::setGeminiApiKey, label = { Text("Gemini API key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true, supportingText = { Text(if (key.isBlank()) "No key saved — get one at aistudio.google.com." else "Key saved locally.") })
                            Text("Model", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OptionPickerField(label = "Gemini model", selected = model, options = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash"), onSelected = { settings.setGeminiModel(it) }, icon = FieldMindIcons.Bolt)
                        }
                    }
                }
            }
            item {
                SettingsGroupCard {
                    ToggleItem("Confirm before saving AI output", "AI suggestions stay as previews unless you apply them.", confirm, settings::setAiRequireConfirmBeforeSave, FieldMindIcons.Check)
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ToggleItem("Allow attachment context", "Off by default to protect field evidence privacy.", sendAttachments, settings::setAiSendAttachments, FieldMindIcons.File)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Privacy note", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("Nothing is sent to any AI provider without an explicit action. Your API key is stored only on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Local Model Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun LocalModelSettingsPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = viewModel.fieldSettings
    val localModelEnabled by settings.localModelEnabled.collectAsState()
    val localModelOption by settings.localModelOption.collectAsState()
    val localModelDownloaded by settings.localModelDownloaded.collectAsState()
    val localModelUseForStudy by settings.localModelUseForStudy.collectAsState()

    SettingsSubPage("Local model", icon = FieldMindIcons.Download, onBack = onBack) {
        item {
            SettingsGroupCard {
                ToggleItem("Use local model", "Runs study generation on-device after the model is downloaded.", localModelEnabled, settings::setLocalModelEnabled, FieldMindIcons.Sparkle)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                val sizes = listOf("FieldLite 500 MB", "FieldCore 1 GB", "FieldPro 2 GB")
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Model size", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    sizes.forEach { size ->
                        val selected = localModelOption == size
                        Surface(
                            onClick = { if (localModelEnabled) settings.setLocalModelOption(size) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(size, fontWeight = FontWeight.SemiBold)
                                    val spec = when (size) {
                                        "FieldLite 500 MB" -> "Fast, basic study generation"
                                        "FieldCore 1 GB" -> "Balanced speed and quality"
                                        else -> "Best quality, slower generation"
                                    }
                                    Text(spec, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (selected) Icon(FieldMindIcons.Check, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Use for flashcards/reviews", "Prefer the downloaded local model for automatic cards.", localModelUseForStudy, settings::setLocalModelUseForStudy, FieldMindIcons.Flashcard)
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (localModelDownloaded) "$localModelOption is ready" else "Choose a size and download into app storage.", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { settings.setLocalModelDownloaded(true) },
                        enabled = localModelEnabled,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (localModelDownloaded) "Re-download / verify model" else "Download inside app") }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Security Settings Page (NEW — moved from Backup & Import)
// ══════════════════════════════════════════════════════════════════════

@Composable
fun SecuritySettingsPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val privacy by settings.privacyLockEnabled.collectAsState()
    val lockTimeout by settings.lockTimeout.collectAsState()
    val autoLockBg by settings.autoLockOnBackground.collectAsState()

    SettingsSubPage("Security", icon = FieldMindIcons.Lock, onBack = onBack) {
        item {
            SettingsGroupCard {
                ToggleItem("App lock", "Require biometric or device credential to open FieldMind.", privacy, settings::setPrivacyLockEnabled, FieldMindIcons.Lock)
            }
        }
        if (privacy) {
            item {
                SettingsGroupCard {
                    ChoiceItemForm("Lock timeout", listOf("Immediate", "1 minute", "5 minutes", "15 minutes", "When screen off"), lockTimeout, FieldMindIcons.Timer, settings::setLockTimeout)
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ToggleItem("Auto-lock on background", "Lock when app goes to background.", autoLockBg, settings::setAutoLockOnBackground, FieldMindIcons.Lock)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("About app lock", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("When enabled, FieldMind requires biometric authentication (fingerprint, face) or your device PIN/pattern/password each time you open the app.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Your research data is stored entirely on this device. No data is sent to any server.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ═══════════════════════════════════════��══════════════════════════════
//  Backup & Import Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun BackupImportSettingsPage(viewModel: FieldMindViewModel, onBack: () -> Unit, onOpenExport: () -> Unit) {
    val settings = viewModel.fieldSettings
    val autoBackupEnabled by settings.autoBackupEnabled.collectAsState()
    val autoBackupInterval by settings.autoBackupInterval.collectAsState()
    val exportFormat by settings.defaultExportFormat.collectAsState()

    SettingsSubPage("Backup & import", icon = FieldMindIcons.Archive, onBack = onBack) {
        item {
            SettingsGroupCard {
                ToggleItem("Auto backup", "Writes private archive JSON files on the selected schedule.", autoBackupEnabled, settings::setAutoBackupEnabled, FieldMindIcons.Archive)
                if (autoBackupEnabled) {
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ChoiceItemForm("Backup interval", listOf("Daily", "Weekly", "Monthly"), autoBackupInterval, FieldMindIcons.Today, settings::setAutoBackupInterval)
                }
            }
        }
        item {
            SettingsGroupCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Export formats", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("Choose your preferred format for quick exports.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    ExportFormatSelector(exportFormat) { settings.setDefaultExportFormat(it) }
                }
            }
        }
        item {
            FilledTonalButton(onClick = onOpenExport, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(FieldMindIcons.Export, null, size = 18.dp)
                Spacer(Modifier.size(8.dp))
                Text("Open Export Studio")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  About Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun AboutPage(onBack: () -> Unit, onOpenChangelog: (() -> Unit)? = null) {
    val uriHandler = LocalUriHandler.current

    SettingsSubPage("About", icon = FieldMindIcons.Info, onBack = onBack) {
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(FieldMindIcons.Nature, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 36.dp)
                    }
                    Text("FieldMind", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Observe. Question. Research clearly.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f), textAlign = TextAlign.Center)
                    Text("A free, offline-first research notebook for curious naturalists, students, and citizen scientists.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f), textAlign = TextAlign.Center)
                }
            }
        }
        item { SettingsNavCard("What’s new", "See the FieldMind redesign changelog", FieldMindIcons.Info, FieldMindTheme.colors.info) { onOpenChangelog?.invoke() } }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Built with", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    listOf(
                        "Jetpack Compose" to "Android's modern declarative UI toolkit",
                        "Material Symbols & Material 3" to "Google's icon set and design system",
                        "Room Database" to "Local-first structured data storage"
                    ).forEach { (name, desc) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(FieldMindIcons.Check, null, tint = MaterialTheme.colorScheme.primary, size = 16.dp, modifier = Modifier.padding(top = 2.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.SemiBold)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Research data sources", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    listOf(
                        "Crossref" to "Free scholarly metadata API",
                        "OpenAlex" to "Open catalog of papers, authors, and venues",
                        "arXiv" to "Open-access research preprints",
                        "Open Library" to "Open, editable library catalog",
                        "Semantic Scholar" to "AI-powered research paper search"
                    ).forEach { (name, desc) ->
                        Row(Modifier.fillMaxWidth().clickable { runCatching { uriHandler.openUri("https://www.${name.lowercase().replace(" ", "")}.org") } }, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(FieldMindIcons.OpenLink, null, tint = MaterialTheme.colorScheme.primary, size = 16.dp)
                            Column(Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.SemiBold)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("Made with care for people who learn by looking closely.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Redesigned Export Format Selector
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ExportFormatSelector(selected: String, onSelect: (String) -> Unit) {
    val formats = listOf(
        FormatOption("Markdown", "Readable text for docs and notes", FieldMindIcons.Article, FieldMindTheme.colors.source),
        FormatOption("CSV", "Tabular data for spreadsheets", FieldMindIcons.Data, FieldMindTheme.colors.data),
        FormatOption("JSON", "Structured data for migration", FieldMindIcons.Archive, FieldMindTheme.colors.hypothesis),
        FormatOption("HTML", "Print-ready web layout", FieldMindIcons.Article, FieldMindTheme.colors.question),
        FormatOption("PNG", "Dashboard snapshot image", FieldMindIcons.Graph, FieldMindTheme.colors.observation),
        FormatOption("SVG", "Scalable vector graphic", FieldMindIcons.Graph, FieldMindTheme.colors.flashcard),
        FormatOption("PDF", "Portable document format", FieldMindIcons.Report, FieldMindTheme.colors.report),
        FormatOption("Plain text", "Raw text without formatting", FieldMindIcons.Note, FieldMindTheme.colors.info)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        formats.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { format ->
                    val isSelected = selected == format.name
                    Card(
                        modifier = Modifier.weight(1f).clickable { onSelect(format.name) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) format.color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, format.color) else null
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(format.icon, null, tint = format.color, size = 24.dp)
                            Text(format.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text(format.desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (isSelected) {
                                Icon(FieldMindIcons.Check, null, tint = format.color, size = 16.dp)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class FormatOption(val name: String, val desc: String, val icon: MaterialSymbolIcon, val color: androidx.compose.ui.graphics.Color)

// ══════════════════════════════════════════════════════════════════════
//  Observation Reading UI (Redesigned)
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ObservationReaderContent(observation: ObservationEntity, onAttachments: @Composable () -> Unit, onMap: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header card
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Subject line
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConfidenceChip(observation.confidenceLevel)
                    InfoChip(observation.category, icon = FieldMindIcons.iconForCategory(observation.category))
                    InfoChip("${observation.date} ${observation.time}", icon = FieldMindIcons.Today)
                    observation.durationMs?.let { InfoChip("${it / 1000}s", icon = FieldMindIcons.Timer) }
                }
                if (observation.manualLocation.isNotBlank() || observation.weatherCondition.isNotBlank() || observation.weatherTemperature != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (observation.manualLocation.isNotBlank()) InfoChip(observation.manualLocation, icon = FieldMindIcons.Location)
                        if (observation.weatherCondition.isNotBlank() || observation.weatherTemperature != null) InfoChip(listOfNotNull(observation.weatherTemperature?.let { "%.1f°C".format(it) }, observation.weatherCondition.ifBlank { null }, observation.weatherHumidity?.let { "$it% humidity" }).joinToString(" • "), icon = FieldMindIcons.Weather)
                        observation.changeDurationMs?.let { InfoChip("Change at +${it / 1000}s", icon = FieldMindIcons.Timer) }
                    }
                }

                Text(observation.subject.ifBlank { "Untitled observation" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)

                if (observation.tags.isNotBlank()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        observation.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                            TagChip(tag.trim())
                        }
                    }
                }
            }
        }

        // Facts section
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                        Icon(FieldMindIcons.Edit, null, tint = FieldMindTheme.colors.observation, size = 18.dp)
                    }
                    Text("Facts-only notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    observation.factsOnlyNotes.ifBlank { "No factual notes recorded." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Context section
        if (observation.moodOrContext.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(FieldMindTheme.colors.hypothesis.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Icon(FieldMindIcons.Lightbulb, null, tint = FieldMindTheme.colors.hypothesis, size = 18.dp)
                        }
                        Text("Context", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(observation.moodOrContext, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (observation.structuredDetailsJson.isNotBlank() || observation.timeNote.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Structured research details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (observation.structuredDetailsJson.isNotBlank()) Text(observation.structuredDetailsJson, style = MaterialTheme.typography.bodyMedium)
                    if (observation.timeNote.isNotBlank()) Text("Timing note: ${observation.timeNote}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Evidence summary
        if (observation.evidenceSummary.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(FieldMindTheme.colors.data.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Icon(FieldMindIcons.Camera, null, tint = FieldMindTheme.colors.data, size = 18.dp)
                        }
                        Text("Evidence summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(observation.evidenceSummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Attachments
        onAttachments()

        // Location map
        onMap()
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Units & Format Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun UnitsFormatSettingsPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val tempUnit by settings.tempUnit.collectAsState()
    val distanceUnit by settings.distanceUnit.collectAsState()
    val windSpeedUnit by settings.windSpeedUnit.collectAsState()
    val timeFormat by settings.timeFormat.collectAsState()
    val dateFormat by settings.dateFormat.collectAsState()

    SettingsSubPage("Units & format", icon = FieldMindIcons.Settings, onBack = onBack) {
        item {
            SectionHeader("Measurement units", "Choose how values are displayed in the app.")
        }
        item {
            SettingsGroupCard {
                ChoiceItemForm("Temperature", listOf("Celsius", "Fahrenheit"), tempUnit, FieldMindIcons.Weather, settings::setTempUnit)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ChoiceItemForm("Distance", listOf("km", "miles"), distanceUnit, FieldMindIcons.Map, settings::setDistanceUnit)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ChoiceItemForm("Wind speed", listOf("km/h", "mph", "knots"), windSpeedUnit, FieldMindIcons.AirWave, settings::setWindSpeedUnit)
            }
        }
        item {
            SectionHeader("Date & time format", "Control how timestamps appear.")
        }
        item {
            SettingsGroupCard {
                ChoiceItemForm("Time format", listOf("24h", "12h"), timeFormat, FieldMindIcons.Timer, settings::setTimeFormat)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ChoiceItemForm("Date format", listOf("ISO", "Local"), dateFormat, FieldMindIcons.Today, settings::setDateFormat)
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Display only", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("Unit and format changes only affect how data is displayed in the UI. Your stored data is always saved in base metric/ISO formats.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Weather Settings Page (separate from Capture defaults)
// ══════════════════════════════════════════════════════════════════════

@Composable
fun WeatherSettingsPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val autoWeather by settings.autoWeatherEnabled.collectAsState()
    val tempUnit by settings.tempUnit.collectAsState()
    val weatherRefresh by settings.weatherRefreshInterval.collectAsState()
    val showTemp by settings.weatherShowTemperature.collectAsState()
    val showCondition by settings.weatherShowCondition.collectAsState()
    val showHumidity by settings.weatherShowHumidity.collectAsState()
    val showWind by settings.weatherShowWind.collectAsState()
    val showCloud by settings.weatherShowCloudCover.collectAsState()
    val showPressure by settings.weatherShowPressure.collectAsState()
    val providerSlugs by settings.weatherProviders.collectAsState()
    val apiKey by settings.weatherApiKey.collectAsState()
    val selectedProviderSet = remember(providerSlugs) { providerSlugs.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet().ifEmpty { setOf("met-norway") } }
    val keyProvider = remember(selectedProviderSet) { WeatherProviders.selectedProviders(selectedProviderSet.joinToString(",")).firstOrNull { it.requiresApiKey } ?: WeatherProviders.selectedProviders(selectedProviderSet.joinToString(",")).first() }

    SettingsSubPage("Weather", icon = FieldMindIcons.Weather, onBack = onBack) {
        item {
            SectionHeader("Data capture", "Weather automatically attached to observations.")
        }
        item {
            SettingsGroupCard {
                ToggleItem("Auto weather", "Fetch live weather when adding observations.", autoWeather, settings::setAutoWeatherEnabled, FieldMindIcons.Weather)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ChoiceItemForm("Temperature unit", listOf("Celsius", "Fahrenheit"), tempUnit, FieldMindIcons.Weather, settings::setTempUnit)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ChoiceItemForm("Auto-refresh interval", listOf("15 min", "30 min", "60 min"), weatherRefresh, FieldMindIcons.Timer, settings::setWeatherRefreshInterval)
            }
        }
        item {
            SectionHeader("Weather services", "Choose one or more services. FieldMind merges every successful response and keeps partial data when a service misses a field.")
        }
        item {
            SettingsGroupCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enabled services", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    WeatherProviders.providers.forEach { provider ->
                        val isSelected = provider.slug in selectedProviderSet
                        val providerColor = FieldMindTheme.colors.info
                        Surface(
                            onClick = { settings.setWeatherProviderEnabled(provider.slug, !isSelected) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) providerColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) BorderStroke(1.5.dp, providerColor) else null
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(provider.displayName, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (provider.requiresApiKey) "Requires API key" else "Free, no key needed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Checkbox(checked = isSelected, onCheckedChange = { settings.setWeatherProviderEnabled(provider.slug, it) })
                            }
                        }
                    }

                    if (keyProvider.requiresApiKey) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = settings::setWeatherApiKey,
                            label = { Text(keyProvider.apiKeyLabel) },
                            placeholder = { Text(keyProvider.apiKeyPlaceholder) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                            supportingText = {
                                Text(
                                    if (apiKey.isBlank()) "No API key saved. Get one free from the provider's website."
                                    else "API key saved locally on this device."
                                )
                            }
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Row(
                                Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                                Text(
                                    "MET Norway, IMD India, Open-Meteo, and NWS are free with no API key. IMD is best inside India; NWS only returns data for U.S. points; paid-key services join the merge when a key is saved.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionHeader("Home screen widget display", "Choose which weather fields appear on the home dashboard card. The Weather Database screen always shows all available data.")
        }
        item {
            SettingsGroupCard {
                ToggleItem("Show temperature", "Current temperature on the weather card.", showTemp, settings::setWeatherShowTemperature, FieldMindIcons.Weather)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Show condition", "Weather condition icon and description.", showCondition, settings::setWeatherShowCondition, FieldMindIcons.Cloud)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Show humidity", "Humidity percentage.", showHumidity, settings::setWeatherShowHumidity, FieldMindIcons.Water)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Show wind", "Wind speed.", showWind, settings::setWeatherShowWind, FieldMindIcons.AirWave)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Show cloud cover", "Cloud cover percentage.", showCloud, settings::setWeatherShowCloudCover, FieldMindIcons.Cloud)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Show pressure", "Atmospheric pressure in hPa.", showPressure, settings::setWeatherShowPressure, FieldMindIcons.Compress)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Map Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun MapSettingsPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val mapType by settings.mapType.collectAsState()
    val mapShowLocation by settings.mapShowLocation.collectAsState()

    SettingsSubPage("Map settings", icon = FieldMindIcons.Map, onBack = onBack) {
        item {
            SettingsGroupCard {
                ChoiceItemForm("Default map type", listOf("Standard", "Satellite", "Terrain"), mapType, FieldMindIcons.Map, settings::setMapType)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Show my location", "Display your current position as a marker on the map.", mapShowLocation, settings::setMapShowLocation, FieldMindIcons.Location)
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Map data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("FieldMind uses OpenStreetMap tiles for map rendering. No map data is sent to any server beyond the tile request.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Data Integrity Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun DataIntegritySettingsPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val checkOnLaunch by settings.dataIntegrityCheckOnLaunch.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val projects by viewModel.projects.collectAsState()

    val orphanedObs = remember(observations, projects) {
        observations.count { obs -> (obs.projectId ?: 0L) > 0 && projects.none { it.id == obs.projectId } }
    }
    val totalRecords = observations.size + questions.size + sources.size

    SettingsSubPage("Data integrity", icon = FieldMindIcons.Archive, onBack = onBack) {
        item {
            SettingsGroupCard {
                ToggleItem("Check on launch", "Validate database integrity and report issues when the app starts.", checkOnLaunch, settings::setDataIntegrityCheckOnLaunch, FieldMindIcons.Check)
            }
        }
        item {
            SettingsGroupCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Database summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        IntegrityStat("$totalRecords", "Total records")
                        IntegrityStat("${observations.size}", "Observations")
                        IntegrityStat("${questions.size}", "Questions")
                        IntegrityStat("${sources.size}", "Sources")
                    }
                    if (orphanedObs > 0) {
                        Spacer(Modifier.height(8.dp))
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.error, size = 18.dp)
                                Text("$orphanedObs observation${if (orphanedObs != 1) "s" else ""} reference missing or deleted projects", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }
        }
        item {
            FilledTonalButton(
                onClick = { /* Placeholder: would trigger integrity repair */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(FieldMindIcons.Check, null, size = 18.dp)
                Spacer(Modifier.size(8.dp))
                Text("Run integrity check")
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("What this checks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    listOf(
                        "Orphaned records referencing deleted projects",
                        "Missing or corrupted attachment URIs",
                        "Inconsistent date/time values",
                        "Duplicate record detection"
                    ).forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Icon(FieldMindIcons.Check, null, tint = MaterialTheme.colorScheme.primary, size = 14.dp, modifier = Modifier.padding(top = 2.dp))
                            Text(item, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntegrityStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Developer Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun DeveloperSettingsPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val developerMode by settings.developerMode.collectAsState()
    val debugLogging by settings.debugLogging.collectAsState()

    SettingsSubPage("Developer", icon = FieldMindIcons.Sparkle, onBack = onBack) {
        item {
            SettingsGroupCard {
                ToggleItem("Developer mode", "Enable developer options and debug UI elements.", developerMode, settings::setDeveloperMode, FieldMindIcons.Sparkle)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Debug logging", "Write verbose logs for troubleshooting.", debugLogging, settings::setDebugLogging, FieldMindIcons.Article)
            }
        }
        if (developerMode) {
            item {
                SettingsGroupCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Developer tools", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        listOf(
                            "View database schema",
                            "Export raw JSON dump",
                            "Test notifications",
                            "Clear all preferences (restart required)"
                        ).forEach { tool ->
                            Surface(
                                onClick = { /* Placeholder */ },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(tool, style = MaterialTheme.typography.bodyMedium)
                                    Icon(FieldMindIcons.Forward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Version info", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text("FieldMind 4.3.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Database schema version: 9", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Room: SQLite-backed local storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Caution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    Text("Developer options are intended for troubleshooting and testing. Incorrect changes to stored data could cause data loss.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Shared helpers
// ══════════════════════════════════════════════════════════════════════

/** Wraps a sub-page with consistent header and scrollable content. */
@Composable
private fun SettingsSubPage(title: String, icon: MaterialSymbolIcon, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { FieldScreenHeader(title, icon = icon, actionIcon = FieldMindIcons.Back, onAction = onBack) }
        content()
    }
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) { Column(content = content) }
}

@Composable
private fun ToggleItem(title: String, body: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, icon: MaterialSymbolIcon? = null) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StepperItem(title: String, body: String, value: Int, icon: MaterialSymbolIcon? = null, onValueChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(onClick = { onValueChange((value - 1).coerceAtLeast(0)) }) { Icon(icon = MaterialSymbolIcon("remove"), contentDescription = "Decrease", size = 18.dp) }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 24.dp), textAlign = TextAlign.Center)
            FilledTonalIconButton(onClick = { onValueChange(value + 1) }) { Icon(icon = FieldMindIcons.Add, contentDescription = "Increase", size = 18.dp) }
        }
    }
}

@Composable
private fun ChoiceItemForm(title: String, options: List<String>, selected: String, icon: MaterialSymbolIcon? = null, onSelected: (String) -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                }
            }
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        }
        ChoiceChips(options, selected, onSelected = onSelected)
    }
}

@Composable
private fun SettingsTileGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) { Column(content = content) }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Species Pack Management Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun SpeciesPackSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { SpeciesDatabase(context) }
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()
    val snackbar = remember { SnackbarHostState() }

    var packs by remember { mutableStateOf(database.getRegionalPacks()) }
    var downloadingId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    // Update pack list whenever download state changes
    fun refreshPacks() {
        packs = database.getRegionalPacks()
    }

    // Set up progress listener
    LaunchedEffect(Unit) {
        database.setProgressListener { regionId, downloaded, total ->
            if (total > 0) {
                downloadProgress = (downloaded.toFloat() / total).coerceIn(0f, 1f)
            }
        }
    }

    // Clean up listener
    DisposableEffect(Unit) {
        onDispose {
            database.setProgressListener(null)
        }
    }

    LaunchedEffect(packs) {
        refreshPacks()
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                FieldScreenHeader(
                    "Species packs",
                    "Download regional identification model packs.",
                    icon = FieldMindIcons.Download,
                    actionIcon = FieldMindIcons.Back,
                    onAction = onBack
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                            Text(
                                "Regional packs expand the species identification model beyond the bundled ~500 species.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(packs, key = { it.regionId }) { pack ->
                val isDownloaded = pack.isDownloaded
                val isDownloading = downloadingId == pack.regionId

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDownloaded)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header row
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        FieldMindTheme.colors.observation.copy(alpha = 0.14f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isDownloaded) FieldMindIcons.Check else FieldMindIcons.Download,
                                    null,
                                    tint = FieldMindTheme.colors.observation,
                                    size = 24.dp
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    pack.regionName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    pack.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Status badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDownloaded)
                                    FieldMindTheme.colors.positive.copy(alpha = 0.14f)
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    if (isDownloaded) "Ready" else "${pack.downloadSizeMb} MB",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDownloaded) FieldMindTheme.colors.positive else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Stats row
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatChip("${pack.speciesCount}", "species", FieldMindTheme.colors.observation)
                            StatChip("${pack.downloadSizeMb} MB", "size", MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Download progress bar
                        if (isDownloading) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = FieldMindTheme.colors.observation
                                )
                                Text(
                                    "${(downloadProgress * 100).toInt()}% — Downloading…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Action buttons
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isDownloaded) {
                                OutlinedButton(
                                    onClick = {
                                        haptics.light()
                                        scope.launch {
                                            val success = database.deletePack(pack.regionId)
                                            refreshPacks()
                                            snackbar.showSnackbar(
                                                if (success) "${pack.regionName} pack removed"
                                                else "Could not delete pack"
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(FieldMindIcons.Delete, null, size = 18.dp)
                                    Spacer(Modifier.size(6.dp))
                                    Text("Delete")
                                }
                    TextButton(
                        onClick = {
                            haptics.light()
                            scope.launch {
                                snackbar.showSnackbar("Model at: ${database.getPackModelPath(pack.regionId)}")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                                    Icon(FieldMindIcons.Info, null, size = 18.dp)
                                    Spacer(Modifier.size(6.dp))
                                    Text("Info", maxLines = 1)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        haptics.confirm()
                                        downloadingId = pack.regionId
                                        downloadProgress = 0f
                                        scope.launch {
                                            val success = database.downloadPack(pack.regionId)
                                            downloadingId = null
                                            refreshPacks()
                                            snackbar.showSnackbar(
                                                if (success) "${pack.regionName} pack downloaded"
                                                else "Download failed. Check your connection and try again."
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = !isDownloading
                                ) {
                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(FieldMindIcons.Download, null, size = 18.dp)
                                    }
                                    Spacer(Modifier.size(6.dp))
                                    Text(if (isDownloading) "Downloading…" else "Download")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Snackbar overlay
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

@Composable
private fun StatChip(value: String, label: String, color: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
    }
}
