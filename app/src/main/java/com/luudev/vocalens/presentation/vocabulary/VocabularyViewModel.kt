package com.luudev.vocalens.presentation.vocabulary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.luudev.vocalens.domain.model.VocabularyCategory
import com.luudev.vocalens.domain.model.VocabularySort
import com.luudev.vocalens.domain.model.VocabularyWord
import com.luudev.vocalens.domain.usecase.DeleteVocabularyWord
import com.luudev.vocalens.domain.usecase.FilterVocabulary
import com.luudev.vocalens.domain.usecase.GetSavedVocabulary

data class VocabularyUiState(
    val savedWords: List<VocabularyWord> = emptyList(),
    val visibleWords: List<VocabularyWord> = emptyList(),
    val query: String = "",
    val category: VocabularyCategory? = null,
    val sort: VocabularySort = VocabularySort.RecentlySaved,
    val pendingDeletion: VocabularyWord? = null,
    val reviewWords: List<VocabularyWord> = emptyList(),
    val reviewIndex: Int = 0,
    val meaningRevealed: Boolean = false
)

class VocabularyViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getSavedVocabulary: GetSavedVocabulary,
    private val deleteVocabularyWord: DeleteVocabularyWord,
    private val filterVocabulary: FilterVocabulary = FilterVocabulary()
) : ViewModel() {
    var uiState by mutableStateOf(VocabularyUiState(
        savedWords = getSavedVocabulary(),
        query = savedStateHandle[QUERY] ?: "",
        category = savedStateHandle.get<String>(CATEGORY)?.let { name -> VocabularyCategory.entries.find { it.name == name } },
        sort = savedStateHandle.get<String>(SORT)?.let { name -> VocabularySort.entries.find { it.name == name } }
            ?: VocabularySort.RecentlySaved
    ))
        private set

    init { updateVisibleWords() }

    fun onQueryChange(query: String) {
        savedStateHandle[QUERY] = query
        uiState = uiState.copy(query = query)
        updateVisibleWords()
    }

    fun onCategorySelected(category: VocabularyCategory?) {
        savedStateHandle[CATEGORY] = category?.name
        uiState = uiState.copy(category = category)
        updateVisibleWords()
    }

    fun onSortSelected(sort: VocabularySort) {
        savedStateHandle[SORT] = sort.name
        uiState = uiState.copy(sort = sort)
        updateVisibleWords()
    }

    fun onDeleteRequested(word: VocabularyWord) { uiState = uiState.copy(pendingDeletion = word) }
    fun onDeleteDismissed() { uiState = uiState.copy(pendingDeletion = null) }

    fun onDeleteConfirmed() {
        val word = uiState.pendingDeletion ?: return
        deleteVocabularyWord(word.id)
        uiState = uiState.copy(savedWords = getSavedVocabulary(), pendingDeletion = null)
        updateVisibleWords()
    }

    fun onStartReview() {
        uiState = uiState.copy(reviewWords = uiState.visibleWords.toList(), reviewIndex = 0, meaningRevealed = false)
    }

    fun onRevealMeaning() { uiState = uiState.copy(meaningRevealed = true) }

    fun onNextReviewWord() {
        if (uiState.reviewIndex + 1 >= uiState.reviewWords.size) onDismissReview()
        else uiState = uiState.copy(reviewIndex = uiState.reviewIndex + 1, meaningRevealed = false)
    }

    fun onDismissReview() { uiState = uiState.copy(reviewWords = emptyList(), reviewIndex = 0, meaningRevealed = false) }

    private fun updateVisibleWords() {
        uiState = uiState.copy(visibleWords = filterVocabulary(uiState.savedWords, uiState.query, uiState.category, uiState.sort))
    }

    private companion object {
        const val QUERY = "vocabulary_query"
        const val CATEGORY = "vocabulary_category"
        const val SORT = "vocabulary_sort"
    }
}
