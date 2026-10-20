package com.v2ray.ang.ui.main

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.v2ray.ang.R
import com.v2ray.ang.dto.FutureFastAiService
import com.v2ray.ang.dto.FutureFastPolicy
import com.v2ray.ang.ui.compose.LocalDarkTheme

@Composable
fun FutureFastScreen(
    mainViewModel: MainViewModel,
    futureViewModel: FutureFastViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val main by mainViewModel.uiState.collectAsStateWithLifecycle()
    val state by futureViewModel.state.collectAsStateWithLifecycle()
    val error by futureViewModel.error.collectAsStateWithLifecycle()
    val intervalInput by futureViewModel.intervalInput.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("home") }
    var aiPicker by rememberSaveable { mutableStateOf<String?>(null) }
    var intervalPicker by rememberSaveable { mutableStateOf<String?>(null) }
    val backdrop = rememberLayerBackdrop()
    val dark = LocalDarkTheme.current
    val accent = Color(0xFF287BFA)
    val ink = if (dark) Color(0xFFEDF4FF) else Color(0xFF14294D)
    val action = futureViewModel::onAction
    BackHandler(enabled = page != "home") { page = "home" }

    LaunchedEffect(state.selectedGuid) {
        // The selection may have changed in the background optimizer process.
        onAction(MainAction.RefreshSelectedServer)
    }

    CompositionLocalProvider(LocalContentColor provides ink) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(Brush.linearGradient(
                if (dark) listOf(Color(0xFF101D32), Color(0xFF1C2947), Color(0xFF122E3C))
                else listOf(Color(0xFFF8FCFF), Color(0xFFE3F2FF), Color(0xFFF0EAFE), Color(0xFFE4F8FC))
            )))
            if (page == "nodes") {
                Column(Modifier.fillMaxSize().navigationBarsPadding()) {
                    Row(Modifier.statusBarsPadding().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { page = "home" }) { Text(stringResource(R.string.ff_home)) }
                        Text(stringResource(R.string.ff_node_management), fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.weight(1f)) { MainScreen(mainViewModel, onAction, onNavigate) }
                }
            } else {
                Scaffold(containerColor = Color.Transparent, contentColor = ink,
                    bottomBar = {
                        if (page != "automation") {
                            Row(Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)
                                .fillMaxWidth().glass(backdrop).padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                listOf("home" to R.string.ff_home, "nodes" to R.string.ff_nodes, "ai" to R.string.ff_ai).forEach { (id, title) ->
                                    Box(Modifier.weight(1f).clip(RoundedCornerShape(32.dp))
                                        .background(if (page == id) accent.copy(alpha = 0.15f) else Color.Transparent)
                                        .selectable(page == id, role = Role.Tab, onClick = { page = id }).padding(vertical = 18.dp),
                                        contentAlignment = Alignment.Center) {
                                        Text(stringResource(title), fontWeight = if (page == id) FontWeight.Bold else FontWeight.Normal,
                                            color = if (page == id) accent else ink)
                                    }
                                }
                            }
                        }
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                        .verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (page == "automation") {
                                IconButton(onClick = { page = "home" }) {
                                    Icon(painterResource(R.drawable.ic_arrow_back_24dp), stringResource(R.string.acc_back))
                                }
                            }
                            Text(stringResource(if (page == "automation") R.string.ff_automation else if (page == "ai") R.string.ff_ai else R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onNavigate(MainDestination.Settings) }) {
                                Icon(painterResource(R.drawable.ic_settings_24dp), stringResource(R.string.title_settings))
                            }
                        }
                        when (page) {
                            "home" -> {
                                Spacer(Modifier.height(44.dp))
                                Text(stringResource(if (main.isRunning) R.string.ff_connected else R.string.ff_disconnected),
                                    Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleMedium)
                                Box(Modifier.align(Alignment.CenterHorizontally).size(190.dp)
                                    .glass(backdrop, CircleShape)
                                    .clickable(enabled = !state.loading, role = Role.Button) { onAction(MainAction.ToggleService) }
                                    .semantics(mergeDescendants = true) {}, contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(painterResource(if (main.isRunning) R.drawable.ic_stop_24dp else R.drawable.ic_play_24dp),
                                            contentDescription = null, modifier = Modifier.size(56.dp), tint = accent)
                                        Text(stringResource(if (main.isRunning) R.string.ff_disconnect else R.string.ff_connect),
                                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Text(stringResource(if (main.isRunning) R.string.ff_current_node else R.string.ff_next_node),
                                    Modifier.align(Alignment.CenterHorizontally), color = ink.copy(alpha = 0.65f))
                                TextButton(onClick = { page = "nodes" }, Modifier.align(Alignment.CenterHorizontally)) {
                                    Text(state.selectedName ?: stringResource(R.string.ff_add_service), style = MaterialTheme.typography.titleMedium)
                                }
                                if (state.delayMillis > 0) Text(stringResource(R.string.ff_delay, state.delayMillis),
                                    Modifier.align(Alignment.CenterHorizontally), color = accent)
                                Spacer(Modifier.height(24.dp))
                                GlassRow(backdrop, stringResource(R.string.ff_automation), stringResource(R.string.ff_automation_summary)) { page = "automation" }
                                if (state.nodes.isEmpty() && !state.loading) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                        TextButton(onClick = { onAction(MainAction.ImportClipboard) }) { Text(stringResource(R.string.ff_paste)) }
                                        TextButton(onClick = { onAction(MainAction.ImportQRcode) }) { Text(stringResource(R.string.ff_scan)) }
                                    }
                                }
                                TextButton(onClick = { onNavigate(MainDestination.About) }, Modifier.align(Alignment.CenterHorizontally)) {
                                    Text(stringResource(R.string.ff_about_link), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            "automation" -> {
                                Text(stringResource(R.string.ff_schedule_note), color = ink.copy(alpha = 0.7f))
                                Column(Modifier.fillMaxWidth().glass(backdrop).padding(18.dp)) {
                                    ToggleRow(stringResource(R.string.ff_subscription_auto), state.subscriptionAuto, state.subscriptionCount > 0) {
                                        action(FutureFastAction.SubscriptionSchedule(it, state.subscriptionMinutes))
                                    }
                                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                                    GlassRow(null, stringResource(R.string.ff_subscription_interval), stringResource(R.string.ff_minutes, state.subscriptionMinutes)) {
                                        intervalPicker = "subscription"
                                    }
                                    TextButton(onClick = { onNavigate(MainDestination.Subscriptions) }) { Text(stringResource(R.string.ff_manage_subscriptions)) }
                                }
                                Column(Modifier.fillMaxWidth().glass(backdrop).padding(18.dp)) {
                                    ToggleRow(stringResource(R.string.ff_auto_select), state.preferences.autoSelect) { action(FutureFastAction.AutoSelect(it)) }
                                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                                    GlassRow(null, stringResource(R.string.ff_test_interval), stringResource(R.string.ff_minutes, state.preferences.testIntervalMinutes)) {
                                        intervalPicker = "test"
                                    }
                                    Text(stringResource(R.string.ff_test_note), style = MaterialTheme.typography.bodySmall)
                                }
                                val outcome = when (state.preferences.lastTestOutcome) {
                                    "selected" -> R.string.ff_test_selected
                                    "none" -> R.string.ff_test_none
                                    "paused" -> R.string.ff_test_paused
                                    "error" -> R.string.ff_error
                                    else -> R.string.ff_test_idle
                                }
                                Text(stringResource(outcome), style = MaterialTheme.typography.bodyMedium)
                                OutlinedButton(onClick = { action(FutureFastAction.TestNow) }, enabled = !main.isRunning && state.nodes.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ff_test_now)) }
                            }
                            "ai" -> {
                                Text(stringResource(R.string.ff_ai_summary), color = ink.copy(alpha = 0.7f))
                                Column(Modifier.fillMaxWidth().glass(backdrop).padding(18.dp)) {
                                    ToggleRow(stringResource(R.string.ff_ai_enable), state.preferences.aiEnabled) { action(FutureFastAction.AiEnabled(it)) }
                                }
                                Column(Modifier.fillMaxWidth().glass(backdrop).padding(18.dp)) {
                                    FutureFastAiService.entries.forEach { service ->
                                        key(service.name) {
                                            val reference = state.preferences.aiNodes[service.name]
                                            val guid = reference?.let { FutureFastPolicy.resolveNode(it, state.nodes) }
                                            val name = if (reference == null) stringResource(R.string.ff_follow_default)
                                                else state.nodes.firstOrNull { it.guid == guid }?.remarks ?: stringResource(R.string.ff_node_missing)
                                            GlassRow(null, stringResource(aiTitle(service)), name, enabled = state.preferences.aiEnabled) { aiPicker = service.name }
                                            if (service != FutureFastAiService.entries.last()) HorizontalDivider(Modifier.padding(vertical = 10.dp))
                                        }
                                    }
                                }
                                Text(stringResource(R.string.ff_ai_note), style = MaterialTheme.typography.bodySmall)
                                Text(stringResource(R.string.ff_ai_restart), style = MaterialTheme.typography.bodySmall)
                                if (main.isRunning) OutlinedButton(onClick = { onAction(MainAction.RestartService) }, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.ff_apply_ai))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
    aiPicker?.let { id ->
        val service = FutureFastAiService.valueOf(id)
        val reference = state.preferences.aiNodes[id]
        val selected = reference?.let { FutureFastPolicy.resolveNode(it, state.nodes) }
        AlertDialog(onDismissRequest = { aiPicker = null }, title = { Text(stringResource(aiTitle(service))) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    SelectNodeRow(stringResource(R.string.ff_follow_default), reference == null) {
                        action(FutureFastAction.AiNode(service, null)); aiPicker = null
                    }
                    state.nodes.forEach { node -> key(node.guid) {
                        SelectNodeRow(node.remarks, node.guid == selected) {
                            action(FutureFastAction.AiNode(service, node.guid)); aiPicker = null
                        }
                    } }
                }
            }, confirmButton = { TextButton(onClick = { aiPicker = null }) { Text(stringResource(R.string.action_cancel)) } })
    }
    intervalPicker?.let { kind ->
        AlertDialog(onDismissRequest = { intervalPicker = null }, title = { Text(stringResource(R.string.ff_choose_interval)) },
            text = { Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()).imePadding()) {
                OutlinedTextField(value = intervalInput.text,
                    onValueChange = { action(FutureFastAction.IntervalInput(it)) },
                    label = { Text(stringResource(R.string.ff_custom_interval)) },
                    isError = intervalInput.text.isNotEmpty() && intervalInput.minutes == null,
                    supportingText = { if (intervalInput.text.isNotEmpty() && intervalInput.minutes == null) Text(stringResource(R.string.ff_interval_invalid)) },
                    singleLine = true)
                listOf(15L, 30L, 60L, 180L, 360L, 720L, 1440L).forEach { minutes ->
                    TextButton(onClick = {
                        action(if (kind == "test") FutureFastAction.TestInterval(minutes)
                            else FutureFastAction.SubscriptionSchedule(state.subscriptionAuto, minutes))
                        intervalPicker = null
                    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ff_minutes, minutes)) }
                }
            } }, confirmButton = {
                TextButton(enabled = intervalInput.minutes != null, onClick = {
                    intervalInput.minutes?.let { minutes ->
                        action(if (kind == "test") FutureFastAction.TestInterval(minutes)
                            else FutureFastAction.SubscriptionSchedule(state.subscriptionAuto, minutes))
                        intervalPicker = null
                    }
                }) { Text(stringResource(R.string.action_ok)) }
            }, dismissButton = { TextButton(onClick = { intervalPicker = null }) { Text(stringResource(R.string.action_cancel)) } })
    }
    error?.let { errorId -> AlertDialog(onDismissRequest = { action(FutureFastAction.DismissError) },
        text = { Text(stringResource(errorId)) }, confirmButton = { TextButton(onClick = { action(FutureFastAction.DismissError) }) { Text(stringResource(R.string.action_ok)) } }) }
}

private fun aiTitle(service: FutureFastAiService): Int = when (service) {
    FutureFastAiService.CHATGPT -> R.string.ff_chatgpt
    FutureFastAiService.CLAUDE -> R.string.ff_claude
    FutureFastAiService.GEMINI -> R.string.ff_gemini
}

/** Real refraction on Android 13+, with a clear, readable translucent fallback. */
private fun Modifier.glass(backdrop: Backdrop, shape: Shape = RoundedCornerShape(28.dp)): Modifier =
    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) drawBackdrop(
        backdrop = backdrop, shape = { shape }, effects = { vibrancy(); blur(8.dp.toPx()); lens(12.dp.toPx(), 24.dp.toPx()) },
        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
    ) else background(Color(0xFFBBD4E9).copy(alpha = 0.25f), shape))
        .clip(shape).border(1.dp, Color.White.copy(alpha = 0.48f), shape)

@Composable
private fun GlassRow(backdrop: Backdrop?, title: String, subtitle: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column((if (backdrop != null) Modifier.glass(backdrop) else Modifier)
        .fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) {}.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.65f))
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, enabled: Boolean = true, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onToggle)
        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun SelectNodeRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick = null)
        Text(title, Modifier.padding(start = 12.dp))
    }
}
