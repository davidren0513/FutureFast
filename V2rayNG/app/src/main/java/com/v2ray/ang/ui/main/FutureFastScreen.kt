package com.v2ray.ang.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ripple
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.v2ray.ang.R
import com.v2ray.ang.dto.FutureFastAiService
import com.v2ray.ang.dto.FutureFastPolicy
import com.v2ray.ang.ui.compose.LocalDarkTheme
import com.v2ray.ang.ui.compose.GlassSwitch
import com.v2ray.ang.ui.compose.GlassAlertDialog

private val LocalCompactGlass = compositionLocalOf { false }

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
    val compact = LocalConfiguration.current.screenHeightDp < 760
    var showAiInfo by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("home") }
    val pageScroll = key(page) { rememberScrollState() }
    var aiPicker by rememberSaveable { mutableStateOf<String?>(null) }
    var intervalPicker by rememberSaveable { mutableStateOf<String?>(null) }
    val backdrop = rememberLayerBackdrop()
    val dark = LocalDarkTheme.current
    val accent = Color(0xFF287BFA)
    val ink = if (dark) Color(0xFFEDF4FF) else Color(0xFF14294D)
    val positive = if (dark) Color(0xFF67E1BA) else Color(0xFF16885F)
    val action = futureViewModel::onAction
    BackHandler(enabled = page != "home") { page = "home" }

    LaunchedEffect(state.selectedGuid) {
        // The selection may have changed in the background optimizer process.
        onAction(MainAction.RefreshSelectedServer)
    }

    CompositionLocalProvider(LocalContentColor provides ink, LocalCompactGlass provides compact) {
        Box(Modifier.fillMaxSize()) {
            FutureFastBackdrop(backdrop, dark)
                Scaffold(containerColor = Color.Transparent, contentColor = ink,
                    bottomBar = {
                        if (page != "automation") {
                            FutureFastNavigation(page, backdrop) { page = it }
                        }
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                        .then(if (page == "automation") Modifier.verticalScroll(pageScroll) else Modifier)
                        .imePadding().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)) {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (page == "automation") {
                                IconButton(onClick = { page = "home" }) {
                                    Icon(painterResource(R.drawable.ic_arrow_back_24dp), stringResource(R.string.acc_back))
                                }
                            }
                            Text(stringResource(when {
                                page == "automation" -> R.string.ff_automation
                                compact && page == "ai" -> R.string.ff_ai
                                compact && page == "nodes" -> R.string.ff_nodes
                                else -> R.string.app_name
                            }),
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f))
                            IconButton(onClick = { onNavigate(MainDestination.Settings) }, modifier = Modifier.glass(backdrop, CircleShape)) {
                                Icon(painterResource(R.drawable.ic_settings_24dp), stringResource(R.string.title_settings))
                            }
                        }
                        when (page) {
                            "home" -> {
                                Spacer(Modifier.height(2.dp))
                                Text(stringResource(R.string.ff_hero_title), style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(
                                        if (main.isRunning) Color(0xFF20AD78) else ink.copy(alpha = 0.35f)))
                                    Text(stringResource(if (main.isRunning) R.string.ff_connected else R.string.ff_disconnected),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (main.isRunning) positive else ink.copy(alpha = 0.65f))
                                }
                                GlassPowerButton(backdrop,
                                    stringResource(if (main.isRunning) R.string.ff_tap_disconnect else R.string.ff_connect),
                                    !state.loading, { onAction(MainAction.ToggleService) },
                                    Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp))
                                Row(Modifier.fillMaxWidth().glass(backdrop, RoundedCornerShape(32.dp), prominent = true)
                                    .clickable(role = Role.Button) { page = "nodes" }
                                    .semantics(mergeDescendants = true) {}
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    GlassIcon(GlassGlyph.NODES, Modifier.size(28.dp), accent)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(stringResource(if (main.isRunning) R.string.ff_current_node else R.string.ff_next_node),
                                            style = MaterialTheme.typography.labelMedium, color = ink.copy(alpha = 0.6f))
                                        Text(state.selectedName ?: stringResource(R.string.ff_add_service),
                                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                    if (state.delayMillis > 0) Text(stringResource(R.string.ff_delay, state.delayMillis),
                                        style = MaterialTheme.typography.labelLarge, color = positive)
                                    GlassIcon(GlassGlyph.ARROW, Modifier.size(18.dp), ink.copy(alpha = 0.65f))
                                }
                                GlassRow(backdrop, stringResource(R.string.ff_automation), stringResource(R.string.ff_automation_summary), glyph = GlassGlyph.LAYERS) { page = "automation" }

                            }
                            "nodes" -> {
                                if (!compact) Text(stringResource(R.string.ff_nodes), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                if (!compact) Text(stringResource(R.string.ff_nodes_summary), color = ink.copy(alpha = 0.65f))
                                FutureFastNodes(mainViewModel, backdrop, state.preferences, state.nodes, onAction)
                                ToggleCard(backdrop, stringResource(R.string.ff_auto_select), state.preferences.autoSelect,
                                    verticalPadding = 8.dp, onToggle = { action(FutureFastAction.AutoSelect(it)) }) {
                                    Text(stringResource(R.string.ff_node_selection_note), style = MaterialTheme.typography.bodySmall,
                                        color = ink.copy(alpha = 0.65f), modifier = Modifier.padding(bottom = 8.dp))
                                }

                            }
                            "automation" -> {
                                Text(stringResource(R.string.ff_auto_summary), color = ink.copy(alpha = 0.6f))
                                ToggleCard(backdrop, stringResource(R.string.ff_subscription_auto), state.subscriptionAuto,
                                    enabled = state.subscriptionCount > 0,
                                    onToggle = { action(FutureFastAction.SubscriptionSchedule(it, state.subscriptionMinutes)) }) {
                                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                                    GlassRow(null, stringResource(R.string.ff_subscription_interval), stringResource(R.string.ff_minutes, state.subscriptionMinutes), glyph = GlassGlyph.CLOCK) {
                                        intervalPicker = "subscription"
                                    }
                                    Text(stringResource(R.string.ff_schedule_note), style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.6f))
                                }
                                ToggleCard(backdrop, stringResource(R.string.ff_auto_select), state.preferences.autoSelect,
                                    onToggle = { action(FutureFastAction.AutoSelect(it)) }) {
                                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                                    GlassRow(null, stringResource(R.string.ff_test_interval), stringResource(R.string.ff_minutes, state.preferences.testIntervalMinutes), glyph = GlassGlyph.SPEED) {
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
                                if (!compact) Text(stringResource(R.string.ff_ai), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                if (!compact) Text(stringResource(R.string.ff_ai_summary), color = ink.copy(alpha = 0.7f))
                                ToggleCard(backdrop, stringResource(R.string.ff_ai_enable), state.preferences.aiEnabled,
                                    verticalPadding = if (compact) 4.dp else 18.dp,
                                    onToggle = { action(FutureFastAction.AiEnabled(it)) })
                                Column(Modifier.fillMaxWidth().glass(backdrop).padding(horizontal = 18.dp, vertical = 6.dp)) {
                                    FutureFastAiService.entries.forEach { service ->
                                        key(service.name) {
                                            val reference = state.preferences.aiNodes[service.name]
                                            val guid = reference?.let { FutureFastPolicy.resolveNode(it, state.nodes) }
                                            val name = if (reference == null) stringResource(R.string.ff_follow_default)
                                                else state.nodes.firstOrNull { it.guid == guid }?.remarks ?: stringResource(R.string.ff_node_missing)
                                            GlassRow(null, stringResource(aiTitle(service)), name, enabled = state.preferences.aiEnabled,
                                                badge = if (reference == null) stringResource(R.string.ff_default_badge) else stringResource(R.string.ff_dedicated_badge),
                                                glyph = when (service) { FutureFastAiService.CHATGPT -> GlassGlyph.CHAT; FutureFastAiService.CLAUDE -> GlassGlyph.CLAUDE; FutureFastAiService.GEMINI -> GlassGlyph.GEMINI }) { aiPicker = service.name }
                                            if (service != FutureFastAiService.entries.last()) HorizontalDivider(Modifier.padding(vertical = 2.dp))
                                        }
                                    }
                                }
                                if (compact) {
                                    TextButton(onClick = { showAiInfo = true }, contentPadding = PaddingValues(0.dp)) {
                                        Text(stringResource(R.string.ff_ai_short_note), style = MaterialTheme.typography.labelSmall, maxLines = 2)
                                    }
                                } else {
                                Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(painterResource(R.drawable.ic_lock_24dp), null, Modifier.size(18.dp), tint = ink.copy(alpha = 0.6f))
                                    Text(stringResource(R.string.ff_ai_short_note), style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.7f))
                                }
                                Text(stringResource(R.string.ff_ai_restart), style = MaterialTheme.typography.bodySmall)
                                }
                                if (main.isRunning) Button(onClick = { onAction(MainAction.RestartService) },
                                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = if (compact) 40.dp else 52.dp)) {
                                    Text(stringResource(R.string.ff_apply_ai))
                                }
                            }
                        }
                        if (page == "automation") Spacer(Modifier.height(16.dp))
                    }
                }
        }
    }
    if (showAiInfo) GlassAlertDialog(onDismissRequest = { showAiInfo = false },
        title = { Text(stringResource(R.string.ff_ai)) },
        text = { Text(stringResource(R.string.ff_ai_note) + "\n\n" + stringResource(R.string.ff_ai_restart)) },
        confirmButton = { TextButton(onClick = { showAiInfo = false }) { Text(stringResource(R.string.action_ok)) } })
    aiPicker?.let { id ->
        val service = FutureFastAiService.valueOf(id)
        val reference = state.preferences.aiNodes[id]
        val selected = reference?.let { FutureFastPolicy.resolveNode(it, state.nodes) }
        GlassAlertDialog(onDismissRequest = { aiPicker = null }, title = { Text(stringResource(aiTitle(service))) },
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
        GlassAlertDialog(onDismissRequest = { intervalPicker = null }, title = { Text(stringResource(R.string.ff_choose_interval)) },
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
    error?.let { errorId -> GlassAlertDialog(onDismissRequest = { action(FutureFastAction.DismissError) },
        text = { Text(stringResource(errorId)) }, confirmButton = { TextButton(onClick = { action(FutureFastAction.DismissError) }) { Text(stringResource(R.string.action_ok)) } }) }
}

private fun aiTitle(service: FutureFastAiService): Int = when (service) {
    FutureFastAiService.CHATGPT -> R.string.ff_chatgpt
    FutureFastAiService.CLAUDE -> R.string.ff_claude
    FutureFastAiService.GEMINI -> R.string.ff_gemini
}

@Composable
private fun GlassRow(backdrop: Backdrop?, title: String, subtitle: String, enabled: Boolean = true,
    glyph: GlassGlyph? = null, badge: String? = null, onClick: () -> Unit) {
    val compact = LocalCompactGlass.current
    Row((if (backdrop != null) Modifier.glass(backdrop, prominent = true) else Modifier)
        .fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) {}.padding(horizontal = if (backdrop != null) 16.dp else 0.dp, vertical = if (compact) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        glyph?.let {
            Box(Modifier.size(if (compact) 34.dp else 46.dp).clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.22f))
                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) {
                GlassIcon(it, Modifier.size(27.dp), when (it) {
                    GlassGlyph.CLAUDE -> Color(0xFFD77950)
                    GlassGlyph.LAYERS, GlassGlyph.GEMINI -> Color(0xFF3583FF)
                    else -> LocalContentColor.current.copy(alpha = if (enabled) 0.85f else 0.4f)
                })
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = LocalContentColor.current.copy(alpha = if (enabled) 1f else 0.5f))
            Text(subtitle, style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.65f),
                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        badge?.let { Text(it, style = MaterialTheme.typography.labelSmall,
            color = if (enabled) Color(0xFF287BFA) else LocalContentColor.current.copy(alpha = 0.5f)) }
        GlassIcon(GlassGlyph.ARROW, Modifier.size(19.dp), LocalContentColor.current.copy(alpha = 0.5f))
    }
}

/** One clipped card owns the action, including its padding and explanatory text.
 * Rows with an interval button retain that button as a separate child action.
 */
@Composable
private fun ToggleCard(backdrop: Backdrop, title: String, checked: Boolean,
    enabled: Boolean = true, verticalPadding: Dp = 18.dp, onToggle: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit = {}) {
    val interaction = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxWidth().glass(backdrop)
        .toggleable(value = checked, interactionSource = interaction, indication = ripple(bounded = true),
            enabled = enabled, role = Role.Switch, onValueChange = onToggle)
        .semantics(mergeDescendants = true) {}
        .padding(horizontal = 18.dp, vertical = verticalPadding)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            GlassSwitch(checked, onCheckedChange = null, enabled = enabled, interactionSource = interaction)
        }
        content()
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
