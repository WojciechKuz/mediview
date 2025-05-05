
/** Stores tag and value. Extends [DicomTag]. */
open class DicomDataElement<T>(hex1: UInt, hex2: UInt, vr: String, vl: UInt, val value: T): DicomTag(hex1, hex2, vr, vl) {
    constructor(dicomTag: DicomTag, value: T): this(dicomTag.tagPt1, dicomTag.tagPt2, dicomTag.vr, dicomTag.vl, value)

    val dicomTag: DicomTag
        get() = this

    override fun toString(): String = super.toString() + when(value) {
        is ByteArray -> ": " + if(vl < 256u) // isLengthDefined() // value.size <= 256
                " \"" + value.toString(Charsets.US_ASCII) + "\""
            else
                "" //""\n" + value.toHexString()
        is String -> " " + value
        //is UInt -> " 0x" + hexString(value)
        is UInt -> " " + value
        else -> " " + value.toString()
    }

    fun valueAsString(): DicomDataElement<String> {
        if (value !is ByteArray) {
            throw IllegalArgumentException("DicomDataElement.value is not a ByteArray. Only ByteArrays can be converted to other types.")
        }
        var str = value.toString(Charsets.US_ASCII)

        if(str.isNotEmpty() && str.last().code == 0) {
            str = str.dropLast(1) + " " // replace '\0' with ' '
        }
        return DicomDataElement(dicomTag, str)
    }
    fun valueAsInt(len: Int): DicomDataElement<Int> {
        if (value !is ByteArray) {
            throw IllegalArgumentException("DicomDataElement.value is not a ByteArray. Only ByteArrays can be converted to other types.")
        }
        // TODO value to signed (u2) integer of given length
        return DicomDataElement(dicomTag, -1099)
    }
    fun valueAsUInt(len: Int): DicomDataElement<UInt> {
        if (value !is ByteArray) {
            throw IllegalArgumentException("DicomDataElement.value is not a ByteArray. Only ByteArrays can be converted to other types.")
        }
        val cur = DicomCursor(value)
        val uint = cur.readNextInt(len)
        return DicomDataElement(dicomTag, uint)
    }
}

typealias DicomByteData = DicomDataElement<ByteArray>