package com.luudev.vocalens.data.repository

import androidx.room.withTransaction
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.local.entity.*
import com.luudev.vocalens.data.sync.LibrarySyncScheduler
import com.luudev.vocalens.domain.repository.LibrarySyncRepository
import com.luudev.vocalens.domain.model.LibrarySyncStatus
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** Room owns UI state. Cloud writes use deterministic IDs and server commit order (last accepted write wins). */
class FirebaseLibrarySyncRepository(
    private val database: VocabLensDatabase,
    private val firestore: FirebaseFirestore,
    private val currentUid: () -> String?,
    private val scheduler: LibrarySyncScheduler? = null
) : LibrarySyncRepository {
    private val dao get() = database.librarySyncDao()
    override fun requestSync(userId: String) { scheduler?.enqueue(userId) }
    override fun observeStatus(userId: String) = combine(dao.observePending(userId), dao.observeControl(userId),
        dao.observeLatestChange(userId)) { count, control, version ->
        LibrarySyncStatus(count, control?.error != null, version)
    }
    private fun ensureOwner(uid: String) { check(currentUid() == uid) { "Library account changed" } }
    private fun stream(uid: String, kind: String) = firestore.collection("users").document(uid).collection(kind)
    private fun documentId(event: LibrarySyncEntity): String = if (event.stream == "items")
        UUID.nameUUIDFromBytes("${event.parentId}:${event.documentId}".toByteArray(Charsets.UTF_8)).toString()
        else event.documentId

    override suspend fun synchronize(userId: String) = lock.withLock {
        ensureOwner(userId)
        seedExisting(userId)
        // Local unsent edits win over downloaded edits; among cloud writes the last server commit wins.
        push(userId)
        for (kind in listOf("vocabulary", "collections", "items")) pull(userId, kind)
        database.withTransaction {
            val control = dao.cursor(userId, "control") ?: LibrarySyncCursor(userId, "control")
            dao.saveCursor(control.copy(error = null))
        }
    }

    suspend fun recordError(uid: String, code: String) = database.withTransaction {
        val control = dao.cursor(uid, "control") ?: LibrarySyncCursor(uid, "control")
        dao.saveCursor(control.copy(error = code))
    }

    private suspend fun seedExisting(uid: String) = database.withTransaction {
        val control = dao.cursor(uid, "control") ?: LibrarySyncCursor(uid, "control")
        if (control.initialized) return@withTransaction
        val sql = database.openHelper.writableDatabase
        // Include data saved before this feature was installed. Existing dirty rows already cover later deletes.
        listOf(Triple("user_vocabulary", "vocabulary", "vocabularyId"), Triple("collections", "collections", "collectionId"),
            Triple("collection_items", "items", "vocabularyId")).forEach { (table, kind, id) ->
            val parent = if (kind == "items") "source.collectionId" else "''"
            sql.execSQL("""INSERT INTO library_sync_queue(userId, stream, documentId, parentId, deleted)
                SELECT source.userId, '$kind', source.$id, $parent, 0 FROM $table source WHERE source.userId = ?
                AND NOT EXISTS(SELECT 1 FROM library_sync_queue q WHERE q.userId = source.userId
                    AND q.stream = '$kind' AND q.documentId = source.$id AND q.parentId = $parent)""", arrayOf(uid))
        }
        dao.saveCursor(control.copy(initialized = true))
    }

    private suspend fun push(uid: String) {
        while (true) {
            ensureOwner(uid)
            val pending = dao.pending(uid)
            if (pending.isEmpty()) return
            val batch = firestore.batch()
            for (event in pending) {
                val payload = database.withTransaction { payload(event) }
                batch.set(stream(uid, event.stream).document(documentId(event)), payload +
                    ("serverUpdatedAt" to FieldValue.serverTimestamp()))
            }
            ensureOwner(uid)
            batch.commit().await()
            ensureOwner(uid)
            database.withTransaction {
                pending.forEach { dao.acknowledge(uid, it.stream, it.documentId, it.parentId, it.sequence) }
            }
        }
    }

    private fun payload(event: LibrarySyncEntity): Map<String, Any?> {
        val base = if (event.stream == "collections") mapOf("collectionId" to event.documentId)
            else if (event.stream == "items") mapOf("collectionId" to event.parentId, "wordId" to event.documentId)
            else mapOf("wordId" to event.documentId)
        if (event.deleted) return base + ("deleted" to true)
        val sql = database.openHelper.writableDatabase
        val (query, args) = when (event.stream) {
            "vocabulary" -> """SELECT v.*, uv.savedAt FROM vocabulary v JOIN user_vocabulary uv ON uv.vocabularyId = v.vocabularyId
                WHERE uv.userId = ? AND uv.vocabularyId = ?""" to arrayOf(event.userId, event.documentId)
            "collections" -> "SELECT * FROM collections WHERE userId = ? AND collectionId = ?" to arrayOf(event.userId, event.documentId)
            else -> "SELECT * FROM collection_items WHERE userId = ? AND collectionId = ? AND vocabularyId = ?" to
                arrayOf(event.userId, event.parentId, event.documentId)
        }
        sql.query(query, args).use { row ->
            if (!row.moveToFirst()) return base + ("deleted" to true)
            fun text(column: String): String? = row.getColumnIndexOrThrow(column).let { if (row.isNull(it)) null else row.getString(it) }
            fun number(column: String) = row.getLong(row.getColumnIndexOrThrow(column))
            return base + ("deleted" to false) + when (event.stream) {
                "vocabulary" -> mapOf("english" to text("english"), "phonetic" to text("phonetic"), "meaning" to text("meaningVi"),
                    "partOfSpeech" to text("partOfSpeech"), "exampleSentence" to text("exampleSentence"),
                    "category" to (text("categoryCode") ?: "Objects"), "createdAt" to number("createdAt"), "savedAt" to number("savedAt"))
                "collections" -> mapOf("name" to text("name"), "description" to text("description"),
                    "createdAt" to number("createdAt"), "updatedAt" to number("updatedAt"))
                else -> mapOf("addedAt" to number("addedAt"))
            }
        }
    }

    private suspend fun pull(uid: String, kind: String) {
        val cursor = dao.cursor(uid, kind)
        // Inclusive timestamp replays the boundary so equal server timestamps cannot be skipped.
        var query: Query = stream(uid, kind).orderBy("serverUpdatedAt").limit(100)
        if (cursor != null) query = query.whereGreaterThanOrEqualTo("serverUpdatedAt", Timestamp(cursor.seconds, cursor.nanos))
        var last: com.google.firebase.firestore.DocumentSnapshot? = null
        while (true) {
            ensureOwner(uid)
            val page = (if (last == null) query else query.startAfter(last)).get(Source.SERVER).await()
            ensureOwner(uid)
            if (page.isEmpty) return
            for (document in page.documents) {
                val data = document.data ?: continue
                val timestamp = document.getTimestamp("serverUpdatedAt") ?: error("Missing library timestamp")
                // Fetch parents only for an item whose parents are absent locally (concurrent device edits).
                if (kind == "items" && data["deleted"] == false) ensureParents(uid, data)
                database.withTransaction {
                    ensureOwner(uid)
                    val control = dao.cursor(uid, "control") ?: LibrarySyncCursor(uid, "control", initialized = true)
                    dao.saveCursor(control.copy(importing = true))
                    apply(uid, kind, data)
                    dao.saveCursor(LibrarySyncCursor(uid, kind, timestamp.seconds, timestamp.nanoseconds))
                    dao.saveCursor(control.copy(importing = false))
                }
            }
            last = page.documents.last()
            if (page.size() < 100) return
        }
    }

    private suspend fun ensureParents(uid: String, data: Map<String, Any?>) {
        for ((kind, field) in listOf("vocabulary" to "wordId", "collections" to "collectionId")) {
            val id = data[field] as String
            val exists = database.withTransaction { exists(uid, kind, id) }
            if (exists || dao.dirty(uid, kind, id)) continue
            val parent = stream(uid, kind).document(id).get(Source.SERVER).await().data ?: continue
            ensureOwner(uid)
            database.withTransaction {
                val control = dao.cursor(uid, "control") ?: LibrarySyncCursor(uid, "control", initialized = true)
                dao.saveCursor(control.copy(importing = true))
                apply(uid, kind, parent)
                dao.saveCursor(control.copy(importing = false))
            }
        }
    }

    private fun exists(uid: String, kind: String, id: String): Boolean {
        val table = if (kind == "vocabulary") "user_vocabulary" else "collections"
        val column = if (kind == "vocabulary") "vocabularyId" else "collectionId"
        return database.openHelper.writableDatabase.query("SELECT 1 FROM $table WHERE userId = ? AND $column = ?", arrayOf(uid, id))
            .use { it.moveToFirst() }
    }

    private suspend fun apply(uid: String, kind: String, data: Map<String, Any?>) {
        fun text(key: String) = data[key] as String
        fun time(key: String) = (data[key] as Number).toLong()
        val id = text(if (kind == "collections") "collectionId" else "wordId")
        val parent = if (kind == "items") text("collectionId") else ""
        if (dao.dirty(uid, kind, id, parent)) return
        val deleted = data["deleted"] == true
        when (kind) {
            "vocabulary" -> if (deleted) database.vocabularyDao().deleteWord(uid, id) else {
                // Legacy Room rows may have no category; their domain category is Objects.
                val existing = database.vocabularyDao().content(id)
                val category = if (existing != null && existing.categoryCode == null && text("category") == "Objects")
                    null else text("category")
                database.vocabularyDao().saveWord(uid, VocabularyEntity(id, text("english"), text("phonetic"), text("meaning"),
                    data["partOfSpeech"] as? String, data["exampleSentence"] as? String, category, time("createdAt")), time("savedAt"))
            }
            "collections" -> if (deleted) database.collectionDao().delete(uid, id) else {
                database.collectionDao().save(CollectionEntity(uid, id, text("name"), data["description"] as? String,
                    time("createdAt"), time("updatedAt")))
            }
            "items" -> if (deleted) database.collectionDao().removeItem(uid, parent, id) else {
                if (exists(uid, "vocabulary", id) && exists(uid, "collections", parent))
                    database.collectionDao().addItem(CollectionItemEntity(uid, parent, id, time("addedAt")))
            }
        }
    }

    companion object { private val lock = Mutex() }
}
