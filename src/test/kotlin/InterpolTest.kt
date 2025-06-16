import transform3d.Interpolation
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
}