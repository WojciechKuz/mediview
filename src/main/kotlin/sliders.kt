import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Okienko z suwakami */
@Composable
@Preview
fun uiSliders(imgsize: Int, horizontal: Boolean = false) {
    if (horizontal) {
        Row { slidersGroup(imgsize) }
    }
    else {
        Column {
            slidersGroup(imgsize)
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

/** dodaje 3 suwaki, musi być umieszczone w layoucie */
@Composable
@Preview
private fun slidersGroup(imgsize: Int) {
    var redSliderPosition by remember { mutableStateOf(128f) }
    var greenSliderPosition by remember { mutableStateOf(128f) }
    var blueSliderPosition by remember { mutableStateOf(128f) }
    val modifier = Modifier.width(imgsize.dp)
    Slider(
        value = redSliderPosition,
        valueRange = 0f..256f,
        onValueChange = {
            redSliderPosition = it
            println("Red $redSliderPosition")
        },
        colors = getSliderColors(Color.Red),
        steps = 256,
        modifier = modifier,
    )
    Slider(
        value = greenSliderPosition,
        valueRange = 0f..256f,
        onValueChange = {
            greenSliderPosition = it
            println("Green $greenSliderPosition")
        },
        colors = getSliderColors(Color.Green),
        steps = 256,
        modifier = modifier
    )
    Slider(
        value = blueSliderPosition,
        valueRange = 0f..256f,
        onValueChange = {
            blueSliderPosition = it
            println("Blue $blueSliderPosition")
        },
        colors = getSliderColors(Color.Blue),
        steps = 256,
        modifier = modifier
    )
}