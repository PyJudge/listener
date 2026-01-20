package chromahub.rhythm.app.infrastructure.widget.glance

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import chromahub.rhythm.app.infrastructure.service.MediaPlaybackService
import kotlinx.coroutines.delay

/**
 * Play/Pause action callback for widget
 */
class PlayPauseAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_PLAY_PAUSE
        }
        context.startService(intent)
        
        // Trigger immediate widget update after short delay for state change
        delay(100)
        RhythmMusicWidget().updateAll(context)
    }
}

/**
 * Skip to next track action callback for widget
 */
class SkipNextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_SKIP_NEXT
        }
        context.startService(intent)
        
        // Trigger immediate widget update after short delay for state change
        delay(100)
        RhythmMusicWidget().updateAll(context)
    }
}

/**
 * Skip to previous track action callback for widget
 */
class SkipPreviousAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_SKIP_PREVIOUS
        }
        context.startService(intent)
        
        // Trigger immediate widget update after short delay for state change
        delay(100)
        RhythmMusicWidget().updateAll(context)
    }
}
