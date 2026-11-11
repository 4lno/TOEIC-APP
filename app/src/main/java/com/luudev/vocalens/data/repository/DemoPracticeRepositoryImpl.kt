package com.luudev.vocalens.data.repository

import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.repository.PracticeRepository

/** Display samples only; no media files or download state are simulated. */
class DemoPracticeRepositoryImpl : PracticeRepository {
    override fun getLessons() = listOf(
        PracticeLesson("small-talk", "Daily English: Small Talk", "Giao tiếp hằng ngày",
            PracticeMediaType.Video, LessonDifficulty.Easy, 600, 0f, 98, 4, "student", isFeatured = true),
        PracticeLesson("cafe", "Giao tiếp tại quán cà phê", "Giao tiếp hằng ngày",
            PracticeMediaType.Video, LessonDifficulty.Easy, 504, 0.65f, 95, 3, "cafe"),
        PracticeLesson("workday", "A productive workday", "Tiếng Anh công sở",
            PracticeMediaType.Video, LessonDifficulty.Intermediate, 730, 0.30f, 84, 2, "workspace"),
        PracticeLesson("toeic-conversations", "TOEIC Part 3: Conversations", "TOEIC Listening",
            PracticeMediaType.Video, LessonDifficulty.Hard, 945, 0f, 99, 1, "student", isToeic = true),
        PracticeLesson("daily-podcast", "Daily English: On the go", "Tiếng Anh mỗi ngày",
            PracticeMediaType.Podcast, LessonDifficulty.Easy, 480, 0f, 92, 4, "student", isFeatured = true),
        PracticeLesson("cafe-podcast", "A coffee and a conversation", "Giao tiếp hằng ngày",
            PracticeMediaType.Podcast, LessonDifficulty.Easy, 420, 0f, 88, 3, "cafe"),
        PracticeLesson("work-podcast", "English at work", "Tiếng Anh công sở",
            PracticeMediaType.Podcast, LessonDifficulty.Intermediate, 780, 0f, 94, 2, "workspace"),
        PracticeLesson("toeic-podcast", "TOEIC: Listening strategies", "TOEIC Listening",
            PracticeMediaType.Podcast, LessonDifficulty.Hard, 900, 0f, 90, 1, "student", isToeic = true)
    )
}
