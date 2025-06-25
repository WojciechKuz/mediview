import androidx.compose.ui.graphics.ImageBitmap
import dicom.tagAsUInt
import transform3d.ArrayOps
import transform3d.ImageAndData
import transform3d.InterpretData
import transform3d.getComposeImage
import kotlinx.coroutines.*
import transform3d.Angle
import transform3d.Displaying
import transform3d.ExtView
import transform3d.Mode
import transform3d.MyColor
import transform3d.MySize3
import transform3d.getComposeImageAngled
import transform3d.tagNotFoundErr
import transform3d.toExtView
import transform3d.toView
import kotlin.math.PI
import kotlin.math.round
import kotlin.system.measureTimeMillis

// val trigger: () -> Unit

class UIManager(val uiImageMap: MutableMap<ExtView, ImageBitmap?>) {
    private lateinit var imageAndData: ImageAndData<ArrayOps>
    private lateinit var size: MySize3
    private val freeQueue = LaunchQueue()
    private val allQueue = LaunchQueue()

    var mode = Mode.EFFICIENT_NONE
    var displaying = Displaying.THREE
    var color = MyColor.GREYSCALE

    /** **Only to compare** with previous values. Do not use in any computations! */
    val depthSliderVals = mutableMapOf(
        ExtView.SLICE to 0,
        ExtView.SIDE to 0,
        ExtView.TOP to 0,
        ExtView.FREE to 0,
    )
    /** **Only to compare** with previous values. Do not use in any computations! */
    val angleSliderVals = mutableMapOf(
        Angle.XZAngle to 0,
        Angle.YZAngle to 0,
    )

    private var firstHitValue: Short = 0 // set real value in loadDicom()
    private var adjustedValueRange = 0..1 // set real value in loadDicom()
    private fun getImageValueRange(): IntRange {
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
        return minRescaled..maxRescaled
    }
    /** set lower end of value range */
    fun setLowestValue(lowest256: Float) {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return } // yes, slider may be moved before loadDicom()

        val imgValRange = getImageValueRange()
        val diff = imgValRange.endInclusive - imgValRange.start
        val normLow = Config.sliderRange.normalizeValue(lowest256)

        val oldUpperLimit = adjustedValueRange.endInclusive
        val lowerLimit = round(imgValRange.start + diff * normLow).toInt()
        if(lowerLimit < oldUpperLimit) {
            adjustedValueRange = lowerLimit..oldUpperLimit
        }

