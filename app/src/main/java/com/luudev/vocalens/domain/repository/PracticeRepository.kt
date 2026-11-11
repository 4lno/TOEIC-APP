package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.PracticeLesson

interface PracticeRepository {
    fun getLessons(): List<PracticeLesson>
}
