package com.luudev.vocalens.presentation.ranking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.luudev.vocalens.R
import com.luudev.vocalens.domain.model.RankingEntry
import com.luudev.vocalens.domain.model.RankingSnapshot
import com.luudev.vocalens.presentation.components.AppGlyph
import com.luudev.vocalens.presentation.components.AppIcon
import com.luudev.vocalens.presentation.components.pondBackground
import com.luudev.vocalens.presentation.theme.KoiColors
import com.luudev.vocalens.presentation.theme.VocaLensTheme

@Composable
fun RankingRoute(viewModel: RankingViewModel, onBack: () -> Unit) {
    LifecycleResumeEffect(viewModel, viewModel.uiState.userId) {
        viewModel.start()
        onPauseOrDispose { viewModel.stop() }
    }
    RankingScreen(viewModel.uiState, viewModel::retry, onBack)
}

@Composable
fun RankingScreen(state: RankingUiState, onRetry: () -> Unit, onBack: () -> Unit) {
    val ranking = state.ranking
    var personal by rememberSaveable { mutableStateOf(false) }
    val leaderboardScroll = rememberLazyListState()
    val personalScroll = rememberLazyListState()
    Box(Modifier.fillMaxSize().pondBackground(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 600.dp).fillMaxSize(),
            state = if (personal) personalScroll else leaderboardScroll,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = onBack, colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = KoiColors.Surface, contentColor = KoiColors.Ink
                    )) {
                        AppIcon(AppGlyph.Back, KoiColors.Ink, description = stringResource(R.string.ranking_back))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.ranking_eyebrow), color = KoiColors.Primary,
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(18.dp))
                Text(stringResource(if (personal) R.string.ranking_personal_title else R.string.ranking_title), color = KoiColors.Ink,
                    style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.ranking_subtitle), color = KoiColors.Muted,
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }
            if (personal) item {
                PersonalXpCard(ranking.myTotalXp, ranking.entries.firstOrNull { it.userId == state.userId }?.rank)
            }
            if (!personal && ranking.entries.isNotEmpty()) item { RankingPodium(ranking.entries.take(3), state.userId) }
            if (state.loading) item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(CircleShape),
                    color = KoiColors.Primary, trackColor = KoiColors.SoftWater)
            }
            if (state.error) item { RankingNotice(stringResource(R.string.ranking_error), warning = true) }
            if (ranking.fromCache) item { RankingNotice(stringResource(R.string.ranking_cached)) }
            if (ranking.pendingSessions > 0) item { RankingNotice(stringResource(R.string.ranking_pending, ranking.pendingSessions)) }
            if (ranking.rejectedSessions > 0) item {
                RankingNotice(stringResource(R.string.ranking_rejected, ranking.rejectedSessions), warning = true)
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(if (personal) R.string.ranking_sync_status else R.string.ranking_learners), color = KoiColors.Ink,
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.ranking_order), color = KoiColors.Muted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onRetry) {
                        AppIcon(AppGlyph.Download, KoiColors.Primary, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.ranking_sync), color = KoiColors.Primary)
                    }
                }
            }
            if (!personal && !state.loading && !state.error && ranking.entries.isEmpty()) item {
                Surface(shape = RoundedCornerShape(24.dp), color = KoiColors.Surface,
                    border = BorderStroke(1.dp, KoiColors.Outline)) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(60.dp).background(KoiColors.SoftOrange, CircleShape), contentAlignment = Alignment.Center) {
                            AppIcon(AppGlyph.Sparkles, KoiColors.OrangeInk, Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.ranking_empty_title), color = KoiColors.Ink,
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.ranking_empty), color = KoiColors.Muted,
                            style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
            if (!personal) {
                items(ranking.entries, key = { it.userId }) { entry -> RankingRow(entry, entry.userId == state.userId) }
            } else {
                item {
                    Surface(color = KoiColors.Surface, shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, KoiColors.Outline)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            XpStatusRow(stringResource(R.string.ranking_pending_label), ranking.pendingSessions.toString(), AppGlyph.Clock)
                            HorizontalDivider(color = KoiColors.Outline)
                            XpStatusRow(stringResource(R.string.ranking_rejected_label), ranking.rejectedSessions.toString(), AppGlyph.Bell)
                        }
                    }
                }
            }
            item {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIcon(AppGlyph.Check, KoiColors.Muted, Modifier.size(16.dp))
                    Text(stringResource(R.string.ranking_note), style = MaterialTheme.typography.bodySmall,
                        color = KoiColors.Muted)
                }
            }
        }
        RankingSwitcher(personal, { personal = it }, Modifier.align(Alignment.BottomCenter)
            .navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp).widthIn(max = 420.dp).fillMaxWidth())
    }
}

