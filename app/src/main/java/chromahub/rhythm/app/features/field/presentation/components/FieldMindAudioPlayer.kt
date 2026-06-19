package fieldmind.research.app.features.field.presentation.components

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

// ── Waveform constants ──
private const val WAVEFORM_BAR_COUNT = 80
private val WaveformBarGap = 0.18f // fraction of bar width used as gap

/**
 * Full-screen dialog for playing audio files in-app.
 * Features real PCM waveform visualization, playback controls, and seek.
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
    var waveformBars by remember { mutableStateOf<FloatArray?>(null) }
    var waveformLoading by remember { mutableStateOf(true) }

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

    // ── Extract real PCM waveform data on a background thread ──
    LaunchedEffect(uri) {
        waveformLoading = true
        val bars = extractWaveformPcm(context, uri, WAVEFORM_BAR_COUNT)
        waveformBars = bars
        waveformLoading = false
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
        val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(ms.toLong())
        val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(ms.toLong()) % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f

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
                    !isPrepared || waveformLoading -> {
                        Spacer(Modifier.height(48.dp))
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (waveformLoading) "Generating waveform…" else "Loading…",
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
                        val bars = waveformBars
                        if (bars != null) {
                            WaveformCanvas(
                                bars = bars,
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
                        } else {
                            // Fallback: simplified generated waveform
                            GeneratedWaveformPlaceholder(
                                progress = progress,
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
                        }

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
//  Real PCM Waveform Extraction via MediaExtractor + MediaCodec
// ══════════════════════════════════════════════════════════════════════

/**
 * Decodes an audio file using [MediaExtractor] + [MediaCodec] and extracts
 * the RMS amplitude envelope, downsampled to [targetBars] values (0f…1f).
 *
 * Runs synchronously — call from a background [Dispatchers.IO] coroutine.
 *
 * Returns `null` if decoding fails (caller falls back to generated waveform).
 */
private suspend fun extractWaveformPcm(
    context: android.content.Context,
    uri: String,
    targetBars: Int
): FloatArray? = withContext(Dispatchers.IO) {
    var extractor: MediaExtractor? = null
    var decoder: MediaCodec? = null
    try {
        extractor = MediaExtractor()
        extractor.setDataSource(context, Uri.parse(uri), null)

        // ── Select the first audio track ──
        var audioTrackIndex = -1
        var trackFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioTrackIndex = i
                trackFormat = fmt
                break
            }
        }
        if (audioTrackIndex < 0 || trackFormat == null) return null
        extractor.selectTrack(audioTrackIndex)

        // ── Configure decoder ──
        val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: return null
        decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(trackFormat, null, null, 0)
        decoder.start()

        val inputBuffers = decoder.inputBuffers
        val outputBuffers = decoder.outputBuffers
        val bufferInfo = MediaCodec.BufferInfo()

        // ── Collect RMS values per decoded frame ──
        val rmsValues = mutableListOf<Float>()
        var inputDone = false
        var outputDone = false
        val timeoutUs = 10_000L
        var frameCount = 0
        val maxFrames = 500_000 // safety cap (~10 min at 44.1kHz/1000 frames/sec)

        while (!outputDone && frameCount < maxFrames) {
            // Feed input
            if (!inputDone) {
                val inputIndex = try {
                    decoder.dequeueInputBuffer(timeoutUs)
                } catch (_: Exception) { -1 }
                if (inputIndex >= 0) {
                    val inputBuf = inputBuffers[inputIndex] ?: continue
                    val sampleSize = extractor.readSampleData(inputBuf, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(
                            inputIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputDone = true
                    } else {
                        val flags = if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0)
                            MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                        decoder.queueInputBuffer(
                            inputIndex, 0, sampleSize,
                            extractor.sampleTime,
                            flags
                        )
                        extractor.advance()
                    }
                }
            }

            // Drain output
            val outputIndex = try {
                decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            } catch (_: Exception) { -1 }

            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // Format changed — continue
                }
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (inputDone) outputDone = true
                }
                outputIndex >= 0 -> {
                    frameCount++
                    val isEos = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    if (isEos) outputDone = true
                    if (bufferInfo.size > 0) {
                        val outputBuf = outputBuffers[outputIndex] ?: continue
                        outputBuf.position(bufferInfo.offset)
                        outputBuf.limit(bufferInfo.offset + bufferInfo.size)

                        // Compute RMS for this frame
                        val rms = computeRms(outputBuf, bufferInfo.size)
                        rmsValues.add(rms)

                        decoder.releaseOutputBuffer(outputIndex, false)
                    } else {
                        decoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
        }

        // ── Downsample collected RMS values to targetBars ──
        if (rmsValues.isEmpty()) return null

        val result = FloatArray(targetBars)
        for (i in 0 until targetBars) {
            val startIdx = (i.toLong() * rmsValues.size / targetBars).toInt()
            val endIdx = (((i + 1).toLong() * rmsValues.size / targetBars).toInt())
                .coerceAtMost(rmsValues.size)
            if (endIdx > startIdx) {
                var sum = 0f
                for (j in startIdx until endIdx) {
                    sum += rmsValues[j]
                }
                result[i] = (sum / (endIdx - startIdx)).coerceIn(0.03f, 1f)
            } else {
                result[i] = 0.1f
            }
        }

        return@withContext result
    } catch (_: Exception) {
        return@withContext null
    } finally {
        runCatching { decoder?.stop() }
        runCatching { decoder?.release() }
        runCatching { extractor?.release() }
    }
}

