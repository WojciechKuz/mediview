import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import dicom.DicomTag
import dicom.byteArrayToFile
import dicom.byteArrayToImageBitmap
import dicom.toHexString
import dicom.filestructure.DataRead
import dicom.filestructure.Header
import dicom.filestructure.ImageReader
import dicom.filestructure.informationGroupLength
import java.io.File
import javax.imageio.IIOException
import kotlin.test.Test

// Warning! some tests which open a window might end as failed when closing a window.
// This is a bug in test framework and the test results are OK.

class TestAnyDicom {
    private fun strHex(u: UInt, pad: Int = 4) = ReadHelp.strHex(u, pad)

    private fun pickDicom() = ReadHelp.pickDicom()
    private fun getCursor(path: String) = ReadHelp.getCursor(path)
    // USE: getCursor(pickDicom())

    @Test
    fun testDicomRead() {
        val cursor = getCursor(pickDicom())
        Header.filePreamble(cursor)
        assert(Header.dicomPrefix(cursor, true) == "DICM")
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
    fun testImageRead() {
        val cursor = ReadHelp.cursorAtDataSet(pickDicom())
        val dataMap = DataRead().getFullDataMap(cursor) //dataMapUntilImage(cursor)
        println("Reading finished. Print content...")
        ImageReader.readImageData(dataMap)
    }

    private val openWindow1 = true
    private val swapWithRealImage = false
    @Test
    fun testBytesToImage() {
        val imgTagID = 0x7FE00010u  // (7FE0,0010)
        val cursor = ReadHelp.getCursor().findTag(imgTagID)
        println("Looking for ${strHex(imgTagID)} tag.")

        if (cursor.hasReachedEnd()) {
            println("ImgTag not found, reached end.")
            return
        }
        val imgTag = DicomTag.readTag(cursor)//displCursor.readNextTag()
        println("Found tag ${strHex(imgTag.tag)}.")

        // this byteData is OB tag value. It has sub-elements. This needs to be converted to get element[1]
        val byteData = DataRead().determineOBLength(cursor, imgTag) // create DicomDataElement = tag + value. Set length
        println(byteData.toString())
        val obData = DataRead().interpretOBData(byteData)

        val imageBytes = if(!swapWithRealImage) obData.value[1].value else File("image1.jpg").readBytes()
        val bitmap = byteArrayToImageBitmap(imageBytes)
        if (bitmap != null) {
            if(openWindow1)
                testApp2(bitmap)
        } else {
            println("Bitmap is null.")
        }
    }

    @Test
    fun testAppOnly() {
        testApp(null) // null, so -> try default path dicom, can't read it -> display imagenotfound512.jpg
    }

    private val openWindow2 = false
    private val outputFile = true

    @Test
    fun testImgPrint() {
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

        // this byteData is OB tag value. It has sub-elements. This needs to be converted to get element[1]
        val byteData = DataRead().determineOBLength(cursor, imgTag) // create DicomDataElement = tag + value. Set length
        val obData = DataRead().interpretOBData(byteData)

        val imageBytes = obData.value[1].value
        //val imageBytes = File("image1.jpg").readBytes() // jpg -> bytes -> jpg works

        println(obData.value)
        println("image size in bytes: " + imageBytes.size)
        if(outputFile) {
            println("Outputting image bytes to file...")
            byteArrayToFile(imageBytes, "testImg.jpg")
            println("Output succeeded")
        }
        if(!openWindow2) return
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
    private fun testApp2(imageBitmap: ImageBitmap) = application {
        val imgsize = 512
        val state = rememberWindowState(size = DpSize.Unspecified)
        Window(onCloseRequest = ::exitApplication, title = "MediView by wojkuzb", state = state) {
            image(
                BitmapPainter( imageBitmap )
                , imgsize, Color.Blue
            )
        }
    }
    /*
    // for coil solution
    private fun testApp3(imageBytes: ByteArray) = application {
        val imgsize = 512
        val state = rememberWindowState(size = DpSize.Unspecified)
        Window(onCloseRequest = ::exitApplication, title = "MediView by wojkuzb", state = state) {
            Image(painter = byteArrayToCoilPainter(imageBytes)!!, contentDescription = "lol")
        }
    }*/
}