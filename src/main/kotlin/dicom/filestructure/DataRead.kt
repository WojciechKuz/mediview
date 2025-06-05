package dicom.filestructure

import dicom.DicomByteData
import dicom.DicomCursor
import dicom.DicomDataElement
import dicom.DicomTag
import dicom.OBItemList
import dicom.SQItemList
import dicom.TagToDataMap
import dicom.determineDataType
import dicom.tagAsUInt

/** High level dicom reading functions.
 * @param ignoreWarnings When reading tag with vr of `"  "` don't print any warning. */
class DataRead(private val ignoreWarnings: Boolean = false) {

    /** Get Full Data Map. Control tags like (FFFE,E00D) are ignored */
    fun getFullDataMap(cursor: DicomCursor): TagToDataMap {
        return createDataMap(cursor).mapValues { (_, v) -> determineDataType(v) }
    }

    fun getPartialDataMap(cursor: DicomCursor, selectedTags: List<UInt>): TagToDataMap {
        return mapOfCertainTags(cursor, selectedTags).mapValues { (_, v) -> determineDataType(v) }
    }

    /** While condition. Has side effect of writing to a map
     * @return false if stopped because of user's condition, or reached end.
     * True if stopped because of internal stop condition */
    private fun whyStopped(cursor: DicomCursor, createUntil: (DicomTag) -> Boolean  = {false}, dataMap: MutableMap<UInt, DicomByteData>): Boolean {
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

        specialTreatment(stopTag, cursor, dataMap)

        return true
    }

    /** Tags with VR's that require special processing */
    private val specialVRs: List<String> = listOf("SQ", "OB", "  ")

    private val specialTags: List<UInt> = listOf("(7FE0,0010)").map { tagAsUInt(it) }

    /** Decide what to do with some tags individually.
     * If tag needs to be added to a Map, it needs to be done manually here. */
    private fun specialTreatment(stopTag: DicomTag, cursor: DicomCursor, dataMap: MutableMap<UInt, DicomByteData>) {
        when(stopTag.vr) {
            "OB" -> {
                if(stopTag.isLengthDefined()) {
                    if(stopTag.canReadValue(cursor)) {
                        dataMap[stopTag.tag] = DicomByteData(stopTag, cursor.readNextByteField(stopTag.len))
                    }
                    // just read value
                }
                else {
                    val alteredStopTagData = determineOBLength(cursor, stopTag)
                    dataMap[alteredStopTagData.tag] = alteredStopTagData
                }
            }
            "SQ" -> {
                if( stopTag.canReadValue(cursor) ) {
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
                if(!ignoreWarnings) {
                    println("Warning! Control tags in global context.")
                    println("They are not unique, not suitable for a Map.")
                }
                // do nothing, continue reading just after tag. Don't add this tag to a Map
            }
        }
    }

    /** Call for OB of undefined length. Will determine the length and set ByteArray */
    fun determineOBLength(cursor: DicomCursor, obTag: DicomTag, createUntil: (DicomTag) -> Boolean  = {false}): DicomByteData {
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
    private val internalStopCondition: (DicomTag) -> Boolean = {  it.vr in specialVRs || it.tag in specialTags }

    /** Read all to a map from UInt tag, to DicomByteData */
    private fun createDataMap(cursor: DicomCursor, createUntil: (DicomTag) -> Boolean  = {false}): Map<UInt, DicomByteData> {
        val dataMapPart: MutableMap<UInt, DicomByteData> = mutableMapOf()

        do {
            dataMapPart += cursor.readAllTags { createUntil(it) || internalStopCondition(it) }.associateBy { it.tag }
        }
        while(whyStopped(cursor, createUntil, dataMapPart))

        return dataMapPart.toMap()
    }

    /** Do not create full data map, only map of selected tags.
     * @param tagsAreSorted if tags in list are ordered as in dicom file */
    private fun mapOfCertainTags(
        cursor: DicomCursor, selectedTags: List<UInt>, tagsAreSorted: Boolean = false
    ): Map<UInt, DicomByteData> {
        val dataMapPart: MutableMap<UInt, DicomByteData> = mutableMapOf()

        val beggining = cursor.cursor
        var maxCursor = cursor.cursor
        for(tag in selectedTags) {
            if(!tagsAreSorted) {
                cursor.restoreCursorPosition(beggining)
            }
            val atTag = cursor.findTag(tag)
            if (atTag.hasReachedEnd()) {
                println("Tag ${ReadHelp.strHex(tag, pad = 8)} was not found")
                //return dataMapPart.toMap()
                continue
            }
            val foundTag = atTag.readNextTag()
            if(!foundTag.canReadValue(atTag)) {
                println("Can't read the value at tag ${ReadHelp.strHex(tag, pad = 8)}")
                //return dataMapPart.toMap()
                continue
            }
            dataMapPart[foundTag.tag] = DicomByteData(foundTag, atTag.safeReadNextByteField(foundTag))
            if(maxCursor < cursor.cursor) {
                maxCursor = cursor.cursor
            }
        }
        cursor.restoreCursorPosition(maxCursor)

        return dataMapPart.toMap()
    }

    private fun getSQItem(cursor: DicomCursor): TagToDataMap {
        if(DicomTag.canReadTag(cursor) == 0u) {
            throw Exception("Can't get item. ByteArray too short to read tag.")
        }
        val tag = cursor.readNextTag()
        val subCursor = DicomCursor(cursor.readNextByteField(tag.len))  // cursor over Item
        val subDataMap = DataRead(true).getFullDataMap(subCursor)
        return subDataMap
    }
    /** Read SQ's content. We don't interpret control characters, so it's safe to flatMap it. */
    private fun readSQData(sqData: DicomByteData): SQItemList {
        val subCursor = DicomCursor(sqData.value)   // cursor over SQ
        val subData = mutableListOf<TagToDataMap>()

        while(DicomTag.canReadTag(subCursor) != 0u) {
            subData.add(getSQItem(subCursor))
        }
        return SQItemList(subData)
    }
    /** For DicomByteData with tag SQ, get DicomDataElement with data type determined. */
    fun interpretSQData(sqData: DicomByteData): DicomDataElement<SQItemList> {
        return DicomDataElement(
            sqData.dicomTag,
            readSQData(sqData)
        )
    }

    private fun getOBItem(cursor: DicomCursor): DicomByteData {
        if(DicomTag.canReadTag(cursor) == 0u) {
            throw Exception("Can't get item. ByteArray too short to read tag.")
        }
        val tag = cursor.readNextTag()
        if(!tag.canReadValue(cursor)) {
            throw Exception("Can't get item. ByteArray too short to read value.")
        }
        return DicomByteData(
            tag,
            cursor.readNextByteField(tag.len)
        )
    }
    /** Read OB's content. */
    private fun readOBData(obData: DicomByteData): OBItemList {
        val subCursor = DicomCursor(obData.value)   // cursor over OB
        val subData = mutableListOf<DicomByteData>()
        while(DicomTag.canReadTag(subCursor) != 0u) {
            subData.add(getOBItem(subCursor))
        }
        return OBItemList(subData)
    }
    /** interpret OB. Don't use on all OB tags, only on the ones, that you know have item tags inside them. */
    fun interpretOBData(obData: DicomByteData): DicomDataElement<OBItemList> {
        return DicomDataElement(
            obData.dicomTag,
            readOBData(obData)
        )
    }

//    /**
//     * @param imgInfo Map of tag to data containing information about image */
//    fun readImageData(cursor: DicomCursor, imgInfo: Map<UInt, DicomByteData>) = ImageReader.skipImageData(cursor, imgInfo)

    //companion object {
    //}
}