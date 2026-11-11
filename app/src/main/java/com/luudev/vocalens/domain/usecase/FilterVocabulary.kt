package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.VocabularyCategory
import com.luudev.vocalens.domain.model.VocabularySort
import com.luudev.vocalens.domain.model.VocabularyWord
import java.text.Normalizer
import java.util.Locale

class FilterVocabulary {
    operator fun invoke(words: List<VocabularyWord>, query: String,
        category: VocabularyCategory?, sort: VocabularySort): List<VocabularyWord> {
        val search = normalize(query.trim())
        val filtered = words.filter { word ->
            (category == null || word.category == category) &&
                (search.isEmpty() || normalize(word.english).contains(search) || normalize(word.meaning).contains(search))
        }
        return if (sort == VocabularySort.Alphabetical) filtered.sortedBy { it.english.lowercase(Locale.ROOT) } else filtered
    }

    private fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(DIACRITICS, "").lowercase(Locale.ROOT).replace('đ', 'd')

    private companion object {
        val DIACRITICS = Regex("\\p{M}+")
    }
}
