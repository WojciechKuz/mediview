import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import transform3d.ExtView

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
                            manager.viewTapChange(absx, absy, ExtView.SLICE)
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
                            manager.viewTapChange(absx, absy, ExtView.TOP)
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
                            manager.viewTapChange(absx, absy, ExtView.SIDE)
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
    Box { ui3Sliders(imgsize, manager::viewSliderChange) }
}
/** Okienko z suwakami. dodaje 3 suwaki, musi być umieszczone w layoucie.
 * @param sliderValChange float jest od 0 do 256, a View: Red-SLICE, Green-TOP, Blue-SIDE */
@Composable
@Preview
private fun ui3Sliders(imgsize: Int, sliderValChange: (Float, ExtView) -> Unit) {
    var redSliderPosition by remember { mutableStateOf(Config.sliderRange.startVal) }
    var greenSliderPosition by remember { mutableStateOf(Config.sliderRange.startVal) }
    var blueSliderPosition by remember { mutableStateOf(Config.sliderRange.startVal) }
    val modifier = Modifier.width(imgsize.dp)
    Row {
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
                colors = getSliderDefaultColors(Color.Red),
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
                colors = getSliderDefaultColors(Color.Green),
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
                colors = getSliderDefaultColors(Color.Blue),
                steps = Config.sliderSteps,
                modifier = modifier
            )
        }
    }
}