@Composable
private fun RankingSwitcher(personal: Boolean, onSelect: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(28.dp), color = KoiColors.Surface,
        shadowElevation = 12.dp, border = BorderStroke(1.dp, KoiColors.Outline)) {
        Row(Modifier.padding(6.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(false, true).forEach { tab ->
                val selected = personal == tab
                val color = if (selected) KoiColors.Surface else KoiColors.Muted
                Row(Modifier.weight(1f).clip(RoundedCornerShape(23.dp))
                    .testTag(if (tab) "ranking_tab_personal" else "ranking_tab_leaderboard")
                    .background(if (selected) KoiColors.DeepWater else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab) })
                    .heightIn(min = 52.dp).padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(if (tab) AppGlyph.Person else AppGlyph.Chart, color, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(stringResource(if (tab) R.string.ranking_personal_tab else R.string.ranking_title),
                        color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun XpStatusRow(label: String, value: String, glyph: AppGlyph) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppIcon(glyph, KoiColors.Primary, Modifier.size(20.dp))
        Text(label, Modifier.weight(1f), color = KoiColors.Muted, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = KoiColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

private val PodiumShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width * 0.91f, size.height)
            lineTo(size.width * 0.09f, size.height)
            close()
        })
}

@Composable
private fun RankingPodium(entries: List<RankingEntry>, userId: String?) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Place the leader in the centre; ranks still come from the domain (including ties).
        listOf(1, 0, 2).forEach { index ->
            val entry = entries.getOrNull(index)
            if (entry == null) Spacer(Modifier.weight(1f)) else {
                val leader = index == 0
                val colors = when (index) {
                    0 -> listOf(KoiColors.Orange, KoiColors.SoftOrange)
                    1 -> listOf(KoiColors.Water, KoiColors.SoftWater)
                    else -> listOf(KoiColors.Lily, KoiColors.SoftWater)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (leader) {
                        AppIcon(AppGlyph.Sparkles, KoiColors.OrangeInk, Modifier.size(23.dp))
                        Spacer(Modifier.height(6.dp))
                    }
                    Surface(shape = CircleShape, color = KoiColors.Surface,
                        border = BorderStroke(3.dp, colors.first()), shadowElevation = 3.dp,
                        modifier = Modifier.size(if (leader) 66.dp else 54.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(entry.displayName.trim().take(1).uppercase().ifEmpty { "?" },
                                color = KoiColors.DeepWater, fontSize = if (leader) 26.sp else 22.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.fillMaxWidth().clip(PodiumShape)
                        .background(Brush.verticalGradient(colors)).heightIn(min = when(index) { 0 -> 154.dp; 1 -> 122.dp; else -> 100.dp })
                        .padding(horizontal = 7.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(entry.displayName, color = KoiColors.Ink, style = MaterialTheme.typography.labelMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        if (entry.userId == userId) Text(stringResource(R.string.ranking_you_label),
                            color = KoiColors.Ink, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(5.dp))
                        Text(stringResource(R.string.ranking_xp, entry.totalXp), color = KoiColors.Ink,
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("${entry.rank}", color = KoiColors.Ink, fontSize = if (leader) 38.sp else 30.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalXpCard(xp: Long, rank: Int?) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
        .background(Brush.linearGradient(listOf(KoiColors.DeepWater, KoiColors.Primary))).padding(22.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.ranking_my_xp), color = KoiColors.SoftWater,
                style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Box(Modifier.size(36.dp).background(Color.White.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                AppIcon(AppGlyph.Sparkles, KoiColors.SoftOrange, Modifier.size(20.dp))
            }
        }
        Text(stringResource(R.string.ranking_xp, xp), color = KoiColors.Surface,
            style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIcon(AppGlyph.Chart, KoiColors.SoftOrange, Modifier.size(18.dp))
            Text(if (rank != null) stringResource(R.string.ranking_position, rank)
                else stringResource(R.string.ranking_keep_learning),
                color = KoiColors.Surface, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RankingRow(entry: RankingEntry, own: Boolean) {
    val accent = when (entry.rank) {
        1 -> KoiColors.OrangeInk
        2 -> KoiColors.Primary
        3 -> KoiColors.Lily
        else -> KoiColors.Muted
    }
    val badge = when (entry.rank) {
        1 -> KoiColors.SoftOrange
        2 -> KoiColors.SoftWater
        3 -> KoiColors.Mint
        else -> Color.Transparent
    }
    Surface(shape = RoundedCornerShape(20.dp), color = if (own) KoiColors.SoftWater else KoiColors.Surface,
        border = BorderStroke(1.dp, if (own) KoiColors.Primary else KoiColors.Outline)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).background(badge, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Text("${entry.rank}", color = accent, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(40.dp).background(if (own) KoiColors.Primary else KoiColors.Mint, CircleShape),
                contentAlignment = Alignment.Center) {
                Text(entry.displayName.trim().take(1).uppercase().ifEmpty { "?" },
                    color = if (own) KoiColors.Surface else KoiColors.Primary, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(entry.displayName, color = KoiColors.Ink, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (own) Text(stringResource(R.string.ranking_you_label), color = KoiColors.Primary,
                    style = MaterialTheme.typography.labelSmall)
            }
            Text(stringResource(R.string.ranking_xp, entry.totalXp), color = if (own) KoiColors.DeepWater else KoiColors.OrangeInk,
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RankingNotice(message: String, warning: Boolean = false) {
    Surface(color = if (warning) KoiColors.SoftOrange else KoiColors.Surface,
        shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppIcon(if (warning) AppGlyph.Bell else AppGlyph.Clock,
                if (warning) KoiColors.OrangeInk else KoiColors.Primary, Modifier.size(18.dp))
            Text(message, color = if (warning) KoiColors.OrangeInk else KoiColors.Muted,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RankingPreview() {
    VocaLensTheme(darkTheme = false, dynamicColor = false) {
        RankingScreen(RankingUiState(userId = "me", loading = false, ranking = RankingSnapshot(
            myTotalXp = 1250, entries = listOf(
                RankingEntry("first", "Minh Anh", 2400, 1),
                RankingEntry("second", "Hoàng Nam", 1800, 2),
                RankingEntry("me", "Trương Lưu", 1250, 3),
                RankingEntry("fourth", "Khánh Linh", 980, 4)
            )
        )), {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun EmptyRankingPreview() {
    VocaLensTheme(darkTheme = false, dynamicColor = false) {
        RankingScreen(RankingUiState(loading = false), {}, {})
    }
}
