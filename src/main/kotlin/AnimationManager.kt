import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import transform3d.Angle
import transform3d.ArrayOps
import transform3d.ExtView
import transform3d.Indices3
import transform3d.MySize3
import transform3d.createShortArrayWithCoroutines
import transform3d.getComposeImageAngled
import transform3d.justForLoop
import kotlin.collections.get

class AnimationManager(val managerRef: UIManager) {
    var animStartAngles = mutableMapOf(
        Angle.XZAngle to 0f,
        Angle.YZAngle to 0f
    )
    var animEndAngles = mutableMapOf(
        Angle.XZAngle to 0f,
        Angle.YZAngle to 0f
    )
    var animFrameCount = 0

    var depth = 0 // for now, leave 0

    private var volume: ArrayOps? = null
    private var size: MySize3
    private var indi: Indices3
    init {
        val sz = Config.animationResolution
        size = MySize3(sz, sz, sz)
        indi = Indices3(size)
        setVolume(managerRef.arrayOps)
    }
    /** Use in init
     * @param source should contain cube sized array. This means x = y = z. */
    private fun setVolume(source: ArrayOps) = CoroutineScope(Dispatchers.Default).launch {
        val sourceIndi = Indices3(source.size)
        val sizeScale = source.size.width / size.width // this times smaller: 4
        val targetVolume = createShortArrayWithCoroutines(size.total) { i -> // i = targetIndi.total
            source.array[ indi.absoluteIndexOf3(
                sourceIndi.absoluteToX(i * sizeScale),
                sourceIndi.absoluteToY(i * sizeScale),
                sourceIndi.absoluteToZ(i * sizeScale)
            ) ]
        }
        volume = ArrayOps(targetVolume, size.width, size.height)
    }

    /** Generated frames */
    private var frames: MutableList<ImageBitmap> = mutableListOf()
    /** get only the number of animation frames. To set target animation frame count use animFrameCount */
    val framesCount: Int; get() = frames.size

    /** Used in interpolating angle depending on which frame it is.
     * @param index in range 0 to 1 */ // alternative: data0 + index*(data1-data0)
    private fun interpolateValues(data0: Float, data1: Float, index: Float): Float = ( (data0 * (1-index)) + (data1 * index) )

    private suspend fun generateFrame(xzAngle: Double, yzAngle: Double): ImageBitmap? {
        return getComposeImageAngled(volume!!, ExtView.FREE, depth.toFloat(),
            managerRef.adjustedValueRange, yzAngle, xzAngle,
            managerRef.mode, managerRef.color, managerRef.firstHitValue)
    }
    var genFinish: UISetter<Boolean>? = null
    fun generateAnimation() {
        if(volume == null) return
        infoTextSetter?.set("Generating...")
        CoroutineScope(Dispatchers.Default).launch {
            frames = mutableListOf()
            direction = 1
            //coroutineForLoopSus(animFrameCount)
            justForLoop(animFrameCount)
            { i ->
                val frameAngles = animStartAngles.keys.map { key ->
                    key to interpolateValues(
                        animStartAngles[key]!!,
                        animEndAngles[key]!!,
                        i * 1f / animFrameCount
                    ).toDouble()
                }.associate { it }
                val bitmap = generateFrame(frameAngles[Angle.XZAngle]!!, frameAngles[Angle.YZAngle]!!)
                if(bitmap == null) return@justForLoop
                frames.add(bitmap)
            }
        }.invokeOnCompletion {
            infoTextSetter?.set("Animation is ready")
            setSliderPos?.set(0f)
            setFrameRange?.set(0f..(frames.size-1).toFloat())
            genFinish?.set(true)
            System.gc()
        }
    }
    var setFrameRange: UISetter<ClosedFloatingPointRange<Float>>? = null
    var setSliderPos: UISetter<Float>? = null

    ///** control play / pause */
    //var played = false
    //fun playOrPause() { played = !played }
    enum class Animode {
        LOOP,
        REVERSE,
        PAUSE
    }
    var aMode: Animode = Animode.LOOP
    private var direction = 1
    fun nextFrameIdx(currFrameIdx: Int): Int {
        val newIdx = when(aMode) {
            Animode.LOOP -> currFrameIdx % frames.size
            Animode.REVERSE -> {
                when(currFrameIdx) {
                    0 -> direction = 1
                    (frames.size - 1) -> direction = -1
                }
                currFrameIdx + direction
            }
            Animode.PAUSE -> if (currFrameIdx + 1 < frames.size) currFrameIdx + 1 else currFrameIdx
        }
        return newIdx
    }
    fun getFrame(frameIdx: Int) = frames[frameIdx]

    /** for messages like generating progress, etc. */
    var infoTextSetter: UISetter<String>? = null
    fun setInfoText() {}
}