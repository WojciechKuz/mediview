package transform3d



class MySize3(val width: Int, val height: Int, val depth: Int) {
    constructor(whd: WidthHeightDepth): this(whd.width, whd.height, whd.depth)
    val total: Int; get() = width * height * depth
    fun toWhd() = WidthHeightDepth(width, height, depth)
    override fun toString(): String {
        return "(w:$width, h:$height, d:$depth)"
    }
}
class MySize2(val width: Int, val height: Int) {
    val total: Int; get() = width * height
    override fun toString(): String {
        return "(w:$width, h:$height)"
    }
}
class Indices3(val size: MySize3) {
    constructor(size: WidthHeightDepth): this(MySize3(size))
    fun absoluteIndexOf3(x: Int, y: Int, z: Int) = (z * size.height + y) * size.width + x
    val absoluteSize = size.width * size.height * size.depth
    val absoluteToX = {absIndex: Int -> absIndex%size.width }
    val absoluteToY = {absIndex: Int -> absIndex/size.width%size.height }
    val absoluteToZ = {absIndex: Int -> absIndex/size.width/size.height }
    fun valueAtAbsIndexInArray(array: Array<Array<Array<Short>>>, absIndex: Int) =
            array [absIndex/size.width/size.height] [absIndex/size.width%size.height] [absIndex%size.width]
    fun valueAtAbsIndexInArray(array: ShortArray, absIndex: Int) =
        array[absIndex]
}
/** whd.depth is ignored */
class Indices2(val size: MySize2) {
    fun absoluteIndexOf2(x: Int, y: Int) = y * size.width + x
    val absoluteSize = size.width * size.height
    val absoluteToX = {absIndex: Int -> absIndex%size.width }
    val absoluteToY = {absIndex: Int -> absIndex/size.width }
    fun valueAtAbsIndexInArray(array: Array<Array<Array<Short>>>, absIndex: Int) =
        array [absIndex/size.width/size.height] [absIndex/size.width%size.height] [absIndex%size.width]
    fun valueAtAbsIndexInArray(array: ShortArray, absIndex: Int) =
        array[absIndex]
}


/*
class DimensionOrder private constructor(private val dimensions: List<Dimensions>) {
    private constructor(vararg dims: Dimensions) : this(dims.toList())
    public constructor(vararg dims: DimensionOrder) : this(
        dims.toList().reduce { acc, d -> acc + d }.dimensions
    )
    operator fun plus(otherDim: DimensionOrder): DimensionOrder {
        return DimensionOrder(this.dimensions + otherDim.dimensions )
    }
    private enum class Dimensions { X, Y, Z }
    companion object {
        val Dx = DO(DimensionOrder.Dimensions.X)
        val Dy = DO(DimensionOrder.Dimensions.Y)
        val Dz = DO(DimensionOrder.Dimensions.Z)
    }
}
val Dx = DO.Dx
val Dy = DO.Dy
val Dz = DO.Dz
typealias DO = DimensionOrder
*/

// /** -1 means not set */
// class TripleIndex(var x: Int = -1, var y: Int = -1, var z: Int = -1) {}

/*
class VirtualArray3D(val data: ShortArray, val whd: WidthHeightDepth, val tripleIndex: TripleIndex) {
    fun toVirtualArray1D() = VirtualArray1D(data, whd, tripleIndex)
    private val absoluteIndexOf3 = {x: Int, y: Int, z: Int -> (z * whd.height + y) * whd.width + x }

    operator fun get(x: Int, y: Int, z: Int): Short = data[absoluteIndexOf3(x, y, z)]
    operator fun set(x: Int, y: Int, z: Int, value: Short) { data[absoluteIndexOf3(x, y, z)] = value }

    /** Get 1D array. Leave one parameter unspecified */
    fun get1D(x: Int = -1, y: Int = -1, z: Int = -1) = when {
        (x == -1) && (y != -1) && (z != -1) -> ShortArray(whd.width) { i ->
            data[absoluteIndexOf3(i, y, z)]
        }  // width const
        (x != -1) && (y == -1) && (z != -1) -> ShortArray(whd.height) { i ->
            data[absoluteIndexOf3(x, i, z)]
        }  // height const
        (x != -1) && (y != -1) && (z == -1) -> ShortArray(whd.depth) { i ->
            data[absoluteIndexOf3(x, y, i)]
        }  // depth const
        else -> {throw Exception("get2 should have 1 parameter (one of x,y,z) set to -1") }
    }

    /** Get 1D array. Leave one parameter unspecified */
    fun get1DArray(ti: TripleIndex) = get1D(ti.x, ti.y, ti.z)
    fun set1D(ti: TripleIndex, value: ShortArray) = set1D(ti.x, ti.y, ti.z, value)

    /*/** Get 1D array. Leave one parameter unspecified */
    fun get1DArray(x: Int = -1, y: Int = -1, z: Int = -1): VirtualArray1D = when {
        (x == -1) && (y != -1) && (z != -1) -> VirtualArray1D(ShortArray(whd.width) { i ->
            data[absoluteIndexOf3(i, y, z)]
        }, WidthHeightDepth(whd.width, -1, -1), TripleIndex(x, y, z))
        (x != -1) && (y == -1) && (z != -1) -> VirtualArray1D(ShortArray(whd.width) { i ->
            data[absoluteIndexOf3(x, i, z)]
        }, WidthHeightDepth(-1, whd.height, -1), TripleIndex(x, y, z))
        (x != -1) && (y != -1) && (z == -1) -> VirtualArray1D(ShortArray(whd.width) { i ->
            data[absoluteIndexOf3(x, y, i)]
        }, WidthHeightDepth(-1, -1, whd.depth), TripleIndex(x, y, z))
        else -> {
            throw Exception("get2 should have 1 parameter (one of x,y,z) set to -1")
        }
    }*/

    /** Set 1D array */
    fun set1D(x: Int = -1, y: Int = -1, z: Int = -1, value: ShortArray) = when {
        (x == -1) && (y != -1) && (z != -1) -> {
            value.forEachIndexed { i, _ -> data[absoluteIndexOf3(i, y, z)] }
        }  // width const
        (x != -1) && (y == -1) && (z != -1) -> {
            value.forEachIndexed { i, _ ->
                data[absoluteIndexOf3(x, i, z)]
            }
        }
        (x != -1) && (y != -1) && (z == -1) -> {
            value.forEachIndexed { i, _ ->
                data[absoluteIndexOf3(x, y, i)]
            }
        }
        else -> {throw Exception("set2 should have 1 parameter (one of x,y,z) set to -1") }
    }

    /*
    /** Get 2D array */
    fun get1(x: Int = -1, y: Int = -1, z: Int = -1): ShortArray = when {
        (x == -1) && (y == -1) && (z != -1) -> ShortArray(whd.width*whd.height) { i -> // yx
            data[absoluteIndexOf3(i, y, z)]
        }
        (x != -1) && (y == -1) && (z != -1) -> ShortArray(whd.height*whd.depth) { i -> // yz
            data[absoluteIndexOf3(x, i, z)]
        }
        (x != -1) && (y != -1) && (z == -1) -> ShortArray(whd.width*whd.depth) { i -> // xz
            data[absoluteIndexOf3(x, y, i)]
        }
        else -> {throw Exception("get2 should have 1 parameter (one of x,y,z) set to -1") }
    }
    /** Set 2D array */
    fun set1(x: Int = -1, y: Int = -1, z: Int = -1, value: ShortArray) {
        data[absoluteIndexOf3(x, y, z)] = value
    }*/
}
class VirtualArray2D(val data: ShortArray, val whd: WidthHeightDepth, val tripleIndex: TripleIndex) {}

