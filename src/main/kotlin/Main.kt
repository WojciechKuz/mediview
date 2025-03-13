import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState



@Composable
@Preview
fun App(imgsize: Int, layout3inRow: Boolean = false) {
    MaterialTheme {
        /*
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            //rows = GridCells.Fixed(2),
        ) {
            //items(4)
            // this scope is not @Composable!
        }
        */
        if (layout3inRow) {
            layout3plus1(imgsize)
        }
        else {
            layout2x2(imgsize)
        }
    }
}

@Composable
@Preview
fun layout2x2(imgsize: Int) {
    Column(modifier = Modifier.width((imgsize*2).dp).height((imgsize*2).dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(painterResource("topBridgeEnter.png"), "opis",
                modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Red)
                //modifier = Modifier.drawBehind {}
            )
            Image(painterResource("topBridgeEnter.png"), "opis",
                modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Green)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(painterResource("topBridgeEnter.png"), "opis",
                modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Blue)
            )
            uiSliders(imgsize, false)
        }
    }
}

@Composable
@Preview
fun layout3plus1(imgsize: Int) {
    Column(modifier = Modifier.width((imgsize*3).dp).height((imgsize+60).dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(painterResource("topBridgeEnter.png"), "opis",
                modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Red)
            )
            Image(painterResource("topBridgeEnter.png"), "opis",
                modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Green)
            )
            Image(painterResource("topBridgeEnter.png"), "opis",
                modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Blue)
            )
        }
        Box(modifier = Modifier.fillMaxSize()) { uiSliders(imgsize, true) }
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

fun main() = application {
    val imgsize = 512
    val use3inRowLayout = true
    val state = rememberWindowState(size = DpSize.Unspecified)
    Window(onCloseRequest = ::exitApplication, title = "MediView by wojkuzb", state = state) {
        App(imgsize, use3inRowLayout)
    }
}
