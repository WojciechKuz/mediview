package transform3d

import org.jetbrains.kotlinx.multik.api.d3array
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.ndarray.data.D3Array
import kotlin.collections.flatten
import kotlin.math.PI
import kotlin.math.sin

/** @param array default it's Z.Y.X */
class ArrayOps(array: Array<Array<Array<Short>>>) {
    /** default is Z.Y.X */
    var array: Array<Array<Array<Short>>> = array
        private set
    val whd = WidthHeightDepth(
        array[0][0].size,
        array[0].size,
        array.size,
    )
    val rowSize: Int; get() = whd.width

    // Builder OK, but create 3d array of some library!
    class Array3DBuilder() {
        private var list: MutableList<Array<Short>> = mutableListOf()
        fun getList() = list.toList()

        /** returns itself */
        fun add(imageAndData: ImageAndData<ShortArray>) = add(imageAndData.imageArray)
        /** returns itself */
        fun add(imageAndData: ShortArray): Array3DBuilder {
            list.add(imageAndData.toTypedArray())
            return this
        }
        /** returns itself */
        fun addAll(imageAndDataList: List<ImageAndData<ShortArray>>) = addAll(imageAndDataList.map { it.imageArray })
        /** returns itself */
        fun addAll(imageAndDataList: List<ShortArray>): Array3DBuilder {
            list.addAll(imageAndDataList.map { it.toTypedArray() })
            return this
        }
        fun create(rowWidth: Int): ArrayOps {
            val immutList = list.toList()
            return ArrayOps(
                Array3D(list.size) { zi ->
                    list[zi].splitTo(rowWidth)
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
                    this[mainArrI *pieceLength + pieceArrI]
                }
            }
        }

        // flatten already is
        private inline fun <reified T> Array<Array<T>>.flattenToTyped(): Array<T> = this.flatten().toTypedArray()

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
    fun interpolateOverZ(scaleFactor: Double, interpolate: (Array<Short>, Double) -> Array<Short>): ArrayOps {
        val targetSize = (array.size * scaleFactor).toInt()

        // here image means flattened(x, y)
        // Starting with listOf( image ), z.yx
        // 1. Transform it to yx.z
        // 2. interpolate over those z values
        // 3. transform back to listOf( image ), but with different z size. z.yx
        val interpolatedArray = oldform.rotate().doOnSecond { zPixels -> // same pixel on different images
            interpolate(zPixels, scaleFactor)
        }.rotate() //.flatten()

        oldform = interpolatedArray
        return this
    }

    /** Negative rotationOrigin means use `y_size - 1`. */
    fun shearByGantry(gantryAngle: Double, width: Int, move: (Array<Short>, Double) -> Array<Short>, rotationOrigin: Int = -1): ArrayOps {
        val rotCt = if(rotationOrigin < 0)
            oldform[0].size/width - 1 else rotationOrigin
        val moveRow = { yi: Int ->
            val radiansAngle = gantryAngle * PI / 180
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

    fun convertMultik(wdh: WidthHeightDepth): D3Array<Short> {
        // flatten array for Multik to load
        val asOneList = oldform.flatten()

        // perform conversion
        val array3d: D3Array<Short> = mk.d3array(wdh.width, wdh.height, wdh.depth) { i -> asOneList[i] }
        return array3d

    }
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
}
typealias Array1D = Array<Short>
typealias Array2D = Array<Array<Short>>
typealias Array3D = Array<Array<Array<Short>>>
