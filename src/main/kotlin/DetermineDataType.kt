

fun determineDataType(byteData: DicomByteData): DicomDataElement<out Any> {
    //
    return when(byteData.vr) {
        "AS" -> {
            byteData.valueAsString()
        } 	// [0x4153U] Age String
        "CS" -> {
            byteData.valueAsString()
        } 	// [0x4353U] Code String
        "DS" -> {
            byteData.valueAsString()
        } 	// [0x4453U] Decimal String
        "IS" -> {
            byteData.valueAsString()
        } 	// [0x4953U] Integer String
        "LO" -> {
            byteData.valueAsString()
        } 	// [0x4C4FU] Long String
        "LT" -> {
            byteData.valueAsString()
        } 	// [0x4C54U] Long Text
        "SH" -> {
            byteData.valueAsString()
        } 	// [0x5348U] Short String
        "ST" -> {
            byteData.valueAsString()
        } 	// [0x5354U] Short Text
        "UI" -> {
            byteData.valueAsString()
        } 	// [0x5549U] Unique Identifier (UID)

        "OB" -> { byteData } 	// [0x4F42U] Other Byte

        //"AE" -> {} 	// [0x4145U] Application Entity
        //"AT" -> {} 	// [0x4154U] Attribute Tag
        "DA" -> { byteData } 	// [0x4441U] Date
        //"DT" -> {} 	// [0x4454U] Date Time
        //"FD" -> {} 	// [0x4644U] Floating Point Double
        //"FL" -> {} 	// [0x464CU] Floating Point Single
        //"OL" -> {} 	// [0x4F4CU] Other Long
        //"OD" -> {} 	// [0x4F44U] Other Double
        //"OF" -> {} 	// [0x4F46U] Other Float
        "OW" -> { byteData } 	// [0x4F57U] Other Word
        "PN" -> {
            byteData.valueAsString()
        } 	// [0x504EU] Person Name
        //"SL" -> {} 	// [0x534CU] Signed Long
        "SQ" -> { byteData } 	// [0x5351U] Sequence of Items
        "SS" -> { byteData } 	// [0x5353U] Signed Short
        "TM" -> { byteData } 	// [0x544DU] Time
        //"UC" -> {} 	// [0x5543U] Unlimited Characters
        "UL" -> {
            byteData.valueAsUInt(4) // FIXME u sure 4?
        } 	// [0x554CU] Unsigned Long
        //"UN" -> {} 	// [0x554EU] Unknown
        //"UR" -> {} 	// [0x5552U] Universal Resource Identifier or Universal Resource Locator(URI/URL)
        "US" -> {
            byteData.valueAsUInt(2) // FIXME u sure 2?
        } 	// [0x5553U] Unsigned Short
        //"UT" -> {} 	// [0x5554U] Unlimited Text
        else -> {
            throw Exception("Can't process data type with VR of ${byteData.vr}.")
        }
    }
    // IMPORTANT US, UL, OB, SQ
}