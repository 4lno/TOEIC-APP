package com.luudev.vocalens.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.luudev.vocalens.data.local.dao.VocabularyDao
import com.luudev.vocalens.data.local.entity.SavedVocabularyEntity

@Database(entities = [SavedVocabularyEntity::class], version = 1, exportSchema = true)
abstract class VocabLensDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao

    companion object {
        @Volatile private var instance: VocabLensDatabase? = null

        fun getInstance(context: Context): VocabLensDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, VocabLensDatabase::class.java, "vocab_lens.db"
            ).build().also { instance = it }
        }
    }
}
