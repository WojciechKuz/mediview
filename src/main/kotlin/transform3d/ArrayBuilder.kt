package transform3d

import org.jetbrains.kotlinx.multik.api.d3array
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.ndarray.data.D3Array
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round


class ArrayBuilder() { // Builder OK, but create 3d array of some library!
    val list: MutableList<ImageAndData<ShortArray>> = mutableListOf()

    /** returns itself */
    fun add(imageAndData: ImageAndData<ShortArray>): ArrayBuilder {
        list.add(imageAndData)
        return this
    }
    /** returns itself */
    fun addAll(imageAndDataList: List<ImageAndData<ShortArray>>): ArrayBuilder {
        list.addAll(imageAndDataList)
        return this
    }

    fun convertWithInterpolationU(width: UInt, height: UInt, depth: UInt, targetDepth: UInt, interpolate: (List<Short>, Int, Int) -> List<Short>) = convertWithInterpolation(width.toInt(), height.toInt(), depth.toInt(), targetDepth.toInt(), interpolate)
    fun convertWithInterpolation(width: Int, height: Int, depth: Int, targetDepth: Int, interpolate: (List<Short>, Int, Int) -> List<Short>): ArrayAndDataMaps<D3Array<Short>> {
        val flatShortList = list.map { iad: ImageAndData<ShortArray> ->
            val image = iad.imageBytes.toList()
            val newImage = interpolate(image, depth, targetDepth)
            newImage
        }.flatten()
        val dataMaps = list.map { it.dataMap }

        // perform conversion
        val array3d: D3Array<Short> = mk.d3array(width, height, targetDepth) { i -> flatShortList[i] }
        return ArrayAndDataMaps(dataMaps, array3d)
    }

    fun convert(width: UInt, height: UInt, depth: UInt) = convert(width.toInt(), height.toInt(), depth.toInt())
    fun convert(width: Int, height: Int, depth: Int): ArrayAndDataMaps<D3Array<Short>> {
        val asOneList = list.flatMap { iad: ImageAndData<ShortArray> -> iad.imageBytes.toList() }
        val dataMaps = list.map { it.dataMap }
        // perform conversion
        val array3d: D3Array<Short> = mk.d3array(width, height, depth) { i -> asOneList[i] }
        return ArrayAndDataMaps(dataMaps, array3d)

    }
}
