
import dicom.TagToDataMap
import dicom.filestructure.DataRead
import dicom.tagAsUInt
import transform3d.ArrayBuilder
import transform3d.Interpolation
import transform3d.InterpretData
import transform3d.InterpretData.columnsTag
import transform3d.InterpretData.rowsTag
import transform3d.WidthHeightDepth
import transform3d.tagNotFoundErr

private fun directory() = ReadHelp.pickDirAndDicom().first

private fun filter(sortedImagesData: List<TagToDataMap>): List<TagToDataMap> {
    val seriesOf1st = sortedImagesData[0][tagAsUInt("[0020 0011]")]?: throw tagNotFoundErr("[0020 0011]") // series number
    val filteredData = sortedImagesData.filter { datMap ->
        val elem = datMap[tagAsUInt("[0020 0011]")]?: throw tagNotFoundErr("[0020 0011]") // series number
        (elem.value as UInt) == (seriesOf1st.value as UInt)
    }
    if(filteredData.isEmpty()) {
        return listOf()
    }
    return filteredData
}

fun useIt() {

    // 1. choose directory
    val selectedTags = InterpretData.necessaryInfo
    val dirName = directory() // should contain only ONE set of images
    val cursors = ReadHelp.listFilesInDir(dirName).map { fileName -> ReadHelp.cursorAtDataSet(dirName + fileName) }

    // 2. get data map
    val imagesDataMap = cursors.map { cursor ->
        //DataRead().getPartialDataMap(cursor, selectedTags)
        DataRead().getFullDataMap(cursor)
    }

    // 3. sort images
    val sortedImagesData = ImageSorter.sortByInstanceNumber(imagesDataMap)
    //val sortedImagesData = sortBySliceLocation(imagesDataMap)
    //val sortedImagesData = sortByImagePosition(imagesDataMap) // TODO sort by image position
    if(sortedImagesData.isEmpty()) return

    // 4. filter images. All should have same series number
    // val filteredData = filter(sortedImagesData)
    // val data = filteredData
    val data = sortedImagesData

    // 5. take images out of dataset, store separately
    val imgAndDataList = data.map { dataMap -> InterpretData.dataMapToImageData(dataMap) }

    // Interpolation will make other image's data useless. Keep only one. The image unique data are used only for ordering of them.
    val oneDataMap = imgAndDataList[0].dataMap

    // 6. interpolate z axis
    val slThk = oneDataMap[tagAsUInt("[0018 0050]")]?: throw tagNotFoundErr("[0018 0050]")
    val scaleZ = InterpretData.interpretZScaleFactor(slThk)
    val arrayBuilder = ArrayBuilder().addAll(imgAndDataList).interpolateOverZ(
        scaleZ, // computed from pixel size/spacing whatever
        Interpolation::interpolateBL
    )

    val wthDat = oneDataMap[columnsTag]?: throw tagNotFoundErr(columnsTag)
    val hthDat = oneDataMap[rowsTag]?: throw tagNotFoundErr(rowsTag)

    // 7. convert to Multik's 3D array
    val whd = WidthHeightDepth.unsignedConstruct(
    wthDat.value as UInt,
        hthDat.value as UInt,
        arrayBuilder.getList().size.toUInt(),
    )

    // ❌ at this point we have 512x512x512 array ❌ Not true. rescaled with 1 / sliceThickness.

    // TODO rescale hounsfield
    // get tag rescale slope, rescale intercept
    // arrayBuilder.transformEachPixel {  }

    // TODO gantry
    // get tag gantry
    // arrayBuilder.transformEachPixel {  }

    val array = arrayBuilder.convert(whd)

    // TODO array to ImageBitmap
}