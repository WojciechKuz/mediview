package dicom

import java.util.function.IntFunction
import dicom.toHexString

// For when List<DicomByteData> Can't infer inner type.
// List<*> won't do the job, thus separate class OBItemList.

/** Use in place of List<DicomByteData>  */
class OBItemList(val list: List<DicomByteData>): List<DicomByteData> by list {

    /** prints all items. Items are in `{}` braces, separated by just a `;` */
    override fun toString(): String {
        println("OBItemList.toString()")
        val sb = StringBuilder()
        list.forEach {
            if(it.tag != tagAsUInt("(FFFE,E0DD)")) { // Don't print sequence delimiter
                sb.append("Item: ")
                sb.append(it)
            }
            sb.append("\n")
        }
        if(sb.isNotEmpty())
            sb.deleteCharAt(sb.length - 1) // removes last '\n'
        //sb.deleteCharAt(sb.length - 1) // removes last ','
        return "{\n$sb}"
    }

    companion object {
        /** public for OWItemList to use it as it's the same code */
        public fun getOBItem(cursor: DicomCursor): DicomByteData {
            if(DicomTag.canReadTag(cursor) == 0u) {
                throw Exception("Can't get item. ByteArray too short to read tag.")
            }
            val tag = cursor.readNextTag()
            if(!tag.canReadValue(cursor)) {
                println("ERR. Dumping sequence:")
                if(cursor.hasNext(32)) println(cursor.readNextByteField(32).joinToString("") { b -> hexString(b.toU(), pad = 2) })
                if(cursor.hasNext(32)) println(cursor.readNextByteField(32).joinToString(" ") { b -> hexString(b.toU(), pad = 2) })
                throw Exception("Can't get item. ByteArray too short to read value.\n" +
                        "Wanted to read ${tag.vl}, but cursor is on position ${cursor.c} in ${cursor.bytes.size} Long byteArray. " +
                        "(${cursor.bytes.size.toUInt() - cursor.c} left)\nThe tag is $tag")
            }
            return DicomByteData(
                tag,
                cursor.readNextByteField(tag.len)
            )
        }
        /** Read OB's content. */
        private fun readOBData(obData: DicomByteData): OBItemList {
            val subCursor = DicomCursor(obData.value)   // cursor over OB
            val subData = mutableListOf<DicomByteData>()
            while(DicomTag.canReadTag(subCursor) != 0u) {
                subData.add(getOBItem(subCursor))
            }
            return OBItemList(subData)
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