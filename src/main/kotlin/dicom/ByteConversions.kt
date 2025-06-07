package dicom

import java.nio.ByteOrder

/** Read string tag formatted like `(0028,0100)` or `[0028 0100]` to uint */
fun tagAsUInt(tagStr: String): UInt {
    val b1 = charSequenceToByteArray(tagStr.subSequence(1,5))
    val b2 = charSequenceToByteArray(tagStr.subSequence(6,10))
    return mergeUInt(
        byteArrayToUInt(b1),
        byteArrayToUInt(b2)
    )
}
/** Return tag formatted like `(0028,0100)` from uint */
fun uIntAsTag(tagUInt: UInt): String = "(${hexString(tagUInt shl 16)},${hexString(tagUInt and 0xffffu)})"

/** Used in converting 4 hexadecimal characters to 2 Byte unsigned integer. */
private fun byteArrayToUInt(bytes: ByteArray): UInt {
    var uint: UInt = 0u
    bytes.forEach { b ->
        uint = (uint shl 4) + b.toU()
    }
    return uint
}

/** Used in converting 4 hexadecimal characters to 2 Byte unsigned integer. */
private fun unoptimizedByteArrayToUInt(bytes: ByteArray): UInt = bytes.map { it.toU() }.reduce { acc, byte -> (acc shl 4) + byte }

// THIS IS WRONG char->Byte:  { it.code.toString(16).toByte() }

/** Used in converting 4 hexadecimal characters to 2 Byte unsigned integer. */
private fun charSequenceToByteArray(s: CharSequence): ByteArray {
    val ba = ByteArray(4)
    s.forEachIndexed { i, c ->
        ba[i] = charToByte(c)
    }
    return ba
}

private fun unoptimizedCharSequenceToByteArray(s: CharSequence): ByteArray = s.map { charToByte(it) }.toByteArray()

/** Merge two uint hexadecimal numbers, each 4 hex digits long. */
private fun mergeUInt(u1: UInt, u2: UInt): UInt = (u1 shl 16) + u2

/** Convert hexadecimal digit to byte */
fun charToByte(c: Char): Byte = when(true) {
    ((c >= '0') && (c <= '9')) -> (c - '0')
    ((c >= 'a') && (c <= 'f')) -> (c - 'a' + 10)
    ((c >= 'A') && (c <= 'F')) -> (c - 'A' + 10)
    else -> throw Exception("cannot convert $c to byte")
}.toByte()

/** Converts lower 4 bits to hexadecimal character */
private fun byteToHexChar(u: UInt): Char {
    val v = (u and 0x0Fu).toInt()
    when(v) {
        in 0..9 -> return '0' + v
        in 10..15 -> return 'A' + v
        else -> return ' '
    }
}

/** 1 byte to ascii character as a String. I added this to shorten the byte to character conversion. */
fun Byte.toCharStr() = this.toInt().toChar().toString()

/** Short for `byte.toUByte().toUInt()`. Interpret Byte as UByte, convert to UInt. */
fun Byte.toU() = this.toUByte().toUInt()

/** Using `Byte.toUInt()` is banned in "mediview" project because of unexpected behaviour. Use `Byte.toU()` */
fun Byte.toUInt(): UInt { println("Using `Byte.toUInt()` is banned in \"mediview\" project. Use `Byte.toU()` instead."); return 0u }

/** My toHexString(). HexString in format: FF FF ... */
fun ByteArray.toHexString(): String {
    if (this.isEmpty()) return ""
    return byteArrToHexStrOptLvl2(this)
}

private fun byteArrToHexStrUnoptimized(ba: ByteArray): String {
    // for ByteArray 275466 long, 9156 ms!
    return ba.map { String.format("%02X ", it) }.reduce { acc, s -> acc + s }.trim()
}
private fun byteArrToHexStrOptLvl1(ba: ByteArray): String {
    // for ByteArray 275466 long, 1145 ms!
    val sb = StringBuilder()
    ba.forEach { sb.append(String.format("%02X ", it)) }
    sb.trim()
    return sb.toString()
}

private fun byteArrToHexStrOptLvl2(ba: ByteArray): String {
    // for ByteArray 275466 long, 11 ms!
    val chs = CharArray(ba.size * 3)
    ba.forEachIndexed { i, byte ->
        val u = byte.toU()
        chs[3*i  ] = byteToHexChar(u shl 4)
        chs[3*i+1] = byteToHexChar(u)
        chs[3*i+2] = ' '
    }
    //return chs.toString().trim()
    return String(chs).trim()
}

/** return 4 digit hexadecimal. */
fun hexString(uInt: UInt, pad: Int = 4) = uInt.toString(16).padStart(pad, '0')

/** Parse (unsigned) int with given endian. Parse first length bytes in FieldLength range. Does NOT increase cursor. */
fun endianIntParser(cursor: DicomCursor, endian: ByteOrder, len: UInt = 4u): UInt {
    if(cursor.hasNext(len)) throw Exception("ByteArray is too short to parse Int 🤨")
    return endianIntParser(cursor.byteField(len), endian)
}

/** Parse (unsigned) int with given endian. Parse first length bytes in FieldLength range. Does NOT increase cursor. */
fun endianIntParser(bytes: ByteArray, endian: ByteOrder): UInt {
    if(endian == ByteOrder.BIG_ENDIAN) {
        return bytes.map { it.toU() }.reduce { acc, i -> acc * 256u + i }
    }
    return bytes.map { it.toU() }.reduceRight { i, acc -> acc * 256u + i } //.reduce { acc, i -> acc + i * 256u }
}

/** Parse (unsigned) int with little endian. Parse first length bytes in FieldLength range. Does NOT increase cursor. */
fun littleEndianIntParser(cursor: DicomCursor, len: UInt = 4u): UInt = littleEndianIntParser(cursor.byteField(len))

fun littleEndianIntParser(bytes: ByteArray): UInt = bytes.map { it.toU() }.reduceRight { i, acc -> acc * 256u + i  }

// Does not make sense. practically everytime it's little endian.
//fun getEncoding(bitsAllocated: UInt) = if(bitsAllocated > 8) ByteOrder.LITTLE_ENDIAN else