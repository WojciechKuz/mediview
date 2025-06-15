import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import transform3d.Config
import transform3d.View
/*
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.compose.LocalPlatformContext
*/

/*
@Composable
@Preview
fun App(imgsize: Int, layout3inRow: Boolean = false) {
    MaterialTheme {
        layout3plus1(imgsize, layout3inRow)
        /*
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            //rows = GridCells.Fixed(2),
        ) {
            //items(4)
            // this scope is not @Composable!
        }
        */
    }
}
*/

@Composable
@Preview
fun App(imgsize: Int, layout3inRow: Boolean = false) {

    // get modifier
    val modif = if (layout3inRow) {
        Modifier.width((imgsize * 3).dp).height((imgsize + 60).dp) // 3+1 modifier
    } else {
        Modifier.width((imgsize * 2).dp).height((imgsize * 2).dp) // 2x2 modifier
    }

    // Actual UI:
    MaterialTheme {
        Column(modifier = modif) {
            val imageName = "bounce.jpg" // C:\Users\Wojtek\Documents\Programy_IntelliJ\mediview\
            var filePicked by remember { mutableStateOf(false) } // UI redraw is triggered when value changes

            //val manager = UIManager(imageBitmap)
            var manager: UIManager by remember { mutableStateOf(UIManager()) }
            if (!filePicked) {
                //imageName = pickFile()
                //println("\"$imageName\" chosen.")
                manager.loadDicom()
                filePicked = true
            } else {
                println("LaunchedEffect() again")
            }

            if (layout3inRow) { // 3+1 window
                Box {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            choosePainter(manager.getImage(View.SLICE), imageName),
                            "slice XY",
                            modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Red)
                        )
                        Image(
                            choosePainter(manager.getImage(View.TOP), imageName),
                            "top ZX",
                            modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Green)
                        )
                        Image(
                            choosePainter(manager.getImage(View.SIDE), imageName),
                            "side ZY",
                            modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Blue)
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) { uiSliders(imgsize, true, manager::viewSliderChange) }
            } // end 3+1
            else { // 2x2 window
                Row(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        choosePainter(manager.getImage(View.SLICE), imageName),
                        "slice XY",
                        modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Red)
                        //modifier = Modifier.drawBehind {}
                    )
                    Image(
                        choosePainter(manager.getImage(View.TOP), imageName),
                        "top ZX",
                        modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Green)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        choosePainter(manager.getImage(View.SIDE), imageName),
                        "side ZY",
                        modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Blue)
                    )
                    uiSliders(imgsize, false, manager::viewSliderChange)
                }
            } // end 2x2

        } // main column end
    } // theme end
}

@Composable
@Preview
fun whatsWrongWithSlidersExample() {
    var sliderPosition by remember { mutableStateOf(128f) }
    // why the f*** is activeColor after thumb, even docs specify otherwise.
    // answer: steps is set so high, that ticks fill it completely, and actual track color can't be seen
    val slidcol = SliderDefaults.colors(
        activeTrackColor = Color.Red,
        inactiveTrackColor = Color.Black,
        thumbColor = Color.Red,
        inactiveTickColor = Color.Transparent,
        activeTickColor = Color.Transparent,
    )
    Slider(
        value = sliderPosition,
        valueRange = 0f..256f,
        onValueChange = {
            sliderPosition = it
            println("Red $sliderPosition")
        },
        colors = slidcol,
        steps = 256,
        modifier = Modifier.width(256.dp)
    )
}

fun printMaxMemory() {
    val maxSize = Runtime.getRuntime().maxMemory() / 1024L / 1024.0
    println("Max memory heap size of program: $maxSize MB")
}

fun main() = application {
    // printMaxMemory()
    val imgsize = Config.displayImageSize
    val use3inRowLayout = true
    val state = rememberWindowState(size = DpSize.Unspecified)
    Window(onCloseRequest = ::exitApplication, title = Config.windowName, state = state) {
        App(imgsize, use3inRowLayout)
    }
}
