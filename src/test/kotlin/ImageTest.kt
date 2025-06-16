import dicom.fileToImageBitmap
import dicom.imageBitmapToByteArray
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import transform3d.rawByteArrayToImageBitmap
import java.awt.FileDialog
import java.awt.Frame
import kotlin.test.Test

class ImageTest {

    @Test
    fun imageToBytesAndBack() {
        val dialog = FileDialog(null as Frame?, "Select JPEG File to Open", FileDialog.LOAD)
        dialog.isVisible = true
        val filepath = dialog.directory + dialog.file
        println("File: " + filepath)
        val bitmap = fileToImageBitmap(filepath)
        if (bitmap == null) {
            println("Failed to get bitmap")
            return
        }
        println("Got bitmap with info: ${bitmap.config}")
        val info = ImageInfo(
            bitmap.width,
            bitmap.height,
            ColorType.RGBA_8888,
            ColorAlphaType.OPAQUE
        )
        val bytes = imageBitmapToByteArray(bitmap, info)
        println("Got ${bytes.size} bytes")
        val img = rawByteArrayToImageBitmap(bytes, bitmap.width, bitmap.height, 4)
        println("Got image back!")
    }

    @Test
    fun testIsArrayWritable() {
        val arr = arrayOf(50, 60, 70, 80, 90)
        println(arr.toList().toString())
        for (i in arr.indices) {
            arr[i] = arr[i] + 2
        }
        println(arr.toList().toString())
    }
}