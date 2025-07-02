import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import transform3d.Angle
import transform3d.ArrayOps
import transform3d.ExtView
import transform3d.Indices3
import transform3d.MySize3
import transform3d.View
import transform3d.createShortArrayWithCoroutines
import transform3d.getComposeImageAngled
import transform3d.justForLoop
import transform3d.printAngles
import transform3d.InterpolationSA as Interpol

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

    var depth = 0.5f // for now, leave it

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
        val sizeScale = source.size.width / size.width // this times smaller: 4 // 512/128 = 4
        val targetVolume = createShortArrayWithCoroutines(size.total) { i -> // i = targetIndi.total
            source.array[ sourceIndi.absoluteIndexOf3(
                indi.absoluteToX(i) * sizeScale,
                indi.absoluteToY(i) * sizeScale,
                indi.absoluteToZ(i) * sizeScale
            ) ]
        }
        volume = ArrayOps(targetVolume, size.width, size.height)
    }

    /** Generated frames */
    private var frames: MutableList<ImageBitmap> = mutableListOf()
    /** get only the number of animation frames. To set target animation frame count use animFrameCount */
    val framesCount: Int; get() = frames.size
    val allFrames: List<ImageBitmap>; get() = frames

    private suspend fun generateFrame(xzAngle: Double, yzAngle: Double): ImageBitmap? {
        return getComposeImageAngled(volume!!, ExtView.FREE, depth,
            managerRef.adjustedValueRange, yzAngle, xzAngle,
            managerRef.mode, managerRef.color, managerRef.firstHitValue)
    }
    var genFinish: UISetter<Boolean>? = null

    fun generateAnimation() {
        if(volume == null) return
        println("Generating $animFrameCount frames of animation with starting angle ${printAngles(animStartAngles)} and end ${printAngles(animEndAngles)}")
        infoTextSetter?.set("Generating...")
        CoroutineScope(Dispatchers.Default).launch {
            frames = mutableListOf()
            direction = 1
            //coroutineForLoopSus(animFrameCount)
            justForLoop(animFrameCount)
            { i ->
                val frameAngles = interpolatedAngles(animStartAngles, animEndAngles, i * 1f / animFrameCount).toMutableMap()
                when(Config.animateView) {
                    View.SLICE -> {} // do nothing
                    View.SIDE -> {
                        frameAngles[Angle.XZAngle] = frameAngles[Angle.XZAngle]!! + 90f
                    }
                    View.TOP -> {
                        frameAngles[Angle.XZAngle] = frameAngles[Angle.XZAngle]!! + 90f
                        frameAngles[Angle.YZAngle] = frameAngles[Angle.YZAngle]!! + 90f
                    }
                }
                val bitmap = generateFrame(frameAngles[Angle.XZAngle]!!.toDouble(), frameAngles[Angle.YZAngle]!!.toDouble())
                if(bitmap == null) {
                    println("return due to null bitmap")
                    return@justForLoop
                }
                frames.add(bitmap)
            }
        }.invokeOnCompletion {
            infoTextSetter?.set("Animation is ready")
            println("Animation is ready")
            println("frames array has ${frames.size} frames")
            frozenStartAngles = animStartAngles.toMap()
            frozenEndAngles = animEndAngles.toMap()
            setSliderPos?.set(0f)
            setFrameRange?.set(0f..(frames.size-1).toFloat())
            genFinish?.set(true)
            System.gc()
        }
    }
    var setFrameRange: UISetter<ClosedFloatingPointRange<Float>>? = null
    var setSliderPos: UISetter<Float>? = null

    enum class Animode {
        LOOP,
        REVERSE,
        PAUSE
    }
    var aMode: Animode = Animode.LOOP
    private var direction = 1
    fun nextFrameIdx(currFrameIdx: Int): Int {
        val newIdx = when(aMode) {
            Animode.LOOP -> if(frames.isNotEmpty()) (currFrameIdx + 1) % frames.size else currFrameIdx
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
    private var frozenStartAngles = mapOf(
        Angle.XZAngle to 0f,
        Angle.YZAngle to 0f
    )
    private var frozenEndAngles = mapOf(
        Angle.XZAngle to 0f,
        Angle.YZAngle to 0f
    )
    fun interpolatedAngles(startAngles: Map<Angle, Float>, endAngles: Map<Angle, Float>, index: Float): Map<Angle, Float> {
        return startAngles.keys.map { key ->
            key to Interpol.interpolate2Values(
                startAngles[key]!!,
                endAngles[key]!!,
                index
            )
        }.associate { it }
    }

    fun safeGetFrame(frameIdx: Int, whenGotFrame: (ImageBitmap) -> Unit) {
        if(frameIdx < frames.size) {
            infoTextSetter?.set(
                "Animation is ready. Current angles ${printAngles(interpolatedAngles(frozenStartAngles, frozenEndAngles, frameIdx * 1f / frames.size))}"
            )
            whenGotFrame(frames[frameIdx])
        }
        else {
            println("Can't get frame for $frameIdx")
        }
    }
    fun getFrame(frameIdx: Int) = frames[frameIdx]

    /** for messages like generating progress, etc. */
    var infoTextSetter: UISetter<String>? = null
    fun setInfoText() {}
}