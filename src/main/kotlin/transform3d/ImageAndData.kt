package transform3d

import dicom.TagToDataMap

/** TArray can be ShortArray or Multik's D3Array<Short>
 * ImageAndData structure, stores image and rest of the data separately.
*  New structure stores image as ShortArray. Rest of the data is still stored in TagToDataMap. */
class ImageAndData<TArray>(val dataMap: TagToDataMap, val imageArray: TArray)
