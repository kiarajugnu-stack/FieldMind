package chromahub.rhythm.app.features.field.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.features.field.data.database.entity.*
import chromahub.rhythm.app.features.field.data.settings.FieldMindSettings
import chromahub.rhythm.app.features.field.presentation.components.*
import chromahub.rhythm.app.features.field.presentation.theme.FieldMindTheme
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
// ══════════════════════════════════════════════════════════════════════
//  Settings
// ══════════════════════════════════════════════════════════════════════

@Composable
fun FieldMindSettingsScreen(viewModel: FieldMindViewModel? = null, onBack: () -> Unit, onResetOnboarding: () -> Unit) {
    val context = LocalContext.current
    val settings = viewModel?.fieldSettings ?: chromahub.rhythm.app.features.field.data.settings.FieldMindSettings.getInstance(context)
    val goal by settings.dailyObservationGoal.collectAsState()
    val category by settings.defaultCategory.collectAsState()
    val confidence by settings.defaultConfidence.collectAsState()
    val locationMode by settings.locationMode.collectAsState()
    val media by settings.mediaAttachmentsEnabled.collectAsState()
    val audio by settings.audioRecordingEnabled.collectAsState()
    val exportMode by settings.attachmentExportMode.collectAsState()
    val ai by settings.geminiEnabled.collectAsState()
    val provider by settings.aiProvider.collectAsState()
    val key by settings.geminiApiKey.collectAsState()
    val model by settings.geminiModel.collectAsState()
    val openAiKey by settings.openAiApiKey.collectAsState()
    val openAiModel by settings.openAiModel.collectAsState()
    val confirm by settings.aiRequireConfirmBeforeSave.collectAsState()
    val sendAttachments by settings.aiSendAttachments.collectAsState()
    val reminders by settings.remindersEnabled.collectAsState()
    val streaks by settings.streaksEnabled.collectAsState()
    val exportFormat by settings.defaultExportFormat.collectAsState()
    val privacy by settings.privacyLockEnabled.collectAsState()
    val dynamicColor by settings.dynamicColorEnabled.collectAsState()
    val themeMode by settings.themeMode.collectAsState()
    val profileName by settings.profileName.collectAsState()
    val profileRole by settings.profileRole.collectAsState()
    val profileFocus by settings.profileFocus.collectAsState()
    val localModelEnabled by settings.localModelEnabled.collectAsState()
    val localModelOption by settings.localModelOption.collectAsState()
    val localModelDownloaded by settings.localModelDownloaded.collectAsState()
    val localModelUseForStudy by settings.localModelUseForStudy.collectAsState()
    val autoBackupEnabled by settings.autoBackupEnabled.collectAsState()
    val autoBackupInterval by settings.autoBackupInterval.collectAsState()
    if (!FieldMindPrivacyGate(privacy, "Settings are locked", "Confirm your device lock to edit FieldMind privacy, export, and AI settings.")) return
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { FieldScreenHeader("Settings", "Offline-first setup, profile, capture, local AI, export, and privacy.", icon = FieldMindIcons.Settings, actionIcon = FieldMindIcons.Back, onAction = onBack) }

        item {
            SettingsGroup("Research profile", "Personalizes insights and setup while staying on this device.") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("FieldMind has no app server: profile, observations, sources, and local model settings are stored on this device unless you export or share them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = profileName, onValueChange = settings::setProfileName, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true)
                    ChoiceChips(listOf("Field learner", "Student", "Naturalist", "Researcher"), profileRole) { settings.setProfileRole(it) }
                    ChoiceItem("Focus", listOf("Wildlife & ecology", "Plants & botany", "Weather", "Water", "Geology", "General science"), profileFocus, FieldMindIcons.Nature, settings::setProfileFocus)
                }
            }
        }

        item {
            SettingsGroup("Appearance") {
                ChoiceItem("Theme", listOf("System", "Light", "Dark"), themeMode, FieldMindIcons.Palette, settings::setThemeMode)
                SettingDivider()
                ToggleItem("Material You dynamic color", "Use system wallpaper colors that auto-adapt to light/dark. Off keeps the FieldMind brand palette.", dynamicColor, settings::setDynamicColorEnabled, FieldMindIcons.Palette)
            }
        }

        item {
            SettingsGroup("Capture defaults") {
                StepperItem("Daily observation goal", "Drives the Today dashboard and progress ring.", goal, FieldMindIcons.Today) { settings.setDailyObservationGoal(it) }
                SettingDivider()
                ChoiceItem("Default category", observationCategories, category, FieldMindIcons.Observation, settings::setDefaultCategory)
                SettingDivider()
                ChoiceItem("Default confidence", confidenceOptions, confidence, FieldMindIcons.Check, settings::setDefaultConfidence)
                SettingDivider()
                ChoiceItem("Location mode", listOf("Manual only", "Approximate", "Precise"), locationMode, FieldMindIcons.Location, settings::setLocationMode)
            }
        }

        item {
            SettingsGroup("Evidence & media") {
                ToggleItem("Media attachments", "Enable camera, gallery, and file evidence tools.", media, settings::setMediaAttachmentsEnabled, FieldMindIcons.Camera)
                SettingDivider()
                ToggleItem("Audio recording", "Enable voice-note evidence capture with an in-app recording indicator.", audio, settings::setAudioRecordingEnabled, FieldMindIcons.Mic)
                SettingDivider()
                ChoiceItem("Attachment export", listOf("Reference URIs", "Copy media later", "Skip media"), exportMode, FieldMindIcons.Export, settings::setAttachmentExportMode)
            }
        }

        item {
            SettingsGroup("AI assistant", "Optional. Choose Gemini or OpenAI; nothing is sent without an explicit action.") {
                ToggleItem("Enable AI assistant", "Review factuality, suggest papers, and answer questions.", ai, settings::setGeminiEnabled, FieldMindIcons.Sparkle)
                if (ai) {
                    SettingDivider()
                    ChoiceItem("Provider", listOf("Gemini", "OpenAI"), provider, FieldMindIcons.Sparkle, settings::setAiProvider)
                    SettingDivider()
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (provider == "OpenAI") {
                            OutlinedTextField(value = openAiKey, onValueChange = settings::setOpenAiApiKey, label = { Text("OpenAI API key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true, supportingText = { Text(if (openAiKey.isBlank()) "No OpenAI key saved. You can ship a managed key here for private builds." else "OpenAI key saved locally on this device.") })
                            ChoiceChips(listOf("gpt-4.1-mini", "gpt-4.1", "gpt-4o-mini"), openAiModel) { settings.setOpenAiModel(it) }
                        } else {
                            OutlinedTextField(value = key, onValueChange = settings::setGeminiApiKey, label = { Text("Gemini API key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true, supportingText = { Text(if (key.isBlank()) "No key saved — get one at aistudio.google.com/apikey." else "Key saved locally on this device only.") })
                            ChoiceChips(listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash"), model) { settings.setGeminiModel(it) }
                        }
                    }
                    SettingDivider()
                    ToggleItem("Confirm before saving AI output", "AI suggestions stay as previews unless you apply them.", confirm, settings::setAiRequireConfirmBeforeSave, FieldMindIcons.Check)
                    SettingDivider()
                    ToggleItem("Allow attachment context", "Off by default to protect field evidence privacy.", sendAttachments, settings::setAiSendAttachments, FieldMindIcons.File)
                }
            }
        }

        item {
            SettingsGroup("Local model", "Download an app-private offline model for automated flashcards and review prompts.") {
                ToggleItem("Use local model", "Runs study generation on-device after the model is downloaded.", localModelEnabled, settings::setLocalModelEnabled, FieldMindIcons.Sparkle)
                SettingDivider()
                ChoiceItem("Model size", listOf("FieldLite 500 MB", "FieldCore 1 GB", "FieldPro 2 GB"), localModelOption, FieldMindIcons.Download, settings::setLocalModelOption)
                SettingDivider()
                ToggleItem("Use for flashcards/reviews", "Prefer the downloaded local model for automatic cards and review suggestions.", localModelUseForStudy, settings::setLocalModelUseForStudy, FieldMindIcons.Flashcard)
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (localModelDownloaded) "$localModelOption is marked downloaded inside FieldMind." else "Choose a size from 500 MB to 2 GB, then download it into app storage before offline generation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { settings.setLocalModelDownloaded(true); android.widget.Toast.makeText(context, "$localModelOption ready for offline study", android.widget.Toast.LENGTH_SHORT).show() }, enabled = localModelEnabled, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Text(if (localModelDownloaded) "Re-download / verify model" else "Download inside app") }
                }
            }
        }

        item {
            SettingsGroup("Backup & import", "Portable files only. Auto-backup scheduling is stored locally.") {
                ToggleItem("Auto backup", "Writes private archive JSON files on the selected schedule; manual export still chooses shared locations.", autoBackupEnabled, settings::setAutoBackupEnabled, FieldMindIcons.Archive)
                SettingDivider()
                ChoiceItem("Backup interval", listOf("Daily", "Weekly", "Monthly"), autoBackupInterval, FieldMindIcons.Today, settings::setAutoBackupInterval)
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Auto-backups stay in app-private storage at files/fieldmind/backups and keep the latest 8 archives. Nothing is uploaded automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            SettingsGroup("Discipline & ownership") {
                ToggleItem("Reminders", "Schedules a daily WorkManager prompt and skips it after you log today’s observation.", reminders, settings::setRemindersEnabled, FieldMindIcons.Notifications)
                SettingDivider()
                ToggleItem("Streaks", "Shows consecutive observation days on the Today dashboard without replacing real work.", streaks, settings::setStreaksEnabled, FieldMindIcons.Streak)
            }
        }

        item {
            SettingsGroup("Export & privacy") {
                ChoiceItem("Default export format", listOf("Markdown", "CSV", "JSON", "PNG", "SVG", "Plain text"), exportFormat, FieldMindIcons.Export, settings::setDefaultExportFormat)
                SettingDivider()
                ToggleItem("Privacy lock", "Persisted toggle; wires to the app lock flow when available.", privacy, settings::setPrivacyLockEnabled, FieldMindIcons.Lock)
            }
        }

        if (viewModel != null) item { SettingsExportSection(viewModel) }

        item { AboutSection() }

        item { OutlinedButton(onClick = onResetOnboarding, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Reset onboarding") } }
    }
}

@Composable
private fun SettingsExportSection(viewModel: FieldMindViewModel) {
    val context = LocalContext.current
    SettingsGroup("Export Studio", "Preview scope, schedule auto backups, import a backup, or save portable research files.") {
        ExportStudioContent(
            viewModel = viewModel,
            modifier = Modifier.heightIn(max = 760.dp),
            contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 16.dp),
            showHeader = false,
            onMessage = { message -> android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show() }
        )
    }
}

@Composable
private fun AboutSection() {
    val uriHandler = LocalUriHandler.current
    SettingsGroup("About", "FieldMind — observe, question, research clearly.") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("FieldMind is a free, offline-first research notebook for curious naturalists, students, and citizen scientists.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Credits & acknowledgements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            CreditRow("Open-source Android foundation", "FieldMind is built on a modern Compose app foundation.", "https://developer.android.com/jetpack/compose", uriHandler)
            CreditRow("Material Symbols & Material 3", "Google's icon set and design system.", "https://fonts.google.com/icons", uriHandler)
            CreditRow("Jetpack Compose", "Android's modern declarative UI toolkit.", "https://developer.android.com/jetpack/compose", uriHandler)
            CreditRow("Crossref", "Free scholarly metadata API.", "https://www.crossref.org", uriHandler)
            CreditRow("OpenAlex", "Open catalog of papers, authors, and venues.", "https://openalex.org", uriHandler)
            CreditRow("arXiv", "Open-access research preprints.", "https://arxiv.org", uriHandler)
            CreditRow("Open Library", "Open, editable library catalog.", "https://openlibrary.org", uriHandler)
            CreditRow("Semantic Scholar", "AI-powered research paper search.", "https://www.semanticscholar.org", uriHandler)
            Text("Made with care for people who learn by looking closely.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreditRow(title: String, subtitle: String, url: String, uriHandler: androidx.compose.ui.platform.UriHandler) {
    Row(Modifier.fillMaxWidth().clickable { runCatching { uriHandler.openUri(url) } }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon = FieldMindIcons.OpenLink, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

