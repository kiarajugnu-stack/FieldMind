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
import chromahub.rhythm.app.util.HapticType
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
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.LyricsApiPriorityBottomSheet
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


// Lyrics Settings Screen
@Composable
fun LyricsSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val hapticFeedback = LocalHapticFeedback.current

    val lyricsSourcePreference by appSettings.lyricsSourcePreference.collectAsState()
    var showPriorityBottomSheet by remember { mutableStateOf(false) }

    val showLyrics by appSettings.showLyrics.collectAsState()
    val tapLyricsToFullScreen by appSettings.tapLyricsToFullScreen.collectAsState()
    val keepScreenOnLyrics by appSettings.keepScreenOnLyrics.collectAsState()
    val autoHideLyricsControls by appSettings.autoHideLyricsControls.collectAsState()
    val showLyricsBackgroundArtwork by appSettings.showLyricsBackgroundArtwork.collectAsState()
    val playerShowArtBelowLyrics by appSettings.playerShowArtBelowLyrics.collectAsState()
    val playerLyricsTransition by appSettings.playerLyricsTransition.collectAsState()
    val playerLyricsTextSize by appSettings.playerLyricsTextSize.collectAsState()
    val playerLyricsAlignment by appSettings.playerLyricsAlignment.collectAsState()
    val playerThemeId by appSettings.playerThemeId.collectAsState()
    val isExpressiveActive = playerThemeId == "EXPRESSIVE"

    val lyricBold by appSettings.lyricBold.collectAsState()
    val trimLyrics by appSettings.trimLyrics.collectAsState()
    val lyricNoAnimation by appSettings.lyricNoAnimation.collectAsState()
    val translationAutoWord by appSettings.translationAutoWord.collectAsState()
    val showLyricsTranslation by appSettings.showLyricsTranslation.collectAsState()
    val showLyricsRomanization by appSettings.showLyricsRomanization.collectAsState()

    CollapsibleHeaderScreen(
        title = "Lyrics",
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

            // 1. General & Behavior Settings
            item {
                Text(
                    text = "General Settings",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                Material3SettingsGroup(
                    items = buildList {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = hapticFeedback,
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
                                    hapticFeedback = hapticFeedback,
                                    item = SettingItem(
                                        icon = MaterialSymbolIcon("fullscreen", filled = true),
                                        title = stringResource(R.string.playercustomizationsettingsscreen_tap_lyrics_for_immersive),
                                        description = "Open a full-screen lyrics screen by tapping the lyrics view",
                                        toggleState = tapLyricsToFullScreen,
                                        onToggleChange = { appSettings.setTapLyricsToFullScreen(it) }
                                    )
                                )
                            )
                            add(
                                toMaterial3SettingsItem(
                                    context = context,
                                    hapticFeedback = hapticFeedback,
                                    item = SettingItem(
                                        icon = MaterialSymbolIcon("blur_on"),
                                        title = stringResource(R.string.settings_show_lyrics_background_artwork),
                                        description = stringResource(R.string.settings_show_lyrics_background_artwork_desc),
                                        toggleState = showLyricsBackgroundArtwork,
                                        onToggleChange = { appSettings.setShowLyricsBackgroundArtwork(it) }
                                    )
                                )
                            )
                            add(
                                toMaterial3SettingsItem(
                                    context = context,
                                    hapticFeedback = hapticFeedback,
                                    item = SettingItem(
                                        icon = MaterialSymbolIcon("translate"),
                                        title = context.getString(R.string.lyrics_show_translation),
                                        description = context.getString(R.string.lyrics_show_translation_desc),
                                        toggleState = showLyricsTranslation,
                                        onToggleChange = { appSettings.setShowLyricsTranslation(it) }
                                    )
                                )
                            )
                            add(
                                toMaterial3SettingsItem(
                                    context = context,
                                    hapticFeedback = hapticFeedback,
                                    item = SettingItem(
                                        icon = MaterialSymbolIcon("subtitles"),
                                        title = context.getString(R.string.lyrics_show_romanization),
                                        description = context.getString(R.string.lyrics_show_romanization_desc),
                                        toggleState = showLyricsRomanization,
                                        onToggleChange = { appSettings.setShowLyricsRomanization(it) }
                                    )
                                )
                            )
                            add(
                                toMaterial3SettingsItem(
                                    context = context,
                                    hapticFeedback = hapticFeedback,
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
                                    hapticFeedback = hapticFeedback,
                                    item = SettingItem(
                                        icon = MaterialSymbolIcon("visibility_off"),
                                        title = "Auto-hide lyrics controls",
                                        description = "Automatically hide controls in full-screen lyrics screen after a few seconds of inactivity",
                                        toggleState = autoHideLyricsControls,
                                        onToggleChange = { appSettings.setAutoHideLyricsControls(it) }
                                    )
                                )
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // 2. Lyrics Source Priority
            item {
                Spacer(modifier = Modifier.height(12.dp))
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
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
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
                            HapticType.LIGHT
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
                                contentDescription = stringResource(R.string.streaming_selected),
                                
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // 3. Online APIs
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.lyricssourcesettingsscreen_online_api_options),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                val apiPriority by appSettings.lyricsApiPriority.collectAsState()
                val apiFallback by appSettings.lyricsApiFallbackRetry.collectAsState()

                Material3SettingsGroup(
                    items = listOf(
                        Material3SettingsItem(
                            icon = MaterialSymbolIcon("lyrics"),
                            title = { Text(stringResource(R.string.lyricssourcesettingsscreen_lyrics_api_priority)) },
                            description = {
                                Text(
                                    text = if (apiPriority == chromahub.rhythm.app.shared.data.model.LyricsApiPriority.APPLE_MUSIC_FIRST) "Apple Music First" else "LRCLib First",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                HapticUtils.performHapticFeedback(context, hapticFeedback, HapticType.HEAVY)
                                showPriorityBottomSheet = true
                            }
                        ),
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = hapticFeedback,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("compare_arrows"),
                                title = stringResource(R.string.lyricssourcesettingsscreen_retry_using_fallbacks),
                                description = "Attempt fallback APIs if the preferred API fails to return lyrics",
                                toggleState = apiFallback,
                                onToggleChange = { appSettings.setLyricsApiFallbackRetry(it) }
                            )
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // 4. Styling & Typography Customization (Only active if lyrics enabled)
            if (showLyrics) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Display & Styling",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )

                    val stylingItems = buildList {
                        add(
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = hapticFeedback,
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
                                hapticFeedback = hapticFeedback,
                                item = SettingItem(
                                    icon = MaterialSymbolIcon("format_align_center"),
                                    title = stringResource(R.string.settings_lyrics_alignment),
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
                                                HapticUtils.performHapticFeedback(context, hapticFeedback, HapticType.LIGHT)
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
                                hapticFeedback = hapticFeedback,
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
                                            text = stringResource(R.string.settings_lyrics_transition_desc),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        ExpressiveButtonGroup(
                                            items = listOf("Slide", "Fade", "Scale", "Up"),
                                            selectedIndex = playerLyricsTransition,
                                            onItemClick = { index ->
                                                HapticUtils.performHapticFeedback(context, hapticFeedback, HapticType.LIGHT)
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
                                hapticFeedback = hapticFeedback,
                                item = SettingItem(
                                    icon = RhythmIcons.Image,
                                    title = context.getString(R.string.settings_show_art_below_lyrics),
                                    description = if (isExpressiveActive) "Not supported by Expressive theme" else context.getString(R.string.settings_show_art_below_lyrics_desc),
                                    toggleState = playerShowArtBelowLyrics,
                                    onToggleChange = { appSettings.setPlayerShowArtBelowLyrics(it) },
                                    enabled = !isExpressiveActive
                                )
                            )
                        )
                    }

                    Material3SettingsGroup(
                        items = stylingItems,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                }
            }

            // 5. Customization, Formatting & Behaviors
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Formatting & Behaviors",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                Material3SettingsGroup(
                    items = listOf(
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = hapticFeedback,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("format_bold"),
                                title = "Bold Text",
                                description = "Make the text of active lyrics bold",
                                toggleState = lyricBold,
                                onToggleChange = { appSettings.setLyricBold(it) }
                            )
                        ),
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = hapticFeedback,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("content_cut"),
                                title = "Trim Lyrics",
                                description = "Trim leading and trailing whitespace from lyric lines",
                                toggleState = trimLyrics,
                                onToggleChange = { appSettings.setTrimLyrics(it) }
                            )
                        ),
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = hapticFeedback,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("motion_photos_off"),
                                title = "Disable Animations",
                                description = "Disable transitions and animations in the lyrics view",
                                toggleState = lyricNoAnimation,
                                onToggleChange = { appSettings.setLyricNoAnimation(it) }
                            )
                        ),
                        toMaterial3SettingsItem(
                            context = context,
                            hapticFeedback = hapticFeedback,
                            item = SettingItem(
                                icon = MaterialSymbolIcon("translate"),
                                title = "Word-by-word Translation",
                                description = "Try to automatically sync syllable or word translations",
                                toggleState = translationAutoWord,
                                onToggleChange = { appSettings.setTranslationAutoWord(it) }
                            )
                        )
                    ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
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
                                text = stringResource(R.string.settings_about_lyrics_sources),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Text(
                            text = stringResource(R.string.lyricssourcesettingsscreen_embedded_lyrics_are_stored) +
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

    if (showPriorityBottomSheet) {
        LyricsApiPriorityBottomSheet(
            onDismiss = { showPriorityBottomSheet = false },
            appSettings = appSettings
        )
    }
}

