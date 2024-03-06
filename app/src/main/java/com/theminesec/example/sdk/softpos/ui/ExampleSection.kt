package com.theminesec.example.sdk.softpos.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.gson.GsonBuilder
import com.theminesec.MineHades.MhdCPOC
import com.theminesec.MineHades.vo.MhdEmvTransactionDto
import com.theminesec.MineHades.vo.MhdEmvTransactionType
import com.theminesec.example.sdk.softpos.converter.CardReadResult
import com.theminesec.example.sdk.softpos.converter.toCardReadResult
import com.theminesec.example.sdk.softpos.ui.component.BrandedButton
import com.theminesec.example.sdk.softpos.ui.component.LabeledSwitch
import com.theminesec.example.sdk.softpos.ui.component.Title
import com.theminesec.example.sdk.softpos.util.findActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

@Composable
fun ExampleSection() {
    val localContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = viewModel(modelClass = ExampleViewModel::class.java)
    val prettyGson = remember {
        GsonBuilder().setPrettyPrinting().create()
    }

    // local UI state
    var uiReaderEnabled by remember { mutableStateOf(false) }
    var uiAmountLarge by remember { mutableStateOf(false) }
    val uiLastCardRead by viewModel.cardReadResult.collectAsState()
    val uiPinData by viewModel.encryptedPinData.collectAsState()

    // hold reference of sdk singleton
    val sdk = remember { MhdCPOC.getInstance() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Init
        Title(text = "1. Init SDK")
        Text(text = "Need to do it everytime, so might consider to put it in application")
        BrandedButton(
            label = "Get SDK info",
            onClick = { viewModel.getSdkInfo() }
        )
        Divider()

        // Configs
        Title(text = "2. Config")
        Text(text = "For the EMV config, it's separated into few parts - EmvAppParams, CAPKs & TerminalParams")

        Title(text = "2a. EMV App (Kernel) Params")
        Text(text = "Here's the first part - EmvAppParams")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BrandedButton(
                modifier = Modifier.weight(1f),
                onClick = { viewModel.setEmvApps() },
                label = "Load EmvAppParams"
            )
            BrandedButton(
                modifier = Modifier.weight(1f),
                onClick = { viewModel.getEmvApps() },
                label = "Get EmvAppParams"
            )
        }

        Title(text = "2b. CAPKs")
        Text(text = "Then load the CAPKs")

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BrandedButton(
                modifier = Modifier.weight(1f),
                label = "Load CAPKs",
                onClick = { viewModel.setCapks() }
            )
            BrandedButton(
                modifier = Modifier.weight(1f),
                onClick = { viewModel.getCapks() },
                label = "Get CAPKs"
            )
        }

        Title(text = "2c. Terminal (Device) Param")
        Text(text = "Then, load the TermParam")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BrandedButton(
                modifier = Modifier.weight(1f),
                label = "Load TermParam",
                onClick = { viewModel.setTermParam() }
            )
            BrandedButton(
                modifier = Modifier.weight(1f),
                label = "Get TermParam",
                onClick = { viewModel.getTermParam() }
            )
        }
        Divider()

        // Keys
        Title(text = "3. Key Ceremony")
        Text(text = "Load the initial key for data encryption")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BrandedButton(
                modifier = Modifier.weight(1f),
                label = "Inject Card IK",
                onClick = { viewModel.injectCardInitialKey() }
            )
            BrandedButton(
                modifier = Modifier.weight(1f),
                label = "Inject PIN IK",
                onClick = { viewModel.injectPinInitialKey() }
            )
        }
        Text(text = "Inject PAN HMAC key")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BrandedButton(
                modifier = Modifier.weight(1f),
                label = "Inject PAN HMAC key",
                onClick = { viewModel.injectPanHmacKey() }
            )
        }
        Divider()

        // Card read
        Title(text = "4. NFC Read Card")
        Text(text = "Alright, now we can start contactless card reading")

        AndroidPermissionMess {
            LabeledSwitch(
                label = "Toggle NFC reader",
                checked = uiReaderEnabled,
                onCheckedChange = { uiReaderEnabled = it }
            )

            LabeledSwitch(
                label = "Toggle Amount (with PIN CVM)",
                checked = uiAmountLarge,
                onCheckedChange = { uiAmountLarge = it }
            )
        }

        LaunchedEffect(uiReaderEnabled) {
            if (uiReaderEnabled) {
                lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                    val nfcEnableResult = sdk.MhdNfc_Enablev2(localContext.findActivity())
                    viewModel.writeMessage("NFC enable? ${nfcEnableResult.errorMsg}")
                }

                // callback for card read data
                sdk.setOnEmvHandler {
                    viewModel.writeMessage("onEmvHandler")
                    thread {

                        // since we've injected our own IK for card & PIN,
                        // before transaction we'll need to tell SDK to derive working key (for the correct KSN)
                        viewModel.deriveWorkingKeysBeforeTran()

                        val txnRequest = MhdEmvTransactionDto
                            .builder()
                            .txnAmount(if (uiAmountLarge) 20000 else 200)
                            .txnType(MhdEmvTransactionType.MHD_EMV_TRANS_PURCHASE)
                            // auto prompt PIN pad when PIN CVM is applicable
                            // default on
                            //.autoPinEntry(true)

                            // since we'll using own IK and call derive manually
                            // turn off the `deriveKeysFromIK` to avoid sdk default IK derivation
                            .deriveKeysFromIK(false)

                            // will handle the EPB by the real PAN
                            .enablePANToken(false)
                            .build()

                        val mineHadesResult = sdk.payInterface.EmvPerformTransaction(localContext, txnRequest)
                        val cardReadResult = mineHadesResult.toCardReadResult()
                        if (cardReadResult is CardReadResult.Success) {
                            viewModel.writeMessage("cardReadResult: \n${prettyGson.toJson(cardReadResult)}")

                            // holding encrypted card data in viewmodel for later demo decrypt
                            cardReadResult.emvData["57"]?.let {
                                val cardKsn = Ksn(cardReadResult.cardKsn)
                                val iv = Iv(it.substring(0, 32))
                                val encrypted57 = Encrypted57(it.substring(32))
                                viewModel.setCardReadResult(Triple(cardKsn, iv, encrypted57))
                                viewModel.writeMessage("required for decryption (in backend): $cardKsn, $iv, $encrypted57")
                            }

                            // holding encrypted pin data in viewmodel for later demo decrypt
                            cardReadResult.emvData["99"]?.let {
                                val pinKsn = Ksn(cardReadResult.pinKsn!!)
                                val epbHex = EncryptedPinBlock(it)
                                viewModel.setEncryptedPinData(Triple(pinKsn, epbHex, null))
                                viewModel.writeMessage("required for translate: $pinKsn, $epbHex")
                            } ?: run {
                                viewModel.setEncryptedPinData(null)
                            }
                        } else {
                            viewModel.writeMessage("cardReadResult failed: \n${prettyGson.toJson(cardReadResult)}")
                            viewModel.setCardReadResult(null)
                        }
                    }
                }
            } else {
                sdk.MhdNfc_DisableReader()
                viewModel.writeMessage("NFC disabled")
            }
        }

        Divider()

        // Decrypt card data
        Title(text = "6. (Host side) Data Decrypt")
        Text(text = "After the card read, at host side you can decrypt the card data")
        BrandedButton(
            label = "Decrypt card data",
            enabled = uiLastCardRead != null,
            onClick = { viewModel.dangerouslyDecryptCardDataLocally() }
        )
        Text(text = "(host side) or, for demo purpose the PIN")
        BrandedButton(
            label = "Decrypt PIN data",
            enabled = uiPinData?.third != null,
            onClick = { viewModel.dangerouslyDecryptPinDataLocally() }
        )
    }

}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AndroidPermissionMess(content: @Composable () -> Unit) {
    // camera permission
    var didRequestPermission by rememberSaveable { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA) {
        didRequestPermission = true
    }
    val localContext = LocalContext.current

    when {
        // happy path
        cameraPermissionState.status.isGranted -> content()

        // permanently denied
        didRequestPermission && !cameraPermissionState.status.isGranted && !cameraPermissionState.status.shouldShowRationale -> {
            // permanently denied
            Text(text = "Camera permission permanently denied")
            Text(text = "Please open the app setting and grant permission")
            BrandedButton(
                label = "Open up app setting",
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .apply {
                            data = Uri.fromParts("package", localContext.packageName, null)
                        }
                    localContext.startActivity(intent)
                }
            )
        }

        // finally show request perm button
        else -> {
            Text(text = "First we'll need the CAMERA permission")
            BrandedButton(
                label = "Request Camera Permission",
                onClick = {
                    cameraPermissionState.launchPermissionRequest()
                })
        }
    }
}

