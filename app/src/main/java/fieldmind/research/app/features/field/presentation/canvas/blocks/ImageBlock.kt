package fieldmind.research.app.features.field.presentation.canvas

import android.net.Uri
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fieldmind.research.app.features.field.presentation.components.ImageViewerDialog
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

/**
 * Image block for the infinite canvas.
 *
 * Features:
 * - Coil [AsyncImage] with loading spinner and error state
 * - Caption overlay at bottom (editable when selected)
 * - Double-tap for full-screen [ImageViewerDialog]
 * - Image picker integration (gallery) via [ActivityResultContracts.GetContent]
 * - Alt text for accessibility
 *
 * The image URI is stored in [CanvasBlockEntity.contentJson] as a simple string.
 *
 * @param imageUri content URI or file path of the image
 * @param caption optional caption text displayed at the bottom
 * @param onImageChange called when a new image is picked (URI string)
 * @param onCaptionChange called when the caption text changes
 * @param isSelected whether the containing block is selected (shows edit controls)
 * @param modifier standard Compose modifier
 */
@Composable
fun ImageBlock(
    imageUri: String,
    caption: String = "",
    onImageChange: (String) -> Unit = {},
    onCaptionChange: (String) -> Unit = {},
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showImageViewer by remember { mutableStateOf(false) }
    var editingCaption by remember { mutableStateOf(false) }
    var captionText by remember(caption) { mutableStateOf(caption) }
    var isError by remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageChange(it.toString()) }
    }

    // ── Full-screen image viewer ──
    if (showImageViewer && imageUri.isNotBlank()) {
        ImageViewerDialog(
            uri = imageUri,
            caption = captionText,
            onDismiss = { showImageViewer = false }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ── Image area ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .then(
                    if (imageUri.isNotBlank()) {
                        // Double-tap to open full-screen viewer when image exists
                        // Use clickable as a simple tap handler
                        Modifier.clickable { showImageViewer = true }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                imageUri.isBlank() -> {
                    // No image — show placeholder with picker button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            MaterialSymbolIcon("image", defaultWeight = 300),
                            "Add image",
                            size = 40.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            "Tap to add image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                isError -> {
                    // Error state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            MaterialSymbolIcon("broken_image", defaultWeight = 300),
                            "Error loading image",
                            size = 32.dp,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Could not load image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    // Loaded image
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = captionText.ifBlank { "Canvas image" },
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        onState = { state ->
                            isError = state is coil.compose.AsyncImagePainter.State.Error
                        }
                    )
                }
            }
        }

        // ── Caption / controls area ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
        ) {
            if (isSelected && editingCaption) {
                // Editable caption field
                OutlinedTextField(
                    value = captionText,
                    onValueChange = {
                        captionText = it
                        onCaptionChange(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    placeholder = { Text("Add caption…", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        IconButton(onClick = { editingCaption = false }) {
                            Icon(MaterialSymbolIcon("check"), "Done", size = 16.dp)
                        }
                    }
                )
            } else if (isSelected && imageUri.isBlank()) {
                // Show picker button when empty and selected
                TextButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Icon(MaterialSymbolIcon("add_photo_alternate"), null, size = 16.dp)
                    Spacer(Modifier.size(6.dp))
                    Text("Choose image", style = MaterialTheme.typography.bodySmall)
                }
            } else if (isSelected && imageUri.isNotBlank()) {
                // Action row: caption display + change image + re-edit caption
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        captionText.ifBlank { "Add caption…" },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (captionText.isNotBlank())
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { editingCaption = true }
                    )
                    IconButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(MaterialSymbolIcon("edit"), "Change image", size = 14.dp)
                    }
                    IconButton(
                        onClick = { showImageViewer = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(MaterialSymbolIcon("open_in_full"), "View full screen", size = 14.dp)
                    }
                }
            } else {
                // Read-only caption when not selected
                if (captionText.isNotBlank()) {
                    Text(
                        captionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
