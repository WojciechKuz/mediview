package dicom

object InterpretData {
    fun transferSyntaxUID(transferSyntax: String) {
        when(transferSyntax.trim()) {
            "1.2.840.10008.1.2.1" -> {
                // PS3.5 A.2: Explicit VR Little Endian Transfer Syntax
            }
            "1.2.840.10008.1.2.4.70" -> {
                // JPEG Lossless, Non-Hierarchical, First-Order Prediction(Process 14[Selection Value 1])
            }
        }
    }
}