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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.BorderStroke
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════
//  Data Classes
// ══════════════════════════════════════════════════════════════════════

private data class ScreenVisibilityItem(
    val title: String,
    val description: String,
    val isEnabled: Boolean,
    val icon: MaterialSymbolIcon,
    val accentColor: Color
)

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
    onOpenSpeciesPacks: (() -> Unit)? = null,
    onOpenSpeciesId: (() -> Unit)? = null,
    onOpenAutoGen: (() -> Unit)? = null,
    onOpenScreenVisibility: (() -> Unit)? = null
) {
    BackHandler(enabled = true) { onBack() }
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StandardScreenHeader(
                title = "Settings",
                subtitle = "Offline-first setup, profile, capture, local AI, backup, and privacy.",
                icon = FieldMindIcons.Settings,
                trailing = {
                    BackButton(onClick = onBack)
                }
            )
        }

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

        // ╔════════════════════════════════════════════╗
        // ║  PROFILE                                   ║
        // ╚════════════════════════════════════════════╝
        item { SectionHeader("Profile", "Your research identity and preferences") }
        item { SettingsNavCard("Research profile", "Name, role, and research focus", FieldMindIcons.Nature, FieldMindTheme.colors.observation) { onOpenProfile?.invoke() } }

        // ╔════════════════════════════════════════════╗
        // ║  DISPLAY & THEME                           ║
        // ╚════════════════════════════════════════════╝
        item { SectionHeader("Display & theme", "Appearance, units, and format") }
        item { SettingsNavCard("Appearance", "Theme, dynamic color, and layout", FieldMindIcons.Palette, FieldMindTheme.colors.info) { onOpenAppearance?.invoke() } }
        item { SettingsNavCard("Units & format", "Temperature, distance, date/time display", FieldMindIcons.Settings, FieldMindTheme.colors.info) { onOpenUnits?.invoke() } }

        // ╔════════════════════════════════════════════╗
        // ║  DATA ENTRY                                ║
        // ╚════════════════════════════════════════════╝
        item { SectionHeader("Data entry", "Capture defaults, weather, and species ID") }
        item { SettingsNavCard("Capture defaults", "Categories, confidence, goal, location", FieldMindIcons.Capture, FieldMindTheme.colors.observation) { onOpenCapture?.invoke() } }
        item { SettingsNavCard("Weather", "Auto-weather, temperature unit, refresh, widget display", FieldMindIcons.Weather, FieldMindTheme.colors.info) { onOpenWeather?.invoke() } }
        item { SettingsNavCard("Species identification", "Image analysis, API key, and model URL", FieldMindIcons.Nature, FieldMindTheme.colors.observation) { onOpenSpeciesId?.invoke() } }
        item { SettingsNavCard("Species packs", "Download regional model packs for species ID", FieldMindIcons.Download, FieldMindTheme.colors.observation) { onOpenSpeciesPacks?.invoke() } }

        // ╔════════════════════════════════════════════╗
        // ║  TOOLS                                     ║
        // ╚══���═════════════════════════════════════════╝
        item { SectionHeader("Tools", "Maps, navigation, and automation") }
        item { SettingsNavCard("Map settings", "Map type, default zoom, location marker", FieldMindIcons.Map, FieldMindTheme.colors.data) { onOpenMap?.invoke() } }
        item { SettingsNavCard("Screen visibility", "Show/hide navigation tabs", FieldMindIcons.Visibility, FieldMindTheme.colors.info) { onOpenScreenVisibility?.invoke() } }
        item { SettingsNavCard("Auto generation", "Automatic flashcards & questions from observations", FieldMindIcons.Sparkle, FieldMindTheme.colors.flashcard) { onOpenAutoGen?.invoke() } }

        // ╔════════════════════════════════════════════╗
        // ║  AI ASSISTANCE                             ║
        // ╚════════════════════════════════════════════╝
        item { SectionHeader("AI assistance", "On-device and cloud AI settings") }
        item { SettingsNavCard("AI assistant", "Gemini, OpenAI, provider settings", FieldMindIcons.Sparkle, FieldMindTheme.colors.flashcard) { onOpenAi?.invoke() } }
        item { SettingsNavCard("Local model", "On-device study generation", FieldMindIcons.Download, FieldMindTheme.colors.hypothesis) { onOpenLocalModel?.invoke() } }

        // ╔════════════════════════════════════════════╗
        // ║  DATA & STORAGE                            ║
        // ╚════════════════════════════════════════════╝
        item { SectionHeader("Data & storage", "Backup, export, and database health") }
        item { SettingsNavCard("Backup & Restore", "Export, import, backup with folder picker, encryption", FieldMindIcons.Archive, FieldMindTheme.colors.data) { onOpenBackup?.invoke() } }
        item { SettingsNavCard("Data integrity", "Orphaned records, database health", FieldMindIcons.Archive, FieldMindTheme.colors.hypothesis) { onOpenDataIntegrity?.invoke() } }

        // ╔════════════════════════════════════════════╗
        // ║  SECURITY & PRIVACY                        ║
        // ╚════════════════════════════════════════════╝
        item { SectionHeader("Security & privacy", "Lock, PIN, and privacy controls") }
        item { SettingsNavCard("Security", "App lock, PIN lock, privacy typing, auto-lock", FieldMindIcons.Lock, FieldMindTheme.colors.confidenceVerify) { onOpenSecurity?.invoke() } }

        // ╔════════════════════════════════════════════╗
        // ║  ADVANCED                                  ║
        // ╚════════════════════════════════════════════╝
        item { SectionHeader("Advanced", "Developer tools and debugging") }
        item { SettingsNavCard("Developer", "Debug logging, dev tools, version info", FieldMindIcons.Sparkle, FieldMindTheme.colors.flashcard) { onOpenDeveloper?.invoke() } }

        // ╔════════════════════════════════════════════╗
        // ║  INFO                                      ║
        // ╚════════════════════════════════════════════╝
        item { SectionHeader("Info", "Release notes and app info") }
        item { SettingsNavCard("What’s new", "FieldMind redesign notes and migration changes", FieldMindIcons.Info, FieldMindTheme.colors.info) { onOpenChangelog?.invoke() } }
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
                    OutlinedTextField(value = profileName, onValueChange = settings::setProfileName, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true, keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                                        trailingIcon = {
                                            if (LocalPrivacyTypingEnabled.current) {
                                                PrivacyTypingIndicator()
                                            }
                                        }
                                    )
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
                            OutlinedTextField(value = openAiKey, onValueChange = settings::setOpenAiApiKey, label = { Text("OpenAI API key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true, supportingText = { Text(if (openAiKey.isBlank()) "No OpenAI key saved." else "OpenAI key saved locally.") }, keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                                                trailingIcon = {
                                                    if (LocalPrivacyTypingEnabled.current) {
                                                        PrivacyTypingIndicator()
                                                    }
                                                }
                                            )
                            Text("Model", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OptionPickerField(label = "OpenAI model", selected = openAiModel, options = listOf("gpt-4.1-mini", "gpt-4.1", "gpt-4o-mini"), onSelected = { settings.setOpenAiModel(it) }, icon = FieldMindIcons.Bolt)
                        } else {
                            OutlinedTextField(value = key, onValueChange = settings::setGeminiApiKey, label = { Text("Gemini API key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true, supportingText = { Text(if (key.isBlank()) "No key saved — get one at aistudio.google.com." else "Key saved locally.") }, keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                                                trailingIcon = {
                                                    if (LocalPrivacyTypingEnabled.current) {
                                                        PrivacyTypingIndicator()
                                                    }
                                                }
                                            )
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
    val settings = viewModel.fieldSettings
    val localModelEnabled by settings.localModelEnabled.collectAsState()
    val localModelOption by settings.localModelOption.collectAsState()
    val localModelUseForStudy by settings.localModelUseForStudy.collectAsState()

    SettingsSubPage("Study profiles", icon = FieldMindIcons.Sparkle, onBack = onBack) {
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(FieldMindTheme.colors.flashcard.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Icon(FieldMindIcons.Sparkle, null, tint = FieldMindTheme.colors.flashcard, size = 18.dp)
                        }
                        Text("On-device study generation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "This is not a web download — FieldMind generates study content and flashcards directly on your device using your existing observations and sources. The \"profile\" you select controls how much detail the on-device generator aims for.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No internet connection, no external server, no model files to download. Everything stays on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            SettingsGroupCard {
                ToggleItem("Enable study generator", "Generates flashcards and review content from your observations and sources using on-device logic.", localModelEnabled, settings::setLocalModelEnabled, FieldMindIcons.Sparkle)
                if (localModelEnabled) {
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    val profiles = listOf("FieldLite — Quick cards", "FieldCore — Balanced detail", "FieldPro — Deep study")
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Generation profile", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        profiles.forEach { profile ->
                            val selected = localModelOption == profile
                            Surface(
                                onClick = { settings.setLocalModelOption(profile) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(profile, fontWeight = FontWeight.SemiBold)
                                        val spec = when (profile) {
                                            "FieldLite — Quick cards" -> "Fast, concise flashcard generation"
                                            "FieldCore — Balanced detail" -> "Balanced speed and depth"
                                            else -> "Most detailed, thorough study content"
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
                    ToggleItem("Use for flashcards/reviews", "Use generated content for flashcard review sessions.", localModelUseForStudy, settings::setLocalModelUseForStudy, FieldMindIcons.Flashcard)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("How it works", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "FieldMind's study generator creates flashcards by scanning your saved observations, sources, and notes for key concepts, terms, and facts. It then builds question-answer pairs — all on your device, with no data leaving your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    val privacyTyping by settings.privacyTypingEnabled.collectAsState()
    val lockTimeout by settings.lockTimeout.collectAsState()
    val autoLockOnBackground by settings.autoLockOnBackground.collectAsState()
    val appPinEnabled by settings.appPinEnabled.collectAsState()
    val appPinHash by settings.appPinHash.collectAsState()
    val screenCaptureProtectionStatus by settings.screenCaptureProtectionEnabled.collectAsState()
    var showPinSetup by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var showCurrentPinDialog by remember { mutableStateOf(false) }
    var currentPinInput by remember { mutableStateOf("") }
    var currentPinError by remember { mutableStateOf(false) }

    // Derived convenience
    val appLockActive = privacy || (appPinEnabled && appPinHash.isNotBlank())
    // Backup encryption defaults on when biometric lock is active
    val backupEncryptionDefaultsOn = privacy

    SettingsSubPage("Security", icon = FieldMindIcons.Lock, onBack = onBack) {

        // ── Privacy Status Card ──
        item {
            PrivacyStatusCard(
                screenCaptureEnabled = screenCaptureProtectionStatus,
                privacyKeyboardEnabled = privacyTyping,
                appLockEnabled = appLockActive,
                backupEncryptionEnabled = backupEncryptionDefaultsOn
            )
        }

        // ── Device biometric lock ──
        item {
            SettingsGroupCard {
                ToggleItem("Device biometric lock", "Require fingerprint, face, or device PIN to open FieldMind.", privacy, settings::setPrivacyLockEnabled, FieldMindIcons.Lock)
            }
        }

        // ── In-app PIN lock (independent, no device lock required) ──
        item {
            SettingsGroupCard {
                if (!showPinSetup) {
                    if (appPinEnabled && appPinHash.isNotBlank()) {
                        Row(
                            Modifier.fillMaxWidth().clickable { showPinSetup = true }.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(FieldMindIcons.Lock, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("App PIN lock", fontWeight = FontWeight.SemiBold)
                                Text("Self-contained 4-6 digit PIN, no device lock needed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = true, onCheckedChange = { enabled ->
                                if (enabled) {
                                    showPinSetup = true
                                } else {
                                    showCurrentPinDialog = true
                                }
                            })
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth().clickable { showPinSetup = true }.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(FieldMindIcons.Lock, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("App PIN lock", fontWeight = FontWeight.SemiBold)
                                Text("Self-contained 4-6 digit PIN, no device lock needed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = false, onCheckedChange = { showPinSetup = true })
                        }
                    }
                }

                // ── PIN setup form ──
                if (showPinSetup) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            if (appPinHash.isNotBlank()) "Change PIN" else "Set a 4-6 digit PIN",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 6) { pinInput = it; pinError = false } },
                            label = { Text("Enter PIN") },
                            singleLine = true,
                            isError = pinError,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password).withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                            trailingIcon = {
                                if (LocalPrivacyTypingEnabled.current) {
                                    PrivacyTypingIndicator()
                                }
                            },
                            supportingText = if (pinError) {{ Text("PINs don't match. Try again.") }} else null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            textStyle = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 8.sp, textAlign = TextAlign.Center)
                        )
                        OutlinedTextField(
                            value = pinConfirm,
                            onValueChange = { if (it.length <= 6) { pinConfirm = it; pinError = false } },
                            label = { Text("Confirm PIN") },
                            singleLine = true,
                            isError = pinError,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password).withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                            trailingIcon = {
                                if (LocalPrivacyTypingEnabled.current) {
                                    PrivacyTypingIndicator()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            textStyle = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 8.sp, textAlign = TextAlign.Center)
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    showPinSetup = false
                                    pinInput = ""; pinConfirm = ""; pinError = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Cancel") }
                            Button(
                                onClick = {
                                    if (pinInput.length >= 4 && pinInput == pinConfirm) {
                                        val hash = settings.hashAppPin(pinInput)
                                        settings.setAppPinHash(hash)
                                        settings.setAppPinEnabled(true)
                                        showPinSetup = false
                                        pinInput = ""; pinConfirm = ""; pinError = false
                                    } else {
                                        pinError = true
                                    }
                                },
                                enabled = pinInput.length >= 4 && pinConfirm.length >= 4,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Save PIN") }
                        }
                    }
                }
            }
        }

        // ── Privacy typing ──
        item {
            SettingsGroupCard {
                ToggleItem(
                    "Privacy keyboard",
                    "Asks the keyboard not to learn from your input in sensitive fields. " +
                    "Gboard shows an incognito badge; most other keyboards apply the hint silently or may ignore it. " +
                    "Not every keyboard guarantees compliance.",
                    privacyTyping,
                    settings::setPrivacyTypingEnabled,
                    FieldMindIcons.Lock
                )
            }
        }

        // ── Lock timeout ──
        item {
            SettingsGroupCard {
                ChoiceItemForm("Lock timeout", listOf("Immediate", "1 minute", "5 minutes", "15 minutes", "When screen off"), lockTimeout, FieldMindIcons.Timer) { newValue ->
                    settings.setLockTimeout(newValue)
                }
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleItem("Auto-lock on background", "Lock the app when it goes to background.", autoLockOnBackground, { newValue ->
                    settings.setAutoLockOnBackground(newValue)
                }, FieldMindIcons.Lock)
            }
        }

        // ── Screen capture protection ──
        item {
            val screenCaptureProtection by settings.screenCaptureProtectionEnabled.collectAsState()
            SettingsGroupCard {
                ToggleItem("Screen capture protection", "Prevent screenshots and screen recordings of sensitive research data.", screenCaptureProtection, settings::setScreenCaptureProtectionEnabled, FieldMindIcons.Lock)
            }
        }

        // ── Always-on screen ──
        item {
            val alwaysOnScreenEnabled by settings.alwaysOnScreenEnabled.collectAsState()
            val alwaysOnDuration by settings.alwaysOnScreenDuration.collectAsState()
            SettingsGroupCard {
                ToggleItem("Always-on screen", "Keep display on during field research sessions.", alwaysOnScreenEnabled, settings::setAlwaysOnScreenEnabled, FieldMindIcons.Timer)
                if (alwaysOnScreenEnabled) {
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ChoiceItemForm("Duration", listOf("5 min", "10 min", "15 min", "30 min", "Unlimited"), alwaysOnDuration, FieldMindIcons.Timer, settings::setAlwaysOnScreenDuration)
                }
            }
        }

        // ── Clipboard auto-cleanup ──
        item {
            val clipboardCleanup by settings.clipboardAutoCleanupEnabled.collectAsState()
            val cleanupDelay by settings.clipboardCleanupDelay.collectAsState()
            SettingsGroupCard {
                ToggleItem("Clipboard auto-clear", "Automatically clear sensitive copied data from clipboard.", clipboardCleanup, settings::setClipboardAutoCleanupEnabled, FieldMindIcons.Lock)
                if (clipboardCleanup) {
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ChoiceItemForm("Clear after", listOf("10 sec", "30 sec", "1 min", "2 min"), cleanupDelay, FieldMindIcons.Timer, settings::setClipboardCleanupDelay)
                }
            }
        }

        // ─�� Info cards ──
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Privacy features", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("• Device biometric lock — uses Android's built-in security (fingerprint, face, PIN)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• App PIN lock — self-contained 4-6 digit PIN, works even if device has no lock set", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Privacy keyboard — requests that keyboards suppress learning in sensitive fields; Gboard shows an incognito badge, other keyboards may apply the hint silently", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Screen capture protection — prevents screenshots and recordings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Always-on screen — keep display active during field work", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Clipboard auto-clear — automatically clears sensitive copied data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Data encryption — encrypted backups with password protection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Your data stays on this device. No data is sent to any server.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // ── Confirm current PIN before disabling ──
    if (showCurrentPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showCurrentPinDialog = false
                currentPinInput = ""; currentPinError = false
            },
            icon = { Icon(FieldMindIcons.Lock, null, size = 28.dp) },
            title = { Text("Enter current PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter your current PIN to disable the app PIN lock.")
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = {
                            if (it.length <= 6) {
                                currentPinInput = it
                                currentPinError = false
                            }
                        },
                        label = { Text("Current PIN") },
                        singleLine = true,
                        isError = currentPinError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password).withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                        trailingIcon = {
                            if (LocalPrivacyTypingEnabled.current) {
                                PrivacyTypingIndicator()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 8.sp, textAlign = TextAlign.Center)
                    )
                    if (currentPinError) {
                        Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (settings.verifyAppPin(currentPinInput)) {
                            settings.setAppPinEnabled(false)
                            settings.setAppPinHash("")
                            showCurrentPinDialog = false
                            currentPinInput = ""; currentPinError = false
                        } else {
                            currentPinError = true
                        }
                    },
                    enabled = currentPinInput.length >= 4
                ) { Text("Disable") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCurrentPinDialog = false
                    currentPinInput = ""; currentPinError = false
                }) { Text("Cancel") }
            }
        )
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
                    ChoiceItemForm("Backup interval", listOf("Every 6 hours", "Every 12 hours", "Daily", "Weekly", "Monthly"), autoBackupInterval, FieldMindIcons.Today, settings::setAutoBackupInterval)
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
                Text("Open Backup & Restore")
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
//  Redesigned Export Format Selector — 4-column full grid with consistent sizing
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportFormatSelector(selected: String, onSelect: (String) -> Unit) {
    val formats = listOf(
        FormatOption("Markdown", "Readable text", FieldMindIcons.Article, FieldMindTheme.colors.source),
        FormatOption("CSV", "Tabular data", FieldMindIcons.Data, FieldMindTheme.colors.data),
        FormatOption("JSON", "Structured archive", FieldMindIcons.Archive, FieldMindTheme.colors.hypothesis),
        FormatOption("HTML", "Web layout", FieldMindIcons.Article, FieldMindTheme.colors.question),
        FormatOption("PNG", "Snapshot image", FieldMindIcons.Graph, FieldMindTheme.colors.observation),
        FormatOption("SVG", "Vector graphic", FieldMindIcons.Graph, FieldMindTheme.colors.flashcard),
        FormatOption("PDF", "Document format", FieldMindIcons.Report, FieldMindTheme.colors.report),
        FormatOption("Plain text", "Raw text", FieldMindIcons.Note, FieldMindTheme.colors.info)
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        formats.forEach { format ->
            val isSelected = selected == format.name
            val itemWeight = 0.235f // ~4 items per row with spacing (100% - 3*8dp gaps / 4)
            Surface(
                onClick = { onSelect(format.name) },
                modifier = Modifier.fillMaxWidth(itemWeight),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) format.color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (isSelected) BorderStroke(1.5.dp, format.color) else null
            ) {
                Column(
                    Modifier.padding(10.dp).heightIn(min = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) format.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(format.icon, null, tint = format.color, size = 20.dp)
                    }
                    Text(format.name, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(format.desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    if (isSelected) {
                        Box(
                            Modifier.size(18.dp).clip(CircleShape)
                                .background(format.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(FieldMindIcons.Check, null, tint = MaterialTheme.colorScheme.onPrimary, size = 12.dp)
                        }
                    }
                }
            }
        }
    }
}


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

// ═════════��════════════════════════════════════════════════════════════
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
    val showCloudAnimation by settings.weatherShowCloudAnimation.collectAsState()
    val providerSlugs by settings.weatherProviders.collectAsState()
    val apiKey by settings.weatherApiKey.collectAsState()
    val openWeatherMapKey by settings.openWeatherMapApiKey.collectAsState()
    val weatherApiDotComKey by settings.weatherApiDotComApiKey.collectAsState()
    val imdApiKey by settings.imdApiKey.collectAsState()
    val openMeteoConfig by settings.openMeteoApiConfig.collectAsState()
    val selectedProviderSet = remember(providerSlugs) { providerSlugs.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet().ifEmpty { setOf("met-norway") } }
    val keyProvider = remember(selectedProviderSet) { WeatherProviders.selectedProviders(selectedProviderSet.joinToString(",")).firstOrNull { it.requiresApiKey } ?: WeatherProviders.selectedProviders(selectedProviderSet.joinToString(",")).first() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearOpenMeteoConfigDialog by remember { mutableStateOf(false) }
    val openMeteoConfigPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (jsonString.isBlank()) {
                    Toast.makeText(context, "Empty file selected.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                // Validate JSON before saving
                com.google.gson.JsonParser.parseString(jsonString)
                settings.setOpenMeteoApiConfig(jsonString)
                Toast.makeText(context, "Open-Meteo config imported successfully.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Invalid JSON config: ${e.message?.take(60)}", Toast.LENGTH_LONG).show()
            }
        }
    }

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

                    // ── Per-provider API key fields ──
                    // Show individual API key fields for each enabled provider that requires a key
                    val enabledKeyProviders = WeatherProviders.selectedProviders(providerSlugs).filter { it.requiresApiKey }
                    enabledKeyProviders.forEach { provider ->
                        Spacer(Modifier.height(8.dp))
                        val (keyValue, onKeyChange) = when (provider.slug) {
                            "openweathermap" -> openWeatherMapKey to { v: String -> settings.setOpenWeatherMapApiKey(v) }
                            "weatherapi" -> weatherApiDotComKey to { v: String -> settings.setWeatherApiDotComApiKey(v) }
                            "imd-india" -> imdApiKey to { v: String -> settings.setImdApiKey(v) }
                            else -> apiKey to { v: String -> settings.setWeatherApiKey(v) }
                        }
                        OutlinedTextField(
                            value = keyValue,
                            onValueChange = onKeyChange,
                            label = { Text(provider.apiKeyLabel) },
                            placeholder = { Text(provider.apiKeyPlaceholder) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                            trailingIcon = {
                                if (LocalPrivacyTypingEnabled.current) {
                                    PrivacyTypingIndicator()
                                }
                            },
                            supportingText = {
                                Text(
                                    if (keyValue.isBlank()) "No API key saved. Get one free from the provider's website."
                                    else "API key saved locally on this device."
                                )
                            }
                        )
                    }

                    // ── Open-Meteo response format selector ──
                    Spacer(Modifier.height(8.dp))
                    ChoiceItemForm("Response format", listOf("json", "csv", "xlsx"),
                        try { com.google.gson.JsonParser.parseString(openMeteoConfig).asJsonObject.get("responseFormat")?.asString ?: "json" } catch (_: Exception) { "json" },
                        FieldMindIcons.File,
                        ) { newFormat ->
                            scope.launch {
                                val existing = try { com.google.gson.JsonParser.parseString(openMeteoConfig).asJsonObject } catch (_: Exception) { com.google.gson.JsonObject() }
                                if (newFormat == "json") { existing.remove("responseFormat") } else { existing.addProperty("responseFormat", newFormat) }
                                settings.setOpenMeteoApiConfig(existing.toString())
                                Toast.makeText(context, "Response format set to ${newFormat.uppercase()}", Toast.LENGTH_SHORT).show()
                            }
                        }

                    // ── Open-Meteo custom config import ──
                    if ("open-meteo" in selectedProviderSet) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            onClick = {
                                if (openMeteoConfig.isNotBlank()) {
                                    showClearOpenMeteoConfigDialog = true
                                } else {
                                    openMeteoConfigPicker.launch(arrayOf("application/json", "*/*"))
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (openMeteoConfig.isNotBlank())
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (openMeteoConfig.isNotBlank()) FieldMindIcons.Check else FieldMindIcons.File,
                                    null,
                                    tint = if (openMeteoConfig.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    size = 20.dp
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (openMeteoConfig.isNotBlank()) "Custom Open-Meteo config imported" else "Import Open-Meteo API config",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        if (openMeteoConfig.isNotBlank()) "Tap to replace or clear"
                                        else "Import a JSON file with custom API endpoint and parameters",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // ── Info note about free providers ──
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
                                "Each provider that requires an API key shows its own field above. " +
                                "MET Norway, Open-Meteo, and NWS are free with no key needed. " +
                                "IMD is best inside India; NWS only returns data for U.S. points.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ToggleItem("Cloud animations", "Animated clouds and partly-cloudy sky effects.", showCloudAnimation, settings::setWeatherShowCloudAnimation, FieldMindIcons.Cloud)
        }
        }
    }

    // ── Clear Open-Meteo config confirmation dialog ──
    if (showClearOpenMeteoConfigDialog) {
        AlertDialog(
            onDismissRequest = { showClearOpenMeteoConfigDialog = false },
            icon = { Icon(FieldMindIcons.File, null, size = 28.dp) },
            title = { Text("Open-Meteo config", fontWeight = FontWeight.Bold) },
            text = {
                Text("You have a custom Open-Meteo configuration imported. What would you like to do?")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        settings.setOpenMeteoApiConfig("")
                        showClearOpenMeteoConfigDialog = false
                        Toast.makeText(context, "Open-Meteo config cleared.", Toast.LENGTH_SHORT).show()
                    }) { Text("Clear config") }
                    Button(onClick = {
                        showClearOpenMeteoConfigDialog = false
                        openMeteoConfigPicker.launch(arrayOf("application/json", "*/*"))
                    }) { Text("Replace config") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearOpenMeteoConfigDialog = false }) { Text("Cancel") }
            }
        )
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
//  Screen Visibility Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ScreenVisibilitySettingsPage(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val settings = viewModel.fieldSettings
    val screenVis by settings.screenVisibility.collectAsState()
    val colors = FieldMindTheme.colors

    val visibilityToggles = listOf(
        ScreenVisibilityItem("Capture / Observe", "Observation capture screen in bottom nav", screenVis.showCapture, FieldMindIcons.Capture, colors.observation),
        ScreenVisibilityItem("Projects", "Project workspace and management", screenVis.showProjects, FieldMindIcons.Project, colors.project),
        ScreenVisibilityItem("Insights", "Research insights, health scores, graphs", screenVis.showInsights, FieldMindIcons.Graph, colors.info),
        ScreenVisibilityItem("Library", "Sources, notes, flashcards, reading", screenVis.showLibrary, FieldMindIcons.Book, colors.source),
        ScreenVisibilityItem("Map", "Offline map with drawing tools", screenVis.showMap, FieldMindIcons.Map, colors.data),
        ScreenVisibilityItem("Weather database", "Historical weather data screen", screenVis.showWeather, FieldMindIcons.Weather, colors.info),
        ScreenVisibilityItem("Species browser", "Taxonomic browser and species catalog", screenVis.showSpeciesBrowser, FieldMindIcons.Nature, colors.observation),
        ScreenVisibilityItem("Flashcards", "Flashcard review sessions", screenVis.showFlashcards, FieldMindIcons.Flashcard, colors.flashcard),
        ScreenVisibilityItem("Export studio", "Data export and report builder", screenVis.showExport, FieldMindIcons.Export, colors.data),

    )

    SettingsSubPage("Screen visibility", icon = FieldMindIcons.Visibility, onBack = onBack) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                        Text("Hide screens you don't use to keep navigation clean. Hidden screens are still accessible from settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            SettingsGroupCard {
                visibilityToggles.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            val cur = screenVis
                            val updated = when (item.icon) {
                                FieldMindIcons.Capture -> cur.copy(showCapture = !item.isEnabled)
                                FieldMindIcons.Project -> cur.copy(showProjects = !item.isEnabled)
                                FieldMindIcons.Graph -> cur.copy(showInsights = !item.isEnabled)
                                FieldMindIcons.Book -> cur.copy(showLibrary = !item.isEnabled)
                                FieldMindIcons.Map -> cur.copy(showMap = !item.isEnabled)
                                FieldMindIcons.Weather -> cur.copy(showWeather = !item.isEnabled)
                                FieldMindIcons.Nature -> cur.copy(showSpeciesBrowser = !item.isEnabled)
                                FieldMindIcons.Flashcard -> cur.copy(showFlashcards = !item.isEnabled)
                                FieldMindIcons.Export -> cur.copy(showExport = !item.isEnabled)
                                else -> cur
                            }
                            settings.setScreenVisibility(updated)
                        }.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(item.accentColor.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Icon(icon = item.icon, contentDescription = null, tint = item.accentColor, size = 22.dp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.SemiBold)
                            Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = item.isEnabled, onCheckedChange = {
                            val cur = screenVis
                            val updated = when (item.icon) {
                                FieldMindIcons.Capture -> cur.copy(showCapture = it)
                                FieldMindIcons.Project -> cur.copy(showProjects = it)
                                FieldMindIcons.Graph -> cur.copy(showInsights = it)
                                FieldMindIcons.Book -> cur.copy(showLibrary = it)
                                FieldMindIcons.Map -> cur.copy(showMap = it)
                                FieldMindIcons.Weather -> cur.copy(showWeather = it)
                                FieldMindIcons.Nature -> cur.copy(showSpeciesBrowser = it)
                                FieldMindIcons.Flashcard -> cur.copy(showFlashcards = it)
                                FieldMindIcons.Export -> cur.copy(showExport = it)
                                else -> cur
                            }
                            settings.setScreenVisibility(updated)
                        })
                    }
                    if (item.title != visibilityToggles.last().title) {
                        HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Navigation impact", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("Disabling the Capture, Projects, Insights, or Library tabs removes them from the bottom navigation bar. The screens remain accessible via deep links and search.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
    var testWeatherCode by remember { mutableStateOf<Int?>(null) }
    var testIsNight by remember { mutableStateOf(false) }

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
                DevWeatherTestPanel(
                    testCode = testWeatherCode,
                    testNight = testIsNight,
                    onCodeChange = { testWeatherCode = it },
                    onNightChange = { testIsNight = it }
                )
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

/** Wraps a sub-page with consistent StandardScreenHeader and scrollable content. */
@Composable
private fun SettingsSubPage(title: String, icon: MaterialSymbolIcon, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    BackHandler(enabled = true) { onBack() }
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StandardScreenHeader(
                title = title,
                icon = icon,
                trailing = {
                    BackButton(onClick = onBack)
                }
            )
        }
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
// ════════════════════════════════════════��═════════════════════════════

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
                StandardScreenHeader(
                    title = "Species packs",
                    subtitle = "Download regional identification model packs.",
                    icon = FieldMindIcons.Download,
                    trailing = {
                        BackButton(onClick = onBack)
                    }
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
                                            showFastSnackbar(snackbar, scope, if (success) "${pack.regionName} pack removed"
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
                                showFastSnackbar(snackbar, scope, "Model at: ${database.getPackModelPath(pack.regionId)}")
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
                                            val result = runCatching {
                                                database.downloadPack(pack.regionId)
                                            }
                                            downloadingId = null
                                            refreshPacks()
                                            if (result.isSuccess) {
                                                showFastSnackbar(snackbar, scope, "${pack.regionName} pack downloaded")
                                            } else {
                                                val errorMsg = result.exceptionOrNull()?.message
                                                    ?: "Unknown error"
                                                showFastSnackbar(
                                                    snackbar,
                                                    scope,
                                                    "Download failed: $errorMsg"
                                                )
                                            }
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

        // Top snackbar overlay
        FieldMindSnackbarOverlay(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Species Identification Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun SpeciesIdentificationSettingsPage(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val settings = viewModel.fieldSettings
    val apiKey by settings.speciesIdApiKey.collectAsState()
    val offlineFirst by settings.speciesIdOfflineFirst.collectAsState()
    val modelBaseUrl by settings.speciesModelBaseUrl.collectAsState()
    val perenualKey by settings.perenualApiKey.collectAsState()
    val uriHandler = LocalUriHandler.current

    SettingsSubPage("Species identification", icon = FieldMindIcons.Nature, onBack = onBack) {
        // ── How it works ──
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(FieldMindTheme.colors.observation.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(FieldMindIcons.Nature, null, tint = FieldMindTheme.colors.observation, size = 18.dp)
                        }
                        Text("How identification works", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "FieldMind uses a pure-Kotlin image analysis engine — color histograms, edge detection, texture analysis, and perceptual hashing — to identify species from photos. No AI, no internet, no server needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Predictions improve as you confirm IDs. Regional species packs (Settings > Species packs) can expand the built-in ~500 species database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Offline-first mode ──
        item {
            SettingsGroupCard {
                ToggleItem(
                    "Offline-first mode (recommended)",
                    "Use the on-device image analyzer. No internet required. Turn off to allow cloud reference lookups if you add an API key below.",
                    offlineFirst,
                    settings::setSpeciesIdOfflineFirst,
                    FieldMindIcons.Nature
                )
            }
        }

        // ── Cloud API section (Perenual + generic) ──
        if (!offlineFirst) {
            item {
                SettingsGroupCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(FieldMindIcons.Sparkle, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Cloud species reference", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Optional cloud APIs help look up species details and images. The offline analyzer still runs first.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // ���─ Perenual API key ──
                        Text("Perenual API (plant & botany data)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Free tier: 100 requests/day, 3,000+ species. Get a key at perenual.com (free signup). Used to fetch plant details, care guides, and images.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = perenualKey,
                            onValueChange = settings::setPerenualApiKey,
                            label = { Text("Perenual API key") },
                            placeholder = { Text("Paste your Perenual API key") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                            trailingIcon = {
                                if (LocalPrivacyTypingEnabled.current) {
                                    PrivacyTypingIndicator()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                            supportingText = {
                                Text(
                                    if (perenualKey.isBlank()) "No key saved. Sign up free at perenual.com"
                                    else "Perenual key saved locally."
                                )
                            }
                        )
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    uriHandler.openUri("https://perenual.com/")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(FieldMindIcons.OpenLink, null, size = 18.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Get Perenual API key (free)")
                        }

                        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // ── Generic species API key ──
                        Text("Other species API (optional)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "For custom or alternative species data APIs. iNaturalist API is free and open — no key required (just use the public endpoint).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = settings::setSpeciesIdApiKey,
                            label = { Text("Custom API key (optional)") },
                            placeholder = { Text("Leave blank if using iNaturalist") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                            trailingIcon = {
                                if (LocalPrivacyTypingEnabled.current) {
                                    PrivacyTypingIndicator()
                                }
                            },
                            supportingText = {
                                Text(
                                    if (apiKey.isBlank()) "No key — iNaturalist API is free without a key. Just paste the URL: api.inaturalist.org"
                                    else "Custom key saved locally."
                                )
                            }
                        )

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
                                    "All API keys are stored only on this device. iNaturalist does not require an API key for basic read-only access.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Regional pack URL ──
        item {
            SettingsGroupCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(FieldMindIcons.Download, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Species pack URL (advanced)", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Custom URL for regional species pack downloads. Only needed if hosting your own pack server.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = modelBaseUrl,
                        onValueChange = settings::setSpeciesModelBaseUrl,
                        label = { Text("Pack base URL") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                        trailingIcon = {
                            if (LocalPrivacyTypingEnabled.current) {
                                PrivacyTypingIndicator()
                            }
                        },
                        supportingText = {
                            Text(
                                if (modelBaseUrl.isBlank()) "Default URL — packs list species, not models"
                                else "Using: $modelBaseUrl"
                            )
                        }
                    )
                }
            }
        }
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

// ══════════════════════════════════════════════════════════════════════
//  Auto Generation Settings Page
// ══════════════════════════════════════════════════════════════════════

@Composable
fun AutoGenerationSettingsPage(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val settings = viewModel.fieldSettings
    val autoFlashcards by settings.autoFlashcardsEnabled.collectAsState()
    val autoQuestions by settings.autoQuestionsEnabled.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }

    // Error dialog
    if (showErrorDialog && errorMessage != null) {
        AlertDialog(
            onDismissRequest = { 
                showErrorDialog = false
                errorMessage = null
            },
            title = { Text("Error") },
            text = { Text(errorMessage ?: "An unknown error occurred") },
            confirmButton = {
                Button(
                    onClick = { 
                        showErrorDialog = false
                        errorMessage = null
                    },
                    shape = RoundedCornerShape(14.dp)
                ) { Text("OK") }
            }
        )
    }

    SettingsSubPage("Auto generation", icon = FieldMindIcons.Sparkle, onBack = onBack) {
        item {
            SettingsGroupCard {
                ToggleItem(
                    "Generate flashcards from observations",
                    "When enabled, new flashcards are automatically created from your observation data, notes, and sources as they come in.",
                    autoFlashcards,
                    { enabled ->
                        try {
                            settings.setAutoFlashcardsEnabled(enabled)
                        } catch (e: Exception) {
                            errorMessage = "Failed to toggle flashcard generation: ${e.message ?: "Unknown error"}"
                            showErrorDialog = true
                        }
                    },
                    FieldMindIcons.Flashcard
                )
            }
        }
        item {
            SettingsGroupCard {
                ToggleItem(
                    "Generate questions from observations",
                    "When enabled, research questions are automatically derived from your observation patterns and data.",
                    autoQuestions,
                    { enabled ->
                        try {
                            settings.setAutoQuestionsEnabled(enabled)
                        } catch (e: Exception) {
                            errorMessage = "Failed to toggle question generation: ${e.message ?: "Unknown error"}"
                            showErrorDialog = true
                        }
                    },
                    FieldMindIcons.Question
                )
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(FieldMindTheme.colors.info.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Icon(FieldMindIcons.Info, null, tint = FieldMindTheme.colors.info, size = 18.dp)
                        }
                        Text("Daily generation cap", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        """FieldMind limits auto-generation to 20 items (flashcards + questions combined) per day.
                        This prevents duplicate content loops and keeps your study queue manageable.
                        The counter resets automatically each day.""".trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("How it works", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        """When enabled, auto-generation runs in the background after you add new observations.
                        It scans your data for key concepts, patterns, and facts, then builds flashcards and questions — all on your device, with no data leaving your phone.
                        You can always manually create flashcards and questions from the Library tab regardless of these settings.""".trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
