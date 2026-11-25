package com.luudev.vocalens.data.local

import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.firestore.Source
import com.luudev.vocalens.BuildConfig
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.remote.FirebaseServices
import com.luudev.vocalens.data.repository.FirebaseXpSyncRepository
import com.luudev.vocalens.domain.usecase.SyncPendingXp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit opt-in recovery tool for developer emulator data, never for production. */
@RunWith(AndroidJUnit4::class)
class XpRecoveryTest {
    @Test fun recoverSpecifiedRejectedSessionsThroughNormalSync() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val requested = arguments.getString("xpRecoverySessions")
        assumeTrue(!requested.isNullOrBlank())
        check(BuildConfig.DEBUG && BuildConfig.FIREBASE_EMULATOR_HOST.isNotBlank())
        val uid = requireNotNull(arguments.getString("xpRecoveryOwner"))
        val ids = requireNotNull(requested).split(',').filter { it.isNotBlank() }.distinct()
        require(ids.isNotEmpty())
        val expectedXp = requireNotNull(arguments.getString("xpRecoveryExpectedXp")).toLong()
        check(FirebaseServices.auth().currentUser?.uid == uid) { "Recovery owner is not signed in" }
        val database = VocabLensDatabase.getInstance(ApplicationProvider.getApplicationContext())
        val localXpBefore = ids.associateWith { requireNotNull(database.studyDao().session(uid, it)).earnedXp }
        database.withTransaction {
            for (id in ids) {
                val session = requireNotNull(database.studyDao().session(uid, id))
                check(session.status == "COMPLETED")
                database.openHelper.writableDatabase.execSQL("""UPDATE xp_sync SET status='PENDING', errorCode=NULL
                    WHERE userId=? AND sessionId=? AND status='REJECTED' AND errorCode='INVALID_ARGUMENT'""",
                    arrayOf(uid, id))
            }
        }
        withTimeout(30_000) { SyncPendingXp(FirebaseXpSyncRepository(database))(uid) }
        for (id in ids) {
            database.openHelper.readableDatabase.query(
                "SELECT status FROM xp_sync WHERE userId=? AND sessionId=?", arrayOf(uid, id)).use {
                check(it.moveToFirst())
                assertEquals("SYNCED", it.getString(0))
            }
            assertEquals(localXpBefore.getValue(id), database.studyDao().session(uid, id)!!.earnedXp)
        }
        val remote = FirebaseServices.firestore().collection("leaderboard").document(uid).get(Source.SERVER).await()
        assertEquals(expectedXp, remote.getLong("totalXp"))
    }
}
