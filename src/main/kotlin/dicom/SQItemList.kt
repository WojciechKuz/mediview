package dicom

import dicom.filestructure.DataRead

/** Use in place of List<TagToDataMap>  */
class SQItemList(val list: List<TagToDataMap>): List<TagToDataMap> by list {

    private fun TagToDataMap.dataMapToString(): String {
        val sb = StringBuilder()
        this.forEach{ (k, v) ->
            sb.append(v.toString())
            sb.append('\n')
        }
        return sb.toString()
    }

    /** prints all items. Items are in `{}` braces, separated by just a `;` */
    override fun toString(): String {
        val sb = StringBuilder()
        list.forEach {
            //sb.append("Item ${it.size()}:\n")
            sb.append("Item:\n")
            sb.append(it.dataMapToString())
            //sb.append("\n")
        }
        //sb.deleteCharAt(sb.length - 1) // removes last '\n'
        //sb.deleteCharAt(sb.length - 1) // removes last ','
        return "{\n$sb}"
    }

    companion object {
        private fun getSQItem(cursor: DicomCursor): TagToDataMap {
            if(DicomTag.canReadTag(cursor) == 0u) {
                throw Exception("Can't get item. ByteArray too short to read tag.")
            }
            val tag = cursor.readNextTag()
            val subCursor = DicomCursor(cursor.readNextByteField(tag.len))  // cursor over Item
            val subDataMap = DataRead(true).getFullDataMap(subCursor)
            return subDataMap
        }
        /** Read SQ's content. We don't interpret control characters, so it's safe to flatMap it. */
        private fun readSQData(sqData: DicomByteData): SQItemList {
            val subCursor = DicomCursor(sqData.value)   // cursor over SQ
            val subData = mutableListOf<TagToDataMap>()

            while(DicomTag.canReadTag(subCursor) != 0u) {
                subData.add(getSQItem(subCursor))
            }
            return SQItemList(subData)
        }
        /** For DicomByteData with tag SQ, get DicomDataElement with data type determined. */
        fun interpretSQData(sqData: DicomByteData): DicomDataElement<SQItemList> {
            return DicomDataElement(
                sqData.dicomTag,
                readSQData(sqData)
            )
        }
    }
}