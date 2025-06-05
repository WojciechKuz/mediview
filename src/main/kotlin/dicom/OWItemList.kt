package dicom

//typealias OWItemList = OBItemList
class OWItemList(val list: List<DicomByteData>): List<DicomByteData> by list {

    /** prints all items. Items are in `{}` braces, separated by just a `;` */
    override fun toString(): String {
        println("OWItemList.toString()")
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
        private fun getOWItem(cursor: DicomCursor) = OBItemList.getOBItem(cursor)
        /** Read OW's content. */
        private fun readOWData(owData: DicomByteData): OWItemList {
            println("I'm at OW data")
            val subCursor = DicomCursor(owData.value)   // cursor over OB
            val subData = mutableListOf<DicomByteData>()
            while(DicomTag.canReadTag(subCursor) != 0u) {
                println("New OW Item")
                subData.add(getOWItem(subCursor))
            }
            return OWItemList(subData)
        }
        /** interpret OW. Don't use on all OW tags, only on the ones, that you know have item tags inside them. */
        fun interpretOWData(owData: DicomByteData): DicomDataElement<OWItemList> {
            return DicomDataElement(
                owData.dicomTag,
                readOWData(owData)
            )
        }
    }
}