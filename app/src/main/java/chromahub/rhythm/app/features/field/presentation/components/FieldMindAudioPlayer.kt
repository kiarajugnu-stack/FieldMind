package fieldmind.research.app.features.field.presentation.components

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.util.concurrent.TimeUnit

/**
 * Full-screen dialog for playing audio files in-app.
 * Supports playback, pause, seek, and displays elapsed/total time.
 *
 * Uses Android's [MediaPlayer] for playback.
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
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
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
                            tint = androidx.compose.ui.graphics.Color.White,
                            size = 24.dp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Audio icon ──
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        MaterialSymbolIcon(if (isPlaying) "audiotrack" else "music_note"),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        size = 48.dp
                    )
                }

                // ── Title ──
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                when {
                    error -> {
                        Text(
                            "Could not play this audio file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                        )
                    }
                    !isPrepared -> {
                        CircularProgressIndicator(
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            "Loading…",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)
                        )
                    }
                    else -> {
                        // ── Playback progress bar ──
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Slider(
                                value = progress,
                                onValueChange = { seekTo((it * duration).toInt()) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = androidx.compose.ui.graphics.Color.White,
                                    activeTrackColor = androidx.compose.ui.graphics.Color.White,
                                    inactiveTrackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    formatTime(currentPosition),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                                )
                                Text(
                                    formatTime(duration),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // ── Play/Pause button ──
                        IconButton(
                            onClick = { togglePlayPause() },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        MaterialSymbolIcon(if (isPlaying) "pause" else "play_arrow"),
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = androidx.compose.ui.graphics.Color.White,
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
                                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                                size = 16.dp
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                "Restart",
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
