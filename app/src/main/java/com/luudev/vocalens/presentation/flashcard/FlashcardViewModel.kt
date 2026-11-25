package com.luudev.vocalens.presentation.flashcard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.usecase.*
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

enum class FlashcardStage { Setup, Studying, Completed }

data class FlashcardUiState(
    val stage: FlashcardStage = FlashcardStage.Setup,
    val availableWords: List<VocabularyWord> = emptyList(),
    val collections: List<VocabularyCollection> = emptyList(),
    val collectionId: String? = null,
    val collectionWords: List<VocabularyWord> = emptyList(),
    val shuffled: Boolean = true,
    val meaningFirst: Boolean = false,
    val cards: List<VocabularyWord> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val ratings: List<FlashcardRating> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: Boolean = false,
    val showExit: Boolean = false
) {
    val deck get() = if (collectionId == null) availableWords else
        collectionWords.filter { card -> availableWords.any { it.id == card.id } }
    val current get() = cards.getOrNull(index)
    val remembered get() = ratings.count { it == FlashcardRating.GOOD || it == FlashcardRating.EASY }
    val earnedXp get() = CalculateFlashcardXp()(ratings.size, stage == FlashcardStage.Completed)
}

class FlashcardViewModel(
    private val getSavedVocabulary: GetSavedVocabulary,
    private val startFlashcards: StartFlashcards,
    private val rateFlashcard: RateFlashcard,
    private val abandonFlashcards: AbandonFlashcards,
    private val observeCollections: ObserveCollections? = null,
    private val observeCollectionWords: ObserveCollectionWords? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val monotonic: () -> Long = { System.nanoTime() / 1_000_000 }
) : ViewModel() {
    var uiState by mutableStateOf(FlashcardUiState())
        private set
    private var owner: String? = null
    private var observation: Job? = null
    private var collectionObservation: Job? = null
    private var baseReady = false
    private var collectionLoading = false
    private var operation: Job? = null
    private var sessionId: String? = null
    private var startedAt = 0L
    private var cardStarted = 0L
    private var activeMs = 0L
    private var resumedAt: Long? = null
    private var foreground = false
    // Keep the exact event for retries; a second tap cannot create a new review.
    private var pendingReview: FlashcardReview? = null

    fun onUserChanged(userId: String?) {
        if (owner == userId) return
        observation?.cancel()
        collectionObservation?.cancel()
        operation?.cancel()
        owner = userId
        baseReady = false
        collectionLoading = false
        sessionId = null
        pendingReview = null
        resumedAt = null
        uiState = FlashcardUiState(loading = userId != null)
        if (userId != null) observeWords(userId)
    }

    private fun observeWords(userId: String) {
        observation?.cancel()
        observation = viewModelScope.launch {
            try {
                combine(getSavedVocabulary(userId), observeCollections?.invoke(userId) ?: flowOf(emptyList())) { words, groups ->
                    words to groups
                }.collect { (words, groups) ->
                    if (owner == userId) {
                        baseReady = true
                        uiState = uiState.copy(availableWords = words, collections = groups, loading = collectionLoading)
                        if (uiState.collectionId != null && groups.none { it.id == uiState.collectionId }) {
                            collectionObservation?.cancel()
                            collectionLoading = false
                            uiState = uiState.copy(collectionWords = emptyList(), loading = false)
                        }
                    }
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (owner == userId) uiState = uiState.copy(loading = false, error = true) }
        }
    }

    fun prepare(collectionId: String? = null) {
        if (uiState.stage == FlashcardStage.Studying || uiState.busy) return
        uiState = uiState.copy(stage = FlashcardStage.Setup, cards = emptyList(),
            ratings = emptyList(), index = 0, revealed = false, showExit = false, error = false)
        selectCollection(collectionId)
    }

    fun selectCollection(collectionId: String?) {
        if (uiState.stage != FlashcardStage.Setup || uiState.busy) return
        if (collectionId != null && uiState.collections.none { it.id == collectionId }) return
        if (collectionId == uiState.collectionId) return
        collectionObservation?.cancel()
        collectionLoading = collectionId != null
        uiState = uiState.copy(collectionId = collectionId, collectionWords = emptyList(),
            loading = !baseReady || collectionLoading, error = false)
        if (collectionId != null) owner?.let { observeSelectedCollection(it, collectionId) }
    }

    private fun observeSelectedCollection(uid: String, id: String) {
        collectionObservation = viewModelScope.launch {
            try {
                (observeCollectionWords?.invoke(uid, id) ?: flowOf(emptyList())).collect { words ->
                    if (owner == uid && uiState.collectionId == id) {
                        collectionLoading = false
                        uiState = uiState.copy(collectionWords = words, loading = !baseReady)
                    }
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (owner == uid && uiState.collectionId == id) {
                    collectionLoading = false
                    uiState = uiState.copy(loading = false, error = true)
                }
            }
        }
    }
    fun setShuffled(value: Boolean) { if (!uiState.busy) uiState = uiState.copy(shuffled = value) }
    fun setMeaningFirst(value: Boolean) { if (!uiState.busy) uiState = uiState.copy(meaningFirst = value) }

    fun start(difficultOnly: Boolean = false) {
        val userId = owner ?: return
        if (uiState.busy || uiState.loading || uiState.error || uiState.stage == FlashcardStage.Studying) return
        val words = if (difficultOnly) uiState.cards.filterIndexed { index, _ ->
            uiState.ratings.getOrNull(index) in setOf(FlashcardRating.AGAIN, FlashcardRating.HARD)
        }.filter { card -> uiState.availableWords.any { it.id == card.id } } else uiState.deck
        if (words.isEmpty()) { prepare(uiState.collectionId); return }
        val cards = if (uiState.shuffled) words.shuffled() else words.toList()
        val id = UUID.randomUUID().toString()
        val at = now()
        uiState = uiState.copy(busy = true, error = false)
        operation = viewModelScope.launch {
            try {
                startFlashcards(userId, id, at)
                if (owner != userId) return@launch
                sessionId = id
                startedAt = at
                activeMs = 0
                cardStarted = monotonic()
                resumedAt = if (foreground) cardStarted else null
                pendingReview = null
                uiState = uiState.copy(stage = FlashcardStage.Studying, cards = cards, index = 0,
                    ratings = emptyList(), revealed = false, busy = false)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (owner == userId) uiState = uiState.copy(busy = false, error = true) }
        }
    }

    fun reveal() {
        if (uiState.stage == FlashcardStage.Studying && !uiState.busy) uiState = uiState.copy(revealed = true)
    }

    fun rate(rating: FlashcardRating) {
        val userId = owner ?: return
        val id = sessionId ?: return
        val card = uiState.current ?: return
        if (uiState.stage != FlashcardStage.Studying || !uiState.revealed || uiState.busy) return
        val last = uiState.index == uiState.cards.lastIndex
        val review = pendingReview ?: FlashcardReview(id, card.id, rating, now().coerceAtLeast(startedAt),
            (monotonic() - cardStarted).coerceAtLeast(0), elapsedActive(), last)
        pendingReview = review
        uiState = uiState.copy(busy = true, error = false)
        operation = viewModelScope.launch {
            try {
                rateFlashcard(userId, review)
                if (owner != userId) return@launch
                pendingReview = null
                cardStarted = monotonic()
                uiState = uiState.copy(busy = false, ratings = uiState.ratings + review.rating,
                    stage = if (last) FlashcardStage.Completed else FlashcardStage.Studying,
                    index = if (last) uiState.index else uiState.index + 1, revealed = false)
                if (last) { sessionId = null; resumedAt = null }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (owner == userId) uiState = uiState.copy(busy = false, error = true) }
        }
    }

    fun retry() {
        val pending = pendingReview
        if (pending != null) rate(pending.rating)
        else owner?.let {
            uiState = uiState.copy(loading = true, error = false)
            baseReady = false
            observeWords(it)
            uiState.collectionId?.let { id -> collectionObservation?.cancel(); collectionLoading = true; observeSelectedCollection(it, id) }
        }
    }

    fun setForeground(value: Boolean) {
        if (foreground == value) return
        if (!value) { activeMs = elapsedActive(); resumedAt = null }
        foreground = value
        if (value && sessionId != null) resumedAt = monotonic()
    }

    private fun elapsedActive() = activeMs + (resumedAt?.let { (monotonic() - it).coerceAtLeast(0) } ?: 0)

    fun requestExit(onExit: () -> Unit) {
        if (uiState.busy) return
        if (uiState.stage == FlashcardStage.Studying) uiState = uiState.copy(showExit = true)
        else onExit()
    }
    fun dismissExit() { if (!uiState.busy) uiState = uiState.copy(showExit = false) }

    fun confirmExit(onExit: () -> Unit) {
        val userId = owner ?: return
        val id = sessionId ?: return
        if (uiState.busy) return
        uiState = uiState.copy(busy = true, error = false)
        operation = viewModelScope.launch {
            try {
                abandonFlashcards(userId, id, now().coerceAtLeast(startedAt), elapsedActive())
                if (owner != userId) return@launch
                sessionId = null
                pendingReview = null
                resumedAt = null
                uiState = uiState.copy(stage = FlashcardStage.Setup, busy = false, showExit = false)
                onExit()
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (owner == userId) uiState = uiState.copy(busy = false, error = true, showExit = false) }
        }
    }
}
