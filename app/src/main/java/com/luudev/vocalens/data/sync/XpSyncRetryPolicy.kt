package com.luudev.vocalens.data.sync

/** Infrastructure failures may retry twice, for at most three executions per work request. */
internal object XpSyncRetryPolicy {
    const val MAX_ATTEMPTS = 3
    private val temporaryCodes = setOf("UNAVAILABLE", "DEADLINE_EXCEEDED", "ABORTED", "RESOURCE_EXHAUSTED", "INTERNAL")

    fun shouldRetry(runAttemptCount: Int, functionsCode: String? = null, networkFailure: Boolean = false): Boolean {
        if (runAttemptCount !in 0 until MAX_ATTEMPTS - 1) return false
        // An explicit server error takes precedence over a generic network classification.
        return if (functionsCode != null) functionsCode in temporaryCodes else networkFailure
    }
}
