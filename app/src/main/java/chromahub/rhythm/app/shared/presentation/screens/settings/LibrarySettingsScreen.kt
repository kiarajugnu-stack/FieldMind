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
    val appleMusicApiEnabled by appSettings.appleMusicApiEnabled.collectAsState()
    val autoFetchArtwork by appSettings.autoFetchArtwork.collectAsState()

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
                    ),
                    SettingItem(
                        icon = MaterialSymbolIcon("cloud_download"),
                        title = stringResource(R.string.librarysettingsscreen_autofetch_artwork),
                        description = "Automatically search online APIs for missing cover artwork on startup",
                        toggleState = autoFetchArtwork && appleMusicApiEnabled,
                        onToggleChange = { enabled -> appSettings.setAutoFetchArtwork(enabled) },
                        enabled = appleMusicApiEnabled
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
                            fieldmind.research.app.util.CacheManager.clearAllCache(context, null)
                            appSettings.requestFullMediaRescanOnNextLaunch(reason = "library_artwork_settings_restart")
                        } catch (e: Exception) {
                            Log.e("CacheManagement", "Error clearing cache before artwork settings restart", e)
                        }
                    }
                    fieldmind.research.app.util.AppRestarter.restartApp(context)
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