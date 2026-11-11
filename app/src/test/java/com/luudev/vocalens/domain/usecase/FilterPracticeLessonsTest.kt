package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class FilterPracticeLessonsTest {
    private val filter = FilterPracticeLessons()
    private val cafe = PracticeLesson("cafe", "Giao tiếp tại quán cà phê", "Giao tiếp hằng ngày",
        PracticeMediaType.Video, LessonDifficulty.Easy, 600, 0.65f, 80, 3, "cafe")
    private val work = PracticeLesson("work", "A productive workday", "Tiếng Anh công sở",
        PracticeMediaType.Video, LessonDifficulty.Intermediate, 601, 0.3f, 70, 2, "workspace")
    private val toeic = PracticeLesson("toeic", "TOEIC Part 3", "TOEIC Listening",
        PracticeMediaType.Video, LessonDifficulty.Hard, 900, 0f, 99, 1, "student", isToeic = true)
    private val podcast = cafe.copy(id = "podcast", mediaType = PracticeMediaType.Podcast)
    private val lessons = listOf(cafe, work, toeic, podcast)

    private fun search(query: String = "", selectedFilter: PracticeFilter = PracticeFilter.All,
        duration: PracticeDuration = PracticeDuration.All, sort: PracticeSort = PracticeSort.Newest,
        type: PracticeMediaType = PracticeMediaType.Video) = filter(lessons, type, query, selectedFilter, duration, sort)

    @Test
    fun keepsMediaTypesSeparate() {
        assertEquals(listOf(podcast), search(type = PracticeMediaType.Podcast))
        assertFalse(search().contains(podcast))
    }

    @Test
    fun searchesVietnameseAndEnglishIgnoringCaseAndAccents() {
        assertEquals(listOf(cafe), search("  QUAN CA PHE  "))
        assertEquals(listOf(work), search("productive"))
        assertEquals(listOf(work), search("cong so"))
    }

    @Test
    fun toeicFilterMatchesCategoryRatherThanDifficulty() {
        val easyToeic = toeic.copy(id = "easy-toeic", difficulty = LessonDifficulty.Easy)
        assertEquals(listOf(easyToeic), filter(listOf(cafe, easyToeic), PracticeMediaType.Video,
            "", PracticeFilter.Toeic, PracticeDuration.All, PracticeSort.Newest))
    }

    @Test
    fun durationBoundaryIncludesExactlyTenMinutesInShortLessons() {
        assertEquals(listOf(cafe), search(duration = PracticeDuration.Short))
        assertEquals(listOf(work, toeic), search(duration = PracticeDuration.Long))
    }

    @Test
    fun combinesDifficultySearchAndDuration() {
        assertTrue(search("workday", PracticeFilter.Easy).isEmpty())
        assertEquals(listOf(work), search("workday", PracticeFilter.Intermediate, PracticeDuration.Long))
    }

    @Test
    fun popularityAndNewestHaveDifferentOrders() {
        assertEquals(listOf(cafe, work, toeic), search())
        assertEquals(listOf(toeic, cafe, work), search(sort = PracticeSort.Popular))
    }
}
