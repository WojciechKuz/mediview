package dicom

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skiko.toBufferedImage

@Deprecated("")
val standardImgInfo = ImageInfo(
    512, 512, ColorType.BGRA_8888, ColorAlphaType.OPAQUE
)
//val greyImgInfo = ImageInfo(512, 512, ColorType.GRAY_8, ColorAlphaType.OPAQUE)

@Deprecated("")
class BitmapCreator() {

    companion object {
        fun getValuesOfImageBitmap(image: ImageBitmap) {
            println("image color space:\n" + image.colorSpace)
            println("config:\n" + image.config)
            println("hasAlpha: " + image.hasAlpha)
            println("${image.width} X ${image.height}")
        }
        val defaultConfig = ImageBitmapConfig.Argb8888
        val defaultColorSpace = ColorSpaces.Srgb

        /** PLACEHOLDER */
        private fun byteToIntPixels(): IntArray {
            return IntArray(512*512)
        }

        fun toImageBitmap(pixels: IntArray): ImageBitmap { // pixels are argb
            val imgInfo = ImageInfo(
                512, 512, ColorType.BGRA_8888, ColorAlphaType.OPAQUE
            )
            val skiaBitmap = Bitmap()
            val ba =  ByteArray(512*512)
            skiaBitmap.installPixels(imgInfo,ba, 512)
            return skiaBitmap.toBufferedImage().toComposeImageBitmap()
        }
    }
}