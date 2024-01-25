package com.theminesec.example.sdk.softpos.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun Title(text: String) = Text(text, style = MaterialTheme.typography.titleMedium)
