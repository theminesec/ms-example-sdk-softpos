package com.theminesec.example.sdk.softpos.converter

sealed class CardReadResult {
    abstract val respCode: String
    abstract val respMessage: String

    data class Failed(
        override val respCode: String,
        override val respMessage: String,
    ) : CardReadResult()

    data class Success(
        override val respCode: String,
        override val respMessage: String,
        val sdkTranId: String,
        val paymentMethod: String,
        val maskedPan: String,
        val needOnline: Boolean,
        val cardKsn: String,
        val cvmPerformed: CvmPerformed? = null,
        val pinKsn: String? = null,
        val panToken: String? = null,
        val emvData: Map<String, String> = mapOf(),
    ) : CardReadResult()
}

enum class CvmPerformed {
    NO_CVM,
    SIGNATURE,
    PIN,
    CDCVM
}

//@OptIn(ExperimentalStdlibApi::class)
//fun MhdEmvTransResult.toCardReadResult(): CardReadResult {
//    if (outComeCode == MhdReturnCode.MHD_SUCCESS.toString()) {
//        return CardReadResult.Success(
//            respCode = outComeCode,
//            respMessage = outComeMessage,
//            sdkTranId = sdkTxnId,
//            paymentMethod = cardBrand,
//            maskedPan = maskedPan,
//            needOnline = isNeedOnline,
//            cvmPerformed = when (cardCVM) {
//                0 -> CvmPerformed.NO_CVM
//                1 -> CvmPerformed.SIGNATURE
//                2 -> CvmPerformed.PIN
//                3 -> CvmPerformed.CDCVM
//                else -> null
//            },
//            cardKsn = cardKeyId,
//            pinKsn = pinKeyId,
//            panToken = pinBlockPan,
//            emvData = kernelInfoData.mapValues { (_, byteArray) ->
//                byteArray.toHexString()
//            }
//        )
//    } else {
//        return CardReadResult.Failed(
//            respCode = outComeCode,
//            respMessage = outComeMessage
//        )
//    }
//}