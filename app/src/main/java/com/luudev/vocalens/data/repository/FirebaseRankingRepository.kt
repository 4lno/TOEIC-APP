package com.luudev.vocalens.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.luudev.vocalens.data.local.dao.XpSyncDao
import com.luudev.vocalens.data.sync.XpSyncScheduler
import com.luudev.vocalens.domain.model.RankingEntry
import com.luudev.vocalens.domain.model.RankingSnapshot
import com.luudev.vocalens.domain.repository.RankingRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine

class FirebaseRankingRepository(
    private val firestore: FirebaseFirestore,
    private val outbox: XpSyncDao,
    private val scheduler: XpSyncScheduler
) : RankingRepository {
    private fun top() = callbackFlow {
        val listener = firestore.collection("leaderboard")
            .orderBy("totalXp", Query.Direction.DESCENDING).orderBy("userId").limit(50)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) trySend(RankingSnapshot(entries = snapshot.documents.map { document ->
                    RankingEntry(document.id, document.getString("displayName") ?: "Người học",
                        document.getLong("totalXp") ?: 0)
                }, fromCache = snapshot.metadata.isFromCache))
            }
        awaitClose { listener.remove() }
    }

    private fun ownXp(uid: String) = callbackFlow {
        val listener = firestore.collection("leaderboard").document(uid)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) trySend((snapshot.getLong("totalXp") ?: 0L) to snapshot.metadata.isFromCache)
            }
        awaitClose { listener.remove() }
    }

    override fun observeRanking(userId: String) = combine(top(), ownXp(userId), outbox.observeCounts(userId)) { top, own, counts ->
        top.copy(myTotalXp = own.first, fromCache = top.fromCache || own.second,
            pendingSessions = counts.pending, rejectedSessions = counts.rejected)
    }

    override fun requestSync(userId: String) = scheduler.enqueue(userId)
}
