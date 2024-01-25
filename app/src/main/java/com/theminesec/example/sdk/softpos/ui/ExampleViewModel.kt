package com.theminesec.example.sdk.softpos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.theminesec.MineHades.*
import com.theminesec.example.sdk.softpos.converter.*
import com.theminesec.example.sdk.softpos.util.isAllZero
import com.theminesec.example.sdk.softpos.util.loadJsonFromAsset
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
    private val sdk = MhdCPOC.getInstance(app)

    init {
        initSdk()
    }

    private fun initSdk() = viewModelScope.launch(Dispatchers.IO) {
        writeMessage("Start init SDK")
        val yourDeviceId = "client-example-123"
        val licenseFileName = "example.license"
        val initResult = sdk.MhdCPOC_Init2(app, licenseFileName, yourDeviceId)
        writeMessage("init result: \n${prettyGson.toJson(initResult)}")
    }

    fun getSdkInfo() {
        writeMessage("SDK version: ${sdk.mineHadesVersion}")
        writeMessage("SDK ID (By MineSec): ${sdk.mineHadesIdentifier}")
    }

    // EMV Apps
    // https://docs.minesec.tools/tech-sdk/getting-started/quickstart#emv-kernel-app-param
    fun setEmvApps() = viewModelScope.launch(Dispatchers.Default) {
        writeMessage("setEmvApps")
        val emvApps: List<EmvApp> = app.loadJsonFromAsset("emv-app.json")
        emvApps.forEach { emvApp ->
            val result = sdk.MhdEmv_AddApp(emvApp.toMhdEmvApp())
            writeMessage("EMV AID ${emvApp.aid}\nsuccess?: ${result == MhdReturnCode.MHD_SUCCESS}")
        }
    }

    private val maxEmvAppSlots = 32
    fun getEmvApps() = viewModelScope.launch {
        writeMessage("getEmvApps")
        for (i in 0 until maxEmvAppSlots) {
            val emvAppDump = EMV_APPLIST()
            sdk.MhdEmv_GetApp(i, emvAppDump)
            if (emvAppDump.aid.isAllZero()) break

            writeMessage("EmvApp $i, AID: ${emvAppDump.toEmvApp().aid}\n${emvAppDump.toEmvApp()}")
        }
    }

    // CAPKs
    // https://docs.minesec.tools/tech-sdk/getting-started/quickstart#capk-param
    fun setCapks() = viewModelScope.launch(Dispatchers.Default) {
        writeMessage("setCapks")
        val capks: List<Capk> = app.loadJsonFromAsset("capk.json")

        capks.filter { it.keyType == CapkKeyType.TEST }
            .forEach { capk ->
                val result = sdk.MhdEmv_AddCapk(capk.toMhdCapk())
                writeMessage("CAPK RID ${capk.rid}\nsuccess?: ${result == MhdReturnCode.MHD_SUCCESS}")
            }
    }

    private val maxCapkSlots = 64
    fun getCapks() {
        writeMessage("getCapks")
        for (i in 0 until maxCapkSlots) {
            val msCapkDump = EMVCAPK()
            sdk.MhdEmv_GetCapk(i, msCapkDump)
            if (msCapkDump.rid.isAllZero()) break

            writeMessage("Capk $i, RID: ${msCapkDump.toCapk().rid}, key index: ${msCapkDump.toCapk().keyIndex}\n${msCapkDump.toCapk()}")
        }
    }

    // Terminal Param
    // https://docs.minesec.tools/tech-sdk/getting-started/quickstart#terminal-device-param
    fun setTermParam() = viewModelScope.launch(Dispatchers.Default) {
        writeMessage("setTermParam")

        // termCap: 0060c8
        // × plaintext offline pin
        // ✓ enciphered online pin
        // ✓ signature
        // × enciphered offline pin
        // × no cvm
        val termParams: TerminalParam = app.loadJsonFromAsset("term.json")
        val result = sdk.MhdEmv_SetParam(termParams.toMhdTermParams(), app)
        writeMessage("TermParams success?: ${result == MhdReturnCode.MHD_SUCCESS}")
    }

    fun getTermParam() = viewModelScope.launch {
        writeMessage("getTermParam")

        val dump = EMV_PARAM()
        sdk.MhdEmv_GetParam(dump)
        writeMessage("getTermParam: ${dump.toTermParams()}")
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