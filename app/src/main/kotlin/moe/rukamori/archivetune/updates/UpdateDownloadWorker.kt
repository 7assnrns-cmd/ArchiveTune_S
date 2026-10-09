/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.updates

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import moe.rukamori.archivetune.constants.UpdatesDownloadedApkVersionKey
import moe.rukamori.archivetune.utils.AppUpdateInstaller
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.reportException

class UpdateDownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val version = inputData.getString(KEY_VERSION) ?: return Result.failure()
        val notifier = UpdateDownloadNotifier(applicationContext)
        notifier.createChannel()
        notifier.showProgress(null)

        return try {
            val result =
                AppUpdateInstaller.downloadOnly(applicationContext, url) { progress ->
                    notifier.showProgress(progress.fraction)
                }
            val apkFile = result.getOrNull()
            if (result.isSuccess && apkFile != null) {
                applicationContext.dataStore.edit { prefs ->
                    prefs[UpdatesDownloadedApkVersionKey] = version
                }
                notifier.showComplete(apkFile)
                Result.success()
            } else {
                notifier.showFailure(result.exceptionOrNull()?.message)
                Result.retry()
            }
        } catch (e: CancellationException) {
            notifier.cancel()
            throw e
        } catch (e: Exception) {
            reportException(e)
            notifier.showFailure(e.message)
            Result.retry()
        }
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_VERSION = "version"
    }
}
