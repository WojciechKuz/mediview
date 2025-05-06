import filestructure.*
import filestructure.groups.AllTagsFromPDF
import java.io.File
import kotlin.test.Test


class TestDicomRead {

    fun getCursor(): DicomCursor {
        val dicomread = DICOMByteRead(File("src/test/resources/IMG-0001-00001.dcm"))
        val cursor = DicomCursor(dicomread.bytes)
        return cursor
    }
    /** pass Header, put cursor at DataSet */
    fun cursorAtDataSet(): DicomCursor {
        val cursor = getCursor()
        Header.skipPreamble(cursor)
        Header.dicomPrefix(cursor)
        return cursor
    }
    fun strHex(u: UInt, pad: Int = 4): String {
//        if (u != 0u)
//            return "0x" + u.toString(16)
//        return "0x0000"
        return "0x" + u.toString(16).padStart(pad, '0') // same as "0x" + hexString(u)
    }



    @Test
    fun testDicomRead() {
        val cursor = getCursor()
        Header.filePreamble(cursor) //println(filePreamble(cursor))
        assert(Header.dicomPrefix(cursor) == "DICM")
        val infoLength = informationGroupLength(cursor)
        // read next infoLength bytes and see what's after it. There are still some tags.
        cursor.moveBy(infoLength)       // FIXME we're skipping infoGroup?
        println(cursor.readNextByteField(16).toHexString())
        println(cursor.readNextByteField(16).toHexString())
    }

    @Test
    fun testFindImgTag() {
        val imgTagID = mergeUInt(0x7FE0u, 0x0010u)
        val cursor = getCursor().findTag(imgTagID)
        println("Looking for ${strHex(imgTagID)} tag.")

        if (cursor.hasReachedEnd()) {
            println("ImgTag not found, reached end.")
            return
        }
        val imgTag = DicomTag.readTag(cursor)//displCursor.readNextTag()
        println(imgTag.toString() + ", canReadValue: " + imgTag.canReadValue(cursor))
        println("Found tag ${strHex(imgTag.tag)}.")
        assert(imgTag.tag == imgTagID)
    }

    /** read file until 7FE0 to a map. */
    fun dataMapUntilImage(cursor: DicomCursor): Map<UInt, DicomDataElement<out Any>> {
        val dataMap = DataSet().createDataMap(cursor) {
                tag -> tag.tag == tagAsUInt("(7FE0,0010)") // stop reading at "Pixel Data"
        }.mapValues { (_, v) -> determineDataType(v) }
        return dataMap
    }
    fun fullDataMap(cursor: DicomCursor): Map<UInt, DicomDataElement<out Any>> {
        val dataMap = DataSet().createDataMap(cursor).mapValues { (_, v) -> determineDataType(v) }
        return dataMap
    }

    @Test
    fun testSiemensTagEnd() {
        val cursor = cursorAtDataSet()
        println("Scanning file IMG-0001-00001.dcm...")
        val dataMap = dataMapUntilImage(cursor)
        val siemensTag = tagAsUInt("[0008 0070]")
        val weirdEndTag = tagAsUInt("[0002 0002]")
        println("Check suspicious tags - ${strHex(siemensTag)} ${strHex(weirdEndTag)}")
        if(dataMap.containsKey(siemensTag)){
            val siemens = dataMap[siemensTag]!!
            println("Last char in \"${siemens.value}\" is \'${(siemens.value as String).last()}\' and has code ${(siemens.value as String).last().code}")
        }
        if(dataMap.containsKey(weirdEndTag)){
            val weirdEnd = dataMap[weirdEndTag]!!
            println("Last char in \"${weirdEnd.value}\" is \'${(weirdEnd.value as String).last()}\' and has code ${(weirdEnd.value as String).last().code}")
        }
        assert(dataMap.containsKey(siemensTag)  && (dataMap[siemensTag]!!.value as String).last() == ' ')
        assert(dataMap.containsKey(weirdEndTag) && (dataMap[weirdEndTag]!!.value as String).last() == ' ')
    }

    fun printTags(dataMap: TagToDataMap) {
        val descriptionNotFoundList = mutableListOf<String>()
        dataMap.forEach { (k, v) ->
            if (v.len > 256u) {
                println(v.toString())
            }
            else {
                println( v.toString() +
                        when(k) {
                            in DataSet().tagNames -> "\t -> " + DataSet().tagNames[k]
                            in AllTagsFromPDF.allTagMap -> "\t -> " + AllTagsFromPDF.allTagMap[k]
                            else -> "".also { descriptionNotFoundList.add(v.getStringTag()) }
                        }
                )
            }
        }
        println("\nThese tag descriptions were not found:\n$descriptionNotFoundList")
    }

    @Test
    fun testPrintAllFileTags() {
        val cursor = cursorAtDataSet()
        println("Scanning file IMG-0001-00001.dcm...")
        val dataMap = fullDataMap(cursor) //dataMapUntilImage(cursor)
        println("Reading finished. Print...")
        printTags(dataMap)
    }


    @Test
    fun printAllVRs() {
        val cursor = cursorAtDataSet()
        val set = mutableSetOf<String>()
        DataSet().createDataMap(cursor).forEach { (k, v) ->
            if(v.vr !in set) {
                set.add(v.vr)
            }
        }
        println("All VRs in file: \n${set.joinToString(", ")}")
    }
}