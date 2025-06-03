import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
/*
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.compose.LocalPlatformContext
*/
import java.awt.*
import java.io.ByteArrayInputStream

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
fun getPainterByteArray(bytes: ByteArray): Painter {
    val imageBitmap = byteArrayToImageBitmap(bytes)
    if (imageBitmap != null) {
        return BitmapPainter( imageBitmap )
    }
    return painterResource("imagenotfound512.png")
}

@Composable
fun getPainter(fileName: String): Painter {
    val imageBitmap = fileToImageBitmap(fileName)
    if (imageBitmap != null) {
        return BitmapPainter( imageBitmap )
    }
    return painterResource("imagenotfound512.png")
}




@Composable
@Preview
fun image(painter: Painter, imgsize: Int, color: Color) {
    Image(
        painter, "opis",
        modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, color)
    )
}

@Composable
@Preview
fun layout2x2(imgsize: Int) {
    Column(modifier = Modifier.width((imgsize*2).dp).height((imgsize*2).dp)) {
        var imageName by remember { mutableStateOf("") }
        var filePicked by remember { mutableStateOf(false) } // UI redraw is triggered when value changes
        if (!filePicked) {
            imageName = pickFile()
            println("\"$imageName\" chosen.")
            filePicked = true
        }
        else { println("LaunchedEffect() again") }
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(getPainter(imageName), "opis",
                modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Red)
                //modifier = Modifier.drawBehind {}
            )
            Image(getPainter(imageName), "opis",
                modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.Green)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(getPainter(imageName), "opis",
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
        var imageName by remember { mutableStateOf("") }
        var filePicked by remember { mutableStateOf(false) }
        if (!filePicked) {
            imageName = pickFile()
            println("\"$imageName\" chosen.")
            filePicked = true
        }
        else { println("LaunchedEffect() again") }
        Box {
            Row(modifier = Modifier.fillMaxWidth()) {
                image(getPainter(imageName), imgsize, Color.Red)
                image(getPainter(imageName), imgsize, Color.Green)
                image(getPainter(imageName), imgsize, Color.Blue)
            }
        }
        Box(modifier = Modifier.fillMaxSize()) { uiSliders(imgsize, true) }
    }
}

fun pickFile(): String {
    val dialog = FileDialog(null as Frame?, "Select File to Open", FileDialog.LOAD)
    dialog.isVisible = true
    //return File(dialog.directory, dialog.file).toString()
    return dialog.directory + dialog.file
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
