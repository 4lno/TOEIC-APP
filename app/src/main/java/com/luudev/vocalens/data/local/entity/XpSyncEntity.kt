package com.luudev.vocalens.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

/** A durable outbox: only new completed sessions are enrolled, never legacy XP. */
@Entity(tableName = "xp_sync", primaryKeys = ["userId", "sessionId"], foreignKeys = [
    ForeignKey(entity = StudySessionEntity::class, parentColumns = ["userId", "sessionId"],
        childColumns = ["userId", "sessionId"], onDelete = ForeignKey.CASCADE)
])
data class XpSyncEntity(
    val userId: String,
    val sessionId: String,
    val status: String = "PENDING",
    val errorCode: String? = null
)
