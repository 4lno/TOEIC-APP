package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.XpSubmissionRejected
import com.luudev.vocalens.domain.repository.RankingRepository
import com.luudev.vocalens.domain.repository.XpSyncRepository
import kotlinx.coroutines.flow.map

class ObserveRanking(private val repository: RankingRepository) {
    operator fun invoke(userId: String) = repository.observeRanking(userId).map { snapshot ->
        var rank = 0
        var previousXp: Long? = null
        snapshot.copy(entries = snapshot.entries.mapIndexed { index, entry ->
            if (previousXp != entry.totalXp) rank = index + 1
            previousXp = entry.totalXp
            entry.copy(rank = rank)
        })
    }
}

class RequestXpSync(private val repository: RankingRepository) {
    operator fun invoke(userId: String) = repository.requestSync(userId)
}

class SyncPendingXp(private val repository: XpSyncRepository) {
    suspend operator fun invoke(userId: String) {
        for (session in repository.pending(userId)) {
            try {
                repository.submit(userId, session)
                repository.markSynced(userId, session.sessionId)
            } catch (error: XpSubmissionRejected) {
                repository.markRejected(userId, session.sessionId, error.code)
            }
            // Transient errors and cancellation propagate; leave the outbox pending for retry.
        }
    }
}
