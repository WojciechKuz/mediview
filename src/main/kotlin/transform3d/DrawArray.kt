package transform3d

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import kotlin.math.round

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

enum class View {
    SLICE,  // xy
    SIDE,   // zy
    TOP     // zx
}
val viewDepth = { view: View, sizes: WidthHeightDepth -> when(view) {
    View.SLICE -> sizes.depth
    View.SIDE -> sizes.width
    View.TOP -> sizes.height
} }

// TODO array to ImageBitmap
// (on ImageAndData<ArrayOps>)
/** @param depth value from 0.0 to 1.0 */
fun getComposeImage(imgAndData: ImageAndData<ArrayOps>, view: View, depth: Float): ImageBitmap? {
    if(depth !in 0f..1f) {
        println("depth $depth out of range 0.0--1.0")
        return null
    }
    val imgArr = imgAndData.imageArray
    val depthToIndex = { depth: Float, view: View ->
        round(depth * viewDepth(view, imgArr.whd)).toInt() //.also { println("Get image at index $it") }
    }
    val shArrArr = when(view) {
        View.SLICE -> imgArr.zyx[depthToIndex(depth, view)]
        View.SIDE -> imgArr.xyz[depthToIndex(depth, view)]
        View.TOP -> imgArr.yxz[depthToIndex(depth, view)]
    }

    val imageBitmap = rawByteArrayToImageBitmap(
        transformPixels(shArrArr.flatten().toShortArray()), shArrArr[0].size, shArrArr.size, 4
    )
    return imageBitmap // non-null
}
fun getComposeImage(imgAndData: ImageAndData<ArrayOps>): ImageBitmap {
    //
    TODO()
}

// TODO on ArrayOps:
// combine valuesAtIndices and getAnyOrientationSlice to get slice in any orientation
