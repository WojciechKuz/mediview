package transform3d

import dicom.DicomDataElement
import dicom.OBItemList
import dicom.OWItemList
import dicom.TagToDataMap
import dicom.adaptGreyInfo
import dicom.jpegToByteArray
import dicom.tagAsUInt
import dicom.uIntAsTag
import org.jetbrains.skia.ImageInfo
import kotlin.collections.component1
import kotlin.collections.component2

/** Only dataMapToImageData() is important here */
object InterpretData {
    val pixelDataTag = tagAsUInt("[7FE0 0010]")
    val transferSyntaxUIDTag = tagAsUInt("[0002 0010]")

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

    val columnsTag = tagAsUInt("(0028,0011)") // x, width
    val rowsTag    = tagAsUInt("(0028,0010)") // y, height

    fun getSkiaImageInfo(dataMap: TagToDataMap): ImageInfo {
        val width = dataMap[columnsTag]?: throw tagNotFoundErr(columnsTag)
        val height = dataMap[rowsTag]?: throw tagNotFoundErr(rowsTag)
        val bitsAlloc = dataMap[tagAsUInt("(0028,0100)")]?: throw tagNotFoundErr("(0028,0100)")

        return adaptGreyInfo(
            width.value as UInt,
            height.value as UInt,
            bitsAlloc.value as UInt
        )
    }

    /** remove pixel data (7FE0,0010) from data map. Leave only descriptive elements. */
    private fun removeImageData(dataMap: TagToDataMap): TagToDataMap
        = dataMap.filter { (key, value) -> key != tagAsUInt("(7FE0,0010)") }

    /** for imageData and transfer syntax returns decoded (if needed) to ByteArray Image */
    private fun getRawImage(imageData: DicomDataElement<out Any>, trStxData: DicomDataElement<out Any>): ByteArray {
        if(imageData.tag != pixelDataTag) {
            throwWrongTag(pixelDataTag,imageData.tag)
        }
        val imageBytes = when (imageData.value) {
            is OWItemList -> imageData.value.get()
            is OBItemList -> imageData.value.get()
            is ByteArray -> imageData.value
            else -> imageData.value as ByteArray
        }
        val rawImage = when(transferSyntaxUID(trStxData)) {
            ImageType.JPEG -> {
                println("Decode from JPEG")
                jpegToByteArray(imageBytes as ByteArray)
            }
            else -> {
                println("No need to decode")
                imageData.value as ByteArray
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

    /** dataMap (with image as one of tags) ---> ImageData structure, which stores image and rest of the data separately.
     * Also, new structure stores image as 2DShortArray. Rest of the data is still stored in TagToDataMap. */
    fun dataMapToImageData(dataMap: TagToDataMap): ImageAndData<ShortArray> {
        val pxDat = dataMap[pixelDataTag]?: throw tagNotFoundErr(pixelDataTag)
        val trxDat = dataMap[transferSyntaxUIDTag]?: throw tagNotFoundErr(transferSyntaxUIDTag)
        val wthDat = dataMap[columnsTag]?: throw tagNotFoundErr(columnsTag)
        val hthDat = dataMap[rowsTag]?: throw tagNotFoundErr(rowsTag)

        val imageBytes = getRawImage(pxDat, trxDat)
        val removedImageDataMap = removeImageData(dataMap)
        val bytesConfig = getBytesConfig(removedImageDataMap)

        return ImageAndData<ShortArray>(
            removedImageDataMap,
            pixelsToShort(imageBytes, bytesConfig)
        )
    }

    fun image1Dto2D(array1D: ShortArray, width: UInt, height: UInt) = image1Dto2D(array1D.toTypedArray(), width.toInt(), height.toInt())

    fun image1Dto2D(array1D: Array<Short>, width: Int, height: Int): Short2D {
        val idx = { x: Int, y: Int -> y * width + x }
        return Array<Array<Short>>(height) { y ->
            Array<Short>(width) { x ->
                array1D[idx(x, y)]
            }
        }
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

        ""
    ).map { it -> tagAsUInt(it) }
}
typealias Short2D = Array<Array<Short>>