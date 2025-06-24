import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import transform3d.Displaying
import transform3d.ExtView
import transform3d.Mode
import transform3d.MyColor

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
            var displText by remember { mutableStateOf("Display: ${manager.displaying}") }
            //var color by remember { mutableStateOf(manager.color) }
            var colorText by remember { mutableStateOf("Colors: ${manager.color}") }
            Row {
                Button(
                    onClick = {
                        manager.loadDicom()
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Load again")
                }
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
                        manager.buttonChange()
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
                        if(manager.displaying != Displaying.ANIMATION) // animation is same view as projection, no need to redraw
                            manager.buttonChange()
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(displText)
                }
                Button(
                    onClick = {
                        manager.color = when (manager.color) {
                            MyColor.GREYSCALE -> MyColor.RYGSCALE
                            MyColor.RYGSCALE -> MyColor.GREYSCALE
                        }
                        //color = manager.color
                        colorText = "Colors: ${manager.color}"
                        manager.buttonChange()
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(colorText)
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

            alwaysSliders(manager, mode)

        } // main column end
    } // theme end
}

@Composable
@Preview
fun alwaysSliders(manager: UIManager, mode: Mode) {
    val imgsize = Config.displayImageSize
    Box {
        Row {
            colorfulSlider(imgsize, "min value", getSliderDefaultColors(Color.DarkGray), startVal = Config.sliderRange.minStartVal) {
                manager.setLowestValue(it)
            }
            colorfulSlider(imgsize, "max value", getSliderDefaultColors(Color.DarkGray), startVal = Config.sliderRange.maxStartVal) {
                manager.setHighestValue(it)
            }
            if(mode == Mode.FIRST_HIT /*|| mode == Mode.NONE*/)
                colorfulSlider(imgsize, "first hit min value", getSliderDefaultColors(Color.DarkGray), startVal = Config.sliderRange.minStartVal) {
                    manager.setFirstHitValue(it)
                }
        }
    }
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
