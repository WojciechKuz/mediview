import java.io.File
import kotlin.test.Test


class TestDicomRead {
    @Test
    fun testDicomRead() {
        val dicomread = DICOMByteRead(File("src/test/resources/IMG-0001-00001.dcm"))
        val cursor = DicomCursor(dicomread.bytes)
        filePreamble(cursor) //println(filePreamble(cursor))
        assert(dicomPrefix(cursor) == "DICM")
        informationGroupLength(cursor)
    }
}