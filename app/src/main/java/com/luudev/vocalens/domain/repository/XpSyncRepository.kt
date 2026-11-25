package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.CompletedFlashcardSession

interface XpSyncRepository {
    suspend fun pending(userId: String): List<CompletedFlashcardSession>
    suspend fun submit(userId: String, session: CompletedFlashcardSession)
    suspend fun markSynced(userId: String, sessionId: String)
    suspend fun markRejected(userId: String, sessionId: String, code: String)
}
