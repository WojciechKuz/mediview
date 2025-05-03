
// this file is just for reference

/*

// /x*x* Stores reference to last created FieldLength object *x/
lateinit var last: FieldLength

@Deprecated("xd", ReplaceWith("DicomCursor"))
class FieldLength(val beg: Int, val end: Int = 0) {
    init {last = this}
    fun nextField(length: Int) = FieldLength(end, end + length)
}


// hmm, better define 1 or 2 loaders (int and string) and add these values to mutableMap
val preambleFL = FieldLength(0, 128)
val dicomPrefixFL = last.nextField(4) // "DICM"

val tag01FL = last.nextField(8) // [0002 0000] UL 04 00       // UL = Length to end
val informationGroupLengthFL = last.nextField(4)  // d196 00 00 00

val tag02FL = last.nextField(8) // [0002 0001] OB 00 00
val unknownOffsetFL = last.nextField(2) // 02 00 // ???? // FIXME what is it?
val informationVersionFL = last.nextField(4) //  00 00 00 01

val tag03FL = last.nextField(8) // [0002 0002] UI 1A 0 // 0x1A = 26, OB = Record key, UI = instace creator id
val mediaStorageSOPClassUID_FL = last.nextField(26) // "1.2.840.10008.5.1.4.1.1.2" \0

val tag04FL = last.nextField(8) // [0002 0003] UI 32 00 // 0x32 = 50
val mediaStorageSOPInstanceUID_FL = last.nextField(50) // "2.25.1265666812068438113210734800235235159893.1.3" \0

val tag05FL =last.nextField(8) // [0002 0010] UI 16 00 // 0x16 = 22
val TransferSyntaxUID_FL = last.nextField(22)       // "1.2.840.10008.1.2.4.70" NO-ENDL!

val tag06FL = last.nextField(8) // [0002 0012] UI 1C 00 // 0x1C = 28
val ImplementationClassUID_FL = last.nextField(28)  // "1.2.276.0.7230010.3.0.3.6.8" \0

val tag07FL = last.nextField(8) // [0002 0013] SH 10 00 // 0x10 = 16
val implementationVersionNameFL = last.nextField(16) // "OFFIS_DCMTK_368 "

val tag08FL = last.nextField(8) // [0008 0005] CS 0A 00 // 0x0A = 10
val characterSetFL = last.nextField(10) // "ISO_IR 100"

val endianness: ByteOrder = ByteOrder.LITTLE_ENDIAN
*/

/*
/** returns file preamble as List of Int
 *  use `resultList.map { it.toString(16) } // to hex. There are two options: toString(16) or toHexString() (experimental)`
 *  to convert to hexadecimal numbers. */
@Deprecated("", ReplaceWith("filePreamble(cursor: DicomCursor)"))
fun filePreambleOld(bytes: ByteArray): List<Int> {
    if(bytes.size >= preambleFL.end) {
        val preambleAsInts = bytes[preambleFL.beg, preambleFL.end].map { it.toInt() }
        if(preambleAsInts.any { it != 0 })
            println("file preamble has something interesting")
        return preambleAsInts
    }
    return listOf()
}

@Deprecated("", ReplaceWith("dicomPrefix(cursor: DicomCursor)"))
fun dicomPrefixOld(bytes: ByteArray): String {
    if(bytes.size >= dicomPrefixFL.end) {
        val prefix = bytes[dicomPrefixFL.beg, dicomPrefixFL.end].map { it.toInt().toChar().toString() }.reduce { acc, b -> "$acc$b" }
        println(prefix)
        return prefix
    }
    return ""
}
*/

// "Except for the 128 byte preamble and the 4 byte prefix, ... Explicit VR Little Endian Transfer Syntax"



/*
/** Number of bytes following this File Meta Element (end of the Value field) up to and including
 *  the last File Meta Element of the Group 2 File Meta Information */
fun informationGroupLength(bytes: ByteArray): Int {
    if(bytes.size >= informationGroupLength.end) {
        return bytes[informationGroupLength.beg].toInt()
    }
    return -3
}

/** This is a two byte field where each bit identifies a version of this File Meta Information header.
 *  In version 1 the first byte value is 00H and the second value byte value is 01H.
 *  In example data both bytes were set to 0 in hex editor :/ */
fun informationVersion(bytes: ByteArray): Int {
    if(bytes.size >= informationVersion.end) {
        val v = bytes[informationVersion.beg, informationVersion.end]
        val version = if(endianness == ByteOrder.LITTLE_ENDIAN) {
            v[0].toInt() + v[1].toInt() * 256
        } else {
            v[0].toInt() * 256 + v[1].toInt()
        }
        return version
    }
    return -4
}
*/

