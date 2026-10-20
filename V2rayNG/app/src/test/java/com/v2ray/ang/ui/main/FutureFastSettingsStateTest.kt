package com.v2ray.ang.ui.main

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FutureFastSettingsStateTest {
    @Test fun `committed switch state is visible before scheduling completes`() = runBlocking {
        val owner = FutureFastSettingsState()
        assertTrue(owner.state.value.loading)
        val scheduling = CompletableDeferred<Unit>()
        val resume = CompletableDeferred<Unit>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            owner.transaction(change = { FutureFastUiState(loading = false,
                preferences = com.v2ray.ang.dto.FutureFastPreferences(aiEnabled = true)) }, afterPublish = {
                scheduling.complete(Unit)
                resume.await()
            })
        }
        scheduling.await()
        assertTrue(owner.state.value.preferences.aiEnabled)
        assertFalse(owner.state.value.loading)
        assertTrue(job.isActive)
        resume.complete(Unit)
        job.join()
    }

    @Test fun `a failed write retains the last committed state and scheduling failure retains saved value`() = runBlocking {
        val owner = FutureFastSettingsState()
        val previous = FutureFastUiState(loading = false)
        owner.transaction(change = { previous })
        assertTrue(runCatching { owner.transaction(change = { error("write failed") }) }.isFailure)
        assertEquals(previous, owner.state.value)
        val next = previous.copy(subscriptionAuto = true)
        assertTrue(runCatching { owner.transaction(change = { next }, afterPublish = { error("schedule failed") }) }.isFailure)
        assertEquals(next, owner.state.value)
    }

    @Test fun `an in-flight poll cannot overwrite a later switch write`() = runBlocking {
        val owner = FutureFastSettingsState()
        val reading = CompletableDeferred<Unit>()
        val finishRead = CompletableDeferred<Unit>()
        val poll = launch(start = CoroutineStart.UNDISPATCHED) {
            owner.transaction(change = { reading.complete(Unit); finishRead.await(); FutureFastUiState(loading = false) })
        }
        reading.await()
        val write = async(start = CoroutineStart.UNDISPATCHED) {
            owner.transaction(change = { FutureFastUiState(loading = false, subscriptionAuto = true) })
        }
        assertFalse(write.isCompleted)
        finishRead.complete(Unit)
        poll.join(); write.await()
        assertTrue(owner.state.value.subscriptionAuto)
    }

    @Test fun `rapid toggle transactions preserve the last requested value`() = runBlocking {
        val owner = FutureFastSettingsState()
        val gate = CompletableDeferred<Unit>()
        val first = launch(start = CoroutineStart.UNDISPATCHED) {
            owner.transaction(change = { gate.await(); FutureFastUiState(loading = false, subscriptionAuto = true) })
        }
        val second = launch(start = CoroutineStart.UNDISPATCHED) {
            owner.transaction(change = { FutureFastUiState(loading = false, subscriptionAuto = false) })
        }
        gate.complete(Unit)
        first.join(); second.join()
        assertFalse(owner.state.value.subscriptionAuto)
    }
}
