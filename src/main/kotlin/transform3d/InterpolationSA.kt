package transform3d

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

/** Same as Interpolation, but on ShortArray instead of Array<Short>. This is more efficient. */
object InterpolationSA {
    /** Performs Nearest Neighbor Interpolation
     * @param scaleFactor scale list by a factor. Values greater than 1 increase, lower decrease */
    fun rescaleNN(dataList: ShortArray, scaleFactor: Double): ShortArray {
        val targetSize = (dataList.size * scaleFactor).toInt() // round? ceil? floor?
        val target = ShortArray(targetSize) { ti ->
            dataList[
                round( ti / scaleFactor ).toInt()
            ]
        }
        return target
    }

    /** Performs Nearest Neighbor Interpolation. */
    fun rescaleNN(dataList: ShortArray, depth: Int, targetDepth: Int): ShortArray
            = rescaleNN(dataList, depth.toDouble() / targetDepth)

    /** Performs bilinear interpolation
     * @param scaleFactor scale list by a factor. Values greater than 1 increase, lower decrease */
    fun rescaleBL(dataList: ShortArray, scaleFactor: Double): ShortArray {
        val targetSize = (dataList.size * scaleFactor).toInt() // round? ceil? floor?
        val target = ShortArray(targetSize) { ti ->
            val oldI = ti / scaleFactor
            linearInterpolation(dataList, oldI)
        }
        return target
    }

    /** Performs bilinear interpolation */
    fun rescaleBL(dataList: ShortArray, depth: Int, targetDepth: Int): ShortArray {
        // depth is actually list size, lol
        val scale = depth / targetDepth.toDouble()
        return rescaleBL(dataList, scale)
    }

    /** Moves array by given floating amount. Uses Nearest neighbor to get value. */
    fun moveNN(dataList: ShortArray, moveBy: Double): ShortArray {
        val target = ShortArray(dataList.size) { ti ->
            dataList[
                round( ti - moveBy ).toInt()
            ]
        }
        return target
    }

    /** Moves array by given floating amount. Uses Bilinear to get value.
     * Values that exceed the length are discarded, empty space is filled by nearest value */
    fun moveBL(dataList: ShortArray, moveBy: Double): ShortArray {
        val target = ShortArray(dataList.size) { ti ->
            val oldI = ti - moveBy
            linearInterpolation(dataList, oldI)
        }
        return target
    }

    /** fillTo should be integer. If initial array is too short, pad it to desired size */
    fun fillTo(dataList: ShortArray, targetSize: Int, padValue: Short): ShortArray {
        val padStart = (targetSize - dataList.size) / 2
        val target = ShortArray(targetSize) { i -> when {
                i in (padStart until padStart+dataList.size) -> dataList[i - padStart]
                else -> padValue
            }
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
    fun linearInterpolation(dataList: ShortArray, index: Double): Short {
        val origI1 = ensureInBounds(floor(index), dataList.size).toInt()
        val origI2 = ensureInBounds(ceil(index), dataList.size).toInt()
        val w = index - origI1
        val val1 = dataList[origI1] * (1-w)
        val val2 = dataList[origI2] * w
        return round(val1 + val2).toInt().toShort()
    }
    /** For interpolating between two values. If index falls in between real indices, it takes
     * into account both values by a factor. Does not check bounds */
    fun manualLinearInterpolationOf2(data0: Short, data1: Short, index: Double): Short =
        round( (data0 * (1-index)) + (data1 * index) ).toInt().toShort()

    /** Used in interpolating angle depending on which frame it is.
     * @param index in range 0 to 1 */ // alternative: data0 + index*(data1-data0)
    fun interpolate2Values(data0: Float, data1: Float, index: Float): Float = ( (data0 * (1-index)) + (data1 * index) )
}