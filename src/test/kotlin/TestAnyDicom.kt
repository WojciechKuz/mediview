import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import filestructure.DataRead
import filestructure.Header
import filestructure.ImageReader
import filestructure.informationGroupLength
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.imageio.IIOException
import kotlin.test.Test

class TestAnyDicom {
    private fun strHex(u: UInt, pad: Int = 4) = ReadHelp.strHex(u, pad)

    private fun pickDicom() = ReadHelp.pickDicom()
    private fun getCursor(path: String) = ReadHelp.getCursor(path)
    // USE: getCursor(pickDicom())

    @Test
    fun testDicomRead() {
        val cursor = getCursor(pickDicom())
        Header.filePreamble(cursor)
        assert(Header.dicomPrefix(cursor) == "DICM")
        val infoLength = informationGroupLength(cursor)
        println(cursor.readNextByteField(16).toHexString())
        println(cursor.readNextByteField(16).toHexString())
    }

    @Test
    fun testPrintAllFileTags() {
        val cursor = ReadHelp.cursorAtDataSet(pickDicom())
        val dataMap = DataRead().getFullDataMap(cursor) //dataMapUntilImage(cursor)
        println("Reading finished. Print content...")
        ReadHelp.printTags(dataMap)
    }

    @Test
    fun testAppOnly() {
        testApp(null) // null, so -> try default path dicom, can't read it -> display imagenotfound512.jpg
    }

    @Test
    fun testReadImg() {
        val imgTagID = 0x7FE00010u  // (7FE0,0010)
        val cursor = ReadHelp.getCursor().findTag(imgTagID)
        println("Looking for ${strHex(imgTagID)} tag.")

        if (cursor.hasReachedEnd()) {
            println("ImgTag not found, reached end.")
            return
        }
        val imgTag = DicomTag.readTag(cursor)//displCursor.readNextTag()
        println("Found tag ${strHex(imgTag.tag)}.")
        println(imgTag.toString() + ", canReadValue: " + imgTag.canReadValue(cursor))

        // TODO use testApp to display image from dicom

        // this byteData is OB tag value. It has sub-elements. This needs to be converted to get element[1]
        val byteData = DataRead().determineOBLength(cursor, imgTag) // create DicomDataElement = tag + value. Set length
        val obData = DataRead().interpretOBData(byteData)
        val imageBytes = obData.value[1].value
        println(obData.value)
        println("image size in bytes: " + imageBytes.size)
        println("Trying to start app...")
        try {
            testApp(imageBytes)
            println("App start successful :)")
        } catch (exc: IIOException) {
            println("Failed to start app :(")
            exc.printStackTrace()
        }
    }
    /*
    // SHOULD BE:
OB Item List to String
OBItemList.toString()
[7fe0 0010] OB 275466 {
Item: [fffe e000]    0
Item: [fffe e000]    275442
}	 -> Pixel Data Element
    // IRL IS:
     */

    private fun testApp(imageBytes: ByteArray?) = application {
        val imgsize = 512
        val state = rememberWindowState(size = DpSize.Unspecified)
        Window(onCloseRequest = ::exitApplication, title = "MediView by wojkuzb", state = state) {
            image(
                if(imageBytes != null) getPainterByteArray(imageBytes) else getPainter(ReadHelp.defaultPath)
                , imgsize, Color.Blue
            )
        }
    }
}