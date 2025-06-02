import java.io.File


fun byteArrayToFile(byteArray: ByteArray, fileName: String) {
    val file = File(fileName)
    file.writeBytes(byteArray)
}