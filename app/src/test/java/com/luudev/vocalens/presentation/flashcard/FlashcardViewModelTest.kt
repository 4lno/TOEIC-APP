package com.luudev.vocalens.presentation.flashcard

import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.repository.FlashcardRepository
import com.luudev.vocalens.domain.repository.VocabularyRepository
import com.luudev.vocalens.domain.repository.CollectionRepository
import com.luudev.vocalens.domain.usecase.*
import com.luudev.vocalens.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlashcardViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val book = VocabularyWord("book", "Book", "", "Sách", VocabularyCategory.Objects)
    private val phone = VocabularyWord("phone", "Phone", "", "Điện thoại", VocabularyCategory.Technology)
    private var clock = 1000L
    private val words = MutableStateFlow(listOf(book, phone))
    private val groups = MutableStateFlow(listOf(VocabularyCollection("books", "Sách của tôi", wordCount = 1),
        VocabularyCollection("empty", "Bộ rỗng")))
    private val membership = MutableStateFlow(listOf(book))
    private val collections = object : CollectionRepository {
        override fun observeCollections(userId: String) = if (userId == "A") groups else flowOf(emptyList())
        override fun observeWords(userId: String, collectionId: String) =
            if (userId == "A" && collectionId == "books") membership else flowOf(emptyList())
        override suspend fun create(userId: String, name: String, description: String) = Unit
        override suspend fun update(userId: String, collectionId: String, name: String, description: String) = Unit
        override suspend fun delete(userId: String, collectionId: String) = Unit
        override suspend fun setWords(userId: String, collectionId: String, wordIds: Set<String>) = Unit
        override suspend fun removeWord(userId: String, collectionId: String, wordId: String) = Unit
    }
    private val vocabulary = object : VocabularyRepository {
        override fun observeSavedWords(userId: String) = words
        override suspend fun saveWord(userId: String, word: VocabularyWord) = Unit
        override suspend fun deleteWord(userId: String, wordId: String) = Unit
    }
    private class Repository : FlashcardRepository {
        val starts = mutableListOf<String>()
        val reviews = mutableListOf<Pair<String, FlashcardReview>>()
        var abandoned = false
        var failed = false
        var latency = 0L
        override suspend fun start(userId: String, sessionId: String, startedAt: Long) { starts += sessionId }
        override suspend fun review(userId: String, review: FlashcardReview) {
            delay(latency)
            if (failed) error("Disk unavailable")
            reviews += userId to review
        }
        override suspend fun abandon(userId: String, sessionId: String, endedAt: Long, activeDurationMs: Long) {
            if (failed) error("Disk unavailable")
            abandoned = true
        }
    }
    private fun vm(repo: Repository) = FlashcardViewModel(GetSavedVocabulary(vocabulary),
        StartFlashcards(repo), RateFlashcard(repo), AbandonFlashcards(repo),
        ObserveCollections(collections), ObserveCollectionWords(collections), now = { clock }, monotonic = { clock })
        .apply { onUserChanged("A"); setShuffled(false); setForeground(true) }

    @Test fun collectionAndRevealAreRequiredBeforeRatingAndFinalReviewCompletes() = runTest {
        val repo = Repository()
        val vm = vm(repo)
        runCurrent()
        vm.selectCollection("books")
        runCurrent()
        vm.start()
        runCurrent()
        assertEquals(listOf(book), vm.uiState.cards)
        vm.rate(FlashcardRating.EASY)
        runCurrent()
        assertTrue(repo.reviews.isEmpty())
        vm.reveal()
        clock += 500
        vm.rate(FlashcardRating.GOOD)
        runCurrent()
        assertEquals(FlashcardStage.Completed, vm.uiState.stage)
        assertEquals(1, vm.uiState.remembered)
        assertEquals(5, vm.uiState.earnedXp)
        val event = repo.reviews.single().second
        assertTrue(event.completed)
        assertEquals(500L, event.activeDurationMs)
    }

    @Test fun failedWriteKeepsCardAndRetriesIdenticalEventWithoutSkipping() = runTest {
        val repo = Repository()
        val vm = vm(repo)
        runCurrent(); vm.start(); runCurrent(); vm.reveal()
        repo.failed = true
        clock += 100
        vm.rate(FlashcardRating.HARD)
        runCurrent()
        assertTrue(vm.uiState.error)
        assertEquals(0, vm.uiState.index)
        assertTrue(vm.uiState.ratings.isEmpty())
        repo.failed = false
        clock += 200
        vm.retry()
        runCurrent()
        assertEquals(1100L, repo.reviews.single().second.reviewedAt)
        assertEquals(1, vm.uiState.index)
        assertFalse(vm.uiState.revealed)
        assertEquals(listOf(FlashcardRating.HARD), vm.uiState.ratings)
        assertEquals(0, vm.uiState.earnedXp)
    }

    @Test fun doubleTapCannotRecordTwoReviews() = runTest {
        val repo = Repository()
        val vm = vm(repo)
        runCurrent(); vm.start(); runCurrent(); vm.reveal()
        vm.rate(FlashcardRating.AGAIN)
        vm.rate(FlashcardRating.EASY)
        runCurrent()
        assertEquals(1, repo.reviews.size)
        assertEquals(FlashcardRating.AGAIN, repo.reviews.single().second.rating)
        assertEquals(1, vm.uiState.index)
    }

    @Test fun difficultRoundContainsOnlyAgainAndHardCardsWithNewSession() = runTest {
        val repo = Repository()
        val vm = vm(repo)
        runCurrent(); vm.start(); runCurrent(); vm.reveal()
        vm.rate(FlashcardRating.HARD); runCurrent(); vm.reveal()
        vm.rate(FlashcardRating.EASY); runCurrent()
        vm.start(difficultOnly = true); runCurrent()
        assertEquals(listOf(book), vm.uiState.cards)
        assertEquals(2, repo.starts.distinct().size)
        assertTrue(vm.uiState.ratings.isEmpty())
    }

    @Test fun backgroundTimeDoesNotEarnStudyMinutesAndSameUserKeepsSession() = runTest {
        val repo = Repository()
        val vm = vm(repo)
        runCurrent(); vm.selectCollection("books"); runCurrent(); vm.start(); runCurrent()
        clock += 100
        vm.setForeground(false)
        clock += 60_000
        vm.onUserChanged("A")
        assertEquals(FlashcardStage.Studying, vm.uiState.stage)
        vm.setForeground(true)
        clock += 200
        vm.reveal(); vm.rate(FlashcardRating.GOOD); runCurrent()
        assertEquals(300L, repo.reviews.single().second.activeDurationMs)
    }

    @Test fun switchingAccountCancelsPendingWriteAndClearsSession() = runTest {
        val repo = Repository().apply { latency = 10_000 }
        val vm = vm(repo)
        runCurrent(); vm.start(); runCurrent(); vm.reveal()
        vm.rate(FlashcardRating.EASY); runCurrent()
        vm.onUserChanged("B")
        assertEquals(FlashcardStage.Setup, vm.uiState.stage)
        assertTrue(vm.uiState.cards.isEmpty())
        assertTrue(vm.uiState.ratings.isEmpty())
        runCurrent()
        assertTrue(repo.reviews.isEmpty())
    }

    @Test fun exitNeedsConfirmationAndFailureKeepsSessionForRetry() = runTest {
        val repo = Repository()
        val vm = vm(repo)
        var exited = false
        runCurrent(); vm.start(); runCurrent()
        vm.requestExit { exited = true }
        assertTrue(vm.uiState.showExit)
        assertFalse(exited)
        vm.dismissExit()
        assertEquals(FlashcardStage.Studying, vm.uiState.stage)
        repo.failed = true
        vm.confirmExit { exited = true }; runCurrent()
        assertFalse(exited)
        assertTrue(vm.uiState.error)
        repo.failed = false
        vm.confirmExit { exited = true }; runCurrent()
        assertTrue(exited)
        assertTrue(repo.abandoned)
    }

    @Test fun emptyDeckDoesNotCreateSession() = runTest {
        words.value = emptyList()
        val repo = Repository()
        val vm = vm(repo)
        runCurrent(); vm.start(); runCurrent()
        assertTrue(repo.starts.isEmpty())
        assertEquals(FlashcardStage.Setup, vm.uiState.stage)
    }

    @Test fun allSavedWordsAndCollectionMembershipAreSeparateAndReactToEdits() = runTest {
        val repo = Repository()
        val vm = vm(repo)
        runCurrent()
        assertEquals(listOf(book, phone), vm.uiState.deck)
        vm.selectCollection("books")
        vm.start() // A tap before membership is loaded must not start an old deck.
        assertTrue(repo.starts.isEmpty())
        runCurrent()
        assertEquals(listOf(book), vm.uiState.deck)
        membership.value = listOf(phone) // Membership wins even when the category differs.
        runCurrent()
        assertEquals(listOf(phone), vm.uiState.deck)
        vm.selectCollection(null)
        assertEquals(listOf(book, phone), vm.uiState.deck)
        vm.start(); runCurrent()
        assertEquals(listOf(book, phone), vm.uiState.cards)
    }

    @Test fun emptyOrDeletedCollectionCannotStartAndSwitchingAccountClearsSelection() = runTest {
        val repo = Repository()
        val vm = vm(repo)
        runCurrent()
        vm.selectCollection("empty"); runCurrent(); vm.start()
        assertTrue(repo.starts.isEmpty())
        vm.selectCollection("books"); runCurrent()
        groups.value = emptyList(); runCurrent()
        assertTrue(vm.uiState.deck.isEmpty())
        vm.start(); runCurrent()
        assertTrue(repo.starts.isEmpty())
        vm.onUserChanged("B")
        assertNull(vm.uiState.collectionId)
        assertTrue(vm.uiState.collections.isEmpty())
        runCurrent()
        assertTrue(vm.uiState.collections.isEmpty())
    }
}
