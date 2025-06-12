package transform3d

import dev.romainguy.kotlin.math.Float3

object Config {
    const val gantryDirection = 1 // gantry rotation direction. 1 or -1
    const val interpolateByDicomValue = true // if to interpolate by dicom value. if false interpolate by 512 / nofImages
    val rotateDirection = Float3(1f, 1f, 1f)
}