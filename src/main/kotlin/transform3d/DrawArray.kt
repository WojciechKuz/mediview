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
            maxByte       // alpha
        } else {
            (source[bi/4] / 256).toByte() // R, G, B
        }
    }
}

private val byteRange = ReARanger(0, 255)
/** map value from range min..max to range 0..255 */
fun sample(value: Short, minValue: Int, maxValue: Int): Byte {
    return when {
        (value < minValue) -> byte0
        (value in minValue..maxValue) -> {
            val sourceR = ReARanger(minValue.toShort(), maxValue.toShort())
            //val targetR = ReARanger(0, 255)
            sourceR.valueToRange(value, byteRange).toByte()
        }
        (value > maxValue) -> maxByte
        else -> {
            throw Exception("DrawArray.kt/sample(): This will never happen")
        }
    }//.toUShort().toUByte().toByte()
}

private const val maxByte = 0xFF.toByte()
private const val byte0 = 0x00.toByte()
private const val sh0 = 0.toShort()

/** Short value to RGBA value as greyscale */
fun fasterTransformPixelsToRGBA(source: ShortArray, fromRange: IntRange): ByteArray {
    // 4 bytes: R, G, B, alpha
    return ByteArray(source.size * 4) { bi ->
        if(bi % 4 == 3) {
            maxByte       // alpha
        } else {
            if(source[bi/4] == Config.meansColorRed) { // if true, mark pixel with red
                Config.redPixel[bi%4]
            } else {
                sample(source[bi/4], fromRange.start, fromRange.endInclusive) // R, G, B
            }
        }
    }
}

val redYellowGreenRange = ReARanger(0, 511)
/** Short value to RGBA value as red-yellow-green scale */
fun redYellowGreenTransformPixelsToRGBA(source: ShortArray, fromRange: IntRange): ByteArray {
    // 4 bytes: R, G, B, alpha
    return ByteArray(source.size * 4) { bi ->
        val sourceR = ReARanger(fromRange.start.toShort(), fromRange.endInclusive.toShort())
        val ryg = sourceR.valueToRange(source[bi/4], redYellowGreenRange)

        when {
            source[bi/4] == Config.meansColorRed -> { // if true, mark pixel with red
                Config.redPixel[bi%4]
            }
            bi%4 == 0 -> {
                if (ryg > 255) maxByte else ryg.toByte()
            } // Red
            bi%4 == 1 -> {
                if (ryg < 256) maxByte else (511 - ryg).toByte()
            } // Green
            bi%4 == 2 -> byte0 // Blue
            bi % 4 == 3 -> maxByte // alpha
            else -> throw Exception("This will never happen")
        }
    }
}

/** ByteArray -> Compose Image. BYTES per pixel: 1, 2 or 4 */
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
val viewDepth = { view: View, sizes: MySize3 -> when(view) {
    View.SLICE -> sizes.depth
    View.SIDE -> sizes.width
    View.TOP -> sizes.height
} }

