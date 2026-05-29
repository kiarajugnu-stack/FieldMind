@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.screens.settings


import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import chromahub.rhythm.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Slider
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import chromahub.rhythm.app.BuildConfig
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Playlist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.data.repository.PlaybackStatsRepository
import chromahub.rhythm.app.shared.data.repository.StatsTimeRange
import chromahub.rhythm.app.util.GsonUtils
import chromahub.rhythm.app.util.HapticUtils
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlin.system.exitProcess
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.common.ButtonGroupStyle
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveScrollBar
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveButtonGroup
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveGroupButton
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.StandardBottomSheetHeader
import chromahub.rhythm.app.shared.presentation.components.common.StyledProgressBar
import chromahub.rhythm.app.shared.presentation.components.common.ProgressStyle
import chromahub.rhythm.app.shared.presentation.components.common.ThumbStyle
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.LicensesBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.UpdateBottomSheet
import chromahub.rhythm.app.ui.utils.LazyListStateSaver
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeProvider
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapes
import chromahub.rhythm.app.shared.presentation.components.common.buildSplashBackdropShapes
import chromahub.rhythm.app.shared.presentation.components.common.SplashBackgroundOrbs
import chromahub.rhythm.app.shared.presentation.viewmodel.AppUpdaterViewModel
import chromahub.rhythm.app.shared.presentation.viewmodel.AppVersion
import chromahub.rhythm.app.ui.theme.getFontPreviewStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import chromahub.rhythm.app.utils.FontLoader
import chromahub.rhythm.app.ui.theme.parseCustomColorScheme
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.text.HtmlCompat
import chromahub.rhythm.app.shared.presentation.components.common.M3FourColorCircularLoader
import chromahub.rhythm.app.shared.presentation.components.player.PlayingEqIcon
import chromahub.rhythm.app.shared.presentation.components.dialogs.CreatePlaylistDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.BulkPlaylistExportDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.PlaylistImportDialog
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShape
import chromahub.rhythm.app.shared.presentation.components.dialogs.PlaylistOperationProgressDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.PlaylistOperationResultDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.AppRestartDialog
import chromahub.rhythm.app.shared.presentation.components.player.PlayerChipOrderBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.settings.HomeSectionOrderBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.settings.LibraryTabOrderBottomSheet
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem

import chromahub.rhythm.app.shared.presentation.screens.settings.TunerSettingRow
import chromahub.rhythm.app.shared.presentation.screens.settings.TunerAnimatedSwitch
import chromahub.rhythm.app.shared.presentation.screens.settings.TunerSettingCard
import chromahub.rhythm.app.shared.presentation.screens.settings.SettingItem
import chromahub.rhythm.app.shared.presentation.screens.settings.SettingGroup



@Composable
fun BackupInfoItem(
    icon: MaterialSymbolIcon,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}




