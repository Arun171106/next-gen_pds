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
                    
                    // 3. Crop face using bounding box coordinates
                    val croppedFace = cropFace(bitmap, face.boundingBox)
                    
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

    private fun cropFace(bitmap: Bitmap, boundingBox: Rect): Bitmap? {
        return try {
            // Apply padding to bounding box if needed, ensuring indices are within bounds
            val x = boundingBox.left.coerceAtLeast(0)
            val y = boundingBox.top.coerceAtLeast(0)
            val width = boundingBox.width().coerceAtMost(bitmap.width - x)
            val height = boundingBox.height().coerceAtMost(bitmap.height - y)

            if (width > 0 && height > 0) {
                Bitmap.createBitmap(bitmap, x, y, width, height)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
