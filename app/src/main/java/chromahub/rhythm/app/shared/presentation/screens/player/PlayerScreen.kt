package chromahub.rhythm.app.shared.presentation.screens.player

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavController
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.ExtraControlBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AddToPlaylistBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AlbumBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.ArtistBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.PlaybackBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.QueueBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.SongInfoBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.dialogs.PlaybackPitchDialog
import chromahub.rhythm.app.features.local.presentation.components.dialogs.PlaybackSpeedDialog
import chromahub.rhythm.app.features.local.presentation.components.player.SleepTimerBottomSheetNew
import chromahub.rhythm.app.features.local.presentation.components.lyrics.LyricsEditorBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.player.formatDuration
import chromahub.rhythm.app.features.local.presentation.navigation.Screen
import chromahub.rhythm.app.features.local.presentation.screens.LibraryTab
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.model.LyricsData
import chromahub.rhythm.app.shared.data.model.PlaybackLocation
import chromahub.rhythm.app.shared.data.model.Playlist
import chromahub.rhythm.app.shared.data.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    song: Song?,
    isPlaying: Boolean,
    progress: () -> Float,
    location: PlaybackLocation?,
    queuePosition: Int = 1,
    queueTotal: Int = 1,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onLyricsSeek: ((Long) -> Unit)? = null,
    onBack: () -> Unit,
    onLocationClick: () -> Unit,
    onQueueClick: () -> Unit,
    locations: List<PlaybackLocation> = emptyList(),
    onLocationSelect: (PlaybackLocation) -> Unit = {},
    volume: Float = 0.7f,
    isMuted: Boolean = false,
    onVolumeChange: (Float) -> Unit = {},
    onToggleMute: () -> Unit = {},
    onMaxVolume: () -> Unit = {},
    onRefreshDevices: () -> Unit = {},
    onStopDeviceMonitoring: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    isShuffleEnabled: Boolean = false,
    repeatMode: Int = 0,
    isFavorite: Boolean = false,
    showLyrics: Boolean = true,
    onlineOnlyLyrics: Boolean = false,
    lyrics: LyricsData? = null,
    isLoadingLyrics: Boolean = false,
    onRetryLyrics: () -> Unit = {},
    onEditLyrics: (String) -> Unit = {},
    onPickLyricsFile: () -> Unit = {},
    onSaveLyrics: (String, String) -> Unit = { _, _ -> },
    playlists: List<Playlist> = emptyList(),
    queue: List<Song> = emptyList(),
    onSongClick: (Song) -> Unit = {},
    onSongClickAtIndex: (Int) -> Unit = { _ -> },
    onRemoveFromQueueAtIndex: (Int) -> Unit = { _ -> },
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onAddSongsToQueue: () -> Unit = {},
    onNavigateToLibrary: (LibraryTab) -> Unit = {},
    showAddToPlaylistSheet: Boolean = false,
    onAddToPlaylistSheetDismiss: () -> Unit = {},
    onAddSongToPlaylist: (Song, String) -> Unit = { _, _ -> },
    onCreatePlaylist: (String) -> Unit = {},
    onShowCreatePlaylistDialog: () -> Unit = {},
    onClearQueue: () -> Unit = {},
    isMediaLoading: Boolean = false,
    isSeeking: Boolean = false,
    onShowAlbumBottomSheet: () -> Unit = {},
    onShowArtistBottomSheet: () -> Unit = {},
    songs: List<Song> = emptyList(),
    albums: List<Album> = emptyList(),
    artists: List<Artist> = emptyList(),
    onPlayAlbumSongs: (List<Song>) -> Unit = {},
    onShuffleAlbumSongs: (List<Song>) -> Unit = {},
    onPlayArtistSongs: (List<Song>) -> Unit = {},
    onShuffleArtistSongs: (List<Song>) -> Unit = {},
    appSettings: AppSettings,
    musicViewModel: MusicViewModel,
    navController: NavController,
    isStreamingMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val playerThemeId by appSettings.playerThemeId.collectAsState()

    if (playerThemeId == "EXPRESSIVE") {
        val context = LocalContext.current
        val useHoursFormat by appSettings.useHoursInTimeFormat.collectAsState()
        val progressValue = progress().coerceIn(0f, 1f)
        val totalTimeMs = song?.duration ?: 0L
        val currentTimeMs = (progressValue * totalTimeMs).toLong()

        var showQueueSheet by remember { mutableStateOf(false) }
        var showSongInfoSheet by remember { mutableStateOf(false) }
        var showMoreSheet by remember { mutableStateOf(false) }
        var showDeviceOutputSheet by remember { mutableStateOf(false) }
        var showAddToPlaylistSheetInternal by remember { mutableStateOf(false) }
        var showPlaybackSpeedDialog by remember { mutableStateOf(false) }
        var showPlaybackPitchDialog by remember { mutableStateOf(false) }
        var showSleepTimerBottomSheet by remember { mutableStateOf(false) }
        var showAlbumSheet by remember { mutableStateOf(false) }
        var showArtistSheet by remember { mutableStateOf(false) }
        var selectedAlbum by remember { mutableStateOf<Album?>(null) }
        var selectedArtist by remember { mutableStateOf<Artist?>(null) }
        var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }
        var showLyricsView by remember { mutableStateOf(false) }
        var showLyricsEditorDialog by remember { mutableStateOf(false) }

        val playbackSpeed by musicViewModel.playbackSpeed.collectAsState()
        val playbackPitch by musicViewModel.playbackPitch.collectAsState()
        val sleepTimerActive by musicViewModel.sleepTimerActive.collectAsState()
        val sleepTimerRemainingSeconds by musicViewModel.sleepTimerRemainingSeconds.collectAsState()
        val equalizerEnabled by musicViewModel.equalizerEnabled.collectAsState()
        val hiddenChips by appSettings.hiddenPlayerChips.collectAsState()
        val syncSpeedAndPitch by appSettings.syncSpeedAndPitch.collectAsState()
        val artistSeparatorEnabled by appSettings.artistSeparatorEnabled.collectAsState()
        val artistSeparatorDelimiters by appSettings.artistSeparatorDelimiters.collectAsState()

        val splitArtistNames: (String) -> List<String> = remember {
            { artistName ->
                chromahub.rhythm.app.util.ArtistSeparator.splitArtistNames(
                    artistName = artistName,
                    delimiters = artistSeparatorDelimiters,
                    enabled = artistSeparatorEnabled
                )
            }
        }

        fun resolveAlbumForSong(currentSong: Song): Album? {
            return albums.firstOrNull { album ->
                (currentSong.albumId.isNotBlank() && album.id == currentSong.albumId) ||
                        album.title.equals(currentSong.album, ignoreCase = true) ||
                        (album.title.equals(currentSong.album, ignoreCase = true) &&
                                album.artist.equals(currentSong.artist, ignoreCase = true))
            }
        }

        fun resolveArtistForSong(currentSong: Song): Artist? {
            val albumArtist = currentSong.albumArtist?.trim().orEmpty()
            val artistNames = if (albumArtist.isNotBlank() && !albumArtist.equals("<unknown>", ignoreCase = true)) {
                splitArtistNames(albumArtist)
            } else {
                splitArtistNames(currentSong.artist)
            }

            return artists.firstOrNull { artist ->
                artistNames.any { it.equals(artist.name, ignoreCase = true) }
            }
        }

        val lyricsTimeOffset by musicViewModel.lyricsTimeOffset.collectAsState()
        val lyricsWritePermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                musicViewModel.completeLyricsWriteAfterPermission(
                    onSuccess = { },
                    onError = { errorMessage ->
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                musicViewModel.cancelPendingLyricsWrite()
                Toast.makeText(context, "Permission denied. Could not embed lyrics.", Toast.LENGTH_LONG).show()
            }
        }

        val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val deviceOutputSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val addToPlaylistSheetState = rememberModalBottomSheetState()
        val albumBottomSheetState = rememberModalBottomSheetState()
        val artistBottomSheetState = rememberModalBottomSheetState()
        val currentSongAlbumForSheet = remember(song, albums, songs) {
            song?.let { currentSong ->
                resolveAlbumForSong(currentSong)
            }
        }
        val currentSongArtistForSheet = remember(song, artists) {
            song?.let { currentSong ->
                resolveArtistForSong(currentSong)
            }
        }

        ExpressivePlayerScreen(
            song = song,
            isPlaying = isPlaying,
            isFavorite = isFavorite,
            progress = { progressValue },
            currentTimeStr = formatDuration(currentTimeMs, useHoursFormat),
            totalTimeStr = formatDuration(totalTimeMs, useHoursFormat),
            queuePosition = queuePosition,
            queueTotal = queueTotal,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            showLyricsView = showLyricsView,
            showLyrics = showLyrics,
            lyrics = lyrics,
            isLoadingLyrics = isLoadingLyrics,
            onlineOnlyLyrics = onlineOnlyLyrics,
            onLyricsSeek = onLyricsSeek,
            onRetryLyrics = onRetryLyrics,
            onShowLyricsEditor = { showLyricsEditorDialog = true },
            isMediaLoading = isMediaLoading,
            isSeeking = isSeeking,
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onSkipPrevious = onSkipPrevious,
            onSkipNext = onSkipNext,
            onToggleFavorite = onToggleFavorite,
            onToggleShuffle = onToggleShuffle,
            onToggleRepeat = onToggleRepeat,
            onToggleLyrics = { showLyricsView = !showLyricsView },
            onSongInfoClick = { showSongInfoSheet = true },
            onShowAlbumBottomSheet = {
                currentSongAlbumForSheet?.let { album ->
                    selectedAlbum = album
                    showAlbumSheet = true
                }
            },
            onShowArtistBottomSheet = {
                currentSongArtistForSheet?.let { artist ->
                    selectedArtist = artist
                    showArtistSheet = true
                }
            },
            onMoreClick = {
                showSongInfoSheet = false
                showMoreSheet = true
            },
            onDeviceClick = { showDeviceOutputSheet = true },
            onQueueClick = { showQueueSheet = true },
            onBack = onBack,
            location = location,
            appSettings = appSettings,
            modifier = modifier
        )

        if (showDeviceOutputSheet) {
            LaunchedEffect(showDeviceOutputSheet) {
                if (showDeviceOutputSheet) {
                    onRefreshDevices()
                }
            }

            PlaybackBottomSheet(
                locations = locations,
                currentLocation = location,
                volume = volume,
                isMuted = isMuted,
                musicViewModel = musicViewModel,
                onLocationSelect = {
                    onLocationSelect(it)
                    showDeviceOutputSheet = false
                },
                onVolumeChange = onVolumeChange,
                onToggleMute = onToggleMute,
                onMaxVolume = onMaxVolume,
                onRefreshDevices = onRefreshDevices,
                onDismiss = {
                    showDeviceOutputSheet = false
                    onStopDeviceMonitoring()
                },
                appSettings = appSettings,
                onNavigateToSettings = {
                    showDeviceOutputSheet = false
                    navController.navigate(Screen.TunerQueuePlayback.route)
                },
                onNavigateToGoMode = null,
                onNavigateToEqualizer = {
                    showDeviceOutputSheet = false
                    navController.navigate(Screen.Equalizer.route)
                },
                sheetState = deviceOutputSheetState
            )
        }

        if (showQueueSheet && song != null) {
            QueueBottomSheet(
                currentSong = song,
                queue = queue,
                currentQueueIndex = queuePosition - 1,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                onSongClick = { selectedSong ->
                    onSongClick(selectedSong)
                    showQueueSheet = false
                },
                onSongClickAtIndex = { index ->
                    onSongClickAtIndex(index)
                    showQueueSheet = false
                },
                onDismiss = { showQueueSheet = false },
                onRemoveSongAtIndex = onRemoveFromQueueAtIndex,
                onMoveQueueItem = onMoveQueueItem,
                onAddSongsClick = {
                    showQueueSheet = false
                    onNavigateToLibrary(LibraryTab.SONGS)
                },
                onClearQueue = {
                    onClearQueue()
                    showQueueSheet = false
                },
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                sheetState = queueSheetState
            )
        }

        if (showSongInfoSheet && song != null) {
            SongInfoBottomSheet(
                song = song,
                onDismiss = { showSongInfoSheet = false },
                appSettings = appSettings,
                isStreamingMode = isStreamingMode,
                onEditSong = { _, _, _, _, _, _, _, _ -> },
                onShowLyricsEditor = { }
            )
        }

        if (showMoreSheet) {
            val moreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val haptic = LocalHapticFeedback.current

            ExtraControlBottomSheet(
                onDismiss = { showMoreSheet = false },
                sheetState = moreSheetState,
                hiddenChips = hiddenChips,
                equalizerEnabled = equalizerEnabled,
                sleepTimerActive = sleepTimerActive,
                sleepTimerRemainingSeconds = sleepTimerRemainingSeconds,
                lyrics = lyrics,
                isFavorite = isFavorite,
                onAddToPlaylist = { showAddToPlaylistSheetInternal = true },
                onToggleFavorite = onToggleFavorite,
                onPlaybackSpeed = { showPlaybackSpeedDialog = true },
                onPlaybackPitch = { showPlaybackPitchDialog = true },
                onEqualizer = { navController.navigate(Screen.Equalizer.route) },
                onSleepTimer = { showSleepTimerBottomSheet = true },
                onLyricsEditor = { showLyricsEditorDialog = true },
                onAlbum = {
                    currentSongAlbumForSheet?.let { album ->
                        selectedAlbum = album
                        showAlbumSheet = true
                    }
                },
                onArtist = {
                    currentSongArtistForSheet?.let { artist ->
                        selectedArtist = artist
                        showArtistSheet = true
                    }
                },
                onSongInfo = { showSongInfoSheet = true },
                onShareFile = {
                    song?.let { currentSong ->
                        try {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "audio/*"
                                putExtra(android.content.Intent.EXTRA_STREAM, currentSong.uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share ${currentSong.title}"))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Unable to share file", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                haptic = haptic,
                isExtraSmallWidth = false,
                isCompactWidth = false
            )
        }

        if (showAddToPlaylistSheetInternal && song != null) {
            AddToPlaylistBottomSheet(
                song = selectedSongForPlaylist ?: song,
                playlists = playlists,
                onDismissRequest = {
                    showAddToPlaylistSheetInternal = false
                    selectedSongForPlaylist = null
                },
                onAddToPlaylist = { playlist ->
                    onAddSongToPlaylist(selectedSongForPlaylist ?: song, playlist.id)
                    showAddToPlaylistSheetInternal = false
                    selectedSongForPlaylist = null
                },
                onCreateNewPlaylist = onShowCreatePlaylistDialog,
                sheetState = addToPlaylistSheetState
            )
        }

        if (showPlaybackSpeedDialog) {
            PlaybackSpeedDialog(
                currentSpeed = playbackSpeed,
                syncEnabled = syncSpeedAndPitch,
                onSyncChange = { appSettings.setSyncSpeedAndPitch(it) },
                onDismiss = { showPlaybackSpeedDialog = false },
                onSave = { speed ->
                    musicViewModel.setPlaybackSpeed(speed)
                    if (syncSpeedAndPitch) {
                        musicViewModel.setPlaybackPitch(speed)
                    }
                    showPlaybackSpeedDialog = false
                }
            )
        }

        if (showPlaybackPitchDialog) {
            PlaybackPitchDialog(
                currentPitch = playbackPitch,
                syncEnabled = syncSpeedAndPitch,
                onSyncChange = { appSettings.setSyncSpeedAndPitch(it) },
                onDismiss = { showPlaybackPitchDialog = false },
                onSave = { pitch ->
                    musicViewModel.setPlaybackPitch(pitch)
                    if (syncSpeedAndPitch) {
                        musicViewModel.setPlaybackSpeed(pitch)
                    }
                    showPlaybackPitchDialog = false
                }
            )
        }

        if (showSleepTimerBottomSheet) {
            SleepTimerBottomSheetNew(
                onDismiss = { showSleepTimerBottomSheet = false },
                currentSong = song,
                isPlaying = isPlaying,
                musicViewModel = musicViewModel
            )
        }

        if (showAlbumSheet && selectedAlbum != null && song != null) {
            AlbumBottomSheet(
                album = selectedAlbum!!,
                onDismiss = { showAlbumSheet = false },
                onSongClick = onSongClick,
                onPlayAll = onPlayAlbumSongs,
                onShufflePlay = onShuffleAlbumSongs,
                onAddToQueue = { onAddSongsToQueue() },
                onAddSongToPlaylist = { track ->
                    selectedSongForPlaylist = track
                    showAddToPlaylistSheetInternal = true
                },
                onPlayerClick = { showAlbumSheet = false },
                sheetState = albumBottomSheetState,
                haptics = LocalHapticFeedback.current,
                onToggleFavorite = { onToggleFavorite() },
                onShowSongInfo = { showSongInfoSheet = true },
                currentSong = song,
                isPlaying = isPlaying,
                showAddToQueueAction = true,
                showAddToPlaylistAction = true
            )
        }

        if (showArtistSheet && selectedArtist != null && song != null) {
            ArtistBottomSheet(
                artist = selectedArtist!!,
                onDismiss = { showArtistSheet = false },
                onSongClick = onSongClick,
                onAlbumClick = { album ->
                    selectedAlbum = album
                    showAlbumSheet = true
                },
                onPlayAll = onPlayArtistSongs,
                onShufflePlay = onShuffleArtistSongs,
                onAddToQueue = { onAddSongsToQueue() },
                onAddSongToPlaylist = { track ->
                    selectedSongForPlaylist = track
                    showAddToPlaylistSheetInternal = true
                },
                onPlayerClick = { showArtistSheet = false },
                sheetState = artistBottomSheetState,
                haptics = LocalHapticFeedback.current,
                onToggleFavorite = { onToggleFavorite() },
                onShowSongInfo = { showSongInfoSheet = true },
                currentSong = song,
                isPlaying = isPlaying,
                songs = songs,
                albums = albums
            )
        }

        if (showLyricsEditorDialog) {
            LyricsEditorBottomSheet(
                currentLyrics = lyrics?.getBestLyrics() ?: "",
                songTitle = song?.title ?: "Unknown",
                initialTimeOffset = lyricsTimeOffset,
                onDismiss = { showLyricsEditorDialog = false },
                onSave = { editedLyrics, timeOffset ->
                    musicViewModel.saveEditedLyrics(editedLyrics, timeOffset)
                },
                onRefresh = {
                    musicViewModel.clearLyricsCacheAndRefetch()
                },
                onEmbedInFile = { editedLyrics ->
                    musicViewModel.embedLyricsInFile(
                        lyrics = editedLyrics,
                        onPermissionRequired = { pendingRequest ->
                            try {
                                val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(
                                    pendingRequest.intentSender
                                ).build()
                                lyricsWritePermissionLauncher.launch(intentSenderRequest)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Failed to request permission: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                musicViewModel.cancelPendingLyricsWrite()
                            }
                        }
                    )
                }
            )
        }
    } else {
        MaterialPlayerScreen(
            song = song,
            isPlaying = isPlaying,
            progress = progress,
            location = location,
            queuePosition = queuePosition,
            queueTotal = queueTotal,
            onPlayPause = onPlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onSeek = onSeek,
            onLyricsSeek = onLyricsSeek,
            onBack = onBack,
            onLocationClick = onLocationClick,
            onQueueClick = onQueueClick,
            locations = locations,
            onLocationSelect = onLocationSelect,
            volume = volume,
            isMuted = isMuted,
            onVolumeChange = onVolumeChange,
            onToggleMute = onToggleMute,
            onMaxVolume = onMaxVolume,
            onRefreshDevices = onRefreshDevices,
            onStopDeviceMonitoring = onStopDeviceMonitoring,
            onToggleShuffle = onToggleShuffle,
            onToggleRepeat = onToggleRepeat,
            onToggleFavorite = onToggleFavorite,
            onAddToPlaylist = onAddToPlaylist,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            isFavorite = isFavorite,
            showLyrics = showLyrics,
            onlineOnlyLyrics = onlineOnlyLyrics,
            lyrics = lyrics,
            isLoadingLyrics = isLoadingLyrics,
            onRetryLyrics = onRetryLyrics,
            onEditLyrics = onEditLyrics,
            onPickLyricsFile = onPickLyricsFile,
            onSaveLyrics = onSaveLyrics,
            playlists = playlists,
            queue = queue,
            onSongClick = onSongClick,
            onSongClickAtIndex = onSongClickAtIndex,
            onRemoveFromQueueAtIndex = onRemoveFromQueueAtIndex,
            onMoveQueueItem = onMoveQueueItem,
            onAddSongsToQueue = onAddSongsToQueue,
            onNavigateToLibrary = onNavigateToLibrary,
            showAddToPlaylistSheet = showAddToPlaylistSheet,
            onAddToPlaylistSheetDismiss = onAddToPlaylistSheetDismiss,
            onAddSongToPlaylist = onAddSongToPlaylist,
            onCreatePlaylist = onCreatePlaylist,
            onShowCreatePlaylistDialog = onShowCreatePlaylistDialog,
            onClearQueue = onClearQueue,
            isMediaLoading = isMediaLoading,
            isSeeking = isSeeking,
            onShowAlbumBottomSheet = onShowAlbumBottomSheet,
            onShowArtistBottomSheet = onShowArtistBottomSheet,
            songs = songs,
            albums = albums,
            artists = artists,
            onPlayAlbumSongs = onPlayAlbumSongs,
            onShuffleAlbumSongs = onShuffleAlbumSongs,
            onPlayArtistSongs = onPlayArtistSongs,
            onShuffleArtistSongs = onShuffleArtistSongs,
            appSettings = appSettings,
            musicViewModel = musicViewModel,
            navController = navController,
            isStreamingMode = isStreamingMode,
            modifier = modifier
        )
    }
}
