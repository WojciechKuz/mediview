
class DataType(tag: String, val description: String, vararg vrs: String) {
    val tag: UInt = tagAsUInt(tag)
    val vrs: List<String> = vrs.asList()
    // type currently ignored
    override fun toString(): String {
        val tagStr = "[${hexString(tag shr 16)} ${hexString(tag and 0xffffu)}]"
        return description // "$tagStr ${vrs.joinToString("/")} $description"
    }
}