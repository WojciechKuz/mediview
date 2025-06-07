package dicom

object InterpretData {
    val pixelDataTag = tagAsUInt("[7FE0 0010]")

    val transferSyntaxUIDTag = tagAsUInt("[0002 0010]")
    fun transferSyntaxUID(transferSyntax: String): ImageType = when(transferSyntax.trim()) {
        "1.2.840.10008.1.2.1" -> {
            // PS3.5 A.2: Explicit VR Little Endian Transfer Syntax
            ImageType.RAW_PIXELS
        }
        "1.2.840.10008.1.2.4.70" -> {
            // JPEG Lossless, Non-Hierarchical, First-Order Prediction(Process 14[Selection Value 1])
            ImageType.JPEG
        }
        else -> ImageType.UNKNOWN
    }
    enum class ImageType { JPEG, RAW_PIXELS, UNKNOWN }
}