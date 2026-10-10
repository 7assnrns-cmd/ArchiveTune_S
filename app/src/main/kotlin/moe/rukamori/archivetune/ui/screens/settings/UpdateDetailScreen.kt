/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package moe.rukamori.archivetune.ui.screens.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.BuildConfig
import moe.rukamori.archivetune.LocalAnimationsDisabled
import moe.rukamori.archivetune.LocalPlayerAwareWindowInsets
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.constants.UpdateChannel
import moe.rukamori.archivetune.ui.component.IconButton
import moe.rukamori.archivetune.ui.component.MarkdownText
import moe.rukamori.archivetune.ui.component.drawHyperOsGradient
import moe.rukamori.archivetune.ui.utils.backToMain
import moe.rukamori.archivetune.updates.ApkAsset
import moe.rukamori.archivetune.updates.UpdateDownloadScheduler
import moe.rukamori.archivetune.utils.ReleaseInfo
import moe.rukamori.archivetune.utils.Updater
import java.util.Locale

/** State machine for the update detail screen. */
private sealed interface UpdateCheckState {
    data object Checking : UpdateCheckState

    data object UpToDate : UpdateCheckState

    data class UpdateAvailable(val release: ReleaseInfo) : UpdateCheckState

    data class Error(val message: String?) : UpdateCheckState
}

@Composable
fun UpdateDetailScreen(
    navController: NavController,
    channel: UpdateChannel = UpdateChannel.MY_VERSION,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var checkState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Checking) }

    // Animated gradient background. Same palette and speed as the main
    // update screen so the transition between the two feels continuous.
    val hyperOsAnimationsDisabled = LocalAnimationsDisabled.current
    val hyperOsDark = isSystemInDarkTheme()
    val hyperOsTransition = rememberInfiniteTransition(label = "hyperos_detail_bg")
    val hyperOsPhaseRaw by
        hyperOsTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI.toFloat()),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(40_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "hyperos_detail_phase",
        )
    val hyperOsPhase = if (hyperOsAnimationsDisabled) 0f else hyperOsPhaseRaw

    // Kick off the check. The short delay lets the "Checking for updates"
    // placeholder render before the state flips, so the animation is not
    // skipped on fast connections.
    LaunchedEffect(channel) {
        checkState = UpdateCheckState.Checking
        val started = System.currentTimeMillis()
        val result =
            when (channel) {
                UpdateChannel.MY_VERSION ->
                    Updater.getLatestReleaseInfo(forceRefresh = true)
                UpdateChannel.OFFICIAL_VERSION ->
                    Updater.getOfficialLatestReleaseInfo(forceRefresh = true)
            }
        val elapsed = System.currentTimeMillis() - started
        val remaining = 600L - elapsed
        if (remaining > 0) delay(remaining)

        checkState =
            result.fold(
                onSuccess = { release ->
                    val latest = Updater.getReleaseVersionName(release)
                    if (Updater.isUpdateAvailable(latest, BuildConfig.VERSION_NAME)) {
                        UpdateCheckState.UpdateAvailable(release)
                    } else {
                        UpdateCheckState.UpToDate
                    }
                },
                onFailure = { UpdateCheckState.Error(it.message) },
            )
    }

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .drawBehind { drawHyperOsGradient(hyperOsPhase, hyperOsDark) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.update_detail_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                        ),
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))

            when (val state = checkState) {
                UpdateCheckState.Checking -> CheckingContent()

                is UpdateCheckState.Error ->
                    ErrorContent(
                        message = state.message,
                        onRetry = {
                            coroutineScope.launch {
                                // Force a fresh fetch by re-keying the effect
                                // through a direct call. The state will reset
                                // to Checking and the LaunchedEffect will not
                                // re-run because channel did not change, so
                                // perform the check inline here.
                                checkState = UpdateCheckState.Checking
                                val started = System.currentTimeMillis()
                                val result =
                                    when (channel) {
                                        UpdateChannel.MY_VERSION ->
                                            Updater.getLatestReleaseInfo(forceRefresh = true)
                                        UpdateChannel.OFFICIAL_VERSION ->
                                            Updater.getOfficialLatestReleaseInfo(forceRefresh = true)
                                    }
                                val elapsed = System.currentTimeMillis() - started
                                val remaining = 600L - elapsed
                                if (remaining > 0) delay(remaining)
                                checkState =
                                    result.fold(
                                        onSuccess = { release ->
                                            val latest = Updater.getReleaseVersionName(release)
                                            if (Updater.isUpdateAvailable(latest, BuildConfig.VERSION_NAME)) {
                                                UpdateCheckState.UpdateAvailable(release)
                                            } else {
                                                UpdateCheckState.UpToDate
                                            }
                                        },
                                        onFailure = { UpdateCheckState.Error(it.message) },
                                    )
                            }
                        },
                    )

                UpdateCheckState.UpToDate ->
                    UpToDateContent(
                        channel = channel,
                        onOpenChangelog = {
                            navController.navigate(
                                "settings/changelog?channel=${channel.name}",
                            )
                        },
                    )

                is UpdateCheckState.UpdateAvailable ->
                    UpdateAvailableContent(
                        release = state.release,
                        onDownload = { asset ->
                            val versionForDownload =
                                Updater.getReleaseVersionName(state.release)
                            UpdateDownloadScheduler.cancel(context)
                            UpdateDownloadScheduler.schedule(
                                context = context,
                                url = asset.url,
                                version = versionForDownload,
                                wifiOnly = true,
                            )
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.updates_download_started),
                                )
                            }
                        },
                        onOpenChangelog = {
                            navController.navigate(
                                "settings/changelog?channel=${channel.name}",
                            )
                        },
                    )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Content states
