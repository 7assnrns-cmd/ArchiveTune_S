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

    /** Temporary path used while a download is in flight. */
    fun tmpFile(context: Context): File = File(directory(context), "archivetune-update.download")

    fun hasPendingApk(context: Context): Boolean = apkFile(context).let { it.isFile && it.length() > 0L }

    /**
     * Sidecar file that records which URL the in-flight .download file
     * belongs to. Resume is only valid when the URL matches; a different
     * version or ABI must start a fresh download.
     */
    fun tmpUrlFile(context: Context): File = File(directory(context), "archivetune-update.url")

    fun readTmpUrl(context: Context): String? =
        tmpUrlFile(context).takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotBlank() }

    fun writeTmpUrl(context: Context, url: String) {
        runCatching { tmpUrlFile(context).writeText(url) }
    }

    fun deleteTmpUrl(context: Context) {
        runCatching { tmpUrlFile(context).takeIf { it.exists() }?.delete() }
    }

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
