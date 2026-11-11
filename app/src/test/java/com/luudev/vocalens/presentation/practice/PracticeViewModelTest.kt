package com.luudev.vocalens.presentation.practice

import androidx.lifecycle.SavedStateHandle
import com.luudev.vocalens.data.repository.DemoPracticeRepositoryImpl
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.usecase.GetPracticeLessons
import org.junit.Assert.*
import org.junit.Test

class PracticeViewModelTest {
    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        PracticeViewModel(handle, GetPracticeLessons(DemoPracticeRepositoryImpl()))

    @Test
    fun featuredLessonIsShownOnceAboveNewestVideos() {
        val state = viewModel().uiState
        assertEquals("small-talk", state.featuredLesson?.id)
        assertEquals(listOf("cafe", "workday", "toeic-conversations"), state.lessons.map { it.id })
        assertTrue(state.lessons.all { !it.isFeatured && it.mediaType == PracticeMediaType.Video })
    }

    @Test
    fun switchingMediaUpdatesFeaturedAndList() {
        val vm = viewModel()
        vm.onMediaTypeSelected(PracticeMediaType.Podcast)
        assertEquals("daily-podcast", vm.uiState.featuredLesson?.id)
        assertTrue(vm.uiState.lessons.all { it.mediaType == PracticeMediaType.Podcast })
    }

    @Test
    fun restoresAllSearchControlsAfterRecreation() {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.onMediaTypeSelected(PracticeMediaType.Podcast)
        vm.onQueryChange("TOEIC")
        vm.onFilterSelected(PracticeFilter.Toeic)
        vm.onDurationSelected(PracticeDuration.Long)
        vm.onToggleSort()
        val restored = viewModel(handle).uiState
        assertEquals(vm.uiState, restored)
        assertEquals(listOf("toeic-podcast"), restored.lessons.map { it.id })
        assertNull(restored.featuredLesson)
    }

    @Test
    fun emptySearchDoesNotLeaveUnrelatedFeaturedLesson() {
        val vm = viewModel()
        vm.onQueryChange("does not exist")
        assertNull(vm.uiState.featuredLesson)
        assertTrue(vm.uiState.lessons.isEmpty())
    }

    @Test
    fun detailsAndDownloadsCanBeDismissedWithoutChangingProgress() {
        val vm = viewModel()
        val original = vm.uiState.lessons
        vm.onLessonClick(original.first())
        assertEquals(original.first(), vm.uiState.selectedLesson)
        vm.onDismissLesson()
        vm.onDownloadsClick()
        assertTrue(vm.uiState.showDownloads)
        vm.onDismissDownloads()
        assertNull(vm.uiState.selectedLesson)
        assertFalse(vm.uiState.showDownloads)
        assertEquals(original, vm.uiState.lessons)
    }
}
