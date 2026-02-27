package com.example.nextgen_pds_kiosk.ml

import kotlin.math.sqrt

object EmbeddingComparator {

    /**
     * Calculates the Cosine Similarity between two N-dimensional embedding vectors.
     * Returns a value between -1.0 and 1.0 (1.0 is a perfect match).
     */
    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) return 0f

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }

        // Prevent divide by zero error
        if (norm1 == 0f || norm2 == 0f) return 0f 
        
        return dotProduct / (sqrt(norm1) * sqrt(norm2))
    }

    /**
     * Calculates the Euclidean (L2) Distance between two N-dimensional embedding vectors.
     * DeepFace and standard FaceNet implementations typically use this metric.
     * Returns a positive float value (0.0 represents a perfect identical match).
     * 
     * To ensure thresholds like < 0.8 work across all FaceNet variants, we MUST
     * L2-Normalize the vectors first. This maps them to a unit hypersphere.
     */
    fun euclideanDistance(v1: FloatArray, v2: FloatArray): Float {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) return 0f

        // 1. Calculate L2 Norms for both vectors
        var norm1 = 0f
        var norm2 = 0f
        for (i in v1.indices) {
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }
        val sqrtNorm1 = sqrt(norm1).coerceAtLeast(1e-10f)
        val sqrtNorm2 = sqrt(norm2).coerceAtLeast(1e-10f)

        // 2. Compute Euclidean Distance on the Normalized Vectors
        var sumSq = 0f
        for (i in v1.indices) {
            // L2-Normalize each element on the fly
            val n1 = v1[i] / sqrtNorm1
            val n2 = v2[i] / sqrtNorm2
            
            val diff = n1 - n2
            sumSq += diff * diff
        }

        return sqrt(sumSq)
    }
}
