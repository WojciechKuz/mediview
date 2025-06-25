import dev.romainguy.kotlin.math.Float3

@Suppress("MayBeConstant")
object Config {
    val gantryDirection = -1 // gantry rotation direction. 1 or -1
    enum class InterpolateBy {To512, JustThickness, SliceThickn, SliceDist}
    val interpolateBy = InterpolateBy.SliceDist
    val fillDepthToWidthSize = true
    val rotateDirection = Float3(1f, 1f, 1f)
    val windowName = "feetpic 📷🦶" //"MediView by wojkuzb"
    val sliderSteps = 0
    val sliderRange = MySliderRange(0f, 256f)
    val selectedPixel = (512 * 3 / 4) * 512 + (512 * 1 / 2)
    val oddlySpecificValue = -16007
    /** Mark a pixel to color it red. */ val meansColorRed: Short = 32730 // max is 32768
    val redPixel = byteArrayOf(160.toByte(), 12, 0x00, 0xFF.toByte()) // 160, 12, 0, 255
    // UI:
    val displayImageSize = 400 //512
    val uiRescaleWidth = false
    val useThreads = 8
}
class MySliderRange(val start: Float, val end: Float) {
    val range = start..(end - 1) // do not change
    val startVal = (end - start) / 2 + start // do not change
    val minStartVal = start
    val maxStartVal = end - 1
    /** To 0.0-1.0 range */
    fun normalizeValue(value: Float) = (value - start) / (end - start) // do not change
}