package dicom
// For when List<DicomByteData> Can't infer inner type.
// List<*> won't do the job, thus separate class OBItemList.

/** Use in place of List<DicomByteData>  */
class OBItemList(val list: List<DicomByteData>): List<DicomByteData> by list {
    /*private fun DicomByteData.dataMapToString(): String {
        val sb = StringBuilder()
        this.forEach{ (k, v) ->
            sb.append(v.toString())
            sb.append('\n')
        }
        return sb.toString()
    }*/
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
}