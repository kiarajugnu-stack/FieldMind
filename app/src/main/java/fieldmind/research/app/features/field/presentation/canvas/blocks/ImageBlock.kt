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
 * Clean image block for the canvas.
 *
 * Design:
 * - No caption bar — just the image with tap-to-view
 * - Placeholder when empty with picker trigger
 * - Compact, minimal
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
    var isError by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageChange(it.toString()) }
    }

    // ── Full-screen image viewer ──
    if (showImageViewer && imageUri.isNotBlank()) {
        ImageViewerDialog(
            uri = imageUri,
            caption = caption,
            onDismiss = { showImageViewer = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .then(
                if (imageUri.isNotBlank()) {
                    Modifier.clickable { showImageViewer = true }
                } else {
                    Modifier.clickable { imagePicker.launch("image/*") }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            imageUri.isBlank() -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        MaterialSymbolIcon("image", defaultWeight = 300),
                        "Add image",
                        size = 32.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                    if (isSelected) {
                        Text(
                            "Tap to add image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }
            isError -> {
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
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Canvas image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    onState = { state ->
                        isError = state is coil.compose.AsyncImagePainter.State.Error
                    }
                )
            }
        }
    }
}
