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
    val appleMusicApiEnabled by appSettings.appleMusicApiEnabled.collectAsState()

    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_api_management),
        showBackButton = true,
        onBackClick = {
            HapticUtils.performHapticFeedback(context, hapticFeedback, HapticType.HEAVY)
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
                    if (fieldmind.research.app.BuildConfig.ENABLE_DEEZER) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = hapticFeedback,
                                item = SettingItem(
                                    icon = RhythmIcons.Public,
                                    title = stringResource(R.string.onboarding_integration_deezer),
                                    description = "Free artist images and album artwork - no setup needed",
                                    toggleState = deezerApiEnabled,
                                    onToggleChange = { enabled -> appSettings.setDeezerApiEnabled(enabled) }
                                )
                            )
                        )
                    }

                    if (fieldmind.research.app.BuildConfig.ENABLE_LRCLIB) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = hapticFeedback,
                                item = SettingItem(
                                    icon = RhythmIcons.Queue,
                                    title = stringResource(R.string.onboarding_integration_lrclib),
                                    description = "Free line-by-line synced lyrics (Fallback)",
                                    toggleState = lrclibApiEnabled,
                                    onToggleChange = { enabled -> appSettings.setLrcLibApiEnabled(enabled) }
                                )
                            )
                        )
                    }

                    if (fieldmind.research.app.BuildConfig.ENABLE_APPLE_MUSIC) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = hapticFeedback,
                                item = SettingItem(
                                    icon = MaterialSymbolIcon("music_note"),
                                    title = stringResource(R.string.apimanagementsettingsscreen_apple_music),
                                    description = "Word-by-word synchronized lyrics (Highest Quality)",
                                    toggleState = appleMusicApiEnabled,
                                    onToggleChange = { enabled -> appSettings.setAppleMusicApiEnabled(enabled) }
                                )
                            )
                        )
                    }

                    if (fieldmind.research.app.BuildConfig.ENABLE_YOUTUBE_MUSIC) {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = hapticFeedback,
                                item = SettingItem(
                                    icon = RhythmIcons.Album,
                                    title = stringResource(R.string.onboarding_integration_ytmusic),
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
                            title = { Text(stringResource(R.string.apimanagementsettingsscreen_github)) },
                            description = { Text(stringResource(R.string.apimanagementsettingsscreen_app_updates_and_release)) }
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