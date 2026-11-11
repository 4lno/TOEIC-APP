package com.luudev.vocalens.data.repository

import android.content.SharedPreferences
import com.luudev.vocalens.domain.model.UserProfile
import com.luudev.vocalens.domain.repository.ProfileRepository

/** Locally editable demo profile; learning statistics are sample data. */
class LocalProfileRepositoryImpl(private val preferences: SharedPreferences) : ProfileRepository {
    override fun getProfile() = UserProfile(
        fullName = preferences.getString("full_name", "Nguyễn Minh Anh") ?: "Nguyễn Minh Anh",
        email = preferences.getString("email", "minhanh@email.com") ?: "minhanh@email.com",
        dailyGoal = preferences.getInt("daily_goal", 25),
        reminderEnabled = preferences.getBoolean("reminder_enabled", true),
        soundEnabled = preferences.getBoolean("sound_enabled", true)
    )

    override fun saveProfile(profile: UserProfile) {
        preferences.edit().putString("full_name", profile.fullName).putString("email", profile.email)
            .putInt("daily_goal", profile.dailyGoal).putBoolean("reminder_enabled", profile.reminderEnabled)
            .putBoolean("sound_enabled", profile.soundEnabled).apply()
    }
}
