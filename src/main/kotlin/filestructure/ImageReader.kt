package filestructure

import DicomCursor
import DicomDataElement
import TagToDataMap
import tagAsUInt

object ImageReader {
    private fun TagToDataMap.getTag(tag: String): DicomDataElement<out Any> {
        val utag = tagAsUInt(tag)
        if(this.containsKey(utag)) {
            return this[utag]!!
        }
        throw Exception("Tag $tag not found in TagToDataMap")
    }
    fun skipImageData(cursor: DicomCursor, imgInfo: TagToDataMap) {
        val rows = imgInfo.getTag("(0028,0010)").value as UInt
        val columns = imgInfo.getTag("(0028,0011)").value as UInt
        val bitsAllocated = imgInfo.getTag("(0028,0100)").value as UInt
        val bitsStored = imgInfo.getTag("(0028,0101)").value as UInt
        val highBit = imgInfo.getTag("(0028,0102)").value as UInt

        val skipBy = rows * columns * (bitsAllocated / 8u) / 2u // FIXME 512*512*2 262144 != official 275442
        println("Skip image - $skipBy Bytes")
        cursor.readNextTag()
        val cursorBeforeMove = cursor.cursor
        cursor.moveBy(
            skipBy
        )
        println("Cursor moved from $cursorBeforeMove to ${cursor.cursor}, file length ${cursor.bytes.size}, can${if(cursor.hasReachedEnd()) "'t" else ""} read any more\n")
    }
}