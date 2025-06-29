package transform3d

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch


class MySize3(val width: Int, val height: Int, val depth: Int) {
    constructor(whd: WidthHeightDepth): this(whd.width, whd.height, whd.depth)
    val total: Int; get() = width * height * depth
    fun toWhd() = WidthHeightDepth(width, height, depth)
    override fun toString(): String {
        return "(w:$width, h:$height, d:$depth)"
    }
}
class MySize2(val width: Int, val height: Int) {
    val total: Int; get() = width * height
    override fun toString(): String {
        return "(w:$width, h:$height)"
    }
}
class Indices3(val size: MySize3) {
    constructor(size: WidthHeightDepth): this(MySize3(size))
    fun absoluteIndexOf3(x: Int, y: Int, z: Int) = (z * size.height + y) * size.width + x
    val absoluteSize = size.width * size.height * size.depth
    val absoluteToX = {absIndex: Int -> absIndex%size.width }
    val absoluteToY = {absIndex: Int -> absIndex/size.width%size.height }
    val absoluteToZ = {absIndex: Int -> absIndex/size.width/size.height }
    fun valueAtAbsIndexInArray(array: Array<Array<Array<Short>>>, absIndex: Int) =
            array [absIndex/size.width/size.height] [absIndex/size.width%size.height] [absIndex%size.width]
    fun valueAtAbsIndexInArray(array: ShortArray, absIndex: Int) =
        array[absIndex]
}

/** whd.depth is ignored */
class Indices2(val size: MySize2) {
    fun absoluteIndexOf2(x: Int, y: Int) = y * size.width + x
    val absoluteSize = size.width * size.height
    val absoluteToX = {absIndex: Int -> absIndex%size.width }
    val absoluteToY = {absIndex: Int -> absIndex/size.width }
    fun valueAtAbsIndexInArray(array: Array<Array<Array<Short>>>, absIndex: Int) =
        array [absIndex/size.width/size.height] [absIndex/size.width%size.height] [absIndex%size.width]
    fun valueAtAbsIndexInArray(array: ShortArray, absIndex: Int) =
        array[absIndex]
}

///** Execute for loop. Execution is divided by `Config.useThreads` and each part is executed by different coroutine. */
suspend fun coroutineForLoop(loopTimes: Int, thingToDo: (Int) -> Unit) = coroutineForLoopSus(loopTimes, thingToDo)

/** Execute for loop. Execution is divided by `Config.useThreads` and each part is executed by different coroutine. */
suspend fun coroutineForLoopSus(loopTimes: Int, thingToDo: suspend (Int) -> Unit) {
    val threads = Config.useThreads
    val workPerThread = loopTimes / threads
    val jobList = mutableListOf<Job>()

    val job = CoroutineScope(Dispatchers.Default).launch {
        try {
            for(i in (threads-1)*workPerThread until loopTimes) { // this is the last thread, but adding as first because it has a bit more work to do.
                thingToDo(i)
            }
        } catch(e: Exception) {
            println("Exception dla thread ${threads-1}. Prawdopodobnie podano zły rozmiar do doSomethingOnYXArrayOfZArrays().")
            e.printStackTrace()
        }
    }
    jobList.add(job)

    for (thr in 0 until (threads-1)) { // all threads excluding last one
        val job = CoroutineScope(Dispatchers.Default).launch {
            try {
                for(i in thr*workPerThread until (thr+1)*workPerThread) {
                    thingToDo(i)
                }
            } catch(e: Exception) {
                println("Exception dla thread $thr. Prawdopodobnie podano zły rozmiar do doSomethingOnYXArrayOfZArrays().")
                e.printStackTrace()
            }
        }
        jobList.add(job)
    }

    jobList.joinAll()
}

suspend fun justForLoop(loopTimes: Int, thingToDo: suspend (Int) -> Unit) {
    for(i in 0 until loopTimes) {
        thingToDo(i)
    }
}

/** Create an ShortArray with element initialization. Initialization of the array is divided by `Config.useThreads` and each part is executed by different coroutine. */
suspend fun createShortArrayWithCoroutines(size: Int, init: (Int) -> Short): ShortArray {
    val array = ShortArray(size)
    val threads = Config.useThreads
    val workPerThread = size / threads
    val jobList = mutableListOf<Job>()

    val job = CoroutineScope(Dispatchers.Default).launch {
        for(i in (threads-1)*workPerThread until size) { // this is last thread, but adding as first because it has a bit more work to do.
            array[i] = init(i)
        }
    }
    jobList.add(job)

    for (thr in 0 until (threads-1)) {

        val job = CoroutineScope(Dispatchers.Default).launch {
            for(i in thr*workPerThread until (thr+1)*workPerThread) {
                array[i] = init(i)
            }
        }
        jobList.add(job)
    }

    jobList.joinAll()
    return array
}
