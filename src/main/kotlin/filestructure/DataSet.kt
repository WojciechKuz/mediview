package filestructure

import DataType
import DicomByteData
import DicomCursor
import DicomDataElement
import DicomTag
import filestructure.groups.*
import tagAsUInt

object DataSet: GroupBase() {
    /** Read all to map from UInt tag, to DicomByteData */
    fun createDataMap(cursor: DicomCursor, createUntil: (DicomTag) -> Boolean  = {false}): Map<UInt, DicomByteData> {
        return cursor.readAllTags(createUntil).associateBy { it.tag }
    }
    fun dataMapUntilPixelData(cursor: DicomCursor): Map<UInt, DicomByteData> = createDataMap(cursor) { tag -> tag.tag == tagAsUInt("(7FE0,0010)") }

    /**
     * @param imgInfo Map of tag to data containing information about image */
    fun readImageData(cursor: DicomCursor, imgInfo: Map<UInt, DicomByteData>) = ImageReader.skipImageData(cursor, imgInfo)

    /** Decode a tag given in format `Name (ffff,ffff)` to a pair. Tag name as value, tag number as key. */
    fun tagToPair(str: String, vr: String = "UN"): Pair<UInt, DataType> {
        val splitted = str.split('(', ',', ')')
        if(splitted.size < 4) {
            println("ERR, for \"$str\" split resulted in $splitted")
        }
        val dtag = DataType("(${splitted[1]},${splitted[2]})", (splitted[0] + splitted[3]).trim(), vr)
        return Pair(
            dtag.tag,
            dtag
        )
    }

    val tagNames: Map<UInt, DataType> = listOf(
        "Instance Number (0020,0013)" * "IS", // image number. unique in directory
        "Pixel Data Element (7FE0,0010)" * "OB",
        +"Last Data element (FFFC,FFFC)",
        "(2020,0110) Basic Grayscale Image Sequence" * "SQ", // Basic Grayscale Image Sequence SQ 1 M/M
        "(2020,0010) Image Position" * "US",
        "(2020,0020) Polarity" * "CS",
    ).associateBy { it.tag } + PixelGroup.pixelDataTagNames + StudyGroup.dataTagNames + DeviceGroup.deviceDataTagNames
}

typealias TagToDataMap = Map<UInt, DicomDataElement<out Any>>