package dicom

//typealias OWItemList = OBItemList
class OWItemList(list: List<DicomByteData>, nested: Boolean = true): ItemListBase(list, nested) {

    /** prints all items. Items are in `{}` braces, separated by just a `;` */
    override fun toString(): String {
        //println("OWItemList.toString()")
        return super.toString()
    }

    companion object {
        /** Read OW's content. */
        private fun readOWData(owData: DicomByteData): OWItemList {
            return if(owData.isNested)
                OWItemList(readDataToList(owData))
            else
                OWItemList(listOf(owData), nested = false)
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