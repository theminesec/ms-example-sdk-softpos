package com.theminesec.example.sdk.softpos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.theminesec.MineHades.*
import com.theminesec.MineHades.KMS.DukptKeyGenParameter
import com.theminesec.MineHades.KMS.KeyLoader
import com.theminesec.MineHades.KMS.MsKeyProperties
import com.theminesec.MineHades.KMS.MsWrappedSecretKeyEntry
import com.theminesec.example.sdk.softpos.converter.*
import com.theminesec.example.sdk.softpos.util.crypto.*
import com.theminesec.example.sdk.softpos.util.isAllZero
import com.theminesec.example.sdk.softpos.util.loadJsonFromAsset
import com.theminesec.example.sdk.softpos.util.sequentialString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.crypto.Cipher

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
    // https://docs.theminesec.com/tech-sdk/getting-started/quickstart#init-sdk
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
    // https://docs.theminesec.com/tech-sdk/getting-started/quickstart#emv-kernel-app-param
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
    // https://docs.theminesec.com/tech-sdk/getting-started/quickstart#capk-param
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
    // https://docs.theminesec.com/tech-sdk/getting-started/quickstart#terminal-device-param
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
    // https://docs.theminesec.com/tech-sdk/getting-started/quickstart#key-ceremony
    // !!!!!!! DEMO ONLY !!!!!!!
    // AES 128 BDK
    // DO NOT do it in any production environment
    // always use HSM or equivalent secure module
    private val dangerouslyLocalCardBdk = "f1".repeat(16)
    private val dangerouslyLocalPinBdk = "f2".repeat(16)

    enum class WrappingMethod(val minesecInt: Int) {
        RSA(MsKeyProperties.WRAPPING_METHOD_RSA),
        RSA_OAEP_SHA256(MsKeyProperties.WRAPPING_METHOD_RSA_OAEP_SHA256)
    }

    enum class ClientKeyType(val keyAlias: String, val msKeyProp: String) {
        CARD_KEY("client_card_key", MsKeyProperties.MINESEC_CARD_KEY_NAME),
        PIN_KEY("client_pin_key", MsKeyProperties.MINESEC_PIN_KEY_NAME),
    }

    // Simulate initial key wrapping
    // !!!!!!! DEMO ONLY !!!!!!!
    // DO NOT do it in any production environment
    private fun dangerouslyLocalWrapIkWithMineSecPublic(
        bdk: ByteArray,
        keyAlias: String,
        wrappingMethod: WrappingMethod = WrappingMethod.RSA_OAEP_SHA256,
    ): MsWrappedSecretKeyEntry? {
        val mineSecPubName = "minesecpk"

        val dangerouslyLocalIkId = 16.sequentialString()
        val dangerouslyLocalIk = DukptAesHost.deriveInitialKeyByBdk(
            bdk,
            KeyType.AES128,
            dangerouslyLocalIkId
        )

        // support 3 modes;
        // - Simple RSA
        // - RSA OAEP
        // - TR31, check doc for more details
        val publicStr = KeyLoader.ReadKeyfromKeyStore(mineSecPubName).wrappedKey.decodeToString()
        val kekPublic = RSAUtils.getPublicKeyFromPEM(publicStr)

        val dangerouslyLocalWrapEntry = when (wrappingMethod) {
            WrappingMethod.RSA -> RSAUtils.simpleCrypt(
                mode = Cipher.ENCRYPT_MODE,
                key = kekPublic,
                data = dangerouslyLocalIk
            )

            WrappingMethod.RSA_OAEP_SHA256 -> RSAUtils.oaepMgf1Sha256Crypt(
                mode = Cipher.ENCRYPT_MODE,
                key = kekPublic,
                data = dangerouslyLocalIk
            )
        }

        return MsWrappedSecretKeyEntry.Builder(
            keyAlias,
            dangerouslyLocalWrapEntry,
            MsKeyProperties.KEY_TYPE_AES_AES128
        )
            .setKeyId(dangerouslyLocalIkId)
            .setWrappingKeyAlias(MsKeyProperties.MINESEC_KEK_NAME)
            .setWrappingMethod(wrappingMethod.minesecInt)
            .setKeyUsage(MsKeyProperties.KEY_USAGE_DUKPT_INITIAL_KEY)
            .build()
    }

    /**
     * MineHades full SDK supports you to load a DUKPT Initial Key(AES-128) in the app initialization stage.
     * Once the IK is injected into SDK. you can derive working ket during each transaction.
     * DUKPT Initial Key shall be encrypted using the minesec public key in **server side**,
     * and download it into SDK via key loading interface.
     */
    fun injectCardInitialKey() = viewModelScope.launch(Dispatchers.Default) {
        // should be getting from your backend
        val wrappedKeyEntryCard = dangerouslyLocalWrapIkWithMineSecPublic(
            dangerouslyLocalCardBdk.hexToByteArray(),
            ClientKeyType.CARD_KEY.keyAlias,
            WrappingMethod.RSA
        )
        val injectResp = sdk.payInterface.CryptoInjectKey(wrappedKeyEntryCard)
        writeMessage("Card key injectResp, code: ${injectResp.errorcode}, msg: ${injectResp.errorMsg}")
    }

    fun injectPinInitialKey() = viewModelScope.launch(Dispatchers.Default) {
        // should be getting from your backend
        val wrappedKeyEntryPin = dangerouslyLocalWrapIkWithMineSecPublic(
            dangerouslyLocalPinBdk.hexToByteArray(),
            ClientKeyType.PIN_KEY.keyAlias,
        )
        val injectResp = sdk.payInterface.CryptoInjectKey(wrappedKeyEntryPin)
        writeMessage("PIN key injectResp, code: ${injectResp.errorcode}, msg: ${injectResp.errorMsg}")
    }

    // Card Read & PIN
    // https://docs.theminesec.com/tech-sdk/getting-started/quickstart#request-contactless-card-read
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

    fun deriveWorkingKeysBeforeTran() {
        writeMessage("since we've injected our own IK for card & PIN, before transaction we'll need to tell SDK to derive working key (for the correct KSN)")

        val keyGenCardWk = DukptKeyGenParameter.Builder(ClientKeyType.CARD_KEY.msKeyProp, ClientKeyType.CARD_KEY.keyAlias)
            .setKeyUsage(MsKeyProperties.KEY_USAGE_DATA_ENC_BOTH)
            .setIsCounterUpdate(true)
            .setKeyType(MsKeyProperties.KEY_TYPE_AES_AES128)
            .build()
        val cardWkRes = sdk.MhdSdk_GetKeyStore().DeriveKey(keyGenCardWk)
        writeMessage("next card ksn: ${cardWkRes.data.ksn}")

        val keyGenPinWk = DukptKeyGenParameter.Builder(ClientKeyType.PIN_KEY.msKeyProp, ClientKeyType.PIN_KEY.keyAlias)
            .setKeyUsage(MsKeyProperties.KEY_USAGE_PIN_ENCRYPTION)
            .setIsCounterUpdate(true)
            .setKeyType(MsKeyProperties.KEY_TYPE_AES_AES128)
            .build()
        val pinWkRes = sdk.MhdSdk_GetKeyStore().DeriveKey(keyGenPinWk)
        writeMessage("next pin ksn: ${pinWkRes.data.ksn}")
    }

    // DEMO PURPOSE ONLY
    // Data decrypt
    // https://docs.theminesec.com/tech-sdk/getting-started/quickstart#decrypt-data
    fun dangerouslyDecryptCardDataLocally() {
        writeMessage("card read result: ${cardReadResult.value.toString()}")
        cardReadResult.value?.let { (ksn, iv, encrypted) ->
            // get working key by ksn and bdk
            val dangerouslyDemoWorkingCardKey = DukptAesHost.deriveWorkingKeyByBdk(
                bdk = dangerouslyLocalCardBdk.hexToByteArray(),
                bdkKeyType = KeyType.AES128,
                workingKeyType = KeyType.AES128,
                workingKeyUsage = KeyUsage.DataEncryptionBothWays,
                ksn = ksn.value
            )
            try {
                val plainTrack2 = Aes.decrypt(
                    encrypted.value.hexToByteArray(),
                    dangerouslyDemoWorkingCardKey,
                    Aes.Padding.PKCS5Padding,
                    iv.value.hexToByteArray()
                )
                val plainPan = plainTrack2.toHexString().lowercase().substringBefore("d")
                writeMessage("decrypted track 2: ${plainTrack2.toHexString()}")
                writeMessage("decrypted PAN: $plainPan")

                // set PanToken for the EPB translate
                viewModelScope.launch {
                    _encryptedPinData.emit(_encryptedPinData.value?.copy(third = PanToken(plainPan)))
                }
            } catch (e: Exception) {
                writeMessage("err: $e")
            }
        }
    }

    fun dangerouslyDecryptPinDataLocally() {
        writeMessage("epb: ${encryptedPinData.value.toString()}")
        encryptedPinData.value?.let { (ksn, epb, panToken) ->
            // get working key by ksn and bdk
            val dangerouslyDemoWorkingPinKey = DukptAesHost.deriveWorkingKeyByBdk(
                bdk = dangerouslyLocalPinBdk.hexToByteArray(),
                bdkKeyType = KeyType.AES128,
                workingKeyType = KeyType.AES128,
                workingKeyUsage = KeyUsage.PinEncryption,
                ksn = ksn.value
            )

            try {
                writeMessage("$ksn, $epb, $panToken")
                panToken?.let {
                    val plainPin = PinBlock.dangerouslyDecryptIso4EpbToPin(
                        dangerouslyDemoWorkingPinKey,
                        epb.value.hexToByteArray(),
                        encryptedPinData.value?.third?.value!!
                    )
                    writeMessage("decrypted PIN: $plainPin")
                } ?: writeMessage("no pan token, please decrypt first")
            } catch (e: Exception) {
                writeMessage("err: $e")
            }
        }
    }
}