/**
 * Computes the Root-Mean-Square amplitude of a [ByteBuffer] containing
 * 16-bit signed little-endian PCM audio data.
 *
 * Returns a normalised value in 0f…1f range.
 */
private fun computeRms(buffer: ByteBuffer, size: Int): Float {
    if (size < 2) return 0f
    val sampleCount = size / 2
    val shortBuf = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
    val shorts = ShortArray(sampleCount)
    shortBuf.get(shorts, 0, sampleCount.coerceAtMost(shorts.size))

    var sumSquares = 0.0
    for (s in shorts) {
        sumSquares += (s.toDouble() / 32767.0) * (s.toDouble() / 32767.0)
    }
    val rms = sqrt(sumSquares / sampleCount)
    // Normalize: scale RMS (0.0-1.0) so it fills the visual range nicely
    val normalized = (rms * 2.5).coerceIn(0.0, 1.0)
    return normalized.toFloat().coerceIn(0.03f, 1f)
}

// ══════════════════════════════════════════════════════════════════════
//  Fallback generated waveform (when real extraction fails)
// ══════════════════════════════════════════════════════════════════════

private val generatedBars by lazy {
    FloatArray(WAVEFORM_BAR_COUNT) { i ->
        val t = i.toDouble() / WAVEFORM_BAR_COUNT
        val envelope = kotlin.math.sin(t * kotlin.math.PI).toFloat() * 0.6f + 0.4f
        val detail = (0.2f * abs((i % 7) - 3) / 3f) + 0.08f
        envelope * detail
    }
}

private val barColors = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF7C4DFF),
        Color(0xFF448AFF),
        Color(0xFF03DAC6)
    )
)

// ══════════════════════════════════════════════════════════════════════
//  Waveform Canvas composable
// ══════════════════════════════════════════════════════════════════════

/**
 * Renders the real PCM waveform bars using Compose [Canvas].
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
                brush = if (isPlayed) barColors else SolidColor(whiteDim),
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

/**
 * Simplified fallback waveform shown when PCM extraction fails.
 */
@Composable
private fun GeneratedWaveformPlaceholder(
    progress: Float,
    modifier: Modifier = Modifier
) {
    WaveformCanvas(
        bars = generatedBars,
        progress = progress,
        isPlaying = false,
        modifier = modifier
    )
}
