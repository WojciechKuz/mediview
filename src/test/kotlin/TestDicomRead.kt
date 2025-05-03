import filestructure.Header
import filestructure.informationGroupLength
import java.io.File
import kotlin.test.Test


class TestDicomRead {
    @Test
    fun testDicomRead() {
        val dicomread = DICOMByteRead(File("src/test/resources/IMG-0001-00001.dcm"))
        val cursor = DicomCursor(dicomread.bytes)
        Header.filePreamble(cursor) //println(filePreamble(cursor))
        assert(Header.dicomPrefix(cursor) == "DICM")
        val infoLength = informationGroupLength(cursor)
        // read next infoLength bytes and see what's after it. There are still some tags.
        cursor.moveBy(infoLength)       // FIXME we're skipping infoGroup?
        println(cursor.readNextByteField(16).toHexString())
        println(cursor.readNextByteField(16).toHexString())

    }

    /*@Test
    fun testIntConversion() {
        val byte = (196).toByte() // byte 0b11000100 can mean either -60 or 196
        println("To int " + byte.toInt())
        println("My conversion " + byte.toPositiveInt())
        println("UnaryPlus " + byte.unaryPlus())
        println("UnaryMinus " + byte.unaryMinus())
        assert(byte.toPositiveInt() == 196)
    }*/
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