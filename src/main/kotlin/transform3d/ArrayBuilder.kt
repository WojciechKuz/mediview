package transform3d

import org.jetbrains.kotlinx.multik.api.d3array
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.ndarray.data.D3Array
import kotlin.collections.flatten


class ArrayBuilder() { // Builder OK, but create 3d array of some library!
    private var list: MutableList<Array<Short>> = mutableListOf()
    fun getList() = list.toList()

    /** returns itself */
    fun add(imageAndData: ImageAndData<ShortArray>) = add(imageAndData.imageBytes)
    /** returns itself */
    fun add(imageAndData: ShortArray): ArrayBuilder {
        list.add(imageAndData.toTypedArray())
        return this
    }
    /** returns itself */
    fun addAll(imageAndDataList: List<ImageAndData<ShortArray>>) = addAll(imageAndDataList.map { it.imageBytes })
    /** returns itself */
    fun addAll(imageAndDataList: List<ShortArray>): ArrayBuilder {
        list.addAll(imageAndDataList.map { it.toTypedArray() })
        return this
    }

    companion object {
        /** For `array[x][y]` return `array[y][x]` */
        @Suppress("KDocUnresolvedReference")
        fun rotateArray(mainArray: Array<Array<Short>>): Array<Array<Short>> {
            return Array<Array<Short>>(mainArray[0].size) { j -> // sub-arr size
                Array<Short>(mainArray.size) { i ->     // main-arr size
                    mainArray[i][j]
                }
            }
        }
        /** For `array[x][y]` return `array[y][x]` */
        @Suppress("KDocUnresolvedReference")
        fun Array<Array<Short>>.rotate() = rotateArray(this)
    }
    /** map pixels. Writes to internal list. Returns itself. */
    fun transformEachPixel(transform: (Short) -> Short): ArrayBuilder {
        list.forEach { shArr -> shArr.map { transform(it) }.toTypedArray() }
        return this
    }

    /** Interpolate (rescale) pixels. Writes to internal list. Returns itself.
     * `!!!` Interpolate changes the amount of images, so you can't match image with its data after this operation is performed! */
    fun interpolateOverZ(scaleFactor: Double, interpolate: (Array<Short>, Double) -> Array<Short>): ArrayBuilder {
        val targetSize = (list.size * scaleFactor).toInt()

        // here image means flattened(x, y)
        // Starting with listOf( image ), list[z] = image
        // 1. Transform it to list[imgByte] = zPixels (Array (imgByte long) of same pixel on different images)
        // 2. interpolate over those z values
        // 3. transform back to list[z] = image, but with different z size.
        val interpolatedArray = list.toTypedArray().rotate().map { zPixels -> // same pixel on different images
            interpolate(zPixels, scaleFactor)
        }.toTypedArray().rotate().toList() //.flatten()

        list = interpolatedArray.toMutableList()
        return this
    }

    fun convert(wdh: WidthHeightDepth): D3Array<Short> {
        // flatten array for Multik to load
        val asOneList = list.toTypedArray().flatten()

        // perform conversion
        val array3d: D3Array<Short> = mk.d3array(wdh.width, wdh.height, wdh.depth) { i -> asOneList[i] }
        return array3d

    }
}
