package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.DictionaryRepository
import java.util.Locale

class LookupVocabulary(private val repository: DictionaryRepository) {
    // Vocabulary lookup should not change meaning when the keyboard capitalizes a word.
    suspend operator fun invoke(query: String) = repository.lookup(query.trim().lowercase(Locale.ROOT))
}
