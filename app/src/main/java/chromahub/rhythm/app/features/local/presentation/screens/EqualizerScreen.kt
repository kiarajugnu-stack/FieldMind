package chromahub.rhythm.app.features.local.presentation.screens

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
import android.provider.Settings
import android.media.audiofx.AudioEffect
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

import androidx.compose.ui.unit.dp
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chromahub.rhythm.app.BuildConfig
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Playlist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.util.GsonUtils
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.EqualizerUtils
import chromahub.rhythm.app.activities.MainActivity
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlin.system.exitProcess
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.common.ArcProgressSlider
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.StandardBottomSheetHeader
import chromahub.rhythm.app.shared.presentation.components.common.StyledProgressBar
import chromahub.rhythm.app.shared.presentation.components.common.ProgressStyle
import chromahub.rhythm.app.shared.presentation.components.common.ThumbStyle
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.LicensesBottomSheet
import chromahub.rhythm.app.ui.utils.LazyListStateSaver
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.shared.presentation.viewmodel.AppUpdaterViewModel
import chromahub.rhythm.app.ui.theme.getFontPreviewStyle
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.EaseInBack
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.features.local.presentation.screens.LibraryTab
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import chromahub.rhythm.app.R
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AddToPlaylistBottomSheet
import chromahub.rhythm.app.features.local.presentation.screens.AddToPlaylistScreen
import chromahub.rhythm.app.features.local.presentation.components.dialogs.CreatePlaylistDialog
import chromahub.rhythm.app.features.local.presentation.components.dialogs.QueueActionDialog
import chromahub.rhythm.app.shared.presentation.components.player.MiniPlayer
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons.Delete
import chromahub.rhythm.app.features.local.presentation.screens.LibraryScreen
import chromahub.rhythm.app.features.local.presentation.screens.HomeScreen
import chromahub.rhythm.app.features.local.presentation.screens.ListeningStatsScreen
import chromahub.rhythm.app.shared.presentation.screens.player.PlayerScreen
import chromahub.rhythm.app.features.local.presentation.screens.PlaylistDetailScreen
import chromahub.rhythm.app.features.local.presentation.screens.settings.SettingsScreenWrapper
import chromahub.rhythm.app.features.local.presentation.screens.settings.*
import chromahub.rhythm.app.shared.presentation.components.MediaScanLoader
import chromahub.rhythm.app.shared.presentation.viewmodel.ThemeViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.EaseInBack
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AutoEQPresetPickerBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.DeviceConfigurationBottomSheet
import chromahub.rhythm.app.shared.data.model.AutoEQProfile

// Equalizer Preset Data Class
data class EqualizerPreset(
    val name: String,
    val icon: MaterialSymbolIcon,
    val bands: List<Float>
)

@Composable
fun TunerAnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    chromahub.rhythm.app.features.local.presentation.screens.settings.TunerAnimatedSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier
    )
}

