@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package fieldmind.research.app.shared.presentation.screens.settings


import fieldmind.research.app.shared.presentation.components.icons.RhythmIcons
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import fieldmind.research.app.shared.presentation.components.icons.Icon

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import fieldmind.research.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Slider
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fieldmind.research.app.BuildConfig
import fieldmind.research.app.shared.data.model.AppSettings
import fieldmind.research.app.shared.data.model.Playlist
import fieldmind.research.app.shared.data.model.Song
import fieldmind.research.app.shared.data.repository.PlaybackStatsRepository
import fieldmind.research.app.shared.data.repository.StatsTimeRange
import fieldmind.research.app.util.GsonUtils
import fieldmind.research.app.util.HapticUtils
import fieldmind.research.app.util.HapticType
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlin.system.exitProcess
import fieldmind.research.app.shared.presentation.components.common.CollapsibleHeaderScreen
import fieldmind.research.app.shared.presentation.components.common.ButtonGroupStyle
import fieldmind.research.app.shared.presentation.components.common.ExpressiveScrollBar
import fieldmind.research.app.shared.presentation.components.common.ExpressiveButtonGroup
import fieldmind.research.app.shared.presentation.components.common.ExpressiveGroupButton
import fieldmind.research.app.shared.presentation.components.bottomsheets.StandardBottomSheetHeader
import fieldmind.research.app.shared.presentation.components.common.StyledProgressBar
import fieldmind.research.app.shared.presentation.components.common.ProgressStyle
import fieldmind.research.app.shared.presentation.components.common.ThumbStyle
import fieldmind.research.app.shared.presentation.components.bottomsheets.LicensesBottomSheet
import fieldmind.research.app.shared.presentation.components.bottomsheets.UpdateBottomSheet
import fieldmind.research.app.ui.utils.LazyListStateSaver
import fieldmind.research.app.features.local.presentation.viewmodel.MusicViewModel
import fieldmind.research.app.shared.presentation.components.common.ExpressiveShapeProvider
import fieldmind.research.app.shared.presentation.components.common.ExpressiveShapes
import fieldmind.research.app.shared.presentation.components.common.buildSplashBackdropShapes
import fieldmind.research.app.shared.presentation.components.common.SplashBackgroundOrbs
import fieldmind.research.app.shared.presentation.viewmodel.AppUpdaterViewModel
import fieldmind.research.app.shared.presentation.viewmodel.AppVersion
import fieldmind.research.app.ui.theme.getFontPreviewStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import fieldmind.research.app.utils.FontLoader
import fieldmind.research.app.ui.theme.parseCustomColorScheme
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.text.HtmlCompat
import fieldmind.research.app.shared.presentation.components.common.M3FourColorCircularLoader
import fieldmind.research.app.shared.presentation.components.player.PlayingEqIcon
import fieldmind.research.app.shared.presentation.components.dialogs.CreatePlaylistDialog
import fieldmind.research.app.shared.presentation.components.dialogs.BulkPlaylistExportDialog
import fieldmind.research.app.shared.presentation.components.dialogs.PlaylistImportDialog
import fieldmind.research.app.shared.presentation.components.common.rememberExpressiveShape
import fieldmind.research.app.shared.presentation.components.dialogs.PlaylistOperationProgressDialog
import fieldmind.research.app.shared.presentation.components.dialogs.PlaylistOperationResultDialog
import fieldmind.research.app.shared.presentation.components.dialogs.AppRestartDialog
import fieldmind.research.app.shared.presentation.components.player.PlayerChipOrderBottomSheet
import fieldmind.research.app.features.local.presentation.components.settings.HomeSectionOrderBottomSheet
import fieldmind.research.app.features.local.presentation.components.settings.LibraryTabOrderBottomSheet
import fieldmind.research.app.shared.presentation.components.Material3SettingsGroup
import fieldmind.research.app.shared.presentation.components.Material3SettingsItem

import fieldmind.research.app.shared.presentation.screens.settings.TunerSettingRow
import fieldmind.research.app.shared.presentation.screens.settings.TunerAnimatedSwitch
import fieldmind.research.app.shared.presentation.screens.settings.TunerSettingCard
import fieldmind.research.app.shared.presentation.screens.settings.SettingItem
import fieldmind.research.app.shared.presentation.screens.settings.SettingGroup


// Color Source and Font Source Dialogs for Theme Customization
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSourceDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    selectedColorSource: ColorSource,
    onColorSourceSelected: (ColorSource) -> Unit,
    appSettings: AppSettings,
    context: Context,
    haptic: HapticFeedback
) {
    if (showDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // Animation states
        var showContent by remember { mutableStateOf(false) }

        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "contentAlpha"
        )

        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.theme_color_source),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                text = context.getString(R.string.theme_color_source_desc),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorSource.entries.forEach { source ->
                        val isSelected = selectedColorSource == source
                        Card(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                onColorSourceSelected(source)
                                when (source) {
                                    ColorSource.MONET -> {
                                        appSettings.setUseDynamicColors(true)
                                        appSettings.setColorSource("MONET")
                                    }
                                    ColorSource.ALBUM_ART -> {
                                        appSettings.setUseDynamicColors(false)
                                        appSettings.setColorSource("ALBUM_ART")
                                    }
                                    ColorSource.CUSTOM -> {
                                        appSettings.setUseDynamicColors(false)
                                        appSettings.setColorSource("CUSTOM")
                                    }
                                }
                                onDismiss()
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = if (isSelected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = source.icon,
                                            contentDescription = null,
                                            tint = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = source.getDisplayName(context),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = source.getDescription(context),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = RhythmIcons.CheckCircle,
                                        contentDescription = stringResource(R.string.streaming_selected),
                                        
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}