import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
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
    Box(modifier = Modifier.fillMaxSize()) { uiSliders(imgsize, true, manager::viewSliderChange) }
}