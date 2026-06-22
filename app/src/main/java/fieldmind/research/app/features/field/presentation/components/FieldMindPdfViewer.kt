package fieldmind.research.app.features.field.presentation.components

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Maximum bitmap dimension (px) to prevent OOM on large PDF pages. */
private const val MAX_PAGE_DIMENSION = 4096

/** Scale factor when rendering to cap resolution. */
private const val RENDER_SCALE = 1.5f

/**
 * Opens a full-screen Dialog that renders a PDF document page by page
 * using Android's built-in [PdfRenderer].
 *
 * Handles content:// and file:// URIs. On error, shows a helpful message
 * with an "Open externally" fallback that launches the system PDF viewer.
 *
 * @param uri         The content://, file://, or http(s):// URI of the PDF.
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

    // ── Renderer state ──
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isOpening by remember { mutableStateOf(true) }

    // ── Page state ──
    var currentPage by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // ── Open the renderer off the main thread ──
    LaunchedEffect(uri) {
        val result = withContext(Dispatchers.IO) {
            try {
                openPdfRenderer(context, uri)
            } catch (e: Exception) {
                null to e.message ?: "Unknown error"
            }
        }
        val (r, err) = result
        if (r != null) {
            renderer = r
            totalPages = r.pageCount
            isOpening = false
            if (totalPages == 0) {
                loadError = true
                errorMessage = "PDF has no pages"
            }
        } else {
            renderer = null
            totalPages = 0
            loadError = true
            errorMessage = err
            isOpening = false
        }
    }

    // ── Cleanup renderer on dispose ──
    DisposableEffect(renderer) {
        onDispose {
            renderer?.close()
        }
    }

    // ── Current page bitmap, rendered off main thread ──
    var pageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(false) }
    var renderError by remember { mutableStateOf(false) }

    // Track previous bitmap so we can recycle it after the new one is rendered
    var previousBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(renderer, currentPage) {
        if (renderer == null || totalPages == 0) return@LaunchedEffect

        isRendering = true
        renderError = false

        val bitmap = withContext(Dispatchers.IO) {
            try {
                renderer?.let { r ->
                    if (currentPage in 0 until r.pageCount) {
                        val page = r.openPage(currentPage)
                        // Scale down if page is extremely large to prevent OOM
                        val renderWidth = page.width.coerceAtMost(MAX_PAGE_DIMENSION)
                        val renderHeight = (page.height.toFloat() * (renderWidth.toFloat() / page.width.toFloat())).toInt()
                            .coerceAtMost(MAX_PAGE_DIMENSION)

                        val bm = android.graphics.Bitmap.createBitmap(
                            renderWidth, renderHeight,
                            android.graphics.Bitmap.Config.ARGB_8888
                        )
                        // Render with a white background to prevent transparency/black issues
                        val canvas = android.graphics.Canvas(bm)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        canvas.setMatrix(android.graphics.Matrix().apply {
                            setScale(
                                renderWidth.toFloat() / page.width.toFloat(),
                                renderHeight.toFloat() / page.height.toFloat()
                            )
                        })
                        page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        bm
                    } else null
                }
            } catch (e: Exception) {
                null
            }
        }

        // Recycle previous bitmap now that new one is ready
        previousBitmap?.recycle()
        previousBitmap = bitmap

        pageBitmap = bitmap
        isRendering = false
        if (bitmap == null) renderError = true

        // Reset zoom on page change
        scale = 1f
        panX = 0f
        panY = 0f
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
                        IconButton(onClick = onDismiss) {
                            Icon(
                                MaterialSymbolIcon("close"),
                                contentDescription = "Close",
                                tint = androidx.compose.ui.graphics.Color.White,
                                size = 24.dp
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleSmall,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            if (totalPages > 0 && !loadError) {
                                Text(
                                    "Page ${currentPage + 1} of $totalPages",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { scale = (scale - 0.25f).coerceAtLeast(0.5f); panX = 0f; panY = 0f },
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
                        isOpening -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    "Opening PDF…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                        loadError -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    MaterialSymbolIcon("description_off"),
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                                    size = 56.dp
                                )
                                Text(
                                    "Could not open PDF",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (errorMessage.isNotBlank()) errorMessage
                                    else "The file may be corrupted, inaccessible, or in an unsupported format.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                                // ── Open in system viewer fallback ──
                                Button(
                                    onClick = {
                                        openPdfExternally(context, uri)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(MaterialSymbolIcon("open_in_new"), null, size = 18.dp)
                                    Spacer(Modifier.size(6.dp))
                                    Text("Open in system viewer")
                                }
                            }
                        }
                        isRendering -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    "Rendering page ${currentPage + 1}…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                        renderError -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    MaterialSymbolIcon("page_off"),
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                                    size = 48.dp
                                )
                                Text(
                                    "Failed to render this page",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                                )
                                OutlinedButton(
                                    onClick = { renderError = false; isRendering = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = androidx.compose.ui.graphics.Color.White
                                    )
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                        pageBitmap != null -> {
                            // Wrap in a scrollable column so the page can be scrolled when zoomed
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                                            panX += pan.x
                                            panY += pan.y
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
                                            translationX = panX
                                            translationY = panY
                                        },
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                // ── Bottom page navigation bar ──
                AnimatedVisibility(
                    visible = totalPages > 0 && !loadError,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
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
                                enabled = currentPage > 0 && !isRendering,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = androidx.compose.ui.graphics.Color.White
                                )
                            ) {
                                Icon(MaterialSymbolIcon("chevron_left"), null, size = 18.dp)
                                Spacer(Modifier.size(4.dp))
                                Text("Previous")
                            }

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
                                enabled = currentPage < totalPages - 1 && !isRendering,
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

// ──────────────────────────────────────────────────────────────────────
//  Private helpers
// ──────────────────────────────────────────────────────────────────────

/**
 * Opens a [PdfRenderer] from a content://, file://, or HTTP(S) URI.
 *
 * - **content://** → uses [android.content.ContentResolver.openFileDescriptor]
 * - **file://**    → uses [File] directly
 * - **http(s)://** → downloads to a temp file first (requires INTERNET)
 *
 * Returns the renderer or null if the PDF could not be opened.
 */
