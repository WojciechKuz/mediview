package transform3d

import dicom.DicomDataElement
import dicom.OBItemList
import dicom.OWItemList
import dicom.TagToDataMap
import dicom.jpegByteArrayToRawByteArray
import dicom.tagAsUInt
import kotlin.collections.component1
import kotlin.collections.component2

/** Only dataMapToImageData() is important here */
object InterpretData {

    var said = 0
    fun sayOnce(say: () -> Unit) {
        if(said == 0) {
            say()
            said++
        }
    }

    val pixelDataTag = tagAsUInt("[7FE0 0010]")
    val transferSyntaxUIDTag = tagAsUInt("[0002 0010]")
    val imagePosTag = tagAsUInt("(0020,0032)")

    private fun transferSyntaxUID(data: DicomDataElement<out Any>): ImageType {
        if(data.tag != transferSyntaxUIDTag) {
            throwWrongTag(transferSyntaxUIDTag,data.tag)
        }
        return when((data.value as String).trim()) {
            "1.2.840.10008.1.2.1" -> {
                // PS3.5 A.2: Explicit VR Little Endian Transfer Syntax
                ImageType.RAW_PIXELS
            }
            "1.2.840.10008.1.2.4.70" -> {
                // JPEG Lossless, Non-Hierarchical, First-Order Prediction(Process 14[Selection Value 1])
                ImageType.JPEG
            }
            else -> ImageType.UNKNOWN
        }
    }

    fun getZPosition(data: DicomDataElement<out Any>): Double {
        if(data.tag != imagePosTag) {
            throwWrongTag(imagePosTag,data.tag)
        }
        return (data.value as String).trim().split("\\")[2].trim().toDouble() // z
    }

    /** For pixelSize element and distance between slices, get by how much z axis should be rescaled. */
    fun interpretZScaleFactor(zPosition1: Double, zPosition2: Double, pxSpData: DicomDataElement<out Any>): Double {
        val zDist = zPosition2 - zPosition1
        val pxSpacing = (pxSpData.value as String).trim().split("\\")[0].trim().toDouble() // 0.43/0.43
        return zDist / pxSpacing // 2.423/0.43 = 5.63
    }

    /** For pixelSize element and slice thickness (instead of distance between slices), get by how much z axis should be rescaled. */
    fun interpretZScaleFactorBySliceWidth(sliceThickn: DicomDataElement<out Any>, pxSpData: DicomDataElement<out Any>): Double {
        return interpretZScaleFactor(getSliceThickness(sliceThickn), 0.0, pxSpData)
    }

    /** This just returns slice thickness */
    fun getSliceThickness(sliceThickn: DicomDataElement<out Any>): Double {
        val sliceTag = tagAsUInt("[0018 0050]") // ---> Slice Thickness
        if(sliceThickn.tag != sliceTag) {
            throwWrongTag(sliceTag,sliceThickn.tag)
        }
        val sliceThickness = (sliceThickn.value as String).trim().toDouble()
        return sliceThickness
    }

    val columnsTag = tagAsUInt("(0028,0011)") // x, width
    val rowsTag    = tagAsUInt("(0028,0010)") // y, height

    /** remove pixel data (7FE0,0010) from data map. Leave only descriptive elements. */
    fun removeImageData(dataMap: TagToDataMap): TagToDataMap
        = dataMap.filter { (key, value) -> key != tagAsUInt("(7FE0,0010)") }

    /** for imageData and transfer syntax returns decoded (if needed) to ByteArray Image */
    private fun getRawImage(imageData: DicomDataElement<out Any>, trStxData: DicomDataElement<out Any>): ByteArray {
        if(imageData.tag != pixelDataTag) {
            throwWrongTag(pixelDataTag,imageData.tag)
        }
        val imageBytes: ByteArray = when(imageData.value) {
            is OWItemList -> imageData.value.get().value
            is OBItemList -> imageData.value.get().value
            is ByteArray -> imageData.value
            else -> imageData.value as ByteArray
        }
        val rawImage = when(transferSyntaxUID(trStxData)) {
            ImageType.JPEG -> {

                val jpegba = jpegByteArrayToRawByteArray(imageBytes)
                //sayOnce { println("Got ${jpegba?.size} bytes of JPEG") } // printed multiple times due to concurrency
                jpegba
            }
            else -> {
                //sayOnce { println("No need to decode") }
                imageBytes
            }
        }
        if (rawImage == null) {
            throw Exception("Obtaining raw image did not succeed")
        }
        return rawImage
    }