class VirtualArray1D(val data: ShortArray, val whd: WidthHeightDepth, val tripleIndex: TripleIndex) {
    operator fun get(start: Int, endInclusive: Int) = data.sliceArray(start..endInclusive)
    operator fun get(i: Int) = data[i]
    fun toVirtualArray3D() = VirtualArray3D(data, whd, tripleIndex)
}
 */

/* // This one is nearly completed, unlike other comments
enum class Dimensions { X, Y, Z }
val Dx = Dimensions.X
val Dy = Dimensions.Y
val Dz = Dimensions.Z
//fun multiDimension(vararg dimensions: Dimensions): List<Dimensions> = listOf(*dimensions)

class VirtualArray3D(val array: Array3D, startingOrder: List<Dimensions> = listOf(Dz, Dy, Dx)) {

    private val realSize = WidthHeightDepth(
        array[0][0].size,
        array[0].size,
        array.size,
    )
    val realOrder = listOf(Dz, Dy, Dx)

    var order = startingOrder
    private var realIndices = IntArray(order.size) { i -> -1 } // Ordered as RealOrder. -1 means whole dimension

    /** removes first dimension in (virtual) order */
    private fun List<Dimensions>.getDimension(): List<Dimensions> {
        val getDims = order.toMutableList(); getDims.removeFirst()
        return getDims.toList()
    }

    /** get VirtualArray with access to first dimension. */
    operator fun get(i: Int): VirtualArray3D {
        val realIndex = realOrder.indexOf(order.first())
        val va = VirtualArray3D(array, order.getDimension())
        va.realIndices[realIndex] = i
        return va
    }

    fun canGet() = realIndices.all { it != -1 }
    fun get(): Short {
        if (!canGet()) {
            throw Exception("Can't get Array3D element, as not all indices were set. Check with canGet() before calling get()")
        }
        return array[realIndices[0]][realIndices[1]][realIndices[2]]
    }
}*/

/*
open class VirtualArray(val array: Array3D, startingOrder: List<Dimensions> = listOf(Dz, Dy, Dx)) {

    /*private constructor(array: Array3D, startingOrder: List<Dimensions>, indices: IntArray) : this(array, startingOrder) {
        optionalIndices = indices
    }*/
    //private var mutableOrder: MutableList<Dimensions> = startingOrder.toMutableList()
    //var order: MutableList<Dimensions>; get() = mutableOrder; set(value) { mutableOrder = value.toMutableList() }

    private val realSize = WidthHeightDepth(
        array[0][0].size,
        array[0].size,
        array.size,
    )

    val realOrder = listOf(Dz, Dy, Dx)

    var order = startingOrder
    private var realIndices = IntArray(order.size) { i -> -1 } // Ordered as RealOrder. -1 means whole dimension

    /** removes first dimension in (virtual) order */
    private fun List<Dimensions>.getDimension(): List<Dimensions> {
        val getDims = order.toMutableList(); getDims.removeFirst()
        return getDims.toList()
    }

    /** get VirtualArray with access to first dimension. */
    protected operator fun get(i: Int): VirtualArray {
        val realIndex = realOrder.indexOf(order[0])
        val va = VirtualArray(array, order.getDimension())
        va.realIndices[realIndex] = i
        return va
    }
}
class VirtualArray3D(array: Array3D, startingOrder: List<Dimensions> = listOf(Dz, Dy, Dx)): VirtualArray(array) {
    fun get(): {
        //
    }
}
class VirtualArray2D(val array: Array3D, startingOrder: List<Dimensions>) {
    //
}
class VirtualArray1D(val array: Array3D, startingOrder: List<Dimensions>) {
    //
}*/
