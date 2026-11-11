package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.VocabularyCategory
import com.luudev.vocalens.domain.model.VocabularySort
import com.luudev.vocalens.domain.model.VocabularyWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterVocabularyTest {
    private val filter = FilterVocabulary()
    private val headphones = VocabularyWord("headphones", "Headphones", "", "Tai nghe", VocabularyCategory.Technology)
    private val notebook = VocabularyWord("notebook", "Notebook", "", "Sổ ghi chép", VocabularyCategory.Objects)
    private val words = listOf(headphones, notebook)

    @Test
    fun searchesEnglishWithoutCaseOrSurroundingWhitespace() {
        assertEquals(listOf(headphones), filter(words, "  HEAD  ", null, VocabularySort.RecentlySaved))
    }

    @Test
    fun searchesVietnameseMeaningWithOrWithoutAccents() {
        listOf("sổ", "so ghi chep", "SỔ GHI").forEach { query ->
            assertEquals(listOf(notebook), filter(words, query, null, VocabularySort.RecentlySaved))
        }
    }

    @Test
    fun appliesCategoryAndSearchTogether() {
        assertTrue(filter(words, "head", VocabularyCategory.Objects, VocabularySort.RecentlySaved).isEmpty())
        assertEquals(listOf(headphones), filter(words, "head", VocabularyCategory.Technology, VocabularySort.RecentlySaved))
    }

    @Test
    fun sortsAlphabeticallyWithoutChangingRecentOrder() {
        val newestFirst = listOf(notebook, headphones)
        assertEquals(words, filter(newestFirst, "", null, VocabularySort.Alphabetical))
        assertEquals(newestFirst, filter(newestFirst, "", null, VocabularySort.RecentlySaved))
    }

    @Test
    fun handlesEmptyCategoryAndEmptyWordBank() {
        assertTrue(filter(words, "", VocabularyCategory.Travel, VocabularySort.RecentlySaved).isEmpty())
        assertTrue(filter(emptyList(), "", null, VocabularySort.Alphabetical).isEmpty())
    }
}
