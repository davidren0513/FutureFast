package com.v2ray.ang.ui.terms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserTermsState(
    val loading: Boolean = true,
    val accepted: Boolean = false,
    val saving: Boolean = false,
    val error: Boolean = false,
)

enum class UserTermsAction { Accept }

class UserTermsViewModel internal constructor(
    private val store: UserTermsStore = MmkvUserTermsStore,
    // ViewModel owns and cancels this scope; injection lets JVM tests exercise pending writes.
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel(scope) {
    private val _state = MutableStateFlow(UserTermsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val result = store.version()
            _state.value = UserTermsState(loading = false,
                accepted = result.getOrNull()?.let(::acceptedUserTerms) == true,
                error = result.isFailure)
        }
    }

    fun onAction(action: UserTermsAction) {
        if (action != UserTermsAction.Accept || state.value.loading || state.value.saving || state.value.accepted) return
        _state.value = state.value.copy(saving = true, error = false)
        viewModelScope.launch {
            val result = store.accept()
            _state.value = state.value.copy(saving = false, accepted = result.isSuccess, error = result.isFailure)
        }
    }
}
