package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.PracticeRepository

class GetPracticeLessons(private val repository: PracticeRepository) {
    operator fun invoke() = repository.getLessons()
}
