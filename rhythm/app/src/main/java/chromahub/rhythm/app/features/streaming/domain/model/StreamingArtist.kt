package chromahub.rhythm.app.features.streaming.domain.model

import chromahub.rhythm.app.core.domain.model.AlbumItem
import chromahub.rhythm.app.core.domain.model.ArtistItem
import chromahub.rhythm.app.core.domain.model.PlayableItem
import chromahub.rhythm.app.core.domain.model.SourceType

/**
 * Represents an artist from a streaming service.
 */
data class StreamingArtist(
    override val id: String,
    override val name: String,
    override val artworkUri: String?,
    override val songCount: Int,
    override val albumCount: Int,
    override val sourceType: SourceType,
    val externalId: String? = null,
    val genres: List<String> = emptyList(),
    val followers: Long? = null,
    val popularity: Int? = null,
    val bio: String? = null,
    private val topTracks: List<StreamingSong> = emptyList(),
    private val albums: List<StreamingAlbum> = emptyList()
) : ArtistItem {
    
    override suspend fun getSongs(): List<PlayableItem> = topTracks
    
    override suspend fun getAlbums(): List<AlbumItem> = albums
    
    /**
     * Get top tracks if already loaded.
     */
    fun getTopTracks(): List<StreamingSong> = topTracks
    
    /**
     * Get albums list if already loaded.
     */
    fun getAlbumsList(): List<StreamingAlbum> = albums
}
