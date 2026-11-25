package com.luudev.vocalens.domain.model

data class LibrarySyncStatus(val pending: Int = 0, val error: Boolean = false, val pendingVersion: Long = 0)