@Composable
fun BackupRestoreSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val musicViewModel: MusicViewModel = viewModel()

    // Collect states
    val autoBackupEnabled by appSettings.autoBackupEnabled.collectAsState()
    val lastBackupTimestamp by appSettings.lastBackupTimestamp.collectAsState()
    val backupLocation by appSettings.backupLocation.collectAsState()

    // Local states
    var isCreatingBackup by remember { mutableStateOf(false) }
    var isPreparingRestore by remember { mutableStateOf(false) }
    var isRestoringFromFile by remember { mutableStateOf(false) }
    var isRestoringFromClipboard by remember { mutableStateOf(false) }
    var showBackupSelectionSheet by remember { mutableStateOf(false) }
    var showRestoreSelectionSheet by remember { mutableStateOf(false) }
    var pendingRestorePayload by remember { mutableStateOf<String?>(null) }
    var pendingBackupSections by remember { mutableStateOf(AppSettings.BackupRestoreSections()) }
    var backupSections by remember { mutableStateOf(AppSettings.BackupRestoreSections()) }
    var restoreSections by remember { mutableStateOf(AppSettings.BackupRestoreSections()) }
    var resultSheetState by remember { mutableStateOf<BackupRestoreResultState?>(null) }

    val isBusy = isCreatingBackup || isPreparingRestore || isRestoringFromFile || isRestoringFromClipboard

    fun selectedSectionsSummary(sections: AppSettings.BackupRestoreSections): String {
        val lines = mutableListOf<String>()
        if (sections.includeGeneralSettings) lines += "• General app settings"
        if (sections.includeLibraryData) lines += "• Playlists, favorites, and folder lists"
        if (sections.includeStatsAndRhythmGuard) lines += "• Listening stats and Rhythm Guard data"
        return lines.joinToString("\n")
    }

    fun showError(message: String) {
        resultSheetState = BackupRestoreResultState(
            title = context.getString(R.string.ui_error),
            message = message,
            isError = true,
            requiresRestart = false
        )
    }

    // File picker launcher for backup export
    val backupLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                scope.launch {
                    try {
                        isCreatingBackup = true
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)

                        musicViewModel.ensurePlaylistsSaved()
                        val backupJson = appSettings.createBackup(pendingBackupSections)

                        val outputStream = context.contentResolver.openOutputStream(uri)
                            ?: throw IllegalStateException("Unable to open backup destination")
                        outputStream.use { stream ->
                            stream.write(backupJson.toByteArray())
                            stream.flush()
                        }

                        appSettings.setLastBackupTimestamp(System.currentTimeMillis())
                        appSettings.setBackupLocation(uri.toString())

                        resultSheetState = BackupRestoreResultState(
                            title = context.getString(R.string.settings_backup_created),
                            message = "Backup completed successfully.\n\nIncluded sections:\n${selectedSectionsSummary(pendingBackupSections)}",
                            isError = false,
                            requiresRestart = false
                        )

                        // Also copy to clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Rhythm Backup", backupJson)
                        clipboard.setPrimaryClip(clip)
                    } catch (e: Exception) {
                        showError("Failed to create backup: ${e.message}")
                    } finally {
                        isCreatingBackup = false
                    }
                }
            } ?: run {
                isCreatingBackup = false
            }
        } else {
            isCreatingBackup = false
        }
    }

    // File picker launcher for restore
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                scope.launch {
                    try {
                        isRestoringFromFile = true
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)

                        val inputStream = context.contentResolver.openInputStream(uri)
                        val backupJson = inputStream?.bufferedReader()?.use { it.readText() }

                        if (!backupJson.isNullOrEmpty()) {
                            pendingRestorePayload = backupJson
                            restoreSections = AppSettings.BackupRestoreSections()
                            showRestoreSelectionSheet = true
                        } else {
                            showError("Unable to read the backup file")
                        }
                    } catch (e: Exception) {
                        showError("Failed to restore from file: ${e.message}")
                    } finally {
                        isRestoringFromFile = false
                    }
                }
            } ?: run {
                isRestoringFromFile = false
            }
        } else {
            isRestoringFromFile = false
        }
    }

    // Restore from clipboard logic
    fun restoreFromClipboard() {
        scope.launch {
            try {
                isRestoringFromClipboard = true
                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)

                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val backupJson = clip.getItemAt(0).coerceToText(context)?.toString()
                    if (!backupJson.isNullOrBlank()) {
                        pendingRestorePayload = backupJson
                        restoreSections = AppSettings.BackupRestoreSections()
                        showRestoreSelectionSheet = true
                    } else {
                        showError("Clipboard does not contain readable backup text")
                    }
                } else {
                    showError("No backup data found in clipboard. Please copy a backup first.")
                }
            } catch (e: Exception) {
                showError("Failed to restore backup: ${e.message}")
            } finally {
                isRestoringFromClipboard = false
            }
        }
    }

    fun applyRestoreWithSections(backupJson: String, sections: AppSettings.BackupRestoreSections) {
        scope.launch {
            try {
                isPreparingRestore = true
                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)

                if (appSettings.restoreFromBackup(backupJson, sections)) {
                    musicViewModel.reloadPlaylistsFromSettings()
                    resultSheetState = BackupRestoreResultState(
                        title = context.getString(R.string.settings_restore_completed),
                        message = "Restore completed successfully.\n\nRestored sections:\n${selectedSectionsSummary(sections)}",
                        isError = false,
                        requiresRestart = true
                    )
                } else {
                    showError("Invalid backup format, no sections selected, or corrupted data")
                }
            } catch (e: Exception) {
                showError("Failed to restore backup: ${e.message}")
            } finally {
                isPreparingRestore = false
            }
        }
    }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_backup_restore),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                val hasBackup = lastBackupTimestamp > 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasBackup)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (hasBackup)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.1f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (hasBackup) RhythmIcons.CheckCircle else RhythmIcons.Warning,
                                        contentDescription = null,
                                        tint = if (hasBackup)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = if (hasBackup) "Data Backed Up" else "No Backups Found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasBackup)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = if (hasBackup) {
                                        val sdf = SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault())
                                        sdf.format(Date(lastBackupTimestamp))
                                    } else {
                                        "Protect your library and settings"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (hasBackup)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        if (hasBackup && backupLocation != null) {
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = backupLocation!!.substringAfterLast("/"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            val settingGroups = listOf(
                SettingGroup(
                    title = context.getString(R.string.settings_backup_action_short),
                    items = listOf(
                        SettingItem(
                            MaterialSymbolIcon("save"),
                            context.getString(R.string.settings_create_backup),
                            context.getString(R.string.settings_create_backup_desc),
                            onClick = {
                                if (!isBusy) {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                    backupSections = AppSettings.BackupRestoreSections()
                                    showBackupSelectionSheet = true
                                }
                            }
                        ),
                        SettingItem(
                            MaterialSymbolIcon("autorenew"),
                            context.getString(R.string.settings_auto_backup),
                            context.getString(R.string.settings_auto_backup_desc),
                            toggleState = autoBackupEnabled,
                            onToggleChange = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                appSettings.setAutoBackupEnabled(it)
                                if (it) appSettings.triggerImmediateBackup()
                            }
                        )
                    )
                ),
                SettingGroup(
                    title = context.getString(R.string.settings_restore_action_short),
                    items = listOf(
                        SettingItem(
                            RhythmIcons.ContentCopy,
                            context.getString(R.string.settings_restore_clipboard),
                            context.getString(R.string.settings_restore_clipboard_desc),
                            onClick = {
                                if (!isBusy) {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                    restoreFromClipboard()
                                }
                            }
                        ),
                        SettingItem(
                            RhythmIcons.FolderOpen,
                            context.getString(R.string.settings_restore_file),
                            context.getString(R.string.settings_restore_file_desc),
                            onClick = {
                                if (!isBusy) {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                    isRestoringFromFile = true
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "application/json"
                                    }
                                    filePickerLauncher.launch(intent)
                                }
                            }
                        )
                    )
                )
            )

            items(settingGroups, key = { "setting_${it.title}_${settingGroups.indexOf(it)}" }) { group ->
                Spacer(modifier = Modifier.height(8.dp))

                val materialItems = group.items.map { item ->
                    toMaterial3SettingsItem(context = context, item = item, hapticFeedback = haptics)
                }

                Material3SettingsGroup(
                    title = group.title,
                    items = materialItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = MaterialSymbolIcon("lightbulb", filled = true),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.backup_whats_included_placeholder),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        BackupInfoItem(
                            icon = MaterialSymbolIcon("save", filled = true),
                            text = context.getString(R.string.backup_all_settings_placeholder)
                        )
                        BackupInfoItem(
                            icon = MaterialSymbolIcon("restore_from_trash", filled = true),
                            text = context.getString(R.string.backup_restore_tap_placeholder)
                        )
                        BackupInfoItem(
                            icon = RhythmIcons.Security,
                            text = context.getString(R.string.backup_local_storage_placeholder)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showBackupSelectionSheet) {
        BackupRestoreSectionPickerBottomSheet(
            title = stringResource(R.string.backuprestoresettingsscreen_choose_backup_sections),
            subtitle = stringResource(R.string.backuprestoresettingsscreen_select_what_to_include),
            confirmLabel = context.getString(R.string.settings_backup_action_short),
            confirmIcon = MaterialSymbolIcon("backup"),
            sections = backupSections,
            isProcessing = isCreatingBackup,
            onSectionsChange = { backupSections = it },
            onDismiss = { showBackupSelectionSheet = false },
            onConfirm = { selectedSections ->
                if (!selectedSections.hasAtLeastOneSectionSelected) {
                    showError("Choose at least one section to create a backup")
                    return@BackupRestoreSectionPickerBottomSheet
                }

                pendingBackupSections = selectedSections
                showBackupSelectionSheet = false

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(
                        Intent.EXTRA_TITLE,
                        "rhythm_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(Date())}.json"
                    )
                }
                backupLocationLauncher.launch(intent)
            }
        )
    }

    if (showRestoreSelectionSheet && pendingRestorePayload != null) {
        BackupRestoreSectionPickerBottomSheet(
            title = stringResource(R.string.backuprestoresettingsscreen_choose_restore_sections),
            subtitle = stringResource(R.string.backuprestoresettingsscreen_select_which_sections_from),
            confirmLabel = context.getString(R.string.settings_restore_action_short),
            confirmIcon = MaterialSymbolIcon("system_update_alt"),
            sections = restoreSections,
            isProcessing = isPreparingRestore,
            onSectionsChange = { restoreSections = it },
            onDismiss = {
                showRestoreSelectionSheet = false
                pendingRestorePayload = null
            },
            onConfirm = { selectedSections ->
                if (!selectedSections.hasAtLeastOneSectionSelected) {
                    showError("Choose at least one section to restore")
                    return@BackupRestoreSectionPickerBottomSheet
                }

                val backupJson = pendingRestorePayload ?: return@BackupRestoreSectionPickerBottomSheet
                showRestoreSelectionSheet = false
                pendingRestorePayload = null
                applyRestoreWithSections(backupJson, selectedSections)
            }
        )
    }

    resultSheetState?.let { state ->
        BackupRestoreResultBottomSheet(
            state = state,
            onDismiss = { resultSheetState = null },
            onPrimaryAction = {
                resultSheetState = null
                if (state.requiresRestart) {
                    val packageManager = context.packageManager
                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                    val componentName = intent?.component
                    val mainIntent = Intent.makeRestartActivityTask(componentName)
                    context.startActivity(mainIntent)
                    (context as? Activity)?.finish()
                    Runtime.getRuntime().exit(0)
                }
            }
        )
    }
}

@Composable
fun BackupRestoreSectionRow(
    icon: MaterialSymbolIcon,
    title: String,
    description: String,
    checked: Boolean,
    badge: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                TunerAnimatedSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (checked) "Included" else "Excluded",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}