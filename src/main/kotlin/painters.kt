import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import dicom.byteArrayToImageBitmap
import dicom.fileToImageBitmap


@Composable
fun getPainter(bytes: ByteArray): Painter {
    val imageBitmap = byteArrayToImageBitmap(bytes)
    if (imageBitmap != null) {
        return BitmapPainter( imageBitmap )
    }
    return painterResource("imagenotfound512.png")
}
@Composable
fun getPainter(fileName: String): Painter {
    //println("getPainter: File $fileName to ImageBitmap")
    val imageBitmap = fileToImageBitmap(fileName)
    if (imageBitmap != null) {
        return BitmapPainter( imageBitmap )
    }
    return painterResource("imagenotfound512.png")
}
@Composable
fun getPainter(imgBitmap: ImageBitmap?): Painter {  // We use this one for dicom images
    if (imgBitmap != null) {
        return BitmapPainter( imgBitmap )
    }
    println("Image changed, but it's null!")
    return painterResource("imagenotfound512.png")
}

@Composable
fun choosePainter(imageBitmap: ImageBitmap?, imgFile: String) = if (imageBitmap != null) {
    getPainter(imageBitmap)
} else {
    println("Displaying backup image as bitmap is null.")
    getPainter(imgFile)
}
