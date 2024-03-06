package com.theminesec.example.sdk.softpos.util.crypto

import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object PemUtil {
    @OptIn(ExperimentalEncodingApi::class)
    fun String.stripPemHeader(): PublicKey {
        // strip pem marker just in case
        val striped = this
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
}
