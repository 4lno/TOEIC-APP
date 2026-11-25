package com.luudev.vocalens.presentation.ranking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luudev.vocalens.R
import com.luudev.vocalens.presentation.components.pondBackground
import com.luudev.vocalens.presentation.theme.KoiColors

@Composable
fun RankingRoute(viewModel: RankingViewModel, onBack: () -> Unit) {
    DisposableEffect(viewModel) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }
    RankingScreen(viewModel.uiState, viewModel::retry, onBack)
}

@Composable
fun RankingScreen(state: RankingUiState, onRetry: () -> Unit, onBack: () -> Unit) {
    val ranking = state.ranking
    Box(Modifier.fillMaxSize().pondBackground(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 600.dp).fillMaxSize(),
            contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TextButton(onClick = onBack) { Text(stringResource(R.string.ranking_back)) }
                Text(stringResource(R.string.ranking_title), style = MaterialTheme.typography.headlineMedium,
                    color = KoiColors.Ink, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.ranking_subtitle), color = KoiColors.Muted)
            }
            item {
                Surface(color = KoiColors.DeepWater, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text(stringResource(R.string.ranking_my_xp), color = KoiColors.SoftWater)
                        Text(stringResource(R.string.ranking_xp, ranking.myTotalXp),
                            style = MaterialTheme.typography.headlineLarge, color = KoiColors.Surface)
                    }
                }
            }
            if (state.loading) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            if (state.error) item { Text(stringResource(R.string.ranking_error), color = MaterialTheme.colorScheme.error) }
            if (ranking.fromCache) item { Text(stringResource(R.string.ranking_cached), color = KoiColors.Muted) }
            if (ranking.pendingSessions > 0) item { Text(stringResource(R.string.ranking_pending, ranking.pendingSessions), color = KoiColors.Primary) }
            if (ranking.rejectedSessions > 0) item { Text(stringResource(R.string.ranking_rejected, ranking.rejectedSessions), color = MaterialTheme.colorScheme.error) }
            item { OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.ranking_retry)) } }
            if (!state.loading && !state.error && ranking.entries.isEmpty()) item { Text(stringResource(R.string.ranking_empty), color = KoiColors.Muted) }
            items(ranking.entries, key = { it.userId }) { entry ->
                val own = entry.userId == state.userId
                Surface(shape = RoundedCornerShape(16.dp), color = if (own) KoiColors.SoftWater else KoiColors.Surface,
                    border = BorderStroke(1.dp, if (own) KoiColors.Primary else KoiColors.Outline)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("#${entry.rank}", color = KoiColors.Primary, fontWeight = FontWeight.Bold)
                        Text(if (own) stringResource(R.string.ranking_you, entry.displayName) else entry.displayName,
                            modifier = Modifier.weight(1f), color = KoiColors.Ink, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.ranking_xp, entry.totalXp), color = KoiColors.OrangeInk)
                    }
                }
            }
            item { Text(stringResource(R.string.ranking_note), style = MaterialTheme.typography.bodySmall, color = KoiColors.Muted) }
        }
    }
}