private fun openPdfRenderer(context: Context, uriStr: String): PdfRenderer? {
    val uri = Uri.parse(uriStr)

    return when (uri.scheme) {
        "content" -> {
            val fd = context.contentResolver.openFileDescriptor(uri, "r")
            fd?.let { PdfRenderer(it) }
        }
        "file" -> {
            val file = File(uri.path ?: return null)
            if (!file.exists()) return null
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(fd)
        }
        "http", "https" -> {
            // For remote URLs, download to temp file first
            val tempFile = File(context.cacheDir, "pdf_remote_${System.nanoTime()}.pdf")
            try {
                val stream = java.net.URL(uriStr).openStream()
                tempFile.outputStream().use { output ->
                    stream.use { input -> input.copyTo(output) }
                }
                val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                PdfRenderer(fd)
            } catch (e: Exception) {
                tempFile.delete()
                null
            }
        }
        else -> {
            // Try as file path directly
            try {
                val file = File(uriStr)
                if (file.exists()) {
                    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    PdfRenderer(fd)
                } else {
                    // Last attempt: try content resolver
                    val fd = context.contentResolver.openFileDescriptor(uri, "r")
                    fd?.let { PdfRenderer(it) }
                }
            } catch (_: Exception) { null }
        }
    }
}

/**
 * Opens the PDF in the system's default PDF viewer via an [Intent].
 */
private fun openPdfExternally(context: Context, uriStr: String) {
    try {
        val uri = Uri.parse(uriStr)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Verify there's an app to handle this
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback: just open the URI without specifying MIME type
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    } catch (_: Exception) {
        // Silently fail — user can still see the error in the PDF viewer
    }
}
