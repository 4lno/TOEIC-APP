package com.luudev.vocalens.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.local.entity.SavedVocabularyEntity
import com.luudev.vocalens.data.repository.LocalVocabularyRepositoryImpl
import com.luudev.vocalens.domain.model.VocabularyCategory
import com.luudev.vocalens.domain.model.VocabularyWord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VocabularyDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun word(uid: String, id: String = "word", time: Long = 1) = SavedVocabularyEntity(
        uid, id, "Notebook", "/test/", "Sổ ghi chép", "Objects", time
    )

    @Test fun upsertOrderAndDeletionAreScopedToUser() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, VocabLensDatabase::class.java).build()
        try {
            val dao = db.vocabularyDao()
            dao.saveWord(word("A"))
            dao.saveWord(word("B"))
            dao.saveWord(word("A").copy(meaning = "Updated"))
            dao.saveWord(word("A", "newer", 2))
            assertEquals(listOf("newer", "word"), dao.observeWords("A").first().map { it.wordId })
            assertEquals("Updated", dao.observeWords("A").first().last().meaning)
            dao.deleteWord("A", "word")
            assertEquals(1, dao.observeWords("A").first().size)
            assertEquals(1, dao.observeWords("B").first().size)
            assertTrue(dao.observeWords("C").first().isEmpty())
        } finally { db.close() }
    }

    @Test fun repositoryPersistsWordsAcrossDatabaseReopen() = runBlocking {
        val name = "vocabulary-persistence-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, VocabLensDatabase::class.java, name).build()
        val expected = VocabularyWord("notebook", "Notebook", "/test/", "Sổ ghi chép", VocabularyCategory.Objects)
        try {
            LocalVocabularyRepositoryImpl(db.vocabularyDao()).saveWord("A", expected)
            db.close()
            db = Room.databaseBuilder(context, VocabLensDatabase::class.java, name).build()
            val repo = LocalVocabularyRepositoryImpl(db.vocabularyDao())
            assertEquals(listOf(expected), repo.observeSavedWords("A").first())
            assertTrue(repo.observeSavedWords("B").first().isEmpty())
            repo.deleteWord("B", expected.id)
            assertEquals(listOf(expected), repo.observeSavedWords("A").first())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
