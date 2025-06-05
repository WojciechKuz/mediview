package dicom.filestructure

import dicom.DicomCursor
import dicom.DicomCursorException
import dicom.toU

object Header {
    const val preambleLength = 128
    const val dicomPrefixLen = 4 // "DICM"

    /** returns file preamble as List of Int (1 Int for every byte in file)
     *  use `resultList.map { it.toString(16) } // to hex. There are two options: toString(16) or toHexString() (experimental)`
     *  to convert to hexadecimal numbers. */
    fun filePreamble(cursor: DicomCursor): List<UInt> {
        if (!cursor.hasNext(preambleLength))
            throw DicomCursorException("preamble")

        val preambleAsInts = cursor.readNextByteField(preambleLength).map { it.toU() }
        if(preambleAsInts.any { it != 0u })
            println("file preamble has something interesting")
        else
            println("file preamble - 128 bytes of 0x00.")
        return preambleAsInts
    }

    fun skipPreamble(cursor: DicomCursor) {
        if (!cursor.hasNext(preambleLength))
            throw DicomCursorException("preamble")

        cursor.moveBy(preambleLength)
    }

    fun dicomPrefix(cursor: DicomCursor, printDICM: Boolean = false): String {
        if (!cursor.hasNext(dicomPrefixLen))
            throw DicomCursorException("prefix")

        val prefix = cursor.readNextStr(dicomPrefixLen)
        if(printDICM)
            println(prefix)
        return prefix
    }
}