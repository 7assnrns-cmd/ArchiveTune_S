/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.updates

/**
 * One APK attached to a GitHub release.
 *
 * [architecture] is the flavor suffix used in the APK filename
 * ("universal", "arm64", "armeabi", "x86_64", "x86").
 * [displayName] is what the picker shows to the user
 * ("arm64-v8a", "armeabi-v7a", "Universal (all devices)", ...).
 */
data class ApkAsset(
    val architecture: String,
    val displayName: String,
    val url: String,
    val sizeBytes: Long,
)