// ─────────────────────────────────────────────────────────────

@Composable
private fun CheckingContent() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.updates_app_display_name),
            style =
                MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    letterSpacing = (-0.5).sp,
                ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = BuildConfig.VERSION_NAME,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(60.dp))
        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.update_detail_checking),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.update_detail_checking_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ErrorContent(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.update_detail_error),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shapes = ButtonDefaults.shapes(),
        ) {
            Text(stringResource(R.string.update_detail_retry))
        }
    }
}

@Composable
private fun UpToDateContent(
    channel: UpdateChannel,
    onOpenChangelog: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.updates_app_display_name),
            style =
                MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    letterSpacing = (-0.5).sp,
                ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = BuildConfig.VERSION_NAME,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(40.dp))

        HyperOsCard(
            version = BuildConfig.VERSION_NAME,
            sizeLabel = null,
            statusText = stringResource(R.string.update_detail_up_to_date),
            onClick = onOpenChangelog,
        )
    }
}

@Composable
private fun UpdateAvailableContent(
    release: ReleaseInfo,
    onDownload: (ApkAsset) -> Unit,
    onOpenChangelog: () -> Unit,
) {
    val newVersion = remember(release) { Updater.getReleaseVersionName(release) }

    // Order assets: device-matched first, then universal, then anything else.
    val orderedAssets =
        remember(release) {
            val raw = release.apkAssets
            val deviceArch = Updater.detectDeviceArchitecture()
            val device = deviceArch?.let { arch -> raw.firstOrNull { it.architecture == arch } }
            val universal = raw.firstOrNull { it.architecture == "universal" }
            buildList {
                device?.let { add(it) }
                universal?.let { if (it != device) add(it) }
                raw.filter { it != device && it != universal }.forEach { add(it) }
            }
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.update_detail_update_available),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(20.dp))

        if (orderedAssets.isEmpty()) {
            // Fallback: no per-ABI metadata available, show a single card
            // with the generic download URL if present.
            val fallbackUrl = release.downloadUrl
            HyperOsCard(
                version = newVersion,
                sizeLabel = null,
                statusText = null,
                onClick = onOpenChangelog,
            )
            if (!fallbackUrl.isNullOrBlank()) {
                Spacer(Modifier.height(24.dp))
                DownloadButton(
                    onClick = {
                        onDownload(
                            ApkAsset(
                                architecture = "universal",
                                displayName = "",
                                url = fallbackUrl,
                                sizeBytes = 0L,
                            ),
                        )
                    },
                )
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { orderedAssets.size })
            HorizontalPager(
                state = pagerState,
                pageSpacing = 12.dp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val asset = orderedAssets[page]
                val label =
                    if (asset.architecture == "universal") {
                        stringResource(R.string.update_detail_universal)
                    } else {
                        stringResource(R.string.update_detail_best_device)
                    }
                HyperOsCard(
                    version = newVersion,
                    sizeLabel = formatSize(asset.sizeBytes),
                    statusText = label,
                    onClick = onOpenChangelog,
                )
            }
            Spacer(Modifier.height(12.dp))
            PagerIndicator(
                pageCount = orderedAssets.size,
                currentPage = pagerState.currentPage,
            )
            Spacer(Modifier.height(24.dp))
            DownloadButton(
                onClick = { onDownload(orderedAssets[pagerState.currentPage]) },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Components
// ─────────────────────────────────────────────────────────────

@Composable
private fun HyperOsCard(
    version: String,
    sizeLabel: String?,
    statusText: String?,
    onClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(24.dp)
    Surface(
        onClick = onClick,
        shape = cardShape,
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f),
        color = Color.Transparent,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(cardShape)
                    .background(
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color(0xFF5C6BFF),
                                    Color(0xFF6E56CF),
                                ),
                        ),
                    )
                    .padding(24.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.updates_app_display_name),
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                        ),
                    color = Color.White,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = version,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                if (!sizeLabel.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = sizeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }

                Spacer(Modifier.weight(1f))

                if (!statusText.isNullOrBlank()) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    Spacer(Modifier.height(10.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.update_detail_learn_more),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.size(6.dp))
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier =
                            Modifier
                                .size(16.dp)
                                .androidxRotate(),
                    )
                }
            }
        }
    }
}

/** Rotate the back-arrow by 180° to look like a forward chevron. */
private fun Modifier.androidxRotate(): Modifier =
    androidx.compose.ui.draw.rotate(this, 180f)

@Composable
private fun DownloadButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp),
        shapes = ButtonDefaults.shapes(),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2F6BFF),
                contentColor = Color.White,
            ),
    ) {
        Text(
            text = stringResource(R.string.update_detail_download),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PagerIndicator(pageCount: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            Box(
                modifier =
                    Modifier
                        .size(if (isActive) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                            },
                        ),
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val mb = bytes / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}
