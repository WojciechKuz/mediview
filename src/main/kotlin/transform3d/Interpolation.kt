package transform3d

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

object Interpolation {
    /** Performs Nearest Neighbor Interpolation */
    fun interpolateNN(dataList: List<Short>, depth: Int, targetDepth: Int): List<Short> {
        val target = List(targetDepth) { ti ->
            dataList[
                round( ti * depth / targetDepth.toDouble() ).toInt()
            ]
        }
        return target
    }
    /** Performs bilinear interpolation */
    fun interpolateBL(dataList: List<Short>, depth: Int, targetDepth: Int): List<Short> {
        val target = List(targetDepth) { ti ->
            val floPos = ti * depth / targetDepth.toDouble()
            val origI1 = floor(floPos).toInt()
            val origI2 = ceil(floPos).toInt()
            val w = floPos - origI1
            val val1 = dataList[origI1] * (1-w)
            val val2 = dataList[origI2] * w
            round(val1 + val2).toInt().toShort()
        }
        return target
    }

    // also, 1:1 and fill rest with last. If to shorter, just cut off
}