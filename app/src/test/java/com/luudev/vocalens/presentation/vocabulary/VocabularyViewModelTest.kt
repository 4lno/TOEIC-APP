package com.luudev.vocalens.presentation.vocabulary

import androidx.lifecycle.SavedStateHandle
import com.luudev.vocalens.domain.model.VocabularyCategory
import com.luudev.vocalens.domain.model.VocabularySort
import com.luudev.vocalens.domain.model.VocabularyWord
import com.luudev.vocalens.domain.repository.VocabularyRepository
import com.luudev.vocalens.domain.usecase.DeleteVocabularyWord
import com.luudev.vocalens.domain.usecase.GetSavedVocabulary
import org.junit.Assert.*
import org.junit.Test

class VocabularyViewModelTest {
    private val headphones = VocabularyWord("headphones", "Headphones", "", "Tai nghe", VocabularyCategory.Technology)
    private val notebook = VocabularyWord("notebook", "Notebook", "", "Sổ ghi chép", VocabularyCategory.Objects)

    private class FakeRepository(initialWords: List<VocabularyWord>) : VocabularyRepository {
        private var words = initialWords
        override fun getSavedWords() = words
        override fun deleteWord(id: String) { words = words.filterNot { it.id == id } }
    }

    private fun viewModel(repository: VocabularyRepository, handle: SavedStateHandle = SavedStateHandle()) =
        VocabularyViewModel(handle, GetSavedVocabulary(repository), DeleteVocabularyWord(repository))

    @Test
    fun cancelingDeletionDoesNotChangeRepository() {
        val repository = FakeRepository(listOf(headphones, notebook))
        val vm = viewModel(repository)
        vm.onDeleteRequested(headphones)
        vm.onDeleteDismissed()
        assertEquals(2, repository.getSavedWords().size)
        assertNull(vm.uiState.pendingDeletion)
    }

    @Test
    fun deletionRefreshesFilteredListAndSurvivesViewModelRecreation() {
        val repository = FakeRepository(listOf(headphones, notebook))
        val vm = viewModel(repository)
        vm.onCategorySelected(VocabularyCategory.Technology)
        vm.onDeleteRequested(headphones)
        vm.onDeleteConfirmed()
        assertTrue(vm.uiState.visibleWords.isEmpty())
        vm.onCategorySelected(null)
        assertEquals(listOf(notebook), vm.uiState.visibleWords)
        assertEquals(listOf(notebook), viewModel(repository).uiState.savedWords)
    }

    @Test
    fun restoresSearchCategoryAndSort() {
        val repository = FakeRepository(listOf(headphones, notebook))
        val handle = SavedStateHandle()
        val vm = viewModel(repository, handle)
        vm.onQueryChange("so ghi")
        vm.onCategorySelected(VocabularyCategory.Objects)
        vm.onSortSelected(VocabularySort.Alphabetical)
        val restored = viewModel(repository, handle).uiState
        assertEquals("so ghi", restored.query)
        assertEquals(VocabularyCategory.Objects, restored.category)
        assertEquals(VocabularySort.Alphabetical, restored.sort)
        assertEquals(listOf(notebook), restored.visibleWords)
    }

    @Test
    fun reviewUsesFilteredWordsAndFinishesAfterLastCard() {
        val vm = viewModel(FakeRepository(listOf(headphones, notebook)))
        vm.onCategorySelected(VocabularyCategory.Objects)
        vm.onStartReview()
        assertEquals(listOf(notebook), vm.uiState.reviewWords)
        assertFalse(vm.uiState.meaningRevealed)
        vm.onRevealMeaning()
        assertTrue(vm.uiState.meaningRevealed)
        vm.onNextReviewWord()
        assertTrue(vm.uiState.reviewWords.isEmpty())
    }

    @Test
    fun advancingReviewHidesNextMeaningAndCancelClosesSession() {
        val vm = viewModel(FakeRepository(listOf(headphones, notebook)))
        vm.onStartReview()
        vm.onRevealMeaning()
        vm.onNextReviewWord()
        assertEquals(1, vm.uiState.reviewIndex)
        assertFalse(vm.uiState.meaningRevealed)
        vm.onDismissReview()
        assertTrue(vm.uiState.reviewWords.isEmpty())
    }

    @Test
    fun deletingLastWordLeavesEmptyBank() {
        val vm = viewModel(FakeRepository(listOf(headphones)))
        vm.onDeleteRequested(headphones)
        vm.onDeleteConfirmed()
        assertTrue(vm.uiState.savedWords.isEmpty())
        vm.onStartReview()
        assertTrue(vm.uiState.reviewWords.isEmpty())
    }
}
