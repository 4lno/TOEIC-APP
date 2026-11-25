package com.luudev.vocalens.data.local.dao

import androidx.room.*
import com.luudev.vocalens.data.local.entity.XpSyncEntity
import kotlinx.coroutines.flow.Flow

data class XpSyncCounts(val pending: Int, val rejected: Int)

@Dao
interface XpSyncDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(value: XpSyncEntity)

    @Query("""SELECT x.* FROM xp_sync x JOIN study_sessions s
        ON s.userId = x.userId AND s.sessionId = x.sessionId
        WHERE x.userId = :uid AND x.status = 'PENDING' ORDER BY s.startedAt, x.sessionId""")
    suspend fun pending(uid: String): List<XpSyncEntity>

    @Query("""UPDATE xp_sync SET status = :status, errorCode = :errorCode
        WHERE userId = :uid AND sessionId = :sessionId AND status = 'PENDING'""")
    suspend fun mark(uid: String, sessionId: String, status: String, errorCode: String? = null)

    @Query("""SELECT COALESCE(SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END),0) AS pending,
        COALESCE(SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END),0) AS rejected
        FROM xp_sync WHERE userId = :uid""")
    fun observeCounts(uid: String): Flow<XpSyncCounts>
}
