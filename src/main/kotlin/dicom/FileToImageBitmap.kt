package dicom

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.File
import javax.imageio.ImageIO

/** Same as byteArrayToImageBitmap, but works on files instead of ByteArrays */
fun fileToImageBitmap(imgPath: String): ImageBitmap? {
    val file = File(imgPath)
    try {
        val image = ImageIO.read(file)
        return image.toComposeImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}