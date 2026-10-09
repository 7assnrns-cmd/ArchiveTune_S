/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import moe.rukamori.archivetune.BuildConfig
import moe.rukamori.archivetune.constants.AutoDownloadUpdatesKey
import moe.rukamori.archivetune.constants.AutomaticUpdateCheckKey
import moe.rukamori.archivetune.constants.LastUpdateCheckKey
import moe.rukamori.archivetune.constants.UpdatesWifiOnlyKey
import moe.rukamori.archivetune.updates.UpdateDownloadScheduler
import moe.rukamori.archivetune.constants.UpdateChannel
import moe.rukamori.archivetune.constants.UpdateChannelKey
import moe.rukamori.archivetune.defaultUpdateChannel

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!BuildConfig.UPDATER_AVAILABLE) {
            return Result.success()
        }

        return try {
            val dataStore = applicationContext.dataStore

            val preferences = dataStore.data.first()
            val automaticChecksEnabled = preferences[AutomaticUpdateCheckKey] ?: true
            if (!automaticChecksEnabled) return Result.success()

            // Rate limit: skip if we ran within the last 6 hours. The
            // immediate worker (scheduleImmediate) can fire on every launch,
            // so this check keeps that cheap.
            val lastCheck = preferences[LastUpdateCheckKey] ?: 0L
            val now = System.currentTimeMillis()
            if (now - lastCheck < CHECK_INTERVAL_MS) return Result.success()
            dataStore.edit { it[LastUpdateCheckKey] = now }

            val updateChannel =
                UpdateChannel.fromStoredName(preferences[UpdateChannelKey], defaultUpdateChannel)

            val latestVersion =
                when (updateChannel) {
                    UpdateChannel.MY_VERSION -> Updater.getLatestVersionName()
                    UpdateChannel.OFFICIAL_VERSION -> Updater.getLatestVersionName()
                }.getOrElse { throw it }

            if (Updater.isUpdateAvailable(latestVersion, BuildConfig.VERSION_NAME)) {
                UpdateNotificationManager.notifyIfNewVersion(
                    applicationContext,
                    latestVersion,
                    updateChannel,
                )

                val autoDownload = preferences[AutoDownloadUpdatesKey] ?: false
                if (autoDownload) {
                    val wifiOnly = preferences[UpdatesWifiOnlyKey] ?: true
                    val downloadUrl =
                        when (updateChannel) {
                            UpdateChannel.MY_VERSION -> Updater.getLatestDownloadUrl()
                            UpdateChannel.OFFICIAL_VERSION -> Updater.getOfficialLatestDownloadUrl()
                        }
                    if (downloadUrl.isNotBlank()) {
                        UpdateDownloadScheduler.schedule(
                            applicationContext,
                            downloadUrl,
                            latestVersion,
                            wifiOnly,
                        )
                    }
                }
            }

            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            reportException(exception)
            Result.retry()
        }
    }
}
