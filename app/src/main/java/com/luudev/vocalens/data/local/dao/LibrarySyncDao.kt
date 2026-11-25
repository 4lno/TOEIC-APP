package com.luudev.vocalens.data.local.dao

import androidx.room.*
import com.luudev.vocalens.data.local.entity.LibrarySyncEntity
import com.luudev.vocalens.data.local.entity.LibrarySyncCursor
import kotlinx.coroutines.flow.Flow

@Dao
interface LibrarySyncDao {
    @Query("""SELECT * FROM library_sync_queue WHERE sequence IN
        (SELECT MAX(sequence) FROM library_sync_queue WHERE userId = :uid GROUP BY stream, documentId, parentId)
        ORDER BY CASE stream WHEN 'vocabulary' THEN 0 WHEN 'collections' THEN 1 ELSE 2 END, sequence LIMIT 100""")
    suspend fun pending(uid: String): List<LibrarySyncEntity>
    @Query("""DELETE FROM library_sync_queue WHERE userId = :uid AND stream = :stream
        AND documentId = :id AND parentId = :parent AND sequence <= :sequence""")
    suspend fun acknowledge(uid: String, stream: String, id: String, parent: String, sequence: Long)
    @Query("""SELECT EXISTS(SELECT 1 FROM library_sync_queue WHERE userId = :uid AND stream = :stream
        AND documentId = :id AND parentId = :parent)""")
    suspend fun dirty(uid: String, stream: String, id: String, parent: String = ""): Boolean
    @Query("SELECT COUNT(*) FROM library_sync_queue WHERE userId = :uid")
    fun observePending(uid: String): Flow<Int>
    @Query("SELECT COALESCE(MAX(sequence), 0) FROM library_sync_queue WHERE userId = :uid")
    fun observeLatestChange(uid: String): Flow<Long>
    @Query("SELECT * FROM library_sync_cursor WHERE userId = :uid AND stream = :stream")
    suspend fun cursor(uid: String, stream: String): LibrarySyncCursor?
    @Upsert suspend fun saveCursor(cursor: LibrarySyncCursor)
    @Query("SELECT * FROM library_sync_cursor WHERE userId = :uid AND stream = 'control'")
    fun observeControl(uid: String): Flow<LibrarySyncCursor?>
}
