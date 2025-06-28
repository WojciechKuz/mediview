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
import transform3d.View
import transform3d.getComposeImageAngled
import transform3d.tagNotFoundErr
import transform3d.toExtView
import transform3d.toView
import kotlin.math.round
import kotlin.system.measureTimeMillis

// val trigger: () -> Unit

class UIManager(val uiImageMap: MutableMap<ExtView, ImageBitmap?>) {
    private lateinit var imageAndData: ImageAndData<ArrayOps>
    private lateinit var size: MySize3
    private val freeQueue = LaunchQueue()
    private val allQueue = LaunchQueue()
    /** passes denormalized value to be set. */
    val sliderSetters = mutableMapOf<ExtView, UISetter<Float>>()

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
    var adjustedValueRange = 0..1 // set real value in loadDicom()
        private set
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

        val oldLowerLimit = adjustedValueRange.start
        val oldUpperLimit = adjustedValueRange.endInclusive
        val lowerLimit = round(imgValRange.start + diff * normLow).toInt()
        if (oldLowerLimit == lowerLimit) return
        if(lowerLimit < oldUpperLimit) {
            adjustedValueRange = lowerLimit..oldUpperLimit
        }

        allQueue.startJob(true) {
            valuesChanged().invokeOnCompletion { allQueue.finishJob() }
        }
        minValUpdater?.set(lowerLimit)
    }
    /** set upper end of value range */
    fun setHighestValue(highest256: Float) {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return } // yes, slider may be moved before loadDicom()

        val imgValRange = getImageValueRange()
        val diff = imgValRange.endInclusive - imgValRange.start
        val normHigh = Config.sliderRange.normalizeValue(highest256)

        val oldLowerLimit = adjustedValueRange.start
        val oldUpperLimit = adjustedValueRange.endInclusive
        val upperLimit = round(imgValRange.start + diff * normHigh).toInt()
        if(oldUpperLimit == upperLimit) return
        if(upperLimit > oldLowerLimit) {
            adjustedValueRange = oldLowerLimit..upperLimit
        }

        allQueue.startJob(true) {
            valuesChanged().invokeOnCompletion { allQueue.finishJob() }
        }
        maxValUpdater?.set(upperLimit)
    }
    /** set first hit value. Only applicable in first hit mode. And None mode? */
    fun setFirstHitValue(fHitVal256: Float) {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return } // yes, slider may be moved before loadDicom()

        val normSlider = Config.sliderRange.normalizeValue(fHitVal256)
        val imgValRange = getImageValueRange()
        val diff = imgValRange.endInclusive - imgValRange.start
        val newFirstHitVal = round(imgValRange.start + diff * normSlider).toInt().toShort()
        if(newFirstHitVal == firstHitValue) return
        firstHitValue = newFirstHitVal

        allQueue.startJob(true) {
            valuesChanged().invokeOnCompletion { allQueue.finishJob() }
        }
        firstHitUpdater?.set(firstHitValue.toInt())
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

        textUpdater()
        minValUpdater?.set(adjustedValueRange.start)
        maxValUpdater?.set(adjustedValueRange.endInclusive)
        firstHitUpdater?.set(firstHitValue.toInt())

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

        // skip calling getImage with same value as before
        val imgDepth = (normDepth * maxDepth(view)).toInt()
        if (depthSliderVals[view] == imgDepth) {
            return
        }
        depthSliderVals[view] = imgDepth
        // up to this point all operations in this function are light, thus not in CoroutineScope

        assignNewImage(view)
        textUpdater()
    }

    /** angle slider in UI moved */
    fun angleSliderChange(angleVal256: Float, angle: Angle) {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return } // yes, slider may be moved before loadDicom()

        val normAngle = Config.sliderRange.normalizeValue(angleVal256) * 2f - 1f // [0:1] -> [-1:1]
        angleValues[angle] = normAngle

        val imgAngle = (normAngle * 180.0f).toInt() // [-180:180]
        if(angleSliderVals[angle] == imgAngle) {
            return
        }
        angleSliderVals[angle] = imgAngle
        val view = angle.toExtView()

        // discards previous operations if they didn't complete
        assignNewImage(view)
        textUpdater()
    }

    /** image tapped. parameters absx and absy should be normalized. */
    fun viewTapChange(absx: Float, absy: Float, view: ExtView) {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return } // yes, slider may be moved before loadDicom()

        // width of this view as depth of view1, height of this view as depth of view2
        val view1 = widthOfThisViewIsDepthOfView(view.toView()).toExtView()
        val view2 = heightOfThisViewIsDepthOfView(view.toView()).toExtView()

        depthValues[view1] = absx
        depthValues[view2] = absy

        // skip calling getImage with same value as before
        val imgDepth1 = (absx * maxDepth(view1)).toInt()
        val imgDepth2 = (absy * maxDepth(view2)).toInt()
        if (depthSliderVals[view1] == imgDepth1) {
            return
        }
        if (depthSliderVals[view2] == imgDepth2) {
            return
        }
        // set slider when depths changed via tap
        depthSliderVals[view1] = imgDepth1
        depthSliderVals[view2] = imgDepth2

        // update real sliders
        sliderSetters[view1]?.set( Config.sliderRange.denormalize(absx) )
        sliderSetters[view2]?.set( Config.sliderRange.denormalize(absy) )

        assignNewImage(view1)
        assignNewImage(view2)
        textUpdater()
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
    private fun widthOfThisViewIsDepthOfView(view: View): View = when(view) {
        View.SLICE -> View.SIDE
        View.SIDE -> View.SLICE
        View.TOP -> View.SLICE
    }
    private fun heightOfThisViewIsDepthOfView(view: View): View = when(view) {
        View.SLICE -> View.TOP
        View.SIDE -> View.TOP
        View.TOP -> View.SIDE
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

    val angleSetters = mutableMapOf<Angle, UISetter<Float>>()
    /** for showing in UI. */
    fun scaleAngleSlider(sliderVal256: Float): Float {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return 0f }
        val normAngle = Config.sliderRange.normalizeValue(sliderVal256) * 2f - 1f // [0:1] -> [-1:1]
        return normAngle * 180f
    }
    fun scaleDepthSlider(view: ExtView, sliderVal256: Float): Int {
        if(!::imageAndData.isInitialized || !::size.isInitialized) { return Config.sliderRange.startVal.toInt() }
        val normDepth = Config.sliderRange.normalizeValue(sliderVal256)
        return (normDepth * maxDepth(view)).toInt()
    }
    private var textSetter: UISetter<String>? = null
    fun setTextSetter(setter: UISetter<String>) { textSetter = setter }

    /** Update text in row with buttons. Print there position, value, angles. */
    fun textUpdater() {
        if(!::imageAndData.isInitialized || !::size.isInitialized) return
        val mappedDepths = depthValues.keys.associateWith { key -> (depthValues[key]!! * maxDepth(key)).toInt() }
        val depthsText = "x: ${mappedDepths[ExtView.SIDE]}, y: ${mappedDepths[ExtView.TOP]}, z: ${mappedDepths[ExtView.SLICE]}"
        val xzAngle = angleValues[Angle.XZAngle]!! * 90f
        val yzAngle = angleValues[Angle.YZAngle]!! * 90f
        val angleText = "horizontal: ${"%.2f".format(xzAngle)}°, vertical: ${"%.2f".format(yzAngle)}°"
        val valueText = "value: ${valueAt(mappedDepths[ExtView.SIDE]!!, mappedDepths[ExtView.TOP]!!, mappedDepths[ExtView.SLICE]!!)}; "
        when(displaying) {
            Displaying.THREE -> textSetter?.set(valueText + depthsText)
            Displaying.PROJECTION -> textSetter?.set(valueText + "depth ${mappedDepths[ExtView.FREE]}; angles: $angleText")
            Displaying.ANIMATION -> textSetter?.set(valueText + "depth ${mappedDepths[ExtView.FREE]}; angles: $angleText")
        }
        // probably there's a better place to call this function, but it's light enough, so unnecessary calls are ok.
        updateValuesAlong()
    }
    private var minValUpdater: UISetter<Int>? = null
    private var maxValUpdater: UISetter<Int>? = null
    private var firstHitUpdater: UISetter<Int>? = null
    fun setMinValUpdater(setter: UISetter<Int>) { minValUpdater = setter }
    fun setMaxValUpdater(setter: UISetter<Int>) { maxValUpdater = setter }
    fun setFirstHitUpdater(setter: UISetter<Int>) { firstHitUpdater = setter }
    fun valueAt(x: Int, y: Int, z: Int): Short {
        if(!::imageAndData.isInitialized || !::size.isInitialized) return 0
        return imageAndData.imageArray.valueAt(x, y, z)
    }
    /** reference */
    var valuesAlong: MutableMap<View, ShortArray>? = null
    /** return value along given view's depth */
    fun valueAlong(x: Int, y: Int, z: Int, view: View): ShortArray {
        if(!::imageAndData.isInitialized || !::size.isInitialized) return shortArrayOf(50, 60, 70, 60, 60, 60, 90)
        return when(view) {
            View.SLICE -> imageAndData.imageArray.getValuesAlongZ(x, y)
            View.SIDE -> imageAndData.imageArray.getValuesAlongX(z, y)
            View.TOP -> imageAndData.imageArray.getValuesAlongY(z, x)
        }
    }
    fun updateValuesAlong() {
        val mappedDepths = depthValues.keys.associateWith { key -> (depthValues[key]!! * maxDepth(key)).toInt() }
        if(mode == Mode.NONE && displaying == Displaying.THREE && valuesAlong != null) {
            // 3x512
            fun setValuesAlong(view: View) {
                valuesAlong!![view] = valueAlong(mappedDepths[ExtView.SIDE]!!,mappedDepths[ExtView.TOP]!!, mappedDepths[ExtView.SLICE]!!, view)
            }
            setValuesAlong(View.SLICE)
            setValuesAlong(View.SIDE)
            setValuesAlong(View.TOP)
        }
    }


    // Getting image
    /** Asynchronously call getImage(view) and put results in uiImageMap. Works for ONE image */
    private fun assignNewImage(view: ExtView) {
        freeQueue.startJob(/*view == ExtView.FREE*/ true) {
            CoroutineScope(Dispatchers.Default).launch {
                // assign, when getImage completes, without blocking
                uiImageMap[view] = getImage(view).also { img ->
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