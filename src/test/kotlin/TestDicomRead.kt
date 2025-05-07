import filestructure.*
import filestructure.groups.AllTagsFromPDF
import java.io.File
import kotlin.test.Test


class TestDicomRead {

    fun getCursor(): DicomCursor {
        val cursor = DicomCursor(File("src/test/resources/IMG-0001-00001.dcm"))
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

    fun printTags(dataMap: TagToDataMap) {
        val descriptionNotFoundList = mutableListOf<String>()
        dataMap.forEach { (k, v) ->
            if (v.len > 256u) {
                println(v.toString())
            }
            else {
                println( v.toString() +
                        when(k) {
                            in DataSet.tagNames -> "\t -> " + DataSet.tagNames[k]
                            in AllTagsFromPDF.allTagMap -> "\t -> " + AllTagsFromPDF.allTagMap[k]
                            else -> "".also { descriptionNotFoundList.add(v.getStringTag()) }
                        }
                )
            }
        }
        println("\nThese tag descriptions were not found:\n$descriptionNotFoundList")
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
        val imgTagID = 0x7FE00010u  // (7FE0,0010)
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

    @Test
    fun testSiemensTagEnd() {
        val cursor = cursorAtDataSet()
        println("Scanning file IMG-0001-00001.dcm...")
        val dataMap = DataRead().getFullDataMap(cursor)
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

    @Test
    fun testPrintAllFileTags() {
        val cursor = cursorAtDataSet()
        println("Scanning file IMG-0001-00001.dcm...")
        val dataMap = DataRead().getFullDataMap(cursor) //dataMapUntilImage(cursor)
        println("Reading finished. Print...")
        printTags(dataMap)
    }


    @Test
    fun printAllVRs() {
        val cursor = cursorAtDataSet()
        val set = mutableSetOf<String>()
        DataRead().getFullDataMap(cursor).forEach { (k, v) ->
            if(v.vr !in set) {
                set.add(v.vr)
            }
        }
        println("All VRs in file: \n${set.joinToString(", ")}")
    }
}