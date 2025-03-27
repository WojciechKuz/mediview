

/** Stores cursor (ByteArray position) and ByteArray, which this cursor iterates over. */
class DicomCursor(val bytes: ByteArray, position: Int = 0) {

    var cursor: Int = position
        private set

    /** Abbreviation for cursor */
    val c: Int
        get() = cursor

    /** Check if next nofBytes can be read. */
    fun hasNext(nofBytes: Int = 1): Boolean = cursor + nofBytes < bytes.size

    /** Move cursor by given value forward. */
    fun moveBy(value: Int) {
        cursor += value
    }

    fun field(length: Int) = FieldLength(cursor, cursor + length)

    /** Returns part of ByteArray. Does NOT increase cursor. */
    fun byteField(len: Int) = bytes[cursor, cursor + len]

    /** Reads next length bytes as int. Increases cursor. */
    fun readNextInt(len: Int = 4): Int {
        return littleEndianIntParser(this, len).also { cursor += len }
    }

    /** Reads next length bytes as string. Increases cursor. */
    fun readNextStr(len: Int): String {
        return byteField(len).map { it.toCharStr() }.reduce { acc, i -> acc + i }.also { cursor += len }
    }

    /** Reads next 8 bytes as Dicom tag. Increases cursor. */
    fun readNextTag() = DicomTag.readTag(this) // cursor is increased underneath, cuz it's implemented with readNextInt/Str
}