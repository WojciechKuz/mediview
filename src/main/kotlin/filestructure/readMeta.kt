package filestructure

import DicomCursor
import java.nio.ByteOrder


val preambleLength = 128
val endianness: ByteOrder = ByteOrder.LITTLE_ENDIAN

// reading tags start here

fun informationGroupLength(cursor: DicomCursor): Int {
    val tag = cursor.readNextTag()
    val infoGroupLength = cursor.readNextInt(tag.len)
    println("$tag info group length: $infoGroupLength")
    return infoGroupLength
}

// TODO universal tag reading function.
// maybe only one special case for information version, which has 2 byte offset.
