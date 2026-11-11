package com.luudev.vocalens.presentation.vocabulary

import androidx.lifecycle.SavedStateHandle
import com.luudev.vocalens.domain.model.VocabularyCategory
import com.luudev.vocalens.domain.model.VocabularySort
import com.luudev.vocalens.domain.model.VocabularyWord
import com.luudev.vocalens.domain.repository.VocabularyRepository
import com.luudev.vocalens.domain.usecase.DeleteVocabularyWord
import com.luudev.vocalens.domain.usecase.GetSavedVocabulary
import com.luudev.vocalens.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomVocabularyViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val headphones = VocabularyWord("headphones", "Headphones", "", "Tai nghe", VocabularyCategory.Technology)
    private val notebook = VocabularyWord("notebook", "Notebook", "", "Sổ ghi chép", VocabularyCategory.Objects)

    private class FakeRepository : VocabularyRepository {
        val users = mutableMapOf<String, MutableStateFlow<List<VocabularyWord>>>()
        var failDeletion = false
        fun words(uid: String) = users.getOrPut(uid) { MutableStateFlow(emptyList()) }
        override fun observeSavedWords(userId: String): Flow<List<VocabularyWord>> = words(userId)
        override suspend fun saveWord(userId: String, word: VocabularyWord) {
            words(userId).value = words(userId).value.filterNot { it.id == word.id } + word
        }
        override suspend fun deleteWord(userId: String, wordId: String) {
            if (failDeletion) error("Disk unavailable")
            words(userId).value = words(userId).value.filterNot { it.id == wordId }
        }
    }

    private fun repository() = FakeRepository().apply { words("A").value = listOf(headphones, notebook) }
    private fun viewModel(repo: FakeRepository, handle: SavedStateHandle = SavedStateHandle()) =
        VocabularyViewModel(handle, GetSavedVocabulary(repo), DeleteVocabularyWord(repo)).apply { onUserChanged("A") }

    @Test fun cancelingDeletionKeepsWords() = runTest {
        val repo = repository()
        val vm = viewModel(repo)
        runCurrent()
        vm.onDeleteRequested(headphones)
        vm.onDeleteDismissed()
        assertEquals(2, repo.words("A").value.size)
        assertNull(vm.uiState.pendingDeletion)
    }

    @Test fun deletionRefreshesFilteredListAndSurvivesViewModelRecreation() = runTest {
        val repo = repository()
        val vm = viewModel(repo)
        runCurrent()
        vm.onCategorySelected(VocabularyCategory.Technology)
        vm.onDeleteRequested(headphones)
        vm.onDeleteConfirmed()
        runCurrent()
        assertTrue(vm.uiState.visibleWords.isEmpty())
        vm.onCategorySelected(null)
        assertEquals(listOf(notebook), vm.uiState.visibleWords)
        val restored = viewModel(repo)
        runCurrent()
        assertEquals(listOf(notebook), restored.uiState.savedWords)
    }

    @Test fun restoresFiltersForSameOwner() = runTest {
        val repo = repository()
        val handle = SavedStateHandle()
        val vm = viewModel(repo, handle)
        runCurrent()
        vm.onQueryChange("so ghi")
        vm.onCategorySelected(VocabularyCategory.Objects)
        vm.onSortSelected(VocabularySort.Alphabetical)
        val restored = viewModel(repo, handle)
        runCurrent()
        assertEquals("so ghi", restored.uiState.query)
        assertEquals(VocabularyCategory.Objects, restored.uiState.category)
        assertEquals(VocabularySort.Alphabetical, restored.uiState.sort)
        assertEquals(listOf(notebook), restored.uiState.visibleWords)
    }

    @Test fun reviewUsesFilteredWordsAndFinishesAfterLastCard() = runTest {
        val vm = viewModel(repository())
        runCurrent()
        vm.onCategorySelected(VocabularyCategory.Objects)
        vm.onStartReview()
        assertEquals(listOf(notebook), vm.uiState.reviewWords)
        vm.onRevealMeaning()
        assertTrue(vm.uiState.meaningRevealed)
        vm.onNextReviewWord()
        assertTrue(vm.uiState.reviewWords.isEmpty())
    }

    @Test fun advancingReviewHidesMeaningAndCancelClosesSession() = runTest {
        val vm = viewModel(repository())
        runCurrent()
        vm.onStartReview()
        vm.onRevealMeaning()
        vm.onNextReviewWord()
        assertEquals(1, vm.uiState.reviewIndex)
        assertFalse(vm.uiState.meaningRevealed)
        vm.onDismissReview()
        assertTrue(vm.uiState.reviewWords.isEmpty())
    }

    @Test fun deletingLastWordLeavesEmptyBank() = runTest {
        val repo = repository().apply { words("A").value = listOf(headphones) }
        val vm = viewModel(repo)
        runCurrent()
        vm.onDeleteRequested(headphones)
        vm.onDeleteConfirmed()
        runCurrent()
        assertTrue(vm.uiState.savedWords.isEmpty())
    }

    @Test fun switchingAccountsImmediatelyClearsOldWordsReviewAndFilters() = runTest {
        val repo = repository().apply { words("B").value = listOf(notebook) }
        val vm = viewModel(repo)
        runCurrent()
        vm.onQueryChange("head")
        vm.onStartReview()
        vm.onUserChanged("B")
        assertTrue(vm.uiState.savedWords.isEmpty())
        assertTrue(vm.uiState.reviewWords.isEmpty())
        assertEquals("", vm.uiState.query)
        runCurrent()
        assertEquals(listOf(notebook), vm.uiState.savedWords)
        repo.words("A").value = emptyList()
        runCurrent()
        assertEquals(listOf(notebook), vm.uiState.savedWords)
        vm.onUserChanged(null)
        assertTrue(vm.uiState.savedWords.isEmpty())
        repo.words("B").value = listOf(headphones)
        runCurrent()
        assertTrue(vm.uiState.savedWords.isEmpty())
    }

    @Test fun failedDeletionKeepsWordAndShowsStorageError() = runTest {
        val repo = repository().apply { failDeletion = true }
        val vm = viewModel(repo)
        runCurrent()
        vm.onDeleteRequested(headphones)
        vm.onDeleteConfirmed()
        runCurrent()
        assertTrue(vm.uiState.storageError)
        assertEquals(2, vm.uiState.savedWords.size)
    }
}
