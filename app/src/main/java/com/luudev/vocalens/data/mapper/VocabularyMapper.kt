package com.luudev.vocalens.data.mapper

import com.luudev.vocalens.data.local.entity.SavedVocabularyEntity
import com.luudev.vocalens.domain.model.VocabularyCategory
import com.luudev.vocalens.domain.model.VocabularyWord

fun SavedVocabularyEntity.toDomain() = VocabularyWord(
    id = wordId, english = english, phonetic = phonetic, meaning = meaning,
    category = VocabularyCategory.entries.find { it.name == category } ?: VocabularyCategory.Objects
)

fun VocabularyWord.toEntity(userId: String, savedAt: Long) = SavedVocabularyEntity(
    userId = userId, wordId = id, english = english, phonetic = phonetic,
    meaning = meaning, category = category.name, savedAt = savedAt
)
