package chromahub.rhythm.app.features.streaming.data.repository

import android.content.Context
import android.util.Log
import chromahub.rhythm.app.core.domain.model.AlbumItem
import chromahub.rhythm.app.core.domain.model.ArtistItem
import chromahub.rhythm.app.core.domain.model.PlayableItem
import chromahub.rhythm.app.core.domain.model.PlaylistItem
import chromahub.rhythm.app.core.domain.model.SourceType
import chromahub.rhythm.app.core.utils.NetworkUtils
import chromahub.rhythm.app.features.streaming.data.provider.JellyfinApiClient
import chromahub.rhythm.app.features.streaming.data.provider.ProviderConnectionResult
import chromahub.rhythm.app.features.streaming.data.provider.ProviderAlbum
import chromahub.rhythm.app.features.streaming.data.provider.ProviderArtist
import chromahub.rhythm.app.features.streaming.data.provider.ProviderPlaylist
import chromahub.rhythm.app.features.streaming.data.provider.ProviderSong
import chromahub.rhythm.app.features.streaming.data.provider.SubsonicApiClient
import chromahub.rhythm.app.features.streaming.domain.model.BrowseCategory
import chromahub.rhythm.app.features.streaming.domain.model.StreamingAlbum
import chromahub.rhythm.app.features.streaming.domain.model.StreamingArtist
import chromahub.rhythm.app.features.streaming.domain.model.StreamingPlaylist
import chromahub.rhythm.app.features.streaming.domain.model.StreamingServiceId
import chromahub.rhythm.app.features.streaming.domain.model.StreamingSong
import chromahub.rhythm.app.features.streaming.domain.repository.StreamingMusicRepository
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.network.NetworkClient
import chromahub.rhythm.app.network.DeezerApiService
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import chromahub.rhythm.app.util.ArtistSeparator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provider-backed implementation used by Rhythm GO mode.
 */
