package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.DictionaryEntry
import com.luudev.vocalens.domain.repository.DictionaryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LookupVocabularyTest {
    @Test fun keyboardCapitalizationAndWhitespaceUseTheSameDictionaryHeadword() = runTest {
        val requests = mutableListOf<String>()
        val lookup = LookupVocabulary(object : DictionaryRepository {
            override suspend fun lookup(query: String): List<DictionaryEntry> {
                requests += query
                return emptyList()
            }
        })

        listOf("fish", "Fish", " FISH ").forEach { lookup(it) }

        assertEquals(listOf("fish", "fish", "fish"), requests)
    }
}
