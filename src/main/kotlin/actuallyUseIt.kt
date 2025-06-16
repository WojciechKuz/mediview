
import dicom.TagToDataMap
import dicom.filestructure.DataRead
import dicom.tagAsUInt
import transform3d.ArrayOps
import transform3d.Config
import transform3d.ImageAndData
import transform3d.Interpolation
import transform3d.InterpolationSA
import transform3d.InterpretData
import transform3d.InterpretData.columnsTag
import transform3d.InterpretData.rowsTag
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

/** Open DICOM file (files in same directory) ---> dicomDataMap + Array3D */
fun loadDicomData(): ImageAndData<ArrayOps> {

    // 1. choose directory
    val selectedTags = InterpretData.necessaryInfo
    val dirName = directory() // should contain only ONE set of images

    // new way - ez to parallelize:
    val instanceNumTag = tagAsUInt("(0020,0013)") // It is 'IS' - integer string. Copied from ImageSorter to make it faster

    // Interpolation will make other image's data useless. Keep only one. The image unique data are used only for ordering of them.
    var oneDataMap: TagToDataMap? = null


    // Merged steps 1-5. directory -> sorted list of images (as ShortArray)
    val imageList = ReadHelp.listFilesInDir(dirName).map { fileName ->

        // 1. pick file, get cursor
        val cursor = ReadHelp.cursorAtDataSet(dirName + fileName) // string -> cursor

        // 2. get data map
        val data = DataRead().getFullDataMap(cursor) // cursor -> TagToDataMap

        // 3.(was 4) filter data here if necessary. All should have same series number
        val filteredData = data

        // 4.(was 3) order images (add key to know later how to sort)
        val sortPair = (filteredData[instanceNumTag]!!.value as String).trim().toInt() to filteredData // [whichImg] = data

        // 5. image data as short array, keep only one copy of non-image data
        if(sortPair.first == 1) { // first image is actually 1
            oneDataMap = sortPair.second
        }
        val imagePair = sortPair.first to InterpretData.dataMapToImage(sortPair.second)
        imagePair
    }.sortedBy { it.first }.map { it.second }


    if(oneDataMap == null) {
        //println("ERR: No data found. (TagToDataMap is null)")
        throw Exception("No data found. (TagToDataMap is null)")
    }

    /* // old way:
    val cursors = ReadHelp.listFilesInDir(dirName).map { fileName -> ReadHelp.cursorAtDataSet(dirName + fileName) }

    // 2. get data map
    val imagesDataMap = cursors.map { cursor ->
        //DataRead().getPartialDataMap(cursor, selectedTags)
        DataRead().getFullDataMap(cursor)
    }

    // 3. sort images
    val sortedImagesData = ImageSorter.sortByInstanceNumber(imagesDataMap)
    //val sortedImagesData = sortBySliceLocation(imagesDataMap)
    //val sortedImagesData = sortByImagePosition(imagesDataMap)
    if(sortedImagesData.isEmpty()) throw Exception("Error: After sorting, the array is empty!")

    // 4. filter images.
    ///val filteredData = filter(sortedImagesData)
    // val data = filteredData
    val data = sortedImagesData

    // 5. take images out of dataset, store separately
    val imgAndDataList = data.map { dataMap -> InterpretData.dataMapToImageData(dataMap) }

    // Interpolation will make other image's data useless. Keep only one. The image unique data are used only for ordering of them.
    //val oneDataMap = imgAndDataList[0].dataMap
    */

    val wthDat = oneDataMap[columnsTag]?: throw tagNotFoundErr(columnsTag)
    val hthDat = oneDataMap[rowsTag]?: throw tagNotFoundErr(rowsTag)

    val slThk = oneDataMap[tagAsUInt("[0018 0050]")]?: throw tagNotFoundErr("[0018 0050]")
    val scaleZ = if( Config.interpolateByDicomValue ) InterpretData.interpretZScaleFactor(slThk) else {
        println("backup scaleZ")
        (wthDat.value as UInt).toDouble() / imageList.size
    }

    // Build 3D Array
    val array3D = ArrayOps.Array3DBuilder().addAllSA(imageList).create((wthDat.value as UInt).toInt(), (hthDat.value as UInt).toInt())

    // 6. interpolate z axis
    array3D.interpolateOverZ(
        scaleZ, // computed from pixel size/spacing whatever
        InterpolationSA::rescaleBL
    )
    //println("selectedPixel is " + array3D.isSelectedPixelTheSame().toString() + " step 6")

    val whd = array3D.whd

    // ❌ at this point we have 512x512x512 array ❌ Not true. rescaled with 1 / sliceThickness.

    // 7. shear by gantry angle
    val gantryTag = tagAsUInt("[0018 1120]")
    val gantryDat = oneDataMap[gantryTag]?: throw tagNotFoundErr(gantryTag)
    val gantryAngle = InterpretData.interpretGantryAkaDetectorTilt(gantryDat)
    array3D.shearByGantry(gantryAngle, whd.width,
        InterpolationSA::moveBL, whd.height/2)
    //println("selectedPixel is " + array3D.isSelectedPixelTheSame().toString() + " step 7")

    // 8. rescale hounsfield
    // get tag rescale slope, rescale intercept
    val rescItDat = oneDataMap[tagAsUInt("[0028 1052]")]?: throw tagNotFoundErr(tagAsUInt("[0028 1052]"))
    val rescSlDat = oneDataMap[tagAsUInt("[0028 1053]")]?: throw tagNotFoundErr(tagAsUInt("[0028 1053]"))
    val rescaleFunction = InterpretData.interpretRescale(rescItDat, rescSlDat)
    array3D.transformEachPixel(rescaleFunction)
    //println("selectedPixel is " + array3D.isSelectedPixelTheSame().toString() + " step 8")

    // 9. convert to Multik's 3D array
    //val array = array3D.convertMultik(whd) // ???

    println("Processing images finished.")

    return ImageAndData<ArrayOps>(oneDataMap, array3D)
}