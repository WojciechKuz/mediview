import transform3d.Interpolation
import transform3d.sample
import kotlin.test.Test

class InterpolTest {
    val startArray = listOf(1536, 1040, 1040, 1072, 976, 1040, 432, 16).map {it.toShort()}.toTypedArray()
    @Test
    fun testRescaleBL() {
        val result = Interpolation.rescaleBL(startArray, 1.25).toList()
        println(result)
    }
    @Test
    fun testMoveBL() {
        val result = Interpolation.moveBL(startArray, 0.5).toList()
        println(result)
    }

    @Test
    fun testSample() {
        val step = 10
        var printCount = 0
        for (i in 0..3632 step step) {
            val x = sample(i.toShort(), 0, Short.MAX_VALUE)
            print(" [${i.toString(16).padStart(4)}]: ${x.toString(16).padEnd(4)} ")
            if(printCount % 10 == 9) println()
            printCount++
        }
    }
}