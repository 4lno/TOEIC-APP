package com.luudev.vocalens.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "library_sync_queue", indices = [Index(value = ["userId", "stream", "documentId", "parentId"])])
data class LibrarySyncEntity(
    @PrimaryKey(autoGenerate = true) val sequence: Long = 0,
    val userId: String,
    val stream: String,
    val documentId: String,
    val parentId: String = "",
    val deleted: Boolean = false
)

@Entity(tableName = "library_sync_cursor", primaryKeys = ["userId", "stream"])
data class LibrarySyncCursor(
    val userId: String,
    val stream: String,
    val seconds: Long = 0,
    val nanos: Int = 0,
    val importing: Boolean = false,
    val initialized: Boolean = false,
    val error: String? = null
)
