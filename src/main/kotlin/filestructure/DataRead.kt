package filestructure

import DataType
import DicomByteData
import DicomCursor
import DicomDataElement
import DicomTag
import SQItemList
import TagToDataMap
import determineDataType
import filestructure.groups.*
import tagAsUInt

/** @param ignoreWarnings When reading tag with vr of `"  "` don't print any warning */
class DataRead(private val ignoreWarnings: Boolean = false) {

    /** Get Full Data Map */
    fun getFullDataMap(cursor: DicomCursor): TagToDataMap {
        return createDataMap(cursor).mapValues { (_, v) -> determineDataType(v) }
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
                    //dataMap[stopTag.tag] = interpretSQData(cursor.readNextByteField(stopTag.len))
                    // TODO new function that will interpret what's inside sequence in place, then flatMap it.
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
    private val internalStopCondition: (DicomTag) -> Boolean = {  it.vr in specialVRs || it.tag in specialTags }

    /** Read all to a map from UInt tag, to DicomByteData */
    private fun createDataMap(cursor: DicomCursor, createUntil: (DicomTag) -> Boolean  = {false}): Map<UInt, DicomByteData> {
        val dataMapPart: MutableMap<UInt, DicomByteData> = mutableMapOf()

        do {
            dataMapPart += cursor.readAllTags { createUntil(it) || internalStopCondition(it) }.associateBy { it.tag }
        }
        while (whyStopped(cursor, createUntil, dataMapPart))

        return dataMapPart.toMap()
    }

    private fun getItem(cursor: DicomCursor): TagToDataMap {
        if(DicomTag.canReadTag(cursor) == 0u) {
            throw Exception("Can't get item. ByteArray too short to read tag.")
        }
        val tag = cursor.readNextTag()
        val subCursor = DicomCursor(cursor.readNextByteField(tag.len))  // cursor over Item
        val subDataMap = DataRead(true).getFullDataMap(subCursor)
        return subDataMap
    }

    /** Read SQ's content. We don't interpret control characters, so it's safe to flatMap it. */
    fun readSQData(sqData: DicomByteData): SQItemList {
        val subCursor = DicomCursor(sqData.value)   // cursor over SQ
        val subData = mutableListOf<TagToDataMap>()

        while(DicomTag.canReadTag(subCursor) != 0u) {
            subData.add(getItem(subCursor))
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

    /**
     * @param imgInfo Map of tag to data containing information about image */
    fun readImageData(cursor: DicomCursor, imgInfo: Map<UInt, DicomByteData>) = ImageReader.skipImageData(cursor, imgInfo)

    //companion object {
    //}
}