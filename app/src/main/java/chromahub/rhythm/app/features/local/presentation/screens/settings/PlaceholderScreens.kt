@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.features.local.presentation.screens.settings


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
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.StandardBottomSheetHeader
import chromahub.rhythm.app.shared.presentation.components.common.StyledProgressBar
import chromahub.rhythm.app.shared.presentation.components.common.ProgressStyle
import chromahub.rhythm.app.shared.presentation.components.common.ThumbStyle
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.LicensesBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.UpdateBottomSheet
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
import chromahub.rhythm.app.features.local.presentation.components.player.PlayingEqIcon
import chromahub.rhythm.app.features.local.presentation.components.dialogs.CreatePlaylistDialog
import chromahub.rhythm.app.features.local.presentation.components.dialogs.BulkPlaylistExportDialog
import chromahub.rhythm.app.features.local.presentation.components.dialogs.PlaylistImportDialog
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShape
import chromahub.rhythm.app.features.local.presentation.components.dialogs.PlaylistOperationProgressDialog
import chromahub.rhythm.app.features.local.presentation.components.dialogs.PlaylistOperationResultDialog
import chromahub.rhythm.app.features.local.presentation.components.dialogs.AppRestartDialog
import chromahub.rhythm.app.features.local.presentation.components.player.PlayerChipOrderBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.settings.HomeSectionOrderBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.settings.LibraryTabOrderBottomSheet
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem

@Composable
fun TunerSettingRow(item: SettingItem) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val hapticFeedbackEnabled by appSettings.hapticFeedbackEnabled.collectAsState()
    
    // Animation states
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tuner_setting_scale"
    )
    
    val iconBackgroundColor by animateColorAsState(
        targetValue = when {
            !item.enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            item.toggleState == true -> MaterialTheme.colorScheme.primaryContainer
            isPressed -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tuner_icon_bg_color"
    )
    
    val iconTintColor by animateColorAsState(
        targetValue = when {
            !item.enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            item.toggleState == true -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tuner_icon_tint_color"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (item.enabled) 1f else 0.6f
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (item.enabled && item.onClick != null && item.toggleState == null) {
                    Modifier.clickable(onClick = {
                        isPressed = true
                        HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.LongPress)
                        item.onClick()
                    })
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container with expressive design
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(34.dp),
            color = iconBackgroundColor,
            tonalElevation = if (item.toggleState == true) 2.dp else 0.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(24.dp),
                    tint = iconTintColor
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (item.enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            item.description?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
            }
        }

        if (item.toggleState != null && item.onClick != null) {
            Icon(
                imageVector = MaterialSymbolIcon("arrow_forward_ios", filled = true),
                contentDescription = context.getString(R.string.cd_navigate),
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            TunerAnimatedSwitch(
                checked = item.toggleState,
                onCheckedChange = {
                    if (!item.enabled) return@TunerAnimatedSwitch
                    HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.TextHandleMove)
                    item.onToggleChange?.invoke(it)
                },
                enabled = item.enabled
            )
        } else if (item.toggleState != null) {
            TunerAnimatedSwitch(
                checked = item.toggleState,
                onCheckedChange = {
                    if (!item.enabled) return@TunerAnimatedSwitch
                    HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.TextHandleMove)
                    item.onToggleChange?.invoke(it)
                },
                enabled = item.enabled
            )
        } else if (item.onClick != null) {
            Icon(
                imageVector = MaterialSymbolIcon("arrow_forward_ios", filled = true),
                contentDescription = context.getString(R.string.cd_navigate),
                modifier = Modifier.size(20.dp),
                tint = if (item.enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                }
            )
        }
    }
}

@Composable
fun TunerAnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isAppDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val thumbColor by animateColorAsState(
        targetValue = when {
            checked && !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            checked -> MaterialTheme.colorScheme.onPrimary
            else -> if (isAppDarkTheme) Color.White else Color.Black
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tuner_thumb_color"
    )
    
    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled && checked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
            checked -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tuner_track_color"
    )

    val iconTint by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = if (enabled) 1f else 0.55f)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tuner_switch_icon_tint"
    )
    
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = thumbColor,
            checkedTrackColor = trackColor,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = thumbColor,
            uncheckedTrackColor = trackColor,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            disabledCheckedThumbColor = thumbColor,
            disabledCheckedTrackColor = trackColor,
            disabledUncheckedThumbColor = thumbColor,
            disabledUncheckedTrackColor = trackColor,
        ),
        thumbContent = {
            AnimatedContent(
                targetState = checked,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(140)) + scaleIn(initialScale = 0.85f)) togetherWith
                        (fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.85f))
                },
                label = "tuner_switch_icon"
            ) { isChecked ->
                Icon(
                    imageVector = if (isChecked) RhythmIcons.Check else RhythmIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = iconTint
                )
            }
        }
    )
}

