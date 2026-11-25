package com.luudev.vocalens.data.repository

import com.google.firebase.functions.FirebaseFunctionsException
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.remote.FirebaseServices
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.repository.XpSyncRepository
import kotlinx.coroutines.tasks.await

class FirebaseXpSyncRepository(private val database: VocabLensDatabase) : XpSyncRepository {
    override suspend fun pending(userId: String) = database.xpSyncDao().pending(userId).map { item ->
        val session = requireNotNull(database.studyDao().session(userId, item.sessionId))
        CompletedFlashcardSession(session.sessionId, session.startedAt, requireNotNull(session.endedAt),
            session.activeDurationMs, database.studyDao().sessionReviews(userId, item.sessionId).map {
                CompletedFlashcardReview(it.vocabularyId, FlashcardRating.valueOf(requireNotNull(it.rating)), it.reviewedAt)
            })
    }

    override suspend fun submit(userId: String, session: CompletedFlashcardSession) {
        check(FirebaseServices.auth().currentUser?.uid == userId) { "Account changed" }
        try {
            FirebaseServices.functions().getHttpsCallable("submitFlashcardSession").call(mapOf(
                "ownerId" to userId,
                "session" to mapOf(
                    "sessionId" to session.sessionId, "startedAt" to session.startedAt,
                    "endedAt" to session.endedAt, "activeDurationMs" to session.activeDurationMs,
                    "reviews" to session.reviews.map { mapOf(
                        "wordId" to it.wordId, "rating" to it.rating.name, "reviewedAt" to it.reviewedAt) }
                )
            )).await()
        } catch (error: FirebaseFunctionsException) {
            check(FirebaseServices.auth().currentUser?.uid == userId) { "Account changed" }
            if (error.code in setOf(FirebaseFunctionsException.Code.INVALID_ARGUMENT,
                    FirebaseFunctionsException.Code.ALREADY_EXISTS, FirebaseFunctionsException.Code.FAILED_PRECONDITION)) {
                throw XpSubmissionRejected(error.code.name)
            }
            throw error
        }
    }

    override suspend fun markSynced(userId: String, sessionId: String) =
        database.xpSyncDao().mark(userId, sessionId, "SYNCED")

    override suspend fun markRejected(userId: String, sessionId: String, code: String) =
        database.xpSyncDao().mark(userId, sessionId, "REJECTED", code)
}
