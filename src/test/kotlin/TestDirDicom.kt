import dicom.TagToDataMap
import dicom.filestructure.DataRead
import dicom.tagAsUInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import transform3d.InterpretData
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

    @Test
    fun distancePrintTest() = runBlocking {
        val pxSpacingTag = tagAsUInt("(0028,0030)") // pixel spacing

        val dirName = directory()

        // new way - ez to parallelize:
        val instanceNumTag = tagAsUInt("(0020,0013)") // It is 'IS' - integer string. Copied from ImageSorter to make it faster
        val imagePosTag = tagAsUInt("(0020,0032)")

        // Interpolation will make other image's data useless. Keep only one. The image unique data are used only for ordering of them.
        var oneDataMap: TagToDataMap? = null
        val notSetVal = -11000.0
        var sliceZPos1: Double = notSetVal; var sliceZPos2: Double = notSetVal

        // Merged steps 1-5. directory -> sorted list of images (as ShortArray)
        suspend fun mergedSteps1to5(fileName: String): Pair<Double, ShortArray> = withContext(Dispatchers.Default) {

            // 1. pick file, get cursor
            val cursor = ReadHelp.cursorAtDataSet(dirName + fileName) // string -> cursor

            // 2. get data map
            val data = DataRead().getFullDataMap(cursor) // cursor -> TagToDataMap

            // 3.(was 4) filter data here if necessary. All should have same series number
            val filteredData = data

            // 4.(was 3) order images (add key to know later how to sort)
            //val sortPair = (filteredData[instanceNumTag]!!.value as String).trim().toInt() to filteredData // [whichImg] = data
            val sortPair = (filteredData[imagePosTag]!!.value as String).trim().split("\\")[2].trim().toDouble() to filteredData // z
            val whichImg = (filteredData[instanceNumTag]!!.value as String).trim().toInt()

            // 5. image data as short array, keep only one copy of non-image data
            if(whichImg == 1) { // first image is actually 1
                oneDataMap = sortPair.second //InterpretData.removeImageData(sortPair.second)
                sliceZPos1 = sortPair.first
            }
            if(whichImg == 2) {
                sliceZPos2 = sortPair.first
            }
            val imagePair = sortPair.first to InterpretData.dataMapToImage(sortPair.second)

            // result:
            imagePair
        }

        // Execute steps 1-5 asynchronously
        val imageList = ReadHelp.listFilesInDir(dirName).map { fileName ->
            async {
                mergedSteps1to5(fileName)
            }
        }.awaitAll().sortedBy { it.first }
        val dists = DoubleArray(imageList.size - 1)
        imageList.forEachIndexed { i, it ->
            if (i + 1 < imageList.size) {
                val dist2next = imageList[i + 1].first - it.first
                dists[i] = dist2next
                println("[$i]: ${it.first}  dist: $dist2next")
            }
        }
        println("AVG: ${dists.average()}")
        Unit
    }
}