package transform3d

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import kotlin.math.round

/** Short value to RGBA value */
fun fasterTransformPixelsToRGBA(source: ShortArray): ByteArray {
    // 4 bytes: R, G, B, alpha
    return ByteArray(source.size * 4) { bi ->
        if(bi % 4 == 3) {
            0xFF.toByte()       // alpha
        } else {
            (source[bi/4] / 256).toByte() // R, G, B
        }
    }
}

/** map value from range min..max to range 0..255 */
fun sample(value: Short, minValue: Int, maxValue: Int): Byte =
    sample(value, minValue.toShort(), maxValue.toShort()).toByte()

/** map value from range min..max to range 0..255 */
fun sample(value: Short, minValue: Short, maxValue: Short): Int {
    return when {
        (value < minValue) -> 0
        (value in minValue..maxValue) -> {
            val sourceR = ReARanger(minValue, maxValue)
            val targetR = ReARanger(0, 255)
            sourceR.valueToRange(value, targetR).toInt()
        }
        (value > maxValue) -> 255
        else -> {
            throw Exception("This will never happen")
        }
    }//.toUShort().toUByte().toByte()
}

fun fasterTransformPixelsToRGBA(source: ShortArray, fromRange: IntRange): ByteArray =
    fasterTransformPixelsToRGBA(source, fromRange.start, fromRange.endInclusive)
/** Short value to RGBA value */
fun fasterTransformPixelsToRGBA(source: ShortArray, minValue: Int, maxValue: Int): ByteArray {
    // 4 bytes: R, G, B, alpha
    return ByteArray(source.size * 4) { bi ->
        if(bi % 4 == 3) {
            0xFF.toByte()       // alpha
        } else {
            sample(source[bi/4], minValue, maxValue) // R, G, B
        }
    }
}

/*
/** Short value to RGBA value */
fun transformPixelsToRGBA(source: ShortArray): ByteArray {
    return source.flatMap { sh ->
        val byte = (sh / 256).toByte()
        listOf(byte, byte, byte, 0xFF.toByte()) // R, G, B as grey before, but full alpha
    }.toByteArray()
}
/** Short value to RGBA value */
fun transformPixelsToRGBA(source: List<Short>): ByteArray {
    return source.flatMap { sh ->
        val byte = (sh / 256).toByte()
        listOf(byte, byte, byte, 0xFF.toByte()) // R, G, B as grey before, but full alpha
    }.toByteArray()
}*/

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

enum class ExtView {
    SLICE, SIDE, TOP, FREE
    ///** poziomy kąt. Ten sam widok co [YZAngle] */ XZAngle,
    ///** pionowy kąt. Ten sam widok co [XZAngle] */ YZAngle,
}
fun View.toExtView() = when(this) {
    View.SLICE -> ExtView.SLICE
    View.TOP -> ExtView.TOP
    View.SIDE -> ExtView.SIDE
}
fun ExtView.toView() = when(this) {
    ExtView.SLICE -> View.SLICE
    ExtView.TOP -> View.TOP
    ExtView.SIDE -> View.SIDE
    ExtView.FREE -> View.SLICE
}
fun Angle.toExtView() = ExtView.FREE

enum class Mode {
    NONE,
    MEAN,
    MAX,
    FIRST_HIT
}
enum class Displaying {
    THREE,
    PROJECTION,
    ANIMATION,
}
enum class Angle {
    /** poziomy kąt */ XZAngle,
    /** pionowy kąt */ YZAngle,
}

fun modeMergeStrategy(mode: Mode, minValue: Int): (ShortArray) -> Short = when(mode) {
    Mode.NONE -> { shArr: ShortArray -> shArr[0] }
    Mode.MEAN -> { shArr: ShortArray ->
        val sum = shArr.reduce { acc, sh -> (acc + sh).toShort() }
        round((sum / shArr.size).toDouble()).toInt().toShort()
    }
    Mode.MAX -> { shArr: ShortArray -> shArr.max() }
    Mode.FIRST_HIT -> { shArr: ShortArray -> shArr.first { it >= minValue } }
}

// (on ImageAndData<ArrayOps>)
/** @param depth value from 0.0 to 1.0 */
fun getComposeImage(imgAndData: ImageAndData<ArrayOps>, view: View, depth: Float, valRange: IntRange): ImageBitmap? {
    if(depth !in 0f..1f) {
        println("depth $depth out of range 0.0--1.0")
        return null
    }
    val imgArr = imgAndData.imageArray
    val depthToIndex = { depth: Float, view: View ->
        round(depth * viewDepth(view, imgArr.whd)).toInt() //.also { println("Get image at index $it") }
    }
    val shArr = when(view) {
        View.SLICE -> imgArr.getFlatYXforZ(depthToIndex(depth, view))
        View.SIDE -> imgArr.getFlatYZforX(depthToIndex(depth, view))
        View.TOP -> imgArr.getFlatXZforY(depthToIndex(depth, view))
    }
    val shArrHByW = when(view) { // first is height, second width
        View.SLICE -> imgArr.size.height to imgArr.size.width // YX for Z
        View.SIDE -> imgArr.size.height to imgArr.size.depth  // YZ for X
        View.TOP -> imgArr.size.width to imgArr.size.depth    // XZ for Y
    }

    val imageBitmap = rawByteArrayToImageBitmap(
        fasterTransformPixelsToRGBA(shArr, valRange),
        shArrHByW.second,
        shArrHByW.first,
        4
    )
    return imageBitmap // non-null
}

fun getComposeImageAngled(imgAndData: ImageAndData<ArrayOps>, view: ExtView, depth: Float, valRange: IntRange,
                          yzAngle: Double, xzAngle: Double, mode: Mode = Mode.NONE): ImageBitmap? {
    if(depth !in 0f..1f) {
        println("depth $depth out of range 0.0--1.0")
        return null
    }
    val imgArr = imgAndData.imageArray
    val depthToIndex = { depth: Float ->
        round(depth * imgArr.size.depth).toInt() //.also { println("Get image at index $it") }
    }
    val merge = modeMergeStrategy(mode, -16000)
    val shArr = when(view) {
        ExtView.SLICE -> imgArr.getMergedSlicesAtAnyOrientation(depthToIndex(depth), yzAngle, xzAngle, merge)
        ExtView.SIDE -> imgArr.getMergedSlicesAtAnyOrientation(depthToIndex(depth), yzAngle, xzAngle+90.0, merge)
        ExtView.TOP -> imgArr.getMergedSlicesAtAnyOrientation(depthToIndex(depth), yzAngle+90.0, xzAngle+90.0, merge)
        ExtView.FREE -> imgArr.getMergedSlicesAtAnyOrientation(depthToIndex(depth), yzAngle, xzAngle, merge)
    }
    val shArrHByW = when(view) { // first is height, second width
        ExtView.SLICE -> imgArr.size.height to imgArr.size.width // YX for Z
        ExtView.SIDE -> imgArr.size.height to imgArr.size.depth  // YZ for X
        ExtView.TOP -> imgArr.size.width to imgArr.size.depth    // XZ for Y
        ExtView.FREE -> imgArr.size.height to imgArr.size.width // YX for Z
    }

    val imageBitmap = rawByteArrayToImageBitmap(
        fasterTransformPixelsToRGBA(shArr, valRange),
        shArrHByW.second,
        shArrHByW.first,
        4
    )
    return imageBitmap // non-null
}
