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
import transform3d.ExtView

/** Okienko z suwakami.
 * @param sliderValChange float jest od 0 do 256, a View: Red-SLICE, Green-TOP, Blue-SIDE */
@Composable
@Preview
fun uiSliders(imgsize: Int, horizontal: Boolean = false, sliderValChange: (Float, ExtView) -> Unit) {
    if (horizontal) {
        Row { slidersGroup(imgsize, sliderValChange) }
    }
    else {
        Column {
            slidersGroup(imgsize, sliderValChange)
        }
    }
}

@Composable
fun getSliderColors(color: Color) = SliderDefaults.colors(
    activeTrackColor = color,
    inactiveTrackColor = Color.Black,
    thumbColor = color,
    inactiveTickColor = Color.Transparent,
    activeTickColor = Color.Transparent,
)


@Composable
@Preview
fun uiSliderBox(imgsize: Int, horizontal: Boolean = false, sliderDeclaration: @Composable () -> Unit) {
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
            steps = Config.sliderSteps,
            modifier = modifier,
        )
    }
}

/** dodaje 3 suwaki, musi być umieszczone w layoucie
 * @param sliderValChange float jest od 0 do 256, a View: Red-SLICE, Green-TOP, Blue-SIDE */
@Composable
@Preview
private fun slidersGroup(imgsize: Int, sliderValChange: (Float, ExtView) -> Unit) {
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
                sliderValChange(it, ExtView.SLICE)
                //println("Red $redSliderPosition")
            },
            colors = getSliderColors(Color.Red),
            steps = Config.sliderSteps,
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
                sliderValChange(it, ExtView.TOP)
                //println("Green $greenSliderPosition")
            },
            colors = getSliderColors(Color.Green),
            steps = Config.sliderSteps,
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
                sliderValChange(it, ExtView.SIDE)
                //println("Blue $blueSliderPosition")
            },
            colors = getSliderColors(Color.Blue),
            steps = Config.sliderSteps,
            modifier = modifier
        )
    }
}