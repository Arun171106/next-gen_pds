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
}
