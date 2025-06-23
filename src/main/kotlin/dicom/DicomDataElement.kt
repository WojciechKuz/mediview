package dicom
/** Stores tag and value. Extends [DicomTag].
 *
 * ⚠ Use `valueAsSomeType()` methods only when value is ByteArray.
 * When type of value is already known, use `as SomeType` instead. */
open class DicomDataElement<T>(
    hex1: UInt, hex2: UInt, vr: String, vl: UInt, val value: T, private var nestedData: Boolean = false
): DicomTag(hex1, hex2, vr, vl) {

    constructor(dicomTag: DicomTag, value: T, nestedData: Boolean = false):
            this(dicomTag.tagPt1, dicomTag.tagPt2, dicomTag.vr, dicomTag.vl, value, nestedData)

    val dicomTag: DicomTag
        get() = this

    companion object {
        /** Too long to print */
        const val tooLong = 256u
    }

    override fun toString(): String = super.toString() + when(value) {
        is ByteArray -> " " + when(true) {
            (vl == 0u) -> ""
            (vl <= tooLong) -> " \"" + value.toString(Charsets.US_ASCII) + "\""
            else -> ""
        }
        is String -> " " + if (vl <= tooLong)
                value
            else
                ""
        //is UInt -> " 0x" + hexString(value)
        is UInt -> " " + value
        is SQItemList -> {
            " " + if(value.isNotEmpty())
                value.toString()
            else
                ""
        }
        is OBItemList -> {
            //println("OB Item List to String")
            " " + if(value.isNotEmpty())
                value.toString()
            else
                ""
        }
        is OWItemList -> {
            //println("OW Item List to String")
            " " + if(value.isNotEmpty())
                value.toString()
            else
                ""
        }
        else -> " " + value.toString()
    }

    fun setNested() { nestedData = true }
    val isNested: Boolean
        get() = when(vr) {
            "SQ", "OB", "OW" -> nestedData
            else -> false
        }

    /** ⚠ Use this method only when value is ByteArray. */
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

    /** ⚠ Use this method only when value is ByteArray. */
    fun valueAsInt(): DicomDataElement<Int> {
        if (value !is ByteArray) {
            throw IllegalArgumentException("DicomDataElement.value is not a ByteArray. Only ByteArrays can be converted to other types.")
        }
        // notTODO value to signed (u2) integer of length vl. Can be done using -> toString -> trim -> toInt
        return DicomDataElement(dicomTag, -1099)
    }

    /** ⚠ Use this method only when value is ByteArray. */
    fun valueAsUInt(): DicomDataElement<UInt> {
        if (value !is ByteArray) {
            throw IllegalArgumentException("DicomDataElement.value is not a ByteArray. Only ByteArrays can be converted to other types.")
        }
        val cur = DicomCursor(value)
        val uint = cur.readNextInt(vl)
        return DicomDataElement(dicomTag, uint)
    }

    /** ⚠ Use this method only when value is ByteArray. */
    fun valueAsHexStr(): DicomDataElement<String> {
        if (value !is ByteArray) {
            throw IllegalArgumentException("DicomDataElement.value is not a ByteArray. Only ByteArrays can be converted to other types.")
        }
        val str = value.toHexString()
        return DicomDataElement(dicomTag, str)
    }
}

typealias DicomByteData = DicomDataElement<ByteArray>
typealias TagToDataMap = Map<UInt, DicomDataElement<out Any>>
