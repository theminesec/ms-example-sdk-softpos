package com.theminesec.example.sdk.softpos.converter

import androidx.core.text.isDigitsOnly
import com.theminesec.MineHades.EMVCAPK

enum class CapkKeyType {
    TEST,
    LIVE
}

data class Capk(
    val rid: String,
    val keyIndex: String,
    val exponent: String,
    val modulus: String,
    val expiryYyMmDd: String,
    val checksum: String,
    val keyType: CapkKeyType? = null,
)

@OptIn(ExperimentalStdlibApi::class)
fun Capk.toMhdCapk() = EMVCAPK().apply {
    rid = this@toMhdCapk.rid.hexToByteArray()
    keyID = this@toMhdCapk.keyIndex.hexToByte()
    hashInd = 1.toByte()
    arithInd = 1.toByte()
    modul = this@toMhdCapk.modulus.hexToByteArray()
    modulLen = this@toMhdCapk.modulus.hexToByteArray().size.toByte()
    exponent = this@toMhdCapk.exponent.hexToByteArray()
    exponentLen = this@toMhdCapk.exponent.hexToByteArray().size.toByte()
    expDate = (if (expiryYyMmDd.isDigitsOnly()) expiryYyMmDd else "491231").hexToByteArray()
    checkSum = this@toMhdCapk.checksum.hexToByteArray()
}

@OptIn(ExperimentalStdlibApi::class)
fun EMVCAPK.toCapk() = Capk(
    rid = rid.toHexString(),
    keyIndex = keyID.toHexString(),
    modulus = modul.toHexString(),
    exponent = exponent.toHexString(),
    expiryYyMmDd = expDate.toHexString(),
    checksum = checkSum.toHexString()
)
