// Experimental API opt-ins required for:
// - Material3 Carousel APIs (HorizontalCenteredHeroCarousel, HorizontalUncontainedCarousel)
// - ModalBottomSheet, rememberModalBottomSheetState
// - Window Size Class APIs
// These will become stable in future Material3 releases
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)

package chromahub.rhythm.app.features.local.presentation.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.ui.theme.festive.FestiveConfig
import chromahub.rhythm.app.ui.theme.festive.FestiveThemeEngine
import chromahub.rhythm.app.ui.theme.festive.FestiveThemeType
import chromahub.rhythm.app.shared.data.model.AppSettings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import chromahub.rhythm.app.util.performIfEnabled
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.features.local.presentation.components.player.MiniPlayer
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.features.local.presentation.components.settings.HomeSectionOrderBottomSheet
import chromahub.rhythm.app.shared.presentation.components.common.M3PlaceholderType
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveFilledButton
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveFilledTonalButton
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveOutlinedButton
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveFilledIconButton
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveFilledTonalIconButton
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveLargeIconButton
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveCard
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveElevatedCard
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapes
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeTarget
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShapeFor
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveButtonGroup
import chromahub.rhythm.app.shared.presentation.components.common.ButtonGroupStyle
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveGroupButton
import chromahub.rhythm.app.shared.presentation.components.common.ActionProgressLoader
import chromahub.rhythm.app.shared.presentation.components.common.NetworkOperationLoader
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveAnimatedCounter
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AlbumBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AddToPlaylistBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.SongInfoBottomSheet
import chromahub.rhythm.app.util.ImageUtils
import chromahub.rhythm.app.util.M3ImageUtils
import chromahub.rhythm.app.shared.presentation.viewmodel.AppVersion
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.features.local.presentation.components.dialogs.CreatePlaylistDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import java.util.Calendar
import kotlin.random.Random
import androidx.core.text.HtmlCompat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    musicViewModel: chromahub.rhythm.app.viewmodel.MusicViewModel,
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    recentlyPlayed: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Float,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlayPause: () -> Unit,
    onPlayerClick: () -> Unit,
    onViewAllSongs: () -> Unit,
    onViewAllAlbums: () -> Unit,
    onViewAllArtists: () -> Unit,
    onSkipNext: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onAddSongToPlaylist: (Song, String) -> Unit = { _, _ -> },
    onNavigateToPlaylist: (String) -> Unit = {},
    onCreatePlaylist: (String) -> Unit = { _ -> },
    onNavigateToStats: () -> Unit = {},
    onNavigateToArtist: (Artist) -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }

    // Home header customization
    val headerDisplayMode by appSettings.homeHeaderDisplayMode.collectAsState()
    val showAppIcon by appSettings.homeShowAppIcon.collectAsState()
    val iconVisibilityMode by appSettings.homeAppIconVisibility.collectAsState()

    // State for album bottom sheet
    var showAlbumBottomSheet by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    val albumSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // State for AddToPlaylist bottom sheet
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }
    val addToPlaylistSheetState = rememberModalBottomSheetState()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    // Song info bottom sheet state
    var showSongInfoSheet by remember { mutableStateOf(false) }

    // Home section order bottom sheet state
    var showHomeSectionOrderSheet by remember { mutableStateOf(false) }

    // Pending write request for metadata editing (Android 11+)
    val pendingWriteRequest by musicViewModel.pendingWriteRequest.collectAsState()

    // Write permission launcher for Android 11+ metadata editing
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            musicViewModel.completeMetadataWriteAfterPermission(
                onSuccess = {
                    Toast.makeText(context, "Metadata saved successfully!", Toast.LENGTH_SHORT).show()
                },
                onError = { errorMessage ->
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            )
        } else {
            musicViewModel.cancelPendingMetadataWrite()
            Toast.makeText(context, "Permission denied. Changes saved to library only.", Toast.LENGTH_LONG).show()
        }
    }

    // Select featured content from all albums (enhanced selection)
    val featuredContent = remember(albums) {
        albums.shuffled()
    }

    // Get all unique artists
    val availableArtists = remember(artists) {
        artists.sortedBy { it.name }
    }

    val quickPicks = songs.take(8)
    val topArtists = availableArtists

    // Enhanced filtering for new releases
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentYearReleases = remember(albums, currentYear) {
        albums.filter { it.year == currentYear }
            .ifEmpty {
                albums.sortedByDescending { it.year }.take(6)
            }
    }

    // Enhanced recently added songs
    val recentlyAddedSongs = remember(songs) {
        val oneMonthAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
        songs.filter { it.dateAdded >= oneMonthAgo }
            .sortedByDescending { it.dateAdded }
    }

    // Enhanced recently added albums
    val recentlyAddedAlbums = remember(albums, songs) {
        val oneMonthAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
        val recentSongIds = songs.filter { it.dateAdded >= oneMonthAgo }.map { it.id }.toSet()
        albums.filter { album ->
            album.songs.any { song -> song.id in recentSongIds }
        }.sortedByDescending { album ->
            album.songs.mapNotNull { song ->
                if (song.id in recentSongIds) song.dateAdded else null
            }.maxOfOrNull { it } ?: 0L
        }
    }

    // Album bottom sheet
    if (showAlbumBottomSheet && selectedAlbum != null) {
        AlbumBottomSheet(
            album = selectedAlbum!!,
            onDismiss = { showAlbumBottomSheet = false },
            onSongClick = onSongClick,
            onPlayAll = { songsToPlay ->
                if (songsToPlay.isNotEmpty()) {
                    musicViewModel.playQueueWithUserRule(songsToPlay, sourceLabel = "Home Album")
                }
                coroutineScope.launch {
                    albumSheetState.hide()
                }.invokeOnCompletion {
                    if (!albumSheetState.isVisible) {
                        showAlbumBottomSheet = false
                    }
                }
            },
            onShufflePlay = { songsToPlay ->
                if (songsToPlay.isNotEmpty()) {
                    musicViewModel.playShuffled(songsToPlay)
                }
                coroutineScope.launch {
                    albumSheetState.hide()
                }.invokeOnCompletion {
                    if (!albumSheetState.isVisible) {
                        showAlbumBottomSheet = false
                    }
                }
            },
            onAddToQueue = onAddToQueue,
            onAddSongToPlaylist = { song ->
                selectedSongForPlaylist = song
                coroutineScope.launch {
                    albumSheetState.hide()
                }.invokeOnCompletion {
                    if (!albumSheetState.isVisible) {
                        showAlbumBottomSheet = false
                        showAddToPlaylistSheet = true
                    }
                }
            },
            onPlayerClick = onPlayerClick,
            haptics = LocalHapticFeedback.current,
            sheetState = albumSheetState,
            onPlayNext = { song -> musicViewModel.playNext(song) },
            onToggleFavorite = { song -> musicViewModel.toggleFavorite(song) },
            favoriteSongs = musicViewModel.favoriteSongs.collectAsState().value,
            onShowSongInfo = { song ->
                selectedSongForPlaylist = song
                coroutineScope.launch {
                    albumSheetState.hide()
                }.invokeOnCompletion {
                    if (!albumSheetState.isVisible) {
                        showAlbumBottomSheet = false
                        showSongInfoSheet = true
                    }
                }
            },
            onAddToBlacklist = { song ->
                val appSettings = AppSettings.getInstance(context)
                appSettings.addToBlacklist(song.id)
                Toast.makeText(context, "${song.title} added to blacklist", Toast.LENGTH_SHORT).show()
            },
            currentSong = currentSong,
            isPlaying = isPlaying
        )
    }

    // Song info bottom sheet
    if (showSongInfoSheet && selectedSongForPlaylist != null) {
        SongInfoBottomSheet(
            song = selectedSongForPlaylist,
            onDismiss = { showSongInfoSheet = false },
            appSettings = AppSettings.getInstance(context),
            onEditSong = { title, artist, album, genre, year, trackNumber, artworkUri, removeArtwork ->
                musicViewModel.saveMetadataChanges(
                    song = selectedSongForPlaylist!!,
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre,
                    year = year,
                    trackNumber = trackNumber,
                    artworkUri = artworkUri,
                    removeArtwork = removeArtwork,
                    onSuccess = { fileWriteSucceeded ->
                        if (fileWriteSucceeded) {
                            Toast.makeText(context, "Metadata saved successfully to file!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onError = { errorMessage ->
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    },
                    onPermissionRequired = { pendingRequest ->
                        try {
                            val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(
                                pendingRequest.intentSender
                            ).build()
                            writePermissionLauncher.launch(intentSenderRequest)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Failed to request permission: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            musicViewModel.cancelPendingMetadataWrite()
                        }
                    }
                )
            }
        )
    }

    if (showAddToPlaylistSheet && selectedSongForPlaylist != null) {
        val playlists by musicViewModel.playlists.collectAsState()

        AddToPlaylistBottomSheet(
            song = selectedSongForPlaylist!!,
            playlists = playlists,
            onDismissRequest = { showAddToPlaylistSheet = false },
            onAddToPlaylist = { playlist ->
                onAddSongToPlaylist(selectedSongForPlaylist!!, playlist.id)
                coroutineScope.launch {
                    addToPlaylistSheetState.hide()
                }.invokeOnCompletion {
                    if (!addToPlaylistSheetState.isVisible) {
                        showAddToPlaylistSheet = false
                    }
                }
            },
            onCreateNewPlaylist = {
                coroutineScope.launch {
                    addToPlaylistSheetState.hide()
                }.invokeOnCompletion {
                    if (!addToPlaylistSheetState.isVisible) {
                        showAddToPlaylistSheet = false
                        showCreatePlaylistDialog = true
                    }
                }
            },
            sheetState = addToPlaylistSheetState
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                musicViewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }

    if (showHomeSectionOrderSheet) {
        HomeSectionOrderBottomSheet(
            onDismiss = { showHomeSectionOrderSheet = false },
            appSettings = AppSettings.getInstance(context)
        )
    }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.home_title),
        headerDisplayMode = headerDisplayMode,
        showAppIcon = showAppIcon,
        iconVisibilityMode = iconVisibilityMode,
        actions = {
            ExpressiveFilledTonalIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    showHomeSectionOrderSheet = true
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Reorder,
                    contentDescription = context.getString(R.string.cd_reorder_home_sections)
                )
            }
            ExpressiveFilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    onSettingsClick()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = RhythmIcons.Settings,
                    contentDescription = context.getString(R.string.home_settings_cd)
                )
            }
        }
    ) { modifier ->
        ModernScrollableContent(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = if (currentSong != null) 0.dp else 0.dp),
            featuredContent = featuredContent,
            albums = albums,
            topArtists = topArtists,
            newReleases = currentYearReleases,
            recentlyAddedSongs = recentlyAddedSongs,
            recentlyAddedAlbums = recentlyAddedAlbums,
            recentlyPlayed = recentlyPlayed,
            songs = songs,
            onSongClick = onSongClick,
            onAlbumClick = { album: Album ->
                selectedAlbum = album
                showAlbumBottomSheet = true
            },
            onArtistClick = { artist: Artist ->
                onNavigateToArtist(artist)
            },
            onViewAllSongs = onViewAllSongs,
            onViewAllAlbums = onViewAllAlbums,
            onViewAllArtists = onViewAllArtists,
            onSearchClick = onSearchClick,
            onSettingsClick = onSettingsClick,
            onNavigateToLibrary = onNavigateToLibrary,
            onNavigateToPlaylist = onNavigateToPlaylist,
            onNavigateToStats = onNavigateToStats,
            musicViewModel = musicViewModel,
            coroutineScope = coroutineScope
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernScrollableContent(
    modifier: Modifier = Modifier,
    featuredContent: List<Album>,
    albums: List<Album>,
    topArtists: List<Artist>,
    newReleases: List<Album>,
    recentlyAddedSongs: List<Song>,
    recentlyAddedAlbums: List<Album>,
    recentlyPlayed: List<Song>,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onViewAllSongs: () -> Unit,
    onViewAllAlbums: () -> Unit,
    onViewAllArtists: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToPlaylist: (String) -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    musicViewModel: chromahub.rhythm.app.viewmodel.MusicViewModel,
    coroutineScope: CoroutineScope
) {
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as android.app.Activity)
    val widthSizeClass = windowSizeClass.widthSizeClass
    val heightSizeClass = windowSizeClass.heightSizeClass
    val isTablet = widthSizeClass == WindowWidthSizeClass.Medium ||
            widthSizeClass == WindowWidthSizeClass.Expanded
    val scrollState = rememberScrollState()
    val allSongs by musicViewModel.filteredSongs.collectAsState()

    // Home Screen Customization Settings
    val appSettings = AppSettings.getInstance(context)
    val sectionOrder by appSettings.homeSectionOrder.collectAsState()
    val showRecentlyPlayed by appSettings.homeShowRecentlyPlayed.collectAsState()
    val showDiscoverCarousel by appSettings.homeShowDiscoverCarousel.collectAsState()
    val showArtists by appSettings.homeShowArtists.collectAsState()
    val showNewReleases by appSettings.homeShowNewReleases.collectAsState()
    val showRecentlyAdded by appSettings.homeShowRecentlyAdded.collectAsState()
    val showRecommended by appSettings.homeShowRecommended.collectAsState()
    val showListeningStats by appSettings.homeShowListeningStats.collectAsState()
    val discoverItemCount by appSettings.homeDiscoverItemCount.collectAsState()
    val recentlyPlayedCount by appSettings.homeRecentlyPlayedCount.collectAsState()
    val artistsCount by appSettings.homeArtistsCount.collectAsState()
    val newReleasesCount by appSettings.homeNewReleasesCount.collectAsState()
    val recentlyAddedCount by appSettings.homeRecentlyAddedCount.collectAsState()
    val recommendedCount by appSettings.homeRecommendedCount.collectAsState()
    val discoverCarouselStyle by appSettings.homeDiscoverCarouselStyle.collectAsState()

    // Discover widget card content visibility settings
    val discoverShowAlbumName by appSettings.homeDiscoverShowAlbumName.collectAsState()
    val discoverShowArtistName by appSettings.homeDiscoverShowArtistName.collectAsState()
    val discoverShowYear by appSettings.homeDiscoverShowYear.collectAsState()
    val discoverShowPlayButton by appSettings.homeDiscoverShowPlayButton.collectAsState()
    val discoverShowGradient by appSettings.homeDiscoverShowGradient.collectAsState()

    // Enhanced artist computation
    val availableArtists = remember(allSongs, topArtists) {
        val collaborationSeparators = listOf(
            ", ", ",", " & ", " and ", "&", " feat. ", " featuring ", " ft. ",
            " with ", " x ", " X ", " + ", " vs ", " VS ", " / ", ";", " · "
        )

        val collaborationRegex = collaborationSeparators
            .map { Regex.escape(it) }
            .joinToString("|")
            .toRegex(RegexOption.IGNORE_CASE)

        val filteredTopArtists = topArtists.filter { artist ->
            !artist.name.contains(collaborationRegex)
        }

        val extractedArtistNames = allSongs.asSequence()
            .flatMap { song ->
                var artistString = song.artist
                collaborationSeparators.forEach { separator ->
                    artistString = artistString.replace(separator, "||")
                }

                artistString.split("||")
                    .map { it.trim() }
                    .map { name ->
                        if (name.contains("(") && (name.contains("feat") || name.contains("ft") || name.contains("featuring"))) {
                            name.substringBefore("(").trim()
                        } else {
                            name
                        }
                    }
            }
            .filter { it.length > 1 }
            .distinct()

        extractedArtistNames
            .mapNotNull { artistName ->
                filteredTopArtists.find { it.name.equals(artistName, ignoreCase = true) }
                    ?: filteredTopArtists.find {
                        it.name.equals(artistName, ignoreCase = true) ||
                                (artistName.length > 3 && it.name.contains(artistName, ignoreCase = true))
                    }
            }
            .distinct()
            .sortedBy { it.name }
            .toList()
    }

    // Featured albums with auto-refresh - ensure proper count handling
    var currentFeaturedAlbums by remember(featuredContent, discoverItemCount) {
        mutableStateOf(
            if (featuredContent.isEmpty()) listOf()
            else featuredContent.take(discoverItemCount)
        )
    }

    // Auto-refresh featured content periodically - respect discoverItemCount setting
    LaunchedEffect(albums, discoverItemCount) {
        while (true) {
            delay(45000) // 45 seconds for better user experience
            if (albums.size > discoverItemCount) {
                // Shuffle and take only the configured count
                currentFeaturedAlbums = albums.shuffled().take(discoverItemCount)
            } else if (albums.isNotEmpty()) {
                // If we have fewer albums than the count, use all available
                currentFeaturedAlbums = albums.shuffled()
            }
        }
    }

    // Get festive theme settings
    val festiveEnabled by appSettings.festiveThemeEnabled.collectAsState()
    val festiveTypeString by appSettings.festiveThemeType.collectAsState()
    val festiveAutoDetect by appSettings.festiveThemeAutoDetect.collectAsState()

    // Determine active festive theme
    val activeFestiveTheme = remember(festiveEnabled, festiveTypeString, festiveAutoDetect) {
        if (festiveEnabled) {
            val festiveConfig = FestiveConfig(
                enabled = festiveEnabled,
                type = try {
                    FestiveThemeType.valueOf(festiveTypeString)
                } catch (e: IllegalArgumentException) {
                    FestiveThemeType.NONE
                },
                autoDetect = festiveAutoDetect
            )
            FestiveThemeEngine.getActiveFestiveTheme(festiveConfig)
        } else {
            FestiveThemeType.NONE
        }
    }

    val lazyListState = rememberLazyListState()

    // Determine padding to apply individually to non-full-bleed sections
    val horizontalPadding = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 20.dp
        WindowWidthSizeClass.Medium -> 48.dp
        WindowWidthSizeClass.Expanded -> 64.dp
        else -> 20.dp
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(when (widthSizeClass) {
                WindowWidthSizeClass.Compact -> when (heightSizeClass) {
                    WindowHeightSizeClass.Compact -> 32.dp // Landscape phone - tighter spacing
                    else -> 40.dp // Portrait phone
                }
                WindowWidthSizeClass.Medium -> when (heightSizeClass) {
                    WindowHeightSizeClass.Compact -> 48.dp // Landscape tablet - more spacing
                    else -> 56.dp // Portrait tablet - more spacing
                }
                WindowWidthSizeClass.Expanded -> when (heightSizeClass) {
                    WindowHeightSizeClass.Compact -> 52.dp // Landscape large tablet
                    else -> 64.dp // Portrait large tablet - generous spacing
                }
                else -> 40.dp
            }),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Render sections dynamically based on sectionOrder
            sectionOrder.forEach { sectionId ->
                when (sectionId) {
                    "GREETING" -> {
                        // Greeting widget is intentionally removed from home.
                    }
                    "RECENTLY_PLAYED" -> {
                        if (showRecentlyPlayed) {
                            item(key = "section_recently_played") {
                                Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                                    ModernRecentlyPlayedSection(
                                        recentlyPlayed = recentlyPlayed.take(recentlyPlayedCount),
                                        onSongClick = onSongClick,
                                        musicViewModel = musicViewModel,
                                        coroutineScope = coroutineScope,
                                        widthSizeClass = widthSizeClass,
                                        heightSizeClass = heightSizeClass
                                    )
                                }
                            }
                        }
                    }
                    "DISCOVER" -> {
                        if (showDiscoverCarousel) {
                            item(key = "section_discover") {
                                Column {
                                    if (currentFeaturedAlbums.isNotEmpty()) {
                                        // NO horizontal padding applied here to allow full-bleed hero layout!
                                        ModernFeaturedSection(
                                            albums = currentFeaturedAlbums,
                                            onAlbumClick = onAlbumClick,
                                            showAlbumName = discoverShowAlbumName,
                                            showArtistName = discoverShowArtistName,
                                            showYear = discoverShowYear,
                                            showPlayButton = discoverShowPlayButton,
                                            showGradient = discoverShowGradient,
                                            carouselStyle = discoverCarouselStyle,
                                            widthSizeClass = widthSizeClass,
                                            heightSizeClass = heightSizeClass
                                        )
                                    } else {
                                        Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                                            ModernEmptyState(
                                                icon = Icons.Rounded.Album,
                                                title = context.getString(R.string.home_no_featured_albums),
                                                subtitle = context.getString(R.string.home_no_featured_albums_desc),
                                                iconSize = 48.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "ARTISTS" -> {
                        if (showArtists) {
                            item(key = "section_artists") {
                                Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                                    if (availableArtists.isNotEmpty()) {
                                        ModernArtistsSection(
                                            artists = availableArtists.take(artistsCount),
                                            songs = allSongs,
                                            onArtistClick = onArtistClick,
                                            onViewAllArtists = onViewAllArtists,
                                            widthSizeClass = widthSizeClass,
                                            heightSizeClass = heightSizeClass
                                        )
                                    } else {
                                        Column {
                                            ModernSectionTitle(
                                                title = context.getString(R.string.home_artists),
                                                subtitle = context.getString(R.string.home_explore_musicians),
                                                viewAllAction = onViewAllArtists
                                            )
                                            Spacer(modifier = Modifier.height(20.dp))
                                            ModernEmptyState(
                                                icon = Icons.Rounded.Person,
                                                title = context.getString(R.string.home_no_artists),
                                                subtitle = context.getString(R.string.home_no_artists_desc),
                                                iconSize = 48.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "NEW_RELEASES" -> {
                        if (showNewReleases) {
                            item(key = "section_new_releases") {
                                Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                                    Column {
                                        ModernSectionTitle(
                                            title = context.getString(R.string.home_new_releases),
                                            subtitle = context.getString(R.string.home_fresh_music),
                                            onPlayAll = {
                                                coroutineScope.launch {
                                                    val allNewReleaseSongs = newReleases.flatMap { album ->
                                                        musicViewModel.getMusicRepository().getSongsForAlbumLocal(album.id)
                                                    }
                                                    if (allNewReleaseSongs.isNotEmpty()) {
                                                        musicViewModel.playQueueWithUserRule(allNewReleaseSongs, sourceLabel = "Home New Releases")
                                                    }
                                                }
                                            },
                                            onShufflePlay = {
                                                coroutineScope.launch {
                                                    val allNewReleaseSongs = newReleases.flatMap { album ->
                                                        musicViewModel.getMusicRepository().getSongsForAlbumLocal(album.id)
                                                    }
                                                    if (allNewReleaseSongs.isNotEmpty()) {
                                                        musicViewModel.playShuffled(allNewReleaseSongs)
                                                    }
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        if (newReleases.isNotEmpty()) {
                                            val isTablet = widthSizeClass != WindowWidthSizeClass.Compact
                                            if (isTablet) {
                                                val gridColumns = when (widthSizeClass) {
                                                    WindowWidthSizeClass.Medium -> 3
                                                    WindowWidthSizeClass.Expanded -> 4
                                                    else -> 2
                                                }
                                                val gridState = rememberLazyGridState()
                                                val estimatedRows = (newReleases.take(newReleasesCount).size + gridColumns - 1) / gridColumns
                                                val cardHeight = when (widthSizeClass) {
                                                    WindowWidthSizeClass.Medium -> 300.dp
                                                    WindowWidthSizeClass.Expanded -> 330.dp
                                                    else -> 240.dp
                                                }
                                                val gridHeight = (cardHeight.value * minOf(estimatedRows, 2) + 20f * (minOf(estimatedRows, 2) - 1)).dp

                                                LazyVerticalGrid(
                                                    columns = GridCells.Fixed(gridColumns),
                                                    state = gridState,
                                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                                    verticalArrangement = Arrangement.spacedBy(24.dp),
                                                    modifier = Modifier.height(gridHeight),
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    items(
                                                        items = newReleases.take(newReleasesCount),
                                                        key = { "newrelease_${it.id}" },
                                                        contentType = { "album" }
                                                    ) { album ->
                                                        ModernAlbumCard(
                                                            album = album,
                                                            onClick = { onAlbumClick(album) },
                                                            widthSizeClass = widthSizeClass,
                                                            heightSizeClass = heightSizeClass
                                                        )
                                                    }
                                                }
                                            } else {
                                                val newReleasesListState = rememberLazyListState()
                                                LazyRow(
                                                    state = newReleasesListState,
                                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    items(
                                                        items = newReleases.take(newReleasesCount),
                                                        key = { "newrelease_${it.id}" },
                                                        contentType = { "album" }
                                                    ) { album ->
                                                        ModernAlbumCard(
                                                            album = album,
                                                            onClick = { onAlbumClick(album) },
                                                            widthSizeClass = widthSizeClass,
                                                            heightSizeClass = heightSizeClass
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            ModernEmptyState(
                                                icon = Icons.Rounded.NewReleases,
                                                title = context.getString(R.string.home_no_new_releases),
                                                subtitle = context.getString(R.string.home_no_new_releases_desc),
                                                iconSize = 48.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "RECENTLY_ADDED" -> {
                        if (showRecentlyAdded) {
                            item(key = "section_recently_added") {
                                Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                                    Column {
                                        ModernSectionTitle(
                                            title = context.getString(R.string.home_recently_added),
                                            subtitle = context.getString(R.string.home_latest_additions),
                                            onPlayAll = {
                                                if (recentlyAddedSongs.isNotEmpty()) {
                                                    musicViewModel.playQueueWithUserRule(recentlyAddedSongs, sourceLabel = "Home Recently Added")
                                                }
                                            },
                                            onShufflePlay = {
                                                if (recentlyAddedSongs.isNotEmpty()) {
                                                    musicViewModel.playShuffled(recentlyAddedSongs)
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        if (recentlyAddedAlbums.isNotEmpty()) {
                                            val isTablet = widthSizeClass != WindowWidthSizeClass.Compact
                                            if (isTablet) {
                                                val gridColumns = when (widthSizeClass) {
                                                    WindowWidthSizeClass.Medium -> 3
                                                    WindowWidthSizeClass.Expanded -> 4
                                                    else -> 2
                                                }
                                                val gridState = rememberLazyGridState()
                                                val estimatedRows = (recentlyAddedAlbums.take(recentlyAddedCount).size + gridColumns - 1) / gridColumns
                                                val cardHeight = when (widthSizeClass) {
                                                    WindowWidthSizeClass.Medium -> 300.dp
                                                    WindowWidthSizeClass.Expanded -> 330.dp
                                                    else -> 240.dp
                                                }
                                                val gridHeight = (cardHeight.value * minOf(estimatedRows, 2) + 24f * (minOf(estimatedRows, 2) - 1)).dp

                                                LazyVerticalGrid(
                                                    columns = GridCells.Fixed(gridColumns),
                                                    state = gridState,
                                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                                    verticalArrangement = Arrangement.spacedBy(24.dp),
                                                    modifier = Modifier.height(gridHeight),
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    items(
                                                        items = recentlyAddedAlbums.take(recentlyAddedCount),
                                                        key = { "recentalbum_${it.id}" },
                                                        contentType = { "album" }
                                                    ) { album ->
                                                        ModernAlbumCard(
                                                            album = album,
                                                            onClick = { onAlbumClick(album) },
                                                            widthSizeClass = widthSizeClass,
                                                            heightSizeClass = heightSizeClass
                                                        )
                                                    }
                                                }
                                            } else {
                                                val recentlyAddedListState = rememberLazyListState()
                                                LazyRow(
                                                    state = recentlyAddedListState,
                                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    items(
                                                        items = recentlyAddedAlbums.take(recentlyAddedCount),
                                                        key = { "recentalbum_${it.id}" },
                                                        contentType = { "album" }
                                                    ) { album ->
                                                        ModernAlbumCard(
                                                            album = album,
                                                            onClick = { onAlbumClick(album) },
                                                            widthSizeClass = widthSizeClass,
                                                            heightSizeClass = heightSizeClass
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            ModernEmptyState(
                                                icon = Icons.Rounded.LibraryAdd,
                                                title = context.getString(R.string.home_no_recently_added),
                                                subtitle = context.getString(R.string.home_no_recently_added_desc),
                                                iconSize = 48.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "RECOMMENDED" -> {
                        if (showRecommended) {
                            item(key = "section_recommended") {
                                Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                                    val recommendedSongs = remember(recentlyPlayed, songs, recommendedCount) {
                                        if (recentlyPlayed.isNotEmpty()) {
                                            val playedArtists = recentlyPlayed.map { it.artist }.distinct()
                                            val playedAlbums = recentlyPlayed.map { it.album }.distinct()

                                            songs.filter { song ->
                                                (song.artist in playedArtists || song.album in playedAlbums) &&
                                                        !recentlyPlayed.contains(song)
                                            }.shuffled().take(recommendedCount)
                                        } else {
                                            songs.shuffled().take(recommendedCount)
                                        }
                                    }

                                    ModernRecommendedSection(
                                        recommendedSongs = recommendedSongs,
                                        onSongClick = onSongClick
                                    )
                                }
                            }
                        }
                    }
                    "STATS" -> {
                        if (showListeningStats) {
                            item(key = "section_stats") {
                                Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                                    Column {
                                        ModernSectionTitle(
                                            title = context.getString(R.string.home_listening_stats),
                                            subtitle = context.getString(R.string.home_listening_stats_subtitle),
                                            viewAllAction = onNavigateToStats
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        ModernListeningStatsSection(onClick = onNavigateToStats, showHeader = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add some bottom padding for mini player
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// Modern Component Functions - Part 2

@Composable
private fun ModernWelcomeSection(
    greeting: String,
    festiveTheme: FestiveThemeType = FestiveThemeType.NONE,
    onSearchClick: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = viewModel<chromahub.rhythm.app.viewmodel.MusicViewModel>()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Responsive font sizes
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isCompactWidth = screenWidthDp < 400
    val isTablet = screenWidthDp >= 600

    val greetingFontSize = when {
        isCompactWidth -> 28.sp
        isTablet -> 36.sp
        else -> 32.sp
    }
    val messageFontSize = when {
        isCompactWidth -> 12.sp
        isTablet -> 16.sp
        else -> 14.sp
    }
    val quoteFontSize = when {
        isCompactWidth -> 11.sp
        isTablet -> 14.sp
        else -> 12.sp
    }

    // Enhanced time-based quotes with proper time ranges
    val timeBasedQuote = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour in 0..4 -> listOf(
                context.getString(R.string.home_quote_late_night_1),
                context.getString(R.string.home_quote_late_night_2),
                context.getString(R.string.home_quote_late_night_3),
                context.getString(R.string.home_quote_late_night_4)
            )
            hour in 5..11 -> listOf(
                context.getString(R.string.home_quote_morning_1),
                context.getString(R.string.home_quote_morning_2),
                context.getString(R.string.home_quote_morning_3),
                context.getString(R.string.home_quote_morning_4)
            )
            hour in 12..16 -> listOf(
                context.getString(R.string.home_quote_afternoon_1),
                context.getString(R.string.home_quote_afternoon_2),
                context.getString(R.string.home_quote_afternoon_3),
                context.getString(R.string.home_quote_afternoon_4)
            )
            hour in 17..20 -> listOf(
                context.getString(R.string.home_quote_evening_1),
                context.getString(R.string.home_quote_evening_2),
                context.getString(R.string.home_quote_evening_3),
                context.getString(R.string.home_quote_evening_4)
            )
            else -> listOf(
                context.getString(R.string.home_quote_night_1),
                context.getString(R.string.home_quote_night_2),
                context.getString(R.string.home_quote_night_3),
                context.getString(R.string.home_quote_night_4)
            )
        }.random()
    }

    val personalizedMessage = remember(recentlyPlayed) {
        if (recentlyPlayed.isNotEmpty()) {
            val recentSong = recentlyPlayed.firstOrNull()?.title
            if (!recentSong.isNullOrBlank()) {
                context.getString(R.string.home_continue_song, recentSong)
            } else {
                context.getString(R.string.home_adventure_continues)
            }
        } else {
            context.getString(R.string.home_discover_next_favorite)
        }
    }

    val timeBasedTheme = remember(festiveTheme) {
        when (festiveTheme) {
            FestiveThemeType.CHRISTMAS -> Triple("🎄", "christmas", "🎅")
            FestiveThemeType.NEW_YEAR -> Triple("🎉", "new_year", "🥳")
            FestiveThemeType.HALLOWEEN -> Triple("🎃", "halloween", "👻")
            FestiveThemeType.VALENTINES -> Triple("💝", "valentines", "💕")
            else -> {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                when {
                    hour in 0..4 -> Triple("🌙", "late_night", "⭐")  // Late night
                    hour in 5..11 -> Triple("☀️", "morning", "🌻")      // Morning
                    hour in 12..16 -> Triple("🌤️", "afternoon", "⚡") // Afternoon
                    hour in 17..20 -> Triple("🌅", "evening", "✨")    // Evening
                    else -> Triple("🌙", "night", "🌟")                // Night
                }
            }
        }
    }

    ExpressiveCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                onSearchClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = ExpressiveShapes.ExtraLarge
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Decorative elements in background
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Text(
                        text = timeBasedTheme.third,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.alpha(0.12f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Main greeting
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 0.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "emoji_pulse")
                    val emojiScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "emoji_scale"
                    )

                    Text(
                        text = timeBasedTheme.first,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .graphicsLayer {
                                scaleX = emojiScale
                                scaleY = emojiScale
                            }
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = timeBasedQuote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }

                    // Modern search button with expressive design
                    ExpressiveFilledIconButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            onSearchClick()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = context.getString(R.string.cd_search),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernRecentlyPlayedSection(
    recentlyPlayed: List<Song>,
    onSongClick: (Song) -> Unit,
    musicViewModel: chromahub.rhythm.app.viewmodel.MusicViewModel,
    coroutineScope: CoroutineScope,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Medium
) {
    val context = LocalContext.current
    Column {
        ModernSectionTitle(
            title = context.getString(R.string.home_recently_played),
            subtitle = context.getString(R.string.home_recently_played_subtitle),
            onPlayAll = {
                coroutineScope.launch {
                    if (recentlyPlayed.isNotEmpty()) {
                        musicViewModel.playQueueWithUserRule(recentlyPlayed, sourceLabel = "Home Recently Played")
                    }
                }
            },
            onShufflePlay = {
                coroutineScope.launch {
                    if (recentlyPlayed.isNotEmpty()) {
                        musicViewModel.playShuffled(recentlyPlayed)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (recentlyPlayed.isNotEmpty()) {
            val isTablet = widthSizeClass != WindowWidthSizeClass.Compact
            if (isTablet) {
                // Grid layout for tablets - better use of space
                val gridColumns = when (widthSizeClass) {
                    WindowWidthSizeClass.Medium -> 2
                    WindowWidthSizeClass.Expanded -> 3
                    else -> 2
                }
                val gridState = rememberLazyGridState()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(
                        items = recentlyPlayed,
                        key = { "recentplay_${it.id}" },
                        contentType = { "song" }
                    ) { song ->
                        ModernRecentSongCard(
                            song = song,
                            onClick = { onSongClick(song) },
                            widthSizeClass = widthSizeClass,
                            heightSizeClass = heightSizeClass
                        )
                    }
                }
            } else {
                // Horizontal scroll for phones
                val recentlyPlayedListState = rememberLazyListState()
                LazyRow(
                    state = recentlyPlayedListState,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = recentlyPlayed,
                        key = { "recentplay_${it.id}" },
                        contentType = { "song" }
                    ) { song ->
                        ModernRecentSongCard(
                            song = song,
                            onClick = { onSongClick(song) },
                            widthSizeClass = widthSizeClass,
                            heightSizeClass = heightSizeClass
                        )
                    }
                }
            }
        } else {
            // Empty state for recently played
            ModernEmptyState(
                icon = Icons.Rounded.History,
                title = context.getString(R.string.home_no_recent_activity),
                subtitle = context.getString(R.string.home_no_recent_activity_desc),
                iconSize = 48.dp
            )
        }
    }
}

@Composable
private fun ModernRecentSongCard(
    song: Song,
    onClick: () -> Unit,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Medium
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val (cardWidth, cardHeight) = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 160.dp to 70.dp
            else -> 180.dp to 80.dp
        }
        WindowWidthSizeClass.Medium -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> Dp.Unspecified to 90.dp
            else -> Dp.Unspecified to 95.dp
        }
        WindowWidthSizeClass.Expanded -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> Dp.Unspecified to 100.dp
            else -> Dp.Unspecified to 100.dp
        }
        else -> 180.dp to 80.dp
    }

    val cardModifier = if (cardWidth == Dp.Unspecified) {
        Modifier
            .fillMaxWidth()
            .height(cardHeight)
    } else {
        Modifier
            .width(cardWidth)
            .height(cardHeight)
    }

    ExpressiveCard(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = cardModifier
            .shadow(
                elevation = 4.dp,
                shape = ExpressiveShapes.Large,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = ExpressiveShapes.Large
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            M3ImageUtils.TrackImage(
                imageUrl = song.artworkUri,
                trackName = song.title,
                modifier = Modifier.size(52.dp),
                applyExpressiveShape = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = context.getString(R.string.cd_play),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ModernSectionTitle(
    title: String,
    subtitle: String? = null,
    viewAllAction: (() -> Unit)? = null,
    onPlayAll: (() -> Unit)? = null,
    onShufflePlay: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onPlayAll != null || onShufflePlay != null) {
                ExpressiveButtonGroup(
                    style = ButtonGroupStyle.Tonal
                ) {
                    onPlayAll?.let { playAction ->
                        ExpressiveGroupButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                playAction()
                            },
                            isStart = true,
                            isEnd = onShufflePlay == null
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = context.getString(R.string.action_play),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    onShufflePlay?.let { shuffleAction ->
                        ExpressiveGroupButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                shuffleAction()
                            },
                            isStart = onPlayAll == null,
                            isEnd = true
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            if (onPlayAll == null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = context.getString(R.string.cd_shuffle),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            viewAllAction?.let {
                var isPressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "viewAllScale"
                )

                ExpressiveOutlinedButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        it()
                    },
                    shape = ExpressiveShapes.Full,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .scale(scale)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                }
                            )
                        }
                ) {
                    Text(
                        text = context.getString(R.string.ui_view_all),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ModernFeaturedSection(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    showAlbumName: Boolean = true,
    showArtistName: Boolean = true,
    showYear: Boolean = true,
    showPlayButton: Boolean = true,
    showGradient: Boolean = true,
    carouselStyle: Int = 0,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Medium
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val viewModel = viewModel<chromahub.rhythm.app.viewmodel.MusicViewModel>()

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Perfect fit for any display size
    val headerHeight = when (heightSizeClass) {
        WindowHeightSizeClass.Compact -> 280.dp
        else -> when (widthSizeClass) {
            WindowWidthSizeClass.Medium -> 500.dp
            WindowWidthSizeClass.Expanded -> 600.dp
            else -> screenWidth
        }
    }

    val carouselState = rememberCarouselState { albums.size }

    // Auto-scroll one album at a time so the active item always snaps correctly.
    LaunchedEffect(albums.size) {
        if (albums.size > 1) {
            while (true) {
                delay(4500)
                val currentItem = carouselState.currentItem
                val nextItem = (currentItem + 1) % albums.size
                carouselState.animateScrollToItem(nextItem)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
    ) {
        HorizontalUncontainedCarousel(
            state = carouselState,
            itemWidth = screenWidth,
            itemSpacing = 0.dp,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val album = albums[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape)
                    .clickable {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        onAlbumClick(album)
                    }
            ) {
                // Background Artwork
                M3ImageUtils.AlbumArt(
                    imageUrl = album.artworkUri,
                    albumName = album.title,
                    modifier = Modifier.fillMaxSize(),
                    applyExpressiveShape = false
                )

                // Heavy gradient using theme background
                if (showGradient) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.background, // Blend top edge
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                        Color.Transparent,
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                        MaterialTheme.colorScheme.background // Blend bottom edge
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                }

                // Bottom Content Overlays using theme typography colors
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    if (showAlbumName) {
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (showArtistName) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = album.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Row to handle Play Button on the left and Year on the right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        
                        if (showPlayButton) {
                            Button(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                    viewModel.playAlbum(album)
                                },
                                shape = RoundedCornerShape(percent = 50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = context.getString(R.string.action_play),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        } else {
                            // Empty spacer to push year to the right if the play button is hidden
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Display the Year on the bottom right
                        if (showYear && album.year > 0) {
                            Text(
                                text = album.year.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f), // Looks like a subtle watermark
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Additional Modern Components

@Composable
private fun ModernArtistsSection(
    artists: List<Artist>,
    songs: List<Song>,
    onArtistClick: (Artist) -> Unit,
    onViewAllArtists: () -> Unit,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Medium
) {
    val context = LocalContext.current
    Column {
        ModernSectionTitle(
            title = context.getString(R.string.home_top_artists),
            subtitle = context.getString(R.string.home_top_artists_subtitle),
            viewAllAction = onViewAllArtists
        )

        Spacer(modifier = Modifier.height(16.dp))

        val isTablet = widthSizeClass != WindowWidthSizeClass.Compact
        if (isTablet) {
            val gridColumns = when (widthSizeClass) {
                WindowWidthSizeClass.Medium -> 4
                WindowWidthSizeClass.Expanded -> 6
                else -> 3
            }
            val gridState = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.height(320.dp)
            ) {
                items(
                    items = artists,
                    key = { "artist_${it.id}" },
                    contentType = { "artist" }
                ) { artist ->
                    ModernArtistCard(
                        artist = artist,
                        songs = songs,
                        onClick = { onArtistClick(artist) },
                        widthSizeClass = widthSizeClass,
                        heightSizeClass = heightSizeClass
                    )
                }
            }
        } else {
            val artistsListState = rememberLazyListState()
            LazyRow(
                state = artistsListState,
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = artists,
                    key = { "artist_${it.id}" },
                    contentType = { "artist" }
                ) { artist ->
                    ModernArtistCard(
                        artist = artist,
                        songs = songs,
                        onClick = { onArtistClick(artist) },
                        widthSizeClass = widthSizeClass,
                        heightSizeClass = heightSizeClass
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernArtistCard(
    artist: Artist,
    songs: List<Song>,
    onClick: () -> Unit,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Medium
) {
    val context = LocalContext.current
    val viewModel = viewModel<chromahub.rhythm.app.viewmodel.MusicViewModel>()
    val haptic = LocalHapticFeedback.current

    val cardSize = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 100.dp
            else -> 120.dp
        }
        WindowWidthSizeClass.Medium -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 110.dp
            else -> 120.dp
        }
        WindowWidthSizeClass.Expanded -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 115.dp
            else -> 125.dp
        }
        else -> 120.dp
    }

    val isTablet = widthSizeClass != WindowWidthSizeClass.Compact
    val columnModifier = if (isTablet) {
        Modifier
            .fillMaxWidth()
            .clickable {
                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                onClick()
            }
    } else {
        Modifier
            .width(cardSize)
            .clickable {
                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                onClick()
            }
    }

    Column(
        modifier = columnModifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(cardSize)) {
            M3ImageUtils.ArtistImage(
                imageUrl = artist.artworkUri,
                artistName = artist.name,
                modifier = Modifier.fillMaxSize()
            )

            ExpressiveFilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                    viewModel.playArtist(artist)
                    onClick()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = context.getString(R.string.cd_play_artist, artist.name),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun ModernAlbumCard(
    album: Album,
    onClick: (Album) -> Unit,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Medium
) {
    val context = LocalContext.current
    val viewModel = viewModel<chromahub.rhythm.app.viewmodel.MusicViewModel>()
    val haptic = LocalHapticFeedback.current

    val (cardWidth, cardHeight) = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 140.dp to 210.dp
            else -> 160.dp to 240.dp
        }
        WindowWidthSizeClass.Medium -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 180.dp to 270.dp
            else -> 200.dp to 300.dp
        }
        WindowWidthSizeClass.Expanded -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 200.dp to 300.dp
            else -> 220.dp to 330.dp
        }
        else -> 160.dp to 240.dp
    }

    ExpressiveElevatedCard(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
            onClick(album)
        },
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .shadow(
                elevation = 12.dp,
                shape = ExpressiveShapes.SquircleLarge,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            ),
        shape = ExpressiveShapes.SquircleLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(
            when (widthSizeClass) {
                WindowWidthSizeClass.Compact -> 12.dp
                WindowWidthSizeClass.Medium -> 16.dp
                WindowWidthSizeClass.Expanded -> 20.dp
                else -> 12.dp
            }
        )) {
            Box {
                M3ImageUtils.AlbumArt(
                    imageUrl = album.artworkUri,
                    albumName = album.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = rememberExpressiveShapeFor(ExpressiveShapeTarget.ALBUM_ART)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            when (widthSizeClass) {
                                WindowWidthSizeClass.Compact -> 12.dp
                                WindowWidthSizeClass.Medium -> 16.dp
                                WindowWidthSizeClass.Expanded -> 20.dp
                                else -> 12.dp
                            }
                        )
                ) {
                    ExpressiveFilledIconButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                            viewModel.playAlbum(album)
                        },
                        modifier = Modifier.size(
                            when (widthSizeClass) {
                                WindowWidthSizeClass.Compact -> 40.dp
                                WindowWidthSizeClass.Medium -> 48.dp
                                WindowWidthSizeClass.Expanded -> 52.dp
                                else -> 40.dp
                            }
                        ),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = context.getString(R.string.cd_play_album),
                            modifier = Modifier.size(
                                when (widthSizeClass) {
                                    WindowWidthSizeClass.Compact -> 20.dp
                                    WindowWidthSizeClass.Medium -> 24.dp
                                    WindowWidthSizeClass.Expanded -> 26.dp
                                    else -> 20.dp
                                }
                            )
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        when (widthSizeClass) {
                            WindowWidthSizeClass.Compact -> 12.dp
                            WindowWidthSizeClass.Medium -> 16.dp
                            WindowWidthSizeClass.Expanded -> 20.dp
                            else -> 12.dp
                        }
                    ),
                verticalArrangement = Arrangement.spacedBy(
                    when (widthSizeClass) {
                        WindowWidthSizeClass.Compact -> 4.dp
                        WindowWidthSizeClass.Medium -> 6.dp
                        WindowWidthSizeClass.Expanded -> 8.dp
                        else -> 4.dp
                    }
                )
            ) {
                Text(
                    text = album.title,
                    style = when (widthSizeClass) {
                        WindowWidthSizeClass.Compact -> MaterialTheme.typography.titleSmall
                        WindowWidthSizeClass.Medium -> MaterialTheme.typography.titleMedium
                        WindowWidthSizeClass.Expanded -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = when (widthSizeClass) {
                        WindowWidthSizeClass.Compact -> 18.sp
                        WindowWidthSizeClass.Medium -> 22.sp
                        WindowWidthSizeClass.Expanded -> 24.sp
                        else -> 18.sp
                    }
                )

                Text(
                    text = album.artist,
                    style = when (widthSizeClass) {
                        WindowWidthSizeClass.Compact -> MaterialTheme.typography.bodySmall
                        WindowWidthSizeClass.Medium -> MaterialTheme.typography.bodyMedium
                        WindowWidthSizeClass.Expanded -> MaterialTheme.typography.bodyMedium
                        else -> MaterialTheme.typography.bodySmall
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ModernSongCard(
    song: Song,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    ExpressiveElevatedCard(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier
            .width(190.dp)
            .height(270.dp)
            .shadow(
                elevation = 10.dp,
                shape = ExpressiveShapes.SquircleLarge,
                ambientColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = ExpressiveShapes.SquircleLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = ExpressiveShapes.SquircleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    M3ImageUtils.TrackImage(
                        imageUrl = song.artworkUri,
                        trackName = song.title,
                        modifier = Modifier.fillMaxSize(),
                        applyExpressiveShape = true
                    )
                }
            }

            Column(
                modifier = Modifier.height(60.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ModernListeningStatsSection(
    onClick: () -> Unit = {},
    showHeader: Boolean = true
) {
    val context = LocalContext.current
    val viewModel = viewModel<chromahub.rhythm.app.viewmodel.MusicViewModel>()
    val songs by viewModel.songs.collectAsState()

    var statsSummary by remember { mutableStateOf<chromahub.rhythm.app.shared.data.repository.PlaybackStatsRepository.PlaybackStatsSummary?>(null) }

    LaunchedEffect(songs) {
        statsSummary = viewModel.loadPlaybackStats(chromahub.rhythm.app.shared.data.repository.StatsTimeRange.ALL_TIME)
    }

    val listeningTimeHours = remember(statsSummary) {
        val totalMillis = statsSummary?.totalDurationMs ?: 0L
        val hours = totalMillis / (1000 * 60 * 60)
        if (hours < 1) 0 else hours.toInt()
    }

    val songsPlayed = remember(statsSummary) {
        statsSummary?.totalPlayCount ?: 0
    }

    val uniqueArtistsCount = remember(statsSummary) {
        statsSummary?.uniqueArtists ?: 0
    }

    ExpressiveElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = ExpressiveShapes.SquircleLarge
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            if (showHeader) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = ExpressiveShapes.SquircleMedium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = context.getString(R.string.home_listening_stats),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = context.getString(R.string.home_listening_stats_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ExpressiveLargeIconButton(
                        onClick = onClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = context.getString(R.string.cd_view_detailed_stats),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ExpressiveStatItem(
                    modifier = Modifier.weight(1f),
                    value = listeningTimeHours,
                    suffix = if (listeningTimeHours < 1) "" else "h",
                    label = if (listeningTimeHours < 1) context.getString(R.string.home_stat_listening_time_short) else context.getString(R.string.home_stat_listening_time),
                    icon = RhythmIcons.Player.Timer,
                    accentColor = MaterialTheme.colorScheme.primary
                )

                ExpressiveStatItem(
                    modifier = Modifier.weight(1f),
                    value = songsPlayed,
                    label = context.getString(R.string.home_stat_songs_played),
                    icon = RhythmIcons.Music.MusicNote,
                    accentColor = MaterialTheme.colorScheme.secondary
                )

                ExpressiveStatItem(
                    modifier = Modifier.weight(1f),
                    value = uniqueArtistsCount,
                    label = context.getString(R.string.home_stat_artists),
                    icon = RhythmIcons.Artist,
                    accentColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun ExpressiveStatItem(
    modifier: Modifier = Modifier,
    value: Int,
    suffix: String = "",
    label: String,
    icon: ImageVector,
    accentColor: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = ExpressiveShapes.SquircleMedium,
            color = accentColor.copy(alpha = 0.12f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        ExpressiveAnimatedCounter(
            value = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            suffix = suffix
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ModernStatCard(
    value: String,
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color
) {
    ExpressiveCard(
        modifier = Modifier.width(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = ExpressiveShapes.Large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModernRecommendedSection(
    recommendedSongs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        ModernSectionTitle(
            title = context.getString(R.string.home_recommended_title),
            subtitle = context.getString(R.string.home_recommended_subtitle)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (recommendedSongs.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                shape = ExpressiveShapes.Large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(25.dp)
                ) {
                    recommendedSongs.forEachIndexed { index, song ->
                        RecommendedSongItem(
                            song = song,
                            onClick = { onSongClick(song) }
                        )

                        if (index != recommendedSongs.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        } else {
            ModernEmptyState(
                icon = Icons.Rounded.TipsAndUpdates,
                title = context.getString(R.string.home_no_recommendations),
                subtitle = context.getString(R.string.home_no_recommendations_desc),
                iconSize = 48.dp
            )
        }
    }
}

@Composable
private fun RecommendedSongItem(
    song: Song,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                onClick()
            })
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3ImageUtils.TrackImage(
            imageUrl = song.artworkUri,
            trackName = song.title,
            modifier = Modifier.size(52.dp),
            applyExpressiveShape = true
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        ExpressiveFilledIconButton(
            onClick = {
                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play",
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
private fun ModernEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconSize: Dp = 64.dp
) {
    ExpressiveElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = ExpressiveShapes.SquircleLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = ExpressiveShapes.SquircleMedium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.size(iconSize + 24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}