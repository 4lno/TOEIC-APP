package com.luudev.vocalens.presentation.ranking

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.luudev.vocalens.domain.model.RankingSnapshot
import com.luudev.vocalens.domain.repository.RankingRepository
import com.luudev.vocalens.domain.usecase.ObserveRanking
import com.luudev.vocalens.domain.usecase.RequestXpSync
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class RankingLifecycleTest {
    @get:Rule val compose = createComposeRule()

    private class Owner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
    }

    private class Repository : RankingRepository {
        val activeListeners = AtomicInteger()
        val syncRequests = AtomicInteger()
        override fun observeRanking(userId: String) = callbackFlow {
            activeListeners.incrementAndGet()
            trySend(RankingSnapshot())
            awaitClose { activeListeners.decrementAndGet() }
        }
        override fun requestSync(userId: String) { syncRequests.incrementAndGet() }
    }

    @Test fun listenersCloseOnPauseStopAndLeavingRankingWithoutStartingSyncOnResume() {
        val owner = Owner()
        val repository = Repository()
        val vm = RankingViewModel(ObserveRanking(repository), RequestXpSync(repository))
        val visible = mutableStateOf(true)
        compose.runOnUiThread {
            vm.onUserChanged("A")
            owner.lifecycle.currentState = Lifecycle.State.RESUMED
        }
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                VocaLensTheme(darkTheme = false, dynamicColor = false) {
                    if (visible.value) RankingRoute(vm, {})
                }
            }
        }
        compose.waitUntil(5000) { repository.activeListeners.get() == 1 }
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        compose.waitUntil(5000) { repository.activeListeners.get() == 0 }
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.CREATED }
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        compose.waitUntil(5000) { repository.activeListeners.get() == 1 }
        compose.runOnIdle { visible.value = false }
        compose.waitUntil(5000) { repository.activeListeners.get() == 0 }
        assertEquals(0, repository.syncRequests.get())
    }
}
