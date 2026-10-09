/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.client.request.headers
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.BuildConfig
import moe.rukamori.archivetune.updates.UpdateApkStorage
import okhttp3.ConnectionPool
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

object AppUpdateInstaller {
    data class Progress(
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) {
        val fraction: Float?
            get() =
                totalBytes
                    .takeIf { it > 0L }
                    ?.let { downloadedBytes.toFloat() / it.toFloat() }
                    ?.coerceIn(0f, 1f)
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(60, TimeUnit.SECONDS)
                    connectionPool(ConnectionPool(2, 30, TimeUnit.SECONDS))
                    retryOnConnectionFailure(true)
                    followRedirects(true)
                }
            }
        }
    }

    suspend fun downloadAndInstall(
        context: Context,
        url: String,
        onProgress: (Progress) -> Unit,
    ): Result<Unit> {
        if (BuildConfig.DISTRIBUTION != "gms") {
            return Result.failure(IllegalStateException("In-app updates are only available for GMS builds"))
        }

        return try {
            val apkFile =
                withContext(Dispatchers.IO) {
                    downloadApk(context.applicationContext, url, onProgress)
                }
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Progress) -> Unit,
    ): File {
        if (url.isBlank()) {
            throw IOException("Update download URL is empty")
        }

        UpdateApkStorage.clearLegacyCache(context)
        val targetApk = UpdateApkStorage.apkFile(context)
        val downloadedFile = UpdateApkStorage.tmpFile(context)

        // Resume: only valid when the sidecar URL matches. A different URL
        // means a different version or ABI, so we start from scratch.
        val storedUrl = UpdateApkStorage.readTmpUrl(context)
        val existingBytes =
            if (downloadedFile.isFile && downloadedFile.length() > 0L && storedUrl == url) {
                downloadedFile.length()
            } else {
                downloadedFile.takeIf { it.exists() }?.delete()
                targetApk.takeIf { it.exists() }?.delete()
                UpdateApkStorage.writeTmpUrl(context, url)
                0L
            }

        client.prepareGet(url) {
            if (existingBytes > 0L) {
                headers.append(HttpHeaders.Range, "bytes=$existingBytes-")
            }
        }.execute { response ->
            val responseCode = response.status.value

            // 206 = Partial Content (resume worked)
            // 200 = OK (server ignored Range; must restart from 0)
            // 416 = Range Not Satisfiable (local file is already at/beyond
            //       the remote size; assume it is complete)
            when (responseCode) {
                206, 200 -> { /* continue */ }
                416 -> {
                    emitProgress(existingBytes, existingBytes, onProgress)
                    return@execute
                }
                else -> throw IOException("Update download failed: HTTP $responseCode")
            }

            val append = responseCode == 206
            val contentLength = response.contentLength() ?: -1L
            val totalBytes =
                if (append && contentLength >= 0L) existingBytes + contentLength else contentLength

            if (!append) {
                // Server sent a full body; restart.
                downloadedFile.takeIf { it.exists() }?.delete()
            }

            val channel = response.bodyAsChannel()
            FileOutputStream(downloadedFile, append).use { output ->
                val buffer = ByteArray(STREAM_BUFFER_SIZE)
                var downloadedBytes = if (append) existingBytes else 0L
                var lastUpdateMs = 0L
                while (!channel.isClosedForRead) {
                    currentCoroutineContext().ensureActive()
                    val read = channel.readAvailable(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloadedBytes += read.toLong()
                    val now = System.currentTimeMillis()
                    if (now - lastUpdateMs >= PROGRESS_UPDATE_INTERVAL_MS) {
                        emitProgress(downloadedBytes, totalBytes, onProgress)
                        lastUpdateMs = now
                    }
                }
                emitProgress(downloadedBytes, totalBytes, onProgress)
            }
        }

        val resultFile =
            if (url.lowercase(Locale.US).substringBefore('?').endsWith(".apk")) {
                downloadedFile
            } else {
                extractGmsApk(downloadedFile, targetApk)
                    ?: if (downloadedFile.containsApkManifest()) {
                        downloadedFile
                    } else {
                        throw IOException("No GMS APK found in update artifact")
                    }
            }
        // Finalise: move the completed file to UpdateApkStorage.apkFile().
        // renameTo is atomic within the same filesystem; fall back to copy
        // if the rename is refused (rare, but possible on some OEM FS).
        if (resultFile != targetApk) {
            if (!resultFile.renameTo(targetApk)) {
                resultFile.copyTo(targetApk, overwrite = true)
                resultFile.delete()
            }
        }
        return targetApk
    }

    private suspend fun emitProgress(
        downloadedBytes: Long,
        totalBytes: Long,
        onProgress: (Progress) -> Unit,
    ) {
        withContext(Dispatchers.Main.immediate) {
            onProgress(Progress(downloadedBytes, totalBytes))
        }
    }

    private fun extractGmsApk(
        sourceFile: File,
        targetFile: File,
    ): File? =
        runCatching {
            ZipFile(sourceFile).use { zip ->
                val entries =
                    zip.entries().asSequence().filter { entry ->
                        val fileName = entry.name.substringAfterLast('/')
                        !entry.isDirectory &&
                            entry.name.endsWith(".apk", ignoreCase = true) &&
                            !fileName.contains("foss-", ignoreCase = true) &&
                            !fileName.contains("izzy-", ignoreCase = true)
                    }
                val preferredArtifactNames =
                    listOf(
                        "app-gms-${BuildConfig.DEVICE}-${BuildConfig.ARCHITECTURE}-",
                        "app-${BuildConfig.DEVICE}-${BuildConfig.ARCHITECTURE}-",
                    )
                val selectedEntry =
                    entries
                        .sortedBy { entry ->
                            val fileName = entry.name.substringAfterLast('/')
                            val preferredIndex =
                                preferredArtifactNames.indexOfFirst { preferredArtifactName ->
                                    fileName.contains(preferredArtifactName, ignoreCase = true)
                                }
                            if (preferredIndex >= 0) preferredIndex else preferredArtifactNames.size
                        }.firstOrNull()
                        ?: return@runCatching null

                zip.getInputStream(selectedEntry).use { input ->
                    targetFile.outputStream().use { output -> input.copyTo(output) }
                }
                targetFile
            }
        }.getOrNull()

    private fun File.containsApkManifest(): Boolean =
        runCatching {
            ZipFile(this).use { zip -> zip.getEntry("AndroidManifest.xml") != null }
        }.getOrDefault(false)

    private fun File.renameAsApk(): File {
        val apkFile = File(parentFile, ApkFileName)
        if (this == apkFile) return this
        if (!renameTo(apkFile)) {
            copyTo(apkFile, overwrite = true)
            delete()
        }
        return apkFile
    }

    private fun installApk(
        context: Context,
        apkFile: File,
    ) {
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                apkFile,
            )
        val intent =
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, ApkMimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    suspend fun downloadOnly(
        context: Context,
        url: String,
        onProgress: (Progress) -> Unit,
    ): Result<File> =
        try {
            val apk =
                withContext(Dispatchers.IO) {
                    downloadApk(context.applicationContext, url, onProgress)
                }
            Result.success(apk)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }

    suspend fun installDownloaded(context: Context): Result<Unit> =
        try {
            val apk = UpdateApkStorage.apkFile(context.applicationContext)
            if (!apk.isFile || apk.length() == 0L) {
                Result.failure(IOException("No downloaded APK"))
            } else if (apk.length() < MIN_APK_SIZE_BYTES) {
                // A partially-downloaded APK would fail signature
                // verification at install time. Reject it early.
                Result.failure(IOException("Downloaded APK looks truncated (${apk.length()} bytes)"))
            } else {
                withContext(Dispatchers.Main) { installApk(context, apk) }
                Result.success(Unit)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }

    private const val UpdateDirectoryName = "app_update"
    private const val DownloadFileName = "archive-tune-update.download"
    private const val ApkFileName = "archive-tune-update.apk"
    private const val ApkMimeType = "application/vnd.android.package-archive"
    private const val STREAM_BUFFER_SIZE = 256 * 1024
    private const val PROGRESS_UPDATE_INTERVAL_MS = 200L
    private const val MIN_APK_SIZE_BYTES = 1_000_000L
}
