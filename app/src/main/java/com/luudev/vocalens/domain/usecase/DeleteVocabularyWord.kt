package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.VocabularyRepository

class DeleteVocabularyWord(private val repository: VocabularyRepository) {
    operator fun invoke(id: String) = repository.deleteWord(id)
}
