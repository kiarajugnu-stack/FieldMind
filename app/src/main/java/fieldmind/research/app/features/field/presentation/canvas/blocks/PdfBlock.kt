package fieldmind.research.app.features.field.presentation.canvas

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import fieldmind.research.app.features.field.presentation.components.PdfViewerDialog
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import org.json.JSONObject
import java.io.File

/**
 * PDF block for the canvas — shows a file picker to select a PDF,
 * displays metadata (name, pages), and opens a native [PdfRenderer] viewer on tap.
 *
 * Content JSON format:
 * ```json
 * { "uri": "content://...", "name": "document.pdf", "pageCount": 5 }
 * ```
 */
@Composable
fun PdfBlock(
    contentJson: String,
    onContentChanged: (String) -> Unit,
    isSelected: Boolean
) {
    val context = LocalContext.current
    var showViewer by remember { mutableStateOf(false) }

    // Parse content
    val (uri, name, pageCount) = remember(contentJson) {
        if (contentJson.isNotBlank()) {
            try {
                val obj = JSONObject(contentJson)
                Triple(
                    obj.optString("uri", ""),
                    obj.optString("name", ""),
                    obj.optInt("pageCount", 0)
                )
            } catch (_: Exception) {
                Triple("", "", 0)
            }
        } else {
            Triple("", "", 0)
        }
    }

    // PDF file picker
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        if (selectedUri != null) {
            // Take persistable permission for future access
            try {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }

            // Get display name and page count
            val displayName = getPdfDisplayName(context, selectedUri)
            val pages = getPdfPageCount(context, selectedUri)

            val json = JSONObject().apply {
                put("uri", selectedUri.toString())
                put("name", displayName)
                put("pageCount", pages)
            }.toString()
            onContentChanged(json)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (uri.isBlank()) {
            // ── Empty state: file picker button ──
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    MaterialSymbolIcon("picture_as_pdf", defaultWeight = 300),
                    "PDF",
                    size = 36.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "PDF Document",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Icon(MaterialSymbolIcon("add"), null, size = 16.dp)
                    Spacer(Modifier.size(4.dp))
                    Text("Select PDF", style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            // ── PDF metadata display ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showViewer = true },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    MaterialSymbolIcon("picture_as_pdf", defaultWeight = 400),
                    "PDF",
                    size = 32.dp,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    name.ifBlank { "PDF Document" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (pageCount > 0) {
                    Text(
                        "$pageCount page${if (pageCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap to view",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            // ── Replace button (top-right, when selected) ──
            if (isSelected) {
                IconButton(
                    onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                ) {
                    Icon(
                        MaterialSymbolIcon("edit"),
                        "Replace PDF",
                        size = 16.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // ── Full-screen PDF viewer dialog ──
    if (showViewer && uri.isNotBlank()) {
        PdfViewerDialog(
            uri = uri,
            title = name.ifBlank { "PDF Document" },
            onDismiss = { showViewer = false }
        )
    }
}

private fun getPdfDisplayName(context: Context, uri: Uri): String {
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val nameIdx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0) return c.getString(nameIdx) ?: ""
            }
        }
    } catch (_: Exception) { }
    // Fallback: extract from URI path
    return uri.lastPathSegment ?: "PDF Document"
}

private fun getPdfPageCount(context: Context, uri: Uri): Int {
    return try {
        val fd = context.contentResolver.openFileDescriptor(uri, "r")
        fd?.use { f ->
            val renderer = PdfRenderer(f)
            val count = renderer.pageCount
            renderer.close()
            count
        } ?: 0
    } catch (_: Exception) { 0 }
}
