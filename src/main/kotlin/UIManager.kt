import androidx.compose.ui.graphics.ImageBitmap
import dicom.tagAsUInt
import transform3d.ArrayOps
import transform3d.Config
import transform3d.ImageAndData
import transform3d.InterpretData
import transform3d.View
import transform3d.getComposeImage
import kotlinx.coroutines.*
import transform3d.MySize3
import kotlin.system.measureTimeMillis

// val trigger: () -> Unit

class UIManager(val uiImageMap: MutableMap<View, ImageBitmap?>) {
    private var imageAndData: ImageAndData<ArrayOps>? = null
    private var size: MySize3? = null

    fun loadDicom() = CoroutineScope(Dispatchers.Default).launch {
        val time = measureTimeMillis {
            imageAndData = loadDicomData()
        }
        println("Loaded dicom data in $time ms")
        size = imageAndData?.imageArray?.size
        println("3D array is $size")
        println()
        //printInfoOnce()
        println()
        imageAndData?.let {
            uiImageMap.forEach { (key, value) ->
                uiImageMap[key] = getImage(key)?: uiImageMap[key]
            }
            /* // DEBUG:
            if(it.imageArray.checkIfAllTheSame())
                println("All images are the same :(")
            else
                println("OK, images differ from each other")
            if(it.imageArray.isSelectedPixelTheSame(true)) // println("selectedPixel is " + it.imageArray.isSelectedPixelTheSame(true).toString())
                println("Selected pixel is the same on all images :(")
            else
                println("OK, selected pixel values differ from each other")
            */
        }
    } // loadDicom.launch end

    fun getSliceImage() {}
    fun onClick() {}
    fun onRelease() {}
    var sliderChange = 0
    fun viewSliderChange(depth256: Float, view: View) {
        when(view) {
            View.SLICE -> depthSlice = Config.sliderRange.normalizeValue(depth256)
            View.SIDE -> depthSide = Config.sliderRange.normalizeValue(depth256)
            View.TOP -> depthTop = Config.sliderRange.normalizeValue(depth256)
        }
        //println("$view slider change, $depth256. SliceD $depthSlice, SideD $depthSide, TopD $depthTop")

        // TODO remember previous getImg args (whichSlice for each view) to not refresh too much
        uiImageMap[view] = getImage(view)
        /*
        if(sliderChange%3==0) {
            println()
            // do stuff
        }
        sliderChange++
        z*/

    }
    val startVal = { Config.sliderRange.normalizeValue(Config.sliderRange.startVal) }
    var depthSlice = startVal() // 0-1 range
    var depthSide = startVal()
    var depthTop = startVal()

    /**  */
    fun getImage(view: View): ImageBitmap? {
        val depth = when(view) {
            View.SLICE -> depthSlice
            View.SIDE -> depthSide
            View.TOP -> depthTop
        }
        val maxDepth = when(view) {
            View.SLICE -> size?.depth
            View.SIDE -> size?.width
            View.TOP -> size?.height
        }
        //println("getImage")
        if(imageAndData == null)
            println("imageAndData is null")
        val writeImgRef: ImageBitmap? = imageAndData?.let { // when not null and no other process writes
            //println("get compose img")
            getComposeImage(it, view, depth)
        }
        writeImgRef?.let {
            println("Got $view image ${if(maxDepth != null) (depth * maxDepth).toInt() else depth} of size ${it.width}x${it.height}")
        }
        println()
        //if(writeImgRef != null)
        uiImageMap[view] = writeImgRef
        return writeImgRef
    }
    //fun writeImage(view: View) { writeImgRef = getImage(view) }

    fun getImage(depth256: Float, view: View): ImageBitmap? {
        TODO()
    }
    //fun writeImage(depth256: Float, view: View) { writeImgRef = getImage(depth256, view) }

    fun printInfoOnce() {
        if(printed) return
        printed = true
        val info = imageAndData?.dataMap
        if(info == null) {
            println("info is null")
            return
        }
        for(key in InterpretData.necessaryInfo) {
            if(key == tagAsUInt("(7FE0,0010)")) {
                if(info.containsKey(key)) println("Tag (7FE0,0010) is not supposed to be in tag to data map!")
                continue
            }
            val value = info[key]
            println(value.toString())
        }

    }
    companion object {
        private var printed = false
    }
}