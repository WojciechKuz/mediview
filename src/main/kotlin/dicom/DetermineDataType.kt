package dicom

import DataAndStatus
import Status
import withStatus

fun determineDataType(byteData: DicomByteData) = safeDetermineDataType(byteData).data

private val fallbackStatus = Status("As fallback interpret as byte array")

fun safeDetermineDataType(byteData: DicomByteData): DataAndStatus<DicomDataElement<out Any>> {

    // some unsupported VRs are interpreted as byteData
    return when(byteData.vr) {
        "AS" -> {
            byteData.valueAsString().withStatus()
        } 	// [0x4153U] Age String
        "CS" -> {
            byteData.valueAsString().withStatus()
        } 	// [0x4353U] Code String
        "DS" -> {
            byteData.valueAsString().withStatus()
        } 	// [0x4453U] Decimal String
        "IS" -> {
            byteData.valueAsString().withStatus()
        } 	// [0x4953U] Integer String
        "LO" -> {
            byteData.valueAsString().withStatus()
        } 	// [0x4C4FU] Long String
        "LT" -> {
            byteData.valueAsString().withStatus()
        } 	// [0x4C54U] Long Text
        "SH" -> {
            byteData.valueAsString().withStatus()
        } 	// [0x5348U] Short String
        "ST" -> {
            byteData.valueAsString().withStatus()
        } 	// [0x5354U] Short Text
        "UI" -> {
            byteData.valueAsString().withStatus()
        } 	// [0x5549U] Unique Identifier (UID)

        "OB" -> {
            if(byteData.isLengthDefined() /*&& byteData.vl <= DicomDataElement.tooLong*/) {
                //byteData.valueAsHexStr()
                if(byteData.len > 4u) {
                    //println("Long OB")
                    OBItemList.interpretOBData(byteData).withStatus()
                }
                else {
                    //println("Short OB")
                    byteData.valueAsHexStr().withStatus()
                }
            }
            else {
                byteData.withStatus() // shouldn't print in that case
            }
        } 	// [0x4F42U] Other Byte

        //"AE" -> {} 	// [0x4145U] Application Entity
        //"AT" -> {} 	// [0x4154U] Attribute Tag
        "DA" -> { byteData.withStatus(fallbackStatus) } 	// [0x4441U] Date
        //"DT" -> {} 	// [0x4454U] Date Time
        //"FD" -> {} 	// [0x4644U] Floating Point Double
        //"FL" -> {} 	// [0x464CU] Floating Point Single
        //"OL" -> {} 	// [0x4F4CU] Other Long
        //"OD" -> {} 	// [0x4F44U] Other Double
        //"OF" -> {} 	// [0x4F46U] Other Float

        "OW" -> { // currently same as OB, but OW should be affected by Byte Ordering
            if(byteData.isLengthDefined() /*&& byteData.vl <= DicomDataElement.tooLong*/) {
                //byteData.valueAsHexStr()
                if(byteData.len > 4u) {
                    //println("Long OW")
                    OWItemList.interpretOWData(byteData).withStatus()
                }
                else {
                    //println("Short OW")
                    byteData.valueAsHexStr().withStatus()
                }
            }
            else {
                byteData.withStatus() // shouldn't print in that case
            }
        } 	// [0x4F57U] Other Word
        "PN" -> {
            byteData.valueAsString().withStatus()
        } 	// [0x504EU] Person Name
        //"SL" -> {} 	// [0x534CU] Signed Long
        "SQ" -> {
            SQItemList.interpretSQData(byteData).withStatus()
        } 	// [0x5351U] Sequence of Items
        "SS" -> { byteData.withStatus() } 	// [0x5353U] Signed Short
        "TM" -> { byteData.withStatus() } 	// [0x544DU] Time
        //"UC" -> {} 	// [0x5543U] Unlimited Characters
        "UL" -> {
            byteData.valueAsUInt().withStatus()
        } 	// [0x554CU] Unsigned Long
        //"UN" -> {} 	// [0x554EU] Unknown
        //"UR" -> {} 	// [0x5552U] Universal Resource Identifier or Universal Resource Locator(URI/URL)
        "US" -> {
            byteData.valueAsUInt().withStatus()
        } 	// [0x5553U] Unsigned Short
        //"UT" -> {} 	// [0x5554U] Unlimited Text
        "  " -> {
            byteData.valueAsHexStr().withStatus() // shouldn't print in that case
        }   // My VR for control tags
        else -> {
            fun strHex(i: Int) = i.toString(16).padStart(2, '0')
            val warnMsg = "WARN: Can't process data type with VR of ${byteData.vr}. ${strHex(byteData.vr[0].code)} ${strHex(byteData.vr[1].code)}"
            println(warnMsg)
            byteData.withStatus(Status(warnMsg, Status.St.ERROR))
        }
    }
    // IMPORTANT US, UL, OB, SQ
}