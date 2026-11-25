package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.repository.RankingRepository
import com.luudev.vocalens.domain.repository.XpSyncRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RankingUseCasesTest {
    private val session = CompletedFlashcardSession("one", 1000, 2000, 1000,
        listOf(CompletedFlashcardReview("book", FlashcardRating.GOOD, 2000)))

    private class Queue(private val sessions: List<CompletedFlashcardSession>) : XpSyncRepository {
        val synced = mutableListOf<String>()
        val rejected = mutableListOf<String>()
        val owners = mutableListOf<String>()
        var failure: Exception? = null
        override suspend fun pending(userId: String) = sessions
        override suspend fun submit(userId: String, session: CompletedFlashcardSession) {
            owners += userId
            if (session.sessionId == "one") failure?.let { throw it }
        }
        override suspend fun markSynced(userId: String, sessionId: String) { synced += sessionId }
        override suspend fun markRejected(userId: String, sessionId: String, code: String) { rejected += sessionId }
    }

    @Test fun equalXpSharesCompetitionRankAndOwnConfirmedXpIsPreserved() = runTest {
        val repository = object : RankingRepository {
            override fun observeRanking(userId: String) = flowOf(RankingSnapshot(
                entries = listOf(RankingEntry("A", "An", 50), RankingEntry("B", "Binh", 50), RankingEntry("C", "Chi", 45)),
                myTotalXp = 10, pendingSessions = 2))
            override fun requestSync(userId: String) = Unit
        }
        val result = ObserveRanking(repository)("D").first()
        assertEquals(listOf(1, 1, 3), result.entries.map { it.rank })
        assertEquals(10L, result.myTotalXp)
        assertEquals(2, result.pendingSessions)
    }

    @Test fun acceptedSessionsAreMarkedOnlyAfterSuccessfulSubmission() = runTest {
        val repository = Queue(listOf(session, session.copy(sessionId = "two")))
        SyncPendingXp(repository)("A")
        assertEquals(listOf("one", "two"), repository.synced)
        assertEquals(listOf("A", "A"), repository.owners)
    }

    @Test fun rejectedSessionDoesNotBlockLaterSessions() = runTest {
        val repository = Queue(listOf(session, session.copy(sessionId = "two"))).apply { failure = XpSubmissionRejected("INVALID_ARGUMENT") }
        SyncPendingXp(repository)("A")
        assertEquals(listOf("one"), repository.rejected)
        assertEquals(listOf("two"), repository.synced)
    }

    @Test fun networkFailureLeavesSessionPendingAndStopsUntilRetry() = runTest {
        val repository = Queue(listOf(session, session.copy(sessionId = "two"))).apply { failure = IllegalStateException("Offline") }
        try { SyncPendingXp(repository)("A"); fail("Expected retryable error") } catch (_: IllegalStateException) { }
        assertTrue(repository.synced.isEmpty())
        assertTrue(repository.rejected.isEmpty())
        assertEquals(1, repository.owners.size)
    }

    @Test fun cancellationIsNeverConvertedIntoARejectedSession() = runTest {
        val repository = Queue(listOf(session)).apply { failure = CancellationException() }
        try { SyncPendingXp(repository)("A"); fail("Expected cancellation") } catch (_: CancellationException) { }
        assertTrue(repository.synced.isEmpty())
        assertTrue(repository.rejected.isEmpty())
    }
}
