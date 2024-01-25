package com.theminesec.example.sdk.softpos.util.crypto

import android.util.Log
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Aes {
    private const val TAG = "AES"

    enum class Padding {
        NoPadding,
        PKCS5Padding,
        // PKCS1Padding,
        // PKCS7Padding
    }

    // enum class EncryptionMode {
    //     ECB,
    //     CBC,
    //     //CFB,
    //     //OFB,
    //     //CTR,
    //     //GCM
    // }

    fun encryptEcb(data: ByteArray, key: ByteArray, padding: Padding): ByteArray {
        val keySpec = SecretKeySpec(key, "AES")
        try {
            val cipher = Cipher.getInstance("AES/ECB/${padding}")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            return cipher.doFinal(data)
        } catch (e: Exception) {
            Log.d(TAG, "AES-ECB encrypt failed: $e")
            throw e
        }
    }

    fun decryptEcb(data: ByteArray, key: ByteArray, padding: Padding): ByteArray {
        val keySpec = SecretKeySpec(key, "AES")
        try {
            val cipher = Cipher.getInstance("AES/ECB/${padding}")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            return cipher.doFinal(data)
        } catch (e: Exception) {
            Log.d(TAG, "AES-ECB decrypt failed: $e")
            throw e
        }
    }

    fun encrypt(data: ByteArray, key: ByteArray, padding: Padding, iv: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(key, "AES")
        try {
            val cipher = Cipher.getInstance("AES/CBC/${padding}")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
            return cipher.doFinal(data)
        } catch (e: Exception) {
            Log.d(TAG, "AES-CBC encrypt failed: $e")
            throw e
        }
    }

    fun decrypt(data: ByteArray, key: ByteArray, padding: Padding, iv: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(key, "AES")
        try {
            val cipher = Cipher.getInstance("AES/CBC/${padding}")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
            return cipher.doFinal(data)
        } catch (e: Exception) {
            Log.d(TAG, "AES-CBC decrypt failed: $e")
            throw e
        }
    }
}