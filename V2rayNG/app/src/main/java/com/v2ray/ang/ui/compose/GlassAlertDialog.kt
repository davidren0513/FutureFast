package com.v2ray.ang.ui.compose

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.v2ray.ang.ui.main.glass

/** Keep Material's focus, window and button behavior, replacing the opaque dialog material. */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
    @Suppress("UNUSED_PARAMETER") containerColor: Color = Color.Transparent,
) {
    AlertDialog(onDismissRequest = onDismissRequest, confirmButton = confirmButton,
        modifier = modifier.glass(LocalGlassBackdrop.current, RoundedCornerShape(28.dp), prominent = true),
        dismissButton = dismissButton, icon = icon, title = title, text = text,
        containerColor = Color.Transparent, tonalElevation = 0.dp, properties = properties)
}
