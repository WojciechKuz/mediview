package transform3d

//import org.jetbrains.kotlinx.multik.api.d1array
//import org.jetbrains.kotlinx.multik.api.d3array
//import org.jetbrains.kotlinx.multik.api.mk
//import org.jetbrains.kotlinx.multik.api.ndarray
//import org.jetbrains.kotlinx.multik.ndarray.data.D3Array
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.rotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.collections.flatten
import kotlin.collections.get
import kotlin.collections.toTypedArray
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sin

/** @param array default it's Z.Y.X */
class ArrayOps(
    /** array combining Z.Y.X. Size must be specified. Z size is deduced. */
    var array: ShortArray, initialWidth: Int, initialHeight: Int,
) {
    private operator fun ShortArray.get(start: Int, endInclusive: Int) = this@get.sliceArray(start..endInclusive)
    private fun getArrayPart(start: Int, length: Int) = array.sliceArray(start until start+length)
    var size: MySize3 = MySize3(
        initialWidth,
        initialHeight,
        array.size/initialWidth/initialHeight,
    )

    val whd: WidthHeightDepth
            get() = size.toWhd()
            //private set(value) { size = MySize3(value) }

    // Builder OK, but create 3d array of some library!
    class Array3DBuilder() {
        private var list: MutableList<ShortArray> = mutableListOf()
        fun getList() = list.toList()
        /*var printed = 0
        fun sayOnce(say: () -> Unit) {
            if(printed==0) {
                say()
                printed++
            }
        }*/

        /** returns itself */
        fun add(imageAndData: ImageAndData<ShortArray>) = addSA(imageAndData.imageArray)
        /** returns itself */
        fun add(imageAndData: Array<Short>): Array3DBuilder {
            list.add(imageAndData.toShortArray())
            return this
        }
        /** returns itself */
        fun addSA(imageAndData: ShortArray): Array3DBuilder {
            list.add(imageAndData)
            return this
        }
        /** add all image and data. returns itself */
        fun addAllIAD(imageAndDataList: List<ImageAndData<ShortArray>>) = addAllSA(imageAndDataList.map { it.imageArray })
        /** returns itself */
        fun addAll(imageList: List<Array<Short>>): Array3DBuilder {
            list.addAll(imageList.map { it.toShortArray() })
            return this
        }
        /** returns itself */
        fun addAllSA(imageList: List<ShortArray>): Array3DBuilder {
            list.addAll(imageList)
            return this
        }
        fun create(width: Int, height: Int): ArrayOps {
            val indi3 = Indices3(MySize3(width, height, list.size))
            val indi2 = Indices2(MySize2(width, height))
            return ArrayOps(
                ShortArray(indi3.size.total) { zyxi ->
                    val z = indi3.absoluteToZ(zyxi)
                    val yx = indi2.absoluteIndexOf2(indi3.absoluteToX(zyxi), indi3.absoluteToY(zyxi))
                    list[z][yx]
                },
                width, height
            )
            /*
            Array3D(list.size) { zi ->
                val splitArr = list[zi].splitTo(rowWidth) // list[zi] is Array<Short> (yx)
                //sayOnce { println("${list.size} times split array from ${list[zi].size} to ${splitArr[0].size}x${splitArr.size} using row width of $rowWidth.") }
                splitArr
            }
            */
        }
    }


    companion object {
        /** For `array[x][y]` return `array[y][x]` on two topmost arrays. */
        @Suppress("KDocUnresolvedReference")
        inline fun <reified T> Array<Array<T>>.rotate(): Array<Array<T>> {
            return Array<Array<T>>(this[0].size) { j -> // sub-arr size
                Array<T>(this.size) { i ->     // main-arr size
                    this[i][j]
                }
            }
        }

        // those 'inline' and 'reified' keywords are to prevent type erasing on JVM
        /** Think of it as dividing topmost 1D array to 2D array.
         * @param pieceLength is sort of rowLength
         * @returns row-array(y) of array(x) of T */
        private inline fun <reified T> Array<T>.splitTo(pieceLength: Int): Array<Array<T>> {
            val pieceCount = this.size / pieceLength
            return Array<Array<T>>(pieceCount) { mainArrI ->
                Array<T>(pieceLength) { pieceArrI ->
                    this[mainArrI * pieceLength + pieceArrI]
                }
            }
        }

        //private inline fun <reified T> Array<Array<T>>.flattenToTyped(): Array<T> = this.flatten().toTypedArray()

        inline fun <reified T> List<Array<T>>.flattenListToArray(): Array<T> {
            val indi2 = Indices2(MySize2(this[0].size, this.size))
            return Array<T>(indi2.size.total) { lai ->
                this[indi2.absoluteToY(lai)][indi2.absoluteToX(lai)]
            }
        }

        /** Same as flattenToTyped() before, but perhaps faster */
        inline fun <reified T> Array<Array<T>>.flattenArray(): Array<T> {
            val indi2 = Indices2(MySize2(this[0].size, this.size))
            return Array<T>(indi2.size.total) { lai ->
                this[indi2.absoluteToY(lai)][indi2.absoluteToX(lai)]
            }
        }

        fun List<ShortArray>.flattenListToShortArray(): ShortArray {
            val indi2 = Indices2(MySize2(this[0].size, this.size))
            return ShortArray(indi2.size.total) { lai ->
                this[indi2.absoluteToY(lai)][indi2.absoluteToX(lai)]
            }
        }

        /*fun Array<Short>.myToShortArray(): ShortArray {
            return ShortArray(this.size) { lai ->
                this[lai]
            }
        }*/

        // new doOnSecond
        inline fun <reified T, reified U> Array<T>.forSecond(alterSecond: (T) -> U): Array<U> {
            return Array<U>(this.size) { lai ->
                alterSecond(this[lai])
            }
        }

        /** Same as map, but returns Array<U> instead of List<U>. Uses operations on collections */
        private inline fun <reified T, reified U> Array<T>.doOnSecond(alterSecond: (T) -> U): Array<U>
            = this.map(alterSecond).toTypedArray()

        /** Same as map, but returns Array<U> instead of List<U>. Uses operations on collections */
        private inline fun <reified T, reified U> Array<T>.indexedDoOnSecond(alterSecond: (Int, T) -> U): Array<U>
                = this.mapIndexed(alterSecond).toTypedArray()

        /** reverse order of elements in array. Uses operations on collections */
        private inline fun <reified T> Array<T>.inverse(): Array<T>
            = this.mapIndexed { index, _ -> this[this.size - index - 1] }.toTypedArray()
    }

    /** map pixels. Writes to internal list. Returns itself. */
    suspend fun transformEachPixel(transform: (Short) -> Short): ArrayOps {
        val indi3 = Indices3(size)
        val jobList = mutableListOf<Job>()
        for(zi in 0 until size.depth) {
            val job = CoroutineScope(Dispatchers.Default).launch {
                for (yi in 0 until size.height) {
                    for(xi in 0 until size.width) {
                        val absi = indi3.absoluteIndexOf3(xi, yi, zi)
                        if(absi == 15367) println("Transforming ${array[absi]} to ${transform(array[absi])}")
                        array[absi] = transform(array[absi])
                    }
                }
            }
            jobList.add(job)
        }
        jobList.joinAll()
        return this
    }

    /** from flat ZYX array creates YX.Z array to perform some operation on this z-rows. Then transforms it back to ZYX and writes to array
     * In lambda first parameter is z-ShortArray, second is yxi value (in Indices2 of width and height) */
    suspend fun doSomethingOnYXArrayOfZArrays(doOnZArr: (ShortArray, Int) -> ShortArray): ArrayOps {
        // CoroutineScope(Dispatchers.Default). launch or async

        // Z.Y.X
        val indi2 = Indices2(MySize2(size.width,size.height))
        val indi3 = Indices3(size)

        var arrayOfZArrays: Array<ShortArray>

        // Not image, it's YX.Z
        val indexArray = Array(indi2.size.height) { yi -> yi }
        arrayOfZArrays = indexArray.map { yi ->
            CoroutineScope(Dispatchers.Default).async {
                Array<ShortArray>(size.width) { xi ->
                    val zArr = ShortArray(size.depth) { z ->
                        array[indi3.absoluteIndexOf3(xi, yi, z)]
                    }
                    doOnZArr(zArr, indi2.absoluteIndexOf2(xi, yi))
                }
            }
        }.awaitAll().flattenListToArray()

        val newSize = MySize3(indi3.size.width, indi3.size.height, arrayOfZArrays[0].size)
        //val newIndi3 = Indices3(newSize)

        // y and x size didn't change. We can still use indi2 for new array
        val indexArray2 = Array(newSize.depth) { zi -> zi } // top
        val newShArr = indexArray2.map { zi ->
            CoroutineScope(Dispatchers.Default).async {
                ShortArray(indi2.size.total) { yxi ->
                    arrayOfZArrays[yxi][zi]
                }
            }
        }.awaitAll().flattenListToShortArray()
        size = newSize
        array = newShArr
        return this
    }

    /*
    /** Interpolate (rescale) pixels. Writes to internal list. Returns itself.
     * `!!!` Interpolate changes the amount of images, so you can't match image with its data after this operation is performed! */
    fun interpolateOverZ(scaleFactor: Double, rescale: (ShortArray, Double) -> ShortArray): ArrayOps {
        return doSomethingOnYXArrayOfZArrays { zArr, _ -> rescale(zArr, scaleFactor) }
    }

    /** Negative rotationOrigin means use `y_size/2`, not `y_size - 1`. */
    fun shearByGantry(gantryAngle: Double, move: (ShortArray, Double) -> ShortArray, rotationOrigin: Int = -1): ArrayOps {

        val rotCt = if(rotationOrigin < 0)
            size.height - 1 /*oldform[0].size/width - 1*/ /*whd.height/2*/
        else
            rotationOrigin

        val radiansAngle = Math.toRadians(gantryAngle)
        val moveRow = { yi: Int ->
            sin(radiansAngle) * (rotCt - yi)
        }

        val indi2 = Indices2(MySize2(size.width,size.height))
        return doSomethingOnYXArrayOfZArrays { zArr, yxi ->
            val rowShear = moveRow(indi2.absoluteToY(yxi))
            move(zArr, rowShear)
        }
    }
    */

    /** Has to be executed in `doSomethingOnYXArrayOfZArrays`. pass this function to doSomethingOnYXArrayOfZArrays.
     * Interpolate (rescale) pixels. Writes to internal list. Returns itself.
     * `!!!` Interpolate changes the amount of images, so you can't match image with its data after this operation is performed! */
    fun prepareLambdaForScaleZ(scaleFactor: Double, rescale: (ShortArray, Double) -> ShortArray): (ShortArray, Int) -> ShortArray {
        return { zArr, _ -> rescale(zArr, scaleFactor) }
    }
    /** Has to be executed in `doSomethingOnYXArrayOfZArrays`. pass this function to doSomethingOnYXArrayOfZArrays.
     * Negative rotationOrigin means use `y_size/2`, not `y_size - 1`. */
    fun prepareLambdaForShearByGantry(gantryAngle: Double, move: (ShortArray, Double) -> ShortArray, rotationOrigin: Int = -1): (ShortArray, Int) -> ShortArray {
        val rotCt = if(rotationOrigin < 0)
            size.height - 1 /*oldform[0].size/width - 1*/ /*whd.height/2*/
        else
            rotationOrigin

        val radiansAngle = Math.toRadians(gantryAngle)
        val moveRow = { yi: Int ->
            sin(radiansAngle) * (rotCt - yi)
        }

        val indi2 = Indices2(MySize2(size.width,size.height))
        return { zArr, yxi ->
            val rowShear = moveRow(indi2.absoluteToY(yxi))
            move(zArr, rowShear)
        }
    }

/* // While convenient to read, it really hurt performance:
    // 0 step:
//    var zyx: Array3D
//        get() = array3d
//        set(value) { array3d = value } // default
    // 1 step:
//    var yzx: Array3D
//        get() = zyx.rotate()
//        set(yzxVal) { zyx = yzxVal.rotate() }
//    val zxy: Array3D
//        get() = zyx.doOnSecond{ yxArr -> yxArr.rotate() }
//        // set
    // 2 step:
//    val xzy: Array3D
//        get() = zxy.rotate()
//        // set
//    var yxz: Array3D
//        get() = yzx.doOnSecond { zxArr -> zxArr.rotate() }
//        set(yxzVal) { yzx = yxzVal.doOnSecond { xzArr -> xzArr.rotate() } }
    // 3 step:
//    val xyz: Array3D
//        get() = yxz.rotate()
//        // set
*/

    private fun absoluteIndexOf3(x: Int, y: Int, z: Int) = (z * size.height + y) * size.width + x

    /** ShortArray of flat YX for Z index */ // zyx image - OK, EZ. Already is
    fun getFlatYXforZ(depthZ: Int): ShortArray {
        val indi3 = Indices3(size)
        val start = indi3.absoluteIndexOf3(0, 0, depthZ)
        val endInclusive = indi3.absoluteIndexOf3(0, 0, depthZ + 1) - 1
        return array[start, endInclusive]
    }

    /** ShortArray of flattened YZ for X index */   // xyz
    fun getFlatYZforX(depthX: Int): ShortArray {
        val sizeOf2 = size.height * size.depth
        val absoluteIndexOf1 = {
            yzi: Int ->
            val z = yzi % size.depth
            val y = yzi / size.depth
            absoluteIndexOf3(depthX, y, z)
        }
        return ShortArray(sizeOf2) { yzi -> array[absoluteIndexOf1(yzi)]}
    }

    /** ShortArray of flattened XZ for Y index */   // yxz
    fun getFlatXZforY(depthY: Int): ShortArray {
        val absoluteIndexOf1 = {xzi: Int ->
            val z = xzi % size.depth
            val x = xzi / size.depth
            absoluteIndexOf3(x, depthY, z)
        }
        val sizeOf2 = size.width * size.depth
        return ShortArray(sizeOf2) { xzi -> array[absoluteIndexOf1(xzi)]}
    }

    private fun ensureInBounds3D(vec: Float3): Float3 {
        fun ensure(index: Float, upperBound: Int): Float {
            return when {
                index < 0f -> 0f
                index > (upperBound - 1f) -> (upperBound - 1f)
                else -> index
            }
        }
        return Float3(
            ensure(vec.x, size.width),
            ensure(vec.y, size.height),
            ensure(vec.z, size.depth)
        )
    }
    /** Performs Trilinear interpolation */
    private fun valueAtIndex3D(vec: Float3): Short {
        val ensVec = ensureInBounds3D(vec)
        val xIndices = arrayOf( floor(ensVec.x).toInt(), ceil(ensVec.x).toInt() )
        val yIndices = arrayOf( floor(ensVec.y).toInt(), ceil(ensVec.y).toInt() )
        val zIndices = arrayOf( floor(ensVec.z).toInt(), ceil(ensVec.z).toInt() )
        val dx = ensVec.x - xIndices[0]
        val dy = ensVec.y - yIndices[0]
        val dz = ensVec.z - zIndices[0]
        val origValueArray = Array(2) { z ->
            Array(2) { y ->
                Array<Short>(2) { x ->
                    array[absoluteIndexOf3(xIndices[x], yIndices[y], zIndices[z])]
                }
            }
        }
        // probably can be merged with creation of origValueArray, but it's more readable this way
        val collapsedYXArray = origValueArray.forSecond { yxArr ->
            val collapsedXArr = yxArr.forSecond { xArr ->
                Interpolation.linearInterpolation(xArr, dx.toDouble())
            }
            Interpolation.linearInterpolation(collapsedXArr, dy.toDouble())
        }
        val interpolatedValue = Interpolation.linearInterpolation(collapsedYXArray, dz.toDouble())

        return interpolatedValue
    }

    /** For 2D index array (Float4) get 2D array of values. */
    fun valuesAtIndices(indiciesArr: Array<Array<Float4>>): Array<Array<Short>> {
        return indiciesArr.onIndices { vec4 ->
            val vec3 = vec4[1, 2, 3]
            valueAtIndex3D(vec3)
        }
    }

    private fun indicesAtZSlice(zi: Float) = Array(size.height) { yi ->
        Array(size.width) { xi ->
            Float4(zi, yi.toFloat(), xi.toFloat(), 1f)
        }
    }


    // for each row: in row: map values fl4 -> fl4
    /** Does not overwrite anything, just returns
     * For each index (Float4) in 2D array of indices, perform operation */
    private inline fun <reified T> Array<Array<Float4>>.onIndices(operation: (Float4) -> T) =
        this.forSecond { it.forSecond { fl4 -> operation(fl4) } }

    /** @param depth floating pixels
     * @param yzAngle
     * @param xzAngle both are angle in degrees
     * @return 2D array of indices (Float4) */
    fun getAnyOrientationSlice(depth: Float, yzAngle: Double, xzAngle: Double): Array<Array<Float4>> {
        val center = Float4(
            size.width / 2f,
            size.height / 2f,
            size.depth / 2f,
            1f
        )
        //val xAxisAngle = Math.toRadians(yzAngle).toFloat()
        //val yAxisAngle = Math.toRadians(xzAngle).toFloat()
        val slice = indicesAtZSlice(depth)
        val rotMX = rotation(axis = Float3(x = Config.rotateDirection.x), angle = yzAngle.toFloat()) // takes degrees
        val rotMY = rotation(axis = Float3(y = Config.rotateDirection.y), angle = xzAngle.toFloat())
        val rotatedSlice = slice.onIndices { fl4 ->
            val step1 = fl4 - center    // translate, center -> 0,0,0
            val step2 = rotMX * step1   // rotate around X
            val step3 = rotMY * step2   // rotate around Y
            val step4 = step3 + center  // translate, 0,0,0 -> center
            step4
        }
        return rotatedSlice
    }


    /** default is Z.Y.X Use only for prototyping and debugging. Expensive to compute! */
    var array3d: Array<Array<Array<Short>>>
        get() = array.toTypedArray().splitTo(size.width).splitTo(size.height)
        set(value) {
            size = MySize3(
                value[0][0].size,
                value[0].size,
                value.size
            )
            array = value.flattenArray().flattenArray().toShortArray()
        }

    /** only for debugging */
    fun checkIfAllTheSame(): Boolean {
        val arr = array3d.rotate().forSecond { zxArr -> zxArr.rotate() }.flattenArray() // y.x.z -> yx.z
        return arr.all { zArr -> zArr.all { it == zArr[0] } }
    }
    /** only for debugging */
    fun isSelectedPixelTheSame(doPrint: Boolean = false): Boolean {
        val arr = array3d.rotate().forSecond { zxArr -> zxArr.rotate() }.flattenArray() // y.x.z -> yx.z
        val zValsOfSelectedPx = arr[Config.selectedPixel].toList()
        if(doPrint)
            println("zValsOfSelectedPx: $zValsOfSelectedPx")
        return zValsOfSelectedPx.all { it == zValsOfSelectedPx[0] }
    }
}
typealias Array1D = Array<Short>
typealias Array2D = Array<Array<Short>>
typealias Array3D = Array<Array<Array<Short>>>
