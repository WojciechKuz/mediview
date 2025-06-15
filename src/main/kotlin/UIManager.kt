import androidx.compose.ui.graphics.ImageBitmap
import dicom.tagAsUInt
import transform3d.ArrayOps
import transform3d.ImageAndData
import transform3d.InterpretData
import transform3d.View
import transform3d.getComposeImage

// val trigger: () -> Unit

class UIManager(var writeImgRef: ImageBitmap? = null) {
    private var imageAndData: ImageAndData<ArrayOps>? = null
    fun loadDicom() {
        imageAndData = loadDicomData()
        val whd = imageAndData?.imageArray?.whd
        println("3D array is $whd")
        println()
        printInfoOnce()
        println()
    }
    fun getSliceImage() {}
    fun onClick() {}
    fun onRelease() {}
    fun sliderChange(depth256: Float, view: View) {
        when(view) {
            View.SLICE -> depthSlice = depth256 / 256f; // FIXME hardcoded
            View.SIDE -> depthSide = depth256 / 256f;
            View.TOP -> depthTop = depth256 / 256f;
        }
        println("slider change")
        //println("trigger")
        //trigger()
        getImage(view)
    }
    var depthSlice = 0f
    var depthSide = 0f
    var depthTop = 0f
    /** For starting view */
    fun getImage(view: View): ImageBitmap? {
        val depth = when(view) {
            View.SLICE -> depthSlice
            View.SIDE -> depthSide
            View.TOP -> depthTop
        }
        println("getImage")
        if(imageAndData == null)
            println("imageAndData is null")
        writeImgRef = imageAndData?.let { // when not null and no other process writes
            getComposeImage(it, view, depth.toDouble())
        }
        writeImgRef?.let {
            println("Got image of size ${it.width}x${it.height}")
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
                println("Tag (7FE0,0010) is not supposed to be in tag to data map!")
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