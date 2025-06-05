package dicom
class DataType
    private constructor(
        val tag: UInt,
        val description: String,
        vrs: List<String>) {

    constructor(tag: String, description: String, vararg vrs: String):
            this(tagAsUInt(tag), description, vrs.asList())
    constructor(dType: DataType, vararg vrs: String):
            this(dType.tag, dType.description, (dType.vrs + vrs.asList()) )

    val vrs: List<String> = vrs.ifEmpty { listOf("UN") }
    // type currently ignored
    override fun toString(): String {
        val tagStr = "[${hexString(tag shr 16)} ${hexString(tag and 0xffffu)}]"
        return description // "$tagStr ${vrs.joinToString("/")} $description"
    }
}