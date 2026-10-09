/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.updates

import android.content.Context
import java.io.File

/**
 * Persistent storage for a downloaded update APK.
 *
 * Files live under filesDir/updates/, which is covered by the existing
 * FileProvider paths entry, so the APK can be handed to the package
 * installer without any additional manifest changes.
 */
object UpdateApkStorage {
    private const val DirectoryName = "updates"
    private const val ApkFileName = "archivetune-update.apk"

    fun directory(context: Context): File =
        File(context.filesDir, DirectoryName).apply { mkdirs() }

    fun apkFile(context: Context): File = File(directory(context), ApkFileName)

    fun hasPendingApk(context: Context): Boolean = apkFile(context).let { it.isFile && it.length() > 0L }

    fun deletePendingApk(context: Context) {
        apkFile(context).takeIf { it.exists() }?.delete()
    }

    /**
     * Remove any stale APKs left in the legacy cacheDir/app_update path
     * used by older versions of AppUpdateInstaller.
     */
    fun clearLegacyCache(context: Context) {
        runCatching {
            File(context.cacheDir, "app_update").takeIf { it.exists() }?.deleteRecursively()
        }
    }
}
