package dicom

import java.util.function.IntFunction
import dicom.toHexString

// For when List<DicomByteData> Can't infer inner type.
// List<*> won't do the job, thus separate class OBItemList.

/** Use in place of List<DicomByteData>  */
class OBItemList(list: List<DicomByteData>, nested: Boolean = true): ItemListBase(list, nested) {

    /** prints all items. Items are in `{}` braces, separated by just a `;` */
    override fun toString(): String {
        println("OBItemList.toString()")
        return super.toString()
    }

    companion object {
        /** Read OB's content. */
        private fun readOBData(obData: DicomByteData): OBItemList {
            return if(obData.isNested)
                OBItemList(readDataToList(obData))
            else
                OBItemList(listOf(obData), nested = false)
        }

        /** interpret OB. Don't use on all OB tags, only on the ones, that you know have item tags inside them. */
        fun interpretOBData(obData: DicomByteData): DicomDataElement<OBItemList> {
            return DicomDataElement(
                obData.dicomTag,
                readOBData(obData)
            )
        }
    }
}