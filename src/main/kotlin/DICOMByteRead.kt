import java.io.File


class DICOMByteRead(file: File) {
    val bytes: ByteArray = file.readBytes()

    public fun readDicom() {
        readMeta()
    }
    private fun readMeta() {
        filePreamble(bytes)
        dicomPrefix(bytes)
    }
}