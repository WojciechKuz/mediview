package filestructure

import DicomCursor

object Header {
    /** returns file preamble as List of Int (1 Int for every byte in file)
     *  use `resultList.map { it.toString(16) } // to hex. There are two options: toString(16) or toHexString() (experimental)`
     *  to convert to hexadecimal numbers. */
    fun filePreamble(cursor: DicomCursor): List<Int> {
        if(cursor.hasNext(preambleLength)) {
            val preambleAsInts = cursor.readNextByteField(preambleLength).map { it.toInt() }
            if(preambleAsInts.any { it != 0 })
                println("file preamble has something interesting")
            else
                println("file preamble - 128 bytes of 0x00.")
            return preambleAsInts
        }
        return listOf()
    }

    fun dicomPrefix(cursor: DicomCursor): String {
        val dicomPrefixLen = 4
        if(cursor.hasNext(dicomPrefixLen)) {
            val prefix = cursor.readNextStr(dicomPrefixLen)
            println(prefix)
            return prefix
        }
        return ""
    }
}