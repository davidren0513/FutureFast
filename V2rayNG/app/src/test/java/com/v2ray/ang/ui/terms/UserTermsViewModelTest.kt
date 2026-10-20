package com.v2ray.ang.ui.terms

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserTermsViewModelTest {
    private class Store(var saved: Int = 0) : UserTermsStore {
        var readFailure = false
        var writeFailure = false
        var readGate: CompletableDeferred<Unit>? = null
        var writeGate: CompletableDeferred<Unit>? = null
        var writes = 0
        override suspend fun version(): Result<Int> {
            readGate?.await()
            return if (readFailure) Result.failure(IllegalStateException("read failed")) else Result.success(saved)
        }
        override suspend fun accept(): Result<Unit> {
            writes++
            writeGate?.await()
            if (writeFailure) return Result.failure(IllegalStateException("write failed"))
            saved = USER_TERMS_VERSION
            return Result.success(Unit)
        }
    }

    @Test fun `fresh or invalid version needs consent and current or later version does not`() {
        for (version in listOf(-1, 0, USER_TERMS_VERSION, USER_TERMS_VERSION + 1)) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val vm = UserTermsViewModel(Store(version), scope)
                assertFalse(vm.state.value.loading)
                assertEquals(version >= USER_TERMS_VERSION, vm.state.value.accepted)
            } finally { scope.cancel() }
        }
    }

    @Test fun `consent is unavailable during loading and pending save blocks duplicate taps`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val store = Store().apply { readGate = CompletableDeferred(); writeGate = CompletableDeferred() }
            val vm = UserTermsViewModel(store, scope)
            assertTrue(vm.state.value.loading)
            vm.onAction(UserTermsAction.Accept)
            assertEquals(0, store.writes)
            store.readGate!!.complete(Unit)
            vm.onAction(UserTermsAction.Accept)
            assertTrue(vm.state.value.saving)
            assertFalse(vm.state.value.accepted)
            vm.onAction(UserTermsAction.Accept)
            assertEquals(1, store.writes)
            store.writeGate!!.complete(Unit)
            assertTrue(vm.state.value.accepted)
            assertFalse(vm.state.value.saving)
            vm.onAction(UserTermsAction.Accept)
            assertEquals(1, store.writes)
        } finally { scope.cancel() }
    }

    @Test fun `failed read or write never grants access and successful retry persists across new viewmodels`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val store = Store().apply { readFailure = true; writeFailure = true }
            val vm = UserTermsViewModel(store, scope)
            assertTrue(vm.state.value.error)
            assertFalse(vm.state.value.accepted)
            vm.onAction(UserTermsAction.Accept)
            assertTrue(vm.state.value.error)
            assertFalse(vm.state.value.accepted)
            assertEquals(0, store.saved)
            store.writeFailure = false
            vm.onAction(UserTermsAction.Accept)
            assertTrue(vm.state.value.accepted)
            assertFalse(vm.state.value.error)
            store.readFailure = false
            assertTrue(UserTermsViewModel(store, scope).state.value.accepted)
        } finally { scope.cancel() }
    }

    @Test fun `cancelling the owner while saving cannot grant transient consent`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val store = Store().apply { writeGate = CompletableDeferred() }
        val vm = UserTermsViewModel(store, scope)
        vm.onAction(UserTermsAction.Accept)
        scope.cancel()
        store.writeGate!!.complete(Unit)
        assertFalse(vm.state.value.accepted)
        assertEquals(0, store.saved)
    }
}
