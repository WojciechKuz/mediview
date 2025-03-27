import java.nio.ByteOrder


operator fun ByteArray.get(fl: FieldLength): ByteArray {
    return this[fl.beg, fl.end]
}

/** To ascii character as String. I added this to shorten the byte to character conversion. */
fun Byte.toCharStr() = this.toInt().toChar().toString()

/** Parse int with given endian. Parse first 4 bytes in FieldLength range */
@Deprecated("Using FieldLength is no longer supported. womp womp", ReplaceWith("littleEndianIntParser(cursor, length)"))
fun endianIntParser(bytes: ByteArray, fl: FieldLength, endian: ByteOrder): Int {
    val fl2 = if(fl.end - fl.beg > 4) FieldLength(fl.beg, fl.beg + 4) else fl
    if(endian == ByteOrder.BIG_ENDIAN) {
        return bytes[fl2].map { it.toInt() }.reduce { acc, i -> acc * 256 + i }
    }
    return bytes[fl2].map { it.toInt() }.reduce { acc, i -> acc + i * 256 }
}

/** Parse int with given endian. Parse first length bytes in FieldLength range */
fun endianIntParser(cursor: DicomCursor, endian: ByteOrder, len: Int = 4): Int {
    if(cursor.hasNext(len)) throw Exception("ByteArray is too short to parse Int 🤨")
    if(endian == ByteOrder.BIG_ENDIAN) {
        return cursor.byteField(len).map { it.toInt() }.reduce { acc, i -> acc * 256 + i }
    }
    return cursor.byteField(len).map { it.toInt() }.reduce { acc, i -> acc + i * 256 }
}

fun littleEndianIntParser(cursor: DicomCursor, len: Int = 4): Int = cursor.byteField(len).map { it.toInt() }.reduce { acc, i -> acc + i * 256 }

/*
interface DicomValue {}

class DicomInt(val value: Int) : DicomValue {
    val v: Int
        get() = value
    companion object {
        fun readInt(cursor: DicomCursor, len: Int) = DicomInt(littleEndianIntParser(cursor, len))
    }
}

class DicomString(val str: String): DicomValue {
    companion object {
        fun readString(cursor: DicomCursor, len: Int) = DicomString(
            cursor.byteField(len).map { it.toString() }. reduce { acc, i -> acc + i }
        )
    }
}
*/