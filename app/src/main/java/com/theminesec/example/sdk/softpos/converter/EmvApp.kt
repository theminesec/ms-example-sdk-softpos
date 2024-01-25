package com.theminesec.example.sdk.softpos.converter

import com.theminesec.MineHades.EMV_APPLIST
import com.theminesec.example.sdk.softpos.util.prependLengthByte
import com.theminesec.example.sdk.softpos.util.trimLengthByte

data class EmvApp(
    /**
     * 9F06 - AID
     */
    val aid: String,

    /**
     * 9F09 - Application Version Number
     * hex string, e.g., 1.5.0 (150) for Visa VIS would be hex "0096".
     * "0096".hexToInt()
     */
    val appVersion: String,

    val tacDenial: String,
    val tacOnline: String,
    val tacDefault: String,
    /**
     *
     * DDOL to be used for constructing the internal authenticate command if the DDOL in the card is not present
     */
    val defaultDdol: String,

    /**
     * DEFAULT TRANSACTION CERTIFICATE DATA OBJECT LIST (TDOL)
     * TDOL to be used for generating the TC Hash Value if the TDOL in the card is not present
     */
    val defaultTdol: String,

    /**
     * APPLICATION SELECTION INDICATOR
     * 0: Exact match
     * 1: Partial match
     */
    val isFullMatchSelect: Boolean,
    val selectionPriority: Int,

    // random selection
    val enabledRandomSelect: Boolean,
    /**
     * Random selection target percentage
     */
    val randomSelectTargetPercent: Int,
    /**
     * Maximum target percentage to be used for Biased Random Selection
     * in the range of 0 to 99
     * This is the desired percentage of transactions just below the floor limit
     */
    val randomSelectMaxTargetPercent: Int,
    /**
     * Threshold Value for Biased Random Selection (which must be zero or a positive number less than the floor limit)
     */
    val randomSelectThreshold: String,

    /**
     * If both lowerConsecutiveOfflineLimit (tag '9F14') & upperConsecutiveOfflineLimit (tag '9F23') exist,
     * the emv app shall perform velocity check.
     * if either of the tag missing, the emv app would skip velocity check
     */
    val enabledVelocityCheck: Boolean,

    val enabledFloorLimitCheck: Boolean,
    /**
     * 9F1B - Terminal contactless floor limit
     * N12
     */
    val ctlFloorLimit: String,
    /**
     * Indicates the amount above which a contactless transaction is not allowed
     * the cardholder should be directed to use the contact chip instead.
     */
    val ctlTranLimit: String,
    /**
     * Indicates the amount above which a CVM is required for contactless transactions.
     */
    val ctlCvmRequiredLimit: String,

    /**
     * 9F66 - Terminal transaction qualifier
     * Applicable: Visa, UPI, Discover
     * Indicates the requirements for online and CVM processing as a result of Entry Point processing.
     * The scope of this tag is limited to Entry Point. Kernels may use this tag for different purposes.
     */
    val ttq: String? = null,
    /**
     * 9F1D Terminal risk management data
     * Applicable: Mastercard
     */
    val riskMgmtData: String? = null,
    val mcKernelConfig: String? = null,

    /**
     * Applicable: UPI
     */
    val enabledCtlStatusCheck: Boolean?,
)

@OptIn(ExperimentalStdlibApi::class)
fun EmvApp.toMhdEmvApp() = EMV_APPLIST().apply {
    aid = this@toMhdEmvApp.aid.hexToByteArray()
    aidLen = this@toMhdEmvApp.aid.hexToByteArray().size.toByte()
    version = this@toMhdEmvApp.appVersion.hexToByteArray()

    acquierId = "00".hexToByteArray()
    setdDOL(defaultDdol.hexToByteArray().prependLengthByte())
    settDOL(defaultTdol.hexToByteArray().prependLengthByte())
    tacDenial = this@toMhdEmvApp.tacDenial.hexToByteArray()
    tacDefault = this@toMhdEmvApp.tacDefault.hexToByteArray()
    tacOnline = this@toMhdEmvApp.tacOnline.hexToByteArray()

    selFlag = if (isFullMatchSelect) 1 else 0
    priority = selectionPriority.toByte()

    randTransSel = if (enabledRandomSelect) 1 else 0
    targetPer = randomSelectTargetPercent.toByte()
    maxTargetPer = randomSelectMaxTargetPercent.toByte()
    threshold = this@toMhdEmvApp.randomSelectThreshold.toLong()

    velocityCheck = if (enabledVelocityCheck) 1 else 0

    floorLimitCheck = if (enabledFloorLimitCheck) 1 else 0
    floorLimit = ctlFloorLimit.toLong()
    cL_bStatusCheck = if (enabledCtlStatusCheck == true) 1 else 0
    cL_FloorLimit = ctlFloorLimit.toLong()
    cL_TransLimit = ctlTranLimit.toLong()
    cL_CVMLimit = ctlCvmRequiredLimit.toLong()

    mcKernelConfig?.let { kernelConfig = it.hexToByte() }
    riskMgmtData?.let { riskManData = it.hexToByteArray().prependLengthByte() }
    ttq?.let { t_TTQ = it.hexToByteArray() }
}

@OptIn(ExperimentalStdlibApi::class)
fun EMV_APPLIST.toEmvApp() = EmvApp(
    aid = aid.toHexString(),
    appVersion = version.toHexString(),

    defaultDdol = getdDOL().trimLengthByte().toHexString(),
    defaultTdol = gettDOL().trimLengthByte().toHexString(),
    tacDenial = tacDenial.toHexString(),
    tacOnline = tacOnline.toHexString(),
    tacDefault = tacDefault.toHexString(),

    isFullMatchSelect = selFlag == 1.toByte(),
    selectionPriority = priority.toInt(),

    enabledRandomSelect = randTransSel == 1.toByte(),
    randomSelectTargetPercent = targetPer.toInt(),
    randomSelectMaxTargetPercent = maxTargetPer.toInt(),
    randomSelectThreshold = threshold.toString(),

    enabledVelocityCheck = velocityCheck == 1.toByte(),

    enabledFloorLimitCheck = floorLimitCheck == 1.toByte(),
    ctlFloorLimit = cL_FloorLimit.toString(),
    enabledCtlStatusCheck = cL_bStatusCheck == 1.toByte(),
    ctlTranLimit = cL_TransLimit.toString(),
    ctlCvmRequiredLimit = cL_CVMLimit.toString(),

    riskMgmtData = riskManData.trimLengthByte().toHexString(),
    ttq = t_TTQ.toHexString(),

    mcKernelConfig = kernelConfig.toHexString()

)