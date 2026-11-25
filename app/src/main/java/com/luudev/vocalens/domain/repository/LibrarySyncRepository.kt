package com.luudev.vocalens.domain.repository

import kotlinx.coroutines.flow.Flow
import com.luudev.vocalens.domain.model.LibrarySyncStatus

interface LibrarySyncRepository {
    suspend fun synchronize(userId: String)
    fun requestSync(userId: String)
    fun observeStatus(userId: String): Flow<LibrarySyncStatus>
}
