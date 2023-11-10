package uz.mlsoft.facefinderapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream


fun takePhoto(
    controller: LifecycleCameraController,
    context: Context
) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                Toast.makeText(context, "Image saved", Toast.LENGTH_SHORT).show()
                val imageUri = getImageUri(context, image.toBitmap())
                saveImageToInternalStorage(context, imageUri!!)
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "Error:${exception.localizedMessage}", Toast.LENGTH_SHORT)
                    .show()
                myLog2("exception:${exception.imageCaptureError}, ${exception.localizedMessage}")
            }
        })

}

fun saveImageToInternalStorage(context: Context, uri: Uri) {
    val inputStream = context.contentResolver.openInputStream(uri)
    val outputStream = context.openFileOutput("image.jpg", Context.MODE_PRIVATE)
    inputStream?.use { input ->
        outputStream.use { output ->
            input.copyTo(output)
        }
    }
}

fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
    val bytes = ByteArrayOutputStream()
    inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
    val path =
        MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
    return Uri.parse(path)
}