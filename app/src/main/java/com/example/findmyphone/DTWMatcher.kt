package com.example.findmyphone

class DTWMatcher {
    fun similarity(template: FloatArray, sample: FloatArray): Double {
        val n = template.size
        val m = sample.size
        val dtw = Array(n + 1) { DoubleArray(m + 1) { Double.POSITIVE_INFINITY } }
        dtw[0][0] = 0.0
        for (i in 1..n) {
            for (j in 1..m) {
                val cost = Math.abs(template[i - 1] - sample[j - 1])
                dtw[i][j] = cost + minOf(dtw[i - 1][j], dtw[i][j - 1], dtw[i - 1][j - 1])
            }
        }
        return dtw[n][m] // Lower value means more similar
    }
}
