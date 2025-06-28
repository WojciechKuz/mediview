package transform3d

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.Mat4
import dev.romainguy.kotlin.math.rotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
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
    /** Same as size/2 but keeping this for efficiency */
    var center: MySize3 = MySize3(
        initialWidth/2,
        initialHeight/2,
        array.size/initialWidth/initialHeight/2
    )

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

        /** Data is ignored. Takes only imageArray part. returns itself */
        fun add(imageAndData: ImageAndData<ShortArray>) = addSA(imageAndData.imageArray)
        /** returns itself */
        fun add(imageArray: Array<Short>): Array3DBuilder {
            list.add(imageArray.toShortArray())
            return this
        }
        /** returns itself */
        fun addSA(imageArray: ShortArray): Array3DBuilder {
            list.add(imageArray)
            return this
        }
        /** Add all image and data. Data is ignored. Takes only imageArray part. returns itself */
        fun addAllIAD(imageAndDataList: List<ImageAndData<ShortArray>>) =
            addAllSA(imageAndDataList.map { it.imageArray })
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
            return ArrayOps(
                list.flattenListToShortArray(),
                width, height
            )
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

        fun List<ShortArray>.flattenListToShortArray(): ShortArray { // Number of coroutines can be minimized with use of coroutineForLoop()
            val indi2 = Indices2(MySize2(this[0].size, this.size))
            // indi2.size.height as depth, indi2.size.width as combined height and width
            return ShortArray(indi2.size.total) { lai ->
                this[indi2.absoluteToY(lai)][indi2.absoluteToX(lai)]
            }
        }

        // new doOnSecond
        inline fun <reified T, reified U> Array<T>.forSecond(alterSecond: (T) -> U): Array<U> {
            return Array<U>(this.size) { lai ->
                alterSecond(this[lai])
            }
        }

        /** Same as map, but returns Array<U> instead of List<U>. Uses operations on collections */
        private inline fun <reified T, reified U> Array<T>.indexedDoOnSecond(alterSecond: (Int, T) -> U): Array<U> {
            return Array<U>(this.size) { lai ->
                alterSecond(lai, this[lai])
            }
        }

        /** reverse order of elements in array. Uses operations on collections */
        private inline fun <reified T> Array<T>.inverse(): Array<T>
            = this.mapIndexed { index, _ -> this[this.size - index - 1] }.toTypedArray()
    }

    /** map pixels. Writes to internal list. Returns itself. */
    suspend fun transformEachPixel(transform: (Short) -> Short): ArrayOps {
        coroutineForLoop(size.depth) { zi ->
            for (yi in 0 until size.height) {
                for(xi in 0 until size.width) {
                    val absi = absoluteIndexOf3(xi, yi, zi)
                    //if(absi == 15367) println("Transforming ${array[absi]} to ${transform(array[absi])}")
                    array[absi] = transform(array[absi])
                }
            }
        }
        return this
    }

    /** from flat ZYX array creates YX.Z array to perform some operation on this z-rows. Then transforms it back to ZYX and writes to array
     * In lambda first parameter is z-ShortArray, second is yxi value (in Indices2 of width and height) */
    @Deprecated("It's not efficient enough. Use when you don't know target size", ReplaceWith("doSomethingOnYXArrayOfZArrays(targetSize, lambda)"))
    suspend fun doSomethingOnYXArrayOfZArrays(doOnZArr: (ShortArray, Int) -> ShortArray): ArrayOps {

        // Z.Y.X
        val indi2 = Indices2(MySize2(size.width,size.height))
        val indi3 = Indices3(size)

        // Possible upgrade. If this function received new depth parameter,
        // then this function will have 1 triple loop instead of 2.

        val indexArray = Array(indi2.size.height) { yi -> yi }
        val arrayOfZArrays: Array<ShortArray> = indexArray.map { yi ->
            CoroutineScope(Dispatchers.Default).async {
                Array<ShortArray>(size.width) { xi ->
                    val zArr = ShortArray(size.depth) { z ->
                        array[indi3.absoluteIndexOf3(xi, yi, z)]
                    }
                    doOnZArr(zArr, indi2.absoluteIndexOf2(xi, yi))
                }
            } // results in List<Array<ShortArray>> // y.x.z
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
        center = MySize3(newSize.width/2, newSize.height/2, newSize.depth/2)
        array = newShArr
        return this
    }

    /** Performs some operation on z-rows. If you provide size, then it's 2x faster.
     * In lambda first parameter is z-ShortArray, second is yxi value (in Indices2 of width and height) */
    suspend fun doSomethingOnYXArrayOfZArrays(newZSize: Int, doOnZArr: (ShortArray, Int) -> ShortArray): ArrayOps {

        val indi2 = Indices2(MySize2(size.width,size.height))
        val indi3 = Indices3(size)

        val newSize = MySize3(indi3.size.width, indi3.size.height, newZSize)
        val newIndi3 = Indices3(newSize)
        val newArray = ShortArray(newSize.total)

        coroutineForLoop(indi2.size.height) { yi ->
            for(xi in 0 until size.width) {
                val zArr = ShortArray(size.depth) { z ->
                    array[indi3.absoluteIndexOf3(xi, yi, z)]
                }
                val newZArr = doOnZArr(zArr, indi2.absoluteIndexOf2(xi, yi))
                for(zi in newZArr.indices) {
                    newArray[newIndi3.absoluteIndexOf3(xi, yi, zi)] = newZArr[zi]
                }
            }
        }
        size = newSize
        center = MySize3(newSize.width/2, newSize.height/2, newSize.depth/2)
        array = newArray
        return this
    }

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

    private fun absoluteIndexOf3(x: Int, y: Int, z: Int) = (z * size.height + y) * size.width + x

    /** ShortArray of flat YX for Z index */ // zyx image - OK, EZ. Already is
    fun getFlatYXforZ(depthZ: Int): ShortArray {
        val indi3 = Indices3(size)
        val start = indi3.absoluteIndexOf3(0, 0, depthZ)
        val endInclusive = indi3.absoluteIndexOf3(0, 0, depthZ + 1) - 1
        return array[start, endInclusive]
    }

    suspend fun getFlatYXforMergedZ(depthZ: Int, merge: (ShortArray) -> Short = { it[0] }): ShortArray {
        val indi2 = Indices2(MySize2(size.width,size.height)) // target image: y vertical, x horizontal,  z depth
        return createShortArrayWithCoroutines(indi2.size.total) { xyi ->
            val values =  ShortArray(size.depth - depthZ) { zi ->
                array[absoluteIndexOf3(indi2.absoluteToX(xyi), indi2.absoluteToY(xyi), depthZ+zi)]
            }
            merge(values)
        }
    }

    /** ShortArray of flattened YZ for X index */   // xyz
    fun getFlatYZforX(depthX: Int): ShortArray {
        val indi2 = Indices2(MySize2(size.depth, size.height)) // target image: y vertical, z horizontal,  x depth
        return ShortArray(indi2.size.total) { yzi -> array[absoluteIndexOf3(depthX, indi2.absoluteToY(yzi), indi2.absoluteToX(yzi))]}
    }

    suspend fun getFlatYZforMergedX(depthX: Int, merge: (ShortArray) -> Short = { it[0] }): ShortArray {
        val indi2 = Indices2(MySize2(size.depth,size.height)) // target image: y vertical, z horizontal,  x depth
        return createShortArrayWithCoroutines(indi2.size.total) { yzi ->
            val values =  ShortArray(size.width - depthX) { xi ->
                array[absoluteIndexOf3(depthX+xi, indi2.absoluteToY(yzi), indi2.absoluteToX(yzi))]
            }
            merge(values)
        }
    }

    /** ShortArray of flattened XZ for Y index */   // yxz
    fun getFlatXZforY(depthY: Int): ShortArray {
        val indi2 = Indices2(MySize2(size.depth, size.width)) // target image: x vertical, z horizontal,  y depth
        return ShortArray(indi2.size.total) { xzi -> array[absoluteIndexOf3(indi2.absoluteToY(xzi), depthY, indi2.absoluteToX(xzi))] }
    }

    suspend fun getFlatXZforMergedY(depthY: Int, merge: (ShortArray) -> Short = { it[0] }): ShortArray {
        val indi2 = Indices2(MySize2(size.depth, size.width)) // target image: x vertical, z horizontal,  y depth
        return createShortArrayWithCoroutines(indi2.size.total) { xzi ->
            val values =  ShortArray(size.height - depthY) { yi ->
                array[absoluteIndexOf3(indi2.absoluteToY(xzi), depthY+yi, indi2.absoluteToX(xzi))]
            }
            merge(values)
        }
    }

    fun getValuesAlongZ(x: Int, y: Int): ShortArray {
        return ShortArray(size.depth) { zi->
            array[absoluteIndexOf3(x, y, zi)]
        }
    }
    fun getValuesAlongY(z: Int, x: Int): ShortArray {
        return ShortArray(size.depth) { yi->
            array[absoluteIndexOf3(x, yi, z)]
        }
    }
    fun getValuesAlongX(z: Int, y: Int): ShortArray {
        return ShortArray(size.depth) { xi->
            array[absoluteIndexOf3(xi, y, z)]
        }
    }

    private fun ensureInBounds3D(vec: Float4): Float3 {
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
    @Deprecated("It's not efficient enough", ReplaceWith("manualValueAtIndex3D"))
    private fun valueAtIndex3D(vec: Float4): Short {
        val ensVec = ensureInBounds3D(vec)
        val xIndices = intArrayOf( floor(ensVec.x).toInt(), ceil(ensVec.x).toInt() )
        val yIndices = intArrayOf( floor(ensVec.y).toInt(), ceil(ensVec.y).toInt() )
        val zIndices = intArrayOf( floor(ensVec.z).toInt(), ceil(ensVec.z).toInt() )
        val dx = ensVec.x - xIndices[0]
        val dy = ensVec.y - yIndices[0]
        val dz = ensVec.z - zIndices[0]
        val collapsedYXArray = ShortArray(2) { z ->
            val collapsedXArr = ShortArray(2) { y ->
                val xArr = ShortArray(2) { x ->
                    array[absoluteIndexOf3(xIndices[x], yIndices[y], zIndices[z])]
                }
                InterpolationSA.linearInterpolation(xArr, dx.toDouble())
            }
            InterpolationSA.linearInterpolation(collapsedXArr, dy.toDouble())
        }
        return InterpolationSA.linearInterpolation(collapsedYXArray, dz.toDouble())
    }

    fun valueAt(x: Int, y: Int, z: Int): Short = manualValueAtIndex3D(Float4(x.toFloat(), y.toFloat(), z.toFloat(), 1f))
    fun valueAt(x: Float, y: Float, z: Float): Short = manualValueAtIndex3D(Float4(x, y, z, 1f))

    /** Performs Trilinear interpolation. It's called manual cuz it doesn't use arrays.
     * Seems like using arrays in Kotlin is too slow. WTF Kotlin??? */
    private fun manualValueAtIndex3D(vec: Float4): Short {
        fun ensure(index: Float, upperBound: Int): Float {
            return when {
                index < 0f -> 0f
                index > (upperBound - 1f) -> (upperBound - 1f)
                else -> index
            }
        }
        val ensX = ensure(vec.x, size.width)
        val ensY = ensure(vec.y, size.height)
        val ensZ = ensure(vec.z, size.depth)
        val xInd0 = floor(ensX).toInt(); val xInd1 = ceil(ensX).toInt()
        val yInd0 = floor(ensY).toInt(); val yInd1 = ceil(ensY).toInt()
        val zInd0 = floor(ensZ).toInt(); val zInd1 = ceil(ensZ).toInt()
        val dx = ensX - xInd0
        val dy = ensY - yInd0
        val dz = ensZ - zInd0

        // I know it's stupid writing trilinear interpolation without arrays, but this performs much better
        // Even ShortArrays and intArrays were too much!
        return InterpolationSA.manualLinearInterpolationOf2( // 8 + 4 + 2 operations
            InterpolationSA.manualLinearInterpolationOf2(
                InterpolationSA.manualLinearInterpolationOf2(
                    array[absoluteIndexOf3(xInd0, yInd0, zInd0)],
                    array[absoluteIndexOf3(xInd1, yInd0, zInd0)],
                    dx.toDouble()
                ),
                InterpolationSA.manualLinearInterpolationOf2(
                    array[absoluteIndexOf3(xInd0, yInd1, zInd0)],
                    array[absoluteIndexOf3(xInd1, yInd1, zInd0)],
                    dx.toDouble()
                ),
                dy.toDouble()
            ),
            InterpolationSA.manualLinearInterpolationOf2(
                InterpolationSA.manualLinearInterpolationOf2(
                    array[absoluteIndexOf3(xInd0, yInd0, zInd1)],
                    array[absoluteIndexOf3(xInd1, yInd0, zInd1)],
                    dx.toDouble()
                ),
                InterpolationSA.manualLinearInterpolationOf2(
                    array[absoluteIndexOf3(xInd0, yInd1, zInd1)],
                    array[absoluteIndexOf3(xInd1, yInd1, zInd1)],
                    dx.toDouble()
                ),
                dy.toDouble()
            ),
            dz.toDouble()
        )
    } // manualValueAtIndex3D end

    /** Combined previously 2 steps - creating indices array, getting value */
    private fun valueAtTransformedPosition(startZ: Int, x: Int, y: Int, rotMX: Mat4, rotMY: Mat4): ShortArray {
        val startZFl = startZ.toFloat()
        val fl4 = Float4(       // translate, center -> 0,0,0
            x.toFloat() - center.width, // incorporated translation to the center
            y.toFloat() - center.height,
            startZFl - center.depth,
            1f
        )
        return ShortArray(size.depth - startZ) { i ->
            fl4.z = (startZFl + i) - center.depth // update z (and translate to center)
            val step1 = rotMX * fl4     // rotate around X
            val step2 = rotMY * step1   // rotate around Y
            val index = Float4(
                step2.x + center.width, // incorporated translation from the center
                step2.y + center.height,
                step2.z + center.depth,
                1f
            )                           // translate, 0,0,0 -> center
            manualValueAtIndex3D(index)   // return short
        }
    }

    /** Od zadanego z do końca, łącząc piksele zadaną funkcją, zwróć dowolnie zorientowaną klatkę obrazu
     * @param depth
     * @param yzAngle
     * @param xzAngle both are angle in degrees
     * @param merge function, that merges pixels in user-screen orientation. Examples: first element, max, min, average, etc.
     */
    suspend fun getMergedSlicesAtAnyOrientation(depth: Int, yzAngle: Double, xzAngle: Double, merge: (ShortArray) -> Short = { it[0] }): ShortArray {
        //println("AngledImg. depth: $depth, yzAngle: $yzAngle, xzAngle: $xzAngle")

        val rotMX = rotation(axis = Float3(x = Config.rotateDirection.x), angle = yzAngle.toFloat()) // takes degrees
        val rotMY = rotation(axis = Float3(y = Config.rotateDirection.y), angle = xzAngle.toFloat())

        val indi2 = Indices2(MySize2(size.width,size.height))

        val orientedSlice = createShortArrayWithCoroutines(indi2.size.total) { i ->
            val values = valueAtTransformedPosition(depth, indi2.absoluteToX(i), indi2.absoluteToY(i), rotMX, rotMY)
            merge(values)
        }

        return orientedSlice
    }
}
