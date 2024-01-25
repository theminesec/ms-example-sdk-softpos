package com.theminesec.example.sdk.softpos

import com.theminesec.example.sdk.softpos.util.crypto.DukptAesHost
import com.theminesec.example.sdk.softpos.util.crypto.KeyType.*
import com.theminesec.example.sdk.softpos.util.crypto.KeyUsage.*
import com.theminesec.example.sdk.softpos.util.removeSpace
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@OptIn(ExperimentalStdlibApi::class)
class DukptAesHostUnitTest {
    @Test
    fun `test derive initial key from bdk with aes 128, 192, 256`() {
        val bdk128 = "f1".repeat(16).hexToByteArray()
        val bdk192 = "f1".repeat(24).hexToByteArray()
        val bdk256 = "f1".repeat(32).hexToByteArray()
        val ksn = "1122334455667788"

        println("========= 128 =========")
        val ik128 = DukptAesHost.deriveInitialKeyByBdk(bdk128, AES128, ksn)
        println("ik128: ${ik128.toHexString()}")
        assertEquals("0e870691f56a670c438612ae868d668d", ik128.toHexString())

        println("========= 192 =========")
        val ik192 = DukptAesHost.deriveInitialKeyByBdk(bdk192, AES192, ksn)
        println("ik192: ${ik192.toHexString()}")
        assertEquals("662e213522ef22c20e65ca0fab0c20c5340fcd7072fe3f54", ik192.toHexString())

        println("========= 256 =========")
        val ik256 = DukptAesHost.deriveInitialKeyByBdk(bdk256, AES256, ksn)
        println("ik256: ${ik256.toHexString()}")
        assertEquals("2e6c334f36e48fd78ee3a2db4a691c549de0cee9419b4388bad15f09ca7e76e7", ik256.toHexString())
    }

    /**
     * ANSI X9.24
     * B.1.Sample Test Vectors for Generating AES-128 keys from AES-128 BDK
     */
    @Test
    fun `test derive initial key from bdk`() {
        val bdk = "FEDCBA98 76543210 F1F1F1F1 F1F1F1F1".removeSpace().hexToByteArray()
        val bdkId = "12345678"
        val derivationId = "90123456"
        val initialKeyId = "1234567890123456"
        val ik = DukptAesHost.deriveInitialKeyByBdk(bdk, AES128, initialKeyId)
        val expect = "1273671E A26AC29A FA4D1084 127652A1".removeSpace()
        assertEquals(expect.lowercase(), ik.toHexString())
    }

    /**
     * ANSI X9.24
     * B.1.Sample Test Vectors for Generating AES-128 keys from AES-128 BDK
     */
    @Test
    fun `test derive working key with counter`() {
        val bdk = "FEDCBA98 76543210 F1F1F1F1 F1F1F1F1".removeSpace().hexToByteArray()
        val initialKeyId = "1234567890123456"
        val ik = DukptAesHost.deriveInitialKeyByBdk(bdk, AES128, initialKeyId)
        val count1 = 1.toHexString().padStart(8, '0')
        val wkPin1 = DukptAesHost.deriveWorkingKeyByInitialKey(ik, AES128, PinEncryption, AES128, initialKeyId + count1)
        val wkMac1 = DukptAesHost.deriveWorkingKeyByInitialKey(ik, AES128, MessageAuthenticationGeneration, AES128, initialKeyId + count1)
        val wkDat1 = DukptAesHost.deriveWorkingKeyByInitialKey(ik, AES128, DataEncryptionEncrypt, AES128, initialKeyId + count1)
        assertEquals("AF8CB133 A78F8DC2 D1359F18 527593FB".removeSpace().lowercase(), wkPin1.toHexString())
        assertEquals("A2DC23DE 6FDE0824 A2BC321E 08E4B8B7".removeSpace().lowercase(), wkMac1.toHexString())
        assertEquals("A35C412E FD41FDB9 8B69797C 02DCD08F".removeSpace().lowercase(), wkDat1.toHexString())

        val count131070 = 131070.toString(16).padStart(8, '0')
        val wkPin131070 = DukptAesHost.deriveWorkingKeyByInitialKey(ik, AES128, PinEncryption, AES128, initialKeyId + count131070)
        val wkMac131070 = DukptAesHost.deriveWorkingKeyByInitialKey(ik, AES128, MessageAuthenticationGeneration, AES128, initialKeyId + count131070)
        val wkDat131070 = DukptAesHost.deriveWorkingKeyByInitialKey(ik, AES128, DataEncryptionEncrypt, AES128, initialKeyId + count131070)
        assertEquals("DDF7E08A 84B5478C 498D007C 743BF762".removeSpace().lowercase(), wkPin131070.toHexString())
        assertEquals("6D7623AD 652734B8 FAE1B6E0 93EACE3D".removeSpace().lowercase(), wkMac131070.toHexString())
        assertEquals("8E4E5D5E 0F01C54F 01F4ACA1 C8F8EDCE".removeSpace().lowercase(), wkDat131070.toHexString())

        val count4294901760 = 4294901760.toString(16).padStart(8, '0')
        val wkPin4294901760 = DukptAesHost.deriveWorkingKeyByInitialKey(ik, AES128, PinEncryption, AES128, initialKeyId + count4294901760)
        val wkMac4294901760 = DukptAesHost.deriveWorkingKeyByInitialKey(ik, AES128, MessageAuthenticationGeneration, AES128, initialKeyId + count4294901760)
        val wkDat4294901760 = DukptAesHost.deriveWorkingKeyByInitialKey(ik, AES128, DataEncryptionEncrypt, AES128, initialKeyId + count4294901760)
        assertEquals("27EFAC1D 15863258 8F4AC69E 45C247C4".removeSpace().lowercase(), wkPin4294901760.toHexString())
        assertEquals("AE558BAB C206D303 FDF68B11 81F228C6".removeSpace().lowercase(), wkMac4294901760.toHexString())
        assertEquals("08878BFC C45CA5AE F6A1AB40 BAC882B5".removeSpace().lowercase(), wkDat4294901760.toHexString())
    }

    @Test
    fun `test derive working key from bdk`() {
        val bdk = "FEDCBA98 76543210 F1F1F1F1 F1F1F1F1".removeSpace().hexToByteArray()
        val initialKeyId = "1234567890123456"
        val count1 = 1.toHexString().padStart(8, '0')
        val wkPin1 = DukptAesHost.deriveWorkingKeyByBdk(bdk, AES128, AES128, PinEncryption, initialKeyId + count1)
        assertEquals("AF8CB133 A78F8DC2 D1359F18 527593FB".removeSpace().lowercase(), wkPin1.toHexString())
    }
}