/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.playback

import androidx.media3.common.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages A/B player alternation for crossfade.
 * 
 * Ensures only one player is "active" (producing sound) at a time.
 * The other player prepares the next track independently.
 * Only after the incoming player is ready do we swap roles.
 */
internal class CrossfadeCoordinator(
    private val playerA: ExoPlayer,
    private val playerB: ExoPlayer,
) {
    private val mutex = Mutex()
    
    private val _activePlayerRole = MutableStateFlow(PlayerRole.A)
    val activePlayerRole: StateFlow<PlayerRole> = _activePlayerRole.asStateFlow()
    
    val activePlayer: ExoPlayer
        get() = when (_activePlayerRole.value) {
            PlayerRole.A -> playerA
            PlayerRole.B -> playerB
        }
    
    val standbyPlayer: ExoPlayer
        get() = when (_activePlayerRole.value) {
            PlayerRole.A -> playerB
            PlayerRole.B -> playerA
        }
    
    enum class PlayerRole {
        A, B
    }
    
    suspend fun swapRoles() = mutex.withLock {
        _activePlayerRole.value = when (_activePlayerRole.value) {
            PlayerRole.A -> PlayerRole.B
            PlayerRole.B -> PlayerRole.A
        }
    }
    
    /**
     * Prepares standby player for incoming media.
     * Does NOT touch active player.
     */
    suspend fun prepareStandbyForMedia(mediaItem: MediaItem) = mutex.withLock {
        standbyPlayer.apply {
            setMediaItem(mediaItem)
            prepare()
        }
    }
    
    /**
     * Waits for standby player to reach ready state.
     * Returns true if ready before timeout, false if timeout.
     */
    suspend fun waitForStandbyReady(timeoutMs: Long): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val playbackState = standbyPlayer.playbackState
            if (playbackState == Player.STATE_READY) return true
            if (playbackState == Player.STATE_IDLE) return false // Won't become ready
            kotlinx.coroutines.delay(10)
        }
        return false
    }
    
    /**
     * After successful handoff, parks the old active player.
     */
    suspend fun parkOldActivePlayer() = mutex.withLock {
        val oldActive = when (_activePlayerRole.value) {
            PlayerRole.A -> playerB // We just swapped, so B is now active
            PlayerRole.B -> playerA
        }
        oldActive.apply {
            stop()
            clearMediaItems()
        }
    }
}
