package com.luudev.vocalens.data.repository

import android.content.SharedPreferences
import com.luudev.vocalens.domain.model.VocabularyCategory
import com.luudev.vocalens.domain.model.VocabularyWord
import com.luudev.vocalens.domain.repository.VocabularyRepository

/** Local prototype word bank. Deleted items stay deleted when the app restarts. */
class LocalVocabularyRepositoryImpl(private val preferences: SharedPreferences) : VocabularyRepository {
    override fun getSavedWords(): List<VocabularyWord> {
        val deletedIds = preferences.getStringSet(DELETED_IDS, emptySet()).orEmpty()
        return SAMPLE_WORDS.filterNot { it.id in deletedIds }
    }

    override fun deleteWord(id: String) {
        val deletedIds = preferences.getStringSet(DELETED_IDS, emptySet()).orEmpty().toMutableSet()
        deletedIds.add(id)
        preferences.edit().putStringSet(DELETED_IDS, deletedIds).apply()
    }

    private companion object {
        const val DELETED_IDS = "deleted_vocabulary_ids"
        val SAMPLE_WORDS = listOf(
            VocabularyWord("headphones", "Headphones", "/ˈhed.fəʊnz/", "Tai nghe", VocabularyCategory.Technology),
            VocabularyWord("notebook", "Notebook", "/ˈnəʊt.bʊk/", "Sổ ghi chép", VocabularyCategory.Objects)
        )
    }
}
