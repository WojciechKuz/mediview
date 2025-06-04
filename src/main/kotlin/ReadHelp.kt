import dicom.DicomCursor
import dicom.TagToDataMap
import dicom.filestructure.DataSet
import dicom.filestructure.Header
import dicom.filestructure.groups.AllTagsFromPDF
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** Easily open DICOM File. Use for example in tests. */
object ReadHelp {
    val defaultPath = "src/test/resources/IMG-0001-00001.dcm"

    /** Open system file picker */
    fun pickDicom(): String {
        val dialog = FileDialog(null as Frame?, "Select DICOM File to Open", FileDialog.LOAD)
        dialog.isVisible = true
        return dialog.directory + dialog.file
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