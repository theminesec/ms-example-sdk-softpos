package com.theminesec.example.sdk.softpos

import com.theminesec.example.sdk.softpos.util.crypto.PinBlockUtil
import com.theminesec.example.sdk.softpos.util.crypto.PinBlockUtil.PinBlockFormat.*
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalStdlibApi::class)
class PinBlockUtilUnitTest {

    @Test
    fun `test pin to pinblock iso0`() {
        val pan = "43219876543210987"
        val pin = "123456"
        val expected = "0612ac20abcdef67"
        val pinBlock = PinBlockUtil.getPinBlock(pin, Iso0, pan)
        assertEquals(expected.lowercase(), pinBlock.lowercase())
    }

    @Test
    fun `test pin to pinblock iso1`() {
        val pin = "123456"
        // [1] [Len] [PIN] [Random] - len total 16
        val expected = "16123456ad608835"
        val pinBlock = PinBlockUtil.getPinBlock(pin, Iso1)
        assertEquals(expected.lowercase().take(2 + pin.length), pinBlock.lowercase().take(2 + pin.length))
    }

    @Test
    fun `test pin to pinblock iso2`() {
        val pin = "123456"
        // [2] [Len] [PIN] [pad F] - len total 16
        val expected = "26123456ffffffff"
        val pinBlock = PinBlockUtil.getPinBlock(pin, Iso2)
        assertEquals(expected.lowercase(), pinBlock.lowercase())
    }

    @Test
    fun `test pin to pinblock iso3`() {
        val pan = "43219876543210987"
        val pin = "123456"
        // [3] [pin len] [PIN] [pad random & xor pan] - len total 16
        val expected = "3612ac20a322c346"
        val pinBlock = PinBlockUtil.getPinBlock(pin, PinBlockUtil.PinBlockFormat.Iso3, pan)
        assertEquals(expected.lowercase().take(2 + pin.length), pinBlock.take(2 + pin.length))
    }

    @Test
    fun `test format4 epb to plain pin`() {
        val pan = "43219876543210987"
        val pin = "123456"
        val pinKey = "f1".repeat(16).hexToByteArray()
        val epb = PinBlockUtil.getPinBlock(pin, Iso4, pan, pinKey)
        println("iso4 epb: $epb")
        val calculated = PinBlockUtil.dangerouslyDecryptIso4EpbToPin(pinKey, epb.hexToByteArray(), pan)
        println("calculated pin str: $calculated")
        assertEquals(pin, calculated)
    }
}
