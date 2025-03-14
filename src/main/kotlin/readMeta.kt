
/** returns file preamble either List of Int
 *  use `resultList.map { it.toString(16) } // to hex. There are two options: toString(16) or toHexString() (experimental)`
 *  to convert to hexadecimal numbers. */
fun filePreamble(bytes: ByteArray): List<Int> {
    if(bytes.size > 128) {
        val preambleAsInts = bytes[0, 128].map { it.toInt() }
        if(preambleAsInts.any { it != 0 })
            println("file preamble has something interesting")
        return preambleAsInts
    }
    return listOf()
}

fun dicomPrefix(bytes: ByteArray): String {
    if(bytes.size > (128+4)) {
        val prefix = bytes[128, (128+4)].map { it.toInt().toChar().toString() }.reduce { acc, b -> "$acc$b" }
        println(prefix)
        return prefix
    }
    return ""
}