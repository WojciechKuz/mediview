package dicom

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/** Using Twelve Monkeys library for JPEG decoding, as basic ImageIO does not support SOI of FFC3.
 * Skija and coil libraries failed too. 12 M overrides default ImageIO. */
fun byteArrayToImageBitmap(byteArray: ByteArray): ImageBitmap? {
    try {
        val inputStream = ByteArrayInputStream(byteArray)
        val image = ImageIO.read(inputStream)

        return image.toComposeImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}