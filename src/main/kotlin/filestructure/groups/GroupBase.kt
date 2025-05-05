package filestructure.groups

import DataType
import filestructure.DataSet.tagToPair

open class GroupBase {
    protected operator fun String.unaryPlus(): DataType {
        return tagToPair(this).second
    }
    protected operator fun String.times(vr: String): DataType {
        return tagToPair(this, vr).second
    }
}