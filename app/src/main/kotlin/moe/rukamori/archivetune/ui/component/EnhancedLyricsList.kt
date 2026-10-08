/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package moe.rukamori.archivetune.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

/**
 * Hand-rolled replacement for the library's KaraokeLyricsView.
 *
 * The library renderer invoked the position lambda for every visible
 * line, which forced a recomposition of the full list on every tick.
 * This implementation applies the same sentinel optimisation used by
 * LyricsV2: past lines receive Int.MAX_VALUE, future lines receive
 * Int.MIN_VALUE, and only the active line reads the live position.
 * Non-active lines therefore never subscribe to the hot state.
 */
@Composable
internal fun EnhancedLyricsList(
    listState: LazyListState,
    lyrics: SyncedLyrics,
    currentLineIndex: Int,
    playbackSyncPosition: () -> Int,
    textColor: Color,
    normalTextStyle: TextStyle,
    accompanimentTextStyle: TextStyle,
    phoneticTextStyle: TextStyle,
    showTranslation: Boolean,
    showPhonetic: Boolean,
    useBlurEffect: Boolean,
    selectedLineKeys: Set<String>,
    isSelectionModeActive: Boolean,
    viewportOffset: Dp,
    lineKey: (ISyncedLine) -> String,
    onLineClicked: (ISyncedLine) -> Unit,
    onLinePressed: (ISyncedLine) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = viewportOffset, bottom = 200.dp),
    ) {
        itemsIndexed(
            items = lyrics.lines,
            key = { _, line -> lineKey(line) },
        ) { index, line ->
            val isActive = index == currentLineIndex
            val isPast = index < currentLineIndex
            val isSelected = isSelectionModeActive && lineKey(line) in selectedLineKeys

            val linePositionMs = when {
                isActive -> playbackSyncPosition()
                isPast -> Int.MAX_VALUE
                else -> Int.MIN_VALUE
            }

            EnhancedLyricLine(
                line = line,
                currentPositionMs = linePositionMs,
                isActive = isActive,
                isPast = isPast,
                isSelected = isSelected,
                textColor = textColor,
                normalTextStyle = normalTextStyle,
                accompanimentTextStyle = accompanimentTextStyle,
                phoneticTextStyle = phoneticTextStyle,
                showTranslation = showTranslation,
                showPhonetic = showPhonetic,
                useBlurEffect = useBlurEffect,
                onClick = { onLineClicked(line) },
                onLongClick = { onLinePressed(line) },
            )
        }
    }
}

@Composable
private fun EnhancedLyricLine(
    line: ISyncedLine,
    currentPositionMs: Int,
    isActive: Boolean,
    isPast: Boolean,
    isSelected: Boolean,
    textColor: Color,
    normalTextStyle: TextStyle,
    accompanimentTextStyle: TextStyle,
    phoneticTextStyle: TextStyle,
    showTranslation: Boolean,
    showPhonetic: Boolean,
    useBlurEffect: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val targetAlpha = when {
        isActive -> 1f
        isPast -> 0.55f
        else -> 0.35f
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 250),
        label = "enhancedLineAlpha",
    )

    val targetBlur = if (useBlurEffect && !isActive && !isPast) 3f else 0f
    val animatedBlur by animateFloatAsState(
        targetValue = targetBlur,
        animationSpec = tween(durationMillis = 250),
        label = "enhancedLineBlur",
    )
    val blurModifier = if (useBlurEffect && animatedBlur > 0.01f) {
        Modifier.blur(animatedBlur.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    } else {
        Modifier
    }

    val textAlign: TextAlign
    val flowArrangement: Arrangement.Horizontal
    val karaokeAlignment = (line as? KaraokeLine)?.alignment
    when (karaokeAlignment) {
        KaraokeAlignment.End -> {
            textAlign = TextAlign.End
            flowArrangement = Arrangement.End
        }
        else -> {
            textAlign = TextAlign.Start
            flowArrangement = Arrangement.Start
        }
    }

    val columnAlignment = when (textAlign) {
        TextAlign.End -> Alignment.End
        TextAlign.Center -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                } else {
                    Color.Transparent
                },
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .then(blurModifier)
            .alpha(animatedAlpha)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = columnAlignment,
    ) {
        when (line) {
            is KaraokeLine -> KaraokeLineContent(
                line = line,
                currentPositionMs = currentPositionMs,
                isPast = isPast,
                textColor = textColor,
                normalTextStyle = normalTextStyle,
                accompanimentTextStyle = accompanimentTextStyle,
                phoneticTextStyle = phoneticTextStyle,
                showTranslation = showTranslation,
                showPhonetic = showPhonetic,
                textAlign = textAlign,
                flowArrangement = flowArrangement,
            )
            is SyncedLine -> SyncedLineContent(
                line = line,
                textColor = textColor,
                normalTextStyle = normalTextStyle,
                phoneticTextStyle = phoneticTextStyle,
                showTranslation = showTranslation,
                textAlign = textAlign,
            )
            else -> Unit
        }
    }
}

