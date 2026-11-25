package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.LibrarySyncRepository

class SyncLibrary(private val repository: LibrarySyncRepository) {
    suspend operator fun invoke(userId: String) = repository.synchronize(userId)
}

class RequestLibrarySync(private val repository: LibrarySyncRepository) {
    operator fun invoke(userId: String) = repository.requestSync(userId)
}

class ObserveLibrarySync(private val repository: LibrarySyncRepository) {
    operator fun invoke(userId: String) = repository.observeStatus(userId)
}
