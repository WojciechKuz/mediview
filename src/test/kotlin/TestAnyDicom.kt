import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import dicom.InterpretData
import dicom.OBItemList
import dicom.OWItemList
import dicom.TagToDataMap
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

    private val swapWithRealImage = false

    private fun getDataMap(tagList: List<UInt>): TagToDataMap {
        val cursor = ReadHelp.cursorAtDataSet(pickDicom()) //.findTag(imgTagID)
        return DataRead().getPartialDataMap(cursor, tagList)
    }
    private fun getByteArray(dataMap: TagToDataMap): ByteArray? {
        val imgTagID = InterpretData.pixelDataTag  // (7FE0,0010)

        val imgData = dataMap[imgTagID]
        if (imgData == null) {
            println("ImgTag not found.")
            return null
        }
        println("Found tag ${strHex(imgData.tag)}.")
        println(imgData.toString())

        val byteData: ByteArray = when(imgData.value) {
            is OWItemList -> imgData.value.get().value
            is OBItemList -> imgData.value.get().value
            else -> { imgData.value as ByteArray }
        }

        val imageBytes: ByteArray = if(!swapWithRealImage) byteData else File("image1.jpg").readBytes()
        return imageBytes
    }

    @Test
    fun testAppOnly() {
        testApp(null) // null, so -> try default path dicom, can't read it -> display imagenotfound512.jpg
    }


    private val openWindow1 = false // with bitmap
    private val openWindow2 = false // with bytearray
    private val outputFile = true

    @Test
    fun testImgPrint() {
        val imgTagID = InterpretData.pixelDataTag  // (7FE0,0010)
        val selectedTags = listOf(
            imgTagID,
            InterpretData.transferSyntaxUIDTag,
        )
        val dataMap = getDataMap(selectedTags)

        val imageBytes: ByteArray = getByteArray(dataMap)?: return

        println("image size in bytes: " + imageBytes.size)
        if(outputFile) {
            println("Outputting image bytes to file...")
            byteArrayToFile(imageBytes, "testImg.jpg")
            println("Output succeeded")
        }
        if(openWindow1) {
            val bitmap = byteArrayToImageBitmap(imageBytes)
            //val imageBytes = File("image1.jpg").readBytes() // jpg -> bytes -> jpg works
            if (bitmap != null) {
                println("Bitmap loaded successfully.")
                println("Open app with bitmap")
                testApp2(bitmap)
            } else {
                println("Bitmap is null.")
            }
        }
        if (openWindow2) {
            println("Open app with byteArray")
            try {
                testApp(imageBytes)
            } catch (exc: IIOException) {
                println("Failed to start app :(")
                exc.printStackTrace()
            }
        }
        println("Test finished successfully.")
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