    private fun getBytesConfig(dataMap: TagToDataMap): BytesConfig {
        // this verification of obvious drives me crazy!!!
        val tgBA = dataMap[tagAsUInt("(0028,0100)")]?: throw tagNotFoundErr("(0028,0100)") //"Bits Allocated"
        val tgBS = dataMap[tagAsUInt("(0028,0101)")]?: throw tagNotFoundErr("(0028,0101)") //"Bits Stored"
        val tgHB = dataMap[tagAsUInt("(0028,0102)")]?: throw tagNotFoundErr("(0028,0102)") //"High Bit",
        val tgDEPR = dataMap[tagAsUInt("[0028 0103]")]?: throw tagNotFoundErr("[0028 0103]") // -> Data Element Pixel Representation (SIGN -)
        val signed = DAPixelRepresentation(tgDEPR)

        return BytesConfig.constructor(
            tgBA.value as UInt,
            tgBS.value as UInt,
            tgHB.value as UInt,
            signed
        )
    }

    /** dataMap (with image as one of tags) ---> image as ShortArray.
     * Discards other data! */
    fun dataMapToImage(dataMap: TagToDataMap): ShortArray {
        val pxDat = dataMap[pixelDataTag]?: throw tagNotFoundErr(pixelDataTag)
        val trxDat = dataMap[transferSyntaxUIDTag]?: throw tagNotFoundErr(transferSyntaxUIDTag)
        //val wthDat = dataMap[columnsTag]?: throw tagNotFoundErr(columnsTag)
        //val hthDat = dataMap[rowsTag]?: throw tagNotFoundErr(rowsTag)

        val imageBytes = getRawImage(pxDat, trxDat)
        val bytesConfig = getBytesConfig(dataMap)

        return pixelsToShort(imageBytes, bytesConfig)
    }

    enum class ImageType { JPEG, RAW_PIXELS, UNKNOWN }

    /** If pixels are signed (true, -) or not (false, +) */
    private fun DAPixelRepresentation(data: DicomDataElement<out Any>): Boolean {
        val dapxrep = tagAsUInt("[0028 0103]")
        if (data.tag != dapxrep) {
            throwWrongTag(dapxrep, data.tag)
        }
        return (data.value as UInt) == 1u
    }

    // rows, columns; distBtwImages( sliceThickness, distanceBetweenSlices, imagePosition );
    // px ->{Hounsfield scale}-> vx,
    // pxValRange( windowCenter, windowWidth)???

    val necessaryInfo = listOf<String>(
        "[7FE0 0010]", // pixel data
        "(0028,0010)", // rows
        "(0028,0011)", // columns
        "(0028,1050)", // window center ?
        "(0028,1051)", // window width ?
        "(0028,0100)", //"Bits Allocated"
        "(0028,0101)", //"Bits Stored"
        "(0028,0102)", //"High Bit",
        "[0018 0050]", // ---> Slice Thickness
        "[0018 1120]", // ---> Gantry/Detector Tilt (GANTRY)
        "[0020 1041]", // ---> Slice location
        // distanceBetweenSlices ???
        "[0020 0032]", // ---> Image Position
        "[0028 0103]", // -> Data Element Pixel Representation (SIGN -)
        "[0028 0106]", // -> Smallest Image Pixel Value (MIN VAL)
        "[0028 0107]", // -> Largest Image Pixel Value  (MAX VAL)
        "[0028 1052]", // -> "Rescale Intercept"
        "[0028 1053]", // -> "Rescale Slope"
        "[0020 0011]", // -> "Series Number"
        "[0020 0013]", // -> "Instance Number"
    ).map { it -> tagAsUInt(it) }



    fun interpretRescale(resIntercept: DicomDataElement<out Any>, resSlope: DicomDataElement<out Any>): (Short) -> Short {
        val resIcTag = tagAsUInt("[0028 1052]")
        val resSlTag = tagAsUInt("[0028 1053]")
        if(resIcTag != resIntercept.tag) { throwWrongTag(resIcTag, resIntercept.tag) }
        if(resSlTag != resSlope.tag) { throwWrongTag(resSlTag, resSlope.tag) }

        // ok, now value can be read
        val intercept = (resIntercept.value as String).trim().toDouble().toInt()
        val slope = (resSlope.value as String).trim().toDouble().toInt()

        return { v -> (v * slope + intercept).toShort() } // return as lambda { v -> v * slope + intercept }
    }

    /** @return gantry angle in degrees */
    fun interpretGantryAkaDetectorTilt(gantryTilt: DicomDataElement<out Any>): Double {

        val gantryTag = tagAsUInt("[0018 1120]")
        if(gantryTag != gantryTilt.tag) { throwWrongTag(gantryTag, gantryTilt.tag) }

        // ok, now value can be read
        val gantry = Config.gantryDirection * (gantryTilt.value as String).trim().toDouble() // in degrees
        return gantry
    }


}
typealias Short2D = Array<Array<Short>>