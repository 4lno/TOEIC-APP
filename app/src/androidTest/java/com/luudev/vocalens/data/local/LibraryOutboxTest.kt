package com.luudev.vocalens.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.luudev.vocalens.data.local.database.LibrarySyncSchema
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.local.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LibraryOutboxTest {
    @Test fun mutationsAndCascadeDeletesAreAtomicAndOldAcknowledgementKeepsNewEdit() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VocabLensDatabase::class.java).addCallback(LibrarySyncSchema).build()
        try {
            db.userDao().synchronize(UserEntity("A", "a@example.com", "An", createdAt = 1, updatedAt = 1))
            db.vocabularyDao().saveWord("A", VocabularyEntity("fish", "fish", "", "cá", createdAt = 1), 1)
            db.collectionDao().create(CollectionEntity("A", "animals", "Animals", createdAt = 1, updatedAt = 1))
            db.collectionDao().addItem(CollectionItemEntity("A", "animals", "fish", 1))
            assertEquals(3, db.librarySyncDao().pending("A").size)
            val old = db.librarySyncDao().pending("A").first { it.stream == "collections" }
            db.collectionDao().update("A", "animals", "New name", "", 2)
            db.librarySyncDao().acknowledge("A", old.stream, old.documentId, old.parentId, old.sequence)
            assertTrue(db.librarySyncDao().dirty("A", "collections", "animals"))
            try {
                db.withTransaction {
                    db.collectionDao().create(CollectionEntity("A", "rollback", "Rollback", createdAt = 1, updatedAt = 1))
                    error("rollback")
                }
            } catch (_: IllegalStateException) { }
            assertFalse(db.librarySyncDao().dirty("A", "collections", "rollback"))
            db.collectionDao().delete("A", "animals")
            assertTrue(db.librarySyncDao().pending("A").filter { it.stream != "vocabulary" }.all { it.deleted })
            assertTrue(db.librarySyncDao().pending("other-user").isEmpty())
        } finally { db.close() }
    }
}
