package com.theminesec.example.sdk.softpos.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun LabeledSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit),
) {
    val interactSrc = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable(
            interactionSource = interactSrc,
            indication = null,
            role = Role.Switch,
            onClick = { onCheckedChange(!checked) }
        )
    ) {
        Switch(
            checked = checked,
            interactionSource = interactSrc,
            onCheckedChange = onCheckedChange,
        )
        Text(text = label)
    }
}