import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import transform3d.Config
import transform3d.View

/** Okienko z suwakami.
 * @param sliderValChange float jest od 0 do 256, a View: Red-SLICE, Green-TOP, Blue-SIDE */
@Composable
@Preview
fun uiSliders(imgsize: Int, horizontal: Boolean = false, sliderValChange: (Float, View) -> Unit) {
    if (horizontal) {
        Row { slidersGroup(imgsize, sliderValChange) }
    }
    else {
        Column {
            slidersGroup(imgsize, sliderValChange)
            /*
            var text by remember { mutableStateOf("Hello, World!") }
            Button(onClick = {
                text = "Hello, Desktop!"
            }) {
                Text(text)
            }
            Text("na prawo od przycisku", modifier = Modifier.background(Color(0xff32ff7b))) // green
            // Kolor zachowuje się bardzo dziwnie. Pierwsze 2 cyfry szesnastkowe to alfa.
            // Ten kolor, który jest wyświetlany w interfejsie jest nieprawidłowy.
            // element będzie miał taki kolor jak gdy skasuje się pierwsze dwa ff.

            Text("Kotlin Compose / Jetpack Compose")
            //val bitmap = ImageBitmap(512, 512, ImageBitmapConfig.Argb8888)
            //val bitmap = ImageBitmap.imageResource()
            //Canvas(bitmap)
            */
        }
    }
}

@Composable
private fun getSliderColors(color: Color) = SliderDefaults.colors(
    activeTrackColor = color,
    inactiveTrackColor = Color.Black,
    thumbColor = color,
    inactiveTickColor = Color.Transparent,
    activeTickColor = Color.Transparent,
)


@Composable
@Preview
fun uiSliderBox(imgsize: Int, horizontal: Boolean = false, sliderDeclaration: () -> Unit) {
    if (horizontal) {
        Row { sliderDeclaration() }
    }
    else {
        Column {
            sliderDeclaration()
        }
    }
}
@Composable
@Preview
fun singleSlider(imgsize: Int, description: String, sliderValChange: (Float) -> Unit) {
    var thisSliderPosition by remember { mutableStateOf(Config.sliderRange.startVal) }
    val modifier = Modifier.width(imgsize.dp)
    Column {
        Text(description)
        Slider(
            value = thisSliderPosition,
            valueRange = Config.sliderRange.range,
            onValueChange = {
                thisSliderPosition = it
                sliderValChange(it)
                //println("Red $redSliderPosition")
            },
            colors = getSliderColors(Color.DarkGray),
            steps = 256,
            modifier = modifier,
        )
    }
}

/** dodaje 3 suwaki, musi być umieszczone w layoucie
 * @param sliderValChange float jest od 0 do 256, a View: Red-SLICE, Green-TOP, Blue-SIDE */
@Composable
@Preview
private fun slidersGroup(imgsize: Int, sliderValChange: (Float, View) -> Unit) {
    var redSliderPosition by remember { mutableStateOf(Config.sliderRange.startVal) }
    var greenSliderPosition by remember { mutableStateOf(Config.sliderRange.startVal) }
    var blueSliderPosition by remember { mutableStateOf(Config.sliderRange.startVal) }
    val modifier = Modifier.width(imgsize.dp)
    Column {
        Text("slice:")
        Slider(
            value = redSliderPosition,
            valueRange = Config.sliderRange.range,
            onValueChange = {
                redSliderPosition = it
                sliderValChange(it, View.SLICE)
                //println("Red $redSliderPosition")
            },
            colors = getSliderColors(Color.Red),
            steps = 256,
            modifier = modifier,
        )
    }
    Column {
        Text("top:")
        Slider(
            value = greenSliderPosition,
            valueRange = Config.sliderRange.range,
            onValueChange = {
                greenSliderPosition = it
                sliderValChange(it, View.TOP)
                //println("Green $greenSliderPosition")
            },
            colors = getSliderColors(Color.Green),
            steps = 256,
            modifier = modifier
        )
    }
    Column {
        Text("side:")
        Slider(
            value = blueSliderPosition,
            valueRange = Config.sliderRange.range,
            onValueChange = {
                blueSliderPosition = it
                sliderValChange(it, View.SIDE)
                //println("Blue $blueSliderPosition")
            },
            colors = getSliderColors(Color.Blue),
            steps = 256,
            modifier = modifier
        )
    }
}