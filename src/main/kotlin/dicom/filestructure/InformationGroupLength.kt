package dicom.filestructure

import dicom.DicomCursor

// reading tags start here

// Formerly called "readMeta.kt"

fun informationGroupLength(cursor: DicomCursor): UInt {
    val tag = cursor.readNextTag()
    val infoGroupLength = cursor.readNextInt(tag.len)
    println("$tag info group length: $infoGroupLength")
    return infoGroupLength
}