@Composable
private fun TunerSettingCard(
    title: String,
    description: String,
    icon: MaterialSymbolIcon,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && checked == null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (checked != null && onCheckedChange != null) {
                TunerAnimatedSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            } else if (onClick != null) {
                val context = LocalContext.current
                Icon(
                    imageVector = MaterialSymbolIcon("arrow_forward_ios", filled = true),
                    contentDescription = context.getString(R.string.cd_navigate),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun toMaterial3SettingsItem(
    context: Context,
    item: SettingItem,
    hapticFeedback: HapticFeedback? = null,
    description: (@Composable () -> Unit)? = item.description?.let { desc -> { Text(desc) } }
): Material3SettingsItem {
    return Material3SettingsItem(
        icon = item.icon,
        title = { Text(item.title) },
        description = description,
        trailingContent = if (item.toggleState != null && item.onClick != null) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = MaterialSymbolIcon("arrow_forward_ios", filled = true),
                        contentDescription = context.getString(R.string.cd_navigate),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    TunerAnimatedSwitch(
                        checked = item.toggleState,
                        onCheckedChange = {
                            if (!item.enabled) return@TunerAnimatedSwitch
                            hapticFeedback?.let { haptic ->
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            }
                            item.onToggleChange?.invoke(it)
                        },
                        enabled = item.enabled
                    )
                }
            }
        } else if (item.toggleState != null) {
            {
                TunerAnimatedSwitch(
                    checked = item.toggleState,
                    onCheckedChange = {
                        if (!item.enabled) return@TunerAnimatedSwitch
                        hapticFeedback?.let { haptic ->
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        }
                        item.onToggleChange?.invoke(it)
                    },
                    enabled = item.enabled
                )
            }
        } else if (item.onClick != null) {
            {
                Icon(
                    imageVector = MaterialSymbolIcon("arrow_forward_ios", filled = true),
                    contentDescription = context.getString(R.string.cd_navigate),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            null
        },
        isHighlighted = item.toggleState == true,
        enabled = item.enabled,
        onClick = when {
            item.onClick != null -> {
                {
                    if (item.enabled) {
                        hapticFeedback?.let { haptic ->
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                        }
                        item.onClick.invoke()
                    }
                }
            }

            item.toggleState != null && item.onToggleChange != null -> {
                {
                    if (item.enabled) {
                        hapticFeedback?.let { haptic ->
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        }
                        item.onToggleChange.invoke(!item.toggleState)
                    }
                }
            }

            else -> null
        }
    )
}

private data class ArtworkCacheStats(
    val sizeBytes: Long,
    val fileCount: Int
)

private const val ARTWORK_CACHE_TRIM_MAX_BYTES = 256L * 1024 * 1024
private const val ARTWORK_CACHE_TRIM_MAX_FILES = 1200

private fun collectArtworkCacheFiles(cacheDir: File): MutableList<File> {
    val artworkCacheDir = File(cacheDir, "embedded_artwork")
    val currentArtworkFiles = artworkCacheDir
        .listFiles { file -> file.isFile }
        ?.toMutableList()
        ?: mutableListOf()

    val legacyArtworkFiles = cacheDir
        .listFiles { file ->
            file.isFile &&
                (file.name.startsWith("embedded_art_") || file.name.startsWith("embedded_art_lossless_"))
        }
        ?.toList()
        .orEmpty()

    return mutableListOf<File>().apply {
        addAll(currentArtworkFiles)
        addAll(legacyArtworkFiles)
    }
}

private fun readArtworkCacheStats(cacheDir: File): ArtworkCacheStats {
    val files = collectArtworkCacheFiles(cacheDir)
    return ArtworkCacheStats(
        sizeBytes = files.sumOf { it.length() },
        fileCount = files.size
    )
}

private fun trimArtworkCacheNow(cacheDir: File): ArtworkCacheStats {
    val files = collectArtworkCacheFiles(cacheDir)
    if (files.isEmpty()) {
        return ArtworkCacheStats(sizeBytes = 0L, fileCount = 0)
    }

    var totalSize = files.sumOf { it.length() }
    var fileCount = files.size

    if (totalSize <= ARTWORK_CACHE_TRIM_MAX_BYTES && fileCount <= ARTWORK_CACHE_TRIM_MAX_FILES) {
        return ArtworkCacheStats(sizeBytes = totalSize, fileCount = fileCount)
    }

    files.sortBy { it.lastModified() }

    for (file in files) {
        if (totalSize <= ARTWORK_CACHE_TRIM_MAX_BYTES && fileCount <= ARTWORK_CACHE_TRIM_MAX_FILES) {
            break
        }

        val fileSize = file.length()
        if (file.delete()) {
            totalSize -= fileSize
            fileCount--
        }
    }

    return ArtworkCacheStats(
        sizeBytes = totalSize.coerceAtLeast(0L),
        fileCount = fileCount.coerceAtLeast(0)
    )
}

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
                                HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.TextHandleMove)
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

// Queue & Playback Settings Screen
@Composable
fun QueuePlaybackSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val hapticFeedback = LocalHapticFeedback.current

    val shuffleUsesExoplayer by appSettings.shuffleUsesExoplayer.collectAsState()
    val autoAddToQueue by appSettings.autoAddToQueue.collectAsState()
    val clearQueueOnNewSong by appSettings.clearQueueOnNewSong.collectAsState()
    val hidePlayedQueueSongs by appSettings.hidePlayedQueueSongs.collectAsState()
    val contextQueuePreference by appSettings.contextQueuePreference.collectAsState()
    val contextQueuePersistenceRaw by appSettings.contextQueuePersistence.collectAsState()
    val showAlreadyPlayedSongsInQueue = !hidePlayedQueueSongs
    val effectiveContextQueuePreference = if (contextQueuePreference == "GENRE_FIRST") {
        "GENRE_FIRST"
    } else {
        "ARTIST_FIRST"
    }
    val showQueueDialog by appSettings.showQueueDialog.collectAsState()
    val repeatModePersistence by appSettings.repeatModePersistence.collectAsState()
    val shuffleModePersistence by appSettings.shuffleModePersistence.collectAsState()
    val queuePersistenceEnabled by appSettings.queuePersistenceEnabled.collectAsState()
    val playlistClickBehavior by appSettings.playlistClickBehavior.collectAsState(initial = "ask")
    val listQueueActionBehavior by appSettings.listQueueActionBehavior.collectAsState(initial = "replace")
    val useHoursInTimeFormat by appSettings.useHoursInTimeFormat.collectAsState()
    val gaplessEnabled by appSettings.gaplessPlayback.collectAsState()
    val crossfadeEnabled by appSettings.crossfade.collectAsState()
    val crossfadeDuration by appSettings.crossfadeDuration.collectAsState()
    val crossfadeRepeatOne by appSettings.crossfadeRepeatOne.collectAsState()
    val stopPlaybackOnAppClose by appSettings.stopPlaybackOnAppClose.collectAsState()

    var showPlaylistBehaviorDialog by remember { mutableStateOf(false) }
    var showListQueueBehaviorDialog by remember { mutableStateOf(false) }
    var showQueueDialogSettingDialog by remember { mutableStateOf(false) }
    var showContextPrefBottomSheet by remember { mutableStateOf(false) }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_queue_playback),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
        val settingGroups = listOf(
            SettingGroup(
                title = context.getString(R.string.settings_queue_behavior),
                items = buildList {
                    add(
                        SettingItem(
                            RhythmIcons.Shuffle,
                            context.getString(R.string.settings_use_exoplayer_shuffle),
                            context.getString(R.string.settings_use_exoplayer_shuffle_desc),
                            toggleState = shuffleUsesExoplayer,
                            onToggleChange = { appSettings.setShuffleUsesExoplayer(it) }
                        )
                    )
                    add(
                        SettingItem(
                            RhythmIcons.AddToQueue,
                            context.getString(R.string.settings_auto_queue),
                            context.getString(R.string.settings_auto_queue_desc),
                            toggleState = autoAddToQueue,
                            onToggleChange = { appSettings.setAutoAddToQueue(it) }
                        )
                    )
                    if (autoAddToQueue) {
                        add(
                            SettingItem(
                                RhythmIcons.Tune,
                                context.getString(R.string.settings_context_queue_preference),
                                when (effectiveContextQueuePreference) {
                                    "ARTIST_FIRST" -> context.getString(R.string.settings_context_pref_artist_first)
                                    else -> context.getString(R.string.settings_context_pref_genre_first)
                                },
                                onClick = { showContextPrefBottomSheet = true }
                            )
                        )
                        add(
                            SettingItem(
                                RhythmIcons.Repeat,
                                context.getString(R.string.settings_context_queue_persistence),
                                context.getString(R.string.settings_context_queue_persistence_desc),
                                data = "context_queue_persistence"
                            )
                        )
                    }
                    add(
                        SettingItem(
                            RhythmIcons.Delete,
                            context.getString(R.string.settings_clear_queue_on_new_song),
                            context.getString(R.string.settings_clear_queue_on_new_song_desc),
                            toggleState = clearQueueOnNewSong,
                            onToggleChange = { appSettings.setClearQueueOnNewSong(it) }
                        )
                    )
                    add(
                        SettingItem(
                            RhythmIcons.Queue,
                            context.getString(R.string.settings_show_played_queue_songs),
                            context.getString(R.string.settings_show_played_queue_songs_desc),
                            toggleState = showAlreadyPlayedSongsInQueue,
                            onToggleChange = { appSettings.setHidePlayedQueueSongs(!it) }
                        )
                    )
                    add(
                        SettingItem(
                            MaterialSymbolIcon("help", filled = true),
                            context.getString(R.string.settings_queue_action_dialog),
                            when {
                                clearQueueOnNewSong -> context.getString(R.string.settings_queue_action_dialog_desc_disabled)
                                showQueueDialog -> context.getString(R.string.settings_queue_action_dialog_desc_ask)
                                else -> context.getString(R.string.settings_queue_action_dialog_desc_always)
                            },
                            onClick = { showQueueDialogSettingDialog = true },
                            enabled = !clearQueueOnNewSong
                        )
                    )
                    add(
                        SettingItem(
                            RhythmIcons.Queue,
                            context.getString(R.string.settings_playlist_action_dialog),
                            when (playlistClickBehavior) {
                                "play_all" -> context.getString(R.string.settings_playlist_action_play_all)
                                "play_one" -> context.getString(R.string.settings_playlist_action_play_one)
                                else -> context.getString(R.string.settings_playlist_action_ask)
                            },
                            onClick = { showPlaylistBehaviorDialog = true }
                        )
                    )
                    add(
                        SettingItem(
                            RhythmIcons.Sort,
                            context.getString(R.string.settings_list_queue_action_dialog),
                            when (listQueueActionBehavior) {
                                "ask" -> context.getString(R.string.settings_list_queue_action_ask)
                                "play_next" -> context.getString(R.string.settings_list_queue_action_play_next)
                                "add_to_end" -> context.getString(R.string.settings_list_queue_action_add_to_end)
                                else -> context.getString(R.string.settings_list_queue_action_replace)
                            },
                            onClick = { showListQueueBehaviorDialog = true }
                        )
                    )
                }
            ),
            SettingGroup(
                title = context.getString(R.string.settings_playback_persistence),
                items = listOf(
                    SettingItem(
                        RhythmIcons.Repeat,
                        context.getString(R.string.settings_remember_repeat_mode),
                        context.getString(R.string.settings_remember_repeat_mode_desc),
                        toggleState = repeatModePersistence,
                        onToggleChange = { appSettings.setRepeatModePersistence(it) }
                    ),
                    SettingItem(
                        RhythmIcons.Shuffle,
                        context.getString(R.string.settings_remember_shuffle_mode),
                        context.getString(R.string.settings_remember_shuffle_mode_desc),
                        toggleState = shuffleModePersistence,
                        onToggleChange = { appSettings.setShuffleModePersistence(it) }
                    ),
                    SettingItem(
                        RhythmIcons.Queue,
                        context.getString(R.string.settings_remember_queue),
                        context.getString(R.string.settings_remember_queue_desc),
                        toggleState = queuePersistenceEnabled,
                        onToggleChange = { appSettings.setQueuePersistenceEnabled(it) }
                    ),
                    SettingItem(
                        RhythmIcons.Stop,
                        context.getString(R.string.settings_stop_playback_on_close),
                        context.getString(R.string.settings_stop_playback_on_close_desc),
                        toggleState = stopPlaybackOnAppClose,
                        onToggleChange = { appSettings.setStopPlaybackOnAppClose(it) }
                    )
                )
            ),
            SettingGroup(
                title = context.getString(R.string.settings_audio_effects),
                items = listOf(
                    SettingItem(
                        MaterialSymbolIcon("graphic_eq"),
                        context.getString(R.string.settings_gapless_playback),
                        context.getString(R.string.settings_gapless_playback_desc),
                        toggleState = gaplessEnabled,
                        onToggleChange = { appSettings.setGaplessPlayback(it) }
                    ),
                    SettingItem(
                        RhythmIcons.Tune,
                        context.getString(R.string.settings_crossfade),
                        context.getString(R.string.settings_crossfade_desc),
                        toggleState = crossfadeEnabled,
                        onToggleChange = { appSettings.setCrossfade(it) },
                        // Pass the crossfade duration as extra data for rendering
                        data = if (crossfadeEnabled) crossfadeDuration else null
                    ),
                    SettingItem(
                        RhythmIcons.Repeat,
                        context.getString(R.string.settings_crossfade_repeat_one),
                        context.getString(R.string.settings_crossfade_repeat_one_desc),
                        toggleState = crossfadeRepeatOne,
                        onToggleChange = { appSettings.setCrossfadeRepeatOne(it) },
                        enabled = crossfadeEnabled
                    )
                )
            ),
            SettingGroup(
                title = context.getString(R.string.settings_time_display),
                items = listOf(
                    SettingItem(
                        RhythmIcons.AccessTime,
                        context.getString(R.string.settings_use_hours),
                        if (useHoursInTimeFormat) context.getString(R.string.settings_use_hours_enabled) else context.getString(R.string.settings_use_hours_disabled),
                        toggleState = useHoursInTimeFormat,
                        onToggleChange = { appSettings.setUseHoursInTimeFormat(it) }
                    )
                )
            )
        )

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            items(
                items = settingGroups,
                key = { "queueplayback_${it.title}" },
                contentType = { "settingGroup" }
            ) { group ->
                Spacer(modifier = Modifier.height(24.dp))

                val materialItems = group.items.map { item ->
                    Material3SettingsItem(
                        icon = item.icon,
                        title = { Text(item.title) },
                        description = {
                            Column {
                                item.description?.let { desc -> Text(desc) }

                                // Keep crossfade duration slider integrated in this item when enabled.
                                if (item.data is Float && item.toggleState == true) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = context.getString(R.string.settings_crossfade_duration),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = context.getString(R.string.settings_crossfade_duration_desc, crossfadeDuration),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Slider(
                                        value = crossfadeDuration,
                                        onValueChange = { appSettings.setCrossfadeDuration(it) },
                                        valueRange = 0.5f..12f,
                                        steps = 22,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = context.getString(R.string.settings_crossfade_min),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = context.getString(R.string.settings_crossfade_max),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (item.data == "context_queue_persistence") {
                                    val persistenceOptions = listOf(
                                        "EPHEMERAL" to context.getString(R.string.settings_context_persistence_ephemeral),
                                        "PERSISTENT" to context.getString(R.string.settings_context_persistence_persistent)
                                    )
                                    val selectedIndex = persistenceOptions
                                        .indexOfFirst { it.first == contextQueuePersistenceRaw }
                                        .coerceAtLeast(0)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    ExpressiveButtonGroup(
                                        items = persistenceOptions.map { it.second },
                                        selectedIndex = selectedIndex,
                                        onItemClick = { index ->
                                            HapticUtils.performHapticFeedback(
                                                context,
                                                hapticFeedback,
                                                HapticFeedbackType.TextHandleMove
                                            )
                                            appSettings.setContextQueuePersistence(
                                                persistenceOptions[index].first
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        },
                        trailingContent = if (item.toggleState != null) {
                            {
                                TunerAnimatedSwitch(
                                    checked = item.toggleState,
                                    onCheckedChange = {
                                        HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.TextHandleMove)
                                        item.onToggleChange?.invoke(it)
                                    }
                                )
                            }
                        } else if (item.onClick != null) {
                            {
                                Icon(
                                    imageVector = MaterialSymbolIcon("arrow_forward_ios", filled = true),
                                    contentDescription = context.getString(R.string.cd_navigate),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            null
                        },
                        isHighlighted = item.toggleState == true,
                        enabled = item.enabled,
                        onClick = when {
                            item.onClick != null -> {
                                {
                                    HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.LongPress)
                                    item.onClick.invoke()
                                }
                            }

                            item.toggleState != null && item.onToggleChange != null -> {
                                {
                                    HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.TextHandleMove)
                                    item.onToggleChange.invoke(!item.toggleState)
                                }
                            }

                            else -> null
                        }
                    )
                }

                Material3SettingsGroup(
                    title = group.title,
                    items = materialItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            item(key = "queue_playback_bottom_spacer") { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    if (showContextPrefBottomSheet) {
        ContextQueuePreferenceBottomSheet(
            currentPreference = effectiveContextQueuePreference,
            onDismiss = { showContextPrefBottomSheet = false },
            onSelect = { pref ->
                appSettings.setContextQueuePreference(pref)
                showContextPrefBottomSheet = false
            }
        )
    }

    // Playlist Click Behavior Dialog
    if (showPlaylistBehaviorDialog) {
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        val playlistSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // Animation states
        var showContent by remember { mutableStateOf(false) }

        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "contentAlpha"
        )

        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        ModalBottomSheet(
            onDismissRequest = { showPlaylistBehaviorDialog = false },
            sheetState = playlistSheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.playlist_action_title),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = context.getString(R.string.playlist_action_desc),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Option 1: Ask each time
                    Card(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                appSettings.setPlaylistClickBehavior("ask")
                                showPlaylistBehaviorDialog = false
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (playlistClickBehavior == "ask")
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = if (playlistClickBehavior == "ask") {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (playlistClickBehavior == "ask")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = MaterialSymbolIcon("help", filled = true),
                                        contentDescription = null,
                                        tint = if (playlistClickBehavior == "ask")
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = context.getString(R.string.playlist_ask_each_time),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (playlistClickBehavior == "ask")
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = context.getString(R.string.playlist_ask_each_time_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (playlistClickBehavior == "ask")
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (playlistClickBehavior == "ask") {
                                Icon(
                                    imageVector = RhythmIcons.CheckCircle,
                                    contentDescription = context.getString(R.string.ui_selected),
                                    
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Option 2: Load entire playlist
                    Card(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                appSettings.setPlaylistClickBehavior("play_all")
                                showPlaylistBehaviorDialog = false
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (playlistClickBehavior == "play_all")
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = if (playlistClickBehavior == "play_all") {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (playlistClickBehavior == "play_all")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Queue,
                                        contentDescription = null,
                                        tint = if (playlistClickBehavior == "play_all")
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = context.getString(R.string.playlist_action_load_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (playlistClickBehavior == "play_all")
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = context.getString(R.string.playlist_action_load_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (playlistClickBehavior == "play_all")
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (playlistClickBehavior == "play_all") {
                                Icon(
                                    imageVector = RhythmIcons.CheckCircle,
                                    contentDescription = "Selected",
                                    
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Option 3: Play only this song
                    Card(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                appSettings.setPlaylistClickBehavior("play_one")
                                showPlaylistBehaviorDialog = false
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (playlistClickBehavior == "play_one")
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = if (playlistClickBehavior == "play_one") {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (playlistClickBehavior == "play_one")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Play,
                                        contentDescription = null,
                                        tint = if (playlistClickBehavior == "play_one")
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = context.getString(R.string.playlist_action_single_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (playlistClickBehavior == "play_one")
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = context.getString(R.string.playlist_action_single_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (playlistClickBehavior == "play_one")
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (playlistClickBehavior == "play_one") {
                                Icon(
                                    imageVector = RhythmIcons.CheckCircle,
                                    contentDescription = "Selected",
                                    
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // List Queue Behavior Dialog
    if (showListQueueBehaviorDialog) {
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        val listQueueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        var showContent by remember { mutableStateOf(false) }

        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "contentAlpha"
        )

        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        ModalBottomSheet(
            onDismissRequest = { showListQueueBehaviorDialog = false },
            sheetState = listQueueSheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.list_queue_behavior_title),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = context.getString(R.string.list_queue_behavior_desc),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val options = listOf(
                        "replace" to Triple(
                            context.getString(R.string.list_queue_behavior_replace_title),
                            context.getString(R.string.list_queue_behavior_replace_desc),
                            RhythmIcons.Playlist
                        ),
                        "ask" to Triple(
                            context.getString(R.string.list_queue_behavior_ask_title),
                            context.getString(R.string.list_queue_behavior_ask_desc),
                            MaterialSymbolIcon("help", filled = true)
                        ),
                        "play_next" to Triple(
                            context.getString(R.string.list_queue_behavior_play_next_title),
                            context.getString(R.string.list_queue_behavior_play_next_desc),
                            RhythmIcons.Play
                        ),
                        "add_to_end" to Triple(
                            context.getString(R.string.list_queue_behavior_add_end_title),
                            context.getString(R.string.list_queue_behavior_add_end_desc),
                            RhythmIcons.AddToPlaylist
                        )
                    )

                    options.forEach { (value, option) ->
                        val isSelected = listQueueActionBehavior == value

                        Card(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                scope.launch {
                                    appSettings.setListQueueActionBehavior(value)
                                    showListQueueBehaviorDialog = false
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = if (isSelected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = option.third,
                                            contentDescription = null,
                                            tint = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.first,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = option.second,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = RhythmIcons.CheckCircle,
                                        contentDescription = context.getString(R.string.ui_selected),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Show Queue Dialog Setting Dialog
    if (showQueueDialogSettingDialog) {
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // Animation states
        var showContent by remember { mutableStateOf(false) }

        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "contentAlpha"
        )

        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        ModalBottomSheet(
            onDismissRequest = { showQueueDialogSettingDialog = false },
            sheetState = queueSheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.queue_action_title),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = context.getString(R.string.queue_action_choose),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Option 1: Ask each time (show dialog)
                    Card(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                appSettings.setShowQueueDialog(true)
                                showQueueDialogSettingDialog = false
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (showQueueDialog)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = if (showQueueDialog) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (showQueueDialog)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = MaterialSymbolIcon("help", filled = true),
                                        contentDescription = null,
                                        tint = if (showQueueDialog)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = context.getString(R.string.queue_action_ask_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (showQueueDialog)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = context.getString(R.string.queue_action_ask_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (showQueueDialog)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (showQueueDialog) {
                                Icon(
                                    imageVector = RhythmIcons.CheckCircle,
                                    contentDescription = "Selected",
                                    
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Option 2: Always add to queue
                    Card(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                appSettings.setShowQueueDialog(false)
                                showQueueDialogSettingDialog = false
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (!showQueueDialog)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = if (!showQueueDialog) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (!showQueueDialog)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.AddToPlaylist,
                                        contentDescription = null,
                                        tint = if (!showQueueDialog)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = context.getString(R.string.queue_action_always_add_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!showQueueDialog)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = context.getString(R.string.queue_action_always_add_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (!showQueueDialog)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!showQueueDialog) {
                                Icon(
                                    imageVector = RhythmIcons.CheckCircle,
                                    contentDescription = "Selected",
                                    
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun ContextQueuePreferenceBottomSheet(
    currentPreference: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val options = listOf(
        "ARTIST_FIRST" to context.getString(R.string.settings_context_pref_artist_first),
        "GENRE_FIRST" to context.getString(R.string.settings_context_pref_genre_first)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        StandardBottomSheetHeader(
            title = context.getString(R.string.settings_context_queue_preference),
            subtitle = context.getString(R.string.settings_context_queue_preference_desc),
            visible = true
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            options.forEach { (key, label) ->
                val isSelected = currentPreference == key

                Card(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        onSelect(key)
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = RhythmIcons.CheckCircle,
                                contentDescription = context.getString(R.string.ui_selected),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// ✅ FULLY MERGED Playlists Screen (simplified playlist management)
@Composable
fun PlaylistsSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appSettings = AppSettings.getInstance(context)
    val musicViewModel: MusicViewModel = viewModel()
    val playlists by musicViewModel.playlists.collectAsState()
    val defaultPlaylistsEnabled by appSettings.defaultPlaylistsEnabled.collectAsState()

    val defaultPlaylists = playlists.filter { it.isDefault }
    val userPlaylists = playlists.filter { !it.isDefault }
    val emptyPlaylists = playlists.filter { !it.isDefault && it.songs.isEmpty() }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var showBulkExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showOperationProgress by remember { mutableStateOf(false) }
    var operationProgressText by remember { mutableStateOf("") }
    var showCleanupConfirmDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }

    val settingGroups = listOf(
        SettingGroup(
            title = context.getString(R.string.settings_playlists_overview),
            items = listOf() // Empty items - we'll add the stat card separately
        ),
        SettingGroup(
            title = context.getString(R.string.settings_playlists_management),
            items = listOf(
                SettingItem(
                    MaterialSymbolIcon("add_circle"),
                    context.getString(R.string.settings_create_new_playlist),
                    context.getString(R.string.settings_create_new_playlist_desc),
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        showCreatePlaylistDialog = true
                    }
                ),
                SettingItem(
                    MaterialSymbolIcon("upload"),
                    context.getString(R.string.settings_import_playlists),
                    context.getString(R.string.settings_import_playlists_desc),
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        showImportDialog = true
                    }
                ),
                SettingItem(
                    RhythmIcons.Download,
                    context.getString(R.string.settings_export_all_playlists),
                    context.getString(R.string.settings_export_all_playlists_desc),
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        showBulkExportDialog = true
                    }
                )
            ) + if (emptyPlaylists.isNotEmpty()) listOf(
                SettingItem(
                    RhythmIcons.Delete,
                    context.getString(R.string.settings_cleanup_empty_playlists),
                    context.getString(R.string.settings_cleanup_empty_playlists_desc, emptyPlaylists.size, if (emptyPlaylists.size > 1) "s" else ""),
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        showCleanupConfirmDialog = true
                    }
                )
            ) else emptyList()
        ),
        SettingGroup(
            title = context.getString(R.string.settings_default_playlists),
            items = listOf(
                SettingItem(
                    RhythmIcons.Library,
                    context.getString(R.string.settings_enable_default_playlists),
                    context.getString(R.string.settings_enable_default_playlists_desc),
                    onClick = null,
                    toggleState = defaultPlaylistsEnabled,
                    onToggleChange = { enabled ->
                        appSettings.setDefaultPlaylistsEnabled(enabled)
                        // Reload playlists to apply the change
                        if (enabled) {
                            // Add default playlists if they don't exist
                            val currentPlaylists = playlists.toMutableList()
                            if (currentPlaylists.none { it.id == "2" }) {
                                currentPlaylists.add(Playlist("2", "Recently Added"))
                            }
                            if (currentPlaylists.none { it.id == "3" }) {
                                currentPlaylists.add(Playlist("3", "Most Played"))
                            }
                            // Save updated playlists
                            val playlistsJson = GsonUtils.gson.toJson(currentPlaylists)
                            appSettings.setPlaylists(playlistsJson)
                        } else {
                            // Remove default playlists (except Favorites)
                            val filteredPlaylists = playlists.filter { it.id == "1" || !it.isDefault }
                            val playlistsJson = GsonUtils.gson.toJson(filteredPlaylists)
                            appSettings.setPlaylists(playlistsJson)
                        }
                        // Show restart dialog
                        showRestartDialog = true
                    }
                )
            ) + if (defaultPlaylists.isNotEmpty()) {
                defaultPlaylists.map { playlist ->
                    SettingItem(
                        RhythmIcons.MusicNote,
                        playlist.name,
                        "${playlist.songs.size} songs",
                        onClick = null, // No action for default playlists
                        data = playlist.id
                    )
                }
            } else {
                listOf(
                    SettingItem(
                        RhythmIcons.Info,
                        context.getString(R.string.settings_no_default_playlists),
                        context.getString(R.string.settings_no_default_playlists_desc),
                        onClick = null
                    )
                )
            }
        ),
        SettingGroup(
            title = context.getString(R.string.settings_my_playlists),
            items = if (userPlaylists.isNotEmpty()) {
                userPlaylists.map { playlist ->
                    SettingItem(
                        RhythmIcons.Queue,
                        playlist.name,
                        "${playlist.songs.size} songs",
                        onClick = null, // No navigation
                        data = playlist.id // Store playlist ID for deletion
                    )
                }
            } else {
                listOf(
                    SettingItem(
                        RhythmIcons.Add,
                        context.getString(R.string.settings_no_custom_playlists),
                        context.getString(R.string.settings_no_custom_playlists_desc),
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            showCreatePlaylistDialog = true
                        }
                    )
                )
            }
        )
    )

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_playlists),
        showBackButton = true,
        onBackClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onBackClick()
        }
    ) { modifier ->
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
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Collection Statistics Card
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = context.getString(R.string.settings_playlists_overview),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "${playlists.size}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = context.getString(R.string.settings_total),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "${userPlaylists.size}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = context.getString(R.string.settings_custom),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "${defaultPlaylists.size}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = context.getString(R.string.settings_default),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(settingGroups.filter { it.title != context.getString(R.string.settings_playlists_overview) }) { group ->
                Spacer(modifier = Modifier.height(24.dp))

                val isDefaultPlaylistGroup =
                    group.title == context.getString(R.string.settings_default_playlists)
                val isPlaylistRowsGroup =
                    isDefaultPlaylistGroup ||
                        group.title == context.getString(R.string.settings_my_playlists)

                val materialItems = if (isPlaylistRowsGroup) {
                    group.items.map { item ->
                        val playlistId = item.data as? String
                        val playlist = playlists.find { it.id == playlistId }

                        if (item.onClick != null || item.toggleState != null || playlistId == null) {
                            toMaterial3SettingsItem(
                                context = context,
                                item = item,
                                hapticFeedback = haptic
                            )
                        } else {
                            Material3SettingsItem(
                                icon = item.icon,
                                title = { Text(item.title) },
                                description = item.description?.let { desc -> { Text(desc) } },
                                trailingContent = if (!isDefaultPlaylistGroup && playlist != null) {
                                    {
                                        IconButton(
                                            onClick = {
                                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                                playlistToDelete = playlist
                                            }
                                        ) {
                                            Icon(
                                                imageVector = RhythmIcons.Delete,
                                                contentDescription = context.getString(R.string.dialog_delete),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                } else {
                                    null
                                },
                                onClick = item.onClick
                            )
                        }
                    }
                } else {
                    group.items.map { item ->
                        toMaterial3SettingsItem(
                            context = context,
                            item = item,
                            hapticFeedback = haptic
                        )
                    }
                }

                Material3SettingsGroup(
                    title = group.title,
                    items = materialItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }
        }
    }

    // Dialogs
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                musicViewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }

    if (showBulkExportDialog) {
        BulkPlaylistExportDialog(
            playlistCount = playlists.size,
            onDismiss = { showBulkExportDialog = false },
            onExport = { format, includeDefault ->
                showBulkExportDialog = false
                showOperationProgress = true
                operationProgressText = context.getString(R.string.operation_exporting_playlists)
                musicViewModel.exportAllPlaylists(format, includeDefault) { result ->
                    showOperationProgress = false
                }
            },
            onExportToCustomLocation = { format, includeDefault, directoryUri ->
                showBulkExportDialog = false
                showOperationProgress = true
                operationProgressText = context.getString(R.string.operation_exporting_playlists_location)
                musicViewModel.exportAllPlaylists(format, includeDefault, directoryUri) { result ->
                    showOperationProgress = false
                }
            }
        )
    }

    if (showImportDialog) {
        PlaylistImportDialog(
            onDismiss = { showImportDialog = false },
            onImport = { uri, onResult, _ ->
                showImportDialog = false
                showOperationProgress = true
                operationProgressText = context.getString(R.string.operation_importing_playlist)
                musicViewModel.importPlaylist(uri, onResult) {
                    // When import completes successfully, show restart dialog
                    showOperationProgress = false
                    showRestartDialog = true
                }
            }
        )
    }

    if (showOperationProgress) {
        PlaylistOperationProgressDialog(
            operation = operationProgressText,
            onDismiss = { /* Cannot dismiss during operation */ }
        )
    }

    if (showCleanupConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCleanupConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = RhythmIcons.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = context.getString(R.string.dialog_cleanup_empty_playlists_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(context.getString(R.string.dialog_cleanup_empty_playlists_message, emptyPlaylists.size, if (emptyPlaylists.size > 1) "s" else ""))
            },
            confirmButton = {
                Button(
                    onClick = {
                        emptyPlaylists.forEach { playlist ->
                            musicViewModel.deletePlaylist(playlist.id)
                        }
                        showCleanupConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = RhythmIcons.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.dialog_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCleanupConfirmDialog = false }) {
                    Icon(
                        imageVector = RhythmIcons.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.dialog_cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // App Restart Dialog for default playlists toggle
    if (showRestartDialog) {
        AppRestartDialog(
            onDismiss = { showRestartDialog = false },
            onRestart = {
                showRestartDialog = false
                chromahub.rhythm.app.util.AppRestarter.restartApp(context)
            },
            onContinue = {
                showRestartDialog = false
                // Continue without restart
            }
        )
    }

    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            icon = {
                Icon(
                    imageVector = RhythmIcons.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = context.getString(R.string.dialog_delete_playlist_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(context.getString(R.string.dialog_delete_playlist_message, playlist.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        musicViewModel.deletePlaylist(playlist.id)
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = RhythmIcons.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.dialog_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { playlistToDelete = null }) {
                    Icon(
                        imageVector = RhythmIcons.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.dialog_cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ✅ REDESIGNED Media Scan Screen with improved UI
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScanSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appSettings = AppSettings.getInstance(context)
    val musicViewModel: MusicViewModel = viewModel()

    // Get all songs and filtered items
    val allSongs by musicViewModel.songs.collectAsState()
    val filteredSongs by musicViewModel.filteredSongs.collectAsState()
    val blacklistedSongs by appSettings.blacklistedSongs.collectAsState()
    val blacklistedFolders by appSettings.blacklistedFolders.collectAsState()
    val whitelistedSongs by appSettings.whitelistedSongs.collectAsState()
    val whitelistedFolders by appSettings.whitelistedFolders.collectAsState()

    // Get current media scan mode from settings
    val mediaScanMode by appSettings.mediaScanMode.collectAsState()
    val includeHiddenWhitelistedMedia by appSettings.includeHiddenWhitelistedMedia.collectAsState()

    // Mode state
    var currentMode by remember {
        mutableStateOf(
            if (mediaScanMode == "whitelist") chromahub.rhythm.app.shared.presentation.components.MediaScanMode.WHITELIST
            else chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST
        )
    }

    // Bottom sheet states
    var showSongsBottomSheet by remember { mutableStateOf(false) }
    var showFoldersBottomSheet by remember { mutableStateOf(false) }

    // File picker launcher for folder selection
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val docId = DocumentsContract.getTreeDocumentId(uri)
                    val split = docId.split(":")

                    if (split.size >= 2) {
                        val storageType = split[0] // e.g., "primary", "home", or specific SD card ID
                        val relativePath = split[1] // e.g., "Music/MyFolder"

                        // Build the full path based on storage type
                        val fullPath = when (storageType) {
                            "primary" -> "/storage/emulated/0/$relativePath"
                            "home" -> "/storage/emulated/0/$relativePath"
                            else -> {
                                // For SD cards or other storage, try to construct path
                                // This is a best-effort approach
                                if (storageType.contains("-")) {
                                    // SD card UUID format
                                    "/storage/$storageType/$relativePath"
                                } else {
                                    // Fallback to emulated storage
                                    "/storage/emulated/0/$relativePath"
                                }
                            }
                        }

                        if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) {
                            appSettings.addFolderToBlacklist(fullPath)
                        } else {
                            appSettings.addFolderToWhitelist(fullPath)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MediaScanSettingsScreen", "Error parsing folder path", e)
                }
            }
        }
    }

    // Computed values OUTSIDE LazyColumn
    val filteredSongDetails = remember(allSongs, blacklistedSongs, whitelistedSongs, currentMode) {
        when (currentMode) {
            chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST ->
                allSongs.filter { song -> blacklistedSongs.contains(song.id) }
            chromahub.rhythm.app.shared.presentation.components.MediaScanMode.WHITELIST ->
                allSongs.filter { song -> whitelistedSongs.contains(song.id) }
        }
    }

    val filteredFoldersList = remember(blacklistedFolders, whitelistedFolders, currentMode) {
        when (currentMode) {
            chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST -> blacklistedFolders
            chromahub.rhythm.app.shared.presentation.components.MediaScanMode.WHITELIST -> whitelistedFolders
        }
    }

    val settingGroups = listOf(
        SettingGroup(
            title = context.getString(R.string.settings_mode_selection),
            items = listOf(
                SettingItem(
                    RhythmIcons.Block,
                    context.getString(R.string.settings_blacklist_mode),
                    context.getString(R.string.settings_blacklist_mode_desc),
                    toggleState = currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST,
                    onToggleChange = { enabled ->
                        if (enabled) {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            currentMode = chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST
                            appSettings.setMediaScanMode("blacklist")
                        }
                    }
                ),
                SettingItem(
                    RhythmIcons.CheckCircle,
                    context.getString(R.string.settings_whitelist_mode),
                    context.getString(R.string.settings_whitelist_mode_desc),
                    toggleState = currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.WHITELIST,
                    onToggleChange = { enabled ->
                        if (enabled) {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            currentMode = chromahub.rhythm.app.shared.presentation.components.MediaScanMode.WHITELIST
                            appSettings.setMediaScanMode("whitelist")
                        }
                    }
                )
            )
        ),
        SettingGroup(
            title = context.getString(R.string.settings_song_management),
            items = listOf(
                SettingItem(
                    RhythmIcons.Queue,
                    context.getString(R.string.settings_manage_songs),
                    context.getString(R.string.settings_manage_songs_desc, filteredSongDetails.size, if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) context.getString(R.string.settings_blocked) else context.getString(R.string.settings_whitelisted)),
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        showSongsBottomSheet = true
                    }
                ),
                SettingItem(
                    MaterialSymbolIcon("clear"),
                    context.getString(R.string.settings_clear_all_songs),
                    context.getString(R.string.settings_clear_all_songs_desc, if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) context.getString(R.string.settings_blocked) else context.getString(R.string.settings_whitelisted)),
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) {
                            appSettings.clearBlacklist()
                        } else {
                            appSettings.clearWhitelist()
                        }
                    }
                )
            )
        ),
        SettingGroup(
            title = context.getString(R.string.settings_folder_management),
            items = listOf(
                SettingItem(
                    RhythmIcons.Folder,
                    context.getString(R.string.settings_manage_folders),
                    context.getString(R.string.settings_manage_folders_desc, filteredFoldersList.size, if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) context.getString(R.string.settings_blocked) else context.getString(R.string.settings_whitelisted)),
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        showFoldersBottomSheet = true
                    }
                ),
                SettingItem(
                    RhythmIcons.Add,
                    context.getString(R.string.settings_add_folder),
                    context.getString(R.string.settings_add_folder_desc, if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) context.getString(R.string.settings_block) else context.getString(R.string.settings_whitelist)),
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        folderPickerLauncher.launch(intent)
                    }
                ),
                SettingItem(
                    MaterialSymbolIcon("clear"),
                    context.getString(R.string.settings_clear_all_folders),
                    context.getString(R.string.settings_clear_all_folders_desc, if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) context.getString(R.string.settings_blocked) else context.getString(R.string.settings_whitelisted)),
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) {
                            blacklistedFolders.forEach { folder ->
                                appSettings.removeFolderFromBlacklist(folder)
                            }
                        } else {
                            whitelistedFolders.forEach { folder ->
                                appSettings.removeFolderFromWhitelist(folder)
                            }
                        }
                    }
                )
            )
        )
    )

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_media_scan),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
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
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main overview content
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            // Mode Selection with ExpressiveButtonGroup
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = context.getString(R.string.settings_mode_selection),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            ExpressiveButtonGroup(
                                items = listOf(
                                    context.getString(R.string.settings_blacklist_mode),
                                    context.getString(R.string.settings_whitelist_mode)
                                ),
                                selectedIndex = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) 0 else 1,
                                onItemClick = { index ->
                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                    currentMode = if (index == 0) {
                                        appSettings.setMediaScanMode("blacklist")
                                        chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST
                                    } else {
                                        appSettings.setMediaScanMode("whitelist")
                                        chromahub.rhythm.app.shared.presentation.components.MediaScanMode.WHITELIST
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST)
                                    context.getString(R.string.settings_blacklist_mode_desc)
                                else
                                    context.getString(R.string.settings_whitelist_mode_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                val scanBehaviorItems = listOf(
                    toMaterial3SettingsItem(
                        context = context,
                        hapticFeedback = haptic,
                        item = SettingItem(
                            icon = RhythmIcons.Visibility,
                            title = context.getString(R.string.settings_include_hidden_whitelisted_media),
                            description = context.getString(R.string.settings_include_hidden_whitelisted_media_desc),
                            toggleState = includeHiddenWhitelistedMedia,
                            onToggleChange = { appSettings.setIncludeHiddenWhitelistedMedia(it) }
                        )
                    )
                )

                Material3SettingsGroup(
                    title = context.getString(R.string.settings_scan_behavior),
                    items = scanBehaviorItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            settingGroups.drop(1).forEach { group ->
                item {
                    val materialItems = group.items.map { item ->
                        toMaterial3SettingsItem(
                            context = context,
                            item = item,
                            hapticFeedback = haptic
                        )
                    }

                    Material3SettingsGroup(
                        title = group.title,
                        items = materialItems,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                }
            }

            // Quick Tips Card
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MaterialSymbolIcon("lightbulb", filled = true),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.settings_quick_tips),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        MediaScanTipItem(
                            icon = RhythmIcons.Block,
                            text = context.getString(R.string.settings_quick_tip_blacklist)
                        )
                        MediaScanTipItem(
                            icon = RhythmIcons.CheckCircle,
                            text = context.getString(R.string.settings_quick_tip_whitelist)
                        )
                        MediaScanTipItem(
                            icon = RhythmIcons.Folder,
                            text = context.getString(R.string.settings_quick_tip_folder)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Songs bottom sheet
    if (showSongsBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // Animation states
        var showContent by remember { mutableStateOf(false) }
        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "contentAlpha"
        )

        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        ModalBottomSheet(
            onDismissRequest = { showSongsBottomSheet = false },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.settings_manage_songs),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) context.getString(R.string.settings_blocked_songs) else context.getString(R.string.settings_whitelisted_songs),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) RhythmIcons.Block else RhythmIcons.CheckCircle,
                                contentDescription = null,
                                tint = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST)
                                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${filteredSongDetails.size}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) context.getString(R.string.settings_blocked) else context.getString(R.string.settings_whitelisted),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = RhythmIcons.MusicNote,
                                contentDescription = null,
                                
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${allSongs.size}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = context.getString(R.string.settings_total_songs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Songs list with lazy column
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredSongDetails,
                        key = { "filtered_${it.id}" },
                        contentType = { "song" }
                    ) { song ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST)
                                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.MusicNote,
                                        contentDescription = null,
                                        tint = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST)
                                            MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                FilledIconButton(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                        if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) {
                                            appSettings.removeFromBlacklist(song.id)
                                        } else {
                                            appSettings.removeFromWhitelist(song.id)
                                        }
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST)
                                            MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Close,
                                        contentDescription = "Remove",
                                        tint = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST)
                                            MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Clear button at bottom
                if (filteredSongDetails.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                            if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) {
                                appSettings.clearBlacklist()
                            } else {
                                appSettings.clearWhitelist()
                            }
                            showSongsBottomSheet = false
                        },
                        border = BorderStroke(2.dp, if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST)
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = MaterialSymbolIcon("delete_sweep", filled = true),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(context.getString(R.string.settings_clear_all_button, if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) context.getString(R.string.settings_blocked) else context.getString(R.string.settings_whitelisted)))
                    }
                }
            }
        }
    }

    // Folders bottom sheet
    if (showFoldersBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // Animation states
        var showContent by remember { mutableStateOf(false) }
        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "contentAlpha"
        )

        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        ModalBottomSheet(
            onDismissRequest = { showFoldersBottomSheet = false },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.settings_manage_folders),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) context.getString(R.string.settings_blocked_folders) else context.getString(R.string.settings_whitelisted_folders),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) MaterialSymbolIcon("folder_off") else RhythmIcons.Folder,
                            contentDescription = null,
                            tint = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST)
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${filteredFoldersList.size}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) context.getString(R.string.settings_blocked_folders) else context.getString(R.string.settings_whitelisted_folders),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Folders list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFoldersList, key = { "folder_${it.hashCode()}" }) { folder ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST)
                                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Folder,
                                        contentDescription = null,
                                        tint = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST)
                                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = File(folder).name.ifEmpty { context.getString(R.string.settings_root) },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = folder,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                FilledIconButton(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                        if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) {
                                            appSettings.removeFolderFromBlacklist(folder)
                                        } else {
                                            appSettings.removeFolderFromWhitelist(folder)
                                        }
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST)
                                            MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Close,
                                        contentDescription = context.getString(R.string.cd_remove),
                                        tint = if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST)
                                            MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Action buttons at bottom
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            folderPickerLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(context.getString(R.string.settings_add_folder_button))
                    }

                    if (filteredFoldersList.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST) {
                                    blacklistedFolders.forEach { folder ->
                                        appSettings.removeFolderFromBlacklist(folder)
                                    }
                                } else {
                                    whitelistedFolders.forEach { folder ->
                                        appSettings.removeFolderFromWhitelist(folder)
                                    }
                                }
                                showFoldersBottomSheet = false
                            },
                            border = BorderStroke(2.dp, if (currentMode == chromahub.rhythm.app.shared.presentation.components.MediaScanMode.BLACKLIST)
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = MaterialSymbolIcon("delete_sweep", filled = true),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(context.getString(R.string.settings_clear_all_button_short))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistSeparatorsSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appSettings = AppSettings.getInstance(context)
    val scope = rememberCoroutineScope()

    val artistSeparatorEnabled by appSettings.artistSeparatorEnabled.collectAsState()
    val artistSeparatorDelimiters by appSettings.artistSeparatorDelimiters.collectAsState()
    val groupByAlbumArtist by appSettings.groupByAlbumArtist.collectAsState()

    var showDelimiterBottomSheet by remember { mutableStateOf(false) }
    var tempDelimiters by remember { mutableStateOf(artistSeparatorDelimiters) }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.artists_title),
        showBackButton = true,
        onBackClick = {
            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
            onBackClick()
        }
    ) { modifier ->
        val settingGroups = listOf(
            SettingGroup(
                title = context.getString(R.string.artist_multi_parsing),
                items = listOf(
                    SettingItem(
                        RhythmIcons.Artist,
                        context.getString(R.string.artist_enable_separation),
                        context.getString(R.string.artist_enable_separation_desc),
                        toggleState = artistSeparatorEnabled,
                        onToggleChange = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            appSettings.setArtistSeparatorEnabled(it)
                        }
                    ),
                    SettingItem(
                        RhythmIcons.Settings,
                        context.getString(R.string.artist_configure_delimiters),
                        context.getString(R.string.artist_current_delimiters, artistSeparatorDelimiters.toCharArray().joinToString(", ")),
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            tempDelimiters = artistSeparatorDelimiters
                            showDelimiterBottomSheet = true
                        }
                    )
                ),
            ),
            SettingGroup(
                title = context.getString(R.string.artist_library_organization),
                items = listOf(
                    SettingItem(
                        RhythmIcons.Album,
                        context.getString(R.string.artist_group_by_album_artist),
                        context.getString(R.string.artist_group_by_album_artist_desc),
                        toggleState = groupByAlbumArtist,
                        onToggleChange = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            appSettings.setGroupByAlbumArtist(it)
                        }
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

                val materialItems = if (group.title == "Multi-Artist Parsing") {
                    buildList {
                        if (group.items.isNotEmpty()) {
                            add(toMaterial3SettingsItem(context = context, item = group.items[0], hapticFeedback = haptic))
                        }
                        if (artistSeparatorEnabled && group.items.size > 1) {
                            add(toMaterial3SettingsItem(context = context, item = group.items[1], hapticFeedback = haptic))
                        }
                    }
                } else {
                    group.items.map { item ->
                        toMaterial3SettingsItem(context = context, item = item, hapticFeedback = haptic)
                    }
                }

                Material3SettingsGroup(
                    title = group.title,
                    items = materialItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // Info Card
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = RhythmIcons.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.settings_about_multi_artist),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = context.getString(R.string.settings_multi_artist_parsing_info),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Examples
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MaterialSymbolIcon("lightbulb"),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.settings_examples),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        ArtistSeparatorExampleItem(
                            original = "Artist1/Artist2",
                            result = "Artist1, Artist2"
                        )
                        ArtistSeparatorExampleItem(
                            original = "Artist1; Artist2",
                            result = "Artist1, Artist2"
                        )
                        ArtistSeparatorExampleItem(
                            original = "Artist1 & Artist2",
                            result = "Artist1, Artist2"
                        )
                        ArtistSeparatorExampleItem(
                            original = "Artist1\\\\/Artist2",
                            result = "Artist1/Artist2 (escaped)"
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Delimiter Configuration Bottom Sheet
    if (showDelimiterBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // Animation states
        var showContent by remember { mutableStateOf(false) }
        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "contentAlpha"
        )

        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        ModalBottomSheet(
            onDismissRequest = { showDelimiterBottomSheet = false },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding()
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.settings_configure_delimiters),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = context.getString(R.string.settings_select_artist_separators),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val commonDelimiters = listOf(
                    '/' to context.getString(R.string.delimiter_slash),
                    ';' to context.getString(R.string.delimiter_semicolon),
                    ',' to context.getString(R.string.delimiter_comma),
                    '+' to context.getString(R.string.delimiter_plus),
                    '&' to context.getString(R.string.delimiter_ampersand)
                )

                // Delimiter options in a responsive two-column layout
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    commonDelimiters.chunked(2).forEach { delimiterRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            delimiterRow.forEach { (char, name) ->
                                val isSelected = tempDelimiters.contains(char)

                                // Master animation states
                                var isPressed by remember { mutableStateOf(false) }
                                val scale by animateFloatAsState(
                                    targetValue = if (isPressed) 0.96f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "delimiter_scale"
                                )

                                val containerColor by animateColorAsState(
                                    targetValue = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "delimiter_container_color"
                                )

                                Card(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                        isPressed = true
                                        tempDelimiters = if (tempDelimiters.contains(char)) {
                                            tempDelimiters.replace(char.toString(), "")
                                        } else {
                                            tempDelimiters + char
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(130.dp)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = containerColor
                                    ),
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // Delimiter Preview
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(
                                                    color = if (isSelected)
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = char.toString(),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        // Optional description could go here if needed
                                    }
                                }

                                // Reset press state
                                LaunchedEffect(isPressed) {
                                    if (isPressed) {
                                        delay(150)
                                        isPressed = false
                                    }
                                }
                            }

                            if (delimiterRow.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons Row - Consistent with other bottom sheets
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, delayMillis = 600)) +
                            slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                initialOffsetY = { 40 }
                            )
                ) {
                    var savePressed by remember { mutableStateOf(false) }
                    var resetPressed by remember { mutableStateOf(false) }

                    val saveScale by animateFloatAsState(
                        targetValue = if (savePressed) 0.96f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "saveScale"
                    )

                    val resetScale by animateFloatAsState(
                        targetValue = if (resetPressed) 0.96f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "resetScale"
                    )

                    LaunchedEffect(savePressed) {
                        if (savePressed) {
                            delay(150)
                            savePressed = false
                        }
                    }

                    LaunchedEffect(resetPressed) {
                        if (resetPressed) {
                            delay(150)
                            resetPressed = false
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Reset Button - Secondary action
                        OutlinedButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                resetPressed = true
                                tempDelimiters = artistSeparatorDelimiters // Reset to original
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .graphicsLayer {
                                    scaleX = resetScale
                                    scaleY = resetScale
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                width = 1.5.dp
                            )
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Save Changes Button - Primary action
                        Button(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                savePressed = true
                                scope.launch {
                                    appSettings.setArtistSeparatorDelimiters(tempDelimiters)
                                    showDelimiterBottomSheet = false
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .graphicsLayer {
                                    scaleX = saveScale
                                    scaleY = saveScale
                                },
                            shape = RoundedCornerShape(16.dp),
                            enabled = tempDelimiters.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistSeparatorExampleItem(original: String, result: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = RhythmIcons.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "\"$original\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 24.dp, top = 2.dp)
        ) {
            Icon(
                imageVector = RhythmIcons.Forward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = result,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ApiServiceRow(
    title: String,
    description: String,
    status: String,
    isConfigured: Boolean,
    icon: ImageVector,
    isEnabled: Boolean = true,
    showToggle: Boolean = false,
    onToggle: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !isEnabled -> MaterialTheme.colorScheme.surfaceVariant
                        isConfigured -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    isConfigured -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onErrorContainer
                },
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = when {
                        !isEnabled -> MaterialTheme.colorScheme.surfaceVariant
                        isConfigured -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (!isEnabled) context.getString(R.string.status_disabled) else status,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                            isConfigured -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        // Toggle or Arrow icon
        if (showToggle && onToggle != null) {
            TunerAnimatedSwitch(
                checked = isEnabled,
                onCheckedChange = { enabled ->
                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                    onToggle(enabled)
                }
            )
        }
    }
}

// SpotifyApiConfigDialog removed - Canvas API has been removed



@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onNavigateToUpdates: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val appUpdaterViewModel: AppUpdaterViewModel = viewModel()
    var showLicensesSheet by remember { mutableStateOf(false) }

    val openUrl: (String) -> Unit = { url ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_about_title),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
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
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                val appSettings = remember { AppSettings.getInstance(context) }
                val expressiveShapesEnabled by appSettings.expressiveShapesEnabled.collectAsState()
                val expressiveShapeA by appSettings.expressiveShapeSongArt.collectAsState()
                val expressiveShapeB by appSettings.expressiveShapePlayerArt.collectAsState()
                val expressiveShapeC by appSettings.expressiveShapeAlbumArt.collectAsState()
                val expressiveShapeD by appSettings.expressiveShapePlaylistArt.collectAsState()
                val expressiveShapeE by appSettings.expressiveShapeArtistArt.collectAsState()
                val expressiveShapeF by appSettings.expressiveShapePlayerControls.collectAsState()
                val expressiveShapeG by appSettings.expressiveShapeMiniPlayer.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                        if (expressiveShapesEnabled) {
                            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                            val screenWidthDp = configuration.screenWidthDp
                            val screenHeightDp = configuration.screenHeightDp
                            val expressivePreset by appSettings.expressiveShapePreset.collectAsState()
                            val seed = System.nanoTime().toInt()
                            val primaryBackdropColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            val secondaryBackdropColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.82f)
                            val tertiaryBackdropColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                            val neutralBackdropColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

                            val aboutBackdropShapes = remember(
                                seed,
                                expressiveShapeA,
                                expressiveShapeB,
                                expressiveShapeC,
                                expressiveShapeD,
                                expressiveShapeE,
                                expressiveShapeF,
                                expressiveShapeG,
                                expressivePreset
                            ) {
                                buildSplashBackdropShapes(
                                    seed = seed,
                                    shapeIds = listOf(
                                        expressiveShapeA,
                                        expressiveShapeB,
                                        expressiveShapeC,
                                        expressiveShapeD,
                                        expressiveShapeE,
                                        expressiveShapeF,
                                        expressiveShapeG
                                    ),
                                    preset = expressivePreset,
                                    screenWidthDp = screenWidthDp,
                                    screenHeightDp = screenHeightDp,
                                    primaryColor = primaryBackdropColor,
                                    secondaryColor = secondaryBackdropColor,
                                    tertiaryColor = tertiaryBackdropColor,
                                    neutralColor = neutralBackdropColor
                                )
                            }

                            SplashBackgroundOrbs(shapes = aboutBackdropShapes)
                        }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = chromahub.rhythm.app.R.drawable.rhythm_splash_logo),
                                contentDescription = context.getString(R.string.updates_rhythm_logo_cd),
                                modifier = Modifier.size(82.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = context.getString(R.string.app_name),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Text(
                            text = context.getString(R.string.settings_about_music_player),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Your Music, Your Rhythm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                val appInfo = context.applicationInfo
                val buildVariant = if (BuildConfig.FLAVOR.isNotBlank()) {
                    "${BuildConfig.BUILD_TYPE} (${BuildConfig.FLAVOR})"
                } else {
                    BuildConfig.BUILD_TYPE
                }
                val detectedAbis = Build.SUPPORTED_ABIS
                    .take(2)
                    .joinToString(separator = ", ")
                    .ifBlank { context.getString(R.string.settings_about_architecture_value) }

                val detailsItems = listOf(
                    Material3SettingsItem(
                        icon = RhythmIcons.Info,
                        title = { Text(context.getString(R.string.settings_about_version_label)) },
                        description = { Text(BuildConfig.VERSION_NAME) }
                    ),
                    Material3SettingsItem(
                        icon = MaterialSymbolIcon("build"),
                        title = { Text(context.getString(R.string.settings_about_build)) },
                        description = { Text("${BuildConfig.VERSION_CODE} • $buildVariant") }
                    ),
                    Material3SettingsItem(
                        icon = MaterialSymbolIcon("developer_mode"),
                        title = { Text(context.getString(R.string.settings_about_target_sdk)) },
                        description = { Text(appInfo.targetSdkVersion.toString()) }
                    ),
                    Material3SettingsItem(
                        icon = MaterialSymbolIcon("memory"),
                        title = { Text(context.getString(R.string.settings_about_architecture)) },
                        description = { Text(detectedAbis) }
                    )
                )

                Material3SettingsGroup(
                    title = context.getString(R.string.settings_about_project_details),
                    items = detailsItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = context.getString(R.string.settings_about_credits),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        CommunityMember(
                            name = "Anjishnu Nandi",
                            role = "Lead Developer & Project Architect",
                            githubUsername = "cromaguy",
                            avatarUrl = "https://github.com/cromaguy.png",
                            supportUrl = "https://ko-fi.com/anjishnunandi",
                            context = context
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                openUrl("https://ko-fi.com/anjishnunandi")
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = MaterialSymbolIcon("local_cafe", filled = true),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Support Development")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = context.getString(R.string.settings_about_team_chromahub),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = context.getString(R.string.settings_about_team_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = context.getString(R.string.settings_about_community),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = context.getString(R.string.settings_about_community_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CommunityMember(
                                name = "Izzy",
                                role = "Manages updates on IzzyOnDroid",
                                githubUsername = "IzzySoft",
                                avatarUrl = "https://github.com/IzzySoft.png",
                                context = context
                            )
                            CommunityMember(
                                name = "linsui",
                                role = "Manages updates on F-Droid",
                                githubUsername = "linsui",
                                avatarUrl = "https://github.com/linsui.png",
                                context = context
                            )
                            CommunityMember(
                                name = "Licaon_Kter",
                                role = "Manages updates on F-Droid",
                                githubUsername = "licaon-kter",
                                avatarUrl = "https://github.com/licaon-kter.png",
                                context = context
                            )
                            CommunityMember(
                                name = "Christian",
                                role = "Collab & Project Booming's Lead Dev",
                                githubUsername = "mardous",
                                avatarUrl = "https://github.com/mardous.png",
                                context = context
                            )
                            CommunityMember(
                                name = "theovilardo",
                                role = "Collab & Project PixelPlayer's Lead Dev",
                                githubUsername = "theovilardo",
                                avatarUrl = "https://github.com/theovilardo.png",
                                context = context
                            )
                            CommunityMember(
                                name = "itzKane",
                                role = "UI Concept Designer",
                                githubUsername = "soykane",
                                avatarUrl = "https://github.com/soykane.png",
                                context = context
                            )
                            CommunityMember(
                                name = "firefly-sylestia",
                                role = "Beta Tester & QA",
                                githubUsername = "firefly-sylestia",
                                avatarUrl = "https://github.com/firefly-sylestia.png",
                                context = context
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sponsors",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Thanks to everyone supporting Rhythm's development.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CommunityMember(
                                name = "HyXeny",
                                role = "Sponsor",
                                githubUsername = "HyXeny",
                                avatarUrl = "https://github.com/HyXeny.png",
                                context = context
                            )
                            CommunityMember(
                                name = "Xiaomiraphealin",
                                role = "Sponsor",
                                githubUsername = "Xiaomiraphealin",
                                avatarUrl = "https://github.com/Xiaomiraphealin.png",
                                context = context
                            )
                        }
                    }
                }
            }

            item {
                val actionItems = listOf(
                    toMaterial3SettingsItem(
                        context = context,
                        hapticFeedback = haptics,
                        item = SettingItem(
                            icon = RhythmIcons.Download,
                            title = context.getString(R.string.settings_about_check_updates),
                            description = context.getString(R.string.updates_check_again),
                            onClick = {
                                appUpdaterViewModel.checkForUpdates(force = true)
                                onNavigateToUpdates?.invoke()
                            }
                        )
                    ),
                    toMaterial3SettingsItem(
                        context = context,
                        hapticFeedback = haptics,
                        item = SettingItem(
                            icon = RhythmIcons.Language,
                            title = context.getString(R.string.settings_about_visit_website),
                            description = "rhythmweb.vercel.app",
                            onClick = { openUrl("https://rhythmweb.vercel.app/") }
                        )
                    ),
                    toMaterial3SettingsItem(
                        context = context,
                        hapticFeedback = haptics,
                        item = SettingItem(
                            icon = RhythmIcons.Code,
                            title = context.getString(R.string.settings_about_view_github),
                            description = "github.com/cromaguy/Rhythm",
                            onClick = { openUrl("https://github.com/cromaguy/Rhythm") }
                        )
                    ),
                    toMaterial3SettingsItem(
                        context = context,
                        hapticFeedback = haptics,
                        item = SettingItem(
                            icon = RhythmIcons.BugReport,
                            title = context.getString(R.string.settings_about_report_bug),
                            description = "github.com/cromaguy/Rhythm/issues",
                            onClick = { openUrl("https://github.com/cromaguy/Rhythm/issues") }
                        )
                    ),
                    toMaterial3SettingsItem(
                        context = context,
                        hapticFeedback = haptics,
                        item = SettingItem(
                            icon = RhythmIcons.Settings,
                            title = context.getString(R.string.settings_about_open_source_libs),
                            description = context.getString(R.string.settings_about_view_dependencies),
                            onClick = { showLicensesSheet = true }
                        )
                    ),
                    toMaterial3SettingsItem(
                        context = context,
                        hapticFeedback = haptics,
                        item = SettingItem(
                            icon = MaterialSymbolIcon("chat", filled = true),
                            title = "Discord Community",
                            description = "discord.gg/XjPyUYPQYc",
                            onClick = { openUrl("https://discord.gg/XjPyUYPQYc") }
                        )
                    ),
                    toMaterial3SettingsItem(
                        context = context,
                        hapticFeedback = haptics,
                        item = SettingItem(
                            icon = MaterialSymbolIcon("send", filled = true),
                            title = "Telegram Support",
                            description = "t.me/RhythmSupport",
                            onClick = { openUrl("https://t.me/RhythmSupport") }
                        )
                    )
                )

                Material3SettingsGroup(
                    title = context.getString(R.string.settings_about_actions),
                    items = actionItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        if (showLicensesSheet) {
            LicensesBottomSheet(
                onDismiss = { showLicensesSheet = false }
            )
        }
    }
}

@Composable
private fun CommunityMember(
    name: String,
    role: String,
    githubUsername: String,
    avatarUrl: String,
    supportUrl: String? = null,
    context: android.content.Context
) {
    val haptics = LocalHapticFeedback.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$githubUsername"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        // Avatar with fallback
        val fallbackPainter = painterResource(id = chromahub.rhythm.app.R.drawable.ic_music_note)

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = "$name's avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            error = fallbackPainter,
            placeholder = fallbackPainter
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = role,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = RhythmIcons.Link,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "@$githubUsername",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                if (supportUrl != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = MaterialSymbolIcon("local_cafe", filled = true),
                        contentDescription = "Support",
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF13C3FF)
                    )
                }
            }
        }

        Icon(
            imageVector = RhythmIcons.ArtistFilled,
            contentDescription = "View GitHub Profile",
            
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.5f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TechStackItem(
    technology: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = "•",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = technology,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreditItem(
    name: String,
    role: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = role,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun UpdatesSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val updaterViewModel: AppUpdaterViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Collect state from ViewModel and AppSettings
    val updatesEnabled by appSettings.updatesEnabled.collectAsState()
    val autoCheckForUpdates by appSettings.autoCheckForUpdates.collectAsState()
    val useSmartUpdatePolling by appSettings.useSmartUpdatePolling.collectAsState()
    val updateChannel by appSettings.updateChannel.collectAsState()
    val updateSource by appSettings.updateSource.collectAsState()
    val updateCheckIntervalHours by appSettings.updateCheckIntervalHours.collectAsState()
    val currentVersion by updaterViewModel.currentVersion.collectAsState()
    val latestVersion by updaterViewModel.latestVersion.collectAsState()
    val isCheckingForUpdates by updaterViewModel.isCheckingForUpdates.collectAsState()
    val updateAvailable by updaterViewModel.updateAvailable.collectAsState()
    val error by updaterViewModel.error.collectAsState()
    val isDownloading by updaterViewModel.isDownloading.collectAsState()
    val downloadProgress by updaterViewModel.downloadProgress.collectAsState()
    val downloadedFile by updaterViewModel.downloadedFile.collectAsState()

    // Simulation state variables
    var simulateEnabled by remember { mutableStateOf(false) }
    var simIsChecking by remember { mutableStateOf(false) }
    var simUpdateAvailable by remember { mutableStateOf(false) }
    var simIsDownloading by remember { mutableStateOf(false) }
    var simDownloadProgress by remember { mutableStateOf(0f) }
    var simDownloadedFile by remember { mutableStateOf<File?>(null) }
    var simError by remember { mutableStateOf<String?>(null) }

    val simLatestVersion = remember {
        AppVersion(
            versionName = "3.2.0-beta",
            versionCode = 320,
            releaseDate = "2026-05-21",
            whatsNew = listOf(
                "Added a brand new <b>UI Test Sandbox</b> for easy developer updates debugging!",
                "Stunning onboarding-style wavy progress animations for app status updates.",
                "Premium glassmorphic card layouts and dynamic gradient highlights.",
                "Smooth micro-animations and improved layout responsiveness across all screen sizes."
            ),
            knownIssues = listOf(
                "Simulated sandbox mode overrides actual remote check updates."
            ),
            downloadUrl = "https://github.com/cromaguy/Rhythm/releases",
            apkAssetName = "rhythm-v3.2.0-beta.apk",
            apkSize = 18454937, // ~17.6 MB
            releaseNotes = "Simulated update notes",
            isPreRelease = true,
            buildNumber = 3200
        )
    }

    val activeIsCheckingForUpdates = if (simulateEnabled) simIsChecking else isCheckingForUpdates
    val activeUpdateAvailable = if (simulateEnabled) simUpdateAvailable else updateAvailable
    val activeError = if (simulateEnabled) simError else error
    val activeIsDownloading = if (simulateEnabled) simIsDownloading else isDownloading
    val activeDownloadProgress = if (simulateEnabled) simDownloadProgress else downloadProgress
    val activeDownloadedFile = if (simulateEnabled) simDownloadedFile else downloadedFile
    val activeLatestVersion = if (simulateEnabled) {
        if (simUpdateAvailable) simLatestVersion else null
    } else {
        latestVersion
    }
    val activeWhatsNew = activeLatestVersion?.whatsNew ?: emptyList()
    val activeKnownIssues = activeLatestVersion?.knownIssues ?: emptyList()

    // Dialog states
    var showChannelDialog by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    val intervalOptions = listOf(
        1 to context.getString(R.string.settings_interval_every_hour),
        6 to context.getString(R.string.settings_interval_every_6_hours),
        12 to context.getString(R.string.settings_interval_every_12_hours),
        24 to context.getString(R.string.settings_interval_once_a_day),
        168 to context.getString(R.string.settings_interval_once_a_week)
    )
    val updateIntervalLabel = intervalOptions.firstOrNull { (hours, _) ->
        hours == updateCheckIntervalHours
    }?.second ?: context.getString(R.string.settings_check_interval_value, updateCheckIntervalHours)

    // Check for updates when the screen is first shown and updates are enabled
    LaunchedEffect(updatesEnabled) {
        if (updatesEnabled) {
            updaterViewModel.checkForUpdates(force = true)
        }
    }

    // Infinite transition for continuous animations
    val infiniteTransition = rememberInfiniteTransition(label = "update_animations")

    // Rotating icon for downloading state
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Success scale animation
    val successScale = remember { Animatable(0.7f) }
    LaunchedEffect(activeDownloadedFile) {
        if (activeDownloadedFile != null) {
            successScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    // Determine status components
    val statusIcon = when {
        activeError != null -> RhythmIcons.BugReport
        activeDownloadedFile != null -> RhythmIcons.CheckCircle
        activeIsDownloading -> MaterialSymbolIcon("autorenew", filled = true)
        activeUpdateAvailable -> RhythmIcons.Download
        !updatesEnabled -> MaterialSymbolIcon("update_disabled", filled = true)
        else -> RhythmIcons.SystemUpdate
    }

    val statusColor = when {
        activeError != null -> MaterialTheme.colorScheme.error
        activeDownloadedFile != null -> MaterialTheme.colorScheme.tertiary
        activeIsDownloading -> MaterialTheme.colorScheme.secondary
        activeUpdateAvailable -> MaterialTheme.colorScheme.primary
        !updatesEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.primary
    }

    val statusTitle = when {
        activeError != null -> context.getString(R.string.updates_check_failed)
        !updatesEnabled -> context.getString(R.string.updates_disabled)
        activeIsCheckingForUpdates -> context.getString(R.string.updates_checking)
        activeIsDownloading -> context.getString(R.string.updates_downloading)
        activeDownloadedFile != null -> context.getString(R.string.updates_download_complete)
        activeUpdateAvailable -> context.getString(R.string.updates_available)
        !autoCheckForUpdates -> context.getString(R.string.updates_manual_check)
        else -> context.getString(R.string.updates_up_to_date_message)
    }

    val statusDescription = when {
        activeError != null -> activeError ?: context.getString(R.string.updates_unknown_error)
        !updatesEnabled -> context.getString(R.string.updates_disabled_message)
        activeIsCheckingForUpdates -> context.getString(R.string.fetching_latest_version)
        activeIsDownloading -> "${((activeLatestVersion?.apkSize ?: 0) * activeDownloadProgress / 100).toLong().let { updaterViewModel.getReadableFileSize(it) }} / ${activeLatestVersion?.let { updaterViewModel.getReadableFileSize(it.apkSize) } ?: ""}"
        activeDownloadedFile != null -> "Version ${activeLatestVersion?.versionName ?: "?"} is ready to install"
        activeUpdateAvailable -> "Version ${activeLatestVersion?.versionName ?: "?"} • ${activeLatestVersion?.let { updaterViewModel.getReadableFileSize(it.apkSize) } ?: ""}"
        !autoCheckForUpdates -> context.getString(R.string.updates_auto_disabled)
        else -> "Rhythm is up to date with the latest features and security updates"
    }

    val headerBlendHeight = 24.dp
    val headerBlendBaseColor = MaterialTheme.colorScheme.surface

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                // Static, non-collapsible Top Bar
                androidx.compose.material3.TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.Back,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerBlendHeight)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    headerBlendBaseColor,
                                    headerBlendBaseColor.copy(alpha = 0.72f),
                                    headerBlendBaseColor.copy(alpha = 0.32f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    ) { paddingValues ->
        val lazyListState = rememberSaveable(
            saver = LazyListStateSaver
        ) {
            androidx.compose.foundation.lazy.LazyListState()
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .offset(y = -headerBlendHeight),
            contentPadding = PaddingValues(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live Update Status display (No card bg, onboarding-like placements and wavy loaders)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Centered Icon with no container background (Onboarding style)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier
                                .size(72.dp)
                                .then(
                                    if (activeIsDownloading) {
                                        Modifier.graphicsLayer(rotationZ = rotationAngle)
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }

                    // Status Title
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )

                    // Linear Wavy Progress Bar between Title and Description (Onboarding style)
                    if (activeIsCheckingForUpdates) {
                        LinearWavyProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        )
                    }

                    if (activeIsDownloading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularWavyProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = context.getString(R.string.onboarding_in_progress),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    text = "${activeDownloadProgress.toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            LinearWavyProgressIndicator(
                                progress = { activeDownloadProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            )
                        }
                    }

                    // Status Description
                    Text(
                        text = statusDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (activeError != null) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )

                    // Action buttons (Install, Download, Cancel, Retry, Enable Updates, Check Again)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when {
                            activeError != null -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (simulateEnabled) {
                                                simError = null
                                            } else {
                                                updaterViewModel.clearError()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = context.getString(R.string.ui_dismiss),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            if (simulateEnabled) {
                                                simError = null
                                                scope.launch {
                                                    simIsChecking = true
                                                    delay(1500)
                                                    simIsChecking = false
                                                    simUpdateAvailable = true
                                                }
                                            } else {
                                                if (error?.contains("unknown sources", ignoreCase = true) == true ||
                                                    error?.contains("install from unknown", ignoreCase = true) == true) {
                                                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                        data = Uri.parse("package:${context.packageName}")
                                                    }
                                                    try {
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        val fallbackIntent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                                                        context.startActivity(fallbackIntent)
                                                    }
                                                } else {
                                                    updaterViewModel.checkForUpdates(force = true)
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        elevation = ButtonDefaults.buttonElevation(
                                            defaultElevation = 4.dp,
                                            pressedElevation = 8.dp
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (!simulateEnabled && (error?.contains("unknown sources", ignoreCase = true) == true ||
                                                              error?.contains("install from unknown", ignoreCase = true) == true))
                                                RhythmIcons.SettingsFilled else RhythmIcons.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (!simulateEnabled && (error?.contains("unknown sources", ignoreCase = true) == true ||
                                                     error?.contains("install from unknown", ignoreCase = true) == true))
                                                context.getString(R.string.updates_open_settings)
                                            else
                                                context.getString(R.string.updates_retry),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }
                            }

                            !updatesEnabled -> {
                                Button(
                                    onClick = { appSettings.setUpdatesEnabled(true) },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.SystemUpdate,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = context.getString(R.string.updates_enable_updates),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }

                            activeIsDownloading -> {
                                OutlinedButton(
                                    onClick = {
                                        if (simulateEnabled) {
                                            simIsDownloading = false
                                            simDownloadProgress = 0f
                                            simUpdateAvailable = true
                                        } else {
                                            updaterViewModel.cancelDownload()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Block,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = context.getString(R.string.updates_cancel_download),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }

                            activeDownloadedFile != null -> {
                                Button(
                                    onClick = {
                                        if (simulateEnabled) {
                                            Toast.makeText(context, "Simulating update installation (Success!)", Toast.LENGTH_SHORT).show()
                                            simDownloadedFile = null
                                            simUpdateAvailable = false
                                        } else {
                                            updaterViewModel.installDownloadedApk()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                        .scale(successScale.value),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 6.dp,
                                        pressedElevation = 12.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = context.getString(R.string.updates_install_update),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }

                            activeUpdateAvailable -> {
                                Button(
                                    onClick = {
                                        if (simulateEnabled) {
                                            scope.launch {
                                                simIsDownloading = true
                                                simDownloadProgress = 0f
                                                while (simDownloadProgress < 100f && simIsDownloading) {
                                                    delay(100)
                                                    simDownloadProgress += 4f
                                                }
                                                if (simIsDownloading) {
                                                    simIsDownloading = false
                                                    simDownloadedFile = File(context.cacheDir, "simulated_update.apk")
                                                }
                                            }
                                        } else {
                                            updaterViewModel.downloadUpdate()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 6.dp,
                                        pressedElevation = 12.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = context.getString(R.string.updates_download_update),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }

                            !activeIsCheckingForUpdates -> {
                                if (!autoCheckForUpdates) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (simulateEnabled) {
                                                    scope.launch {
                                                        simIsChecking = true
                                                        simError = null
                                                        delay(1500)
                                                        simIsChecking = false
                                                        simUpdateAvailable = true
                                                    }
                                                } else {
                                                    updaterViewModel.checkForUpdates(force = true)
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = RhythmIcons.SearchFilled,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Check Now",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { appSettings.setAutoCheckForUpdates(true) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                        ) {
                                            Icon(
                                                imageVector = MaterialSymbolIcon("autorenew", filled = true),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = context.getString(R.string.updates_enable_auto_check),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (simulateEnabled) {
                                                scope.launch {
                                                    simIsChecking = true
                                                    simError = null
                                                    delay(1500)
                                                    simIsChecking = false
                                                    simUpdateAvailable = true
                                                }
                                            } else {
                                                updaterViewModel.checkForUpdates(force = true)
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = context.getString(R.string.updates_check_again),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Build Info display
            item {
                val isUpdateAvail = activeUpdateAvailable && activeLatestVersion != null
                val displayVersionName = if (isUpdateAvail) activeLatestVersion.versionName else currentVersion.versionName
                val displayReleaseDate = if (isUpdateAvail) activeLatestVersion.releaseDate else currentVersion.releaseDate

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            val tag = currentVersion.versionName
                            val releaseUrl = if (tag.startsWith("v", ignoreCase = true)) {
                                "https://github.com/cromaguy/Rhythm/releases/tag/$displayVersionName"
                            } else {
                                "https://github.com/cromaguy/Rhythm/releases/tag/v$displayVersionName"
                            }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to open release page", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isUpdateAvail) "Available Version: V $displayVersionName" else context.getString(R.string.updates_version_prefix, displayVersionName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUpdateAvail) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isUpdateAvail) "Released: $displayReleaseDate" else context.getString(R.string.updates_released_prefix, displayReleaseDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3. What's New section
            item {
                AnimatedVisibility(
                    visible = updatesEnabled && activeUpdateAvailable && activeLatestVersion != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = context.getString(R.string.updates_whats_new),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
                                if (activeWhatsNew.isEmpty()) {
                                    Text(
                                        text = "Refer to release notes on GitHub releases page.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    activeWhatsNew.forEachIndexed { index, change ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .padding(top = 8.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            AndroidView(
                                                modifier = Modifier.fillMaxWidth(),
                                                factory = { ctx ->
                                                    TextView(ctx).apply {
                                                        setTextColor(onSurfaceColor)
                                                    }
                                                },
                                                update = { textView ->
                                                    textView.text = HtmlCompat.fromHtml(change, HtmlCompat.FROM_HTML_MODE_COMPACT)
                                                    textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                                                }
                                            )
                                        }
                                        if (index < activeWhatsNew.size - 1) {
                                            Spacer(modifier = Modifier.height(1.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Known Issues section
            item {
                AnimatedVisibility(
                    visible = updatesEnabled && activeUpdateAvailable && activeLatestVersion != null && activeKnownIssues.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = context.getString(R.string.updates_known_issues),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
                                activeKnownIssues.forEachIndexed { index, issue ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .padding(top = 8.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.error,
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        AndroidView(
                                            modifier = Modifier.fillMaxWidth(),
                                            factory = { ctx ->
                                                TextView(ctx).apply {
                                                    setTextColor(onSurfaceColor)
                                                }
                                            },
                                            update = { textView ->
                                                textView.text = HtmlCompat.fromHtml(issue, HtmlCompat.FROM_HTML_MODE_COMPACT)
                                                textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                                            }
                                        )
                                    }
                                    if (index < activeKnownIssues.size - 1) {
                                        Spacer(modifier = Modifier.height(1.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Settings Section below
            item {
                Spacer(modifier = Modifier.height(24.dp))
                val updateSettingsItems = buildList {
                    add(
                        SettingItem(
                            RhythmIcons.SystemUpdate,
                            context.getString(R.string.updates_enable),
                            context.getString(R.string.updates_enable_updates),
                            toggleState = updatesEnabled,
                            onToggleChange = { appSettings.setUpdatesEnabled(it) }
                        )
                    )
                    if (updatesEnabled) {
                        add(
                            SettingItem(
                                RhythmIcons.Update,
                                context.getString(R.string.onboarding_periodic_check_title),
                                context.getString(R.string.onboarding_periodic_check_desc),
                                toggleState = autoCheckForUpdates,
                                onToggleChange = { appSettings.setAutoCheckForUpdates(it) }
                            )
                        )
                        add(
                            SettingItem(
                                MaterialSymbolIcon("cloud_sync"),
                                context.getString(R.string.onboarding_smart_polling_title),
                                context.getString(R.string.onboarding_smart_polling_desc),
                                toggleState = useSmartUpdatePolling,
                                onToggleChange = { appSettings.setUseSmartUpdatePolling(it) }
                            )
                        )
                        add(
                            SettingItem(
                                RhythmIcons.Category,
                                context.getString(R.string.updates_channel_title),
                                "${updateChannel.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} - Tap to change",
                                onClick = { showChannelDialog = true }
                            )
                        )
                        add(
                            SettingItem(
                                RhythmIcons.Category,
                                context.getString(R.string.updates_source_title),
                                getUpdateSourceLabel(context, updateSource),
                                onClick = { showSourceDialog = true }
                            )
                        )
                        add(
                            SettingItem(
                                RhythmIcons.AccessTime,
                                context.getString(R.string.updates_check_interval_title),
                                updateIntervalLabel,
                                onClick = { showIntervalDialog = true }
                            )
                        )
                    }
                }

                Material3SettingsGroup(
                    title = context.getString(R.string.updates_settings),
                    items = updateSettingsItems.map { item ->
                        toMaterial3SettingsItem(context = context, item = item, hapticFeedback = haptics)
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // 6. Informational card about smart polling
            item {
                AnimatedVisibility(
                    visible = updatesEnabled && useSmartUpdatePolling,
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = context.getString(R.string.updates_smart_polling),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = context.getString(R.string.updates_smart_polling_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // 7. Developer UI Test Sandbox Card
//            item {
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(18.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
//                    ),
//                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//                ) {
//                    Column(
//                        modifier = Modifier.padding(16.dp),
//                        verticalArrangement = Arrangement.spacedBy(12.dp)
//                    ) {
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.spacedBy(8.dp)
//                            ) {
//                                Icon(
//                                    imageVector = MaterialSymbolIcon("science"),
//                                    contentDescription = null,
//                                    tint = MaterialTheme.colorScheme.secondary,
//                                    modifier = Modifier.size(24.dp)
//                                )
//                                Text(
//                                    text = "UI Test Sandbox",
//                                    style = MaterialTheme.typography.titleMedium,
//                                    fontWeight = FontWeight.Bold,
//                                    color = MaterialTheme.colorScheme.onSecondaryContainer
//                                )
//                            }
//                            TunerAnimatedSwitch(
//                                checked = simulateEnabled,
//                                onCheckedChange = {
//                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
//                                    simulateEnabled = it
//                                    if (!it) {
//                                        // Reset simulated states when disabled
//                                        simIsChecking = false
//                                        simUpdateAvailable = false
//                                        simIsDownloading = false
//                                        simDownloadProgress = 0f
//                                        simDownloadedFile = null
//                                        simError = null
//                                    }
//                                }
//                            )
//                        }
//
//                        Text(
//                            text = "Enable to simulate different update states and progress bars for UI testing.",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
//                        )
//
//                        AnimatedVisibility(
//                            visible = simulateEnabled,
//                            enter = fadeIn() + expandVertically(),
//                            exit = fadeOut() + shrinkVertically()
//                        ) {
//                            Column(
//                                verticalArrangement = Arrangement.spacedBy(8.dp)
//                            ) {
//                                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
//
//                                Text(
//                                    text = "Simulate State:",
//                                    style = MaterialTheme.typography.titleSmall,
//                                    fontWeight = FontWeight.SemiBold,
//                                    color = MaterialTheme.colorScheme.onSecondaryContainer
//                                )
//
//                                // Row 1 of presets: Checking & Update Available
//                                Row(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                                ) {
//                                    val isCheckingSelected = simIsChecking && !simUpdateAvailable && !simIsDownloading && simDownloadedFile == null && simError == null
//                                    if (isCheckingSelected) {
//                                        Button(
//                                            onClick = {
//                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
//                                                simIsChecking = true
//                                                simUpdateAvailable = false
//                                                simIsDownloading = false
//                                                simDownloadedFile = null
//                                                simError = null
//                                            },
//                                            modifier = Modifier.weight(1f).height(36.dp),
//                                            shape = RoundedCornerShape(12.dp),
//                                            contentPadding = PaddingValues(0.dp)
//                                        ) {
//                                            Text("Checking", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
//                                        }
//                                    } else {
//                                        OutlinedButton(
//                                            onClick = {
//                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
//                                                simIsChecking = true
//                                                simUpdateAvailable = false
//                                                simIsDownloading = false
//                                                simDownloadedFile = null
//                                                simError = null
//                                            },
//                                            modifier = Modifier.weight(1f).height(36.dp),
//                                            shape = RoundedCornerShape(12.dp),
//                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
//                                            contentPadding = PaddingValues(0.dp)
//                                        ) {
//                                            Text("Checking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
//                                        }
//                                    }
//
//                                    val isAvailableSelected = !simIsChecking && simUpdateAvailable && !simIsDownloading && simDownloadedFile == null && simError == null
//                                    if (isAvailableSelected) {
//                                        Button(
//                                            onClick = {
//                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
//                                                simIsChecking = false
//                                                simUpdateAvailable = true
//                                                simIsDownloading = false
//                                                simDownloadedFile = null
//                                                simError = null
//                                            },
//                                            modifier = Modifier.weight(1f).height(36.dp),
//                                            shape = RoundedCornerShape(12.dp),
//                                            contentPadding = PaddingValues(0.dp)
//                                        ) {
//                                            Text("Update Available", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
//                                        }
//                                    } else {
//                                        OutlinedButton(
//                                            onClick = {
//                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
//                                                simIsChecking = false
//                                                simUpdateAvailable = true
//                                                simIsDownloading = false
//                                                simDownloadedFile = null
//                                                simError = null
//                                            },
//                                            modifier = Modifier.weight(1f).height(36.dp),
//                                            shape = RoundedCornerShape(12.dp),
//                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
//                                            contentPadding = PaddingValues(0.dp)
//                                        ) {
//                                            Text("Update Available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
//                                        }
//                                    }
//                                }
//
//                                // Row 2 of presets: Downloading & Downloaded
//                                Row(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                                ) {
//                                    val isDownloadingSelected = simIsDownloading
//                                    if (isDownloadingSelected) {
//                                        Button(
//                                            onClick = {
//                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
//                                                simIsChecking = false
//                                                simUpdateAvailable = true
//                                                simIsDownloading = true
//                                                simDownloadedFile = null
//                                                simError = null
//
//                                                // Start simulating a progress cycle
//                                                scope.launch {
//                                                    simDownloadProgress = 0f
//                                                    while (simDownloadProgress < 100f && simIsDownloading) {
//                                                        delay(100)
//                                                        simDownloadProgress += 4f
//                                                    }
//                                                    if (simIsDownloading) {
//                                                        simIsDownloading = false
//                                                        simDownloadedFile = File(context.cacheDir, "simulated_update.apk")
//                                                    }
//                                                }
//                                            },
//                                            modifier = Modifier.weight(1f).height(36.dp),
//                                            shape = RoundedCornerShape(12.dp),
//                                            contentPadding = PaddingValues(0.dp)
//                                        ) {
//                                            Text("Downloading", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
//                                        }
//                                    } else {
//                                        OutlinedButton(
//                                            onClick = {
//                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
//                                                simIsChecking = false
//                                                simUpdateAvailable = true
//                                                simIsDownloading = true
//                                                simDownloadedFile = null
//                                                simError = null
//
//                                                // Start simulating a progress cycle
//                                                scope.launch {
//                                                    simDownloadProgress = 0f
//                                                    while (simDownloadProgress < 100f && simIsDownloading) {
//                                                        delay(100)
//                                                        simDownloadProgress += 4f
//                                                    }
//                                                    if (simIsDownloading) {
//                                                        simIsDownloading = false
//                                                        simDownloadedFile = File(context.cacheDir, "simulated_update.apk")
//                                                    }
//                                                }
//                                            },
//                                            modifier = Modifier.weight(1f).height(36.dp),
//                                            shape = RoundedCornerShape(12.dp),
//                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
//                                            contentPadding = PaddingValues(0.dp)
//                                        ) {
//                                            Text("Downloading", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
//                                        }
//                                    }
//
//                                    val isDownloadedSelected = simDownloadedFile != null && !simIsDownloading
//                                    if (isDownloadedSelected) {
//                                        Button(
//                                            onClick = {
//                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
//                                                simIsChecking = false
//                                                simUpdateAvailable = true
//                                                simIsDownloading = false
//                                                simDownloadedFile = File(context.cacheDir, "simulated_update.apk")
//                                                simError = null
//                                            },
//                                            modifier = Modifier.weight(1f).height(36.dp),
//                                            shape = RoundedCornerShape(12.dp),
//                                            contentPadding = PaddingValues(0.dp)
//                                        ) {
//                                            Text("Downloaded", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
//                                        }
//                                    } else {
//                                        OutlinedButton(
//                                            onClick = {
//                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
//                                                simIsChecking = false
//                                                simUpdateAvailable = true
//                                                simIsDownloading = false
//                                                simDownloadedFile = File(context.cacheDir, "simulated_update.apk")
//                                                simError = null
//                                            },
//                                            modifier = Modifier.weight(1f).height(36.dp),
//                                            shape = RoundedCornerShape(12.dp),
//                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
//                                            contentPadding = PaddingValues(0.dp)
//                                        ) {
//                                            Text("Downloaded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
//                                        }
//                                    }
//                                }
//
//                                // Row 3 of presets: Error & Reset
//                                Row(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                                ) {
//                                    val isErrorSelected = simError != null
//                                    if (isErrorSelected) {
//                                        Button(
//                                            onClick = {
//                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
//                                                simIsChecking = false
//                                                simUpdateAvailable = false
//                                                simIsDownloading = false
//                                                simDownloadedFile = null
//                                                simError = "Simulated network timeout error. Please check connection and try again."
//                                            },
//                                            modifier = Modifier.weight(1f).height(36.dp),
//                                            shape = RoundedCornerShape(12.dp),
//                                            contentPadding = PaddingValues(0.dp)
//                                        ) {
//                                            Text("Error", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
//                                        }
//                                    } else {
//                                        OutlinedButton(
//                                            onClick = {
//                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
//                                                simIsChecking = false
//                                                simUpdateAvailable = false
//                                                simIsDownloading = false
//                                                simDownloadedFile = null
//                                                simError = "Simulated network timeout error. Please check connection and try again."
//                                            },
//                                            modifier = Modifier.weight(1f).height(36.dp),
//                                            shape = RoundedCornerShape(12.dp),
//                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
//                                            contentPadding = PaddingValues(0.dp)
//                                        ) {
//                                            Text("Error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
//                                        }
//                                    }
//
//                                    val isResetSelected = !simIsChecking && !simUpdateAvailable && !simIsDownloading && simDownloadedFile == null && simError == null
//                                    if (isResetSelected) {
//                                        Button(
//                                            onClick = {
//                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
//                                                simIsChecking = false
//                                                simUpdateAvailable = false
//                                                simIsDownloading = false
//                                                simDownloadedFile = null
//                                                simError = null
//                                            },
//                                            modifier = Modifier.weight(1f).height(36.dp),
//                                            shape = RoundedCornerShape(12.dp),
//                                            contentPadding = PaddingValues(0.dp)
//                                        ) {
//                                            Text("Reset", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
//                                        }
//                                    } else {
//                                        OutlinedButton(
//                                            onClick = {
//                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
//                                                simIsChecking = false
//                                                simUpdateAvailable = false
//                                                simIsDownloading = false
//                                                simDownloadedFile = null
//                                                simError = null
//                                            },
//                                            modifier = Modifier.weight(1f).height(36.dp),
//                                            shape = RoundedCornerShape(12.dp),
//                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
//                                            contentPadding = PaddingValues(0.dp)
//                                        ) {
//                                            Text("Reset", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
        }
    }

    // Update Channel Dialog
    if (showChannelDialog) {
        AlertDialog(
            onDismissRequest = { showChannelDialog = false },
            icon = {
                Icon(
                    imageVector = RhythmIcons.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = { Text(context.getString(R.string.updates_channel_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = context.getString(R.string.updates_channel_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val channels = listOf(
                        "stable" to context.getString(R.string.updates_channel_desc),
                        "beta" to context.getString(R.string.updates_experimental_coming)
                    )

                    channels.forEach { (channel, description) ->
                        Card(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                appSettings.setUpdateChannel(channel)
                                showChannelDialog = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (updateChannel == channel)
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = channel.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (updateChannel == channel) {
                                    Icon(
                                        imageVector = RhythmIcons.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showChannelDialog = false }) {
                    Icon(
                        imageVector = RhythmIcons.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.ui_close))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Update Check Interval Dialog
    if (showIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            icon = {
                Icon(
                    imageVector = RhythmIcons.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = { Text(context.getString(R.string.updates_check_interval_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = context.getString(R.string.updates_check_frequency),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    intervalOptions.forEach { (hours, label) ->
                        Card(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                appSettings.setUpdateCheckIntervalHours(hours)
                                showIntervalDialog = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (updateCheckIntervalHours == hours)
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
                                    fontWeight = if (updateCheckIntervalHours == hours) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (updateCheckIntervalHours == hours) {
                                    Icon(
                                        imageVector = RhythmIcons.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showIntervalDialog = false }) {
                    Icon(
                        imageVector = RhythmIcons.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.ui_close))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            icon = {
                Icon(
                    imageVector = RhythmIcons.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = { Text(context.getString(R.string.updates_source_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = context.getString(R.string.updates_source_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val sources = listOf(
                        "installed" to getUpdateSourceLabel(context, "installed"),
                        "github" to context.getString(R.string.updates_source_github_desc),
                        "fdroid" to context.getString(R.string.updates_source_fdroid_desc)
                    )

                    sources.forEach { (source, description) ->
                        Card(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                appSettings.setUpdateSource(source)
                                showSourceDialog = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (updateSource == source)
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when (source) {
                                            "installed" -> context.getString(R.string.updates_source_installed)
                                            else -> source.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (updateSource == source) {
                                    Icon(
                                        imageVector = RhythmIcons.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showSourceDialog = false }) {
                    Icon(
                        imageVector = RhythmIcons.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.ui_close))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun ExperimentalFeaturesScreen(onBackClick: () -> Unit, onNavigateToGoSettings: (() -> Unit)? = null) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val appMode by appSettings.appMode.collectAsState()
    val hapticFeedbackEnabled by appSettings.hapticFeedbackEnabled.collectAsState()
    val showLyrics by appSettings.showLyrics.collectAsState()
    val showLyricsTranslation by appSettings.showLyricsTranslation.collectAsState()
    val showLyricsRomanization by appSettings.showLyricsRomanization.collectAsState()
    val skipSilenceEnabled by appSettings.skipSilenceEnabled.collectAsState()
    val audioRoutingMode by appSettings.audioRoutingMode.collectAsState()
    val haptic = LocalHapticFeedback.current
    
    // Third-party integrations states
    val scrobblingEnabled by appSettings.scrobblingEnabled.collectAsState()
    val discordRichPresenceEnabled by appSettings.discordRichPresenceEnabled.collectAsState()
    val broadcastStatusEnabled by appSettings.broadcastStatusEnabled.collectAsState()
    val bluetoothLyricsEnabled by appSettings.bluetoothLyricsEnabled.collectAsState()
    
    val forcePlayerCompactMode by appSettings.forcePlayerCompactMode.collectAsState()
    val useExperimentalPlayerUi by appSettings.useExperimentalPlayerUi.collectAsState()
    
    val updaterViewModel: AppUpdaterViewModel = viewModel()
    val latestVersion by updaterViewModel.latestVersion.collectAsState()

    var showRestartDialog by remember { mutableStateOf(false) }
    var restartDialogMessage by remember { mutableStateOf("") }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_experimental),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
        val settingGroups = buildList {
            add(
                SettingGroup(
                    title = context.getString(R.string.settings_audio_effects),
                    items = listOf(
                        SettingItem(
                            MaterialSymbolIcon("hearing"),
                            context.getString(R.string.settings_skip_silence),
                            context.getString(R.string.settings_skip_silence_desc),
                            toggleState = skipSilenceEnabled,
                            onToggleChange = {
                                appSettings.setSkipSilenceEnabled(it)
                            }
                        )
                    )
                )
            )


            add(
                SettingGroup(
                    title = context.getString(R.string.settings_lyrics_customization),
                    items = if (showLyrics) {
                        listOf(
                            SettingItem(
                                MaterialSymbolIcon("translate", filled = true),
                                context.getString(R.string.settings_lyrics_show_translation),
                                context.getString(R.string.settings_lyrics_show_translation_desc),
                                toggleState = showLyricsTranslation,
                                onToggleChange = { appSettings.setShowLyricsTranslation(it) }
                            ),
                            SettingItem(
                                RhythmIcons.Language,
                                context.getString(R.string.settings_lyrics_show_romanization),
                                context.getString(R.string.settings_lyrics_show_romanization_desc),
                                toggleState = showLyricsRomanization,
                                onToggleChange = { appSettings.setShowLyricsRomanization(it) }
                            )
                        )
                    } else {
                        listOf(
                            SettingItem(
                                RhythmIcons.Info,
                                context.getString(R.string.settings_show_lyrics_player),
                                context.getString(R.string.settings_show_lyrics_player_desc)
                            )
                        )
                    }
                )
            )
            
            // Developer/Debugging features group
            add(
                SettingGroup(
                    title = context.getString(R.string.exp_developer_debugging),
                    items = listOf(
                        SettingItem(
                            RhythmIcons.Code,
                            context.getString(R.string.exp_codec_monitoring),
                            context.getString(R.string.exp_codec_monitoring_desc),
                            toggleState = appSettings.codecMonitoringEnabled.collectAsState().value,
                            onToggleChange = { appSettings.setCodecMonitoringEnabled(it) }
                        ),
                        SettingItem(
                            RhythmIcons.Headphones,
                            context.getString(R.string.exp_audio_device_logging),
                            context.getString(R.string.exp_audio_device_logging_desc),
                            toggleState = appSettings.audioDeviceLoggingEnabled.collectAsState().value,
                            onToggleChange = { appSettings.setAudioDeviceLoggingEnabled(it) }
                        ),
                        SettingItem(
                            MaterialSymbolIcon("restart_alt"),
                            context.getString(R.string.exp_launch_onboarding),
                            context.getString(R.string.exp_launch_onboarding_desc),
                            onClick = { appSettings.setOnboardingCompleted(false) }
                        ),
                        SettingItem(
                            RhythmIcons.BugReport,
                            context.getString(R.string.exp_test_crash),
                            context.getString(R.string.exp_test_crash_desc),
                            onClick = { chromahub.rhythm.app.util.CrashReporter.testCrash() }
                        ),
                        SettingItem(
                            MaterialSymbolIcon("smartphone"),
                            context.getString(R.string.exp_force_player_compact_mode),
                            context.getString(R.string.exp_force_player_compact_mode_desc),
                            toggleState = forcePlayerCompactMode,
                            onToggleChange = { appSettings.setForcePlayerCompactMode(it) }
                        ),
                        SettingItem(
                            MaterialSymbolIcon("cloud_queue"),
                            context.getString(R.string.exp_go_mode),
                            context.getString(R.string.exp_go_mode_desc),
                            toggleState = appMode == "STREAMING",
                            onToggleChange = { enabled ->
                                appSettings.setAppMode(if (enabled) "STREAMING" else "LOCAL")
                            },
                            onClick = { onNavigateToGoSettings?.invoke() }
                        )
                    )
                )
            )
            
            // Third-Party Integrations group
            add(
                SettingGroup(
                    title = context.getString(R.string.exp_third_party_integrations),
                    items = listOf(
                        SettingItem(
                            RhythmIcons.MusicNote,
                            context.getString(R.string.scrobbling_enabled),
                            context.getString(R.string.scrobbling_desc),
                            toggleState = scrobblingEnabled,
                            onToggleChange = { appSettings.setScrobblingEnabled(it) }
                        ),
                        SettingItem(
                            MaterialSymbolIcon("forum"),
                            context.getString(R.string.discord_enabled),
                            context.getString(R.string.discord_desc),
                            toggleState = discordRichPresenceEnabled,
                            onToggleChange = { appSettings.setDiscordRichPresenceEnabled(it) }
                        ),
                        SettingItem(
                            MaterialSymbolIcon("wifi"),
                            context.getString(R.string.broadcast_status_enabled),
                            context.getString(R.string.broadcast_status_desc),
                            toggleState = broadcastStatusEnabled,
                            onToggleChange = { appSettings.setBroadcastStatusEnabled(it) }
                        ),
                        SettingItem(
                            MaterialSymbolIcon("lyrics"),
                            context.getString(R.string.bluetooth_lyrics_enabled),
                            context.getString(R.string.bluetooth_lyrics_desc),
                            toggleState = bluetoothLyricsEnabled,
                            onToggleChange = {
                                appSettings.setBluetoothLyricsEnabled(it)
                                if (it && !broadcastStatusEnabled) {
                                    appSettings.setBroadcastStatusEnabled(true)
                                }
                            }
                        )
                    )
                )
            )
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            items(settingGroups, key = { "setting_${it.title}_${settingGroups.indexOf(it)}" }) { group ->
                Spacer(modifier = Modifier.height(24.dp))

                val materialItems = group.items.map { item ->
                    toMaterial3SettingsItem(context = context, item = item)
                }

                Material3SettingsGroup(
                    title = group.title,
                    items = materialItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            item(key = "experimental_audio_routing_section") {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = context.getString(R.string.settings_audio_routing),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(34.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                tonalElevation = 0.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Headphones,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = context.getString(R.string.settings_dac_usb_audio),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = context.getString(R.string.settings_dac_usb_audio_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ExpressiveButtonGroup(
                            items = listOf(context.getString(R.string.settings_audio_routing_default), context.getString(R.string.settings_audio_routing_app), context.getString(R.string.settings_audio_routing_system)),
                            selectedIndex = when (audioRoutingMode) {
                                "default" -> 0
                                "app" -> 1
                                "system" -> 2
                                else -> 0
                            },
                            onItemClick = { index ->
                                when (index) {
                                    0 -> {
                                        appSettings.setAudioRoutingMode("default")
                                        showRestartDialog = true
                                        restartDialogMessage = "Audio routing changed to Default. Restart the app to apply the changes."
                                    }
                                    1 -> {
                                        appSettings.setAudioRoutingMode("app")
                                        showRestartDialog = true
                                        restartDialogMessage = "Audio routing changed to App mode. Restart the app to apply the changes."
                                    }
                                    2 -> {
                                        appSettings.setAudioRoutingMode("system")
                                        showRestartDialog = true
                                        restartDialogMessage = "Audio routing changed to System. Restart the app to apply the changes."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = when (audioRoutingMode) {
                                "app" -> context.getString(R.string.settings_audio_routing_app_desc)
                                "system" -> context.getString(R.string.settings_audio_routing_system_desc)
                                else -> context.getString(R.string.settings_audio_routing_default_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = MaterialSymbolIcon("science"),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = context.getString(R.string.updates_experimental_coming),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    if (showRestartDialog) {
        AppRestartDialog(
            onDismiss = { showRestartDialog = false },
            onRestart = {
                showRestartDialog = false
                chromahub.rhythm.app.util.AppRestarter.restartApp(context)
            },
            onContinue = {
                showRestartDialog = false
            },
            message = restartDialogMessage
        )
    }

    // Show update bottomsheet - removed, now handled globally in LocalNavigation
}

private fun getUpdateSourceLabel(context: Context, source: String): String {
    return when (source.lowercase()) {
        "github" -> context.getString(R.string.updates_source_github_label)
        "fdroid" -> context.getString(R.string.updates_source_fdroid_label)
        else -> context.getString(R.string.updates_source_installed_label)
    }
}

/**
 * Toggle card for individual decoration elements
 */
@Composable
private fun DecorationToggleCard(
    title: String,
    description: String,
    icon: MaterialSymbolIcon,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon with background
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isEnabled)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Toggle switch
            TunerAnimatedSwitch(
                checked = isEnabled,
                onCheckedChange = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                    onToggle(it)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FestivalSelectionBottomSheet(
    currentFestival: String,
    onDismiss: () -> Unit,
    onFestivalSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Animation states
    var showContent by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentAlpha"
    )

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.primary
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        val contentHorizontalPadding = 24.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 0.dp)
                .padding(bottom = 24.dp)
                .graphicsLayer(alpha = contentAlpha)
        ) {
            StandardBottomSheetHeader(
                title = context.getString(R.string.settings_select_festival),
                subtitle = context.getString(R.string.settings_choose_festive_theme),
                visible = showContent,
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Festival Options
            val festivals = listOf(
                Triple("CHRISTMAS", context.getString(R.string.settings_festival_christmas), MaterialSymbolIcon("ac_unit")),
                Triple("NEW_YEAR", context.getString(R.string.settings_festival_new_year), MaterialSymbolIcon("celebration"))
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = contentHorizontalPadding)
            ) {
                festivals.forEach { (value, name, icon) ->
                    val isSelected = currentFestival == value
                    val isAvailable = true

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            if (isAvailable) {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                onFestivalSelected(value)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = when {
                                        !isAvailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = when {
                                            !isAvailable -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = RhythmIcons.CheckCircle,
                                    contentDescription = context.getString(R.string.ui_selected),
                                    
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentHorizontalPadding)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = RhythmIcons.Info,
                        contentDescription = null,
                        
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = context.getString(R.string.settings_more_festivals),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getFestivalDisplayName(festivalType: String): String {
    return when (festivalType) {
        "CHRISTMAS" -> "Christmas"
        "NEW_YEAR" -> "New Year"
        "NONE" -> "None"
        "CUSTOM" -> "Custom"
        else -> "Not selected"
    }
}

//            item { Spacer(modifier = Modifier.height(40.dp)) }
//        }
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSourceDialog(
    onDismiss: () -> Unit,
    appSettings: AppSettings,
    context: Context,
    haptic: HapticFeedback
) {
    val lyricsSourcePreference by appSettings.lyricsSourcePreference.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.primary
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {

                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = context.getString(R.string.lyrics_source_priority),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = CircleShape
                                    )
                            ) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    text = context.getString(R.string.lyrics_choose_source),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

            // Options
            val sourceOptions = listOf(
                chromahub.rhythm.app.shared.data.model.LyricsSourcePreference.EMBEDDED_FIRST to Triple(
                    "Embedded First",
                    "Try audio file metadata → Online APIs → .lrc files",
                    RhythmIcons.MusicNote
                ),
                chromahub.rhythm.app.shared.data.model.LyricsSourcePreference.API_FIRST to Triple(
                    "API First",
                    "Try online services → Audio metadata → .lrc files",
                    RhythmIcons.CloudDownload
                ),
                chromahub.rhythm.app.shared.data.model.LyricsSourcePreference.LOCAL_FIRST to Triple(
                    "Local .lrc First",
                    "Try .lrc files → Audio metadata → Online APIs",
                    RhythmIcons.Folder
                )
            )

            sourceOptions.forEach { (preference, info) ->
                val (title, description, icon) = info
                val isSelected = lyricsSourcePreference == preference

                Card(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        appSettings.setLyricsSourcePreference(preference)
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = RhythmIcons.CheckCircle,
                                contentDescription = "Selected",
                                
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = RhythmIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = context.getString(R.string.lyrics_embedded_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibrarySettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val enableRatingSystem by appSettings.enableRatingSystem.collectAsState()
    val libraryCombineDiscs by appSettings.libraryCombineDiscs.collectAsState()
    val preferSongArtwork by appSettings.preferSongArtwork.collectAsState()
    val losslessArtwork by appSettings.losslessArtwork.collectAsState()
    val albumBottomSheetGradientBlur by appSettings.albumBottomSheetGradientBlur.collectAsState()

    var showLibraryTabOrderBottomSheet by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var restartRequiresArtworkRescan by remember { mutableStateOf(false) }
    var restartDialogMessage by remember {
        mutableStateOf(context.getString(R.string.settings_song_artwork_restart_required))
    }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_library_settings),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
        val settingGroups = listOf(
            SettingGroup(
                title = context.getString(R.string.settings_library_group_organization),
                items = listOf(
                    SettingItem(
                        MaterialSymbolIcon("star"),
                        context.getString(R.string.settings_song_ratings),
                        context.getString(R.string.settings_song_ratings_desc),
                        toggleState = enableRatingSystem,
                        onToggleChange = { appSettings.setEnableRatingSystem(it) }
                    ),
                    SettingItem(
                        MaterialSymbolIcon("reorder"),
                        context.getString(R.string.settings_library_tab_order),
                        context.getString(R.string.settings_library_tab_order_desc),
                        onClick = { showLibraryTabOrderBottomSheet = true }
                    ),
                    SettingItem(
                        RhythmIcons.Album,
                        context.getString(R.string.settings_library_combine_discs),
                        context.getString(R.string.settings_library_combine_discs_desc),
                        toggleState = libraryCombineDiscs,
                        onToggleChange = { appSettings.setLibraryCombineDiscs(it) }
                    )
                )
            ),
            SettingGroup(
                title = context.getString(R.string.settings_library_group_artwork),
                items = listOf(
                    SettingItem(
                        RhythmIcons.Album,
                        context.getString(R.string.settings_ignore_mediastore_covers),
                        context.getString(R.string.settings_ignore_mediastore_covers_desc),
                        toggleState = preferSongArtwork,
                        onToggleChange = {
                            if (it != preferSongArtwork) {
                                appSettings.setPreferSongArtwork(it)
                                restartRequiresArtworkRescan = true
                                restartDialogMessage = context.getString(R.string.settings_song_artwork_restart_required)
                                showRestartDialog = true
                            }
                        }
                    ),
                    SettingItem(
                        RhythmIcons.MusicNote,
                        context.getString(R.string.settings_lossless_artwork),
                        context.getString(R.string.settings_lossless_artwork_desc),
                        toggleState = losslessArtwork,
                        onToggleChange = {
                            if (it != losslessArtwork) {
                                appSettings.setLosslessArtwork(it)
                                restartRequiresArtworkRescan = true
                                restartDialogMessage = context.getString(R.string.settings_song_artwork_restart_required)
                                showRestartDialog = true
                            }
                        }
                    ),
                    SettingItem(
                        MaterialSymbolIcon("lens_blur"),
                        context.getString(R.string.settings_album_bottom_sheet_gradient_blur),
                        context.getString(R.string.settings_album_bottom_sheet_gradient_blur_desc),
                        toggleState = albumBottomSheetGradientBlur,
                        onToggleChange = { appSettings.setAlbumBottomSheetGradientBlur(it) }
                    )
                )
            )
        )

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            items(settingGroups, key = { "libsettings_${it.title}" }) { group ->
                Spacer(modifier = Modifier.height(24.dp))

                val materialItems = group.items.map { item ->
                    toMaterial3SettingsItem(context = context, item = item)
                }

                Material3SettingsGroup(
                    title = group.title,
                    items = materialItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    if (showLibraryTabOrderBottomSheet) {
        LibraryTabOrderBottomSheet(
            onDismiss = { showLibraryTabOrderBottomSheet = false },
            appSettings = appSettings,
            haptics = haptics
        )
    }

    if (showRestartDialog) {
        AppRestartDialog(
            onDismiss = {
                showRestartDialog = false
                restartRequiresArtworkRescan = false
            },
            onRestart = {
                val shouldRefreshLibrary = restartRequiresArtworkRescan
                showRestartDialog = false
                restartRequiresArtworkRescan = false
                scope.launch {
                    if (shouldRefreshLibrary) {
                        try {
                            chromahub.rhythm.app.util.CacheManager.clearAllCache(context, null)
                            appSettings.requestFullMediaRescanOnNextLaunch(reason = "library_artwork_settings_restart")
                        } catch (e: Exception) {
                            Log.e("CacheManagement", "Error clearing cache before artwork settings restart", e)
                        }
                    }
                    chromahub.rhythm.app.util.AppRestarter.restartApp(context)
                }
            },
            onContinue = {
                showRestartDialog = false
                restartRequiresArtworkRescan = false
            },
            message = restartDialogMessage
        )
    }
}

@Composable
fun RhythmGuardSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val haptic = LocalHapticFeedback.current

    val auraMode by appSettings.rhythmGuardMode.collectAsState()
    val auraAge by appSettings.rhythmGuardAge.collectAsState()
    val manualWarningsEnabled by appSettings.rhythmGuardManualWarningsEnabled.collectAsState()
    val manualVolumeThreshold by appSettings.rhythmGuardManualVolumeThreshold.collectAsState()
    val alertThresholdMinutes by appSettings.rhythmGuardAlertThresholdMinutes.collectAsState()
    val warningTimeoutMinutes by appSettings.rhythmGuardWarningTimeoutMinutes.collectAsState()
    val postTimeoutCooldownMinutes by appSettings.rhythmGuardPostTimeoutCooldownMinutes.collectAsState()
    val breakResumeMinutes by appSettings.rhythmGuardBreakResumeMinutes.collectAsState()
    val timeoutUntilMs by appSettings.rhythmGuardTimeoutUntilMs.collectAsState()
    val timeoutStartedAtMs by appSettings.rhythmGuardTimeoutStartedAtMs.collectAsState()
    val timeoutCooldownUntilMs by appSettings.rhythmGuardTimeoutCooldownUntilMs.collectAsState()

    val dailyListeningStats by appSettings.dailyListeningStats.collectAsState()

    val stopPlaybackOnZeroVolume by appSettings.stopPlaybackOnZeroVolume.collectAsState()
    val rhythmGuardApplyVolumeLimitOnSpeaker by appSettings.rhythmGuardApplyVolumeLimitOnSpeaker.collectAsState()

    val currentSystemVolume = rememberSystemMusicVolumeFraction(context)
    val playbackStatsRepository = remember(context) { PlaybackStatsRepository.getInstance(context) }

    var todayExposureMs by remember { mutableLongStateOf(0L) }
    var weeklyAverageSessions by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(dailyListeningStats) {
        val todaySummary = runCatching {
            playbackStatsRepository.loadSummary(StatsTimeRange.TODAY)
        }.getOrNull()
        val weekSummary = runCatching {
            playbackStatsRepository.loadSummary(StatsTimeRange.WEEK)
        }.getOrNull()

        todayExposureMs = todaySummary?.totalDurationMs ?: 0L
        weeklyAverageSessions = weekSummary?.averageSessionsPerDay
            ?: rhythmGuardWeeklyAverageSessions(dailyListeningStats)
    }

    val nowEpochMs by produceState(
        initialValue = System.currentTimeMillis(),
        key1 = timeoutUntilMs,
        key2 = timeoutCooldownUntilMs
    ) {
        while (true) {
            value = System.currentTimeMillis()
            val timeoutActive = timeoutUntilMs > value
            val cooldownActive = timeoutCooldownUntilMs > value
            if (!timeoutActive && !cooldownActive) break
            delay(1000L)
        }
    }

    val activePolicy = remember(auraAge) { appSettings.getRhythmGuardPolicy(auraAge) }
    val policyTable = remember { appSettings.getRhythmGuardPolicyBands() }
    val isRhythmGuardEnabled = auraMode != AppSettings.RHYTHM_GUARD_MODE_OFF
    val recommendedVolumeThreshold = activePolicy.maxVolumeThreshold
    val recommendedDailyMinutes = activePolicy.recommendedDailyMinutes
    val effectiveExposureLimitMinutes = if (auraMode == AppSettings.RHYTHM_GUARD_MODE_AUTO) {
        recommendedDailyMinutes
    } else if (alertThresholdMinutes > 0) {
        alertThresholdMinutes
    } else {
        recommendedDailyMinutes
    }
    val totalExposureMinutes = (todayExposureMs / 60000L).toInt().coerceAtLeast(0)

    val currentVolumePercent = (currentSystemVolume * 100f).toInt().coerceIn(0, 100)
    val manualThresholdPercent = (manualVolumeThreshold * 100f).toInt().coerceIn(0, 100)
    val recommendedThresholdPercent = (recommendedVolumeThreshold * 100f).toInt().coerceIn(0, 100)
    val formattedTotalExposure = remember(todayExposureMs) {
        rhythmGuardFormatDurationFromMillis(todayExposureMs)
    }
    val formattedDailyTarget = remember(effectiveExposureLimitMinutes) {
        rhythmGuardFormatDurationFromMinutes(effectiveExposureLimitMinutes)
    }
    val formattedTimeout = remember(warningTimeoutMinutes) {
        rhythmGuardFormatDurationFromMinutes(warningTimeoutMinutes)
    }
    val formattedPostTimeoutCooldown = remember(postTimeoutCooldownMinutes) {
        rhythmGuardFormatDurationFromMinutes(postTimeoutCooldownMinutes)
    }
    val formattedResumeInterval = remember(breakResumeMinutes) {
        rhythmGuardFormatDurationFromMinutes(breakResumeMinutes)
    }
    val activeVolumeThreshold = if (auraMode == AppSettings.RHYTHM_GUARD_MODE_AUTO) {
        recommendedVolumeThreshold
    } else {
        manualVolumeThreshold
    }
    val activeThresholdPercent = if (auraMode == AppSettings.RHYTHM_GUARD_MODE_AUTO) {
        recommendedThresholdPercent
    } else {
        manualThresholdPercent
    }
    val isTimeoutActive = isRhythmGuardEnabled && timeoutUntilMs > nowEpochMs
    val isCooldownActive = isRhythmGuardEnabled && !isTimeoutActive && timeoutCooldownUntilMs > nowEpochMs
    val timeoutRemainingSeconds = ((timeoutUntilMs - nowEpochMs) / 1000L).coerceAtLeast(0L)
    val cooldownRemainingSeconds = ((timeoutCooldownUntilMs - nowEpochMs) / 1000L).coerceAtLeast(0L)

    val timeoutStartFallbackMs = timeoutUntilMs - breakResumeMinutes.coerceIn(1, 180).toLong() * 60_000L
    val resolvedTimeoutStartMs = timeoutStartedAtMs
        .takeIf { it > 0L && it < timeoutUntilMs }
        ?: timeoutStartFallbackMs
    val timeoutTotalMs = (timeoutUntilMs - resolvedTimeoutStartMs).coerceAtLeast(1_000L)
    val timeoutElapsedMs = (timeoutTotalMs - (timeoutUntilMs - nowEpochMs).coerceAtLeast(0L))
        .coerceIn(0L, timeoutTotalMs)
    val timeoutProgress = (timeoutElapsedMs.toFloat() / timeoutTotalMs.toFloat()).coerceIn(0f, 1f)

    val cooldownTotalMs = postTimeoutCooldownMinutes.coerceIn(1, 60).toLong() * 60_000L
    val cooldownElapsedMs = (cooldownTotalMs - (timeoutCooldownUntilMs - nowEpochMs).coerceAtLeast(0L))
        .coerceIn(0L, cooldownTotalMs)
    val cooldownProgress = (cooldownElapsedMs.toFloat() / cooldownTotalMs.toFloat()).coerceIn(0f, 1f)

    val showVolumeWarning = isRhythmGuardEnabled &&
        auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL &&
        manualWarningsEnabled &&
        currentSystemVolume > manualVolumeThreshold
    val showExposureWarning = isRhythmGuardEnabled &&
        auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL &&
        manualWarningsEnabled &&
        totalExposureMinutes > effectiveExposureLimitMinutes
    val safetySnapshot = remember(
        isRhythmGuardEnabled,
        auraMode,
        manualWarningsEnabled,
        currentSystemVolume,
        activeVolumeThreshold,
        totalExposureMinutes,
        effectiveExposureLimitMinutes,
        weeklyAverageSessions,
        isTimeoutActive,
        isCooldownActive,
        cooldownProgress
    ) {
        rhythmGuardCalculateSafetySnapshot(
            isEnabled = isRhythmGuardEnabled,
            isManualMode = auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL,
            manualWarningsEnabled = manualWarningsEnabled,
            currentVolumeFraction = currentSystemVolume,
            safeVolumeThresholdFraction = activeVolumeThreshold,
            exposureMinutes = totalExposureMinutes,
            exposureLimitMinutes = effectiveExposureLimitMinutes,
            weeklyAverageSessions = weeklyAverageSessions,
            timeoutActive = isTimeoutActive,
            cooldownActive = isCooldownActive,
            cooldownProgress = cooldownProgress
        )
    }
    val healthRiskScore = safetySnapshot.riskScore
    val safetyScorePercent = (safetySnapshot.safetyProgress * 100f).toInt().coerceIn(0, 100)
    val overallHealthLevel = when {
        !isRhythmGuardEnabled -> RhythmGuardOverallHealthLevel.OFF
        isTimeoutActive -> RhythmGuardOverallHealthLevel.TIMEOUT
        isCooldownActive -> RhythmGuardOverallHealthLevel.COOLDOWN
        healthRiskScore < 0.40f -> RhythmGuardOverallHealthLevel.GOOD
        healthRiskScore < 0.72f -> RhythmGuardOverallHealthLevel.FAIR
        else -> RhythmGuardOverallHealthLevel.RISK
    }
    val overallHealthProgress = when {
        !isRhythmGuardEnabled -> 0f
        isTimeoutActive -> timeoutProgress
        isCooldownActive -> cooldownProgress
        else -> safetySnapshot.safetyProgress
    }
    val guardStatusText = when {
        !isRhythmGuardEnabled -> context.getString(R.string.settings_rhythm_guard_state_inactive)
        isTimeoutActive -> context.getString(R.string.settings_rhythm_guard_state_timeout_active)
        isCooldownActive -> context.getString(R.string.settings_rhythm_guard_state_cooldown_active)
        else -> context.getString(R.string.settings_rhythm_guard_state_active)
    }
    val guardStatusDetail = when {
        isTimeoutActive -> context.getString(
            R.string.settings_rhythm_guard_state_timeout_remaining,
            rhythmGuardFormatCountdownFromSeconds(timeoutRemainingSeconds)
        )
        isCooldownActive -> context.getString(
            R.string.settings_rhythm_guard_state_cooldown_remaining,
            rhythmGuardFormatCountdownFromSeconds(cooldownRemainingSeconds)
        )
        isRhythmGuardEnabled -> context.getString(
            R.string.settings_rhythm_guard_state_safety_score,
            safetyScorePercent
        )
        else -> null
    }
    val guardStatusAccentColor = when {
        isCooldownActive -> Color(0xFF1565C0)
        isTimeoutActive -> MaterialTheme.colorScheme.error
        isRhythmGuardEnabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val activeManualPreset = remember(
        manualVolumeThreshold,
        alertThresholdMinutes,
        warningTimeoutMinutes,
        postTimeoutCooldownMinutes,
        breakResumeMinutes,
        manualWarningsEnabled
    ) {
        rhythmGuardResolveProtectionPreset(
            manualVolumeThreshold = manualVolumeThreshold,
            alertThresholdMinutes = alertThresholdMinutes,
            warningTimeoutMinutes = warningTimeoutMinutes,
            postTimeoutCooldownMinutes = postTimeoutCooldownMinutes,
            breakResumeMinutes = breakResumeMinutes,
            manualWarningsEnabled = manualWarningsEnabled
        )
    }
    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_rhythm_guard),
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
            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                RhythmGuardOverallHealthCard(
                    level = overallHealthLevel,
                    progress = overallHealthProgress,
                    statusText = guardStatusText,
                    statusDetail = guardStatusDetail,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RhythmGuardHeroCard(
                        title = context.getString(R.string.settings_rhythm_guard_snapshot_exposure_title),
                        value = formattedTotalExposure,
                        subtitle = "/$formattedDailyTarget",
                        progress = (totalExposureMinutes / maxOf(effectiveExposureLimitMinutes, 1).toFloat()).coerceIn(0f, 1f),
                        icon = RhythmIcons.AccessTime,
                        isWarning = showExposureWarning,
                        modifier = Modifier.weight(1f)
                    )

                    RhythmGuardHeroCard(
                        title = context.getString(R.string.settings_rhythm_guard_snapshot_volume_title),
                        value = "$currentVolumePercent%",
                        subtitle = "of ${activeThresholdPercent}%",
                        progress = (currentSystemVolume / maxOf(activeVolumeThreshold, 0.01f)).coerceIn(0f, 1f),
                        icon = MaterialSymbolIcon("graphic_eq"),
                        isWarning = showVolumeWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(34.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                tonalElevation = 0.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Security,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = guardStatusAccentColor
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = context.getString(R.string.settings_rhythm_guard_mode_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = context.getString(R.string.settings_rhythm_guard_mode_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = guardStatusText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = guardStatusAccentColor
                                )
                            }

                            TunerAnimatedSwitch(
                                checked = isRhythmGuardEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        val restoredMode = if (auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL) {
                                            AppSettings.RHYTHM_GUARD_MODE_MANUAL
                                        } else {
                                            AppSettings.RHYTHM_GUARD_MODE_AUTO
                                        }
                                        appSettings.setRhythmGuardMode(restoredMode)
                                    } else {
                                        appSettings.setRhythmGuardMode(AppSettings.RHYTHM_GUARD_MODE_OFF)
                                    }
                                }
                            )
                        }

                        if (isRhythmGuardEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))

                            ExpressiveButtonGroup(
                                items = listOf(
                                    context.getString(R.string.settings_rhythm_guard_mode_auto),
                                    context.getString(R.string.settings_rhythm_guard_mode_manual)
                                ),
                                selectedIndex = if (auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL) 1 else 0,
                                onItemClick = { index ->
                                    when (index) {
                                        0 -> appSettings.setRhythmGuardMode(AppSettings.RHYTHM_GUARD_MODE_AUTO)
                                        else -> appSettings.setRhythmGuardMode(AppSettings.RHYTHM_GUARD_MODE_MANUAL)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            if (isRhythmGuardEnabled) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = context.getString(R.string.settings_rhythm_guard_age_label, auraAge),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = context.getString(
                                        R.string.settings_rhythm_guard_age_desc,
                                        recommendedThresholdPercent,
                                        recommendedDailyMinutes
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Slider(
                            value = auraAge.toFloat(),
                            onValueChange = { appSettings.setRhythmGuardAge(it.toInt()) },
                            valueRange = 8f..80f,
                            steps = 71
                        )
                    }
                }
            }

            item {
                val outputSettingItems = listOf(
                    SettingItem(
                        RhythmIcons.Speaker,
                        context.getString(R.string.settings_rhythm_guard_device_controls_speaker_limit_title),
                        context.getString(R.string.settings_rhythm_guard_device_controls_speaker_limit_desc),
                        toggleState = rhythmGuardApplyVolumeLimitOnSpeaker,
                        onToggleChange = { appSettings.setRhythmGuardApplyVolumeLimitOnSpeaker(it) }
                    )
                )

                val materialItems = outputSettingItems.map { item ->
                    toMaterial3SettingsItem(context = context, item = item, hapticFeedback = haptic)
                }

                Material3SettingsGroup(
                    title = context.getString(R.string.settings_rhythm_guard_device_controls_title),
                    items = materialItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            if (auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL) {
                item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.settings_rhythm_guard_alert_controls_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Threshold control
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = context.getString(
                                    R.string.settings_rhythm_guard_alert_threshold_title,
                                    if (alertThresholdMinutes > 0) {
                                        rhythmGuardFormatDurationFromMinutes(alertThresholdMinutes)
                                    } else {
                                        context.getString(R.string.settings_rhythm_guard_alert_threshold_policy_default)
                                    }
                                ),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(-1, 60, 90, 120).forEach { option ->
                                    FilterChip(
                                        selected = alertThresholdMinutes == option,
                                        onClick = { appSettings.setRhythmGuardAlertThresholdMinutes(option) },
                                        label = {
                                            Text(
                                                if (option > 0) {
                                                    rhythmGuardFormatDurationFromMinutes(option)
                                                } else {
                                                    context.getString(R.string.settings_rhythm_guard_alert_threshold_policy_default)
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            Slider(
                                value = maxOf(alertThresholdMinutes, 15).toFloat(),
                                onValueChange = { appSettings.setRhythmGuardAlertThresholdMinutes(it.toInt()) },
                                valueRange = 15f..360f,
                                steps = 344
                            )
                        }

                        // Timeout control
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = context.getString(
                                    R.string.settings_rhythm_guard_alert_timeout_title,
                                    formattedTimeout
                                ),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2, 5, 10, 15).forEach { option ->
                                    FilterChip(
                                        selected = warningTimeoutMinutes == option,
                                        onClick = { appSettings.setRhythmGuardWarningTimeoutMinutes(option) },
                                        label = { Text(rhythmGuardFormatDurationFromMinutes(option)) }
                                    )
                                }
                            }
                            Slider(
                                value = warningTimeoutMinutes.toFloat(),
                                onValueChange = { appSettings.setRhythmGuardWarningTimeoutMinutes(it.toInt()) },
                                valueRange = 1f..30f,
                                steps = 28
                            )
                        }

                        // Post-timeout cooldown control
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = context.getString(
                                    R.string.settings_rhythm_guard_post_timeout_cooldown_title,
                                    formattedPostTimeoutCooldown
                                ),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(3, 5, 10, 15).forEach { option ->
                                    FilterChip(
                                        selected = postTimeoutCooldownMinutes == option,
                                        onClick = { appSettings.setRhythmGuardPostTimeoutCooldownMinutes(option) },
                                        label = { Text(rhythmGuardFormatDurationFromMinutes(option)) }
                                    )
                                }
                            }
                            Slider(
                                value = postTimeoutCooldownMinutes.toFloat(),
                                onValueChange = { appSettings.setRhythmGuardPostTimeoutCooldownMinutes(it.toInt()) },
                                valueRange = 1f..30f,
                                steps = 28
                            )
                        }

                        // Break interval control
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = context.getString(
                                    R.string.settings_rhythm_guard_break_resume_default_title,
                                    formattedResumeInterval
                                ),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(10, 15, 30, 60).forEach { option ->
                                    FilterChip(
                                        selected = breakResumeMinutes == option,
                                        onClick = { appSettings.setRhythmGuardBreakResumeMinutes(option) },
                                        label = { Text(rhythmGuardFormatDurationFromMinutes(option)) }
                                    )
                                }
                            }
                            Slider(
                                value = breakResumeMinutes.toFloat(),
                                onValueChange = { appSettings.setRhythmGuardBreakResumeMinutes(it.toInt()) },
                                valueRange = 1f..120f,
                                steps = 118
                            )
                        }

                        // Manual protection presets (quick multi-setting tunes)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = context.getString(R.string.settings_rhythm_guard_protection_presets_title),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = context.getString(R.string.settings_rhythm_guard_protection_presets_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    RhythmGuardProtectionPreset.GENTLE,
                                    RhythmGuardProtectionPreset.BALANCED,
                                    RhythmGuardProtectionPreset.STRICT
                                ).forEach { preset ->
                                    FilterChip(
                                        selected = activeManualPreset == preset,
                                        onClick = {
                                            val values = rhythmGuardPresetValues(preset)
                                            appSettings.setRhythmGuardManualWarningsEnabled(true)
                                            appSettings.setRhythmGuardManualVolumeThreshold(values.volumeThreshold)
                                            appSettings.setRhythmGuardAlertThresholdMinutes(values.alertThresholdMinutes)
                                            appSettings.setRhythmGuardWarningTimeoutMinutes(values.warningTimeoutMinutes)
                                            appSettings.setRhythmGuardPostTimeoutCooldownMinutes(values.postTimeoutCooldownMinutes)
                                            appSettings.setRhythmGuardBreakResumeMinutes(values.breakResumeMinutes)
                                        },
                                        label = {
                                            Text(
                                                text = when (preset) {
                                                    RhythmGuardProtectionPreset.GENTLE -> context.getString(R.string.settings_rhythm_guard_protection_preset_gentle)
                                                    RhythmGuardProtectionPreset.BALANCED -> context.getString(R.string.settings_rhythm_guard_protection_preset_balanced)
                                                    RhythmGuardProtectionPreset.STRICT -> context.getString(R.string.settings_rhythm_guard_protection_preset_strict)
                                                    RhythmGuardProtectionPreset.CUSTOM -> context.getString(R.string.settings_rhythm_guard_protection_preset_custom)
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            if (auraMode == AppSettings.RHYTHM_GUARD_MODE_AUTO) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = context.getString(R.string.settings_rhythm_guard_auto_policy_table_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            policyTable.forEachIndexed { index, band ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = context.getString(
                                            R.string.settings_rhythm_guard_auto_policy_band,
                                            band.minAge,
                                            band.maxAge
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = context.getString(
                                            R.string.settings_rhythm_guard_auto_policy_value,
                                            (band.maxVolumeThreshold * 100f).toInt(),
                                            band.recommendedDailyMinutes
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (auraAge in band.minAge..band.maxAge) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontWeight = if (auraAge in band.minAge..band.maxAge) {
                                            FontWeight.SemiBold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                }
                                if (index < policyTable.lastIndex) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL) {
                item {
                    val manualSettingItems = listOf(
                        SettingItem(
                            RhythmIcons.Warning,
                            context.getString(R.string.settings_rhythm_guard_manual_warning_toggle),
                            context.getString(R.string.settings_rhythm_guard_manual_warning_toggle_desc),
                            toggleState = manualWarningsEnabled,
                            onToggleChange = { appSettings.setRhythmGuardManualWarningsEnabled(it) }
                        ),
                        SettingItem(
                            RhythmIcons.Stop,
                            context.getString(R.string.settings_stop_playback_on_zero_volume),
                            context.getString(R.string.settings_stop_playback_on_zero_volume_desc),
                            toggleState = stopPlaybackOnZeroVolume,
                            onToggleChange = { appSettings.setStopPlaybackOnZeroVolume(it) }
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = context.getString(R.string.settings_rhythm_guard_manual_threshold_title, manualThresholdPercent),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = context.getString(R.string.settings_rhythm_guard_manual_threshold_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Slider(
                                    value = manualVolumeThreshold,
                                    onValueChange = { appSettings.setRhythmGuardManualVolumeThreshold(it) },
                                    valueRange = 0.40f..0.95f
                                )
                            }
                        }

                        val materialItems = manualSettingItems.map { item ->
                            toMaterial3SettingsItem(context = context, item = item, hapticFeedback = haptic)
                        }

                        Material3SettingsGroup(
                            title = context.getString(R.string.settings_rhythm_guard_manual_controls_title),
                            items = materialItems,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    }
                }
            }

            if (showVolumeWarning || showExposureWarning) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = context.getString(R.string.settings_rhythm_guard_warning_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when {
                                    showExposureWarning -> context.getString(
                                        R.string.settings_rhythm_guard_warning_daily_exposure,
                                        formattedTotalExposure,
                                        formattedDailyTarget
                                    )
                                    else -> context.getString(
                                        R.string.settings_rhythm_guard_warning_high_volume,
                                        activeThresholdPercent
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

private fun rhythmGuardWeeklyAverageSessions(stats: Map<String, Long>): Float {
    if (stats.isEmpty()) return 0f
    val recentDays = stats.toList()
        .sortedByDescending { it.first }
        .take(7)
        .map { it.second }

    if (recentDays.isEmpty()) return 0f
    return recentDays.average().toFloat()
}

internal data class RhythmGuardSafetySnapshot(
    val riskScore: Float,
    val safetyProgress: Float
)

internal fun rhythmGuardCalculateSafetySnapshot(
    isEnabled: Boolean,
    isManualMode: Boolean,
    manualWarningsEnabled: Boolean,
    currentVolumeFraction: Float,
    safeVolumeThresholdFraction: Float,
    exposureMinutes: Int,
    exposureLimitMinutes: Int,
    weeklyAverageSessions: Float,
    timeoutActive: Boolean,
    cooldownActive: Boolean,
    cooldownProgress: Float
): RhythmGuardSafetySnapshot {
    if (!isEnabled) {
        return RhythmGuardSafetySnapshot(riskScore = 0f, safetyProgress = 0f)
    }

    if (timeoutActive) {
        return RhythmGuardSafetySnapshot(riskScore = 1f, safetyProgress = 0f)
    }

    val safeVolumeThreshold = rhythmGuardSanitizeFloat(safeVolumeThresholdFraction, fallback = 0.01f)
        .coerceIn(0.01f, 1f)
    val safeCurrentVolume = rhythmGuardSanitizeFloat(currentVolumeFraction)
    val safeExposureMinutes = exposureMinutes.coerceAtLeast(0)
    val safeExposureLimit = exposureLimitMinutes.coerceAtLeast(1)
    val safeWeeklySessions = rhythmGuardSanitizeFloat(weeklyAverageSessions)

    val volumeRatio = safeCurrentVolume / safeVolumeThreshold
    val exposureRatio = safeExposureMinutes.toFloat() / safeExposureLimit.toFloat()
    val sessionRatio = safeWeeklySessions / 8f

    val volumePressure = rhythmGuardNormalizePressure(volumeRatio)
    val exposurePressure = rhythmGuardNormalizePressure(exposureRatio)
    val sessionPressure = rhythmGuardNormalizePressure(sessionRatio)

    var riskScore = (
        (volumePressure * 0.40f) +
            (exposurePressure * 0.42f) +
            (sessionPressure * 0.18f)
        ).coerceIn(0f, 1f)

    if (isManualMode && !manualWarningsEnabled) {
        riskScore = (riskScore + 0.07f).coerceIn(0f, 1f)
    }

    if (cooldownActive) {
        val recoveryProgress = rhythmGuardSanitizeFloat(cooldownProgress).coerceIn(0f, 1f)
        val decay = 0.82f - (recoveryProgress * 0.30f)
        riskScore = (riskScore * decay).coerceIn(0.18f, 0.70f)
    }

    val safetyProgress = (1f - riskScore).coerceIn(0f, 1f)
    return RhythmGuardSafetySnapshot(riskScore = riskScore, safetyProgress = safetyProgress)
}

private fun rhythmGuardSanitizeFloat(value: Float, fallback: Float = 0f): Float {
    return if (value.isFinite()) value.coerceAtLeast(0f) else fallback
}

private fun rhythmGuardNormalizePressure(ratio: Float): Float {
    val safeRatio = rhythmGuardSanitizeFloat(ratio)
    return when {
        safeRatio <= 0f -> 0f
        safeRatio <= 0.6f -> (safeRatio / 0.6f) * 0.32f
        safeRatio <= 1f -> 0.32f + ((safeRatio - 0.6f) / 0.4f) * 0.24f
        safeRatio <= 1.5f -> 0.56f + ((safeRatio - 1f) / 0.5f) * 0.29f
        safeRatio <= 2f -> 0.85f + ((safeRatio - 1.5f) / 0.5f) * 0.10f
        else -> 0.95f + ((safeRatio - 2f) / 2f) * 0.05f
    }.coerceIn(0f, 1f)
}

@Composable
private fun rememberSystemMusicVolumeFraction(context: Context): Float {
    var systemVolume by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

        fun refreshVolume() {
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            systemVolume = if (maxVolume > 0) currentVolume.toFloat() / maxVolume.toFloat() else 0f
        }

        refreshVolume()

        val observer = object : android.database.ContentObserver(
            android.os.Handler(android.os.Looper.getMainLooper())
        ) {
            override fun onChange(selfChange: Boolean) {
                refreshVolume()
            }
        }

        context.contentResolver.registerContentObserver(
            android.provider.Settings.System.CONTENT_URI,
            true,
            observer
        )

        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    return systemVolume
}

private fun rhythmGuardFormatDurationFromMinutes(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    val days = safeMinutes / (24 * 60)
    val hours = (safeMinutes % (24 * 60)) / 60
    val mins = safeMinutes % 60

    return when {
        days > 0 && hours > 0 && mins > 0 -> "${days}d ${hours}h ${mins}m"
        days > 0 && hours > 0 -> "${days}d ${hours}h"
        days > 0 && mins > 0 -> "${days}d ${mins}m"
        days > 0 -> "${days}d"
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

private fun rhythmGuardFormatDurationFromMillis(durationMs: Long): String {
    return rhythmGuardFormatDurationFromMinutes((durationMs / 60000L).toInt())
}

private fun rhythmGuardFormatCountdownFromSeconds(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0L)
    val hours = safeSeconds / 3600L
    val minutes = (safeSeconds % 3600L) / 60L
    val secs = safeSeconds % 60L

    return if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

@Composable
private fun RhythmGuardOverviewGauge(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    progress: Float,
    isWarning: Boolean,
    icon: MaterialSymbolIcon
) {
    val progressValue = progress.coerceIn(0f, 1f)
    val progressPercent = (progressValue * 100f).toInt()
    val containerColor = if (isWarning) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (isWarning) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = contentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isWarning) "Risk" else "Safe",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            StyledProgressBar(
                progress = progressValue,
                style = ProgressStyle.WAVY,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                progressColor = if (isWarning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = contentColor.copy(alpha = 0.16f),
                isPlaying = true,
                showThumb = false,
                waveAmplitudeWhenPlaying = 2.5.dp,
                waveLength = 80.dp,
                height = 4.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isWarning) {
                        stringResource(R.string.settings_rhythm_guard_snapshot_widget_above_limit)
                    } else {
                        stringResource(R.string.settings_rhythm_guard_snapshot_widget_within_limit)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun RhythmGuardMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    progress: Float,
    containerColor: Color,
    contentColor: Color
) {
    val progressValue = progress.coerceIn(0f, 1f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${(progressValue * 100f).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            StyledProgressBar(
                progress = progressValue,
                style = ProgressStyle.WAVY,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                progressColor = contentColor.copy(alpha = 0.78f),
                trackColor = contentColor.copy(alpha = 0.2f),
                isPlaying = true,
                showThumb = false,
                waveAmplitudeWhenPlaying = 2.5.dp,
                waveLength = 80.dp,
                height = 4.dp
            )

            Text(
                text = stringResource(R.string.settings_rhythm_guard_snapshot_widget_load_label),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.76f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RhythmGuardHeroCard(
    title: String,
    value: String,
    subtitle: String,
    progress: Float,
    icon: MaterialSymbolIcon,
    isWarning: Boolean,
    modifier: Modifier = Modifier
) {
    val progressValue = progress.coerceIn(0f, 1.5f)
    val visualLevel = when {
        isWarning || progressValue >= 1f -> RhythmGuardWidgetVisualLevel.CRITICAL
        progressValue >= 0.82f -> RhythmGuardWidgetVisualLevel.ELEVATED
        progressValue >= 0.56f -> RhythmGuardWidgetVisualLevel.WATCH
        else -> RhythmGuardWidgetVisualLevel.STABLE
    }
    val iconColor = when (visualLevel) {
        RhythmGuardWidgetVisualLevel.STABLE -> MaterialTheme.colorScheme.primary
        RhythmGuardWidgetVisualLevel.WATCH -> MaterialTheme.colorScheme.tertiary
        RhythmGuardWidgetVisualLevel.ELEVATED -> MaterialTheme.colorScheme.secondary
        RhythmGuardWidgetVisualLevel.CRITICAL -> MaterialTheme.colorScheme.error
    }
    val containerColor = when (visualLevel) {
        RhythmGuardWidgetVisualLevel.STABLE -> MaterialTheme.colorScheme.primaryContainer
        RhythmGuardWidgetVisualLevel.WATCH -> MaterialTheme.colorScheme.tertiaryContainer
        RhythmGuardWidgetVisualLevel.ELEVATED -> MaterialTheme.colorScheme.secondaryContainer
        RhythmGuardWidgetVisualLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (visualLevel) {
        RhythmGuardWidgetVisualLevel.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
        RhythmGuardWidgetVisualLevel.ELEVATED -> MaterialTheme.colorScheme.onSecondaryContainer
        RhythmGuardWidgetVisualLevel.WATCH -> MaterialTheme.colorScheme.onTertiaryContainer
        RhythmGuardWidgetVisualLevel.STABLE -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val dynamicValueFontSize = rememberRhythmGuardHeroValueFontSize(value)

    Column(
        modifier = modifier
            .height(184.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = iconColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = contentColor.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = dynamicValueFontSize,
                lineHeight = (dynamicValueFontSize.value + 3f).sp
            ),
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.7f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        StyledProgressBar(
            progress = progress.coerceIn(0f, 1f),
            style = ProgressStyle.WAVY,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            progressColor = iconColor,
            trackColor = contentColor.copy(alpha = 0.2f),
            isPlaying = true,
            showThumb = false,
            waveAmplitudeWhenPlaying = 2.5.dp,
            waveLength = 80.dp,
            height = 3.dp
        )
    }
}

@Composable
private fun rememberRhythmGuardHeroValueFontSize(value: String): TextUnit {
    val targetSize = when {
        value.length <= 5 -> 34.sp
        value.length <= 8 -> 30.sp
        value.length <= 11 -> 26.sp
        value.length <= 15 -> 22.sp
        else -> 20.sp
    }
    val animatedSize by animateFloatAsState(
        targetValue = targetSize.value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rhythm_guard_hero_value_font_size"
    )
    return animatedSize.sp
}

private enum class RhythmGuardOverallHealthLevel {
    OFF,
    GOOD,
    FAIR,
    RISK,
    COOLDOWN,
    TIMEOUT
}

private enum class RhythmGuardProtectionPreset {
    GENTLE,
    BALANCED,
    STRICT,
    CUSTOM
}

private data class RhythmGuardProtectionPresetValues(
    val volumeThreshold: Float,
    val alertThresholdMinutes: Int,
    val warningTimeoutMinutes: Int,
    val postTimeoutCooldownMinutes: Int,
    val breakResumeMinutes: Int
)

private fun rhythmGuardPresetValues(preset: RhythmGuardProtectionPreset): RhythmGuardProtectionPresetValues {
    return when (preset) {
        RhythmGuardProtectionPreset.GENTLE -> RhythmGuardProtectionPresetValues(
            volumeThreshold = 0.80f,
            alertThresholdMinutes = 120,
            warningTimeoutMinutes = 10,
            postTimeoutCooldownMinutes = 5,
            breakResumeMinutes = 10
        )
        RhythmGuardProtectionPreset.BALANCED -> RhythmGuardProtectionPresetValues(
            volumeThreshold = 0.68f,
            alertThresholdMinutes = 90,
            warningTimeoutMinutes = 5,
            postTimeoutCooldownMinutes = 10,
            breakResumeMinutes = 15
        )
        RhythmGuardProtectionPreset.STRICT -> RhythmGuardProtectionPresetValues(
            volumeThreshold = 0.58f,
            alertThresholdMinutes = 60,
            warningTimeoutMinutes = 3,
            postTimeoutCooldownMinutes = 15,
            breakResumeMinutes = 20
        )
        RhythmGuardProtectionPreset.CUSTOM -> RhythmGuardProtectionPresetValues(
            volumeThreshold = 0.68f,
            alertThresholdMinutes = 90,
            warningTimeoutMinutes = 5,
            postTimeoutCooldownMinutes = 10,
            breakResumeMinutes = 15
        )
    }
}

private fun rhythmGuardResolveProtectionPreset(
    manualVolumeThreshold: Float,
    alertThresholdMinutes: Int,
    warningTimeoutMinutes: Int,
    postTimeoutCooldownMinutes: Int,
    breakResumeMinutes: Int,
    manualWarningsEnabled: Boolean
): RhythmGuardProtectionPreset {
    if (!manualWarningsEnabled) {
        return RhythmGuardProtectionPreset.CUSTOM
    }

    return when {
        rhythmGuardMatchesPreset(
            manualVolumeThreshold,
            alertThresholdMinutes,
            warningTimeoutMinutes,
            postTimeoutCooldownMinutes,
            breakResumeMinutes,
            rhythmGuardPresetValues(RhythmGuardProtectionPreset.GENTLE)
        ) -> RhythmGuardProtectionPreset.GENTLE

        rhythmGuardMatchesPreset(
            manualVolumeThreshold,
            alertThresholdMinutes,
            warningTimeoutMinutes,
            postTimeoutCooldownMinutes,
            breakResumeMinutes,
            rhythmGuardPresetValues(RhythmGuardProtectionPreset.BALANCED)
        ) -> RhythmGuardProtectionPreset.BALANCED

        rhythmGuardMatchesPreset(
            manualVolumeThreshold,
            alertThresholdMinutes,
            warningTimeoutMinutes,
            postTimeoutCooldownMinutes,
            breakResumeMinutes,
            rhythmGuardPresetValues(RhythmGuardProtectionPreset.STRICT)
        ) -> RhythmGuardProtectionPreset.STRICT

        else -> RhythmGuardProtectionPreset.CUSTOM
    }
}

private fun rhythmGuardMatchesPreset(
    manualVolumeThreshold: Float,
    alertThresholdMinutes: Int,
    warningTimeoutMinutes: Int,
    postTimeoutCooldownMinutes: Int,
    breakResumeMinutes: Int,
    preset: RhythmGuardProtectionPresetValues
): Boolean {
    return kotlin.math.abs(manualVolumeThreshold - preset.volumeThreshold) <= 0.015f &&
        alertThresholdMinutes == preset.alertThresholdMinutes &&
        warningTimeoutMinutes == preset.warningTimeoutMinutes &&
        postTimeoutCooldownMinutes == preset.postTimeoutCooldownMinutes &&
        breakResumeMinutes == preset.breakResumeMinutes
}

private enum class RhythmGuardWidgetVisualLevel {
    STABLE,
    WATCH,
    ELEVATED,
    CRITICAL
}

@Composable
private fun RhythmGuardOverallHealthCard(
    level: RhythmGuardOverallHealthLevel,
    progress: Float,
    statusText: String,
    statusDetail: String?,
    modifier: Modifier = Modifier
) {
    val statusGreen = Color(0xFF2E7D32)
    val statusOrange = Color(0xFFED6C02)
    val statusRed = Color(0xFFC62828)
    val statusBlue = Color(0xFF1565C0)
    val indicatorColor = when (level) {
        RhythmGuardOverallHealthLevel.GOOD -> statusGreen
        RhythmGuardOverallHealthLevel.FAIR -> statusOrange
        RhythmGuardOverallHealthLevel.RISK -> statusRed
        RhythmGuardOverallHealthLevel.COOLDOWN -> statusBlue
        RhythmGuardOverallHealthLevel.TIMEOUT -> statusRed
        RhythmGuardOverallHealthLevel.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusContainerColor = when (level) {
        RhythmGuardOverallHealthLevel.GOOD -> statusGreen.copy(alpha = 0.14f)
        RhythmGuardOverallHealthLevel.FAIR -> statusOrange.copy(alpha = 0.16f)
        RhythmGuardOverallHealthLevel.RISK -> statusRed.copy(alpha = 0.16f)
        RhythmGuardOverallHealthLevel.COOLDOWN -> statusBlue.copy(alpha = 0.16f)
        RhythmGuardOverallHealthLevel.TIMEOUT -> statusRed.copy(alpha = 0.20f)
        RhythmGuardOverallHealthLevel.OFF -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val statusColor = when (level) {
        RhythmGuardOverallHealthLevel.GOOD -> statusGreen
        RhythmGuardOverallHealthLevel.FAIR -> statusOrange
        RhythmGuardOverallHealthLevel.RISK -> statusRed
        RhythmGuardOverallHealthLevel.COOLDOWN -> statusBlue
        RhythmGuardOverallHealthLevel.TIMEOUT -> statusRed
        RhythmGuardOverallHealthLevel.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rhythm_guard_overall_progress"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeWidth = 10.dp,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                color = indicatorColor,
                strokeWidth = 10.dp,
                trackColor = Color.Transparent
            )
            Icon(
                imageVector = RhythmIcons.Security,
                contentDescription = null,
                tint = indicatorColor,
                modifier = Modifier.size(46.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = statusContainerColor
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                color = statusColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        if (!statusDetail.isNullOrBlank()) {
            Text(
                text = statusDetail,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor.copy(alpha = 0.88f),
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
fun CacheManagementSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val musicViewModel: MusicViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Collect states
    val maxCacheSize by appSettings.maxCacheSize.collectAsState()
    val clearCacheOnExit by appSettings.clearCacheOnExit.collectAsState()

    // Local states
    var currentCacheSize by remember { mutableStateOf(0L) }
    var isCalculatingSize by remember { mutableStateOf(false) }
    var isClearingCache by remember { mutableStateOf(false) }
    var showCacheSizeDialog by remember { mutableStateOf(false) }
    var showClearCacheSuccess by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var restartDialogMessage by remember { mutableStateOf("") }
    var cacheDetails by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var isRebuildingRoom by remember { mutableStateOf(false) }
    var roomSongCount by remember { mutableStateOf(-1) }

    val refreshCacheStats: suspend () -> Unit = {
        isCalculatingSize = true
        try {
            val (totalCacheSize, detailedCacheStats) = kotlinx.coroutines.withContext(
                kotlinx.coroutines.Dispatchers.IO
            ) {
                Pair(
                    chromahub.rhythm.app.util.CacheManager.getCacheSize(context),
                    chromahub.rhythm.app.util.CacheManager.getDetailedCacheSize(context)
                )
            }
            currentCacheSize = totalCacheSize
            cacheDetails = detailedCacheStats
        } catch (e: Exception) {
            Log.e("CacheManagement", "Error calculating cache size", e)
        } finally {
            isCalculatingSize = false
        }
    }

    // Calculate cache size when the screen opens
    LaunchedEffect(Unit) {
        refreshCacheStats()
    }

    // Calculate storage backend stats
    LaunchedEffect(Unit) {
        try {
            val repo = musicViewModel.getMusicRepository()
            roomSongCount = try {
                repo.getRoomSongCount()
            } catch (_: Exception) { -1 }
        } catch (e: Exception) {
            Log.e("CacheManagement", "Error calculating storage stats", e)
        }
    }

    // Show cache size dialog
    if (showCacheSizeDialog) {
        CacheSizeDialog(
            currentSize = maxCacheSize,
            onDismiss = { showCacheSizeDialog = false },
            onSave = { size ->
                appSettings.setMaxCacheSize(size)
                showCacheSizeDialog = false
            }
        )
    }

    if (showRestartDialog) {
        AppRestartDialog(
            onDismiss = { showRestartDialog = false },
            onRestart = {
                showRestartDialog = false
                chromahub.rhythm.app.util.AppRestarter.restartApp(context)
            },
            onContinue = {
                showRestartDialog = false
            },
            message = restartDialogMessage
        )
    }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_cache),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Current Cache Status
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = MaterialSymbolIcon("pie_chart", filled = true),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.cache_current_status),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isCalculatingSize) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = context.getString(R.string.cache_calculating),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // Total cache size
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = context.getString(R.string.cache_total_size),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = chromahub.rhythm.app.util.CacheManager.formatBytes(currentCacheSize),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Cache breakdown
                            cacheDetails.forEach { (label, size) ->
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "  • $label:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = chromahub.rhythm.app.util.CacheManager.formatBytes(size),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Cache limit
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = context.getString(R.string.cache_limit),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format("%.1f", maxCacheSize / (1024f * 1024f))} MB",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Cache Settings
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = context.getString(R.string.settings_cache_settings),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Material3SettingsGroup(
                    items = listOf(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("data_usage", filled = true),
                                title = context.getString(R.string.cache_max_size),
                                description = "${String.format("%.1f", maxCacheSize / (1024f * 1024f))} MB",
                                onClick = { showCacheSizeDialog = true }
                            )
                        ),
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("auto_delete", filled = true),
                                title = context.getString(R.string.cache_clear_on_exit),
                                description = context.getString(R.string.settings_cache_clear_on_exit_desc),
                                toggleState = clearCacheOnExit,
                                onToggleChange = { appSettings.setClearCacheOnExit(it) }
                            )
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // Cache Actions
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = context.getString(R.string.settings_cache_actions),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Material3SettingsGroup(
                    items = listOf(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = RhythmIcons.MusicNote,
                                title = context.getString(R.string.settings_clear_lyrics_cache),
                                description = context.getString(R.string.settings_clear_lyrics_cache_desc),
                                onClick = {
                                    scope.launch {
                                        try {
                                            isClearingCache = true
                                            musicViewModel.clearLyricsCacheAndRefetch()
                                            refreshCacheStats()
                                            Toast.makeText(context, context.getString(R.string.settings_lyrics_cache_cleared), Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Log.e("CacheManagement", "Error clearing lyrics cache", e)
                                            Toast.makeText(context, context.getString(R.string.settings_lyrics_cache_clear_failed), Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isClearingCache = false
                                        }
                                    }
                                }
                            )
                        ),
                        Material3SettingsItem(
                            icon = MaterialSymbolIcon("delete_sweep", filled = true),
                            title = { Text(context.getString(R.string.settings_clear_all_cache)) },
                            description = { Text(context.getString(R.string.settings_clear_all_cache_desc)) },
                            trailingContent = {
                                if (isClearingCache) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Icon(
                                        imageVector = MaterialSymbolIcon("arrow_forward_ios", filled = true),
                                        contentDescription = context.getString(R.string.cd_navigate),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                scope.launch {
                                    try {
                                        isClearingCache = true
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            trimArtworkCacheNow(context.cacheDir)
                                        }
                                        chromahub.rhythm.app.util.CacheManager.clearAllCache(context, null)
                                        musicViewModel.getMusicRepository().clearInMemoryCaches()
                                        musicViewModel.getMusicRepository().clearSongCacheData()
                                        refreshCacheStats()
                                        showClearCacheSuccess = true
                                        restartDialogMessage = context.getString(R.string.settings_cache_restart_required)
                                        showRestartDialog = true
                                        Toast.makeText(context, context.getString(R.string.settings_all_cache_cleared), Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Log.e("CacheManagement", "Error clearing cache", e)
                                        Toast.makeText(context, context.getString(R.string.settings_cache_clear_failed), Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isClearingCache = false
                                    }
                                }
                            }
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Library Storage section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = context.getString(R.string.settings_cache_backend),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Material3SettingsGroup(
                    items = listOf(
                        Material3SettingsItem(
                            icon = MaterialSymbolIcon("table_chart", filled = true),
                            title = { Text(context.getString(R.string.settings_storage_room_stats)) },
                            description = {
                                Text(
                                    if (roomSongCount >= 0) {
                                        context.getString(R.string.settings_storage_song_count, roomSongCount)
                                    } else {
                                        context.getString(R.string.settings_storage_not_available)
                                    }
                                )
                            }
                        ),
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("sync", filled = true),
                                title = context.getString(R.string.settings_storage_rebuild_room),
                                description = context.getString(R.string.settings_storage_rebuild_room_desc),
                                onClick = {
                                    if (!isRebuildingRoom) {
                                        scope.launch {
                                            isRebuildingRoom = true
                                            try {
                                                musicViewModel.getMusicRepository().clearSongCacheData()
                                                musicViewModel.refreshLibrary()
                                                roomSongCount = musicViewModel.getMusicRepository().getRoomSongCount()
                                                Toast.makeText(context, context.getString(R.string.settings_storage_rebuild_success), Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Log.e("CacheManagement", "Error rebuilding Room DB", e)
                                                Toast.makeText(context, context.getString(R.string.settings_storage_rebuild_failed), Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isRebuildingRoom = false
                                                restartDialogMessage = context.getString(R.string.settings_storage_rebuild_room_desc)
                                                showRestartDialog = true
                                            }
                                        }
                                    }
                                }
                            )
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )

                if (isRebuildingRoom) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            // Information section
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Info,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.cache_about),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        listOf(
                            context.getString(R.string.settings_cache_info_1),
                            context.getString(R.string.settings_cache_info_2),
                            context.getString(R.string.settings_cache_info_3),
                            context.getString(R.string.settings_cache_info_4)
                        ).forEach { info ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = MaterialSymbolIcon("fiber_manual_record", filled = true),
                                    contentDescription = null,
                                    modifier = Modifier.size(8.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = info,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// CacheSizeDialog composable for setting maximum cache size
@Composable
fun CacheSizeDialog(
    currentSize: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    // Convert size ranges to slider values (in MB)
    val minSize = 64L * 1024L * 1024L // 64 MB
    val maxSize = 2048L * 1024L * 1024L // 2 GB
    val stepSize = 64L * 1024L * 1024L // 64 MB steps

    // Current size in MB for slider
    val currentSizeMB = (currentSize / (1024L * 1024L)).coerceIn(64L, 2048L)
    var selectedSizeMB by remember { mutableFloatStateOf(currentSizeMB.toFloat()) }

    // Helper function to format size display
    fun formatSizeDisplay(sizeMB: Float): String {
        return when {
            sizeMB >= 1024f -> "${String.format("%.1f", sizeMB / 1024f)} GB"
            else -> "${sizeMB.toInt()} MB"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = context.getString(R.string.cache_max_size),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = context.getString(R.string.cache_max_size_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Current selection display
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = formatSizeDisplay(selectedSizeMB),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = context.getString(R.string.cache_size_limit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Slider
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "64 MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "2 GB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = selectedSizeMB,
                        onValueChange = { newValue ->
                            // Snap to 64MB increments
                            val snappedValue =
                                ((newValue / 64f).toInt() * 64f).coerceIn(64f, 2048f)
                            selectedSizeMB = snappedValue
                            HapticUtils.performHapticFeedback(
                                context,
                                haptics,
                                HapticFeedbackType.TextHandleMove
                            )
                        },
                        valueRange = 64f..2048f,
                        steps = ((2048 - 64) / 64) - 1, // Number of steps between min and max
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick size options
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val quickSizes = listOf(128f, 256f, 512f, 1024f)
                    val quickLabels = listOf("128MB", "256MB", "512MB", "1GB")

                    quickSizes.forEachIndexed { index, size ->
                        Surface(
                            onClick = {
                                selectedSizeMB = size
                                HapticUtils.performHapticFeedback(
                                    context,
                                    haptics,
                                    HapticFeedbackType.TextHandleMove
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedSizeMB == size)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = quickLabels[index],
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedSizeMB == size)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    HapticUtils.performHapticFeedback(
                        context,
                        haptics,
                        HapticFeedbackType.LongPress
                    )
                    val sizeInBytes = selectedSizeMB.toLong() * 1024L * 1024L
                    onSave(sizeInBytes)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = MaterialSymbolIcon("save", filled = true),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(context.getString(R.string.ui_save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Icon(
                    imageVector = RhythmIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(context.getString(R.string.ui_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// BackupInfoItem composable for displaying backup information
@Composable
private fun BackupInfoItem(
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

// Backup & Restore Screen (merged from BackupRestoreBottomSheet)
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

                // Get backup from clipboard
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Status Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Last Backup Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (lastBackupTimestamp > 0)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (lastBackupTimestamp > 0) RhythmIcons.CheckCircle else RhythmIcons.Warning,
                                contentDescription = null,
                                tint = if (lastBackupTimestamp > 0)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lastBackupTimestamp > 0) {
                                    val sdf = SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                                    sdf.format(Date(lastBackupTimestamp))
                                } else "Never",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (lastBackupTimestamp > 0)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = context.getString(R.string.last_backup),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (lastBackupTimestamp > 0)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    // Auto Backup Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (autoBackupEnabled)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (autoBackupEnabled) MaterialSymbolIcon("autorenew", filled = true) else RhythmIcons.AccessTime,
                                contentDescription = null,
                                tint = if (autoBackupEnabled)
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (autoBackupEnabled) context.getString(R.string.settings_backup_enabled) else context.getString(R.string.settings_backup_manual),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (autoBackupEnabled)
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = context.getString(R.string.auto_backup),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (autoBackupEnabled)
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Backup location info if available
            backupLocation?.let { location ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = location.substringAfterLast("/"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            val settingGroups = listOf(
                SettingGroup(
                    title = context.getString(R.string.settings_backup_settings),
                    items = listOf(
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
                    title = context.getString(R.string.settings_backup_actions),
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
                        )
                    )
                ),
                SettingGroup(
                    title = context.getString(R.string.settings_restore_actions),
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
                Spacer(modifier = Modifier.height(24.dp))

                val materialItems = group.items.map { item ->
                    toMaterial3SettingsItem(context = context, item = item, hapticFeedback = haptics)
                }

                Material3SettingsGroup(
                    title = group.title,
                    items = materialItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // Tips/Information Card
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MaterialSymbolIcon("lightbulb", filled = true),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.backup_whats_included_placeholder),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

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

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }

    if (showBackupSelectionSheet) {
        BackupRestoreSectionPickerBottomSheet(
            title = "Choose Backup Sections",
            subtitle = "Select what to include in this backup file.",
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
            title = "Choose Restore Sections",
            subtitle = "Select which sections from the backup should be restored.",
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

private data class BackupRestoreResultState(
    val title: String,
    val message: String,
    val isError: Boolean,
    val requiresRestart: Boolean
)

@Composable
private fun BackupRestoreSectionPickerBottomSheet(
    title: String,
    subtitle: String,
    confirmLabel: String,
    confirmIcon: MaterialSymbolIcon,
    sections: AppSettings.BackupRestoreSections,
    isProcessing: Boolean,
    onSectionsChange: (AppSettings.BackupRestoreSections) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (AppSettings.BackupRestoreSections) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var showContent by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "backup_restore_picker_alpha"
    )

    LaunchedEffect(Unit) {
        delay(80)
        showContent = true
    }

    val selectedSectionCount = listOf(
        sections.includeGeneralSettings,
        sections.includeLibraryData,
        sections.includeStatsAndRhythmGuard
    ).count { it }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .graphicsLayer(alpha = contentAlpha)
        ) {
            StandardBottomSheetHeader(
                title = title,
                subtitle = subtitle,
                visible = showContent,
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = RhythmIcons.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Choose sections",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$selectedSectionCount of 3 enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                onSectionsChange(
                                    sections.copy(
                                        includeGeneralSettings = true,
                                        includeLibraryData = true,
                                        includeStatsAndRhythmGuard = true
                                    )
                                )
                            },
                            enabled = !isProcessing,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text("All", style = MaterialTheme.typography.labelLarge)
                        }
                        FilledTonalButton(
                            onClick = {
                                onSectionsChange(
                                    sections.copy(
                                        includeGeneralSettings = false,
                                        includeLibraryData = false,
                                        includeStatsAndRhythmGuard = false
                                    )
                                )
                            },
                            enabled = !isProcessing,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text("None", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BackupRestoreSectionRow(
                    icon = RhythmIcons.Settings,
                    title = "General Settings",
                    description = "Theme, player, UI, API, and app preferences.",
                    checked = sections.includeGeneralSettings,
                    badge = "Core",
                    onCheckedChange = { onSectionsChange(sections.copy(includeGeneralSettings = it)) }
                )

                BackupRestoreSectionRow(
                    icon = RhythmIcons.Library,
                    title = "Library Data",
                    description = "Playlists, favorites, blacklist/whitelist, pinned folders.",
                    checked = sections.includeLibraryData,
                    badge = "Collection",
                    onCheckedChange = { onSectionsChange(sections.copy(includeLibraryData = it)) }
                )

                BackupRestoreSectionRow(
                    icon = MaterialSymbolIcon("auto_graph"),
                    title = "Stats & Rhythm Guard",
                    description = "Play counts, daily stats, genres, and Rhythm Guard configuration.",
                    checked = sections.includeStatsAndRhythmGuard,
                    badge = "Insight",
                    onCheckedChange = { onSectionsChange(sections.copy(includeStatsAndRhythmGuard = it)) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            ExpressiveButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                style = ButtonGroupStyle.Tonal
            ) {
                ExpressiveGroupButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing,
                    isStart = true
                ) {
                    Icon(
                        imageVector = RhythmIcons.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(context.getString(R.string.ui_cancel))
                }

                ExpressiveGroupButton(
                    onClick = { onConfirm(sections) },
                    modifier = Modifier.weight(1f),
                    enabled = sections.hasAtLeastOneSectionSelected && !isProcessing,
                    isEnd = true
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = confirmIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupRestoreSectionRow(
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

@Composable
private fun BackupRestoreResultBottomSheet(
    state: BackupRestoreResultState,
    onDismiss: () -> Unit,
    onPrimaryAction: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showContent by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "backup_restore_result_alpha"
    )

    LaunchedEffect(Unit) {
        delay(80)
        showContent = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .graphicsLayer(alpha = contentAlpha)
        ) {
            StandardBottomSheetHeader(
                title = state.title,
                subtitle = if (state.requiresRestart) {
                    "Restart is required to finish applying changes"
                } else if (state.isError) {
                    "Action could not be completed"
                } else {
                    "Backup and restore status"
                },
                visible = showContent,
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isError) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (state.isError) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (state.isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.isError) RhythmIcons.Close else RhythmIcons.Check,
                                contentDescription = null,
                                tint = if (state.isError) {
                                    MaterialTheme.colorScheme.onError
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!state.requiresRestart) {
                ExpressiveButtonGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    style = ButtonGroupStyle.Tonal
                ) {
                    ExpressiveGroupButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        isStart = true
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(context.getString(R.string.ui_close))
                    }

                    ExpressiveGroupButton(
                        onClick = onPrimaryAction,
                        modifier = Modifier.weight(1f),
                        isEnd = true
                    ) {
                        Icon(
                            imageVector = if (state.isError) {
                                RhythmIcons.Refresh
                            } else {
                                RhythmIcons.Check
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(context.getString(R.string.ui_ok))
                    }
                }
            } else {
                ExpressiveButtonGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    style = ButtonGroupStyle.Tonal
                ) {
                    ExpressiveGroupButton(
                        onClick = onPrimaryAction,
                        modifier = Modifier.weight(1f),
                        isStart = true,
                        isEnd = true
                    ) {
                        Icon(
                            imageVector = MaterialSymbolIcon("restart_alt"),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(context.getString(R.string.settings_restart_now))
                    }
                }
            }
        }
    }
}

// Library Tab Order Screen (merged from LibraryTabOrderBottomSheet)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryTabOrderSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val tabOrder by appSettings.libraryTabOrder.collectAsState()
    var reorderableList by remember { mutableStateOf(tabOrder.toList()) }

    // Helper function to get display name and icon for tab
    fun getTabInfo(tabId: String): Pair<String, MaterialSymbolIcon> {
        return when (tabId) {
            "SONGS" -> Pair("Songs", RhythmIcons.Relax)
            "PLAYLISTS" -> Pair("Playlists", RhythmIcons.PlaylistFilled)
            "ALBUMS" -> Pair("Albums", RhythmIcons.Music.Album)
            "ARTISTS" -> Pair("Artists", RhythmIcons.Artist)
            "EXPLORER" -> Pair("Explorer", RhythmIcons.Folder)
            else -> Pair(tabId, RhythmIcons.Music.Song)
        }
    }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_tab_order),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Text(
                    text = context.getString(R.string.library_reorder_tabs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Reorderable list
            itemsIndexed(
                items = reorderableList,
                key = { _, item -> item }
            ) { index, tabId ->
                val (tabName, tabIcon) = getTabInfo(tabId)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .animateItem(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Position indicator
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // Tab icon
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )

                            // Tab name
                            Text(
                                text = tabName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Reorder buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Move up button
                            FilledIconButton(
                                onClick = {
                                    if (index > 0) {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                        val newList = reorderableList.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index - 1, item)
                                        reorderableList = newList
                                    }
                                },
                                enabled = index > 0,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.ArrowUpward,
                                    contentDescription = context.getString(R.string.settings_move_up),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Move down button
                            FilledIconButton(
                                onClick = {
                                    if (index < reorderableList.size - 1) {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                        val newList = reorderableList.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index + 1, item)
                                        reorderableList = newList
                                    }
                                },
                                enabled = index < reorderableList.size - 1,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.ArrowDownward,
                                    contentDescription = context.getString(R.string.settings_move_down),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reset button
                    OutlinedButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            appSettings.resetLibraryTabOrder()
                            reorderableList = listOf("SONGS", "PLAYLISTS", "ALBUMS", "ARTISTS", "EXPLORER")
                            Toast.makeText(context, context.getString(R.string.settings_tab_order_reset), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = MaterialSymbolIcon("restart_alt"),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(context.getString(R.string.ui_reset))
                    }

                    // Save button
                    Button(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            appSettings.setLibraryTabOrder(reorderableList)
                            Toast.makeText(context, context.getString(R.string.settings_tab_order_saved), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

// Gestures Settings Screen
@Composable
fun GesturesSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val haptic = LocalHapticFeedback.current

    // Gesture settings
    val miniPlayerSwipeGestures by appSettings.miniPlayerSwipeGestures.collectAsState()
    val gesturePlayerSwipeDismiss by appSettings.gesturePlayerSwipeDismiss.collectAsState()
    val gesturePlayerSwipeTracks by appSettings.gesturePlayerSwipeTracks.collectAsState()
    val gestureArtworkDoubleTap by appSettings.gestureArtworkDoubleTap.collectAsState()

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_gestures),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Mini Player Gestures
            item(key = "miniplayer_gestures_group") {
                Spacer(modifier = Modifier.height(24.dp))

                val miniPlayerItems = listOf(
                    SettingItem(
                        MaterialSymbolIcon("swipe", filled = true),
                        "Swipe Gestures",
                        "Swipe up/down to open/dismiss, left/right to skip tracks",
                        toggleState = miniPlayerSwipeGestures,
                        onToggleChange = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            appSettings.setMiniPlayerSwipeGestures(it)
                        }
                    )
                )

                Material3SettingsGroup(
                    title = context.getString(R.string.settings_miniplayer),
                    items = miniPlayerItems.map { item ->
                        toMaterial3SettingsItem(context = context, item = item, hapticFeedback = haptic)
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // Full Player Gestures
            item(key = "player_gestures_group") {
                Spacer(modifier = Modifier.height(24.dp))

                val playerGestureItems = listOf(
                    SettingItem(
                        MaterialSymbolIcon("swipe_down", filled = true),
                        "Swipe Down to Dismiss",
                        "Close player by swiping down on the screen",
                        toggleState = gesturePlayerSwipeDismiss,
                        onToggleChange = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            appSettings.setGesturePlayerSwipeDismiss(it)
                        }
                    ),
                    SettingItem(
                        MaterialSymbolIcon("swipe_left", filled = true),
                        "Swipe Artwork for Tracks",
                        "Swipe left/right on album artwork to skip tracks",
                        toggleState = gesturePlayerSwipeTracks,
                        onToggleChange = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            appSettings.setGesturePlayerSwipeTracks(it)
                        }
                    ),
                    SettingItem(
                        MaterialSymbolIcon("touch_app", filled = true),
                        "Double Tap Artwork",
                        "Double tap on album art to play/pause",
                        toggleState = gestureArtworkDoubleTap,
                        onToggleChange = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            appSettings.setGestureArtworkDoubleTap(it)
                        }
                    )
                )

                Material3SettingsGroup(
                    title = context.getString(R.string.settings_full_player),
                    items = playerGestureItems.map { item ->
                        toMaterial3SettingsItem(context = context, item = item, hapticFeedback = haptic)
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // Tips
            item(key = "gesture_tips") {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MaterialSymbolIcon("lightbulb", filled = true),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.settings_quick_tips),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        GestureTipItem(
                            icon = MaterialSymbolIcon("swipe_vertical"),
                            text = context.getString(R.string.settings_swipe_up_open)
                        )
                        GestureTipItem(
                            icon = MaterialSymbolIcon("swipe_down"),
                            text = context.getString(R.string.settings_swipe_down_dismiss_tip)
                        )
                        GestureTipItem(
                            icon = MaterialSymbolIcon("touch_app"),
                            text = context.getString(R.string.settings_double_tap_artwork_tip)
                        )
                        GestureTipItem(
                            icon = MaterialSymbolIcon("speed"),
                            text = context.getString(R.string.settings_disable_unused_gestures)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GestureTipItem(
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

// MiniPlayer Customization Screen
@Composable
fun MiniPlayerCustomizationSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val haptics = LocalHapticFeedback.current

    // MiniPlayer settings
    val miniPlayerProgressStyle by appSettings.miniPlayerProgressStyle.collectAsState()
    val miniPlayerShowProgress by appSettings.miniPlayerShowProgress.collectAsState()
    val miniPlayerShowArtwork by appSettings.miniPlayerShowArtwork.collectAsState()
    val miniPlayerArtworkSize by appSettings.miniPlayerArtworkSize.collectAsState()
    val miniPlayerCornerRadius by appSettings.miniPlayerCornerRadius.collectAsState()
    val miniPlayerShowTime by appSettings.miniPlayerShowTime.collectAsState()
    val miniPlayerUseCircularProgress by appSettings.miniPlayerUseCircularProgress.collectAsState()
    val miniPlayerAlwaysShowTablet by appSettings.miniPlayerAlwaysShowTablet.collectAsState()
    val expressiveShapesEnabled by appSettings.expressiveShapesEnabled.collectAsState()

    var showMiniPlayerProgressStyleSheet by remember { mutableStateOf(false) }
    var showMiniPlayerArtworkSizeSheet by remember { mutableStateOf(false) }
    var showMiniPlayerCornerRadiusSheet by remember { mutableStateOf(false) }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_miniplayer),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {

            // Progress Display Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = context.getString(R.string.settings_progress_display),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                val previewStyle = try {
                    ProgressStyle.valueOf(miniPlayerProgressStyle)
                } catch (e: IllegalArgumentException) {
                    ProgressStyle.NORMAL
                }
                if (miniPlayerShowProgress && !miniPlayerUseCircularProgress) {
                    StyledProgressBar(
                        progress = 0.45f,
                        style = previewStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        progressColor = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        height = 4.dp,
                        isPlaying = true
                    )
                } else if (miniPlayerShowProgress && miniPlayerUseCircularProgress) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularWavyProgressIndicator(
                            progress = { 0.45f },
                            modifier = Modifier.size(40.dp)
                        )
                    }
                } else {
                    Text(
                        text = context.getString(R.string.settings_progress_hidden),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                val progressSettingsItems = buildList {
                    add(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = RhythmIcons.Visibility,
                                title = context.getString(R.string.settings_show_progress),
                                description = context.getString(R.string.settings_show_progress_desc),
                                toggleState = miniPlayerShowProgress,
                                onToggleChange = { appSettings.setMiniPlayerShowProgress(it) }
                            )
                        )
                    )

                    if (miniPlayerShowProgress) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptics,
                                item = SettingItem(
                                    icon = MaterialSymbolIcon("change_circle"),
                                    title = context.getString(R.string.settings_progress_mode),
                                    description = context.getString(R.string.settings_choose_progress_style)
                                ),
                                description = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = context.getString(R.string.settings_choose_progress_style),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        ExpressiveButtonGroup(
                                            items = listOf("Linear", "Circular"),
                                            selectedIndex = if (miniPlayerUseCircularProgress) 1 else 0,
                                            onItemClick = { index ->
                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                                appSettings.setMiniPlayerUseCircularProgress(index == 1)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            )
                        )

                        if (!miniPlayerUseCircularProgress) {
                            add(
                                toMaterial3SettingsItem(
                                    context = context,
                                    hapticFeedback = haptics,
                                    item = SettingItem(
                                        icon = MaterialSymbolIcon("linear_scale"),
                                        title = "Progress Style",
                                        description = miniPlayerProgressStyle.lowercase().replaceFirstChar { it.uppercase() },
                                        onClick = { showMiniPlayerProgressStyleSheet = true }
                                    )
                                )
                            )
                        }
                    }
                }

                Material3SettingsGroup(
                    items = progressSettingsItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // Artwork Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = context.getString(R.string.settings_artwork),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Material3SettingsGroup(
                    items = listOf(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = RhythmIcons.Album,
                                title = context.getString(R.string.settings_show_artwork),
                                description = context.getString(R.string.settings_show_artwork_desc),
                                toggleState = miniPlayerShowArtwork,
                                onToggleChange = { appSettings.setMiniPlayerShowArtwork(it) }
                            )
                        ),
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("photo_size_select_large"),
                                title = "Artwork Size",
                                description = "${miniPlayerArtworkSize}dp",
                                onClick = { showMiniPlayerArtworkSizeSheet = true }
                            )
                        ),
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = if (expressiveShapesEnabled) {
                                SettingItem(
                                    icon = MaterialSymbolIcon("rounded_corner"),
                                    title = "Corner Radius",
                                    description = "Managed by Expressive Shapes"
                                )
                            } else {
                                SettingItem(
                                    icon = MaterialSymbolIcon("rounded_corner"),
                                    title = "Corner Radius",
                                    description = "${miniPlayerCornerRadius}dp",
                                    onClick = { showMiniPlayerCornerRadiusSheet = true }
                                )
                            }
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // Display Options Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = context.getString(R.string.settings_display_options),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Material3SettingsGroup(
                    items = listOf(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("timer"),
                                title = context.getString(R.string.settings_show_time),
                                description = context.getString(R.string.settings_show_time_desc),
                                toggleState = miniPlayerShowTime,
                                onToggleChange = { appSettings.setMiniPlayerShowTime(it) }
                            )
                        ),
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("tablet"),
                                title = context.getString(R.string.settings_tablet_layout),
                                description = context.getString(R.string.settings_tablet_layout_desc),
                                toggleState = miniPlayerAlwaysShowTablet,
                                onToggleChange = { appSettings.setMiniPlayerAlwaysShowTablet(it) }
                            )
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Description Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Info,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "MiniPlayer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = context.getString(R.string.settings_customize_miniplayer),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // MiniPlayer Progress Style Bottom Sheet
    if (showMiniPlayerProgressStyleSheet) {
        ProgressStyleBottomSheet(
            title = "Progress Style",
            currentStyle = miniPlayerProgressStyle,
            onStyleSelected = { style ->
                appSettings.setMiniPlayerProgressStyle(style)
                showMiniPlayerProgressStyleSheet = false
            },
            onDismiss = { showMiniPlayerProgressStyleSheet = false },
            context = context,
            haptics = haptics
        )
    }

    // MiniPlayer Artwork Size Bottom Sheet
    if (showMiniPlayerArtworkSizeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var tempSize by remember { mutableIntStateOf(miniPlayerArtworkSize) }

        ModalBottomSheet(
            onDismissRequest = { showMiniPlayerArtworkSizeSheet = false },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Artwork Size",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = "${tempSize}dp",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = tempSize.toFloat(),
                    onValueChange = { tempSize = it.toInt() },
                    onValueChangeFinished = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                        appSettings.setMiniPlayerArtworkSize(tempSize)
                    },
                    valueRange = 40f..72f,
                    steps = 31,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // MiniPlayer Corner Radius Bottom Sheet
    if (showMiniPlayerCornerRadiusSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var tempRadius by remember { mutableIntStateOf(miniPlayerCornerRadius) }

        ModalBottomSheet(
            onDismissRequest = { showMiniPlayerCornerRadiusSheet = false },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Corner Radius",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = "${tempRadius}dp",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = tempRadius.toFloat(),
                    onValueChange = { tempRadius = it.toInt() },
                    onValueChangeFinished = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                        appSettings.setMiniPlayerCornerRadius(tempRadius)
                    },
                    valueRange = 0f..28f,
                    steps = 27,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// Player Customization Screen
@Composable
fun PlayerCustomizationSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val haptics = LocalHapticFeedback.current

    // State variables
    val showLyrics by appSettings.showLyrics.collectAsState()
    val playerShowGradientOverlay by appSettings.playerShowGradientOverlay.collectAsState()
    val playerLyricsTransition by appSettings.playerLyricsTransition.collectAsState()
    val playerLyricsTextSize by appSettings.playerLyricsTextSize.collectAsState()
    val playerLyricsAlignment by appSettings.playerLyricsAlignment.collectAsState()
    val playerShowArtBelowLyrics by appSettings.playerShowArtBelowLyrics.collectAsState()
    val keepScreenOnLyrics by appSettings.keepScreenOnLyrics.collectAsState()
    val playerShowSeekButtons by appSettings.playerShowSeekButtons.collectAsState()
    val playerTextAlignment by appSettings.playerTextAlignment.collectAsState()
    val playerShowSongInfoOnArtwork by appSettings.playerShowSongInfoOnArtwork.collectAsState()
    val playerArtworkCornerRadius by appSettings.playerArtworkCornerRadius.collectAsState()
    val playerShowAudioQualityBadges by appSettings.playerShowAudioQualityBadges.collectAsState()
    val expressiveShapesEnabled by appSettings.expressiveShapesEnabled.collectAsState()

    // Progress bar settings
    val playerProgressStyle by appSettings.playerProgressStyle.collectAsState()
    val playerProgressThumbStyle by appSettings.playerProgressThumbStyle.collectAsState()

    var showChipOrderBottomSheet by remember { mutableStateOf(false) }
    var showLyricsSourceDialog by remember { mutableStateOf(false) }
    var showTextAlignmentSheet by remember { mutableStateOf(false) }
    var showCornerRadiusSheet by remember { mutableStateOf(false) }
    var showPlayerProgressStyleSheet by remember { mutableStateOf(false) }
    var showPlayerThumbStyleSheet by remember { mutableStateOf(false) }

    if (showLyricsSourceDialog) {
        LyricsSourceDialog(onDismiss = { showLyricsSourceDialog = false }, appSettings = appSettings, context = context, haptic = haptics)
    }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_player),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {

            // Player Controls Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = context.getString(R.string.settings_player_controls),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Material3SettingsGroup(
                    items = listOf(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("reorder"),
                                title = context.getString(R.string.settings_chip_order),
                                description = context.getString(R.string.settings_chip_order_desc),
                                onClick = { showChipOrderBottomSheet = true }
                            )
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // Display Options Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Display Options",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Material3SettingsGroup(
                    items = listOf(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("gradient"),
                                title = context.getString(R.string.settings_artwork_overlay),
                                description = context.getString(R.string.settings_artwork_overlay_desc),
                                toggleState = playerShowGradientOverlay,
                                onToggleChange = { appSettings.setPlayerShowGradientOverlay(it) }
                            )
                        ),
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = RhythmIcons.Info,
                                title = context.getString(R.string.settings_song_info_artwork),
                                description = context.getString(R.string.settings_song_info_artwork_desc),
                                toggleState = playerShowSongInfoOnArtwork,
                                onToggleChange = { appSettings.setPlayerShowSongInfoOnArtwork(it) }
                            )
                        ),
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("high_quality"),
                                title = context.getString(R.string.settings_audio_quality_badges),
                                description = context.getString(R.string.settings_audio_quality_badges_desc),
                                toggleState = playerShowAudioQualityBadges,
                                onToggleChange = { appSettings.setPlayerShowAudioQualityBadges(it) }
                            )
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // Lyrics Customization Section (always visible)
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = context.getString(R.string.settings_lyrics_customization),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                val lyricsSettingsItems = buildList {
                    add(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("lyrics", filled = true),
                                title = context.getString(R.string.settings_show_lyrics),
                                description = context.getString(R.string.settings_show_lyrics_desc),
                                toggleState = showLyrics,
                                onToggleChange = { appSettings.setShowLyrics(it) }
                            )
                        )
                    )

                    if (showLyrics) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptics,
                                item = SettingItem(
                                    icon = MaterialSymbolIcon("lyrics"),
                                    title = context.getString(R.string.lyrics_source_priority),
                                    description = context.getString(R.string.playback_lyrics_priority_desc),
                                    onClick = { showLyricsSourceDialog = true }
                                )
                            )
                        )
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptics,
                                item = SettingItem(
                                    icon = MaterialSymbolIcon("light_mode", filled = true),
                                    title = context.getString(R.string.settings_keep_screen_on_lyrics),
                                    description = context.getString(R.string.settings_keep_screen_on_lyrics_desc),
                                    toggleState = keepScreenOnLyrics,
                                    onToggleChange = { appSettings.setKeepScreenOnLyrics(it) }
                                )
                            )
                        )
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptics,
                                item = SettingItem(
                                    icon = MaterialSymbolIcon("animation"),
                                    title = context.getString(R.string.settings_lyrics_transition),
                                    description = "Art ↔ Lyrics switch animation"
                                ),
                                description = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Art ↔ Lyrics switch animation",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        ExpressiveButtonGroup(
                                            items = listOf("Slide", "Fade", "Scale", "Up"),
                                            selectedIndex = playerLyricsTransition,
                                            onItemClick = { index ->
                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                                appSettings.setPlayerLyricsTransition(index)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            )
                        )
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptics,
                                item = SettingItem(
                                    icon = MaterialSymbolIcon("format_size"),
                                    title = context.getString(R.string.settings_lyrics_text_size),
                                    description = "Size: ${(playerLyricsTextSize * 100).toInt()}%"
                                ),
                                description = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Size: ${(playerLyricsTextSize * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Slider(
                                            value = playerLyricsTextSize,
                                            onValueChange = { appSettings.setPlayerLyricsTextSize(it) },
                                            valueRange = 0.5f..2.0f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("50%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("200%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            )
                        )
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptics,
                                item = SettingItem(
                                    icon = MaterialSymbolIcon("format_align_center"),
                                    title = "Lyrics Alignment",
                                    description = context.getString(R.string.settings_lyrics_alignment_desc)
                                ),
                                description = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = context.getString(R.string.settings_lyrics_alignment_desc),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        ExpressiveButtonGroup(
                                            items = listOf("Left", "Center", "Right"),
                                            selectedIndex = when (playerLyricsAlignment) {
                                                "START" -> 0
                                                "END" -> 2
                                                else -> 1
                                            },
                                            onItemClick = { index ->
                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                                appSettings.setPlayerLyricsAlignment(when (index) {
                                                    0 -> "START"
                                                    2 -> "END"
                                                    else -> "CENTER"
                                                })
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            )
                        )
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptics,
                                item = SettingItem(
                                    icon = RhythmIcons.Image,
                                    title = context.getString(R.string.settings_show_art_below_lyrics),
                                    description = context.getString(R.string.settings_show_art_below_lyrics_desc),
                                    toggleState = playerShowArtBelowLyrics,
                                    onToggleChange = { appSettings.setPlayerShowArtBelowLyrics(it) }
                                )
                            )
                        )
                    }
                }

                Material3SettingsGroup(
                    items = lyricsSettingsItems,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = context.getString(R.string.settings_layout_options),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Material3SettingsGroup(
                    items = listOf(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = RhythmIcons.Forward10,
                                title = context.getString(R.string.settings_seek_buttons),
                                description = context.getString(R.string.settings_seek_buttons_desc),
                                toggleState = playerShowSeekButtons,
                                onToggleChange = { appSettings.setPlayerShowSeekButtons(it) }
                            )
                        ),
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("format_align_center"),
                                title = context.getString(R.string.settings_text_alignment),
                                description = when(playerTextAlignment) {
                                    "START" -> context.getString(R.string.settings_left_aligned)
                                    "END" -> context.getString(R.string.settings_right_aligned)
                                    else -> context.getString(R.string.settings_center_aligned)
                                },
                                onClick = { showTextAlignmentSheet = true }
                            )
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // Progress Bar Style Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Progress Display",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                val previewStyle = try {
                    ProgressStyle.valueOf(playerProgressStyle)
                } catch (e: IllegalArgumentException) {
                    ProgressStyle.WAVY
                }
                val previewThumbStyle = try {
                    ThumbStyle.valueOf(playerProgressThumbStyle)
                } catch (e: IllegalArgumentException) {
                    ThumbStyle.CIRCLE
                }
                StyledProgressBar(
                    progress = 0.65f,
                    style = previewStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    progressColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    height = 8.dp,
                    isPlaying = true,
                    showThumb = previewThumbStyle != ThumbStyle.NONE,
                    thumbStyle = previewThumbStyle,
                    thumbSize = 14.dp
                )

                Material3SettingsGroup(
                    items = listOf(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("linear_scale"),
                                title = "Progress Style",
                                description = playerProgressStyle.lowercase().replaceFirstChar { it.uppercase() },
                                onClick = { showPlayerProgressStyleSheet = true }
                            )
                        ),
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("touch_app"),
                                title = context.getString(R.string.settings_thumb_style),
                                description = playerProgressThumbStyle.lowercase().replaceFirstChar { it.uppercase() },
                                onClick = { showPlayerThumbStyleSheet = true }
                            )
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // Artwork Customization Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Artwork",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Material3SettingsGroup(
                    items = listOf(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptics,
                            item = if (expressiveShapesEnabled) {
                                SettingItem(
                                    icon = MaterialSymbolIcon("rounded_corner"),
                                    title = "Corner Radius",
                                    description = "Managed by Expressive Shapes"
                                )
                            } else {
                                SettingItem(
                                    icon = MaterialSymbolIcon("rounded_corner"),
                                    title = "Corner Radius",
                                    description = "${playerArtworkCornerRadius}dp",
                                    onClick = { showCornerRadiusSheet = true }
                                )
                            }
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Description Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Info,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Player Screen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        PlayerTipItem(
                            icon = MaterialSymbolIcon("swipe_down"),
                            text = context.getString(R.string.settings_swipe_down_dismiss_tip)
                        )
                        PlayerTipItem(
                            icon = MaterialSymbolIcon("touch_app"),
                            text = context.getString(R.string.settings_double_tap_artwork_tip)
                        )
                        PlayerTipItem(
                            icon = MaterialSymbolIcon("speed"),
                            text = context.getString(R.string.settings_disable_unused_gestures)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showChipOrderBottomSheet) {
        PlayerChipOrderBottomSheet(
            onDismiss = { showChipOrderBottomSheet = false },
            appSettings = appSettings,
            haptics = haptics
        )
    }

    if (showTextAlignmentSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showTextAlignmentSheet = false },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Text Alignment",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    Triple("START", "Left", MaterialSymbolIcon("align_horizontal_left", filled = true)),
                    Triple("CENTER", "Center", MaterialSymbolIcon("format_align_center")),
                    Triple("END", "Right", MaterialSymbolIcon("align_horizontal_right", filled = true))
                ).forEach { (value, label, icon) ->
                    val isSelected = playerTextAlignment == value
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            appSettings.setPlayerTextAlignment(value)
                            showTextAlignmentSheet = false
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    imageVector = RhythmIcons.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Player Progress Style Bottom Sheet
    if (showPlayerProgressStyleSheet) {
        ProgressStyleBottomSheet(
            title = "Progress Style",
            currentStyle = playerProgressStyle,
            onStyleSelected = { style ->
                appSettings.setPlayerProgressStyle(style)
                showPlayerProgressStyleSheet = false
            },
            onDismiss = { showPlayerProgressStyleSheet = false },
            context = context,
            haptics = haptics
        )
    }

    // Player Thumb Style Bottom Sheet
    if (showPlayerThumbStyleSheet) {
        ThumbStyleBottomSheet(
            title = "Thumb Style",
            currentStyle = playerProgressThumbStyle,
            onStyleSelected = { style ->
                appSettings.setPlayerProgressThumbStyle(style)
                showPlayerThumbStyleSheet = false
            },
            onDismiss = { showPlayerThumbStyleSheet = false },
            context = context,
            haptics = haptics
        )
    }

    // Corner Radius Bottom Sheet
    if (showCornerRadiusSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var tempRadius by remember { mutableIntStateOf(playerArtworkCornerRadius) }

        ModalBottomSheet(
            onDismissRequest = { showCornerRadiusSheet = false },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Corner Radius",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = "${tempRadius}dp",
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = tempRadius.toFloat(),
                    onValueChange = { tempRadius = it.toInt() },
                    onValueChangeFinished = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                        appSettings.setPlayerArtworkCornerRadius(tempRadius)
                    },
                    valueRange = 0f..40f,
                    steps = 39,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Info card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = context.getString(R.string.settings_adjust_artwork_corners),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: MaterialSymbolIcon,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
    toggleState: Boolean? = null,
    onToggleChange: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    onClick()
                }
                else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                .padding(8.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (toggleState != null && onToggleChange != null) {
            TunerAnimatedSwitch(
                checked = toggleState,
                onCheckedChange = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                    onToggleChange(it)
                }
            )
        } else if (onClick != null) {
            Icon(
                imageVector = MaterialSymbolIcon("arrow_forward_ios", filled = true),
                contentDescription = "Navigate",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlayerTipItem(
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

/**
 * Bottom sheet for selecting progress bar style with visual previews
 */
@Composable
private fun ProgressStyleBottomSheet(
    title: String,
    currentStyle: String,
    onStyleSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    context: Context,
    haptics: HapticFeedback
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val progressStyles = listOf(
        ProgressStyleOption("NORMAL", "Normal", MaterialSymbolIcon("linear_scale"), "Standard progress bar"),
        ProgressStyleOption("WAVY", "Wavy", MaterialSymbolIcon("graphic_eq"), "Animated wavy line"),
        ProgressStyleOption("ROUNDED", "Rounded", MaterialSymbolIcon("rounded_corner"), "Pill-shaped progress"),
        ProgressStyleOption("THIN", "Thin", RhythmIcons.Remove, "Thin elegant line"),
        ProgressStyleOption("THICK", "Thick", RhythmIcons.DragHandle, "Bold thick bar"),
        ProgressStyleOption("GRADIENT", "Gradient", MaterialSymbolIcon("gradient"), "Multi-color gradient"),
        ProgressStyleOption("SEGMENTED", "Segmented", MaterialSymbolIcon("more_horiz"), "Segmented blocks"),
        ProgressStyleOption("DOTS", "Dots", MaterialSymbolIcon("fiber_manual_record"), "Dot indicators")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Style options in a grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(progressStyles) { styleOption ->
                    val isSelected = currentStyle == styleOption.id
                    val progressStyleEnum = try {
                        ProgressStyle.valueOf(styleOption.id)
                    } catch (e: IllegalArgumentException) {
                        ProgressStyle.NORMAL
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        border = if (isSelected)
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null,
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            onStyleSelected(styleOption.id)
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Preview of the style
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                StyledProgressBar(
                                    progress = 0.6f,
                                    style = progressStyleEnum,
                                    modifier = Modifier.fillMaxWidth(),
                                    progressColor = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.primary,
                                    trackColor = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    height = 6.dp,
                                    isPlaying = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = styleOption.icon,
                                    contentDescription = null,
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = styleOption.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = styleOption.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    imageVector = RhythmIcons.Check,
                                    contentDescription = "Selected",
                                    
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Data class for progress style options
 */
private data class ProgressStyleOption(
    val id: String,
    val label: String,
    val icon: MaterialSymbolIcon,
    val description: String
)

/**
 * Data class for thumb style options
 */
private data class ThumbStyleOption(
    val id: String,
    val label: String,
    val icon: MaterialSymbolIcon,
    val description: String
)

/**
 * Bottom sheet for selecting thumb style
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThumbStyleBottomSheet(
    title: String,
    currentStyle: String,
    onStyleSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    context: Context,
    haptics: HapticFeedback
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val thumbStyles = listOf(
        ThumbStyleOption("NONE", "None", RhythmIcons.VisibilityOff, "No thumb indicator"),
        ThumbStyleOption("CIRCLE", "Circle", MaterialSymbolIcon("fiber_manual_record"), "Circular with highlight"),
        ThumbStyleOption("PILL", "Pill", MaterialSymbolIcon("rounded_corner"), "Vertical pill shape"),
        ThumbStyleOption("DIAMOND", "Diamond", MaterialSymbolIcon("change_history"), "Diamond rhombus"),
        ThumbStyleOption("LINE", "Line", RhythmIcons.Remove, "Thin vertical line"),
        ThumbStyleOption("SQUARE", "Square", MaterialSymbolIcon("crop_square"), "Rounded square"),
        ThumbStyleOption("GLOW", "Glow", MaterialSymbolIcon("flare"), "Glowing circle"),
        ThumbStyleOption("ARROW", "Arrow", RhythmIcons.Play, "Arrow pointer"),
        ThumbStyleOption("DOT", "Dot", MaterialSymbolIcon("adjust"), "Small dot with ring")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Style options in a grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(thumbStyles) { styleOption ->
                    val isSelected = currentStyle == styleOption.id
                    val thumbStyleEnum = try {
                        ThumbStyle.valueOf(styleOption.id)
                    } catch (e: IllegalArgumentException) {
                        ThumbStyle.CIRCLE
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        border = if (isSelected)
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null,
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            onStyleSelected(styleOption.id)
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Preview of the thumb style
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                StyledProgressBar(
                                    progress = 0.6f,
                                    style = ProgressStyle.NORMAL,
                                    modifier = Modifier.fillMaxWidth(),
                                    progressColor = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.primary,
                                    trackColor = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    height = 6.dp,
                                    isPlaying = true,
                                    showThumb = thumbStyleEnum != ThumbStyle.NONE,
                                    thumbStyle = thumbStyleEnum,
                                    thumbSize = 12.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = styleOption.icon,
                                    contentDescription = null,
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = styleOption.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = styleOption.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    imageVector = RhythmIcons.Check,
                                    contentDescription = "Selected",
                                    
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Data classes and enums for theme customization
data class ColorSchemeOption(
    val name: String,
    val displayName: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color
)

data class FontOption(
    val name: String,
    val displayName: String,
    val description: String
)

enum class ColorSource(val displayName: String, val description: String, val icon: MaterialSymbolIcon) {
    ALBUM_ART("Album Art", "Extract colors from currently playing album artwork", RhythmIcons.Image),
    MONET("System Colors", "Use Material You colors from your wallpaper", MaterialSymbolIcon("color_lens", filled = true)),
    CUSTOM("Custom Scheme", "Choose from predefined color schemes", RhythmIcons.Palette)
}

enum class FontSource(val displayName: String, val description: String, val icon: MaterialSymbolIcon) {
    SYSTEM("System Font", "Use the device's default font", MaterialSymbolIcon("phone_android", filled = true)),
    CUSTOM("Custom Font", "Import and use a custom font file", MaterialSymbolIcon("text_fields", filled = true))
}

// HSL Color conversion utilities
data class HSLColor(val hue: Float, val saturation: Float, val lightness: Float)

fun Color.toHSL(): HSLColor {
    val r = red
    val g = green
    val b = blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val diff = max - min

    val lightness = (max + min) / 2f

    val saturation = if (diff == 0f) 0f else diff / (1f - kotlin.math.abs(2f * lightness - 1f))

    val hue = when (max) {
        min -> 0f
        r -> ((g - b) / diff) % 6
        g -> (b - r) / diff + 2
        b -> (r - g) / diff + 4
        else -> 0f
    } * 60f

    return HSLColor(
        hue = if (hue < 0) hue + 360f else hue,
        saturation = saturation,
        lightness = lightness
    )
}

fun HSLColor.toColor(): Color {
    val c = (1f - kotlin.math.abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
    val m = lightness - c / 2f

    val (r, g, b) = when {
        hue < 60 -> Triple(c, x, 0f)
        hue < 120 -> Triple(x, c, 0f)
        hue < 180 -> Triple(0f, c, x)
        hue < 240 -> Triple(0f, x, c)
        hue < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f),
        alpha = 1f
    )
}

@Composable
fun ThemeCustomizationSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val haptic = LocalHapticFeedback.current

    // Theme states
    val useSystemTheme by appSettings.useSystemTheme.collectAsState()
    val darkMode by appSettings.darkMode.collectAsState()
    val amoledTheme by appSettings.amoledTheme.collectAsState()
    val useDynamicColors by appSettings.useDynamicColors.collectAsState()
    val customColorScheme by appSettings.customColorScheme.collectAsState()
    val colorSource by appSettings.colorSource.collectAsState()
    val extractedAlbumColors by appSettings.extractedAlbumColors.collectAsState()

    // Font states
    val fontSource by appSettings.fontSource.collectAsState()
    val customFontPath by appSettings.customFontPath.collectAsState()
    val customFontFamily by appSettings.customFontFamily.collectAsState()

    // Color source state - initialize based on saved setting
    var selectedColorSource by remember(colorSource) {
        mutableStateOf(
            when (colorSource) {
                "ALBUM_ART" -> ColorSource.ALBUM_ART
                "MONET" -> ColorSource.MONET
                "CUSTOM" -> ColorSource.CUSTOM
                else -> ColorSource.CUSTOM
            }
        )
    }

    // Font source state - initialize based on saved setting
    var selectedFontSource by remember(fontSource) {
        mutableStateOf(
            when (fontSource) {
                "CUSTOM" -> FontSource.CUSTOM
                "SYSTEM" -> FontSource.SYSTEM
                else -> FontSource.SYSTEM
            }
        )
    }

    // Font file picker launcher
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Copy font to internal storage
            val fontPath = FontLoader.copyFontToInternalStorage(context, it)
            if (fontPath != null) {
                // Validate that the font can be loaded
                val testFont = FontLoader.loadCustomFont(context, fontPath)
                if (testFont != null) {
                    // Save to settings
                    appSettings.setCustomFontPath(fontPath)
                    appSettings.setFontSource("CUSTOM")

                    // Extract and save font name
                    val fontName = FontLoader.getFontFileName(fontPath) ?: "Custom Font"
                    appSettings.setCustomFontFamily(fontName)

                    // Show success feedback
                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                    Toast.makeText(context, context.getString(R.string.theme_font_imported), Toast.LENGTH_SHORT).show()
                } else {
                    // Font file copied but can't be loaded
                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.Reject)
                    Toast.makeText(context, context.getString(R.string.theme_font_invalid), Toast.LENGTH_SHORT).show()
                }
            } else {
                // Failed to copy font file
                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.Reject)
                Toast.makeText(context, context.getString(R.string.theme_font_import_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Color schemes - expanded list matching bottomsheet
    val colorSchemes = remember {
        listOf(
            ColorSchemeOption(
                name = "Default",
                displayName = "Rhythm Default",
                description = "The classic Rhythm experience with vibrant purple tones",
                primaryColor = Color(0xFF5C4AD5),
                secondaryColor = Color(0xFF5D5D6B),
                tertiaryColor = Color(0xFFFFDDB6)
            ),
            ColorSchemeOption(
                name = "Warm",
                displayName = "Warm Sunset",
                description = "Cozy orange and red tones for a warm atmosphere",
                primaryColor = Color(0xFFFF6B35),
                secondaryColor = Color(0xFFF7931E),
                tertiaryColor = Color(0xFFFFC857)
            ),
            ColorSchemeOption(
                name = "Cool",
                displayName = "Cool Ocean",
                description = "Refreshing blue and teal tones for a calming vibe",
                primaryColor = Color(0xFF1E88E5),
                secondaryColor = Color(0xFF00897B),
                tertiaryColor = Color(0xFF80DEEA)
            ),
            ColorSchemeOption(
                name = "Forest",
                displayName = "Forest Green",
                description = "Natural green tones inspired by nature",
                primaryColor = Color(0xFF2E7D32),
                secondaryColor = Color(0xFF558B2F),
                tertiaryColor = Color(0xFF9CCC65)
            ),
            ColorSchemeOption(
                name = "Rose",
                displayName = "Rose Pink",
                description = "Elegant pink and magenta tones",
                primaryColor = Color(0xFFE91E63),
                secondaryColor = Color(0xFFC2185B),
                tertiaryColor = Color(0xFFF8BBD0)
            ),
            ColorSchemeOption(
                name = "Monochrome",
                displayName = "Monochrome",
                description = "Minimalist grayscale for a clean, modern look",
                primaryColor = Color(0xFF424242),
                secondaryColor = Color(0xFF616161),
                tertiaryColor = Color(0xFF9E9E9E)
            ),
            ColorSchemeOption(
                name = "Lavender",
                displayName = "Lavender",
                description = "Calming purple and lavender tones for relaxation",
                primaryColor = Color(0xFF7C4DFF),
                secondaryColor = Color(0xFF9575CD),
                tertiaryColor = Color(0xFFBA68C8)
            ),
            ColorSchemeOption(
                name = "Ocean",
                displayName = "Deep Ocean",
                description = "Deep blues and aquamarines for oceanic serenity",
                primaryColor = Color(0xFF006064),
                secondaryColor = Color(0xFF00838F),
                tertiaryColor = Color(0xFF00ACC1)
            ),
            ColorSchemeOption(
                name = "Aurora",
                displayName = "Northern Lights",
                description = "Vibrant greens and blues inspired by the aurora borealis",
                primaryColor = Color(0xFF00C853),
                secondaryColor = Color(0xFF00E676),
                tertiaryColor = Color(0xFF69F0AE)
            ),
            ColorSchemeOption(
                name = "Amber",
                displayName = "Golden Amber",
                description = "Rich amber and gold tones for a luxurious feel",
                primaryColor = Color(0xFFFF6F00),
                secondaryColor = Color(0xFFFF8F00),
                tertiaryColor = Color(0xFFFFC107)
            ),
            ColorSchemeOption(
                name = "Crimson",
                displayName = "Deep Crimson",
                description = "Bold burgundy and crimson shades for drama",
                primaryColor = Color(0xFFB71C1C),
                secondaryColor = Color(0xFFC62828),
                tertiaryColor = Color(0xFFD32F2F)
            ),
            ColorSchemeOption(
                name = "Emerald",
                displayName = "Emerald Dream",
                description = "Fresh emerald greens with natural forest hues",
                primaryColor = Color(0xFF2E7D32),
                secondaryColor = Color(0xFF388E3C),
                tertiaryColor = Color(0xFF4CAF50)
            ),
            ColorSchemeOption(
                name = "Mint",
                displayName = "Mint",
                description = "Fresh and clean cyan and mint green tones",
                primaryColor = Color(0xFF0097A7),
                secondaryColor = Color(0xFF00ACC1),
                tertiaryColor = Color(0xFF00BCD4)
            )
        )
    }

    // Font options - matching bottomsheet
    val fontOptions = remember {
        listOf(
            FontOption(
                name = "Geom",
                displayName = "Geom",
                description = "Modern, clean sans-serif font from Google"
            ),
            FontOption(
                name = "System",
                displayName = "System Default",
                description = "Use your device's default font"
            ),
            FontOption(
                name = "Slate",
                displayName = "Slate",
                description = "Elegant serif font with a classic, traditional appearance"
            ),
            FontOption(
                name = "Inter",
                displayName = "Inter",
                description = "Clean and modern sans-serif font, highly readable"
            ),
            FontOption(
                name = "JetBrains",
                displayName = "JetBrains Mono",
                description = "Monospace font perfect for technical content"
            ),
            FontOption(
                name = "Quicksand",
                displayName = "Quicksand",
                description = "Rounded font with a softer, friendlier appearance"
            )
        )
    }
    val currentFont by appSettings.customFont.collectAsState()

    // Dialog states
    var showColorSourceDialog by remember { mutableStateOf(false) }
    var showFontSourceDialog by remember { mutableStateOf(false) }
    var showColorSchemesDialog by remember { mutableStateOf(false) }
    var showCustomColorsDialog by remember { mutableStateOf(false) }
    var showFontSelectionDialog by remember { mutableStateOf(false) }
    var navigateToExpressiveShapes by remember { mutableStateOf(false) }
    
    // Restart dialog states
    var showRestartDialog by remember { mutableStateOf(false) }
    var restartDialogMessage by remember { mutableStateOf("") }
    
    // Festive theme states
    val festiveThemeEnabled by appSettings.festiveThemeEnabled.collectAsState()
    val festiveThemeAutoDetect by appSettings.festiveThemeAutoDetect.collectAsState()
    val festiveThemeType by appSettings.festiveThemeType.collectAsState()
    val festiveThemeIntensity by appSettings.festiveThemeIntensity.collectAsState()
    val festiveSnowflakeSize by appSettings.festiveSnowflakeSize.collectAsState()
    val festiveSnowflakeArea by appSettings.festiveSnowflakeArea.collectAsState()
    val festiveShowTopLights by appSettings.festiveShowTopLights.collectAsState()
    val festiveShowSideGarland by appSettings.festiveShowSideGarland.collectAsState()
    val festiveShowBottomSnow by appSettings.festiveShowBottomSnow.collectAsState()
    val festiveShowSnowfall by appSettings.festiveShowSnowfall.collectAsState()
    var showFestivalSelectionDialog by remember { mutableStateOf(false) }
    
    // Handle navigation to Expressive Shapes screen with proper animation
    AnimatedContent(
        targetState = navigateToExpressiveShapes,
        transitionSpec = {
            if (targetState) {
                // Slide in from right when navigating to Expressive Shapes
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = EaseOutCubic
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 350,
                        delayMillis = 50
                    )
                ) + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = EaseOutCubic
                    )
                ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { -it / 4 },
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = EaseInCubic
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 250)
                ) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = EaseInCubic
                    )
                )
            } else {
                // Slide in from left when going back to Theme
                slideInHorizontally(
                    initialOffsetX = { -it / 4 },
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = EaseOutCubic
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 350,
                        delayMillis = 50
                    )
                ) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = EaseOutCubic
                    )
                ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = EaseInCubic
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 250)
                ) + scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = EaseInCubic
                    )
                )
            }
        },
        label = "theme_to_shapes_navigation",
        contentKey = { it }
    ) { isNavigatingToShapes ->
        if (isNavigatingToShapes) {
            ExpressiveShapesSettingsScreen(onBackClick = { navigateToExpressiveShapes = false })
        } else {
            CollapsibleHeaderScreen(
                title = context.getString(R.string.settings_theme),
                showBackButton = true,
                onBackClick = onBackClick
            ) { modifier ->
        val settingGroups = listOf(
            SettingGroup(
                title = context.getString(R.string.settings_display_mode),
                items = listOf(
                    // Display Mode Button Group
                    SettingItem(
                        RhythmIcons.Settings,
                        context.getString(R.string.settings_theme_mode),
                        context.getString(R.string.settings_theme_mode_desc),
                        onClick = {
                            // This will be replaced with button group below
                        }
                    ),
                    // AMOLED Theme - always in list, rendered conditionally via AnimatedVisibility
                    SettingItem(
                        RhythmIcons.DarkMode,
                        context.getString(R.string.settings_amoled_theme),
                        context.getString(R.string.settings_amoled_theme_desc),
                        toggleState = amoledTheme,
                        onToggleChange = { appSettings.setAmoledTheme(it) }
                    )
                )
            ),
            SettingGroup(
                title = context.getString(R.string.settings_color_customization),
                items = listOf(
                    SettingItem(
                        RhythmIcons.Palette,
                        context.getString(R.string.settings_color_source),
                        when (selectedColorSource) {
                            ColorSource.ALBUM_ART -> context.getString(R.string.settings_color_source_album)
                            ColorSource.MONET -> context.getString(R.string.settings_color_source_monet)
                            ColorSource.CUSTOM -> context.getString(R.string.settings_color_source_custom, customColorScheme)
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            showColorSourceDialog = true
                        }
                    ),
                    SettingItem(
                        MaterialSymbolIcon("color_lens"),
                        context.getString(R.string.settings_color_schemes),
                        if (selectedColorSource == ColorSource.CUSTOM)
                            context.getString(R.string.settings_color_schemes_desc)
                        else
                            context.getString(R.string.settings_custom_only),
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            showColorSchemesDialog = true
                        }
                    ),
                    SettingItem(
                        MaterialSymbolIcon("brush"),
                        context.getString(R.string.settings_custom_colors),
                        if (selectedColorSource == ColorSource.CUSTOM)
                            context.getString(R.string.settings_custom_colors_desc)
                        else
                            context.getString(R.string.settings_custom_only),
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            showCustomColorsDialog = true
                        }
                    )
                )
            ),
            SettingGroup(
                title = context.getString(R.string.settings_font_customization),
                items = listOf(
                    SettingItem(
                        MaterialSymbolIcon("text_fields"),
                        context.getString(R.string.settings_font_source),
                        when (selectedFontSource) {
                            FontSource.SYSTEM -> context.getString(R.string.settings_font_source_system, currentFont)
                            FontSource.CUSTOM -> context.getString(
                                R.string.settings_font_source_custom,
                                customFontFamily
                            )
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            showFontSourceDialog = true
                        }
                    ),
                    SettingItem(
                        MaterialSymbolIcon("text_fields"),
                        context.getString(R.string.settings_font_selection),
                        if (selectedFontSource == FontSource.SYSTEM)
                            context.getString(R.string.settings_font_selection_desc)
                        else
                            context.getString(R.string.settings_system_font_only),
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            showFontSelectionDialog = true
                        }
                    ),
                    SettingItem(
                        MaterialSymbolIcon("file_upload"),
                        context.getString(R.string.settings_import_custom_font),
                        if (customFontPath != null)
                            context.getString(R.string.settings_font_imported_name, customFontFamily)
                        else
                            context.getString(R.string.settings_import_font_desc),
                        onClick = {
                            HapticUtils.performHapticFeedback(
                                context,
                                haptic,
                                HapticFeedbackType.LongPress
                            )
                            fontPickerLauncher.launch("font/*")
                        }
                    )
                )
            ),
            SettingGroup(
                title = "Player Themes",
                items = listOf(
                    SettingItem(
                        MaterialSymbolIcon("palette"),
                        "Playback Themes",
                        "Choose between Rhythm Default or Expressive theme"
                    )
                )
            ),
            SettingGroup(
                title = context.getString(R.string.settings_festive_themes),
                items = buildList {
                    add(
                        SettingItem(
                            MaterialSymbolIcon("celebration"),
                            context.getString(R.string.settings_enable_festive),
                            context.getString(R.string.settings_enable_festive_desc),
                            toggleState = festiveThemeEnabled,
                            onToggleChange = { appSettings.setFestiveThemeEnabled(it) }
                        )
                    )
                    if (festiveThemeEnabled) {
                        add(
                            SettingItem(
                                MaterialSymbolIcon("event_available"),
                                context.getString(R.string.settings_auto_detect_holidays),
                                context.getString(R.string.settings_auto_detect_holidays_desc),
                                toggleState = festiveThemeAutoDetect,
                                onToggleChange = { appSettings.setFestiveThemeAutoDetect(it) }
                            )
                        )
                        if (!festiveThemeAutoDetect) {
                            add(
                                SettingItem(
                                    RhythmIcons.AutoAwesome,
                                    context.getString(R.string.settings_select_festival),
                                    getFestivalDisplayName(festiveThemeType),
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                        showFestivalSelectionDialog = true
                                    }
                                )
                            )
                        }
                    }
                }
            )
        )

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            items(settingGroups, key = { "setting_${it.title}_${settingGroups.indexOf(it)}" }) { group ->
                Spacer(modifier = Modifier.height(24.dp))

                val materialItems = when (group.title) {
                    context.getString(R.string.settings_display_mode) -> {
                        buildList {
                            add(
                                Material3SettingsItem(
                                    icon = RhythmIcons.Settings,
                                    title = { Text(context.getString(R.string.settings_theme_mode)) },
                                    description = {
                                        Column {
                                            Text(context.getString(R.string.settings_theme_mode_desc))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            ExpressiveButtonGroup(
                                                items = listOf(
                                                    context.getString(R.string.settings_theme_system),
                                                    context.getString(R.string.settings_theme_light),
                                                    context.getString(R.string.settings_theme_dark)
                                                ),
                                                selectedIndex = when {
                                                    useSystemTheme -> 0
                                                    !darkMode -> 1
                                                    else -> 2
                                                },
                                                onItemClick = { index ->
                                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                    when (index) {
                                                        0 -> {
                                                            appSettings.setUseSystemTheme(true)
                                                        }

                                                        1 -> {
                                                            appSettings.setUseSystemTheme(false)
                                                            appSettings.setDarkMode(false)
                                                        }

                                                        2 -> {
                                                            appSettings.setUseSystemTheme(false)
                                                            appSettings.setDarkMode(true)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                )
                            )

                            if (!useSystemTheme && darkMode && group.items.size > 1) {
                                add(
                                    toMaterial3SettingsItem(
                                        context = context,
                                        item = group.items[1],
                                        hapticFeedback = haptic
                                    )
                                )
                            }
                        }
                    }
                    "Player Themes" -> {
                        buildList {
                            val playerThemeId by appSettings.playerThemeId.collectAsState()
                            add(
                                Material3SettingsItem(
                                    icon = MaterialSymbolIcon("palette"),
                                    title = { Text("Playback Themes") },
                                    description = {
                                        Column {
                                            Text("Choose between Rhythm Default or Expressive theme")
                                            Spacer(modifier = Modifier.height(12.dp))
                                            ExpressiveButtonGroup(
                                                items = listOf(
                                                        "Rhythm",
                                                    "Expressive"
                                                ),
                                                selectedIndex = if (playerThemeId == "EXPRESSIVE") 1 else 0,
                                                onItemClick = { index ->
                                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                                    if (index == 1) {
                                                        appSettings.setPlayerThemeId("EXPRESSIVE")
                                                    } else {
                                                        appSettings.setPlayerThemeId("MATERIAL")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                )
                            )
                        }
                    }
                    else -> {
                        group.items.map { item ->
                            toMaterial3SettingsItem(context = context, item = item, hapticFeedback = haptic)
                        }
                    }
                }

                Material3SettingsGroup(
                    title = group.title,
                    items = materialItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // Tips Card
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MaterialSymbolIcon("lightbulb", filled = true),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.theme_good_to_know),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        ThemeTipItem(
                            icon = RhythmIcons.Palette,
                            text = context.getString(R.string.theme_tip_album_art)
                        )
                        ThemeTipItem(
                            icon = MaterialSymbolIcon("wallpaper", filled = true),
                            text = context.getString(R.string.theme_tip_material_you)
                        )
                        ThemeTipItem(
                            icon = MaterialSymbolIcon("font_download", filled = true),
                            text = context.getString(R.string.theme_tip_custom_fonts)
                        )
                    }
                }
            }
        }
        }  // End of CollapsibleHeaderScreen else block
        }  // End of AnimatedContent
    }

    // App Restart Dialog for theme changes
    if (showRestartDialog) {
        AppRestartDialog(
            onDismiss = { showRestartDialog = false },
            onRestart = {
                showRestartDialog = false
                chromahub.rhythm.app.util.AppRestarter.restartApp(context)
            },
            onContinue = {
                showRestartDialog = false
                // Continue without restart
            },
            message = restartDialogMessage
        )
    }

    // Dialogs
    ColorSourceDialog(
        showDialog = showColorSourceDialog,
        onDismiss = { showColorSourceDialog = false },
        selectedColorSource = selectedColorSource,
        onColorSourceSelected = { selectedColorSource = it },
        appSettings = appSettings,
        context = context,
        haptic = haptic
    )

    FontSourceDialog(
        showDialog = showFontSourceDialog,
        onDismiss = { showFontSourceDialog = false },
        selectedFontSource = selectedFontSource,
        onFontSourceSelected = { selectedFontSource = it },
        appSettings = appSettings,
        customFontPath = customFontPath,
        context = context,
        haptic = haptic,
        onShowRestartDialog = { message ->
            showRestartDialog = true
            restartDialogMessage = message
        }
    )

    ColorSchemesDialog(
        showDialog = showColorSchemesDialog,
        onDismiss = { showColorSchemesDialog = false },
        colorSchemes = colorSchemes,
        currentScheme = customColorScheme,
        selectedColorSource = selectedColorSource,
        onSchemeSelected = { scheme ->
            appSettings.setCustomColorScheme(scheme)
            showColorSchemesDialog = false
        },
        appSettings = appSettings,
        context = context,
        haptic = haptic
    )

    CustomColorsDialog(
        showDialog = showCustomColorsDialog,
        onDismiss = { showCustomColorsDialog = false },
        currentScheme = customColorScheme,
        selectedColorSource = selectedColorSource,
        onApply = { primary, secondary, tertiary ->
            val primaryHex = String.format("%06X", (primary.toArgb() and 0xFFFFFF))
            val secondaryHex = String.format("%06X", (secondary.toArgb() and 0xFFFFFF))
            val tertiaryHex = String.format("%06X", (tertiary.toArgb() and 0xFFFFFF))
            val customScheme = "custom_${primaryHex}_${secondaryHex}_${tertiaryHex}"
            appSettings.setCustomColorScheme(customScheme)
            showCustomColorsDialog = false
        },
        appSettings = appSettings,
        context = context,
        haptic = haptic
    )

    FontSelectionDialog(
        showDialog = showFontSelectionDialog,
        onDismiss = { showFontSelectionDialog = false },
        fontOptions = fontOptions,
        currentFont = currentFont,
        selectedFontSource = selectedFontSource,
        onFontSelected = { selectedFont ->
            appSettings.setCustomFont(selectedFont)
            showFontSelectionDialog = false
            showRestartDialog = true
            restartDialogMessage = "Font changed. Restart the app to apply the new font."
        },
        appSettings = appSettings,
        context = context,
        haptic = haptic
    )

    // Festival Selection Dialog with Intensity Controls
    if (showFestivalSelectionDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        ModalBottomSheet(
            onDismissRequest = { showFestivalSelectionDialog = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary) },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            val festiveContentPadding = 24.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                item {
                    StandardBottomSheetHeader(
                        title = context.getString(R.string.theme_festive_settings),
                        subtitle = context.getString(R.string.settings_choose_festive_theme),
                        visible = true,
                        modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp)
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = festiveContentPadding)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = context.getString(R.string.settings_select_festival),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val festivals = listOf(
                            "CHRISTMAS" to context.getString(R.string.settings_festival_christmas),
                            "NEW_YEAR" to context.getString(R.string.settings_festival_new_year)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            festivals.forEach { (id, name) ->
                                val isSelected = id == festiveThemeType
                                Card(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                        appSettings.setFestiveThemeType(id)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = RhythmIcons.CheckCircle,
                                                contentDescription = context.getString(R.string.ui_selected),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = context.getString(R.string.settings_decoration_intensity),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.settings_intensity),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(festiveThemeIntensity * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = festiveThemeIntensity,
                            onValueChange = { appSettings.setFestiveThemeIntensity(it) },
                            valueRange = 0.1f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.settings_snowflake_size),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(festiveSnowflakeSize * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = festiveSnowflakeSize,
                            onValueChange = { appSettings.setFestiveSnowflakeSize(it) },
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = context.getString(R.string.settings_snowflake_display_area),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = festiveSnowflakeArea == "FULL_SCREEN",
                                onClick = { appSettings.setFestiveSnowflakeArea("FULL_SCREEN") },
                                label = { Text(context.getString(R.string.settings_area_full)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = festiveSnowflakeArea == "LEFT_RIGHT_ONLY",
                                onClick = { appSettings.setFestiveSnowflakeArea("LEFT_RIGHT_ONLY") },
                                label = { Text(context.getString(R.string.settings_area_sides)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = festiveSnowflakeArea == "TOP_ONE_THIRD",
                                onClick = { appSettings.setFestiveSnowflakeArea("TOP_ONE_THIRD") },
                                label = { Text(context.getString(R.string.settings_area_top_third)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = context.getString(R.string.settings_decoration_elements),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DecorationToggleCard(
                                title = context.getString(R.string.settings_snowfall),
                                description = context.getString(R.string.settings_snowfall_desc),
                                icon = MaterialSymbolIcon("ac_unit", filled = true),
                                isEnabled = festiveShowSnowfall,
                                onToggle = { appSettings.setFestiveShowSnowfall(it) }
                            )
                            DecorationToggleCard(
                                title = context.getString(R.string.settings_top_lights),
                                description = context.getString(R.string.settings_top_lights_desc),
                                icon = MaterialSymbolIcon("lightbulb", filled = true),
                                isEnabled = festiveShowTopLights,
                                onToggle = { appSettings.setFestiveShowTopLights(it) }
                            )
                            DecorationToggleCard(
                                title = context.getString(R.string.settings_side_garland),
                                description = context.getString(R.string.settings_side_garland_desc),
                                icon = MaterialSymbolIcon("park", filled = true),
                                isEnabled = festiveShowSideGarland,
                                onToggle = { appSettings.setFestiveShowSideGarland(it) }
                            )
                            DecorationToggleCard(
                                title = context.getString(R.string.settings_snow_pile),
                                description = context.getString(R.string.settings_snow_pile_desc),
                                icon = MaterialSymbolIcon("terrain", filled = true),
                                isEnabled = festiveShowBottomSnow,
                                onToggle = { appSettings.setFestiveShowBottomSnow(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// Color Source and Font Source Dialogs for Theme Customization
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorSourceDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    selectedColorSource: ColorSource,
    onColorSourceSelected: (ColorSource) -> Unit,
    appSettings: AppSettings,
    context: Context,
    haptic: HapticFeedback
) {
    if (showDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // Animation states
        var showContent by remember { mutableStateOf(false) }

        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "contentAlpha"
        )

        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.theme_color_source),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = context.getString(R.string.theme_color_source_desc),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorSource.entries.forEach { source ->
                        val isSelected = selectedColorSource == source
                        Card(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                onColorSourceSelected(source)
                                when (source) {
                                    ColorSource.MONET -> {
                                        appSettings.setUseDynamicColors(true)
                                        appSettings.setColorSource("MONET")
                                    }
                                    ColorSource.ALBUM_ART -> {
                                        appSettings.setUseDynamicColors(false)
                                        appSettings.setColorSource("ALBUM_ART")
                                    }
                                    ColorSource.CUSTOM -> {
                                        appSettings.setUseDynamicColors(false)
                                        appSettings.setColorSource("CUSTOM")
                                    }
                                }
                                onDismiss()
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = if (isSelected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = source.icon,
                                            contentDescription = null,
                                            tint = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = source.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = source.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = RhythmIcons.CheckCircle,
                                        contentDescription = "Selected",
                                        
                                        modifier = Modifier.size(28.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontSourceDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    selectedFontSource: FontSource,
    onFontSourceSelected: (FontSource) -> Unit,
    appSettings: AppSettings,
    customFontPath: String?,
    context: Context,
    haptic: HapticFeedback,
    onShowRestartDialog: (String) -> Unit
) {
    if (showDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // Animation states
        var showContent by remember { mutableStateOf(false) }

        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "contentAlpha"
        )

        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.theme_font_source),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = context.getString(R.string.theme_font_source_desc),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FontSource.entries.forEach { source ->
                        val isSelected = selectedFontSource == source
                        Card(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                onFontSourceSelected(source)
                                when (source) {
                                    FontSource.SYSTEM -> {
                                        appSettings.setFontSource("SYSTEM")
                                        if (customFontPath == null) {
                                            appSettings.setCustomFont("System")
                                        }
                                    }
                                    FontSource.CUSTOM -> {
                                        appSettings.setFontSource("CUSTOM")
                                    }
                                }
                                onDismiss()
                                onShowRestartDialog("Font source changed. Restart the app to apply the new font.")
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = if (isSelected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = source.icon,
                                            contentDescription = null,
                                            tint = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = source.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = source.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = RhythmIcons.CheckCircle,
                                        contentDescription = "Selected",
                                        
                                        modifier = Modifier.size(28.dp)
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
//                                    modifier = Modifier.size(40.dp)
//                                ) {
//                                    Box(
//                                        contentAlignment = Alignment.Center,
//                                        modifier = Modifier.fillMaxSize()
//                                    ) {
//                                        Icon(
//                                            imageVector = source.icon,
//                                            contentDescription = null,
//                                            tint = if (isSelected)
//                                                MaterialTheme.colorScheme.onPrimary
//                                            else
//                                                MaterialTheme.colorScheme.onSurfaceVariant,
//                                            modifier = Modifier.size(20.dp)
//                                        )
//                                    }
//                                }
//
//                                Spacer(modifier = Modifier.width(16.dp))
//
//                                Column(modifier = Modifier.weight(1f)) {
//                                    Text(
//                                        text = source.displayName,
//                                        style = MaterialTheme.typography.titleMedium,
//                                        fontWeight = FontWeight.SemiBold,
//                                        color = if (isSelected)
//                                            MaterialTheme.colorScheme.onPrimaryContainer
//                                        else
//                                            MaterialTheme.colorScheme.onSurface
//                                    )
//                                    Spacer(modifier = Modifier.height(4.dp))
//                                    Text(
//                                        text = source.description,
//                                        style = MaterialTheme.typography.bodySmall,
//                                        color = if (isSelected)
//                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
//                                        else
//                                            MaterialTheme.colorScheme.onSurfaceVariant
//                                    )
//                                }
//
//                                if (isSelected) {
//                                    Icon(
//                                        imageVector = RhythmIcons.CheckCircle,
//                                        contentDescription = "Selected",
//                                        
//                                        modifier = Modifier.size(24.dp)
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

// Color Schemes Dialog for Theme Customization
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorSchemesDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    colorSchemes: List<ColorSchemeOption>,
    currentScheme: String,
    selectedColorSource: ColorSource,
    onSchemeSelected: (String) -> Unit,
    appSettings: AppSettings,
    context: Context,
    haptic: HapticFeedback
) {
    if (showDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // Animation states
        var showContent by remember { mutableStateOf(false) }

        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "contentAlpha"
        )

        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.theme_color_schemes),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = context.getString(R.string.theme_color_schemes_desc),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedColorSource != ColorSource.CUSTOM) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Info,
                                contentDescription = null,
                                
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = context.getString(R.string.theme_color_schemes_unavailable),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = context.getString(R.string.theme_color_schemes_switch),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Featured schemes
                        item {
                            Text(
                                text = context.getString(R.string.theme_featured_schemes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        val featuredSchemes = colorSchemes.filter {
                            it.name in listOf("Default", "Warm", "Cool", "Forest", "Rose", "Monochrome")
                        }

                        items(featuredSchemes, key = { "featured_${it.name}" }) { option ->
                            ColorSchemeCard(
                                option = option,
                                isSelected = currentScheme == option.name,
                                onSelect = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                    onSchemeSelected(option.name)
                                }
                            )
                        }

                        // More schemes
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = context.getString(R.string.theme_more_schemes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        val otherSchemes = colorSchemes.filter {
                            it.name !in listOf("Default", "Warm", "Cool", "Forest", "Rose", "Monochrome")
                        }

                        items(otherSchemes, key = { "other_${it.name}" }) { option ->
                            ColorSchemeCard(
                                option = option,
                                isSelected = currentScheme == option.name,
                                onSelect = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                    onSchemeSelected(option.name)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSchemeCard(
    option: ColorSchemeOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enhanced color preview with better visibility
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = option.primaryColor,
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                ) {}
                Surface(
                    shape = CircleShape,
                    color = option.secondaryColor,
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                ) {}
                Surface(
                    shape = CircleShape,
                    color = option.tertiaryColor,
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                ) {}
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = RhythmIcons.CheckCircle,
                    contentDescription = "Selected",
                    
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// Custom Colors Dialog for Theme Customization
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomColorsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    currentScheme: String,
    selectedColorSource: ColorSource,
    onApply: (Color, Color, Color) -> Unit,
    appSettings: AppSettings,
    context: Context,
    haptic: HapticFeedback
) {
    if (showDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // Animation states
        var showContent by remember { mutableStateOf(false) }

        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "contentAlpha"
        )

        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        // Parse current custom colors from the scheme name, or use defaults
        val customScheme = parseCustomColorScheme(currentScheme, false)

        var primaryColor by remember(currentScheme) {
            if (customScheme != null) {
                mutableStateOf(customScheme.primary)
            } else {
                mutableStateOf(Color(0xFF5C4AD5)) // Default purple
            }
        }
        var secondaryColor by remember(currentScheme) {
            if (customScheme != null) {
                mutableStateOf(customScheme.secondary)
            } else {
                mutableStateOf(Color(0xFF5D5D6B))
            }
        }
        var tertiaryColor by remember(currentScheme) {
            if (customScheme != null) {
                mutableStateOf(customScheme.tertiary)
            } else {
                mutableStateOf(Color(0xFFFFDDB6))
            }
        }

        var selectedColorType by remember { mutableStateOf(ColorType.PRIMARY) }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Header
                AnimateIn {
                    StandardBottomSheetHeader(
                        title = context.getString(R.string.theme_custom_picker),
                        subtitle = context.getString(R.string.theme_custom_picker_desc),
                        visible = showContent
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (selectedColorSource != ColorSource.CUSTOM) {
                        // Unavailable state
                        item {
                            AnimateIn {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Info,
                                            contentDescription = null,
                                            
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = context.getString(R.string.theme_custom_colors_unavailable),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = context.getString(R.string.theme_custom_colors_switch),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Main Color Customization Card
                        item {
                            AnimateIn {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                ) {
                                    Column(modifier = Modifier.padding(24.dp)) {
                                        // Color Preview Section
                                        Text(
                                            text = "Color Preview",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        // Color row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            ColorPreviewItem(
                                                label = "Primary",
                                                color = primaryColor,
                                                isSelected = selectedColorType == ColorType.PRIMARY,
                                                onClick = {
                                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                    selectedColorType = ColorType.PRIMARY
                                                }
                                            )
                                            ColorPreviewItem(
                                                label = "Secondary",
                                                color = secondaryColor,
                                                isSelected = selectedColorType == ColorType.SECONDARY,
                                                onClick = {
                                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                    selectedColorType = ColorType.SECONDARY
                                                }
                                            )
                                            ColorPreviewItem(
                                                label = "Tertiary",
                                                color = tertiaryColor,
                                                isSelected = selectedColorType == ColorType.TERTIARY,
                                                onClick = {
                                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                    selectedColorType = ColorType.TERTIARY
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        // Color Picker Section
                                        Text(
                                            text = "Color Picker",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        when (selectedColorType) {
                                            ColorType.PRIMARY -> ColorPickerControls(
                                                color = primaryColor,
                                                onColorChange = { primaryColor = it }
                                            )
                                            ColorType.SECONDARY -> ColorPickerControls(
                                                color = secondaryColor,
                                                onColorChange = { secondaryColor = it }
                                            )
                                            ColorType.TERTIARY -> ColorPickerControls(
                                                color = tertiaryColor,
                                                onColorChange = { tertiaryColor = it }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        // Color Inspirations Section
                                        Text(
                                            text = "Color Inspirations",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        // Warm colors
                                        PresetColorRow(
                                            title = "Warm & Cozy",
                                            colors = listOf(
                                                Color(0xFFFF6B35), Color(0xFFFF8F00), Color(0xFFFF6F00),
                                                Color(0xFFFF5722), Color(0xFFE91E63), Color(0xFF9C27B0)
                                            ),
                                            onColorSelected = { color ->
                                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                when (selectedColorType) {
                                                    ColorType.PRIMARY -> primaryColor = color
                                                    ColorType.SECONDARY -> secondaryColor = color
                                                    ColorType.TERTIARY -> tertiaryColor = color
                                                }
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Cool colors
                                        PresetColorRow(
                                            title = "Cool & Fresh",
                                            colors = listOf(
                                                Color(0xFF1E88E5), Color(0xFF0097A7), Color(0xFF00ACC1),
                                                Color(0xFF00BCD4), Color(0xFF4DD0E1), Color(0xFF26A69A)
                                            ),
                                            onColorSelected = { color ->
                                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                when (selectedColorType) {
                                                    ColorType.PRIMARY -> primaryColor = color
                                                    ColorType.SECONDARY -> secondaryColor = color
                                                    ColorType.TERTIARY -> tertiaryColor = color
                                                }
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Nature colors
                                        PresetColorRow(
                                            title = "Nature & Earth",
                                            colors = listOf(
                                                Color(0xFF2E7D32), Color(0xFF388E3C), Color(0xFF4CAF50),
                                                Color(0xFF66BB6A), Color(0xFF81C784), Color(0xFFA5D6A7)
                                            ),
                                            onColorSelected = { color ->
                                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                when (selectedColorType) {
                                                    ColorType.PRIMARY -> primaryColor = color
                                                    ColorType.SECONDARY -> secondaryColor = color
                                                    ColorType.TERTIARY -> tertiaryColor = color
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Action Buttons
                        item {
                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                        onApply(primaryColor, secondaryColor, tertiaryColor)
                                        val primaryHex = String.format("%06X", (primaryColor.toArgb() and 0xFFFFFF))
                                        val secondaryHex = String.format("%06X", (secondaryColor.toArgb() and 0xFFFFFF))
                                        val tertiaryHex = String.format("%06X", (tertiaryColor.toArgb() and 0xFFFFFF))
                                        val customScheme = "custom_${primaryHex}_${secondaryHex}_${tertiaryHex}"
                                        appSettings.setCustomColorScheme(customScheme)
                                        Toast.makeText(context, context.getString(R.string.theme_colors_applied), Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(context.getString(R.string.ui_apply))
                                }
                            }
                        }

                        // Bottom padding
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimateIn(
    delay: Int = 50,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350, delayMillis = 0),
        label = "alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale"
    )

    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "translationY"
    )

    Box(
        modifier = modifier.graphicsLayer(
            alpha = alpha,
            scaleX = scale,
            scaleY = scale,
            translationY = translationY
        )
    ) {
        content()
    }
}

@Composable
private fun ExpressiveColorPickerControls(
    color: Color,
    onColorChange: (Color) -> Unit
) {
    val context = LocalContext.current
    val hsl = remember(color) { color.toHSL() }

    var hue by remember(color) { mutableStateOf(hsl.hue) }
    var saturation by remember(color) { mutableStateOf(hsl.saturation) }
    var lightness by remember(color) { mutableStateOf(hsl.lightness) }

    var showAdvanced by remember { mutableStateOf(false) }

    // Update color when HSL values change
    LaunchedEffect(hue, saturation, lightness) {
        val newColor = HSLColor(hue, saturation, lightness).toColor()
        onColorChange(newColor)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Enhanced Current color display with gradient background
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Subtle pattern overlay
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Palette,
                                contentDescription = null,
                                tint = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format("#%06X", (color.toArgb() and 0xFFFFFF)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.9f) else Color.White,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Color properties
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.alpha(0.8f)
                        ) {
                            Text(
                                text = "H:${hue.toInt()}°",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "S:${(saturation * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "L:${(lightness * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Enhanced Hue Slider with gradient preview
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hue",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "${hue.toInt()}°",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hue slider with enhanced gradient
            Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = (0..360 step 10).map { h ->
                                    HSLColor(h.toFloat(), 1f, 0.5f).toColor()
                                }
                            )
                        )
                ) {
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxSize(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Enhanced Saturation Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saturation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "${(saturation * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.LightGray,
                                    HSLColor(hue, 1f, lightness).toColor()
                                )
                            )
                        )
                ) {
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxSize(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Enhanced Lightness Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lightness",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "${(lightness * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black,
                                    HSLColor(hue, saturation, 0.5f).toColor(),
                                    Color.White
                                )
                            )
                        )
                ) {
                    Slider(
                        value = lightness,
                        onValueChange = { lightness = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxSize(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Advanced RGB controls toggle with enhanced design
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = RhythmIcons.Tune,
                        contentDescription = null,
                        
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = context.getString(R.string.theme_advanced_rgb),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TunerAnimatedSwitch(
                    checked = showAdvanced,
                    onCheckedChange = { showAdvanced = it }
                )
            }
        }

        // Advanced RGB controls with enhanced design
        AnimatedVisibility(
            visible = showAdvanced,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val red = (color.red * 255).toInt()
                    val green = (color.green * 255).toInt()
                    val blue = (color.blue * 255).toInt()

                    var redValue by remember(color) { mutableStateOf(red.toFloat()) }
                    var greenValue by remember(color) { mutableStateOf(green.toFloat()) }
                    var blueValue by remember(color) { mutableStateOf(blue.toFloat()) }

                    // Update HSL when RGB changes
                    LaunchedEffect(redValue, greenValue, blueValue) {
                        val rgbColor = Color(
                            red = redValue / 255f,
                            green = greenValue / 255f,
                            blue = blueValue / 255f
                        )
                        val newHsl = rgbColor.toHSL()
                        hue = newHsl.hue
                        saturation = newHsl.saturation
                        lightness = newHsl.lightness
                    }

                    Text(
                        text = "RGB Values",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ExpressiveColorSlider(
                        label = "Red",
                        value = redValue,
                        onValueChange = { redValue = it },
                        color = Color.Red,
                        valueRange = 0f..255f
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExpressiveColorSlider(
                        label = "Green",
                        value = greenValue,
                        onValueChange = { greenValue = it },
                        color = Color.Green,
                        valueRange = 0f..255f
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExpressiveColorSlider(
                        label = "Blue",
                        value = blueValue,
                        onValueChange = { blueValue = it },
                        color = Color.Blue,
                        valueRange = 0f..255f
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetColorRow(
    title: String,
    colors: List<Color>,
    onColorSelected: (Color) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(colors) { color ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = color,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onColorSelected(color) },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    // Empty content
                }
            }
        }
    }
}

@Composable
private fun ExpressiveColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = value.toInt().toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = color.copy(alpha = 0.3f),
                    activeTickColor = color,
                    inactiveTickColor = color.copy(alpha = 0.3f)
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// Helper function to get color temperature (approximate)
private fun getColorTemperature(color: Color): Int {
    // Simplified color temperature calculation
    val r = color.red
    val g = color.green
    val b = color.blue

    // Rough approximation using RGB ratios
    val temperature = if (r > g && r > b) {
        2000 + (r - b) * 3000 // Warm
    } else if (b > r && b > g) {
        6000 + (b - r) * 4000 // Cool
    } else {
        4000 + (g - r) * 2000 // Neutral
    }

    return temperature.toInt().coerceIn(2000, 10000)
}

@Composable
private fun ColorPickerControls(
    color: Color,
    onColorChange: (Color) -> Unit
) {
    val context = LocalContext.current
    val hsl = remember(color) { color.toHSL() }

    var hue by remember(color) { mutableStateOf(hsl.hue) }
    var saturation by remember(color) { mutableStateOf(hsl.saturation) }
    var lightness by remember(color) { mutableStateOf(hsl.lightness) }

    var showAdvanced by remember { mutableStateOf(false) }

    // Update color when HSL values change
    LaunchedEffect(hue, saturation, lightness) {
        val newColor = HSLColor(hue, saturation, lightness).toColor()
        onColorChange(newColor)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Current color display with hex code
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Palette,
                            contentDescription = null,
                            tint = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format("#%06X", (color.toArgb() and 0xFFFFFF)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.9f) else Color.White,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hue Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hue",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = "${hue.toInt()}°",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hue slider with color gradient
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = (0..360 step 20).map { h ->
                                HSLColor(h.toFloat(), 1f, 0.5f).toColor()
                            }
                        )
                    )
            ) {
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxSize(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Saturation Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saturation",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = "${(saturation * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.LightGray,
                                HSLColor(hue, 1f, lightness).toColor()
                            )
                        )
                    )
            ) {
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxSize(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Lightness Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lightness",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = "${(lightness * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black,
                                HSLColor(hue, saturation, 0.5f).toColor(),
                                Color.White
                            )
                        )
                    )
            ) {
                Slider(
                    value = lightness,
                    onValueChange = { lightness = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxSize(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Advanced RGB controls toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = context.getString(R.string.theme_advanced_rgb),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TunerAnimatedSwitch(
                checked = showAdvanced,
                onCheckedChange = { showAdvanced = it }
            )
        }

        // Advanced RGB controls
        AnimatedVisibility(
            visible = showAdvanced,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                val red = (color.red * 255).toInt()
                val green = (color.green * 255).toInt()
                val blue = (color.blue * 255).toInt()

                var redValue by remember(color) { mutableStateOf(red.toFloat()) }
                var greenValue by remember(color) { mutableStateOf(green.toFloat()) }
                var blueValue by remember(color) { mutableStateOf(blue.toFloat()) }

                // Update HSL when RGB changes
                LaunchedEffect(redValue, greenValue, blueValue) {
                    val rgbColor = Color(
                        red = redValue / 255f,
                        green = greenValue / 255f,
                        blue = blueValue / 255f
                    )
                    val newHsl = rgbColor.toHSL()
                    hue = newHsl.hue
                    saturation = newHsl.saturation
                    lightness = newHsl.lightness
                }

                ColorSlider(
                    label = "Red",
                    value = redValue,
                    onValueChange = { redValue = it },
                    color = Color.Red,
                    valueRange = 0f..255f
                )

                Spacer(modifier = Modifier.height(16.dp))

                ColorSlider(
                    label = "Green",
                    value = greenValue,
                    onValueChange = { greenValue = it },
                    color = Color.Green,
                    valueRange = 0f..255f
                )

                Spacer(modifier = Modifier.height(16.dp))

                ColorSlider(
                    label = "Blue",
                    value = blueValue,
                    onValueChange = { blueValue = it },
                    color = Color.Blue,
                    valueRange = 0f..255f
                )
            }
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    text = value.toInt().toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.3f),
                activeTickColor = color,
                inactiveTickColor = color.copy(alpha = 0.3f)
            )
        )
    }
}

private enum class ColorType {
    PRIMARY, SECONDARY, TERTIARY
}

// Font Selection Dialog for Theme Customization
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontSelectionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    fontOptions: List<FontOption>,
    currentFont: String,
    selectedFontSource: FontSource,
    onFontSelected: (String) -> Unit,
    appSettings: AppSettings,
    context: Context,
    haptic: HapticFeedback
) {
    if (showDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // Animation states
        var showContent by remember { mutableStateOf(false) }

        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "contentAlpha"
        )

        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.theme_font_selection),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = context.getString(R.string.theme_font_selection_desc),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedFontSource != FontSource.SYSTEM) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Info,
                            contentDescription = null,
                            
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = context.getString(R.string.theme_system_fonts_unavailable),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = context.getString(R.string.theme_system_fonts_switch),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(fontOptions, key = { "font_${it.name}" }) { option ->
                            FontCard(
                                option = option,
                                isSelected = currentFont == option.name,
                                onSelect = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                    onFontSelected(option.name)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontCard(
    option: FontOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = RhythmIcons.CheckCircle,
                        contentDescription = "Selected",
                        
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Font preview text
            Surface(
                color = if (isSelected)
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "The quick brown fox jumps over the lazy dog",
                    style = getFontPreviewStyle(option.name),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// API Management Screen
@Composable
fun ApiManagementSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val appSettings = AppSettings.getInstance(context)

    // API states
    val deezerApiEnabled by appSettings.deezerApiEnabled.collectAsState()
    val lrclibApiEnabled by appSettings.lrclibApiEnabled.collectAsState()
    val ytMusicApiEnabled by appSettings.ytMusicApiEnabled.collectAsState()

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_api_management),
        showBackButton = true,
        onBackClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onBackClick()
        }
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }


            // API Services
            item {
                Text(
                    text = context.getString(R.string.external_services),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                val apiServiceItems = buildList {
                    if (chromahub.rhythm.app.BuildConfig.ENABLE_DEEZER) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = hapticFeedback,
                                item = SettingItem(
                                    icon = RhythmIcons.Public,
                                    title = "Deezer",
                                    description = "Free artist images and album artwork - no setup needed",
                                    toggleState = deezerApiEnabled,
                                    onToggleChange = { enabled -> appSettings.setDeezerApiEnabled(enabled) }
                                )
                            )
                        )
                    }

                    if (chromahub.rhythm.app.BuildConfig.ENABLE_LRCLIB) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = hapticFeedback,
                                item = SettingItem(
                                    icon = RhythmIcons.Queue,
                                    title = "LRCLib",
                                    description = "Free line-by-line synced lyrics (Fallback)",
                                    toggleState = lrclibApiEnabled,
                                    onToggleChange = { enabled -> appSettings.setLrcLibApiEnabled(enabled) }
                                )
                            )
                        )
                    }

                    if (chromahub.rhythm.app.BuildConfig.ENABLE_YOUTUBE_MUSIC) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = hapticFeedback,
                                item = SettingItem(
                                    icon = RhythmIcons.Album,
                                    title = "YouTube Music",
                                    description = "Fallback for artist images and album artwork",
                                    toggleState = ytMusicApiEnabled,
                                    onToggleChange = { enabled -> appSettings.setYTMusicApiEnabled(enabled) }
                                )
                            )
                        )
                    }

                    add(
                        Material3SettingsItem(
                            icon = RhythmIcons.Download,
                            title = { Text("GitHub") },
                            description = { Text("App updates and release information") }
                        )
                    )
                }

                Material3SettingsGroup(
                    items = apiServiceItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Info,
                                contentDescription = null,
                                
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.api_services),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Text(
                            text = context.getString(R.string.external_services_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

// Sleep Timer Settings - Removed (now accessed via navigation in player screen)
// Sleep timer launches as bottom sheet from player or via Settings -> Sleep Timer
// No dedicated settings screen needed

// Crash Log History Settings Screen
@Composable
fun CrashLogHistorySettingsScreen(onBackClick: () -> Unit, appSettings: AppSettings) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val crashLogHistory by appSettings.crashLogHistory.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    var showLogDetailDialog by remember { mutableStateOf(false) }
    var selectedLog: String? by remember { mutableStateOf(null) }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_crash_log),
        showBackButton = true,
        onBackClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onBackClick()
        }
    ) { modifier ->
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


            if (crashLogHistory.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = RhythmIcons.CheckCircle,
                                contentDescription = "No crashes",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = context.getString(R.string.settings_no_crash_logs),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Crash logs section
                item {
                    val crashLogItems = crashLogHistory.map { entry ->
                        Material3SettingsItem(
                            icon = MaterialSymbolIcon("error", filled = true),
                            title = { Text("Crashed on: ${dateFormat.format(Date(entry.timestamp))}") },
                            description = {
                                Text(
                                    text = entry.log.lines().firstOrNull() ?: "No details available.",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                selectedLog = entry.log
                                showLogDetailDialog = true
                            }
                        )
                    }

                    Material3SettingsGroup(
                        title = context.getString(R.string.crash_reports),
                        items = crashLogItems,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                }
            }

            // Action buttons
            item {
                Material3SettingsGroup(
                    items = listOf(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = hapticFeedback,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("delete_sweep", filled = true),
                                title = context.getString(R.string.settings_clear_all_logs),
                                description = "Remove all stored crash reports",
                                enabled = crashLogHistory.isNotEmpty(),
                                onClick = { appSettings.clearCrashLogHistory() }
                            )
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }
        }
    }

    // Log detail dialog
    if (showLogDetailDialog) {
        AlertDialog(
            onDismissRequest = { showLogDetailDialog = false },
            icon = {
                Icon(
                    imageVector = RhythmIcons.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text(context.getString(R.string.settings_crash_log_details)) },
            text = {
                OutlinedTextField(
                    value = selectedLog ?: "No log details available.",
                    onValueChange = { /* Read-only */ },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Rhythm Crash Log", selectedLog)
                        clipboard.setPrimaryClip(clip)
                        showLogDetailDialog = false
                        Toast.makeText(context, context.getString(R.string.settings_log_copied), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = RhythmIcons.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.settings_copy_log))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogDetailDialog = false }) {
                    Icon(
                        imageVector = RhythmIcons.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun ThemeTipItem(
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
private fun MediaScanTipItem(
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

// Lyrics Source Settings Screen
@Composable
fun LyricsSourceSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val hapticFeedback = LocalHapticFeedback.current

    val lyricsSourcePreference by appSettings.lyricsSourcePreference.collectAsState()

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_lyrics_source),
        showBackButton = true,
        onBackClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onBackClick()
        }
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Text(
                    text = context.getString(R.string.lyrics_source_priority_title),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Text(
                    text = context.getString(R.string.lyrics_source_priority_desc_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )
            }

            val sourceOptions = listOf<Pair<chromahub.rhythm.app.shared.data.model.LyricsSourcePreference, Triple<String, String, MaterialSymbolIcon>>>(
                chromahub.rhythm.app.shared.data.model.LyricsSourcePreference.EMBEDDED_FIRST to Triple(
                    "Embedded First",
                    "Prefer lyrics embedded in audio files, fallback to online APIs",
                    RhythmIcons.MusicNote
                ),
                chromahub.rhythm.app.shared.data.model.LyricsSourcePreference.API_FIRST to Triple(
                    "Online First",
                    "Prefer online APIs (Rhythm word-by-word, LRCLib), fallback to embedded",
                    MaterialSymbolIcon("cloud_queue")
                ),
                chromahub.rhythm.app.shared.data.model.LyricsSourcePreference.LOCAL_FIRST to Triple(
                    "Local First",
                    "Prefer local .lrc files, then embedded lyrics, then online APIs",
                    RhythmIcons.Storage
                )
            )

            items(sourceOptions, key = { (pref, _) -> "source_${pref.name}" }) { (preference, info) ->
                val (title, description, icon) = info
                val isSelected = lyricsSourcePreference == preference

                Card(
                    onClick = {
                        HapticUtils.performHapticFeedback(
                            context,
                            hapticFeedback,
                            HapticFeedbackType.TextHandleMove
                        )
                        appSettings.setLyricsSourcePreference(preference)
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(18.dp),
                    border = if (isSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else null,
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) 2.dp else 0.dp
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = RhythmIcons.CheckCircle,
                                contentDescription = "Selected",
                                
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Info Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Info,
                                contentDescription = null,
                                
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "About Lyrics Sources",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Text(
                            text = "• Embedded lyrics are stored in your audio files\n" +
                                    "• Online APIs provide high-quality synced lyrics\n" +
                                "• Rhythm offers word-by-word sync\n" +
                                    "• LRCLib provides free line-by-line sync",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenCustomizationSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val haptic = LocalHapticFeedback.current

    // State for home section order bottom sheet
    var showHomeSectionOrderSheet by remember { mutableStateOf(false) }

    // Collect all home screen settings
    val showRecentlyPlayed by appSettings.homeShowRecentlyPlayed.collectAsState()
    val showDiscoverCarousel by appSettings.homeShowDiscoverCarousel.collectAsState()
    val showArtists by appSettings.homeShowArtists.collectAsState()
    val showNewReleases by appSettings.homeShowNewReleases.collectAsState()
    val showRecentlyAdded by appSettings.homeShowRecentlyAdded.collectAsState()
    val showRecommended by appSettings.homeShowRecommended.collectAsState()
    val showListeningStats by appSettings.homeShowListeningStats.collectAsState()
    val discoverItemCount by appSettings.homeDiscoverItemCount.collectAsState()
    val recentlyPlayedCount by appSettings.homeRecentlyPlayedCount.collectAsState()
    val artistsCount by appSettings.homeArtistsCount.collectAsState()
    val newReleasesCount by appSettings.homeNewReleasesCount.collectAsState()
    val recentlyAddedCount by appSettings.homeRecentlyAddedCount.collectAsState()
    val recommendedCount by appSettings.homeRecommendedCount.collectAsState()

    // Discover Widget visibility settings
    val discoverShowAlbumName by appSettings.homeDiscoverShowAlbumName.collectAsState()
    val discoverShowArtistName by appSettings.homeDiscoverShowArtistName.collectAsState()
    val discoverShowYear by appSettings.homeDiscoverShowYear.collectAsState()
    val discoverShowPlayButton by appSettings.homeDiscoverShowPlayButton.collectAsState()
    val discoverShowGradient by appSettings.homeDiscoverShowGradient.collectAsState()

    // Show bottom sheet if requested
    if (showHomeSectionOrderSheet) {
        HomeSectionOrderBottomSheet(
            onDismiss = { showHomeSectionOrderSheet = false },
            appSettings = appSettings
        )
    }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_home_screen),
        showBackButton = true,
        onBackClick = {
            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
            onBackClick()
        }
    ) { modifier ->
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
            // ==================== HEADER CUSTOMIZATION ====================
            item(key = "header_customization_header", contentType = "section_header") {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = context.getString(R.string.settings_header_customization),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }
            
            // Consolidated Header Settings Card
            item(key = "header_settings_card", contentType = "settings_card") {
                val collapseBehavior by appSettings.headerCollapseBehavior.collectAsState()
                val displayMode by appSettings.homeHeaderDisplayMode.collectAsState()
                val visibilityMode by appSettings.homeAppIconVisibility.collectAsState()
                
                val displayLabel = when (displayMode) {
                    0 -> "icon"
                    1 -> "name"
                    2 -> "icon & name"
                    else -> "content"
                }

                val headerItems = buildList {
                    add(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptic,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("unfold_less", filled = true),
                                title = context.getString(R.string.settings_always_start_collapsed),
                                description = context.getString(R.string.settings_start_collapsed),
                                toggleState = collapseBehavior == 1,
                                onToggleChange = { appSettings.setHeaderCollapseBehavior(if (it) 1 else 0) }
                            )
                        )
                    )

                    add(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptic,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("stars", filled = true),
                                title = context.getString(R.string.settings_header_display),
                                description = context.getString(R.string.settings_choose_header_content)
                            ),
                            description = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = context.getString(R.string.settings_choose_header_content),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    ExpressiveButtonGroup(
                                        items = listOf("Icon", "Name", "Both"),
                                        selectedIndex = displayMode,
                                        onItemClick = { index ->
                                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                            appSettings.setHomeHeaderDisplayMode(index)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        )
                    )

                    if (displayMode != 2) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptic,
                                item = SettingItem(
                                    icon = RhythmIcons.Visibility,
                                    title = context.getString(R.string.settings_visibility),
                                    description = "When to show $displayLabel in header"
                                ),
                                description = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "When to show $displayLabel in header",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        ExpressiveButtonGroup(
                                            items = listOf("Always", "Expanded", "Collapsed"),
                                            selectedIndex = visibilityMode,
                                            onItemClick = { index ->
                                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                appSettings.setHomeAppIconVisibility(index)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            )
                        )
                    }
                }

                Material3SettingsGroup(
                    items = headerItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // ==================== SECTION ORDER & VISIBILITY ====================
            item(key = "section_order_header", contentType = "section_header") {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = context.getString(R.string.settings_section_order_visibility),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }

            item(key = "section_order_button", contentType = "action_button") {
                Material3SettingsGroup(
                    items = listOf(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = haptic,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("reorder", filled = true),
                                title = context.getString(R.string.settings_reorder_toggle_sections),
                                description = context.getString(R.string.settings_customize_home_layout),
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                    showHomeSectionOrderSheet = true
                                }
                            )
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // ==================== WIDGET ITEM COUNTS ====================
            item(key = "widget_counts_settings", contentType = "slider_group") {
                val widgetCountItems = buildList {
                    if (showRecentlyPlayed) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptic,
                                item = SettingItem(
                                    icon = MaterialSymbolIcon("history", filled = true),
                                    title = "Recently Played",
                                    description = "$recentlyPlayedCount songs"
                                ),
                                description = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "$recentlyPlayedCount songs",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Slider(
                                            value = recentlyPlayedCount.toFloat(),
                                            onValueChange = {
                                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                appSettings.setHomeRecentlyPlayedCount(it.toInt())
                                            },
                                            valueRange = 3f..12f,
                                            steps = 8,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            )
                        )
                    }

                    if (showArtists) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptic,
                                item = SettingItem(
                                    icon = MaterialSymbolIcon("people", filled = true),
                                    title = "Top Artists",
                                    description = "$artistsCount artists"
                                ),
                                description = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "$artistsCount artists",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Slider(
                                            value = artistsCount.toFloat(),
                                            onValueChange = {
                                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                appSettings.setHomeArtistsCount(it.toInt())
                                            },
                                            valueRange = 4f..20f,
                                            steps = 15,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            )
                        )
                    }

                    if (showNewReleases) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptic,
                                item = SettingItem(
                                    icon = MaterialSymbolIcon("new_releases", filled = true),
                                    title = "New Releases",
                                    description = "$newReleasesCount albums"
                                ),
                                description = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "$newReleasesCount albums",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Slider(
                                            value = newReleasesCount.toFloat(),
                                            onValueChange = {
                                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                appSettings.setHomeNewReleasesCount(it.toInt())
                                            },
                                            valueRange = 4f..20f,
                                            steps = 15,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            )
                        )
                    }

                    if (showRecentlyAdded) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptic,
                                item = SettingItem(
                                    icon = MaterialSymbolIcon("library_add", filled = true),
                                    title = "Recently Added",
                                    description = "$recentlyAddedCount albums"
                                ),
                                description = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "$recentlyAddedCount albums",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Slider(
                                            value = recentlyAddedCount.toFloat(),
                                            onValueChange = {
                                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                appSettings.setHomeRecentlyAddedCount(it.toInt())
                                            },
                                            valueRange = 4f..20f,
                                            steps = 15,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            )
                        )
                    }

                    if (showRecommended) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptic,
                                item = SettingItem(
                                    icon = MaterialSymbolIcon("recommend", filled = true),
                                    title = "Recommended",
                                    description = "$recommendedCount songs"
                                ),
                                description = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "$recommendedCount songs",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Slider(
                                            value = recommendedCount.toFloat(),
                                            onValueChange = {
                                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                appSettings.setHomeRecommendedCount(it.toInt())
                                            },
                                            valueRange = 2f..8f,
                                            steps = 5,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            )
                        )
                    }
                }

                if (widgetCountItems.isNotEmpty()) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = context.getString(R.string.settings_widget_item_counts),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Material3SettingsGroup(
                            items = widgetCountItems,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    }
                }
            }

            // ==================== DISCOVER CAROUSEL SETTINGS ====================
            item(key = "discover_carousel_settings", contentType = "settings_card") {
                AnimatedVisibility(visible = showDiscoverCarousel) {
                    Column {
                        val isDiscoverImmersiveMode = true

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = context.getString(R.string.settings_discover_carousel),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )

                        if (isDiscoverImmersiveMode) {
                            Text(
                                text = "Immersive mode is active. Only supported discover controls are shown.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            )
                        }

                        val discoverToggleItems = buildList {
                            add(
                            SettingItem(
                                RhythmIcons.AlbumFilled,
                                "Album Name",
                                "Show album title on card",
                                toggleState = discoverShowAlbumName,
                                onToggleChange = { appSettings.setHomeDiscoverShowAlbumName(it) }
                            )
                            )

                            add(
                            SettingItem(
                                RhythmIcons.ArtistFilled,
                                "Artist Name",
                                "Show artist name on card",
                                toggleState = discoverShowArtistName,
                                onToggleChange = { appSettings.setHomeDiscoverShowArtistName(it) }
                            )
                            )

                            add(
                            SettingItem(
                                MaterialSymbolIcon("calendar_today", filled = true),
                                "Release Year",
                                "Show album release year",
                                toggleState = discoverShowYear,
                                onToggleChange = { appSettings.setHomeDiscoverShowYear(it) }
                            )
                            )

                            add(
                            SettingItem(
                                RhythmIcons.Play,
                                "Play Button",
                                "Show quick play button",
                                toggleState = discoverShowPlayButton,
                                onToggleChange = { appSettings.setHomeDiscoverShowPlayButton(it) }
                            )
                            )

                            add(
                            SettingItem(
                                MaterialSymbolIcon("gradient", filled = true),
                                "Gradient Overlay",
                                "Show gradient behind text",
                                toggleState = discoverShowGradient,
                                onToggleChange = { appSettings.setHomeDiscoverShowGradient(it) }
                            )
                            )
                        }

                        val discoverItems = buildList {
                            addAll(
                                discoverToggleItems.map { item ->
                                    toMaterial3SettingsItem(context = context, item = item, hapticFeedback = haptic)
                                }
                            )

                            add(
                                toMaterial3SettingsItem(
                                    context = context,
                                    hapticFeedback = haptic,
                                    item = SettingItem(
                                        icon = MaterialSymbolIcon("view_carousel", filled = true),
                                        title = "Album Count",
                                        description = "$discoverItemCount albums"
                                    ),
                                    description = {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "$discoverItemCount albums",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Slider(
                                                value = discoverItemCount.toFloat(),
                                                onValueChange = {
                                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                                    appSettings.setHomeDiscoverItemCount(it.toInt())
                                                },
                                                valueRange = 3f..12f,
                                                steps = 8,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                )
                            )

                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Material3SettingsGroup(
                            items = discoverItems,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    }
                }
            }

            // Quick Tips Card
            item(key = "tips_card", contentType = "tips") {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MaterialSymbolIcon("lightbulb", filled = true),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentDescription = null,
                                
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Quick Tips",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        HomeScreenTipItem(
                            icon = RhythmIcons.Visibility,
                            text = "Toggle widgets to customize your home screen"
                        )
                        HomeScreenTipItem(
                            icon = MaterialSymbolIcon("speed"),
                            text = "Disable unused sections for faster loading"
                        )
                        HomeScreenTipItem(
                            icon = RhythmIcons.Album,
                            text = "Discover carousel showcases featured albums"
                        )
                        HomeScreenTipItem(
                            icon = RhythmIcons.TrendingUp,
                            text = "Statistics update based on listening habits"
                        )
                    }
                }
            }

            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun HomeScreenTipItem(
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
private fun CarouselStyleSelector(
    selectedStyle: Int,
    onStyleSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val styles = listOf(
        Triple(0, "Default", "2 side peek albums"),
        Triple(1, "Hero", "1 side peek album")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                // Icon container with expressive design matching TunerSettingRow
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(34.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 0.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = MaterialSymbolIcon("view_carousel", filled = true),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Carousel Style",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose how albums are displayed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                styles.forEach { (style, title, description) ->
                    val isSelected = selectedStyle == style

                    Card(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            onStyleSelected(style)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = if (isSelected)
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else
                            null
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (style) {
                                    0 -> MaterialSymbolIcon("view_column", filled = true)
                                    else -> MaterialSymbolIcon("center_focus_weak", filled = true)
                                },
                                contentDescription = null,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSettingsSliderCard(
    icon: ImageVector,
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = value,
                onValueChange = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                    onValueChange(it)
                },
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun HomeSettingsSliderRow(
    icon: ImageVector,
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon container with expressive design matching TunerSettingRow
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(34.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = {
                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                onValueChange(it)
            },
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 8.dp)
        )
    }
}

// ============================================================================
// EXPRESSIVE SHAPES SETTINGS SCREEN
// ============================================================================

/**
 * Data class for shape option display
 */
private data class ShapeOption(
    val id: String,
    val displayName: String,
    val description: String,
    val category: String
)

/**
 * Data class for shape preset display
 */
private data class PresetOption(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: MaterialSymbolIcon
)

/**
 * Settings screen for configuring Material 3 Expressive Shapes
 * Allows users to customize organic shapes for different UI components
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveShapesSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val haptic = LocalHapticFeedback.current
    
    // Collect settings states
    val expressiveShapesEnabled by appSettings.expressiveShapesEnabled.collectAsState()
    val currentPreset by appSettings.expressiveShapePreset.collectAsState()
    val shapeAlbumArt by appSettings.expressiveShapeAlbumArt.collectAsState()
    val shapePlayerArt by appSettings.expressiveShapePlayerArt.collectAsState()
    val shapeSongArt by appSettings.expressiveShapeSongArt.collectAsState()
    val shapePlaylistArt by appSettings.expressiveShapePlaylistArt.collectAsState()
    val shapeArtistArt by appSettings.expressiveShapeArtistArt.collectAsState()
    val shapePlayerControls by appSettings.expressiveShapePlayerControls.collectAsState()
    val shapeMiniPlayer by appSettings.expressiveShapeMiniPlayer.collectAsState()
    
    // Dialog states
    var showPresetDialog by remember { mutableStateOf(false) }
    var showShapePickerDialog by remember { mutableStateOf<String?>(null) } // Target ID
    
    // Define presets
    val presets = remember {
        listOf(
            PresetOption("DEFAULT", "Default", "Gentle expressive shapes for all ages", RhythmIcons.RadioButtonUnchecked),
            PresetOption("FRIENDLY", "Friendly", "Warm and approachable shapes", RhythmIcons.FavoriteFilled),
            PresetOption("CHEERFUL", "Cheerful", "Bright and expressive shapes", MaterialSymbolIcon("wb_sunny")),
            PresetOption("MODERN", "Modern", "Contemporary expressive design", MaterialSymbolIcon("star")),
            PresetOption("PLAYFUL", "Playful", "Fun and expressive shapes", MaterialSymbolIcon("celebration")),
            PresetOption("ORGANIC", "Organic", "Nature-inspired shapes", MaterialSymbolIcon("park")),
            PresetOption("GEOMETRIC", "Geometric", "Clean and modern shapes", RhythmIcons.Category),
            PresetOption("RETRO", "Retro", "Pixelated nostalgic shapes", MaterialSymbolIcon("gamepad")),
            PresetOption("CUSTOM", "Custom", "Your personalized selection", RhythmIcons.Tune)
        )
    }
    
    // Define available shapes grouped by category
    val allShapes = remember {
        listOf(
            // Basic Shapes
            ShapeOption("CIRCLE", "Circle", "A perfect circle", "Basic"),
            ShapeOption("SQUARE", "Square", "Rounded square", "Basic"),
            ShapeOption("OVAL", "Oval", "Elongated oval", "Basic"),
            ShapeOption("PILL", "Pill", "Capsule shape", "Basic"),
            ShapeOption("DIAMOND", "Diamond", "Diamond shape", "Basic"),
            ShapeOption("TRIANGLE", "Triangle", "Rounded triangle", "Basic"),
            ShapeOption("PENTAGON", "Pentagon", "Five-sided polygon", "Basic"),
            // Organic Shapes
            ShapeOption("FLOWER", "Flower", "Flower with petals", "Organic"),
            ShapeOption("CLOVER_4_LEAF", "4-Leaf Clover", "Four-leaf clover", "Organic"),
            ShapeOption("CLOVER_8_LEAF", "8-Leaf Clover", "Eight-leaf clover", "Organic"),
            ShapeOption("HEART", "Heart", "Heart shape", "Organic"),
            ShapeOption("BUN", "Bun", "Bun/bread shape", "Organic"),
            // Playful Shapes
            ShapeOption("BOOM", "Boom", "Explosion shape", "Playful"),
            ShapeOption("SOFT_BOOM", "Soft Boom", "Softer explosion", "Playful"),
            ShapeOption("BURST", "Burst", "Starburst shape", "Playful"),
            ShapeOption("SOFT_BURST", "Soft Burst", "Softer starburst", "Playful"),
            ShapeOption("SUNNY", "Sunny", "Sun with rays", "Playful"),
            ShapeOption("VERY_SUNNY", "Very Sunny", "Sun with more rays", "Playful"),
            // Cookie Shapes
            ShapeOption("COOKIE_4", "Cookie 4", "4-sided cookie", "Cookie"),
            ShapeOption("COOKIE_6", "Cookie 6", "6-sided cookie", "Cookie"),
            ShapeOption("COOKIE_7", "Cookie 7", "7-sided cookie", "Cookie"),
            ShapeOption("COOKIE_9", "Cookie 9", "9-sided cookie", "Cookie"),
            ShapeOption("COOKIE_12", "Cookie 12", "12-sided cookie", "Cookie"),
            // Whimsical Shapes
            ShapeOption("GHOSTISH", "Ghostish", "Ghost-like shape", "Whimsical"),
            ShapeOption("PUFFY", "Puffy", "Cloud-like shape", "Whimsical"),
            ShapeOption("PUFFY_DIAMOND", "Puffy Diamond", "Puffy diamond", "Whimsical"),
            ShapeOption("BUN", "Bun", "Bun/bread shape", "Whimsical"),
            ShapeOption("FAN", "Fan", "Fan shape", "Whimsical"),
            ShapeOption("ARROW", "Arrow", "Arrow pointer", "Whimsical"),
            // Special Shapes
            ShapeOption("ARCH", "Arch", "Arch shape", "Special"),
            ShapeOption("CLAM_SHELL", "Clam Shell", "Shell shape", "Special"),
            ShapeOption("GEM", "Gem", "Gemstone shape", "Special"),
            ShapeOption("SEMI_CIRCLE", "Semi Circle", "Half circle", "Special"),
            ShapeOption("SLANTED", "Slanted", "Slanted square", "Special"),
            // Pixel Shapes
            ShapeOption("PIXEL_CIRCLE", "Pixel Circle", "Pixelated circle", "Pixel"),
            ShapeOption("PIXEL_TRIANGLE", "Pixel Triangle", "Pixelated triangle", "Pixel")
        )
    }

    // Migrate legacy unsupported shape IDs to currently available equivalents.
    val availableShapeIds = remember(allShapes) { allShapes.map { it.id }.toSet() }
    val legacyShapeReplacements = remember {
        mapOf(
            "STAR" to "BURST",
            "BUTTERFLY" to "FLOWER"
        )
    }

    LaunchedEffect(
        shapeAlbumArt,
        shapePlayerArt,
        shapeSongArt,
        shapePlaylistArt,
        shapeArtistArt,
        shapePlayerControls,
        shapeMiniPlayer,
        availableShapeIds
    ) {
        fun sanitizeShapeId(value: String, fallback: String): String {
            val mapped = legacyShapeReplacements[value] ?: value
            return if (mapped in availableShapeIds) mapped else fallback
        }

        val sanitizedAlbumArt = sanitizeShapeId(shapeAlbumArt, "GHOSTISH")
        if (sanitizedAlbumArt != shapeAlbumArt) appSettings.setExpressiveShapeAlbumArt(sanitizedAlbumArt)

        val sanitizedPlayerArt = sanitizeShapeId(shapePlayerArt, "BUN")
        if (sanitizedPlayerArt != shapePlayerArt) appSettings.setExpressiveShapePlayerArt(sanitizedPlayerArt)

        val sanitizedSongArt = sanitizeShapeId(shapeSongArt, "CLOVER_8_LEAF")
        if (sanitizedSongArt != shapeSongArt) appSettings.setExpressiveShapeSongArt(sanitizedSongArt)

        val sanitizedPlaylistArt = sanitizeShapeId(shapePlaylistArt, "CLOVER_4_LEAF")
        if (sanitizedPlaylistArt != shapePlaylistArt) appSettings.setExpressiveShapePlaylistArt(sanitizedPlaylistArt)

        val sanitizedArtistArt = sanitizeShapeId(shapeArtistArt, "PIXEL_CIRCLE")
        if (sanitizedArtistArt != shapeArtistArt) appSettings.setExpressiveShapeArtistArt(sanitizedArtistArt)

        val sanitizedPlayerControls = sanitizeShapeId(shapePlayerControls, "COOKIE_12")
        if (sanitizedPlayerControls != shapePlayerControls) appSettings.setExpressiveShapePlayerControls(sanitizedPlayerControls)

        val sanitizedMiniPlayer = sanitizeShapeId(shapeMiniPlayer, "COOKIE_4")
        if (sanitizedMiniPlayer != shapeMiniPlayer) appSettings.setExpressiveShapeMiniPlayer(sanitizedMiniPlayer)
    }
    
    // Define shape targets with current values
    val shapeTargets = remember(shapeAlbumArt, shapePlayerArt, shapeSongArt, shapePlaylistArt, shapeArtistArt, shapePlayerControls, shapeMiniPlayer) {
        listOf(
            Triple("ALBUM_ART", "Album Artwork" to "Shape for album artwork", shapeAlbumArt),
            Triple("PLAYER_ART", "Player Artwork" to "Shape for player screen artwork", shapePlayerArt),
            Triple("SONG_ART", "Song Artwork" to "Shape for song artwork in lists", shapeSongArt),
            Triple("PLAYLIST_ART", "Playlist Artwork" to "Shape for playlist covers", shapePlaylistArt),
            Triple("ARTIST_ART", "Artist Artwork" to "Shape for artist images", shapeArtistArt),
            Triple("PLAYER_CONTROLS", "Player Controls" to "Shape for player control buttons", shapePlayerControls),
            Triple("MINI_PLAYER", "Mini Player" to "Shape for mini player artwork", shapeMiniPlayer)
        )
    }
    
    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_expressive_shapes),
        showBackButton = true,
        onBackClick = onBackClick,
        headerContent = {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (expressiveShapesEnabled)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(40.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Icon(
                        imageVector = MaterialSymbolIcon("interests"),
                        contentDescription = null,
                        tint = if (expressiveShapesEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (expressiveShapesEnabled) "Active" else "Disabled",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TunerAnimatedSwitch(
                        checked = expressiveShapesEnabled,
                        onCheckedChange = { enabled ->
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                            appSettings.setExpressiveShapesEnabled(enabled)
                        }
                    )
                }
            }
        }
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preset Selection with animation
            item(key = "preset_section") {
                AnimatedVisibility(
                    visible = expressiveShapesEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Quick Presets",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                        Material3SettingsGroup(
                            items = listOf(
                                toMaterial3SettingsItem(
                                    context = context,
                                    hapticFeedback = haptic,
                                    item = SettingItem(
                                        MaterialSymbolIcon("style"),
                                        "Shape Preset",
                                        presets.find { it.id == currentPreset }?.displayName ?: "Default",
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                            showPresetDialog = true
                                        }
                                    )
                                )
                            ),
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    }
                }
            }
            
            // Preset Preview Row with animation
            item(key = "preset_preview") {
                AnimatedVisibility(
                    visible = expressiveShapesEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        items(presets.filter { it.id != "CUSTOM" }) { preset ->
                            val isSelected = preset.id == currentPreset
                            Card(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                    appSettings.applyExpressiveShapePreset(preset.id)
                                },
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(90.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = preset.icon,
                                        contentDescription = preset.displayName,
                                        modifier = Modifier.size(28.dp),
                                        tint = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = preset.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Randomize Button
            item(key = "randomize_shapes") {
                AnimatedVisibility(
                    visible = expressiveShapesEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                            appSettings.randomizeExpressiveShapes()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Shuffle,
                                contentDescription = "Randomize shapes",
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Randomize All Shapes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
            
            // Individual Shape Customization with animation
            item(key = "individual_shapes_header") {
                AnimatedVisibility(
                    visible = expressiveShapesEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Individual Shape Settings",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    Material3SettingsGroup(
                        items = shapeTargets.map { (targetId, namePair, currentShape) ->
                            val (targetName, _) = namePair
                            val currentShapeName = allShapes.find { it.id == currentShape }?.displayName ?: currentShape
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptic,
                                item = SettingItem(
                                    icon = when (targetId) {
                                        "ALBUM_ART" -> RhythmIcons.Album
                                        "PLAYER_ART" -> RhythmIcons.MusicNote
                                        "SONG_ART" -> MaterialSymbolIcon("audio_file")
                                        "PLAYLIST_ART" -> RhythmIcons.Queue
                                        "ARTIST_ART" -> RhythmIcons.Artist
                                        "PLAYER_CONTROLS" -> MaterialSymbolIcon("play_circle")
                                        "MINI_PLAYER" -> RhythmIcons.MusicNote
                                        else -> RhythmIcons.Category
                                    },
                                    title = targetName,
                                    description = currentShapeName,
                                    onClick = { showShapePickerDialog = targetId }
                                )
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                    } // Column
                } // AnimatedVisibility
            }
            
            // Info/Tip Card about M3 Expressive
            item(key = "expressive_info_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MaterialSymbolIcon("lightbulb", filled = true),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "About Expressive Shapes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Material 3 Expressive introduces organic, playful shapes like flowers, hearts, cookies, and more. These shapes create a unique, expressive experience that makes your music app feel more personal and fun. Enable expressive shapes to bring your music collection to life with beautiful, age-appropriate organic forms.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
                        )
                    }
                }
            }

            // Bottom spacer
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
    
    // Preset Selection Bottom Sheet
    if (showPresetDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var showPresetContent by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(100)
            showPresetContent = true
        }

        LaunchedEffect(sheetState) {
            sheetState.expand()
        }

        ModalBottomSheet(
            onDismissRequest = { showPresetDialog = false },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                // Header with animation
                StandardBottomSheetHeader(
                    title = "Choose a Preset",
                    subtitle = "Select a theme for all components",
                    visible = showPresetContent
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = preset.id == currentPreset

                        // Master animation states
                        var isPressed by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.96f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "preset_scale"
                        )

                        val containerColor by animateColorAsState(
                            targetValue = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "preset_container_color"
                        )

                        Card(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                isPressed = true
                                appSettings.applyExpressiveShapePreset(preset.id)
                                showPresetDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = containerColor
                            ),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = preset.icon,
                                            contentDescription = preset.displayName,
                                            modifier = Modifier.size(24.dp),
                                            tint = if (isSelected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = preset.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = preset.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        // Reset press state
                        LaunchedEffect(isPressed) {
                            if (isPressed) {
                                delay(150)
                                isPressed = false
                            }
                        }
                    }
                }
            }
        }
    }

    // Individual Shape Picker Bottom Sheet
    showShapePickerDialog?.let { targetId ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val targetName = shapeTargets.find { it.first == targetId }?.second?.first ?: targetId
        var showShapeContent by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(100)
            showShapeContent = true
        }

        LaunchedEffect(sheetState) {
            sheetState.expand()
        }

        val currentShapeForTarget = when (targetId) {
            "ALBUM_ART" -> shapeAlbumArt
            "PLAYER_ART" -> shapePlayerArt
            "SONG_ART" -> shapeSongArt
            "PLAYLIST_ART" -> shapePlaylistArt
            "ARTIST_ART" -> shapeArtistArt
            "PLAYER_CONTROLS" -> shapePlayerControls
            "MINI_PLAYER" -> shapeMiniPlayer
            else -> "CIRCLE"
        }

        // Group shapes by category
        val groupedShapes = allShapes.groupBy { it.category }

        ModalBottomSheet(
            onDismissRequest = { showShapePickerDialog = null },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(bottom = 24.dp)
            ) {
                // Header with animation
                StandardBottomSheetHeader(
                    title = "Shape for $targetName",
                    subtitle = "Choose an expressive shape",
                    visible = showShapeContent
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Shape options in a grid
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    groupedShapes.forEach { (category, shapes) ->
                        item(key = "category_$category", span = { GridItemSpan(2) }) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                            )
                        }

                        items(
                            items = shapes,
                            key = { "shape_${it.id}" }
                        ) { shape ->
                            val isSelected = shape.id == currentShapeForTarget

                            // Master animation states
                            var isPressed by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.96f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "shape_scale"
                            )

                            val containerColor by animateColorAsState(
                                targetValue = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "shape_container_color"
                            )

                            Card(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                    isPressed = true
                                    when (targetId) {
                                        "ALBUM_ART" -> appSettings.setExpressiveShapeAlbumArt(shape.id)
                                        "PLAYER_ART" -> appSettings.setExpressiveShapePlayerArt(shape.id)
                                        "SONG_ART" -> appSettings.setExpressiveShapeSongArt(shape.id)
                                        "PLAYLIST_ART" -> appSettings.setExpressiveShapePlaylistArt(shape.id)
                                        "ARTIST_ART" -> appSettings.setExpressiveShapeArtistArt(shape.id)
                                        "PLAYER_CONTROLS" -> appSettings.setExpressiveShapePlayerControls(shape.id)
                                        "MINI_PLAYER" -> appSettings.setExpressiveShapeMiniPlayer(shape.id)
                                    }
                                    showShapePickerDialog = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = containerColor
                                ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Shape Preview
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = rememberExpressiveShape(shape.id, CircleShape),
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize())
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = shape.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = shape.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.1f
                                    )
                                }
                            }

                            // Reset press state
                            LaunchedEffect(isPressed) {
                                if (isPressed) {
                                    delay(150)
                                    isPressed = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPreviewItem(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color,
            modifier = Modifier
                .size(48.dp)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            if (isSelected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = RhythmIcons.Check,
                        contentDescription = "Selected",
                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun PlaceholderSettingsScreen() {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large settings icon
        Surface(
            shape = RoundedCornerShape(55.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(120.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = RhythmIcons.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = context.getString(R.string.settings_select_option),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )

//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Description
//        Text(
//            text = context.getString(R.string.settings_select_option_desc),
//            style = MaterialTheme.typography.bodyLarge,
//            color = MaterialTheme.colorScheme.onSurfaceVariant,
//            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
//            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
//        )

        Spacer(modifier = Modifier.height(48.dp))

        // Additional visual elements for tablet UI
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(200.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = MaterialSymbolIcon("lightbulb"),
                    contentDescription = null,
                    
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Choose a setting from the left panel",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap on any setting option to view and modify its preferences here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PerformanceSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }
    val haptic = LocalHapticFeedback.current

    // Collect settings states
    val batterySaverEnabled by appSettings.batterySaverEnabled.collectAsState()
    val batterySaverMode by appSettings.batterySaverMode.collectAsState()
    
    val batterySaverDisableHaptics by appSettings.batterySaverDisableHaptics.collectAsState()
    val batterySaverEnableOffload by appSettings.batterySaverEnableOffload.collectAsState()
    val batterySaverDisableMarquee by appSettings.batterySaverDisableMarquee.collectAsState()
    val batterySaverDisableLosslessArtwork by appSettings.batterySaverDisableLosslessArtwork.collectAsState()

    CollapsibleHeaderScreen(
        title = "Performance",
        showBackButton = true,
        onBackClick = onBackClick,
        headerContent = {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (batterySaverEnabled)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = MaterialSymbolIcon("battery_charging_full"),
                            contentDescription = null,
                            tint = if (batterySaverEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (batterySaverEnabled) "Active" else "Disabled",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TunerAnimatedSwitch(
                            checked = batterySaverEnabled,
                            onCheckedChange = { enabled ->
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                appSettings.setBatterySaverEnabled(enabled)
                            }
                        )
                    }
                    if (batterySaverEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ExpressiveButtonGroup(
                            items = listOf("Auto", "Manual"),
                            selectedIndex = if (batterySaverMode == "auto") 0 else 1,
                            onItemClick = { index ->
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                appSettings.setBatterySaverMode(if (index == 0) "auto" else "manual")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "options_section") {
                AnimatedVisibility(
                    visible = batterySaverEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (batterySaverMode == "auto") "Auto Overrides (Enforced)" else "Manual Overrides",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                        
                        if (batterySaverMode == "auto") {
                            Material3SettingsGroup(
                                items = listOf(
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("touch_app"),
                                            title = "Disable Haptics",
                                            description = "All physical haptic feedback is disabled to conserve battery.",
                                            toggleState = true,
                                            onToggleChange = {},
                                            enabled = false
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("bolt"),
                                            title = "Enable Audio Offload",
                                            description = "Forced hardware DSP decoding to minimize CPU workload.",
                                            toggleState = true,
                                            onToggleChange = {},
                                            enabled = false
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("slideshow"),
                                            title = "Disable Text Marquee",
                                            description = "Sliding animations are paused to reduce display refresh cycles.",
                                            toggleState = true,
                                            onToggleChange = {},
                                            enabled = false
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = RhythmIcons.Image,
                                            title = "Disable Lossless Artwork",
                                            description = "Lossless artwork is disabled to reduce data decoding and memory overhead.",
                                            toggleState = true,
                                            onToggleChange = {},
                                            enabled = false
                                        )
                                    )
                                ),
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        } else {
                            // Manual mode: switch controls
                            Material3SettingsGroup(
                                items = listOf(
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("touch_app"),
                                            title = "Disable Haptics",
                                            description = "Disable touch vibrations to extend battery life",
                                            toggleState = batterySaverDisableHaptics,
                                            onToggleChange = { appSettings.setBatterySaverDisableHaptics(it) }
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("bolt"),
                                            title = "Enable Audio Offload",
                                            description = "Use hardware DSP decoding under Performance",
                                            toggleState = batterySaverEnableOffload,
                                            onToggleChange = { appSettings.setBatterySaverEnableOffload(it) }
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("slideshow"),
                                            title = "Disable Text Marquee",
                                            description = "Pause title sliding animations to save screen power",
                                            toggleState = batterySaverDisableMarquee,
                                            onToggleChange = { appSettings.setBatterySaverDisableMarquee(it) }
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = RhythmIcons.Image,
                                            title = "Disable Lossless Artwork",
                                            description = "Use standard artwork instead of lossless under Performance",
                                            toggleState = batterySaverDisableLosslessArtwork,
                                            onToggleChange = { appSettings.setBatterySaverDisableLosslessArtwork(it) }
                                        )
                                    )
                                ),
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        }
                    }
                }
            }
            
            // Padding item
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}





