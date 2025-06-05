package dicom.filestructure.groups

import dicom.DataType

open class GroupBase {

    /** Decode a tag given in format `Name (ffff,ffff)` or `Name [ffff ffff]` to a pair. Tag name as value, tag number as key.
     * Tag name in String can be before or after the tag code. */
    fun tagToPair(str: String, vararg vrs: String): Pair<UInt, DataType> {
        val firstSplit = str.split('(', ')', '[', ']')
        val split = firstSplit[1].split(' ', ',')
        if (firstSplit.size < 3 || split.size < 2) {
            println("ERR, badly formatted \"$str\" read GroupBase.tagToPair() documentation.")
        }
        val dtag = DataType("(${split[0]},${split[1]})", (firstSplit[0] + firstSplit[2]).trim(), *vrs)
        return Pair(
            dtag.tag,
            dtag
        )
    }

    protected operator fun String.unaryPlus(): DataType {
        return tagToPair(this).second
    }
    /** To DataType */
    protected fun String.toDT() = +this
    protected operator fun DataType.times(vr: String): DataType {
        return DataType(this, vr)
    }
    protected fun DataType.withVRs(vararg vrs: String): DataType {
        return DataType(this, *vrs)
    }
}