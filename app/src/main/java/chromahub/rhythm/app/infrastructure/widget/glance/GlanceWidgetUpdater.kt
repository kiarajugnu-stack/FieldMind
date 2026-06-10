package chromahub.rhythm.app.infrastructure.widget.glance

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import chromahub.rhythm.app.shared.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.withContext

/**
 * Utility object for updating the Glance-based widget
 * 
 * This handles updating widget state when playback changes
 */
object GlanceWidgetUpdater {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    /**
     * Update widget with current playback state
     */
    fun updateWidget(
        context: Context,
        song: Song?,
        isPlaying: Boolean,
        hasPrevious: Boolean = false,
        hasNext: Boolean = false,
        isFavorite: Boolean = false
    ) {
        // Update SharedPreferences for legacy widget
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (song != null) {
                putString(RhythmMusicWidget.KEY_SONG_ID, song.id)
                putString(RhythmMusicWidget.KEY_SONG_TITLE, song.title)
                putString(RhythmMusicWidget.KEY_ARTIST_NAME, song.artist)
                putString(RhythmMusicWidget.KEY_ALBUM_NAME, song.album)
                putString(RhythmMusicWidget.KEY_ARTWORK_URI, song.artworkUri?.toString())
            } else {
                putString(RhythmMusicWidget.KEY_SONG_ID, "")
                putString(RhythmMusicWidget.KEY_SONG_TITLE, "Rhythm")
                putString(RhythmMusicWidget.KEY_ARTIST_NAME, "")
                putString(RhythmMusicWidget.KEY_ALBUM_NAME, "")
                remove(RhythmMusicWidget.KEY_ARTWORK_URI)
            }
            putBoolean(RhythmMusicWidget.KEY_IS_PLAYING, isPlaying)
            putBoolean(RhythmMusicWidget.KEY_HAS_PREVIOUS, hasPrevious)
            putBoolean(RhythmMusicWidget.KEY_HAS_NEXT, hasNext)
            putBoolean(RhythmMusicWidget.KEY_IS_FAVORITE, isFavorite)
            apply() // Use apply for async write
        }
        
        // Update Glance widget state directly using Glance state system
        scope.launch {
            try {
                // Preload bitmap in background if artworkUri exists
                val artworkUri = song?.artworkUri?.toString()
                if (!artworkUri.isNullOrBlank()) {
                    try {
                        withContext(Dispatchers.IO) {
                            val imageLoader = ImageLoader(context)
                            val request = ImageRequest.Builder(context)
                                .data(artworkUri)
                                .size(Size(150, 150))
                                .build()
                            val result = imageLoader.execute(request)
                            val loaded = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                            if (loaded != null) {
                                RhythmMusicWidget.cacheBitmap(artworkUri, loaded)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GlanceWidgetUpdater", "Error preloading bitmap in updater", e)
                    }
                }

                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(RhythmMusicWidget::class.java)
                
                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        if (song != null) {
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_SONG_ID)] = song.id
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_SONG_TITLE)] = song.title
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ARTIST_NAME)] = song.artist
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ALBUM_NAME)] = song.album
                            song.artworkUri?.let {
                                prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ARTWORK_URI)] = it.toString()
                            }
                        } else {
                            prefs.remove(stringPreferencesKey(RhythmMusicWidget.KEY_SONG_ID))
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_SONG_TITLE)] = "Rhythm"
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ARTIST_NAME)] = ""
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ALBUM_NAME)] = ""
                            prefs.remove(stringPreferencesKey(RhythmMusicWidget.KEY_ARTWORK_URI))
                        }
                        prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_IS_PLAYING)] = isPlaying
                        prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_HAS_PREVIOUS)] = hasPrevious
                        prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_HAS_NEXT)] = hasNext
                        prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_IS_FAVORITE)] = isFavorite
                    }
                }
                
                // Update RhythmLyricsWidget as well
                val lyricGlanceIds = manager.getGlanceIds(RhythmLyricsWidget::class.java)
                lyricGlanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        if (song != null) {
                            prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_SONG_TITLE)] = song.title
                            prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_ARTIST_NAME)] = song.artist
                            song.artworkUri?.let {
                                prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_ARTWORK_URI)] = it.toString()
                            } ?: prefs.remove(stringPreferencesKey(RhythmLyricsWidget.KEY_ARTWORK_URI))
                        } else {
                            prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_SONG_TITLE)] = "Rhythm"
                            prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_ARTIST_NAME)] = ""
                            prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_LYRIC_LINES)] = ""
                            prefs[intPreferencesKey(RhythmLyricsWidget.KEY_ACTIVE_INDEX)] = -1
                            prefs.remove(stringPreferencesKey(RhythmLyricsWidget.KEY_ARTWORK_URI))
                        }
                        prefs[booleanPreferencesKey(RhythmLyricsWidget.KEY_IS_PLAYING)] = isPlaying
                    }
                }
                
                // Force update all widgets
                try { RhythmMusicWidget().updateAll(context) } catch (_: Exception) {}
                try { RhythmLyricsWidget().updateAll(context) } catch (_: Exception) {}
            } catch (e: Exception) {
                android.util.Log.e("GlanceWidgetUpdater", "Error updating widget", e)
            }
        }
    }
    
    /**
     * Update widget to show "No song playing" state
     */
    fun updateWidgetEmpty(context: Context) {
        updateWidget(
            context = context,
            song = null,
            isPlaying = false,
            hasPrevious = false,
            hasNext = false
        )
    }
    
    /**
     * Force update all widgets
     */
    fun forceUpdateAll(context: Context) {
        scope.launch {
            try {
                RhythmMusicWidget().updateAll(context)
            } catch (e: Exception) {
                android.util.Log.e("GlanceWidgetUpdater", "Error forcing widget update", e)
            }
            try {
                RhythmLyricsWidget().updateAll(context)
            } catch (e: Exception) {
                android.util.Log.e("GlanceWidgetUpdater", "Error forcing lyrics widget update", e)
            }
        }
        
        // Also trigger worker update
        scheduleWidgetUpdate(context, delayMillis = 0)
    }
    
    /**
     * Schedule a widget update using WorkManager for reliability
     */
    private fun scheduleWidgetUpdate(context: Context, delayMillis: Long = 0) {
        try {
            val updateRequest = OneTimeWorkRequestBuilder<RhythmWidgetWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()
            
            WorkManager.getInstance(context).enqueue(updateRequest)
        } catch (e: Exception) {
            android.util.Log.e("GlanceWidgetUpdater", "Error scheduling widget update", e)
        }
    }
    
    /**
     * Update lyrics widget with dynamic lyric lines and active index
     */
    fun updateLyrics(
        context: Context,
        lyricTexts: List<String>,
        activeIndex: Int
    ) {
        scope.launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(RhythmLyricsWidget::class.java)
                if (glanceIds.isEmpty()) return@launch
                
                val joined = lyricTexts.joinToString("##LINE##")
                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_LYRIC_LINES)] = joined
                        prefs[intPreferencesKey(RhythmLyricsWidget.KEY_ACTIVE_INDEX)] = activeIndex
                    }
                }
                try { RhythmLyricsWidget().updateAll(context) } catch (_: Exception) {}
            } catch (e: Exception) {
                android.util.Log.e("GlanceWidgetUpdater", "Error updating lyrics widget", e)
            }
        }
    }
}
