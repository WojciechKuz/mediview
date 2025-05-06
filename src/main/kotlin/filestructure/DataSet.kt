package filestructure

import DataType
import DicomByteData
import DicomCursor
import DicomDataElement
import DicomTag
import Reading
import determineDataType
import filestructure.groups.*
import tagAsUInt

class DataSet(): GroupBase() {

//    private var currentlyReading = Reading.Global
//    private var sequenceUpTo = UInt.MAX_VALUE

    /** While condition. Has side effect of writing to a map
     * @return false if stopped because of user's condition, or reached end.
     * True if stopped because of internal stop condition */
    private fun whyStopped(cursor: DicomCursor, createUntil: (DicomTag) -> Boolean  = {false}, dataMap :MutableMap<UInt, DicomByteData>): Boolean {
        /*if(cursor.cursor >= sequenceUpTo) { // reset status
            currentlyReading = Reading.Global
            sequenceUpTo = UInt.MAX_VALUE
            println("No longer sequence")
        }*/
        val cursorPosition = cursor.cursor
        if(DicomTag.canReadTag(cursor) == 0u) { // reached end
            return false
        }
        val stopTag = cursor.readNextTag()
        if(createUntil(stopTag)) {    // stopped because of user's condition
            cursor.restoreCursorPosition(cursorPosition)
            return false
        }
        if( !stopTag.canReadValue(cursor) ) { // reached end
            cursor.restoreCursorPosition(cursorPosition)
            return false
        }
        when(stopTag.vr) {
            "OB" -> {
                if(stopTag.isLengthDefined()) {
                    if(stopTag.canReadValue(cursor)) {
                        dataMap[stopTag.tag] = DicomByteData(stopTag, cursor.readNextByteField(stopTag.len))
                    }
                    // just read value
                }
                else {
                    //dataMap[stopTag.tag] = DicomByteData(stopTag, byteArrayOf())
                    //currentlyReading = Reading.Ob
                    // when length is undefined, continue reading just after tag.
                    val alteredStopTagData = determineOBLength(cursor, stopTag)
                    dataMap[alteredStopTagData.tag] = alteredStopTagData
                }
            }
            "SQ" -> {
                if( stopTag.canReadValue(cursor) ) {
                    //dataMap += readSequence(cursor, createUntil)
                    //dataMap[stopTag.tag] = DicomByteData(stopTag, byteArrayOf())
                    //currentlyReading = Reading.Sequence
                    //sequenceUpTo = cursor.cursor + stopTag.len
                    // do nothing, continue reading just after tag.
                    dataMap[stopTag.tag] = DicomByteData(stopTag, cursor.readNextByteField(stopTag.len))
                }
            }
            "  " -> {
                /*when( stopTag.tag ) {
                    tagAsUInt("(FFFE,E000)") -> {
                        if(currentlyReading == Reading.Ob) {
                            dataMap[stopTag.tag] = DicomByteData(stopTag, cursor.readNextByteField(stopTag.len))
                            // in OB, nested items have length and content
                        }
                        if(currentlyReading == Reading.Sequence) {
                            dataMap[stopTag.tag] = DicomByteData(stopTag, byteArrayOf())
                            // in sequence, nested items have length, but content is in separate tags
                        }
                    }
                    tagAsUInt("(FFFE,E00D)") -> {
                        dataMap[stopTag.tag] = DicomByteData(stopTag, byteArrayOf())
                        // do nothing, continue reading just after tag.
                    }
                    tagAsUInt("(FFFE,E0DD)") -> {
                        dataMap[stopTag.tag] = DicomByteData(stopTag, byteArrayOf())
                        currentlyReading = Reading.Global
                        // do nothing, continue reading just after tag.
                    }
                }*/
                println("Warning! Control tags in global context.")
                println("They are not unique, not suitable for a Map.")
                // do nothing, continue reading just after tag.
            }
        }
        return true
    }

//    fun readSequence(cursor: DicomCursor, seqTag: DicomTag, createUntil: (DicomTag) -> Boolean  = {false}): Map<UInt, DicomByteData> {
//        //
//    }

