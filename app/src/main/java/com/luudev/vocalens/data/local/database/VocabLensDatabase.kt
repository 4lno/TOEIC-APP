package com.luudev.vocalens.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.luudev.vocalens.data.local.dao.*
import com.luudev.vocalens.data.local.entity.*

@Database(entities = [UserEntity::class, UserSettingsEntity::class, VocabularyEntity::class,
    UserVocabularyEntity::class, CollectionEntity::class, CollectionItemEntity::class,
    LessonEntity::class, UserLessonProgressEntity::class, StudySessionEntity::class,
    VocabularyReviewEntity::class, XpSyncEntity::class], version = 2, exportSchema = true)
abstract class VocabLensDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun userDao(): UserDao
    abstract fun collectionDao(): CollectionDao
    abstract fun lessonDao(): LessonDao
    abstract fun studyDao(): StudyDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun xpSyncDao(): XpSyncDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `xp_sync` (
                    `userId` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `status` TEXT NOT NULL,
                    `errorCode` TEXT, PRIMARY KEY(`userId`, `sessionId`),
                    FOREIGN KEY(`userId`, `sessionId`) REFERENCES `study_sessions`(`userId`, `sessionId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE)""")
            }
        }
        @Volatile private var instance: VocabLensDatabase? = null

        fun getInstance(context: Context): VocabLensDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, VocabLensDatabase::class.java, "vocab_lens_app.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
