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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }

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
        Column {
            Row {
                Image(painterResource("topBridgeEnter.png"), "opis",
                    modifier = Modifier.width(300.dp).height(300.dp).border(1.dp, Color.Red)
                )
                Image(painterResource("topBridgeEnter.png"), "opis",
                    modifier = Modifier.width(300.dp).height(300.dp).border(1.dp, Color.Green)
                    //modifier = Modifier.drawBehind {}
                )
            }
            Row {
                Image(painterResource("topBridgeEnter.png"), "opis",
                    modifier = Modifier.width(300.dp).height(300.dp).border(1.dp, Color.Blue)
                )
                Column {
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
                    var redSliderPosition by remember { mutableStateOf(128f) }
                    var greenSliderPosition by remember { mutableStateOf(128f) }
                    var blueSliderPosition by remember { mutableStateOf(128f) }
                    Slider(
                        value = redSliderPosition,
                        valueRange = 0f..256f,
                        onValueChange = {
                            redSliderPosition = it
                            println("Red $redSliderPosition")
                        },
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Red,
                        ),
                        steps = 256,
                    )
                    Slider(
                        value = greenSliderPosition,
                        valueRange = 0f..256f,
                        onValueChange = {
                            greenSliderPosition = it
                            println("Green $greenSliderPosition")
                        },
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Green,
                        ),
                        steps = 256,
                    )
                    Slider(
                        value = blueSliderPosition,
                        valueRange = 0f..256f,
                        onValueChange = {
                            blueSliderPosition = it
                            println("Blue $blueSliderPosition")
                        },
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Blue,
                        ),
                        steps = 256,
                    )
                }
            }
        }

    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "MediView by wojkuzb") {
        App()
    }
}
