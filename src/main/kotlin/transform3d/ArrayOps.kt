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
import kotlin.collections.get
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sin

/** @param array default it's Z.Y.X */
class ArrayOps(array: Array<Array<Array<Short>>>) {

    /** default is Z.Y.X */
    var array: Array<Array<Array<Short>>> = array
        private set(value) {
            field = value
            whd = WidthHeightDepth(
                array[0][0].size,
                array[0].size,
                array.size,
            )
            //println("modified ArrayOps, sizes: $whd")
        }
    var whd: WidthHeightDepth = WidthHeightDepth(
        array[0][0].size,
        array[0].size,
        array.size,
    ); private set

    init {
        //println("constructed ArrayOps with sizes: $whd")
    }

    val rowSize: Int; get() = array[0][0].size

    // Builder OK, but create 3d array of some library!
    class Array3DBuilder() {
        private var list: MutableList<Array<Short>> = mutableListOf()
        fun getList() = list.toList()
        /*var printed = 0
        fun sayOnce(say: () -> Unit) {
            if(printed==0) {
                say()
                printed++
            }
        }*/

        /** returns itself */
        fun add(imageAndData: ImageAndData<ShortArray>) = add(imageAndData.imageArray)
        /** returns itself */
        fun add(imageAndData: ShortArray): Array3DBuilder {
            list.add(imageAndData.toTypedArray())
            return this
        }
        /** add all image and data. returns itself */
        fun addAllIAD(imageAndDataList: List<ImageAndData<ShortArray>>) = addAll(imageAndDataList.map { it.imageArray })
        /** returns itself */
        fun addAll(imageList: List<ShortArray>): Array3DBuilder {
            list.addAll(imageList.map { it.toTypedArray() })
            return this
        }
        fun create(rowWidth: Int): ArrayOps {
            return ArrayOps(
                Array3D(list.size) { zi ->
                    val splitArr = list[zi].splitTo(rowWidth) // list[zi] is Array<Short> (yx)
                    //sayOnce { println("${list.size} times split array from ${list[zi].size} to ${splitArr[0].size}x${splitArr.size} using row width of $rowWidth.") }
                    splitArr
                }
            )
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

        /** reverse order of elements in array */
        private inline fun <reified T> Array<T>.inverse(): Array<T>
            = this.mapIndexed { index, _ -> this[this.size - index - 1] }.toTypedArray()
    }
    /** map pixels. Writes to internal list. Returns itself. */
    fun transformEachPixel(transform: (Short) -> Short): ArrayOps {
        array.forEach { yxArr -> yxArr.forEach { xArr -> xArr.map { transform(it) }.toTypedArray() } }
        return this
    }

    /** Interpolate (rescale) pixels. Writes to internal list. Returns itself.
     * `!!!` Interpolate changes the amount of images, so you can't match image with its data after this operation is performed! */
    fun interpolateOverZ(scaleFactor: Double, rescale: (Array<Short>, Double) -> Array<Short>): ArrayOps {
        val targetSize = (array.size * scaleFactor).toInt()

        // here image means flattened(x, y)
        // Starting with listOf( image ), z.yx
        // 1. Transform it to yx.z
        // 2. interpolate over those z values
        // 3. transform back to listOf( image ), but with different z size. z.yx
        val interpolatedArray = oldform.rotate().doOnSecond { zPixels -> // same pixel on different images
            rescale(zPixels, scaleFactor)
        }.rotate() //.flatten()

        oldform = interpolatedArray
        return this
    }

    /** Negative rotationOrigin means use `y_size - 1`. */
    fun shearByGantry(gantryAngle: Double, width: Int, move: (Array<Short>, Double) -> Array<Short>, rotationOrigin: Int = -1): ArrayOps {
        val rotCt = if(rotationOrigin < 0)
            oldform[0].size/width - 1 else rotationOrigin
        val moveRow = { yi: Int ->
            val radiansAngle = Math.toRadians(gantryAngle)
            sin(radiansAngle) * (rotationOrigin - yi)
        }

        /* Starting with listOf( image ), list[z] = image
         * 1. Transform (rotate) z.yx array to yx.z array
         * 2. Split yx.z array to y.x.z array
         * 3. compute move value for every row
         * 4. move row (x) in z axis
         * 5. flatten y.x.z to yx.z
         * 6. transform (rotate) back to z.yx
         */
        val shearedArray = oldform.rotate().splitTo(width).mapIndexed { yi, xzArray ->
            val rowShear = moveRow(yi)

            xzArray.doOnSecond { zPixels ->    // same pixel on different images
                move(zPixels, rowShear)
            }

        }.toTypedArray().flatten().toTypedArray().rotate()

        oldform = shearedArray
        return this
    }

    /*fun convertMultik(wdh: WidthHeightDepth): D3Array<Short> {
        // flatten array for Multik to load
        val asOneList = oldform.flatten()

        // perform conversion
        val array3d: D3Array<Short> = mk.d3array(wdh.width, wdh.height, wdh.depth) { i -> asOneList[i] }
        return array3d

    }*/
    /** oldform is z.yx */
    var oldform: Array<Array<Short>> // z.yx
        get() = array.doOnSecond { yxArr -> yxArr.flattenToTyped() }
        set(oldF: Array<Array<Short>>) {
            array = oldF.doOnSecond { yxArr -> yxArr.splitTo(rowSize) }
        }
    val xyz: Array3D; get() = yxz.rotate()
    val xzy: Array3D; get() = zxy.rotate()
    val yxz: Array3D; get() = yzx.doOnSecond { zxArr -> zxArr.rotate() }
    val yzx: Array3D; get() = zyx.rotate()
    val zxy: Array3D; get() = zyx.doOnSecond{ yxArr -> yxArr.rotate() }
    val zyx: Array3D; get() = array // default

    fun indicesAtZSlice(zi: Float) = Array(whd.height) { yi ->
        Array(whd.width) { xi ->
            //mk.d1array<Double>(4) { i -> vec[i] }
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
            ensure(vec.x, whd.width),
            ensure(vec.y, whd.height),
            ensure(vec.z, whd.depth)
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
                    array[zIndices[z]][yIndices[y]][xIndices[x]]
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

    // z.yx -> yx -> y.x
    fun getYX(zi: Int, rowWidth: Int) = zyx[zi]
    // default stored array is z.yx
    fun getFlatYX(zi: Int): Array<Short> = oldform[zi]

    // y.x.z -> x.z // should x axis be inverted?
    fun getXZ(yi: Int) = yxz[yi]
    fun getFlatXZ(yi: Int) = yxz[yi].flatten()

    // z.yx -> yx.z -> y.x.z -> x.y.z -> y.z
    fun getYZ(xi: Int) = xyz[xi]
    fun getFlatYZ(xi: Int) = xyz[xi].flatten()


    /** @param depth floating pixels
     * @param yzAngle
     * @param xzAngle both are angle in degrees */
    fun getAnyOrientationSlice(depth: Float, yzAngle: Double, xzAngle: Double): Array<Array<Float4>> {
        val center = Float4(
            whd.width / 2f,
            whd.height / 2f,
            whd.depth / 2f,
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
}
typealias Array1D = Array<Short>
typealias Array2D = Array<Array<Short>>
typealias Array3D = Array<Array<Array<Short>>>
