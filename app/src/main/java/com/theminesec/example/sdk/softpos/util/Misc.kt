package com.theminesec.example.sdk.softpos.util

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

inline fun <reified T> Context.loadJsonFromAsset(fileName: String): T {
    val jsonStr = applicationContext.assets
        .open(fileName)
        .bufferedReader()
        .use { it.readText() }
    val type = object : TypeToken<T>() {}.type
    return Gson().fromJson(jsonStr, type)
}

tailrec fun Context.findActivity(): ComponentActivity = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> throw IllegalStateException("no activity")
}

fun ByteArray.isAllZero() = all { it == 0.toByte() }
fun ByteArray.prependLengthByte() = byteArrayOf(this.size.toByte()) + this
fun ByteArray.trimLengthByte() = copyOfRange(1, this.size)

fun String.removeSpace() = replace(" ", "")

fun Int.sequentialString() = (1..this).joinToString("") { (it % 10).toString() }