@Composable
private fun KaraokeLineContent(
    line: KaraokeLine,
    currentPositionMs: Int,
    isPast: Boolean,
    textColor: Color,
    normalTextStyle: TextStyle,
    accompanimentTextStyle: TextStyle,
    phoneticTextStyle: TextStyle,
    showTranslation: Boolean,
    showPhonetic: Boolean,
    textAlign: TextAlign,
    flowArrangement: Arrangement.Horizontal,
) {
    val phoneticText: String? = line.phonetic
    if (showPhonetic && !phoneticText.isNullOrBlank()) {
        Text(
            text = phoneticText,
            style = phoneticTextStyle,
            color = textColor.copy(alpha = 0.55f),
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = flowArrangement,
    ) {
        for (syllable in line.syllables) {
            SyllableText(
                syllable = syllable,
                currentPositionMs = currentPositionMs,
                isPast = isPast,
                textColor = textColor,
                baseStyle = normalTextStyle,
                phoneticTextStyle = phoneticTextStyle,
                showPhonetic = showPhonetic,
                secondary = false,
            )
        }
    }

    val translationText: String? = line.translation
    if (showTranslation && !translationText.isNullOrBlank()) {
        Text(
            text = translationText,
            style = phoneticTextStyle,
            color = textColor.copy(alpha = 0.7f),
            textAlign = textAlign,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }

    val main = line as? KaraokeLine.MainKaraokeLine
    if (main != null) {
        // accompanimentLines is nullable on MainKaraokeLine (it has a
        // default value in the constructor). orEmpty() gives us a plain
        // empty list to iterate over when the field is null.
        for (acc in main.accompanimentLines.orEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = flowArrangement,
            ) {
                for (syllable in acc.syllables) {
                    SyllableText(
                        syllable = syllable,
                        currentPositionMs = currentPositionMs,
                        isPast = isPast,
                        textColor = textColor,
                        baseStyle = accompanimentTextStyle,
                        phoneticTextStyle = phoneticTextStyle,
                        showPhonetic = false,
                        secondary = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyllableText(
    syllable: KaraokeSyllable,
    currentPositionMs: Int,
    isPast: Boolean,
    textColor: Color,
    baseStyle: TextStyle,
    phoneticTextStyle: TextStyle,
    showPhonetic: Boolean,
    secondary: Boolean,
) {
    val isSyllablePast = isPast || currentPositionMs >= syllable.end
    val isSyllableActive = !isPast && currentPositionMs in syllable.start until syllable.end

    val syllableColor = when {
        isSyllablePast || isSyllableActive -> {
            if (secondary) textColor.copy(alpha = 0.85f) else textColor
        }
        else -> {
            if (secondary) textColor.copy(alpha = 0.35f) else textColor.copy(alpha = 0.42f)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val syllablePhonetic: String? = syllable.phonetic
        if (showPhonetic && !syllablePhonetic.isNullOrBlank()) {
            Text(
                text = syllablePhonetic,
                style = phoneticTextStyle,
                color = syllableColor.copy(alpha = 0.65f),
            )
        }
        Text(
            text = syllable.content,
            style = baseStyle,
            color = syllableColor,
        )
    }
}

@Composable
private fun SyncedLineContent(
    line: SyncedLine,
    textColor: Color,
    normalTextStyle: TextStyle,
    phoneticTextStyle: TextStyle,
    showTranslation: Boolean,
    textAlign: TextAlign,
) {
    Text(
        text = line.content,
        style = normalTextStyle,
        color = textColor,
        textAlign = textAlign,
        modifier = Modifier.fillMaxWidth(),
    )
    val translationText: String? = line.translation
    if (showTranslation && !translationText.isNullOrBlank()) {
        Text(
            text = translationText,
            style = phoneticTextStyle,
            color = textColor.copy(alpha = 0.7f),
            textAlign = textAlign,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}
