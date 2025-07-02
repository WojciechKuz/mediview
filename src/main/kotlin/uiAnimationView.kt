import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.ui.graphics.Color
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import dicom.saveImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import transform3d.Angle
import transform3d.ExtView
import transform3d.InterpolationSA
import transform3d.coroutineForLoop
import transform3d.printAngles
import kotlin.math.min

/** if speed's less than 1, show it as fraction */
fun iShowSpeedNicely(speed: Float) = if(speed < 1f) "1/${(1f/speed).toInt()}x" else "${speed.toInt()}x"

fun exportAnimation(animate: AnimationManager) { // TODO test export animation
    val dir = ReadHelp.pickWriteDirJFC()
    if(dir.isEmpty()) return
    val allFrames = animate.allFrames

    CoroutineScope(Dispatchers.Default).launch {
        coroutineForLoop(allFrames.size) { i ->
            saveImage(dir, "animation_frame${i.toString().padStart(3, '0')}.png", allFrames[i])
        }
    }.invokeOnCompletion {
        animate.infoTextSetter?.set("Animation exported")
        println("animation exported")
    }
}

@Composable
@Preview
fun animationBlock(imgsize: Int, uiImageMap: MutableMap<ExtView, ImageBitmap?>, manager: UIManager) {
    val modifier = Modifier.padding(end = 2.dp)
    Box {
        Row {
            // animation control code
            var animate by remember { mutableStateOf(AnimationManager(manager)) }
            // I wanted to make frame generation progress like: "12/30 frames generated". But it's parallelized by z axis, not by frame count.
            var infoText by remember { mutableStateOf("waiting for animation parameters")}
            if(animate.infoTextSetter == null) animate.infoTextSetter = UISetter { infoText = it }

            var speed by remember { mutableStateOf(1f) }
            var play by remember { mutableStateOf(false) }
            if(animate.genFinish == null) animate.genFinish = UISetter { play = it } // when finished generation

            var frameCountText by remember { mutableStateOf("0") }
            var frameIdx by remember { mutableStateOf(0) }
            var frame by remember { mutableStateOf<ImageBitmap?>(null) }

            var framesSliderPosition by remember { mutableStateOf(4f) }
            animate.setSliderPos = UISetter { framesSliderPosition = it }
            var sliderRange: ClosedFloatingPointRange<Float> by remember { mutableStateOf(0f..0f) }
            animate.setFrameRange = UISetter { sliderRange = it }

            LaunchedEffect(animate, play) {
                println("launchEffect, play is $play")
                while(play) {
                    animate.safeGetFrame(frameIdx) {
                        frame = it
                    }
                    delay((1000/speed).toLong())
                    framesSliderPosition = InterpolationSA.interpolate2Values(
                        sliderRange.start, sliderRange.endInclusive, frameIdx * 1f / animate.framesCount
                    )
                    frameIdx = animate.nextFrameIdx(frameIdx)
                }
            }

            // image
            Image(
                choosePainter(frame, "loading.jpg"),
                "animated projection",
                modifier = Modifier.width(imgsize.dp).height(imgsize.dp).border(1.dp, Color.DarkGray)
            )
            Box(
                modifier = Modifier.width((2*imgsize).dp).height(imgsize.dp).padding(start = 2.dp)
            ) { Column {
                // animation control panel
                var firstAngleText by remember { mutableStateOf<String>("Set starting angles") }
                var secondAngleText by remember { mutableStateOf<String>("Set finishing angles") }

                Row {
                    Button(
                        onClick = {
                            animate.animFrameCount = min(frameCountText.trim().toInt(), Config.maxAnimFrameCount)
                            animate.generateAnimation()
                        }, modifier = modifier
                    ) {
                        Text("Generate")
                    } // generate
                    Button(
                        onClick = {
                            exportAnimation(animate)
                        },
                        modifier = modifier
                    ) {
                        Text("Export")
                    }
                    Text(infoText)
                }

                Row {
                    Button(
                        onClick = {
                            animate.animStartAngles = manager.angleValues.map { (key, value) -> key to value * 180f }.toMap().toMutableMap()
                            firstAngleText = "start angle ${printAngles(animate.animStartAngles)}"
                        }, modifier = modifier
                    ) {
                        Text("start angle")
                    }
                    Text(firstAngleText)
                } // start angle

                Row {
                    Button(
                        onClick = {
                            animate.animEndAngles = manager.angleValues.map { (key, value) -> key to value * 180f }.toMap().toMutableMap() //manager.angleValues.toMutableMap() // copy no change
                            secondAngleText = "end angle ${printAngles(animate.animEndAngles)}"
                        }, modifier = modifier
                    ) {
                        Text("finish angle")
                    }
                    Text(secondAngleText)
                } // end angle

                Row {
                    Text("frame count", modifier = modifier)
                    TextField(
                        value = frameCountText,
                        onValueChange = {
                            frameCountText = it
                        }
                    )
                } // frame count

                Row {
                    var playPauseText by remember { mutableStateOf<String>("Play") }
                    playPauseText = if(play) "Pause" else "Play"
                    Button(
                        onClick = {
                            play = !play
                        }, modifier = modifier
                    ) {
                        Text(playPauseText)
                    } // play / pause
                    Button(
                        onClick = {
                            animate.aMode = when(animate.aMode) {
                                AnimationManager.Animode.LOOP -> AnimationManager.Animode.REVERSE
                                AnimationManager.Animode.REVERSE -> AnimationManager.Animode.PAUSE
                                AnimationManager.Animode.PAUSE -> AnimationManager.Animode.LOOP
                            }
                        }
                    ) {
                        Text("Play mode ${animate.aMode}")
                    } // play mode
                }

                Row {
                    Text("Speed: ", modifier = modifier)
                    Button(
                        onClick = {
                            if(speed / 2f >= Config.minSpeed)
                                speed = speed / 2f
                        },
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Text(iShowSpeedNicely(speed/2f))
                    } // slower
                    Box(modifier = Modifier.padding(end = 2.dp)) { Text(iShowSpeedNicely(speed)) }
                    Button(
                        onClick = {
                            if(speed * 2f <= Config.maxSpeed)
                                speed = speed * 2f
                        },
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Text(iShowSpeedNicely(speed*2f))
                    } // faster
                } // speed control

                Row {
                    Column {
                        Text("Animation:")
                        Slider(
                            value = framesSliderPosition,
                            valueRange = sliderRange,
                            onValueChange = {
                                framesSliderPosition = it
                                if(it.toInt() < animate.framesCount) {
                                    frameIdx = it.toInt()
                                    animate.safeGetFrame(frameIdx) { fr -> frame = fr }
                                }
                            },
                            colors = getSliderDefaultColors(Color.DarkGray),
                            steps = Config.sliderSteps,
                            modifier = Modifier.width(imgsize.dp),
                        )
                    }
                } // frame slider

            } } // box & column end
        }

    }
    Box {
        Row {
            var flatSliderPosition by remember { mutableStateOf(Config.sliderRange.startVal) }
            var verticalSliderPosition by remember { mutableStateOf(Config.sliderRange.startVal) }
            fun getUISetter(setter: UISetter<Float>): UISetter<Float> { return setter } // functional interfaces can be set from lambda only through function parameter?
            if(manager.angleSetters[Angle.XZAngle] == null) {
                manager.angleSetters[Angle.XZAngle] = getUISetter{ flatSliderPosition = it }
            }
            if(manager.angleSetters[Angle.YZAngle] == null) {
                manager.angleSetters[Angle.YZAngle] = getUISetter{ verticalSliderPosition = it }
            }
            val modifier = Modifier.width(imgsize.dp)
            Column {
                Text("kąt poziomy ${"%.2f".format(manager.scaleAngleSlider(flatSliderPosition))}°")
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
            Column {
                Text("kąt pionowy ${"%.2f".format(manager.scaleAngleSlider(verticalSliderPosition))}°")
                Slider(
                    value = verticalSliderPosition,
                    valueRange = Config.sliderRange.range,
                    onValueChange = {
                        verticalSliderPosition = it
                        manager.angleSliderChange(it, Angle.YZAngle)
                    },
                    colors = getSliderDefaultColors(Color.DarkGray),
                    steps = Config.sliderSteps,
                    modifier = modifier,
                )
            }
            /*
            var depthSliderPosition by remember { mutableStateOf(Config.sliderRange.startVal) }
            Column {
                Text("głębokość ${manager.scaleDepthSlider(ExtView.FREE, depthSliderPosition)}")
                Slider(
                    value = depthSliderPosition,
                    valueRange = Config.sliderRange.range,
                    onValueChange = {
                        depthSliderPosition = it
                        manager.viewSliderChange(it, ExtView.FREE)
                    },
                    colors = getSliderDefaultColors(Color.DarkGray),
                    steps = Config.sliderSteps,
                    modifier = modifier,
                )
            }
            */
        }
    }
}