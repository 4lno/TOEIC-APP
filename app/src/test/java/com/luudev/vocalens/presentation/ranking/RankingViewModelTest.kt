package com.luudev.vocalens.presentation.ranking

import com.luudev.vocalens.domain.model.RankingEntry
import com.luudev.vocalens.domain.model.RankingSnapshot
import com.luudev.vocalens.domain.repository.RankingRepository
import com.luudev.vocalens.domain.usecase.ObserveRanking
import com.luudev.vocalens.domain.usecase.RequestXpSync
import com.luudev.vocalens.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RankingViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private class Repository : RankingRepository {
        val data = MutableStateFlow(RankingSnapshot())
        val requested = mutableListOf<String>()
        var subscriptions = 0
        var fail = false
        var failSync = false
        override fun observeRanking(userId: String): Flow<RankingSnapshot> = flow {
            subscriptions++
            if (fail) error("Offline")
            emitAll(data)
        }
        override fun requestSync(userId: String) {
            if (failSync) error("Scheduler unavailable")
            requested += userId
        }
    }

    @Test fun accountChangeClearsPreviousRankingAndCancelsListener() = runTest {
        val repository = Repository()
        val vm = RankingViewModel(ObserveRanking(repository), RequestXpSync(repository))
        vm.onUserChanged("A")
        assertTrue(repository.requested.isEmpty())
        vm.start(); runCurrent()
        repository.data.value = RankingSnapshot(entries = listOf(RankingEntry("A", "An", 50)), myTotalXp = 50)
        runCurrent()
        assertEquals(50L, vm.uiState.ranking.myTotalXp)
        vm.onUserChanged("B")
        assertEquals(0L, vm.uiState.ranking.myTotalXp)
        repository.data.value = RankingSnapshot(myTotalXp = 999)
        runCurrent()
        assertEquals(0L, vm.uiState.ranking.myTotalXp)
        vm.onUserChanged(null)
        assertNull(vm.uiState.userId)
    }

    @Test fun retryReconnectsAfterListenerFailureAndStopRemovesSubscription() = runTest {
        val repository = Repository().apply { fail = true }
        val vm = RankingViewModel(ObserveRanking(repository), RequestXpSync(repository))
        vm.onUserChanged("A"); vm.start(); runCurrent()
        assertTrue(vm.uiState.error)
        repository.fail = false
        vm.retry(); runCurrent()
        assertFalse(vm.uiState.error)
        assertEquals(1, repository.requested.size)
        vm.stop(); runCurrent()
        repository.data.value = RankingSnapshot(myTotalXp = 999)
        runCurrent()
        assertEquals(0L, vm.uiState.ranking.myTotalXp)
    }

    @Test fun resumeDoesNotScheduleSyncAndRepeatedClicksKeepHealthyListener() = runTest {
        val repository = Repository()
        var clock = 0L
        val vm = RankingViewModel(ObserveRanking(repository), RequestXpSync(repository), monotonic = { clock })
        vm.onUserChanged("A"); vm.start(); runCurrent()
        assertTrue(repository.requested.isEmpty())
        assertEquals(1, repository.subscriptions)
        repeat(10) { vm.retry() }
        runCurrent()
        assertEquals(1, repository.requested.size)
        assertEquals(1, repository.subscriptions)
        clock += 5000
        vm.retry(); runCurrent()
        assertEquals(2, repository.requested.size)
        assertEquals(1, repository.subscriptions)
        vm.stop(); clock += 5000; vm.retry(); runCurrent()
        assertEquals(2, repository.requested.size)
        vm.start(); runCurrent()
        assertEquals(2, repository.subscriptions)
        assertEquals(2, repository.requested.size)
    }

    @Test fun schedulingFailureIsDisplayedWithoutCrashingOrReopeningTheListener() = runTest {
        val repository = Repository().apply { failSync = true }
        val vm = RankingViewModel(ObserveRanking(repository), RequestXpSync(repository))
        vm.onUserChanged("A"); vm.start(); runCurrent()
        vm.retry(); runCurrent()
        assertTrue(vm.uiState.error)
        assertEquals(1, repository.subscriptions)
        assertTrue(repository.requested.isEmpty())
    }
}
