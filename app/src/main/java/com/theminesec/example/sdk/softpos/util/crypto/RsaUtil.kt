package com.theminesec.example.sdk.softpos.util.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Key
import java.security.Security
import javax.crypto.Cipher

object RsaUtil {
    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun oaepMgf1Sha256Crypt(mode: Int, key: Key, data: ByteArray): ByteArray {
        //RSA/ECB/OAEPWithSHA-1AndMGF1Padding
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC")
        cipher.init(mode, key)
        return cipher.doFinal(data)
    }

    fun simpleCrypt(mode: Int, key: Key, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(mode, key)
        return cipher.doFinal(data)
    }
}
