package com.example.nextgen_pds_kiosk.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.nextgen_pds_kiosk.ml.FaceDetector
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.atan2
import kotlin.math.PI

class CameraAnalyzer(
    private val scope: CoroutineScope,
    private val onFaceDetected: (Bitmap, Face) -> Unit
) : ImageAnalysis.Analyzer {

    private val faceDetector = FaceDetector()
    private var isProcessing = false

    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }
        isProcessing = true

        scope.launch(Dispatchers.Default) {
            try {
                // 1. Run ML Kit Face Detection
                val faces = faceDetector.processImageProxy(imageProxy)
                
                if (faces.isNotEmpty()) {
                    // Only process the first distinct face
                    val face = faces.first()
                    
                    // 2. Convert to Bitmap
                    val bitmap = imageProxy.toBitmap()
                    
                    // 3. Extract and align crop using DeepFace-equivalent Eye rotation
                    val croppedFace = cropFace(bitmap, face)
                    
                    if (croppedFace != null) {
                        onFaceDetected(croppedFace, face)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                imageProxy.close()
                isProcessing = false
            }
        }
    }

    private fun cropFace(bitmap: Bitmap, face: Face): Bitmap? {
        return try {
            val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
            val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
            
            val bounds = face.boundingBox

            // 1. Calculate Rotation Angle if we have both eyes
            var angleDegrees = 0f
            if (leftEye != null && rightEye != null) {
                val deltaY = rightEye.y - leftEye.y
                val deltaX = rightEye.x - leftEye.x
                // Image space has Y pointing down. atan2 gives math geometry angle.
                // We invert it so the Matrix postRotate (which rotates clockwise) levels the eyes.
                val angleRads = atan2(deltaY.toDouble(), deltaX.toDouble())
                angleDegrees = -Math.toDegrees(angleRads).toFloat()
            }

            // 2. Setup Rotation Matrix centered on the Face Bounding Box
            val matrix = Matrix()
            matrix.postRotate(angleDegrees, bounds.exactCenterX(), bounds.exactCenterY())

            // 3. Create a fully rotated version of the main bitmap 
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            // 4. Extract Crop with DeepFace ratio padding
            // We pad the bounding box slightly (15%) to include the full chin and forehead, clamping to avoid crashes
            val paddingX = (bounds.width() * 0.15f).toInt()
            val paddingY = (bounds.height() * 0.15f).toInt()
            
            val cropLeft = (bounds.left - paddingX).coerceAtLeast(0)
            val cropTop = (bounds.top - paddingY).coerceAtLeast(0)
            val cropRight = (bounds.right + paddingX).coerceAtMost(rotatedBitmap.width)
            val cropBottom = (bounds.bottom + paddingY).coerceAtMost(rotatedBitmap.height)

            val width = cropRight - cropLeft
            val height = cropBottom - cropTop

            if (width > 0 && height > 0) {
                Bitmap.createBitmap(rotatedBitmap, cropLeft, cropTop, width, height)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
