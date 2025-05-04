import filestructure.Header
import filestructure.informationGroupLength
import java.io.File
import kotlin.test.Test


class TestDicomRead {

    fun getCursor(): DicomCursor {
        val dicomread = DICOMByteRead(File("src/test/resources/IMG-0001-00001.dcm"))
        val cursor = DicomCursor(dicomread.bytes)
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
    fun testEndianness() {
        val bytearr = byteArrayOf(0x10, 0x00, 0x00, 0x00) // 0x10, 0x00 expected to be 0x0010
        val cursor = DicomCursor(bytearr)
        val nxtInt = cursor.readNextInt(2)
        println(strHex(nxtInt))
        assert(nxtInt == 0x0010u)
    }
    @Test
    fun test4Byte() {
        val bytearr = byteArrayOf(0x10, 0x20, 0x30, 0x40) // expected to be 0x40302010
        val cursor = DicomCursor(bytearr)
        val nxtInt = cursor.readNextInt(4)
        println(strHex(nxtInt))
        assert(nxtInt == 0x40302010u)
    }
    @Test
    fun testTagAsUInt() {
        val strTag = "(0028,0100)"
        val tagAsU = tagAsUInt(strTag)
        val mTag = mergeUInt(0x0028u, 0x0100u)
        println(strTag + ": " + strHex(tagAsU, 8) + " should be " + strHex(mTag, 8))
        assert(tagAsU == mTag)
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

    @Test
    fun mergeUIntTest() {
        val mrg = mergeUInt(0x7FE0u, 0x0010u)
        println("0x" + mrg.toString(16))
        assert(0x7FE00010u == mrg)
    }

    @Test
    fun testUIntConversion() {
        val byte = (196).toByte() // byte 0b11000100 can mean either -60 or 196
        println("To ubyte " + byte.toUByte())
        println("To uint " + byte.toUInt())
        println("To ubyte, then to uint " + byte.toUByte().toUInt())
        println("My toU(), like above. " + byte.toU())
        assert(byte.toU() == 196u)
    }
}