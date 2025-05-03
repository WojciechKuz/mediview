import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.experimental.inv

/*
operator fun ByteArray.get(fl: FieldLength): ByteArray {
    return this[fl.beg, fl.end]
}
*/

/** 1 byte to ascii character as a String. I added this to shorten the byte to character conversion. */
fun Byte.toCharStr() = this.toInt().toChar().toString()

/** Used in Byte to Int conversion, where byte is like unsigned integer. Treats negative sign of an Int like 128. */
fun Int.toPositiveInt() = if(this < 0) -((this and 0b0111_1111).inv() + 1) + 128 else this

/** Byte conversions in Kotlin are hard. Treats byte like unsigned integer, but writes this value to Int. */
fun Byte.toPositiveInt() = this.toInt().toPositiveInt()

fun ByteArray.toHexString() = this.map { String.format("%02X ", it) }.reduce { acc, s -> acc + s }.trim()

/** Parse (unsigned) int with given endian. Parse first length bytes in FieldLength range. Does NOT increase cursor. */
fun endianIntParser(cursor: DicomCursor, endian: ByteOrder, len: Int = 4): Int {
    if(cursor.hasNext(len)) throw Exception("ByteArray is too short to parse Int 🤨")
    if(endian == ByteOrder.BIG_ENDIAN) {
        return cursor.byteField(len).map { it.toPositiveInt() }.reduce { acc, i -> acc * 256 + i }
    }
    return cursor.byteField(len).map { it.toPositiveInt() }.reduce { acc, i -> acc + i * 256 }
}

/** Parse (unsigned) int with little endian. Parse first length bytes in FieldLength range. Does NOT increase cursor. */
fun littleEndianIntParser(cursor: DicomCursor, len: Int = 4): Int = cursor.byteField(len).map { it.toPositiveInt() }.reduce { acc, i -> acc + i * 256 }

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