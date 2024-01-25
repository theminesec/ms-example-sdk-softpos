package com.theminesec.example.sdk.softpos.util.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

@OptIn(ExperimentalStdlibApi::class)
object PinBlock {

    /**
     * reference: https://www.eftlab.com/knowledge-base/complete-list-of-pin-blocks
     * common PIN-block formats based on ISO 9564
     */
    enum class PinBlockFormat(
        val nibble: Int,
    ) {
        Iso0(nibble = 0),
        Iso1(nibble = 1),
        Iso2(nibble = 2),
        Iso3(nibble = 3),
        Iso4(nibble = 4),
    }

    /**
     * Prepares the PIN using the specified PIN block format.
     *
     * @param rawPin The raw PIN entered by the user.
     * @param pinBlockFormat The format of the PIN block.
     * @return The prepared PIN as a hexadecimal string.
     * @throws IllegalArgumentException If the length of the PIN is less than 4.
     */
    private fun preparePin(rawPin: String, pinBlockFormat: PinBlockFormat): String {
        require(rawPin.length >= 4) { "PIN length must be >= 4" }

        val randomBytes = ByteArray(8).apply { SecureRandom().nextBytes(this) }
        return when (pinBlockFormat) {
            PinBlockFormat.Iso0,
            PinBlockFormat.Iso2,
            -> "${pinBlockFormat.nibble}${rawPin.length}$rawPin".padEnd(16, 'f')

            PinBlockFormat.Iso1,
            PinBlockFormat.Iso3,
            -> "${pinBlockFormat.nibble}${rawPin.length}$rawPin".plus(randomBytes.toHexString()).take(16)

            PinBlockFormat.Iso4 -> "${pinBlockFormat.nibble}${rawPin.length}$rawPin".padEnd(16, 'a').plus(randomBytes.toHexString()).take(32)
        }
    }

    /**
     * Prepares the PAN (Primary Account Number) for xor or encryption using the specified PIN block format.
     *
     * @param rawPan The raw PAN entered by the user.
     * @param pinBlockFormat The format of the PIN block.
     * @return The prepared PAN as a string.
     * @throws IllegalArgumentException If the length of the PAN is not in the range of 12 to 19.
     */
    private fun preparePan(rawPan: String, pinBlockFormat: PinBlockFormat): String {
        require(rawPan.length in 12..19) { "PAN length must be in 12..19" }
        // only iso0, iso3, iso4 use the pan for xor or aes
        // but to avoid nullable type just return the same formatted pan for iso1 & 2
        return when (pinBlockFormat) {
            PinBlockFormat.Iso0,
            PinBlockFormat.Iso1,
            PinBlockFormat.Iso2,
            PinBlockFormat.Iso3,
            -> "0000".plus(rawPan.dropLast(1).takeLast(12))

            PinBlockFormat.Iso4 -> {
                // PAN pad length indicating PAN length of 12 plus the value of the field ‘0’-‘7’ (ranging then from 12 to 19)
                val lengthAbove12 = rawPan.length - 12
                "$lengthAbove12$rawPan".padEnd(32, '0')
            }
        }
    }

    /**
     * Calculates the PIN block based on the provided parameters.
     *
     * @param rawPin The raw PIN entered by the user.
     * @param pinBlockFormat The format of the PIN block.
     * @param rawPan The raw PAN (Primary Account Number) used for Iso0, Iso3, or Iso4 formats.
     * @param pinKey The key used for encrypting the PIN block in Iso4 format.
     * @return The calculated PIN block as a hexadecimal string.
     *
     * @throws IllegalArgumentException If the PAN is required but not provided for Iso0, Iso3, or Iso4 formats.
     * @throws IllegalArgumentException If the PIN key is required but not provided for Iso4 format.
     */
    fun getPinBlock(
        rawPin: String,
        pinBlockFormat: PinBlockFormat,
        rawPan: String? = null,
        pinKey: ByteArray? = null,
    ): String {
        if (pinBlockFormat in arrayOf(PinBlockFormat.Iso0, PinBlockFormat.Iso3, PinBlockFormat.Iso4)) {
            require(rawPan != null) { "Require PAN for Iso0, Iso3, or Iso4" }
        }

        val pinBytes = preparePin(rawPin, pinBlockFormat).hexToByteArray()
        val panBytes = rawPan?.let { preparePan(it, pinBlockFormat).hexToByteArray() }

        if (pinBlockFormat in arrayOf(PinBlockFormat.Iso0, PinBlockFormat.Iso3)) {
            return pinBytes
                .mapIndexed { idx, byte -> byte xor panBytes!![idx] }
                .toByteArray()
                .toHexString()
        }

        // ISO 9564-1: 2017 Format 4.
        if (pinBlockFormat == PinBlockFormat.Iso4) {
            require(pinKey != null) { "Require PIN key for Iso4" }

            return pinBytes
                // 3. PIN block is encrypted with AES key - Format 4 uses AES-128 ECB
                .run { Aes.encryptEcb(this, pinKey, Aes.Padding.NoPadding) }
                // 4. The resulting Intermediate Block A is then XOR’ed with PAN Block
                .mapIndexed { idx, byte -> byte xor panBytes!![idx] }.toByteArray()
                // 5. The resulting Intermediate Block B is enciphered with the AES key again to get final Enciphered PIN Block
                .run { Aes.encryptEcb(this, pinKey, Aes.Padding.NoPadding).toHexString() }
        }

        // PinBlockFormat.Iso1, PinBlockFormat.Iso2
        return pinBytes.toHexString()
    }

    /**
     * Decrypts the ISO 9564-1 Format 4 encrypted PIN block into plain PIN string.
     * Warning: DO NOT do this anywhere, you'll always want the PIN block encrypted
     * This is just for demonstration purpose
     *
     * @param pinKey The key used for PIN encryption. It should be a 16-byte AES key.
     * @param epb The encrypted PIN block as a byte array.
     * @param pan The PAN (Primary Account Number) as a string.
     * @return The decrypted plain PIN.
     */
    fun dangerouslyDecryptIso4EpbToPin(pinKey: ByteArray, epb: ByteArray, pan: String): String {
        val cipher = Cipher
            .getInstance("AES/ECB/NoPadding")
            .apply { init(Cipher.DECRYPT_MODE, SecretKeySpec(pinKey, "AES")) }

        val panBytes = preparePan(pan, PinBlockFormat.Iso4).hexToByteArray()

        return cipher
            // decrypt epb to block B
            .doFinal(epb)
            // reverse xor with pan, for block a
            .mapIndexed { index, byte -> byte xor panBytes[index] }.toByteArray()
            // decrypt again for plain pin block
            .run { cipher.doFinal(this).toHexString() }
            // extract pin string
            .run { drop(2).take(substring(1, 2).toInt(16)) }

    }
}