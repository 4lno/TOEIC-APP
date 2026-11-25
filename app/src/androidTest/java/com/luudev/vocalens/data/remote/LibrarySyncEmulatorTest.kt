package com.luudev.vocalens.data.remote

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luudev.vocalens.data.local.database.LibrarySyncSchema
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.local.entity.*
import com.luudev.vocalens.data.repository.FirebaseLibrarySyncRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Explicit demo Firebase apps; this test cannot address the production project. */
class LibrarySyncEmulatorTest {
    @Test fun restoresWordsCollectionsAndMembershipsAcrossDevicesThenPropagatesRenameAndDeletion() = runBlocking {
        withTimeout(90_000) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val options = FirebaseOptions.Builder().setApplicationId("1:123:android:test")
                .setApiKey("demo-key").setProjectId("demo-vocablens").build()
            val app = FirebaseApp.initializeApp(context, options, "library-${UUID.randomUUID()}")!!
            val auth = FirebaseAuth.getInstance(app).apply { useEmulator("10.0.2.2", 9099) }
            val cloud = FirebaseFirestore.getInstance(app).apply { useEmulator("10.0.2.2", 8080) }
            val a = Room.inMemoryDatabaseBuilder(context, VocabLensDatabase::class.java).addCallback(LibrarySyncSchema).build()
            val b = Room.inMemoryDatabaseBuilder(context, VocabLensDatabase::class.java).addCallback(LibrarySyncSchema).build()
            try {
                val user = auth.signInAnonymously().await().user!!
                val uid = user.uid
                for (db in listOf(a, b)) db.userDao().synchronize(UserEntity(uid, "local@example.com", "Test", createdAt = 1, updatedAt = 1))
                var activeUid: String? = uid
                val repoA = FirebaseLibrarySyncRepository(a, cloud, { activeUid })
                val repoB = FirebaseLibrarySyncRepository(b, cloud, { auth.currentUser?.uid })
                // Simulate pre-sync legacy data with no outbox entries and a nullable category.
                a.librarySyncDao().saveCursor(LibrarySyncCursor(uid, "control", importing = true))
                a.vocabularyDao().saveWord(uid, VocabularyEntity("fish", "fish", "/fɪʃ/", "cá", createdAt = 1), 1)
                // More than a page verifies upload batching and timestamp pagination.
                repeat(101) { index ->
                    a.vocabularyDao().saveWord(uid, VocabularyEntity("word-$index", "word $index", "", "nghĩa $index", createdAt = 1), 1)
                }
                a.collectionDao().create(CollectionEntity(uid, "animals", "Animals", createdAt = 1, updatedAt = 1))
                a.collectionDao().addItem(CollectionItemEntity(uid, "animals", "fish", 1))
                a.librarySyncDao().saveCursor(LibrarySyncCursor(uid, "control"))
                assertTrue(a.librarySyncDao().pending(uid).isEmpty())
                repoA.synchronize(uid)
                repoB.synchronize(uid)
                assertEquals(102, b.vocabularyDao().observeWords(uid).first().size)
                assertEquals("cá", b.vocabularyDao().observeWords(uid).first().single { it.vocabularyId == "fish" }.meaningVi)
                assertEquals("fish", b.collectionDao().observeWords(uid, "animals").first().single().english)
                assertTrue(b.librarySyncDao().pending(uid).isEmpty()) // Imported data must not echo back.
                b.collectionDao().update(uid, "animals", "Sea animals", "", 2)
                repoB.synchronize(uid)
                repoA.synchronize(uid)
                assertEquals("Sea animals", a.collectionDao().observeCollections(uid).first().single().name)
                a.collectionDao().update(uid, "animals", "Offline edit A", "", 3)
                b.collectionDao().update(uid, "animals", "Offline edit B", "", 4)
                repoB.synchronize(uid)
                repoA.synchronize(uid)
                repoB.synchronize(uid)
                assertEquals("Offline edit A", b.collectionDao().observeCollections(uid).first().single().name)
                activeUid = "different-account"
                try {
                    repoA.synchronize(uid)
                    fail("A switched account must not synchronize the previous library")
                } catch (_: IllegalStateException) { }
                activeUid = uid
                a.vocabularyDao().deleteWord(uid, "fish")
                a.collectionDao().delete(uid, "animals")
                repoA.synchronize(uid)
                repoB.synchronize(uid)
                assertFalse(b.vocabularyDao().observeWords(uid).first().any { it.vocabularyId == "fish" })
                assertTrue(b.collectionDao().observeCollections(uid).first().isEmpty())
                assertTrue(b.librarySyncDao().pending(uid).isEmpty())
                repoB.synchronize(uid) // Replaying the inclusive cursor must not resurrect deleted data.
                assertEquals(101, b.vocabularyDao().observeWords(uid).first().size)
                assertTrue(cloud.collection("users").document(uid).collection("vocabulary").document("fish").get().await().getBoolean("deleted")!!)
            } finally {
                auth.currentUser?.delete()?.await()
                a.close(); b.close()
                cloud.terminate().await()
                app.delete()
            }
        }
    }
}
