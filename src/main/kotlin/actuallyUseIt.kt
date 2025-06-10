
import dicom.filestructure.DataRead
import dicom.tagAsUInt
import transform3d.ArrayBuilder
import transform3d.Interpolation
import transform3d.InterpretData
import transform3d.InterpretData.columnsTag
import transform3d.InterpretData.rowsTag
import transform3d.tagNotFoundErr

private fun directory() = ReadHelp.pickDirAndDicom().first

fun useIt() {
    val selectedTags = InterpretData.necessaryInfo
    val dirName = directory() // should contain only ONE set of images

    val cursors = ReadHelp.listFilesInDir(dirName).map { fileName -> ReadHelp.cursorAtDataSet(dirName + fileName) }

    // get data map
    val imagesDataMap = cursors.map { cursor ->
        //DataRead().getPartialDataMap(cursor, selectedTags)
        DataRead().getFullDataMap(cursor)
    }

    // sort images
    val sortedImagesData = ImageSorter.sortByInstanceNumber(imagesDataMap)
    //val sortedImagesData = sortBySliceLocation(imagesDataMap)
    //val sortedImagesData = sortByImagePosition(imagesDataMap) // TODO sort by image position

    // filter images
    if(sortedImagesData.isEmpty()) return
    val seriesOf1st = sortedImagesData[0][tagAsUInt("[0020 0011]")]?: throw tagNotFoundErr("[0020 0011]") // series number
    val filteredData = sortedImagesData.filter { datMap ->
        val elem = datMap[tagAsUInt("[0020 0011]")]?: throw tagNotFoundErr("[0020 0011]") // series number
        (elem.value as UInt) == (seriesOf1st.value as UInt)
    }
    if(filteredData.isEmpty()) {
        return
    }
    val imageCount = filteredData.size


    val imgAndDataList = filteredData.map { dataMap -> InterpretData.dataMapToImageData(dataMap) }

    val wthDat = imgAndDataList[0].dataMap[columnsTag]?: throw tagNotFoundErr(columnsTag)
    val hthDat = imgAndDataList[0].dataMap[rowsTag]?: throw tagNotFoundErr(rowsTag)

    val array = ArrayBuilder().addAll(imgAndDataList).convertWithInterpolationU(
        wthDat.value as UInt,
        hthDat.value as UInt,
        imageCount.toUInt(),
        wthDat.value, // FIXME should be computed from pixel size/spacing whatever
        Interpolation::interpolateBL
    )
    // at this point we have 512x512x512 array

    // TODO rescale hounsfield

    // TODO array to ImageBitmap
}