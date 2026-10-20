package com.v2ray.ang.ui.terms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.GlassAlertDialog

@Composable
fun UserTermsGate(viewModel: UserTermsViewModel, onDecline: () -> Unit, content: @Composable () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when {
        state.accepted -> content()
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else -> {
            // Keep the feature UI out of composition until consent has actually been persisted.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            }
            GlassAlertDialog(onDismissRequest = {}, title = { Text(stringResource(R.string.ff_terms_title)) },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                text = {
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.ff_terms_body), style = MaterialTheme.typography.bodyMedium)
                        if (state.error) Text(stringResource(R.string.ff_terms_save_error), color = MaterialTheme.colorScheme.error)
                    }
                },
                confirmButton = {
                    TextButton(enabled = !state.saving, onClick = { viewModel.onAction(UserTermsAction.Accept) }) {
                        Text(stringResource(if (state.saving) R.string.ff_terms_saving else R.string.ff_terms_accept))
                    }
                },
                dismissButton = {
                    TextButton(enabled = !state.saving, onClick = onDecline) { Text(stringResource(R.string.ff_terms_decline)) }
                })
        }
    }
}
