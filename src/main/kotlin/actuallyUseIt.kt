
import dicom.TagToDataMap
import dicom.filestructure.DataRead
import dicom.tagAsUInt
import transform3d.ArrayOps
import transform3d.ImageAndData
import transform3d.InterpolationSA
import transform3d.InterpretData
import transform3d.InterpretData.columnsTag
import transform3d.InterpretData.rowsTag
import transform3d.tagNotFoundErr
import kotlinx.coroutines.*

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
@Suppress("USELESS_CAST")
suspend fun loadDicomData(directory: String, fileName: String = ""): ImageAndData<ArrayOps> = coroutineScope {

    // 1. choose directory
    val selectedTags = InterpretData.necessaryInfo
    val dirName = directory // should contain only ONE set of images

    // new way - ez to parallelize:
    val instanceNumTag = tagAsUInt("(0020,0013)") // It is 'IS' - integer string. Copied from transform3d.ImageSorter to make it faster
    val imagePosTag = tagAsUInt("(0020,0032)")

    // Interpolation will make other image's data useless. Keep only one. The image unique data are used only for ordering of them.
    var oneDataMap: TagToDataMap? = null
    val notSetVal = -11000.0
    var sliceZPos1: Double = notSetVal; var sliceZPos2: Double = notSetVal

    // Merged steps 1-5. directory -> sorted list of images (as ShortArray)
    suspend fun mergedSteps1to5(fileName: String): Pair<Double, ShortArray> = withContext(Dispatchers.Default) {

        // 1. pick file, get cursor
        val cursor = ReadHelp.cursorAtDataSet(dirName + fileName) // string -> cursor

        // 2. get data map
        val data = DataRead().getFullDataMap(cursor) // cursor -> TagToDataMap

        // 3. filter data here if necessary. All should have same series number
        val filteredData = data

        // 4. order images (add key to know later how to sort)
        //val sortPair = (filteredData[instanceNumTag]!!.value as String).trim().toInt() to filteredData // [whichImg] = data
        val sortPair = InterpretData.getZPosition(filteredData[imagePosTag]!!) to filteredData // z
        val whichImg = (filteredData[instanceNumTag]!!.value as String).trim().toInt()

        // 5. image data as short array, keep only one copy of non-image data
        if(whichImg == 1) { // first image is actually 1
            oneDataMap = sortPair.second //InterpretData.removeImageData(sortPair.second)
            sliceZPos1 = sortPair.first
        }
        if(whichImg == 2) {
            sliceZPos2 = sortPair.first
        }
        val imagePair = sortPair.first to InterpretData.dataMapToImage(sortPair.second)

        // result:
        imagePair
    }

    // Execute steps 1-5 asynchronously
    val imageList = ReadHelp.listFilesInDir(dirName).map { fileName ->
        async {
            mergedSteps1to5(fileName)
        }
    }.awaitAll().sortedBy { it.first }.map { it.second }

    /*
    if (sliceZPos1 == notSetVal || sliceZPos2 == notSetVal) {
        throw Exception("Slice positions were not set")
    }
    println("Slice positions 1:${sliceZPos1} 2:$sliceZPos2, distance: ${sliceZPos2 - sliceZPos1}")
    */

    if(oneDataMap == null) {
        throw Exception("No data found. (TagToDataMap is null)")
    }

    val wthDat = oneDataMap[columnsTag]?: throw tagNotFoundErr(columnsTag)
    val hthDat = oneDataMap[rowsTag]?: throw tagNotFoundErr(rowsTag)


    // Build 3D Array
    val array3D = ArrayOps.Array3DBuilder().addAllSA(imageList).create((wthDat.value as UInt).toInt(), (hthDat.value as UInt).toInt())

    // 6. prepare interpolate z axis
    val slThk = oneDataMap[tagAsUInt("[0018 0050]")]?: throw tagNotFoundErr("[0018 0050]")
    val pxSpacing = oneDataMap[tagAsUInt("[0028 0030]")]?: throw tagNotFoundErr("[0028 0030]")
    val scaleZ = when( Config.interpolateBy ) {
        Config.InterpolateBy.To512 -> (wthDat.value as UInt).toDouble() / imageList.size // scale transforming always to 512
        Config.InterpolateBy.JustThickness -> InterpretData.getSliceThickness(slThk) // * 2.4 = 144
        Config.InterpolateBy.SliceThickn -> InterpretData.interpretZScaleFactorBySliceWidth(slThk, pxSpacing) // 2.4/0.43 = 5.58
        Config.InterpolateBy.SliceDist -> InterpretData.interpretZScaleFactor(sliceZPos1, sliceZPos2, pxSpacing) // 2.423/0.43 = 5.63
    }
    val scaleLambda = array3D.prepareLambdaForScaleZ(scaleZ, InterpolationSA::rescaleBL)

    // 7. prepare fill. Fill if less than 512 deep
    val minValTag = oneDataMap[tagAsUInt("[0028 0106]")]?: throw tagNotFoundErr(tagAsUInt("[0028 0106]")) // -> Smallest Image Pixel Value (MIN VAL)
    val maxValTag = oneDataMap[tagAsUInt("[0028 0107]")]?: throw tagNotFoundErr(tagAsUInt("[0028 0107]")) // -> Largest Image Pixel Value  (MAX VAL)
    val dcmMin = minValTag.value as UInt
    val dcmMax = maxValTag.value as UInt
    // get tag rescale slope, rescale intercept
    val rescItDat = oneDataMap[tagAsUInt("[0028 1052]")]?: throw tagNotFoundErr(tagAsUInt("[0028 1052]"))
    val rescSlDat = oneDataMap[tagAsUInt("[0028 1053]")]?: throw tagNotFoundErr(tagAsUInt("[0028 1053]"))
    val rescaleFunction = InterpretData.interpretRescale(rescItDat, rescSlDat)
    val minRescaled = rescaleFunction(dcmMin.toShort())

    fun fillingFunction(shArr: ShortArray, absI: Int) = InterpolationSA.fillTo(
        shArr,
        (wthDat.value as UInt).toInt(),
        minRescaled
    )
    fun notFillingFunction(shArr: ShortArray, absI: Int) = shArr
    val fillLambda: (ShortArray, Int) -> ShortArray =
        if(Config.fillDepthToWidthSize) { ::fillingFunction } else { ::notFillingFunction }

    // 8. prepare shear by gantry angle
    val gantryTag = tagAsUInt("[0018 1120]")
    val gantryDat = oneDataMap[gantryTag]?: throw tagNotFoundErr(gantryTag)
    val gantryAngle = InterpretData.interpretGantryAkaDetectorTilt(gantryDat)
    val gantryLambda = array3D.prepareLambdaForShearByGantry(gantryAngle,
        InterpolationSA::moveBL, array3D.size.height/2)

    //val absMin = array3D.array.min()
    //val absMax = array3D.array.max()
    //println("min $absMin, max $absMax in array before value rescale")

    // Execute combined 6, 7 and 8 lambdas
    array3D.doSomethingOnYXArrayOfZArrays(
        //(array3D.size.depth * scaleZ).toInt()
        (wthDat.value as UInt).toInt() // because filling function
    ) { shArr, absIdx ->
        fillLambda ( gantryLambda ( scaleLambda (shArr, absIdx), absIdx), absIdx)
    }

    // 9. rescale Hounsfield
    array3D.transformEachPixel(rescaleFunction)
    //array3D.transformEachPixel { sh -> (sh * 4).toShort() }
    //println("min value $dcmMin, max value $dcmMax in dicom")
    //println("values will be rescaled to min ${rescaleFunction(absMin)} max ${rescaleFunction(absMax)}")

    println("Processing images finished.")

    // result:
    ImageAndData<ArrayOps>(oneDataMap, array3D)
}