enum class ExtView {
    SLICE, SIDE, TOP, FREE
    // /** poziomy kąt. Ten sam widok co [YZAngle] */ XZAngle,
    // /** pionowy kąt. Ten sam widok co [XZAngle] */ YZAngle,
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
    /** Won't mark first hit value. Besides that, same as none but faster */ EFFICIENT_NONE,
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
fun printAngles(angleMap: Map<Angle, Float>): String {
    return "XZ ${"%.2f".format(angleMap[Angle.XZAngle])}°, YZ ${"%.2f".format(angleMap[Angle.YZAngle])}°"
}
enum class MyColor {
    GREYSCALE,
    /** Red, yellow, green scale */ RYGSCALE
}

private fun firstHit(shArr: ShortArray, minValue: Short): Short {
    for(sh in shArr) {
        if(sh >= minValue) return sh
    }
    return 0
}

/** Check if pixel in None mode meets firstHit condition.
 * This means pixel is greater or equal minValue, and no pixel before it meets this condition.
 * @param wholeArr here is NOT [index, end], but [start, end].
 * @return value from config marking to color this pixel red */
fun redOnNone(wholeArr: ShortArray, minValue: Short, realDepth: Int): Short {
    if(wholeArr[realDepth] < minValue) return wholeArr[realDepth]
    for(i in 0 until realDepth) {
        if(wholeArr[i] >= minValue) return wholeArr[realDepth]
    }
    return Config.meansColorRed
}

/** Provides pixel merging function vor given mode.
 * @param minValue used in FIRST_HIT and NONE mode.
 * @param depth In NONE mode used as depth. */
fun modeMergeStrategy(mode: Mode, minValue: Short, depth: Int = -16000): (ShortArray) -> Short = when(mode) {
    Mode.EFFICIENT_NONE -> { shArr: ShortArray -> shArr[0] } // EFFICIENT_NONE should not use this function.
    Mode.NONE -> { shArr: ShortArray -> redOnNone(shArr, minValue, depth) } // EFFICIENT_NONE should not use this function
    Mode.MEAN -> { shArr: ShortArray ->
        var sum = 0
        for(sh in shArr) { sum += sh }
        round((sum / shArr.size).toDouble()).toInt().toShort()
    }
    Mode.MAX -> { shArr: ShortArray -> shArr.max() }
    Mode.FIRST_HIT -> { shArr: ShortArray -> firstHit(shArr, minValue) }
    /*    { shArr: ShortArray ->
        for(sh in shArr) { if(sh >= minValue) sh } // returns sh if hit
        0
    } */ // did not work as intended. Use standard function
}

// (on ImageAndData<ArrayOps>)
/** @param depth value from 0.0 to 1.0 */
suspend fun getComposeImage(imgAndData: ImageAndData<ArrayOps>, view: View, depth: Float, valRange: IntRange,
                    mode: Mode = Mode.NONE, color: MyColor = MyColor.GREYSCALE, firstHitVal: Short = -16000): ImageBitmap? {
    if(depth !in 0f..1f) {
        println("depth $depth out of range 0.0--1.0")
        return null
    }
    val imgArr = imgAndData.imageArray
    /** real depth index */
    val depthIndex = round(depth * viewDepth(view, imgArr.size)).toInt()
    /** merge starting at this index */
    val mergeFromIndex = if(mode == Mode.NONE) 0 else depthIndex // NONE needs to start at 0
    val shArr = if(mode == Mode.EFFICIENT_NONE) {
        when(view) {
            View.SLICE -> imgArr.getFlatYXforZ(depthIndex)
            View.SIDE -> imgArr.getFlatYZforX(depthIndex)
            View.TOP -> imgArr.getFlatXZforY(depthIndex)
        }
    } else {
        val merge = modeMergeStrategy(mode, firstHitVal, depthIndex)
        when(view) {
            View.SLICE -> imgArr.getFlatYXforMergedZ(mergeFromIndex, merge)
            View.SIDE -> imgArr.getFlatYZforMergedX(mergeFromIndex, merge)
            View.TOP -> imgArr.getFlatXZforMergedY(mergeFromIndex, merge)
        }
    }

    val shArrHByW = when(view) { // first is height, second width
        View.SLICE -> imgArr.size.height to imgArr.size.width // YX for Z
        View.SIDE -> imgArr.size.height to imgArr.size.depth  // YZ for X
        View.TOP -> imgArr.size.width to imgArr.size.depth    // XZ for Y
    }
    val bytes = when(color) {
        MyColor.GREYSCALE -> fasterTransformPixelsToRGBA(shArr, valRange)
        MyColor.RYGSCALE -> redYellowGreenTransformPixelsToRGBA(shArr, valRange)
    }

    val imageBitmap = rawByteArrayToImageBitmap(
        bytes,
        shArrHByW.second,
        shArrHByW.first,
        4
    )
    return imageBitmap // non-null
}

/** Use this function to get image bitmap that can be displayed in UI.
 * @param imgAndData dataMap + ArrayOps
 * @param view which view to display
 * @param depth depth of view, normalized
 * @param valRange value range in which values will have scaled color. Values outside this range take max or min value.
 * @param yzAngle vertical angle, degrees
 * @param xzAngle horizontal angle, degrees
 * @param mode which mode */
suspend fun getComposeImageAngled(
    imgAndData: ImageAndData<ArrayOps>, view: ExtView, depth: Float, valRange: IntRange,
    yzAngle: Double, xzAngle: Double, mode: Mode = Mode.NONE, color: MyColor = MyColor.GREYSCALE, firstHitVal: Short = -16000
): ImageBitmap? = getComposeImageAngled(imgAndData.imageArray, view, depth, valRange, yzAngle, xzAngle, mode, color, firstHitVal)

/** Use this function to get image bitmap that can be displayed in UI.
 * @param imgArr ArrayOps
 * @param view which view to display
 * @param depth depth of view, normalized
 * @param valRange value range in which values will have scaled color. Values outside this range take max or min value.
 * @param yzAngle vertical angle, degrees
 * @param xzAngle horizontal angle, degrees
 * @param mode which mode */
suspend fun getComposeImageAngled(imgArr: ArrayOps, view: ExtView, depth: Float, valRange: IntRange,
                          yzAngle: Double, xzAngle: Double, mode: Mode = Mode.NONE, color: MyColor = MyColor.GREYSCALE, firstHitVal: Short = -16000): ImageBitmap? {
    if(depth !in 0f..1f) {
        println("depth $depth out of range 0.0--1.0")
        return null
    }
    val depthIndex = round(depth * imgArr.size.depth).toInt()
    /** merge starting at this index */
    val mergeFromIndex = if(mode == Mode.NONE) 0 else depthIndex // NONE needs to start at 0
    val merge = modeMergeStrategy(mode, firstHitVal, depthIndex)
    val ensureAngleInRange = { angle: Double ->
        if(angle > 180.0) angle - 360.0 else angle
    }
    /** Angles. first is yzAngle, second is xzAngle */
    val adjustedAngles = when(view) {
        ExtView.SLICE -> yzAngle to xzAngle
        ExtView.SIDE -> yzAngle to ensureAngleInRange(xzAngle+90.0)
        ExtView.TOP -> ensureAngleInRange(yzAngle+90.0) to ensureAngleInRange(xzAngle+90.0)
        ExtView.FREE -> yzAngle to xzAngle
    }
    val shArr = imgArr.getMergedSlicesAtAnyOrientation(mergeFromIndex, adjustedAngles.first, adjustedAngles.second, merge)
    /** Size. first is height, second is width */
    val shArrSizeHByW = when(view) {
        ExtView.SLICE -> imgArr.size.height to imgArr.size.width // YX for Z
        ExtView.SIDE -> imgArr.size.height to imgArr.size.depth  // YZ for X
        ExtView.TOP -> imgArr.size.width to imgArr.size.depth    // XZ for Y
        ExtView.FREE -> imgArr.size.height to imgArr.size.width // YX for Z
    }
    val bytes = when(color) {
        MyColor.GREYSCALE -> fasterTransformPixelsToRGBA(shArr, valRange)
        MyColor.RYGSCALE -> redYellowGreenTransformPixelsToRGBA(shArr, valRange)
    }

    val imageBitmap = rawByteArrayToImageBitmap(
        bytes,
        shArrSizeHByW.second,
        shArrSizeHByW.first,
        4
    )
    return imageBitmap // non-null
}
