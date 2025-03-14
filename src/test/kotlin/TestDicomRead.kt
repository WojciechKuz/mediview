import java.io.File
import kotlin.test.Test


class TestDicomRead {
    @Test
    fun testDicomRead() {
        val dicomread = DICOMByteRead(File("src/test/resources/IMG-0001-00001.dcm"))
        println(filePreamble(dicomread.bytes))
        assert(dicomPrefix(dicomread.bytes) == "DICM")
    }
}