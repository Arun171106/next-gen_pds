package com.example.nextgen_pds_kiosk.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FaceRecognizer(context: Context) {

    private var interpreter: Interpreter? = null
    
    // Standard MobileFaceNet / FaceNet expected inputs
    private val inputImageWidth = 112
    private val inputImageHeight = 112
    
    // Output dimensionality of the embedding vector (e.g., 128, 192, 512 depending on model)
    private val outputSize = 128 

    init {
        try {
            val modelBuffer = loadModelFile(context, "facenet.tflite")
            val options = Interpreter.Options()
            options.setNumThreads(4)
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            System.err.println("FaceNet TFLite model not found in assets. Place facenet.tflite in src/main/assets/")
            e.printStackTrace()
        }
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Preprocesses the cropped face bitmap and runs inference.
     * Returns a 128D FloatArray embedding vector representing the face.
     */
    fun getEmbedding(bitmap: Bitmap): FloatArray {
        if (interpreter == null) {
            // Return dummy representation if model is missing so pipeline still compiles and runs
            return FloatArray(outputSize) { 0f }
        }

        // 1. Preprocess Image (Resize and Normalize to [-1, 1])
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(127.5f, 127.5f)) 
            .build()

        val tensorImage = TensorImage.fromBitmap(bitmap)
        val processedImage = imageProcessor.process(tensorImage)

        // 2. Output Buffer Matrix
        val outputBuffer = Array(1) { FloatArray(outputSize) }
        
        // 3. Run Inference
        interpreter?.run(processedImage.buffer, outputBuffer)

        return outputBuffer[0]
    }
    
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
