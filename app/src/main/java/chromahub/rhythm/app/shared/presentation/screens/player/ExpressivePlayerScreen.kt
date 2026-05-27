package chromahub.rhythm.app.shared.presentation.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.ExtraControlBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.AddToPlaylistBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.AlbumBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.ArtistBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.PlaybackBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.QueueBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.SongInfoBottomSheet
import chromahub.rhythm.app.shared.presentation.components.dialogs.PlaybackPitchDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.PlaybackSpeedDialog
import chromahub.rhythm.app.shared.presentation.components.player.SleepTimerBottomSheetNew
import chromahub.rhythm.app.shared.presentation.components.lyrics.LyricsEditorBottomSheet
import chromahub.rhythm.app.shared.presentation.components.lyrics.SyncedLyricsView
import chromahub.rhythm.app.shared.presentation.components.lyrics.WordByWordLyricsView
import chromahub.rhythm.app.shared.presentation.components.player.formatDuration
import chromahub.rhythm.app.features.local.presentation.navigation.Screen
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.model.LyricsData
import chromahub.rhythm.app.shared.data.model.PlaybackLocation
import chromahub.rhythm.app.shared.data.model.Playlist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.presentation.components.common.AutoScrollingTextOnDemand
import chromahub.rhythm.app.shared.presentation.components.common.ButtonGroupStyle
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveButtonGroup
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveGroupButton
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeTarget
import chromahub.rhythm.app.shared.presentation.components.common.FixedHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.common.M3LinearLoader
import chromahub.rhythm.app.shared.presentation.components.common.M3PlaceholderType
import chromahub.rhythm.app.shared.presentation.components.common.PlaybackBufferingLoader
import chromahub.rhythm.app.shared.presentation.components.common.ProgressStyle
import chromahub.rhythm.app.shared.presentation.components.common.StyledProgressBar
import chromahub.rhythm.app.shared.presentation.components.common.ThumbStyle
import chromahub.rhythm.app.shared.presentation.components.common.WaveSlider
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShapeFor
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.M3ImageUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressivePlayerScreen(
    song: Song?,
    isPlaying: Boolean,
    isFavorite: Boolean,
    progress: () -> Float,
    currentTimeStr: String,
    totalTimeStr: String,
    queuePosition: Int,
    queueTotal: Int,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    showLyricsView: Boolean,
    showLyrics: Boolean,
    lyrics: LyricsData?,
    isLoadingLyrics: Boolean,
    onlineOnlyLyrics: Boolean,
    onLyricsSeek: ((Long) -> Unit)?,
    onRetryLyrics: () -> Unit,
    onShowLyricsEditor: () -> Unit,
    isMediaLoading: Boolean,
    isSeeking: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLyrics: () -> Unit,
    onSongInfoClick: () -> Unit,
    onShowAlbumBottomSheet: () -> Unit,
    onShowArtistBottomSheet: () -> Unit,
    onMoreClick: () -> Unit,
    onDeviceClick: () -> Unit,
    onQueueClick: () -> Unit,
    onBack: () -> Unit,
    location: PlaybackLocation?,
    appSettings: AppSettings,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ArtworkScale"
    )

    val artworkCornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 32.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ArtworkCornerRadius"
    )

    val playerArtworkShape = rememberExpressiveShapeFor(
        ExpressiveShapeTarget.PLAYER_ART,
        RoundedCornerShape(artworkCornerRadius)
    )

    val playerControlShape = rememberExpressiveShapeFor(
        ExpressiveShapeTarget.PLAYER_CONTROLS,
        CircleShape
    )

    val playerProgressStyle by appSettings.playerProgressStyle.collectAsState()
    val playerProgressThumbStyle by appSettings.playerProgressThumbStyle.collectAsState()
    val enhancedSeekingEnabled by appSettings.enhancedSeekingEnabled.collectAsState()
    val playerLyricsTextSize by appSettings.playerLyricsTextSize.collectAsState()
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }
    val progressValue = progress().coerceIn(0f, 1f)
    val currentTimeMs = (progressValue * (song?.duration ?: 0L)).toLong()
    val lyricsVisible = showLyricsView && showLyrics
    val showBuffering = isMediaLoading || isSeeking
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val artworkClipShape = if (lyricsVisible) {
        RoundedCornerShape(artworkCornerRadius)
    } else {
        playerArtworkShape
    }

    val controlsContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val primaryPillColor = MaterialTheme.colorScheme.primaryContainer
    val primaryPillOnColor = MaterialTheme.colorScheme.onPrimaryContainer
    val secondaryButtonColor = MaterialTheme.colorScheme.secondaryContainer
    val secondaryButtonOnColor = MaterialTheme.colorScheme.onSecondaryContainer

    val configuration = LocalConfiguration.current
    val isCompactWidth = configuration.screenWidthDp < 360
    val isCompactHeight = configuration.screenHeightDp < 640
    val songTitle = song?.title ?: "Unknown Track"
    val songArtist = song?.artist ?: "Unknown Artist"
    val titleLength = songTitle.length
    val titleLetterSpacing = when {
        isCompactWidth || titleLength > 32 -> (-0.6).sp
        titleLength > 24 -> (-1.0).sp
        else -> (-1.5).sp
    }
    val titleTextStyle = when {
        isCompactWidth -> MaterialTheme.typography.headlineSmall
        isCompactHeight -> MaterialTheme.typography.headlineMedium
        titleLength > 32 -> MaterialTheme.typography.headlineSmall
        titleLength > 24 -> MaterialTheme.typography.headlineMedium
        else -> MaterialTheme.typography.displaySmall
    }.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = titleLetterSpacing
    )

    val coroutineScope = rememberCoroutineScope()
    val screenHeightPx = with(LocalDensity.current) {
        configuration.screenHeightDp.dp.toPx()
    }

    var swipeOffsetY by remember { mutableStateOf(0f) }
    var isDraggingSwipe by remember { mutableStateOf(false) }
    var isSwipeMinimizing by remember { mutableStateOf(false) }

    val swipeDismissThreshold = screenHeightPx * 0.16f
    val swipeDismissTarget = screenHeightPx * 1.05f
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffsetY,
        animationSpec = when {
            isDraggingSwipe -> spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
            isSwipeMinimizing -> tween(durationMillis = 160, easing = EaseInOut)
            else -> spring(
                dampingRatio = 0.84f,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "rhythmPlayerSwipeOffset"
    )

    val swipeCornerRadius by animateFloatAsState(
        targetValue = when {
            isDraggingSwipe || isSwipeMinimizing -> 64f
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "swipeCornerRadius"
    )
    val clampedSwipeCornerRadius = swipeCornerRadius.coerceAtLeast(0f)

    val swipeMinimizeModifier = modifier
        .graphicsLayer {
            val swipeProgress = (animatedSwipeOffset / screenHeightPx).coerceIn(0f, 1f)
            translationY = animatedSwipeOffset
            val scaleTarget = 1f - (swipeProgress * 0.15f)
            scaleX = scaleTarget
            scaleY = scaleTarget
            alpha = (1f - (swipeProgress * 1.5f)).coerceIn(0f, 1f)
            clip = true
            shape = RoundedCornerShape(clampedSwipeCornerRadius.dp)
        }
        .pointerInput(screenHeightPx) {
            detectVerticalDragGestures(
                onDragStart = {
                    isDraggingSwipe = true
                    isSwipeMinimizing = false
                },
                onVerticalDrag = { change, dragAmount ->
                    if (dragAmount > 0f) {
                        change.consume()
                        val currentSwipeProgress = (swipeOffsetY / screenHeightPx).coerceIn(0f, 1f)
                        val dragResistance = (1f - (currentSwipeProgress * 0.5f)).coerceAtLeast(0.4f)
                        swipeOffsetY = (swipeOffsetY + dragAmount * dragResistance).coerceIn(0f, swipeDismissTarget)
                    }
                },
                onDragEnd = {
                    isDraggingSwipe = false
                    if (swipeOffsetY > swipeDismissThreshold) {
                        HapticUtils.performHapticFeedback(
                            context,
                            haptic,
                            HapticFeedbackType.LongPress
                        )
                        isSwipeMinimizing = true
                        swipeOffsetY = swipeDismissTarget
                        coroutineScope.launch {
                            delay(180)
                            onBack()
                            isSwipeMinimizing = false
                            swipeOffsetY = 0f
                        }
                    } else {
                        isSwipeMinimizing = false
                        swipeOffsetY = 0f
                    }
                },
                onDragCancel = {
                    isDraggingSwipe = false
                    isSwipeMinimizing = false
                    swipeOffsetY = 0f
                }
            )
        }

    FixedHeaderScreen(
        title = "",
        showBackButton = true,
        onBackClick = onBack,
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                song?.album?.takeIf { it.isNotBlank() }?.let { albumName ->
                    AutoScrollingTextOnDemand(
                        text = albumName,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        gradientEdgeColor = MaterialTheme.colorScheme.surface,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .widthIn(max = 140.dp)
                            .clickable { onShowAlbumBottomSheet() },
                        respectGlobalSetting = true
                    )
                }
                ExpressiveButtonGroup() {
                    ExpressiveGroupButton(
                        onClick = onSongInfoClick,
                        isStart = true,
                        isEnd = false,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Info,
                            contentDescription = "Song Info",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    ExpressiveGroupButton(
                        onClick = onMoreClick,
                        isStart = false,
                        isEnd = true,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = RhythmIcons.More,
                            contentDescription = "More",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        screenModifier = swipeMinimizeModifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) { contentModifier ->
        BoxWithConstraints(
            modifier = contentModifier
                .fillMaxSize()
                .padding(
                    top = 8.dp,
                    bottom = 0.dp,
                    start = if (isCompactWidth) 12.dp else 24.dp,
                    end = if (isCompactWidth) 12.dp else 24.dp
                )
        ) {
            val containerMaxWidth = maxWidth
            val containerMaxHeight = maxHeight
            val isPortraitLocal = containerMaxHeight > containerMaxWidth

            // Adjust proportions dynamically for compact sizes
            val artworkHeight = if (isPortraitLocal) {
                if (isCompactHeight) containerMaxHeight * 0.4f else containerMaxHeight * 0.48f
            } else {
                containerMaxHeight * 0.65f
            }

            val controlButtonSize = if (isPortraitLocal) {
                (containerMaxWidth * 0.2f).coerceIn(48.dp, 80.dp)
            } else {
                72.dp
            }

            val smallControlSize = if (isPortraitLocal) {
                (containerMaxWidth * 0.16f).coerceIn(40.dp, 64.dp)
            } else {
                56.dp
            }

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var artworkOffsetX by remember { mutableStateOf(0f) }
                val artworkSwipeThreshold = 140f
                val artworkTranslationX by animateFloatAsState(
                    targetValue = artworkOffsetX.coerceIn(-200f, 200f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "artworkTranslationX"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(artworkHeight)
                            .graphicsLayer {
                                scaleX = artworkScale
                                scaleY = artworkScale
                                shadowElevation = if (isPlaying) 0.dp.toPx() else 0.dp.toPx()
                                translationX = artworkTranslationX
                                shape = artworkClipShape
                                clip = true
                            }
                            .pointerInput(showLyrics, lyricsVisible) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        HapticUtils.performHapticFeedback(
                                            context,
                                            haptic,
                                            HapticFeedbackType.LongPress
                                        )
                                        onPlayPause()
                                    },
                                    onTap = {
                                        if (showLyrics) {
                                            HapticUtils.performHapticFeedback(
                                                context,
                                                haptic,
                                                HapticFeedbackType.TextHandleMove
                                            )
                                            onToggleLyrics()
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        if (artworkOffsetX < -artworkSwipeThreshold) {
                                            HapticUtils.performHapticFeedback(
                                                context,
                                                haptic,
                                                HapticFeedbackType.LongPress
                                            )
                                            onSkipNext()
                                        } else if (artworkOffsetX > artworkSwipeThreshold) {
                                            HapticUtils.performHapticFeedback(
                                                context,
                                                haptic,
                                                HapticFeedbackType.LongPress
                                            )
                                            onSkipPrevious()
                                        }
                                        artworkOffsetX = 0f
                                    },
                                    onDragCancel = { artworkOffsetX = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        artworkOffsetX += dragAmount.x
                                    }
                                )
                            }
                    ) {

                        AnimatedContent(
                            targetState = lyricsVisible,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f)) togetherWith
                                        (fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.95f))
                            },
                            label = "LyricsTransition"
                        ) { isShowingLyrics ->
                            if (isShowingLyrics) {
                                RhythmPlayerLyricsPanel(
                                    lyrics = lyrics,
                                    isLoadingLyrics = isLoadingLyrics,
                                    onlineOnlyLyrics = onlineOnlyLyrics,
                                    currentTimeMs = currentTimeMs,
                                    onLyricsSeek = onLyricsSeek,
                                    textSizeMultiplier = playerLyricsTextSize,
                                    onRetryLyrics = onRetryLyrics,
                                    onShowLyricsEditor = onShowLyricsEditor,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                M3ImageUtils.M3MediaImage(
                                    data = song?.artworkUri,
                                    contentDescription = "Album Artwork",
                                    modifier = Modifier.fillMaxSize(),
                                    shape = artworkClipShape,
                                    type = M3PlaceholderType.TRACK,
                                    name = song?.title,
                                    expressiveShape = if (lyricsVisible) null else playerArtworkShape
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = if (isCompactHeight) 8.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isCompactHeight) 12.dp else 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            AutoScrollingTextOnDemand(
                                text = songTitle,
                                style = titleTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                                gradientEdgeColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth(),
                                respectGlobalSetting = true
                            )
                            AutoScrollingTextOnDemand(
                                text = songArtist,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                gradientEdgeColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onShowArtistBottomSheet() },
                                respectGlobalSetting = true
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        val lyricsContainerColor by animateColorAsState(
                            targetValue = if (showLyricsView) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            label = "LyricsContainerColor"
                        )
                        val lyricsContentColor by animateColorAsState(
                            targetValue = if (showLyricsView) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "LyricsContentColor"
                        )
                        val favContainerColor by animateColorAsState(
                            targetValue = if (isFavorite) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            label = "FavContainerColor"
                        )
                        val favContentColor by animateColorAsState(
                            targetValue = if (isFavorite) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "FavContentColor"
                        )

                        ExpressiveButtonGroup() {
                            ExpressiveGroupButton(
                                onClick = onToggleLyrics,
                                isStart = true,
                                isEnd = false,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = lyricsContainerColor,
                                    contentColor = lyricsContentColor
                                ),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 16.dp)
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.Player.Lyrics,
                                    contentDescription = "Lyrics",
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                            ExpressiveGroupButton(
                                onClick = onToggleFavorite,
                                isStart = false,
                                isEnd = true,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = favContainerColor,
                                    contentColor = favContentColor
                                ),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 16.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) RhythmIcons.FavoriteFilled else RhythmIcons.Favorite,
                                    contentDescription = "Toggle Favorite",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = controlsContainerColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(if (isCompactWidth) 12.dp else 20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 8.dp else 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = onPlayPause,
                                    shape = CircleShape,
                                    color = primaryPillColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(controlButtonSize)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (showBuffering) {
                                            PlaybackBufferingLoader(
                                                modifier = Modifier.size(40.dp),
                                                color = primaryPillOnColor
                                            )
                                        } else {
                                            Text(
                                                text = if (isPlaying) "Pause" else "Play",
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = if (isCompactWidth) 20.sp else MaterialTheme.typography.headlineMedium.fontSize
                                                ),
                                                color = primaryPillOnColor
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    onClick = onSkipNext,
                                    shape = playerControlShape,
                                    color = secondaryButtonColor,
                                    modifier = Modifier.size(controlButtonSize)
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Player.SkipNext,
                                        contentDescription = "Next Track",
                                        modifier = Modifier.padding(if (isCompactWidth) 16.dp else 24.dp),
                                        tint = secondaryButtonOnColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 8.dp else 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = onSkipPrevious,
                                    shape = playerControlShape,
                                    color = secondaryButtonColor,
                                    modifier = Modifier.size(controlButtonSize)
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Player.SkipPrevious,
                                        contentDescription = "Previous Track",
                                        modifier = Modifier.padding(if (isCompactWidth) 16.dp else 24.dp),
                                        tint = secondaryButtonOnColor
                                    )
                                }

                                val canSeek = (song?.duration ?: 0L) > 0L

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (showBuffering) {
                                        M3LinearLoader(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp),
                                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                        )
                                    } else if (playerProgressStyle == "WAVY") {
                                        WaveSlider(
                                            value = if (isScrubbing && enhancedSeekingEnabled) scrubProgress else progressValue,
                                            onValueChange = { newValue ->
                                                if (canSeek && enhancedSeekingEnabled) {
                                                    isScrubbing = true
                                                    scrubProgress = newValue
                                                } else if (canSeek) {
                                                    onSeek(newValue)
                                                }
                                            },
                                            onValueChangeFinished = {
                                                if (canSeek && enhancedSeekingEnabled && isScrubbing) {
                                                    onSeek(scrubProgress)
                                                    isScrubbing = false
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = canSeek,
                                            isPlaying = isPlaying,
                                            activeTrackColor = primaryPillColor,
                                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.2f
                                            )
                                        )
                                    } else {
                                        val progressStyle = try {
                                            ProgressStyle.valueOf(playerProgressStyle)
                                        } catch (e: IllegalArgumentException) {
                                            ProgressStyle.NORMAL
                                        }
                                        val thumbStyle = try {
                                            ThumbStyle.valueOf(playerProgressThumbStyle)
                                        } catch (e: IllegalArgumentException) {
                                            ThumbStyle.CIRCLE
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            StyledProgressBar(
                                                progress = progressValue,
                                                style = progressStyle,
                                                modifier = Modifier.fillMaxWidth(),
                                                progressColor = primaryPillColor,
                                                trackColor = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.2f
                                                ),
                                                height = when (progressStyle) {
                                                    ProgressStyle.THIN -> 2.dp
                                                    ProgressStyle.THICK -> 12.dp
                                                    else -> 8.dp
                                                },
                                                isPlaying = isPlaying,
                                                showThumb = thumbStyle != ThumbStyle.NONE,
                                                thumbStyle = thumbStyle,
                                                thumbSize = 14.dp,
                                                waveAmplitudeWhenPlaying = 3.dp,
                                                waveLength = 60.dp
                                            )

                                            Slider(
                                                value = progressValue,
                                                onValueChange = { onSeek(it) },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = canSeek,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color.Transparent,
                                                    activeTrackColor = Color.Transparent,
                                                    inactiveTrackColor = Color.Transparent
                                                )
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = currentTimeStr,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = totalTimeStr,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 12.dp else 24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        val deviceIcon = when {
                            location?.id?.startsWith("bt_") == true -> RhythmIcons.BluetoothFilled
                            location?.id == "wired_headset" -> RhythmIcons.HeadphonesFilled
                            location?.id == "speaker" -> RhythmIcons.SpeakerFilled
                            else -> RhythmIcons.Location
                        }
                        val queueLabel =
                            if (queueTotal > 0) "Queue $queuePosition/$queueTotal" else "Queue"

                        Surface(
                            onClick = onDeviceClick,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = deviceIcon,
                                    contentDescription = "Device",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                AutoScrollingTextOnDemand(
                                    text = location?.name ?: "Output",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    gradientEdgeColor = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.widthIn(max = 160.dp),
                                    respectGlobalSetting = true
                                )
                            }
                        }

                        Surface(
                            onClick = onQueueClick,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.Queue,
                                    contentDescription = "Queue",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                AutoScrollingTextOnDemand(
                                    text = queueLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    gradientEdgeColor = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.widthIn(max = 160.dp),
                                    respectGlobalSetting = true
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
private fun RhythmPlayerLyricsPanel(
    lyrics: LyricsData?,
    isLoadingLyrics: Boolean,
    onlineOnlyLyrics: Boolean,
    currentTimeMs: Long,
    onLyricsSeek: ((Long) -> Unit)?,
    textSizeMultiplier: Float,
    onRetryLyrics: () -> Unit,
    onShowLyricsEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hasLyrics = lyrics?.hasLyrics() == true && lyrics.isErrorMessage().not()
    val textAlignment = TextAlign.Center

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoadingLyrics -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = context.getString(R.string.player_loading_lyrics),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            !hasLyrics -> {
                val message = if (onlineOnlyLyrics) {
                    "Currently no lyrics are available for this song.\n"
                } else {
                    "No lyrics available for this song."
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = RhythmIcons.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = textAlignment
                    )
                    if (!isLoadingLyrics) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ExpressiveButtonGroup() {
                            ExpressiveGroupButton(
                                onClick = {
                                    HapticUtils.performHapticFeedback(
                                        context,
                                        haptic,
                                        HapticFeedbackType.LongPress
                                    )
                                    onRetryLyrics()
                                },
                                isStart = true,
                                isEnd = false
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                            ExpressiveGroupButton(
                                onClick = {
                                    HapticUtils.performHapticFeedback(
                                        context,
                                        haptic,
                                        HapticFeedbackType.LongPress
                                    )
                                    onShowLyricsEditor()
                                },
                                isStart = false,
                                isEnd = true
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.Player.Lyrics,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add")
                            }
                        }
                    }
                }
            }

            else -> {
                val wordByWord = lyrics?.getWordByWordLyricsOrNull()
                val synced = lyrics?.getSyncedLyricsOrNull()
                val plain = lyrics?.getPlainLyricsOrNull()

                when {
                    !wordByWord.isNullOrBlank() -> {
                        WordByWordLyricsView(
                            wordByWordLyrics = wordByWord,
                            currentPlaybackTime = currentTimeMs,
                            onSeek = onLyricsSeek,
                            textSizeMultiplier = textSizeMultiplier,
                            textAlignment = textAlignment,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    !synced.isNullOrBlank() -> {
                        SyncedLyricsView(
                            lyrics = synced,
                            currentPlaybackTime = currentTimeMs,
                            onSeek = onLyricsSeek,
                            textSizeMultiplier = textSizeMultiplier,
                            textAlignment = textAlignment,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    !plain.isNullOrBlank() -> {
                        val baseStyle = MaterialTheme.typography.bodyLarge
                        Text(
                            text = plain,
                            style = baseStyle.copy(
                                fontSize = baseStyle.fontSize * textSizeMultiplier,
                                lineHeight = baseStyle.lineHeight * 1.4f * textSizeMultiplier
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = textAlignment,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp)
                        )
                    }

                    else -> {
                        Text(
                            text = "No lyrics available for this song.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = textAlignment
                        )
                    }
                }
            }
        }
    }
}

