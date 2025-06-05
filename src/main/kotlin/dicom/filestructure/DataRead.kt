package dicom.filestructure

import dicom.DicomByteData
import dicom.DicomCursor
import dicom.DicomDataElement
import dicom.DicomTag
import dicom.TagToDataMap
import dicom.determineDataType
import dicom.tagAsUInt

/** High level dicom reading functions.
 * @param ignoreWarnings When reading tag with vr of `"  "` don't print any warning. */
class DataRead(private val ignoreWarnings: Boolean = false) {

    private fun tryDetermineType(v: DicomByteData): DicomDataElement<out Any> {
        try {
            return determineDataType(v)
        }
        catch (ex: Exception) {
            ex.printStackTrace()
        }
        return v as DicomDataElement<out Any>
    }

    /** Get Full Data Map. Control tags like (FFFE,E00D) are ignored */
    fun getFullDataMap(cursor: DicomCursor): TagToDataMap {
        return createDataMap(cursor).mapValues { (_, v) -> tryDetermineType(v)
        }
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
    private val specialVRs: List<String> = listOf("SQ", "OB", "OW", "  ")

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
            "OW" -> {
                if(stopTag.isLengthDefined()) {
                    if(stopTag.canReadValue(cursor)) {
                        dataMap[stopTag.tag] = DicomByteData(stopTag, cursor.readNextByteField(stopTag.len))
                    }
                    // just read value
                }
                else {
                    println("Length undefined, determine it...")
                    val alteredStopTagData = determineOWLength(cursor, stopTag)
                    dataMap[alteredStopTagData.tag] = alteredStopTagData
                }
            }
            "SQ" -> {
                if( stopTag.canReadValue(cursor) ) {
                    dataMap[stopTag.tag] = DicomByteData(stopTag, cursor.readNextByteField(stopTag.len))
                }
            }
            "  " -> {
                if(!ignoreWarnings) {
                    println("Warning! Control tags in global context.")
                    println("They are not unique, not suitable for a Map.")
                }
                // do nothing, continue reading just after tag. Don't add this tag to a Map
            }
        }
    }

    fun determineOWLength(cursor: DicomCursor, obTag: DicomTag, createUntil: (DicomTag) -> Boolean  = {false}) = determineOBLength(cursor, obTag, createUntil)

    /** Call for OB of undefined length. Will determine the length and set ByteArray */
    fun determineOBLength(cursor: DicomCursor, obTag: DicomTag, createUntil: (DicomTag) -> Boolean  = {false}): DicomByteData {
        println("Determine OB/OW length")
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
}