

/** Stores cursor (ByteArray position) and ByteArray, which this cursor iterates over.
 *  Before you read any value through readNext* methods, make sure file is long enough to read this value:
 *  ```
 *  if(!cursor.hasNext(nofBytes))
 *      throw DicomCursorException(...)
 *  ```
 *  */
class DicomCursor(val bytes: ByteArray, position: UInt = 0u): Comparable<UInt> {

    constructor(prevCursor: DicomCursor): this(prevCursor.bytes, prevCursor.cursor)

    var cursor: UInt = position
        private set

    /** Abbreviation for cursor */
    val c: UInt
        get() = cursor

    /** Check if next nofBytes can be read. */
    fun hasNext(nofBytes: Int = 1): Boolean = hasNext( nofBytes.toUInt() )
    /** Check if next nofBytes can be read. */
    fun hasNext(nofBytes: UInt = 1u): Boolean = cursor + nofBytes < bytes.size.toUInt()

    fun hasReachedEnd() = !hasNext(1)

    /** Check if next tag can be read. */
    fun hasNextTag(): Boolean = DicomTag.canReadTag(this) > 0u

    /** Move cursor by given value forward. */
    fun moveBy(value: UInt) {
        cursor += value
    }
    /** Move cursor by given value forward. */
    fun moveBy(value: Int) = moveBy(value.toUInt())

    /** Returns part of ByteArray. Does NOT increase cursor. */
    fun byteField(len: UInt) = bytes[cursor, (cursor + len)]   // Used in EndianParserFunctions.kt
    /** Returns part of ByteArray. Does NOT increase cursor. */
    fun byteField(len: Int) = byteField(len.toUInt())

    /** Reads next length bytes as int. Increases cursor. */
    fun readNextInt(len: UInt = 4u): UInt {
        return littleEndianIntParser(this, len).also { cursor += len }
    }
    /** Reads next length bytes as int. Increases cursor. */
    fun readNextInt(len: Int = 4): UInt = readNextInt(len.toUInt())

    /** Reads next length bytes as string. Increases cursor. */
    fun readNextStr(len: UInt): String {
        return byteField(len).map { it.toCharStr() }.reduce { acc, i -> acc + i }.also { cursor += len }
    }
    /** Reads next length bytes as string. Increases cursor. */
    fun readNextStr(len: Int): String = readNextStr(len.toUInt())

    /** Reads next length byte field of given length. Increases cursor. */
    fun readNextByteField(len: UInt) = byteField(len).also { cursor += len }
    /** Reads next length byte field of given length. Increases cursor. */
    fun readNextByteField(len: Int) = readNextByteField(len.toUInt())

    /** Reads next 8 bytes as Dicom tag. Increases cursor. */
    fun readNextTag() = DicomTag.readTag(this) // cursor is increased underneath, cuz it's implemented with readNextInt/Str

    /** @param tag 4 byte tag
     * @return new cursor with position just before tag. If nothing found, returns cursor set to the end. */
    fun findTag(tag: UInt): DicomCursor {
        val iterateCursor = DicomCursor(this)
        while(iterateCursor.hasNextTag()) {
            val cursorBeforeTag = iterateCursor.cursor
            val nextTag = iterateCursor.readNextTag()
            if(nextTag.tag == tag) {
                //return DicomCursor(iterateCursor.bytes, cursorBeforeTag)
                iterateCursor.moveBy(cursorBeforeTag - iterateCursor.cursor) // move back cursor
                return iterateCursor
            }
            if(!nextTag.canReadValue(iterateCursor))
                break
            iterateCursor.moveBy(nextTag.len)
        }
        return iterateCursor
    }

    override fun compareTo(other: UInt): Int = cursor.compareTo(other)
}