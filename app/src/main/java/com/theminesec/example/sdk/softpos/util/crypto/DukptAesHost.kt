package com.theminesec.example.sdk.softpos.util.crypto

import com.theminesec.example.sdk.softpos.util.crypto.Aes.Padding

/**
 * ANSI X9.24-3-2017
 * 6.3.2 Derivation Data, Table 2 & 3 Derivation Data
 * @param algoIndicator Indicates the algorithm that is going to use the derived key.
 * @param length Length, in bits, of the keying material being generated.
 */
enum class KeyType(val algoIndicator: String, val length: String) {
    //`2TDEA`("0000", "0080"),
    //`3TDEA`("0001", "00C0"),
    AES128("0002", "0080"),
    AES192("0003", "00C0"),
    AES256("0004", "0100"),
}

/**
 * ANSI X9.24-3-2017
 * 6.3.2 Derivation Data, Table 2 & 3 Derivation Data
 * keyUsageIndicator: Indicates how the key to be derived is to be used
 */
enum class KeyUsage(val usageIndicator: String) {
    KeyEncryptionKey("0002"),
    PinEncryption("1000"),
    MessageAuthenticationGeneration("2000"),
    MessageAuthenticationVerification("2001"),
    MessageAuthenticationBothWays("2002"),
    DataEncryptionEncrypt("3000"),
    DataEncryptionDecrypt("3001"),
    DataEncryptionBothWays("3002"),
    KeyDerivation("8000"),
    KeyDerivationInitialKey("8001")
}

/**
 * AES DUKPT ANSI X9.24-3-2017 for more details
 * 6.4 Host Security Module Algorithm
 */
@OptIn(ExperimentalStdlibApi::class)
class DukptAesHost {
    /**
     * Derive functions should be stateless and hence in companion object
     */
    companion object {
        /**
         * 6.3 key derivation function
         */
        private fun deriveKey(
            derivationKey: ByteArray,
            keyType: KeyType,
            derivationData: ByteArray,
        ): ByteArray {
            val length = keyType.length.hexToInt()
            val time = (length + 127) / 128
            var result = byteArrayOf()
            for (i in 1..time) {
                val tmp = derivationData.copyOf().apply {
                    set(1, i.toByte())
                }
                result += Aes.encryptEcb(tmp, derivationKey, Padding.NoPadding)
            }
            return result.copyOfRange(0, length / 8)
        }

        /**
         * 6.3.2 & 6.3.2 Create derivation data
         */
        private fun createDerivationData(
            keyUsage: KeyUsage,
            keyType: KeyType,
            initialKeyId: String,
            hexCounter: String,
        ): ByteArray {
            val version = "01"
            val keyBlockCounter = "01"
            val data = if (keyUsage == KeyUsage.KeyDerivationInitialKey) {
                initialKeyId
            } else {
                initialKeyId.substring(8) + hexCounter.padStart(8, '0')
            }
            return "$version$keyBlockCounter${keyUsage.usageIndicator}${keyType.algoIndicator}${keyType.length}$data".hexToByteArray()
        }

        fun deriveInitialKeyByBdk(
            bdk: ByteArray,
            keyType: KeyType,
            initialKeyId: String,
        ): ByteArray {
            val derivationData = createDerivationData(KeyUsage.KeyDerivationInitialKey, keyType, initialKeyId, "")
            return deriveKey(bdk, keyType, derivationData)
        }

        fun deriveWorkingKeyByBdk(
            bdk: ByteArray,
            bdkKeyType: KeyType,
            workingKeyType: KeyType,
            workingKeyUsage: KeyUsage,
            ksn: String,
        ): ByteArray {
            val initialKey = deriveInitialKeyByBdk(bdk, bdkKeyType, ksn.substring(0..15))
            return deriveWorkingKeyByInitialKey(initialKey, bdkKeyType, workingKeyUsage, workingKeyType, ksn)
        }

        fun deriveWorkingKeyByInitialKey(
            initialKey: ByteArray,
            deriveKeyType: KeyType,
            workingKeyUsage: KeyUsage,
            workingKeyType: KeyType,
            ksn: String,
        ): ByteArray {
            require(ksn.length == 24)
            val initialKeyId = ksn.substring(0..15)
            val transactionCounter = ksn.substring(16..23)

            // set the most significant bit to one and all other bits to zero
            var mask = 0x80000000
            var workingCounter = 0L
            var derivationKey = initialKey

            // calculate current derivation key from initial key
            while (mask > 0) {
                if (mask.and(transactionCounter.toLong(16)) != 0L) {
                    workingCounter = workingCounter.or(mask)
                    val derivationData = createDerivationData(
                        KeyUsage.KeyDerivation,
                        deriveKeyType,
                        initialKeyId,
                        workingCounter.toString(16)
                    )
                    derivationKey = deriveKey(derivationKey, deriveKeyType, derivationData)
                }
                mask = mask shr 1
            }

            // derive working key from current derivation key
            val derivationData = createDerivationData(workingKeyUsage, workingKeyType, initialKeyId, transactionCounter)
            return deriveKey(derivationKey, workingKeyType, derivationData)
        }
    }
}
