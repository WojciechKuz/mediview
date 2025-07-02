import dicom.DicomCursor
import dicom.TagToDataMap
import dicom.filestructure.DataSet
import dicom.filestructure.Header
import dicom.filestructure.groups.AllTagsFromPDF
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView

/** Easily open DICOM File. Use for example in tests. */
object ReadHelp {
    val defaultPath = "src/test/resources/IMG-0001-00001.dcm"

    /** Open system file picker */
    fun pickDicom(): String {
        val dialog = FileDialog(null as Frame?, "Select DICOM File to Open", FileDialog.LOAD)
        dialog.isVisible = true
        return dialog.directory + dialog.file
    }
    /** Open system file picker. Get a pair of directory and file. If cancelled, return empty directory */
    fun pickDirAndDicom(): Pair<String, String> {
        val dialog = FileDialog(null as Frame?, "Select DICOM File to Open", FileDialog.LOAD)
        dialog.isVisible = true
        return Pair(dialog.directory?: "", dialog.file?: "")
    }
    fun listFilesInDir(dir: String): List<String> {
        val dirF = File(dir)
        val fList = dirF.listFiles()?: return emptyList()
        return fList.map { it.name }
    }

    fun pickWriteDirFD(): String {
        if(Config.forbiddenApple) System.setProperty("apple.awt.fileDialogForDirectories", "true")

        val dialog = FileDialog(null as Frame?, "Select Directory to write to", FileDialog.LOAD)
        dialog.isVisible = true

        if(Config.forbiddenApple) System.setProperty("apple.awt.fileDialogForDirectories", "false")

        return dialog.directory
    }

    fun pickWriteDirJFC(): String {
        val fileChooser = JFileChooser(FileSystemView.getFileSystemView()).apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY // Ustawienie trybu na wybór katalogu
            dialogTitle = "Wybierz folder"
            approveButtonText = "Wybierz"
            approveButtonToolTipText = "Wybierz bieżący katalog jako miejsce docelowe"
        }

        val result = fileChooser.showOpenDialog(null as Frame?)
        return if(result == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile.toString()
        } else ""
    }

    /** Get DICOM file cursor for specified file */
    fun getCursor(path: String = defaultPath): DicomCursor {
        val cursor = DicomCursor(File(path))
        return cursor
    }
    /** pass Header, put cursor at DataSet */
    fun cursorAtDataSet(path: String = defaultPath): DicomCursor {
        val cursor = getCursor(path)
        Header.skipPreamble(cursor)
        Header.dicomPrefix(cursor)
        return cursor
    }

    /** print all tags from TagToDataMap. At the end prints also the list of tags without definitions. */
    fun printTags(dataMap: TagToDataMap) {
        val descriptionNotFoundList = mutableListOf<String>()
        dataMap.forEach { (k, v) ->
            println( v.toString() +
                    when(k) {
                        in DataSet.tagNames -> "\t -> " + DataSet.tagNames[k]
                        in AllTagsFromPDF.allTagMap -> "\t -> " + AllTagsFromPDF.allTagMap[k]
                        else -> "".also { descriptionNotFoundList.add(v.getStringTag()) }
                    }
            )
        }
        if (descriptionNotFoundList.isNotEmpty()) {
            println("\nThese tag descriptions were not found:\n$descriptionNotFoundList")
        }
    }

    fun strHex(u: UInt, pad: Int = 4): String {
//        if (u != 0u)
//            return "0x" + u.toString(16)
//        return "0x0000"
        return "0x" + u.toString(16).padStart(pad, '0') // same as "0x" + hexString(u)
    }
}