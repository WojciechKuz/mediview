import filestructure.DataRead
import filestructure.Header
import filestructure.informationGroupLength
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlin.test.Test

class TestAnyDicom {
    private fun pickDicom() = ReadHelp.pickDicom()
    private fun getCursor(path: String) = ReadHelp.getCursor(path)
    // USE: getCursor(pickDicom())

    @Test
    fun testDicomRead() {
        val cursor = getCursor(pickDicom())
        Header.filePreamble(cursor)
        assert(Header.dicomPrefix(cursor) == "DICM")
        val infoLength = informationGroupLength(cursor)
        // read next infoLength bytes and see what's after it. There are still some tags.
        cursor.moveBy(infoLength)       // FIXME we're skipping infoGroup?
        println(cursor.readNextByteField(16).toHexString())
        println(cursor.readNextByteField(16).toHexString())
    }

    @Test
    fun testPrintAllFileTags() {
        val cursor = ReadHelp.cursorAtDataSet(pickDicom())
        println("Scanning file...")
        val dataMap = DataRead().getFullDataMap(cursor) //dataMapUntilImage(cursor)
        println("Reading finished. Print...")
        ReadHelp.printTags(dataMap)
    }
}