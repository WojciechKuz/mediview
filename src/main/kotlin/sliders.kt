import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.Slider
import androidx.compose.material.SliderColors
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import transform3d.ExtView


@Composable
fun getSliderDefaultColors(color: Color) = SliderDefaults.colors(
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
            },
            colors = getSliderDefaultColors(Color.DarkGray),
            steps = Config.sliderSteps,
            modifier = modifier,
        )
    }
}

@Composable
@Preview
fun colorfulSlider(imgsize: Int, description: String, colors: SliderColors, startVal: Float = Config.sliderRange.startVal, sliderValChange: (Float) -> Unit) {
    var thisSliderPosition by remember { mutableStateOf(startVal) }
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
            colors = colors,
            steps = Config.sliderSteps,
            modifier = modifier,
        )
    }
}