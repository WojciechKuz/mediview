import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import transform3d.Angle
import transform3d.Config
import transform3d.Displaying
import transform3d.ExtView
import transform3d.Mode
import transform3d.View
import kotlin.collections.getValue
import kotlin.collections.setValue

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

fun imageModifier(imgsize: Int, color: Color) =
    Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, color)

val rescaleWidth = if(Config.uiRescaleWidth) ContentScale.FillBounds else ContentScale.Fit

@Composable
@Preview
fun App() {

    // get values
    val imgsize = Config.displayImageSize
    val modif = Modifier.width((imgsize * 3).dp).height((imgsize + 60 + 60 + 60).dp) // 3+1 modifier

    // Actual UI:
    MaterialTheme {
        Column(modifier = modif) {
            var filePicked by remember { mutableStateOf(false) } // UI redraw is triggered when value changes

            val uiImageMap: MutableMap<ExtView, ImageBitmap?> = remember { mutableStateMapOf(
                ExtView.SLICE to null as ImageBitmap?,
                ExtView.SIDE to null as ImageBitmap?,
                ExtView.TOP to null as ImageBitmap?,
                ExtView.FREE to null as ImageBitmap?,
            ) }
            var manager: UIManager by remember { mutableStateOf(UIManager(uiImageMap)) }
            if (!filePicked) {
                manager.loadDicom()
                filePicked = true
            } else {
                //println("LaunchedEffect() again")
            }

            var mode by remember { mutableStateOf(manager.mode) }
            var modeText by remember { mutableStateOf("Mode: ${manager.mode}") }
            var displ by remember { mutableStateOf(manager.displaying) }
            var displText by remember { mutableStateOf("Mode: ${manager.displaying}") }
            Row {
                Button(
                    onClick = {
                        manager.mode = when (manager.mode) {
                            Mode.NONE -> Mode.MEAN
                            Mode.MEAN -> Mode.MAX
                            Mode.MAX -> Mode.FIRST_HIT
                            Mode.FIRST_HIT -> Mode.NONE
                        }
                        mode = manager.mode
                        modeText = "Mode: ${manager.mode}"
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(modeText)
                }
                Button(
                    onClick = {
                        manager.displaying = when (manager.displaying) {
                            Displaying.THREE -> Displaying.PROJECTION
                            Displaying.PROJECTION -> Displaying.ANIMATION
                            Displaying.ANIMATION -> Displaying.THREE
                        }
                        displ = manager.displaying
                        displText = "Display: ${manager.displaying}"
                    },
                ) {
                    Text(displText)
                }
            }
            when(displ) {
                Displaying.THREE -> {
                    threeImagesBlock(imgsize, uiImageMap, manager)
                    threeSlidersBlock(imgsize, manager)
                }
                Displaying.PROJECTION -> {
                    projectionBlock(imgsize, uiImageMap, manager)
                }
                Displaying.ANIMATION -> {
                    animationBlock(imgsize, uiImageMap, manager)
                }
            }

        } // main column end
    } // theme end
}

@Composable
@Preview
fun projectionBlock(imgsize: Int, uiImageMap: MutableMap<ExtView, ImageBitmap?>, manager: UIManager) {
    Box {
        Image(
            choosePainter(uiImageMap[ExtView.FREE], "loading.jpg"),
            "slice XY",
            modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.DarkGray)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val absx = offset.x / size.width
                            val absy = offset.y / size.height
                        },
                    )
                }
        )
    }
    Box {
        uiSliderBox(imgsize, true) {
            singleSlider(imgsize, "kąt poziomy") { manager.angleSliderChange(it, Angle.XZAngle) }
            singleSlider(imgsize, "kąt pionowy") { manager.angleSliderChange(it, Angle.YZAngle) }
            singleSlider(imgsize, "głębokość") { manager.viewSliderChange(it, ExtView.FREE)}
        }
    }
}

@Composable
@Preview
fun animationBlock(imgsize: Int, uiImageMap: MutableMap<ExtView, ImageBitmap?>, manager: UIManager) {
    Box {
        Image(
            choosePainter(uiImageMap[ExtView.FREE], "loading.jpg"),
            "slice XY",
            modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.DarkGray),
        )
    }
    Box {
        singleSlider(imgsize, "nic") {}
    }
}


@Composable
@Preview
fun threeImagesBlock(imgsize: Int, uiImageMap: MutableMap<ExtView, ImageBitmap?>, manager: UIManager) {
    val imageName = "loading.jpg" //"bounce.jpg"
    Box {
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(
                choosePainter(uiImageMap[ExtView.SLICE], imageName),
                "slice XY",
                modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Red).
                pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val absx = offset.x / size.width
                            val absy = offset.y / size.height
                        },
                        // Możesz dodać inne gesty, np. onDoubleTap, onLongPress
                    )
                },
            )
            Image(
                choosePainter(uiImageMap[ExtView.TOP], imageName),
                "top ZX",
                modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Green).
                pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val absx = offset.x / size.width
                            val absy = offset.y / size.height
                        },
                        // inne: onDoubleTap, onLongPress
                    )
                },
                contentScale = rescaleWidth,
            )
            Image(
                choosePainter(uiImageMap[ExtView.SIDE], imageName),
                "side ZY",
                modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Blue).
                pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val absx = offset.x / size.width
                            val absy = offset.y / size.height
                        },
                        // inne: onDoubleTap, onLongPress
                    )
                },
                contentScale = rescaleWidth,
            )
        }
    }
}
@Composable
@Preview
fun threeSlidersBlock(imgsize: Int, manager: UIManager) {
    Box(modifier = Modifier.fillMaxSize()) { uiSliders(imgsize, true, manager::viewSliderChange) }
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
    val state = rememberWindowState(size = DpSize.Unspecified)
    Window(onCloseRequest = ::exitApplication, title = Config.windowName, state = state) {
        App()
    }
}
