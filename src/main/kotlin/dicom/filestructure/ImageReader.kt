package dicom.filestructure

import dicom.DicomByteData
import dicom.DicomCursor
import dicom.DicomDataElement
import dicom.OBItemList
import dicom.TagToDataMap
import dicom.tagAsUInt

object ImageReader {
    private fun TagToDataMap.getTag(tag: String): DicomDataElement<out Any> {
        val utag = tagAsUInt(tag)
        if(this@getTag.containsKey(utag)) {
            return this@getTag[utag]!!
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
        readImageData(imgInfo)
        println("Cursor moved to ${cursor.cursor}, file length ${cursor.bytes.size}, can${if(cursor.hasReachedEnd()) "'t" else ""} read any more\n")
    }

    fun readImageData(imgInfo: TagToDataMap) {
        val rows = imgInfo.getTag("(0028,0010)").value as UInt
        val columns = imgInfo.getTag("(0028,0011)").value as UInt
        val bitsAllocated = imgInfo.getTag("(0028,0100)").value as UInt
        val bitsStored = imgInfo.getTag("(0028,0101)").value as UInt
        val highBit = imgInfo.getTag("(0028,0102)").value as UInt
        val pixelRepresentation = imgInfo.getTag("(0028,0103)").value as UInt
        val photometricInterpretation = imgInfo.getTag("(0028,0004)").value as String
        val samplesPerPixel = imgInfo.getTag("(0028,0002)").value as UInt
        val pixelData = getPixelData(imgInfo)
        println(
            """Info about image:
            - rows $rows, columns $columns,
            - bits alloc $bitsAllocated, bits stored $bitsStored, high bit $highBit,
            - pixelRepresentation: $pixelRepresentation,
            - photometricInterpretation: $photometricInterpretation,
            - samplesPerPixel: $samplesPerPixel
            """.trimMargin()
        )
    }
}

// TODO dicom image (tag 7fe0 0010) to BitmapPainter (Kt Compose)
//