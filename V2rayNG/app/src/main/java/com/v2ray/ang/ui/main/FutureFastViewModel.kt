package com.v2ray.ang.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.FutureFastAiService
import com.v2ray.ang.dto.FutureFastNodeReference
import com.v2ray.ang.dto.FutureFastPolicy
import com.v2ray.ang.dto.FutureFastPreferences
import com.v2ray.ang.handler.FutureFastOptimizer
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.handler.SubscriptionUpdater
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FutureFastUiState(
    val preferences: FutureFastPreferences = FutureFastPreferences(),
    val nodes: List<FutureFastNodeReference> = emptyList(),
    val selectedGuid: String? = null,
    val selectedName: String? = null,
    val delayMillis: Long = 0,
    val subscriptionCount: Int = 0,
    val subscriptionAuto: Boolean = false,
    val subscriptionMinutes: Long = 360,
    val loading: Boolean = true,
)

data class FutureFastIntervalInput(val text: String = "", val minutes: Long? = null)

sealed interface FutureFastAction {
    data class IntervalInput(val text: String) : FutureFastAction
    data class AutoSelect(val enabled: Boolean) : FutureFastAction
    data class TestInterval(val minutes: Long) : FutureFastAction
    data class AiEnabled(val enabled: Boolean) : FutureFastAction
    data class AiNode(val service: FutureFastAiService, val guid: String?) : FutureFastAction
    data class SubscriptionSchedule(val enabled: Boolean, val minutes: Long) : FutureFastAction
    data object TestNow : FutureFastAction
    data object DismissError : FutureFastAction
}

/** Settings and background-maintenance state; connection operations remain MainAction operations. */
class FutureFastViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = FutureFastSettingsState()
    private val _error = MutableStateFlow<Int?>(null)
    val error = _error.asStateFlow()
    private val _intervalInput = MutableStateFlow(FutureFastIntervalInput())
    val intervalInput = _intervalInput.asStateFlow()
    val state = settings.state

    init {
        viewModelScope.launch {
            settings.observed.collectLatest { active ->
                if (active) while (true) {
                    try {
                        settings.transaction(change = { withContext(Dispatchers.IO) { loadState() } })
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        LogUtil.e(AppConfig.TAG, "Future Fast: load settings failed", error)
                        _error.value = R.string.ff_error
                    }
                    delay(2000)
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FutureFastOptimizer.schedule(application)
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Future Fast: schedule optimization failed", error)
                _error.value = R.string.ff_error
            }
        }
    }

    private fun loadState(): FutureFastUiState {
        val guid = MmkvManager.getSelectServer()
        val profile = guid?.let(MmkvManager::decodeServerConfig)
        val subscriptions = MmkvManager.decodeSubscriptions().filter { it.subscription.url.isNotBlank() }
        return FutureFastUiState(
            preferences = MmkvManager.futureFastPreferences(),
            nodes = SettingsManager.futureFastNodes(),
            selectedGuid = guid,
            selectedName = profile?.remarks,
            delayMillis = guid?.let { MmkvManager.decodeServerAffiliationInfo(it)?.testDelayMillis } ?: 0,
            subscriptionCount = subscriptions.size,
            subscriptionAuto = subscriptions.isNotEmpty() && subscriptions.all { it.subscription.autoUpdate },
            subscriptionMinutes = subscriptions.firstOrNull()?.subscription?.updateInterval ?: 360,
            loading = false,
        )
    }

    fun prepareConnection(onReady: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                MmkvManager.updateFutureFastPreferences {
                    it.copy(connectionRevision = it.connectionRevision + 1, connectionRequestedAt = System.currentTimeMillis())
                }
                withContext(Dispatchers.Main) { onReady() }
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Future Fast: prepare connection failed", error)
                _error.value = R.string.ff_error
            }
        }
    }

    fun onAction(action: FutureFastAction) {
        if (action is FutureFastAction.IntervalInput) {
            val minutes = action.text.toLongOrNull()?.takeIf { it in 15..10080 }
            _intervalInput.value = FutureFastIntervalInput(action.text, minutes)
            return
        }
        if (action == FutureFastAction.DismissError) {
            _error.value = null
            return
        }
        // Acquire the settings transaction in event order on Main; disk work stays on IO.
        viewModelScope.launch {
            try {
                settings.transaction(change = {
                    withContext(Dispatchers.IO) {
                        when (action) {
                            is FutureFastAction.IntervalInput -> Unit
                            is FutureFastAction.AutoSelect -> {
                                MmkvManager.updateFutureFastPreferences { it.copy(autoSelect = action.enabled) }
                            }
                            is FutureFastAction.TestInterval -> {
                                require(action.minutes in 15..10080)
                                MmkvManager.updateFutureFastPreferences { it.copy(testIntervalMinutes = action.minutes) }
                            }
                            is FutureFastAction.AiEnabled -> MmkvManager.updateFutureFastPreferences { it.copy(aiEnabled = action.enabled) }
                            is FutureFastAction.AiNode -> {
                                val node = action.guid?.let { guid -> SettingsManager.futureFastNodes().firstOrNull { it.guid == guid }
                                    ?: error("Selected AI node no longer exists") }
                                MmkvManager.updateFutureFastPreferences {
                                    val nodes = it.aiNodes.toMutableMap()
                                    if (node == null) nodes.remove(action.service.name) else nodes[action.service.name] = node
                                    it.copy(aiNodes = nodes)
                                }
                            }
                            is FutureFastAction.SubscriptionSchedule -> {
                                require(action.minutes in 15..10080)
                                val subscriptions = MmkvManager.decodeSubscriptions().filter { it.subscription.url.isNotBlank() }
                                val originals = subscriptions.associate { it.guid to it.subscription.copy() }
                                try {
                                    subscriptions.forEach { sub ->
                                        sub.subscription.autoUpdate = action.enabled
                                        sub.subscription.updateInterval = action.minutes
                                        MmkvManager.encodeSubscription(sub.guid, sub.subscription)
                                        check(MmkvManager.decodeSubscription(sub.guid) == sub.subscription)
                                    }
                                } catch (error: Exception) {
                                    originals.forEach { (id, item) -> MmkvManager.encodeSubscription(id, item) }
                                    throw error
                                }
                            }
                            FutureFastAction.TestNow -> FutureFastOptimizer.runNow(getApplication())
                            FutureFastAction.DismissError -> Unit
                        }
                        loadState()
                    }
                }, afterPublish = {
                    withContext(Dispatchers.IO) {
                        when (action) {
                            is FutureFastAction.AutoSelect, is FutureFastAction.TestInterval ->
                                FutureFastOptimizer.schedule(getApplication())
                            is FutureFastAction.SubscriptionSchedule ->
                                MmkvManager.decodeSubscriptions().filter { it.subscription.url.isNotBlank() }
                                    .forEach { SubscriptionUpdater.syncOne(getApplication(), it.guid) }
                            else -> Unit
                        }
                    }
                })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Future Fast: settings operation failed", error)
                _error.value = R.string.ff_error
            }
        }
    }
}
