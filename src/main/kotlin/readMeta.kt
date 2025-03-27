

val preambleLength = 128


/** returns file preamble as List of Int
 *  use `resultList.map { it.toString(16) } // to hex. There are two options: toString(16) or toHexString() (experimental)`
 *  to convert to hexadecimal numbers. */
fun filePreamble(cursor: DicomCursor): List<Int> {
    if(cursor.hasNext(preambleLength)) {
        val preambleAsInts = cursor.byteField(preambleLength).map { it.toInt() }
        cursor.moveBy(preambleLength)
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

// reading tags start here

fun informationGroupLength(cursor: DicomCursor): Int {
    val tag = cursor.readNextTag()
    val infoGroupLength = cursor.readNextInt(tag.len)
    println("$tag info group length: $infoGroupLength")
    return infoGroupLength
}

// TODO universal tag reading function.
// maybe only one special case for information version, which has 2 byte offset.
