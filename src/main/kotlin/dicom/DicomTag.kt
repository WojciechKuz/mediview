package dicom

/** Stores tag of the data field.
 *  readTag() reads tag from ByteArray.
 *  First in dicom file is hex2, then hex1
 *  @param hex1 1/2 part of 2 byte tag. "group number"
 *  @param hex2 2/2 part of 2 byte tag. "element number"
 *  @param vr 2 character code. Value representation, type of tag
 *  @param vl length of value field
 */
open class DicomTag(hex1: UInt, hex2: UInt, val vr: String, val vl: UInt) {

    /** ordered like in documentation. hex1, then hex2 but both in little-endian. */
    val tag: UInt = ((hex1 shl 16) + hex2) // shl means shift left
    val tagPt1: UInt
        get() = (tag shr 16)
    val tagPt2: UInt
        get() = (tag and 0xffFFu)
    val len: UInt
        get() = vl

    override fun toString(): String {
        val tagStr = "[${hexString(tagPt1)} ${hexString(tagPt2)}]"
        if (isLengthDefined())
            return "$tagStr $vr $vl"
        return "$tagStr $vr length undefined"
    }

    fun getStringTag() = "[${hexString(tagPt1)} ${hexString(tagPt2)}]"

    fun isLengthDefined(): Boolean = !(vl == 0xffFFffFF.toUInt() && vrAllowsUndefinedLength())

    /** checks if cursor has next len bytes */
    fun canReadValue(cursor: DicomCursor) = cursor.hasNext(vl)

    private fun vrAllowsUndefinedLength() = when(vr) {
        "OB", "OW", "SQ", "UN" -> true
        "UT" -> false
        else -> false
    }

    companion object {
        /** If data element IN FILE has additional 4 bytes to store value length as 32 bit unsigned int.
         * Default tag length is 8 bytes, but if this is true, then tag length is increased to 12 bytes. */
        private fun hasAdditional4Bytes(vr: String): Boolean = when(vr) {
            "OB", "OW", "SQ", "UN" -> true     // has 32bit length displaced by 2 bytes
            "UT" -> true
            else -> false
        }

        /** Check if file is long enough to read next tag.
         * @return number of bytes that are safe to read. 0, 8 or 12. */
        fun canReadTag(cursor: DicomCursor): UInt {
            if (!cursor.hasNext(8)) {
                return 0u
            }
            val bytes = cursor.bytes[cursor.cursor, cursor.cursor + 2u]
            val code = bytes.map { it.toCharStr() }.reduce { acc, i -> acc + i }
            if (hasAdditional4Bytes(code))
                return 12u
            return 8u
        }

        /** Reads 8 byte tag from ByteArray, on position pointed by cursor.
         *  example: 8 bytes -> readTag() = DicomTag(0x0002, 0x0003, "UI", 50) // [0002 0003] UI 50.
         *  hex1 and hex2 make tag together, code (is some kind of name abbreviation), length is field length.
         *  every hex1, hex2, code, len is 2 bytes.
         *  Increases cursor.
         */
        fun readTag(cursor: DicomCursor): DicomTag {
            if (!cursor.hasNext(8)) {
                throw Exception(
                    """At cursor ${cursor.cursor} ByteArray is too short to read tag 🤨.
                       ByteArray ends at ${cursor.bytes.size}.""".trimMargin()
                )
            }
            val hex1 = cursor.readNextInt(2)
            val hex2 = cursor.readNextInt(2)    // both are TAG

            if(hex1 == 0xFFFEu) { // control tag, doesn't have VR
                val len = cursor.readNextInt(4)
                return DicomTag(
                    hex1, hex2, "  ", len
                )
            }

            val code = cursor.readNextStr(2)    // VR
            val isLen32bit = hasAdditional4Bytes(code)
            val len = if(isLen32bit) {
                if (!cursor.hasNext(2+4)) {
                    throw Exception("ByteArray is too short to read tag 🤨")
                }
                //val followingVR = cursor.readNextByteField(2)
                cursor.moveBy(2)
                cursor.readNextInt(4)
            }
            else {
                cursor.readNextInt(2)
            }
            return DicomTag(
                hex1,
                hex2,
                code,
                len,
            )
        }
    }
}