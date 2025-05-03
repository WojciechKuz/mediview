import filestructure.Header
import java.io.File


class DICOMByteRead(file: File) {
    val cursor = DicomCursor(file.readBytes())
    val bytes: ByteArray
        get() = cursor.bytes

    public fun readDicom() {
        readMeta()
    }
    private fun readMeta() {
        Header.filePreamble(cursor)
        Header.dicomPrefix(cursor)
        // TODO read more!!!
    }

    // TODO read meta to dictionary
}