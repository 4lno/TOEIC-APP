package com.luudev.vocalens.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.luudev.vocalens.data.local.entity.SavedVocabularyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM saved_vocabulary WHERE userId = :userId ORDER BY savedAt DESC, wordId ASC")
    fun observeWords(userId: String): Flow<List<SavedVocabularyEntity>>

    @Upsert
    suspend fun saveWord(word: SavedVocabularyEntity)

    @Query("DELETE FROM saved_vocabulary WHERE userId = :userId AND wordId = :wordId")
    suspend fun deleteWord(userId: String, wordId: String)
}
