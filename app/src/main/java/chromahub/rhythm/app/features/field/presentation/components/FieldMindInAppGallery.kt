package fieldmind.research.app.features.field.presentation.components

import android.net.Uri
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import fieldmind.research.app.features.field.presentation.viewmodel.DraftEvidenceAttachment
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-app gallery showing only FieldMind-captured images from the app's
 * private pictures directory. Provides a multi-select grid with options
 * to add selected images to observations or research sessions.
 *
 * @param onDismiss Close the gallery
 * @param onSelectImage Called with selected image URIs when user confirms
 * @param title Gallery title
 */
@Composable
fun FieldMindInAppGallery(
    onDismiss: () -> Unit,
    onSelectImage: (List<DraftEvidenceAttachment>) -> Unit,
    title: String = "FieldMind Gallery"
) {
    val context = LocalContext.current

    // ── Load images from FieldMind directory ──
    val capturedImages = remember {
        loadFieldMindImages(context)
    }

    // ── Selection state ──
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
    var showConfirmButton by remember { mutableStateOf(false) }

    // ── Toggle selection ──
    fun toggleSelection(index: Int) {
        selectedIndices = if (index in selectedIndices) {
            selectedIndices - index
        } else {
            selectedIndices + index
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Header ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .safeDrawingPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Close
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(FieldMindIcons.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${capturedImages.size} images • ${selectedIndices.size} selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Select all / Deselect all
                    if (capturedImages.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                selectedIndices = if (selectedIndices.size == capturedImages.size) {
                                    emptySet()
                                } else {
                                    capturedImages.indices.toSet()
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (selectedIndices.size == capturedImages.size) "Deselect all"
                                else "Select all"
                            )
                        }
                    }
                }
            }

            // ── Empty state ──
            if (capturedImages.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            MaterialSymbolIcon("photo_library"),
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            size = 64.dp
                        )
                        Text(
                            "No captured images yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Photos taken with FieldMind camera will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Go back")
                        }
                    }
                }
            } else {
                // ── Image grid ──
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(capturedImages.size) { index ->
                        val item = capturedImages[index]
                        val isSelected = index in selectedIndices

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { toggleSelection(index) }
                        ) {
                            // Thumbnail
                            AsyncImage(
                                model = item.uri,
                                contentDescription = item.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Selection overlay
                            if (isSelected) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                ) {
                                    // Check indicator
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            FieldMindIcons.Check,
                                            null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            size = 14.dp
                                        )
                                    }

                                    // Timestamp at bottom
                                    Box(
                                        Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            item.timestamp,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                // Timestamp on non-selected items too
                                Box(
                                    Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        item.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Bottom action bar ──
                AnimatedVisibility(
                    visible = selectedIndices.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .safeDrawingPadding(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${selectedIndices.size} image${if (selectedIndices.size != 1) "s" else ""} selected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Tap more images to add to your observation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    val attachments = selectedIndices.map { i ->
                                        val item = capturedImages[i]
                                        DraftEvidenceAttachment(
                                            type = "Photo",
                                            uri = item.uri,
                                            caption = "FieldMind capture ${item.timestamp}",
                                            mimeType = "image/jpeg"
                                        )
                                    }
                                    onSelectImage(attachments)
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(FieldMindIcons.Archive, null, size = 18.dp)
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    "Add ${selectedIndices.size} to observation",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Data class for gallery items ──
private data class GalleryImageItem(
    val uri: String,
    val name: String,
    val timestamp: String,
    val file: File
)

// ── Load FieldMind captured images from app storage ──
private fun loadFieldMindImages(context: android.content.Context): List<GalleryImageItem> {
    val images = mutableListOf<GalleryImageItem>()
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        // 1. Check app's external pictures directory (FieldMind dir)
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (externalDir?.exists() == true) {
            externalDir.listFiles()
                ?.filter { it.name?.startsWith("FieldMind_") == true && it.extension in listOf("jpg", "jpeg", "png") }
                ?.sortedByDescending { it.lastModified() }
                ?.forEach { file ->
                    images.add(
                        GalleryImageItem(
                            uri = Uri.fromFile(file).toString(),
                            name = file.name,
                            timestamp = formatter.format(Date(file.lastModified())),
                            file = file
                        )
                    )
                }
        }

        // 2. Also check app files dir for any captured images
        val filesDir = File(context.filesDir, "fieldmind")
        if (filesDir.exists()) {
            filesDir.listFiles()
                ?.filter { it.extension in listOf("jpg", "jpeg", "png") }
                ?.sortedByDescending { it.lastModified() }
                ?.forEach { file ->
                    // Avoid duplicates
                    if (images.none { it.file.absolutePath == file.absolutePath }) {
                        images.add(
                            GalleryImageItem(
                                uri = Uri.fromFile(file).toString(),
                                name = file.name,
                                timestamp = formatter.format(Date(file.lastModified())),
                                file = file
                            )
                        )
                    }
                }
        }

        images
    }
}
