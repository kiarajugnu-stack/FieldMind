@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.components.lyrics

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.shared.data.model.LyricsData
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.presentation.components.common.M3PlaceholderType
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShapeFor
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeTarget
import chromahub.rhythm.app.shared.presentation.components.common.ExpressivePlayerControlGroup
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.M3ImageUtils
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.delay
import chromahub.rhythm.app.R
import androidx.compose.ui.res.stringResource

@Composable
fun FullScreenLyricsView(
    song: Song?,
    isPlaying: Boolean,
    currentTimeMs: Long,
    lyrics: LyricsData?,
    isLoadingLyrics: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onLyricsSeek: ((Long) -> Unit)?,
    onRetryLyrics: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Detect light or dark theme based on the app's theme background luminance
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val baseBgColor = if (isDark) Color.Black else Color.White
    val textPrimaryColor = if (isDark) Color.White else Color.Black
    val textSecondaryColor = if (isDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.65f)
    val glassBgColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.08f)
    val glassBorderColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.12f)

    // Local toggles for translation and romanization
    var showTranslation by remember { mutableStateOf(true) }
    var showRomanization by remember { mutableStateOf(true) }
    
    // Local manual sync offset in milliseconds (real-time offset tuning!)
    var manualSyncOffsetMs by remember { mutableLongStateOf(0L) }

    // Infinite transition for organic background morphing
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundAura")
    
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Orb1Scale"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Orb2Scale"
    )

    val translationX1 by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Orb1X"
    )

    val translationY1 by infiniteTransition.animateFloat(
        initialValue = -120f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Orb1Y"
    )

    // Beautiful slow rotation for a premium rotating cosmic aura background
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(38000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CosmicRotation"
    )

    val artworkScaleState by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ArtworkThumbnailScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBgColor)
    ) {
        // 1. DYNAMIC BACKGROUND: Blurred scaled album art with animated moving orbs
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(56.dp)
                .alpha(0.68f)
        ) {
            // Blurred base cover art
            M3ImageUtils.M3MediaImage(
                data = song?.artworkUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
                type = M3PlaceholderType.TRACK,
                name = song?.title,
                expressiveShape = RoundedCornerShape(0.dp)
            )

            // Dynamic Gradient Overlay 1 (Golden-accented Warm Aura)
            Box(
                modifier = Modifier
                    .size(340.dp)
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        translationX = translationX1
                        translationY = translationY1
                        scaleX = pulseScale1
                        scaleY = pulseScale1
                        rotationZ = rotationAngle
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Dynamic Gradient Overlay 2 (Cooler Toned Aurora Accent)
            Box(
                modifier = Modifier
                    .size(420.dp)
                    .align(Alignment.BottomEnd)
                    .graphicsLayer {
                        translationX = -translationX1 * 0.8f
                        translationY = -translationY1 * 0.9f
                        scaleX = pulseScale2
                        scaleY = pulseScale2
                        rotationZ = -rotationAngle * 1.2f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Dynamic dim layer to boost readability of text overlay based on theme
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.52f),
                                Color.Black.copy(alpha = 0.78f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.72f),
                                Color.White.copy(alpha = 0.52f),
                                Color.White.copy(alpha = 0.78f)
                            )
                        }
                    )
                )
        )

        // 2. MAIN LAYOUT CONTAINER
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // A. TOP BAR: Centered Expressive Artwork and Metadata Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Expressive-shaped Cover Art
                val artShape = rememberExpressiveShapeFor(ExpressiveShapeTarget.PLAYER_ART)
                Surface(
                    shape = artShape,
                    color = glassBgColor,
                    border = BorderStroke(1.dp, glassBorderColor),
                    modifier = Modifier
                        .size(108.dp)
                        .graphicsLayer {
                            scaleX = artworkScaleState
                            scaleY = artworkScaleState
                        }
                ) {
                    M3ImageUtils.M3MediaImage(
                        data = song?.artworkUri,
                        contentDescription = stringResource(R.string.fullscreenlyricsview_cover_artwork),
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        type = M3PlaceholderType.TRACK,
                        name = song?.title,
                        expressiveShape = artShape
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Centered song title
                Text(
                    text = song?.title ?: "Unknown Song",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor,
                        letterSpacing = 0.1.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Centered artist name
                Text(
                    text = song?.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = textSecondaryColor
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }

            // B. SCROLLING LYRICS AREA (with floating controls)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val hasLyrics = lyrics?.hasLyrics() == true && lyrics.isErrorMessage().not()

                when {
                    isLoadingLyrics -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.fullscreenlyricsview_loading_synced_lyrics),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textSecondaryColor
                                )
                            }
                        }
                    }

                    !hasLyrics -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.MusicNote,
                                    contentDescription = null,
                                    tint = textSecondaryColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = stringResource(R.string.fullscreenlyricsview_lyrics_arent_available_for),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimaryColor,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onRetryLyrics,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(stringResource(R.string.fullscreenlyricsview_retry_fetching))
                                }
                            }
                        }
                    }

                    else -> {
                        val wordByWordLyrics = remember(lyrics) {
                            lyrics?.getWordByWordLyricsOrNull()
                        }

                        if (wordByWordLyrics != null) {
                            WordByWordLyricsView(
                                wordByWordLyrics = wordByWordLyrics,
                                currentPlaybackTime = currentTimeMs,
                                syncOffset = manualSyncOffsetMs,
                                modifier = Modifier.fillMaxSize(),
                                onSeek = onLyricsSeek,
                                lyricsSource = lyrics?.source,
                                textSizeMultiplier = 1.25f,
                                textAlignment = TextAlign.Center,
                                showTranslation = showTranslation,
                                showRomanization = showRomanization
                            )
                        } else {
                            val lyricsText = remember(lyrics) {
                                lyrics?.getBestLyrics() ?: ""
                            }
                            SyncedLyricsView(
                                lyrics = lyricsText,
                                currentPlaybackTime = currentTimeMs,
                                syncOffset = manualSyncOffsetMs,
                                modifier = Modifier.fillMaxSize(),
                                onSeek = onLyricsSeek,
                                showTranslation = showTranslation,
                                showRomanization = showRomanization,
                                lyricsSource = lyrics?.source,
                                textSizeMultiplier = 1.25f,
                                textAlignment = TextAlign.Center
                            )
                        }
                    }
                }

                // Floating Romanization and Translation Stack
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Toggle Romanization
                    FloatingLyricOptionButton(
                        icon = MaterialSymbolIcon("translate"),
                        contentDescription = stringResource(R.string.fullscreenlyricsview_romanization),
                        isActive = showRomanization,
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            showRomanization = !showRomanization
                        },
                        isDark = isDark
                    )

                    // Quick Toggle Translation
                    FloatingLyricOptionButton(
                        icon = MaterialSymbolIcon("subtitles"),
                        contentDescription = stringResource(R.string.fullscreenlyricsview_translation),
                        isActive = showTranslation,
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            showTranslation = !showTranslation
                        },
                        isDark = isDark
                    )
                }
            }

            // C. SYNC TUNER DOCK: Translucent slider or quick buttons to tweak sync offset in real time
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = glassBgColor,
                border = BorderStroke(1.dp, glassBorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.fullscreenlyricsview_realtime_sync_adjustment),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textSecondaryColor
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Nudge Backwards
                        val nudgeBackInteractionSource = remember { MutableInteractionSource() }
                        val nudgeBackPressed by nudgeBackInteractionSource.collectIsPressedAsState()
                        val nudgeBackScale by animateFloatAsState(
                            targetValue = if (nudgeBackPressed) 0.90f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "nudge_back_bounce"
                        )
                        Surface(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                manualSyncOffsetMs -= 500L
                            },
                            shape = CircleShape,
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.12f),
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer {
                                    scaleX = nudgeBackScale
                                    scaleY = nudgeBackScale
                                },
                            interactionSource = nudgeBackInteractionSource
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(stringResource(R.string.fullscreenlyricsview_str_05s), color = textPrimaryColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        // Display Current Offset
                        val offsetSeconds = manualSyncOffsetMs / 1000f
                        Text(
                            text = String.format("%+.1fs", offsetSeconds),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        // Nudge Forwards
                        val nudgeForwardInteractionSource = remember { MutableInteractionSource() }
                        val nudgeForwardPressed by nudgeForwardInteractionSource.collectIsPressedAsState()
                        val nudgeForwardScale by animateFloatAsState(
                            targetValue = if (nudgeForwardPressed) 0.90f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "nudge_forward_bounce"
                        )
                        Surface(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                                manualSyncOffsetMs += 500L
                            },
                            shape = CircleShape,
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.12f),
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer {
                                    scaleX = nudgeForwardScale
                                    scaleY = nudgeForwardScale
                                },
                            interactionSource = nudgeForwardInteractionSource
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(stringResource(R.string.fullscreenlyricsview_str_05s_1), color = textPrimaryColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        // Reset Button
                        if (manualSyncOffsetMs != 0L) {
                            Icon(
                                imageVector = RhythmIcons.Restore,
                                contentDescription = stringResource(R.string.fullscreenlyricsview_reset_offset),
                                tint = textSecondaryColor,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(20.dp)
                                    .bouncyClickable {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                        manualSyncOffsetMs = 0L
                                    }
                            )
                        }
                    }
                }
            }

            // D. GLASSMORPHIC CONTROL DOCK: Integrated with the official material ExpressivePlayerControlGroup
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                ExpressivePlayerControlGroup(
                    isPlaying = isPlaying,
                    showSeekButtons = false,
                    onPrevious = onSkipPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onSkipNext,
                    onSeekBack = {},
                    onSeekForward = {},
                    useGlassEffect = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // E. FLOATING CLOSE BUTTON: Pill at the top right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 20.dp)
        ) {
            val closeInteractionSource = remember { MutableInteractionSource() }
            val closePressed by closeInteractionSource.collectIsPressedAsState()
            val closeScale by animateFloatAsState(
                targetValue = if (closePressed) 0.90f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "close_bounce"
            )
            Surface(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                    onClose()
                },
                shape = CircleShape,
                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.1f),
                border = BorderStroke(1.dp, (if (isDark) Color.White else Color.Black).copy(alpha = 0.12f)),
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = closeScale
                        scaleY = closeScale
                    },
                interactionSource = closeInteractionSource
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = RhythmIcons.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.onboarding_dismiss),
                        tint = textPrimaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingLyricOptionButton(
    icon: MaterialSymbolIcon,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "floating_lyric_btn_scale"
    )
    
    val activeBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    val inactiveBgColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.08f)
    val activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    val inactiveBorderColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.12f)
    
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isActive) activeBgColor else inactiveBgColor,
        border = BorderStroke(1.dp, if (isActive) activeBorderColor else inactiveBorderColor),
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) MaterialTheme.colorScheme.primary else (if (isDark) Color.White else Color.Black).copy(alpha = 0.65f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncy_click"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}
