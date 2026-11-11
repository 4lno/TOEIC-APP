package com.luudev.vocalens.data.local.entity

import androidx.room.Entity

@Entity(tableName = "saved_vocabulary", primaryKeys = ["userId", "wordId"])
data class SavedVocabularyEntity(
    val userId: String,
    val wordId: String,
    val english: String,
    val phonetic: String,
    val meaning: String,
    val category: String,
    val savedAt: Long
)
