@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package fieldmind.research.app.shared.presentation.screens.settings


import fieldmind.research.app.shared.presentation.components.icons.RhythmIcons
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import fieldmind.research.app.shared.presentation.components.icons.Icon

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
import fieldmind.research.app.R
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
import fieldmind.research.app.BuildConfig
import fieldmind.research.app.shared.data.model.AppSettings
import fieldmind.research.app.shared.data.model.Playlist
import fieldmind.research.app.shared.data.model.Song
import fieldmind.research.app.shared.data.repository.PlaybackStatsRepository
import fieldmind.research.app.shared.data.repository.StatsTimeRange
import fieldmind.research.app.util.GsonUtils
import fieldmind.research.app.util.HapticUtils
import fieldmind.research.app.util.HapticType
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlin.system.exitProcess
import fieldmind.research.app.shared.presentation.components.common.CollapsibleHeaderScreen
import fieldmind.research.app.shared.presentation.components.common.ButtonGroupStyle
import fieldmind.research.app.shared.presentation.components.common.ExpressiveScrollBar
import fieldmind.research.app.shared.presentation.components.common.ExpressiveButtonGroup
import fieldmind.research.app.shared.presentation.components.common.ExpressiveGroupButton
import fieldmind.research.app.shared.presentation.components.bottomsheets.StandardBottomSheetHeader
import fieldmind.research.app.shared.presentation.components.common.StyledProgressBar
import fieldmind.research.app.shared.presentation.components.common.ProgressStyle
import fieldmind.research.app.shared.presentation.components.common.ThumbStyle
import fieldmind.research.app.shared.presentation.components.bottomsheets.LicensesBottomSheet
import fieldmind.research.app.shared.presentation.components.bottomsheets.UpdateBottomSheet
import fieldmind.research.app.ui.utils.LazyListStateSaver
import fieldmind.research.app.features.local.presentation.viewmodel.MusicViewModel
import fieldmind.research.app.shared.presentation.components.common.ExpressiveShapeProvider
import fieldmind.research.app.shared.presentation.components.common.ExpressiveShapes
import fieldmind.research.app.shared.presentation.components.common.buildSplashBackdropShapes
import fieldmind.research.app.shared.presentation.components.common.SplashBackgroundOrbs
import fieldmind.research.app.shared.presentation.viewmodel.AppUpdaterViewModel
import fieldmind.research.app.shared.presentation.viewmodel.AppVersion
import fieldmind.research.app.ui.theme.getFontPreviewStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import fieldmind.research.app.utils.FontLoader
import fieldmind.research.app.ui.theme.parseCustomColorScheme
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.text.HtmlCompat
import fieldmind.research.app.shared.presentation.components.common.M3FourColorCircularLoader
import fieldmind.research.app.shared.presentation.components.player.PlayingEqIcon
import fieldmind.research.app.shared.presentation.components.dialogs.CreatePlaylistDialog
import fieldmind.research.app.shared.presentation.components.dialogs.BulkPlaylistExportDialog
import fieldmind.research.app.shared.presentation.components.dialogs.PlaylistImportDialog
import fieldmind.research.app.shared.presentation.components.common.rememberExpressiveShape
import fieldmind.research.app.shared.presentation.components.dialogs.PlaylistOperationProgressDialog
import fieldmind.research.app.shared.presentation.components.dialogs.PlaylistOperationResultDialog
import fieldmind.research.app.shared.presentation.components.dialogs.AppRestartDialog
import fieldmind.research.app.shared.presentation.components.player.PlayerChipOrderBottomSheet
import fieldmind.research.app.features.local.presentation.components.settings.HomeSectionOrderBottomSheet
import fieldmind.research.app.features.local.presentation.components.settings.LibraryTabOrderBottomSheet
import fieldmind.research.app.shared.presentation.components.Material3SettingsGroup
import fieldmind.research.app.shared.presentation.components.Material3SettingsItem

import fieldmind.research.app.shared.presentation.screens.settings.TunerSettingRow
import fieldmind.research.app.shared.presentation.screens.settings.TunerAnimatedSwitch
import fieldmind.research.app.shared.presentation.screens.settings.TunerSettingCard
import fieldmind.research.app.shared.presentation.screens.settings.SettingItem
import fieldmind.research.app.shared.presentation.screens.settings.SettingGroup


