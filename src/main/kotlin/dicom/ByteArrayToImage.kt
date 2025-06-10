package dicom

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.io.File

fun skiaBitmapInfo(sbitmap: Bitmap) {
    val width = sbitmap.width
    val height = sbitmap.height
    val rbytes = sbitmap.rowBytes
    val pxrbytes = sbitmap.rowBytesAsPixels
    println("width: $width height: $height")
    println("row bytes: $rbytes")
    println("row bytes as pixels: $pxrbytes")
}


/** ByteArray (encoded JPEG) -> Compose ImageBitmap.
 * Using Twelve Monkeys library for JPEG decoding, as basic ImageIO does not support SOI of FFC3.
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

// A16_UNORM - Pixel with a little endian uint16_t for alpha
val greyImgInfo = ImageInfo(512, 512, ColorType.A16_UNORM, ColorAlphaType.OPAQUE)

fun adaptGreyInfo(width: UInt, height: UInt, bitsAllocated: UInt) = ImageInfo(
    width.toInt(),
    height.toInt(),
    if(bitsAllocated > 8u)
        ColorType.A16_UNORM // there's nothing larger than 16 bits
    else
        ColorType.GRAY_8,
    ColorAlphaType.OPAQUE
)

/** Compose ImageBitmap -> ByteArray. */
fun imageBitmapToByteArray(image: ImageBitmap, imgInfo: ImageInfo): ByteArray {
    val skiaBitmap = image.asSkiaBitmap()
    //skiaBitmapInfo(skiaBitmap)
    val destBytes: ByteArray? = skiaBitmap.readPixels(imgInfo, skiaBitmap.rowBytes)
    if (destBytes != null) {
        return destBytes
    }
    else {
        println("Failed to convert image to bytes")
        return byteArrayOf() // TEMPORARY
    }
}

/** Encoded JPEG (in ByteArray) ---> Raw pixel buffer (ByteArray)
 * @param imgInfo target ImageInfo */
fun jpegToByteArray(jpegBytes: ByteArray, imgInfo: ImageInfo = greyImgInfo): ByteArray? {
    val bitmap = byteArrayToImageBitmap(jpegBytes)
    if (bitmap == null)
        return null
    return imageBitmapToByteArray(bitmap, greyImgInfo)
}