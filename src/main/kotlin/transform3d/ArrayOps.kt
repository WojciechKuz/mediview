package transform3d

//import org.jetbrains.kotlinx.multik.api.d1array
//import org.jetbrains.kotlinx.multik.api.d3array
//import org.jetbrains.kotlinx.multik.api.mk
//import org.jetbrains.kotlinx.multik.api.ndarray
//import org.jetbrains.kotlinx.multik.ndarray.data.D3Array
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.rotation
import kotlin.collections.flatten
import kotlin.collections.toTypedArray
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sin

/** @param array default it's Z.Y.X */
class ArrayOps(
    /** array combining Z.Y.X. Size must be specified. Z is deduced. */
    var array: ShortArray, initialWidth: Int, initialHeight: Int,
) {
    private operator fun ShortArray.get(start: Int, endInclusive: Int) = this@get.sliceArray(start..endInclusive)
    private fun getArrayPart(start: Int, length: Int) = array.sliceArray(start until start+length)
    var size: MySize3 = MySize3(
        initialWidth,
        initialHeight,
        array.size/initialWidth/initialHeight,
    )

    /** default is Z.Y.X */
    var array3d: Array<Array<Array<Short>>>
        get() = array.toTypedArray().splitTo(size.width).splitTo(size.height)
        set(value) {
            size = MySize3(
                value[0][0].size,
                value[0].size,
                value.size
            )
            array = value.flattenToTyped().flattenToTyped().toShortArray()
        }
        /*private set(value) {
            field = value
            whd = WidthHeightDepth(
                array3d[0][0].size,
                array3d[0].size,
                array3d.size,
            )
            //println("modified ArrayOps, sizes: $whd")
        }*/
    var whd: WidthHeightDepth
            get() = WidthHeightDepth(size.width, size.height, size.depth);
            private set(value) { size = MySize3(value.width, value.height, value.depth) }

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
        /** For `array[x][y]` return `array[y][x]` (on two topmost arrays) */
        @Suppress("KDocUnresolvedReference")
        inline fun <reified T> rotateArray(mainArray: Array<Array<T>>): Array<Array<T>> {
            return Array<Array<T>>(mainArray[0].size) { j -> // sub-arr size
                Array<T>(mainArray.size) { i ->     // main-arr size
                    mainArray[i][j]
                }
            }
        }
        /** For `array[x][y]` return `array[y][x]` */
        @Suppress("KDocUnresolvedReference")
        inline fun <reified T> Array<Array<T>>.rotate() = rotateArray<T>(this)

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

        // flatten already is
        private inline fun <reified T> Array<Array<T>>.flattenToTyped(): Array<T> = this.flatten().toTypedArray()

        /** Same as map, but returns Array<U> instead of List<U> */
        private inline fun <reified T, reified U> Array<T>.doOnSecond(alterSecond: (T) -> U): Array<U>
            = this.map(alterSecond).toTypedArray()

        /** Same as map, but returns Array<U> instead of List<U> */
        private inline fun <reified T, reified U> Array<T>.indexedDoOnSecond(alterSecond: (Int, T) -> U): Array<U>
                = this.mapIndexed(alterSecond).toTypedArray()

        /** reverse order of elements in array */
        private inline fun <reified T> Array<T>.inverse(): Array<T>
            = this.mapIndexed { index, _ -> this[this.size - index - 1] }.toTypedArray()
    }
    /** map pixels. Writes to internal list. Returns itself. */
    fun transformEachPixel(transform: (Short) -> Short): ArrayOps {
        //array.forEach { yxArr -> yxArr.forEach { xArr -> xArr.map { transform(it) }.toTypedArray() } }
        for(yxArr in array3d) { for (xArr in yxArr) { for(xi in xArr.indices) { xArr[xi] = transform(xArr[xi]) } } }
        return this
    }

    /** Interpolate (rescale) pixels. Writes to internal list. Returns itself.
     * `!!!` Interpolate changes the amount of images, so you can't match image with its data after this operation is performed! */
    fun interpolateOverZ(scaleFactor: Double, rescale: (ShortArray, Double) -> ShortArray): ArrayOps {
        // Z.Y.X
        val indi2 = Indices2(MySize2(size.width,size.height))
        val indi3 = Indices3(size)

        // Not image, it's XY.Z
        val arrayOfZArrays = Array<ShortArray>(indi2.size.total) { yxi ->
            val zArr = ShortArray(size.depth) { z ->
                array[ indi3.absoluteIndexOf3(indi2.absoluteToX(yxi), indi2.absoluteToY(yxi), z) ]
                //valueAtAbsoluteIndex()
            }
            rescale(zArr, scaleFactor)
        }
        val newSize = MySize3(indi3.size.width, indi3.size.height, arrayOfZArrays[0].size)
        val newIndi3 = Indices3(newSize)
        val newShArr = ShortArray(newSize.total) { zyxi ->
            val x = newIndi3.absoluteToX(zyxi)
            val y = newIndi3.absoluteToY(zyxi)
            val z = newIndi3.absoluteToZ(zyxi)
            val zArr = arrayOfZArrays[indi2.absoluteIndexOf2(x, y)]
            zArr[z]
        }
        size = newSize
        array = newShArr
        return this
        /*
        //val arraysOfZ = Indices3(whd).absoluteIndexOf3(x, y, )

        // here image means flattened(x, y)
        // Starting with listOf( image ), z.yx
        // 1. Transform it to yx.z
        // 2. interpolate over those z values
        // 3. transform back to listOf( image ), but with different z size. z.yx
        val interpolatedArray = oldform.rotate().doOnSecond { zPixels -> // same pixel on different images
            rescale(zPixels, scaleFactor)
        }.rotate()
        oldform = interpolatedArray // cost on write and read is 2 (2 full 3D array transformations)

        /*
        val replArr = Array3D(whd.depth) { zi ->
            Array2D(whd.height) {yi ->
                Array1D(whd.depth) { xi ->
                    //
                    0
                }
            }
        }
        */

        return this*/
    }

    /** Negative rotationOrigin means use `y_size/2`, not `y_size - 1`. */
    fun shearByGantry(gantryAngle: Double, width: Int, move: (ShortArray, Double) -> ShortArray, rotationOrigin: Int = -1): ArrayOps {

        val rotCt = if(rotationOrigin < 0)
            size.height - 1 /*oldform[0].size/width - 1*/ /*whd.height/2*/
        else
            rotationOrigin

        val radiansAngle = Math.toRadians(gantryAngle)
        val moveRow = { yi: Int ->
            sin(radiansAngle) * (rotCt - yi)
        }

        // Z.Y.X
        val indi2 = Indices2(MySize2(size.width,size.height))
        val indi3 = Indices3(size)

        // Not image, it's XY.Z
        val arrayOfZArrays = Array<ShortArray>(indi2.size.total) { yxi ->
            val zArr = ShortArray(size.depth) { z ->
                array[ indi3.absoluteIndexOf3(indi2.absoluteToX(yxi), indi2.absoluteToY(yxi), z) ]
                //valueAtAbsoluteIndex()
            }
            val rowShear = moveRow(indi2.absoluteToY(yxi))
            move(zArr, rowShear)
        }
        val newSize = MySize3(indi3.size.width, indi3.size.height, arrayOfZArrays[0].size)
        val newIndi3 = Indices3(newSize)
        val newShArr = ShortArray(newSize.total) { zyxi ->
            val x = newIndi3.absoluteToX(zyxi)
            val y = newIndi3.absoluteToY(zyxi)
            val z = newIndi3.absoluteToZ(zyxi)
            val zArr = arrayOfZArrays[indi2.absoluteIndexOf2(x, y)]
            zArr[z]
        }
        size = newSize
        array = newShArr
        return this
        /*
        /* For y.x.z array:
         * 1. compute move value for every row
         * 2. move row (x) in z axis
         * Warning! cost on write and read is 2 (2 full 3D array transformations)
         */
        val shearedArray = yxz.indexedDoOnSecond { yi, xzArray ->
            val rowShear = moveRow(yi)
            xzArray.doOnSecond { zPixels ->    // same pixel on different images
                move(zPixels, rowShear)
            }
        }
        yxz = shearedArray
        return this
         */
    }

    /*fun convertMultik(wdh: WidthHeightDepth): D3Array<Short> {
        // flatten array for Multik to load
        val asOneList = oldform.flatten()

        // perform conversion
        val array3d: D3Array<Short> = mk.d3array(wdh.width, wdh.height, wdh.depth) { i -> asOneList[i] }
        return array3d

    }*/

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

    private val absoluteIndexOf3 = {x: Int, y: Int, z: Int -> (z * size.height + y) * size.width + x }

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

    private fun indicesAtZSlice(zi: Float) = Array(size.height) { yi ->
        Array(size.width) { xi ->
            Float4(zi, yi.toFloat(), xi.toFloat(), 1f)
        }
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
        val xIndices = arrayOf( floor(ensVec.x).toInt(), ceil(ensVec.x).toInt())
        val yIndices = arrayOf(floor(ensVec.y).toInt(), ceil(ensVec.y).toInt())
        val zIndices = arrayOf(floor(ensVec.z).toInt(), ceil(ensVec.z).toInt())
        val dx = ensVec.x - xIndices[0]
        val dy = ensVec.y - yIndices[0]
        val dz = ensVec.z - zIndices[0]
        val origValueArray = Array(2) { z ->
            Array(2) { y ->
                Array<Short>(2) { x ->
                    array3d[zIndices[z]][yIndices[y]][xIndices[x]]
                }
            }
        }
        // probably can be merged with creation of origValueArray, but it's more readable this way
        val collapsedYXArray = origValueArray.doOnSecond { yxArr ->
            val collapsedXArr = yxArr.doOnSecond{ xArr ->
                Interpolation.linearInterpolation(xArr, dx.toDouble())
            }
            Interpolation.linearInterpolation(collapsedXArr, dy.toDouble())
        }
        val interpolatedValue = Interpolation.linearInterpolation(collapsedYXArray, dz.toDouble())

        return interpolatedValue
    }
    fun valuesAtIndices(indiciesArr: Array<Array<Float4>>): Array<Array<Short>> {
        return indiciesArr.doOnSecond { xArr ->
            xArr.doOnSecond { vec4 ->
                val vec3 = vec4[1, 2, 3]
                valueAtIndex3D(vec3)
            }
        }
    }

    // for each row: in row: map values fl4 -> fl4
    /** Does not overwrite anything, just returns */
    private fun Array<Array<Float4>>.onIndices(operation: (Float4) -> Float4) = this.doOnSecond { it.doOnSecond { fl4 -> operation(fl4) } }

    /** @param depth floating pixels
     * @param yzAngle
     * @param xzAngle both are angle in degrees */
    fun getAnyOrientationSlice(depth: Float, yzAngle: Double, xzAngle: Double): Array<Array<Float4>> {
        val center = Float4(
            size.width / 2f,
            size.height / 2f,
            size.depth / 2f,
            1f
        )
        val xAxisAngle = Math.toRadians(yzAngle).toFloat()
        val yAxisAngle = Math.toRadians(xzAngle).toFloat()
        val slice = indicesAtZSlice(depth)
        val rotMX = rotation(axis = Float3(x = Config.rotateDirection.x), angle = yzAngle.toFloat())
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

    fun checkIfAllTheSame(): Boolean {
        val arr = array3d.rotate().doOnSecond { zxArr -> zxArr.rotate() }.flattenToTyped() // y.x.z -> yx.z
        return arr.all { zArr -> zArr.all { it == zArr[0] } }
    }
    fun isSelectedPixelTheSame(doPrint: Boolean = false): Boolean {
        val arr = array3d.rotate().doOnSecond { zxArr -> zxArr.rotate() }.flattenToTyped() // y.x.z -> yx.z
        val zValsOfSelectedPx = arr[Config.selectedPixel].toList()
        if(doPrint)
            println("zValsOfSelectedPx: $zValsOfSelectedPx")
        return zValsOfSelectedPx.all { it == zValsOfSelectedPx[0] }
    }
}
typealias Array1D = Array<Short>
typealias Array2D = Array<Array<Short>>
typealias Array3D = Array<Array<Array<Short>>>
