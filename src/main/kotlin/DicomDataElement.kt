
/** Stores tag and value. Extends [DicomTag]. */
open class DicomDataElement<T>(hex1: UInt, hex2: UInt, vr: String, vl: UInt, val value: T): DicomTag(hex1, hex2, vr, vl) {
    constructor(dicomTag: DicomTag, value: T): this(dicomTag.tagPt1, dicomTag.tagPt2, dicomTag.vr, dicomTag.vl, value)

    val dicomTag: DicomTag
        get() = this

    companion object {
        /** Too long to print */
        const val tooLong = 256u
    }

    override fun toString(): String = super.toString() + when(value) {
        is ByteArray -> " " + if (vl <= tooLong) // isLengthDefined() // value.size <= 256
                " \"" + value.toString(Charsets.US_ASCII) + "\""
            else
                ""
        //"\n" + value.toHexString()
        is String -> " " + if (vl <= tooLong)
                value
            else
                ""
        //is UInt -> " 0x" + hexString(value)
        is UInt -> " " + value
        is SQItemList -> {     // Can't infer type, thus *. But I know It's TagToDataMap
            " " + if(value.isNotEmpty())
                value.toString()
            else
                ""
        }
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

    fun valueAsInt(): DicomDataElement<Int> {
        if (value !is ByteArray) {
            throw IllegalArgumentException("DicomDataElement.value is not a ByteArray. Only ByteArrays can be converted to other types.")
        }
        // TODO value to signed (u2) integer of length vl
        return DicomDataElement(dicomTag, -1099)
    }

    fun valueAsUInt(): DicomDataElement<UInt> {
        if (value !is ByteArray) {
            throw IllegalArgumentException("DicomDataElement.value is not a ByteArray. Only ByteArrays can be converted to other types.")
        }
        val cur = DicomCursor(value)
        val uint = cur.readNextInt(vl)
        return DicomDataElement(dicomTag, uint)
    }

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

fun DataMapToString(dataMap: TagToDataMap): String {
    return ""
}