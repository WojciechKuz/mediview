import dicom.fileToImageBitmap
import dicom.imageBitmapToByteArray
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import transform3d.ArrayOps
import transform3d.rawByteArrayToImageBitmap
import transform3d.ArrayOps.Companion.flattenListToArray
import transform3d.Indices3
import transform3d.InterpolationSA
import transform3d.InterpolationSA.moveBL
import transform3d.MySize3
import java.awt.FileDialog
import java.awt.Frame
import kotlin.collections.toShortArray
import kotlin.test.Test

class ImageTest {

    @Test
    fun imageToBytesAndBack() {
        val dialog = FileDialog(null as Frame?, "Select JPEG File to Open", FileDialog.LOAD)
        dialog.isVisible = true
        val filepath = dialog.directory + dialog.file
        println("File: " + filepath)
        val bitmap = fileToImageBitmap(filepath)
        if (bitmap == null) {
            println("Failed to get bitmap")
            return
        }
        println("Got bitmap with info: ${bitmap.config}")
        val info = ImageInfo(
            bitmap.width,
            bitmap.height,
            ColorType.RGBA_8888,
            ColorAlphaType.OPAQUE
        )
        val bytes = imageBitmapToByteArray(bitmap, info)
        println("Got ${bytes.size} bytes")
        val img = rawByteArrayToImageBitmap(bytes, bitmap.width, bitmap.height, 4)
        println("Got image back!")
    }

    @Test
    fun testIsArrayWritable() {
        val arr = arrayOf(50, 60, 70, 80, 90)
        println(arr.toList().toString())
        for (i in arr.indices) {
            arr[i] = arr[i] + 2
        }
        println(arr.toList().toString())
    }

    @Test
    fun testFlattenList() {
        val list = listOf(arrayOf(1, 2, 3, 4, 5), arrayOf(50, 60, 70, 80, 90))
        println("[" + list[0].toList().toString() + ", " + list[1].toList().toString() + "]")
        val flat = list.flattenListToArray()
        println(flat.toList().toString())
    }

    @Test
    fun indexingTest() {
        val indi3 = Indices3(MySize3(2, 3, 4))
        val list = List(24) { i ->
            (indi3.absoluteToX(i) + indi3.absoluteToY(i) * 10 + indi3.absoluteToZ(i) * 100).toString().padStart(3, '0')
        }.toMutableList()
        println(list.toString())
        for(zi in 0 until indi3.size.depth) {
            for(yi in 0 until indi3.size.height) {
                for(xi in 0 until indi3.size.width) {
                    list[indi3.absoluteIndexOf3(xi, yi, zi)] = indi3.absoluteIndexOf3(xi, yi, zi).toString()
                }
            }
        }
        println(list.toString())
    }

    @Test
    fun arrayOpsTest() { // this test works only when doSomethingOnYXArrayOfZArrays is not suspend fun
        val indi3 = Indices3(MySize3(2, 3, 4))
        val list = List(24) { i ->
            (indi3.absoluteToX(i) + indi3.absoluteToY(i) * 10 + indi3.absoluteToZ(i) * 100).toShort() //.toString().padStart(3, '0') // only for displaying
        }
        println(list.toString())
        val arr = list.toTypedArray().toShortArray()
        val arrOp = ArrayOps(arr, indi3.size.width, indi3.size.height)
        /*arrOp.doSomethingOnYXArrayOfZArrays { zArr, yxi ->
            ShortArray(indi3.size.depth) { zi -> yxi.toShort() }
        }*/
        println(arrOp.array.toList().toString())
    }

    @Test
    fun testGantryAndScaleTransform() {
        val gantryAngle = -60.0//-16
        val scaleBy = 1.25//2.4
        val scaleIntercept = -1024
        val scaleSlope = 1

        val indi3 = Indices3(MySize3(2, 3, 4))
        val list = List(24) { i ->
            (indi3.absoluteToX(i) + indi3.absoluteToY(i) * 10 + indi3.absoluteToZ(i) * 100).toShort() //.toString().padStart(3, '0') // only for displaying
        }
        //println(list.toString())
        val arr = list.toTypedArray().toShortArray()
        arr[0] = 0
        arr[23] = 3367
        val arrOp = ArrayOps(arr, indi3.size.width, indi3.size.height)
        val gantryLambda = arrOp.prepareLambdaForShearByGantry(gantryAngle,
            InterpolationSA::moveBL, 0)
        val scaleLambda = arrOp.prepareLambdaForScaleZ(scaleBy, InterpolationSA::rescaleBL)

        val zlist = listOf(0.toShort(), 5.toShort(), 3.toShort(), 3367.toShort())
        println(zlist.toString())
        val shArr = zlist.toTypedArray().toShortArray()
        println("\nGantry only for 0: " + gantryLambda(shArr, 0).toList().toString())
        println("Gantry only for 1: " + gantryLambda(shArr, 1).toList().toString())
        println("Gantry only for 2: " + gantryLambda(shArr, 2).toList().toString())
        println("\nScale only for 0: " + scaleLambda(shArr, 0).toList().toString())
        println("Scale only for 1: " + scaleLambda(shArr, 1).toList().toString())
        println("Scale only for 2: " + scaleLambda(shArr, 2).toList().toString())
        println("\ncombined: " + gantryLambda(scaleLambda(shArr, 0), 0).toList().toString())
        println("combined: " + gantryLambda(scaleLambda(shArr, 1), 1).toList().toString())
        println("combined: " + gantryLambda(scaleLambda(shArr, 2), 2).toList().toString())
    }

    @Test
    fun arrayOpsGantryAndScale() {
        val gantryAngle = -60.0//-16
        val scaleBy = 1.25//2.4
        val scaleIntercept = -1024
        val scaleSlope = 1

        val indi3 = Indices3(MySize3(2, 3, 4))
        val list = List(24) { i ->
            (indi3.absoluteToX(i) + indi3.absoluteToY(i) * 10 + indi3.absoluteToZ(i) * 100).toShort() //.toString().padStart(3, '0') // only for displaying
        }
        //println(list.toString())
        val arr = list.toTypedArray().toShortArray()
        arr[0] = 0
        arr[23] = 3367
        println(arr.toList().toString())
        val arrOp = ArrayOps(arr, indi3.size.width, indi3.size.height)
        println(arrOp.array.toList().toString())

        val gantryLambda = arrOp.prepareLambdaForShearByGantry(gantryAngle,
            InterpolationSA::moveBL, 0)
        val scaleLambda = arrOp.prepareLambdaForScaleZ(scaleBy, InterpolationSA::rescaleBL)
        println(arrOp.size)
        println(arrOp.array[23])

        println("\nZ arrays in processing:")
        /*arrOp.doSomethingOnYXArrayOfZArrays { shArr, yxi ->
            println(shArr.toList().toString())
            scaleLambda(shArr, yxi)
            //gantryLambda(shArr, yxi)
            //gantryLambda(scaleLambda(shArr, yxi), yxi)
        }*/
        println("end processing.\n")

        println(arrOp.array.toList().toString())
        println()
        val zlist = listOf(21.toShort(), 121.toShort(), 221.toShort(), 3367.toShort())
        println(zlist.toString())
        val shArr = zlist.toTypedArray().toShortArray()
        println(scaleLambda(shArr, 0).toList().toString()) // scale lambda ignores 2nd parameter

    }
}