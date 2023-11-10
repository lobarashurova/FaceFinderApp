package uz.mlsoft.facefinderapp

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.camera.view.CameraController.UseCases
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import uz.mlsoft.facefinderapp.utils.analyser.MyFaceAnalyser
import uz.mlsoft.facefinderapp.utils.myLog
import uz.mlsoft.facefinderapp.utils.myLog2
import uz.mlsoft.facefinderapp.utils.takePhoto
import uz.mlsoft.facefinderapp.utils.takePhoto2


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceRecognizerContent() {
    val context = LocalContext.current
    val lifeCycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
    val imageCapture = ImageCapture.Builder()
        .setTargetRotation(context.display!!.rotation)
        .build()

    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    val imageAnalysis = ImageAnalysis.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
        )
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setImageQueueDepth(10)
        .build()

    var detectedText by remember {
        mutableStateOf("")
    }
    val controller = remember {
        LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_CAPTURE) }
    }
    var count = 0
    val viewX = remember { mutableStateOf(0f) }
    val viewY = remember { mutableStateOf(0f) }
    fun onFaceUpdated(faces: List<Face?>?) {
        faces?.let {
            if (faces.isNotEmpty()) {
                val currectFace = faces[0]
                val leftEye = currectFace!!.getContour(FaceContour.LEFT_EYE)
                val rightEye = currectFace.getContour(FaceContour.RIGHT_EYE)
                if (rightEye == null && leftEye != null) {
                    Toast.makeText(context, "You are looking right now", Toast.LENGTH_SHORT)
                        .show()
                } else if (leftEye == null && rightEye != null) {
                    Toast.makeText(context, "You are looking left now", Toast.LENGTH_SHORT)
                        .show()
                }
                val boundingBox = faces[0]!!.boundingBox
                val left = boundingBox.left.toFloat()
                val top = boundingBox.top.toFloat()
                val right = boundingBox.right.toFloat()
                val bottom = boundingBox.bottom.toFloat()
                myLog("top:$top, left:$left, right:$right, bottom:$bottom")
                if (((top < 225 && top > 0) && (bottom < 470 && bottom > 0)) && (faces.size == 1)) {
                    count++
                    if (count == 1) {
//                        Toast.makeText(context,"Congratulations", Toast.LENGTH_SHORT).show()
//                        takePhoto(controller, context)
                        val useCase = UseCaseGroup.Builder()
                            .addUseCase(imageAnalysis)
                            .build()

                        cameraProvider.unbind(useCase.useCases[0])
                        takePhoto2(context, imageCapture)
                    }
                    detectedText = "Congratulations!"
                } else if (faces.size > 1) {
                    detectedText = "Too many faces"
                } else {
                    detectedText = "Try again"
                }
            }
        }

    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(text = "Face detector") }) },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                factory = { context ->
                    val previewView = PreviewView(context)
                    val executor = ContextCompat.getMainExecutor(context)
                    cameraProviderFuture.addListener({
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        imageAnalysis.apply {
                            setAnalyzer(executor, MyFaceAnalyser {
                                onFaceUpdated(it)
                            })
                        }

                        val useCase = UseCaseGroup.Builder()
                            .addUseCase(preview)
                            .addUseCase(imageCapture)
                            .addUseCase(imageAnalysis)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifeCycleOwner,
                                cameraSelector,
                                useCase
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                        }
                    }, executor)
                    previewView
                })

            Canvas(
                modifier = Modifier
                    .size(350.dp, 350.dp)
                    .align(Alignment.Center)
                    .onGloballyPositioned { coordinates ->
                        viewX.value = coordinates.positionInRoot().x
                        viewY.value = coordinates.positionInRoot().y
                        myLog2("viewx:${viewX.value}, viewY:${viewY.value}")

                    }
            ) {
                drawRect(Color.Transparent, size = size)
                drawOval(
                    color = Color.Black, // Border color
                    style = Stroke(width = 1.dp.toPx()) // Border width
                )
            }
            Text(
                text = detectedText,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Blue)
                    .padding(16.dp),
                color = Color.Black,
                fontWeight = FontWeight.SemiBold,
                fontSize = 35.sp,
                textAlign = TextAlign.Center
            )
        }

    }

}