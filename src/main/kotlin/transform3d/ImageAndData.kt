package transform3d

import dicom.TagToDataMap

/** TArray can be ShortArray or Multik's D3Array<Short>
 * ImageAndData structure, stores image and rest of the data separately.
*  New structure stores image as ShortArray. Rest of the data is still stored in TagToDataMap. */
class ImageAndData<TArray>(val dataMap: TagToDataMap, val imageBytes: TArray)


class ArrayAndDataMaps<TArray>(val dataMapList: List<TagToDataMap>, val array: TArray/*List<TArray>*/) //: List<ImageAndData<TArray>>
/*
val size: Int
    get() = min(images.size, dataMapList.size)

operator n get(i: Int): ImageAndData<TArray> {
    return ImageAndData(dataMapList[i], images[i])
}
*/

typealias OnlyDataMaps = List<TagToDataMap>