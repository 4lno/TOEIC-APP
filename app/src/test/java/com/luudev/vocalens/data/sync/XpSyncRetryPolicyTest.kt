package com.luudev.vocalens.data.sync

import org.junit.Assert.*
import org.junit.Test

class XpSyncRetryPolicyTest {
    @Test fun temporaryServiceErrorsRetryTwiceAndThenStop() {
        for (code in listOf("UNAVAILABLE", "DEADLINE_EXCEEDED", "ABORTED", "RESOURCE_EXHAUSTED", "INTERNAL")) {
            assertTrue(code, XpSyncRetryPolicy.shouldRetry(0, code))
            assertTrue(code, XpSyncRetryPolicy.shouldRetry(1, code))
            assertFalse(code, XpSyncRetryPolicy.shouldRetry(2, code))
            assertFalse(code, XpSyncRetryPolicy.shouldRetry(100, code))
        }
    }

    @Test fun authPermissionConfigurationAndUnknownErrorsNeverAutoRetry() {
        for (code in listOf("UNAUTHENTICATED", "PERMISSION_DENIED", "NOT_FOUND", "UNIMPLEMENTED",
                "UNKNOWN", "INVALID_ARGUMENT", "FAILED_PRECONDITION", "ALREADY_EXISTS", "CANCELLED", "DATA_LOSS")) {
            assertFalse(code, XpSyncRetryPolicy.shouldRetry(0, code))
            assertFalse(code, XpSyncRetryPolicy.shouldRetry(0, code, networkFailure = true))
        }
        assertFalse(XpSyncRetryPolicy.shouldRetry(0))
    }

    @Test fun networkFailuresAlsoStopAfterThreeExecutions() {
        assertTrue(XpSyncRetryPolicy.shouldRetry(0, networkFailure = true))
        assertTrue(XpSyncRetryPolicy.shouldRetry(1, networkFailure = true))
        assertFalse(XpSyncRetryPolicy.shouldRetry(2, networkFailure = true))
    }
}
