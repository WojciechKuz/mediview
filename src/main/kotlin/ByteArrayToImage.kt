

//import android.graphics.BitmapFactory // android only
//import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
/*
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
*/

fun byteArrayToImageBitmap(byteArray: ByteArray): ImageBitmap? {
    try {
        val inputStream = ByteArrayInputStream(byteArray)
        val image = ImageIO.read(inputStream) // imageIO does not support SOI of FFC3
        //val image = Image.makeFromEncoded(byteArray) // skija failed

        return image.toComposeImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

// won't change anything, as Twelve Monkeys library overwrites ImageIO calls
fun byteArrayToImageBitmap12monkeys(byteArray: ByteArray): ImageBitmap? {
    try {
        val inputStream = ByteArrayInputStream(byteArray)
        val image = ImageIO.read(inputStream) // imageIO does not support SOI of FFC3
        //val image = Image.makeFromEncoded(byteArray) // skija failed

        return image.toComposeImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
/*
@Composable
fun byteArrayToCoilPainter(byteArray: ByteArray): AsyncImagePainter {
    val context = LocalPlatformContext.current

    val imageRequest = ImageRequest.Builder(context)
        .data(byteArray)
        .size(Size.ORIGINAL)
        .build()
    return rememberAsyncImagePainter(imageRequest)
}
*/