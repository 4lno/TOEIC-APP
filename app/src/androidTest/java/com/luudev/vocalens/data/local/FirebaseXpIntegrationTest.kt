package com.luudev.vocalens.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.Source
import com.luudev.vocalens.BuildConfig
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.local.entity.UserEntity
import com.luudev.vocalens.data.remote.FirebaseServices
import com.luudev.vocalens.data.repository.*
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.usecase.SyncPendingXp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FirebaseXpIntegrationTest {
    @Test fun completedRoomSessionIsConfirmedByCallableAndRetryDoesNotAddXpAgain() = runBlocking {
        // This test must never contact production Firebase.
        assumeTrue(BuildConfig.DEBUG && BuildConfig.FIREBASE_EMULATOR_HOST.isNotBlank())
        withTimeout(30_000) {
            val auth = FirebaseServices.auth()
            val user = auth.createUserWithEmailAndPassword("xp-${UUID.randomUUID()}@example.com", "Test-password-123").await().user!!
            val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VocabLensDatabase::class.java).build()
            try {
                val at = System.currentTimeMillis() - 2000
                db.userDao().synchronize(UserEntity(user.uid, user.email!!, "Test user", createdAt = at, updatedAt = at))
                LocalVocabularyRepositoryImpl(db.vocabularyDao()).saveWord(user.uid,
                    VocabularyWord("book", "Book", "", "Sách", VocabularyCategory.Objects))
                val local = LocalFlashcardRepositoryImpl(db)
                val id = UUID.randomUUID().toString()
                local.start(user.uid, id, at)
                local.review(user.uid, FlashcardReview(id, "book", FlashcardRating.AGAIN, at + 1000, 1000, 1000, true))
                assertEquals(1, db.xpSyncDao().pending(user.uid).size)
                val remote = FirebaseXpSyncRepository(db)
                val payload = remote.pending(user.uid).single()
                SyncPendingXp(remote)(user.uid)
                assertTrue(db.xpSyncDao().pending(user.uid).isEmpty())
                remote.submit(user.uid, payload)
                val ranking = FirebaseServices.firestore().collection("leaderboard").document(user.uid).get(Source.SERVER).await()
                assertEquals(5L, ranking.getLong("totalXp"))
                assertEquals(5, db.studyDao().session(user.uid, id)!!.earnedXp)
            } finally {
                db.close()
                auth.signOut()
            }
        }
    }
}