// Individual screens with actual settings
@Composable
fun NotificationsSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val hapticFeedback = LocalHapticFeedback.current
    val updateNotificationsEnabled by appSettings.updateNotificationsEnabled.collectAsState()
    val updateStatusNotificationsEnabled by appSettings.updateStatusNotificationsEnabled.collectAsState()
    val rhythmGuardAlertNotificationsEnabled by appSettings.rhythmGuardAlertNotificationsEnabled.collectAsState()
    val rhythmGuardTimerNotificationsEnabled by appSettings.rhythmGuardTimerNotificationsEnabled.collectAsState()
    val rhythmPulseNotificationsEnabled by appSettings.rhythmPulseNotificationsEnabled.collectAsState()
    val rhythmPulseNotificationIntervalHours by appSettings.rhythmPulseNotificationIntervalHours.collectAsState()

    val mergedUpdateNotificationsEnabled =
        updateNotificationsEnabled || updateStatusNotificationsEnabled

    var showPulseIntervalDialog by remember { mutableStateOf(false) }

    val pulseIntervalLabel = when (rhythmPulseNotificationIntervalHours) {
        6 -> context.getString(R.string.settings_interval_every_6_hours)
        12 -> context.getString(R.string.settings_interval_every_12_hours)
        24 -> context.getString(R.string.settings_interval_once_a_day)
        48 -> context.getString(R.string.settings_interval_every_48_hours)
        72 -> context.getString(R.string.settings_interval_every_72_hours)
        else -> context.getString(R.string.settings_check_interval_value, rhythmPulseNotificationIntervalHours)
    }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_notifications),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
        val settingGroups = listOf(
            SettingGroup(
                title = context.getString(R.string.settings_notifications_updates_group),
                items = listOf(
                    SettingItem(
                        RhythmIcons.Update,
                        context.getString(R.string.settings_update_notifications),
                        context.getString(R.string.settings_update_notifications_merged_desc),
                        toggleState = mergedUpdateNotificationsEnabled,
                        onToggleChange = {
                            appSettings.setUpdateNotificationsEnabled(it)
                            appSettings.setUpdateStatusNotificationsEnabled(it)
                        }
                    )
                )
            ),
            SettingGroup(
                title = context.getString(R.string.settings_notifications_rhythm_guard_group),
                items = listOf(
                    SettingItem(
                        RhythmIcons.Warning,
                        context.getString(R.string.settings_rhythm_guard_alert_notifications),
                        context.getString(R.string.settings_rhythm_guard_alert_notifications_desc),
                        toggleState = rhythmGuardAlertNotificationsEnabled,
                        onToggleChange = { appSettings.setRhythmGuardAlertNotificationsEnabled(it) }
                    ),
                    SettingItem(
                        RhythmIcons.AccessTime,
                        context.getString(R.string.settings_rhythm_guard_timer_notifications),
                        context.getString(R.string.settings_rhythm_guard_timer_notifications_desc),
                        toggleState = rhythmGuardTimerNotificationsEnabled,
                        onToggleChange = { appSettings.setRhythmGuardTimerNotificationsEnabled(it) }
                    )
                )
            ),
            SettingGroup(
                title = context.getString(R.string.settings_notifications_rhythm_pulse_group),
                items = listOf(
                    SettingItem(
                        MaterialSymbolIcon("celebration"),
                        context.getString(R.string.settings_rhythm_pulse_notifications),
                        context.getString(R.string.settings_rhythm_pulse_notifications_desc),
                        toggleState = rhythmPulseNotificationsEnabled,
                        onToggleChange = { appSettings.setRhythmPulseNotificationsEnabled(it) }
                    ),
                    SettingItem(
                        RhythmIcons.AccessTime,
                        context.getString(R.string.settings_rhythm_pulse_interval),
                        pulseIntervalLabel,
                        onClick = { showPulseIntervalDialog = true },
                        enabled = rhythmPulseNotificationsEnabled
                    )
                )
            )
        )

        val lazyListState = rememberSaveable(
            saver = LazyListStateSaver
        ) {
            androidx.compose.foundation.lazy.LazyListState()
        }

        LazyColumn(
            state = lazyListState,
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            items(
                items = settingGroups,
                key = { "setting_${it.title}" },
                contentType = { "settingGroup" }
            ) { group ->
                Spacer(modifier = Modifier.height(24.dp))

                val materialItems = group.items.map { item ->
                    toMaterial3SettingsItem(
                        context = context,
                        item = item,
                        hapticFeedback = hapticFeedback
                    )
                }

                Material3SettingsGroup(
                    title = group.title,
                    items = materialItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }
        }
    }

    if (showPulseIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showPulseIntervalDialog = false },
            icon = {
                Icon(
                    imageVector = RhythmIcons.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text(context.getString(R.string.settings_rhythm_pulse_interval_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val intervals = listOf(
                        6 to context.getString(R.string.settings_interval_every_6_hours),
                        12 to context.getString(R.string.settings_interval_every_12_hours),
                        24 to context.getString(R.string.settings_interval_once_a_day),
                        48 to context.getString(R.string.settings_interval_every_48_hours),
                        72 to context.getString(R.string.settings_interval_every_72_hours)
                    )

                    intervals.forEach { (hours, label) ->
                        Card(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, hapticFeedback, HapticType.LIGHT)
                                appSettings.setRhythmPulseNotificationIntervalHours(hours)
                                showPulseIntervalDialog = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (rhythmPulseNotificationIntervalHours == hours)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (rhythmPulseNotificationIntervalHours == hours) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (rhythmPulseNotificationIntervalHours == hours) {
                                    Icon(
                                        imageVector = RhythmIcons.CheckCircle,
                                        contentDescription = context.getString(R.string.ui_selected),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showPulseIntervalDialog = false }) {
                    Text(context.getString(R.string.ui_close))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}