package filestructure.groups

import DataType

open class GroupBase {

    /** Decode a tag given in format `Name (ffff,ffff)` to a pair. Tag name as value, tag number as key. */
    fun tagToPair(str: String, vr: String = "UN"): Pair<UInt, DataType> {
        val splitted = str.split('(', ',', ')')
        if (splitted.size < 4) {
            println("ERR, for \"$str\" split resulted in $splitted")
        }
        val dtag = DataType("(${splitted[1]},${splitted[2]})", (splitted[0] + splitted[3]).trim(), vr)
        return Pair(
            dtag.tag,
            dtag
        )
    }

    protected operator fun String.unaryPlus(): DataType {
        return tagToPair(this).second
    }
    protected operator fun String.times(vr: String): DataType {
        return tagToPair(this, vr).second
    }
}