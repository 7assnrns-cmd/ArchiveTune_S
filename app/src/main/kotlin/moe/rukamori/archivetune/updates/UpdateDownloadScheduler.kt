/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.updates

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

object UpdateDownloadScheduler {
    const val WORK_NAME = "update_download_work"

    fun schedule(
        context: Context,
        url: String,
        version: String,
        wifiOnly: Boolean,
    ) {
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED,
                ).setRequiresStorageNotLow(true)
                .build()

        val request =
            OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
                .setInputData(
                    workDataOf(
                        UpdateDownloadWorker.KEY_URL to url,
                        UpdateDownloadWorker.KEY_VERSION to version,
                    ),
                ).setConstraints(constraints)
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
