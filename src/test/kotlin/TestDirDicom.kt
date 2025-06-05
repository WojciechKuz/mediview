import dicom.tagAsUInt
import kotlin.test.Test

class TestDirDicom {
    private fun directory() = ReadHelp.pickDirAndDicom().first

    @Test
    fun listDirTest() {
        val dirName = directory()
        println("--- Directory: $dirName")
        val fileList = ReadHelp.listFilesInDir(dirName)
        println("--- FileList (${fileList.size}):\n${fileList.joinToString("\n")}\n--- FileList END.")
    }

    @Test
    fun testHeapSize() {
        val maxSize = printMemSize()
        assert(maxSize > 256.0)
    }

    fun printMemSize(): Double {
        //val size = Runtime.getRuntime().totalMemory() / 1024 / 1024.0
        //println("Total memory heap size of program: $size MB")
        val maxSize = Runtime.getRuntime().maxMemory() / 1024/ 1024.0
        println("Max memory heap size of program: $maxSize MB")
        return maxSize
    }

    @Test
    fun sortDirTest() {
        val dirName = directory()
        printMemSize()
        val data = sortImagesInDirectory(dirName)
        println("--- ${data.size} elements")
        println("--- Listing sorted data (prints instance number):")
        val instanceNumTag = tagAsUInt("(0020,0013)")
        data.forEach {
            val instNum = (it[instanceNumTag]!!.value as String).trim().toUInt()
            println(instNum)
        }       // Numbered from 1 to length (instead of 0 to length-1)
        println("--- List END.")
    }
}