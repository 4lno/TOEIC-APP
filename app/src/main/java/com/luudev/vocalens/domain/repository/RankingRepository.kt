package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.RankingSnapshot
import kotlinx.coroutines.flow.Flow

interface RankingRepository {
    fun observeRanking(userId: String): Flow<RankingSnapshot>
    fun requestSync(userId: String)
}
