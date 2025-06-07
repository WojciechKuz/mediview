package dicom

import java.io.File

/** Write byte array to File */
fun byteArrayToFile(byteArray: ByteArray, fileName: String) {
    val file = File(fileName)
    file.writeBytes(byteArray)
}