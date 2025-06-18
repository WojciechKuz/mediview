import androidx.compose.ui.graphics.ImageBitmap
import dicom.tagAsUInt
import transform3d.ArrayOps
import transform3d.Config
import transform3d.ImageAndData
import transform3d.InterpretData
import transform3d.View
import transform3d.getComposeImage
import kotlinx.coroutines.*
import transform3d.Displaying
import transform3d.InterpretData.columnsTag
import transform3d.Mode
import transform3d.MySize3
import transform3d.tagNotFoundErr
import transform3d.throwWrongTag
import kotlin.system.measureTimeMillis

// val trigger: () -> Unit

class UIManager(val uiImageMap: MutableMap<View, ImageBitmap?>) {
    private lateinit var imageAndData: ImageAndData<ArrayOps>
    private lateinit var size: MySize3

    var mode = Mode.NONE
    var displaying = Displaying.THREE

    fun loadDicom() = CoroutineScope(Dispatchers.Default).launch {

        val dirName = ReadHelp.pickDirAndDicom().first
        val time = measureTimeMillis {
            imageAndData = loadDicomData(dirName)
        }
        println("Loaded dicom data in $time ms")
        size = imageAndData.imageArray.size
        println("3D array is $size")
        uiImageMap.forEach { (key, value) ->
            uiImageMap[key] = getImage(key, depthValues[key]!!)
        }
        println()
        //printInfoOnce()
        println()
    } // loadDicom.launch end

    fun getSliceImage() {}
    fun onClick() {}
    fun onRelease() {}

    /** **Only to compare** with previous values. Do not use in any computations! */
    val sliderVals: MutableMap<View, Int> = mutableMapOf(
        View.SLICE to 0,
        View.SIDE to 0,
        View.TOP to 0,
    )
    fun viewSliderChange(depth256: Float, view: View) {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return } // yes, slider may be moved before loadDicom()

        val normDepth = Config.sliderRange.normalizeValue(depth256)
        depthValues[view] = normDepth
        //println("$view slider change, $depth256. SliceD $depthSlice, SideD $depthSide, TopD $depthTop")

        // skip calling getImage with same value as before
        val imgDepth = normToImageDepth(view, normDepth)
        if (sliderVals[view] == imgDepth) {
            //println("Same $imgDepth value as before")
            return
        }
        sliderVals[view] = imgDepth
        // up to this point all operations in this function are light, thus not in CoroutineScope

        CoroutineScope(Dispatchers.Default).launch {
            uiImageMap[view] = getImage(view, normDepth).also { img ->
                // .let{} when not null and no other process writes
                if (img == null) {
                    println("Failed to get image for $view")
                } else {
                    println("Got $view image $imgDepth of size ${img.width}x${img.height}")
                    println()
                }
            }
            // assign, when getImage completes, without blocking
        }

        /*
        val deferredImage = CoroutineScope(Dispatchers.Default).async {
            getImage(view, normDepth).also { img ->
                // .let{} when not null and no other process writes
                if (img == null) {
                    println("Failed to get image for $view")
                } else {
                    println("Got $view image $imgDepth of size ${img.width}x${img.height}")
                    println()
                }
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            uiImageMap[view] = deferredImage.await()     // assign, when getImage completes, without blocking
        }
         */
    }

    // normalized depths
    val startDepth = { Config.sliderRange.normalizeValue(Config.sliderRange.startVal) }
    val depthValues: MutableMap<View, Float> = mutableMapOf(
        View.SLICE to startDepth(),
        View.SIDE to startDepth(),
        View.TOP to startDepth(),
    )
    private fun maxDepth(view: View) = when(view) {
        View.SLICE -> size.depth
        View.SIDE -> size.width
        View.TOP -> size.height
    }

    private fun normToImageDepth(view: View, normDepth: Float): Int = (normDepth * maxDepth(view)).toInt()

    /**  */
    private fun getImage(view: View, depth: Float): ImageBitmap? {
        if(!::imageAndData.isInitialized || !::size.isInitialized) {
            throw Exception("Don't call getImage() if imageAndData or size are not initialized!")
        }
        val minDicomVal = ((imageAndData.dataMap[tagAsUInt("[0028 0106]")]?: throw tagNotFoundErr("[0028 0106]")).value as UInt).toInt()
        val maxDicomVal = ((imageAndData.dataMap[tagAsUInt("[0028 0107]")]?: throw tagNotFoundErr("[0028 0107]")).value as UInt).toInt()
        val rescItDat = imageAndData.dataMap[tagAsUInt("[0028 1052]")]?: throw tagNotFoundErr(tagAsUInt("[0028 1052]"))
        val rescSlDat = imageAndData.dataMap[tagAsUInt("[0028 1053]")]?: throw tagNotFoundErr(tagAsUInt("[0028 1053]"))
        val rescaleFunction = InterpretData.interpretRescale(rescItDat, rescSlDat)
        val minRescaled = rescaleFunction(minDicomVal.toShort())
        val maxRescaled = rescaleFunction(maxDicomVal.toShort())
        // Short.MIN_VALUE..Short.MAX_VALUE // snowy
        // -1030..3700 // nice, but hardcoded
        // 0..Short.MAX_VALUE // completely dark
        // minDicomVal..maxDicomVal // contours
        // minRescaled..maxRescaled // correct <--

        return getComposeImage(imageAndData, view, depth, minRescaled..maxRescaled)
    }

    fun printInfoOnce() {
        if(printed) return
        printed = true
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return }

        val info = imageAndData.dataMap
        for(key in InterpretData.necessaryInfo) {
            if(key == tagAsUInt("(7FE0,0010)")) {
                //if(info.containsKey(key)) println("Tag (7FE0,0010) is not supposed to be in tag to data map!")
                continue
            }
            val value = info[key]
            println(value.toString())
        }

    }
    companion object {
        private var printed = false // does not guard in concurrent situation
    }
}