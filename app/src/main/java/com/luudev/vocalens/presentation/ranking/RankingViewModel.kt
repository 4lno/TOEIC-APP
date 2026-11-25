package com.luudev.vocalens.presentation.ranking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luudev.vocalens.domain.model.RankingSnapshot
import com.luudev.vocalens.domain.usecase.ObserveRanking
import com.luudev.vocalens.domain.usecase.RequestXpSync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class RankingUiState(
    val userId: String? = null, val ranking: RankingSnapshot = RankingSnapshot(),
    val loading: Boolean = false, val error: Boolean = false
)

class RankingViewModel(
    private val observeRanking: ObserveRanking,
    private val requestXpSync: RequestXpSync,
    private val monotonic: () -> Long = { System.nanoTime() / 1_000_000 }
) : ViewModel() {
    var uiState by mutableStateOf(RankingUiState())
        private set
    private var owner: String? = null
    private var observation: Job? = null
    private var screenActive = false
    private var lastSyncRequestAt: Long? = null

    fun onUserChanged(uid: String?) {
        if (owner == uid) return
        stop()
        owner = uid
        lastSyncRequestAt = null
        uiState = RankingUiState(userId = uid)
        // Subscribe only when the ranking screen is open.
    }

    fun start() {
        screenActive = true
        val uid = owner ?: return
        if (observation?.isActive == true) return
        uiState = uiState.copy(loading = true, error = false)
        observation = viewModelScope.launch {
            try {
                // Login and completed sessions already schedule synchronization. Resuming
                // this screen only reconnects its listeners, never starts a new retry chain.
                observeRanking(uid).collect { ranking ->
                    if (owner == uid) uiState = uiState.copy(ranking = ranking, loading = false, error = false)
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (owner == uid) uiState = uiState.copy(loading = false, error = true) }
        }
    }

    fun stop() {
        screenActive = false
        observation?.cancel()
        observation = null
    }

    fun retry() {
        val uid = owner ?: return
        if (!screenActive) return
        val at = monotonic()
        if (lastSyncRequestAt?.let { at - it < 5_000 } == true) return
        lastSyncRequestAt = at
        try {
            requestXpSync(uid)
            // Keep a healthy listener. Repeated clicks must not reload the top 50.
            start()
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) { uiState = uiState.copy(loading = false, error = true) }
    }
}
