import java.nio.ByteOrder

/** Merge two uint hexadecimal numbers, each 4 hex digits long. */
fun mergeUInt(u1: UInt, u2: UInt): UInt = (u1 shl 16) + u2

/** 1 byte to ascii character as a String. I added this to shorten the byte to character conversion. */
fun Byte.toCharStr() = this.toInt().toChar().toString()

/** Short for `byte.toUByte().toUInt()`. Interpret Byte as UByte, convert to UInt. */
fun Byte.toU() = this.toUByte().toUInt()

/** Using `Byte.toUInt()` is banned in "mediview" project because of unexpected behaviour. Use `Byte.toU()` */
fun Byte.toUInt(): UInt { println("Using `Byte.toUInt()` is banned in \"mediview\" project. Use `Byte.toU()` instead."); return 0u }

fun ByteArray.toHexString() = this.map { String.format("%02X ", it) }.reduce { acc, s -> acc + s }.trim()

/** return 4 digit hexadecimal */
fun hexString(uInt: UInt) = uInt.toString(16).padStart(4, '0')

/** Parse (unsigned) int with given endian. Parse first length bytes in FieldLength range. Does NOT increase cursor. */
fun endianIntParser(cursor: DicomCursor, endian: ByteOrder, len: UInt = 4u): UInt {
    if(cursor.hasNext(len)) throw Exception("ByteArray is too short to parse Int 🤨")
    if(endian == ByteOrder.BIG_ENDIAN) {
        return cursor.byteField(len).map { it.toU() }.reduce { acc, i -> acc * 256u + i }
    }
    return cursor.byteField(len).map { it.toU() }.reduceRight { i, acc -> acc * 256u + i } //.reduce { acc, i -> acc + i * 256u }
}

/** Parse (unsigned) int with little endian. Parse first length bytes in FieldLength range. Does NOT increase cursor. */
fun littleEndianIntParser(cursor: DicomCursor, len: UInt = 4u): UInt = cursor.byteField(len).map { it.toU() }.reduceRight { i, acc -> acc * 256u + i  }
