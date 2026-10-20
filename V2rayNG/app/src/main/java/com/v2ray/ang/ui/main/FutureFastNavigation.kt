package com.v2ray.ang.ui.main

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.LocalDarkTheme
import com.v2ray.ang.ui.main.liquid.LiquidBottomTab
import com.v2ray.ang.ui.main.liquid.LiquidBottomTabs

/** The official catalog lens samples both the background and a second layer of tab icons/text. */
@Composable
internal fun FutureFastNavigation(page: String, backdrop: Backdrop, onPage: (String) -> Unit) {
    val pages = listOf("home", "nodes", "ai")
    val labels = listOf(R.string.ff_home, R.string.ff_nodes, R.string.ff_ai)
    val ink = if (LocalDarkTheme.current) Color(0xFFE8F3FF) else Color(0xFF173451)
    val accent = Color(0xFF0088FF)
    val modifier = Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp).fillMaxWidth()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LiquidBottomTabs(selectedTabIndex = { pages.indexOf(page).coerceAtLeast(0) },
            onTabSelected = { onPage(pages[it]) }, backdrop = backdrop, tabsCount = pages.size, selectedTabLabel = stringResource(labels[pages.indexOf(page).coerceAtLeast(0)]), modifier = modifier) {
            pages.forEachIndexed { index, id ->
                LiquidBottomTab(selected = page == id, onClick = { onPage(id) }) {
                    GlassIcon(when (id) { "home" -> GlassGlyph.LINK; "nodes" -> GlassGlyph.NODES; else -> GlassGlyph.AI },
                        tint = if (page == id) accent else ink.copy(alpha = 0.7f))
                    Text(stringResource(labels[index]), color = if (page == id) accent else ink.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    } else {
        // AGSL lenses require Android 13. Remove this material fallback when minSdk reaches 33.
        Row(modifier.glass(backdrop, RoundedCornerShape(40.dp)).padding(4.dp)) {
            pages.forEachIndexed { index, id ->
                Column(Modifier.weight(1f).background(if (page == id) accent.copy(alpha = 0.15f) else Color.Transparent,
                    RoundedCornerShape(32.dp)).selectable(page == id, role = Role.Tab, onClick = { onPage(id) })
                    .semantics(mergeDescendants = true) {}.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(labels[index]), color = ink, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
