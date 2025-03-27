import java.nio.ByteOrder

/** Stores tag of the data field.
 *  readTag() reads tag from ByteArray.
 */
class DicomTag(val hex1: Int, val hex2: Int, val code: String, val len: Int) {

    override fun toString(): String {
        return "[${hex1.toString(16)} ${hex2.toString(16)}] $code $len"
    }

    companion object {

        /*
        /** Reads tag from ByteArray.
         *  example: 8 bytes -> readTag() = DicomTag(0x0002, 0x0003, "UI", 50) // [0002 0003] UI 50.
         *  hex1 and hex2 make tag together, code (is some kind of name abbreviation), length is field length.
         *  every hex1, hex2, code, len is 2 bytes.
         *  Always reads 8 bytes. FieldLength is used only to find beginning of tag.
         */
        @Deprecated("lol", ReplaceWith("DicomTag.readTag(cursor)"))
        fun readTag(bytes: ByteArray, fl: FieldLength): DicomTag = readTag(bytes, fl.beg)

        /** Reads tag from ByteArray.
         *  example: 8 bytes -> readTag() = DicomTag(0x0002, 0x0003, "UI", 50) // [0002 0003] UI 50.
         *  hex1 and hex2 make tag together, code (is some kind of name abbreviation), length is field length.
         *  every hex1, hex2, code, len is 2 bytes.
         *  Always reads 8 bytes starting at beginning.
         */
        @Deprecated("lol", ReplaceWith("DicomTag.readTag(cursor)"))
        fun readTag(bytes: ByteArray, beginning: Int): DicomTag {
            val hex1field = FieldLength(beginning, beginning + 2)
            val hex2field = hex1field.nextField(2)
            val codeField = hex2field.nextField(2)
            val lenField = codeField.nextField(2)
            return DicomTag(
                endianIntParser(bytes, hex1field, ByteOrder.LITTLE_ENDIAN),
                endianIntParser(bytes, hex2field, ByteOrder.LITTLE_ENDIAN),
                bytes[codeField].toString(),
                endianIntParser(bytes, lenField, ByteOrder.LITTLE_ENDIAN)
            )
        }
        */

        /** Reads 8 byte tag from ByteArray, on position pointed by cursor.
         *  example: 8 bytes -> readTag() = DicomTag(0x0002, 0x0003, "UI", 50) // [0002 0003] UI 50.
         *  hex1 and hex2 make tag together, code (is some kind of name abbreviation), length is field length.
         *  every hex1, hex2, code, len is 2 bytes.
         */
        fun readTag(cursor: DicomCursor) = if(cursor.hasNext(8)) {
            DicomTag(
                cursor.readNextInt(2),
                cursor.readNextInt(2),
                cursor.readNextStr(2),
                cursor.readNextInt(2),
            )
        } else { throw Exception("ByteArray is too short to read tag 🤨") }
    }
}