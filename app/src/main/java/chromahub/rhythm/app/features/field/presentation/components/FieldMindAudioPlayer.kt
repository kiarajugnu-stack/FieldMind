package fieldmind.research.app.features.field.presentation.components

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import java.util.concurrent.TimeUnit

// ── Waveform constants ──
private const val WAVEFORM_BAR_COUNT = 80
private val WaveformBarGap = 0.18f // fraction of bar width used as gap

/**
 * Full-screen dialog for playing audio files in-app.
 * Features waveform visualization, playback controls, and seek.
 */
@Composable
fun AudioPlayerDialog(
    uri: String,
    title: String = "Audio",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    var isPrepared by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    // ── MediaPlayer lifecycle ──
    val mediaPlayer = remember(uri) {
        MediaPlayer().apply {
            try {
                setDataSource(context, Uri.parse(uri))
                setOnPreparedListener { mp ->
                    duration = mp.duration.coerceAtLeast(0)
                    isPrepared = true
                }
                setOnErrorListener { _, _, _ -> error = true; true }
                setOnCompletionListener {
                    isPlaying = false
                    currentPosition = 0
                }
                prepareAsync()
            } catch (_: Exception) {
                error = true
            }
        }
    }

    // Cleanup on dismiss
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            }
        }
    }

    // ── Position update ticker ──
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(250)
            runCatching {
                if (mediaPlayer.isPlaying) {
                    currentPosition = mediaPlayer.currentPosition
                }
            }
        }
    }

    fun togglePlayPause() {
        runCatching {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                isPlaying = false
            } else {
                mediaPlayer.start()
                isPlaying = true
            }
        }
    }

    fun seekTo(ms: Int) {
        runCatching {
            mediaPlayer.seekTo(ms)
            currentPosition = ms
        }
    }

    fun formatTime(ms: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms.toLong()) % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f

    // ── Generate realistic waveform data ──
    val waveformBars = remember(duration) {
        generateWaveformBars(WAVEFORM_BAR_COUNT, seed = uri.hashCode())
    }

    Dialog(
        onDismissRequest = {
            runCatching { mediaPlayer.stop() }
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Top close button ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = {
                        runCatching { mediaPlayer.stop() }
                        onDismiss()
                    }) {
                        Icon(
                            MaterialSymbolIcon("close"),
                            contentDescription = "Close",
                            tint = Color.White,
                            size = 24.dp
                        )
                    }
                }

                when {
                    error -> {
                        Spacer(Modifier.height(48.dp))
                        Icon(
                            MaterialSymbolIcon("music_off"),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            size = 64.dp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Could not play this audio file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    !isPrepared -> {
                        Spacer(Modifier.height(48.dp))
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Loading…",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    else -> {
                        // ── Title ──
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        // ── Waveform visualization ──
                        WaveformCanvas(
                            bars = waveformBars,
                            progress = progress,
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val tapProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                        seekTo((tapProgress * duration).toInt())
                                    }
                                }
                        )

                        // ── Time labels ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                formatTime(currentPosition),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                formatTime(duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        // ── Play/Pause button ──
                        IconButton(
                            onClick = { togglePlayPause() },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        MaterialSymbolIcon(if (isPlaying) "pause" else "play_arrow"),
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        size = 40.dp
                                    )
                                }
                            }
                        }

                        // ── Restart button ──
                        TextButton(onClick = { seekTo(0) }) {
                            Icon(
                                MaterialSymbolIcon("replay"),
                                null,
                                tint = Color.White.copy(alpha = 0.6f),
                                size = 16.dp
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                "Restart",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Waveform data generation
// ══════════════════════════════════════════════════════════════════════

/**
 * Generates a realistic-looking waveform as an array of normalised bar heights
 * (0f…1f). Uses the hash of the URI as a seed so the same file always gets
 * the same pattern.
 *
 * The algorithm blends:
 *  - a sine-based envelope (louder at the middle, quieter at ends)
 *  - a low-frequency modulation (creates "sections")
 *  - high-frequency noise for micro-detail
 */
private fun generateWaveformBars(
    count: Int,
    seed: Int = 42,
    envelopeStrength: Float = 0.5f,
    noiseStrength: Float = 0.5f
): FloatArray {
    val rng = Random(seed)
    val bars = FloatArray(count)
    for (i in 0 until count) {
        val t = i.toFloat() / count
        // Envelope: quiet at ends, loud in the middle (sine window)
        val td = t.toDouble()
        val envelope = sin(td * PI).toFloat() * envelopeStrength + (1f - envelopeStrength)
        // Low-frequency modulation for "sections"
        val lfMod = (sin(td * 4.0 * PI) * 0.3f + sin(td * 7.0 * PI + 1.0) * 0.2f).coerceIn(-0.5f, 0.5f)
        // Noise for organic micro-variation
        val noise = (rng.nextFloat() - 0.5f) * 2f * noiseStrength
        // Combine, clamp, and apply a floor so no bar is totally flat
        bars[i] = ((envelope + lfMod + noise) / 2f).coerceIn(0.08f, 1f)
    }
    return bars
}

// ══════════════════════════════════════════════════════════════════════
//  Waveform Canvas composable
// ══════════════════════════════════════════════════════════════════════

/**
 * Renders the waveform bars using Compose [Canvas].
 *
 * - **Played** portion (left of progress) is a vibrant gradient
 * - **Unplayed** portion is dim white
 * - A bright vertical **position indicator** line tracks playback
 * - Bars near the playback position have a subtle **glow** effect
 * - While playing, the waveform has a gentle **pulse** animation
 */
@Composable
private fun WaveformCanvas(
    bars: FloatArray,
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val playedGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF7C4DFF), // purple
            Color(0xFF448AFF), // blue
            Color(0xFF03DAC6)  // teal
        )
    )
    val whiteDim = Color.White.copy(alpha = 0.25f)
    val indicatorColor = Color.White
    val glowColor = Color(0xFF7C4DFF).copy(alpha = 0.25f)

    // Pulse animation: gentle opacity oscillation while playing
    val pulseAnim = remember { Animatable(1f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                pulseAnim.animateTo(0.85f, animationSpec = tween(600))
                pulseAnim.animateTo(1f, animationSpec = tween(600))
            }
        } else {
            pulseAnim.snapTo(1f)
        }
    }
    val pulse = pulseAnim.value

    Canvas(modifier = modifier) {
        val barCount = bars.size
        val totalBarWidth = size.width / barCount
        val gapWidth = totalBarWidth * WaveformBarGap
        val barWidth = totalBarWidth * (1f - WaveformBarGap)
        val halfHeight = size.height / 2f
        val maxBarHeight = halfHeight * 0.85f

        for (i in 0 until barCount) {
            val barHeight = bars[i] * maxBarHeight * pulse
            val x = i * totalBarWidth + gapWidth / 2f
            val isPlayed = i.toFloat() / barCount <= progress

            // ── Bar glow near playback position ──
            val playIndex = (progress * barCount).toInt().coerceIn(0, barCount - 1)
            val distFromPlay = abs(i - playIndex)
            if (distFromPlay <= 2 && isPlaying) {
                val glowAlpha = (1f - distFromPlay / 3f) * 0.3f
                drawRoundRect(
                    color = glowColor.copy(alpha = glowAlpha),
                    topLeft = Offset(x - 4f, halfHeight - barHeight - 4f),
                    size = Size(barWidth + 8f, barHeight * 2f + 8f),
                    cornerRadius = CornerRadius(6f)
                )
            }

            // ── Main bar ──
            drawRoundRect(
                brush = if (isPlayed) playedGradient else SolidColor(whiteDim),
                topLeft = Offset(x, halfHeight - barHeight),
                size = Size(barWidth, barHeight * 2f),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }

        // ── Playback position indicator line ──
        val indicatorX = progress * size.width
        drawLine(
            color = indicatorColor,
            start = Offset(indicatorX, 0f),
            end = Offset(indicatorX, size.height),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
        )
    }
}
