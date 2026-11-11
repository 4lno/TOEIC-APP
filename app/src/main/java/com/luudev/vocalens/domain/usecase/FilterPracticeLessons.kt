package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.*
import java.text.Normalizer
import java.util.Locale

class FilterPracticeLessons {
    operator fun invoke(lessons: List<PracticeLesson>, mediaType: PracticeMediaType,
        query: String, filter: PracticeFilter, duration: PracticeDuration, sort: PracticeSort): List<PracticeLesson> {
        val search = normalize(query.trim())
        val filtered = lessons.filter { lesson ->
            lesson.mediaType == mediaType &&
                (search.isEmpty() || normalize(lesson.title).contains(search) || normalize(lesson.category).contains(search)) &&
                when (filter) {
                    PracticeFilter.All -> true
                    PracticeFilter.Easy -> lesson.difficulty == LessonDifficulty.Easy
                    PracticeFilter.Intermediate -> lesson.difficulty == LessonDifficulty.Intermediate
                    PracticeFilter.Toeic -> lesson.isToeic
                } && when (duration) {
                    PracticeDuration.All -> true
                    PracticeDuration.Short -> lesson.durationSeconds <= 600
                    PracticeDuration.Long -> lesson.durationSeconds > 600
                }
        }
        return when (sort) {
            PracticeSort.Newest -> filtered.sortedByDescending { it.publishedOrder }
            PracticeSort.Popular -> filtered.sortedByDescending { it.popularity }
        }
    }

    private fun normalize(text: String) = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(DIACRITICS, "").lowercase(Locale.ROOT).replace('đ', 'd')

    private companion object { val DIACRITICS = Regex("\\p{M}+") }
}
