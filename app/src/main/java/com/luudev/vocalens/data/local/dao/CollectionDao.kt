package com.luudev.vocalens.data.local.dao

import androidx.room.*
import com.luudev.vocalens.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CollectionDao {
    @Query("SELECT * FROM collections WHERE userId = :uid ORDER BY updatedAt DESC, collectionId")
    abstract fun observeCollections(uid: String): Flow<List<CollectionEntity>>
    @Query("""SELECT v.* FROM vocabulary v JOIN collection_items i ON i.vocabularyId = v.vocabularyId
        WHERE i.userId = :uid AND i.collectionId = :id ORDER BY i.addedAt DESC, v.vocabularyId""")
    abstract fun observeWords(uid: String, id: String): Flow<List<VocabularyEntity>>
    @Upsert protected abstract suspend fun upsertCollection(collection: CollectionEntity)
    open suspend fun save(collection: CollectionEntity) {
        require(collection.userId.isNotBlank() && collection.collectionId.isNotBlank() && collection.name.isNotBlank())
        upsertCollection(collection.copy(name = collection.name.trim()))
    }
    // Composite FKs require the saved word and collection to belong to the same user.
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun addItem(item: CollectionItemEntity)
    @Query("DELETE FROM collection_items WHERE userId = :uid AND collectionId = :id AND vocabularyId = :wordId")
    abstract suspend fun removeItem(uid: String, id: String, wordId: String)
    @Query("DELETE FROM collections WHERE userId = :uid AND collectionId = :id")
    abstract suspend fun delete(uid: String, id: String)
}
