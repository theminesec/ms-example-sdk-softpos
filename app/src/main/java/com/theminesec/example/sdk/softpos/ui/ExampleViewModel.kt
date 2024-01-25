package com.theminesec.example.sdk.softpos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@JvmInline
value class Iv(val value: String)

@JvmInline
value class Ksn(val value: String)

@JvmInline
value class Encrypted57(val value: String)

@JvmInline
value class EncryptedPinBlock(val value: String)

@JvmInline
value class PanToken(val value: String)

@OptIn(ExperimentalStdlibApi::class)
class ExampleViewModel(private val app: Application) : AndroidViewModel(app) {
    // for demo setups
    private val _messages: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val messages: StateFlow<List<String>> = _messages
    private val prettyGson = GsonBuilder().setPrettyPrinting().create()
    fun writeMessage(message: String) = viewModelScope.launch {
        val temp = _messages.value
            .toMutableList()
            .apply { add("==> $message") }

        _messages.emit(temp)
    }

    fun clearLog() = viewModelScope.launch { _messages.emit(emptyList()) }

    // Init SDK
    // https://docs.minesec.tools/tech-sdk/getting-started/quickstart#init-sdk
    private fun initSdk() = viewModelScope.launch(Dispatchers.IO) {
        writeMessage("Start init SDK")
        TODO()
    }

    fun getSdkInfo() {
        TODO()
    }

    // EMV Apps
    // https://docs.minesec.tools/tech-sdk/getting-started/quickstart#emv-kernel-app-param
    fun setEmvApps() = viewModelScope.launch(Dispatchers.Default) {
        writeMessage("setEmvApps")
        TODO()
    }

    fun getEmvApps() = viewModelScope.launch {
        writeMessage("getEmvApps")
        TODO()
    }

    // CAPKs
    // https://docs.minesec.tools/tech-sdk/getting-started/quickstart#capk-param
    fun setCapks() = viewModelScope.launch(Dispatchers.Default) {
        writeMessage("setCapks")
        TODO()
    }

    fun getCapks() {
        writeMessage("getCapks")
        TODO()
    }

    // Terminal Param
    // https://docs.minesec.tools/tech-sdk/getting-started/quickstart#terminal-device-param
    fun setTermParam() = viewModelScope.launch(Dispatchers.Default) {
        writeMessage("setTermParam")
        TODO()
    }

    fun getTermParam() = viewModelScope.launch {
        writeMessage("getTermParam")
        TODO()
    }

    // Key ceremony
    // https://docs.minesec.tools/tech-sdk/getting-started/quickstart#key-ceremony
    fun injectCardInitialKey() = viewModelScope.launch(Dispatchers.Default) {
        TODO()
    }

    fun injectPinInitialKey() = viewModelScope.launch(Dispatchers.Default) {
        TODO()
    }

    // Card Read & PIN
    // https://docs.minesec.tools/tech-sdk/getting-started/quickstart#request-contactless-card-read
    private val _cardReadResult: MutableStateFlow<Triple<Ksn, Iv, Encrypted57>?> = MutableStateFlow(null)
    val cardReadResult = _cardReadResult.asStateFlow()
    fun setCardReadResult(result: Triple<Ksn, Iv, Encrypted57>?) = viewModelScope.launch {
        _cardReadResult.emit(result)
    }

    private val _encryptedPinData: MutableStateFlow<Triple<Ksn, EncryptedPinBlock, PanToken?>?> = MutableStateFlow(null)
    val encryptedPinData = _encryptedPinData.asStateFlow()
    fun setEncryptedPinData(result: Triple<Ksn, EncryptedPinBlock, PanToken?>?) = viewModelScope.launch {
        _encryptedPinData.emit(result)
    }

    // DEMO PURPOSE ONLY
    // Data decrypt
    // https://docs.minesec.tools/tech-sdk/getting-started/quickstart#decrypt-data
    fun dangerouslyDecryptCardDataLocally() {
        writeMessage("card read result: ${cardReadResult.value.toString()}")
        TODO()
    }

    fun dangerouslyDecryptPinDataLocally() {
        writeMessage("epb: ${encryptedPinData.value.toString()}")
        TODO()
    }
}