    /** Call for OB of undefined length. Will determine the length and set ByteArray */
    private fun determineOBLength(cursor: DicomCursor, obTag: DicomTag, createUntil: (DicomTag) -> Boolean  = {false}): DicomByteData {
        // tag already read, now read value
        if(DicomTag.canReadTag(cursor) != 0u) {
            val prevCursor = cursor.cursor

            var obLength = 0u
            var canRead = DicomTag.canReadTag(cursor) // It's also length of tag
            while(canRead != 0u) {
                val tag = DicomTag.readTag(cursor)
                obLength += canRead
                if(createUntil(tag)) {
                    break
                }
                if(tag.tag == tagAsUInt("(FFFE,E0DD)")) { // end of OB
                    break
                }
                if(tag.canReadValue(cursor)) {
                    cursor.moveBy(tag.len)
                    obLength += tag.len
                }
                else {
                    println("ERR, unexpected end of OB. Tag with no value.")
                    break;
                }
                canRead = DicomTag.canReadTag(cursor)
            } // while end

            cursor.restoreCursorPosition(prevCursor)
            return DicomByteData(
                obTag.tagPt1, obTag.tagPt2, obTag.vr, obLength, cursor.readNextByteField(obLength)
            )
        }
        else {
            throw Exception("ERR, tag $obTag, has undefined length, and it's not possible to read content to determine length!")
        }
    }

    /** Stop reading all tags, because some tag might need special processing */
    private val internalStopCondition: (DicomTag) -> Boolean = {  it.vr == "SQ" || it.vr == "  " || it.vr == "OB" }

    /** Read all to a map from UInt tag, to DicomByteData */
    fun createDataMap(cursor: DicomCursor, createUntil: (DicomTag) -> Boolean  = {false}): Map<UInt, DicomByteData> {
        val dataMapPart: MutableMap<UInt, DicomByteData> = mutableMapOf()

        do {
            dataMapPart += cursor.readAllTags { createUntil(it) || internalStopCondition(it) }.associateBy { it.tag }
        }
        while (whyStopped(cursor, createUntil, dataMapPart))

        return dataMapPart.toMap()
    }

    fun dataMapUntilPixelData(cursor: DicomCursor): Map<UInt, DicomByteData> = createDataMap(cursor) { tag -> tag.tag == tagAsUInt("(7FE0,0010)") }

    fun someFunctionName(cursor: DicomCursor): TagToDataMap {
        val dataMap2 = DataSet().createDataMap(cursor) { tag ->
            !tag.isLengthDefined() // stop reading when not defined length
        }.mapValues { (_, v) -> determineDataType(v) }  // TODO determine nested types, add nested elements to map
        return dataMap2
    }

    /**
     * @param imgInfo Map of tag to data containing information about image */
    fun readImageData(cursor: DicomCursor, imgInfo: Map<UInt, DicomByteData>) = ImageReader.skipImageData(cursor, imgInfo)

    //companion object {
        val tagNames: Map<UInt, DataType> = listOf(
            "Instance Number (0020,0013)" * "IS", // image number. unique in directory
            "Pixel Data Element (7FE0,0010)" * "OB",
            +"Last Data element (FFFC,FFFC)",
            "(2020,0110) Basic Grayscale Image Sequence" * "SQ", // Basic Grayscale Image Sequence SQ 1 M/M
            "(2020,0010) Image Position" * "US",
            "(2020,0020) Polarity" * "CS",
            "(FFFE,E000) Seq Item" * "  ",
            "(FFFE,E00D) Seq Item Delimiter" * "  ",
            "(FFFE,E0DD) Seq Delimiter" * "  ",
        ).associateBy { it.tag } + PixelGroup.pixelDataTagNames + StudyGroup.dataTagNames + DeviceGroup.deviceDataTagNames
    //}
}

typealias TagToDataMap = Map<UInt, DicomDataElement<out Any>>