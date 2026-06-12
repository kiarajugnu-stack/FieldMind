package fieldmind.research.app.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Service interface for Rhythm lyrics API
 * API Documentation: https://lyrics.paxsenix.org/docs
 */
interface RhythmLyricsApiService {
    /**
     * Get word-by-word synchronized lyrics for a specific song
     * @param id Lyrics source song ID
     * @return Lyrics response with word-level timing
     */
    @GET("apple-music/lyrics")
    suspend fun getLyrics(
        @Query("id") id: String
    ): RhythmLyricsResponse
}
