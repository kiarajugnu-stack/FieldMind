package fieldmind.research.app.features.field.presentation.components

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

/**
 * Opens a full-screen Dialog that renders a PDF document page by page
 * using Android's built-in [PdfRenderer].
 *
 * @param uri         The content:// or file:// URI of the PDF.
 * @param title       Optional display title for the header.
 * @param onDismiss   Called when the user closes the viewer.
 */
@Composable
fun PdfViewerDialog(
    uri: String,
    title: String = "PDF Viewer",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // ── Open the PDF renderer in a coroutine-friendly way ──
    val renderer = remember(uri) {
        try {
            val fileDescriptor = context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")
            fileDescriptor?.let { PdfRenderer(it) }
        } catch (_: Exception) { null }
    }

    var currentPage by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var loadError by remember { mutableStateOf(false) }

    // Clean up rendering resources on dispose
    DisposableEffect(renderer) {
        onDispose {
            renderer?.close()
        }
    }

    // Count pages
    LaunchedEffect(renderer) {
        totalPages = runCatching { renderer?.pageCount ?: 0 }.getOrDefault(0)
        if (totalPages == 0) loadError = true
    }

    // Render current page bitmap
    // Track current bitmap for proper recycling on page change
    var currentBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    val pageBitmap = remember(renderer, currentPage) {
        // Recycle previous bitmap before creating new one
        currentBitmap?.recycle()
        val bitmap = try {
            renderer?.let { r ->
                if (currentPage in 0 until r.pageCount) {
                    val page = r.openPage(currentPage)
                    val width = page.width
                    val height = page.height
                    val bm = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bm
                } else null
            }
        } catch (_: Exception) { null }
        currentBitmap = bitmap
        bitmap
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.92f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Top toolbar ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .statusBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Close button
                        IconButton(onClick = onDismiss) {
                            Icon(
                                MaterialSymbolIcon("close"),
                                contentDescription = "Close",
                                tint = androidx.compose.ui.graphics.Color.White,
                                size = 24.dp
                            )
                        }

                        // Title
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleSmall,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            if (totalPages > 0) {
                                Text(
                                    "Page ${currentPage + 1} of $totalPages",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Zoom controls
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                onClick = { scale = (scale + 0.25f).coerceAtMost(3f) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+", color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                }

                // ── Page content area ──
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        loadError || pageBitmap == null -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    MaterialSymbolIcon("description_off"),
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                                    size = 48.dp
                                )
                                Text(
                                    if (loadError) "Could not open PDF" else "Rendering…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "The file may be corrupted or in an unsupported format.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, _, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(0.5f, 3f)
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = BitmapPainter(pageBitmap!!.asImageBitmap()),
                                    contentDescription = "PDF page ${currentPage + 1}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                                )
                            }
                        }
                    }
                }

                // ── Bottom page navigation bar ──
                if (totalPages > 0 && !loadError) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { currentPage = (currentPage - 1).coerceAtLeast(0) },
                                enabled = currentPage > 0,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = androidx.compose.ui.graphics.Color.White
                                )
                            ) {
                                Icon(MaterialSymbolIcon("chevron_left"), null, size = 18.dp)
                                Spacer(Modifier.size(4.dp))
                                Text("Previous")
                            }

                            // Page number slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "${currentPage + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                                Text(
                                    "/ $totalPages",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                                )
                            }

                            OutlinedButton(
                                onClick = { currentPage = (currentPage + 1).coerceAtMost(totalPages - 1) },
                                enabled = currentPage < totalPages - 1,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = androidx.compose.ui.graphics.Color.White
                                )
                            ) {
                                Text("Next")
                                Spacer(Modifier.size(4.dp))
                                Icon(MaterialSymbolIcon("chevron_right"), null, size = 18.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
