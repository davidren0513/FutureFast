package com.v2ray.ang.ui.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.v2ray.ang.ui.main.glass

@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    scrollState: ScrollState = rememberScrollState(),
    @Suppress("UNUSED_PARAMETER") containerColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(expanded, onDismissRequest,
        modifier = modifier.glass(LocalGlassBackdrop.current, RoundedCornerShape(24.dp)),
        offset = offset, scrollState = scrollState, containerColor = Color.Transparent,
        shape = RoundedCornerShape(24.dp), tonalElevation = 0.dp, shadowElevation = 0.dp, content = content)
}
