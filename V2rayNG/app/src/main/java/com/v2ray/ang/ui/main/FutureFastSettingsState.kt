package com.v2ray.ang.ui.main

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One owner serializes foreground settings writes with background snapshots.
 * Publish committed values before WorkManager maintenance, rather than waiting for the next poll.
 */
internal class FutureFastSettingsState {
    private val mutable = MutableStateFlow(FutureFastUiState())
    val state = mutable.asStateFlow()
    val observed = mutable.subscriptionCount.map { it > 0 }.distinctUntilChanged()
    private val mutex = Mutex()

    suspend fun transaction(
        change: suspend () -> FutureFastUiState,
        afterPublish: suspend () -> Unit = {},
    ) = mutex.withLock {
        mutable.value = change()
        afterPublish()
    }
}
