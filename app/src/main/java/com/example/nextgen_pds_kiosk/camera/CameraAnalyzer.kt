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
                // Note: Image space has Y pointing down, so angle calculations might be inverted depending on camera orientation.
                val angleRads = atan2(deltaY.toDouble(), deltaX.toDouble())
                angleDegrees = Math.toDegrees(angleRads).toFloat()
            }

            // 2. Setup Rotation Matrix centered on the Face Bounding Box
            val matrix = Matrix()
            matrix.postRotate(angleDegrees, bounds.exactCenterX(), bounds.exactCenterY())

            // 3. Create a fully rotated version of the main bitmap 
            // (In a highly optimized environment, we would only extract the crop area, but this is accurate enough)
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            // Since the whole image rotated around the face center, the face center hasn't moved relative to the canvas,
            // but the bounds width/height might need expansion. For FaceNet, a tight square crop is usually best.
            // We'll pad the bounding box slightly (like DeepFace does) to include the full chin and forehead.
            val padding = (bounds.width() * 0.15f).toInt()
            
            val cropLeft = (bounds.left - padding).coerceAtLeast(0)
            val cropTop = (bounds.top - padding).coerceAtLeast(0)
            
            // Calculate width and height, clamping to the rotated bitmap boundaries
            val cropRight = (bounds.right + padding).coerceAtMost(rotatedBitmap.width - 1)
            val cropBottom = (bounds.bottom + padding).coerceAtMost(rotatedBitmap.height - 1)

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
