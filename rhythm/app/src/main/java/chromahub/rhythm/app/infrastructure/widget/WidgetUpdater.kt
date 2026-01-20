package chromahub.rhythm.app.widget

import android.content.Context
import android.net.Uri
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.infrastructure.widget.glance.GlanceWidgetUpdater

object WidgetUpdater {
    
    fun updateWidget(
        context: Context,
        song: Song?,
        isPlaying: Boolean,
        hasPrevious: Boolean = false,
        hasNext: Boolean = false
    ) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        if (song != null) {
            editor.putString("song_title", song.title)
            editor.putString("artist_name", song.artist)
            editor.putString("album_name", song.album)
            editor.putString("artwork_uri", song.artworkUri?.toString())
        } else {
            editor.putString("song_title", "No song playing")
            editor.putString("artist_name", "Unknown artist")
            editor.putString("album_name", "")
            editor.remove("artwork_uri")
        }
        
        editor.putBoolean("is_playing", isPlaying)
        editor.apply() // Use apply for async write to prevent ANR
        
        // Update legacy RemoteViews widget
        MusicWidgetProvider.updateWidgets(context)
        
        // Update modern Glance widget
        GlanceWidgetUpdater.updateWidget(context, song, isPlaying, hasPrevious, hasNext)
    }
    
    fun clearWidget(context: Context) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        // Update legacy RemoteViews widget
        MusicWidgetProvider.updateWidgets(context)
        
        // Update modern Glance widget
        GlanceWidgetUpdater.updateWidgetEmpty(context)
    }
}
