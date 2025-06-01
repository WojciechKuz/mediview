package filestructure

import DicomByteData
import DicomCursor
import DicomDataElement
import OBItemList
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
    private fun getPixelData(imgInfo: TagToDataMap): DicomByteData {
        val itemList = (imgInfo.getTag("(7FE0,0010)").value as OBItemList)
        if(itemList.isEmpty()) {
            throw Exception("Item List for a tag (7FE0,0010) is empty")
        }
        if(itemList.size > 2)
            println("NOTE: Usually tag (7FE0,0010) has 2 items. This one has ${itemList.size}.")
        // The first Item in the Sequence of Items before the encoded Pixel Data Stream shall be a Basic Offset Table item.
        // Just take second one, not first with non-zero length
        if (itemList.size >= 2 && (itemList[1].len > 0u)) {
            return itemList[1]
        }
        throw Exception("Item List for a tag (7FE0,0010) does not contain non-empty ByteArray")
    }

    fun readImageData(cursor: DicomCursor, imgInfo: TagToDataMap) {
        val rows = imgInfo.getTag("(0028,0010)").value as UInt
        val columns = imgInfo.getTag("(0028,0011)").value as UInt
        val bitsAllocated = imgInfo.getTag("(0028,0100)").value as UInt
        val bitsStored = imgInfo.getTag("(0028,0101)").value as UInt
        val highBit = imgInfo.getTag("(0028,0102)").value as UInt
        val pixelRepresentation = imgInfo.getTag("(0028,0103)").value as UInt
        val pixelData = getPixelData(imgInfo)

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

// TODO dicom image (tag 7fe0 0010) to BitmapPainter (Kt Compose)
//