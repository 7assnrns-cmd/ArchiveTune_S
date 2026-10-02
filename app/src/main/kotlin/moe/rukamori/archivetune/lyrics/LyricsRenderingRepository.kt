/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.lyrics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.constants.LyricsClickKey
import moe.rukamori.archivetune.constants.LyricsLineBlurKey
import moe.rukamori.archivetune.constants.LyricsLineSpacingKey
import moe.rukamori.archivetune.constants.LyricsRomanizeChineseKey
import moe.rukamori.archivetune.constants.LyricsRomanizeHindiKey
import moe.rukamori.archivetune.constants.LyricsRomanizeJapaneseKey
import moe.rukamori.archivetune.constants.LyricsRomanizeKoreanKey
import moe.rukamori.archivetune.constants.LyricsRomanizeOtherLanguagesKey
import moe.rukamori.archivetune.constants.LyricsScrollKey
import moe.rukamori.archivetune.constants.LyricsTextSizeKey
import moe.rukamori.archivetune.constants.LyricsV2BounceFactorKey
import moe.rukamori.archivetune.constants.LyricsV2FillTransitionWidthKey
import moe.rukamori.archivetune.constants.LyricsV2GlowFactorKey
import moe.rukamori.archivetune.constants.LyricsV2LrcBounceEnabledKey
import moe.rukamori.archivetune.constants.LyricsEnhancedAccompanimentScaleKey
import moe.rukamori.archivetune.constants.LyricsEnhancedFontWeightKey
import moe.rukamori.archivetune.constants.LyricsEnhancedLineSpacingKey
import moe.rukamori.archivetune.constants.LyricsEnhancedPhoneticScaleKey
import moe.rukamori.archivetune.constants.LyricsInactiveLineAlphaKey
import moe.rukamori.archivetune.constants.LyricsKeepAliveZoneDpKey
import moe.rukamori.archivetune.constants.LyricsPhoneticOverrideKey
import moe.rukamori.archivetune.constants.LyricsSelectionLimitKey
import moe.rukamori.archivetune.constants.LyricsSmoothPlaybackKey
import moe.rukamori.archivetune.constants.LyricsTextColorCustomKey
import moe.rukamori.archivetune.constants.LyricsTextColorModeKey
import moe.rukamori.archivetune.constants.LyricsTextContrastGuardKey
import moe.rukamori.archivetune.constants.LyricsTranslationOverrideKey
import moe.rukamori.archivetune.constants.LyricsViewportOffsetFractionKey
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.utils.dataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRenderingRepository
    @Inject
    constructor(
        private val database: MusicDatabase,
        @ApplicationContext context: Context,
    ) {
        private val preferences = context.dataStore.data

        fun observeLyrics(mediaId: String): Flow<String?> =
            database
                .lyrics(mediaId)
                .map { entity -> entity?.lyrics }
                .distinctUntilChanged()

        fun observePreferences(): Flow<LyricsRenderingPreferences> =
            preferences
                .map { values ->
                    LyricsRenderingPreferences(
                        clickEnabled = values[LyricsClickKey] ?: true,
                        scrollEnabled = values[LyricsScrollKey] ?: true,
                        textSizeSp = values[LyricsTextSizeKey] ?: 26f,
                        lineSpacing = values[LyricsLineSpacingKey] ?: 1.3f,
                        lineBlurEnabled = values[LyricsLineBlurKey] ?: true,
                        v2BounceFactor = values[LyricsV2BounceFactorKey] ?: 1f,
                        v2GlowFactor = values[LyricsV2GlowFactorKey] ?: 1f,
                        v2FillTransitionWidthDp = values[LyricsV2FillTransitionWidthKey] ?: 8f,
                        v2LrcBounceEnabled = values[LyricsV2LrcBounceEnabledKey] ?: true,
                        romanization =
                            LyricsRomanizationPreferences(
                                romanizeJapanese = values[LyricsRomanizeJapaneseKey] ?: true,
                                romanizeKorean = values[LyricsRomanizeKoreanKey] ?: true,
                                romanizeChinese = values[LyricsRomanizeChineseKey] ?: true,
                                romanizeHindi = values[LyricsRomanizeHindiKey] ?: true,
                                romanizeOther = values[LyricsRomanizeOtherLanguagesKey] ?: true,
                            ),
                        enhancedAccompanimentScale =
                            (values[LyricsEnhancedAccompanimentScaleKey] ?: 0.82f).coerceIn(0.5f, 1f),
                        enhancedPhoneticScale =
                            (values[LyricsEnhancedPhoneticScaleKey] ?: 0.55f).coerceIn(0.3f, 0.8f),
                        enhancedLineSpacing =
                            (values[LyricsEnhancedLineSpacingKey] ?: 1.3f).coerceIn(0.8f, 2f),
                        enhancedFontWeight =
                            parseEnumOrDefault(values[LyricsEnhancedFontWeightKey], LyricsEnhancedFontWeight.BOLD),
                        viewportOffsetFraction =
                            (values[LyricsViewportOffsetFractionKey] ?: 0.38f).coerceIn(0.2f, 0.5f),
                        keepAliveZoneDp =
                            (values[LyricsKeepAliveZoneDpKey] ?: 72).coerceIn(0, 200),
                        selectionLimit =
                            (values[LyricsSelectionLimitKey] ?: 5).coerceIn(1, 20),
                        translationOverride =
                            parseEnumOrDefault(values[LyricsTranslationOverrideKey], LyricsVisibilityOverride.AUTO),
                        phoneticOverride =
                            parseEnumOrDefault(values[LyricsPhoneticOverrideKey], LyricsVisibilityOverride.AUTO),
                        smoothPlaybackEnabled = values[LyricsSmoothPlaybackKey] ?: true,
                        textColorMode =
                            parseEnumOrDefault(values[LyricsTextColorModeKey], LyricsTextColorMode.DEFAULT),
                        textColorCustom = parseHexColor(values[LyricsTextColorCustomKey]),
                        inactiveLineAlpha =
                            (values[LyricsInactiveLineAlphaKey] ?: 0.35f).coerceIn(0.05f, 0.95f),
                        textContrastGuard = values[LyricsTextContrastGuardKey] ?: true,
                    )
                }.distinctUntilChanged()

        private inline fun <reified T : Enum<T>> parseEnumOrDefault(
            raw: String?,
            default: T,
        ): T {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) return default
            return runCatching { enumValueOf<T>(trimmed) }.getOrDefault(default)
        }

        private fun parseHexColor(raw: String?): Color? {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) return null
            return runCatching {
                val withHash = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
                val argb = android.graphics.Color.parseColor(withHash)
                Color(argb.toLong() and 0xFFFFFFFFL)
            }.getOrNull()
        }
    }
