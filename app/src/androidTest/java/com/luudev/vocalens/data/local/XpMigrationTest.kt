package com.luudev.vocalens.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import org.junit.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XpMigrationTest {
    @Test fun migrationPreservesLegacyXpWithoutEnrollingOldSessions() = runBlocking {
        val name = "xp-migration-test"
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(name)
        try {
            // Reconstruct the released v1 schema, including its identity hash, from Room's export.
            val schema = InstrumentationRegistry.getInstrumentation().context.assets
                .open("com.luudev.vocalens.data.local.database.VocabLensDatabase/1.json")
                .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
                val entities = schema.getJSONArray("entities")
                for (index in 0 until entities.length()) {
                    val entity = entities.getJSONObject(index)
                    val table = entity.getString("tableName")
                    old.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}", table))
                    val indices = entity.optJSONArray("indices") ?: JSONArray()
                    for (i in 0 until indices.length()) {
                        old.execSQL(indices.getJSONObject(i).getString("createSql").replace("${'$'}{TABLE_NAME}", table))
                    }
                }
                val queries = schema.getJSONArray("setupQueries")
                for (index in 0 until queries.length()) old.execSQL(queries.getString(index))
                old.execSQL("""INSERT INTO users (userId,email,displayName,createdAt,updatedAt)
                    VALUES ('A','a@example.com','An',1,1)""")
                old.execSQL("""INSERT INTO study_sessions
                    (userId,sessionId,sessionType,status,startedAt,endedAt,activeDurationMs,earnedXp)
                    VALUES ('A','legacy','VOCAB_REVIEW','COMPLETED',1000,2000,1000,50)""")
                old.version = 1
            }
            val migrated = Room.databaseBuilder(context, VocabLensDatabase::class.java, name)
                .addMigrations(VocabLensDatabase.MIGRATION_1_2).build()
            try {
                assertEquals(50, migrated.studyDao().session("A", "legacy")!!.earnedXp)
                assertEquals(0, migrated.xpSyncDao().pending("A").size)
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
