package chromahub.rhythm.app.features.local.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import chromahub.rhythm.app.R
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.repository.PlaybackStatsRepository
import chromahub.rhythm.app.shared.data.repository.StatsTimeRange
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeTarget
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.common.TabAnimation
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShapeFor
import chromahub.rhythm.app.util.M3ImageUtils
import chromahub.rhythm.app.util.HapticUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ListeningStatsScreen(
    navController: NavController,
    viewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val songs by viewModel.songs.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val ranges = StatsTimeRange.entries

    var selectedRange by remember { mutableStateOf(StatsTimeRange.WEEK) }
    var statsSummary by remember { mutableStateOf<PlaybackStatsRepository.PlaybackStatsSummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val tabRowState = rememberLazyListState()
    val pagerState = rememberPagerState(
        initialPage = ranges.indexOf(selectedRange).coerceAtLeast(0),
        pageCount = { ranges.size }
    )

    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        showContent = true
    }

    LaunchedEffect(selectedRange, songs) {
        isLoading = true
        statsSummary = viewModel.loadPlaybackStats(selectedRange)
        isLoading = false
    }

    LaunchedEffect(pagerState.currentPage) {
        val currentRange = ranges[pagerState.currentPage]
        if (selectedRange != currentRange) {
            selectedRange = currentRange
        }
    }

    LaunchedEffect(selectedRange) {
        val index = ranges.indexOf(selectedRange).coerceAtLeast(0)
        if (pagerState.currentPage != index) {
            pagerState.animateScrollToPage(index)
        }
        tabRowState.animateScrollToItem(index)
    }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.stats_title),
        showBackButton = true,
        onBackClick = {
            HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.LongPress)
            navController.popBackStack()
        }
        ,
        headerContent = {
            // Range Tabs in header
            RhythmTimeRangeTabs(
                selectedRange = selectedRange,
                onRangeSelected = { range ->
                    HapticUtils.performHapticFeedback(context, hapticFeedback, HapticFeedbackType.TextHandleMove)
                    selectedRange = range
                },
                tabRowState = tabRowState
            )
        }
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { _ ->
                val pageScrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(pageScrollState)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    AnimatedContent(
                        targetState = Pair(isLoading, selectedRange),
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "statsContentTransition"
                    ) { (loading, _) ->
                        if (loading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (statsSummary == null || statsSummary!!.totalPlayCount == 0) {
                            EmptyStatsView()
                        } else {
                            val stats = statsSummary!!

                            Column(
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                ListeningOverviewCard(stats = stats)

                                CategoryMetricsSection(stats = stats, artists = artists)

                                ListeningHabitsCard(stats = stats)

                                RatingStatsCard(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tabs & Hero Section
// ---------------------------------------------------------------------------

@Composable
private fun RhythmTimeRangeTabs(
    selectedRange: StatsTimeRange,
    onRangeSelected: (StatsTimeRange) -> Unit,
    tabRowState: androidx.compose.foundation.lazy.LazyListState
) {
    val context = LocalContext.current
    val tabNames = mapOf(
        StatsTimeRange.TODAY to context.getString(R.string.stats_today),
        StatsTimeRange.WEEK to context.getString(R.string.stats_this_week),
        StatsTimeRange.MONTH to context.getString(R.string.stats_this_month),
        StatsTimeRange.ALL_TIME to context.getString(R.string.stats_all_time)
    )

    LazyRow(
        state = tabRowState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        itemsIndexed(StatsTimeRange.entries) { index, range ->
            val isSelected = range == selectedRange
            val title = tabNames[range] ?: range.displayName

            TabAnimation(
                index = index,
                selectedIndex = StatsTimeRange.entries.indexOf(selectedRange),
                title = title,
                selectedColor = MaterialTheme.colorScheme.primary,
                onSelectedColor = MaterialTheme.colorScheme.onPrimary,
                unselectedColor = MaterialTheme.colorScheme.surfaceContainer,
                onUnselectedColor = MaterialTheme.colorScheme.onSurface,
                onClick = { onRangeSelected(range) },
                modifier = Modifier.padding(all = 2.dp),
                content = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }
    }
}

@Composable
private fun ListeningOverviewCard(stats: PlaybackStatsRepository.PlaybackStatsSummary) {
    val timeline = stats.timeline.takeLast(3)
    val leadingEntry = timeline.maxByOrNull { it.playCount }
    val durationText = formatDurationAsMinutes(stats.totalDurationMs)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = if (stats.range == StatsTimeRange.TODAY && leadingEntry != null) {
                    "30 minutes longer than yesterday"
                } else {
                    "Compared with the previous period"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        if (timeline.isNotEmpty()) {
            val segmentColors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
            )
            val maxPlays = timeline.maxOf { it.playCount }.coerceAtLeast(1)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                timeline.forEachIndexed { index, entry ->
                    val progress = (entry.playCount.toFloat() / maxPlays).coerceIn(0.28f, 1f)
                    Surface(
                        shape = when (index) {
                            0 -> RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 4.dp, bottomEnd = 4.dp)
                            timeline.lastIndex -> RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp)
                            else -> RoundedCornerShape(4.dp)
                        },
                        color = segmentColors[index % segmentColors.size],
                        modifier = Modifier
                            .weight(progress)
                            .fillMaxHeight()
                    ) {}
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Most active at ${timeline.firstOrNull()?.label ?: "9:30 am"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                timeline.drop(1).forEach { entry ->
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Top Songs & Artists
// ---------------------------------------------------------------------------

@Composable
private fun TopSongsList(stats: PlaybackStatsRepository.PlaybackStatsSummary) {
    if (stats.topSongs.isEmpty()) return

    val items = stats.topSongs.take(5).mapIndexed { index, song ->
        val songArtShape = rememberExpressiveShapeFor(
            ExpressiveShapeTarget.SONG_ART,
            fallbackShape = RoundedCornerShape(16.dp)
        )

        Material3SettingsItem(
            leadingContent = {
                Surface(
                    shape = songArtShape,
                    color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                    contentColor = if (index == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.albumArtUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(song.albumArtUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            title = {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            description = {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingContent = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = formatDuration(song.totalDurationMs),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${song.playCount} plays",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Top Songs",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Material3SettingsGroup(
            items = items,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    }
}

@Composable
private fun TopArtistsList(
    stats: PlaybackStatsRepository.PlaybackStatsSummary,
    artists: List<Artist>
) {
    if (stats.topArtists.isEmpty()) return

    val artistByName = remember(artists) { artists.associateBy { it.name } }
    val artistArtShape = rememberExpressiveShapeFor(
        ExpressiveShapeTarget.ARTIST_ART,
        fallbackShape = RoundedCornerShape(16.dp)
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Top Artists",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            items(stats.topArtists.take(5)) { artist ->
                val libraryArtist = artistByName[artist.artist]

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = artistArtShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(84.dp)
                    ) {
                        M3ImageUtils.ArtistImage(
                            imageUrl = libraryArtist?.artworkUri,
                            artistName = artist.artist,
                            modifier = Modifier.fillMaxSize(),
                            applyExpressiveShape = false
                        )
                    }
                    Text(
                        text = artist.artist,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Category Metrics Section
// ---------------------------------------------------------------------------

private enum class CategoryDimension(val displayName: String) {
    SONG("Songs"),
    ARTIST("Artists"),
    ALBUM("Albums"),
    GENRE("Genres")
}

private data class CategoryMetricEntry(
    val label: String,
    val durationMs: Long,
    val plays: Int,
    val supporting: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryMetricsSection(
    stats: PlaybackStatsRepository.PlaybackStatsSummary,
    artists: List<Artist>
) {
    var selectedDimension by remember { mutableStateOf(CategoryDimension.SONG) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            CategoryDimension.entries.forEach { dimension ->
                val isSelected = dimension == selectedDimension
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedDimension = dimension },
                    label = {
                        Text(
                            text = dimension.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.Transparent,
                        selectedBorderColor = Color.Transparent,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        when (selectedDimension) {
            CategoryDimension.SONG -> TopSongsList(stats = stats)
            CategoryDimension.ARTIST -> TopArtistsList(stats = stats, artists = artists)
            else -> {
                val entries = when (selectedDimension) {
                    CategoryDimension.ALBUM -> stats.topAlbums.map {
                        CategoryMetricEntry(it.album, it.totalDurationMs, it.playCount, "${it.uniqueSongs} tracks")
                    }
                    CategoryDimension.GENRE -> stats.topGenres.map {
                        CategoryMetricEntry(it.genre, 0L, (it.percentage * stats.totalPlayCount).toInt(), "Top Genre")
                    }
                    else -> emptyList()
                }.filter { it.plays > 0 || it.durationMs > 0 }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Top ${selectedDimension.displayName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (entries.isEmpty()) {
                        Text(
                            "No data available for this category yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val maxVal = entries.maxOf { if (it.durationMs > 0) it.durationMs.toFloat() else it.plays.toFloat() }.coerceAtLeast(1f)

                        val settingsItems = entries.take(8).mapIndexed { index, entry ->
                            val isTop = index == 0
                            val rawVal = if (entry.durationMs > 0) entry.durationMs.toFloat() else entry.plays.toFloat()
                            val progress = (rawVal / maxVal).coerceIn(0f, 1f)

                            val accentColor = if (isTop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            val accentOnColor = if (isTop) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary

                            Material3SettingsItem(
                                isHighlighted = isTop,
                                leadingContent = {
                                    CategoryRankBadge(
                                        rank = index + 1,
                                        accentColor = accentColor,
                                        accentOnColor = accentOnColor,
                                        highlighted = isTop
                                    )
                                },
                                title = {
                                    Text(
                                        text = entry.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                description = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = entry.supporting,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape),
                                            color = accentColor,
                                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        )
                                    }
                                },
                                trailingContent = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (entry.durationMs > 0) {
                                            Text(
                                                text = formatDuration(entry.durationMs),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "${entry.plays} plays",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                        }

                        Material3SettingsGroup(
                            items = settingsItems,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRankBadge(rank: Int, accentColor: Color, accentOnColor: Color, highlighted: Boolean) {
    val containerColor = if (highlighted) accentColor else accentColor.copy(alpha = 0.2f)
    val contentColor = if (highlighted) accentOnColor else accentColor

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Listening Habits & Timeline
// ---------------------------------------------------------------------------

@Composable
private fun ListeningHabitsCard(stats: PlaybackStatsRepository.PlaybackStatsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Listening Habits",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitMetricRow(Icons.Outlined.EventAvailable, "Active Days", "${stats.activeDays} days")
                HabitMetricRow(Icons.Outlined.LocalFireDepartment, "Longest Streak", "${stats.longestStreakDays} days")
                HabitMetricRow(Icons.Outlined.History, "Total Sessions", "${stats.totalSessions}")
                HabitMetricRow(Icons.Outlined.HourglassEmpty, "Avg Session", formatDuration(stats.averageSessionDurationMs))

                stats.peakDayOfWeek?.let {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    HabitMetricRow(Icons.Outlined.Whatshot, "Peak Day", it, isHighlight = true)
                }
            }
        }
    }
}

@Composable
private fun HabitMetricRow(icon: ImageVector, label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isHighlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BeatTimelineCard(timeline: List<PlaybackStatsRepository.TimelineEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Listening Timeline",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val maxPlays = timeline.maxOfOrNull { it.playCount }?.coerceAtLeast(1) ?: 1

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                timeline.takeLast(7).forEach { entry ->
                    val progress = (entry.playCount.toFloat() / maxPlays).coerceIn(0f, 1f)

                    Column(
                        modifier = Modifier.width(56.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${entry.playCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .weight(1f)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(progress)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Text(
                            text = entry.label.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun RatingStatsCard(viewModel: MusicViewModel) {
    val context = LocalContext.current
    val appSettings = chromahub.rhythm.app.shared.data.model.AppSettings.getInstance(context)
    val ratingDistribution = appSettings.getRatingDistribution()
    val totalRated = ratingDistribution.values.sum()

    if (totalRated == 0) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Song Ratings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            (5 downTo 1).forEach { rating ->
                val count = ratingDistribution[rating] ?: 0
                val percentage = if (totalRated > 0) (count.toFloat() / totalRated) else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "$rating ★",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp)
                    )
                    LinearProgressIndicator(
                        progress = { percentage },
                        modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                        color = if (rating >= 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                }
            }

            if ((ratingDistribution[5] ?: 0) > 0 || (ratingDistribution[4] ?: 0) > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if ((ratingDistribution[5] ?: 0) > 0) {
                        AssistChip(
                            onClick = { viewModel.playRatingPlaylist(5, shuffled = false) },
                            label = { Text("Favorites") },
                            leadingIcon = { Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    if (totalRated > 0) {
                        AssistChip(
                            onClick = { viewModel.playMinimumRatingPlaylist(4, shuffled = false) },
                            label = { Text("Loved (4+)") },
                            leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStatsView() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.BarChart,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Text(
            text = "No stats available yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Keep listening to build your musical journey!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}

private fun formatDurationAsMinutes(ms: Long): String {
    val minutes = (ms / 1000 / 60).coerceAtLeast(1)
    return "$minutes min"
}