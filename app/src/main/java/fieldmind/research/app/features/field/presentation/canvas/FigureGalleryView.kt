package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity
import fieldmind.research.app.features.field.presentation.components.expressivePress
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import org.json.JSONObject

/**
 * Data model for a figure displayed in the gallery.
 */
data class FigureGalleryItem(
    val block: CanvasBlockEntity,
    val thumbnailUri: String = "",
    val caption: String = ""
)

/**
 * Extracts figure gallery items from a list of canvas blocks.
 * Filters to image/figure/pdf types and extracts metadata from contentJson.
 */
fun extractGalleryItems(blocks: List<CanvasBlockEntity>): List<FigureGalleryItem> {
    return blocks
        .filter { it.type in listOf("image", "figure", "pdf") }
        .map { block ->
            val blockJson = if (block.contentJson.isNotBlank()) {
                try {
                    JSONObject(block.contentJson)
                } catch (_: Exception) { null }
            } else null
            val uri = blockJson?.optString("uri", "") ?: ""
            val caption = blockJson?.optString("caption", "") ?: ""
            FigureGalleryItem(
                block = block,
                thumbnailUri = uri,
                caption = caption
            )
        }
}

/**
 * Full-screen figure gallery overlay showing all figure/image/pdf blocks
 * in a scrollable grid. Tap a figure to center the canvas on it.
 *
 * @param items list of figure gallery items to display
 * @param onFigureSelected called with the block ID when a figure is tapped
 * @param onDismiss called to close the gallery
 */
@Composable
fun FigureGalleryView(
    items: List<FigureGalleryItem>,
    onFigureSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Top bar ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Back button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            MaterialSymbolIcon("arrow_back"),
                            "Back to canvas",
                            size = 22.dp
                        )
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            "Figure Gallery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${items.size} figure(s) on this canvas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Count badge
                    if (items.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "${items.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // ── Grid content ──
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            MaterialSymbolIcon("collections_bookmark"),
                            null,
                            size = 64.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Text(
                            "No figures on this canvas",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            "Add image or figure blocks to see them here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 240.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.block.id }) { item ->
                        FigureGalleryCard(
                            item = item,
                            onClick = { onFigureSelected(item.block.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single card in the figure gallery grid.
 * Shows a thumbnail placeholder, type badge, dimensions, and caption.
 */
@Composable
private fun FigureGalleryCard(
    item: FigureGalleryItem,
    onClick: () -> Unit
) {
    val hasUri = item.thumbnailUri.isNotBlank()

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 2.dp,
        modifier = Modifier.expressivePress(scaleDown = 0.97f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ── Thumbnail area ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.33f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(
                        if (hasUri) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (hasUri) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            MaterialSymbolIcon("image"),
                            null,
                            size = 36.dp,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Text(
                            "Has image",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            when (item.block.type) {
                                "pdf" -> MaterialSymbolIcon("picture_as_pdf")
                                else -> MaterialSymbolIcon("broken_image")
                            },
                            null,
                            size = 36.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            "No preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }

                // Type badge (top-left)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        item.block.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // ── Info section ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (item.caption.isNotBlank()) {
                    Text(
                        item.caption,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        "${item.block.type.replaceFirstChar { it.uppercase() }} block",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Dimensions
                Text(
                    "${item.block.width.toInt()} × ${item.block.height.toInt()} px",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                // Linked entity indicator
                if (item.block.linkedEntityType.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            MaterialSymbolIcon("link"),
                            null,
                            size = 12.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            item.block.linkedEntityType.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
