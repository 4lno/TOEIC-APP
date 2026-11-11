package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.VocabularyWord

interface VocabularyRepository {
    fun getSavedWords(): List<VocabularyWord>
    fun deleteWord(id: String)
}