class StreamingMusicRepositoryImpl(
    private val context: Context
) : StreamingMusicRepository {

    data class ServiceConnectionInfo(
        val displayName: String,
        val serverUrl: String
    )

    private val appSettings = AppSettings.getInstance(context)

    private val subsonicClient = SubsonicApiClient(context)
    private val jellyfinClient = JellyfinApiClient(context)

    private val songsFlow = MutableStateFlow<List<PlayableItem>>(emptyList())
    private val albumsFlow = MutableStateFlow<List<AlbumItem>>(emptyList())
    private val artistsFlow = MutableStateFlow<List<ArtistItem>>(emptyList())
    private val playlistsFlow = MutableStateFlow<List<PlaylistItem>>(emptyList())

    private val likedSongsFlow = MutableStateFlow<List<StreamingSong>>(emptyList())
    private val followedArtistsFlow = MutableStateFlow<List<StreamingArtist>>(emptyList())
    private val savedAlbumsFlow = MutableStateFlow<List<StreamingAlbum>>(emptyList())
    private val downloadedSongsFlow = MutableStateFlow<List<StreamingSong>>(emptyList())

    private val likedSongIds = linkedSetOf<String>()
    private val followedArtistIds = linkedSetOf<String>()
    private val savedAlbumIds = linkedSetOf<String>()
    private val followedPlaylistIds = linkedSetOf<String>()

    private val songCache = LinkedHashMap<String, StreamingSong>()
    private val artistArtworkCache = LinkedHashMap<String, String>()

    override val currentService: SourceType
        get() = serviceToSourceType(activeServiceId())

    suspend fun connect(
        serviceId: String,
        serverUrl: String,
        username: String,
        password: String
    ): ServiceConnectionInfo {
        val normalizedService = normalizeServiceId(serviceId)

        val result = when (normalizedService) {
            StreamingServiceId.SUBSONIC -> subsonicClient.login(serverUrl, username, password)
            StreamingServiceId.JELLYFIN -> jellyfinClient.login(serverUrl, username, password)
            else -> Result.failure(IllegalArgumentException("Unsupported streaming service"))
        }

        val connection = result.getOrElse { throw it }
        return ServiceConnectionInfo(
            displayName = connection.displayName,
            serverUrl = connection.serverUrl
        )
    }

    suspend fun disconnect(serviceId: String) {
        when (normalizeServiceId(serviceId)) {
            StreamingServiceId.SUBSONIC -> subsonicClient.logout()
            StreamingServiceId.JELLYFIN -> jellyfinClient.logout()
        }

        if (activeServiceId() == normalizeServiceId(serviceId)) {
            clearInMemoryCatalog()
        }
    }

    fun isServiceConnected(serviceId: String): Boolean {
        return when (normalizeServiceId(serviceId)) {
            StreamingServiceId.SUBSONIC -> subsonicClient.isConnected()
            StreamingServiceId.JELLYFIN -> jellyfinClient.isConnected()
            else -> false
        }
    }

    override suspend fun isAuthenticated(): Boolean {
        return isServiceConnected(activeServiceId())
    }

    override suspend fun authenticate(): Boolean {
        return when (activeServiceId()) {
            StreamingServiceId.SUBSONIC -> subsonicClient.ping().isSuccess
            StreamingServiceId.JELLYFIN -> jellyfinClient.ping().isSuccess
            else -> false
        }
    }

    override suspend fun logout() {
        disconnect(activeServiceId())
    }

    override suspend fun getRecommendations(limit: Int): List<StreamingSong> {
        val activePrefix = "${activeServiceId()}::"
        return songCache.values
            .asSequence()
            .filter { it.id.startsWith(activePrefix) }
            .take(limit.coerceAtLeast(1))
            .toList()
    }

    override suspend fun getNewReleases(limit: Int): List<StreamingAlbum> {
        return albumsFlow.value
            .filterIsInstance<StreamingAlbum>()
            .take(limit.coerceAtLeast(1))
    }

    override suspend fun getFeaturedPlaylists(limit: Int): List<StreamingPlaylist> {
        return playlistsFlow.value
            .filterIsInstance<StreamingPlaylist>()
            .take(limit.coerceAtLeast(1))
    }

    override suspend fun getBrowseCategories(): List<BrowseCategory> {
        return listOf(
            BrowseCategory(id = "recent", name = "Recent"),
            BrowseCategory(id = "favorites", name = "Favorites"),
            BrowseCategory(id = "library", name = "Library")
        )
    }

    override suspend fun getCategoryPlaylists(categoryId: String, limit: Int): List<StreamingPlaylist> {
        val normalizedCategory = categoryId.trim().lowercase()
        val safeLimit = limit.coerceAtLeast(1)
        val playlists = playlistsFlow.value.filterIsInstance<StreamingPlaylist>()

        return when (normalizedCategory) {
            "recent", "library" -> playlists.take(safeLimit)
            "favorites" -> playlists.filter { it.id in followedPlaylistIds }.take(safeLimit)
            else -> playlists.filter {
                it.name.contains(categoryId, ignoreCase = true) ||
                    (it.description?.contains(categoryId, ignoreCase = true) == true)
            }.take(safeLimit).ifEmpty { playlists.take(safeLimit) }
        }
    }

    override suspend fun getTopCharts(limit: Int): List<StreamingSong> {
        return getRecommendations(limit)
    }

    override fun getLikedSongs(): Flow<List<StreamingSong>> = likedSongsFlow.asStateFlow()

    override suspend fun likeSong(songId: String): Boolean {
        likedSongIds.add(songId)
        updateLikedSongsFlow()
        return true
    }

    override suspend fun unlikeSong(songId: String): Boolean {
        val removed = likedSongIds.remove(songId)
        updateLikedSongsFlow()
        return removed
    }

    override suspend fun isSongLiked(songId: String): Boolean = likedSongIds.contains(songId)

    override suspend fun followArtist(artistId: String): Boolean {
        followedArtistIds.add(artistId)
        updateFollowedArtistsFlow()
        return true
    }

    override suspend fun unfollowArtist(artistId: String): Boolean {
        val removed = followedArtistIds.remove(artistId)
        updateFollowedArtistsFlow()
        return removed
    }

    override suspend fun isArtistFollowed(artistId: String): Boolean = followedArtistIds.contains(artistId)

    override fun getFollowedArtists(): Flow<List<StreamingArtist>> = followedArtistsFlow.asStateFlow()

    override suspend fun saveAlbum(albumId: String): Boolean {
        savedAlbumIds.add(albumId)
        updateSavedAlbumsFlow()
        return true
    }

    override suspend fun unsaveAlbum(albumId: String): Boolean {
        val removed = savedAlbumIds.remove(albumId)
        updateSavedAlbumsFlow()
        return removed
    }

    override fun getSavedAlbums(): Flow<List<StreamingAlbum>> = savedAlbumsFlow.asStateFlow()

    override suspend fun followPlaylist(playlistId: String): Boolean {
        val playlist = getPlaylistById(playlistId) ?: return false
        followedPlaylistIds.add(playlist.id)
        return true
    }

    override suspend fun unfollowPlaylist(playlistId: String): Boolean {
        return followedPlaylistIds.remove(playlistId)
    }

    override suspend fun createPlaylist(
        name: String,
        description: String?,
        isPublic: Boolean
    ): StreamingPlaylist? {
        val serviceId = activeServiceId()
        if (!isServiceConnected(serviceId)) {
            return null
        }

        val createdPlaylist = when (serviceId) {
            StreamingServiceId.JELLYFIN -> jellyfinClient.createPlaylist(
                name = name,
                songIds = emptyList(),
                description = description,
                isPublic = isPublic
            ).getOrNull()

            StreamingServiceId.SUBSONIC -> null
            else -> null
        } ?: return null

        val mappedPlaylist = mapProviderPlaylist(serviceId, createdPlaylist)
        playlistsFlow.value = (playlistsFlow.value.filterNot { it.id == mappedPlaylist.id } + mappedPlaylist)
            .sortedBy { it.name.lowercase() }
        return mappedPlaylist
    }

    override suspend fun addSongsToPlaylist(playlistId: String, songIds: List<String>): Boolean {
        val decodedPlaylist = decodePlaylistId(playlistId) ?: return false
        val serviceId = decodedPlaylist.first
        val providerPlaylistId = decodedPlaylist.second
        val providerSongIds = songIds.mapNotNull { decodeSongId(it)?.second ?: it }.filter { it.isNotBlank() }

        if (providerSongIds.isEmpty() || !isServiceConnected(serviceId)) {
            return false
        }

        val success = when (serviceId) {
            StreamingServiceId.JELLYFIN -> jellyfinClient.addSongsToPlaylist(providerPlaylistId, providerSongIds).isSuccess
            StreamingServiceId.SUBSONIC -> false
            else -> false
        }

        if (success) {
            syncPlaylists()
        }

        return success
    }

    override suspend fun removeSongsFromPlaylist(playlistId: String, songIds: List<String>): Boolean {
        val decodedPlaylist = decodePlaylistId(playlistId) ?: return false
        val serviceId = decodedPlaylist.first
        val providerPlaylistId = decodedPlaylist.second
        val providerSongIds = songIds.mapNotNull { decodeSongId(it)?.second ?: it }.filter { it.isNotBlank() }

        if (providerSongIds.isEmpty() || !isServiceConnected(serviceId)) {
            return false
        }

        val success = when (serviceId) {
            StreamingServiceId.JELLYFIN -> jellyfinClient.removeSongsFromPlaylist(providerPlaylistId, providerSongIds).isSuccess
            StreamingServiceId.SUBSONIC -> false
            else -> false
        }

        if (success) {
            syncPlaylists()
        }

        return success
    }

    override suspend fun getStreamingUrl(songId: String): String? {
        val (serviceId, providerId) = decodeSongId(songId) ?: return null
        
        // Check if streaming is allowed by network settings
        val allowCellular = appSettings.allowCellularStreaming.value
        if (!NetworkUtils.canStream(context, allowCellular)) {
            return null // Network conditions don't allow streaming
        }
        
        // Check if service is connected
        if (!isServiceConnected(serviceId)) {
            return null // Provider is not connected
        }
        
        // Check if offline mode is enabled - fallback to cache or return null
        if (appSettings.offlineMode.value) {
            // In offline mode, only allow cached URLs
            val cached = songCache[songId]
            return cached?.streamingUrl // May be null if not previously cached
        }
        
        val bitrate = desiredBitrateKbps()

        val resolved = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.buildStreamUrl(providerId, bitrate)
            StreamingServiceId.JELLYFIN -> jellyfinClient.buildStreamUrl(providerId, bitrate)
            else -> null
        }

        if (!resolved.isNullOrBlank()) {
            val existing = songCache[songId]
            if (existing != null) {
                songCache[songId] = existing.copy(streamingUrl = resolved)
            }
        }

        return resolved
    }

    /**
     * Invalidate cached streaming URL for a specific song.
     */
    fun invalidateStreamingUrlCache(songId: String) {
        songCache.remove(songId)
    }

    /**
     * Clear all cached streaming URLs so subsequent requests re-resolve.
     */
    fun invalidateAllStreamingUrlCache() {
        songCache.clear()
    }

    override suspend fun getRelatedTracks(songId: String, limit: Int): List<StreamingSong> {
        val decodedSong = decodeSongId(songId) ?: return emptyList()
        val serviceId = decodedSong.first
        val providerTracks = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.getRelatedTracks(decodedSong.second, limit)
            StreamingServiceId.JELLYFIN -> jellyfinClient.getRelatedTracks(decodedSong.second, limit)
            else -> Result.success(emptyList())
        }.getOrElse { emptyList() }

        return providerTracks.map { mapProviderSong(serviceId, it) }
            .take(limit.coerceAtLeast(1))
    }

    override suspend fun getArtistTopTracks(artistId: String, limit: Int): List<StreamingSong> {
        val artistName = extractArtistNameFromId(artistId)
        if (artistName.isBlank()) {
            return emptyList()
        }

        val serviceId = artistId.substringBefore("::", activeServiceId())
        val providerTracks = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.getArtistTopTracks(artistName, limit)
            StreamingServiceId.JELLYFIN -> jellyfinClient.getArtistTopTracks(artistName, limit)
            else -> Result.success(emptyList())
        }.getOrElse { emptyList() }

        if (providerTracks.isNotEmpty()) {
            return providerTracks.map { mapProviderSong(serviceId, it) }.take(limit.coerceAtLeast(1))
        }

        return cachedSongsForArtist(artistName)
            .take(limit.coerceAtLeast(1))
    }

    override suspend fun getArtistAlbums(artistId: String): List<StreamingAlbum> {
        val artistName = extractArtistNameFromId(artistId)
        if (artistName.isBlank()) {
            return emptyList()
        }

        val serviceId = artistId.substringBefore("::", activeServiceId())
        val providerAlbums = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.getArtistAlbums(artistName)
            StreamingServiceId.JELLYFIN -> jellyfinClient.getArtistAlbums(artistName)
            else -> Result.success(emptyList())
        }.getOrElse { emptyList() }

        if (providerAlbums.isNotEmpty()) {
            return providerAlbums.map { mapProviderAlbum(serviceId, it) }
        }

        val cachedSongs = cachedSongsForArtist(artistName)
        if (cachedSongs.isNotEmpty()) {
            return deriveAlbumsFromSongs(cachedSongs, limit = 24)
        }

        return albumsFlow.value
            .filterIsInstance<StreamingAlbum>()
            .filter { it.artist.equals(artistName, ignoreCase = true) }
    }
    
    private fun extractArtistNameFromId(artistId: String): String {
        // Handle multiple ID formats:
        // "JELLYFIN::artist::artist_name" -> "artist_name"
        if (artistId.contains("::")) {
            val parts = artistId.split("::") 
            if (parts.size >= 3 && parts[1] == "artist") {
                // Format: "JELLYFIN::artist::artist_name"
                return parts[2].replace("_", " ").trim()
            }
        }
        
        // "derived:JELLYFIN:artist:artist_name" -> "artist_name"
        if (artistId.contains(":artist:")) {
            val afterArtist = artistId.substringAfter(":artist:")
            return afterArtist.replace("_", " ").trim()
        }
        
        // Fallback: assume the whole ID is a name
        return artistId.replace("_", " ").trim()
    }

            private suspend fun resolveProviderArtistId(serviceId: String, artistName: String): String? {
                val providerArtists = when (serviceId) {
                    StreamingServiceId.SUBSONIC -> subsonicClient.searchArtists(artistName, limit = 20)
                    StreamingServiceId.JELLYFIN -> jellyfinClient.searchArtists(artistName, limit = 20)
                    else -> Result.success(emptyList())
                }.getOrElse { emptyList() }

                val exactMatch = providerArtists.firstOrNull { it.name.equals(artistName, ignoreCase = true) }
                return (exactMatch ?: providerArtists.firstOrNull())?.providerId
            }

    override suspend fun getRelatedArtists(artistId: String, limit: Int): List<StreamingArtist> {
        val artistName = extractArtistNameFromId(artistId)
        if (artistName.isBlank()) {
            return emptyList()
        }

        val serviceId = artistId.substringBefore("::", activeServiceId())
                val providerArtistId = resolveProviderArtistId(serviceId, artistName) ?: return emptyList()

        val providerArtists = when (serviceId) {
                    StreamingServiceId.SUBSONIC -> subsonicClient.getRelatedArtists(providerArtistId, limit)
                    StreamingServiceId.JELLYFIN -> jellyfinClient.getRelatedArtists(providerArtistId, limit)
            else -> Result.success(emptyList())
        }.getOrElse { emptyList() }

                return providerArtists.map { mapProviderArtist(serviceId, it) }
    }

    override suspend fun downloadSong(songId: String): Boolean = false

    override suspend fun removeDownload(songId: String): Boolean = false

    override suspend fun isDownloaded(songId: String): Boolean = false

    override fun getDownloadedSongs(): Flow<List<StreamingSong>> = downloadedSongsFlow.asStateFlow()

    override suspend fun getRandomSongs(limit: Int): List<StreamingSong> {
        val serviceId = activeServiceId()
        if (!isServiceConnected(serviceId)) {
            return emptyList()
        }

        val providerResult: Result<List<ProviderSong>> = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.getRandomSongs(limit)
            StreamingServiceId.JELLYFIN -> jellyfinClient.getRandomSongs(limit)
            else -> Result.success(emptyList())
        }

        return providerResult.getOrElse { emptyList() }
            .map { mapProviderSong(serviceId, it) }
    }

    override suspend fun getAlbumList(type: String, limit: Int): List<StreamingAlbum> {
        val serviceId = activeServiceId()
        if (!isServiceConnected(serviceId)) {
            return emptyList()
        }

        val providerResult: Result<List<ProviderAlbum>> = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.getAlbumList(type, limit)
            StreamingServiceId.JELLYFIN -> jellyfinClient.getAlbumList(type, limit)
            else -> Result.success(emptyList())
        }

        return providerResult.getOrElse { emptyList() }
            .map { mapProviderAlbum(serviceId, it) }
    }

    override suspend fun searchSongs(query: String): List<PlayableItem> {
        val serviceId = activeServiceId()
        if (!isServiceConnected(serviceId)) {
            clearInMemoryCatalog()
            return emptyList()
        }

        val providerResult: Result<List<ProviderSong>> = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.searchSongs(query, SEARCH_LIMIT)
            StreamingServiceId.JELLYFIN -> jellyfinClient.searchSongs(query, SEARCH_LIMIT)
            else -> Result.success(emptyList())
        }

        val mappedSongs = providerResult.getOrElse { emptyList() }
            .map { mapProviderSong(serviceId, it) }

        mergeCatalog(mappedSongs)
        return mappedSongs
    }

    override suspend fun searchAlbums(query: String): List<AlbumItem> {
        val serviceId = activeServiceId()
        if (!isServiceConnected(serviceId)) {
            return emptyList()
        }

        val providerAlbums = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.searchAlbums(query)
            StreamingServiceId.JELLYFIN -> jellyfinClient.searchAlbums(query)
            else -> Result.success(emptyList())
        }.getOrElse { emptyList() }

        if (providerAlbums.isNotEmpty()) {
            return providerAlbums.map { mapProviderAlbum(serviceId, it) }
        }

        val songs = searchSongs(query).filterIsInstance<StreamingSong>()
        return buildAlbumItems(serviceId, songs)
    }

    override suspend fun searchArtists(query: String): List<ArtistItem> {
        val serviceId = activeServiceId()
        if (!isServiceConnected(serviceId)) {
            return emptyList()
        }

        val providerArtists = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.searchArtists(query)
            StreamingServiceId.JELLYFIN -> jellyfinClient.searchArtists(query)
            else -> Result.success(emptyList())
        }.getOrElse { emptyList() }

        if (providerArtists.isNotEmpty()) {
            return providerArtists.map { mapProviderArtist(serviceId, it) }
        }

        val songs = searchSongs(query).filterIsInstance<StreamingSong>()
        return buildDerivedArtistItems(serviceId, songs)
    }

    override suspend fun searchPlaylists(query: String): List<PlaylistItem> {
        val serviceId = activeServiceId()
        if (!isServiceConnected(serviceId)) {
            return emptyList()
        }

        val result = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.searchPlaylists(query)
            StreamingServiceId.JELLYFIN -> jellyfinClient.searchPlaylists(query)
            else -> Result.success(emptyList())
        }

        return result.getOrElse { emptyList() }
            .map { providerPlaylist ->
                val tracks = fetchPlaylistTracks(serviceId, providerPlaylist.providerId)
                mapProviderPlaylist(serviceId, providerPlaylist, tracks)
            }
    }

    override fun getSongs(): Flow<List<PlayableItem>> = songsFlow.asStateFlow()

    override fun getAlbums(): Flow<List<AlbumItem>> = albumsFlow.asStateFlow()

    override fun getArtists(): Flow<List<ArtistItem>> = artistsFlow.asStateFlow()

    override fun getPlaylists(): Flow<List<PlaylistItem>> = playlistsFlow.asStateFlow()

    override suspend fun getSongById(id: String): PlayableItem? = songCache[id]

    override suspend fun syncCatalog(limit: Int): List<StreamingSong> {
        val serviceId = activeServiceId()
        if (!isServiceConnected(serviceId)) {
            clearInMemoryCatalog()
            return emptyList()
        }

        val providerSongs = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.fetchLibrarySongs(limit)
            StreamingServiceId.JELLYFIN -> jellyfinClient.fetchLibrarySongs(limit)
            else -> Result.success(emptyList())
        }.getOrElse { emptyList() }

        val mappedSongs = providerSongs.map { mapProviderSong(serviceId, it) }
        replaceCatalog(mappedSongs)
        
        // Also sync playlists
        syncPlaylists()
        
        return mappedSongs
    }

    override suspend fun syncPlaylists(): List<StreamingPlaylist> {
        val serviceId = activeServiceId()
        if (!isServiceConnected(serviceId)) {
            playlistsFlow.value = emptyList()
            return emptyList()
        }

        val result = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.getPlaylists()
            StreamingServiceId.JELLYFIN -> jellyfinClient.getPlaylists()
            else -> Result.success(emptyList())
        }

        val playlists = result.getOrElse { emptyList() }
            .map { providerPlaylist ->
                val tracks = fetchPlaylistTracks(serviceId, providerPlaylist.providerId)
                mapProviderPlaylist(serviceId, providerPlaylist, tracks)
            }

        playlistsFlow.value = playlists
        return playlists
    }

    override suspend fun getAlbumById(id: String): AlbumItem? {
        albumsFlow.value.firstOrNull { it.id == id }?.let { return it }

        val decodedAlbum = decodeAlbumId(id) ?: return null
        val serviceId = decodedAlbum.first
        val albumTitle = decodedAlbum.second
        val artistName = decodedAlbum.third

        val providerAlbums = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.searchAlbums(albumTitle, limit = 20)
            StreamingServiceId.JELLYFIN -> jellyfinClient.searchAlbums(albumTitle, limit = 20)
            else -> Result.success(emptyList())
        }.getOrElse { emptyList() }

        val providerAlbum = providerAlbums.firstOrNull {
            it.title.equals(albumTitle, ignoreCase = true) &&
                (artistName.isBlank() || it.artist.equals(artistName, ignoreCase = true))
        } ?: providerAlbums.firstOrNull()

        return providerAlbum?.let { mapProviderAlbum(serviceId, it) }
    }

    override suspend fun getArtistById(id: String): ArtistItem? {
        artistsFlow.value.firstOrNull { it.id == id }?.let { return it }

        val artistName = extractArtistNameFromId(id)
        if (artistName.isBlank()) {
            return null
        }

        val serviceId = id.substringBefore("::", activeServiceId())
        val providerArtists = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.searchArtists(artistName, limit = 20)
            StreamingServiceId.JELLYFIN -> jellyfinClient.searchArtists(artistName, limit = 20)
            else -> Result.success(emptyList())
        }.getOrElse { emptyList() }

        val providerArtist = providerArtists.firstOrNull {
            it.name.equals(artistName, ignoreCase = true)
        } ?: providerArtists.firstOrNull()

        return providerArtist?.let { mapProviderArtist(serviceId, it) }
    }

    override suspend fun getPlaylistById(id: String): PlaylistItem? {
        return playlistsFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun getSongsForAlbum(albumId: String): List<PlayableItem> {
        val localMatches = songsFlow.value
            .filterIsInstance<StreamingSong>()
            .filter { buildAlbumId(activeServiceId(), it.artist, it.album) == albumId }

        if (localMatches.isNotEmpty()) {
            return localMatches
        }

        val decodedAlbum = decodeAlbumId(albumId) ?: return emptyList()
        val serviceId = decodedAlbum.first
        val albumTitle = decodedAlbum.second
        val artistName = decodedAlbum.third

        val providerTracks = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.getAlbumSongs(albumTitle, artistName)
            StreamingServiceId.JELLYFIN -> jellyfinClient.getAlbumSongs(albumTitle, artistName)
            else -> Result.success(emptyList())
        }.getOrElse { emptyList() }

        return providerTracks.map { mapProviderSong(serviceId, it) }
    }

    private suspend fun replaceCatalog(songs: List<StreamingSong>) {
        val serviceId = activeServiceId()

        songCache.clear()
        songs.forEach { song ->
            songCache[song.id] = song
        }

        songsFlow.value = songs
        albumsFlow.value = buildAlbumItems(serviceId, songs)
        artistsFlow.value = buildDerivedArtistItems(serviceId, songs)
        playlistsFlow.value = emptyList()

        updateLikedSongsFlow()
        updateSavedAlbumsFlow()
        updateFollowedArtistsFlow()
    }

    private suspend fun mergeCatalog(songs: List<StreamingSong>) {
        if (songs.isEmpty()) {
            return
        }

        val serviceId = activeServiceId()
        songs.forEach { song ->
            songCache[song.id] = song
        }
        trimSongCache()

        val mergedSongs = (songsFlow.value.filterIsInstance<StreamingSong>() + songs)
            .distinctBy { it.id }

        songsFlow.value = mergedSongs
        albumsFlow.value = buildAlbumItems(serviceId, mergedSongs)
        artistsFlow.value = buildDerivedArtistItems(serviceId, mergedSongs)
        playlistsFlow.value = emptyList()

        updateLikedSongsFlow()
        updateSavedAlbumsFlow()
        updateFollowedArtistsFlow()
    }

    private fun clearInMemoryCatalog() {
        songCache.clear()
        followedPlaylistIds.clear()
        songsFlow.value = emptyList()
        albumsFlow.value = emptyList()
        artistsFlow.value = emptyList()
        playlistsFlow.value = emptyList()
    }

    private fun updateLikedSongsFlow() {
        likedSongsFlow.value = likedSongIds.mapNotNull { id -> songCache[id] }
    }

    private fun updateSavedAlbumsFlow() {
        savedAlbumsFlow.value = savedAlbumIds.mapNotNull { id ->
            albumsFlow.value.firstOrNull { it.id == id } as? StreamingAlbum
        }
    }

    private fun updateFollowedArtistsFlow() {
        followedArtistsFlow.value = followedArtistIds.mapNotNull { id ->
            artistsFlow.value.firstOrNull { it.id == id } as? StreamingArtist
        }
    }

    private fun mapProviderSong(serviceId: String, providerSong: ProviderSong): StreamingSong {
        val encodedId = encodeSongId(serviceId, providerSong.providerId)
        val sourceType = serviceToSourceType(serviceId)
        val streamingUrl = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.buildStreamUrl(providerSong.providerId, desiredBitrateKbps())
            StreamingServiceId.JELLYFIN -> jellyfinClient.buildStreamUrl(providerSong.providerId, desiredBitrateKbps())
            else -> null
        }

        return StreamingSong(
            id = encodedId,
            title = providerSong.title,
            artist = providerSong.artist,
            album = providerSong.album,
            duration = providerSong.durationMs,
            artworkUri = providerSong.artworkUrl,
            sourceType = sourceType,
            streamingUrl = streamingUrl,
            previewUrl = null,
            isPlayable = true,
            externalId = providerSong.providerId
        )
    }

    private suspend fun fetchPlaylistTracks(serviceId: String, providerPlaylistId: String): List<StreamingSong> {
        val result = when (serviceId) {
            StreamingServiceId.SUBSONIC -> subsonicClient.getPlaylistSongs(providerPlaylistId)
            StreamingServiceId.JELLYFIN -> jellyfinClient.getPlaylistSongs(providerPlaylistId)
            else -> Result.success(emptyList())
        }

        return result.getOrElse { emptyList() }
            .map { mapProviderSong(serviceId, it) }
    }

    private fun mapProviderPlaylist(
        serviceId: String,
        providerPlaylist: ProviderPlaylist,
        tracks: List<StreamingSong> = emptyList()
    ): StreamingPlaylist {
        val encodedId = encodePlaylistId(serviceId, providerPlaylist.providerId)
        val resolvedTracks = if (tracks.isNotEmpty()) tracks else emptyList()
        
        return StreamingPlaylist(
            id = encodedId,
            name = providerPlaylist.name,
            description = providerPlaylist.description,
            artworkUri = providerPlaylist.artworkUrl ?: resolvedTracks.firstOrNull()?.artworkUri,
            songCount = providerPlaylist.songCount.takeIf { it > 0 } ?: resolvedTracks.size,
            isEditable = when (serviceId) {
                StreamingServiceId.SUBSONIC -> providerPlaylist.owner?.equals(subsonicClient.getUsername(), ignoreCase = true) == true
                StreamingServiceId.JELLYFIN -> providerPlaylist.owner?.equals(jellyfinClient.getUsername(), ignoreCase = true) == true
                else -> false
            },
            sourceType = serviceToSourceType(serviceId),
            externalId = providerPlaylist.providerId,
            owner = if (providerPlaylist.owner != null) {
                chromahub.rhythm.app.features.streaming.domain.model.PlaylistOwner(
                    id = normalizeKey(providerPlaylist.owner),
                    displayName = providerPlaylist.owner,
                    imageUrl = null,
                    isCurrentUser = false
                )
            } else null,
            isPublic = providerPlaylist.isPublic,
            isCollaborative = false,
            followers = null,
            snapshotId = null,
            tracks = resolvedTracks
        )
    }

    private fun buildAlbumItems(serviceId: String, songs: List<StreamingSong>): List<StreamingAlbum> {
        return songs
            .groupBy { it.artist to it.album }
            .map { (key, tracks) ->
                val (artist, album) = key
                StreamingAlbum(
                    id = buildAlbumId(serviceId, artist, album),
                    title = album,
                    artist = artist,
                    artworkUri = tracks.firstOrNull()?.artworkUri,
                    songCount = tracks.size,
                    year = null,
                    sourceType = serviceToSourceType(serviceId)
                )
            }
            .sortedBy { it.title.lowercase() }
    }

    private fun mapProviderAlbum(serviceId: String, providerAlbum: ProviderAlbum): StreamingAlbum {
        return StreamingAlbum(
            id = buildAlbumId(serviceId, providerAlbum.artist, providerAlbum.title),
            title = providerAlbum.title,
            artist = providerAlbum.artist,
            artworkUri = providerAlbum.artworkUrl,
            songCount = providerAlbum.songCount,
            year = providerAlbum.year,
            sourceType = serviceToSourceType(serviceId),
            externalId = providerAlbum.providerId,
            releaseDate = null,
            albumType = chromahub.rhythm.app.features.streaming.domain.model.AlbumType.ALBUM,
            genres = emptyList(),
            label = null,
            copyright = null,
            isExplicit = false
        )
    }

    private fun buildArtistItems(serviceId: String, songs: List<StreamingSong>): List<StreamingArtist> {
        // Apply artist separator settings to split collaborations, matching ViewModel behavior
        val separatorEnabled = appSettings.artistSeparatorEnabled.value
        val separatorDelimiters = if (separatorEnabled) {
            appSettings.artistSeparatorDelimiters.value.ifBlank { "/;,+&" }
        } else {
            ""
        }
        
        return songs
            .filter { it.artist.isNotBlank() }
            .flatMap { song ->
                val artistNames = ArtistSeparator.splitArtists(
                    artistString = song.artist,
                    delimiters = separatorDelimiters,
                    enabled = separatorEnabled
                )
                
                if (artistNames.isEmpty()) {
                    listOf(song to song.artist.trim())
                } else {
                    artistNames.mapNotNull { artistName ->
                        artistName.trim().takeIf { it.isNotBlank() }?.let { trimmedName ->
                            song to trimmedName
                        }
                    }
                }
            }
            .groupBy { (song, artistName) -> "${song.sourceType.name}:${artistName.lowercase()}" }
            .values
            .sortedByDescending { artistSongs -> artistSongs.size }
            .map { artistSongs ->
                val firstSong = artistSongs.first().first
                val artistName = artistSongs.first().second
                val tracks = artistSongs.map { it.first }
                val albumCount = tracks.map { it.album }.distinct().size
                
                // Look up enriched artwork from cache, with fallback to song artwork
                val artworkUri = artistArtworkCache[normalizeKey(artistName)] 
                    ?: tracks.firstNotNullOfOrNull { it.artworkUri }
                
                StreamingArtist(
                    id = buildArtistId(serviceId, artistName),
                    name = artistName,
                    artworkUri = artworkUri,
                    songCount = tracks.size,
                    albumCount = albumCount,
                    sourceType = serviceToSourceType(serviceId)
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private suspend fun buildDerivedArtistItems(
        serviceId: String,
        songs: List<StreamingSong>
    ): List<StreamingArtist> {
        return enrichArtistsWithDeezerImages(buildArtistItems(serviceId, songs))
    }

    private fun mapProviderArtist(serviceId: String, providerArtist: ProviderArtist): StreamingArtist {
        val cachedArtistSongs = cachedSongsForArtist(providerArtist.name)
        val cachedArtist = buildArtistItems(serviceId, cachedArtistSongs)
            .firstOrNull { it.name.equals(providerArtist.name, ignoreCase = true) }

        return StreamingArtist(
            id = buildArtistId(serviceId, providerArtist.name),
            name = providerArtist.name,
            artworkUri = providerArtist.artworkUrl
                ?: artistArtworkCache[normalizeKey(providerArtist.name)]
                ?: cachedArtist?.artworkUri,
            songCount = providerArtist.songCount.takeIf { it > 0 }
                ?: cachedArtist?.songCount
                ?: cachedArtistSongs.size,
            albumCount = providerArtist.albumCount.takeIf { it > 0 }
                ?: cachedArtist?.albumCount
                ?: cachedArtistSongs.map { it.album }.distinct().size,
            sourceType = serviceToSourceType(serviceId),
            externalId = providerArtist.providerId,
            genres = emptyList(),
            followers = null,
            popularity = null,
            bio = providerArtist.description
        )
    }

    private fun artistIdForSong(song: StreamingSong): String {
        val serviceId = decodeSongId(song.id)?.first ?: activeServiceId()
        return buildArtistId(serviceId, song.artist)
    }

    private fun cachedSongsForArtist(artistName: String): List<StreamingSong> {
        if (artistName.isBlank()) {
            return emptyList()
        }

        return (songsFlow.value.filterIsInstance<StreamingSong>() + songCache.values)
            .asSequence()
            .filter { song ->
                if (!song.artist.contains(artistName, ignoreCase = true)) {
                    false
                } else {
                    val artistNames = ArtistSeparator.splitArtists(
                        artistString = song.artist,
                        delimiters = appSettings.artistSeparatorDelimiters.value.ifBlank { "/;,+&" },
                        enabled = appSettings.artistSeparatorEnabled.value
                    )

                    artistNames.isEmpty() || artistNames.any { it.equals(artistName, ignoreCase = true) }
                }
            }
            .distinctBy { it.id }
            .toList()
    }

    private fun artistIdForAlbum(album: StreamingAlbum): String {
        val serviceId = album.id.substringBefore("::", activeServiceId())
        return buildArtistId(serviceId, album.artist)
    }

    private fun deriveAlbumsFromSongs(
        songs: List<StreamingSong>,
        limit: Int
    ): List<StreamingAlbum> {
        if (songs.isEmpty()) {
            return emptyList()
        }

        return songs
            .filter { it.album.isNotBlank() }
            .groupBy { song -> "${song.sourceType.name}:${song.artist.lowercase()}:${song.album.lowercase()}" }
            .values
            .sortedByDescending { albumSongs -> albumSongs.size }
            .take(limit.coerceAtLeast(1))
            .map { albumSongs ->
                val firstSong = albumSongs.first()
                StreamingAlbum(
                    id = buildAlbumId(
                        serviceId = firstSong.sourceType.name,
                        artist = firstSong.artist,
                        album = firstSong.album
                    ),
                    title = firstSong.album,
                    artist = firstSong.artist,
                    artworkUri = albumSongs.firstNotNullOfOrNull { it.artworkUri },
                    songCount = albumSongs.size,
                    year = firstSong.releaseDate?.take(4)?.toIntOrNull(),
                    sourceType = firstSong.sourceType,
                    tracks = albumSongs
                )
            }
    }
    
    /**
     * Legacy matching - kept for backward compatibility with exact ID matching.
     * Use extractArtistNameFromId and match by artist name instead.
     */
    @Deprecated("Use extractArtistNameFromId and match by artist name")
    private fun matchArtistIdByFormat(artistId: String, generatedId: String): Boolean {
        return artistId == generatedId || extractArtistNameFromId(artistId) == extractArtistNameFromId(generatedId)
    }

    private fun buildArtistId(serviceId: String, artist: String): String {
        return "$serviceId::artist::${normalizeKey(artist)}"
    }

    private fun buildAlbumId(serviceId: String, artist: String, album: String): String {
        return "$serviceId::album::${normalizeKey(artist)}::${normalizeKey(album)}"
    }

    private fun decodeAlbumId(albumId: String): Triple<String, String, String>? {
        val parts = albumId.split("::")
        if (parts.size < 4 || parts[1] != "album") {
            return null
        }

        val serviceId = parts[0]
        val artist = parts[2].replace("_", " ").trim()
        val album = parts.subList(3, parts.size).joinToString("::").replace("_", " ").trim()

        if (artist.isBlank() || album.isBlank()) {
            return null
        }

        return Triple(serviceId, album, artist)
    }

    private fun normalizeKey(value: String): String {
        return value.trim().lowercase().replace("\\s+".toRegex(), "_")
    }

    private fun trimSongCache() {
        if (songCache.size <= MAX_CACHE_SIZE) return

        val toRemove = songCache.size - MAX_CACHE_SIZE
        repeat(toRemove) {
            val firstKey = songCache.entries.firstOrNull()?.key ?: return
            songCache.remove(firstKey)
        }
    }

    private fun activeServiceId(): String {
        return normalizeServiceId(appSettings.streamingService.value)
    }

    private fun encodeSongId(serviceId: String, providerId: String): String {
        return "$serviceId::$providerId"
    }

    private fun decodeSongId(songId: String): Pair<String, String>? {
        val separatorIndex = songId.indexOf("::")
        if (separatorIndex <= 0 || separatorIndex >= songId.length - 2) {
            return null
        }

        val serviceId = songId.substring(0, separatorIndex)
        val providerId = songId.substring(separatorIndex + 2)
        return serviceId to providerId
    }

    private fun decodePlaylistId(playlistId: String): Pair<String, String>? {
        val marker = "::playlist::"
        val separatorIndex = playlistId.indexOf(marker)
        if (separatorIndex <= 0 || separatorIndex >= playlistId.length - marker.length) {
            return null
        }

        val serviceId = playlistId.substring(0, separatorIndex)
        val providerId = playlistId.substring(separatorIndex + marker.length)
        if (providerId.isBlank()) {
            return null
        }

        return serviceId to providerId
    }

    private fun encodePlaylistId(serviceId: String, providerId: String): String {
        return "$serviceId::playlist::$providerId"
    }

    private fun desiredBitrateKbps(): Int {
        return when (appSettings.streamingQuality.value.uppercase()) {
            "LOW" -> 96
            "NORMAL" -> 160
            "HIGH" -> 320
            "LOSSLESS" -> 0
            else -> 320
        }
    }
    
    /**
     * Validate if the requested quality is supported by the provider.
     * Some providers may not support all quality levels.
     */
    private fun isSupportedQuality(quality: String): Boolean {
        // All major providers (Subsonic/Navidrome, Jellyfin) support these quality levels
        return quality.uppercase() in listOf("LOW", "NORMAL", "HIGH", "LOSSLESS")
    }
    
    /**
     * Get the fallback quality if the requested one is not supported.
     */
    private fun getFallbackQuality(unsupportedQuality: String): String {
        return when (unsupportedQuality.uppercase()) {
            else -> "HIGH" // Default fallback
        }
    }
    private fun resolveCookieInput(username: String, password: String): String {
        return when {
            looksLikeCookieBlob(password) -> password
            looksLikeCookieBlob(username) -> username
            else -> password
        }
    }

    private fun looksLikeCookieBlob(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.startsWith("{") || trimmed.contains('=')
    }

    private fun serviceToSourceType(serviceId: String): SourceType {
        return when (serviceId) {
            StreamingServiceId.SUBSONIC -> SourceType.SUBSONIC
            StreamingServiceId.JELLYFIN -> SourceType.JELLYFIN
            else -> SourceType.UNKNOWN
        }
    }

    private fun normalizeServiceId(serviceId: String): String {
        val normalized = serviceId.uppercase()
        return if (StreamingServiceId.all.contains(normalized)) {
            normalized
        } else {
            StreamingServiceId.SUBSONIC
        }
    }

    /**
     * Enrich streaming artists with artwork from Deezer API
     * Call this when loading artists to fill in missing artwork
     */
    suspend fun enrichArtistsWithDeezerImages(artists: List<StreamingArtist>): List<StreamingArtist> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Deezer API is available
                val deezerService = NetworkClient.deezerApiService
                if (deezerService == null) {
                    Log.d("StreamingMusicRepo", "Deezer API service not available, skipping enrichment")
                    return@withContext artists // Return unchanged if API not available
                }

                val enriched = mutableListOf<StreamingArtist>()

                for (artist in artists) {
                    try {
                        // Skip unknown/blank artists
                        if (artist.name.isBlank() || artist.name.equals("Unknown", ignoreCase = true)) {
                            enriched.add(artist)
                            continue
                        }

                        // Always try Deezer enrichment, even if artist has existing artwork
                        // This ensures we prefer actual artist images over album art
                        var enrichedArtist = artist
                        try {
                            // Search for artist on Deezer
                            val searchResponse = deezerService.searchArtists(artist.name, limit = 5)
                            val deezerArtist = searchResponse.data.firstOrNull { 
                                it.name.equals(artist.name, ignoreCase = true)
                            } ?: searchResponse.data.firstOrNull() // Fallback to best match

                            if (deezerArtist != null) {
                                // Choose best quality image available
                                val imageUrl = when {
                                    !deezerArtist.pictureXl.isNullOrEmpty() -> deezerArtist.pictureXl
                                    !deezerArtist.pictureBig.isNullOrEmpty() -> deezerArtist.pictureBig
                                    !deezerArtist.pictureMedium.isNullOrEmpty() -> deezerArtist.pictureMedium
                                    !deezerArtist.picture.isNullOrEmpty() -> deezerArtist.picture
                                    else -> null
                                }

                                if (!imageUrl.isNullOrEmpty()) {
                                    Log.d("StreamingMusicRepo", "Found Deezer image for ${artist.name}")
                                    artistArtworkCache[normalizeKey(artist.name)] = imageUrl
                                    enrichedArtist = artist.copy(artworkUri = imageUrl)
                                } else {
                                    Log.d("StreamingMusicRepo", "Deezer artist found but no image: ${deezerArtist.name}")
                                }
                            } else {
                                Log.d("StreamingMusicRepo", "No Deezer artist found for: ${artist.name}")
                            }
                        } catch (e: Exception) {
                            Log.w("StreamingMusicRepo", "Failed to fetch Deezer image for ${artist.name}: ${e.message}", e)
                        }

                        // Cache the final artwork (Deezer or original)
                        enrichedArtist.artworkUri?.takeIf { it.isNotBlank() }?.let { cachedUri ->
                            artistArtworkCache[normalizeKey(artist.name)] = cachedUri
                        }
                        enriched.add(enrichedArtist)
                    } catch (e: Exception) {
                        Log.w("StreamingMusicRepo", "Failed to process artist ${artist.name}: ${e.message}", e)
                        enriched.add(artist)
                    }
                }

                Log.d("StreamingMusicRepo", "Enriched ${enriched.count { it.artworkUri != null }} of ${artists.size} artists with Deezer images")
                enriched
            } catch (e: Exception) {
                Log.e("StreamingMusicRepo", "Error enriching artists with Deezer images", e)
                artists // Return unchanged on error
            }
        }
    }

    /**
     * Update the repository's artistsFlow with enriched artists.
     * This ensures enriched artwork persists across recompositions and navigations.
     */
    fun updateArtistsFlow(enrichedArtists: List<StreamingArtist>) {
        artistsFlow.value = enrichedArtists
    }

    private companion object {
        private const val SEARCH_LIMIT = 100
        private const val MAX_CACHE_SIZE = 4000
    }
}
