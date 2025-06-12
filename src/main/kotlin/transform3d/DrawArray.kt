package transform3d

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import kotlin.experimental.and

/** Short value to RGBA value */
fun transformPixels(source: ShortArray): ByteArray {
    return source.flatMap { sh ->
        val byte = (sh / 256).toByte()
        listOf(byte, byte, byte, 0xFF.toByte()) // R, G, B as grey before, but full alpha
    }.toByteArray()
}

/** BYTES per pixel. 1, 2 or 4 */
fun rawByteArrayToImageBitmap(bytes: ByteArray, width: Int, height: Int, bytesPerPx: Int): ImageBitmap {
    val sourceInfo = ImageInfo(
        width, height,
        when(bytesPerPx) {
            1 -> ColorType.GRAY_8
            2 -> ColorType.A16_UNORM
            4 -> ColorType.RGBA_8888
            else -> ColorType.RGBA_8888
        },
        ColorAlphaType.OPAQUE,
    )
    val targetInfo = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.OPAQUE)
    val skiaBitmap = Bitmap()
    skiaBitmap.allocPixels(targetInfo)
    skiaBitmap.installPixels(sourceInfo, bytes, width * bytesPerPx)
    return skiaBitmap.asComposeImageBitmap()
}

// TODO array to ImageBitmap
// (on ImageAndData<ArrayOps>)

// TODO on ArrayOps:
// combine valuesAtIndices and getAnyOrientationSlice to get slice in any orientation
