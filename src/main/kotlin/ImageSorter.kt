import dicom.OBItemList
import dicom.TagToDataMap
import dicom.filestructure.DataRead
import dicom.tagAsUInt

/** Just pass in directory that contains DICOM files.
 * @property sortedData loaded and sorted data */
private class ImageSorter(directory: String) {
    val sortedData: List<TagToDataMap> = sortDataOnConstruct(directory)

    /** Used to sort image data. sorted images are stored in this class */
    private fun sortDataOnConstruct(directory: String): List<TagToDataMap> {
        val cursors = ReadHelp.listFilesInDir(directory).map { fileName -> ReadHelp.cursorAtDataSet(directory + fileName) }
        //val fullDataMap = DataRead().getFullDataMap(cursors[0])
        //val selectedTags: List<UInt>
        val imagesDataMap = cursors.map { cursor ->
            //DataRead().getPartialDataMap(cursor, selectedTags)
            DataRead().getFullDataMap(cursor)
        }

        val sortedImagesData = sortByInstanceNumber(imagesDataMap)
        //val sortedImagesData = sortBySliceLocation(imagesDataMap)
        //val sortedImagesData = sortByImagePosition(imagesDataMap) // TODO sort by image position
        return sortedImagesData
    }


    companion object {
        private fun sortByInstanceNumber(unsortedData: List<TagToDataMap>): List<TagToDataMap> {
            val instanceNumTag = tagAsUInt("(0020,0013)") // It is 'IS' - integer string
            return unsortedData.sortedBy { data ->
                (data[instanceNumTag]!!.value as String).trim().toInt()
            }
        }
        private fun sortBySliceLocation(unsortedData: List<TagToDataMap>): List<TagToDataMap> {
            val sliceLocationTag = tagAsUInt("(0020,1041)") // It is 'DS' - decimal string, example: -188.2
            return unsortedData.sortedBy { data ->
                (data[sliceLocationTag]!!.value as String).trim().toFloat()
            }
        }
        @Deprecated("Not deprecated, it's NOT IMPLEMENTED YET")
        private fun sortByImagePosition(unsortedData: List<TagToDataMap>): List<TagToDataMap> {
            val imagePositionTag = tagAsUInt("(0020,0032)") // It is 'DS' - decimal string
            val imageOrientationTag = tagAsUInt("(0020,0037)") // It's also 'DS'. example: 1\0\0.9612\-0.2756
            return unsortedData.sortedBy { data ->
                // Requires more processing
                (data[imagePositionTag]!!.value as String)
                // example imgPos: -110.2841796875\-248.63495760157\-163.23956032249
                (data[imageOrientationTag]!!.value as String)
                // example imgOrient: 1\0\0\0\0.96126169593832\-0.275637355817
                TODO("Not yet implemented")
            }
        }

        private fun getPixelData(imgInfo: TagToDataMap): ByteArray {
            val utag = tagAsUInt("(7FE0,0010)")
            if(!imgInfo.containsKey(utag)) {
                throw Exception("Tag $utag not found in TagToDataMap")
            }
            val itemList = (imgInfo[utag]!!.value as OBItemList)
            if(itemList.isEmpty()) {
                throw Exception("Item List for a tag (7FE0,0010) is empty")
            }
            if(itemList.size > 2)
                println("NOTE: Usually tag (7FE0,0010) has 2 items. This one has ${itemList.size}.")
            // The first Item in the Sequence of Items before the encoded Pixel Data Stream shall be a Basic Offset Table item.
            // Just take second one, not first with non-zero length
            if (itemList.size < 2 || itemList[1].len <= 0u) {
                throw Exception("Item List for a tag (7FE0,0010) does not contain non-empty ByteArray")
            }
            return itemList[1].value
        }
    } // companion end
}

fun sortImagesInDirectory(directory: String): List<TagToDataMap> {
    return ImageSorter(directory).sortedData
}