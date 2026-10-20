package com.v2ray.ang.ui.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.v2ray.ang.ui.terms.UserTermsGate
import com.v2ray.ang.ui.terms.UserTermsViewModel
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.ui.compose.AppTheme

abstract class BaseComponentActivity : AppCompatActivity() {
    private val userTermsViewModel: UserTermsViewModel by viewModels()
    private var termsReadyDelivered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLocaleManager.onActivityCreated(this)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                UserTermsGate(userTermsViewModel, onDecline = { finishAndRemoveTask() }) {
                    ScreenContent()
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userTermsViewModel.state.collect { state ->
                    if (state.accepted && !termsReadyDelivered) {
                        termsReadyDelivered = true
                        onUserTermsAccepted()
                    }
                }
            }
        }
    }

    /** Feature entry work runs only once, after consent and after subclass initialization. */
    protected open fun onUserTermsAccepted() = Unit

    @Composable
    protected abstract fun ScreenContent()
}