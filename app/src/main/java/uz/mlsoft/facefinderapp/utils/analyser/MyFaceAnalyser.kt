package uz.mlsoft.facefinderapp.utils.analyser

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class MyFaceAnalyser(private val onFindListener: (List<com.google.mlkit.vision.face.Face?>?) -> Unit) :
    ImageAnalysis.Analyzer {
    private val realTimeOps = FaceDetectorOptions.Builder()
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(0.20f)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(realTimeOps)

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        mediaImage?.let {
            val inputImage =
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    onFindListener.invoke(faces)

                    imageProxy.close()
                }
                .addOnFailureListener {

                    imageProxy.close()
                }
                .addOnCompleteListener {

                    imageProxy.close()
                }
        }

    }

}