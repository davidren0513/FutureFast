package com.v2ray.ang.ui.main

import com.v2ray.ang.ui.compose.GlassDropdownMenu
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.Backdrop
import com.v2ray.ang.R
import com.v2ray.ang.dto.FutureFastPreferences
import com.v2ray.ang.dto.FutureFastNodeReference
import com.v2ray.ang.dto.FutureFastPolicy
import com.v2ray.ang.ui.compose.LocalDarkTheme
import com.v2ray.ang.ui.compose.QRCodeDialog

/** Node selection, import, editing, sharing and bulk tools operate directly on this list. */
@Composable
internal fun ColumnScope.FutureFastNodes(
    viewModel: MainViewModel,
    backdrop: Backdrop,
    preferences: FutureFastPreferences,
    nodes: List<FutureFastNodeReference>,
    onAction: (MainAction) -> Unit,
) {
    val main by viewModel.uiState.collectAsStateWithLifecycle()
    val groupFlow = remember(viewModel, main.selectedGroupId) { viewModel.serverGroupState(main.selectedGroupId) }
    val group by groupFlow.collectAsStateWithLifecycle()
    val loading by viewModel.isLoading.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LaunchedEffect(main.locateTarget, group.rows) {
        main.locateTarget?.takeIf { it.groupId == main.selectedGroupId }?.let { target ->
            val index = group.rows.indexOfFirst { it.guid == target.serverGuid }
            if (index >= 0) listState.animateScrollToItem(index)
            onAction(MainAction.LocateHandled)
        }
    }
    var groupMenu by rememberSaveable { mutableStateOf(false) }
    var importMenu by rememberSaveable { mutableStateOf(false) }
    var moreMenu by rememberSaveable { mutableStateOf(false) }
    var rowMenuGuid by rememberSaveable { mutableStateOf<String?>(null) }
    var removeTarget by rememberSaveable(stateSaver = ServerDeleteTarget.Saver) { mutableStateOf<ServerDeleteTarget?>(null) }
    var deleteAll by rememberSaveable { mutableStateOf(false) }
    var deleteDuplicate by rememberSaveable { mutableStateOf(false) }
    var deleteInvalid by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    MainDialogs(deleteAll, { deleteAll = false }, { deleteAll = false; onAction(MainAction.RemoveAllServers) },
        deleteDuplicate, { deleteDuplicate = false }, { deleteDuplicate = false; onAction(MainAction.RemoveDuplicateServers) },
        deleteInvalid, { deleteInvalid = false }, { deleteInvalid = false; onAction(MainAction.RemoveInvalidServers) },
        removeTarget, { removeTarget = null }, { guid -> removeTarget = null; onAction(MainAction.RemoveServer(guid)) })
    QRCodeDialog(main.shareQRCodeBitmap) { onAction(MainAction.DismissQRCodeDialog) }
    group.rows.firstOrNull { it.guid == rowMenuGuid }?.let { node ->
        ShareMethodDialog(node.guid, node.profile, true, onDismiss = { rowMenuGuid = null }, onAction = onAction,
            onRemove = { guid, name ->
                if (main.confirmRemove) removeTarget = ServerDeleteTarget(guid, name) else onAction(MainAction.RemoveServer(guid))
            })
    }
    if (showSearch) {
        OutlinedTextField(searchText, onValueChange = { searchText = it; onAction(MainAction.Search(it)) },
            label = { Text(stringResource(R.string.menu_item_search)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
    val ink = LocalContentColor.current
    val accent = Color(0xFF287BFA)
    val positive = if (LocalDarkTheme.current) Color(0xFF67E1BA) else Color(0xFF16885F)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
            TextButton(onClick = { groupMenu = true }, modifier = Modifier.glass(backdrop, CircleShape)) {
                GlassIcon(GlassGlyph.NODES, tint = accent)
                Text(main.groups.firstOrNull { it.id == main.selectedGroupId }?.remarks
                    ?: stringResource(R.string.ff_nodes), Modifier.padding(horizontal = 8.dp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(painterResource(R.drawable.ic_expand_more_24dp), null)
            }
            GlassDropdownMenu(expanded = groupMenu, onDismissRequest = { groupMenu = false }) {
                main.groups.forEach { item -> key(item.id) {
                    DropdownMenuItem(text = { Text(item.remarks) }, onClick = {
                        onAction(MainAction.SelectGroup(item.id)); groupMenu = false
                    })
                } }
            }
        }
        TextButton(onClick = { onAction(MainAction.TestRealAllServers) },
            enabled = !main.isTesting && group.rows.isNotEmpty(), modifier = Modifier.glass(backdrop, CircleShape)) {
            Text(stringResource(if (main.isTesting) R.string.connection_test_testing else R.string.ff_measure))
        }
        Box {
            IconButton(onClick = { importMenu = true }, modifier = Modifier.glass(backdrop, CircleShape)) {
                Icon(painterResource(R.drawable.ic_add_24dp), stringResource(R.string.ff_add_service))
            }
            GlassDropdownMenu(expanded = importMenu, onDismissRequest = { importMenu = false }) {
                ImportMenuContent { event -> importMenu = false; onAction(event) }
            }
        }
        Box {
            IconButton(onClick = { moreMenu = true }, modifier = Modifier.glass(backdrop, CircleShape)) {
                Icon(painterResource(R.drawable.ic_more_vert_24dp), stringResource(R.string.acc_more))
            }
            GlassDropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.menu_item_search)) }, onClick = { moreMenu = false; showSearch = !showSearch; if (!showSearch) { searchText = ""; onAction(MainAction.Search("")) } })
                MoreMenuContent { event ->
                    moreMenu = false
                    when (event) {
                        MainMoreMenuAction.DeleteAll -> deleteAll = true
                        MainMoreMenuAction.DeleteDuplicate -> deleteDuplicate = true
                        MainMoreMenuAction.DeleteInvalid -> deleteInvalid = true
                        MainMoreMenuAction.RestartService -> onAction(MainAction.RestartService)
                        MainMoreMenuAction.ExportAll -> onAction(MainAction.ExportAll)
                        MainMoreMenuAction.LocateSelected -> onAction(MainAction.LocateSelectedServer)
                        MainMoreMenuAction.SortByTestResults -> onAction(MainAction.SortByTestResults)
                        MainMoreMenuAction.TestAll -> onAction(MainAction.TestAllServers)
                        MainMoreMenuAction.TestAllRealPing -> onAction(MainAction.TestRealAllServers)
                        MainMoreMenuAction.UpdateSubscriptions -> onAction(MainAction.UpdateSubscriptions)
                    }
                }
            }
        }
    }
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (group.rows.isEmpty() && !loading) {
        Column(Modifier.fillMaxWidth().glass(backdrop).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GlassIcon(GlassGlyph.NODES, Modifier.size(40.dp), accent)
            Text(stringResource(R.string.ff_empty_nodes), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.ff_empty_nodes_hint), style = MaterialTheme.typography.bodyMedium,
                color = ink.copy(alpha = 0.65f))
            Button(onClick = { onAction(MainAction.ImportClipboard) }) { Text(stringResource(R.string.ff_paste)) }
            TextButton(onClick = { onAction(MainAction.ImportQRcode) }) { Text(stringResource(R.string.ff_scan)) }
        }
    } else if (group.rows.isNotEmpty()) {
        // A bounded, lazy list avoids composing every server in a large subscription.
        LazyColumn(Modifier.fillMaxWidth().weight(1f).glass(backdrop).padding(8.dp), state = listState) {
            items(group.rows, key = { it.guid }) { node ->
                val selected = node.guid == main.selectedGuid
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(if (selected) accent.copy(alpha = 0.09f) else Color.Transparent)
                    .selectable(selected, role = Role.RadioButton,
                        onClick = { onAction(MainAction.SelectServer(node.guid)) })
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                        GlassIcon(GlassGlyph.NODES, Modifier.size(22.dp), accent)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(node.remarks, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(if (selected) R.string.ff_current_node
                            else if (preferences.aiEnabled && preferences.aiNodes.values.any { FutureFastPolicy.resolveNode(it, nodes) == node.guid }) R.string.ff_dedicated_badge
                            else R.string.ff_node_ready), style = MaterialTheme.typography.labelMedium,
                            color = ink.copy(alpha = 0.6f))
                    }
                    Text(if (node.testDelayMillis > 0) stringResource(R.string.ff_delay, node.testDelayMillis)
                        else stringResource(if (node.testDelayMillis < 0) R.string.ff_test_failed else R.string.ff_untested),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (node.testDelayMillis > 0) positive else ink.copy(alpha = 0.6f))
                    RadioButton(selected = selected, onClick = null, modifier = Modifier.size(24.dp),
                        colors = RadioButtonDefaults.colors(selectedColor = accent))
                    IconButton(onClick = { rowMenuGuid = node.guid }, modifier = Modifier.size(32.dp)) {
                        Icon(painterResource(R.drawable.ic_more_vert_24dp), stringResource(R.string.acc_more), tint = ink.copy(alpha = 0.6f))
                    }
                }
                if (node.guid != group.rows.last().guid) HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = ink.copy(alpha = 0.08f))
            }
        }
    }
}
