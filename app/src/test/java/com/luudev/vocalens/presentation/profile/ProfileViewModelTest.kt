package com.luudev.vocalens.presentation.profile

import com.luudev.vocalens.domain.model.UserProfile
import com.luudev.vocalens.domain.repository.ProfileRepository
import com.luudev.vocalens.domain.usecase.GetProfile
import com.luudev.vocalens.domain.usecase.SaveProfile
import org.junit.Assert.*
import org.junit.Test

class ProfileViewModelTest {
    private class FakeRepository : ProfileRepository {
        private var current = UserProfile("Nguyễn Minh Anh", "minhanh@email.com")
        override fun getProfile() = current
        override fun saveProfile(profile: UserProfile) { current = profile }
    }
    private fun viewModel(repository: ProfileRepository) = ProfileViewModel(GetProfile(repository), SaveProfile(repository))

    @Test
    fun cancelDiscardsUnsavedDraftAndReopeningLoadsSavedProfile() {
        val repository = FakeRepository()
        val vm = viewModel(repository)
        vm.onOpenDialog(ProfileDialog.EditInfo)
        vm.onNameChange("Tên chưa lưu")
        vm.onDismissDialog()
        vm.onOpenDialog(ProfileDialog.EditInfo)
        assertEquals("Nguyễn Minh Anh", vm.uiState.draftName)
        assertEquals("Nguyễn Minh Anh", repository.getProfile().fullName)
    }

    @Test
    fun invalidProfileKeepsEditorOpenAndDoesNotChangeSavedData() {
        val repository = FakeRepository()
        val vm = viewModel(repository)
        vm.onOpenDialog(ProfileDialog.EditInfo)
        vm.onEmailChange("invalid")
        vm.onSaveInfo()
        assertEquals(ProfileDialog.EditInfo, vm.uiState.dialog)
        assertTrue(vm.uiState.validation.invalidEmail)
        assertEquals("minhanh@email.com", repository.getProfile().email)
    }

    @Test
    fun savedGoalSurvivesViewModelRecreationAndUpdatesProgress() {
        val repository = FakeRepository()
        val vm = viewModel(repository)
        vm.onOpenDialog(ProfileDialog.EditGoal)
        vm.onGoalSelected(50)
        vm.onSaveGoal()
        assertNull(vm.uiState.dialog)
        val restored = viewModel(repository)
        assertEquals(50, restored.uiState.profile.dailyGoal)
        assertEquals(0.34f, restored.uiState.profile.dailyProgress, 0.001f)
    }

    @Test
    fun togglesPersistWithoutLosingProfileEdits() {
        val repository = FakeRepository()
        val vm = viewModel(repository)
        vm.onOpenDialog(ProfileDialog.EditInfo)
        vm.onNameChange("Trần Bảo An")
        vm.onSaveInfo()
        vm.onSoundChanged(false)
        vm.onReminderChanged(false)
        val restored = viewModel(repository).uiState.profile
        assertFalse(restored.soundEnabled)
        assertFalse(restored.reminderEnabled)
        assertEquals("Trần Bảo An", restored.fullName)
        assertEquals("BA", restored.initials)
    }
}
