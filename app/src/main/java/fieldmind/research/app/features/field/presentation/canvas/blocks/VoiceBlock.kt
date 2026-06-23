package fieldmind.research.app.features.field.presentation.canvas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import fieldmind.research.app.features.field.presentation.components.AudioPlayerDialog
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Voice note block for the canvas — record audio with [MediaRecorder] or
 * import an audio file. Displays duration metadata and opens the full
 * [AudioPlayerDialog] on tap.
 *
 * Content JSON format:
 * ```json
 * { "uri": "file:///...", "duration": 45, "transcript": "", "timestamp": 1234567890 }
 * ```
 */
@Composable
fun VoiceBlock(
    contentJson: String,
    onContentChanged: (String) -> Unit,
    isSelected: Boolean
) {
    val context = LocalContext.current
    var showPlayer by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var audioDuration by remember { mutableIntStateOf(0) }
    var recordingTime by remember { mutableIntStateOf(0) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        )
    }

    // Parse content
    val (uri, duration, transcript, timestamp) = remember(contentJson) {
        if (contentJson.isNotBlank()) {
            try {
                val obj = JSONObject(contentJson)
                Quadruple(
                    obj.optString("uri", ""),
                    obj.optInt("duration", 0),
                    obj.optString("transcript", ""),
                    obj.optLong("timestamp", 0L)
                )
            } catch (_: Exception) {
                Quadruple("", 0, "", 0L)
            }
        } else {
            Quadruple("", 0, "", 0L)
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    // Audio file picker
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        if (selectedUri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }

            // Get duration via MediaPlayer
            val dur = getAudioDuration(context, selectedUri)

            val json = JSONObject().apply {
                put("uri", selectedUri.toString())
                put("duration", dur)
                put("transcript", "")
                put("timestamp", System.currentTimeMillis())
            }.toString()
            onContentChanged(json)
        }
    }

    // Recording time ticker
    LaunchedEffect(isRecording) {
        while (isRecording) {
            kotlinx.coroutines.delay(1000)
            recordingTime++
        }
    }

    fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%d:%02d".format(mins, secs)
    }

    fun formatDate(ts: Long): String {
        if (ts == 0L) return ""
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(ts))
    }

    fun startRecording() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        try {
            val outputDir = context.cacheDir
            val file = File(outputDir, "voice_note_${System.nanoTime()}.m4a")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44100)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                r.setAudioEncodingBitRate(128000)
            } else {
                r.setAudioBitRate(128000)
            }
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            recordingFile = file
            isRecording = true
            recordingTime = 0
        } catch (e: Exception) {
            recorder = null
            isRecording = false
        }
    }

    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) { }
        recorder = null
        isRecording = false

        recordingFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                val dur = recordingTime // approximate from ticker
                val json = JSONObject().apply {
                    put("uri", Uri.fromFile(file).toString())
                    put("duration", dur)
                    put("transcript", "")
                    put("timestamp", System.currentTimeMillis())
                }.toString()
                onContentChanged(json)
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.release()
            } catch (_: Exception) { }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (uri.isBlank() && !isRecording) {
            // ── Empty state: record or import ──
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    MaterialSymbolIcon("mic", defaultWeight = 300),
                    "Voice Note",
                    size = 36.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Voice Note",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { startRecording() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Icon(MaterialSymbolIcon("fiber_manual_record"), null, size = 14.dp)
                        Spacer(Modifier.size(4.dp))
                        Text("Record", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { audioPicker.launch(arrayOf("audio/*")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Icon(MaterialSymbolIcon("audio_file"), null, size = 16.dp)
                        Spacer(Modifier.size(4.dp))
                        Text("Import", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else if (isRecording) {
            // ── Recording state ──
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pulsing red circle
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            MaterialSymbolIcon("fiber_manual_record"),
                            "Recording",
                            size = 24.dp,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    formatDuration(recordingTime),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { stopRecording() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(MaterialSymbolIcon("stop"), null, size = 18.dp)
                    Spacer(Modifier.size(4.dp))
                    Text("Stop Recording")
                }
            }
        } else {
            // ── Recording metadata display ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showPlayer = true },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            MaterialSymbolIcon("play_arrow"),
                            "Play",
                            size = 22.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (duration > 0) {
                    Text(
                        formatDuration(duration),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        "Audio note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (timestamp > 0) {
                    Text(
                        formatDate(timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Tap to play",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            // ── Replace button (top-right, when selected) ──
            if (isSelected) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { startRecording() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(MaterialSymbolIcon("mic"), "Re-record", size = 14.dp)
                    }
                    IconButton(
                        onClick = { audioPicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(MaterialSymbolIcon("edit"), "Replace audio", size = 14.dp)
                    }
                }
            }
        }
    }

    // ── Full-screen audio player dialog ──
    if (showPlayer && uri.isNotBlank()) {
        AudioPlayerDialog(
            uri = uri,
            title = "Voice Note",
            onDismiss = { showPlayer = false }
        )
    }
}

private fun getAudioDuration(context: android.content.Context, uri: Uri): Int {
    return try {
        val mp = MediaPlayer().apply {
            setDataSource(context, uri)
            prepare()
        }
        val dur = mp.duration / 1000 // convert ms to seconds
        mp.release()
        dur
    } catch (_: Exception) { 0 }
}

/**
 * Simple 4-value tuple used for parsing content JSON.
 */
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
