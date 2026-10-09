/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.updates

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles the "Cancel" action on the update download notification.
 * Triggered by a PendingIntent.getBroadcast; the receiver cancels the
 * background worker and removes the notification.
 */
class UpdateDownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CANCEL) return
        UpdateDownloadScheduler.cancel(context.applicationContext)
        UpdateDownloadNotifier(context.applicationContext).cancel()
    }

    companion object {
        const val ACTION_CANCEL = "moe.rukamori.archivetune.action.CANCEL_UPDATE_DOWNLOAD"
    }
}
