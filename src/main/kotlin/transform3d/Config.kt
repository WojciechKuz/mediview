package transform3d

import dev.romainguy.kotlin.math.Float3

@Suppress("MayBeConstant")
object Config {
    val gantryDirection = 1 // gantry rotation direction. 1 or -1
    val interpolateByDicomValue = true // if to interpolate by dicom value. if false interpolate by 512 / nofImages
    val rotateDirection = Float3(1f, 1f, 1f)
    val windowName = "feetpic 📷🦶" //"MediView by wojkuzb"
    val sliderRange = MySliderRange(0f, 256f)
    val displayImageSize = 512
    val selectedPixel = (512 * 3 / 4) * 512 + (512 * 1 / 2)
}
class MySliderRange(val start: Float, val end: Float) {
    val range = start..(end - 1) // do not change
    val startVal = (end - start) / 2 + start // do not change
    fun normalizeValue(value: Float) = (value - start) / (end - start) // do not change
}