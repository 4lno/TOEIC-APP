package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.UserProfile

interface ProfileRepository {
    fun getProfile(): UserProfile
    fun saveProfile(profile: UserProfile)
}
