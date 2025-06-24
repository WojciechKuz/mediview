import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import transform3d.Angle
import transform3d.ExtView

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
        Row {
            var flatSliderPosition by remember { mutableStateOf(Config.sliderRange.startVal) }
            val modifier = Modifier.width(imgsize.dp)
            Column {
                Text("kąt poziomy")
                Slider(
                    value = flatSliderPosition,
                    valueRange = Config.sliderRange.range,
                    onValueChange = {
                        flatSliderPosition = it
                        manager.angleSliderChange(it, Angle.XZAngle)
                    },
                    colors = getSliderDefaultColors(Color.DarkGray),
                    steps = Config.sliderSteps,
                    modifier = modifier,
                )
            }
            singleSlider(imgsize, "kąt pionowy") { manager.angleSliderChange(it, Angle.YZAngle) }
            singleSlider(imgsize, "głębokość") { manager.viewSliderChange(it, ExtView.FREE)}
        }
    }
}
