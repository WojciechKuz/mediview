import androidx.compose.ui.graphics.ImageBitmap
import dev.romainguy.kotlin.math.max
import dicom.tagAsUInt
import transform3d.ArrayOps
import transform3d.Config
import transform3d.ImageAndData
import transform3d.InterpretData
import transform3d.View
import transform3d.WidthHeightDepth
import transform3d.getComposeImage

// val trigger: () -> Unit

class UIManager(var writeImgRef: ImageBitmap? = null) {
    private var imageAndData: ImageAndData<ArrayOps>? = null
    private var whd: WidthHeightDepth? = null
    fun loadDicom() {
        imageAndData = loadDicomData()
        whd = imageAndData?.imageArray?.whd
        println("3D array is $whd")
        println()
        printInfoOnce()
        println()
    }
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

        if(sliderChange%3==0) {
            println()
            getImage(view)
        }
        sliderChange++

    }
    var depthSlice = 0f // 0-1 range
    var depthSide = 0f
    var depthTop = 0f
    /** For starting view */
    fun getImage(view: View): ImageBitmap? {
        val depth = when(view) {
            View.SLICE -> depthSlice
            View.SIDE -> depthSide
            View.TOP -> depthTop
        }
        val maxDepth = when(view) {
            View.SLICE -> whd?.depth
            View.SIDE -> whd?.width
            View.TOP -> whd?.height
        }
        //println("getImage")
        if(imageAndData == null)
            println("imageAndData is null")
        writeImgRef = imageAndData?.let { // when not null and no other process writes
            getComposeImage(it, view, depth.toDouble())
        }
        writeImgRef?.let {
            println("Got $view image ${if(maxDepth != null) (depth * maxDepth).toInt() else depth} of size ${it.width}x${it.height}")
        }
        println()
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