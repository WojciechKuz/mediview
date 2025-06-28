import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.ZeroLineProperties

// MutableState<ShortArray>

@Composable
fun showGraphForData(imgsize: Int, color: Color, valueArray: ShortArray) {
    val modifier = Modifier.width(imgsize.dp).height(Config.graphHeight.dp).border(1.dp, color)
    LineChart(
        modifier = modifier,
        data = listOf(
            Line(
                label = "graph labels",
                values = valueArray.map { it.toDouble() },
                color = SolidColor(color),
                strokeAnimationSpec = tween(durationMillis = 0),
                gradientAnimationSpec = tween(durationMillis = 0) // disable these stupid animations
            )
        ),
        zeroLineProperties = ZeroLineProperties(
            enabled = true,
            color = SolidColor(Color.Black),
        ),
        animationMode = AnimationMode.Together { 0L }
    )
}

//BarGraph(
//data = listOf(BarData(x = "22", y = 20), BarData(x = "23", y = 30)),
//)