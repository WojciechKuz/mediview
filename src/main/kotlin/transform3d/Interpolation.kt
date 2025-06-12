package transform3d

import kotlin.Short
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

object Interpolation {
    /** Performs Nearest Neighbor Interpolation
     * @param scaleFactor scale list by a factor. Values greater than 1 increase, lower decrease */
    fun rescaleNN(dataList: Array<Short>, scaleFactor: Double): Array<Short> {
        val targetSize = (dataList.size * scaleFactor).toInt() // round? ceil? floor?
        val target = Array(targetSize) { ti ->
            dataList[
                round( ti * scaleFactor ).toInt()
            ]
        }
        return target
    }

    /** Performs Nearest Neighbor Interpolation. */
    fun rescaleNN(dataList: Array<Short>, depth: Int, targetDepth: Int): Array<Short>
    = rescaleNN(dataList, depth.toDouble() / targetDepth)

    /** Performs bilinear interpolation
     * @param scaleFactor scale list by a factor. Values greater than 1 increase, lower decrease */
    fun rescaleBL(dataList: Array<Short>, scaleFactor: Double): Array<Short> {
        val targetSize = (dataList.size * scaleFactor).toInt() // round? ceil? floor?
        val target = Array(targetSize) { ti ->
            val oldI = ti * scaleFactor
            linearInterpolation(dataList, oldI)
        }
        return target
    }

    /** Performs bilinear interpolation */
    fun rescaleBL(dataList: Array<Short>, depth: Int, targetDepth: Int): Array<Short> {
        // depth is actually list size, lol
        val scale = depth / targetDepth.toDouble()
        return rescaleBL(dataList, scale)
    }

    /** Moves array by given floating amount. Uses Nearest neighbor to get value. */
    fun moveNN(dataList: Array<Short>, moveBy: Double): Array<Short> {
        val target = Array(dataList.size) { ti ->
            dataList[
                    round( ti - moveBy ).toInt()
            ]
        }
        return target
    }

    /** Moves array by given floating amount. Uses Bilinear to get value.
     * Values that exceed the length are discarded, empty space is filled by nearest value */
    fun moveBL(dataList: Array<Short>, moveBy: Double): Array<Short> {
        val target = Array(dataList.size) { ti ->
            val oldI = ti - moveBy
            linearInterpolation(dataList, oldI)
        }
        return target
    }

    /** ensure floating index is within array bounds  */
    private fun ensureInBounds(expectedIndex: Double, size: Int): Double {
        val last = (size - 1).toDouble()
        return when {
            expectedIndex < 0.0 -> 0.0
            expectedIndex > last -> last
            else -> expectedIndex
        }
    }
    /** Get value from old array by floating index. If index falls in between real indices, it takes
     * into account both values by a factor. */
    fun linearInterpolation(dataList: Array<Short>, index: Double): Short {
        val origI1 = ensureInBounds(floor(index), dataList.size).toInt()
        val origI2 = ensureInBounds(ceil(index), dataList.size).toInt()
        val w = index - origI1
        val val1 = dataList[origI1] * (1-w)
        val val2 = dataList[origI2] * w
        return round(val1 + val2).toInt().toShort()
    }

    // also, 1:1 and fill rest with last. If to shorter, just cut off
}