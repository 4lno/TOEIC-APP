package com.luudev.vocalens.data.local.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** Capture every library mutation in the same SQLite transaction, including FK cascades. */
object LibrarySyncSchema : RoomDatabase.Callback() {
    fun createTables(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS library_sync_queue (
            sequence INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL,
            stream TEXT NOT NULL, documentId TEXT NOT NULL, parentId TEXT NOT NULL, deleted INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS library_sync_cursor (
            userId TEXT NOT NULL, stream TEXT NOT NULL, seconds INTEGER NOT NULL, nanos INTEGER NOT NULL,
            importing INTEGER NOT NULL, initialized INTEGER NOT NULL, error TEXT,
            PRIMARY KEY(userId, stream))""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_library_sync_queue_userId_stream_documentId_parentId ON library_sync_queue(userId, stream, documentId, parentId)")
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        listOf("user_vocabulary" to "vocabulary", "collections" to "collections", "collection_items" to "items").forEach { (table, stream) ->
            val id = if (table == "collections") "collectionId" else "vocabularyId"
            val parent = if (table == "collection_items") "row.collectionId" else "''"
            val operations = if (table == "collections") listOf("INSERT", "UPDATE", "DELETE") else listOf("INSERT", "DELETE")
            operations.forEach { operation ->
                val row = if (operation == "DELETE") "OLD" else "NEW"
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS library_${table}_${operation.lowercase()}
                    AFTER $operation ON $table
                    WHEN NOT EXISTS (SELECT 1 FROM library_sync_cursor WHERE userId = $row.userId AND stream = 'control' AND importing = 1)
                    BEGIN INSERT INTO library_sync_queue(userId, stream, documentId, parentId, deleted)
                    VALUES ($row.userId, '$stream', $row.$id, ${parent.replace("row", row)}, ${if (operation == "DELETE") 1 else 0}); END""")
            }
        }
    }
}