        allQueue.startJob(true) {
            valuesChanged().invokeOnCompletion { allQueue.finishJob() }
        }
    }
    /** set upper end of value range */
    fun setHighestValue(highest256: Float) {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return } // yes, slider may be moved before loadDicom()

        val imgValRange = getImageValueRange()
        val diff = imgValRange.endInclusive - imgValRange.start
        val normHigh = Config.sliderRange.normalizeValue(highest256)

        val oldLowerLimit = adjustedValueRange.start
        val upperLimit = round(imgValRange.start + diff * normHigh).toInt()
        if(upperLimit > oldLowerLimit) {
            adjustedValueRange = oldLowerLimit..upperLimit
        }

        allQueue.startJob(true) {
            valuesChanged().invokeOnCompletion { allQueue.finishJob() }
        }
    }
    /** set first hit value. Only applicable in first hit mode. And None mode? */
    fun setFirstHitValue(fHitVal256: Float) {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return } // yes, slider may be moved before loadDicom()

        val normSlider = Config.sliderRange.normalizeValue(fHitVal256)
        val imgValRange = getImageValueRange()
        val diff = imgValRange.endInclusive - imgValRange.start
        firstHitValue = round(imgValRange.start + diff * normSlider).toInt().toShort()

        allQueue.startJob(true) {
            valuesChanged().invokeOnCompletion { allQueue.finishJob() }
        }
    }


    // UI functions:
    fun loadDicom() = CoroutineScope(Dispatchers.Default).launch {

        val dirName = ReadHelp.pickDirAndDicom().first
        if(dirName.isEmpty()) {
            println("Directory picking abandoned, empty")
            return@launch
        }
        val time = measureTimeMillis {
            imageAndData = loadDicomData(dirName)
        }
        println("Loaded dicom data in $time ms")
        size = imageAndData.imageArray.size
        println("3D array is $size")
        adjustedValueRange = getImageValueRange()
        firstHitValue = adjustedValueRange.start.toShort()

        redrawVisible() // no need to queue

        println()
        //printInfoOnce()
        println()
        System.gc()
    } // loadDicom.launch end

    /** slider in UI moved */
    fun viewSliderChange(depth256: Float, view: ExtView) {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return } // yes, slider may be moved before loadDicom()

        val normDepth = Config.sliderRange.normalizeValue(depth256)
        depthValues[view] = normDepth
        //println("$view slider change, $depth256. SliceD $depthSlice, SideD $depthSide, TopD $depthTop")

        // skip calling getImage with same value as before
        val imgDepth = (normDepth * maxDepth(view)).toInt()
        if (depthSliderVals[view] == imgDepth) {
            //println("Same $imgDepth value as before")
            return
        }
        depthSliderVals[view] = imgDepth
        // up to this point all operations in this function are light, thus not in CoroutineScope

        assignNewImage(view)
    }

    /** angle slider in UI moved */
    fun angleSliderChange(angleVal256: Float, angle: Angle) {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return } // yes, slider may be moved before loadDicom()

        val normAngle = Config.sliderRange.normalizeValue(angleVal256) * 2f - 1f // [0:1] -> [-1:1]
        angleValues[angle] = normAngle

        val imgAngle = (normAngle * 180.0f).toInt(); // [-180:180]
        if(angleSliderVals[angle] == imgAngle) {
            return
        }
        angleSliderVals[angle] = imgAngle
        val view = angle.toExtView()

        // discards previous operations if they didn't complete
        assignNewImage(view)
    }

    /** image tapped */ // TODO tap view
    fun viewTapChange(absx: Float, absy: Float, view: ExtView) {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return } // yes, slider may be moved before loadDicom()

        if(true) { println("viewTapChange not implemented"); return }

        val normDepth = 0f  //Config.sliderRange.normalizeValue(depth256)
        //depthValues[view] = normDepth
        //println("$view slider change, $depth256. SliceD $depthSlice, SideD $depthSide, TopD $depthTop")

        // skip calling getImage with same value as before
        val imgDepth = (normDepth * maxDepth(view)).toInt()
        if (depthSliderVals[view] == imgDepth) {
            //println("Same $imgDepth value as before")
            return
        }
        depthSliderVals[view] = imgDepth
        // up to this point all operations in this function are light, thus not in CoroutineScope

        //assignNewImage(view)
        allQueue.startJob(true) {
            valuesChanged().invokeOnCompletion { allQueue.finishJob() }
        }
    }

    /** When value that affects image changes, trigger redraw.
     * Can be used for buttons, BUT SLIDERS must use it through `allQueue.startJob` */
    private fun valuesChanged(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            redrawVisible()
        }
    }

    /** When value that affects image changes, trigger redraw.
     * Can be used for buttons, BUT SLIDERS must use different function. */
    fun buttonChange() {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return } // yes, slider may be moved before loadDicom()
        valuesChanged()
    }


    // Start values
    val startDepth = { Config.sliderRange.normalizeValue(Config.sliderRange.startVal) }
    private fun maxDepth(view: ExtView) = when(view) {
        ExtView.SLICE -> size.depth
        ExtView.SIDE -> size.width
        ExtView.TOP -> size.height
        ExtView.FREE -> size.depth
    }

    // Normalized values
    val depthValues: MutableMap<ExtView, Float> = mutableMapOf(
        ExtView.SLICE to startDepth(),
        ExtView.SIDE to startDepth(),
        ExtView.TOP to startDepth(),
        ExtView.FREE to startDepth(),
    )
    val angleValues = mutableMapOf( // in range -1.0 to 1.0
        Angle.XZAngle to 0.0f,
        Angle.YZAngle to 0.0f,
    )


    // Getting image
    /** Asynchronously call getImage(view) and put results in uiImageMap. Works for ONE image */
    private fun assignNewImage(view: ExtView) {
        freeQueue.startJob(view == ExtView.FREE) {
            CoroutineScope(Dispatchers.Default).launch {
                // assign, when getImage completes, without blocking
                uiImageMap[view] = getImage(view).also { img ->
                    // .let{} when not null and no other process writes
                    if (img == null) {
                        println("Failed to get image for $view")
                    } else {
                        println("Got $view image of size ${img.width}x${img.height}")
                        println()
                    }
                }
            }. // Coroutine end
            invokeOnCompletion {
                freeQueue.finishJob()
            }
        }
    }

    /** Redraw visible images */
    private suspend fun redrawVisible() {
        if(displaying == Displaying.THREE) {
            // draw the visible three images
            uiImageMap[ExtView.SLICE] = getImage(ExtView.SLICE)
            uiImageMap[ExtView.TOP] = getImage(ExtView.TOP)
            uiImageMap[ExtView.SIDE] = getImage(ExtView.SIDE)
        } else {
            // draw visible FREE image
            uiImageMap[ExtView.FREE] = getImage(ExtView.FREE)
        }
    }

    /** Call only in loadDicom(), redrawAll() and assignNewImage() */
    private suspend fun getImage(view: ExtView): ImageBitmap? {
        if(!::imageAndData.isInitialized || !::size.isInitialized) {
            throw Exception("Don't call getImage() if imageAndData or size are not initialized!")
        }
        val depth: Float = depthValues[view]?: throw Exception("depthValues does not contain $view")


        if(view == ExtView.FREE) {
            val xzAngle = angleValues[Angle.XZAngle]?: throw Exception("angleVals does not contain $view")
            val yzAngle = angleValues[Angle.YZAngle]?: throw Exception("angleVals does not contain $view")

            val composeImg: ImageBitmap?

            val time = measureTimeMillis {
                composeImg = getComposeImageAngled(
                    imageAndData, view, depth, adjustedValueRange,
                    yzAngle * 180.0, xzAngle * 180.0, mode, color, firstHitValue
                )
            }
            println("Angled image in $time ms")
            return composeImg
        }

        val composeImg: ImageBitmap?
        val time = measureTimeMillis {
            composeImg = getComposeImage(
                imageAndData, view.toView(), depth, adjustedValueRange,
                mode, color, firstHitValue)
        }
        println("Image in $time ms")

        return composeImg
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