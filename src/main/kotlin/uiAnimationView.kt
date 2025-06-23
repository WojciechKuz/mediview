import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import transform3d.ExtView

@Composable
@Preview
fun animationBlock(imgsize: Int, uiImageMap: MutableMap<ExtView, ImageBitmap?>, manager: UIManager) {
    Box {
        Image(
            choosePainter(uiImageMap[ExtView.FREE], "loading.jpg"),
            "slice XY",
            modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.DarkGray),
        )
    }
    Box {
        singleSlider(imgsize, "nic") {}
    }
}