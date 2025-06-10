
import dicom.DicomTag
import dicom.filestructure.DataRead
import dicom.filestructure.Header
import dicom.filestructure.informationGroupLength
import dicom.tagAsUInt
import dicom.toHexString
import kotlin.test.Test


class TestDicomRead {
    private fun strHex(u: UInt, pad: Int = 4) = ReadHelp.strHex(u, pad)

    @Test
    fun testDicomRead() {
        val cursor = ReadHelp.getCursor()
        Header.filePreamble(cursor) //println(filePreamble(cursor))
        assert(Header.dicomPrefix(cursor) == "DICM")
        val infoLength = informationGroupLength(cursor)
        // read next infoLength bytes and see what's after it. There are still some tags.
        cursor.moveBy(infoLength)       // we're skipping infoGroup?
        println(cursor.readNextByteField(16).toHexString())
        println(cursor.readNextByteField(16).toHexString())
    }

    @Test
    fun testFindImgTag() {
        val imgTagID = 0x7FE00010u  // (7FE0,0010)
        val cursor = ReadHelp.getCursor().findTag(imgTagID)
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
        val cursor = ReadHelp.cursorAtDataSet()
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
        val cursor = ReadHelp.cursorAtDataSet()
        println("Scanning file IMG-0001-00001.dcm...")
        val dataMap = DataRead().getFullDataMap(cursor) //dataMapUntilImage(cursor)
        println("Reading finished. Print...")
        ReadHelp.printTags(dataMap)
    }


    @Test
    fun printAllVRs() {
        val cursor = ReadHelp.cursorAtDataSet()
        val set = mutableSetOf<String>()
        DataRead().getFullDataMap(cursor).forEach { (k, v) ->
            if(v.vr !in set) {
                set.add(v.vr)
            }
        }
        println("All VRs in file: \n${set.joinToString(", ")}")
    }
}