/**
 * Equalizer Screen - Advanced Audio Equalization
 * Full-featured equalizer with presets, frequency bands, and audio effects
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    navController: NavController,
    viewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Collect states from settings
    val equalizerEnabledState by viewModel.equalizerEnabled.collectAsState()
    val equalizerPresetState by viewModel.equalizerPreset.collectAsState()
    val equalizerBandLevelsState by viewModel.equalizerBandLevels.collectAsState()
    val bassBoostEnabledState by viewModel.bassBoostEnabled.collectAsState()
    val bassBoostStrengthState by viewModel.bassBoostStrength.collectAsState()
    val virtualizerEnabledState by viewModel.virtualizerEnabled.collectAsState()
    val virtualizerStrengthState by viewModel.virtualizerStrength.collectAsState()
    val spatializationStatus by viewModel.spatializationStatus.collectAsState()
    val isSpatializationAvailable by viewModel.isSpatializationAvailable.collectAsState()
    val isBassBoostAvailableState by viewModel.isBassBoostAvailable.collectAsState()

    // Update spatialization and bass boost availability when screen is shown
    LaunchedEffect(Unit) {
        viewModel.updateSpatializationStatus()
        viewModel.updateBassBoostAvailability()
    }

    // Local mutable states for UI
    var isEqualizerEnabled by remember(equalizerEnabledState) { mutableStateOf(equalizerEnabledState) }
    var selectedPreset by remember(equalizerPresetState) { mutableStateOf(equalizerPresetState) }
    var bandLevels by remember(equalizerBandLevelsState) {
        mutableStateOf(
            equalizerBandLevelsState.split(",").mapNotNull { it.toFloatOrNull() }.let { levels ->
                when {
                    levels.size == 10 -> levels
                    levels.size == 5 -> List(10) { if (it < 5) levels[it] else 0f }
                    else -> List(10) { 0f }
                }
            }
        )
    }
    var isBassBoostEnabled by remember(bassBoostEnabledState) { mutableStateOf(bassBoostEnabledState) }
    var bassBoostStrength by remember(bassBoostStrengthState) { mutableFloatStateOf(bassBoostStrengthState.toFloat()) }
    var isVirtualizerEnabled by remember(virtualizerEnabledState) { mutableStateOf(virtualizerEnabledState) }
    var virtualizerStrength by remember(virtualizerStrengthState) { mutableFloatStateOf(virtualizerStrengthState.toFloat()) }

    // Preset definitions - Updated to 10 bands with AutoEQ-style precision values
    // Bands: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
    val presets = listOf(
        EqualizerPreset("Flat", MaterialSymbolIcon("linear_scale", filled = true), listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
        EqualizerPreset("Rock", RhythmIcons.MusicNote, listOf(4.5f, 3.8f, 2.5f, 0.5f, -1.5f, -0.8f, 2.2f, 3.5f, 5.5f, 4.0f)),
        EqualizerPreset("Pop", MaterialSymbolIcon("star", filled = true), listOf(-1.5f, 0.5f, 2.8f, 4.2f, 3.5f, 2.0f, 0.5f, 1.5f, 2.5f, 1.0f)),
        EqualizerPreset("Jazz", MaterialSymbolIcon("piano", filled = true), listOf(3.5f, 2.8f, 1.5f, 0.5f, -1.5f, -0.5f, 1.8f, 2.5f, 4.0f, 3.0f)),
        EqualizerPreset("Classical", RhythmIcons.Library, listOf(3.0f, 1.5f, -0.5f, -1.5f, -2.0f, -1.5f, 0.5f, 2.5f, 3.5f, 2.5f)),
        EqualizerPreset("Electronic", MaterialSymbolIcon("graphic_eq", filled = true), listOf(5.5f, 4.8f, 3.5f, 1.5f, 0.5f, 0f, 2.5f, 4.5f, 5.0f, 4.5f)),
        EqualizerPreset("Hip Hop", MaterialSymbolIcon("graphic_eq", filled = true), listOf(6.5f, 5.5f, 3.5f, 1.5f, -0.5f, -0.8f, 1.8f, 3.5f, 4.5f, 3.5f)),
        EqualizerPreset("Vocal", MaterialSymbolIcon("record_voice_over", filled = true), listOf(-1.0f, 0.5f, 1.5f, 2.8f, 4.5f, 5.0f, 4.0f, 2.5f, 1.5f, 0.5f)),
        EqualizerPreset("Bass Boost", RhythmIcons.SpeakerFilled, listOf(6.0f, 5.0f, 3.5f, 1.5f, 0f, 0f, 0f, 0f, 0f, 0f)),
        EqualizerPreset("Treble Boost", MaterialSymbolIcon("waves", filled = true), listOf(0f, 0f, 0f, 0f, 0f, 0.5f, 1.5f, 3.0f, 5.0f, 6.0f)),
        EqualizerPreset("V-Shape", MaterialSymbolIcon("show_chart", filled = true), listOf(5.5f, 4.0f, 1.5f, -1.0f, -2.5f, -2.5f, -0.5f, 2.0f, 4.5f, 5.5f)),
        EqualizerPreset("Harman", RhythmIcons.HeadphonesFilled, listOf(3.5f, 2.0f, 0.5f, -1.0f, 0f, 0.5f, 1.5f, 2.0f, 2.5f, 1.0f))
    )

    val frequencyLabels = listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")

    // Functions
    fun applyPreset(preset: EqualizerPreset) {
        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
        selectedPreset = preset.name
        bandLevels = preset.bands

        // Save to settings
        viewModel.appSettings.setEqualizerPreset(preset.name)
        viewModel.appSettings.setEqualizerBandLevels(preset.bands.joinToString(","))

        // Apply to service
        viewModel.applyEqualizerPreset(preset.name, preset.bands)
    }

    fun updateBandLevel(band: Int, level: Float) {
        val newLevels = bandLevels.toMutableList()
        newLevels[band] = level
        bandLevels = newLevels
        selectedPreset = "Custom"

        // Save to settings
        viewModel.appSettings.setEqualizerBandLevels(newLevels.joinToString(","))
        viewModel.appSettings.setEqualizerPreset("Custom")

        // Apply to service
        val levelShort = (level * 100).toInt().toShort()
        viewModel.setEqualizerBandLevel(band.toShort(), levelShort)
    }

    var showAutoEQSelector by remember { mutableStateOf(false) }
    var showDeviceConfiguration by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val currentAutoEQProfile by viewModel.appSettings.autoEQProfile.collectAsState()

    // Screen entrance animation
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        showContent = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "contentAlpha"
    )

    val contentOffset by animateFloatAsState(
        targetValue = if (showContent) 0f else 30f,
        animationSpec = tween(durationMillis = 450),
        label = "contentOffset"
    )

    CollapsibleHeaderScreen(
        title = "Equalizer",
        showBackButton = true,
        onBackClick = { navController.popBackStack() },
        actions = {
            // More Options Button
            FilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                    showMenu = true
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = RhythmIcons.More,
                    contentDescription = "More options",
                    modifier = Modifier.size(20.dp)
                )
            }
            // Dropdown Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .widthIn(min = 220.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(5.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                // AutoEQ Profiles option
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "AutoEQ Profiles",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = MaterialSymbolIcon("auto_mode", filled = true),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            showMenu = false
                            showAutoEQSelector = true
                        }
                    )
                }

                // Device Configuration option
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Manage Device",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = MaterialSymbolIcon("device_hub", filled = true),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            showMenu = false
                            showDeviceConfiguration = true
                        }
                    )
                }

                // Open System Equalizer option
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "System Equalizer",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.Equalizer,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            showMenu = false
                            val activity = context as? Activity
                            viewModel.openSystemEqualizer(activity, MainActivity.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL_REQUEST)
                        }
                    )
                }
            }
        },
        headerContent = {
            // Equalizer Enable/Disable Card (moved to header)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isEqualizerEnabled)
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
                        imageVector = RhythmIcons.Equalizer,
                        contentDescription = null,
                        tint = if (isEqualizerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when {
                                    isEqualizerEnabled -> "Active"
                                    else -> "Disabled"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    TunerAnimatedSwitch(
                        checked = isEqualizerEnabled,
                        onCheckedChange = { enabled ->
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            isEqualizerEnabled = enabled
                            viewModel.setEqualizerEnabled(enabled)
                        }
                    )
                }
            }
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
                .graphicsLayer {
                    alpha = contentAlpha
                    translationY = contentOffset
                },
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Expressive Presets Section
            item {
                AnimatedVisibility(
                    visible = isEqualizerEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Presets",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (selectedPreset == "Custom") {
                                TextButton(
                                    onClick = { applyPreset(presets[0]) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Refresh,
                                        contentDescription = "Reset to Flat",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reset", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(presets, key = { "preset_${it.name}" }) { preset ->
                                val isSelected = selectedPreset == preset.name

                                Surface(
                                    onClick = { applyPreset(preset) },
                                    shape = RoundedCornerShape(100), // Perfect pill shape
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = preset.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = preset.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Frequency Bands & Chart Section (Expressive Unified Container)
            item {
                AnimatedVisibility(
                    visible = isEqualizerEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 24.dp)
                        ) {
                            Text(
                                text = context.getString(R.string.frequency_bands),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Chart Area (Seamless)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    val secondaryColor = MaterialTheme.colorScheme.secondary
                                    val tertiaryColor = MaterialTheme.colorScheme.tertiary
                                    val outlineColor = MaterialTheme.colorScheme.outline

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val width = size.width
                                            val height = size.height
                                            val bandWidth = width / bandLevels.size

                                            // Draw center line (0dB)
                                            drawLine(
                                                color = outlineColor.copy(alpha = 0.3f),
                                                start = Offset(0f, height / 2),
                                                end = Offset(width, height / 2),
                                                strokeWidth = 2.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                                            )

                                            val points = bandLevels.mapIndexed { index, level ->
                                                val x = (index + 0.5f) * bandWidth
                                                val normalizedLevel = (level + 15f) / 30f
                                                val y = height * (1f - normalizedLevel)
                                                Offset(x, y)
                                            }

                                            if (points.size > 1) {
                                                val filledPath = Path().apply {
                                                    moveTo(0f, height / 2)
                                                    lineTo(points[0].x, points[0].y)
                                                    for (i in 1 until points.size) {
                                                        val p0 = points[i - 1]
                                                        val p1 = points[i]
                                                        val controlX = (p0.x + p1.x) / 2
                                                        quadraticTo(controlX, p0.y, p1.x, p1.y)
                                                    }
                                                    lineTo(width, height / 2)
                                                    close()
                                                }

                                                drawPath(
                                                    path = filledPath,
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(
                                                            primaryColor.copy(alpha = 0.3f),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )

                                                val curvePath = Path().apply {
                                                    moveTo(points[0].x, points[0].y)
                                                    for (i in 1 until points.size) {
                                                        val p0 = points[i - 1]
                                                        val p1 = points[i]
                                                        val controlX = (p0.x + p1.x) / 2
                                                        quadraticTo(controlX, p0.y, p1.x, p1.y)
                                                    }
                                                }

                                                drawPath(
                                                    path = curvePath,
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(secondaryColor, primaryColor, tertiaryColor)
                                                    ),
                                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                                )
                                            }

                                            points.forEachIndexed { index, point ->
                                                val pointColor = when (index) {
                                                    0, 1 -> secondaryColor
                                                    2, 3, 4, 5, 6, 7 -> primaryColor
                                                    else -> tertiaryColor
                                                }
                                                drawCircle(
                                                    color = pointColor,
                                                    radius = 5.dp.toPx(),
                                                    center = point
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Clean Sliders List
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val secondaryColor = MaterialTheme.colorScheme.secondary
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val tertiaryColor = MaterialTheme.colorScheme.tertiary

                                bandLevels.forEachIndexed { index, level ->
                                    val bandColor = when (index) {
                                        0, 1 -> secondaryColor
                                        2, 3, 4, 5, 6, 7 -> primaryColor
                                        else -> tertiaryColor
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Label Side
                                        Column(
                                            modifier = Modifier.width(48.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = frequencyLabels[index],
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Slider
                                        Slider(
                                            value = level,
                                            onValueChange = { newLevel ->
                                                val roundedLevel = (kotlin.math.round(newLevel * 10) / 10f)
                                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                                updateBandLevel(index, roundedLevel)
                                            },
                                            valueRange = -15f..15f,
                                            steps = 299,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = bandColor,
                                                activeTrackColor = bandColor,
                                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                            )
                                        )

                                        // Value Side
                                        Text(
                                            text = if (level > 0) "+${String.format("%.1f", level)}" else String.format("%.1f", level),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = bandColor,
                                            modifier = Modifier.width(40.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Audio Effects Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = context.getString(R.string.audio_effects),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ExpressiveEffectCard(
                            title = "Bass Boost",
                            icon = RhythmIcons.SpeakerFilled,
                            value = bassBoostStrength,
                            valueRange = 0f..1000f,
                            isEnabled = isBassBoostEnabled && isBassBoostAvailableState,
                            isAvailable = isBassBoostAvailableState,
                            activeColor = MaterialTheme.colorScheme.secondary,
                            activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            onActiveContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onValueChange = { strength ->
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                bassBoostStrength = strength
                                viewModel.setBassBoost(true, strength.toInt().toShort())
                            },
                            onEnabledChange = { enabled ->
                                if (isBassBoostAvailableState) {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    isBassBoostEnabled = enabled
                                    viewModel.setBassBoost(enabled, bassBoostStrength.toInt().toShort())
                                }
                            },
                            statusText = if (isBassBoostAvailableState) {
                                if (isBassBoostEnabled) "Active" else "Enhance lows"
                            } else {
                                "Unavailable"
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ExpressiveEffectCard(
                            title = "Spatial Audio",
                            icon = RhythmIcons.HeadphonesFilled,
                            value = virtualizerStrength,
                            valueRange = 0f..1000f,
                            isEnabled = isVirtualizerEnabled && isSpatializationAvailable,
                            isAvailable = isSpatializationAvailable,
                            activeColor = MaterialTheme.colorScheme.tertiary,
                            activeContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            onActiveContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            onValueChange = { strength ->
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                virtualizerStrength = strength
                                viewModel.setVirtualizer(true, strength.toInt().toShort())
                            },
                            onEnabledChange = { enabled ->
                                if (isSpatializationAvailable) {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    isVirtualizerEnabled = enabled
                                    viewModel.setVirtualizer(enabled, virtualizerStrength.toInt().toShort())
                                }
                            },
                            statusText = when {
                                !isSpatializationAvailable -> "Mono only"
                                isVirtualizerEnabled -> "Active"
                                else -> "Widen sound"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Advanced System / AutoEQ Options
            item {
                AnimatedVisibility(
                    visible = isEqualizerEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Material3SettingsGroup(
                        title = "Advanced",
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        items = listOf(
                            Material3SettingsItem(
                                icon = MaterialSymbolIcon("auto_mode", filled = true),
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                iconBackgroundTint = MaterialTheme.colorScheme.primaryContainer,
                                title = {
                                    Text("AutoEQ Profiles", fontWeight = FontWeight.SemiBold)
                                },
                                description = {
                                    Text("Apply headphone-specific equalization")
                                },
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    showAutoEQSelector = true
                                }
                            ),
                            Material3SettingsItem(
                                icon = MaterialSymbolIcon("device_hub", filled = true),
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                iconBackgroundTint = MaterialTheme.colorScheme.secondaryContainer,
                                title = {
                                    Text("Manage AutoEQ", fontWeight = FontWeight.SemiBold)
                                },
                                description = {
                                    Text("Import, export, and organize profiles")
                                },
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    showDeviceConfiguration = true
                                }
                            ),
                            Material3SettingsItem(
                                icon = MaterialSymbolIcon("arrow_outward", filled = true),
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                iconBackgroundTint = MaterialTheme.colorScheme.tertiaryContainer,
                                title = {
                                    Text("System Equalizer", fontWeight = FontWeight.SemiBold)
                                },
                                description = {
                                    Text("Access Android's built-in settings")
                                },
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    val activity = context as? Activity
                                    viewModel.openSystemEqualizer(activity, MainActivity.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL_REQUEST)
                                }
                            )
                        )
                    )
                }
            }
        }

        // Bottom Sheets
        if (showAutoEQSelector) {
            AutoEQPresetPickerBottomSheet(
                currentProfileName = currentAutoEQProfile,
                onDismissRequest = { showAutoEQSelector = false },
                onProfileSelected = { profile: AutoEQProfile ->
                    viewModel.applyAutoEQProfile(profile)
                    showAutoEQSelector = false
                }
            )
        }

        if (showDeviceConfiguration) {
            DeviceConfigurationBottomSheet(
                musicViewModel = viewModel,
                onDismiss = { showDeviceConfiguration = false }
            )
        }
    }
}

@Composable
private fun ExpressiveEffectCard(
    title: String,
    icon: MaterialSymbolIcon,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    isEnabled: Boolean,
    isAvailable: Boolean,
    activeColor: Color,
    activeContainerColor: Color,
    onActiveContainerColor: Color,
    onValueChange: (Float) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    statusText: String,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isEnabled) activeContainerColor else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "containerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isEnabled) onActiveContainerColor else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )

    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(32.dp), // Expressive high-radius shape
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                TunerAnimatedSwitch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange,
                    enabled = isAvailable,
                    modifier = Modifier.scale(0.85f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                ArcProgressSlider(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxSize(),
                    enabled = isEnabled,
                    valueRange = valueRange,
                    activeTrackColor = if (isEnabled) activeColor else MaterialTheme.colorScheme.outlineVariant,
                    inactiveTrackColor = if (isEnabled) activeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                    thumbColor = if (isEnabled) activeColor else MaterialTheme.colorScheme.outline,
                    waveAmplitude = 2.dp
                )

                val percentage = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start) * 100).toInt()
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}