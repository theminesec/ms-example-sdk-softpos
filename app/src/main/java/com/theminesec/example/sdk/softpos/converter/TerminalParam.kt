package com.theminesec.example.sdk.softpos.converter

import com.theminesec.MineHades.EMV_PARAM

data class TerminalParam(
    val terminalType: String,
    val merchantCategoryCode: String,
    val countryCode: String,
    val tranCurrCode: String,
    val terminalCapability: String,
    val ctlReaderCapability: String?,
    val ctlReaderCapabilityAmex: String?,
)

@OptIn(ExperimentalStdlibApi::class)
fun TerminalParam.toMhdTermParams() = EMV_PARAM().apply {
    terminalType = this@toMhdTermParams.terminalType.hexToByte()
    merchCateCode = this@toMhdTermParams.merchantCategoryCode.hexToByteArray()
    countryCode = this@toMhdTermParams.countryCode.hexToByteArray()
    transCurrCode = this@toMhdTermParams.tranCurrCode.hexToByteArray()
    capability = this@toMhdTermParams.terminalCapability.hexToByteArray()
    this@toMhdTermParams.ctlReaderCapability?.let {
        cL_ReaderCapability = it.hexToByteArray()
    }

    this@toMhdTermParams.ctlReaderCapabilityAmex?.let {
        cL_ReaderCapabilityEx = it.hexToByteArray()
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun EMV_PARAM.toTermParams() = TerminalParam(
    terminalType = terminalType.toHexString(),
    merchantCategoryCode = merchCateCode.toHexString(),
    countryCode = countryCode.toHexString(),
    tranCurrCode = transCurrCode.toHexString(),
    terminalCapability = capability.toHexString(),
    ctlReaderCapability = cL_ReaderCapability.runCatching { toHexString() }.getOrNull(),
    ctlReaderCapabilityAmex = cL_ReaderCapabilityEx.runCatching { toHexString() }.getOrNull(),
)