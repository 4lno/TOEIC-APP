package com.luudev.vocalens.presentation.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.luudev.vocalens.domain.model.UserProfile
import com.luudev.vocalens.domain.usecase.GetProfile
import com.luudev.vocalens.domain.usecase.ProfileValidation
import com.luudev.vocalens.domain.usecase.SaveProfile

enum class ProfileDialog { EditInfo, EditGoal, Statistics, Settings, ReminderInfo, Logout }

data class ProfileUiState(
    val profile: UserProfile,
    val dialog: ProfileDialog? = null,
    val draftName: String = "",
    val draftEmail: String = "",
    val draftGoal: Int = 25,
    val validation: ProfileValidation = ProfileValidation(false, false, false)
)

class ProfileViewModel(private val getProfile: GetProfile, private val saveProfile: SaveProfile) : ViewModel() {
    var uiState by mutableStateOf(ProfileUiState(getProfile()))
        private set

    fun onOpenDialog(dialog: ProfileDialog) {
        uiState = uiState.copy(dialog = dialog, draftName = uiState.profile.fullName,
            draftEmail = uiState.profile.email, draftGoal = uiState.profile.dailyGoal,
            validation = ProfileValidation(false, false, false))
    }

    fun onDismissDialog() { uiState = uiState.copy(dialog = null) }
    fun onNameChange(name: String) {
        uiState = uiState.copy(draftName = name, validation = uiState.validation.copy(invalidName = false))
    }
    fun onEmailChange(email: String) {
        uiState = uiState.copy(draftEmail = email, validation = uiState.validation.copy(invalidEmail = false))
    }
    fun onGoalSelected(goal: Int) {
        uiState = uiState.copy(draftGoal = goal, validation = uiState.validation.copy(invalidGoal = false))
    }

    fun onSaveInfo() {
        val result = saveProfile(uiState.profile.copy(fullName = uiState.draftName, email = uiState.draftEmail))
        uiState = uiState.copy(validation = result)
        if (result.isValid) refreshProfile(closeDialog = true)
    }

    fun onSaveGoal() {
        val result = saveProfile(uiState.profile.copy(dailyGoal = uiState.draftGoal))
        uiState = uiState.copy(validation = result)
        if (result.isValid) refreshProfile(closeDialog = true)
    }

    fun onReminderChanged(enabled: Boolean) {
        if (saveProfile(uiState.profile.copy(reminderEnabled = enabled)).isValid) refreshProfile()
    }

    fun onSoundChanged(enabled: Boolean) {
        if (saveProfile(uiState.profile.copy(soundEnabled = enabled)).isValid) refreshProfile()
    }

    private fun refreshProfile(closeDialog: Boolean = false) {
        uiState = uiState.copy(profile = getProfile(), dialog = if (closeDialog) null else uiState.dialog)
    }
}
