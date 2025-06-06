package dicom

open class ItemListBase(val list: List<DicomByteData>, val nested: Boolean = true): List<DicomByteData> by list {
    fun get() = if(nested) list[1] else list[0]

    /** prints all items. Items are in `{}` braces, separated by just a `;` */
    override fun toString(): String {
        if(!nested)
            return ""
        val sb = StringBuilder()
        list.forEach {
            if (it.tag != tagAsUInt("(FFFE,E0DD)")) { // Don't print sequence delimiter
                sb.append("Item: ")
                sb.append(it)
            }
            sb.append("\n")
        }
        if (sb.isNotEmpty())
            sb.deleteCharAt(sb.length - 1) // removes last '\n'
        //sb.deleteCharAt(sb.length - 1) // removes last ','
        return "{\n$sb}"
    }

    companion object {
        private fun defaultGetItem(cursor: DicomCursor): DicomByteData {
            if(DicomTag.canReadTag(cursor) == 0u) {
                throw Exception("Can't get item. ByteArray too short to read tag.")
            }
            val tag = cursor.readNextTag()
            if(!tag.canReadValue(cursor)) {
                throw Exception("Can't get item. ByteArray too short to read value.\n" +
                        "Wanted to read ${tag.vl}, but cursor is on position ${cursor.c} in ${cursor.bytes.size} Long byteArray. " +
                        "(${cursor.bytes.size.toUInt() - cursor.c} left)\nThe tag is $tag")
            }
            return DicomByteData(
                tag,
                cursor.readNextByteField(tag.len)
            )
        }

        @JvmStatic
        /** Read tag's content. (Tag with child tags) */
        protected fun readDataToList(obData: DicomByteData, itemGetter: (DicomCursor) -> DicomByteData = { defaultGetItem(it) }): List<DicomByteData> {
            val subCursor = DicomCursor(obData.value)   // cursor over this tag
            val subData = mutableListOf<DicomByteData>()
            while(DicomTag.canReadTag(subCursor) != 0u) {
                subData.add(itemGetter(subCursor))
            }
            return subData
        }
    }
}