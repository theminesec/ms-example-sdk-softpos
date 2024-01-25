package com.theminesec.example.sdk.softpos.util.crypto

import java.security.Key
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object RSAUtils {
    @OptIn(ExperimentalEncodingApi::class)
    fun getPublicKeyFromPEM(publicKeyOrCertPem: String): PublicKey {
        // strip pem marker just in case
        val striped = publicKeyOrCertPem
            .trim()
            .trimIndent()
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")

        val keyEncoded = Base64.decode(striped)
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = X509EncodedKeySpec(keyEncoded)
        return keyFactory.generatePublic(keySpec)
    }

    fun oaepMgf1Sha256Crypt(mode: Int, key: Key, data: ByteArray): ByteArray {
        //RSA/ECB/OAEPWithSHA-1AndMGF1Padding
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(mode, key)
        return cipher.doFinal(data)
    }

    fun simpleCrypt(mode: Int, key: Key, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(mode, key)
        return cipher.doFinal(data)
    }
}