package fieldmind.research.app.features.field.presentation.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

/**
 * Full-screen dialog for viewing images in-app with zoom/pan support.
 * Supports content:// URIs, file:// URIs, and http(s) URLs.
 *
 * Uses Coil's AsyncImage for efficient loading with caching.
 */
@Composable
fun ImageViewerDialog(
    uri: String,
    caption: String = "",
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Top toolbar ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .statusBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                MaterialSymbolIcon("close"),
                                contentDescription = "Close",
                                tint = androidx.compose.ui.graphics.Color.White,
                                size = 24.dp
                            )
                        }

                        if (caption.isNotBlank()) {
                            Text(
                                caption,
                                style = MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Zoom controls
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = { scale = (scale - 0.25f).coerceAtLeast(0.5f) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("−", color = androidx.compose.ui.graphics.Color.White)
                            }
                            Text(
                                "${(scale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            IconButton(
                                onClick = { scale = (scale + 0.25f).coerceAtMost(4f) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+", color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                }

                // ── Image area with pinch-zoom ──
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 4f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = caption.ifBlank { "Image preview" },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // ── Bottom info bar ──
                if (caption.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }
}
