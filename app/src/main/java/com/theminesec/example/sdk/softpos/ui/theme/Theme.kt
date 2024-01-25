package com.theminesec.example.sdk.softpos.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

val Purple = Color(0xffB971FF)
val Grey = Color(0xff2B3E64)
val DarkGrey = Color(0xff101A2D)

@Composable
fun MsExampleSdkSoftPOSTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Purple,
        secondary = Grey,
        background = DarkGrey,
        onBackground = Color.White
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Type,
        content = content
    )
}