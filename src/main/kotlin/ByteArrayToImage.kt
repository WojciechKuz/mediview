

//import android.graphics.BitmapFactory // android only
//import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

fun byteArrayToImageBitmap(byteArray: ByteArray): ImageBitmap? {
    try {
        val inputStream = ByteArrayInputStream(byteArray)
        val image = ImageIO.read(inputStream) // FIXME it's not that simple to read it as image
        return image.toComposeImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}