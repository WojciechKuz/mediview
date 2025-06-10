package transform3d

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

object Interpolation {
    /** Performs Nearest Neighbor Interpolation
     * @param scaleFactor scale list by a factor. Values greater than 1 increase, lower decrease */
    fun interpolateNN(dataList: Array<Short>, scaleFactor: Double): Array<Short> {
        val targetSize = (dataList.size * scaleFactor).toInt() // round? ceil? floor?
        val target = Array(targetSize) { ti ->
            dataList[
                round( ti * scaleFactor ).toInt()
            ]
        }
        return target
    }

    /** Performs Nearest Neighbor Interpolation. */
    fun interpolateNN(dataList: Array<Short>, depth: Int, targetDepth: Int): Array<Short> {
        val target = Array(targetDepth) { ti ->
            dataList[
                round( ti * depth / targetDepth.toDouble() ).toInt()
            ]
        }
        return target
    }

    /** Performs bilinear interpolation
     * @param scaleFactor scale list by a factor. Values greater than 1 increase, lower decrease */
    fun interpolateBL(dataList: Array<Short>, scaleFactor: Double): Array<Short> {
        // depth is actually list size, lol
        val targetSize = (dataList.size * scaleFactor).toInt() // round? ceil? floor?
        val target = Array(targetSize) { ti ->
            val floPos = ti * scaleFactor
            val origI1 = floor(floPos).toInt()
            val origI2 = ceil(floPos).toInt()
            val w = floPos - origI1
            val val1 = dataList[origI1] * (1-w)
            val val2 = dataList[origI2] * w
            round(val1 + val2).toInt().toShort()
        }
        return target
    }

    /** Performs bilinear interpolation */
    fun interpolateBL(dataList: Array<Short>, depth: Int, targetDepth: Int): Array<Short> {
        // depth is actually list size, lol
        val scale = depth / targetDepth.toDouble()
        return interpolateBL(dataList, scale)
    }

    // also, 1:1 and fill rest with last. If to shorter, just cut off
}